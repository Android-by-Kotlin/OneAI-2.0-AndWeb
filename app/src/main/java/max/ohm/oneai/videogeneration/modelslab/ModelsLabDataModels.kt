package max.ohm.oneai.videogeneration.modelslab

import com.google.gson.annotations.SerializedName

// Request model for ModelsLab text2video API
data class ModelsLabVideoRequest(
    val key: String,
    val prompt: String,
    @SerializedName("output_type") val outputType: String = "mp4",
    @SerializedName("model_id") val modelId: String = "cogvideox",
    @SerializedName("negative_prompt") val negativePrompt: String = "blurry, low quality, distorted, extra limbs, missing limbs, broken fingers, deformed, glitch, artifacts, unrealistic, low resolution, bad anatomy, duplicate, cropped, watermark, text, logo, jpeg artifacts, noisy, oversaturated, underexposed, overexposed, flicker, unstable motion, motion blur, stretched, mutated, out of frame, bad proportions",
    @SerializedName("num_frames") val numFrames: String = "16",
    @SerializedName("num_inference_steps") val numInferenceSteps: String = "35",
    @SerializedName("guidance_scale") val guidanceScale: String = "7.5",
    @SerializedName("clip_skip") val clipSkip: String = "2",
    @SerializedName("upscale_height") val upscaleHeight: String = "1024",
    @SerializedName("upscale_width") val upscaleWidth: String = "1024",
    @SerializedName("upscale_strength") val upscaleStrength: String = "0.7",
    @SerializedName("upscale_guidance_scale") val upscaleGuidanceScale: String = "6.5",
    @SerializedName("upscale_num_inference_steps") val upscaleNumInferenceSteps: String = "40",
    @SerializedName("use_improved_sampling") val useImprovedSampling: String? = null,
    @SerializedName("improved_sampling_seed") val improvedSamplingSeed: String = "42",
    val fps: String = "16"
)

// Response model for ModelsLab video generation
data class ModelsLabVideoResponse(
    val status: String,
    val message: String?,
    @SerializedName("output") val output: List<String>?,  // Changed from outputUrls to output
    val outputUrls: List<String>? = null,  // Keep for backward compatibility
    val eta: Double?,
    @SerializedName("fetch_result") val fetchResultUrl: String?,
    val id: Long?,
    @SerializedName("generationTime") val generationTime: Double?,
    val error: String?
) {
    // Helper to get the actual video URLs regardless of which field is populated
    fun getVideoUrls(): List<String>? {
        return output ?: outputUrls
    }
}

// Model for async generation status check
data class ModelsLabStatusResponse(
    val status: String,
    val message: String?,
    @SerializedName("output") val output: List<String>?,  // Changed from outputUrls to output
    val outputUrls: List<String>? = null,  // Keep for backward compatibility
    val eta: Double?,
    @SerializedName("generationTime") val generationTime: Double?,
    val error: String?
) {
    // Helper to get the actual video URLs regardless of which field is populated
    fun getVideoUrls(): List<String>? {
        return output ?: outputUrls
    }
}

// Request model for ModelsLab image-to-video API (Seedance I2V)
data class ModelsLabImageToVideoRequest(
    val key: String,
    @SerializedName("model_id") val modelId: String = "seedance-i2v",
    @SerializedName("init_image") val initImage: String,
    val prompt: String,
    @SerializedName("negative_prompt") val negativePrompt: String = "blurry, low quality, distorted, extra limbs, missing limbs, broken fingers, deformed, glitch, artifacts, unrealistic, low resolution, bad anatomy, duplicate, cropped, watermark, text, logo, jpeg artifacts, noisy, oversaturated, underexposed, overexposed, flicker, unstable motion, motion blur, stretched, mutated, out of frame, bad proportions",
    @SerializedName("num_frames") val numFrames: String = "16",
    @SerializedName("num_inference_steps") val numInferenceSteps: String = "25",
    @SerializedName("guidance_scale") val guidanceScale: String = "7.5",
    @SerializedName("motion_bucket_id") val motionBucketId: String = "127",
    @SerializedName("fps") val fps: String = "6",
    @SerializedName("seed") val seed: String? = null,
    @SerializedName("webhook") val webhook: String? = null,
    @SerializedName("track_id") val trackId: String? = null
)

// Request for fetching Seedance generation status
data class SeedanceFetchRequest(
    val key: String
)

// Request model for ModelsLab text-to-video API (Seedance T2V)
data class ModelsLabTextToVideoRequest(
    val key: String,
    val prompt: String,
    @SerializedName("model_id") val modelId: String = "seedance-t2v",
    @SerializedName("aspect_ratio") val aspectRatio: String = "16:9",
    val resolution: String = "480p",  // Reduced from 720p for faster generation
    @SerializedName("camera_fixed") val cameraFixed: String? = null,
    @SerializedName("negative_prompt") val negativePrompt: String = "blurry, low quality, distorted, artifacts, bad anatomy",  // Simplified for faster processing
    @SerializedName("num_frames") val numFrames: String = "8",  // Reduced from 16 for faster generation
    @SerializedName("num_inference_steps") val numInferenceSteps: String = "20",  // Reduced from 25
    @SerializedName("guidance_scale") val guidanceScale: String = "7.0",  // Slightly reduced
    @SerializedName("fps") val fps: String = "16",  // Reduced from 24
    @SerializedName("seed") val seed: String? = null,
    @SerializedName("webhook") val webhook: String? = null,
    @SerializedName("track_id") val trackId: String? = null
)
