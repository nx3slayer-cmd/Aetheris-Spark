package com.kallistocore.ai.data.manager

import android.content.Context
import android.os.StatFs
import com.kallistocore.ai.data.models.AIModelInfo
import com.kallistocore.ai.data.models.DownloadStatus
import com.kallistocore.ai.data.models.ModelCategory
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import okhttp3.OkHttpClient
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class ModelDownloadProgress(
    val modelId: String,
    val status: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null,
    val isResumable: Boolean = false
)

class ModelManager(private val context: Context) {

    // Background SupervisorScope that survives screen transitions
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = mutableMapOf<String, Job>()

    private val okHttpClient = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(true)
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .build()

    private val httpClient = HttpClient(OkHttp) {
        engine {
            preconfigured = okHttpClient
        }
    }

    val baseModelsDir: File by lazy {
        File(context.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }
    }

    private val prefs = context.getSharedPreferences("kallisto_hf_prefs", Context.MODE_PRIVATE)

    var hfToken: String
        get() = prefs.getString("hf_token", "") ?: ""
        set(value) = prefs.edit().putString("hf_token", value.trim()).apply()

    private val _downloadStates = MutableStateFlow<Map<String, ModelDownloadProgress>>(emptyMap())
    val downloadStates: StateFlow<Map<String, ModelDownloadProgress>> = _downloadStates.asStateFlow()

    private val _activeLlmPath = MutableStateFlow<String?>(null)
    val activeLlmPath: StateFlow<String?> = _activeLlmPath.asStateFlow()

    fun getCategoryDir(category: ModelCategory): File {
        val subDir = when (category) {
            ModelCategory.CHAT_LLM -> "llm"
            ModelCategory.VOICE_TTS -> "tts"
            ModelCategory.IMAGE_GEN_AND_EDIT -> "image"
        }
        return File(baseModelsDir, subDir).apply {
            if (!exists()) mkdirs()
        }
    }

    fun getModelFile(model: AIModelInfo): File {
        return File(getCategoryDir(model.category), model.fileName)
    }

    fun getTempFile(model: AIModelInfo): File {
        return File(getCategoryDir(model.category), "${model.fileName}.tmp")
    }

    fun isModelDownloaded(model: AIModelInfo): Boolean {
        val file = getModelFile(model)
        return file.exists() && file.length() > 0
    }

    fun getAvailableStorageBytes(): Long {
        return try {
            val stat = StatFs(context.filesDir.path)
            stat.availableBlocksLong * stat.blockSizeLong
        } catch (_: Exception) {
            10_000_000_000L
        }
    }

    fun formatBytes(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        return when {
            gb >= 1.0 -> String.format("%.2f GB", gb)
            mb >= 1.0 -> String.format("%.1f MB", mb)
            else -> String.format("%.0f KB", kb)
        }
    }

