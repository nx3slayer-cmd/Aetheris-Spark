package com.kallistocore.ai.domain.workflow

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import kotlinx.serialization.Serializable

enum class PortType(val color: Color) {
    MODEL(Color(0xFF818CF8)),     // Indigo
    CLIP(Color(0xFFFBBF24)),      // Amber
    LATENT(Color(0xFFEC4899)),    // Pink
    IMAGE(Color(0xFF38BDF8)),     // Cyan
    VAE(Color(0xFFA3BE8C)),       // Sage Green
    TEXT(Color(0xFFE2E8F0)),      // White
    NUMBER(Color(0xFFF97316))     // Orange
}

enum class NodeType(val displayName: String, val category: String) {
    CHECKPOINT_LOADER("Load Checkpoint / GGUF", "Loaders"),
    CLIP_TEXT_ENCODE("CLIP Text Encode (Prompt)", "Conditioning"),
    KSAMPLER("KSampler (Diffusion Engine)", "Sampling"),
    VAEDECODE("VAE Decode", "Latent"),
    LOAD_IMAGE("Load Image (Img2Img)", "Image"),
    IMAGE_UPSCALE("AI Super-Resolution Upscale", "Post-Processing"),
    SAVE_IMAGE("Save & Preview Output", "Output")
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
