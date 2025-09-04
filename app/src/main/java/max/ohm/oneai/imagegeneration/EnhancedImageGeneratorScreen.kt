package max.ohm.oneai.imagegeneration

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.*
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import androidx.compose.runtime.collectAsState
import android.util.Log
import max.ohm.oneai.utils.SetStatusBarColor
import max.ohm.oneai.utils.StatusBarUtils
import max.ohm.oneai.ui.theme.*
import max.ohm.oneai.components.AnimatedGlassOrb
import max.ohm.oneai.components.EmotionIntelligentButton
import max.ohm.oneai.components.AdaptiveGlassCard
import max.ohm.oneai.components.EmotionIntelligentTextField
import max.ohm.oneai.components.EmotionState
import max.ohm.oneai.components.EmotionStatusIndicator
import max.ohm.oneai.ui.theme.GlassCard
import max.ohm.oneai.ui.theme.PremiumGlassCard
import max.ohm.oneai.ui.theme.GlassAccentType
import max.ohm.oneai.imagegeneration.formatSecondsToMMSS
import max.ohm.oneai.imagegeneration.rememberStoragePermissionState
import max.ohm.oneai.audio.BackgroundMusicManager
import androidx.compose.runtime.DisposableEffect
import max.ohm.oneai.utils.ContentFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import max.ohm.oneai.R

// Data class for generated images history
data class GeneratedImage(
    val id: String = UUID.randomUUID().toString(),
    val prompt: String,
    val imageData: Any?, // ByteArray or URL
    val timestamp: Long = System.currentTimeMillis(),
    val model: String
)

