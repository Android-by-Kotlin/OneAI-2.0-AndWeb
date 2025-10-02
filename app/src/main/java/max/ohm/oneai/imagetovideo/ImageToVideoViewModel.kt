package max.ohm.oneai.imagetovideo

import android.app.Application
import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import max.ohm.oneai.videogeneration.VideoHistoryDataStore
import max.ohm.oneai.videogeneration.VideoThumbnailGenerator
import max.ohm.oneai.videogeneration.modelslab.ModelsLabApiClient
import max.ohm.oneai.videogeneration.modelslab.ModelsLabImageToVideoRequest
import max.ohm.oneai.videogeneration.modelslab.SeedanceFetchRequest
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * ViewModel for Image-to-Video generation
 */
class ImageToVideoViewModel(application: Application) : AndroidViewModel(application) {
    
    private val TAG = "ImageToVideoViewModel"
    private val videoHistoryStore = VideoHistoryDataStore(application.applicationContext)
    
    private val _state = MutableStateFlow<ImageToVideoState>(ImageToVideoState.Idle)
    val state: StateFlow<ImageToVideoState> = _state.asStateFlow()
    
    private val _referenceImageUri = MutableStateFlow<Uri?>(null)
    val referenceImageUri: StateFlow<Uri?> = _referenceImageUri.asStateFlow()
    
    private val _prompt = MutableStateFlow("")
    val prompt: StateFlow<String> = _prompt.asStateFlow()
    
    private val _history = MutableStateFlow<List<ImageToVideoHistoryItem>>(emptyList())
    val history: StateFlow<List<ImageToVideoHistoryItem>> = _history.asStateFlow()
    
    private val _isPolling = MutableStateFlow(false)
    val isPolling: StateFlow<Boolean> = _isPolling.asStateFlow()
    
    /**
     * Set the reference image URI
     */
    fun setReferenceImage(uri: Uri?) {
        _referenceImageUri.value = uri
    }
    
    /**
     * Update the prompt text
     */
    fun updatePrompt(text: String) {
        _prompt.value = text
    }
    
