package max.ohm.oneai.videogeneration

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import max.ohm.oneai.videogeneration.network.VideoApiClient
import max.ohm.oneai.videogeneration.network.VideoGenerationRequest
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.*
import org.json.JSONObject
import android.os.Environment
import android.util.Base64
import retrofit2.Response
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit
import max.ohm.oneai.a4f.A4FClient
import max.ohm.oneai.videogeneration.VideoGenApiKey
import max.ohm.oneai.videogeneration.VideoThumbnailGenerator
import android.net.Uri
import max.ohm.oneai.videogeneration.modelslab.ModelsLabApiClient
import max.ohm.oneai.videogeneration.modelslab.ModelsLabVideoRequest
import max.ohm.oneai.videogeneration.modelslab.ModelsLabImageToVideoRequest
import max.ohm.oneai.videogeneration.modelslab.ModelsLabTextToVideoRequest
import max.ohm.oneai.videogeneration.modelslab.SeedanceFetchRequest

data class VideoGenerationState(
    val isLoading: Boolean = false,
    val videoUrl: String? = null,
    val error: String? = null,
    val generationId: String? = null,
    val currentPrompt: String? = null,
    val currentModel: String? = null
)

class NewVideoGenerationViewModel(application: Application) : AndroidViewModel(application) {
    private val _state = MutableStateFlow(VideoGenerationState())
    val state: StateFlow<VideoGenerationState> = _state
    
    private val videoHistoryStore = VideoHistoryDataStore(application.applicationContext)

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        .build()