// Helper function to get model logo resource
fun getModelLogoResource(modelName: String): Int? {
    return when {
        modelName.contains("FLUX", ignoreCase = true) || modelName.contains("Flux", ignoreCase = true) -> R.drawable.flux_logo
        modelName.contains("Qwen", ignoreCase = true) -> R.drawable.qwen_logo
        modelName.contains("ImageGen", ignoreCase = true) || modelName.contains("Imagen", ignoreCase = true) -> R.drawable.whgoog
        modelName.contains("DALL", ignoreCase = true) -> R.drawable.chatgpt_logo
        modelName.contains("Google", ignoreCase = true) -> R.drawable.brain_logo
        modelName.contains("ChatGPT", ignoreCase = true) -> R.drawable.chatgpt_logo
        modelName.contains("Gemini", ignoreCase = true) -> R.drawable.brain_logo
        modelName.contains("DeepSeek", ignoreCase = true) -> R.drawable.deepseek_logo
        modelName.contains("Perplexity", ignoreCase = true) -> R.drawable.perplexity_logo
        modelName.contains("Moonshot", ignoreCase = true) -> R.drawable.moonshot_logo
        modelName.contains("Nano Banana", ignoreCase = true) -> R.drawable.whgoog
        else -> null // Use fallback icon
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun EnhancedImageGeneratorScreen(
    unifiedImageViewModel: UnifiedImageViewModel = viewModel(),
    initialModelType: String? = null,
    onBackClick: () -> Unit = {}
) {
    val context = LocalContext.current
    val prompt = unifiedImageViewModel.prompt
    val generatedImageData = unifiedImageViewModel.generatedImageData
    val imageUrl = unifiedImageViewModel.imageUrl
    val isLoading = unifiedImageViewModel.isLoading
    val errorMessage = unifiedImageViewModel.errorMessage
    val selectedModelInternalName = unifiedImageViewModel.selectedModel
    
    // Initialize persistent storage
    val imageHistoryStore = remember { ImageHistoryDataStore(context) }
    
    // History of generated images from persistent storage
    val persistedHistory by imageHistoryStore.imageHistory.collectAsState(initial = emptyList())
    var imageHistory by remember { mutableStateOf(listOf<GeneratedImage>()) }
    var showGallery by remember { mutableStateOf(false) }
    var selectedImage by remember { mutableStateOf<GeneratedImage?>(null) }
    var showImageDetail by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportButtonClickTime by remember { mutableStateOf(0L) }
    var reportReason by remember { mutableStateOf("") }
    
    val elapsedTimeInSeconds by unifiedImageViewModel.elapsedTimeInSeconds.collectAsState()
    val totalGenerationTimeInSeconds by unifiedImageViewModel.totalGenerationTimeInSeconds.collectAsState()
    
    val coroutineScope = rememberCoroutineScope()
    
    // Model selection
    var modelMenuExpanded by remember { mutableStateOf(false) }
    val modelChoices = listOf(
        ModelChoice("Flux Dev", "provider-3/FLUX.1-dev"),
       // ModelChoice("Flux Schnell", "flux.1-schnell"),
      //   ModelChoice("Image-1", "provider-6/gpt-image-1"),
        ModelChoice("ImageGen-4", "google/imagen-4"),
        ModelChoice("ImageGen-3", "google/imagen-3"),
        ModelChoice("Qwen", "provider-4/qwen-image"),
//        ModelChoice("ImageGen-4", "provider-4/imagen-4"),
//        ModelChoice("ImageGen-3", "provider-4/imagen-3"),
      //  ModelChoice("DSLR Photograph", "modelslab/epic-realism"),
        ModelChoice("Nano Banana", "modelslab/nano-banana"),
      //  ModelChoice("FLUX Kontext Max", "provider-2/FLUX.1-kontext-max"),
       // ModelChoice("FLUX Kontext Pro", "provider-1/FLUX.1-kontext-pro"),
        //ModelChoice("Flux Pro Raw", "provider-3/FLUX.1.1-pro-ultra-raw"),
      //  ModelChoice("Flux Pro", "provider-1/FLUX.1.1-pro"),
       // ModelChoice("Flux Ultra Pro", "provider-3/FLUX.1.1-pro-ultra"),
        ModelChoice("DALL-E 3", "provider-3/dall-e-3"),
        ModelChoice("Shuttle 3.1 Aesthetic", "provider-3/shuttle-3.1-aesthetic"),
        ModelChoice("Shuttle 3 Diffusion", "provider-3/shuttle-3-diffusion")
        // ModelChoice("Shuttle Jaguar", "provider-3/shuttle-jaguar")
    )
    
    val currentSelectedModelChoice = modelChoices.find { it.internalName == selectedModelInternalName } 
        ?: modelChoices.first()
    
    // Animations
    val infiniteTransition = rememberInfiniteTransition(label = "loading_animation")
    
    val gradientRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "gradient_rotation"
    )
    
    // Initialize model and music manager
    LaunchedEffect(Unit) {
        // Set default model to Flux Dev - this ensures Flux Dev is always the default
        val defaultFluxDevModel = "provider-3/FLUX.1-dev"
        unifiedImageViewModel.updateSelectedModel(defaultFluxDevModel)
        
        // Double-check after a short delay to ensure the model is properly set to Flux Dev
        kotlinx.coroutines.delay(100)
        if (unifiedImageViewModel.selectedModel.isEmpty() || unifiedImageViewModel.selectedModel != defaultFluxDevModel) {
            unifiedImageViewModel.updateSelectedModel(defaultFluxDevModel)
        }
        
        BackgroundMusicManager.initialize(context)
    }
    
    LaunchedEffect(initialModelType) {
        initialModelType?.let {
            unifiedImageViewModel.updateSelectedModel(it)
        }
    }
    
    // Load persisted history
    LaunchedEffect(persistedHistory) {
        Log.d("EnhancedImageGenerator", "Loading ${persistedHistory.size} images from history")
        imageHistory = persistedHistory.mapNotNull { item ->
            val imageData = imageHistoryStore.getImageData(item)
            if (imageData != null) {
                GeneratedImage(
                    id = item.id,
                    prompt = item.prompt,
                    imageData = imageData,
                    timestamp = item.timestamp,
                    model = item.model
                )
            } else {
                Log.e("EnhancedImageGenerator", "Failed to load image data for id: ${item.id}")
                null
            }
        }
        Log.d("EnhancedImageGenerator", "Successfully loaded ${imageHistory.size} images")
    }
    
    // Add generated image to history and persist it
    LaunchedEffect(generatedImageData, imageUrl) {
        if (generatedImageData != null || imageUrl != null) {
            coroutineScope.launch {
                val savedItem = imageHistoryStore.saveImage(
                    prompt = prompt.text,
                    imageData = generatedImageData ?: imageUrl,
                    model = currentSelectedModelChoice.displayName
                )
                if (savedItem != null) {
                    Log.d("EnhancedImageGenerator", "Image saved successfully with id: ${savedItem.id}")
                } else {
                    Log.e("EnhancedImageGenerator", "Failed to save image")
                    Toast.makeText(context, "Failed to save image to gallery", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    
    // Show error messages
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            // Don't clear the error message immediately, let it be displayed in the UI
            // Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            // unifiedImageViewModel.clearErrorMessage()
        }
    }
    
    // Handle music playback based on loading state
    LaunchedEffect(isLoading) {
        if (isLoading) {
            // Start playing music when generation starts
            BackgroundMusicManager.startRandomMusic(context)
        } else {
            // Fade out and stop music when generation completes
            BackgroundMusicManager.fadeOut(duration = 2000L)
        }
    }
    
    // Clean up music when screen is disposed
    DisposableEffect(Unit) {
        onDispose {
            BackgroundMusicManager.stopMusic()
        }
    }
    
    // Set status bar color for image generation screen
    SetStatusBarColor(StatusBarUtils.ImageGenerationStatusBarColor)
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        DarkBackground.copy(alpha = 0.95f),
                        Color(0xFF0F172A)
                    )
                )
            )
    ) {
        // Animated background orbs
        repeat(3) { index ->
            val infiniteTransition = rememberInfiniteTransition(label = "orb_$index")
            val offsetX by infiniteTransition.animateFloat(
                initialValue = -200f + index * 300f,
                targetValue = 200f + index * 300f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 8000 + index * 2000,
                        easing = EaseInOutSine
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "orb_offset_x_$index"
            )
            val offsetY by infiniteTransition.animateFloat(
                initialValue = -100f + index * 200f,
                targetValue = 100f + index * 200f,
                animationSpec = infiniteRepeatable(
                    animation = tween(
                        durationMillis = 10000 + index * 1000,
                        easing = EaseInOutSine
                    ),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "orb_offset_y_$index"
            )
            
            Box(
                modifier = Modifier
                    .offset(x = offsetX.dp, y = offsetY.dp)
                    .size(200.dp + (index * 50).dp)
                    .background(
                        brush = Brush.radialGradient(
                            colors = when (index) {
                                0 -> listOf(
                                    GradientPurple.copy(alpha = 0.1f),
                                    Color.Transparent
                                )
                                1 -> listOf(
                                    GradientPink.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                                else -> listOf(
                                    GradientCyan.copy(alpha = 0.06f),
                                    Color.Transparent
                                )
                            }
                        ),
                        shape = CircleShape
                    )
                    .blur(60.dp)
            )
        }
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Enhanced Top Bar with Glass Effect (No Border)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    // Back button on the left
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier.align(Alignment.CenterStart)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    
                    // App Title with emotion indicator in the center
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "OneAI Studio",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary,
                            modifier = Modifier.graphicsLayer(alpha = 0.99f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = GradientPurple.copy(alpha = 0.1f),
                            modifier = Modifier.padding(top = 2.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.AutoAwesome,
                                    contentDescription = null,
                                    tint = GradientPurple,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Creation Mode",
                                    color = GradientPurple,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // Gallery button on the right
                    Box(
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        IconButton(
                            onClick = { showGallery = true }
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PhotoLibrary,
                                contentDescription = "Gallery",
                                tint = TextPrimary
                            )
                        }
                        if (imageHistory.isNotEmpty()) {
                            Badge(
                                modifier = Modifier.align(Alignment.TopEnd),
                                containerColor = AccentPink
                            ) {
                                Text("${imageHistory.size}")
                            }
                        }
                    }
                }
            }
            
            // Main Content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Enhanced Image Display with Premium Glass Effect
                PremiumGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .animateContentSize(),
                    accentType = GlassAccentType.Purple,
                    cornerRadius = 24.dp,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            isLoading -> {
                                LoadingAnimation(
                                    gradientRotation = gradientRotation,
                                    elapsedTime = elapsedTimeInSeconds
                                )
                            }
                            generatedImageData != null || imageUrl != null -> {
                                val storagePermissionState = rememberStoragePermissionState()
                                GeneratedImageDisplay(
                                    imageData = generatedImageData ?: imageUrl,
                                    prompt = prompt.text,
                                    onDownload = {
                                        if (storagePermissionState.hasPermission) {
                                            coroutineScope.launch {
                                                val success = downloadImage(context, generatedImageData ?: imageUrl, prompt.text)
                                                if (success) {
                                                    Toast.makeText(context, "Image saved to Downloads folder", Toast.LENGTH_SHORT).show()
                                                }
                                            }
                                        } else {
                                            storagePermissionState.requestPermission()
                                            Toast.makeText(context, "Storage permission required to download images", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    onShare = {
                                        coroutineScope.launch {
                                            shareImage(context, generatedImageData ?: imageUrl, prompt.text)
                                        }
                                    },
                                    onFullscreen = {
                                        selectedImage = imageHistory.firstOrNull()
                                        showImageDetail = true
                                    },
                                    onReport = {
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - reportButtonClickTime > 1000) { // Prevent double-click within 1 second
                                            reportButtonClickTime = currentTime
                                            showReportDialog = true
                                            android.util.Log.d("ReportDialog", "Report button clicked, showing dialog")
                                        }
                                    }
                                )
                            }
                            else -> {
                                EmptyStateDisplay()
                            }
                        }
                    }
                }
                
                // Error message display with enhanced warning for inappropriate content
                AnimatedVisibility(
                    visible = !isLoading && errorMessage != null,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    val isContentWarning = errorMessage?.contains("Play Store policies") == true
                    GlassCard(
                        backgroundColor = if (isContentWarning) {
                            Color(0xFFF59E0B).copy(alpha = 0.15f) // Orange for warnings
                        } else {
                            Color(0xFFDC2626).copy(alpha = 0.08f) // Red for errors
                        },
                        borderColor = if (isContentWarning) {
                            Color(0xFFF59E0B).copy(alpha = 0.4f)
                        } else {
                            Color(0xFFDC2626).copy(alpha = 0.25f)
                        },
                        cornerRadius = 16.dp,
                        modifier = Modifier.fillMaxWidth(),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isContentWarning) {
                                    Icons.Outlined.Warning
                                } else {
                                    Icons.Outlined.Error
                                },
                                contentDescription = null,
                                tint = if (isContentWarning) {
                                    Color(0xFFF59E0B)
                                } else {
                                    Color(0xFFDC2626)
                                },
                                modifier = Modifier.size(24.dp)
                            )
                            Column(
                                modifier = Modifier.weight(1f)
                            ) {
                                if (isContentWarning) {
                                    Text(
                                        text = "Content Policy Warning",
                                        color = Color(0xFFF59E0B),
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                }
                                Text(
                                    text = errorMessage ?: "",
                                    color = if (isContentWarning) {
                                        Color(0xFFF59E0B).copy(alpha = 0.9f)
                                    } else {
                                        Color(0xFFDC2626)
                                    },
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            IconButton(
                                onClick = { unifiedImageViewModel.clearErrorMessage() },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Dismiss",
                                    tint = if (isContentWarning) {
                                        Color(0xFFF59E0B)
                                    } else {
                                        Color(0xFFDC2626)
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Generation time display
                val currentTotalTime = totalGenerationTimeInSeconds
                val showGenerationTime = remember(isLoading, currentTotalTime) {
                    !isLoading && currentTotalTime != null && currentTotalTime > 0L
                }
                AnimatedVisibility(
                    visible = showGenerationTime,
                    enter = fadeIn() + slideInVertically(),
                    exit = fadeOut() + slideOutVertically()
                ) {
                    GlassCard(
                        backgroundColor = AccentPurple.copy(alpha = 0.08f),
                        borderColor = AccentPurple.copy(alpha = 0.25f),
                        cornerRadius = 12.dp,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Timer,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Generated in ${formatSecondsToMMSS(totalGenerationTimeInSeconds ?: 0L)}",
                                color = TextPrimary,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                // Recent generations preview
                if (imageHistory.isNotEmpty() && !showGallery) {
                    RecentGenerationsPreview(
                        images = imageHistory.take(5),
                        onImageClick = { image ->
                            selectedImage = image
                            showImageDetail = true
                        },
                        onViewAll = { showGallery = true }
                    )
                }
                
                // Model Selection
                ModelSelectionCard(
                    currentModel = currentSelectedModelChoice,
                    modelChoices = modelChoices,
                    expanded = modelMenuExpanded,
                    onExpandedChange = { modelMenuExpanded = it },
                    onModelSelected = { model ->
                        unifiedImageViewModel.updateSelectedModel(model.internalName)
                        modelMenuExpanded = false
                    }
                )
                
                // Enhanced Prompt Input with Emotion Intelligence
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.Transparent)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = null,
                                tint = TextSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = "Creative Prompt",
                                color = TextSecondary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        EmotionIntelligentTextField(
                            value = prompt.text,
                            onValueChange = { newText -> 
                                unifiedImageViewModel.updatePrompt(TextFieldValue(newText))
                                // Show real-time warning for inappropriate content
                                if (ContentFilter.containsAdultContent(newText)) {
                                    unifiedImageViewModel.errorMessage = "⚠️ This prompt may violate content policies"
                                } else if (unifiedImageViewModel.errorMessage?.contains("content policies") == true) {
                                    unifiedImageViewModel.clearErrorMessage()
                                }
                            },
                            emotion = EmotionState.Creative,
                            placeholder = "Describe your artistic vision (family-friendly content only)...",
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(
                                    width = 2.dp,
                                    brush = Brush.linearGradient(
                                        colors = if (ContentFilter.containsAdultContent(prompt.text)) {
                                            listOf(
                                                Color(0xFFF59E0B).copy(alpha = 0.8f),
                                                Color(0xFFDC2626).copy(alpha = 0.7f),
                                                Color(0xFFF59E0B).copy(alpha = 0.6f)
                                            )
                                        } else {
                                            listOf(
                                                GradientPurple.copy(alpha = 0.6f),
                                                GradientPink.copy(alpha = 0.7f),
                                                GradientCyan.copy(alpha = 0.5f),
                                                GradientPurple.copy(alpha = 0.4f)
                                            )
                                        }
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ),
                            maxLines = 5
                        )
                    }
                }
                
                // Generate Button with Dark Background
                Button(
                    onClick = { 
                        // Ensure we have a valid model selected, fallback to Flux Dev if needed
                        val currentModel = unifiedImageViewModel.selectedModel
                        if (currentModel.isBlank()) {
                            unifiedImageViewModel.updateSelectedModel("provider-3/FLUX.1-dev")
                        }
                        unifiedImageViewModel.generateImage()
                    },
                    enabled = !isLoading && prompt.text.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    GradientPurple.copy(alpha = 0.4f),
                                    GradientPink.copy(alpha = 0.3f),
                                    GradientCyan.copy(alpha = 0.3f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isLoading && prompt.text.isNotEmpty()) {
                            Color(0xFF1F2937).copy(alpha = 0.8f) // Dark background
                        } else {
                            Color(0xFF1F2937).copy(alpha = 0.4f) // Lighter when disabled
                        },
                        disabledContainerColor = Color(0xFF1F2937).copy(alpha = 0.4f)
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = TextPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = null,
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isLoading) "Generating..." else "Generate Art",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
                
                // Traffic notice and content policy reminder
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "⚠️ Some models may not work due to heavy traffic",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center
                    )
                    // Text(
                    //     text = "🛡️ This app enforces family-friendly content policies",
                    //     color = Color(0xFF10B981),
                    //     fontSize = 11.sp,
                    //     fontWeight = FontWeight.Normal,
                    //     textAlign = TextAlign.Center
                    // )
                }
            }
        }
        
        // Gallery Overlay
        AnimatedVisibility(
            visible = showGallery,
            enter = fadeIn() + slideInVertically(),
            exit = fadeOut() + slideOutVertically()
        ) {
            GalleryOverlay(
                images = imageHistory,
                onDismiss = { showGallery = false },
                onImageClick = { image ->
                    selectedImage = image
                    showImageDetail = true
                    showGallery = false
                },
                onDeleteImage = { image ->
                    coroutineScope.launch {
                        imageHistoryStore.deleteImage(image.id)
                    }
                }
            )
        }
        
        // Image Detail Overlay
        if (showImageDetail && selectedImage != null) {
            val storagePermissionStateForDialog = rememberStoragePermissionState()
            ImageDetailDialog(
                image = selectedImage!!,
                onDismiss = { showImageDetail = false },
                onDownload = {
                    if (storagePermissionStateForDialog.hasPermission) {
                        coroutineScope.launch {
                            val success = downloadImage(context, selectedImage!!.imageData, selectedImage!!.prompt)
                            if (success) {
                                Toast.makeText(context, "Image saved to Downloads folder", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        storagePermissionStateForDialog.requestPermission()
                        Toast.makeText(context, "Storage permission required to download images", Toast.LENGTH_SHORT).show()
                    }
                },
                onShare = {
                    coroutineScope.launch {
                        shareImage(context, selectedImage!!.imageData, selectedImage!!.prompt)
                    }
                }
            )
        }

        // Report Dialog
        if (showReportDialog) {
            android.util.Log.d("ReportDialog", "Showing ReportImageDialog")
            ReportImageDialog(
                onDismiss = { 
                    android.util.Log.d("ReportDialog", "Report dialog dismissed")
                    showReportDialog = false
                    reportReason = ""
                },
                onReport = { reason ->
                    // Handle report submission
                    android.util.Log.d("ReportDialog", "Report submitted with reason: $reason")
                    showReportDialog = false
                    reportReason = ""
                    Toast.makeText(context, "Report submitted successfully. Thank you for helping us improve.", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

@Composable
private fun LoadingAnimation(
    gradientRotation: Float,
    elapsedTime: Long
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { rotationZ = gradientRotation }
            ) {
                drawCircle(
                    brush = Brush.sweepGradient(
                        colors = listOf(
                            AccentPurple,
                            AccentPink,
                            AccentPurple.copy(alpha = 0.5f),
                            AccentPink.copy(alpha = 0.5f),
                            AccentPurple
                        )
                    ),
                    radius = size.minDimension / 2,
                    style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                )
            }
            
            Icon(
                imageVector = Icons.Outlined.AutoAwesome,
                contentDescription = null,
                tint = TextPrimary,
                modifier = Modifier.size(48.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Creating your masterpiece...",
            color = TextPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = formatSecondsToMMSS(elapsedTime),
            color = TextSecondary,
            fontSize = 16.sp
        )
    }
}

@Composable
private fun GeneratedImageDisplay(
    imageData: Any?,
    prompt: String,
    onDownload: () -> Unit,
    onShare: () -> Unit,
    onFullscreen: () -> Unit,
    onReport: () -> Unit = {}
) {
    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageData)
                .crossfade(true)
                .build(),
            contentDescription = "Generated Image",
            modifier = Modifier
                .fillMaxSize()
                .clickable { onFullscreen() },
            contentScale = ContentScale.Fit
        )
        
        // Action buttons overlay
        Row(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Report button (same as before)
            ActionButton(
                icon = Icons.Filled.Report,
                onClick = { 
                    // Show report dialog or handle report
                    onReport()
                },
                contentDescription = "Report",
                backgroundColor = Color(0xFFDC2626).copy(alpha = 0.8f),
                buttonSize = 28.dp
            )
            
            // Download button - styled like ImageToImage
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                IconButton(onClick = onDownload) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = "Download",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            
            // Share button - styled like ImageToImage
            Surface(
                modifier = Modifier.size(28.dp),
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.6f)
            ) {
                IconButton(onClick = onShare) {
                    Icon(
                        Icons.Outlined.Share,
                        contentDescription = "Share",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
        
        // Prompt overlay
        Surface(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(12.dp),
            color = Color.Black.copy(alpha = 0.7f),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = prompt,
                color = TextPrimary,
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(12.dp)
            )
        }
    }
}

@Composable
private fun ActionButton(
    icon: ImageVector,
    onClick: () -> Unit,
    contentDescription: String,
    backgroundColor: Color = Color.Black.copy(alpha = 0.6f),
    buttonSize: androidx.compose.ui.unit.Dp = 40.dp
) {
    Surface(
        modifier = Modifier.size(buttonSize),
        shape = CircleShape,
        color = backgroundColor,
        onClick = onClick
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = TextPrimary,
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        )
    }
}

@Composable
private fun EmptyStateDisplay() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
    ) {
        Box(
            modifier = Modifier.size(120.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            AccentPurple.copy(alpha = 0.2f),
                            Color.Transparent
                        )
                    )
                )
            }
            Icon(
                imageVector = Icons.Outlined.Image,
                contentDescription = null,
                tint = AccentPurple,
                modifier = Modifier.size(64.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "Ready to create",
            color = TextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Enter a prompt below and watch AI bring your ideas to life",
            color = TextSecondary,
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun RecentGenerationsPreview(
    images: List<GeneratedImage>,
    onImageClick: (GeneratedImage) -> Unit,
    onViewAll: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Creations",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            TextButton(onClick = onViewAll) {
                Text("View All", color = AccentPurple)
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(images) { image ->
                Card(
                    modifier = Modifier
                        .size(80.dp)
                        .clickable { onImageClick(image) },
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    AsyncImage(
                        model = image.imageData,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ModelSelectionCard(
    currentModel: ModelChoice,
    modelChoices: List<ModelChoice>,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onModelSelected: (ModelChoice) -> Unit
) {
    // Animation states
    val rotationAngle by animateFloatAsState(
        targetValue = if (expanded) 180f else 0f,
        animationSpec = tween(300, easing = FastOutSlowInEasing),
        label = "dropdown_rotation"
    )
    
    val borderAlpha by animateFloatAsState(
        targetValue = if (expanded) 0.8f else 0.3f,
        animationSpec = tween(300),
        label = "border_alpha"
    )
    
    // Premium Glass Card for Model Selection
    PremiumGlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
        accentType = GlassAccentType.Cyan,
        cornerRadius = 20.dp,
        contentPadding = PaddingValues(20.dp)
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header with icon and label
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Animated AI Icon
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    GradientCyan.copy(alpha = 0.2f),
                                    GradientPurple.copy(alpha = 0.2f)
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Psychology,
                        contentDescription = null,
                        tint = GradientCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
                
                Column {
                    Text(
                        text = "AI Model",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Select generation engine",
                        color = TextSecondary.copy(alpha = 0.6f),
                        fontSize = 10.sp
                    )
                }
            }
            
            // Enhanced Dropdown Button
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                GradientCyan.copy(alpha = borderAlpha),
                                GradientPurple.copy(alpha = borderAlpha),
                                GradientPink.copy(alpha = borderAlpha * 0.8f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                color = Color(0xFF1A1F3A).copy(alpha = 0.5f),
                onClick = { onExpandedChange(!expanded) }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        // Model Icon with Gradient Background
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        colors = when {
                                            currentModel.displayName.contains("FLUX") -> listOf(
                                                Color(0xFF6366F1).copy(alpha = 0.2f),
                                                Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                            )
                                            currentModel.displayName.contains("DALL") -> listOf(
                                                Color(0xFF06B6D4).copy(alpha = 0.2f),
                                                Color(0xFF0EA5E9).copy(alpha = 0.2f)
                                            )
                                            currentModel.displayName.contains("Shuttle") -> listOf(
                                                Color(0xFFEC4899).copy(alpha = 0.2f),
                                                Color(0xFFF59E0B).copy(alpha = 0.2f)
                                            )
                                            else -> listOf(
                                                GradientPurple.copy(alpha = 0.2f),
                                                GradientCyan.copy(alpha = 0.2f)
                                            )
                                        }
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            // Try to use model logo, fallback to icon if not available
                            val logoResource = getModelLogoResource(currentModel.displayName)
                            if (logoResource != null) {
                                Image(
                                    painter = painterResource(id = logoResource),
                                    contentDescription = currentModel.displayName,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    contentScale = ContentScale.Fit
                                )
                            } else {
                                Icon(
                                    imageVector = when {
                                        currentModel.displayName.contains("Pro") -> Icons.Outlined.WorkspacePremium
                                        currentModel.displayName.contains("Ultra") -> Icons.Outlined.Stars
                                        currentModel.displayName.contains("Max") -> Icons.Outlined.TipsAndUpdates
                                        else -> Icons.Outlined.AutoAwesome
                                    },
                                    contentDescription = null,
                                    tint = when {
                                        currentModel.displayName.contains("FLUX") -> Color(0xFF6366F1)
                                        currentModel.displayName.contains("DALL") -> Color(0xFF06B6D4)
                                        currentModel.displayName.contains("Shuttle") -> Color(0xFFEC4899)
                                        else -> GradientPurple
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        
                        Column(
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = currentModel.displayName,
                                color = TextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            // Model Type Badge
                            Text(
                                text = when {
                                    currentModel.displayName.contains("Ultra") -> "Premium • Ultra Quality"
                                    currentModel.displayName.contains("Pro") -> "Professional • High Quality"
                                    currentModel.displayName.contains("Max") -> "Maximum • Best Performance"
                                    currentModel.displayName.contains("Dev") -> "Development • Fast Generation"
                                    else -> "Standard • Balanced"
                                },
                                color = when {
                                    currentModel.displayName.contains("Ultra") || currentModel.displayName.contains("Pro") -> 
                                        GradientPink.copy(alpha = 0.9f)
                                    else -> TextSecondary.copy(alpha = 0.7f)
                                },
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Normal
                            )
                        }
                    }
                    
                    // Animated Arrow Icon
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = GradientCyan,
                        modifier = Modifier
                            .size(24.dp)
                            .graphicsLayer {
                                rotationZ = rotationAngle
                            }
                    )
                }
            }
            
            // Enhanced Dropdown Menu
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                ) + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF1A1F3A).copy(alpha = 0.95f),
                                    Color(0xFF0F172A).copy(alpha = 0.98f)
                                )
                            )
                        )
                        .border(
                            width = 1.dp,
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    GradientCyan.copy(alpha = 0.3f),
                                    GradientPurple.copy(alpha = 0.2f)
                                )
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    modelChoices.forEachIndexed { index, model ->
                        val isSelected = model == currentModel
                        
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 8.dp,
                                    vertical = if (index == 0) 8.dp else 2.dp
                                )
                                .animateContentSize(),
                            color = if (isSelected) {
                                GradientPurple.copy(alpha = 0.1f)
                            } else {
                                Color.Transparent
                            },
                            shape = RoundedCornerShape(12.dp),
                            onClick = {
                                onModelSelected(model)
                                onExpandedChange(false)
                            }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Selection Indicator with Animation
                                Box(
                                    modifier = Modifier.size(20.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = GradientPurple,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(20.dp)
                                                .border(
                                                    width = 2.dp,
                                                    color = TextSecondary.copy(alpha = 0.3f),
                                                    shape = CircleShape
                                                )
                                        )
                                    }
                                }
                                
                                // Model Icon
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            if (isSelected) {
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        GradientPurple.copy(alpha = 0.2f),
                                                        GradientCyan.copy(alpha = 0.2f)
                                                    )
                                                )
                                            } else {
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color.White.copy(alpha = 0.05f),
                                                        Color.White.copy(alpha = 0.02f)
                                                    )
                                                )
                                            }
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Try to use model logo, fallback to icon if not available
                                    val logoResource = getModelLogoResource(model.displayName)
                                    if (logoResource != null) {
                                        Image(
                                            painter = painterResource(id = logoResource),
                                            contentDescription = model.displayName,
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .let { modifier ->
                                                    if (!isSelected) {
                                                        modifier.alpha(0.7f)
                                                    } else modifier
                                                },
                                            contentScale = ContentScale.Fit,
                                            colorFilter = if (model.displayName.contains("Qwen") && !isSelected) {
                                                ColorFilter.tint(TextSecondary.copy(alpha = 0.7f), BlendMode.Modulate)
                                            } else null
                                        )
                                    } else {
                                        Icon(
                                            imageVector = when {
                                                model.displayName.contains("Pro") -> Icons.Outlined.WorkspacePremium
                                                model.displayName.contains("Ultra") -> Icons.Outlined.Stars
                                                model.displayName.contains("Max") -> Icons.Outlined.TipsAndUpdates
                                                model.displayName.contains("DALL") -> Icons.Outlined.Palette
                                                model.displayName.contains("Shuttle") -> Icons.Outlined.RocketLaunch
                                                else -> Icons.Outlined.AutoAwesome
                                            },
                                            contentDescription = null,
                                            tint = if (isSelected) {
                                                GradientPurple
                                            } else {
                                                TextSecondary.copy(alpha = 0.7f)
                                            },
                                            modifier = Modifier.size(22.dp)
                                        )
                                    }
                                }
                                
                                // Model Info
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = model.displayName,
                                        color = if (isSelected) TextPrimary else TextPrimary.copy(alpha = 0.9f),
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                                    )
                                    
                                    // Model Features/Tags
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Quality Badge
                                        val (badgeText, badgeColor) = when {
                                            model.displayName.contains("Ultra") -> "Ultra HD" to GradientPink
                                            model.displayName.contains("Pro") -> "Pro" to GradientPurple
                                            model.displayName.contains("Max") -> "Max" to GradientCyan
                                            model.displayName.contains("DALL") -> "Creative" to Color(0xFF06B6D4)
                                            model.displayName.contains("Shuttle") -> "Artistic" to Color(0xFFEC4899)
                                            model.displayName.contains("Dev") -> "Fast" to GradientGreen
                                            else -> "Realism" to GradientGreen
                                        }
                                        
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = badgeColor.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = badgeText,
                                                color = badgeColor,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                        
                                        // Performance indicator
                                        if (model.displayName.contains("Ultra") || model.displayName.contains("Pro")) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Outlined.Bolt,
                                                    contentDescription = null,
                                                    tint = Color(0xFFF59E0B),
                                                    modifier = Modifier.size(12.dp)
                                                )
                                                Text(
                                                    text = "Premium",
                                                    color = Color(0xFFF59E0B),
                                                    fontSize = 10.sp
                                                )
                                            }
                                        }
                                    }
                                }
                                
                                // Status indicator (if selected)
                                if (isSelected) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(GradientGreen)
                                    )
                                }
                            }
                        }
                        
                        // Add divider except for last item
                        if (index < modelChoices.size - 1) {
                            Divider(
                                color = Color.White.copy(alpha = 0.05f),
                                thickness = 0.5.dp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }
                    
                    // Bottom padding
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PromptInputCard(
    prompt: TextFieldValue,
    isLoading: Boolean,
    onPromptChange: (TextFieldValue) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Transparent)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Prompt",
                    color = TextSecondary,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            OutlinedTextField(
                value = prompt,
                onValueChange = onPromptChange,
                placeholder = {
                    Text(
                        "Describe your vision...",
                        color = TextSecondary.copy(alpha = 0.6f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    containerColor = DarkBackground,
                    cursorColor = AccentPurple,
                    focusedBorderColor = AccentPurple,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5
            )
        }
    }
}

@Composable
private fun GenerateButton(
    isLoading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = AccentPurple,
            disabledContainerColor = AccentPurple.copy(alpha = 0.5f)
        )
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = TextPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isLoading) "Generating..." else "Generate",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun GalleryOverlay(
    images: List<GeneratedImage>,
    onDismiss: () -> Unit,
    onImageClick: (GeneratedImage) -> Unit,
    onDeleteImage: (GeneratedImage) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = DarkBackground
    ) {
        Column {
            // Gallery Header - Transparent
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Transparent,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gallery",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextPrimary
                        )
                    }
                }
            }
            
            // Gallery Grid
            if (images.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ImageNotSupported,
                            contentDescription = null,
                            tint = TextSecondary,
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No images yet",
                            color = TextSecondary,
                            fontSize = 16.sp
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(images) { image ->
                        GalleryItem(
                            image = image,
                            onClick = { onImageClick(image) },
                            onDelete = { onDeleteImage(image) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GalleryItem(
    image: GeneratedImage,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            AsyncImage(
                model = image.imageData,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 200f
                        )
                    )
            )
            
            // Image info
            Column(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(12.dp)
            ) {
                Text(
                    text = image.prompt,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        .format(Date(image.timestamp)),
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
            
            // Delete button
            IconButton(
                onClick = { showDeleteConfirm = true },
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(8.dp)
                    .size(36.dp)
                    .background(
                        color = Color.Black.copy(alpha = 0.6f),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
    
    // Delete confirmation dialog
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Image?", color = TextPrimary) },
            text = { Text("This action cannot be undone.", color = TextSecondary) },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDelete()
                        showDeleteConfirm = false
                    }
                ) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = CardBackground,
            tonalElevation = 8.dp
        )
    }
}

@Composable
private fun ImageDetailDialog(
    image: GeneratedImage,
    onDismiss: () -> Unit,
    onDownload: () -> Unit,
    onShare: () -> Unit
) {
    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color.Black.copy(alpha = 0.9f)
        ) {
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
                // Zoomable image
                val state = rememberTransformableState { zoomChange, offsetChange, _ ->
                    scale = (scale * zoomChange).coerceIn(0.5f, 5f)
                    offset = offset + offsetChange
                }
                
                AsyncImage(
                    model = image.imageData,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                        .transformable(state = state)
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            }
                        },
                    contentScale = ContentScale.Fit
                )
                
                // Top bar with actions
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onDismiss) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close",
                                tint = Color.White
                            )
                        }
                        
