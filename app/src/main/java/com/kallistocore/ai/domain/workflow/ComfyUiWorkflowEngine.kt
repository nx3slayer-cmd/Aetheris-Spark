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

enum class JobPreset(val displayName: String, val description: String) {
    TEXT_TO_IMAGE_Z_TURBO("Text-to-Image (Z-Turbo 8-Step)", "Full text prompt to 1024x1024 photorealistic output"),
    IMG2IMG_Z_EDIT("Img2Img Stylizer (Z-Image Edit)", "Transform existing photos with prompt guidance"),
    AI_UPSCALER_SUPER_RES("AI 2x/4x Super-Resolution", "Enhance details and double resolution of input images"),
    LORA_CHARACTER_DETAILER("LoRA Character Detailer", "Chain LoRA weights for face and lighting refinement"),
    FAST_SKETCH_TO_ART("Fast Sketch-to-Art (SD-Turbo)", "Instant 2-4 step mobile transformation")
}

class ComfyUiWorkflowEngine(private val context: Context) {

    val savedWorkflowsDir: File by lazy {
        File(context.filesDir, "saved_workflows").apply { if (!exists()) mkdirs() }
    }

    fun loadPresetWorkflow(preset: JobPreset): WorkflowGraph {
        return when (preset) {
            JobPreset.TEXT_TO_IMAGE_Z_TURBO -> createTextToImageZTurboWorkflow()
            JobPreset.IMG2IMG_Z_EDIT -> createZImageImg2ImgWorkflow()
            JobPreset.AI_UPSCALER_SUPER_RES -> createUpscalerWorkflow()
            JobPreset.LORA_CHARACTER_DETAILER -> createLoraWorkflow()
            JobPreset.FAST_SKETCH_TO_ART -> createFastSketchWorkflow()
        }
    }

    fun createDefaultWorkflow(): WorkflowGraph {
        return createTextToImageZTurboWorkflow()
    }

    private fun createTextToImageZTurboWorkflow(): WorkflowGraph {
        val checkpoint = CanvasNode("1", NodeType.CHECKPOINT_LOADER, "Z-Image Turbo 8-Step", Offset(60f, 120f), emptyList(), listOf(NodePort("out_model", "MODEL", PortType.MODEL, false), NodePort("out_clip", "CLIP", PortType.CLIP, false), NodePort("out_vae", "VAE", PortType.VAE, false)), mutableMapOf("ckpt_name" to "z_image_turbo-Q2_K.gguf"))
        val prompt = CanvasNode("2", NodeType.CLIP_TEXT_ENCODE, "Positive Prompt (CLIP)", Offset(420f, 100f), listOf(NodePort("in_clip", "CLIP", PortType.CLIP, true)), listOf(NodePort("out_cond", "CONDITIONING", PortType.LATENT, false)), mutableMapOf("text" to "Cyberpunk city at neon midnight, 8k, photorealistic"))
        val sampler = CanvasNode("3", NodeType.KSAMPLER, "KSampler (8-Step DiT)", Offset(780f, 140f), listOf(NodePort("in_model", "MODEL", PortType.MODEL, true), NodePort("in_positive", "POSITIVE", PortType.LATENT, true), NodePort("in_latent", "LATENT", PortType.LATENT, true)), listOf(NodePort("out_latent", "LATENT", PortType.LATENT, false)), mutableMapOf("steps" to "8", "cfg" to "1.5", "sampler" to "euler"))
        val vaeDecode = CanvasNode("4", NodeType.VAEDECODE, "VAE Decode & Preview", Offset(1140f, 180f), listOf(NodePort("in_latent", "LATENT", PortType.LATENT, true), NodePort("in_vae", "VAE", PortType.VAE, true)), listOf(NodePort("out_image", "IMAGE", PortType.IMAGE, false)))

        val conns = listOf(
            NodeWireConnection(UUID.randomUUID().toString(), "1", "out_model", "3", "in_model", PortType.MODEL),
            NodeWireConnection(UUID.randomUUID().toString(), "1", "out_clip", "2", "in_clip", PortType.CLIP),
            NodeWireConnection(UUID.randomUUID().toString(), "2", "out_cond", "3", "in_positive", PortType.LATENT),
            NodeWireConnection(UUID.randomUUID().toString(), "1", "out_vae", "4", "in_vae", PortType.VAE),
            NodeWireConnection(UUID.randomUUID().toString(), "3", "out_latent", "4", "in_latent", PortType.LATENT)
        )
        return WorkflowGraph(listOf(checkpoint, prompt, sampler, vaeDecode), conns)
    }

