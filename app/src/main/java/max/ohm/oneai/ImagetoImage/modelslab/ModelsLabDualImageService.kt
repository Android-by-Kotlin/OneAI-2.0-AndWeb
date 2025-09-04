package max.ohm.oneai.imagetoimage.modelslab

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

/**
 * Service for ModelsLab image-to-image API using nano-banana model
 * Supports both single image processing and dual image combining.
 * 
 * For single image processing:
 * - Use generateSingleImage() method or
 * - Use generateDualImage() with initImage2 = null
 * 
 * For dual image combining (original functionality):
 * - Use generateDualImage() with both initImage and initImage2
 * 
 * Based on the curl example:
 * curl -X POST "https://modelslab.com/api/v7/images/image-to-image" \
 *   -H "key: API_KEY" \
 *   -H "Content-Type: application/json" \
 *   -d '{
 *     "key": "<API_KEY>",
 *     "prompt": "transform this image",
 *     "model_id": "nano-banana",
 *     "init_image": "https://...",
 *     "init_image_2": "https://..." // Optional for dual image mode
 *   }'
 */
class ModelsLabDualImageService {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(120, TimeUnit.SECONDS) // Longer timeout for image processing
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build()
    
    data class DualImageToImageRequest(
        val prompt: String,
        val initImage: String, // First input image (URL or base64)
        val initImage2: String? = null, // Second input image (URL or base64) - optional
        val modelId: String = "nano-banana",
        val negativePrompt: String = "",
        val numInferenceSteps: Int = 31,
        val strength: Double = 0.8, // How much to transform the input images
        val scheduler: String = "DPMSolverSinglestepScheduler",
        val guidanceScale: Double = 7.5,
        val width: Int = 768,
        val height: Int = 1024,
        val samples: Int = 1,
        val seed: Int = 0,
        val safetyChecker: Boolean = true,
        val enhancePrompt: Boolean = false,
        val useKarrasSigmas: Boolean = true,
        val clipSkip: Int = 1,
        val tomesd: Boolean = true,
        val base64: Boolean = false
    )
    
    data class DualImageToImageResponse(
        val status: String,
        val generationTime: Double? = null,
        val id: String? = null,
        val output: List<String>? = null,
        val meta: JSONObject? = null,
        val error: String? = null,
        val nsfwContentDetected: Boolean = false
    )
    
    suspend fun generateDualImage(request: DualImageToImageRequest): DualImageToImageResponse = withContext(Dispatchers.IO) {
        try {
            Log.d("ModelsLabDualImage", "Starting dual image-to-image generation with model: ${request.modelId}")
            
            val jsonBody = JSONObject().apply {
                put("key", MODELSLAB_API_KEY)
                put("prompt", request.prompt)
                put("negative_prompt", request.negativePrompt)
                put("model_id", request.modelId)
                put("init_image", request.initImage)
                // Only include second image if provided
                request.initImage2?.let { secondImage ->
                    put("init_image_2", secondImage)
                }
                put("num_inference_steps", request.numInferenceSteps.toString())
                put("strength", request.strength.toString())
                put("scheduler", request.scheduler)
                put("guidance_scale", request.guidanceScale)
                put("width", request.width.toString())
                put("height", request.height.toString())
                put("samples", request.samples.toString())
                put("seed", request.seed.toString())
                put("safety_checker", if (request.safetyChecker) "yes" else "no")
                put("enhance_prompt", request.enhancePrompt)
                put("use_karras_sigmas", if (request.useKarrasSigmas) "yes" else "no")
                put("clip_skip", request.clipSkip)
                put("tomesd", if (request.tomesd) "yes" else "no")
                put("base64", request.base64)
                put("instant_response", "no")
                put("full_url", "no")
                put("temp", "no")
            }
            
            Log.d("ModelsLabDualImage", "Request JSON: ${jsonBody.toString(2)}")
            
            val httpRequest = Request.Builder()
                .url("https://modelslab.com/api/v7/images/image-to-image")
                .addHeader("key", MODELSLAB_API_KEY)
                .addHeader("Content-Type", "application/json")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaTypeOrNull()))
                .build()
            
            val response = client.newCall(httpRequest).execute()
            val responseBody = response.body?.string()
            
            Log.d("ModelsLabDualImage", "Response code: ${response.code}")
            Log.d("ModelsLabDualImage", "Response body: $responseBody")
            
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
                        
                        // Check for NSFW content detection
                        val nsfwDetected = jsonResponse.optBoolean("nsfw_content_detected", false) ||
                                          jsonResponse.optString("nsfw_filter", "").equals("triggered", true) ||
                                          jsonResponse.optJSONObject("meta")?.optBoolean("nsfw_detected", false) ?: false
                        
                        if (nsfwDetected) {
                            Log.w("ModelsLabDualImage", "NSFW content detected - returning blank image")
                            return@withContext DualImageToImageResponse(
                                status = status,
                                generationTime = jsonResponse.optDouble("generationTime"),
                                id = jsonResponse.optString("id"),
                                output = emptyList(),
                                meta = jsonResponse.optJSONObject("meta"),
                                nsfwContentDetected = true
                            )
                        }
                        
