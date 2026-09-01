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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

data class ModelDownloadProgress(
    val modelId: String,
    val status: DownloadStatus = DownloadStatus.NOT_DOWNLOADED,
    val progress: Float = 0f,
    val downloadedBytes: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null
)

class ModelManager(private val context: Context) {

    private val httpClient = HttpClient(OkHttp)

    // Base storage path: /data/user/0/com.kallistocore.ai/files/models/
    val baseModelsDir: File by lazy {
        File(context.filesDir, "models").apply {
            if (!exists()) mkdirs()
        }
    }

    private val _downloadStates = MutableStateFlow<Map<String, ModelDownloadProgress>>(emptyMap())
    val downloadStates: StateFlow<Map<String, ModelDownloadProgress>> = _downloadStates.asStateFlow()

    private val _activeLlmPath = MutableStateFlow<String?>(null)
    val activeLlmPath: StateFlow<String?> = _activeLlmPath.asStateFlow()

    private val _activeTtsPath = MutableStateFlow<String?>(null)
    val activeTtsPath: StateFlow<String?> = _activeTtsPath.asStateFlow()

    private val _activeImg2ImgPath = MutableStateFlow<String?>(null)
    val activeImg2ImgPath: StateFlow<String?> = _activeImg2ImgPath.asStateFlow()

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

    fun isModelDownloaded(model: AIModelInfo): Boolean {
        val file = getModelFile(model)
        return file.exists() && file.length() > 0
    }

    fun getAvailableStorageBytes(): Long {
        val stat = StatFs(context.filesDir.path)
        return stat.availableBlocksLong * stat.blockSizeLong
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
     * Downloads an open-source model with real-time byte stream tracking.
     */
    suspend fun downloadModel(model: AIModelInfo) = withContext(Dispatchers.IO) {
        val destinationFile = getModelFile(model)
        val tempFile = File(destinationFile.parentFile, "${destinationFile.name}.tmp")

        if (getAvailableStorageBytes() < model.sizeBytes) {
            _downloadStates.update { current ->
                current + (model.id to ModelDownloadProgress(
                    modelId = model.id,
                    status = DownloadStatus.FAILED,
                    errorMessage = "Insufficient storage space on device"
                ))
            }
            return@withContext
        }

        try {
            _downloadStates.update { current ->
                current + (model.id to ModelDownloadProgress(
                    modelId = model.id,
                    status = DownloadStatus.DOWNLOADING,
                    totalBytes = model.sizeBytes
                ))
            }

            httpClient.prepareGet(model.downloadUrl).execute { httpResponse ->
                if (!httpResponse.status.isSuccess()) {
                    throw IllegalStateException("HTTP error code: ${httpResponse.status}")
                }

                val totalLength = httpResponse.headers[HttpHeaders.ContentLength]?.toLongOrNull() ?: model.sizeBytes
                val channel: ByteReadChannel = httpResponse.bodyAsChannel()
                val buffer = ByteArray(64 * 1024) // 64 KB download buffer
                var downloaded = 0L

                FileOutputStream(tempFile).use { outputStream ->
                    while (!channel.isClosedForRead) {
                        val read = channel.readAvailable(buffer, 0, buffer.size)
                        if (read <= 0) break
                        outputStream.write(buffer, 0, read)
                        downloaded += read

                        val progressFraction = (downloaded.toFloat() / totalLength).coerceIn(0f, 1f)
                        _downloadStates.update { current ->
                            current + (model.id to ModelDownloadProgress(
                                modelId = model.id,
                                status = DownloadStatus.DOWNLOADING,
                                progress = progressFraction,
                                downloadedBytes = downloaded,
                                totalBytes = totalLength
                            ))
                        }
                    }
                }

                // Finalize file
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
                    throw IllegalStateException("Failed to move temporary model file into place")
                }
            }
        } catch (e: Exception) {
            if (tempFile.exists()) tempFile.delete()
            _downloadStates.update { current ->
                current + (model.id to ModelDownloadProgress(
                    modelId = model.id,
                    status = DownloadStatus.FAILED,
                    errorMessage = e.localizedMessage ?: "Download failed"
                ))
            }
        }
    }

    /**
     * Immediately deletes the model file to free storage space.
     */
    fun deleteModel(model: AIModelInfo): Boolean {
        val file = getModelFile(model)
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
            ModelCategory.VOICE_TTS -> _activeTtsPath.value = path
            ModelCategory.IMAGE_GEN_AND_EDIT -> _activeImg2ImgPath.value = path
        }
    }
}
