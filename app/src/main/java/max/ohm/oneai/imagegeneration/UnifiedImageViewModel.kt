package max.ohm.oneai.imagegeneration

import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import max.ohm.oneai.a4f.A4FClient.A4F_API_KEY
import max.ohm.oneai.fluxproimagegen.network.FluxApiClient
import max.ohm.oneai.fluxproimagegen.network.FluxImageGenerationRequest
import max.ohm.oneai.network.ApiClient
import max.ohm.oneai.network.ImageGenerationRequest
import max.ohm.oneai.utils.ContentFilter
import max.ohm.oneai.imagegeneration.modelslab.ModelsLabTextToImageService
import max.ohm.oneai.imagetoimage.modelslabapikey.MODELSLAB_API_KEY
import max.ohm.oneai.imagegeneration.NEBIUS_API_KEY

class UnifiedImageViewModel : ViewModel() {
    
    private val modelsLabService = ModelsLabTextToImageService()

    var prompt by mutableStateOf(TextFieldValue(""))
        private set

    var generatedImageData by mutableStateOf<ByteArray?>(null) // For base64 images (flux.1-schnell)
        private set

    var imageUrl by mutableStateOf<String?>(null) // For URL images (flux.1.1-pro)
        private set

    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
    
    var nsfwContentDetected by mutableStateOf(false)
        private set

    // Function to convert technical error messages to user-friendly ones
    private fun getUserFriendlyErrorMessage(technicalError: String, modelName: String): String {
        return when {
            technicalError.contains("500") || technicalError.contains("503") || 
            technicalError.contains("Service Unavailable") || technicalError.contains("Internal Server Error") -> {
                "$modelName is experiencing heavy traffic. Please try another model or wait a moment."
            }
            technicalError.contains("429") || technicalError.contains("Too Many Requests") -> {
                "Too many requests to $modelName. Please wait a moment before trying again."
            }
            technicalError.contains("401") || technicalError.contains("Unauthorized") -> {
                "Authentication failed. Please check your API key configuration."
            }
            technicalError.contains("400") || technicalError.contains("Bad Request") -> {
                "Invalid request to $modelName. Please try a different prompt."
            }
            technicalError.contains("404") || technicalError.contains("Not Found") -> {
                "$modelName is currently unavailable. Please try another model."
            }
            technicalError.contains("timeout") || technicalError.contains("Timeout") -> {
                "$modelName is taking too long to respond. Please try again."
            }
            technicalError.contains("Network") || technicalError.contains("network") -> {
                "Network connection issue. Please check your internet connection."
            }
            else -> {
                "$modelName is currently unavailable. Please try another model."
            }
        }
    }











    var selectedModel by mutableStateOf("google/imagen-4") // Default model - ImageGen-4 Premium












    private val _elapsedTimeInSeconds = MutableStateFlow(0L)
    val elapsedTimeInSeconds: StateFlow<Long> = _elapsedTimeInSeconds

    private val _totalGenerationTimeInSeconds = MutableStateFlow<Long?>(null)
    val totalGenerationTimeInSeconds: StateFlow<Long?> = _totalGenerationTimeInSeconds

    private var timerJob: Job? = null
    private var generationStartTime: Long = 0L

    // Initialize the ViewModel with default model
    init {
        // Ensure the default model is properly set
        selectedModel = "google/imagen-4"
    }

    fun updatePrompt(newPrompt: TextFieldValue) {
        prompt = newPrompt
    }

    fun updateSelectedModel(model: String) {
        selectedModel = model
        // Clear previous results and timer when switching models
        generatedImageData = null
        imageUrl = null
        errorMessage = null
        nsfwContentDetected = false
        _elapsedTimeInSeconds.value = 0L
        _totalGenerationTimeInSeconds.value = null
        timerJob?.cancel()
    }

    fun clearErrorMessage() {
        errorMessage = null
    }