    fun createZImageImg2ImgWorkflow(): WorkflowGraph {
        val loadImage = CanvasNode("1", NodeType.LOAD_IMAGE, "Load Source Image", Offset(60f, 120f), emptyList(), listOf(NodePort("out_image", "IMAGE", PortType.IMAGE, false)), mutableMapOf("image" to "input.png"))
        val vaeEncode = CanvasNode("2", NodeType.VAEDECODE, "VAE Encode", Offset(420f, 120f), listOf(NodePort("in_image", "IMAGE", PortType.IMAGE, true), NodePort("in_vae", "VAE", PortType.VAE, true)), listOf(NodePort("out_latent", "LATENT", PortType.LATENT, false)))
        val checkpoint = CanvasNode("3", NodeType.CHECKPOINT_LOADER, "Z-Image Turbo GGUF", Offset(60f, 380f), emptyList(), listOf(NodePort("out_model", "MODEL", PortType.MODEL, false), NodePort("out_clip", "CLIP", PortType.CLIP, false), NodePort("out_vae", "VAE", PortType.VAE, false)), mutableMapOf("ckpt_name" to "z_image_turbo-Q2_K.gguf"))
        val prompt = CanvasNode("4", NodeType.CLIP_TEXT_ENCODE, "Img2Img Prompt", Offset(420f, 380f), listOf(NodePort("in_clip", "CLIP", PortType.CLIP, true)), listOf(NodePort("out_cond", "CONDITIONING", PortType.LATENT, false)), mutableMapOf("text" to "Transform into cyberpunk style, 8k"))
        val sampler = CanvasNode("5", NodeType.KSAMPLER, "KSampler (Denoise 0.65)", Offset(780f, 220f), listOf(NodePort("in_model", "MODEL", PortType.MODEL, true), NodePort("in_positive", "POSITIVE", PortType.LATENT, true), NodePort("in_latent", "LATENT", PortType.LATENT, true)), listOf(NodePort("out_latent", "LATENT", PortType.LATENT, false)), mutableMapOf("steps" to "8", "cfg" to "1.5", "denoise" to "0.65"))
        val vaeDecode = CanvasNode("6", NodeType.VAEDECODE, "VAE Decode Output", Offset(1140f, 220f), listOf(NodePort("in_latent", "LATENT", PortType.LATENT, true), NodePort("in_vae", "VAE", PortType.VAE, true)), listOf(NodePort("out_image", "IMAGE", PortType.IMAGE, false)))

        val conns = listOf(
            NodeWireConnection(UUID.randomUUID().toString(), "1", "out_image", "2", "in_image", PortType.IMAGE),
            NodeWireConnection(UUID.randomUUID().toString(), "3", "out_vae", "2", "in_vae", PortType.VAE),
            NodeWireConnection(UUID.randomUUID().toString(), "3", "out_clip", "4", "in_clip", PortType.CLIP),
            NodeWireConnection(UUID.randomUUID().toString(), "3", "out_model", "5", "in_model", PortType.MODEL),
            NodeWireConnection(UUID.randomUUID().toString(), "4", "out_cond", "5", "in_positive", PortType.LATENT),
            NodeWireConnection(UUID.randomUUID().toString(), "2", "out_latent", "5", "in_latent", PortType.LATENT),
            NodeWireConnection(UUID.randomUUID().toString(), "3", "out_vae", "6", "in_vae", PortType.VAE),
            NodeWireConnection(UUID.randomUUID().toString(), "5", "out_latent", "6", "in_latent", PortType.LATENT)
        )
        return WorkflowGraph(listOf(loadImage, vaeEncode, checkpoint, prompt, sampler, vaeDecode), conns)
    }

    private fun createUpscalerWorkflow(): WorkflowGraph {
        val loadImage = CanvasNode("1", NodeType.LOAD_IMAGE, "Load Low-Res Image", Offset(60f, 160f), emptyList(), listOf(NodePort("out_image", "IMAGE", PortType.IMAGE, false)), mutableMapOf("image" to "input.png"))
        val upscale = CanvasNode("2", NodeType.IMAGE_UPSCALE, "AI Super-Res (2x/4x)", Offset(420f, 160f), listOf(NodePort("in_image", "IMAGE", PortType.IMAGE, true)), listOf(NodePort("out_image", "IMAGE", PortType.IMAGE, false)), mutableMapOf("upscale_by" to "2.0"))
        val saveImage = CanvasNode("3", NodeType.SAVE_IMAGE, "Save to DCIM/KallistoAI", Offset(780f, 160f), listOf(NodePort("in_image", "IMAGE", PortType.IMAGE, true)), emptyList())

        val conns = listOf(
            NodeWireConnection(UUID.randomUUID().toString(), "1", "out_image", "2", "in_image", PortType.IMAGE),
            NodeWireConnection(UUID.randomUUID().toString(), "2", "out_image", "3", "in_image", PortType.IMAGE)
        )
        return WorkflowGraph(listOf(loadImage, upscale, saveImage), conns)
    }