    /**
     * Generate video from image and prompt
     */
    fun generateVideo(imageUrl: String, prompt: String) {
        if (prompt.isBlank()) {
            _state.value = ImageToVideoState.Error("Please enter a prompt")
            return
        }
        
        if (imageUrl.isBlank()) {
            _state.value = ImageToVideoState.Error("Please select a reference image or provide an image URL")
            return
        }
        
        viewModelScope.launch {
            try {
                _state.value = ImageToVideoState.Loading
                
                // Validate and process image URL
                val processedImageUrl = when {
                    imageUrl.startsWith("http://") || imageUrl.startsWith("https://") -> {
                        // Valid URL, use as is
                        imageUrl
                    }
                    imageUrl.startsWith("content://") -> {
                        // Local content URI - needs to be uploaded or converted
                        _state.value = ImageToVideoState.Error("Please use an image URL instead of local file. Upload your image to a service like imgur.com first.")
                        return@launch
                    }
                    else -> {
                        // Assume it's a URL without protocol
                        if (imageUrl.contains(".") && !imageUrl.contains(" ")) {
                            "https://$imageUrl"
                        } else {
                            _state.value = ImageToVideoState.Error("Invalid image URL. Please provide a valid HTTP/HTTPS URL.")
                            return@launch
                        }
                    }
                }
                
                val request = ModelsLabImageToVideoRequest(
                    key = ModelsLabApiClient.API_KEY,
                    initImage = processedImageUrl,
                    prompt = prompt
                )
                
                Log.d(TAG, "Sending Seedance I2V generation request")
                Log.d(TAG, "Image URL: $processedImageUrl")
                Log.d(TAG, "Prompt: $prompt")
                
                val response = withContext(Dispatchers.IO) {
                    ModelsLabApiClient.apiService.generateImageToVideo(request)
                }
                
                if (response.isSuccessful) {
                    val responseBody = response.body()!!
                    Log.d(TAG, "Seedance I2V response status: ${responseBody.status}")
                    Log.d(TAG, "Seedance I2V response body: output=${responseBody.output}, outputUrls=${responseBody.outputUrls}, id=${responseBody.id}, message=${responseBody.message}")
                    
                    if (responseBody.status.equals("success", true)) {
                        // Video is ready immediately - use helper method to get URLs
                        val videoUrls = responseBody.getVideoUrls()
                        if (!videoUrls.isNullOrEmpty()) {
                            val videoUrl = videoUrls.first()
                            Log.d(TAG, "Seedance I2V video URL: $videoUrl")
                            
                            // Save to history
                            saveVideoToHistory(imageUrl, prompt, videoUrl, responseBody.generationTime)
                            
                            _state.value = ImageToVideoState.Success(videoUrl, responseBody.generationTime)
                        } else {
                            Log.e(TAG, "No video URL in response. Full response: $responseBody")
                            _state.value = ImageToVideoState.Error("Failed to generate video. Please try again later.")
                        }
                    } else if (responseBody.status.equals("processing", true) && responseBody.id != null) {
                        // Need to poll for completion
                        Log.d(TAG, "Seedance I2V processing, request ID: ${responseBody.id}")
                        _state.value = ImageToVideoState.Processing
                        pollForResult(responseBody.id, imageUrl, prompt)
                    } else {
                        _state.value = ImageToVideoState.Error(responseBody.message ?: responseBody.error ?: "Unknown error occurred")
                    }
                } else {
                    val errorBody = response.errorBody()?.string()
                    Log.e(TAG, "Error response: $errorBody")
                    val errorMessage = when (response.code()) {
                        400 -> "Invalid request. Please check your image URL and prompt."
                        401 -> "Authentication failed. Please check API key."
                        403 -> "Access forbidden. Please check API permissions."
                        404 -> "API endpoint not found. Please check the service."
                        422 -> "Invalid image URL or parameters. Please use a valid HTTPS image URL."
                        429 -> "Too many requests. Please wait a moment and try again."
                        500, 502, 503, 504 -> "Server error. Please try again later."
                        else -> "Failed to generate video: ${response.code()}"
                    }
                    _state.value = ImageToVideoState.Error(errorMessage)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Exception generating video", e)
                val errorMessage = when {
                    e.message?.contains("timeout", ignoreCase = true) == true -> 
                        "Request timed out. Please check your internet connection and try again."
                    e.message?.contains("UnknownHostException", ignoreCase = true) == true ||
                    e.message?.contains("Unable to resolve host", ignoreCase = true) == true -> 
                        "Unable to connect to server. Please check your internet connection."
                    e.message?.contains("SocketException", ignoreCase = true) == true -> 
                        "Connection error. Please check your internet connection and try again."
                    else -> "Failed to generate video. Please try again."
                }
                _state.value = ImageToVideoState.Error(errorMessage)
            }
        }
    }
    
    /**
     * Poll for video generation result
     */
    private fun pollForResult(requestId: Long, imageUrl: String, prompt: String) {
        viewModelScope.launch {
            _isPolling.value = true
            var attempts = 0
            val maxAttempts = 30 // Poll for up to 5 minutes (30 * 10 seconds)
            
            while (attempts < maxAttempts && _state.value is ImageToVideoState.Processing) {
                delay(10000) // Wait 10 seconds between polls
                attempts++
                
                try {
                    Log.d(TAG, "Polling attempt $attempts for request $requestId")
                    
                    val response = withContext(Dispatchers.IO) {
                        ModelsLabApiClient.apiService.checkSeedanceGenerationStatus(
                            requestId = requestId,
                            body = SeedanceFetchRequest(key = ModelsLabApiClient.API_KEY)
                        )
                    }
                    
                    if (response.isSuccessful) {
                        val responseBody = response.body()!!
                        Log.d(TAG, "Seedance I2V poll status: ${responseBody.status}")
                        Log.d(TAG, "Seedance I2V poll response: output=${responseBody.output}, outputUrls=${responseBody.outputUrls}")
                        
                        if (responseBody.status.equals("success", true)) {
                            val videoUrls = responseBody.getVideoUrls()
                            if (!videoUrls.isNullOrEmpty()) {
                                val videoUrl = videoUrls.first()
                                Log.d(TAG, "Video generated successfully: $videoUrl")
                                
                                // Save to history
                                saveVideoToHistory(imageUrl, prompt, videoUrl, responseBody.generationTime)
                                
                                _state.value = ImageToVideoState.Success(videoUrl, responseBody.generationTime)
                                _isPolling.value = false
                                return@launch
                            } else {
                                Log.e(TAG, "Success status but no video URL in polling response")
                            }
                        } else if (responseBody.status.equals("failed", true)) {
                            _state.value = ImageToVideoState.Error(responseBody.message ?: "Video generation failed")
                            _isPolling.value = false
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Seedance I2V polling error: ${e.message}")
                }
            }
            
            if (attempts >= maxAttempts) {
                _state.value = ImageToVideoState.Error("Video generation timed out")
                _isPolling.value = false
            }
        }
    }
    
    /**
     * Save generated video to history
     */
    private suspend fun saveVideoToHistory(imageUrl: String, prompt: String, videoUrl: String, generationTime: Double?) {
        try {
            // Generate thumbnail for the video
            val videoId = UUID.randomUUID().toString()
            val thumbnailPath = VideoThumbnailGenerator.generateThumbnail(
                context = getApplication(),
                videoUrl = videoUrl,
                videoId = videoId
            )
            
            // Save video to history with thumbnail
            videoHistoryStore.addVideo(
                prompt = prompt,
                videoUrl = videoUrl,
                model = "Image to Video",
                thumbnailPath = thumbnailPath
            )
            
            // Also add to local history
            val historyItem = ImageToVideoHistoryItem(
                id = videoId,
                initImageUrl = imageUrl,
                prompt = prompt,
                videoUrl = videoUrl,
                timestamp = System.currentTimeMillis(),
                generationTime = generationTime
            )
            _history.value = listOf(historyItem) + _history.value
            
            Log.d(TAG, "Video saved to history with thumbnail: $thumbnailPath")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving video to history", e)
            // Save without thumbnail if thumbnail generation fails
            videoHistoryStore.addVideo(prompt, videoUrl, "Image to Video")
        }
    }
    
    /**
     * Reset state
     */
    fun resetState() {
        _state.value = ImageToVideoState.Idle
    }
    
    /**
     * Clear reference image
     */
    fun clearReferenceImage() {
        _referenceImageUri.value = null
    }
    
    /**
     * Clear prompt
     */
    fun clearPrompt() {
        _prompt.value = ""
    }
}

/**
 * Factory for creating ImageToVideoViewModel with Application parameter
 */
class ImageToVideoViewModelFactory(private val application: Application) : androidx.lifecycle.ViewModelProvider.Factory {
    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ImageToVideoViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ImageToVideoViewModel(application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

