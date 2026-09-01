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
        // 1. Z-Image Turbo & Diffusion Models
        // ==========================================
        AIModelInfo(
            id = "z-image-turbo-8step",
            name = "Z-Image Turbo (8-Step S3-DiT)",
            description = "State-of-the-art 8-step Diffusion Transformer. Incredible photorealism, hands, and prompt adherence.",
            category = ModelCategory.IMAGE_GEN_AND_EDIT,
            format = ModelFormat.GGUF,
            fileName = "z_image_turbo_q4_k.gguf",
            sizeBytes = 2_450_000_000L, // ~2.28 GB
            downloadUrl = "https://huggingface.co/Comfy-Org/z_image_turbo/resolve/main/split_files/diffusion_models/z_image_turbo_bf16.safetensors",
            quantization = "Q4_K / INT8",
            ramRequirementMB = 3400,
            supportsImg2Img = true
        ),
        AIModelInfo(
            id = "z-image-edit-mobile",
            name = "Z-Image Edit (Fast Img2Img)",
            description = "Fine-tuned specifically for natural language image-to-image photo transformations.",
            category = ModelCategory.IMAGE_GEN_AND_EDIT,
            format = ModelFormat.ONNX,
            fileName = "z_image_edit_quant.onnx",
            sizeBytes = 1_650_000_000L, // ~1.53 GB
            downloadUrl = "https://huggingface.co/Tongyi-MAI/Z-Image-Turbo/resolve/main/z_image_turbo.safetensors",
            quantization = "INT8",
            ramRequirementMB = 2600,
            supportsImg2Img = true
        ),
        AIModelInfo(
            id = "sd-turbo-lcm-img2img",
            name = "SD-Turbo LCM Fast Img2Img",
            description = "Ultra-fast 2-4 step mobile engine for instant sketch-to-art and fast stylization.",
            category = ModelCategory.IMAGE_GEN_AND_EDIT,
            format = ModelFormat.ONNX,
            fileName = "sd_turbo_lcm_int8.onnx",
            sizeBytes = 1_180_000_000L, // ~1.10 GB
            downloadUrl = "https://huggingface.co/kallistocore/sd-turbo-mobile/resolve/main/sd_turbo_lcm_int8.onnx",
            quantization = "INT8",
            ramRequirementMB = 1800,
            supportsImg2Img = true
        ),
        AIModelInfo(
            id = "instruct-pix2pix-mobile",
            name = "InstructPix2Pix Mobile",
            description = "Prompt-based photo editing (e.g. 'make it snowy', 'turn day to night').",
            category = ModelCategory.IMAGE_GEN_AND_EDIT,
            format = ModelFormat.ONNX,
            fileName = "instruct_pix2pix_quant_int8.onnx",
            sizeBytes = 890_000_000L, // ~848 MB
            downloadUrl = "https://huggingface.co/kallistocore/instruct-pix2pix-mobile/resolve/main/instruct_pix2pix_quant_int8.onnx",
            quantization = "INT8",
            ramRequirementMB = 1400,
            supportsImg2Img = true
        ),

        // ==========================================
        // 2. Conversational LLMs
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
        // 3. Kokoro-82M Voice Engine
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
        )
    )

    fun getModelsByCategory(category: ModelCategory): List<AIModelInfo> {
        return curatedModels.filter { it.category == category }
    }

    fun getModelById(id: String): AIModelInfo? {
        return curatedModels.find { it.id == id }
    }
}
