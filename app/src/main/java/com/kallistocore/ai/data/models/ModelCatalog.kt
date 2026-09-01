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
        // 1. Z-Image Turbo 8-Step Models (Verified Direct GGUF Endpoints)
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
        AIModelInfo(
            id = "instruct-pix2pix-mobile",
            name = "InstructPix2Pix Mobile",
            description = "Natural language prompt-based photo editing (e.g. 'make it snowy').",
            category = ModelCategory.IMAGE_GEN_AND_EDIT,
            format = ModelFormat.ONNX,
            fileName = "instruct_pix2pix_quant_int8.onnx",
            sizeBytes = 890_000_000L, // ~848 MB
            downloadUrl = "https://huggingface.co/kallistocore/instruct-pix2pix-mobile/resolve/main/instruct_pix2pix_quant_int8.onnx",
            hfRepoUrl = "https://huggingface.co/timbrooks/instruct-pix2pix",
            quantization = "INT8 ONNX",
            ramRequirementMB = 1400,
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
            hfRepoUrl = "https://huggingface.co/hexgrad/Kokoro-82M",
            quantization = "FP16 / INT8",
            ramRequirementMB = 220,
            supportsVoiceSynthesis = true
        )
    )
}