    fun generateVideo(prompt: String, model: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(
                isLoading = true, 
                error = null, 
                videoUrl = null,
                currentPrompt = prompt,
                currentModel = model
            )
            
            // Check if it's the original API model
            if (model == "T2V-01-Director") {
                // Use original API for MiniMax
                generateWithOriginalAPI(prompt)
                return@launch
            } else if (model == "cogvideox") {
                // Use ModelsLab API for CogVideoX
                generateWithModelsLabAPI(prompt)
                return@launch
            } else if (model == "seedance-i2v") {
                // Use ModelsLab API for Seedance I2V (Image to Video)
                generateWithSeedanceI2V(prompt)
                return@launch
            } else if (model == "seedance-t2v") {
                // Use ModelsLab API for Seedance T2V (Text to Video)
                generateWithSeedanceT2V(prompt)
                return@launch
            }
            
            // Otherwise use A4F API
            try {
                val response = withContext(Dispatchers.IO) {
                    val json = JSONObject()
                    json.put("model", model)
                    json.put("prompt", prompt)
                    json.put("ratio", "16:9")
                    json.put("quality", "480p")
                    json.put("duration", 4)
                    
                    android.util.Log.d("VideoGen", "Request JSON: ${json.toString()}")
                    android.util.Log.d("VideoGen", "Model: $model")
                    android.util.Log.d("VideoGen", "URL: ${A4FClient.A4F_BASE_URL}video/generations")

                    val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())

                    val request = Request.Builder()
                        .url("${A4FClient.A4F_BASE_URL}video/generations") // Ensure this uses A4F endpoint
                        .addHeader("Authorization", "Bearer ${A4FClient.A4F_API_KEY}")
                        .post(body)
                        .build()

                    client.newCall(request).execute()
                }

                if (response.isSuccessful) {
                    val responseBodyString = response.body?.string() ?: ""
                    android.util.Log.d("VideoGen", "Success response: $responseBodyString")
                    
                    // Try to parse as JSON first
                    try {
                        val responseBody = JSONObject(responseBodyString)
                        
                        // Check if response contains data array with b64_json
                        if (responseBody.has("data")) {
                            val dataArray = responseBody.getJSONArray("data")
                            if (dataArray.length() > 0) {
                                val firstData = dataArray.getJSONObject(0)
                                if (firstData.has("b64_json")) {
                                    // Video is base64 encoded
                                    val base64Video = firstData.getString("b64_json")
                                    android.util.Log.d("VideoGen", "Received base64 video")
                                    
                                    // Save base64 video to file and get local path
                                    val videoPath = saveBase64VideoToFile(base64Video)
                                    if (videoPath != null) {
                                        // Save to video history
                                        saveVideoToHistory(prompt, videoPath, model)
                                        _state.value = _state.value.copy(
                                            isLoading = false,
                                            videoUrl = videoPath
                                        )
                                    } else {
                                        _state.value = _state.value.copy(
                                            isLoading = false,
                                            error = "Failed to save video"
                                        )
                                    }
                                    return@launch
                                }
                            }
                        }
                        
                        // Check for generation ID (async generation)
                        if (responseBody.has("id")) {
                            val generationId = responseBody.getString("id")
                            android.util.Log.d("VideoGen", "Generation ID received: $generationId")
                            _state.value = _state.value.copy(generationId = generationId)
                            pollForVideoCompletion(generationId)
                        } else if (responseBody.has("url")) {
                            // Direct URL in JSON
                            val videoUrl = responseBody.getString("url")
                            android.util.Log.d("VideoGen", "Video URL from JSON: $videoUrl")
                            // Save to video history
                            saveVideoToHistory(prompt, videoUrl, model)
                            _state.value = _state.value.copy(
                                isLoading = false,
                                videoUrl = videoUrl
                            )
                        } else {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "Unexpected response format"
                            )
                        }
                    } catch (e: Exception) {
                        // Not JSON, treat as plain string URL
                        val videoUrl = responseBodyString.trim().removeSurrounding("\"")
                        if (videoUrl.startsWith("http")) {
                            android.util.Log.d("VideoGen", "Video URL as string: $videoUrl")
                            // Save to video history
                            saveVideoToHistory(prompt, videoUrl, model)
                            _state.value = _state.value.copy(
                                isLoading = false,
                                videoUrl = videoUrl
                            )
                        } else {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "Invalid response: $responseBodyString"
                            )
                        }
                    }
                } else {
                    val errorBody = response.body?.string()
                    android.util.Log.e("VideoGen", "Error response: $errorBody")
                    android.util.Log.e("VideoGen", "Response code: ${response.code}")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to generate video: ${response.code} - ${errorBody?.take(100) ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }

    private fun pollForVideoCompletion(generationId: String) {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 30 // Poll for up to 5 minutes (30 * 10 seconds)

            while (attempts < maxAttempts) {
                delay(10000) // Wait 10 seconds between polls
                
                try {
                    val response = withContext(Dispatchers.IO) {
                        val request = Request.Builder()
                            .url("${A4FClient.A4F_BASE_URL}video/generations/$generationId")
                            .addHeader("Authorization", "Bearer ${A4FClient.A4F_API_KEY}")
                            .get()
                            .build()

                        client.newCall(request).execute()
                    }

                    if (response.isSuccessful) {
                        val responseBody = response.body?.string()
                        val jsonResponse = JSONObject(responseBody ?: "{}")
                        
                        if (jsonResponse.has("status")) {
                            when (jsonResponse.getString("status")) {
                                "completed" -> {
                                    val videoUrl = jsonResponse.getString("url")
                                    val currentState = _state.value
                                    // Save to video history using stored prompt and model
                                    if (currentState.currentPrompt != null && currentState.currentModel != null) {
                                        saveVideoToHistory(currentState.currentPrompt, videoUrl, currentState.currentModel)
                                    }
                                    _state.value = _state.value.copy(
                                        isLoading = false,
                                        videoUrl = videoUrl
                                    )
                                    return@launch
                                }
                                "failed" -> {
                                    _state.value = _state.value.copy(
                                        isLoading = false,
                                        error = "Video generation failed"
                                    )
                                    return@launch
                                }
                                // Continue polling for "processing" status
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Continue polling even if there's an error
                }
                
                attempts++
            }

            // Timeout
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Video generation timed out"
            )
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
    
    fun clearVideo() {
        _state.value = _state.value.copy(videoUrl = null, error = null)
    }
    
    private suspend fun saveBase64VideoToFile(base64Video: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                // Decode base64 to bytes
                val videoBytes = Base64.decode(base64Video, Base64.DEFAULT)
                
                // Create a unique filename
                val fileName = "generated_video_${System.currentTimeMillis()}.mp4"
                
                // Create file in external files directory (no permissions needed)
                val videoFile = File(android.os.Environment.getExternalStorageDirectory(), "Download/$fileName")
                
                // Write bytes to file
                FileOutputStream(videoFile).use { fos ->
                    fos.write(videoBytes)
                }
                
                android.util.Log.d("VideoGen", "Saved video to: ${videoFile.absolutePath}")
                
                // Return the file path
                videoFile.absolutePath
            } catch (e: Exception) {
                android.util.Log.e("VideoGen", "Error saving base64 video: ${e.message}", e)
                null
            }
        }
    }
    
    private fun generateWithOriginalAPI(prompt: String) {
        viewModelScope.launch {
            try {
                // Check if we have valid credentials for the original API
                if (!VideoGenApiKey.validateCredentials()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Invalid API credentials for MiniMax. Please check your API key."
                    )
                    return@launch
                }
                
                val authorization = "Bearer ${VideoGenApiKey.API_KEY}"
                val request = VideoGenerationRequest(model = "T2V-01-Director", prompt = prompt)
                
                android.util.Log.d("VideoGen", "Sending MiniMax video generation request")
                
                val response = withContext(Dispatchers.IO) {
                    VideoApiClient.apiService.generateVideo(authorization, request)
                }
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody?.taskId != null) {
                        android.util.Log.d("VideoGen", "Task ID received: ${responseBody.taskId}")
                        _state.value = _state.value.copy(generationId = responseBody.taskId)
                        pollOriginalAPIStatus(responseBody.taskId)
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Task ID not received from MiniMax API"
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "MiniMax API error: ${response.code()} - $errorBody"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoGen", "MiniMax error: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "MiniMax error: ${e.message}"
                )
            }
        }
    }
    
    private fun pollOriginalAPIStatus(taskId: String) {
        viewModelScope.launch {
            val authorization = "Bearer ${VideoGenApiKey.API_KEY}"
            var attempts = 0
            val maxAttempts = 60
            
            while (attempts < maxAttempts && _state.value.isLoading) {
                delay(5000) // Poll every 5 seconds
                
                try {
                    val response = withContext(Dispatchers.IO) {
                        VideoApiClient.apiService.queryVideoGenerationStatus(authorization, taskId)
                    }
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        android.util.Log.d("VideoGen", "Poll status: ${responseBody?.status}")
                        
                        if (responseBody?.status == "Success" && !responseBody.fileId.isNullOrEmpty()) {
                            retrieveOriginalAPIVideo(responseBody.fileId)
                            return@launch
                        } else if (responseBody?.status == "Failed") {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "Video generation failed"
                            )
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoGen", "Polling error: ${e.message}")
                }
                
                attempts++
            }
            
            if (attempts >= maxAttempts) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Video generation timed out"
                )
            }
        }
    }
    
    private fun retrieveOriginalAPIVideo(fileId: String) {
        viewModelScope.launch {
            try {
                val authorization = "Bearer ${VideoGenApiKey.API_KEY}"
                val groupId = VideoGenApiKey.GROUP_ID
                
                val response = withContext(Dispatchers.IO) {
                    VideoApiClient.apiService.retrieveVideoFile(authorization, groupId, fileId)
                }
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    var rawUrl = responseBody?.file?.downloadUrl
                    
                    android.util.Log.d("VideoGen", "Raw download URL: $rawUrl, backup URL: ${responseBody?.file?.backupDownloadUrl}")
                    
                    if (rawUrl.isNullOrEmpty()) {
                        android.util.Log.d("VideoGen", "Primary URL empty, trying backup")
                        rawUrl = responseBody?.file?.backupDownloadUrl
                    }
                    
                    if (!rawUrl.isNullOrEmpty()) {
                        // Process the URL
                        var decodedUrl = Uri.decode(rawUrl)
                        
                        // Add https:// if missing
                        if (!decodedUrl.startsWith("http")) {
                            decodedUrl = "https://$decodedUrl"
                            android.util.Log.d("VideoGen", "Added https:// prefix")
                        }
                        
                        // Remove query parameters if present
                        if (decodedUrl.contains("?")) {
                            val urlParts = decodedUrl.split("?")
                            if (urlParts.isNotEmpty()) {
                                decodedUrl = urlParts[0]
                                android.util.Log.d("VideoGen", "Removed query parameters")
                            }
                        }
                        
                        android.util.Log.d("VideoGen", "Final video URL: $decodedUrl")
                        // Save to video history using stored prompt and model
                        val currentState = _state.value
                        if (currentState.currentPrompt != null && currentState.currentModel != null) {
                            saveVideoToHistory(currentState.currentPrompt, decodedUrl, currentState.currentModel)
                        }
                        _state.value = _state.value.copy(
                            isLoading = false,
                            videoUrl = decodedUrl
                        )
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Video URL not found in response"
                        )
                    }
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Failed to retrieve video file"
                    )
                }
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error retrieving video: ${e.message}"
                )
            }
        }
    }
    
    private suspend fun saveVideoToHistory(prompt: String, videoUrl: String, model: String) {
        try {
            // Generate thumbnail for the video
            val videoId = java.util.UUID.randomUUID().toString()
            val thumbnailPath = VideoThumbnailGenerator.generateThumbnail(
                context = getApplication(),
                videoUrl = videoUrl,
                videoId = videoId
            )
            
            // Save video to history with thumbnail - always show "Wan 2.1" as the provider
            videoHistoryStore.addVideo(
                prompt = prompt,
                videoUrl = videoUrl,
                model = "Wan 2.1", // Display consistent provider name instead of technical model
                thumbnailPath = thumbnailPath
            )
            
            android.util.Log.d("VideoGen", "Video saved to history with thumbnail: $thumbnailPath")
        } catch (e: Exception) {
            android.util.Log.e("VideoGen", "Error saving video to history", e)
            // Save without thumbnail if thumbnail generation fails
            videoHistoryStore.addVideo(prompt, videoUrl, "Wan 2.1")
        }
    }
    
    private fun generateWithModelsLabAPI(prompt: String) {
        viewModelScope.launch {
            try {
                val request = ModelsLabVideoRequest(
                    key = ModelsLabApiClient.API_KEY,
                    prompt = prompt
                )
                
                android.util.Log.d("VideoGen", "Sending ModelsLab video generation request")
                
                val response = withContext(Dispatchers.IO) {
                    ModelsLabApiClient.apiService.generateVideo(request)
                }
                
                if (response.isSuccessful) {
                    val responseBody = response.body()!!
                    android.util.Log.d("VideoGen", "ModelsLab response status: ${responseBody.status}")
                    
                    if (responseBody.status.equals("success", true)) {
                        // Video is ready immediately
                        if (!responseBody.outputUrls.isNullOrEmpty()) {
                            val videoUrl = responseBody.outputUrls.first()
                            android.util.Log.d("VideoGen", "ModelsLab video URL: $videoUrl")
                            
                            // Save to history
                            saveVideoToHistory(prompt, videoUrl, "CogVideoX")
                            
                            _state.value = _state.value.copy(
                                isLoading = false,
                                videoUrl = videoUrl
                            )
                        } else {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "No video URL in ModelsLab response"
                            )
                        }
                    } else if (responseBody.status.equals("processing", true) && responseBody.id != null) {
                        // Need to poll for completion
                        android.util.Log.d("VideoGen", "ModelsLab processing, request ID: ${responseBody.id}")
                        pollModelsLabStatus(responseBody.id)
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "ModelsLab error: ${responseBody.message ?: responseBody.error ?: "Unknown error"}"
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "ModelsLab API error: ${response.code()} - $errorBody"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoGen", "ModelsLab error: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "ModelsLab error: ${e.message}"
                )
            }
        }
    }
    
    private fun pollModelsLabStatus(requestId: Long) {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 30 // Poll for up to 5 minutes
            
            while (attempts < maxAttempts && _state.value.isLoading) {
                delay(10000) // Wait 10 seconds between polls
                
                try {
                    val response = withContext(Dispatchers.IO) {
                        ModelsLabApiClient.apiService.checkGenerationStatus(
                            apiKey = ModelsLabApiClient.API_KEY,
                            requestId = requestId
                        )
                    }
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body()!!
                        android.util.Log.d("VideoGen", "ModelsLab poll status: ${responseBody.status}")
                        
                        if (responseBody.status.equals("success", true)) {
                            if (!responseBody.outputUrls.isNullOrEmpty()) {
                                val videoUrl = responseBody.outputUrls.first()
                                val currentState = _state.value
                                
                                // Save to history
                                if (currentState.currentPrompt != null) {
                                    saveVideoToHistory(currentState.currentPrompt, videoUrl, "CogVideoX")
                                }
                                
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    videoUrl = videoUrl
                                )
                                return@launch
                            }
                        } else if (responseBody.status.equals("failed", true)) {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "ModelsLab video generation failed"
                            )
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoGen", "ModelsLab polling error: ${e.message}")
                }
                
                attempts++
            }
            
            if (attempts >= maxAttempts) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "ModelsLab video generation timed out"
                )
            }
        }
    }
    
    private fun generateWithSeedanceI2V(prompt: String) {
        viewModelScope.launch {
            try {
                // For now, use a default image URL. In the future, this could be user-provided
                val defaultImageUrl = "https://pub-3626123a908346a7a8be8d9295f44e26.r2.dev/livewire-tmp/4yTocqHjDm1KgbSBrVA09hMNuWJpOY-metaZG93bmxvYWRfNC5wbmc=-.png"
                
                val request = ModelsLabImageToVideoRequest(
                    key = ModelsLabApiClient.API_KEY,
                    initImage = defaultImageUrl,
                    prompt = prompt
                )
                
                android.util.Log.d("VideoGen", "Sending Seedance I2V generation request")
                
                val response = withContext(Dispatchers.IO) {
                    ModelsLabApiClient.apiService.generateImageToVideo(request)
                }
                
                if (response.isSuccessful) {
                    val responseBody = response.body()!!
                    android.util.Log.d("VideoGen", "Seedance I2V response status: ${responseBody.status}")
                    
                    if (responseBody.status.equals("success", true)) {
                        // Video is ready immediately
                        // Use helper method to get video URLs from either 'output' or 'outputUrls' field
                        val videoUrls = responseBody.getVideoUrls()
                        if (!videoUrls.isNullOrEmpty()) {
                            val videoUrl = videoUrls.first()
                            android.util.Log.d("VideoGen", "Seedance I2V video URL: $videoUrl")
                            
                            // Save to history
                            saveVideoToHistory(prompt, videoUrl, "Seedance I2V")
                            
                            _state.value = _state.value.copy(
                                isLoading = false,
                                videoUrl = videoUrl
                            )
                        } else {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "No video URL in Seedance I2V response"
                            )
                        }
                    } else if (responseBody.status.equals("processing", true) && responseBody.id != null) {
                        // Need to poll for completion
                        android.util.Log.d("VideoGen", "Seedance I2V processing, request ID: ${responseBody.id}")
                        pollSeedanceI2VStatus(responseBody.id)
                    } else {
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Seedance I2V error: ${responseBody.message ?: responseBody.error ?: "Unknown error"}"
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Seedance I2V API error: ${response.code()} - $errorBody"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoGen", "Seedance I2V error: ${e.message}", e)
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Seedance I2V error: ${e.message}"
                )
            }
        }
    }
    
    private fun pollSeedanceI2VStatus(requestId: Long) {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 30 // Poll for up to 5 minutes
            
            while (attempts < maxAttempts && _state.value.isLoading) {
                delay(10000) // Wait 10 seconds between polls
                
                try {
                    val response = withContext(Dispatchers.IO) {
                        val fetchRequest = SeedanceFetchRequest(
                            key = ModelsLabApiClient.API_KEY
                        )
                        ModelsLabApiClient.apiService.checkSeedanceGenerationStatus(
                            requestId = requestId,
                            body = fetchRequest
                        )
                    }
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body()!!
                        android.util.Log.d("VideoGen", "Seedance I2V poll status: ${responseBody.status}")
                        
                        if (responseBody.status.equals("success", true)) {
                            // Use helper method to get video URLs from either 'output' or 'outputUrls' field
                            val videoUrls = responseBody.getVideoUrls()
                            if (!videoUrls.isNullOrEmpty()) {
                                val videoUrl = videoUrls.first()
                                val currentState = _state.value
                                
                                // Save to history
                                if (currentState.currentPrompt != null) {
                                    saveVideoToHistory(currentState.currentPrompt, videoUrl, "Seedance I2V")
                                }
                                
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    videoUrl = videoUrl
                                )
                                return@launch
                            }
                        } else if (responseBody.status.equals("failed", true)) {
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "Seedance I2V video generation failed"
                            )
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoGen", "Seedance I2V polling error: ${e.message}")
                }
                
                attempts++
            }
            
            if (attempts >= maxAttempts) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Seedance I2V video generation timed out"
                )
            }
        }
    }
    
    private fun generateWithSeedanceT2V(prompt: String) {
        viewModelScope.launch {
            try {
                val request = ModelsLabTextToVideoRequest(
                    key = ModelsLabApiClient.API_KEY,
                    prompt = prompt
                )
                
                android.util.Log.d("VideoGen", "Sending Seedance T2V generation request with prompt: $prompt")
                android.util.Log.d("VideoGen", "Request details: model=${request.modelId}, resolution=${request.resolution}, fps=${request.fps}")
                
                val response = withContext(Dispatchers.IO) {
                    ModelsLabApiClient.apiService.generateTextToVideo(request)
                }
                
                android.util.Log.d("VideoGen", "Seedance T2V response code: ${response.code()}")
                
                if (response.isSuccessful) {
                    val responseBody = response.body()
                    if (responseBody == null) {
                        android.util.Log.e("VideoGen", "Seedance T2V response body is null")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Empty response from Seedance T2V API"
                        )
                        return@launch
                    }
                    
                    android.util.Log.d("VideoGen", "Seedance T2V response - status: ${responseBody.status}, id: ${responseBody.id}, outputUrls: ${responseBody.outputUrls}, fetchResult: ${responseBody.fetchResultUrl}")
                    
                    // Check for immediate success with video URL
                    // Use helper method to get video URLs from either 'output' or 'outputUrls' field
                    val videoUrls = responseBody.getVideoUrls()
                    if (!videoUrls.isNullOrEmpty()) {
                        val videoUrl = videoUrls.first()
                        android.util.Log.d("VideoGen", "Seedance T2V video ready immediately: $videoUrl")
                        
                        // Save to history
                        saveVideoToHistory(prompt, videoUrl, "Seedance T2V")
                        
                        _state.value = _state.value.copy(
                            isLoading = false,
                            videoUrl = videoUrl
                        )
                    }
                    // Check if we have a fetch_result URL to poll
                    else if (!responseBody.fetchResultUrl.isNullOrEmpty()) {
                        android.util.Log.d("VideoGen", "Seedance T2V has fetch_result URL: ${responseBody.fetchResultUrl}")
                        pollSeedanceT2VWithFetchUrl(responseBody.fetchResultUrl, prompt)
                    }
                    // If status indicates processing but no polling mechanism available
                    else if (responseBody.status.equals("processing", true) || responseBody.status.equals("pending", true)) {
                        android.util.Log.w("VideoGen", "Seedance T2V is processing but no polling mechanism available")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Video is processing. Please try again later as the API doesn't support status checking."
                        )
                    }
                    else {
                        val errorMessage = responseBody.message ?: responseBody.error ?: responseBody.status ?: "Unknown error - no video URL returned"
                        android.util.Log.e("VideoGen", "Seedance T2V unexpected response: $errorMessage")
                        _state.value = _state.value.copy(
                            isLoading = false,
                            error = "Seedance T2V: $errorMessage"
                        )
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    android.util.Log.e("VideoGen", "Seedance T2V API error - Code: ${response.code()}, Body: $errorBody")
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Seedance T2V API error: ${response.code()} - ${errorBody?.take(200) ?: "Unknown error"}"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoGen", "Seedance T2V exception: ${e.message}", e)
                e.printStackTrace()
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Seedance T2V error: ${e.message}"
                )
            }
        }
    }
    
    private fun pollSeedanceT2VStatus(requestId: Long, prompt: String) {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 60 // Poll for up to 10 minutes (increased from 5)
            val delayMillis = 10000L // 10 seconds between polls
            
            android.util.Log.d("VideoGen", "Starting Seedance T2V polling for request ID: $requestId")
            
            while (attempts < maxAttempts && _state.value.isLoading) {
                attempts++
                android.util.Log.d("VideoGen", "Seedance T2V poll attempt $attempts/$maxAttempts")
                
                // Wait before polling (except for first attempt)
                if (attempts > 1) {
                    delay(delayMillis)
                }
                
                try {
                    val response = withContext(Dispatchers.IO) {
                        val fetchRequest = SeedanceFetchRequest(
                            key = ModelsLabApiClient.API_KEY
                        )
                        ModelsLabApiClient.apiService.checkSeedanceGenerationStatus(
                            requestId = requestId,
                            body = fetchRequest
                        )
                    }
                    
                    android.util.Log.d("VideoGen", "Seedance T2V poll response code: ${response.code()}")
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body()
                        if (responseBody == null) {
                            android.util.Log.e("VideoGen", "Seedance T2V poll response body is null")
                            continue
                        }
                        
                        android.util.Log.d("VideoGen", "Seedance T2V poll - status: ${responseBody.status}, outputUrls: ${responseBody.outputUrls}, eta: ${responseBody.eta}")
                        
                        when {
                            responseBody.status.equals("success", true) -> {
                                // Use helper method to get video URLs from either 'output' or 'outputUrls' field
                                val videoUrls = responseBody.getVideoUrls()
                                if (!videoUrls.isNullOrEmpty()) {
                                    val videoUrl = videoUrls.first()
                                    android.util.Log.d("VideoGen", "Seedance T2V video ready: $videoUrl")
                                    
                                    // Save to history
                                    saveVideoToHistory(prompt, videoUrl, "Seedance T2V")
                                    
                                    _state.value = _state.value.copy(
                                        isLoading = false,
                                        videoUrl = videoUrl
                                    )
                                    return@launch
                                }
                            }
                            responseBody.status.equals("completed", true) -> {
                                // Use helper method to get video URLs from either 'output' or 'outputUrls' field
                                val videoUrls = responseBody.getVideoUrls()
                                if (!videoUrls.isNullOrEmpty()) {
                                    val videoUrl = videoUrls.first()
                                    android.util.Log.d("VideoGen", "Seedance T2V video completed: $videoUrl")
                                    
                                    // Save to history
                                    saveVideoToHistory(prompt, videoUrl, "Seedance T2V")
                                    
                                    _state.value = _state.value.copy(
                                        isLoading = false,
                                        videoUrl = videoUrl
                                    )
                                    return@launch
                                }
                            }
                            responseBody.status.equals("failed", true) || responseBody.status.equals("error", true) -> {
                                val errorMsg = responseBody.error ?: responseBody.message ?: "Video generation failed"
                                android.util.Log.e("VideoGen", "Seedance T2V generation failed: $errorMsg")
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = "Seedance T2V generation failed: $errorMsg"
                                )
                                return@launch
                            }
                            responseBody.status.equals("processing", true) || responseBody.status.equals("pending", true) || responseBody.status.equals("queued", true) -> {
                                android.util.Log.d("VideoGen", "Seedance T2V still processing, ETA: ${responseBody.eta} seconds")
                                // Continue polling
                            }
                            else -> {
                                android.util.Log.w("VideoGen", "Seedance T2V unknown status: ${responseBody.status}")
                                // Continue polling for unknown status
                            }
                        }
                    } else {
                        val errorBody = response.errorBody()?.string()
                        android.util.Log.e("VideoGen", "Seedance T2V poll error - Code: ${response.code()}, Body: $errorBody")
                        
                        // Don't fail immediately on polling errors, retry
                        if (response.code() == 404) {
                            // Request ID not found, likely expired
                            _state.value = _state.value.copy(
                                isLoading = false,
                                error = "Seedance T2V request expired or not found"
                            )
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoGen", "Seedance T2V polling exception: ${e.message}", e)
                    // Don't fail immediately on network errors, retry
                }
            }
            
            // Timeout reached
            if (_state.value.isLoading) {
                android.util.Log.e("VideoGen", "Seedance T2V generation timed out after $attempts attempts")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Seedance T2V video generation timed out after ${(attempts * delayMillis) / 60000} minutes"
                )
            }
        }
    }
    
    private fun pollSeedanceT2VWithFetchUrl(fetchUrl: String, prompt: String) {
        viewModelScope.launch {
            var attempts = 0
            val maxAttempts = 60 // Poll for up to 10 minutes
            val delayMillis = 10000L // 10 seconds between polls
            
            android.util.Log.d("VideoGen", "Starting Seedance T2V polling with fetch URL: $fetchUrl")
            
            while (attempts < maxAttempts && _state.value.isLoading) {
                attempts++
                android.util.Log.d("VideoGen", "Seedance T2V fetch poll attempt $attempts/$maxAttempts")
                
                // Wait before polling (except for first attempt)
                if (attempts > 1) {
                    delay(delayMillis)
                }
                
                try {
                    // Extract request ID from fetch URL to use the proper API endpoint
                    val requestId = fetchUrl.substringAfterLast("/").toLongOrNull()
                    if (requestId != null) {
                        // Use the proper Retrofit API service with POST method
                        val response = withContext(Dispatchers.IO) {
                            val fetchRequest = SeedanceFetchRequest(
                                key = ModelsLabApiClient.API_KEY
                            )
                            ModelsLabApiClient.apiService.checkSeedanceGenerationStatus(
                                requestId = requestId,
                                body = fetchRequest
                            )
                        }
                        
                        android.util.Log.d("VideoGen", "Seedance T2V fetch response code: ${response.code()}")
                        
                        if (response.isSuccessful) {
                            val responseBody = response.body()
                            android.util.Log.d("VideoGen", "Seedance T2V fetch response: $responseBody")
                            
                            if (responseBody != null) {
                                when {
                                    responseBody.status.equals("success", true) || responseBody.status.equals("completed", true) -> {
                                        // Use helper method to get video URLs from either 'output' or 'outputUrls' field
                                        val videoUrls = responseBody.getVideoUrls()
                                        if (!videoUrls.isNullOrEmpty()) {
                                            val videoUrl = videoUrls.first()
                                            android.util.Log.d("VideoGen", "Seedance T2V video ready via fetch: $videoUrl")
                                            
                                            // Save to history
                                            saveVideoToHistory(prompt, videoUrl, "Seedance T2V")
                                            
                                            _state.value = _state.value.copy(
                                                isLoading = false,
                                                videoUrl = videoUrl
                                            )
                                            return@launch
                                        }
                                    }
                                    responseBody.status.equals("failed", true) || responseBody.status.equals("error", true) -> {
                                        val errorMsg = responseBody.error ?: responseBody.message ?: "Video generation failed"
                                        android.util.Log.e("VideoGen", "Seedance T2V generation failed: $errorMsg")
                                        _state.value = _state.value.copy(
                                            isLoading = false,
                                            error = "Seedance T2V generation failed: $errorMsg"
                                        )
                                        return@launch
                                    }
                                    responseBody.status.equals("processing", true) || responseBody.status.equals("pending", true) || responseBody.status.equals("queued", true) -> {
                                        android.util.Log.d("VideoGen", "Seedance T2V still processing, ETA: ${responseBody.eta} seconds")
                                        // Continue polling
                                    }
                                    else -> {
                                        android.util.Log.w("VideoGen", "Seedance T2V unknown status via fetch: ${responseBody.status}")
                                        // Continue polling for unknown status
                                    }
                                }
                            }
                        } else {
                            android.util.Log.e("VideoGen", "Seedance T2V fetch error - Code: ${response.code()}")
                            
                            // Don't fail immediately on polling errors, retry
                            if (response.code() == 404) {
                                // Request ID not found, likely expired
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = "Seedance T2V request expired or not found"
                                )
                                return@launch
                            }
                        }
                    } else {
                        // Fallback: create POST request manually with API key in body
                        val response = withContext(Dispatchers.IO) {
                            val json = JSONObject()
                            json.put("key", ModelsLabApiClient.API_KEY)
                            val body = json.toString().toRequestBody("application/json; charset=utf-8".toMediaTypeOrNull())
                            
                            val request = Request.Builder()
                                .url(fetchUrl)
                                .post(body)
                                .addHeader("Content-Type", "application/json")
                                .build()
                            
                            client.newCall(request).execute()
                        }
                        
                        android.util.Log.d("VideoGen", "Seedance T2V fetch response code: ${response.code}")
                        
                        if (response.isSuccessful) {
                            val responseBody = response.body?.string()
                            android.util.Log.d("VideoGen", "Seedance T2V fetch response: $responseBody")
                        
                            // Try to parse as JSON
                            try {
                                val json = JSONObject(responseBody ?: "{}")
                                val status = json.optString("status", "")
                            
                            when {
                                status.equals("success", true) || status.equals("completed", true) -> {
                                    // Check for output URL in various possible fields
                                    val videoUrl = json.optString("output", "")
                                        .takeIf { it.isNotEmpty() }
                                        ?: json.optJSONArray("output")?.optString(0, "")
                                        ?: json.optString("url", "")
                                        ?: json.optString("video_url", "")
                                    
                                    if (!videoUrl.isNullOrEmpty()) {
                                        android.util.Log.d("VideoGen", "Seedance T2V video ready via fetch: $videoUrl")
                                        
                                        // Save to history
                                        saveVideoToHistory(prompt, videoUrl, "Seedance T2V")
                                        
                                        _state.value = _state.value.copy(
                                            isLoading = false,
                                            videoUrl = videoUrl
                                        )
                                        return@launch
                                    }
                                }
                                status.equals("failed", true) || status.equals("error", true) -> {
                                    val errorMsg = json.optString("error", json.optString("message", "Video generation failed"))
                                    android.util.Log.e("VideoGen", "Seedance T2V generation failed: $errorMsg")
                                    _state.value = _state.value.copy(
                                        isLoading = false,
                                        error = "Seedance T2V generation failed: $errorMsg"
                                    )
                                    return@launch
                                }
                                status.equals("processing", true) || status.equals("pending", true) || status.equals("queued", true) -> {
                                    val eta = json.optDouble("eta", 0.0)
                                    android.util.Log.d("VideoGen", "Seedance T2V still processing, ETA: $eta seconds")
                                    // Continue polling
                                }
                                else -> {
                                    android.util.Log.w("VideoGen", "Seedance T2V unknown status via fetch: $status")
                                    // Continue polling for unknown status
                                }
                            }
                            } catch (e: Exception) {
                                android.util.Log.e("VideoGen", "Error parsing fetch response: ${e.message}")
                            }
                        } else {
                            android.util.Log.e("VideoGen", "Seedance T2V fetch error - Code: ${response.code}")
                        
                            // Don't fail immediately on polling errors, retry
                            if (response.code == 404) {
                                // URL not found, likely expired
                                _state.value = _state.value.copy(
                                    isLoading = false,
                                    error = "Seedance T2V request expired or not found"
                                )
                                return@launch
                            }
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoGen", "Seedance T2V fetch polling exception: ${e.message}", e)
                    // Don't fail immediately on network errors, retry
                }
            }
            
            // Timeout reached
            if (_state.value.isLoading) {
                android.util.Log.e("VideoGen", "Seedance T2V generation timed out after $attempts attempts")
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Seedance T2V video generation timed out after ${(attempts * delayMillis) / 60000} minutes"
                )
            }
        }
    }
}