    /**
     * Resumable Download Engine with HTTP Range headers and Background Execution.
     */
    fun downloadModel(model: AIModelInfo) {
        if (activeJobs[model.id]?.isActive == true) return

        val job = managerScope.launch {
            val destinationFile = getModelFile(model)
            val tempFile = getTempFile(model)

            val existingBytes = if (tempFile.exists()) tempFile.length() else 0L

            if (getAvailableStorageBytes() < (model.sizeBytes - existingBytes)) {
                _downloadStates.update { current ->
                    current + (model.id to ModelDownloadProgress(
                        modelId = model.id,
                        status = DownloadStatus.FAILED,
                        errorMessage = "Insufficient storage space on device"
                    ))
                }
                return@launch
            }

            try {
                _downloadStates.update { current ->
                    current + (model.id to ModelDownloadProgress(
                        modelId = model.id,
                        status = DownloadStatus.DOWNLOADING,
                        downloadedBytes = existingBytes,
                        totalBytes = model.sizeBytes,
                        progress = if (model.sizeBytes > 0) (existingBytes.toFloat() / model.sizeBytes).coerceIn(0f, 1f) else 0f
                    ))
                }

                httpClient.prepareGet(model.downloadUrl) {
                    // 1. Send Resume Range Header if partial file exists
                    if (existingBytes > 0) {
                        header(HttpHeaders.Range, "bytes=$existingBytes-")
                    }
                    // 2. Inject Hugging Face Auth Token if user entered one
                    if (hfToken.isNotBlank()) {
                        header(HttpHeaders.Authorization, "Bearer $hfToken")
                    }
                    header("User-Agent", "KallistoCore-Android/1.0")
                }.execute { httpResponse ->

                    val statusCode = httpResponse.status.value
                    if (statusCode == 401 || statusCode == 403) {
                        throw IllegalStateException("Hugging Face Auth Required (401/403). Enter your HF Token in Settings.")
                    }
                    if (statusCode == 404) {
                        throw IllegalStateException("File not found on Hugging Face (404). Check model repository.")
                    }
                    if (statusCode !in 200..299) {
                        throw IllegalStateException("HTTP Server Error: ${httpResponse.status}")
                    }

                    val isPartial = statusCode == 206
                    val appendMode = isPartial && existingBytes > 0
                    var currentDownloaded = if (appendMode) existingBytes else 0L

                    val responseContentLength = httpResponse.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: 0L
                    val totalLength = if (isPartial) (existingBytes + responseContentLength) else if (responseContentLength > 0) responseContentLength else model.sizeBytes

                    val channel: ByteReadChannel = httpResponse.bodyAsChannel()
                    val buffer = ByteArray(64 * 1024) // 64 KB chunks

                    FileOutputStream(tempFile, appendMode).use { outputStream ->
                        while (!channel.isClosedForRead && isActive) {
                            val read = channel.readAvailable(buffer, 0, buffer.size)
                            if (read <= 0) break
                            outputStream.write(buffer, 0, read)
                            currentDownloaded += read

                            val progressFraction = (currentDownloaded.toFloat() / totalLength).coerceIn(0f, 1f)
                            _downloadStates.update { current ->
                                current + (model.id to ModelDownloadProgress(
                                    modelId = model.id,
                                    status = DownloadStatus.DOWNLOADING,
                                    progress = progressFraction,
                                    downloadedBytes = currentDownloaded,
                                    totalBytes = totalLength
                                ))
                            }
                        }
                    }

                    if (isActive && tempFile.length() > 0) {
                        if (destinationFile.exists()) destinationFile.delete()
                        if (tempFile.renameTo(destinationFile)) {
                            _downloadStates.update { current ->
                                current + (model.id to ModelDownloadProgress(
                                    modelId = model.id,
                                    status = DownloadStatus.DOWNLOADED,
                                    progress = 1.0f,
                                    downloadedBytes = destinationFile.length(),
                                    totalBytes = destinationFile.length()
                                ))
                            }
                            autoSelectActiveModel(model)
                        } else {
                            throw IllegalStateException("Failed to finalize downloaded file on device")
                        }
                    }
                }
            } catch (e: Exception) {
                val hasPartial = tempFile.exists() && tempFile.length() > 0
                _downloadStates.update { current ->
                    current + (model.id to ModelDownloadProgress(
                        modelId = model.id,
                        status = DownloadStatus.FAILED,
                        progress = if (tempFile.exists() && model.sizeBytes > 0) (tempFile.length().toFloat() / model.sizeBytes).coerceIn(0f, 1f) else 0f,
                        downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L,
                        totalBytes = model.sizeBytes,
                        errorMessage = e.localizedMessage ?: "Download interrupted",
                        isResumable = hasPartial
                    ))
                }
            } finally {
                activeJobs.remove(model.id)
            }
        }
        activeJobs[model.id] = job
    }

    fun pauseOrCancelDownload(model: AIModelInfo) {
        activeJobs[model.id]?.cancel()
        activeJobs.remove(model.id)
        val tempFile = getTempFile(model)
        _downloadStates.update { current ->
            current + (model.id to ModelDownloadProgress(
                modelId = model.id,
                status = DownloadStatus.NOT_DOWNLOADED,
                isResumable = tempFile.exists() && tempFile.length() > 0,
                downloadedBytes = if (tempFile.exists()) tempFile.length() else 0L,
                totalBytes = model.sizeBytes,
                progress = if (tempFile.exists() && model.sizeBytes > 0) (tempFile.length().toFloat() / model.sizeBytes).coerceIn(0f, 1f) else 0f
            ))
        }
    }

    fun deleteModel(model: AIModelInfo): Boolean {
        pauseOrCancelDownload(model)
        val file = getModelFile(model)
        val temp = getTempFile(model)
        if (temp.exists()) temp.delete()
        val deleted = if (file.exists()) file.delete() else true
        if (deleted) {
            _downloadStates.update { current ->
                current + (model.id to ModelDownloadProgress(
                    modelId = model.id,
                    status = DownloadStatus.NOT_DOWNLOADED
                ))
            }
        }
        return deleted
    }

    fun autoSelectActiveModel(model: AIModelInfo) {
        val path = getModelFile(model).absolutePath
        when (model.category) {
            ModelCategory.CHAT_LLM -> _activeLlmPath.value = path
            else -> {}
        }
    }
}