    private fun createLoraWorkflow(): WorkflowGraph {
        val checkpoint = CanvasNode("1", NodeType.CHECKPOINT_LOADER, "Z-Image Turbo", Offset(60f, 160f), emptyList(), listOf(NodePort("out_model", "MODEL", PortType.MODEL, false), NodePort("out_clip", "CLIP", PortType.CLIP, false), NodePort("out_vae", "VAE", PortType.VAE, false)))
        val lora = CanvasNode("2", NodeType.LOAD_LORA, "Apply LoRA Weights", Offset(420f, 160f), listOf(NodePort("in_model", "MODEL", PortType.MODEL, true), NodePort("in_clip", "CLIP", PortType.CLIP, true)), listOf(NodePort("out_model", "MODEL", PortType.MODEL, false), NodePort("out_clip", "CLIP", PortType.CLIP, false)), mutableMapOf("strength" to "0.85"))
        val prompt = CanvasNode("3", NodeType.CLIP_TEXT_ENCODE, "Positive Prompt", Offset(780f, 100f), listOf(NodePort("in_clip", "CLIP", PortType.CLIP, true)), listOf(NodePort("out_cond", "CONDITIONING", PortType.LATENT, false)))
        val sampler = CanvasNode("4", NodeType.KSAMPLER, "KSampler", Offset(1140f, 160f), listOf(NodePort("in_model", "MODEL", PortType.MODEL, true), NodePort("in_positive", "POSITIVE", PortType.LATENT, true)), listOf(NodePort("out_latent", "LATENT", PortType.LATENT, false)))

        val conns = listOf(
            NodeWireConnection(UUID.randomUUID().toString(), "1", "out_model", "2", "in_model", PortType.MODEL),
            NodeWireConnection(UUID.randomUUID().toString(), "1", "out_clip", "2", "in_clip", PortType.CLIP),
            NodeWireConnection(UUID.randomUUID().toString(), "2", "out_clip", "3", "in_clip", PortType.CLIP),
            NodeWireConnection(UUID.randomUUID().toString(), "2", "out_model", "4", "in_model", PortType.MODEL),
            NodeWireConnection(UUID.randomUUID().toString(), "3", "out_cond", "4", "in_positive", PortType.LATENT)
        )
        return WorkflowGraph(listOf(checkpoint, lora, prompt, sampler), conns)
    }

    private fun createFastSketchWorkflow(): WorkflowGraph {
        val loadImage = CanvasNode("1", NodeType.LOAD_IMAGE, "Load Sketch Photo", Offset(60f, 160f), emptyList(), listOf(NodePort("out_image", "IMAGE", PortType.IMAGE, false)))
        val sampler = CanvasNode("2", NodeType.KSAMPLER, "Fast SD-Turbo (4-Step)", Offset(420f, 160f), listOf(NodePort("in_image", "IMAGE", PortType.IMAGE, true)), listOf(NodePort("out_image", "IMAGE", PortType.IMAGE, false)), mutableMapOf("steps" to "4", "denoise" to "0.7"))
        val saveImage = CanvasNode("3", NodeType.SAVE_IMAGE, "Save to Gallery", Offset(780f, 160f), listOf(NodePort("in_image", "IMAGE", PortType.IMAGE, true)), emptyList())

        val conns = listOf(
            NodeWireConnection(UUID.randomUUID().toString(), "1", "out_image", "2", "in_image", PortType.IMAGE),
            NodeWireConnection(UUID.randomUUID().toString(), "2", "out_image", "3", "in_image", PortType.IMAGE)
        )
        return WorkflowGraph(listOf(loadImage, sampler, saveImage), conns)
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

        var xOffset = 60f
        var yOffset = 120f

        val keys = root.keys()
        while (keys.hasNext()) {
            val nodeId = keys.next()
            val nodeObj = root.optJSONObject(nodeId) ?: continue
            val classType = nodeObj.optString("class_type", "Node")

            val mappedType = when {
                classType.contains("Checkpoint", ignoreCase = true) -> NodeType.CHECKPOINT_LOADER
                classType.contains("LoRA", ignoreCase = true) -> NodeType.LOAD_LORA
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
                if (inputVal is JSONArray && inputVal.length() >= 2) {
                    val targetNodeId = inputVal.getString(0)
                    val targetSlot = inputVal.getInt(1)
                    inputs.add(NodePort("in_$inputKey", inputKey, PortType.LATENT, true))
                    connections.add(NodeWireConnection(UUID.randomUUID().toString(), targetNodeId, "out_$targetSlot", nodeId, "in_$inputKey", PortType.LATENT))
                } else {
                    params[inputKey] = inputVal.toString()
                }
            }

            outputs.add(NodePort("out_0", "OUT", PortType.LATENT, false))

            nodes.add(CanvasNode(id = nodeId, type = mappedType, title = classType, position = Offset(xOffset, yOffset), inputs = inputs, outputs = outputs, params = params))

            xOffset += 360f
            if (xOffset > 1500f) {
                xOffset = 60f
                yOffset += 320f
            }
        }

        return if (nodes.isNotEmpty()) WorkflowGraph(nodes, connections) else createDefaultWorkflow()
    }

    suspend fun executeWorkflow(graph: WorkflowGraph, onNodeExecuting: (nodeId: String) -> Unit) = withContext(Dispatchers.IO) {
        for (node in graph.nodes) {
            onNodeExecuting(node.id)
            node.isExecuting = true
            delay(350)
            node.isExecuting = false
        }
    }
}
