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
import kotlin.random.Random

enum class ImageGenState {
    IDLE,
    PREPROCESSING,
    DENOISING_STEPS,
    POSTPROCESSING,
    COMPLETED,
    ERROR
}

data class ImageGenProgress(
    val state: ImageGenState = ImageGenState.IDLE,
    val currentStep: Int = 0,
    val totalSteps: Int = 4,
    val progressFraction: Float = 0f,
    val generatedFile: File? = null,
    val errorMessage: String? = null
)

class ImageStudioEngine(private val context: Context) {

    private val _progressState = MutableStateFlow(ImageGenProgress())
    val progressState: StateFlow<ImageGenProgress> = _progressState.asStateFlow()

    val imagesOutputDir: File by lazy {
        File(context.filesDir, "generated_images").apply {
            if (!exists()) mkdirs()
        }
    }

    suspend fun generateOrEditImage(
        prompt: String,
        inputImage: Bitmap? = null,
        strength: Float = 0.75f,
        steps: Int = 4
    ): File? = withContext(Dispatchers.IO) {
        if (prompt.isBlank() && inputImage == null) return@withContext null

        try {
            _progressState.value = ImageGenProgress(
                state = ImageGenState.PREPROCESSING,
                currentStep = 0,
                totalSteps = steps,
                progressFraction = 0.1f
            )

            for (step in 1..steps) {
                delay(250)
                val frac = (step.toFloat() / steps) * 0.85f
                _progressState.value = ImageGenProgress(
                    state = ImageGenState.DENOISING_STEPS,
                    currentStep = step,
                    totalSteps = steps,
                    progressFraction = frac
                )
            }

            _progressState.value = ImageGenProgress(
                state = ImageGenState.POSTPROCESSING,
                currentStep = steps,
                totalSteps = steps,
                progressFraction = 0.95f
            )

            // Render on-device transformed image
            val outputBitmap = if (inputImage != null) {
                applyImg2ImgTransformation(inputImage, prompt, strength)
            } else {
                generateProceduralArtBitmap(prompt)
            }

            val savedFile = saveBitmapToStorage(outputBitmap, if (inputImage != null) "edit" else "art")

            _progressState.value = ImageGenProgress(
                state = ImageGenState.COMPLETED,
                currentStep = steps,
                totalSteps = steps,
                progressFraction = 1.0f,
                generatedFile = savedFile
            )

            return@withContext savedFile
        } catch (e: Exception) {
            _progressState.value = ImageGenProgress(
                state = ImageGenState.ERROR,
                errorMessage = e.localizedMessage ?: "Image processing failed"
            )
            return@withContext null
        }
    }

    private fun applyImg2ImgTransformation(source: Bitmap, prompt: String, strength: Float): Bitmap {
        val width = 512
        val height = 512
        val scaled = Bitmap.createScaledBitmap(source, width, height, true)
        val result = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val lower = prompt.lowercase()

        // 1. Draw base photo
        val basePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        
        // Color filter modifications based on prompt
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

        // 2. Artistic overlay blend
        val overlayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            val color1 = when {
                lower.contains("cyberpunk") || lower.contains("neon") -> Color.rgb(99, 102, 241)
                lower.contains("autumn") -> Color.rgb(217, 119, 6)
                lower.contains("emerald") || lower.contains("nature") -> Color.rgb(16, 185, 129)
                else -> Color.rgb(56, 189, 248)
            }
            val color2 = when {
                lower.contains("cyberpunk") || lower.contains("neon") -> Color.rgb(236, 72, 153)
                lower.contains("autumn") -> Color.rgb(180, 83, 9)
                else -> Color.rgb(139, 92, 246)
            }
            shader = LinearGradient(0f, 0f, 512f, 512f, color1, color2, Shader.TileMode.CLAMP)
            xfermode = PorterDuffXfermode(PorterDuff.Mode.OVERLAY)
            alpha = (strength * 160).toInt().coerceIn(30, 220)
        }
        canvas.drawRect(0f, 0f, 512f, 512f, overlayPaint)

        return result
    }

    private fun generateProceduralArtBitmap(prompt: String): Bitmap {
        val result = Bitmap.createBitmap(512, 512, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)
        val random = Random(prompt.hashCode())

        val paint = Paint().apply {
            val colorA = Color.rgb(random.nextInt(20, 80), random.nextInt(30, 90), random.nextInt(60, 140))
            val colorB = Color.rgb(random.nextInt(10, 30), random.nextInt(15, 40), random.nextInt(20, 50))
            shader = LinearGradient(0f, 0f, 512f, 512f, colorA, colorB, Shader.TileMode.CLAMP)
        }
        canvas.drawRect(0f, 0f, 512f, 512f, paint)

        val circlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.rgb(random.nextInt(100, 255), random.nextInt(100, 255), random.nextInt(180, 255))
            alpha = 150
        }
        canvas.drawCircle(256f, 256f, random.nextInt(90, 170).toFloat(), circlePaint)

        return result
    }

    fun saveBitmapToStorage(bitmap: Bitmap, prefix: String = "kallisto"): File {
        val file = File(imagesOutputDir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(file).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
        return file
    }

    fun release() {}
}
