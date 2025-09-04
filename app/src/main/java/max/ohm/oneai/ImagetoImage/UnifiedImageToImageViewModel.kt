package max.ohm.oneai.imagetoimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.isActive
import max.ohm.oneai.a4f.A4FClient.A4F_API_KEY
import max.ohm.oneai.imagetoimage.modelslabapikey.MODELSLAB_API_KEY
import max.ohm.oneai.stabilityai.STABILITY_API_KEY
import max.ohm.oneai.stabilityai.repository.StabilityRepository
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.TimeUnit
import android.content.Context

class UnifiedImageToImageViewModel : ViewModel() {

    // Selected input image
    var selectedImage by mutableStateOf<Bitmap?>(null)
        private set

    // Generated output
    var generatedImageBitmap by mutableStateOf<Bitmap?>(null)
        private set

    var generatedImageUrl by mutableStateOf<String?>(null)
        private set

    // UI States
    var isLoading by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)

    var selectedModel by mutableStateOf("nano-banana") // Default model - Nano-Banana Dual Image
        private set

    // Prompt and parameters
    var prompt by mutableStateOf("")
    var negativePrompt by mutableStateOf("")
    var strength by mutableStateOf(0.7f)
    var guidanceScale by mutableStateOf(7.5f)
    var steps by mutableStateOf(20)
    
    // Search and Replace specific parameters
    var searchPrompt by mutableStateOf("")
    var replacePrompt by mutableStateOf("")
    
    // Replace Background and Relight specific parameters
    var backgroundReferenceUri by mutableStateOf<android.net.Uri?>(null)
        private set
    
    var styleReferenceImage by mutableStateOf<Bitmap?>(null)
        private set
    
    // Second image for dual image models (like Flux Kontext Dev Dual)
    var secondImage by mutableStateOf<Bitmap?>(null)
        private set
    
    // Target image for face swap
    var targetImage by mutableStateOf<Bitmap?>(null)
        private set
    
    // Clothing image for fashion try-on
    var clothingImage by mutableStateOf<Bitmap?>(null)
        private set
    
    // Cloth type for fashion try-on
    var clothType by mutableStateOf("upper_body")
        private set
    
    // Stability AI specific parameters
    var imageStrength by mutableStateOf(0.35f)
    var cfgScale by mutableStateOf(7)
    var controlStrength by mutableStateOf(0.7f) // For sketch-to-image
    
    // Outpaint parameters
    var outpaintLeft by mutableStateOf(0)
    var outpaintRight by mutableStateOf(0)
    var outpaintTop by mutableStateOf(0)
    var outpaintBottom by mutableStateOf(0)
    
    // Context for Stability AI
    private var context: Context? = null
    
    // History data store
    private var historyDataStore: ImageToImageHistoryDataStore? = null
    

    // Batch Processing properties
    var batchImages by mutableStateOf<List<Bitmap>>(emptyList())
        private set
    var batchProcessedImages by mutableStateOf<List<Bitmap>>(emptyList())
        private set
    var isBatchProcessing by mutableStateOf(false)
        private set
    var batchProcessingProgress by mutableStateOf(0)
        private set
    var batchProcessingTotal by mutableStateOf(0)
        private set
    var batchProcessingCurrentImage by mutableStateOf<String?>(null)
        private set

    // Timer states
    private val _elapsedTimeInSeconds = MutableStateFlow(0L)
    val elapsedTimeInSeconds: StateFlow<Long> = _elapsedTimeInSeconds

    private val _totalGenerationTimeInSeconds = MutableStateFlow<Long?>(null)
    val totalGenerationTimeInSeconds: StateFlow<Long?> = _totalGenerationTimeInSeconds

    private var timerJob: Job? = null
    private var generationStartTime: Long = 0L
    
    // Loading message for long operations
    var loadingMessage by mutableStateOf("")
        private set

    // HTTP client with extended timeout
    private val client = OkHttpClient.Builder()
        .connectTimeout(600, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(600, TimeUnit.SECONDS)
        .build()

    // Masking state variables
    var maskBitmap by mutableStateOf<Bitmap?>(null)
        private set
    var showMaskingInterface by mutableStateOf(false)
        private set
    
    // Dual image mode state variables
    var isDualImageMode by mutableStateOf(false)
    var isGhibliMode by mutableStateOf(false)
    
    // Available models
    val availableModels = listOf(
        // A4F Models (NEW)
//        "a4f-flux-kontext-dev" to "✨ A4F Flux Kontext Dev Edit",
//        "a4f-flux-kontext-pro" to "✨ A4F Flux Kontext Pro Edit",
//        "a4f-flux-kontext-max" to "✨ A4F Flux Kontext Max Edit",



        
        // Stability AI Models (Premium)
       // "stability-ai-img2img" to "🚀 Stability AI Image-to-Image",
//        "stability-ai-sketch" to "🎨 Stability AI Sketch-to-Image",
//        "stability-ai-structure" to "🏗️ Stability AI Structure Control",
//        "stability-ai-style" to "🎨 Stability AI Style Control",
//        "stability-ai-style-transfer" to "🎨 Stability AI Style Transfer",
//        "stability-ai-search-replace" to "🚀 Stability AI Search & Replace",
//        "stability-ai-search-recolor" to "🚀 Stability AI Search & Recolor",
//        "stability-ai-remove-background" to "🚀 Stability AI Remove Background",
//        "stability-ai-replace-background-relight" to "🚀 Stability AI Replace Background & Relight",
//        "stability-ai-mask-erase" to "🚀 Stability AI Mask Erase",
//        "stability-ai-inpaint" to "🚀 Stability AI Inpaint",
//        "stability-ai-outpaint" to "🚀 Stability AI Outpaint",
//        "stability-ai-upscale" to "🚀 Stability AI Conservative Upscale",
//
        // Standard Image-to-Image Models
      //  "flux-img2img" to "Flux Image-to-Image",
        "flux-kontext-pro-img2img" to "Flux Kontext Pro",
        "flux-kontext-dev-dual" to "Flux Kontext Dev",
        "nano-banana" to "🍌 Nano-Banana Dual Image",
       // "multiple-face-swap" to "🎭 Multiple Face Swap",
        "fashion-try-on" to "Fashion Try-On",
       // "stable-diffusion-img2img" to "Stable Diffusion Img2Img",
      //  "sdxl-img2img" to "SDXL Image-to-Image",
        
        // Inpainting Models
        "v51-inpainting" to "Inpainting",
     //   "object-removal" to "Object Removal",
       // "bg-remove-mask" to "Background Removal (Mask)",

        // Style Transfer Models
        "ghibli-style" to "Ghibli Studio Style",
       // "headshot-img2img" to "Headshot",
      //  "Enhance-img2img" to "Enhance",
       // "Remove-img2img" to "Remove Background",
        "Sketch Img2Img" to "ControlNet/Sketch",
//        "anime-style" to "Anime Style Transfer",
//        "oil-painting" to "Oil Painting Style",
//        "watercolor" to "Watercolor Style",
//
//        "cyberpunk-style" to "Cyberpunk Style",
//        "fantasy-art" to "Fantasy Art Style",

        // Specialized Models
//        "photo-enhance" to "Photo Enhancement",
//        "colorize" to "Black & White Colorization",
//        "super-resolution" to "Super Resolution Upscale",
//
//
//        // NEW: Batch Processing Models
//        "batch-style-transfer" to "🔄 Batch Style Transfer",
//        "batch-enhancement" to "🔄 Batch Enhancement",
//        "batch-upscale" to "🔄 Batch Upscale"
    )

    fun setContext(context: Context) {
        this.context = context
        this.historyDataStore = ImageToImageHistoryDataStore(context)
    }
    
    fun updateSelectedImage(bitmap: Bitmap?) {
        selectedImage = bitmap
        errorMessage = null
        // Reset generated images when new input is selected
        generatedImageBitmap = null
        generatedImageUrl = null
    }

    fun updateSelectedModel(model: String) {
        selectedModel = model
        // Clear previous results when switching models
        generatedImageBitmap = null
        generatedImageUrl = null
        errorMessage = null
        _elapsedTimeInSeconds.value = 0L
        _totalGenerationTimeInSeconds.value = null
        timerJob?.cancel()
    }

    fun clearErrorMessage() {
        errorMessage = null
    }
    
    fun updateImageStrength(strength: Float) {
        imageStrength = strength
    }
    
    fun updateControlStrength(strength: Float) {
        controlStrength = strength
    }
    
    fun updateCfgScale(scale: Int) {
        cfgScale = scale
    }
    
    fun updateBackgroundReferenceUri(uri: android.net.Uri?) {
        backgroundReferenceUri = uri
    }
    
    fun updateStyleReferenceImage(bitmap: Bitmap?) {
        styleReferenceImage = bitmap
    }
    
    fun updateSecondImage(bitmap: Bitmap?) {
        secondImage = bitmap
    }
    
    fun updateTargetImage(bitmap: Bitmap?) {
        targetImage = bitmap
    }
    
    fun updateClothingImage(bitmap: Bitmap?) {
        clothingImage = bitmap
    }
    
    fun updateClothType(type: String) {
        clothType = type
    }
    
    fun updateOutpaintLeft(value: Int) {
        outpaintLeft = value
    }
    
    fun updateOutpaintRight(value: Int) {
        outpaintRight = value
    }
    
    fun updateOutpaintTop(value: Int) {
        outpaintTop = value
    }
    
    fun updateOutpaintBottom(value: Int) {
        outpaintBottom = value
    }
    
    fun updateMaskBitmap(bitmap: Bitmap?) {
        maskBitmap = bitmap
    }
    
    fun toggleMaskingInterface() {
        showMaskingInterface = !showMaskingInterface
    }
    
    fun clearMask() {
        maskBitmap = null
    }
    

    // Batch Processing functions
    fun addBatchImage(bitmap: Bitmap) {
        batchImages = batchImages + bitmap
    }

    fun addBatchImages(bitmaps: List<Bitmap>) {
        batchImages = batchImages + bitmaps
    }

    fun removeBatchImage(index: Int) {
        if (index >= 0 && index < batchImages.size) {
            batchImages = batchImages.filterIndexed { i, _ -> i != index }
        }
    }

    fun clearBatchImages() {
        batchImages = emptyList()
        batchProcessedImages = emptyList()
        batchProcessingProgress = 0
        batchProcessingTotal = 0
        batchProcessingCurrentImage = null
    }

    fun processBatch() {
        if (batchImages.isEmpty()) {
            errorMessage = "Please add images to batch first"
            return
        }

        // Validate that the selected model supports batch processing
        if (!selectedModel.startsWith("batch-")) {
            errorMessage = "Selected model does not support batch processing"
            return
        }

        isBatchProcessing = true
        batchProcessingProgress = 0
        batchProcessingTotal = batchImages.size
        batchProcessedImages = emptyList()
        errorMessage = null

        viewModelScope.launch {
            try {
                val processedImages = mutableListOf<Bitmap>()
                
                for ((index, bitmap) in batchImages.withIndex()) {
                    batchProcessingProgress = index
                    batchProcessingCurrentImage = "Processing image ${index + 1}/${batchImages.size}"
                    
                    // Set the current image for processing
                    selectedImage = bitmap
                    
                    // Convert to base64 for API calls
                    val base64Image = withContext(Dispatchers.IO) {
                        val resizedBitmap = resizeBitmapIfNeeded(bitmap)
                        bitmapToBase64(resizedBitmap)
                    }
                    
                    // Process based on batch model type
                    val processedBitmap = when (selectedModel) {
                        "batch-style-transfer" -> processBatchStyleTransfer(base64Image)
                        "batch-enhancement" -> processBatchEnhancement(base64Image)
                        "batch-upscale" -> processBatchUpscale(base64Image)
                        else -> null
                    }
                    
                    if (processedBitmap != null) {
                        processedImages.add(processedBitmap)
                    } else {
                        // If processing fails, add original image
                        processedImages.add(bitmap)
                    }
                    
                    // Update processed images list
                    batchProcessedImages = processedImages.toList()
                }
                
                batchProcessingProgress = batchImages.size
                batchProcessingCurrentImage = "Batch processing complete!"
                
            } catch (e: Exception) {
                errorMessage = "Batch processing error: ${e.message}"
                Log.e("BatchProcessing", "Error during batch processing", e)
            } finally {
                isBatchProcessing = false
            }
        }
    }
    

    private fun startTimer() {
        _elapsedTimeInSeconds.value = 0L
        _totalGenerationTimeInSeconds.value = null
        generationStartTime = System.currentTimeMillis()
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (isLoading) {
                _elapsedTimeInSeconds.value = (System.currentTimeMillis() - generationStartTime) / 1000
                delay(1000)
            }
        }
    }

    fun generateImage() {
        if (selectedImage == null) {
            errorMessage = "Please select an image first"
            return
        }

        // Validate prompt for models that need it
        val promptRequiredModels = listOf("flux-img2img", "stable-diffusion-img2img", "sdxl-img2img", "stability-ai-img2img", "stability-ai-inpaint", "stability-ai-sketch", "stability-ai-structure")
        if (selectedModel in promptRequiredModels && prompt.isBlank()) {
            errorMessage = "Please enter a prompt for this model"
            return
        }
        
        // Validate search and replace model
        if (selectedModel == "stability-ai-search-replace") {
            if (searchPrompt.isBlank()) {
                errorMessage = "Please enter what to search for"
                return
            }
            if (replacePrompt.isBlank()) {
                errorMessage = "Please enter what to replace with"
                return
            }
        }
        
        // Validate search and recolor model
        if (selectedModel == "stability-ai-search-recolor") {
            if (searchPrompt.isBlank()) {
                errorMessage = "Please enter what to select for recoloring"
                return
            }
            if (replacePrompt.isBlank()) {
                errorMessage = "Please enter the new color/appearance"
                return
            }
        }
        
        // Validate replace background and relight model
        if (selectedModel == "stability-ai-replace-background-relight") {
            if (prompt.isBlank() && backgroundReferenceUri == null) {
                errorMessage = "Please enter a background prompt or select a background reference image"
                return
            }
        }
        
        // Validate dual image models
        if (selectedModel == "flux-kontext-dev-dual") {
            if (secondImage == null) {
                errorMessage = "Please select a second image for dual image processing"
                return
            }
            if (prompt.isBlank()) {
                errorMessage = "Please enter a prompt describing how to combine the images"
                return
            }
        }
        
        // Validate nano-banana model (second image is optional)
        if (selectedModel == "nano-banana") {
            if (prompt.isBlank()) {
                errorMessage = "Please enter a prompt for image transformation"
                return
            }
        }
        
        // Validate fashion try-on model
        if (selectedModel == "fashion-try-on") {
            if (clothingImage == null) {
                errorMessage = "Please select a clothing image"
                return
            }
            if (prompt.isBlank()) {
                errorMessage = "Please enter a prompt describing the desired result"
                return
            }
        }
        
        // Validate multiple face swap model
        if (selectedModel == "multiple-face-swap") {
            if (targetImage == null) {
                errorMessage = "Please select a target image with the face you want to swap"
                return
            }
        }
        
        // Validate Stability AI requirements
        if (selectedModel == "stability-ai-img2img" || selectedModel == "stability-ai-sketch" || selectedModel == "stability-ai-structure" || selectedModel == "stability-ai-search-replace" || selectedModel == "stability-ai-search-recolor" || selectedModel == "stability-ai-remove-background" || selectedModel == "stability-ai-replace-background-relight" || selectedModel == "stability-ai-mask-erase" || selectedModel == "stability-ai-inpaint" || selectedModel == "stability-ai-outpaint" || selectedModel == "stability-ai-style" || selectedModel == "stability-ai-style-transfer" || selectedModel == "stability-ai-upscale") {
            if (STABILITY_API_KEY == "YOUR_STABILITY_API_KEY_HERE" || STABILITY_API_KEY.isBlank()) {
                errorMessage = "Please set your Stability AI API Key"
                return
            }
            if (context == null) {
                errorMessage = "Context not available. Please restart the app."
                return
            }
        }

        isLoading = true
        generatedImageBitmap = null
        generatedImageUrl = null
        errorMessage = null
        loadingMessage = "Initializing..."
        startTimer()

        viewModelScope.launch {
            try {
                Log.d("UnifiedImg2Img", "Starting generation with model: $selectedModel")
                
                // Convert image to base64
                val base64Image = withContext(Dispatchers.IO) {
                    val resizedBitmap = resizeBitmapIfNeeded(selectedImage!!)
                    bitmapToBase64(resizedBitmap)
                }

                when (selectedModel) {
                    // A4F Models
                    "a4f-flux-kontext-dev" -> {
                        performA4FFluxKontextDev(base64Image)
                    }

                    "a4f-flux-kontext-pro" -> {
                        performA4FFluxKontextPro(base64Image)
                    }


                    "a4f-flux-kontext-max" -> {
                        performA4FFluxKontextMax(base64Image)
                    }
                    
                    // Stability AI Models (Premium)
            "stability-ai-img2img" -> {
                performStabilityAIImg2Img()
            }
            "stability-ai-sketch" -> {
                performStabilityAISketchToImage()
            }
            "stability-ai-structure" -> {
                performStabilityAIStructureControl()
            }
            "stability-ai-search-replace" -> {
                performStabilityAISearchAndReplace()
            }
            "stability-ai-search-recolor" -> {
                performStabilityAISearchAndRecolor()
            }
            "stability-ai-remove-background" -> {
                performStabilityAIRemoveBackground()
            }
            "stability-ai-replace-background-relight" -> {
                performStabilityAIReplaceBackgroundAndRelight()
            }
                    
                    "stability-ai-mask-erase" -> {
                        if (maskBitmap == null) {
                            errorMessage = "Please create a mask first by using the brush tool"
                            isLoading = false
                            return@launch
                        }
                        performStabilityAIMaskErase()
                    }
                    
                    "stability-ai-inpaint" -> {
                        if (maskBitmap == null) {
                            errorMessage = "Please create a mask first by using the brush tool"
                            isLoading = false
                            return@launch
                        }
                        if (prompt.isBlank()) {
                            errorMessage = "Please enter a prompt for inpainting"
                            isLoading = false
                            return@launch
                        }
                        performStabilityAIInpaint()
                    }
                    
                    "stability-ai-outpaint" -> {
                        if (outpaintLeft == 0 && outpaintRight == 0 && outpaintTop == 0 && outpaintBottom == 0) {
                            errorMessage = "Please specify at least one outpaint direction"
                            isLoading = false
                            return@launch
                        }
                        performStabilityAIOutpaint()
                    }
                    
                    "stability-ai-style" -> {
                        if (STABILITY_API_KEY == "YOUR_STABILITY_AI_KEY" || STABILITY_API_KEY.isBlank()) {
                            errorMessage = "Please set your Stability AI API Key"
                            isLoading = false
                            return@launch
                        }
                        if (prompt.isBlank()) {
                            errorMessage = "Please enter a prompt for style control"
                            isLoading = false
                            return@launch
                        }
                        performStabilityAIStyleControl()
                    }
                    
                    "stability-ai-style-transfer" -> {
                        if (styleReferenceImage == null) {
                            errorMessage = "Please select a style reference image for style transfer"
                            isLoading = false
                            return@launch
                        }
                        performStabilityAIStyleTransfer()
                    }
                    
                    "stability-ai-upscale" -> {
                        performStabilityAIUpscale()
                    }
                    
                    // Standard Image-to-Image Models
                    "flux-img2img" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        performFluxImg2Img(base64Image)
                    }
                    
                    "flux-kontext-pro-img2img" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        performFluxKontextProImg2Img(base64Image)
                    }
                    
                    "flux-kontext-dev-dual" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        // Convert second image to base64
                        val base64SecondImage = withContext(Dispatchers.IO) {
                            val resizedBitmap = resizeBitmapIfNeeded(secondImage!!)
                            bitmapToBase64(resizedBitmap)
                    }
                    performFluxKontextDevDual(base64Image, base64SecondImage)
                }
                
                "nano-banana" -> {
                    if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                        errorMessage = "Please set your ModelsLab API Key"
                        isLoading = false
                        return@launch
                    }
                    
                    // Check if second image exists (it's optional for nano-banana)
                    if (secondImage != null) {
                        // Dual image mode - both images provided
                        val base64SecondImage = withContext(Dispatchers.IO) {
                            val resizedBitmap = resizeBitmapIfNeeded(secondImage!!)
                            bitmapToBase64(resizedBitmap)
                        }
                        performNanoBananaDualImage(base64Image, base64SecondImage)
                    } else {
                        // Single image mode - only first image provided
                        performNanoBananaSingleImage(base64Image)
                    }
                }
                
                "stable-diffusion-img2img" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        performSDImg2Img(base64Image)
                    }
                    
                    "sdxl-img2img" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        performSDXLImg2Img(base64Image)
                    }
                    
                    // Style Transfer Models
                    "ghibli-style" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        performGhibliStyle(base64Image)
                    }
                    
                    "fashion-try-on" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        // Convert clothing image to base64
                        val base64ClothingImage = withContext(Dispatchers.IO) {
                            val resizedBitmap = resizeBitmapIfNeeded(clothingImage!!)
                            bitmapToBase64(resizedBitmap)
                        }
                        performFashionTryOn(base64Image, base64ClothingImage)
                    }
                    
                    "multiple-face-swap" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        // Convert target image to base64
                        val base64TargetImage = withContext(Dispatchers.IO) {
                            val resizedBitmap = resizeBitmapIfNeeded(targetImage!!)
                            bitmapToBase64(resizedBitmap)
                        }
                        performMultipleFaceSwap(base64Image, base64TargetImage)
                    }


                    "headshot-img2img" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        performHeadshotImg2Img(base64Image)
                    }


                    "Enhance-img2img" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        performEnhanceImg2Img(base64Image)
                    }


                    "Remove-img2img" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                    }
                        performRemoveImg2Img(base64Image)
                    }
                    
                    "v51-inpainting" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        if (maskBitmap == null) {
                            errorMessage = "Please create a mask first by using the brush tool"
                            isLoading = false
                            return@launch
                        }
                        if (prompt.isBlank()) {
                            errorMessage = "Please enter a prompt for inpainting"
                            isLoading = false
                            return@launch
                        }
                        // Convert mask to base64 with proper binary formatting
                        val base64MaskImage = withContext(Dispatchers.IO) {
                            // Ensure mask is properly formatted as binary black and white
                            val binaryMask = ensureBinaryMask(maskBitmap!!)
                            // Resize if needed to match API requirements
                            val resizedMask = resizeBitmapIfNeeded(binaryMask)
                            // Convert to PNG base64 for better mask compatibility
                            maskBitmapToPngBase64(resizedMask)
                        }
                        performInpaintingV51(base64Image, base64MaskImage)
                    }
                    
                    "object-removal" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        if (maskBitmap == null) {
                            errorMessage = "Please create a mask to define the objects to remove using the brush tool"
                            isLoading = false
                            return@launch
                        }
                        // Convert mask to base64 with proper binary formatting
                        val base64MaskImage = withContext(Dispatchers.IO) {
                            // Ensure mask is properly formatted as binary black and white
                            val binaryMask = ensureBinaryMask(maskBitmap!!)
                            // Resize if needed to match API requirements
                            val resizedMask = resizeBitmapIfNeeded(binaryMask)
                            // Convert to PNG base64 for better mask compatibility
                            maskBitmapToPngBase64(resizedMask)
                        }
                        performObjectRemoval(base64Image, base64MaskImage)
                    }
                    
                    "bg-remove-mask" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        if (maskBitmap == null) {
                            errorMessage = "Please create a mask to define the background areas using the brush tool"
                            isLoading = false
                            return@launch
                        }
                        // Convert mask to base64 with proper binary formatting
                        val base64MaskImage = withContext(Dispatchers.IO) {
                            // Ensure mask is properly formatted as binary black and white
                            val binaryMask = ensureBinaryMask(maskBitmap!!)
                            // Resize if needed to match API requirements
                            val resizedMask = resizeBitmapIfNeeded(binaryMask)
                            // Convert to PNG base64 for better mask compatibility
                            maskBitmapToPngBase64(resizedMask)
                        }
                        performBackgroundRemovalMask(base64Image, base64MaskImage)
                    }


                    "Sketch`" -> {
                        if (MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE" || MODELSLAB_API_KEY.isBlank()) {
                            errorMessage = "Please set your ModelsLab API Key"
                            isLoading = false
                            return@launch
                        }
                        performSketchImg2Img(base64Image)
                    }





                    "Sketch Img2Img" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key"
                            isLoading = false
                            return@launch
                        }
                        performSketchImg2Img(base64Image)
                    }

                    
                    "anime-style" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key"
                            isLoading = false
                            return@launch
                        }
                        performAnimeStyle(base64Image)
                    }
                    
                    "oil-painting" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key"
                            isLoading = false
                            return@launch
                        }
                        performOilPaintingStyle(base64Image)
                    }
                    
                    "watercolor" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key"
                            isLoading = false
                            return@launch
                        }
                        performWatercolorStyle(base64Image)
                    }
                    

                    
                    "cyberpunk-style" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key"
                            isLoading = false
                            return@launch
                        }
                        performCyberpunkStyle(base64Image)
                    }
                    
                    "fantasy-art" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key"
                            isLoading = false
                            return@launch
                        }
                        performFantasyArtStyle(base64Image)
                    }
                    
                    // Specialized Models
                    "photo-enhance" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key"
                            isLoading = false
                            return@launch
                        }
                        performPhotoEnhancement(base64Image)
                    }
                    
                    "colorize" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key"
                            isLoading = false
                            return@launch
                        }
                        performColorization(base64Image)
                    }
                    
                    "super-resolution" -> {
                        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
                            errorMessage = "Please set your A4F API Key"
                            isLoading = false
                            return@launch
                        }
                        performSuperResolution(base64Image)
                    }
                    
                    // Batch Processing Models
                    "batch-style-transfer" -> {
                        if (batchImages.isEmpty()) {
                            errorMessage = "Please add images to batch first"
                            isLoading = false
                            return@launch
                        }
                        processBatch()
                    }
                    
                    "batch-enhancement" -> {
                        if (batchImages.isEmpty()) {
                            errorMessage = "Please add images to batch first"
                            isLoading = false
                            return@launch
                        }
                        processBatch()
                    }
                    
                    "batch-upscale" -> {
                        if (batchImages.isEmpty()) {
                            errorMessage = "Please add images to batch first"
                            isLoading = false
                            return@launch
                        }
                        processBatch()
                    }
                    
                    else -> {
                        errorMessage = "Invalid model selected: $selectedModel"
                        isLoading = false
                    }
                }
                
                // Auto-save to history after successful generation
                if ((generatedImageBitmap != null || generatedImageUrl != null) && errorMessage == null) {
                    Log.d("UnifiedImg2Img", "Auto-saving to history...")
                    saveToHistory()
                }
                
            } catch (e: Exception) {
                errorMessage = "Error: ${e.message}"
                e.printStackTrace()
            } finally {
                isLoading = false
                if (generationStartTime > 0) {
                    _totalGenerationTimeInSeconds.value = (System.currentTimeMillis() - generationStartTime) / 1000
                }
                loadingMessage = ""
            }
        }
    }

    // Stability AI Implementation
    private suspend fun performStabilityAIImg2Img() = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with Stability AI..."
        
        try {
            // Create a temporary URI for the selected image
            val tempFile = File.createTempFile("temp_image", ".png", context!!.cacheDir)
            val outputStream = tempFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()
            
            val imageUri = android.net.Uri.fromFile(tempFile)
            
            // Call Stability AI repository
            val response = StabilityRepository.generateImageToImage(
                context = context!!,
                imageUri = imageUri,
                prompt = prompt
            )
            
            if (response?.status == "success" && response.imageData != null) {
                // Convert bytes to bitmap
                val bitmap = BitmapFactory.decodeByteArray(response.imageData, 0, response.imageData.size)
                if (bitmap != null) {
                    generatedImageBitmap = bitmap
                    Log.d("UnifiedImg2Img", "Successfully generated Stability AI image: ${bitmap.width}x${bitmap.height}")
                } else {
                    // Create temporary file for URL display
                    val generatedFile = File.createTempFile("stability_generated", ".png", context!!.cacheDir)
                    generatedFile.writeBytes(response.imageData)
                    generatedImageUrl = generatedFile.toURI().toString()
                    Log.d("UnifiedImg2Img", "Created temporary file for Stability AI image")
                }
            } else {
                val error = response?.error ?: "Failed to generate image with Stability AI"
                errorMessage = "Stability AI Error: $error"
                Log.e("UnifiedImg2Img", "Stability AI generation failed: $error")
            }
            
            // Clean up temp file
            tempFile.delete()
            
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Stability AI generation", e)
            errorMessage = "Stability AI Error: ${e.localizedMessage}"
        }
    }

    // Search and Replace implementation
    private suspend fun performStabilityAISearchAndReplace() = withContext(Dispatchers.IO) {
        loadingMessage = "Performing Search and Replace with Stability AI..."

        try {
            // Create a temporary URI for the selected image
            val tempFile = File.createTempFile("search_replace_temp", ".png", context!!.cacheDir)
            val outputStream = tempFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()

            val imageUri = android.net.Uri.fromFile(tempFile)

            // Call Stability AI repository
            val response = StabilityRepository.searchAndReplace(
                context = context!!,
                imageUri = imageUri,
                prompt = replacePrompt,
                searchPrompt = searchPrompt,
                outputFormat = "webp"
            )

            if (response?.status == "success" && response.imageData != null) {
                // Convert bytes to bitmap
                val bitmap = BitmapFactory.decodeByteArray(response.imageData, 0, response.imageData.size)
                if (bitmap != null) {
                    generatedImageBitmap = bitmap
                } else {
                    // Create temporary file for URL display
                    val generatedFile = File.createTempFile("stability_search_replace", ".webp", context!!.cacheDir)
                    generatedFile.writeBytes(response.imageData)
                    generatedImageUrl = generatedFile.toURI().toString()
                }
            } else {
                val error = response?.error ?: "Failed to perform search and replace"
                errorMessage = "Search and Replace Error: $error"
            }

            // Clean up temp file
            tempFile.delete()

        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Search and Replace", e)
            errorMessage = "Search and Replace Error: ${e.localizedMessage}"
        }
    }

    // Search and Recolor implementation
    private suspend fun performStabilityAISearchAndRecolor() = withContext(Dispatchers.IO) {
        loadingMessage = "Performing Search and Recolor with Stability AI..."

        try {
            // Create a temporary URI for the selected image
            val tempFile = File.createTempFile("search_recolor_temp", ".png", context!!.cacheDir)
            val outputStream = tempFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()

            val imageUri = android.net.Uri.fromFile(tempFile)

            // Call Stability AI repository
            val response = StabilityRepository.searchAndRecolor(
                context = context!!,
                imageUri = imageUri,
                prompt = replacePrompt, // This is the new color/appearance
                selectPrompt = searchPrompt, // This is what to select for recoloring
                outputFormat = "webp"
            )

            if (response?.status == "success" && response.imageData != null) {
                // Always save to file first for WEBP format
                val generatedFile = File.createTempFile("stability_search_recolor", ".webp", context!!.cacheDir)
                generatedFile.writeBytes(response.imageData)
                
                // Try to decode the saved file
                val bitmap = BitmapFactory.decodeFile(generatedFile.absolutePath)
                if (bitmap != null) {
                    generatedImageBitmap = bitmap
                    // Keep the file URL as backup
                    generatedImageUrl = generatedFile.absolutePath
                } else {
                    // If bitmap decode fails, use file URL for display
                    generatedImageUrl = generatedFile.absolutePath
                    Log.e("UnifiedImg2Img", "Failed to decode WEBP image, using file URL: $generatedImageUrl")
                }
            } else {
                val error = response?.error ?: "Failed to perform search and recolor"
                errorMessage = "Search and Recolor Error: $error"
            }

            // Clean up temp file
            tempFile.delete()

        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Search and Recolor", e)
            errorMessage = "Search and Recolor Error: ${e.localizedMessage}"
        }
    }

    // Remove Background implementation
    private suspend fun performStabilityAIRemoveBackground() = withContext(Dispatchers.IO) {
        loadingMessage = "Removing background with Stability AI..."

        try {
            // Create a temporary URI for the selected image
            val tempFile = File.createTempFile("remove_bg_temp", ".png", context!!.cacheDir)
            val outputStream = tempFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()

            val imageUri = android.net.Uri.fromFile(tempFile)

            // Call Stability AI repository
            val response = StabilityRepository.removeBackground(
                context = context!!,
                imageUri = imageUri,
                outputFormat = "webp"
            )

            if (response?.status == "success" && response.imageData != null) {
                // Always save to file first for WEBP format
                val generatedFile = File.createTempFile("stability_remove_bg", ".webp", context!!.cacheDir)
                generatedFile.writeBytes(response.imageData)
                
                // Try to decode the saved file
                val bitmap = BitmapFactory.decodeFile(generatedFile.absolutePath)
                if (bitmap != null) {
                    generatedImageBitmap = bitmap
                    // Keep the file URL as backup
                    generatedImageUrl = generatedFile.absolutePath
                } else {
                    // If bitmap decode fails, use file URL for display
                    generatedImageUrl = generatedFile.absolutePath
                    Log.e("UnifiedImg2Img", "Failed to decode WEBP image, using file URL: $generatedImageUrl")
                }
            } else {
                val error = response?.error ?: "Failed to remove background"
                errorMessage = "Remove Background Error: $error"
            }

            // Clean up temp file
            tempFile.delete()

        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Remove Background", e)
            errorMessage = "Remove Background Error: ${e.localizedMessage}"
        }
    }

    // Replace Background and Relight implementation
    private suspend fun performStabilityAIReplaceBackgroundAndRelight() = withContext(Dispatchers.IO) {
        loadingMessage = "Replacing background and relighting with Stability AI..."

        try {
            // Create a temporary URI for the selected image
            val tempFile = File.createTempFile("replace_bg_relight_temp", ".png", context!!.cacheDir)
            val outputStream = tempFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()

            val imageUri = android.net.Uri.fromFile(tempFile)

            // Call Stability AI repository
            val response = StabilityRepository.replaceBackgroundAndRelight(
                context = context!!,
                imageUri = imageUri,
                backgroundPrompt = prompt, // Using the main prompt for background description
                backgroundReferenceUri = backgroundReferenceUri, // Using the background reference image if provided
                outputFormat = "webp"
            )

            if (response?.status == "success" && response.imageData != null) {
                // Always save to file first for WEBP format
                val generatedFile = File.createTempFile("stability_replace_bg_relight", ".webp", context!!.cacheDir)
                generatedFile.writeBytes(response.imageData)
                
                // Try to decode the saved file
                val bitmap = BitmapFactory.decodeFile(generatedFile.absolutePath)
                if (bitmap != null) {
                    generatedImageBitmap = bitmap
                    // Keep the file URL as backup
                    generatedImageUrl = generatedFile.absolutePath
                } else {
                    // If bitmap decode fails, use file URL for display
                    generatedImageUrl = generatedFile.absolutePath
                    Log.e("UnifiedImg2Img", "Failed to decode WEBP image, using file URL: $generatedImageUrl")
                }
            } else {
                val error = response?.error ?: "Failed to replace background and relight"
                errorMessage = "Replace Background & Relight Error: $error"
            }

            // Clean up temp file
            tempFile.delete()

        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Replace Background & Relight", e)
            errorMessage = "Replace Background & Relight Error: ${e.localizedMessage}"
        }
    }

    // Standard Image-to-Image implementations
    private suspend fun performFluxImg2Img(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with Flux..."
        
        val jsonBody = JSONObject().apply {
            put("key", MODELSLAB_API_KEY)
            put("model_id", "flux")
            put("prompt", prompt)
            put("negative_prompt", negativePrompt)
            put("init_image", base64Image)
            put("samples", "1")
            put("safety_checker", false)
            put("strength", strength)
            put("scheduler", "DPMSolverMultistepScheduler")
            put("base64", true)
            put("seed", null)
            put("webhook", null)
            put("track_id", null)
        }

        val result = makeApiCall(
            url = "https://modelslab.com/api/v4/dreambooth/img2img",
            jsonBody = jsonBody,
            modelName = "Flux Img2Img"
        )
        
        processApiResult(result)
    }

    private suspend fun performFluxKontextProImg2Img(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with Flux Kontext Pro..."

        val jsonBody = JSONObject().apply {
            put("key", MODELSLAB_API_KEY)
            put("init_image", base64Image)
            put("prompt", prompt)
            put("model_id", "flux-kontext-pro")
            put("aspect_ratio", null)
            put("base64", true)
            put("webhook", null)
            put("track_id", null)
        }

        val result = makeApiCallWithPolling(
            url = "https://modelslab.com/api/v7/images/image-to-image",
            jsonBody = jsonBody,
            modelName = "Flux Kontext Pro Img2Img"
        )

        processApiResult(result)
    }
    
    private suspend fun performFluxKontextDevDual(base64Image1: String, base64Image2: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with Flux Kontext Dev Dual Images..."
        
        Log.d("UnifiedImg2Img", "Starting Flux Kontext Dev Dual with:")
        Log.d("UnifiedImg2Img", "Image 1 size: ${base64Image1.length} chars")
        Log.d("UnifiedImg2Img", "Image 2 size: ${base64Image2.length} chars")
        Log.d("UnifiedImg2Img", "Prompt: $prompt")
        
        val jsonBody = JSONObject().apply {
            put("key", MODELSLAB_API_KEY)
            put("init_image", base64Image1)  // Send base64 directly
            put("init_image_2", base64Image2)  // Send base64 directly
            put("prompt", prompt)
            put("negative_prompt", negativePrompt)
            put("model_id", "flux-kontext-dev")
            put("num_inference_steps", "28")
            put("strength", "0.5")
            put("scheduler", "DPMSolverMultistepScheduler")
            put("guidance", "2.5")
            put("enhance_prompt", null)
            put("base64", true)  // Indicate we're sending base64 data
            put("webhook", null)
            put("track_id", null)
        }
        
        Log.d("UnifiedImg2Img", "Request JSON created with base64 images")
        
        // Use makeApiCallWithPolling for better handling
        val result = makeApiCallWithPolling(
            url = "https://modelslab.com/api/v6/images/img2img",
            jsonBody = jsonBody,
            modelName = "Flux Kontext Dev Dual"
        )
        
        processApiResult(result)
    }

    private suspend fun performSDImg2Img(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with Stable Diffusion..."
        
        val jsonBody = JSONObject().apply {
            put("key", MODELSLAB_API_KEY)
            put("model_id", "sdxl")
            put("prompt", prompt)
            put("negative_prompt", negativePrompt)
            put("init_image", base64Image)
            put("samples", "1")
            put("num_inference_steps", steps)
            put("guidance_scale", guidanceScale)
            put("strength", strength)
            put("scheduler", "DDPMScheduler")
            put("base64", true)
            put("seed", null)
            put("webhook", null)
            put("track_id", null)
        }

        val result = makeApiCall(
            url = "https://modelslab.com/api/v6/realtime/img2img",
            jsonBody = jsonBody,
            modelName = "Stable Diffusion Img2Img"
        )
        
        processApiResult(result)
    }

    private suspend fun performSDXLImg2Img(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with SDXL..."
        
        val jsonBody = JSONObject().apply {
            put("key", MODELSLAB_API_KEY)
            put("model_id", "sdxl")
            put("prompt", prompt)
            put("negative_prompt", negativePrompt)
            put("init_image", base64Image)
            put("samples", "1")
            put("num_inference_steps", steps)
            put("guidance_scale", guidanceScale)
            put("strength", strength)
            put("width", "1024")
            put("height", "1024")
            put("scheduler", "DDPMScheduler")
            put("base64", true)
            put("seed", null)
            put("webhook", null)
            put("track_id", null)
        }

        val result = makeApiCall(
            url = "https://modelslab.com/api/v6/realtime/img2img",
            jsonBody = jsonBody,
            modelName = "SDXL Img2Img"
        )
        
        processApiResult(result)
    }

    // Style Transfer implementations
    private suspend fun performGhibliStyle(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Applying Ghibli Studio style..."
        
        val dimensions = calculateDimensions(selectedImage!!)
        
        val jsonBody = JSONObject().apply {
            put("key", MODELSLAB_API_KEY)
            put("model_id", "fluxdev")
            put("init_image", base64Image)
            put("prompt", "Ghibli Studio style, Charming hand-drawn anime-style illustration")
            put("controlnet_type", "ghibli")
            put("controlnet_model", "ghibli")
            put("height", dimensions.second.toString())
            put("width", dimensions.first.toString())
            put("base64", true)
            put("temp", "no")
            put("webhook", null)
            put("track_id", null)
        }

        val result = makeApiCallWithPolling(
            url = "https://modelslab.com/api/v5/controlnet",
            jsonBody = jsonBody,
            modelName = "Ghibli Style"
        )
        
        processApiResult(result)
    }


    private suspend fun performHeadshotImg2Img(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with Headshot..."



        val jsonBody = JSONObject().apply {
            put("key", MODELSLAB_API_KEY)
            put("prompt", prompt)
            put("negative_prompt", negativePrompt)
            put("face_image", base64Image)
            put("width", "512")
            put("height", "768")
            put("samples", "1")
            put("num_inference_steps","31")
            put("safety_checker", false)
            put("base64", true)
            put("seed", "")
            put("guidance_scale", guidanceScale)
            put("s_scale", "2.0")
            put("webhook", null)
            put("track_id", null)
        }

        val result = makeApiCallWithPolling(
            url = "https://modelslab.com/api/v6/image_editing/head_shot",
            jsonBody = jsonBody,
            modelName = "Headshot Img2Img"
        )

        processApiResult(result)
    }


    private suspend fun performEnhanceImg2Img(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with Enhance..."

        val jsonBody = JSONObject().apply {
            put("key", MODELSLAB_API_KEY)
            put("init_image", base64Image)
            put("face_enhance", true)  // default its off
            put("scale", 3)   // default scale 2
            put("model_id", "ultra_resolution")
            put("webhook", null)
            put("track_id", null)
            put("base64", true)

        }

        val result = makeApiCallWithPolling(
            url = "https://modelslab.com/api/v6/image_editing/super_resolution",
            jsonBody = jsonBody,
            modelName = "Enhance Img2Img"
        )

        processApiResult(result)
    }

    private suspend fun performStabilityAIMaskErase() = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with Stability AI Mask Erase..."
        
        try {
            // Create temporary files for image and mask
            val imageFile = File.createTempFile("temp_image", ".png", context!!.cacheDir)
            val maskFile = File.createTempFile("temp_mask", ".png", context!!.cacheDir)
            
            // Save selected image to temp file
            val imageOutputStream = imageFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, imageOutputStream)
            imageOutputStream.close()
            
            // Save mask to temp file
            val maskOutputStream = maskFile.outputStream()
            maskBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, maskOutputStream)
            maskOutputStream.close()
            
            // Debug: Log mask bitmap info
            Log.d("UnifiedImg2Img", "Mask bitmap created: ${maskBitmap!!.width}x${maskBitmap!!.height}")
            Log.d("UnifiedImg2Img", "Original image: ${selectedImage!!.width}x${selectedImage!!.height}")
            Log.d("UnifiedImg2Img", "Mask file size: ${maskFile.length()} bytes")
            
            // Call the erase API
            eraseImageWithMask(imageFile, maskFile)
            
            // Clean up temp files
            imageFile.delete()
            maskFile.delete()
            
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Stability AI mask erase", e)
            errorMessage = "Stability AI Mask Error: ${e.localizedMessage}"
        }
    }

    private suspend fun eraseImageWithMask(imageFile: File, maskFile: File) = withContext(Dispatchers.IO) {
        loadingMessage = "Erasing image area..."

        try {
            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/png".toMediaTypeOrNull()))
                .addFormDataPart("mask", maskFile.name, maskFile.asRequestBody("image/png".toMediaTypeOrNull()))
                .addFormDataPart("output_format", "webp")
                .build()

            val request = Request.Builder()
                .url("https://api.stability.ai/v2beta/stable-image/edit/erase")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $STABILITY_API_KEY")
                .addHeader("accept", "image/*")
                .build()

            val response = client.newCall(request).execute()
            Log.d("UnifiedImg2Img", "Stability AI API response code: ${response.code}")
            Log.d("UnifiedImg2Img", "Stability AI API response message: ${response.message}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.bytes()
                if (responseBody != null) {
                    Log.d("UnifiedImg2Img", "Response body size: ${responseBody.size} bytes")
                    val bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.size)
                    if (bitmap != null) {
                        generatedImageBitmap = bitmap
                        Log.d("UnifiedImg2Img", "Generated bitmap: ${bitmap.width}x${bitmap.height}")
                    } else {
                        Log.e("UnifiedImg2Img", "Failed to decode bitmap from response")
                        errorMessage = "Failed to decode generated image"
                    }
                } else {
                    Log.e("UnifiedImg2Img", "Empty response body")
                    errorMessage = "Error: Empty response from server."
                }
            } else {
                val errorBody = response.body?.string()
                Log.e("UnifiedImg2Img", "API Error: ${response.code} ${response.message}")
                Log.e("UnifiedImg2Img", "Error body: $errorBody")
                errorMessage = "Error: ${response.code} ${response.message}"
            }
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in mask API call", e)
            errorMessage = "Error: ${e.localizedMessage}"
        }
    }

    private suspend fun performRemoveImg2Img(base64Image: String) = withContext(Dispatchers.IO) {


        val jsonBody = JSONObject().apply {
            put("key", MODELSLAB_API_KEY)
            put("image", base64Image)
            put("seed", null)
            put("post_process_mask", false)
            put("only_mask", false)
            put("alpha_matting", false)
            put("base64", true)
            put("webhook", null)
            put("track_id", null)

        }

        val result = makeApiCallWithPolling(
            url = "https://modelslab.com/api/v6/image_editing/removebg_mask",
            jsonBody = jsonBody,
            modelName = "Remove Img2Img"
        )

        processApiResult(result)
    }
    
    private suspend fun performObjectRemoval(base64Image: String, base64MaskImage: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing object removal..."
        
        try {
            Log.d("UnifiedImg2Img", "Starting object removal with enhanced debugging")
            Log.d("UnifiedImg2Img", "Image data length: ${base64Image.length}")
            Log.d("UnifiedImg2Img", "Mask data length: ${base64MaskImage.length}")
            
            // Validate API key first
            if (MODELSLAB_API_KEY.isBlank() || MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE") {
                val error = "Object Removal Error: ModelsLab API key not configured properly. Please check your API key in modelslabapikey.kt"
                Log.e("UnifiedImg2Img", error)
                errorMessage = error
                return@withContext
            }
            
            Log.d("UnifiedImg2Img", "API Key validation passed (length: ${MODELSLAB_API_KEY.length})")
            
            // Validate mask dimensions match original image
            if (maskBitmap != null && selectedImage != null) {
                Log.d("UnifiedImg2Img", "Mask dimensions: ${maskBitmap!!.width}x${maskBitmap!!.height}")
                Log.d("UnifiedImg2Img", "Original image dimensions: ${selectedImage!!.width}x${selectedImage!!.height}")
                
                // Ensure mask has proper format
                if (maskBitmap!!.width != selectedImage!!.width || maskBitmap!!.height != selectedImage!!.height) {
                    Log.w("UnifiedImg2Img", "Mask dimensions don't match original image, resizing mask...")
                    // Resize mask to match original image dimensions
                    maskBitmap = Bitmap.createScaledBitmap(maskBitmap!!, selectedImage!!.width, selectedImage!!.height, true)
                    Log.d("UnifiedImg2Img", "Mask resized to: ${maskBitmap!!.width}x${maskBitmap!!.height}")
                }
            }
            
            // Create the request JSON with proper object removal parameters
            val jsonBody = JSONObject().apply {
                put("key", MODELSLAB_API_KEY)
                put("init_image", base64Image)
                put("mask_image", base64MaskImage)
                put("base64", true)
                put("webhook", JSONObject.NULL)
                put("track_id", JSONObject.NULL)
            }
            
            Log.d("UnifiedImg2Img", "Request JSON prepared for Object Removal")
            
            // Use the ModelsLab v6 object removal endpoint
            val result = makeApiCallWithPolling(
                url = "https://modelslab.com/api/v6/image_editing/object_removal",
                jsonBody = jsonBody,
                modelName = "Object Removal"
            )
            
            if (result != null) {
                Log.d("UnifiedImg2Img", "Success with object removal endpoint")
                processApiResult(result)
            } else {
                val finalError = "Object Removal Error: Failed to remove objects from image. Please ensure the mask clearly defines the objects to remove (white areas = objects to remove, black areas = areas to keep). Check your API key and internet connection."
                Log.e("UnifiedImg2Img", finalError)
                errorMessage = finalError
            }
            
        } catch (e: Exception) {
            val error = "Object Removal Error: Unexpected error occurred - ${e.message}. Please check your inputs and try again."
            Log.e("UnifiedImg2Img", "Critical error in Object Removal", e)
            errorMessage = error
        }
    }
    
    private suspend fun performBackgroundRemovalMask(base64Image: String, base64MaskImage: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing background removal with mask..."
        
        try {
            Log.d("UnifiedImg2Img", "Starting background removal with mask")
            Log.d("UnifiedImg2Img", "Prompt: '$prompt'")
            Log.d("UnifiedImg2Img", "Image data length: ${base64Image.length}")
            Log.d("UnifiedImg2Img", "Mask data length: ${base64MaskImage.length}")
            
            // Validate API key first
            if (MODELSLAB_API_KEY.isBlank() || MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE") {
                val error = "Background Removal Error: ModelsLab API key not configured properly. Please check your API key in modelslabapikey.kt"
                Log.e("UnifiedImg2Img", error)
                errorMessage = error
                return@withContext
            }
            
            Log.d("UnifiedImg2Img", "API Key validation passed (length: ${MODELSLAB_API_KEY.length})")
            
            // Validate mask dimensions match original image
            if (maskBitmap != null && selectedImage != null) {
                Log.d("UnifiedImg2Img", "Mask dimensions: ${maskBitmap!!.width}x${maskBitmap!!.height}")
                Log.d("UnifiedImg2Img", "Original image dimensions: ${selectedImage!!.width}x${selectedImage!!.height}")
                
                // Ensure mask has proper format
                if (maskBitmap!!.width != selectedImage!!.width || maskBitmap!!.height != selectedImage!!.height) {
                    Log.w("UnifiedImg2Img", "Mask dimensions don't match original image, resizing mask...")
                    // Resize mask to match original image dimensions
                    maskBitmap = Bitmap.createScaledBitmap(maskBitmap!!, selectedImage!!.width, selectedImage!!.height, true)
                    Log.d("UnifiedImg2Img", "Mask resized to: ${maskBitmap!!.width}x${maskBitmap!!.height}")
                }
            }
            
            // Create the request JSON with proper background removal parameters according to the API spec
            val jsonBody = JSONObject().apply {
                put("key", MODELSLAB_API_KEY)
                put("prompt", prompt.ifBlank { "change the background with black gray and white shade" })
                put("negative_prompt", negativePrompt)
                put("init_image", base64Image)
                put("mask_image", base64MaskImage)
                put("width", 512)
                put("height", 512)
                put("samples", 1)
                put("num_inference_steps", 30)
                put("safety_checker", "no")
                put("enhance_prompt", "yes")
                put("guidance_scale", 5)
                put("strength", 0.7)
                put("base64", true)
                put("seed", JSONObject.NULL)
                put("webhook", JSONObject.NULL)
                put("track_id", JSONObject.NULL)
            }
            
            Log.d("UnifiedImg2Img", "Request JSON prepared for Background Removal with Mask")
            
            // Use the ModelsLab v6 background removal with mask endpoint
            val result = makeApiCallWithPolling(
                url = "https://modelslab.com/api/v6/image_editing/removebg_mask",
                jsonBody = jsonBody,
                modelName = "Background Removal (Mask)"
            )
            
            if (result != null) {
                Log.d("UnifiedImg2Img", "Success with background removal mask endpoint")
                processApiResult(result)
            } else {
                val finalError = "Background Removal Error: Failed to remove background from image. Please ensure the mask clearly defines the background areas to remove (white areas = background to change, black areas = areas to keep). Check your API key and internet connection."
                Log.e("UnifiedImg2Img", finalError)
                errorMessage = finalError
            }
            
        } catch (e: Exception) {
            val error = "Background Removal Error: Unexpected error occurred - ${e.message}. Please check your inputs and try again."
            Log.e("UnifiedImg2Img", "Critical error in Background Removal with Mask", e)
            errorMessage = error
        }
    }
    
    private suspend fun performInpaintingV51(base64Image: String, base64MaskImage: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with V51 Inpainting..."
        
        try {
            Log.d("UnifiedImg2Img", "Starting V51 Inpainting with enhanced debugging")
            Log.d("UnifiedImg2Img", "Prompt: '$prompt'")
            Log.d("UnifiedImg2Img", "Image data length: ${base64Image.length}")
            Log.d("UnifiedImg2Img", "Mask data length: ${base64MaskImage.length}")
            
            // Validate API key first
            if (MODELSLAB_API_KEY.isBlank() || MODELSLAB_API_KEY == "YOUR_MODELSLAB_API_KEY_HERE") {
                val error = "V51 Inpainting Error: ModelsLab API key not configured properly. Please check your API key in modelslabapikey.kt"
                Log.e("UnifiedImg2Img", error)
                errorMessage = error
                return@withContext
            }
            
            Log.d("UnifiedImg2Img", "API Key validation passed (length: ${MODELSLAB_API_KEY.length})")
            
            // Validate mask dimensions match original image
            if (maskBitmap != null && selectedImage != null) {
                Log.d("UnifiedImg2Img", "Mask dimensions: ${maskBitmap!!.width}x${maskBitmap!!.height}")
                Log.d("UnifiedImg2Img", "Original image dimensions: ${selectedImage!!.width}x${selectedImage!!.height}")
                
                // Ensure mask has proper format
                if (maskBitmap!!.width != selectedImage!!.width || maskBitmap!!.height != selectedImage!!.height) {
                    Log.w("UnifiedImg2Img", "Mask dimensions don't match original image, resizing mask...")
                    // Resize mask to match original image dimensions
                    maskBitmap = Bitmap.createScaledBitmap(maskBitmap!!, selectedImage!!.width, selectedImage!!.height, true)
                    Log.d("UnifiedImg2Img", "Mask resized to: ${maskBitmap!!.width}x${maskBitmap!!.height}")
                }
            }
            
            // Create the request JSON with proper V51 inpainting parameters
            val jsonBody = JSONObject().apply {
                put("key", MODELSLAB_API_KEY)
                put("model_id", "v51_inpainting")
                put("prompt", prompt)
                put("negative_prompt", negativePrompt.ifBlank { 
                    "ugly, tiling, poorly drawn hands, poorly drawn feet, poorly drawn face, out of frame, extra limbs, disfigured, deformed, body out of frame, bad anatomy, watermark, signature, cut off, low contrast, underexposed, overexposed, bad art, beginner, amateur, distorted face, blurry, draft, grainy" 
                })
                put("init_image", base64Image)
                put("mask_image", base64MaskImage)
                put("samples", 1)
                put("steps", 21)
                put("safety_checker", "no")
                put("guidance_scale", guidanceScale.toDouble())
                put("strength", strength.toDouble())
                put("scheduler", "DPMSolverMultistepScheduler")
                put("tomesd", "yes")
                put("use_karras_sigmas", "yes")
                put("base64", true)
                put("webhook", JSONObject.NULL)
                put("track_id", JSONObject.NULL)
            }
            
            Log.d("UnifiedImg2Img", "Request JSON prepared for V51 Inpainting")
            Log.d("UnifiedImg2Img", "Request parameters: model=v51_inpainting, samples=1, steps=21, guidance=${guidanceScale}, strength=${strength}")
            
            // Try the most reliable endpoint for V51 inpainting
            val endpoints = listOf(
                "https://modelslab.com/api/v6/images/inpaint" to "V51 Inpainting (v6/inpaint)",
                "https://modelslab.com/api/v6/realtime/img2img" to "V51 Inpainting (v6/realtime)",
                "https://modelslab.com/api/v5/controlnet" to "V51 Inpainting (v5/controlnet)"
            )
            
            var lastError: String? = null
            
            for ((url, name) in endpoints) {
                Log.d("UnifiedImg2Img", "Trying endpoint: $url ($name)")
                
                try {
                    val result = makeApiCallWithPolling(
                        url = url,
                        jsonBody = jsonBody,
                        modelName = name
                    )
                    
                    if (result != null) {
                        Log.d("UnifiedImg2Img", "Success with endpoint: $url")
                        processApiResult(result)
                        return@withContext
                    } else {
                        Log.w("UnifiedImg2Img", "Endpoint $url returned null, trying next...")
                        lastError = errorMessage ?: "Unknown error from $name"
                    }
                } catch (e: Exception) {
                    Log.e("UnifiedImg2Img", "Exception with endpoint $url: ${e.message}", e)
                    lastError = e.message ?: "Exception occurred with $name"
                }
            }
            
            // All endpoints failed
            val finalError = "V51 Inpainting Error: All API endpoints failed. Last error: $lastError. Please check your API key, internet connection, and try again. Ensure the mask is a black and white image where white areas represent regions to be inpainted."
            Log.e("UnifiedImg2Img", finalError)
            errorMessage = finalError
            
        } catch (e: Exception) {
            val error = "V51 Inpainting Error: Unexpected error occurred - ${e.message}. Please check your inputs and try again."
            Log.e("UnifiedImg2Img", "Critical error in V51 Inpainting", e)
            errorMessage = error
        }
    }
    
    private suspend fun performFashionTryOn(base64PersonImage: String, base64ClothingImage: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing fashion try-on..."
        
        try {
            Log.d("UnifiedImg2Img", "Starting Fashion Try-On with base64 images")
            Log.d("UnifiedImg2Img", "Cloth type: $clothType")
            Log.d("UnifiedImg2Img", "Prompt: $prompt")
            
            // Try sending base64 images directly
            val jsonBody = JSONObject().apply {
                put("key", MODELSLAB_API_KEY)
                put("init_image", base64PersonImage)  // Send base64 directly
                put("cloth_image", base64ClothingImage)  // Send base64 directly
                put("cloth_type", clothType)
                put("prompt", prompt.ifBlank { "A realistic photo of a model wearing a beautiful t-shirt" })
                put("negative_prompt", negativePrompt.ifBlank { "Low quality, unrealistic, bad cloth, warped cloth" })
                put("base64", true) // Indicate we're sending base64 data
                put("webhook", null)
                put("track_id", null)
            }
            
            Log.d("UnifiedImg2Img", "Request JSON created with base64 images")
            
            val result = makeApiCallWithPolling(
                url = "https://modelslab.com/api/v6/image_editing/fashion",
                jsonBody = jsonBody,
                modelName = "Fashion Try-On"
            )
            
            processApiResult(result)
            
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in fashion try-on", e)
            errorMessage = "Fashion Try-On Error: ${e.message}"
        }
    }
    
    private suspend fun performMultipleFaceSwap(base64InitImage: String, base64TargetImage: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing multiple face swap..."

        try {
            Log.d("UnifiedImg2Img", "Starting Multiple Face Swap")

            // Upload images to get URLs
            val initImageUrl = uploadImageToModelsLab(base64InitImage, "Init Image")
            val targetImageUrl = uploadImageToModelsLab(base64TargetImage, "Target Image")

            if (initImageUrl == null || targetImageUrl == null) {
                errorMessage = "Failed to upload images for face swap."
                return@withContext
            }

            val jsonBody = JSONObject().apply {
                put("key", MODELSLAB_API_KEY)
                put("init_image", initImageUrl)
                put("target_image", targetImageUrl)
                put("base64", false)
                put("webhook", null)
                put("track_id", null)
            }

            Log.d("UnifiedImg2Img", "Request JSON created for multiple face swap")

            val result = makeApiCallWithPolling(
                url = "https://modelslab.com/api/v6/deepfake/multiple_face_swap",
                jsonBody = jsonBody,
                modelName = "Multiple Face Swap"
            )

            processApiResult(result)
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in multiple face swap", e)
            errorMessage = "Multiple Face Swap Error: ${e.message}"
        }
    }




    private suspend fun performSketchImg2Img(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing sketch to realistic image..."

        val jsonBody = JSONObject().apply {
            put("key", MODELSLAB_API_KEY)
            put("init_image", base64Image)
            put("prompt", prompt.ifBlank { "a girl, wearing red bikini, looking at camera, ocean in background" })
            put("controlnet_model", "lineart")
            put("controlnet_type", "lineart")
            put("model_id", "boziorealvisxlv4")
            put("controlnet_conditioning_scale", "0.5")
            put("auto_hint", null)
            put("negative_prompt", negativePrompt.ifBlank { "(worst quality:2), (low quality:2), (normal quality:2), (jpeg artifacts), (blurry), (duplicate), (morbid), (mutilated), (out of frame), (extra limbs), (bad anatomy), (disfigured), (deformed), (cross-eye), (glitch), (oversaturated), (overexposed), (underexposed), (bad proportions), (bad hands), (bad feet), (cloned face), (long neck), (missing arms), (missing legs), (extra fingers), (fused fingers), (poorly drawn hands), (poorly drawn face), (mutation), (deformed eyes), watermark, text, logo, signature, grainy, tiling, censored, nsfw, ugly, blurry eyes, noisy image, bad lighting, unnatural skin, asymmetry" })
            put("samples", "1")
            put("guidance_scale", "7.5")
            put("num_inference_steps", "31")
            put("base64", true)
            put("webhook", null)
            put("track_id", null)
        }

        val result = makeApiCallWithPolling(
            url = "https://modelslab.com/api/v5/controlnet",
            jsonBody = jsonBody,
            modelName = "Sketch to Realistic"
        )

        processApiResult(result)
    }

    private suspend fun performAnimeStyle(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Applying anime style..."

        val jsonBody = JSONObject().apply {
            put("key", A4F_API_KEY)
            put("model", "anime-style-transfer")
            put("init_image", base64Image)
            put("prompt", prompt.ifBlank { "anime style, vibrant colors, cel shading" })
            put("strength", strength)
            put("guidance_scale", guidanceScale)
        }

        val result = makeApiCall(
            url = "https://api.a4f.com/v1/style-transfer",
            jsonBody = jsonBody,
            modelName = "Anime Style"
        )

        processApiResult(result)
    }

    private suspend fun performOilPaintingStyle(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Applying oil painting style..."
        
        val jsonBody = JSONObject().apply {
            put("key", A4F_API_KEY)
            put("model", "oil-painting-style")
            put("init_image", base64Image)
            put("prompt", prompt.ifBlank { "oil painting style, thick brushstrokes, impressionist" })
            put("strength", strength)
        }

        val result = makeApiCall(
            url = "https://api.a4f.com/v1/style-transfer",
            jsonBody = jsonBody,
            modelName = "Oil Painting Style"
        )
        
        processApiResult(result)
    }

    private suspend fun performWatercolorStyle(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Applying watercolor style..."
        
        val jsonBody = JSONObject().apply {
            put("key", A4F_API_KEY)
            put("model", "watercolor-style")
            put("init_image", base64Image)
            put("prompt", prompt.ifBlank { "watercolor painting, soft edges, translucent layers" })
            put("strength", strength)
        }

        val result = makeApiCall(
            url = "https://api.a4f.com/v1/style-transfer",
            jsonBody = jsonBody,
            modelName = "Watercolor Style"
        )
        
        processApiResult(result)
    }

    private suspend fun performSketchStyle(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Applying sketch style..."
        
        val jsonBody = JSONObject().apply {
            put("key", A4F_API_KEY)
            put("model", "sketch-style")
            put("init_image", base64Image)
            put("prompt", prompt.ifBlank { "pencil sketch, hand-drawn, artistic lines" })
            put("strength", strength)
        }

        val result = makeApiCall(
            url = "https://api.a4f.com/v1/style-transfer",
            jsonBody = jsonBody,
            modelName = "Sketch Style"
        )
        
        processApiResult(result)
    }

    private suspend fun performCyberpunkStyle(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Applying cyberpunk style..."
        
        val jsonBody = JSONObject().apply {
            put("key", A4F_API_KEY)
            put("model", "cyberpunk-style")
            put("init_image", base64Image)
            put("prompt", prompt.ifBlank { "cyberpunk style, neon lights, futuristic, high tech" })
            put("strength", strength)
            put("guidance_scale", guidanceScale)
        }

        val result = makeApiCall(
            url = "https://api.a4f.com/v1/style-transfer",
            jsonBody = jsonBody,
            modelName = "Cyberpunk Style"
        )
        
        processApiResult(result)
    }

    private suspend fun performFantasyArtStyle(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Applying fantasy art style..."
        
        val jsonBody = JSONObject().apply {
            put("key", A4F_API_KEY)
            put("model", "fantasy-art-style")
            put("init_image", base64Image)
            put("prompt", prompt.ifBlank { "fantasy art style, magical, ethereal, mystical atmosphere" })
            put("strength", strength)
            put("guidance_scale", guidanceScale)
        }

        val result = makeApiCall(
            url = "https://api.a4f.com/v1/style-transfer",
            jsonBody = jsonBody,
            modelName = "Fantasy Art Style"
        )
        
        processApiResult(result)
    }

    // Specialized Model implementations
    private suspend fun performPhotoEnhancement(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Enhancing photo..."
        
        val jsonBody = JSONObject().apply {
            put("key", A4F_API_KEY)
            put("model", "photo-enhance")
            put("init_image", base64Image)
            put("enhancement_type", "auto")
        }

        val result = makeApiCall(
            url = "https://api.a4f.com/v1/enhance",
            jsonBody = jsonBody,
            modelName = "Photo Enhancement"
        )
        
        processApiResult(result)
    }

    private suspend fun performColorization(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Colorizing image..."
        
        val jsonBody = JSONObject().apply {
            put("key", A4F_API_KEY)
            put("model", "colorization")
            put("init_image", base64Image)
        }

        val result = makeApiCall(
            url = "https://api.a4f.com/v1/colorize",
            jsonBody = jsonBody,
            modelName = "Colorization"
        )
        
        processApiResult(result)
    }

    private suspend fun performSuperResolution(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Upscaling image..."
        
        val jsonBody = JSONObject().apply {
            put("key", A4F_API_KEY)
            put("model", "super-resolution")
            put("init_image", base64Image)
            put("scale", "4x")
        }

        val result = makeApiCall(
            url = "https://api.a4f.com/v1/upscale",
            jsonBody = jsonBody,
            modelName = "Super Resolution"
        )
        
        processApiResult(result)
    }

    // Helper methods
    // A4F Flux Kontext Dev implementation
    private suspend fun performA4FFluxKontextDev(base64Image: String) = withContext(Dispatchers.IO) {
        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
            errorMessage = "Please set your A4F API Key"
            return@withContext
        }
        
        loadingMessage = "Processing with A4F Flux Kontext Dev..."
        
        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "image.png",
                    base64ToRequestBody(base64Image))
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("model", "provider-3/flux-kontext-dev")
                .build()
            
            val request = Request.Builder()
                .url("https://api.a4f.co/v1/images/edits")
                .addHeader("Authorization", "Bearer $A4F_API_KEY")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d("UnifiedImg2Img", "A4F Response code: ${response.code}")
            Log.d("UnifiedImg2Img", "A4F Response body: $responseBody")
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                
                // Check for data array (A4F format)
                if (jsonResponse.has("data")) {
                    val dataArray = jsonResponse.getJSONArray("data")
                    if (dataArray.length() > 0) {
                        val firstItem = dataArray.getJSONObject(0)
                        if (firstItem.has("url")) {
                            generatedImageUrl = firstItem.getString("url")
                            Log.d("UnifiedImg2Img", "A4F image URL: $generatedImageUrl")
                            return@withContext
                        }
                    }
                }
                
                errorMessage = "A4F API returned no image data"
            } else {
                val errorData = if (responseBody != null) {
                    try {
                        val errorJson = JSONObject(responseBody)
                        errorJson.optString("error", responseBody)
                    } catch (e: Exception) {
                        responseBody
                    }
                } else {
                    "Unknown error"
                }
                errorMessage = "A4F API Error: ${response.code} - $errorData"
            }
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in A4F API call", e)
            errorMessage = "A4F Error: ${e.message}"
        }
    }



    private suspend fun performA4FFluxKontextPro(base64Image: String) = withContext(Dispatchers.IO) {
        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
            errorMessage = "Please set your A4F API Key"
            return@withContext
        }

        loadingMessage = "Processing with A4F Flux Kontext Pro..."

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "image.png",
                    base64ToRequestBody(base64Image))
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("model", "provider-6/black-forest-labs-flux-1-kontext-pro")
                .build()

            val request = Request.Builder()
                .url("https://api.a4f.co/v1/images/edits")
                .addHeader("Authorization", "Bearer $A4F_API_KEY")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("UnifiedImg2Img", "A4F Response code: ${response.code}")
            Log.d("UnifiedImg2Img", "A4F Response body: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)

                // Check for data array (A4F format)
                if (jsonResponse.has("data")) {
                    val dataArray = jsonResponse.getJSONArray("data")
                    if (dataArray.length() > 0) {
                        val firstItem = dataArray.getJSONObject(0)
                        if (firstItem.has("url")) {
                            generatedImageUrl = firstItem.getString("url")
                            Log.d("UnifiedImg2Img", "A4F image URL: $generatedImageUrl")
                            return@withContext
                        }
                    }
                }

                errorMessage = "A4F API returned no image data"
            } else {
                val errorData = if (responseBody != null) {
                    try {
                        val errorJson = JSONObject(responseBody)
                        errorJson.optString("error", responseBody)
                    } catch (e: Exception) {
                        responseBody
                    }
                } else {
                    "Unknown error"
                }
                errorMessage = "A4F API Error: ${response.code} - $errorData"
            }
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in A4F API call", e)
            errorMessage = "A4F Error: ${e.message}"
        }
    }



    private suspend fun performA4FFluxKontextMax(base64Image: String) = withContext(Dispatchers.IO) {
        if (A4F_API_KEY == "YOUR_A4F_API_KEY_HERE" || A4F_API_KEY.isBlank()) {
            errorMessage = "Please set your A4F API Key"
            return@withContext
        }

        loadingMessage = "Processing with A4F Flux Kontext Max..."

        try {
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("image", "image.png",
                    base64ToRequestBody(base64Image))
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("model", "provider-6/black-forest-labs-flux-1-kontext-max")
                .build()

            val request = Request.Builder()
                .url("https://api.a4f.co/v1/images/edits")
                .addHeader("Authorization", "Bearer $A4F_API_KEY")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("UnifiedImg2Img", "A4F Response code: ${response.code}")
            Log.d("UnifiedImg2Img", "A4F Response body: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)

                // Check for data array (A4F format)
                if (jsonResponse.has("data")) {
                    val dataArray = jsonResponse.getJSONArray("data")
                    if (dataArray.length() > 0) {
                        val firstItem = dataArray.getJSONObject(0)
                        if (firstItem.has("url")) {
                            generatedImageUrl = firstItem.getString("url")
                            Log.d("UnifiedImg2Img", "A4F image URL: $generatedImageUrl")
                            return@withContext
                        }
                    }
                }

                errorMessage = "A4F API returned no image data"
            } else {
                val errorData = if (responseBody != null) {
                    try {
                        val errorJson = JSONObject(responseBody)
                        errorJson.optString("error", responseBody)
                    } catch (e: Exception) {
                        responseBody
                    }
                } else {
                    "Unknown error"
                }
                errorMessage = "A4F API Error: ${response.code} - $errorData"
            }
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in A4F API call", e)
            errorMessage = "A4F Error: ${e.message}"
        }
    }
    
    // Helper method to upload image to ModelsLab and get URL
    private suspend fun uploadImageToModelsLab(base64Image: String, imageType: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("UnifiedImg2Img", "Uploading $imageType image to ModelsLab...")
            
            val jsonBody = JSONObject().apply {
                put("key", MODELSLAB_API_KEY)
                put("image", base64Image)
            }
            
            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url("https://modelslab.com/api/v6/image/upload")
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val status = jsonResponse.optString("status", "")
                
                if (status == "success") {
                    val imageUrl = jsonResponse.optString("link", "")
                    if (imageUrl.isNotEmpty()) {
                        Log.d("UnifiedImg2Img", "$imageType image uploaded successfully: $imageUrl")
                        return@withContext imageUrl
                    }
                }
                
                Log.e("UnifiedImg2Img", "Upload failed: $responseBody")
            } else {
                Log.e("UnifiedImg2Img", "Upload failed with code: ${response.code}")
                Log.e("UnifiedImg2Img", "Response: $responseBody")
            }
            
            null
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error uploading $imageType image", e)
            null
        }
    }
    
    // Helper method to convert base64 to RequestBody
    private fun base64ToRequestBody(base64String: String): okhttp3.RequestBody {
        val decodedBytes = Base64.decode(base64String, Base64.DEFAULT)
        return decodedBytes.toRequestBody("image/png".toMediaTypeOrNull())
    }

    private suspend fun makeApiCall(
        url: String,
        jsonBody: JSONObject,
        modelName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            Log.d("UnifiedImg2Img", "$modelName Response code: ${response.code}")
            Log.d("UnifiedImg2Img", "$modelName Response body: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                
                // Check for direct output
                if (jsonResponse.has("output")) {
                    val outputValue = jsonResponse.get("output")
                    if (outputValue is String && outputValue.isNotEmpty()) {
                        return@withContext outputValue
                    } else if (outputValue is org.json.JSONArray && outputValue.length() > 0) {
                        return@withContext outputValue.getString(0)
                    }
                }
                
                // Check for data array (A4F format)
                if (jsonResponse.has("data")) {
                    val dataArray = jsonResponse.getJSONArray("data")
                    if (dataArray.length() > 0) {
                        val firstItem = dataArray.getJSONObject(0)
                        if (firstItem.has("url")) {
                            return@withContext firstItem.getString("url")
                        }
                    }
                }
                
                // Check status
                val status = jsonResponse.optString("status", "")
                when (status) {
                    "success" -> {
                        // Already checked output above
                        errorMessage = "Success but no image URL found"
                    }
                    "processing" -> {
                        // Some APIs return processing status in regular calls
                        val errorMsg = jsonResponse.optString("message", "Image is being processed")
                        
                        // Check if this indicates we need to poll
                        if (errorMsg.contains("Try to fetch request after") || 
                            errorMsg.contains("seconds estimated") ||
                            errorMsg.contains("processing")) {
                            errorMessage = "$modelName is processing. This model requires polling which is not implemented for this endpoint. Please try a different model."
                        } else {
                            errorMessage = "$modelName is processing: $errorMsg"
                        }
                    }
                    else -> {
                        val errorMsg = jsonResponse.optString("message",
                            jsonResponse.optString("error", "Unknown error"))
                        
                        // Check if this is actually a processing message
                        if (errorMsg.contains("Try to fetch request after") || 
                            errorMsg.contains("seconds estimated") ||
                            errorMsg.contains("processing")) {
                            errorMessage = "$modelName is processing. This model requires polling which is not implemented for this endpoint. Please try a different model."
                        } else {
                            errorMessage = "$modelName Error: $errorMsg"
                        }
                    }
                }
            } else {
                errorMessage = "$modelName Error: ${response.code} - ${response.message}"
            }
            
            null
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in $modelName API call", e)
            errorMessage = "$modelName Error: ${e.message}"
            null
        }
    }

    private suspend fun makeApiCallWithPolling(
        url: String,
        jsonBody: JSONObject,
        modelName: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            // Initial API call
            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            Log.d("UnifiedImg2Img", "$modelName Response code: ${response.code}")
            Log.d("UnifiedImg2Img", "$modelName Response body: $responseBody")

            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val status = jsonResponse.optString("status", "")
                
                // Check if we already have output
                if (jsonResponse.has("output")) {
                    val outputValue = jsonResponse.get("output")
                    if (outputValue is String && outputValue.isNotEmpty()) {
                        return@withContext outputValue
                    } else if (outputValue is org.json.JSONArray && outputValue.length() > 0) {
                        val imageUrl = outputValue.getString(0)
                        if (imageUrl.isNotEmpty() && !imageUrl.equals("null", ignoreCase = true)) {
                            return@withContext imageUrl
                        }
                    }
                }
                
                when (status) {
                    "success" -> {
                        // Already checked output above
                        errorMessage = "Success but no image found"
                        return@withContext null
                    }
                    "processing" -> {
                        val estimatedTime = jsonResponse.optDouble("eta", 30.0)
                        val fetchUrl = jsonResponse.optString("fetch_result", "")
                        
                        if (fetchUrl.isNotEmpty()) {
                            // Wait based on ETA
                            val initialWaitTime = (estimatedTime * 1000).toLong().coerceAtLeast(1000L)
                            delay(initialWaitTime)
                            
                            // Poll for result
                            val maxRetries = 60 // 5 minutes with 5 second intervals
                            val retryDelay = 5000L
                            
                            for (attempt in 1..maxRetries) {
                                withContext(Dispatchers.Main) {
                                    loadingMessage = "$modelName processing... ($attempt/$maxRetries)"
                                }
                                
                                val result = fetchProcessingResult(fetchUrl)
                                if (result != null) {
                                    return@withContext result
                                }
                                
                                if (attempt < maxRetries) {
                                    delay(retryDelay)
                                }
                            }
                            
                            errorMessage = "$modelName generation timed out"
                        } else {
                            errorMessage = "No fetch URL provided"
                        }
                    }
                    else -> {
                        val errorMsg = jsonResponse.optString("message", "Unknown error")
                        
                        // Check if this is a processing message that needs polling
                        if (errorMsg.contains("Try to fetch request after") || 
                            errorMsg.contains("seconds estimated") ||
                            errorMsg.contains("processing")) {
                            // This is actually a processing response, not an error
                            val fetchUrl = jsonResponse.optString("fetch_result", "")
                            if (fetchUrl.isNotEmpty()) {
                                // Wait a bit and start polling
                                delay(5000L) // Wait 5 seconds before first poll
                                
                                val maxRetries = 60 // 5 minutes with 5 second intervals
                                val retryDelay = 5000L
                                
                                for (attempt in 1..maxRetries) {
                                    withContext(Dispatchers.Main) {
                                        loadingMessage = "$modelName processing... ($attempt/$maxRetries)"
                                    }
                                    
                                    val result = fetchProcessingResult(fetchUrl)
                                    if (result != null) {
                                        return@withContext result
                                    }
                                    
                                    if (attempt < maxRetries) {
                                        delay(retryDelay)
                                    }
                                }
                                
                                errorMessage = "$modelName generation timed out after 5 minutes"
                            } else {
                                errorMessage = "Processing response received but no fetch URL provided"
                            }
                        } else {
                            errorMessage = "$modelName Error: $errorMsg"
                        }
                    }
                }
            } else {
                Log.e("UnifiedImg2Img", "$modelName HTTP Error: ${response.code}")
                Log.e("UnifiedImg2Img", "$modelName Error Response: $responseBody")
                
                // Try to parse error message from response
                if (responseBody != null) {
                    try {
                        val errorJson = JSONObject(responseBody)
                        val errorMsg = errorJson.optString("message", 
                            errorJson.optString("error", "HTTP Error ${response.code}"))
                        errorMessage = "$modelName Error: $errorMsg"
                    } catch (e: Exception) {
                        errorMessage = "$modelName HTTP Error: ${response.code} - ${response.message}"
                    }
                } else {
                    errorMessage = "$modelName HTTP Error: ${response.code} - ${response.message}"
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in $modelName API call", e)
            errorMessage = "$modelName Error: ${e.message}"
            null
        }
    }

    private suspend fun fetchProcessingResult(fetchUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val jsonBody = JSONObject().apply {
                put("key", MODELSLAB_API_KEY)
            }
            
            val requestBody = jsonBody.toString()
                .toRequestBody("application/json".toMediaType())
            
            val request = Request.Builder()
                .url(fetchUrl)
                .post(requestBody)
                .build()
            
            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()
            
            if (response.isSuccessful && responseBody != null) {
                val jsonResponse = JSONObject(responseBody)
                val status = jsonResponse.optString("status", "")
                
                when (status) {
                    "success" -> {
                        if (jsonResponse.has("output")) {
                            val outputValue = jsonResponse.get("output")
                            if (outputValue is String && outputValue.isNotEmpty()) {
                                return@withContext outputValue
                            } else if (outputValue is org.json.JSONArray && outputValue.length() > 0) {
                                return@withContext outputValue.getString(0)
                            }
                        }
                    }
                    "processing" -> {
                        // Still processing
                        return@withContext null
                    }
                    else -> {
                        val errorMsg = jsonResponse.optString("message", "Generation failed")
                        errorMessage = errorMsg
                        return@withContext null
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error fetching result", e)
            null
        }
    }

    private suspend fun processApiResult(result: String?) {
        Log.d("UnifiedImg2Img", "processApiResult called with: $result")
        if (result != null) {
            when {
                result.endsWith(".base64") -> {
                    Log.d("UnifiedImg2Img", "Processing base64 URL: $result")
                    // Fetch and decode base64 from URL
                    val base64Data = fetchBase64FromUrl(result)
                    if (base64Data != null) {
                        try {
                            Log.d("UnifiedImg2Img", "Base64 data length: ${base64Data.length}")
                            Log.d("UnifiedImg2Img", "Base64 data preview: ${base64Data.take(100)}...")
                            
                            // Check if this is a data URL (data:image/jpeg;base64,/9j/4AA...)
                            val actualBase64Data = if (base64Data.startsWith("data:image")) {
                                Log.d("UnifiedImg2Img", "Detected data URL format")
                                base64Data.substring(base64Data.indexOf(",") + 1)
                            } else {
                                base64Data
                            }
                            
                            Log.d("UnifiedImg2Img", "Actual base64 data length: ${actualBase64Data.length}")
                            Log.d("UnifiedImg2Img", "Actual base64 preview: ${actualBase64Data.take(50)}...")
                            
                            // Try different base64 decoding approaches
                            val imageBytes = try {
                                Base64.decode(actualBase64Data, Base64.DEFAULT)
                            } catch (e: Exception) {
                                Log.e("UnifiedImg2Img", "Failed to decode base64 with DEFAULT flag", e)
                                try {
                                    Base64.decode(actualBase64Data, Base64.NO_WRAP)
                                } catch (e2: Exception) {
                                    Log.e("UnifiedImg2Img", "Failed to decode base64 with NO_WRAP flag", e2)
                                    throw e2
                                }
                            }
                            
                            Log.d("UnifiedImg2Img", "Decoded bytes length: ${imageBytes.size}")
                            
                            // Try to decode with options for better memory management
                            val options = BitmapFactory.Options().apply {
                                inJustDecodeBounds = false
                                inSampleSize = 1
                                inPreferredConfig = Bitmap.Config.RGB_565 // Use less memory
                            }
                            
                            val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size, options)
                            if (bitmap != null) {
                                generatedImageBitmap = bitmap
                                Log.d("UnifiedImg2Img", "Successfully decoded base64 image: ${bitmap.width}x${bitmap.height}")
                            } else {
                                Log.e("UnifiedImg2Img", "BitmapFactory.decodeByteArray returned null")
                                Log.e("UnifiedImg2Img", "First few bytes: ${imageBytes.take(20).joinToString { "%02x".format(it) }}")
                                
                                // If we have a data URL, use it directly
                                if (base64Data.startsWith("data:image")) {
                                    Log.d("UnifiedImg2Img", "Using data URL directly")
                                    generatedImageUrl = base64Data
                                    errorMessage = null
                                } else {
                                    Log.d("UnifiedImg2Img", "Fallback: Using original URL as image source")
                                    generatedImageUrl = result
                                    errorMessage = null
                                }
                            }
                        } catch (e: Exception) {
                            Log.e("UnifiedImg2Img", "Failed to decode base64", e)
                            // If we have a data URL, use it as fallback
                            if (base64Data.startsWith("data:image")) {
                                Log.d("UnifiedImg2Img", "Exception fallback: Using data URL")
                                generatedImageUrl = base64Data
                                errorMessage = null
                            } else {
                                errorMessage = "Failed to decode generated image: ${e.message}"
                            }
                        }
                    } else {
                        errorMessage = "Failed to fetch base64 data from URL"
                    }
                }
                result.startsWith("data:image") -> {
                    Log.d("UnifiedImg2Img", "Processing data URL: ${result.take(50)}...")
                    // Direct base64 data URL
                    try {
                        val base64Data = result.substring(result.indexOf(",") + 1)
                        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        generatedImageBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                        Log.d("UnifiedImg2Img", "Successfully decoded data URL: ${generatedImageBitmap?.width}x${generatedImageBitmap?.height}")
                    } catch (e: Exception) {
                        Log.e("UnifiedImg2Img", "Failed to decode data URL", e)
                        errorMessage = "Failed to decode generated image"
                    }
                }
                else -> {
                    Log.d("UnifiedImg2Img", "Processing regular URL: $result")
                    // Regular image URL
                    generatedImageUrl = result
                    Log.d("UnifiedImg2Img", "Successfully set image URL: $result")
                }
            }
        } else {
            Log.d("UnifiedImg2Img", "processApiResult called with null result")
            if (errorMessage == null) {
                errorMessage = "Failed to generate image"
            }
        }
        
        // Debug the final state
        Log.d("UnifiedImg2Img", "Final state - URL: ${generatedImageUrl}, Bitmap: ${generatedImageBitmap?.let { "${it.width}x${it.height}" } ?: "null"}")
    }

    private suspend fun fetchBase64FromUrl(url: String): String? = withContext(Dispatchers.IO) {
        try {
            Log.d("UnifiedImg2Img", "Fetching base64 from URL: $url")
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            Log.d("UnifiedImg2Img", "Response code: ${response.code}")
            
            if (response.isSuccessful) {
                val responseBody = response.body?.string()
                Log.d("UnifiedImg2Img", "Response body length: ${responseBody?.length ?: 0}")
                Log.d("UnifiedImg2Img", "Response body preview: ${responseBody?.take(100) ?: "null"}...")
                return@withContext responseBody
            } else {
                Log.e("UnifiedImg2Img", "Failed to fetch base64. Response code: ${response.code}, message: ${response.message}")
            }
            null
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error fetching base64 from URL: $url", e)
            null
        }
    }

    private fun resizeBitmapIfNeeded(bitmap: Bitmap, maxDimension: Int = 1024): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        
        if (width <= maxDimension && height <= maxDimension) {
            return bitmap
        }
        
        val aspectRatio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / aspectRatio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * aspectRatio).toInt()
        }
        
        Log.d("UnifiedImg2Img", "Resizing from ${width}x${height} to ${newWidth}x${newHeight}")
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
        val imageBytes = outputStream.toByteArray()
        return Base64.encodeToString(imageBytes, Base64.NO_WRAP)
    }
    
    // Helper function to convert mask bitmap to PNG base64 (better for inpainting)
    private fun maskBitmapToPngBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        // Use PNG format for masks to preserve alpha channel properly
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        val imageBytes = outputStream.toByteArray()
        val base64String = Base64.encodeToString(imageBytes, Base64.NO_WRAP)
        Log.d("UnifiedImg2Img", "Converted mask to PNG base64, size: ${imageBytes.size} bytes")
        return base64String
    }
    
    // Helper function to ensure mask is properly formatted as binary black and white
    private fun ensureBinaryMask(mask: Bitmap): Bitmap {
        val width = mask.width
        val height = mask.height
        
        Log.d("UnifiedImg2Img", "Processing mask: ${width}x${height}, Config: ${mask.config}")
        
        // Create a new bitmap with RGB_565 config for better compatibility
        val binaryMask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        
        var whitePixels = 0
        var blackPixels = 0
        
        // Process each pixel to ensure it's either pure black or pure white
        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixel = mask.getPixel(x, y)
                
                // Extract ARGB components
                val alpha = (pixel shr 24) and 0xff
                val red = (pixel shr 16) and 0xff
                val green = (pixel shr 8) and 0xff
                val blue = pixel and 0xff
                
                // Calculate brightness using standard luminance formula
                val brightness = (0.299 * red + 0.587 * green + 0.114 * blue).toInt()
                
                // For V51 Inpainting: White pixels = areas to inpaint, Black pixels = areas to keep
                // If pixel has sufficient opacity and brightness, make it white (inpaint area)
                // Use a lower threshold for better mask detection
                val newPixel = if (alpha > 100 && brightness > 5) {
                    whitePixels++
                    0xFFFFFFFF.toInt() // Pure white with full opacity (inpaint this area)
                } else {
                    blackPixels++
                    0xFF000000.toInt() // Pure black with full opacity (keep this area)
                }
                binaryMask.setPixel(x, y, newPixel)
            }
        }
        
        Log.d("UnifiedImg2Img", "Created binary mask: ${width}x${height}")
        Log.d("UnifiedImg2Img", "White pixels (inpaint areas): $whitePixels")
        Log.d("UnifiedImg2Img", "Black pixels (keep areas): $blackPixels")
        Log.d("UnifiedImg2Img", "Mask coverage: ${(whitePixels * 100.0 / (width * height)).toInt()}%")
        
        // Validate that there are actually mask areas to inpaint
        if (whitePixels == 0) {
            Log.w("UnifiedImg2Img", "Warning: No white pixels found in mask. The mask may be invalid.")
        }
        
        return binaryMask
    }

    private fun calculateDimensions(bitmap: Bitmap): Pair<Int, Int> {
        val originalWidth = bitmap.width
        val originalHeight = bitmap.height
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        
        val maxDimension = 1024
        
        return if (aspectRatio > 1) {
            val width = maxDimension
            val height = (maxDimension / aspectRatio).toInt()
            Pair(width - (width % 8), height - (height % 8))
        } else {
            val height = maxDimension
            val width = (maxDimension * aspectRatio).toInt()
            Pair(width - (width % 8), height - (height % 8))
        }
    }

    private suspend fun performStabilityAIOutpaint() = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with Stability AI Outpaint..."

        try {
            // Create temporary file for image
            val imageFile = File.createTempFile("temp_image", ".png", context!!.cacheDir)

            // Save selected image to temp file
            val imageOutputStream = imageFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, imageOutputStream)
            imageOutputStream.close()

            // Call the outpaint API
            outpaintImage(imageFile)

            // Clean up temp file
            imageFile.delete()

        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Stability AI outpaint", e)
            errorMessage = "Stability AI Outpaint Error: ${e.localizedMessage}"
        }
    }

    private suspend fun outpaintImage(imageFile: File) = withContext(Dispatchers.IO) {
        loadingMessage = "Outpainting image..."

        try {
            val requestBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/png".toMediaTypeOrNull()))
                .addFormDataPart("output_format", "webp")

            // Add outpaint parameters only if they are greater than 0
            if (outpaintLeft > 0) {
                requestBodyBuilder.addFormDataPart("left", outpaintLeft.toString())
            }
            if (outpaintRight > 0) {
                requestBodyBuilder.addFormDataPart("right", outpaintRight.toString())
            }
            if (outpaintTop > 0) {
                requestBodyBuilder.addFormDataPart("up", outpaintTop.toString())
            }
            if (outpaintBottom > 0) {
                requestBodyBuilder.addFormDataPart("down", outpaintBottom.toString())
            }

            val requestBody = requestBodyBuilder.build()

            val request = Request.Builder()
                .url("https://api.stability.ai/v2beta/stable-image/edit/outpaint")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $STABILITY_API_KEY")
                .addHeader("accept", "image/*")
                .build()

            val response = client.newCall(request).execute()
            Log.d("UnifiedImg2Img", "Stability AI Outpaint API response code: ${response.code}")
            Log.d("UnifiedImg2Img", "Stability AI Outpaint API response message: ${response.message}")

            if (response.isSuccessful) {
                val responseBody = response.body?.bytes()
                if (responseBody != null) {
                    Log.d("UnifiedImg2Img", "Response body size: ${responseBody.size} bytes")
                    val bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.size)
                    if (bitmap != null) {
                        generatedImageBitmap = bitmap
                        Log.d("UnifiedImg2Img", "Generated outpainted bitmap: ${bitmap.width}x${bitmap.height}")
                    } else {
                        Log.e("UnifiedImg2Img", "Failed to decode bitmap from response")
                        errorMessage = "Failed to decode generated outpainted image"
                    }
                } else {
                    Log.e("UnifiedImg2Img", "Empty response body")
                    errorMessage = "Error: Empty response from server."
                }
            } else {
                val errorBody = response.body?.string()
                Log.e("UnifiedImg2Img", "Outpaint API Error: ${response.code} ${response.message}")
                Log.e("UnifiedImg2Img", "Error body: $errorBody")
                errorMessage = "Error: ${response.code} ${response.message}"
            }
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in outpaint API call", e)
            errorMessage = "Error: ${e.localizedMessage}"
        }
    }

    private suspend fun performStabilityAIInpaint() = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with Stability AI Inpaint..."

        try {
            // Create temporary files for image and mask
            val imageFile = File.createTempFile("temp_image", ".png", context!!.cacheDir)
            val maskFile = File.createTempFile("temp_mask", ".png", context!!.cacheDir)

            // Save selected image to temp file
            val imageOutputStream = imageFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, imageOutputStream)
            imageOutputStream.close()

            // Save mask to temp file
            val maskOutputStream = maskFile.outputStream()
            maskBitmap!!.compress(Bitmap.CompressFormat.PNG, 100, maskOutputStream)
            maskOutputStream.close()

            // Call the inpaint API
            inpaintImageWithMask(imageFile, maskFile)

            // Clean up temp files
            imageFile.delete()
            maskFile.delete()

        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Stability AI inpaint", e)
            errorMessage = "Stability AI Inpaint Error: ${e.localizedMessage}"
        }
    }

    private suspend fun inpaintImageWithMask(imageFile: File, maskFile: File) = withContext(Dispatchers.IO) {
        loadingMessage = "Inpainting image..."

        try {
            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/png".toMediaTypeOrNull()))
                .addFormDataPart("mask", maskFile.name, maskFile.asRequestBody("image/png".toMediaTypeOrNull()))
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("output_format", "webp")
                .build()

            val request = Request.Builder()
                .url("https://api.stability.ai/v2beta/stable-image/edit/inpaint")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $STABILITY_API_KEY")
                .addHeader("accept", "image/*")
                .build()

            val response = client.newCall(request).execute()
            Log.d("UnifiedImg2Img", "Stability AI Inpaint API response code: ${response.code}")
            Log.d("UnifiedImg2Img", "Stability AI Inpaint API response message: ${response.message}")

            if (response.isSuccessful) {
                val responseBody = response.body?.bytes()
                if (responseBody != null) {
                    Log.d("UnifiedImg2Img", "Response body size: ${responseBody.size} bytes")
                    val bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.size)
                    if (bitmap != null) {
                        generatedImageBitmap = bitmap
                        Log.d("UnifiedImg2Img", "Generated inpainted bitmap: ${bitmap.width}x${bitmap.height}")
                    } else {
                        Log.e("UnifiedImg2Img", "Failed to decode bitmap from response")
                        errorMessage = "Failed to decode generated inpainted image"
                    }
                } else {
                    Log.e("UnifiedImg2Img", "Empty response body")
                    errorMessage = "Error: Empty response from server."
                }
            } else {
                val errorBody = response.body?.string()
                Log.e("UnifiedImg2Img", "Inpaint API Error: ${response.code} ${response.message}")
                Log.e("UnifiedImg2Img", "Error body: $errorBody")
                errorMessage = "Error: ${response.code} ${response.message}"
            }
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in inpaint API call", e)
            errorMessage = "Error: ${e.localizedMessage}"
        }
    }

    fun reset() {
        selectedImage = null
        generatedImageBitmap = null
        generatedImageUrl = null
        prompt = ""
        negativePrompt = ""
        strength = 0.7f
        guidanceScale = 7.5f
        steps = 20
        errorMessage = null
        clearBatchImages()
        
        // Reset masking interface
        maskBitmap = null
        showMaskingInterface = false
        
        // Reset outpaint parameters
        outpaintLeft = 0
        outpaintRight = 0
        outpaintTop = 0
        outpaintBottom = 0
        
    }
    
    // Batch Processing Implementation Functions
    private suspend fun processBatchStyleTransfer(base64Image: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Use the anime style transfer as the base for batch style transfer
            val jsonBody = JSONObject().apply {
                put("key", A4F_API_KEY)
                put("model", "anime-style-transfer")
                put("init_image", base64Image)
                put("prompt", prompt.ifBlank { "anime style, vibrant colors, cel shading" })
                put("strength", strength)
                put("guidance_scale", guidanceScale)
            }

            val result = makeApiCall(
                url = "https://api.a4f.com/v1/style-transfer",
                jsonBody = jsonBody,
                modelName = "Batch Style Transfer"
            )
            
            if (result != null) {
                // Convert result to bitmap
                if (result.startsWith("data:image")) {
                    val base64Data = result.substring(result.indexOf(",") + 1)
                    val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    return@withContext BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } else {
                    // If it's a URL, we would need to download it
                    // For now, return null to use original image
                    return@withContext null
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e("BatchProcessing", "Error in batch style transfer", e)
            null
        }
    }
    
    private suspend fun processBatchEnhancement(base64Image: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Use the enhance API for batch enhancement
            val jsonBody = JSONObject().apply {
                put("key", MODELSLAB_API_KEY)
                put("init_image", base64Image)
                put("face_enhance", true)
                put("scale", 2)
                put("model_id", "ultra_resolution")
                put("base64", true)
            }

            val result = makeApiCallWithPolling(
                url = "https://modelslab.com/api/v6/image_editing/super_resolution",
                jsonBody = jsonBody,
                modelName = "Batch Enhancement"
            )
            
            if (result != null) {
                // Convert result to bitmap
                if (result.startsWith("data:image")) {
                    val base64Data = result.substring(result.indexOf(",") + 1)
                    val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    return@withContext BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                } else if (result.endsWith(".base64")) {
                    // Fetch base64 data from URL
                    val base64Data = fetchBase64FromUrl(result)
                    if (base64Data != null) {
                        val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                        return@withContext BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    }
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e("BatchProcessing", "Error in batch enhancement", e)
            null
        }
    }
    
    private suspend fun processBatchUpscale(base64Image: String): Bitmap? = withContext(Dispatchers.IO) {
        try {
            // Use the super resolution API for batch upscale
            val jsonBody = JSONObject().apply {
                put("key", A4F_API_KEY)
                put("model", "super-resolution")
                put("init_image", base64Image)
                put("scale", "4x")
            }

            val result = makeApiCall(
                url = "https://api.a4f.com/v1/upscale",
                jsonBody = jsonBody,
                modelName = "Batch Upscale"
            )
            
            if (result != null) {
                // Convert result to bitmap
                if (result.startsWith("data:image")) {
                    val base64Data = result.substring(result.indexOf(",") + 1)
                    val imageBytes = Base64.decode(base64Data, Base64.DEFAULT)
                    return@withContext BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                }
            }
            
            null
        } catch (e: Exception) {
            Log.e("BatchProcessing", "Error in batch upscale", e)
            null
        }
    }
    
    private suspend fun performStabilityAISketchToImage() = withContext(Dispatchers.IO) {
        loadingMessage = "Transforming sketch with Stability AI..."

        try {
            // Create temporary file for sketch image
            val imageFile = File.createTempFile("sketch_image", ".png", context!!.cacheDir)

            // Save selected image to temp file
            val imageOutputStream = imageFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, imageOutputStream)
            imageOutputStream.close()

            // Create the multipart request
            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/png".toMediaTypeOrNull()))
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("control_strength", controlStrength.toString())
                .addFormDataPart("output_format", "webp")
                .build()

            val request = Request.Builder()
                .url("https://api.stability.ai/v2beta/stable-image/control/sketch")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $STABILITY_API_KEY")
                .addHeader("accept", "image/*")
                .build()

            val response = client.newCall(request).execute()
            Log.d("UnifiedImg2Img", "Stability AI Sketch-to-Image API response code: ${response.code}")
            Log.d("UnifiedImg2Img", "Stability AI Sketch-to-Image API response message: ${response.message}")

            if (response.isSuccessful) {
                val responseBody = response.body?.bytes()
                if (responseBody != null) {
                    Log.d("UnifiedImg2Img", "Response body size: ${responseBody.size} bytes")
                    val bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.size)
                    if (bitmap != null) {
                        generatedImageBitmap = bitmap
                        Log.d("UnifiedImg2Img", "Generated sketch-to-image bitmap: ${bitmap.width}x${bitmap.height}")
                    } else {
                        Log.e("UnifiedImg2Img", "Failed to decode bitmap from response")
                        errorMessage = "Failed to decode generated image from sketch"
                    }
                } else {
                    Log.e("UnifiedImg2Img", "Empty response body")
                    errorMessage = "Error: Empty response from server."
                }
            } else {
                val errorBody = response.body?.string()
                Log.e("UnifiedImg2Img", "Sketch-to-Image API Error: ${response.code} ${response.message}")
                Log.e("UnifiedImg2Img", "Error body: $errorBody")
                errorMessage = "Error: ${response.code} ${response.message}"
            }

            // Clean up temp file
            imageFile.delete()

        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Stability AI sketch-to-image", e)
            errorMessage = "Stability AI Sketch-to-Image Error: ${e.localizedMessage}"
        }
    }
    
    private suspend fun performStabilityAIStructureControl() = withContext(Dispatchers.IO) {
        loadingMessage = "Controlling structure with Stability AI..."

        try {
            // Create temporary file for structure image
            val imageFile = File.createTempFile("structure_image", ".png", context!!.cacheDir)

            // Save selected image to temp file
            val imageOutputStream = imageFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, imageOutputStream)
            imageOutputStream.close()

            // Create the multipart request
            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/png".toMediaTypeOrNull()))
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("control_strength", controlStrength.toString())
                .addFormDataPart("output_format", "webp")
                .build()

            val request = Request.Builder()
                .url("https://api.stability.ai/v2beta/stable-image/control/structure")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $STABILITY_API_KEY")
                .addHeader("accept", "image/*")
                .build()

            val response = client.newCall(request).execute()
            Log.d("UnifiedImg2Img", "Stability AI Structure Control API response code: ${response.code}")
            Log.d("UnifiedImg2Img", "Stability AI Structure Control API response message: ${response.message}")

            if (response.isSuccessful) {
                val responseBody = response.body?.bytes()
                if (responseBody != null) {
                    Log.d("UnifiedImg2Img", "Response body size: ${responseBody.size} bytes")
                    val bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.size)
                    if (bitmap != null) {
                        generatedImageBitmap = bitmap
                        Log.d("UnifiedImg2Img", "Generated structure controlled bitmap: ${bitmap.width}x${bitmap.height}")
                    } else {
                        Log.e("UnifiedImg2Img", "Failed to decode bitmap from response")
                        errorMessage = "Failed to decode generated image from structure control"
                    }
                } else {
                    Log.e("UnifiedImg2Img", "Empty response body")
                    errorMessage = "Error: Empty response from server."
                }
            } else {
                val errorBody = response.body?.string()
                Log.e("UnifiedImg2Img", "Structure Control API Error: ${response.code} ${response.message}")
                Log.e("UnifiedImg2Img", "Error body: $errorBody")
                errorMessage = "Error: ${response.code} ${response.message}"
            }

            // Clean up temp file
            imageFile.delete()

        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Stability AI structure control", e)
            errorMessage = "Stability AI Structure Control Error: ${e.localizedMessage}"
        }
    }
    
    private suspend fun performStabilityAIStyleControl() = withContext(Dispatchers.IO) {
        loadingMessage = "Applying style control with Stability AI..."

        try {
            // Create temporary file for style control image
            val imageFile = File.createTempFile("style_control_image", ".png", context!!.cacheDir)

            // Save selected image to temp file
            val imageOutputStream = imageFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, imageOutputStream)
            imageOutputStream.close()

            // Create the multipart request
            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/png".toMediaTypeOrNull()))
                .addFormDataPart("prompt", prompt)
                .addFormDataPart("output_format", "webp")
                .build()

            val request = Request.Builder()
                .url("https://api.stability.ai/v2beta/stable-image/control/style")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $STABILITY_API_KEY")
                .addHeader("accept", "image/*")
                .build()

            val response = client.newCall(request).execute()
            Log.d("UnifiedImg2Img", "Stability AI Style Control API response code: ${response.code}")
            Log.d("UnifiedImg2Img", "Stability AI Style Control API response message: ${response.message}")

            if (response.isSuccessful) {
                val responseBody = response.body?.bytes()
                if (responseBody != null) {
                    Log.d("UnifiedImg2Img", "Response body size: ${responseBody.size} bytes")
                    val bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.size)
                    if (bitmap != null) {
                        generatedImageBitmap = bitmap
                        Log.d("UnifiedImg2Img", "Generated style controlled bitmap: ${bitmap.width}x${bitmap.height}")
                    } else {
                        Log.e("UnifiedImg2Img", "Failed to decode bitmap from response")
                        errorMessage = "Failed to decode generated image from style control"
                    }
                } else {
                    Log.e("UnifiedImg2Img", "Empty response body")
                    errorMessage = "Error: Empty response from server."
                }
            } else {
                val errorBody = response.body?.string()
                Log.e("UnifiedImg2Img", "Style Control API Error: ${response.code} ${response.message}")
                Log.e("UnifiedImg2Img", "Error body: $errorBody")
                errorMessage = "Error: ${response.code} ${response.message}"
            }

            // Clean up temp file
            imageFile.delete()

        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Stability AI style control", e)
            errorMessage = "Stability AI Style Control Error: ${e.localizedMessage}"
        }
    }
    
    private suspend fun performStabilityAIStyleTransfer() = withContext(Dispatchers.IO) {
        loadingMessage = "Performing style transfer with Stability AI..."

        try {
            // Create temporary file for init/content image
            val initImageFile = File.createTempFile("init_image", ".png", context!!.cacheDir)

            // Save selected image to temp file
            val initOutputStream = initImageFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, initOutputStream)
            initOutputStream.close()

            // Create temporary file for style image (required)
            val styleImageFile = File.createTempFile("style_image", ".png", context!!.cacheDir)
            val styleOutputStream = styleImageFile.outputStream()
            styleReferenceImage!!.compress(Bitmap.CompressFormat.PNG, 100, styleOutputStream)
            styleOutputStream.close()

            // Create the multipart request
            val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("init_image", initImageFile.name, initImageFile.asRequestBody("image/png".toMediaTypeOrNull()))
                .addFormDataPart("style_image", styleImageFile.name, styleImageFile.asRequestBody("image/png".toMediaTypeOrNull()))
                .addFormDataPart("output_format", "webp")
                .build()

            val request = Request.Builder()
                .url("https://api.stability.ai/v2beta/stable-image/control/style-transfer")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $STABILITY_API_KEY")
                .addHeader("accept", "image/*")
                .build()

            val response = client.newCall(request).execute()
            Log.d("UnifiedImg2Img", "Stability AI Style Transfer API response code: ${response.code}")
            Log.d("UnifiedImg2Img", "Stability AI Style Transfer API response message: ${response.message}")

            if (response.isSuccessful) {
                val responseBody = response.body?.bytes()
                if (responseBody != null) {
                    Log.d("UnifiedImg2Img", "Response body size: ${responseBody.size} bytes")
                    val bitmap = BitmapFactory.decodeByteArray(responseBody, 0, responseBody.size)
                    if (bitmap != null) {
                        generatedImageBitmap = bitmap
                        Log.d("UnifiedImg2Img", "Generated style transfer bitmap: ${bitmap.width}x${bitmap.height}")
                    } else {
                        Log.e("UnifiedImg2Img", "Failed to decode bitmap from response")
                        errorMessage = "Failed to decode generated image from style transfer"
                    }
                } else {
                    Log.e("UnifiedImg2Img", "Empty response body")
                    errorMessage = "Error: Empty response from server."
                }
            } else {
                val errorBody = response.body?.string()
                Log.e("UnifiedImg2Img", "Style Transfer API Error: ${response.code} ${response.message}")
                Log.e("UnifiedImg2Img", "Error body: $errorBody")
                errorMessage = "Error: ${response.code} ${response.message}"
            }

            // Clean up temp files
            initImageFile.delete()
            styleImageFile.delete()

        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Stability AI style transfer", e)
            errorMessage = "Stability AI Style Transfer Error: ${e.localizedMessage}"
        }
    }
    
    private suspend fun performStabilityAIUpscale() = withContext(Dispatchers.IO) {
        loadingMessage = "Upscaling image with Stability AI..."

        try {
            // Create temporary file for image
            val imageFile = File.createTempFile("upscale_image", ".png", context!!.cacheDir)

            // Save selected image to temp file
            val outputStream = imageFile.outputStream()
            selectedImage!!.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
            outputStream.close()

            // Create the multipart request
            val requestBodyBuilder = MultipartBody.Builder().setType(MultipartBody.FORM)
                .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/png".toMediaTypeOrNull()))
                .addFormDataPart("output_format", "webp")

            // Add prompt if available (optional for upscaling)
            if (prompt.isNotBlank()) {
                requestBodyBuilder.addFormDataPart("prompt", prompt)
            }

            val requestBody = requestBodyBuilder.build()

            val request = Request.Builder()
                .url("https://api.stability.ai/v2beta/stable-image/upscale/conservative")
                .post(requestBody)
                .addHeader("Authorization", "Bearer $STABILITY_API_KEY")
                .addHeader("accept", "image/*")
                .build()

            val response = client.newCall(request).execute()
            Log.d("UnifiedImg2Img", "Stability AI Upscale API response code: ${response.code}")
            Log.d("UnifiedImg2Img", "Stability AI Upscale API response message: ${response.message}")

            if (response.isSuccessful) {
                val responseBody = response.body?.bytes()
                if (responseBody != null) {
                    Log.d("UnifiedImg2Img", "Response body size: ${responseBody.size} bytes")
                    
                    // Always save to file first for WEBP format
                    val generatedFile = File.createTempFile("stability_upscaled", ".webp", context!!.cacheDir)
                    generatedFile.writeBytes(responseBody)
                    
                    // Try to decode the saved file
                    val bitmap = BitmapFactory.decodeFile(generatedFile.absolutePath)
                    if (bitmap != null) {
                        generatedImageBitmap = bitmap
                        // Keep the file URL as backup
                        generatedImageUrl = generatedFile.absolutePath
                        Log.d("UnifiedImg2Img", "Successfully upscaled image: ${bitmap.width}x${bitmap.height}")
                    } else {
                        // If bitmap decode fails, use file URL for display
                        generatedImageUrl = generatedFile.absolutePath
                        Log.d("UnifiedImg2Img", "Using file URL for display: $generatedImageUrl")
                    }
                } else {
                    Log.e("UnifiedImg2Img", "Empty response body")
                    errorMessage = "Error: Empty response from server."
                }
            } else {
                val errorBody = response.body?.string()
                Log.e("UnifiedImg2Img", "Upscale API Error: ${response.code} ${response.message}")
                Log.e("UnifiedImg2Img", "Error body: $errorBody")
                errorMessage = "Error: ${response.code} ${response.message}"
            }

            // Clean up temp file
            imageFile.delete()

        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in Stability AI upscale", e)
            errorMessage = "Stability AI Upscale Error: ${e.localizedMessage}"
        }
    }
    
    // Dual image mode methods for navigation parameter handling
    fun enableDualImageMode(enabled: Boolean) {
        isDualImageMode = enabled
        if (!enabled) {
            secondImage = null
        }
        Log.d("UnifiedImg2Img", "Dual image mode set to: $enabled")
    }
    
    fun enableGhibliMode(enabled: Boolean) {
        isGhibliMode = enabled
        if (enabled) {
            // Set Ghibli model when enabling Ghibli mode
            updateSelectedModel("ghibli-style")
        }
        Log.d("UnifiedImg2Img", "Ghibli mode set to: $enabled")
    }
    
    // Generate single image using nano-banana model (no second image)
    private suspend fun performNanoBananaSingleImage(base64Image: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with nano-banana model (single image)..."
        
        Log.d("UnifiedImg2Img", "Starting nano-banana single image generation")
        Log.d("UnifiedImg2Img", "Image size: ${base64Image.length} chars")
        Log.d("UnifiedImg2Img", "Prompt: $prompt")
        
        try {
            // Format the base64 image properly with data URI prefix
            val formattedImage = if (base64Image.startsWith("data:image")) {
                base64Image
            } else {
                "data:image/jpeg;base64,$base64Image"
            }
            
            // Use direct API call similar to dual image mode
            val jsonBody = JSONObject().apply {
                put("key", MODELSLAB_API_KEY)
                put("prompt", prompt)
                put("model_id", "nano-banana")
                put("init_image", formattedImage)
                // Don't include init_image_2 for single image mode
                put("width", "768")
                put("height", "1024")
                put("samples", "1")
                put("num_inference_steps", "25")
                put("guidance_scale", guidanceScale.toString())
                put("strength", strength.toString())
                put("negative_prompt", negativePrompt)
                put("seed", null)
                put("webhook", null)
                put("track_id", null)
            }
            
            Log.d("UnifiedImg2Img", "Using direct API for nano-banana single image")
            
            val result = makeApiCallWithPolling(
                url = "https://modelslab.com/api/v7/images/image-to-image",
                jsonBody = jsonBody,
                modelName = "Nano-Banana Single Image"
            )
            processApiResult(result)
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in nano-banana single image generation", e)
            errorMessage = "Nano-Banana Error: ${e.message}"
        }
    }
    
    // Generate dual image using nano-banana model
    private suspend fun performNanoBananaDualImage(base64Image1: String, base64Image2: String) = withContext(Dispatchers.IO) {
        loadingMessage = "Processing with nano-banana dual image model..."
        
        Log.d("UnifiedImg2Img", "Starting nano-banana dual image generation")
        Log.d("UnifiedImg2Img", "Image 1 size: ${base64Image1.length} chars")
        Log.d("UnifiedImg2Img", "Image 2 size: ${base64Image2.length} chars")
        Log.d("UnifiedImg2Img", "Prompt: $prompt")
        
        try {
            // First try uploading images to get URLs instead of using base64
            val imageUrl1 = uploadImageToModelsLab(base64Image1, "First Image")
            val imageUrl2 = uploadImageToModelsLab(base64Image2, "Second Image")
            
            if (imageUrl1 == null || imageUrl2 == null) {
                Log.e("UnifiedImg2Img", "Failed to upload images, trying with base64...")
                
                // Fallback to base64 with proper format
                val jsonBody = JSONObject().apply {
                    put("key", MODELSLAB_API_KEY)
                    put("prompt", prompt)
                    put("model_id", "nano-banana")
                    put("init_image", "data:image/jpeg;base64,$base64Image1")
                    put("init_image_2", "data:image/jpeg;base64,$base64Image2")
                    put("width", "768")
                    put("height", "1024")
                    put("samples", "1")
                    put("num_inference_steps", "25")
                    put("guidance_scale", "7.5")
                    put("seed", null)
                    put("webhook", null)
                    put("track_id", null)
                }
                
                Log.d("UnifiedImg2Img", "Using base64 format for nano-banana")
                
                val result = makeApiCallWithPolling(
                    url = "https://modelslab.com/api/v7/images/image-to-image",
                    jsonBody = jsonBody,
                    modelName = "Nano-Banana Dual Image (Base64)"
                )
                processApiResult(result)
            } else {
                Log.d("UnifiedImg2Img", "Using uploaded image URLs for nano-banana")
                
                // Use uploaded image URLs
                val jsonBody = JSONObject().apply {
                    put("key", MODELSLAB_API_KEY)
                    put("prompt", prompt)
                    put("model_id", "nano-banana")
                    put("init_image", imageUrl1)
                    put("init_image_2", imageUrl2)
                    put("width", "768")
                    put("height", "1024")
                    put("samples", "1")
                    put("num_inference_steps", "25")
                    put("guidance_scale", "7.5")
                    put("seed", null)
                    put("webhook", null)
                    put("track_id", null)
                }
                
                val result = makeApiCallWithPolling(
                    url = "https://modelslab.com/api/v7/images/image-to-image",
                    jsonBody = jsonBody,
                    modelName = "Nano-Banana Dual Image (URLs)"
                )
                processApiResult(result)
            }
        } catch (e: Exception) {
            Log.e("UnifiedImg2Img", "Error in nano-banana dual image generation", e)
            errorMessage = "Nano-Banana Error: ${e.message}"
        }
    }
    
    // Save generated image to history
    fun saveToHistory() {
        viewModelScope.launch {
            try {
                if (historyDataStore == null) {
                    Log.e("UnifiedImg2Img", "History data store not initialized")
                    return@launch
                }
                
                // Determine what to save based on what's available
                val imageData: Any? = when {
                    generatedImageBitmap != null -> generatedImageBitmap
                    generatedImageUrl != null -> generatedImageUrl
                    else -> null
                }
                
                if (imageData == null) {
                    Log.e("UnifiedImg2Img", "No generated image to save")
                    return@launch
                }
                
                // Save to history
                val historyItem = historyDataStore!!.saveImage(
                    prompt = prompt.ifEmpty { "Image to Image transformation" },
                    originalImage = selectedImage,
                    generatedImageData = imageData,
                    model = selectedModel
                )
                
                if (historyItem != null) {
                    Log.d("UnifiedImg2Img", "Successfully saved to history: ${historyItem.id}")
                } else {
                    Log.e("UnifiedImg2Img", "Failed to save to history")
                }
            } catch (e: Exception) {
                Log.e("UnifiedImg2Img", "Error saving to history", e)
            }
        }
    }
}
