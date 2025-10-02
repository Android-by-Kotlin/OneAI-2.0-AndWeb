package max.ohm.oneai.imagetovideo

import com.google.gson.annotations.SerializedName

// Note: Request and Response models are now using the existing ModelsLab models
// from max.ohm.oneai.videogeneration.modelslab package

/**
 * UI State for Image-to-Video generation
 */
sealed class ImageToVideoState {
    object Idle : ImageToVideoState()
    object Loading : ImageToVideoState()
    object Processing : ImageToVideoState()
    data class Success(val videoUrl: String, val generationTime: Double? = null) : ImageToVideoState()
    data class Error(val message: String) : ImageToVideoState()
}

/**
 * History item for generated videos
 */
data class ImageToVideoHistoryItem(
    val id: String,
    val initImageUrl: String,
    val prompt: String,
    val videoUrl: String,
    val timestamp: Long,
    val generationTime: Double? = null,
    val modelId: String = "seedance-i2v"
)

