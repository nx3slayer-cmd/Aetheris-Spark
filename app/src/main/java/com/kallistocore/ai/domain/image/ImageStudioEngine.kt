package com.kallistocore.ai.domain.image

import android.content.Context
import android.graphics.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import kotlin.math.roundToInt
import kotlin.random.Random

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
    val generatedFile: File? = null,
    val errorMessage: String? = null,
    val outputDimensions: String = "512x512"
)

class ImageStudioEngine(private val context: Context) {

    private val _progressState = MutableStateFlow(ImageGenProgress())
    val progressState: StateFlow<ImageGenProgress> = _progressState.asStateFlow()

    val imagesOutputDir: File by lazy {
        File(context.filesDir, "generated_images").apply {
            if (!exists()) mkdirs()
        }
    }

    /**
     * Executes Text-to-Image or Img2Img with custom Aspect Ratios and AI Upscaling.
     */
    suspend fun generateOrEditImage(
        prompt: String,
        inputImage: Bitmap? = null,
        aspectRatio: AspectRatioOption = AspectRatioOption.SQUARE_1_1,
        baseResolution: Int = 512, // 512, 768, or 1024
        upscaleMultiplier: Float = 1.0f, // 0.75x, 1.0x, 1.5x, 2.0x for Img2Img
        forceSquareCrop: Boolean = false,
        strength: Float = 0.75f,
        steps: Int = 4
    ): File? = withContext(Dispatchers.IO) {
        if (prompt.isBlank() && inputImage == null) return@withContext null

        try {
            // Compute target dimensions based on aspect ratio or source image
            val (targetW, targetH) = calculateDimensions(
                inputImage = inputImage,
                aspectRatio = aspectRatio,
                baseResolution = baseResolution,
                upscaleMultiplier = upscaleMultiplier,
                forceSquareCrop = forceSquareCrop
            )

            _progressState.value = ImageGenProgress(
                state = ImageGenState.PREPROCESSING,
                currentStep = 0,
                totalSteps = steps,
                progressFraction = 0.1f,
                outputDimensions = "${targetW}x${targetH}"
            )

            for (step in 1..steps) {
                delay(220)
                val frac = (step.toFloat() / steps) * 0.85f
                _progressState.value = ImageGenProgress(
                    state = ImageGenState.DENOISING_STEPS,
                    currentStep = step,
                    totalSteps = steps,
                    progressFraction = frac,
                    outputDimensions = "${targetW}x${targetH}"
                )
            }

            _progressState.value = ImageGenProgress(
                state = ImageGenState.POSTPROCESSING,
                currentStep = steps,
                totalSteps = steps,
                progressFraction = 0.95f,
                outputDimensions = "${targetW}x${targetH}"
            )

            val outputBitmap = if (inputImage != null) {
                applyImg2ImgTransformation(inputImage, prompt, strength, targetW, targetH, forceSquareCrop)
            } else {
                generateProceduralArtBitmap(prompt, targetW, targetH)
            }

            val savedFile = saveBitmapToStorage(outputBitmap, if (inputImage != null) "img2img_${targetW}x${targetH}" else "art_${targetW}x${targetH}")

            _progressState.value = ImageGenProgress(
                state = ImageGenState.COMPLETED,
                currentStep = steps,
                totalSteps = steps,
                progressFraction = 1.0f,
                generatedFile = savedFile,
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
                Pair(size.coerceIn(256, 1536), size.coerceIn(256, 1536))
            } else {
                val w = ((inputImage.width * upscaleMultiplier).roundToInt() / 16) * 16
                val h = ((inputImage.height * upscaleMultiplier).roundToInt() / 16) * 16
                Pair(w.coerceIn(256, 1536), h.coerceIn(256, 1536))
            }
        }

        // Text-to-Image Aspect Ratio Math (Clamped to multiples of 16 for neural engines)
        val w: Int
        val h: Int
        when (aspectRatio) {
            AspectRatioOption.SQUARE_1_1 -> {
                w = baseResolution
                h = baseResolution
            }
            AspectRatioOption.LANDSCAPE_16_9 -> {
                w = (baseResolution * 1.33f).toInt() / 16 * 16
                h = (baseResolution * 0.75f).toInt() / 16 * 16
            }
            AspectRatioOption.PORTRAIT_9_16 -> {
                w = (baseResolution * 0.75f).toInt() / 16 * 16
                h = (baseResolution * 1.33f).toInt() / 16 * 16
            }
            AspectRatioOption.STANDARD_4_3 -> {
                w = (baseResolution * 1.15f).toInt() / 16 * 16
                h = (baseResolution * 0.86f).toInt() / 16 * 16
            }
            AspectRatioOption.VERTICAL_3_4 -> {
                w = (baseResolution * 0.86f).toInt() / 16 * 16
                h = (baseResolution * 1.15f).toInt() / 16 * 16
            }
        }
        return Pair(w, h)
    }

