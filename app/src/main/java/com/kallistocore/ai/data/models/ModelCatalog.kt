package com.kallistocore.ai.data.models

import kotlinx.serialization.Serializable

enum class ModelCategory {
    CHAT_LLM,
    VOICE_TTS,
    IMAGE_GEN_AND_EDIT
}

enum class ModelFormat {
    GGUF,
    ONNX,
    MNN
}

@Serializable
enum class DownloadStatus {
    NOT_DOWNLOADED,
    QUEUED,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}

@Serializable
data class AIModelInfo(
    val id: String,
    val name: String,
    val description: String,
    val category: ModelCategory,
    val format: ModelFormat,
    val fileName: String,
    val sizeBytes: Long,
    val downloadUrl: String,
    val quantization: String,
    val ramRequirementMB: Int,
    val supportsImg2Img: Boolean = false,
    val supportsVoiceSynthesis: Boolean = false,
    val supportsChat: Boolean = false
) {
    val formattedSize: String
        get() {
            val kb = sizeBytes / 1024.0
            val mb = kb / 1024.0
            val gb = mb / 1024.0
            return when {
                gb >= 1.0 -> String.format("%.2f GB", gb)
                mb >= 1.0 -> String.format("%.1f MB", mb)
                else -> String.format("%.0f KB", kb)
            }
        }
}

object ModelCatalog {

    val curatedModels: List<AIModelInfo> = listOf(
        // ==========================================
        // 1. Conversational On-Device LLMs
        // ==========================================
        AIModelInfo(
            id = "llama-3.2-3b-q4",
            name = "Llama 3.2 3B Instruct",
            description = "Meta's flagship mobile LLM. Exceptional reasoning, tool use, and memory synthesis.",
            category = ModelCategory.CHAT_LLM,
            format = ModelFormat.GGUF,
            fileName = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            sizeBytes = 2_020_000_000L, // ~1.88 GB
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            quantization = "Q4_K_M",
            ramRequirementMB = 2800,
            supportsChat = true
        ),
        AIModelInfo(
            id = "llama-3.2-1b-q4",
            name = "Llama 3.2 1B Instruct (Ultra-Light)",
            description = "High-speed compact model designed for maximum battery saving on Android.",
            category = ModelCategory.CHAT_LLM,
            format = ModelFormat.GGUF,
            fileName = "Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            sizeBytes = 780_000_000L, // ~744 MB
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-1B-Instruct-GGUF/resolve/main/Llama-3.2-1B-Instruct-Q4_K_M.gguf",
            quantization = "Q4_K_M",
            ramRequirementMB = 1200,
            supportsChat = true
        ),
        AIModelInfo(
            id = "qwen-2.5-3b-q4",
            name = "Qwen 2.5 3B Instruct",
            description = "State-of-the-art multilingual and coding capabilities for mobile companions.",
            category = ModelCategory.CHAT_LLM,
            format = ModelFormat.GGUF,
            fileName = "qwen2.5-3b-instruct-q4_k_m.gguf",
            sizeBytes = 2_150_000_000L, // ~2.00 GB
            downloadUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct-GGUF/resolve/main/qwen2.5-3b-instruct-q4_k_m.gguf",
            quantization = "Q4_K_M",
            ramRequirementMB = 3000,
            supportsChat = true
        ),

        // ==========================================
        // 2. Kokoro-82M TTS Voice Engines
        // ==========================================
        AIModelInfo(
            id = "kokoro-82m-onnx",
            name = "Kokoro-82M TTS Engine",
            description = "82M parameter neural speech model delivering human-like voice synthesis locally.",
            category = ModelCategory.VOICE_TTS,
            format = ModelFormat.ONNX,
            fileName = "kokoro-v0_19.onnx",
            sizeBytes = 88_000_000L, // ~84 MB
            downloadUrl = "https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/kokoro-v0_19.onnx",
            quantization = "FP16 / INT8",
            ramRequirementMB = 220,
            supportsVoiceSynthesis = true
        ),
        AIModelInfo(
            id = "kokoro-voices-bundle",
            name = "Kokoro Voices Pack (af_heart, bella, adam)",
            description = "Voice character weight profiles for Kokoro TTS audio generation.",
            category = ModelCategory.VOICE_TTS,
            format = ModelFormat.ONNX,
            fileName = "voices_bundle.bin",
            sizeBytes = 29_000_000L, // ~28 MB
            downloadUrl = "https://huggingface.co/hexgrad/Kokoro-82M/resolve/main/voices.bin",
            quantization = "Raw Tensor Data",
            ramRequirementMB = 50,
            supportsVoiceSynthesis = true
        ),

        // ==========================================
        // 3. Image Generation & Img2Img Editing Models
        // ==========================================
        AIModelInfo(
            id = "instruct-pix2pix-mobile",
            name = "InstructPix2Pix Mobile (Img2Img)",
            description = "Prompt-based image-to-image editing. Modifies your photos using natural language.",
            category = ModelCategory.IMAGE_GEN_AND_EDIT,
            format = ModelFormat.ONNX,
            fileName = "instruct_pix2pix_quant_int8.onnx",
            sizeBytes = 890_000_000L, // ~848 MB
            downloadUrl = "https://huggingface.co/kallistocore/instruct-pix2pix-mobile/resolve/main/instruct_pix2pix_quant_int8.onnx",
            quantization = "INT8",
            ramRequirementMB = 1600,
            supportsImg2Img = true
        ),
        AIModelInfo(
            id = "sd-turbo-lcm-img2img",
            name = "SD-Turbo LCM Fast Img2Img",
            description = "Ultra-fast 2-4 step diffusion engine for instant image variations and sketch-to-art.",
            category = ModelCategory.IMAGE_GEN_AND_EDIT,
            format = ModelFormat.ONNX,
            fileName = "sd_turbo_lcm_int8.onnx",
            sizeBytes = 1_180_000_000L, // ~1.10 GB
            downloadUrl = "https://huggingface.co/kallistocore/sd-turbo-mobile/resolve/main/sd_turbo_lcm_int8.onnx",
            quantization = "INT8",
            ramRequirementMB = 1900,
            supportsImg2Img = true
        ),
        AIModelInfo(
            id = "mobilediffusion-lite",
            name = "MobileDiffusion Lite (Text-to-Image)",
            description = "Sub-second on-device text-to-image generation optimized for mobile ARM NPU/GPU.",
            category = ModelCategory.IMAGE_GEN_AND_EDIT,
            format = ModelFormat.ONNX,
            fileName = "mobilediffusion_quant.onnx",
            sizeBytes = 540_000_000L, // ~515 MB
            downloadUrl = "https://huggingface.co/kallistocore/mobilediffusion/resolve/main/mobilediffusion_quant.onnx",
            quantization = "INT8",
            ramRequirementMB = 1100,
            supportsImg2Img = false
        )
    )

    fun getModelsByCategory(category: ModelCategory): List<AIModelInfo> {
        return curatedModels.filter { it.category == category }
    }

    fun getModelById(id: String): AIModelInfo? {
        return curatedModels.find { it.id == id }
    }
}
