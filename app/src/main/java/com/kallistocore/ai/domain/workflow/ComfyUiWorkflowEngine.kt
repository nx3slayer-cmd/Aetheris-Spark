package com.kallistocore.ai.domain.workflow

import android.content.Context
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.InputStream
import java.util.UUID

class ComfyUiWorkflowEngine(private val context: Context) {

    /**
     * Creates a default starting ComfyUI template workflow graph.
     */
    fun createDefaultWorkflow(): WorkflowGraph {
        val checkpointNode = CanvasNode(
            id = "1",
            type = NodeType.CHECKPOINT_LOADER,
            title = "Load Checkpoint / GGUF",
            position = Offset(80f, 160f),
            inputs = emptyList(),
            outputs = listOf(
                NodePort("out_model", "MODEL", PortType.MODEL, false),
                NodePort("out_clip", "CLIP", PortType.CLIP, false),
                NodePort("out_vae", "VAE", PortType.VAE, false)
            ),
            params = mutableMapOf("ckpt_name" to "z_image_turbo-Q2_K.gguf")
        )

        val promptNode = CanvasNode(
            id = "2",
            type = NodeType.CLIP_TEXT_ENCODE,
            title = "Positive Prompt (CLIP)",
            position = Offset(460f, 120f),
            inputs = listOf(NodePort("in_clip", "CLIP", PortType.CLIP, true)),
            outputs = listOf(NodePort("out_cond", "CONDITIONING", PortType.LATENT, false)),
            params = mutableMapOf("text" to "Cyberpunk city at neon midnight, 8k, photorealistic")
        )

        val samplerNode = CanvasNode(
            id = "3",
            type = NodeType.KSAMPLER,
            title = "KSampler (8-Step DiT)",
            position = Offset(840f, 180f),
            inputs = listOf(
                NodePort("in_model", "MODEL", PortType.MODEL, true),
                NodePort("in_positive", "POSITIVE", PortType.LATENT, true),
                NodePort("in_latent", "LATENT", PortType.LATENT, true)
            ),
            outputs = listOf(NodePort("out_latent", "LATENT", PortType.LATENT, false)),
            params = mutableMapOf(
                "steps" to "8",
                "cfg" to "1.5",
                "sampler_name" to "euler",
                "scheduler" to "simple"
            )
        )

        val vaeNode = CanvasNode(
            id = "4",
            type = NodeType.VAEDECODE,
            title = "VAE Decode & Preview",
            position = Offset(1240f, 220f),
            inputs = listOf(
                NodePort("in_latent", "LATENT", PortType.LATENT, true),
                NodePort("in_vae", "VAE", PortType.VAE, true)
            ),
            outputs = listOf(NodePort("out_image", "IMAGE", PortType.IMAGE, false))
        )

        val connections = listOf(
            NodeWireConnection(UUID.randomUUID().toString(), "1", "out_model", "3", "in_model", PortType.MODEL),
            NodeWireConnection(UUID.randomUUID().toString(), "1", "out_clip", "2", "in_clip", PortType.CLIP),
            NodeWireConnection(UUID.randomUUID().toString(), "2", "out_cond", "3", "in_positive", PortType.LATENT),
            NodeWireConnection(UUID.randomUUID().toString(), "1", "out_vae", "4", "in_vae", PortType.VAE),
            NodeWireConnection(UUID.randomUUID().toString(), "3", "out_latent", "4", "in_latent", PortType.LATENT)
        )

        return WorkflowGraph(listOf(checkpointNode, promptNode, samplerNode, vaeNode), connections)
    }

    /**
     * Parses standard ComfyUI JSON format into a visual mobile workflow graph.
     */
    fun parseComfyUiJson(jsonStream: InputStream): WorkflowGraph {
        val jsonString = jsonStream.bufferedReader().use { it.readText() }
        val root = JSONObject(jsonString)
        val nodes = mutableListOf<CanvasNode>()
        val connections = mutableListOf<NodeWireConnection>()

        var xOffset = 80f
        var yOffset = 140f

        val keys = root.keys()
        while (keys.hasNext()) {
            val nodeId = keys.next()
            val nodeObj = root.optJSONObject(nodeId) ?: continue
            val classType = nodeObj.optString("class_type", "Unknown Node")

            val mappedType = when {
                classType.contains("Checkpoint", ignoreCase = true) -> NodeType.CHECKPOINT_LOADER
                classType.contains("CLIPTextEncode", ignoreCase = true) || classType.contains("Prompt", ignoreCase = true) -> NodeType.CLIP_TEXT_ENCODE
                classType.contains("KSampler", ignoreCase = true) -> NodeType.KSAMPLER
                classType.contains("VAEDecode", ignoreCase = true) -> NodeType.VAEDECODE
                classType.contains("LoadImage", ignoreCase = true) -> NodeType.LOAD_IMAGE
                classType.contains("Upscale", ignoreCase = true) -> NodeType.IMAGE_UPSCALE
                else -> NodeType.SAVE_IMAGE
            }

            val inputs = mutableListOf<NodePort>()
            val outputs = mutableListOf<NodePort>()
            val params = mutableMapOf<String, String>()

            val inputsObj = nodeObj.optJSONObject("inputs")
            inputsObj?.keys()?.forEach { inputKey ->
                val inputVal = inputsObj.get(inputKey)
                if (inputVal is org.json.JSONArray && inputVal.length() >= 2) {
                    val targetNodeId = inputVal.getString(0)
                    val targetSlot = inputVal.getInt(1)
                    inputs.add(NodePort("in_$inputKey", inputKey, PortType.LATENT, true))
                    connections.add(
                        NodeWireConnection(
                            UUID.randomUUID().toString(),
                            targetNodeId,
                            "out_$targetSlot",
                            nodeId,
                            "in_$inputKey",
                            PortType.LATENT
                        )
                    )
                } else {
                    params[inputKey] = inputVal.toString()
                }
            }

            outputs.add(NodePort("out_0", "OUT", PortType.LATENT, false))

            nodes.add(
                CanvasNode(
                    id = nodeId,
                    type = mappedType,
                    title = classType,
                    position = Offset(xOffset, yOffset),
                    inputs = inputs,
                    outputs = outputs,
                    params = params
                )
            )

            xOffset += 380f
            if (xOffset > 1600f) {
                xOffset = 80f
                yOffset += 340f
            }
        }

        return if (nodes.isNotEmpty()) WorkflowGraph(nodes, connections) else createDefaultWorkflow()
    }

    /**
     * Executes the visual workflow sequence node by node.
     */
    suspend fun executeWorkflow(
        graph: WorkflowGraph,
        onNodeExecuting: (nodeId: String) -> Unit
    ) = withContext(Dispatchers.IO) {
        for (node in graph.nodes) {
            onNodeExecuting(node.id)
            node.isExecuting = true
            delay(500) // Simulates node execution pipeline
            node.isExecuting = false
        }
    }
}
