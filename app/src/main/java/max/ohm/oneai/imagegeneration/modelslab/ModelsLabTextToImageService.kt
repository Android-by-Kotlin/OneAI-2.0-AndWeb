package max.ohm.oneai.imagegeneration.modelslab

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import max.ohm.oneai.imagetoimage.modelslabapikey.MODELSLAB_API_KEY
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class ModelsLabTextToImageService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()
    
    data class TextToImageRequest(
        val prompt: String,
        val negativePrompt: String = "",
        val modelId: String = "epic-realism-rc1",
        val scheduler: String = "DPMSolverSinglestepScheduler",
        val width: Int = 768,
        val height: Int = 1024,
        val guidanceScale: Double = 7.5,
        val numInferenceSteps: Int = 31,
        val steps: Int = 21,
        val samples: Int = 1,
        val seed: Int = 0,
        val safetyChecker: Boolean = true,
        val enhancePrompt: Boolean = false,
        val useKarrasSigmas: Boolean = true,
        val clipSkip: Int = 1,
        val tomesd: Boolean = true,
        val base64: Boolean = false  // Changed to false to get URL responses
    )
    
    data class TextToImageResponse(
        val status: String,
        val generationTime: Double? = null,
        val id: String? = null,
        val output: List<String>? = null,
        val meta: JSONObject? = null,
        val error: String? = null,
        val nsfwContentDetected: Boolean = false  // Add NSFW detection flag
    )
    
    suspend fun generateImage(request: TextToImageRequest): TextToImageResponse = withContext(Dispatchers.IO) {
        try {
            Log.d("ModelsLabText2Img", "Starting text-to-image generation with model: ${request.modelId}")
            
            val jsonBody = JSONObject().apply {
                put("key", MODELSLAB_API_KEY)
                put("prompt", request.prompt)
                put("negative_prompt", request.negativePrompt)
                put("model_id", request.modelId)
                put("scheduler", request.scheduler)
                put("safety_checker", if (request.safetyChecker) "yes" else "no")
                put("width", request.width.toString())
                put("height", request.height.toString())
                put("guidance_scale", request.guidanceScale)
                put("num_inference_steps", request.numInferenceSteps.toString())
                put("seed", request.seed.toString())
                put("steps", request.steps.toString())
                put("samples", request.samples.toString())
                put("full_url", "no")
                put("instant_response", "no")
                put("tomesd", if (request.tomesd) "yes" else "no")
                put("free_u", "no")
                put("upscale", 0)
                put("multi_lingual", "no")
                put("panorama", "no")
                put("self_attention", "no")
                put("use_karras_sigmas", if (request.useKarrasSigmas) "yes" else "no")
                put("algorithm_type", "")
                put("safety_checker_type", "sensitive_content_text")
                put("embeddings", null)
                put("vae", null)
                put("lora_model", null)
                put("enhance_prompt", request.enhancePrompt)
                put("lora_strength", 1)
                put("clip_skip", request.clipSkip)
                put("temp", "no")
                put("base64", request.base64)
            }
            
            Log.d("ModelsLabText2Img", "Request JSON: ${jsonBody.toString(2)}")
            
            val httpRequest = Request.Builder()
                .url("https://modelslab.com/api/v7/images/text-to-image")
                .addHeader("key", MODELSLAB_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()
            
            Log.d("ModelsLabText2Img", "Response code: ${response.code}")
            Log.d("ModelsLabText2Img", "Response body: $responseBody")
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val status = jsonResponse.optString("status", "")
                
                when (status) {
                    "success" -> {
                        val output = mutableListOf<String>()
                        val outputArray = jsonResponse.optJSONArray("output")
                        if (outputArray != null) {
                            for (i in 0 until outputArray.length()) {
                                output.add(outputArray.getString(i))
                            }
                        }
                        
                        // Check for NSFW content detection in the response
                        val nsfwDetected = jsonResponse.optBoolean("nsfw_content_detected", false) ||
                                          jsonResponse.optString("nsfw_filter", "").equals("triggered", true) ||
                                          jsonResponse.optJSONObject("meta")?.optBoolean("nsfw_detected", false) ?: false
                        
                        if (nsfwDetected) {
                            Log.w("ModelsLabText2Img", "NSFW content detected - returning blank image")
                            // Return empty output list when NSFW is detected to prevent showing the image
                            return@withContext TextToImageResponse(
                                status = status,
                                generationTime = jsonResponse.optDouble("generationTime"),
                                id = jsonResponse.optString("id"),
                                output = emptyList(), // Return empty list instead of actual image
                                meta = jsonResponse.optJSONObject("meta"),
                                nsfwContentDetected = true
                            )
                        }
                        
                        return@withContext TextToImageResponse(
                            status = status,
                            generationTime = jsonResponse.optDouble("generationTime"),
                            id = jsonResponse.optString("id"),
                            output = output,
                            meta = jsonResponse.optJSONObject("meta"),
                            nsfwContentDetected = false
                        )
                    }
                    "processing" -> {
                        // For async processing, we might need to poll for results
                        val id = jsonResponse.optString("id")
                        Log.d("ModelsLabText2Img", "Image is processing with ID: $id")
                        
                        // Poll for results
                        return@withContext pollForResults(id)
                    }
                    "error", "failed" -> {
                        val errorMessage = jsonResponse.optString("message", "Unknown error occurred")
                        Log.e("ModelsLabText2Img", "API Error: $errorMessage")
                        return@withContext TextToImageResponse(
                            status = "error",
                            error = errorMessage
                        )
                    }
                    else -> {
                        Log.e("ModelsLabText2Img", "Unknown status: $status")
                        return@withContext TextToImageResponse(
                            status = "error",
                            error = "Unknown status: $status"
                        )
                    }
                }
            } else {
                val errorMessage = "HTTP Error ${response.code}: ${response.message}"
                Log.e("ModelsLabText2Img", errorMessage)
                return@withContext TextToImageResponse(
                    status = "error",
                    error = errorMessage
                )
            }
        } catch (e: Exception) {
            Log.e("ModelsLabText2Img", "Exception during image generation", e)
            return@withContext TextToImageResponse(
                status = "error",
                error = e.localizedMessage ?: "Unknown error occurred"
            )
        }
    }
    
    private suspend fun pollForResults(id: String, maxAttempts: Int = 30): TextToImageResponse = withContext(Dispatchers.IO) {
        var attempts = 0
        
        while (attempts < maxAttempts) {
            attempts++
            Log.d("ModelsLabText2Img", "Polling attempt $attempts for ID: $id")
            
            // Wait before polling
            kotlinx.coroutines.delay(2000) // 2 seconds between polls
            
            val jsonBody = JSONObject().apply {
                put("key", MODELSLAB_API_KEY)
                put("request_id", id)
            }
            
            val request = Request.Builder()
                .url("https://modelslab.com/api/v7/images/fetch")
                .addHeader("key", MODELSLAB_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                
                if (response.isSuccessful && responseBody != null) {
                    val jsonResponse = JSONObject(responseBody)
                    val status = jsonResponse.optString("status", "")
                    
                    when (status) {
                        "success" -> {
                            val output = mutableListOf<String>()
                            val outputArray = jsonResponse.optJSONArray("output")
                            if (outputArray != null) {
                                for (i in 0 until outputArray.length()) {
                                    output.add(outputArray.getString(i))
                                }
                            }
                            
                            // Check for NSFW content detection in the response
                            val nsfwDetected = jsonResponse.optBoolean("nsfw_content_detected", false) ||
                                              jsonResponse.optString("nsfw_filter", "").equals("triggered", true) ||
                                              jsonResponse.optJSONObject("meta")?.optBoolean("nsfw_detected", false) ?: false
                            
                            if (nsfwDetected) {
                                Log.w("ModelsLabText2Img", "NSFW content detected in polling result - returning blank image")
                                // Return empty output list when NSFW is detected to prevent showing the image
                                return@withContext TextToImageResponse(
                                    status = status,
                                    generationTime = jsonResponse.optDouble("generationTime"),
                                    id = id,
                                    output = emptyList(), // Return empty list instead of actual image
                                    meta = jsonResponse.optJSONObject("meta"),
                                    nsfwContentDetected = true
                                )
                            }
                            
                            return@withContext TextToImageResponse(
                                status = status,
                                generationTime = jsonResponse.optDouble("generationTime"),
                                id = id,
                                output = output,
                                meta = jsonResponse.optJSONObject("meta"),
                                nsfwContentDetected = false
                            )
                        }
                        "processing" -> {
                            // Continue polling
                            Log.d("ModelsLabText2Img", "Still processing...")
                        }
                        else -> {
                            val errorMessage = jsonResponse.optString("message", "Polling failed")
                            return@withContext TextToImageResponse(
                                status = "error",
                                error = errorMessage
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ModelsLabText2Img", "Error during polling", e)
            }
        }
        
        return@withContext TextToImageResponse(
            status = "error",
            error = "Timeout: Image generation took too long"
        )
    }
    
    companion object {
        // Available models from ModelsLab
        const val MODEL_EPIC_REALISM = "epic-realism-rc1"
        const val MODEL_SDXL = "sdxl"
        const val MODEL_FLUX = "flux"
        const val MODEL_REALISTIC_VISION = "realistic-vision-v5"
        const val MODEL_DREAMSHAPER = "dreamshaper-8"
        const val MODEL_ANYTHING_V5 = "anything-v5"
        const val MODEL_NANO_BANANA = "nano-banana"
//const val MODEL_NANO_BANANA = "imagen-4.0-ultra"
        
        // Common schedulers
        const val SCHEDULER_DPM_SOLVER = "DPMSolverSinglestepScheduler"
        const val SCHEDULER_DPM_MULTISTEP = "DPMSolverMultistepScheduler"
        const val SCHEDULER_DDPM = "DDPMScheduler"
        const val SCHEDULER_EULER_A = "EulerAncestralDiscreteScheduler"
        const val SCHEDULER_EULER = "EulerDiscreteScheduler"
    }
}
