package max.ohm.oneai.imagetoimage

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.ZoomIn
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.*
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import max.ohm.oneai.imagetoimage.ui.BrushMaskTool
import max.ohm.oneai.stabilityai.ui.ImageComparisonView
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import max.ohm.oneai.utils.SetStatusBarColor
import max.ohm.oneai.utils.StatusBarUtils
import max.ohm.oneai.ui.theme.*
import max.ohm.oneai.components.AnimatedGlassOrb
import max.ohm.oneai.audio.BackgroundMusicManager
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import coil.request.ImageRequest
import max.ohm.oneai.imagetoimage.UnifiedImageToImageViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import android.content.ContentValues
import android.content.Intent
import android.os.Build
import android.os.Environment
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.net.HttpURLConnection
import max.ohm.oneai.utils.ContentFilter


//@Composable
//fun RegularImageToImageInputs(viewModel: UnifiedImageToImageViewModel) {
//    Column(modifier = Modifier.fillMaxWidth()) {
//        // Prompt Input
//        OutlinedTextField(
//            value = viewModel.prompt,
//            onValueChange = { viewModel.prompt = it },
//            label = { Text("Prompt") },
//            modifier = Modifier.fillMaxWidth(),
//            colors = OutlinedTextFieldDefaults.colors(
//                focusedBorderColor = Color(0xFF6366F1),
//                focusedLabelColor = Color(0xFF6366F1),
//                unfocusedBorderColor = Color(0xFF374151),
//                unfocusedLabelColor = Color(0xFF9CA3AF)
//            )
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Negative Prompt Input
//        OutlinedTextField(
//            value = viewModel.negativePrompt,
//            onValueChange = { viewModel.negativePrompt = it },
//            label = { Text("Negative Prompt") },
//            modifier = Modifier.fillMaxWidth(),
//            colors = OutlinedTextFieldDefaults.colors(
//                focusedBorderColor = Color(0xFF6366F1),
//                focusedLabelColor = Color(0xFF6366F1),
//                unfocusedBorderColor = Color(0xFF374151),
//                unfocusedLabelColor = Color(0xFF9CA3AF)
//            )
//        )
//
//        Spacer(modifier = Modifier.height(16.dp))
//
//        // Advanced Settings
//        Column(
//            modifier = Modifier.fillMaxWidth()
//        ) {
//            // Strength Slider
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Strength",
//                    color = Color.White,
//                    fontSize = 14.sp
//                )
//                Text(
//                    text = "%.1f".format(viewModel.strength),
//                    color = Color(0xFF6366F1),
//                    fontSize = 14.sp
//                )
//            }
//
//            Slider(
//                value = viewModel.strength,
//                onValueChange = { viewModel.strength = it },
//                valueRange = 0f..1f,
//                colors = SliderDefaults.colors(
//                    thumbColor = Color(0xFF6366F1),
//                    activeTrackColor = Color(0xFF6366F1),
//                    inactiveTrackColor = Color(0xFF374151)
//                )
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Guidance Scale Slider
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Guidance Scale",
//                    color = Color.White,
//                    fontSize = 14.sp
//                )
//                Text(
//                    text = "%.1f".format(viewModel.guidanceScale),
//                    color = Color(0xFF6366F1),
//                    fontSize = 14.sp
//                )
//            }
//
//            Slider(
//                value = viewModel.guidanceScale,
//                onValueChange = { viewModel.guidanceScale = it },
//                valueRange = 1f..20f,
//                colors = SliderDefaults.colors(
//                    thumbColor = Color(0xFF6366F1),
//                    activeTrackColor = Color(0xFF6366F1),
//                    inactiveTrackColor = Color(0xFF374151)
//                )
//            )
//
//            Spacer(modifier = Modifier.height(16.dp))
//
//            // Steps Slider
//            Row(
//                modifier = Modifier.fillMaxWidth(),
//                horizontalArrangement = Arrangement.SpaceBetween,
//                verticalAlignment = Alignment.CenterVertically
//            ) {
//                Text(
//                    text = "Steps",
//                    color = Color.White,
//                    fontSize = 14.sp
//                )
//                Text(
//                    text = viewModel.steps.toString(),
//                    color = Color(0xFF6366F1),
//                    fontSize = 14.sp
//                )
//            }
//
//            Slider(
//                value = viewModel.steps.toFloat(),
//                onValueChange = { viewModel.steps = it.toInt() },
//                valueRange = 1f..50f,
//                colors = SliderDefaults.colors(
//                    thumbColor = Color(0xFF6366F1),
//                    activeTrackColor = Color(0xFF6366F1),
//                    inactiveTrackColor = Color(0xFF374151)
//                )
//            )
//        }
//
//        Spacer(modifier = Modifier.height(16.dp))
//    }
//}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun ImageToImageScreen(
    viewModel: UnifiedImageToImageViewModel = viewModel(),
    onNavigateToGallery: () -> Unit = {}
) {
    val context = LocalContext.current
    LaunchedEffect(Unit) {
        viewModel.setContext(context)
        BackgroundMusicManager.initialize(context)
    }
    
    // Handle music playback based on loading state
    LaunchedEffect(viewModel.isLoading) {
        if (viewModel.isLoading) {
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

    val scrollState = rememberScrollState()
    var showAdvancedSettings by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    var showFullscreenImage by remember { mutableStateOf(false) }
    var expanded by remember { mutableStateOf(false) }
    var showReportDialog by remember { mutableStateOf(false) }
    var reportButtonClickTime by remember { mutableStateOf(0L) }
    
    // Gradient colors for Pro style
    val gradientColors = listOf(
        Color(0xFF6366F1),
        Color(0xFFEC4899)
    )
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                viewModel.updateSelectedImage(bitmap)
            } catch (e: Exception) {
                viewModel.errorMessage = "Failed to load image: ${e.message}"
            }
        }
    }
    
    // Camera launcher
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap: Bitmap? ->
        if (bitmap != null) {
            Toast.makeText(context, "Camera image captured: ${bitmap.width}x${bitmap.height}", Toast.LENGTH_LONG).show()
            viewModel.updateSelectedImage(bitmap)
        } else {
            Toast.makeText(context, "No image captured from camera", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Multiple image picker launcher for batch processing
    val multipleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (uris.isNotEmpty()) {
            val bitmaps = mutableListOf<Bitmap>()
            uris.forEach { uri ->
                try {
                    val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                    bitmaps.add(bitmap)
                } catch (e: Exception) {
                    Log.e("BatchProcessing", "Error loading image", e)
                }
            }
            if (bitmaps.isNotEmpty()) {
                viewModel.addBatchImages(bitmaps)
                Toast.makeText(context, "Added ${bitmaps.size} images to batch", Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    // Background reference image picker launcher
    val backgroundImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                viewModel.updateBackgroundReferenceUri(uri)
                Toast.makeText(context, "Background reference image selected", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                viewModel.errorMessage = "Failed to load background image: ${e.message}"
            }
        }
    }
    
    // Style reference image picker launcher
    val styleImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                viewModel.updateStyleReferenceImage(bitmap)
                Toast.makeText(context, "Style reference image selected", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                viewModel.errorMessage = "Failed to load style image: ${e.message}"
            }
        }
    }
    
    // Second image picker launcher for dual image models
    val secondImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                viewModel.updateSecondImage(bitmap)
                Toast.makeText(context, "Second image selected", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                viewModel.errorMessage = "Failed to load second image: ${e.message}"
            }
        }
    }
    
    // Clothing image picker launcher for fashion try-on
    val clothingImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                viewModel.updateClothingImage(bitmap)
                Toast.makeText(context, "Clothing image selected", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                viewModel.errorMessage = "Failed to load clothing image: ${e.message}"
            }
        }
    }
    
    // Target image picker launcher for face swap
    val targetImagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            try {
                val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uri)
                viewModel.updateTargetImage(bitmap)
                Toast.makeText(context, "Target face image selected", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                viewModel.errorMessage = "Failed to load target image: ${e.message}"
            }
        }
    }
    
    // Set status bar color for image to image screen
    SetStatusBarColor(StatusBarUtils.ImageToImageStatusBarColor)
    
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
        // Animated Background with Glass Orbs
        repeat(3) { index ->
            AnimatedGlassOrb(
                modifier = Modifier
                    .size((200 + index * 50).dp)
                    .offset(
                        x = (index * 150 - 100).dp, 
                        y = (index * 100 - 50).dp
                    ),
                colors = when (index) {
                    0 -> listOf(
                        Color(0xFF6366F1).copy(alpha = 0.15f),
                        Color.Transparent
                    )
                    1 -> listOf(
                        Color(0xFFEC4899).copy(alpha = 0.12f),
                        Color.Transparent
                    )
                    else -> listOf(
                        Color(0xFF10B981).copy(alpha = 0.1f),
                        Color.Transparent
                    )
                },
                size = (200 + index * 50).dp,
                duration = 8000 + index * 2000
            )
        }
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = { 
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = Color(0xFFEC4899),
                                modifier = Modifier.size(24.dp)
                            )
                            Text(
                                "OneAI Image Studio Pro",
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp
                            )
                            // Pro badge
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = Color(0xFFEC4899).copy(alpha = 0.2f),
                                modifier = Modifier.padding(start = 4.dp)
                            ) {
                                Text(
                                    "TRANSFORM",
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFEC4899)
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White
                    ),
                    windowInsets = WindowInsets(0, 0, 0, 0), // Remove all window insets padding