//                        Row {
//                            IconButton(onClick = onShare) {
//                                Icon(
//                                    imageVector = Icons.Default.Share,
//                                    contentDescription = "Share",
//                                    tint = Color.White
//                                )
//                            }
//                            IconButton(onClick = onDownload) {
//                                Icon(
//                                    imageVector = Icons.Default.Download,
//                                    contentDescription = "Download",
//                                    tint = Color.White
//                                )
//                            }
//                        }
                    }
                }
                
                // Bottom info
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter),
                    color = Color.Black.copy(alpha = 0.7f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = image.prompt,
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                text = image.model,
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                            Text(
                                text = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                                    .format(Date(image.timestamp)),
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

// Helper functions
private suspend fun downloadImage(
    context: Context,
    imageData: Any?,
    fileName: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        Log.d("EnhancedImageDownload", "Starting download for image: $fileName")
        val bitmap = when (imageData) {
            is String -> {
                // URL
                Log.d("EnhancedImageDownload", "Downloading from URL: $imageData")
                val url = URL(imageData)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connectTimeout = 30000
                connection.readTimeout = 30000
                connection.connect()
                
                val responseCode = connection.responseCode
                Log.d("EnhancedImageDownload", "Response code: $responseCode")
                
                if (responseCode != HttpURLConnection.HTTP_OK) {
                    throw IOException("HTTP error code: $responseCode")
                }
                
                val input = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)
                input.close()
                connection.disconnect()
                
                if (bitmap == null) {
                    throw IOException("Failed to decode bitmap from stream")
                }
                bitmap
            }
            is ByteArray -> {
                Log.d("EnhancedImageDownload", "Using ByteArray data, size: ${imageData.size}")
                BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            }
            else -> {
                Log.e("EnhancedImageDownload", "Invalid image data type: ${imageData?.javaClass?.simpleName}")
                null
            }
        }
        
        bitmap?.let {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val fileName = "AI_Art_${timeStamp}.png"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // For Android 10 and above, use MediaStore
                Log.d("EnhancedImageDownload", "Using MediaStore for Android 10+")
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                    put(MediaStore.MediaColumns.IS_PENDING, 1)
                }
                
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { output ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    }
                    
                    // Mark as completed
                    contentValues.clear()
                    contentValues.put(MediaStore.MediaColumns.IS_PENDING, 0)
                    resolver.update(uri, contentValues, null, null)
                    
                    Log.d("EnhancedImageDownload", "Image saved successfully to: $uri")
                } else {
                    Log.e("EnhancedImageDownload", "Failed to create MediaStore entry")
                    return@withContext false
                }
            } else {
                // For Android 9 and below
                Log.d("EnhancedImageDownload", "Using legacy storage for Android 9 and below")
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                
                val imageFile = File(downloadsDir, fileName)
                FileOutputStream(imageFile).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
                Log.d("EnhancedImageDownload", "Image saved successfully to: ${imageFile.absolutePath}")
            }
            true
        } ?: false
    } catch (e: Exception) {
        Log.e("EnhancedImageDownload", "Error in downloadImage", e)
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_LONG).show()
        }
        false
    }
}