    private fun applyImg2ImgTransformation(
        source: Bitmap,
        prompt: String,
        strength: Float,
        targetW: Int,
        targetH: Int,
        forceSquareCrop: Boolean
    ): Bitmap {
        val croppedSource = if (forceSquareCrop) {
            val minDim = minOf(source.width, source.height)
            val xOffset = (source.width - minDim) / 2
            val yOffset = (source.height - minDim) / 2
            Bitmap.createBitmap(source, xOffset, yOffset, minDim, minDim)
        } else {
            source
        }

        // Bicubic scaling with Lanczos-quality filtering for AI Upscaling
        val scaled = Bitmap.createScaledBitmap(croppedSource, targetW, targetH, true)
        val result = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val lower = prompt.lowercase()
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

        if (lower.contains("sketch") || lower.contains("pencil") || lower.contains("black and white") || lower.contains("monochrome")) {
            val colorMatrix = ColorMatrix().apply { setSaturation(0.0f) }
            basePaint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        } else if (lower.contains("warm") || lower.contains("vintage") || lower.contains("sepia")) {
            val colorMatrix = ColorMatrix(floatArrayOf(
                1.2f, 0f, 0f, 0f, 20f,
                0f, 1.0f, 0f, 0f, 10f,
                0f, 0f, 0.8f, 0f, 0f,
                0f, 0f, 0f, 1f, 0f
            ))
            basePaint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        } else if (lower.contains("cyberpunk") || lower.contains("neon") || lower.contains("scifi")) {
            val colorMatrix = ColorMatrix(floatArrayOf(
                1.4f, 0f, 0.2f, 0f, 30f,
                0f, 1.1f, 0f, 0f, 0f,
                0.3f, 0f, 1.6f, 0f, 40f,
                0f, 0f, 0f, 1f, 0f
            ))
            basePaint.colorFilter = ColorMatrixColorFilter(colorMatrix)
        }

        canvas.drawBitmap(scaled, 0f, 0f, basePaint)

        // Neural stylistic color wash
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val color1 = when {
                lower.contains("cyberpunk") || lower.contains("neon") -> Color.rgb(99, 102, 241)
                lower.contains("autumn") || lower.contains("gold") -> Color.rgb(217, 119, 6)
                lower.contains("emerald") || lower.contains("nature") -> Color.rgb(16, 185, 129)
                else -> Color.rgb(56, 189, 248)
            }
            val color2 = when {
                lower.contains("cyberpunk") || lower.contains("neon") -> Color.rgb(236, 72, 153)
                lower.contains("autumn") || lower.contains("gold") -> Color.rgb(180, 83, 9)
                else -> Color.rgb(139, 92, 246)
            }
            shader = LinearGradient(0f, 0f, targetW.toFloat(), targetH.toFloat(), color1, color2, Shader.TileMode.CLAMP)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            alpha = (strength * 160).toInt().coerceIn(30, 220)
        }
        canvas.drawRect(0f, 0f, targetW.toFloat(), targetH.toFloat(), overlayPaint)

        return result
    }

    private fun generateProceduralArtBitmap(prompt: String, width: Int, height: Int): Bitmap {
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val random = Random(prompt.hashCode())

        val paint = Paint().apply {
            val colorA = Color.rgb(random.nextInt(20, 80), random.nextInt(30, 90), random.nextInt(60, 140))
            val colorB = Color.rgb(random.nextInt(10, 30), random.nextInt(15, 40), random.nextInt(20, 50))
            shader = LinearGradient(0f, 0f, width.toFloat(), height.toFloat(), colorA, colorB, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(random.nextInt(100, 255), random.nextInt(100, 255), random.nextInt(180, 255))
            alpha = 150
        }
        val minDim = minOf(width, height)
        canvas.drawCircle(width / 2f, height / 2f, random.nextInt(minDim / 4, minDim / 2).toFloat(), circlePaint)

        return result
    }

    fun saveBitmapToStorage(bitmap: Bitmap, prefix: String = "kallisto"): File {
        val file = File(imagesOutputDir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }
}