                        return@withContext DualImageToImageResponse(
                            status = status,
                            generationTime = jsonResponse.optDouble("generationTime"),
                            id = jsonResponse.optString("id"),
                            output = output,
                            meta = jsonResponse.optJSONObject("meta"),
                            nsfwContentDetected = false
                        )
                    }
                    "processing" -> {
                        // For async processing, poll for results
                        val id = jsonResponse.optString("id")
                        Log.d("ModelsLabDualImage", "Dual image is processing with ID: $id")
                        
                        return@withContext pollForResults(id)
                    }
                    "error", "failed" -> {
                        val errorMessage = jsonResponse.optString("message", "Unknown error occurred")
                        Log.e("ModelsLabDualImage", "API Error: $errorMessage")
                        return@withContext DualImageToImageResponse(
                            status = "error",
                            error = errorMessage
                        )
                    }
                    else -> {
                        Log.e("ModelsLabDualImage", "Unknown status: $status")
                        return@withContext DualImageToImageResponse(
                            status = "error",
                            error = "Unknown status: $status"
                        )
                    }
                }
            } else {
                val errorMessage = "HTTP Error ${response.code}: ${response.message}"
                Log.e("ModelsLabDualImage", errorMessage)
                return@withContext DualImageToImageResponse(
                    status = "error",
                    error = errorMessage
                )
            }
        } catch (e: Exception) {
            Log.e("ModelsLabDualImage", "Exception during dual image generation", e)
            return@withContext DualImageToImageResponse(
                status = "error",
                error = e.localizedMessage ?: "Unknown error occurred"
            )
        }
    }
    
    private suspend fun pollForResults(id: String, maxAttempts: Int = 30): DualImageToImageResponse = withContext(Dispatchers.IO) {
        var attempts = 0
        
        while (attempts < maxAttempts) {
            attempts++
            Log.d("ModelsLabDualImage", "Polling attempt $attempts for dual image ID: $id")
            
            // Wait before polling
            kotlinx.coroutines.delay(3000) // 3 seconds between polls for image processing
            
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
                            
                            // Check for NSFW content detection
                            val nsfwDetected = jsonResponse.optBoolean("nsfw_content_detected", false) ||
                                              jsonResponse.optString("nsfw_filter", "").equals("triggered", true) ||
                                              jsonResponse.optJSONObject("meta")?.optBoolean("nsfw_detected", false) ?: false
                            
                            if (nsfwDetected) {
                                Log.w("ModelsLabDualImage", "NSFW content detected in polling result")
                                return@withContext DualImageToImageResponse(
                                    status = status,
                                    generationTime = jsonResponse.optDouble("generationTime"),
                                    id = id,
                                    output = emptyList(),
                                    meta = jsonResponse.optJSONObject("meta"),
                                    nsfwContentDetected = true
                                )
                            }
                            
                            return@withContext DualImageToImageResponse(
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
                            Log.d("ModelsLabDualImage", "Dual image still processing...")
                        }
                        else -> {
                            val errorMessage = jsonResponse.optString("message", "Dual image polling failed")
                            return@withContext DualImageToImageResponse(
                                status = "error",
                                error = errorMessage
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("ModelsLabDualImage", "Error during dual image polling", e)
            }
        }
        
        return@withContext DualImageToImageResponse(
            status = "error",
            error = "Timeout: Dual image generation took too long"
        )
    }
    
    /**
     * Convenience method for single image-to-image generation
     * Uses only one input image with the nano-banana model
     */
    suspend fun generateSingleImage(
        prompt: String,
        initImage: String,
        negativePrompt: String = "",
        strength: Double = 0.8,
        guidanceScale: Double = 7.5,
        width: Int = 768,
        height: Int = 1024,
        enhancePrompt: Boolean = false
    ): DualImageToImageResponse {
        val request = DualImageToImageRequest(
            prompt = prompt,
            initImage = initImage,
            initImage2 = null, // No second image for single image processing
            negativePrompt = negativePrompt,
            strength = strength,
            guidanceScale = guidanceScale,
            width = width,
            height = height,
            enhancePrompt = enhancePrompt
        )
        
        Log.d("ModelsLabDualImage", "Processing single image with nano-banana model")
        return generateDualImage(request)
    }
    
    companion object {
        const val MODEL_NANO_BANANA = "nano-banana"
        
        // Common schedulers for image-to-image
        const val SCHEDULER_DPM_SOLVER = "DPMSolverSinglestepScheduler"
        const val SCHEDULER_DPM_MULTISTEP = "DPMSolverMultistepScheduler"
        const val SCHEDULER_EULER_A = "EulerAncestralDiscreteScheduler"
        const val SCHEDULER_EULER = "EulerDiscreteScheduler"
    }
}
