package com.kallistocore.ai.domain.image

import android.content.ContentValues
import android.content.Context
import android.graphics.*
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.kallistocore.ai.data.network.ComfyUiClient
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt

enum class ImageGenState {
    IDLE,
    PREPROCESSING,
    DENOISING_STEPS,
    POSTPROCESSING,
    COMPLETED,
    ERROR
}

enum class AspectRatioOption(val label: String, val widthRatio: Float, val heightRatio: Float) {
    SQUARE_1_1("1:1 Square", 1f, 1f),
    LANDSCAPE_16_9("16:9 Wide", 16f, 9f),
    PORTRAIT_9_16("9:16 Story", 9f, 16f),
    STANDARD_4_3("4:3 Classic", 4f, 3f),
    VERTICAL_3_4("3:4 Portrait", 3f, 4f)
}

data class ImageGenProgress(
    val state: ImageGenState = ImageGenState.IDLE,
    val currentStep: Int = 0,
    val totalSteps: Int = 4,
    val progressFraction: Float = 0f,
    val generatedBitmap: Bitmap? = null,
    val generatedFile: File? = null,
    val dcimPath: String? = null,
    val stepDescription: String = "",
    val errorMessage: String? = null,
    val outputDimensions: String = "512x512"
)

class ImageStudioEngine(private val context: Context) {

    val comfyClient = ComfyUiClient(context)
    private val httpClient = HttpClient(OkHttp)

    private val _progressState = MutableStateFlow(ImageGenProgress())
    val progressState: StateFlow<ImageGenProgress> = _progressState.asStateFlow()

    suspend fun generateOrEditImage(
        prompt: String,
        inputImage: Bitmap? = null,
        aspectRatio: AspectRatioOption = AspectRatioOption.SQUARE_1_1,
        baseResolution: Int = 512,
        upscaleMultiplier: Float = 1.0f,
        forceSquareCrop: Boolean = false,
        strength: Float = 0.75f,
        steps: Int = 8
    ): File? = withContext(Dispatchers.IO) {
        if (prompt.isBlank() && inputImage == null) return@withContext null

        try {
            val (targetW, targetH) = calculateDimensions(
                inputImage = inputImage,
                aspectRatio = aspectRatio,
                baseResolution = baseResolution,
                upscaleMultiplier = upscaleMultiplier,
                forceSquareCrop = forceSquareCrop
            )

            _progressState.value = ImageGenProgress(
                state = ImageGenState.PREPROCESSING,
                currentStep = 1,
                totalSteps = steps,
                progressFraction = 0.15f,
                stepDescription = "Initializing diffusion pipeline (${targetW}x${targetH})...",
                outputDimensions = "${targetW}x${targetH}"
            )

            var outputBitmap: Bitmap? = null

            // 1. Try ComfyUI Server on Local Network (DGX Spark / PC)
            val isComfyConnected = comfyClient.checkServerConnection()
            if (isComfyConnected) {
                _progressState.value = ImageGenProgress(
                    state = ImageGenState.DENOISING_STEPS,
                    currentStep = 2,
                    totalSteps = steps,
                    progressFraction = 0.45f,
                    stepDescription = "Queuing on DGX ComfyUI Server...",
                    outputDimensions = "${targetW}x${targetH}"
                )

                val uploadedName = if (inputImage != null) comfyClient.uploadImage(inputImage) else null
                outputBitmap = comfyClient.queuePromptAndFetchImage(
                    promptText = prompt,
                    inputImageName = uploadedName,
                    steps = steps,
                    cfg = 1.5f,
                    width = targetW,
                    height = targetH
                )
            }

            // 2. Direct Diffusion Engine (Real Open-Source AI Generation)
            if (outputBitmap == null) {
                _progressState.value = ImageGenProgress(
                    state = ImageGenState.DENOISING_STEPS,
                    currentStep = 3,
                    totalSteps = steps,
                    progressFraction = 0.65f,
                    stepDescription = "Synthesizing diffusion latents for: '$prompt'...",
                    outputDimensions = "${targetW}x${targetH}"
                )

                outputBitmap = fetchDirectDiffusionImage(prompt, targetW, targetH, inputImage, strength)
            }

            if (outputBitmap == null) {
                throw IllegalStateException("Diffusion synthesis failed. Check network or ComfyUI server.")
            }

            _progressState.value = ImageGenProgress(
                state = ImageGenState.POSTPROCESSING,
                currentStep = steps,
                totalSteps = steps,
                progressFraction = 0.95f,
                stepDescription = "Saving to DCIM/KallistoAI album...",
                outputDimensions = "${targetW}x${targetH}"
            )

            val savedFile = saveToDcimAndGallery(outputBitmap, if (inputImage != null) "edit" else "art")

            _progressState.value = ImageGenProgress(
                state = ImageGenState.COMPLETED,
                currentStep = steps,
                totalSteps = steps,
                progressFraction = 1.0f,
                generatedBitmap = outputBitmap,
                generatedFile = savedFile,
                dcimPath = "DCIM/KallistoAI",
                stepDescription = "Complete! Saved to Gallery.",
                outputDimensions = "${targetW}x${targetH}"
            )

            return@withContext savedFile
        } catch (e: Exception) {
            _progressState.value = ImageGenProgress(
                state = ImageGenState.ERROR,
                errorMessage = e.localizedMessage ?: "Generation failed"
            )
            return@withContext null
        }
    }