    private fun startTimer() {
        _elapsedTimeInSeconds.value = 0L
        _totalGenerationTimeInSeconds.value = null
        generationStartTime = System.currentTimeMillis()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isLoading) { // Loop while isLoading is true
                _elapsedTimeInSeconds.value = (System.currentTimeMillis() - generationStartTime) / 1000
                delay(1000)
            }
        }
    }

    fun generateImage() {
        if (prompt.text.isBlank()) {
            errorMessage = "Please enter a prompt"
            return
        }

        // Check for adult/NSFW content
        if (ContentFilter.containsAdultContent(prompt.text)) {
            errorMessage = ContentFilter.getWarningMessage()
            Log.w("ContentFilter", "Adult content detected in prompt: ${ContentFilter.sanitizeForLogging(prompt.text)}")
            
            // Replace the prompt with a safe alternative
            val safePrompt = ContentFilter.getSafePrompt(prompt.text)
            prompt = TextFieldValue(safePrompt)
            
            // Show warning but continue with safe prompt
            viewModelScope.launch {
                delay(3000) // Show warning for 3 seconds
                errorMessage = null
                // Proceed with safe prompt
                generateImageInternal(safePrompt)
            }
            return
        }

        // Log the current model for debugging
        Log.d("ImageGeneration", "Selected model: $selectedModel")

        // Ensure we have a valid model selected
        if (selectedModel.isBlank()) {
            // Set to default model if somehow blank
            selectedModel = "google/imagen-4"
            Log.d("ImageGeneration", "Model was blank, set to default: $selectedModel")
        }
        
        generateImageInternal(prompt.text)
    }
    
    private fun generateImageInternal(promptText: String) {

        isLoading = true
        generatedImageData = null
        imageUrl = null
        errorMessage = null
        nsfwContentDetected = false
        startTimer() // Start the timer

        viewModelScope.launch {
            try {
                // Add safety negative prompts to all requests
                val safePrompt = promptText
                val negativePrompts = ContentFilter.getNegativePrompts()
                
                // Log the model being used for the API call
                Log.d("ImageGeneration", "Using model for API call: $selectedModel")
                Log.d("ImageGeneration", "Safe prompt: ${ContentFilter.sanitizeForLogging(safePrompt)}")
                
                when (selectedModel) {
                    "modelslab/epic-realism" -> {
                        if (MODELSLAB_API_KEY.isBlank() || MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY") {
                            errorMessage = "Please configure ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        
                        Log.d("ModelsLabGeneration", "Using ModelsLab API Key: ${MODELSLAB_API_KEY.take(10)}...")
                        
                        // Comprehensive negative prompt for ModelsLab to prevent inappropriate content
                        val modelsLabNegativePrompt = "(nsfw, naked, nude, show breast, deformed iris, deformed pupils, semi-realistic, cgi, 3d, render, sketch, cartoon, drawing, anime, mutated hands and fingers:1.4), (deformed, distorted, disfigured:1.3), poorly drawn, bad anatomy, wrong anatomy, extra limb, missing limb, floating limbs, disconnected limbs, mutation, mutated, ugly, disgusting, amputation, $negativePrompts"
                        
                        val request = ModelsLabTextToImageService.TextToImageRequest(
                            prompt = safePrompt,
                            negativePrompt = modelsLabNegativePrompt,
                           // modelId = "epic-realism-rc1",
                          //  modelId = "coffeemix-sd1-5-americano-season-1",
                           // modelId="real-dream-classic-sd1-5-1-sd1-5",
                            modelId= "serenity-v2-1-safetensors",
                           // modelId= "flux",
                            scheduler = "DPMSolverSinglestepScheduler",
                            width = 768,
                            height = 1024,
                           // height = 1408,
                            guidanceScale = 7.5,
                            numInferenceSteps = 31,
                            steps = 21,
                            samples = 1,
                            seed = 0,
                            safetyChecker = true,  // Enable safety checker to detect NSFW
                            enhancePrompt = true,
                            useKarrasSigmas = true,
                            clipSkip = 1,
                            tomesd = true,
                            base64 = false  // Changed to false to get URL responses
                        )
                        
                        val response = modelsLabService.generateImage(request)
                        
                        // Set NSFW detection flag first
                        nsfwContentDetected = response.nsfwContentDetected
                        
                        if (response.status == "success") {
                            if (nsfwContentDetected) {
                                // NSFW content detected - no image data will be available
                                Log.w("ModelsLabResponse", "NSFW content detected - no image will be displayed")
                                // Clear any previous image data
                                imageUrl = null
                                generatedImageData = null
                                // Set an error message to inform the user
                               // errorMessage = "NSFW content detected. Image generation blocked for safety."
                                errorMessage = "Please provide safe prompt."
                            } else if (!response.output.isNullOrEmpty()) {
                                // Normal image processing when no NSFW content
                                val imageOutput = response.output!![0]
                                Log.d("ModelsLabResponse", "Image output: ${imageOutput.take(100)}...") // Log first 100 chars
                                
                                // Check if it's a URL or base64 data
                                if (imageOutput.startsWith("http://") || imageOutput.startsWith("https://")) {
                                    // It's a URL, use imageUrl instead of imageData
                                    imageUrl = imageOutput
                                    generatedImageData = null
                                } else if (imageOutput.startsWith("data:image")) {
                                    // Extract base64 data from data URL
                                    val base64Data = imageOutput.substringAfter(",")
                                    try {
                                        generatedImageData = Base64.decode(base64Data, Base64.DEFAULT)
                                        imageUrl = null
                                    } catch (e: Exception) {
                                        Log.e("ModelsLabResponse", "Failed to decode base64 data URL", e)
                                        errorMessage = "Failed to decode image data"
                                    }
                                } else {
                                    // Assume it's direct base64 string
                                    try {
                                        generatedImageData = Base64.decode(imageOutput, Base64.DEFAULT)
                                        imageUrl = null
                                    } catch (e: Exception) {
                                        Log.e("ModelsLabResponse", "Failed to decode direct base64", e)
                                        // If base64 decode fails, try treating it as URL
                                        imageUrl = imageOutput
                                        generatedImageData = null
                                    }
                                }
                            } else {
                                // Success but no output (should not happen normally)
                                errorMessage = "Image generation completed but no image was returned"
                            }
                        } else {
                            val error = response.error ?: "Failed to generate image"
                            Log.e("ModelsLabResponse", "API Error: $error")
                            errorMessage = getUserFriendlyErrorMessage(error, "ModelsLab Epic Realism")
                        }
                        isLoading = false
                    }



                    "modelslab/nano-banana" -> {
                        if (MODELSLAB_API_KEY.isBlank() || MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY") {
                            errorMessage = "Please configure ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        
                        Log.d("ModelsLabGeneration", "Using ModelsLab API Key: ${MODELSLAB_API_KEY.take(10)}...")
                        
                        // Comprehensive negative prompt for ModelsLab to prevent inappropriate content
                        val modelsLabNegativePrompt = "(nsfw, naked, nude, show breast, deformed iris, deformed pupils, semi-realistic, cgi, 3d, render, sketch, cartoon, drawing, anime, mutated hands and fingers:1.4), (deformed, distorted, disfigured:1.3), poorly drawn, bad anatomy, wrong anatomy, extra limb, missing limb, floating limbs, disconnected limbs, mutation, mutated, ugly, disgusting, amputation, $negativePrompts"
                        
                        val request = ModelsLabTextToImageService.TextToImageRequest(
                            prompt = safePrompt,
                            negativePrompt = modelsLabNegativePrompt,
                            modelId = ModelsLabTextToImageService.MODEL_NANO_BANANA,
                            scheduler = "DPMSolverSinglestepScheduler",
                            width = 768,
                            //width = 704,
                            height = 1024,
//                            height = 1408,
                            //height = 1472,
                            guidanceScale = 7.5,
                            numInferenceSteps = 31,
                            steps = 21,
                            samples = 1,
                            seed = 0,
                            safetyChecker = true,  // Enable safety checker to detect NSFW
                            enhancePrompt = true,
                            useKarrasSigmas = true,
                            clipSkip = 1,
                            tomesd = true,
                            base64 = false  // Changed to false to get URL responses
                        )
                        
                        val response = modelsLabService.generateImage(request)
                        
                        // Set NSFW detection flag first
                        nsfwContentDetected = response.nsfwContentDetected
                        
                        if (response.status == "success") {
                            if (nsfwContentDetected) {
                                // NSFW content detected - no image data will be available
                                Log.w("ModelsLabResponse", "NSFW content detected - no image will be displayed")
                                // Clear any previous image data
                                imageUrl = null
                                generatedImageData = null
                                // Set an error message to inform the user
                                errorMessage = "Please provide safe prompt."
                            } else if (!response.output.isNullOrEmpty()) {
                                // Normal image processing when no NSFW content
                                val imageOutput = response.output!![0]
                                Log.d("ModelsLabResponse", "Image output: ${imageOutput.take(100)}...") // Log first 100 chars
                                
                                // Check if it's a URL or base64 data
                                if (imageOutput.startsWith("http://") || imageOutput.startsWith("https://")) {
                                    // It's a URL, use imageUrl instead of imageData
                                    imageUrl = imageOutput
                                    generatedImageData = null
                                } else if (imageOutput.startsWith("data:image")) {
                                    // Extract base64 data from data URL
                                    val base64Data = imageOutput.substringAfter(",")
                                    try {
                                        generatedImageData = Base64.decode(base64Data, Base64.DEFAULT)
                                        imageUrl = null
                                    } catch (e: Exception) {
                                        Log.e("ModelsLabResponse", "Failed to decode base64 data URL", e)
                                        errorMessage = "Failed to decode image data"
                                    }
                                } else {
                                    // Assume it's direct base64 string
                                    try {
                                        generatedImageData = Base64.decode(imageOutput, Base64.DEFAULT)
                                        imageUrl = null
                                    } catch (e: Exception) {
                                        Log.e("ModelsLabResponse", "Failed to decode direct base64", e)
                                        // If base64 decode fails, try treating it as URL
                                        imageUrl = imageOutput
                                        generatedImageData = null
                                    }
                                }
                            } else {
                                // Success but no output (should not happen normally)
                                errorMessage = "Image generation completed but no image was returned"
                            }
                        } else {
                            val error = response.error ?: "Failed to generate image"
                            Log.e("ModelsLabResponse", "API Error: $error")
                            errorMessage = getUserFriendlyErrorMessage(error, "ModelsLab Nano Banana")
                        }
                        isLoading = false
                    }
                    "flux.1-schnell" -> {
                        if (NEBIUS_API_KEY == "YOUR_NEBIUS_API_KEY_HERE" || NEBIUS_API_KEY.isBlank()) {
                            errorMessage = "Please set your Nebius API Key in ImageGenApiKey.kt"
                            isLoading = false
                            return@launch
                        }
                        val request = ImageGenerationRequest(prompt = safePrompt)
                        val response = ApiClient.instance.generateImage("Bearer $NEBIUS_API_KEY", request)

                        if (response.isSuccessful && response.body() != null) {
                            val responseBody = response.body()!!
                            if (responseBody.data.isNotEmpty()) {
                                val base64String = responseBody.data[0].base64Json
                                generatedImageData = Base64.decode(base64String, Base64.DEFAULT)
                            } else {
                                errorMessage = "API returned no image data for flux.1-schnell."
                            }
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "Flux.1-schnell API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "Flux Schnell")
                        }
                        isLoading = false
                    }
                    "provider-6/gpt-image-1" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-6/gpt-image-1", // Use the pro model
                            prompt = "$safePrompt",
                            n = 1,
                            //size = "1024x1536"  //chatgpt app size
                            //size = "720x1280"
                           // size = "1080x2340"
                            size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "GPT Image-1 API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "GPT Image-1")
                        }
                        isLoading = false
                    }



                    "provider-4/imagen-4" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-4/imagen-4", // Use the pro model
                            prompt = "$safePrompt. Avoid: ${negativePrompts}",
                            n = 1,
                            //  size = "1024x1536"  //chatgpt app size
                           // size = "1024x1792"
                            //size = "720x1280"
                            // size = "1080x2340"
                            size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "ImageGen-4 API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "ImageGen-4")
                        }
                        isLoading = false
                    }





                    "google/imagen-4" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-4/imagen-4", // Use the pro model
                            prompt = "$safePrompt",
                            n = 1,
                            //  size = "1024x1536"  //chatgpt app size
                             size = "1024x1792"
                            //size = "720x1280"
                            // size = "1080x2340"
                            //size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "ImageGen-4 API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "ImageGen-4")
                        }
                        isLoading = false
                    }






                    "google/imagen-3" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-4/imagen-3", // Use the pro model
                            prompt = "$safePrompt",
                            n = 1,
                            //  size = "1024x1536"  //chatgpt app size
                            size = "1024x1792"
                            //size = "720x1280"
                            // size = "1080x2340"
                            //size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "ImageGen-4 API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "ImageGen-4")
                        }
                        isLoading = false
                    }