//                    actions = {
//                        // Gallery button
//                        IconButton(
//                            onClick = onNavigateToGallery,
//                            modifier = Modifier
//                                .size(40.dp)
//                                .background(
//                                    Color(0xFFEC4899).copy(alpha = 0.2f),
//                                    CircleShape
//                                )
//                        ) {
//                            Icon(
//                                Icons.Outlined.PhotoLibrary,
//                                contentDescription = "Gallery",
//                                tint = Color(0xFFEC4899)
//                            )
//                        }
//                    }
                )
            }
        ) { paddingValues ->
Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            
            // Batch Processing Status (show only when batch images are selected)
            if (viewModel.batchImages.isNotEmpty()) {
                PremiumGlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    accentType = GlassAccentType.Pink,
                    cornerRadius = 16.dp,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                Icons.Outlined.Collections,
                                contentDescription = null,
                                tint = Color(0xFFEC4899),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                "Batch Processing - ${viewModel.batchImages.size} images",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            TextButton(
                                onClick = { viewModel.clearBatchImages() }
                            ) {
                                Text(
                                    "Clear",
                                    color = Color(0xFFEC4899),
                                    fontSize = 12.sp
                                )
                            }
                        }
                        
                        // Progress indicator when processing
                        if (viewModel.isBatchProcessing) {
                            Column {
                                LinearProgressIndicator(
                                    progress = viewModel.batchProcessingProgress.toFloat() / viewModel.batchProcessingTotal.toFloat(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    color = Color(0xFFEC4899)
                                )
                                Text(
                                    viewModel.batchProcessingCurrentImage ?: "Processing...",
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }
            }
            
            // Model Selection Card with transparent background
            PremiumGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                accentType = GlassAccentType.Purple,
                cornerRadius = 16.dp,
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    // Model Selection Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "AI Model Selection",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    
                    // Regular image to image inputs
                   // RegularImageToImageInputs(viewModel)
                    
                    // Enhanced Model Selection Dropdown with Animation
                    val rotationAngle by animateFloatAsState(
                        targetValue = if (expanded) 180f else 0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "arrow_rotation"
                    )
                    
                    Column {
                        // Enhanced Trigger with Premium Styling
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded },
                            shape = RoundedCornerShape(12.dp),
                            color = if (expanded) {
                                Color(0xFF1A1F3A).copy(alpha = 0.6f)
                            } else {
                                Color.White.copy(alpha = 0.05f)
                            },
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (expanded) {
                                    Color(0xFF6366F1).copy(alpha = 0.5f)
                                } else {
                                    Color.White.copy(alpha = 0.2f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    // Dynamic Icon based on selected model
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = when {
                                                        viewModel.selectedModel.contains("flux") -> listOf(
                                                            Color(0xFF6366F1).copy(alpha = 0.2f),
                                                            Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                                        )
                                                        viewModel.selectedModel.contains("fashion") -> listOf(
                                                            Color(0xFFEC4899).copy(alpha = 0.2f),
                                                            Color(0xFF10B981).copy(alpha = 0.2f)
                                                        )
                                                        else -> listOf(
                                                            Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                                            Color(0xFF06B6D4).copy(alpha = 0.2f)
                                                        )
                                                    }
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = when {
                                                viewModel.selectedModel.contains("flux") -> Icons.Outlined.AutoAwesome
                                                viewModel.selectedModel.contains("fashion") -> Icons.Outlined.Checkroom
                                                viewModel.selectedModel.contains("face") -> Icons.Outlined.Face
                                                viewModel.selectedModel.contains("upscale") -> Icons.Outlined.ZoomIn
                                                else -> Icons.Outlined.ModelTraining
                                            },
                                            contentDescription = null,
                                            tint = when {
                                                viewModel.selectedModel.contains("flux") -> Color(0xFF6366F1)
                                                viewModel.selectedModel.contains("fashion") -> Color(0xFFEC4899)
                                                else -> Color(0xFF8B5CF6)
                                            },
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                    
                                    Spacer(modifier = Modifier.width(12.dp))
                                    
                                    Column(
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        val selectedModelName = viewModel.availableModels.find { it.first == viewModel.selectedModel }?.second ?: "Select Model"
                                        
                                        Text(
                                            text = selectedModelName,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        
                                        // Model Type Badge
                                        Text(
                                            text = when {
                                                viewModel.selectedModel.contains("flux") -> "Advanced • High Quality"
                                                viewModel.selectedModel.contains("fashion") -> "Fashion • Try-On"
                                                viewModel.selectedModel.contains("face") -> "Face • Swap"
                                                viewModel.selectedModel.contains("upscale") -> "Enhancement • 4x"
                                                viewModel.selectedModel.contains("remove") -> "Background • Removal"
                                                else -> "Standard • Transform"
                                            },
                                            color = when {
                                                viewModel.selectedModel.contains("flux") -> Color(0xFF8B5CF6).copy(alpha = 0.9f)
                                                else -> Color.White.copy(alpha = 0.7f)
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
                                    tint = Color(0xFF06B6D4),
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
                                    .padding(top = 8.dp)
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
                                                Color(0xFF06B6D4).copy(alpha = 0.3f),
                                                Color(0xFF8B5CF6).copy(alpha = 0.2f)
                                            )
                                        ),
                                        shape = RoundedCornerShape(16.dp)
                                    ),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                viewModel.availableModels.forEachIndexed { index, model ->
                                    val isSelected = model.first == viewModel.selectedModel
                                    
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(
                                                horizontal = 8.dp,
                                                vertical = if (index == 0) 8.dp else 2.dp
                                            )
                                            .animateContentSize(),
                                        color = if (isSelected) {
                                            Color(0xFF8B5CF6).copy(alpha = 0.1f)
                                        } else {
                                            Color.Transparent
                                        },
                                        shape = RoundedCornerShape(12.dp),
                                        onClick = {
                                            viewModel.updateSelectedModel(model.first)
                                            expanded = false
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
                                                        tint = Color(0xFF8B5CF6),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                } else {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .border(
                                                                width = 2.dp,
                                                                color = Color.White.copy(alpha = 0.3f),
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
                                                                    Color(0xFF8B5CF6).copy(alpha = 0.2f),
                                                                    Color(0xFF06B6D4).copy(alpha = 0.2f)
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
                                                Icon(
                                                    imageVector = when {
                                                        model.first.contains("flux") -> Icons.Outlined.AutoAwesome
                                                        model.first.contains("fashion") -> Icons.Outlined.Checkroom
                                                        model.first.contains("face") -> Icons.Outlined.Face
                                                        model.first.contains("upscale") -> Icons.Outlined.ZoomIn
                                                        model.first.contains("sketch") -> Icons.Outlined.Brush
                                                        model.first.contains("structure") -> Icons.Outlined.Architecture
                                                        model.first.contains("search") -> Icons.Outlined.Search
                                                        model.first.contains("remove") -> Icons.Outlined.RemoveCircleOutline
                                                        model.first.contains("style") -> Icons.Outlined.Palette
                                                        else -> Icons.Outlined.ModelTraining
                                                    },
                                                    contentDescription = null,
                                                    tint = if (isSelected) {
                                                        Color(0xFF8B5CF6)
                                                    } else {
                                                        Color.White.copy(alpha = 0.7f)
                                                    },
                                                    modifier = Modifier.size(22.dp)
                                                )
                                            }
                                            
                                            // Model Details
                                            Column(
                                                modifier = Modifier.weight(1f)
                                            ) {
                                                Text(
                                                    text = model.second,
                                                    color = if (isSelected) Color.White else Color.White.copy(alpha = 0.9f),
                                                    fontSize = 14.sp,
                                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                                )
                                                
                                                // Add special badges or descriptions
                                                when (model.first) {
                                                    "flux-kontext-pro-img2img" -> {
                                                        Row(
                                                            verticalAlignment = Alignment.CenterVertically,
                                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                                        ) {
                                                            Surface(
                                                                shape = RoundedCornerShape(4.dp),
                                                                color = Color(0xFFDC2626).copy(alpha = 0.15f),
                                                                border = BorderStroke(
                                                                    width = 0.5.dp,
                                                                    color = Color(0xFFDC2626).copy(alpha = 0.3f)
                                                                )
                                                            ) {
                                                                Text(
                                                                    "PREMIUM",
                                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                                                    fontSize = 9.sp,
                                                                    fontWeight = FontWeight.Bold,
                                                                    color = Color(0xFFDC2626)
                                                                )
                                                            }
                                                            Text(
                                                                "• High Quality",
                                                                fontSize = 10.sp,
                                                                color = Color.White.copy(alpha = 0.6f)
                                                            )
                                                        }
                                                    }
                                                    "fashion-try-on" -> {
                                                        Text(
                                                            "👗 Virtual clothing try-on",
                                                            fontSize = 10.sp,
                                                            color = Color.White.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                    "stability-ai-upscale" -> {
                                                        Text(
                                                            "4x resolution enhancement",
                                                            fontSize = 10.sp,
                                                            color = Color.White.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                    else -> {
                                                        Text(
                                                            when {
                                                                model.first.contains("face") -> "Face swapping technology"
                                                                model.first.contains("sketch") -> "Transform sketches to images"
                                                                model.first.contains("structure") -> "Preserve structural elements"
                                                                model.first.contains("search") -> "Smart object replacement"
                                                                model.first.contains("remove") -> "Clean background removal"
                                                                model.first.contains("style") -> "Artistic style transfer"
                                                                else -> "Image transformation"
                                                            },
                                                            fontSize = 10.sp,
                                                            color = Color.White.copy(alpha = 0.6f)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                    
                                    if (index < viewModel.availableModels.size - 1) {
                                        Divider(
                                            color = Color.White.copy(alpha = 0.1f),
                                            thickness = 0.5.dp,
                                            modifier = Modifier.padding(horizontal = 12.dp)
                                        )
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            }
            // Hero Section - Image Upload with Glass Morphism
            PremiumGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                accentType = GlassAccentType.Cyan,
                cornerRadius = 24.dp,
                contentPadding = PaddingValues(20.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Title Section - Dynamic based on state
                    AnimatedContent(
                        targetState = viewModel.generatedImageUrl != null || viewModel.generatedImageBitmap != null,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300))
                        }
                    ) { hasGeneratedImage ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(bottom = 16.dp)
                        ) {
                            if (hasGeneratedImage) {
                                Icon(
                                    Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF10B981),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Transformation Complete",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            } else {
                                Icon(
                                    Icons.Outlined.Transform,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Transform Your Vision",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                    
                    // Image Display Area - Shows comparison when generated
                    val hasGeneratedImage = viewModel.generatedImageUrl != null || viewModel.generatedImageBitmap != null
                    
                    AnimatedContent(
                        targetState = hasGeneratedImage,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(500)) + 
                            slideInHorizontally(animationSpec = tween(500)) togetherWith
                            fadeOut(animationSpec = tween(300))
                        },
                        label = "ImageDisplayTransition"
                    ) { showComparison ->
                        if (viewModel.selectedImage != null && showComparison) {
                            // Before/After Comparison View
                            Column {
                                Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(280.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Original Image
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black)
                                ) {
                                    Image(
                                        bitmap = viewModel.selectedImage!!.asImageBitmap(),
                                        contentDescription = "Original Image",
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(12.dp)),
                                        contentScale = ContentScale.Fit
                                    )
                                    
                                    GlassSurface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFF6366F1).copy(alpha = 0.7f),
                                        borderColor = Color.White.copy(alpha = 0.3f),
                                        borderWidth = 0.5.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            "Original",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }
                                
                                // Generated Image
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxHeight()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(Color.Black)
                                        .clickable { showFullscreenImage = true }
                                ) {
                                    // Show URL image if available
viewModel.generatedImageUrl?.let { imageUrl ->
    Log.d("ImageDisplay", "Showing URL image: $imageUrl")
    Image(
        painter = rememberAsyncImagePainter(
            ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build()
        ),
        contentDescription = "Generated Image",
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp)),
        contentScale = ContentScale.Fit
    )
}

// Show bitmap if URL is not available
viewModel.generatedImageBitmap?.let { bitmap ->
    Log.d("ImageDisplay", "Showing bitmap image: ${bitmap.width}x${bitmap.height}")
    if (viewModel.generatedImageUrl == null) {
        Image(
            bitmap = bitmap.asImageBitmap(),
            contentDescription = "Generated Image",
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Fit
        )
    }
}
                                    
                                    // Debug: Show a placeholder if no image is available
                                    if (viewModel.generatedImageUrl == null && viewModel.generatedImageBitmap == null) {
                                        Log.d("ImageDisplay", "No image available to display")
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                "No Image",
                                                color = Color.White,
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    
                                    GlassSurface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = Color(0xFFEC4899).copy(alpha = 0.7f),
                                        borderColor = Color.White.copy(alpha = 0.3f),
                                        borderWidth = 0.5.dp,
                                        modifier = Modifier
                                            .align(Alignment.TopStart)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            "Transformed",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                    
                                    // Report button for generated image
                                    GlassSurface(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                            .size(28.dp)
                                            .clickable { 
                                                val currentTime = System.currentTimeMillis()
                                                if (currentTime - reportButtonClickTime > 1000) { // Prevent double-click within 1 second
                                                    reportButtonClickTime = currentTime
                                                    showReportDialog = true
                                                    android.util.Log.d("ReportDialog", "Report button clicked, showing dialog")
                                                }
                                            },
                                        shape = RoundedCornerShape(14.dp),
                                        color = Color(0xFFDC2626).copy(alpha = 0.6f),
                                        borderColor = Color(0xFFDC2626).copy(alpha = 0.4f),
                                        borderWidth = 0.5.dp
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Report,
                                            contentDescription = "Report",
                                            tint = Color.White,
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(6.dp)
                                        )
                                    }
                                }
                            }
                            
                            // Generation time display
                            val totalTime = viewModel.totalGenerationTimeInSeconds.collectAsState().value
                            if (totalTime != null && totalTime > 0) {
                                GlassSurface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                                    borderColor = Color(0xFF10B981).copy(alpha = 0.25f),
                                    borderWidth = 1.dp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 16.dp, vertical = 8.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.Timer,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Generated in ${totalTime}s",
                                            color = Color(0xFF10B981),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                            
                            // Auto-saved to history - no button needed
                            
                            // Clear button
                            IconButton(
                                onClick = { 
                                    viewModel.updateSelectedImage(null)
                                    viewModel.reset()
                                },
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 8.dp)
                                    .size(36.dp)
                                    .background(
                                        Color.White.copy(alpha = 0.1f),
                                        CircleShape
                                    )
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Clear All",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    } else if (viewModel.selectedImage != null) {
                        // Single image view when no generation yet
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                        ) {
                            Image(
                                bitmap = viewModel.selectedImage!!.asImageBitmap(),
                                contentDescription = "Selected Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(16.dp)),
                                contentScale = ContentScale.Fit
                            )
                            
                            // Overlay controls
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .align(Alignment.TopCenter)
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(
                                                Color.Black.copy(alpha = 0.7f),
                                                Color.Transparent
                                            )
                                        )
                                    )
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                GlassSurface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.15f),
                                    borderColor = Color.White.copy(alpha = 0.3f)
                                ) {
                                    Text(
                                        "Original",
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                }
                                
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    // Brush tool button - show for mask erase, inpaint, V51 inpainting, and object removal models
                                    if (viewModel.selectedModel == "stability-ai-mask-erase" || 
                                        viewModel.selectedModel == "stability-ai-inpaint" ||
                                        viewModel.selectedModel == "v51-inpainting" ||
                                        viewModel.selectedModel == "object-removal") {
                                        IconButton(
                                            onClick = { viewModel.toggleMaskingInterface() },
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(
                                                    Color(0xFFEC4899).copy(alpha = 0.2f),
                                                    CircleShape
                                                )
                                        ) {
                                            Icon(
                                                Icons.Default.Brush,
                                                contentDescription = "Brush Tool",
                                                tint = Color(0xFFEC4899),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                    }
                                    
                                    IconButton(
                                        onClick = { viewModel.updateSelectedImage(null) },
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(
                                                Color.White.copy(alpha = 0.1f),
                                                CircleShape
                                            )
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear Image",
                                            tint = Color.White,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        // Image Upload Area - Matching Cyan Theme
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(
                                    Brush.verticalGradient(
                                        colors = listOf(
                                            Color(0xFF06B6D4).copy(alpha = 0.08f), // Cyan tint
                                            Color(0xFF10B981).copy(alpha = 0.06f), // Green tint
                                            Color(0xFF0A0E27).copy(alpha = 0.9f)   // Dark base
                                        )
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.Center,
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Main image upload button with animation
                                val infiniteTransition = rememberInfiniteTransition(label = "imageButton")
                                val pulseAnimation by infiniteTransition.animateFloat(
                                    initialValue = 0.9f,
                                    targetValue = 1.1f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(2000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "pulse"
                                )
                                
                                val glowAnimation by infiniteTransition.animateFloat(
                                    initialValue = 0.3f,
                                    targetValue = 0.8f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(2000, easing = FastOutSlowInEasing),
                                        repeatMode = RepeatMode.Reverse
                                    ),
                                    label = "glow"
                                )
                                
                                Box(
                                    modifier = Modifier
                                        .size(140.dp)
                                        .scale(pulseAnimation)
                                        .clickable { imagePickerLauncher.launch("image/*") },
                                    contentAlignment = Alignment.Center
                                ) {
                                    // Static gradient background with glow effect
                                    Box(
                                        modifier = Modifier
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(24.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFF10B981).copy(alpha = glowAnimation),
                                                        Color(0xFF6366F1),
                                                        Color(0xFFEC4899).copy(alpha = glowAnimation)
                                                    ),
                                                    start = androidx.compose.ui.geometry.Offset(0f, 0f),
                                                    end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
                                                )
                                            )
                                    )
                                    
                                    // Inner box with icon
                                    Box(
                                        modifier = Modifier
                                            .size(130.dp)
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                Brush.linearGradient(
                                                    colors = listOf(
                                                        Color(0xFF0A0E27).copy(alpha = 0.95f),
                                                        Color(0xFF1A1F3A).copy(alpha = 0.95f)
                                                    )
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.AddPhotoAlternate,
                                                contentDescription = "Choose Image",
                                                modifier = Modifier.size(48.dp),
                                                tint = Color.White
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Gallery",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                                
                                Spacer(modifier = Modifier.height(24.dp))
                                
                                Text(
                                    "Transform Your Image",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                
                                Spacer(modifier = Modifier.height(8.dp))
                                
                                Text(
                                    "Upload an image to begin transformation",
                                    fontSize = 14.sp,
                                    color = Color.White.copy(alpha = 0.7f),
                                    textAlign = TextAlign.Center
                                )
                                
                                Spacer(modifier = Modifier.height(32.dp))
                                
                    // Secondary options row
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Camera option - Cyan theme
                                    OutlinedCard(
                                        onClick = { cameraLauncher.launch(null) },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = Color(0xFF06B6D4).copy(alpha = 0.4f) // Cyan border
                                        ),
                                        colors = CardDefaults.outlinedCardColors(
                                            containerColor = Color(0xFF06B6D4).copy(alpha = 0.08f) // Cyan background
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.PhotoCamera,
                                                contentDescription = "Camera",
                                                modifier = Modifier.size(24.dp),
                                                tint = Color(0xFF06B6D4) // Cyan icon
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Camera",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                    
                                    // Batch processing option - Green theme
                                    OutlinedCard(
                                        onClick = { 
                                            // Launch multiple image picker for batch processing
                                            multipleImagePickerLauncher.launch("image/*")
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(80.dp),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = Color(0xFF10B981).copy(alpha = 0.4f) // Green border
                                        ),
                                        colors = CardDefaults.outlinedCardColors(
                                            containerColor = Color(0xFF10B981).copy(alpha = 0.08f) // Green background
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(12.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Icon(
                                                Icons.Outlined.Collections,
                                                contentDescription = "Batch",
                                                modifier = Modifier.size(24.dp),
                                                tint = Color(0xFF10B981) // Green icon
                                            )
                                            Spacer(modifier = Modifier.height(4.dp))
                                            Text(
                                                "Batch",
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.8f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    }
                }
            }
            
            // Transformation Settings Card with Glass Morphism
            PremiumGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                accentType = GlassAccentType.Neutral,
                cornerRadius = 20.dp,
                contentPadding = PaddingValues(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Prompt Section Header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Transformation Instructions",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                    
                    // Mask status indicator for mask erase, inpaint, V51 inpainting, and object removal models
                    if (viewModel.selectedModel == "stability-ai-mask-erase" || 
                        viewModel.selectedModel == "stability-ai-inpaint" ||
                        viewModel.selectedModel == "v51-inpainting" ||
                        viewModel.selectedModel == "object-removal") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (viewModel.maskBitmap != null) 
                                    Color(0xFF10B981).copy(alpha = 0.2f) 
                                else 
                                    Color(0xFFEF4444).copy(alpha = 0.2f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (viewModel.maskBitmap != null) 
                                        Icons.Default.CheckCircle 
                                    else 
                                        Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = if (viewModel.maskBitmap != null) 
                                        Color(0xFF10B981) 
                                    else 
                                        Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    if (viewModel.maskBitmap != null) {
                                        when (viewModel.selectedModel) {
                                            "stability-ai-mask-erase" -> "Mask created - Ready to erase"
                                            "stability-ai-inpaint" -> "Mask created - Ready to inpaint"
                                            "v51-inpainting" -> "Mask created - Ready for V51 inpainting"
                                            else -> "Mask created - Ready to process"
                                        }
                                    } else {
                                        "Please create a mask using the brush tool"
                                    },
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                    
                    // Handle different prompt inputs based on selected model
                    when (viewModel.selectedModel) {
                        "stability-ai-sketch" -> {
                            OutlinedTextField(
                                value = viewModel.prompt,
                                onValueChange = { newText ->
                                    viewModel.prompt = newText
                                    // Show real-time warning for inappropriate content
                                    if (ContentFilter.containsAdultContent(newText)) {
                                        viewModel.errorMessage = "⚠️ This prompt may violate content policies"
                                    } else if (viewModel.errorMessage?.contains("content policies") == true) {
                                        viewModel.errorMessage = null
                                    }
                                },
                                placeholder = { 
                                    Text(
                                        "What should your sketch become? (e.g., a medieval castle on a hill)",
                                        color = Color.White.copy(alpha = 0.5f)
                                    ) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.5f),
                                    unfocusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.3f),
                                    cursorColor = Color(0xFF6366F1)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4,
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Brush,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                        "stability-ai-structure" -> {
                            OutlinedTextField(
                                value = viewModel.prompt,
                                onValueChange = { newText ->
                                    viewModel.prompt = newText
                                    // Show real-time warning for inappropriate content
                                    if (ContentFilter.containsAdultContent(newText)) {
                                        viewModel.errorMessage = "⚠️ This prompt may violate content policies"
                                    } else if (viewModel.errorMessage?.contains("content policies") == true) {
                                        viewModel.errorMessage = null
                                    }
                                },
                                placeholder = { 
                                    Text(
                                        "What should this structure become? (e.g., a well manicured shrub in an english garden)",
                                        color = Color.White.copy(alpha = 0.5f)
                                    ) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.5f),
                                    unfocusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.3f),
                                    cursorColor = Color(0xFF6366F1)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4,
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Architecture,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                        "stability-ai-search-replace", "stability-ai-search-recolor" -> {
                        OutlinedTextField(
                            value = viewModel.searchPrompt,
                            onValueChange = { viewModel.searchPrompt = it },
                            placeholder = { 
                                Text(
                                    if (viewModel.selectedModel == "stability-ai-search-recolor") 
                                        "What to select for recoloring..."
                                    else 
                                        "What to search for...",
                                    color = Color.White.copy(alpha = 0.5f)
                                ) 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.5f),
                                unfocusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.3f),
                                cursorColor = Color(0xFF6366F1)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 1,
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Search,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        OutlinedTextField(
                            value = viewModel.replacePrompt,
                            onValueChange = { viewModel.replacePrompt = it },
                            placeholder = { 
                                Text(
                                    if (viewModel.selectedModel == "stability-ai-search-recolor") 
                                        "New color/appearance..."
                                    else 
                                        "What to replace with...",
                                    color = Color.White.copy(alpha = 0.5f)
                                ) 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFF6366F1),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.5f),
                                unfocusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.3f),
                                cursorColor = Color(0xFF6366F1)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 3,
                            leadingIcon = {
                                Icon(
                                Icons.Outlined.SwapHoriz,
                                    contentDescription = null,
                                    tint = Color(0xFF6366F1),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                        }
                        "a4f-flux-kontext-dev" -> {
                            // A4F Flux Kontext Dev specific UI
                            OutlinedTextField(
                                value = viewModel.prompt,
                                onValueChange = { viewModel.prompt = it },
                                placeholder = { 
                                    Text(
                                        "Describe the transformation you want (e.g., change style, add elements, modify scene...)",
                                        color = Color.White.copy(alpha = 0.5f)
                                    ) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.5f),
                                    unfocusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.3f),
                                    cursorColor = Color(0xFF6366F1)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4,
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                        "stability-ai-remove-background" -> {
                            // No prompts needed for background removal
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF10B981).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Ready to remove background - no prompts needed!",
                                        color = Color.White,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }
                        "stability-ai-replace-background-relight" -> {
                            // Background reference image picker
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable { backgroundImagePickerLauncher.launch("image/*") },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF0A0E27).copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (viewModel.backgroundReferenceUri != null) 
                                        Color(0xFF10B981) else Color.White.copy(alpha = 0.2f)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Background Reference Image (Optional)",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (viewModel.backgroundReferenceUri != null) {
                                            IconButton(
                                                onClick = { viewModel.updateBackgroundReferenceUri(null) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove background reference",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    if (viewModel.backgroundReferenceUri != null) {
                                        // Show preview of selected background reference
                                        AsyncImage(
                                            model = viewModel.backgroundReferenceUri,
                                            contentDescription = "Background reference",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .padding(top = 8.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Show upload prompt
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = Color(0xFF6366F1),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Tap to upload reference image",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Only show text prompt if no background reference image is selected
                            if (viewModel.backgroundReferenceUri == null) {
                                OutlinedTextField(
                                    value = viewModel.prompt,
                                    onValueChange = { viewModel.prompt = it },
                                    placeholder = { 
                                        Text(
                                            "Describe the new background and lighting...",
                                            color = Color.White.copy(alpha = 0.5f)
                                        ) 
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF6366F1),
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                        focusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.5f),
                                        unfocusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.3f),
                                        cursorColor = Color(0xFF6366F1)
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    minLines = 2,
                                    maxLines = 4,
                                    leadingIcon = {
                                        Icon(
                                            Icons.Outlined.Landscape,
                                            contentDescription = null,
                                            tint = Color(0xFF6366F1),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                )
                            } else {
                                // Show info message when background reference is selected
                                Card(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = Color(0xFF10B981).copy(alpha = 0.2f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.CheckCircle,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Using background reference image",
                                            color = Color.White,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }
                        "flux-kontext-dev-dual", "nano-banana" -> {
                            // Second image picker for dual image model
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable { secondImagePickerLauncher.launch("image/*") },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF0A0E27).copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = when {
                                        viewModel.secondImage != null -> Color(0xFF10B981)
                                        viewModel.selectedModel == "nano-banana" -> Color(0xFF6366F1) // Optional - neutral color
                                        else -> Color(0xFFEF4444) // Required for flux-kontext-dev-dual
                                    }
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            if (viewModel.selectedModel == "nano-banana") 
                                                "Second Image (Optional)" 
                                            else 
                                                "Second Image (Required)",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (viewModel.secondImage != null) {
                                            IconButton(
                                                onClick = { viewModel.updateSecondImage(null) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove second image",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    if (viewModel.secondImage != null) {
                                        // Show preview of second image
                                        Image(
                                            bitmap = viewModel.secondImage!!.asImageBitmap(),
                                            contentDescription = "Second image",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .padding(top = 8.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Show upload prompt
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.AddPhotoAlternate,
                                                contentDescription = null,
                                                tint = Color(0xFF6366F1),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Tap to select second image",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Prompt for dual image combination
                            OutlinedTextField(
                                value = viewModel.prompt,
                                onValueChange = { viewModel.prompt = it },
                                placeholder = { 
                                    Text(
                                        "Describe how to combine the two images (e.g., 'the girl in image one and the girl in image two in one frame')",
                                        color = Color.White.copy(alpha = 0.5f)
                                    ) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.5f),
                                    unfocusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.3f),
                                    cursorColor = Color(0xFF6366F1)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4,
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.GroupAdd,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                        "multiple-face-swap" -> {
                            // Target image picker for face swap
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable { targetImagePickerLauncher.launch("image/*") },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF0A0E27).copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (viewModel.targetImage != null) 
                                        Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Target Face Image (Required)",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (viewModel.targetImage != null) {
                                            IconButton(
                                                onClick = { viewModel.updateTargetImage(null) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove target image",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    if (viewModel.targetImage != null) {
                                        // Show preview of target image
                                        Image(
                                            bitmap = viewModel.targetImage!!.asImageBitmap(),
                                            contentDescription = "Target face",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .padding(top = 8.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Show upload prompt
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.Face,
                                                contentDescription = null,
                                                tint = Color(0xFF6366F1),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Tap to select target face image",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Info card about multiple face swap
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF6366F1).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "This will swap all faces in the main image with the face from the target image",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                        "fashion-try-on" -> {
                            // Clothing image picker for fashion try-on
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable { clothingImagePickerLauncher.launch("image/*") },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF0A0E27).copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (viewModel.clothingImage != null) 
                                        Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Clothing Image (Required)",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (viewModel.clothingImage != null) {
                                            IconButton(
                                                onClick = { viewModel.updateClothingImage(null) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove clothing image",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    if (viewModel.clothingImage != null) {
                                        // Show preview of clothing image
                                        Image(
                                            bitmap = viewModel.clothingImage!!.asImageBitmap(),
                                            contentDescription = "Clothing image",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .padding(top = 8.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Show upload prompt
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.Checkroom,
                                                contentDescription = null,
                                                tint = Color(0xFF6366F1),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Tap to select clothing image",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Cloth type selector
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF0A0E27).copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Text(
                                        "Clothing Type",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.padding(bottom = 8.dp)
                                    )
                                    
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        // Upper body button
                                        Button(
                                            onClick = { viewModel.updateClothType("upper_body") },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (viewModel.clothType == "upper_body") 
                                                    Color(0xFF6366F1) else Color(0xFF374151)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).padding(end = 4.dp)
                                        ) {
                                            Text(
                                                "Upper Body",
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                        }
                                        
                                        // Lower body button
                                        Button(
                                            onClick = { viewModel.updateClothType("lower_body") },
                                            colors = ButtonDefaults.buttonColors(
                                                containerColor = if (viewModel.clothType == "lower_body") 
                                                    Color(0xFF6366F1) else Color(0xFF374151)
                                            ),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier.weight(1f).padding(start = 4.dp)
                                        ) {
                                            Text(
                                                "Lower Body",
                                                fontSize = 12.sp,
                                                color = Color.White
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Prompt for fashion try-on
                            OutlinedTextField(
                                value = viewModel.prompt,
                                onValueChange = { viewModel.prompt = it },
                                placeholder = { 
                                    Text(
                                        "Describe the desired result (e.g., 'A realistic photo of a model wearing a beautiful t-shirt')",
                                        color = Color.White.copy(alpha = 0.5f)
                                    ) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF6366F1),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.5f),
                                    unfocusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.3f),
                                    cursorColor = Color(0xFF6366F1)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 4,
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Checkroom,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                        "stability-ai-style-transfer" -> {
                            // Style reference image picker (required)
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .clickable { styleImagePickerLauncher.launch("image/*") },
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF0A0E27).copy(alpha = 0.5f)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                border = BorderStroke(
                                    width = 1.dp,
                                    color = if (viewModel.styleReferenceImage != null) 
                                        Color(0xFF10B981) else Color(0xFFEF4444)
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "Style Reference Image (Required)",
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (viewModel.styleReferenceImage != null) {
                                            IconButton(
                                                onClick = { viewModel.updateStyleReferenceImage(null) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.Close,
                                                    contentDescription = "Remove style reference",
                                                    tint = Color(0xFFEF4444),
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }
                                    
                                    if (viewModel.styleReferenceImage != null) {
                                        // Show preview of selected style reference
                                        Image(
                                            bitmap = viewModel.styleReferenceImage!!.asImageBitmap(),
                                            contentDescription = "Style reference",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(120.dp)
                                                .padding(top = 8.dp)
                                                .clip(RoundedCornerShape(8.dp)),
                                            contentScale = ContentScale.Crop
                                        )
                                    } else {
                                        // Show upload prompt
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(top = 8.dp),
                                            horizontalArrangement = Arrangement.Center,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Outlined.Palette,
                                                contentDescription = null,
                                                tint = Color(0xFFEC4899),
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                "Tap to upload style reference image",
                                                color = Color.White.copy(alpha = 0.7f),
                                                fontSize = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                            
                            // Info card about style transfer
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF6366F1).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Outlined.Info,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "Style Transfer will apply the artistic style from your reference image to the content image",
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        lineHeight = 18.sp
                                    )
                                }
                            }
                        }
                        "stability-ai-upscale" -> {
                            // Info card about upscaling
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF10B981).copy(alpha = 0.2f)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Outlined.ZoomIn,
                                            contentDescription = null,
                                            tint = Color(0xFF10B981),
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            "Conservative Upscaling",
                                            color = Color.White,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "• Increases resolution up to 4x",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "• Preserves original image details",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 13.sp
                                    )
                                    Text(
                                        "• Optional prompt for enhancement guidance",
                                        color = Color.White.copy(alpha = 0.9f),
                                        fontSize = 13.sp
                                    )
                                }
                            }
                            
                            // Optional prompt for upscaling guidance
                            OutlinedTextField(
                                value = viewModel.prompt,
                                onValueChange = { newText ->
                                    viewModel.prompt = newText
                                    // Show real-time warning for inappropriate content
                                    if (ContentFilter.containsAdultContent(newText)) {
                                        viewModel.errorMessage = "⚠️ This prompt may violate content policies"
                                    } else if (viewModel.errorMessage?.contains("content policies") == true) {
                                        viewModel.errorMessage = null
                                    }
                                },
                                placeholder = { 
                                    Text(
                                        "Optional: Describe details to enhance (e.g., 'sharp details, high quality')",
                                        color = Color.White.copy(alpha = 0.5f)
                                    ) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF10B981),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    focusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.5f),
                                    unfocusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.3f),
                                    cursorColor = Color(0xFF10B981)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 2,
                                maxLines = 3,
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.AutoAwesome,
                                        contentDescription = null,
                                        tint = Color(0xFF10B981),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                        else -> {
                            // Main Prompt Input for other models - Enhanced Styling to match EnhancedImageGeneratorScreen
                            OutlinedTextField(
                                value = viewModel.prompt,
                                onValueChange = { newText ->
                                    viewModel.prompt = newText
                                    // Show real-time warning for inappropriate content
                                    if (ContentFilter.containsAdultContent(newText)) {
                                        viewModel.errorMessage = "⚠️ This prompt may violate content policies"
                                    } else if (viewModel.errorMessage?.contains("content policies") == true) {
                                        viewModel.errorMessage = null
                                    }
                                },
                                placeholder = { 
                                    Text(
                                        "Describe your transformation (family-friendly content only)...",
                                        color = Color.White.copy(alpha = 0.5f)
                                    ) 
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 12.dp)
                                    .border(
                                        width = 2.dp,
                                        brush = Brush.linearGradient(
                                            colors = if (ContentFilter.containsAdultContent(viewModel.prompt)) {
                                                listOf(
                                                    Color(0xFFF59E0B).copy(alpha = 0.8f),
                                                    Color(0xFFDC2626).copy(alpha = 0.7f),
                                                    Color(0xFFF59E0B).copy(alpha = 0.6f)
                                                )
                                            } else {
                                                listOf(
                                                    Color(0xFF8B5CF6).copy(alpha = 0.6f), // GradientPurple
                                                    Color(0xFFEC4899).copy(alpha = 0.7f), // GradientPink
                                                    Color(0xFF06B6D4).copy(alpha = 0.5f), // GradientCyan
                                                    Color(0xFF8B5CF6).copy(alpha = 0.4f)  // GradientPurple
                                                )
                                            }
                                        ),
                                        shape = RoundedCornerShape(12.dp)
                                    ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color.Transparent,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.5f),
                                    unfocusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.3f),
                                    cursorColor = Color(0xFF6366F1)
                                ),
                                shape = RoundedCornerShape(12.dp),
                                minLines = 3,
                                maxLines = 5,
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Edit,
                                        contentDescription = null,
                                        tint = Color(0xFF6366F1),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            )
                        }
                    }
                    
                    // Negative Prompt (Collapsible)
                    AnimatedVisibility(
                        visible = showAdvancedSettings,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        OutlinedTextField(
                            value = viewModel.negativePrompt,
                            onValueChange = { viewModel.negativePrompt = it },
                            placeholder = { 
                                Text(
                                    "What to avoid...",
                                    color = Color.White.copy(alpha = 0.5f)
                                ) 
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color(0xFFEC4899),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                focusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.5f),
                                unfocusedContainerColor = Color(0xFF0A0E27).copy(alpha = 0.3f),
                                cursorColor = Color(0xFFEC4899)
                            ),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 2,
                            maxLines = 3,
                            leadingIcon = {
                                Icon(
                                    Icons.Outlined.Block,
                                    contentDescription = null,
                                    tint = Color(0xFFEC4899),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }
            
            // Advanced Settings Toggle with Glass Morphism
            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clickable { showAdvancedSettings = !showAdvancedSettings },
                cornerRadius = 16.dp,
                backgroundColor = Color.White.copy(alpha = 0.05f),
                borderColor = GlassMorphism.GlassBorderSubtle,
                contentPadding = PaddingValues(0.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Outlined.Tune,
                            contentDescription = null,
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Advanced Settings",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                    
                    Icon(
                        imageVector = if (showAdvancedSettings) 
                            Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.6f)
                    )
                }
            }
            
            // Advanced Settings Content
            AnimatedVisibility(
                visible = showAdvancedSettings,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                GlassCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    cornerRadius = 16.dp,
                    backgroundColor = Color.White.copy(alpha = 0.03f),
                    borderColor = GlassMorphism.GlassBorderSubtle,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        // Strength Slider
                        Column(
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            if (viewModel.selectedModel == "stability-ai-sketch" || viewModel.selectedModel == "stability-ai-structure") {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Control Strength",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        String.format("%.0f%%", viewModel.controlStrength * 100),
                                        color = Color(0xFF6366F1),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Slider(
                                    value = viewModel.controlStrength,
                                    onValueChange = { viewModel.updateControlStrength(it) },
                                    valueRange = 0.1f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF6366F1),
                                        activeTrackColor = Color(0xFF6366F1),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                                Text(
                                    if (viewModel.selectedModel == "stability-ai-sketch") 
                                        "Higher values follow your sketch more closely"
                                    else
                                        "Higher values preserve original structure more",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Transformation Strength",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        String.format("%.0f%%", viewModel.strength * 100),
                                        color = Color(0xFF6366F1),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Slider(
                                    value = viewModel.strength,
                                    onValueChange = { viewModel.strength = it },
                                    valueRange = 0.1f..1f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF6366F1),
                                        activeTrackColor = Color(0xFF6366F1),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                        
                        // Guidance Scale Slider
                        Column(
                            modifier = Modifier.padding(bottom = 20.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Guidance Scale",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    String.format("%.1f", viewModel.guidanceScale),
                                    color = Color(0xFFEC4899),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Slider(
                                value = viewModel.guidanceScale,
                                onValueChange = { viewModel.guidanceScale = it },
                                valueRange = 1f..20f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFFEC4899),
                                    activeTrackColor = Color(0xFFEC4899),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        
                        // Steps Slider
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    "Quality Steps",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "${viewModel.steps}",
                                    color = Color(0xFF6366F1),
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Slider(
                                value = viewModel.steps.toFloat(),
                                onValueChange = { viewModel.steps = it.toInt() },
                                valueRange = 10f..50f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF6366F1),
                                    activeTrackColor = Color(0xFF6366F1),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                ),
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                        
                        // Outpaint Controls - only show for outpaint model
                        if (viewModel.selectedModel == "stability-ai-outpaint") {
                            Spacer(modifier = Modifier.height(20.dp))
                            
                            Text(
                                "Outpaint Settings",
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )
                            
                            // Left Outpaint
                            Column(
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Left Extension",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${viewModel.outpaintLeft}px",
                                        color = Color(0xFF6366F1),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Slider(
                                    value = viewModel.outpaintLeft.toFloat(),
                                    onValueChange = { viewModel.updateOutpaintLeft(it.toInt()) },
                                    valueRange = 0f..512f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF6366F1),
                                        activeTrackColor = Color(0xFF6366F1),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            
                            // Right Outpaint
                            Column(
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Right Extension",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${viewModel.outpaintRight}px",
                                        color = Color(0xFF6366F1),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Slider(
                                    value = viewModel.outpaintRight.toFloat(),
                                    onValueChange = { viewModel.updateOutpaintRight(it.toInt()) },
                                    valueRange = 0f..512f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF6366F1),
                                        activeTrackColor = Color(0xFF6366F1),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            
                            // Top Outpaint
                            Column(
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Top Extension",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${viewModel.outpaintTop}px",
                                        color = Color(0xFF6366F1),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Slider(
                                    value = viewModel.outpaintTop.toFloat(),
                                    onValueChange = { viewModel.updateOutpaintTop(it.toInt()) },
                                    valueRange = 0f..512f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF6366F1),
                                        activeTrackColor = Color(0xFF6366F1),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                            
                            // Bottom Outpaint
                            Column(
                                modifier = Modifier.padding(bottom = 16.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Bottom Extension",
                                        color = Color.White,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${viewModel.outpaintBottom}px",
                                        color = Color(0xFF6366F1),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                
                                Slider(
                                    value = viewModel.outpaintBottom.toFloat(),
                                    onValueChange = { viewModel.updateOutpaintBottom(it.toInt()) },
                                    valueRange = 0f..512f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF6366F1),
                                        activeTrackColor = Color(0xFF6366F1),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    ),
                                    modifier = Modifier.padding(top = 8.dp)
                                )
                            }
                        }
                    }
                }
            }
            
            // Live timer display during generation
            AnimatedVisibility(
                visible = viewModel.isLoading,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = null,
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        "Generating: ${viewModel.elapsedTimeInSeconds.collectAsState().value} sec",
                        color = Color(0xFF6366F1),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Generate Button with Enhanced Style matching EnhancedImageGeneratorScreen
            Button(
                onClick = {
                    when {
                        viewModel.selectedModel.startsWith("batch-") && viewModel.batchImages.isNotEmpty() -> viewModel.processBatch()
                        else -> viewModel.generateImage()
                    }
                },
                enabled = (!viewModel.isLoading && !viewModel.isBatchProcessing) &&
                        (viewModel.selectedImage != null || 
                        (viewModel.selectedModel.startsWith("batch-") && viewModel.batchImages.isNotEmpty())),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 16.dp)
                    .height(56.dp)
                    .border(
                        width = 1.dp,
                        brush = Brush.linearGradient(
                            colors = listOf(
                                Color(0xFF6366F1).copy(alpha = 0.4f),
                                Color(0xFFEC4899).copy(alpha = 0.3f),
                                Color(0xFF10B981).copy(alpha = 0.3f)
                            )
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if ((!viewModel.isLoading && !viewModel.isBatchProcessing) &&
                            (viewModel.selectedImage != null || 
                            (viewModel.selectedModel.startsWith("batch-") && viewModel.batchImages.isNotEmpty()))) {
                        Color(0xFF1F2937).copy(alpha = 0.8f) // Dark background when enabled
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
                    if (viewModel.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = TextPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.selectedModel.startsWith("batch-") && viewModel.isBatchProcessing) {
                                "Processing Batch... (${viewModel.batchProcessingProgress + 1}/${viewModel.batchProcessingTotal})"
                            } else {
                                "Transforming..."
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = TextPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (viewModel.selectedModel.startsWith("batch-") && viewModel.batchImages.isNotEmpty()) {
                                "Transform Batch (${viewModel.batchImages.size})"
                            } else {
                                "Transform Image"
                            },
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                }
            }
            

            
            // Error Message
            AnimatedVisibility(
                visible = viewModel.errorMessage != null,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                viewModel.errorMessage?.let { error ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(
                            width = 1.dp,
                            color = Color(0xFFEF4444).copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = error,
                                color = Color.White,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

                        Spacer(modifier = Modifier.height(80.dp))
          }
        }
        
        // Fullscreen Image Viewer Dialog
        if (showFullscreenImage && (viewModel.generatedImageUrl != null || viewModel.generatedImageBitmap != null)) {
            FullscreenImageDialog(
                originalImage = viewModel.selectedImage,
                generatedImageUrl = viewModel.generatedImageUrl,
                generatedImageBitmap = viewModel.generatedImageBitmap,
                onDismiss = { showFullscreenImage = false }
            )
        }
        
        // Brush mask tool dialog
        if (viewModel.showMaskingInterface && viewModel.selectedImage != null) {
            BrushMaskTool(
                originalImage = viewModel.selectedImage!!,
                onMaskCreated = { maskBitmap ->
                    viewModel.updateMaskBitmap(maskBitmap)
                    viewModel.toggleMaskingInterface()
                },
                onDismiss = { viewModel.toggleMaskingInterface() }
            )
        }
        
        // Report Dialog
        if (showReportDialog) {
            android.util.Log.d("ReportDialog", "Showing ImageToImageReportDialog")
            ImageToImageReportDialog(
                onDismiss = { 
                    android.util.Log.d("ReportDialog", "Report dialog dismissed")
                    showReportDialog = false
                },
                onReport = { reason ->
                    // Handle report submission
                    android.util.Log.d("ReportDialog", "Report submitted with reason: $reason")
                    showReportDialog = false
                    Toast.makeText(context, "Report submitted successfully. Thank you for helping us improve.", Toast.LENGTH_LONG).show()
                }
            )
        }
    }
}

// Helper functions for saving and sharing images
private suspend fun saveGeneratedImage(
    context: android.content.Context,
    viewModel: UnifiedImageToImageViewModel
) {
    try {
        var bitmap = viewModel.generatedImageBitmap
        
        // If no bitmap but we have a URL, download the image first
        if (bitmap == null && viewModel.generatedImageUrl != null) {
            bitmap = downloadBitmapFromUrl(viewModel.generatedImageUrl!!)
        }
        
        if (bitmap != null) {
            val fileName = "AI_Image_${System.currentTimeMillis()}.png"
            
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val contentValues = ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                uri?.let {
                    resolver.openOutputStream(it)?.use { output ->
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                    }
                }
                Toast.makeText(context, "Image saved to Downloads folder", Toast.LENGTH_SHORT).show()
            } else {
                val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) downloadsDir.mkdirs()
                
                val imageFile = File(downloadsDir, fileName)
                FileOutputStream(imageFile).use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
                Toast.makeText(context, "Image saved to Downloads folder", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "No image to save", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to save image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

private suspend fun shareGeneratedImage(
    context: android.content.Context,
    viewModel: UnifiedImageToImageViewModel
) {
    try {
        var bitmap = viewModel.generatedImageBitmap
        
        // If no bitmap but we have a URL, download the image first
        if (bitmap == null && viewModel.generatedImageUrl != null) {
            bitmap = downloadBitmapFromUrl(viewModel.generatedImageUrl!!)
        }
        
        if (bitmap != null) {
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
                putExtra(Intent.EXTRA_TEXT, "Created with AI Image Studio Pro: ${viewModel.prompt}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            
            context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
        } else {
            Toast.makeText(context, "No image to share", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        Toast.makeText(context, "Failed to share image: ${e.message}", Toast.LENGTH_SHORT).show()
    }
}

// Helper function to download bitmap from URL
private suspend fun downloadBitmapFromUrl(urlString: String): Bitmap? = withContext(Dispatchers.IO) {
    try {
        val url = URL(urlString)
        val connection = url.openConnection() as HttpURLConnection
        connection.doInput = true
        connection.connectTimeout = 30000
        connection.readTimeout = 30000
        connection.connect()
        
        if (connection.responseCode == HttpURLConnection.HTTP_OK) {
            val input = connection.inputStream
            val bitmap = BitmapFactory.decodeStream(input)
            input.close()
            connection.disconnect()
            return@withContext bitmap
        }
        null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

@Composable
private fun FullscreenImageDialog(
    originalImage: Bitmap?,
    generatedImageUrl: String?,
    generatedImageBitmap: Bitmap?,
    onDismiss: () -> Unit
) {
    var showOriginal by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.95f))
                .clickable(enabled = false) { } // Consume clicks
        ) {
            // Main image display
            AnimatedContent(
                targetState = showOriginal,
                transitionSpec = {
                    fadeIn(animationSpec = tween(300)) togetherWith
                        fadeOut(animationSpec = tween(300))
                },
                modifier = Modifier.fillMaxSize()
            ) { showingOriginal ->
                if (showingOriginal && originalImage != null) {
                    // Show original image
                    Image(
                        bitmap = originalImage.asImageBitmap(),
                        contentDescription = "Original Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    // Show generated image
                    when {
                        generatedImageUrl != null -> {
                            AsyncImage(
                                model = generatedImageUrl,
                                contentDescription = "Generated Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                        generatedImageBitmap != null -> {
                            Image(
                                bitmap = generatedImageBitmap.asImageBitmap(),
                                contentDescription = "Generated Image",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }
                }
            }
            
            // Top controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Close button
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            Color.White.copy(alpha = 0.1f),
                            CircleShape
                        )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Close",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Right side controls (Download, Share, Toggle)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Download button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    var bitmapToSave = if (showOriginal) originalImage else generatedImageBitmap
                                    
                                    // If showing generated image and no bitmap but we have URL, download it
                                    if (!showOriginal && bitmapToSave == null && generatedImageUrl != null) {
                                        Toast.makeText(context, "Downloading image...", Toast.LENGTH_SHORT).show()
                                        bitmapToSave = downloadBitmapFromUrl(generatedImageUrl)
                                    }
                                    
                                    if (bitmapToSave != null) {
                                        saveImageToDevice(context, bitmapToSave, showOriginal)
                                    } else {
                                        Toast.makeText(context, "Failed to download image", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to save: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color(0xFF10B981).copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Outlined.Download,
                            contentDescription = "Download",
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Share button
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                try {
                                    if (showOriginal && originalImage != null) {
                                        // Share original image bitmap
                                        shareImage(context, originalImage, true)
                                    } else if (!showOriginal) {
                                        // For generated image, always share the actual image
                                        var bitmapToShare = generatedImageBitmap
                                        
                                        // If no bitmap but we have URL, download it silently
                                        if (bitmapToShare == null && generatedImageUrl != null) {
                                            bitmapToShare = downloadBitmapFromUrl(generatedImageUrl)
                                        }
                                        
                                        if (bitmapToShare != null) {
                                            shareImage(context, bitmapToShare, false)
                                        } else {
                                            Toast.makeText(context, "No image to share", Toast.LENGTH_SHORT).show()
                                        }
                                    } else {
                                        Toast.makeText(context, "No image to share", Toast.LENGTH_SHORT).show()
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to share: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                Color(0xFF6366F1).copy(alpha = 0.2f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Outlined.Share,
                            contentDescription = "Share",
                            tint = Color(0xFF6366F1),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    // Toggle button
                    if (originalImage != null) {
                        IconButton(
                            onClick = { showOriginal = !showOriginal },
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    Color.White.copy(alpha = 0.2f),
                                    CircleShape
                                )
                        ) {
                            Icon(
                                Icons.Outlined.Compare,
                                contentDescription = if (showOriginal) "Show Generated" else "Show Original",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
            
            // Bottom action bar - moved up
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 30.dp), // Move up by 60dp
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Current image label
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (showOriginal) Color(0xFF6366F1).copy(alpha = 0.9f) else Color(0xFFEC4899).copy(alpha = 0.9f),
                        modifier = Modifier.padding(bottom = 12.dp)
                    ) {
                        Text(
                            text = if (showOriginal) "Original" else "AI Generated",
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                    

                }
            }
        }
    }
}

// Helper function to save image from fullscreen viewer
private suspend fun saveImageToDevice(
    context: android.content.Context,
    bitmap: Bitmap,
    isOriginal: Boolean
) {
    try {
        val fileName = if (isOriginal) {
            "Original_Image_${System.currentTimeMillis()}.png"
        } else {
            "AI_Generated_${System.currentTimeMillis()}.png"
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
            
            val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let {
                resolver.openOutputStream(it)?.use { output ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
                }
            }
        } else {
            val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            if (!downloadsDir.exists()) downloadsDir.mkdirs()
            
            val imageFile = File(downloadsDir, fileName)
            FileOutputStream(imageFile).use { output ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)
            }
        }
        
        Toast.makeText(context, "Image saved to Downloads folder", Toast.LENGTH_SHORT).show()
    } catch (e: Exception) {
        throw e
    }
}

// Helper function to share image from fullscreen viewer
private suspend fun shareImage(
    context: android.content.Context,
    bitmap: Bitmap,
    isOriginal: Boolean
) {
    try {
        val cachePath = File(context.cacheDir, "images")
        cachePath.mkdirs()
        
        val fileName = if (isOriginal) "original_image.png" else "generated_image.png"
        val stream = FileOutputStream(File(cachePath, fileName))
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
        stream.close()

        val imagePath = File(context.cacheDir, "images/$fileName")
        val imageUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imagePath
        )
        
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, imageUri)
            putExtra(Intent.EXTRA_TEXT, if (isOriginal) "Original Image" else "AI Generated Image")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        
        context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
    } catch (e: Exception) {
        throw e
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ImageToImageReportDialog(
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
            // Animated background with gradient and orbs - same as EnhancedImageGeneratorScreen
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xFF0A0E27), // DarkBackground
                                Color(0xFF0A0E27).copy(alpha = 0.95f),
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
                                easing = FastOutSlowInEasing
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
                                easing = FastOutSlowInEasing
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
                                            Color(0xFF8B5CF6).copy(alpha = 0.1f), // GradientPurple
                                            Color.Transparent
                                        )
                                        1 -> listOf(
                                            Color(0xFFEC4899).copy(alpha = 0.08f), // GradientPink
                                            Color.Transparent
                                        )
                                        else -> listOf(
                                            Color(0xFF06B6D4).copy(alpha = 0.06f), // GradientCyan
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
                        color = Color.White
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
                
                Text(
                    text = "Help us maintain a safe community by reporting inappropriate content.",
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 14.sp
                )
                
                // Report reasons
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Reason for reporting:",
                        color = Color.White,
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
                                    selectedColor = Color(0xFF6366F1),
                                    unselectedColor = Color.White.copy(alpha = 0.7f)
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = reason,
                                color = Color.White,
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
                        label = { Text("Please specify", color = Color.White.copy(alpha = 0.7f)) },
                        placeholder = { Text("Describe the issue...", color = Color.White.copy(alpha = 0.5f)) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF6366F1),
                            unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                            cursorColor = Color(0xFF6366F1),
                            focusedLabelColor = Color(0xFF6366F1),
                            unfocusedLabelColor = Color.White.copy(alpha = 0.7f)
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
                            contentColor = Color.White.copy(alpha = 0.7f)
                        ),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.3f)),
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