    private suspend fun fetchDirectDiffusionImage(
        prompt: String,
        width: Int,
        height: Int,
        inputImage: Bitmap?,
        strength: Float
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val encodedPrompt = prompt.encodeURLParameter()
            val seed = (System.currentTimeMillis() % 1000000).toInt()
            val url = "https://image.pollinations.ai/prompt/$encodedPrompt?width=$width&height=$height&seed=$seed&model=flux&nologo=true"

            val response = httpClient.get(url)
            if (response.status.isSuccess()) {
                val bytes = response.bodyAsChannel().toByteArray()
                val downloadedBmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

                if (inputImage != null && downloadedBmp != null) {
                    // Blend with input image for Img2Img style consistency
                    val blended = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(blended)
                    val scaledInput = Bitmap.createScaledBitmap(inputImage, width, height, true)
                    canvas.drawBitmap(scaledInput, 0f, 0f, null)

                    val paint = Paint().apply {
                        alpha = (strength * 230).toInt().coerceIn(60, 255)
                    }
                    canvas.drawBitmap(downloadedBmp, 0f, 0f, paint)
                    return@withContext blended
                }
                return@withContext downloadedBmp
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun calculateDimensions(
        inputImage: Bitmap?,
        aspectRatio: AspectRatioOption,
        baseResolution: Int,
        upscaleMultiplier: Float,
        forceSquareCrop: Boolean
    ): Pair<Int, Int> {
        if (inputImage != null) {
            return if (forceSquareCrop) {
                val size = ((minOf(inputImage.width, inputImage.height) * upscaleMultiplier).roundToInt() / 16) * 16
                Pair(size.coerceIn(384, 1024), size.coerceIn(384, 1024))
            } else {
                val w = ((inputImage.width * upscaleMultiplier).roundToInt() / 16) * 16
                val h = ((inputImage.height * upscaleMultiplier).roundToInt() / 16) * 16
                Pair(w.coerceIn(384, 1024), h.coerceIn(384, 1024))
            }
        }

        val w: Int
        val h: Int
        when (aspectRatio) {
            AspectRatioOption.SQUARE_1_1 -> { w = baseResolution; h = baseResolution }
            AspectRatioOption.LANDSCAPE_16_9 -> { w = (baseResolution * 1.33f).toInt() / 16 * 16; h = (baseResolution * 0.75f).toInt() / 16 * 16 }
            AspectRatioOption.PORTRAIT_9_16 -> { w = (baseResolution * 0.75f).toInt() / 16 * 16; h = (baseResolution * 1.33f).toInt() / 16 * 16 }
            AspectRatioOption.STANDARD_4_3 -> { w = (baseResolution * 1.15f).toInt() / 16 * 16; h = (baseResolution * 0.86f).toInt() / 16 * 16 }
            AspectRatioOption.VERTICAL_3_4 -> { w = (baseResolution * 0.86f).toInt() / 16 * 16; h = (baseResolution * 1.15f).toInt() / 16 * 16 }
        }
        return Pair(w, h)
    }

    fun saveBitmapToStorage(bitmap: Bitmap, prefix: String = "kallisto"): File? {
        return saveToDcimAndGallery(bitmap, prefix)
    }

    fun saveToDcimAndGallery(bitmap: Bitmap, prefix: String): File? {
        val fileName = "Kallisto_${prefix}_${System.currentTimeMillis()}.png"

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/KallistoAI")
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }

                val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                if (uri != null) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        out.flush()
                    }
                    values.clear()
                    values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    context.contentResolver.update(uri, values, null, null)
                }
            } else {
                val dcimDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), "KallistoAI")
                if (!dcimDir.exists()) dcimDir.mkdirs()
                val targetFile = File(dcimDir, fileName)
                FileOutputStream(targetFile).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                    out.flush()
                }
                MediaScannerConnection.scanFile(context, arrayOf(targetFile.absolutePath), arrayOf("image/png"), null)
            }

            val internalDir = File(context.filesDir, "generated_images").apply { if (!exists()) mkdirs() }
            val internalFile = File(internalDir, fileName)
            FileOutputStream(internalFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
            }
            return internalFile
        } catch (_: Exception) {
            return null
        }
    }

    private suspend fun io.ktor.utils.io.ByteReadChannel.toByteArray(): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!isClosedForRead) {
            val read = readAvailable(buffer, 0, buffer.size)
            if (read <= 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}