//                    "provider-4/imagen-4" -> {
//                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
//                            errorMessage = "Please set your A4F API Key in A4FClinet"
//                            isLoading = false
//                            return@launch
//                        }
//                        val request = FluxImageGenerationRequest(
//                            model = "provider-4/imagen-4", // Use the pro model
//                            prompt = prompt.text,
//                            n = 1,
//                           // size = "1024x1536"  //chatgpt app size
//                            size = "1024x1792"
//                            //size = "720x1280"
//                            // size = "1080x2340"
//                            //size = "1024x1024"
//                        )
//                        val response = FluxApiClient.apiService.generateImage(request)
//
//                        if (response.isSuccessful) {
//                            val generatedFluxImage = response.body()?.data?.firstOrNull()
//                            imageUrl = generatedFluxImage?.url
//                        } else {
//                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
//                            errorMessage = "Flux Pro API Error: ${response.code()} - ${errorBody}"
//                        }
//                        isLoading = false
//                    }



                    "provider-3/imagen-4.0-generate-preview-06-06" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-3/imagen-4.0-generate-preview-06-06", // Use the pro model
                            prompt = "$safePrompt. Avoid: ${negativePrompts}",
                            n = 1,
                            // size = "1024x1536"  //chatgpt app size
                            size = "1024x1792"
                            //size = "720x1280"
                            // size = "1080x2340"
                            //size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "Imagen 4.0 Preview API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "Imagen 4.0 Preview")
                        }
                        isLoading = false
                    }

                    "provider-4/imagen-3" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-4/imagen-3", // Use the pro model
                            prompt = "$safePrompt. Avoid: ${negativePrompts}",
                            n = 1,
                          //  size = "1024x1536"  //chatgpt app size
                            size = "1024x1792"
                            //size = "720x1280"
                            // size = "1080x2340"
                            //size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "ImageGen-3 API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "ImageGen-3")
                        }
                        isLoading = false
                    }



                    "provider-4/qwen-image" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-4/qwen-image", // Use the pro model
                            prompt = "$safePrompt",
                            n = 1,
                            //  size = "1024x1536"  //chatgpt app size
                            size = "1024x1792"
                           // size="1080x2340"
                            //size = "720x1280"
                            // size = "1080x2340"
                           // size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "Qwen API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "Qwen")
                        }
                        isLoading = false
                    }


                    "provider-3/imagen-3.0-generate-002" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-3/imagen-3.0-generate-002", // Use the pro model
                            prompt = "$safePrompt. Avoid: ${negativePrompts}",
                            n = 1,
                            //  size = "1024x1536"  //chatgpt app size
                            size = "1024x1792"
                            //size = "720x1280"
                            // size = "1080x2340"
                            //size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "Imagen 3.0 API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "Imagen 3.0")
                        }
                        isLoading = false
                    }


                    "provider-3/FLUX.1.1-pro-ultra-raw" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-3/FLUX.1.1-pro-ultra-raw", // Use the pro model
                            prompt = "$safePrompt, safe for work, family-friendly",
                            n = 1,
                            size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "Flux Pro Ultra Raw API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "Flux Pro Raw")
                        }
                        isLoading = false
                    }
                    "provider-1/FLUX.1.1-pro" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-1/FLUX.1.1-pro", // Use the pro model
                            prompt = "$safePrompt, safe for work, family-friendly",
                            n = 1,
                            size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "Flux Pro API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "Flux Pro")
                        }
                        isLoading = false
                    }
                    "provider-3/FLUX.1.1-pro-ultra" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-3/FLUX.1.1-pro-ultra",
                            prompt = "$safePrompt, safe for work, family-friendly",
                            n = 1,
                            size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "Flux Ultra Pro API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "Flux Ultra Pro")
                        }
                        isLoading = false
                    }
                    "provider-3/dall-e-3" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-3/dall-e-3",
                            prompt = "$safePrompt, safe for work, family-friendly",
                            n = 1,
                            size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "DALL-E 3 API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "DALL-E 3")
                        }
                        isLoading = false
                    }
                    "provider-3/shuttle-3.1-aesthetic" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-3/shuttle-3.1-aesthetic",
                            prompt = "$safePrompt, safe for work, family-friendly",
                            n = 1,
                            size = "1024x1024"
                            //size = "1024x1792"     //not work
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "Shuttle 3.1 Aesthetic API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "Shuttle 3.1 Aesthetic")
                        }
                        isLoading = false
                    }
                    "provider-3/shuttle-3-diffusion" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-3/shuttle-3-diffusion",
                            prompt = "$safePrompt, safe for work, family-friendly",
                            n = 1,
                            size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "Shuttle 3 Diffusion API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "Shuttle 3 Diffusion")
                        }
                        isLoading = false
                    }
                    "provider-3/shuttle-jaguar" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-3/shuttle-jaguar",
                            prompt = "$safePrompt, safe for work, family-friendly",
                            n = 1,
                            size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "Shuttle Jaguar API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "Shuttle Jaguar")
                        }
                        isLoading = false
                    }
                    "provider-3/FLUX.1-dev" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-3/FLUX.1-dev",
                            prompt = "$safePrompt, safe for work, family-friendly",
                            n = 1,
                            size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "Flux Dev API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "Flux Dev")
                        }
                        isLoading = false
                    }
                    "provider-2/FLUX.1-kontext-max" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-2/FLUX.1-kontext-max",
                            prompt = "$safePrompt, safe for work, family-friendly",
                            n = 1,
                            size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "FLUX Kontext Max API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "FLUX Kontext Max")
                        }
                        isLoading = false
                    }
                    "provider-1/FLUX.1-kontext-pro" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key in A4FClinet"
                            isLoading = false
                            return@launch
                        }
                        val request = FluxImageGenerationRequest(
                            model = "provider-1/FLUX.1-kontext-pro",
                            prompt = "$safePrompt, safe for work, family-friendly",
                            n = 1,
                            size = "1024x1024"
                        )
                        val response = FluxApiClient.apiService.generateImage(request)

                        if (response.isSuccessful) {
                            val generatedFluxImage = response.body()?.data?.firstOrNull()
                            imageUrl = generatedFluxImage?.url
                        } else {
                            val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                            val technicalError = "FLUX Kontext Pro API Error: ${response.code()} - ${errorBody}"
                            errorMessage = getUserFriendlyErrorMessage(technicalError, "FLUX Kontext Pro")
                        }
                        isLoading = false
                    }
                    else -> {
                        // Log the invalid model for debugging
                        Log.e("ImageGeneration", "Invalid model selected: '$selectedModel'")
                        
                        // Try to use the default model as a fallback
                        if (selectedModel != "google/imagen-4") {
                            Log.d("ImageGeneration", "Falling back to google/imagen-4 model")
                            selectedModel = "google/imagen-4"
                            
                            // Retry with the default model (ImageGen-4 Premium)
                            if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                                errorMessage = "Please set your A4F API Key in A4FClient"
                                isLoading = false
                                return@launch
                            }
                            
                            val request = FluxImageGenerationRequest(
                                model = "provider-4/imagen-4",
                                prompt = "$safePrompt",
                                n = 1,
                                size = "1024x1792"
                            )
                            val response = FluxApiClient.apiService.generateImage(request)

                            if (response.isSuccessful) {
                                val generatedFluxImage = response.body()?.data?.firstOrNull()
                                imageUrl = generatedFluxImage?.url
                            } else {
                                val errorBody = response.errorBody()?.string() ?: "Unknown API error"
                                val technicalError = "ImageGen-4 API Error: ${response.code()} - ${errorBody}"
                                errorMessage = getUserFriendlyErrorMessage(technicalError, "ImageGen-4")
                            }
                            isLoading = false
                        } else {
                            errorMessage = "Invalid model selected: $selectedModel"
                            isLoading = false // Ensure loading stops if model is invalid before try-finally
                        }
                    }
                }
            } catch (e: Exception) {
                val technicalError = "Network or other error: ${e.message}"
                errorMessage = getUserFriendlyErrorMessage(technicalError, "Image generation")
                e.printStackTrace()
            } finally {
                isLoading = false // This will stop the timer loop in startTimer()
                if (generationStartTime > 0) { // Calculate total time only if generation started
                    _totalGenerationTimeInSeconds.value = (System.currentTimeMillis() - generationStartTime) / 1000
                }
                // No need to explicitly cancel timerJob here as isLoading=false handles it
                // and startTimer() cancels previous job
            }
        }
    }
}


