package com.kallistocore.ai.domain.workflow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color

enum class PortType(val color: Color) {
    MODEL(Color(0xFF818CF8)),     // Indigo
    CLIP(Color(0xFFFBBF24)),      // Amber
    LATENT(Color(0xFFEC4899)),    // Pink
    IMAGE(Color(0xFF38BDF8)),     // Cyan
    VAE(Color(0xFFA3BE8C)),       // Sage Green
    LORA(Color(0xFFF472B6)),      // Rose
    TEXT(Color(0xFFE2E8F0)),      // White
    NUMBER(Color(0xFFF97316))     // Orange
}

enum class NodeType(val displayName: String, val category: String, val defaultInputs: List<String>, val defaultOutputs: List<String>) {
    CHECKPOINT_LOADER("Load Checkpoint / GGUF", "Loaders", emptyList(), listOf("MODEL", "CLIP", "VAE")),
    LOAD_LORA("Load LoRA", "Loaders", listOf("MODEL", "CLIP"), listOf("MODEL", "CLIP")),
    CLIP_TEXT_ENCODE("CLIP Text Encode (Prompt)", "Conditioning", listOf("CLIP"), listOf("CONDITIONING")),
    KSAMPLER("KSampler (Diffusion Engine)", "Sampling", listOf("MODEL", "POSITIVE", "NEGATIVE", "LATENT"), listOf("LATENT")),
    VAEDECODE("VAE Decode", "Latent", listOf("LATENT", "VAE"), listOf("IMAGE")),
    LOAD_IMAGE("Load Image (Img2Img)", "Image", emptyList(), listOf("IMAGE", "MASK")),
    PREVIEW_IMAGE("Preview Image", "Output", listOf("IMAGE"), emptyList()),
    SAVE_IMAGE("Save Image to DCIM", "Output", listOf("IMAGE"), emptyList()),
    IMAGE_UPSCALE("Image Upscale (Super-Res)", "Post-Processing", listOf("IMAGE"), listOf("IMAGE"))
}

data class NodePort(
    val id: String,
    val name: String,
    val type: PortType,
    val isInput: Boolean
)

data class CanvasNode(
    val id: String,
    val type: NodeType,
    val title: String,
    var position: Offset,
    val inputs: List<NodePort>,
    val outputs: List<NodePort>,
    var params: MutableMap<String, String> = mutableMapOf(),
    var isCollapsed: Boolean = false,
    var isExecuting: Boolean = false
)

data class NodeWireConnection(
    val id: String,
    val fromNodeId: String,
    val fromPortId: String,
    val toNodeId: String,
    val toPortId: String,
    val portType: PortType
)

data class WorkflowGraph(
    val nodes: List<CanvasNode>,
    val connections: List<NodeWireConnection>
)
