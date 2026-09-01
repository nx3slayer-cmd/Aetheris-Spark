package com.kallistocore.ai.domain.workflow

import android.content.Context
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.InputStream
import java.util.UUID

class ComfyUiWorkflowEngine(private val context: Context) {

    val savedWorkflowsDir: File by lazy {
        File(context.filesDir, "saved_workflows").apply { if (!exists()) mkdirs() }
    }

    /**
     * Dedicated Z-Image Turbo Img2Img Template Workflow.
     */
    fun createZImageImg2ImgWorkflow(): WorkflowGraph {
        val loadImage = CanvasNode(
            id = "1",
            type = NodeType.LOAD_IMAGE,
            title = "Load Source Image",
            position = Offset(80f, 160f),
            inputs = emptyList(),
            outputs = listOf(NodePort("out_image", "IMAGE", PortType.IMAGE, false)),
            params = mutableMapOf("image" to "input.png")
        )

        val vaeEncode = CanvasNode(
            id = "2",
            type = NodeType.VAEDECODE,
            title = "VAE Encode (Pixels to Latent)",
            position = Offset(460f, 160f),
            inputs = listOf(
                NodePort("in_image", "IMAGE", PortType.IMAGE, true),
                NodePort("in_vae", "VAE", PortType.VAE, true)
            ),
            outputs = listOf(NodePort("out_latent", "LATENT", PortType.LATENT, false))
        )

        val checkpoint = CanvasNode(
            id = "3",
            type = NodeType.CHECKPOINT_LOADER,
            title = "Z-Image Turbo 8-Step",
            position = Offset(80f, 440f),
            inputs = emptyList(),
            outputs = listOf(
                NodePort("out_model", "MODEL", PortType.MODEL, false),
                NodePort("out_clip", "CLIP", PortType.CLIP, false),
                NodePort("out_vae", "VAE", PortType.VAE, false)
            ),
            params = mutableMapOf("ckpt_name" to "z_image_turbo-Q2_K.gguf")
        )

        val prompt = CanvasNode(
            id = "4",
            type = NodeType.CLIP_TEXT_ENCODE,
            title = "Img2Img Prompt Guidance",
            position = Offset(460f, 440f),
            inputs = listOf(NodePort("in_clip", "CLIP", PortType.CLIP, true)),
            outputs = listOf(NodePort("out_cond", "CONDITIONING", PortType.LATENT, false)),
            params = mutableMapOf("text" to "Transform into cyberpunk neon style, 8k, sharp focus")
        )

        val sampler = CanvasNode(
            id = "5",
            type = NodeType.KSAMPLER,
            title = "KSampler (Denoise 0.65)",
            position = Offset(860f, 260f),
            inputs = listOf(
                NodePort("in_model", "MODEL", PortType.MODEL, true),
                NodePort("in_positive", "POSITIVE", PortType.LATENT, true),
                NodePort("in_latent", "LATENT", PortType.LATENT, true)
            ),
            outputs = listOf(NodePort("out_latent", "LATENT", PortType.LATENT, false)),
            params = mutableMapOf("steps" to "8", "cfg" to "1.5", "denoise" to "0.65", "sampler" to "euler")
        )

        val vaeDecode = CanvasNode(
            id = "6",
            type = NodeType.VAEDECODE,
            title = "VAE Decode Output",
            position = Offset(1260f, 260f),
            inputs = listOf(
                NodePort("in_latent", "LATENT", PortType.LATENT, true),
                NodePort("in_vae", "VAE", PortType.VAE, true)
            ),
            outputs = listOf(NodePort("out_image", "IMAGE", PortType.IMAGE, false))
        )

        val connections = listOf(
            NodeWireConnection(UUID.randomUUID().toString(), "1", "out_image", "2", "in_image", PortType.IMAGE),
            NodeWireConnection(UUID.randomUUID().toString(), "3", "out_vae", "2", "in_vae", PortType.VAE),
            NodeWireConnection(UUID.randomUUID().toString(), "3", "out_clip", "4", "in_clip", PortType.CLIP),
            NodeWireConnection(UUID.randomUUID().toString(), "3", "out_model", "5", "in_model", PortType.MODEL),
            NodeWireConnection(UUID.randomUUID().toString(), "4", "out_cond", "5", "in_positive", PortType.LATENT),
            NodeWireConnection(UUID.randomUUID().toString(), "2", "out_latent", "5", "in_latent", PortType.LATENT),
            NodeWireConnection(UUID.randomUUID().toString(), "3", "out_vae", "6", "in_vae", PortType.VAE),
            NodeWireConnection(UUID.randomUUID().toString(), "5", "out_latent", "6", "in_latent", PortType.LATENT)
        )

        return WorkflowGraph(listOf(loadImage, vaeEncode, checkpoint, prompt, sampler, vaeDecode), connections)
    }

    fun createDefaultWorkflow(): WorkflowGraph {
        return createZImageImg2ImgWorkflow()
    }

    fun saveWorkflowToFile(graph: WorkflowGraph, workflowName: String): File {
        val root = JSONObject()
        val nodesArray = JSONArray()

        for (node in graph.nodes) {
            val nodeObj = JSONObject().apply {
                put("id", node.id)
                put("type", node.type.name)
                put("title", node.title)
                put("posX", node.position.x.toDouble())
                put("posY", node.position.y.toDouble())
                val paramsObj = JSONObject()
                node.params.forEach { (k, v) -> paramsObj.put(k, v) }
                put("params", paramsObj)
            }
            nodesArray.put(nodeObj)
        }
        root.put("nodes", nodesArray)

        val connsArray = JSONArray()
        for (c in graph.connections) {
            val connObj = JSONObject().apply {
                put("fromNode", c.fromNodeId)
                put("fromPort", c.fromPortId)
                put("toNode", c.toNodeId)
                put("toPort", c.toPortId)
                put("portType", c.portType.name)
            }
            connsArray.put(connObj)
        }
        root.put("connections", connsArray)

        val cleanName = workflowName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val file = File(savedWorkflowsDir, "${cleanName}.json")
        file.writeText(root.toString(2))
        return file
    }

    fun listSavedWorkflows(): List<File> {
        return savedWorkflowsDir.listFiles { _, name -> name.endsWith(".json") }?.toList() ?: emptyList()
    }

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
            val classType = nodeObj.optString("class_type", "Node")

            val mappedType = when {
                classType.contains("Checkpoint", ignoreCase = true) -> NodeType.CHECKPOINT_LOADER
                classType.contains("LoRA", ignoreCase = true) -> NodeType.LOAD_LORA
                classType.contains("CLIPTextEncode", ignoreCase = true) -> NodeType.CLIP_TEXT_ENCODE
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
                if (inputVal is JSONArray && inputVal.length() >= 2) {
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

    suspend fun executeWorkflow(graph: WorkflowGraph, onNodeExecuting: (nodeId: String) -> Unit) = withContext(Dispatchers.IO) {
        for (node in graph.nodes) {
            onNodeExecuting(node.id)
            node.isExecuting = true
            delay(400)
            node.isExecuting = false
        }
    }
}
