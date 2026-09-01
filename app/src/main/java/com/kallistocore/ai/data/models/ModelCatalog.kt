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
    val hfRepoUrl: String,
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
        // 1. Z-Image Turbo 8-Step Models (Verified Direct GGUFs)
        // ==========================================
        AIModelInfo(
            id = "z-image-turbo-q2k",
            name = "Z-Image Turbo 8-Step (Compact)",
            description = "Distilled 8-step S3-DiT model quantized for fast mobile inference.",
            category = ModelCategory.IMAGE_GEN_AND_EDIT,
            format = ModelFormat.GGUF,
            fileName = "z_image_turbo-Q2_K.gguf",
            sizeBytes = 2_780_000_000L, // ~2.59 GB
            downloadUrl = "https://huggingface.co/leejet/Z-Image-Turbo-GGUF/resolve/main/z_image_turbo-Q2_K.gguf",
            hfRepoUrl = "https://huggingface.co/leejet/Z-Image-Turbo-GGUF",
            quantization = "Q2_K GGUF",
            ramRequirementMB = 3200,
            supportsImg2Img = true
        ),
        AIModelInfo(
            id = "z-image-turbo-q3k",
            name = "Z-Image Turbo 8-Step (High Precision)",
            description = "Higher precision 8-step Diffusion Transformer for maximum photorealism.",
            category = ModelCategory.IMAGE_GEN_AND_EDIT,
            format = ModelFormat.GGUF,
            fileName = "z_image_turbo-Q3_K.gguf",
            sizeBytes = 3_370_000_000L, // ~3.14 GB
            downloadUrl = "https://huggingface.co/leejet/Z-Image-Turbo-GGUF/resolve/main/z_image_turbo-Q3_K.gguf",
            hfRepoUrl = "https://huggingface.co/Tongyi-MAI/Z-Image-Turbo",
            quantization = "Q3_K GGUF",
            ramRequirementMB = 4200,
            supportsImg2Img = true
        ),

        // ==========================================
        // 2. Conversational LLMs (Verified Direct GGUFs)
        // ==========================================
        AIModelInfo(
            id = "llama-3.2-3b-q4",
            name = "Llama 3.2 3B Instruct",
            description = "Meta's flagship mobile LLM. Exceptional reasoning and memory synthesis.",
            category = ModelCategory.CHAT_LLM,
            format = ModelFormat.GGUF,
            fileName = "Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            sizeBytes = 2_020_000_000L, // ~1.88 GB
            downloadUrl = "https://huggingface.co/bartowski/Llama-3.2-3B-Instruct-GGUF/resolve/main/Llama-3.2-3B-Instruct-Q4_K_M.gguf",
            hfRepoUrl = "https://huggingface.co/meta-llama/Llama-3.2-3B-Instruct",
            quantization = "Q4_K_M GGUF",
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
            hfRepoUrl = "https://huggingface.co/Qwen/Qwen2.5-3B-Instruct",
            quantization = "Q4_K_M GGUF",
            ramRequirementMB = 3000,
            supportsChat = true
        ),

        // ==========================================
        // 3. Kokoro-82M Voice Engine (Direct ONNX Model Path)
        // ==========================================
        AIModelInfo(
            id = "kokoro-82m-onnx",
            name = "Kokoro-82M TTS Engine (Direct ONNX)",
            description = "82M parameter neural speech model delivering human-like voice synthesis locally.",
            category = ModelCategory.VOICE_TTS,
            format = ModelFormat.ONNX,
            fileName = "kokoro_model.onnx",
            sizeBytes = 86_500_000L, // ~82.5 MB
            downloadUrl = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX/resolve/main/onnx/model.onnx",
            hfRepoUrl = "https://huggingface.co/onnx-community/Kokoro-82M-v1.0-ONNX",
            quantization = "ONNX FP32/FP16",
            ramRequirementMB = 220,
            supportsVoiceSynthesis = true
        )
    )
}
