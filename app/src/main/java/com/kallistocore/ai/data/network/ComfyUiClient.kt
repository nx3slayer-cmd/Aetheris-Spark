package com.kallistocore.ai.data.network

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.UUID
import java.util.concurrent.TimeUnit

class ComfyUiClient(private val context: Context) {

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()

    private val httpClient = HttpClient(OkHttp) {
        engine { preconfigured = okHttpClient }
    }

    private val prefs = context.getSharedPreferences("kallisto_comfy_prefs", Context.MODE_PRIVATE)

    var serverUrl: String
        get() = prefs.getString("comfy_server_url", "http://192.168.1.100:8188") ?: "http://192.168.1.100:8188"
        set(value) = prefs.edit().putString("comfy_server_url", value.trim().removeSuffix("/")).apply()

    /**
     * Checks if ComfyUI server is reachable on the local network.
     */
    suspend fun checkServerConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val response = httpClient.get("$serverUrl/system_stats")
            response.status.isSuccess()
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Uploads an image to ComfyUI for Img2Img workflows.
     */
    suspend fun uploadImage(bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            val stream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            val byteArray = stream.toByteArray()
            val fileName = "input_${System.currentTimeMillis()}.png"

            val response: HttpResponse = httpClient.submitFormWithBinaryData(
                url = "$serverUrl/upload/image",
                formData = formData {
                    append("image", byteArray, Headers.build {
                        append(HttpHeaders.ContentType, "image/png")
                        append(HttpHeaders.ContentDisposition, "filename=\"$fileName\"")
                    })
                    append("overwrite", "true")
                }
            )

            if (response.status.isSuccess()) {
                val json = JSONObject(response.bodyAsText())
                json.optString("name", fileName)
            } else null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Sends prompt workflow JSON to ComfyUI /prompt endpoint and retrieves the generated output image.
     */
    suspend fun queuePromptAndFetchImage(
        promptText: String,
        inputImageName: String? = null,
        steps: Int = 8,
        cfg: Float = 1.5f,
        width: Int = 512,
        height: Int = 512
    ): Bitmap? = withContext(Dispatchers.IO) {
        try {
            val clientId = UUID.randomUUID().toString()
            val workflowJson = buildComfyPromptJson(promptText, inputImageName, steps, cfg, width, height, clientId)

            // Submit prompt
            val submitResponse = httpClient.post("$serverUrl/prompt") {
                contentType(ContentType.Application.Json)
                setBody(workflowJson.toString())
            }

            if (!submitResponse.status.isSuccess()) return@withContext null
            val promptResult = JSONObject(submitResponse.bodyAsText())
            val promptId = promptResult.optString("prompt_id")
            if (promptId.isBlank()) return@withContext null

            // Poll /history until completed
            var outputFilename: String? = null
            var subfolder: String? = null
            var attempts = 0

            while (attempts < 60 && outputFilename == null) {
                kotlinx.coroutines.delay(1000)
                attempts++

                val histResponse = httpClient.get("$serverUrl/history/$promptId")
                if (histResponse.status.isSuccess()) {
                    val histJson = JSONObject(histResponse.bodyAsText())
                    if (histJson.has(promptId)) {
                        val promptData = histJson.getJSONObject(promptId)
                        val outputs = promptData.optJSONObject("outputs")
                        if (outputs != null && outputs.length() > 0) {
                            val firstNodeKey = outputs.keys().next()
                            val nodeOutput = outputs.getJSONObject(firstNodeKey)
                            val imagesArray = nodeOutput.optJSONArray("images")
                            if (imagesArray != null && imagesArray.length() > 0) {
                                val imgObj = imagesArray.getJSONObject(0)
                                outputFilename = imgObj.getString("filename")
                                subfolder = imgObj.optString("subfolder", "")
                            }
                        }
                    }
                }
            }

            // Download generated image from ComfyUI /view endpoint
            if (outputFilename != null) {
                val imageUrl = "$serverUrl/view?filename=$outputFilename&subfolder=$subfolder&type=output"
                val imageResponse = httpClient.get(imageUrl)
                if (imageResponse.status.isSuccess()) {
                    val bytes = imageResponse.bodyAsChannel().toByteArray()
                    return@withContext BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun buildComfyPromptJson(
        prompt: String,
        inputImageName: String?,
        steps: Int,
        cfg: Float,
        width: Int,
        height: Int,
        clientId: String
    ): JSONObject {
        val root = JSONObject()
        root.put("client_id", clientId)

        val promptObj = JSONObject()

        if (inputImageName != null) {
            // Img2Img Workflow
            promptObj.put("1", JSONObject().apply {
                put("class_type", "LoadImage")
                put("inputs", JSONObject().put("image", inputImageName))
            })
            promptObj.put("2", JSONObject().apply {
                put("class_type", "CLIPTextEncode")
                put("inputs", JSONObject().put("text", prompt).put("clip", org.json.JSONArray().put("4").put(1)))
            })
            promptObj.put("3", JSONObject().apply {
                put("class_type", "VAEEncode")
                put("inputs", JSONObject().put("pixels", org.json.JSONArray().put("1").put(0)).put("vae", org.json.JSONArray().put("4").put(2)))
            })
            promptObj.put("4", JSONObject().apply {
                put("class_type", "CheckpointLoaderSimple")
                put("inputs", JSONObject().put("ckpt_name", "z_image_turbo_bf16.safetensors"))
            })
            promptObj.put("5", JSONObject().apply {
                put("class_type", "KSampler")
                put("inputs", JSONObject().apply {
                    put("seed", System.currentTimeMillis() % 1000000000)
                    put("steps", steps)
                    put("cfg", cfg)
                    put("sampler_name", "euler")
                    put("scheduler", "simple")
                    put("denoise", 0.75)
                    put("model", org.json.JSONArray().put("4").put(0))
                    put("positive", org.json.JSONArray().put("2").put(0))
                    put("negative", org.json.JSONArray().put("2").put(0))
                    put("latent_image", org.json.JSONArray().put("3").put(0))
                })
            })
            promptObj.put("6", JSONObject().apply {
                put("class_type", "VAEDecode")
                put("inputs", JSONObject().put("samples", org.json.JSONArray().put("5").put(0)).put("vae", org.json.JSONArray().put("4").put(2)))
            })
            promptObj.put("7", JSONObject().apply {
                put("class_type", "SaveImage")
                put("inputs", JSONObject().put("filename_prefix", "Kallisto_Comfy").put("images", org.json.JSONArray().put("6").put(0)))
            })
        } else {
            // Text-to-Image Workflow
            promptObj.put("1", JSONObject().apply {
                put("class_type", "CheckpointLoaderSimple")
                put("inputs", JSONObject().put("ckpt_name", "z_image_turbo_bf16.safetensors"))
            })
            promptObj.put("2", JSONObject().apply {
                put("class_type", "CLIPTextEncode")
                put("inputs", JSONObject().put("text", prompt).put("clip", org.json.JSONArray().put("1").put(1)))
            })
            promptObj.put("3", JSONObject().apply {
                put("class_type", "EmptyLatentImage")
                put("inputs", JSONObject().put("width", width).put("height", height).put("batch_size", 1))
            })
            promptObj.put("4", JSONObject().apply {
                put("class_type", "KSampler")
                put("inputs", JSONObject().apply {
                    put("seed", System.currentTimeMillis() % 1000000000)
                    put("steps", steps)
                    put("cfg", cfg)
                    put("sampler_name", "euler")
                    put("scheduler", "simple")
                    put("denoise", 1.0)
                    put("model", org.json.JSONArray().put("1").put(0))
                    put("positive", org.json.JSONArray().put("2").put(0))
                    put("negative", org.json.JSONArray().put("2").put(0))
                    put("latent_image", org.json.JSONArray().put("3").put(0))
                })
            })
            promptObj.put("5", JSONObject().apply {
                put("class_type", "VAEDecode")
                put("inputs", JSONObject().put("samples", org.json.JSONArray().put("4").put(0)).put("vae", org.json.JSONArray().put("1").put(2)))
            })
            promptObj.put("6", JSONObject().apply {
                put("class_type", "SaveImage")
                put("inputs", JSONObject().put("filename_prefix", "Kallisto_Comfy").put("images", org.json.JSONArray().put("5").put(0)))
            })
        }

        root.put("prompt", promptObj)
        return root
    }

    private suspend fun io.ktor.utils.io.ByteReadChannel.toByteArray(): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(4096)
        while (!isClosedForRead) {
            val read = readAvailable(buffer, 0, buffer.size)
            if (read <= 0) break
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }
}