private suspend fun shareImage(
    context: Context,
    imageData: Any?,
    prompt: String
) = withContext(Dispatchers.IO) {
    try {
        val bitmap = when (imageData) {
            is String -> {
                // URL
                val url = URL(imageData)
                val connection = url.openConnection() as HttpURLConnection
                connection.doInput = true
                connection.connect()
                val input = connection.inputStream
                val bitmap = BitmapFactory.decodeStream(input)
                input.close()
                bitmap
            }
            is ByteArray -> {
                BitmapFactory.decodeByteArray(imageData, 0, imageData.size)
            }
            else -> null
        }
        
        bitmap?.let {
            val cachePath = File(context.cacheDir, "images")
            cachePath.mkdirs()
            
            val stream = FileOutputStream(File(cachePath, "shared_image.png"))
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            stream.close()
            
            val imagePath = File(context.cacheDir, "images/shared_image.png")
            val imageUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                imagePath
            )
            
            val shareIntent = Intent().apply {
                action = Intent.ACTION_SEND
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, imageUri)
                putExtra(Intent.EXTRA_TEXT, "Created with AI: $prompt")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            withContext(Dispatchers.Main) {
                context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
            }
        }
    } catch (e: Exception) {
        withContext(Dispatchers.Main) {
            Toast.makeText(context, "Failed to share image", Toast.LENGTH_SHORT).show()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReportImageDialog(
    onDismiss: () -> Unit,
    onReport: (String) -> Unit
) {
    var selectedReason by remember { mutableStateOf("") }
    var customReason by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }
    
    val reportReasons = listOf(
        "Inappropriate content",
        "Violence or harmful content",
        "Copyright infringement",
        "Spam or misleading content",
        "Other (specify)"
    )
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .padding(16.dp)
        ) {
            // Animated background with gradient and orbs - same as main screen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                DarkBackground,
                                DarkBackground.copy(alpha = 0.95f),
                                Color(0xFF0F172A)
                            )
                        )
                    )
            ) {
                // Animated background orbs
                repeat(3) { index ->
                    val infiniteTransition = rememberInfiniteTransition(label = "orb_dialog_$index")
                    val offsetX by infiniteTransition.animateFloat(
                        initialValue = -100f + index * 150f,
                        targetValue = 100f + index * 150f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 8000 + index * 2000,
                                easing = EaseInOutSine
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "orb_dialog_offset_x_$index"
                    )
                    val offsetY by infiniteTransition.animateFloat(
                        initialValue = -50f + index * 100f,
                        targetValue = 50f + index * 100f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(
                                durationMillis = 10000 + index * 1000,
                                easing = EaseInOutSine
                            ),
                            repeatMode = RepeatMode.Reverse
                        ),
                        label = "orb_dialog_offset_y_$index"
                    )
                    
                    Box(
                        modifier = Modifier
                            .offset(x = offsetX.dp, y = offsetY.dp)
                            .size(150.dp + (index * 30).dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = when (index) {
                                        0 -> listOf(
                                            GradientPurple.copy(alpha = 0.1f),
                                            Color.Transparent
                                        )
                                        1 -> listOf(
                                            GradientPink.copy(alpha = 0.08f),
                                            Color.Transparent
                                        )
                                        else -> listOf(
                                            GradientCyan.copy(alpha = 0.06f),
                                            Color.Transparent
                                        )
                                    }
                                ),
                                shape = CircleShape
                            )
                            .blur(40.dp)
                    )
                }
            }
            
            // Card content with transparent background
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Report Image",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextSecondary
                        )
                    }
                }
                
                Text(
                    text = "Help us maintain a safe community by reporting inappropriate content.",
                    color = TextSecondary,
                    fontSize = 14.sp
                )
                
                // Report reasons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Reason for reporting:",
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    
                    reportReasons.forEach { reason ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedReason = reason
                                    showCustomInput = reason == "Other (specify)"
                                }
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedReason == reason,
                                onClick = {
                                    selectedReason = reason
                                    showCustomInput = reason == "Other (specify)"
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = AccentPurple,
                                    unselectedColor = TextSecondary
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reason,
                                color = TextPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
                
                // Custom reason input
                AnimatedVisibility(visible = showCustomInput) {
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Please specify", color = TextSecondary) },
                        placeholder = { Text("Describe the issue...", color = TextSecondary.copy(alpha = 0.7f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = AccentPurple,
                            unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                            cursorColor = AccentPurple,
                            focusedLabelColor = AccentPurple,
                            unfocusedLabelColor = TextSecondary
                        ),
                        maxLines = 3,
                        minLines = 2
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = TextSecondary
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    
                    Button(
                        onClick = {
                            val finalReason = if (selectedReason == "Other (specify)") {
                                customReason.takeIf { it.isNotBlank() } ?: "Other"
                            } else {
                                selectedReason
                            }
                            if (finalReason.isNotBlank()) {
                                onReport(finalReason)
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        enabled = selectedReason.isNotBlank() && 
                                 (selectedReason != "Other (specify)" || customReason.isNotBlank()),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White,
                            disabledContainerColor = Color(0xFFDC2626).copy(alpha = 0.5f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Report",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }
        }
    }
}