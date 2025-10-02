package max.ohm.oneai.homescreen

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import max.ohm.oneai.imagegeneration.GeneratedImage
import max.ohm.oneai.imagegeneration.ImageHistoryDataStore
import max.ohm.oneai.login.LoginState
import max.ohm.oneai.login.LoginViewModel
import max.ohm.oneai.ui.theme.*
import max.ohm.oneai.videogeneration.GeneratedVideo
import max.ohm.oneai.videogeneration.VideoHistoryDataStore
import max.ohm.oneai.imagetoimage.ImageToImageHistoryDataStore
import max.ohm.oneai.imagetoimage.ImageToImageHistoryItem
import java.text.SimpleDateFormat
import java.util.*
import java.io.File
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.random.Random
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.compose.runtime.DisposableEffect
import android.net.Uri

// Modern color palette
private val DarkBackground = Color(0xFF0A0E27)
private val DarkSecondary = Color(0xFF1A1F3A)

// Sealed class for different creation types
sealed class CreationItem {
    data class ImageItem(val image: GeneratedImage) : CreationItem()
    data class VideoItem(val video: GeneratedVideo) : CreationItem()
    data class ImageToImageItem(val image: GeneratedImage) : CreationItem()
}
private val AccentPurple = Color(0xFF6366F1)
private val AccentPink = Color(0xFFEC4899)
private val AccentCyan = Color(0xFF06B6D4)
private val AccentIndigo = Color(0xFF4F46E5)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB8BCC8)
private val CardBackground = Color(0xFF1E2139)
private val GradientStart = Color(0xFF6366F1)
private val GradientEnd = Color(0xFFEC4899)

// Banner data class
data class BannerItem(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val imageUrl: String,
    val gradientColors: List<Color>,
    val route: String,
    val hasDropdown: Boolean = false,
    val dropdownOptions: List<DropdownOption> = emptyList()
)

// Dropdown option data class
data class DropdownOption(
    val title: String,
    val subtitle: String,
    val route: String,
    val modelType: String? = null
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun EnhancedHomeScreen(
    navController: NavController,
    loginViewModel: LoginViewModel = viewModel()
) {
    val loginState by loginViewModel.loginState.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    // Check if user is logged in
    LaunchedEffect(loginState) {
        if (loginState !is LoginState.Success) {
            navController.navigate("login") {
                popUpTo("home") { inclusive = true }
            }
        }
    }
    
    // Banner items with dropdown functionality
    val bannerItems = remember {
listOf(
            BannerItem(
                title = "AI Chat Assistant",
                subtitle = "Intelligent conversations powered by advanced AI",
                icon = Icons.Outlined.Chat,
                imageUrl = "https://cdn.pixabay.com/photo/2024/05/16/19/29/ai-generated-8766874_1280.jpg",
                gradientColors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
                route = "chatbot"
            ),
            BannerItem(
                title = "Image Generation",
                subtitle = "Choose from multiple AI models",
                icon = Icons.Outlined.Image,
                imageUrl = "https://cdn.pixabay.com/photo/2017/11/07/20/43/christmas-tree-2928142_1280.jpg",
                gradientColors = listOf(Color(0xFF4FACFE), Color(0xFF00F2FE)),
                route = "enhancedImageGenerator",
                hasDropdown = true,
                dropdownOptions = listOf(
                    DropdownOption(
                        title = "FLUX Kontext Max",
                        subtitle = "Maximum quality & performance",
                        route = "enhancedImageGenerator",
                        modelType = "provider-2/FLUX.1-kontext-max"
                    ),
                    DropdownOption(
                        title = "FLUX Kontext Pro",
                        subtitle = "Professional grade quality",
                        route = "enhancedImageGenerator",
                        modelType = "provider-1/FLUX.1-kontext-pro"
                    ),
                    DropdownOption(
                        title = "DALL-E 3",
                        subtitle = "OpenAI's creative model",
                        route = "enhancedImageGenerator",
                        modelType = "provider-3/dall-e-3"
                    ),
                    DropdownOption(
                        title = "Flux Ultra Pro",
                        subtitle = "Ultra high definition",
                        route = "enhancedImageGenerator",
                        modelType = "provider-3/FLUX.1.1-pro-ultra"
                    )
                )
            ),
            BannerItem(
                title = "Image to Image",
                subtitle = "Transform and enhance existing images",
                icon = Icons.Outlined.Transform,
                imageUrl = "https://cdn.pixabay.com/photo/2022/08/27/00/11/plant-7413415_1280.png",
                gradientColors = listOf(Color(0xFFFA709A), Color(0xFFFEE140)),
                route = "imageToImage",
                hasDropdown = true,
                dropdownOptions = listOf(
                    DropdownOption(
                        title = "Single Image Transform",
                        subtitle = "Transform one image using AI",
                        route = "imageToImage",
                        modelType = "single"
                    ),
                    DropdownOption(
                        title = "Dual Image Fusion",
                        subtitle = "Combine two images with nano-banana AI",
                        route = "imageToImage",
                        modelType = "dual-nano-banana"
                    ),
                    DropdownOption(
                        title = "Ghibli Style Transform",
                        subtitle = "Apply Studio Ghibli animation style",
                        route = "imageToImage",
                        modelType = "ghibli"
                    )
                )
            ),
            BannerItem(
                title = "Video Generation",
                subtitle = "Create stunning videos from text",
                icon = Icons.Outlined.VideoLibrary,
                imageUrl = "https://cdn.pixabay.com/photo/2019/04/24/21/55/cinema-4153289_1280.jpg",
                gradientColors = listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
                route = "videoGeneration",
                hasDropdown = true,
                dropdownOptions = listOf(
                    DropdownOption(
                        title = "Runway Gen-3 Alpha",
                        subtitle = "High quality video generation",
                        route = "videoGeneration",
                        modelType = "runway-gen3"
                    ),
                    DropdownOption(
                        title = "Stable Video Diffusion",
                        subtitle = "Stable and consistent videos",
                        route = "videoGeneration",
                        modelType = "stable-video"
                    ),
                    DropdownOption(
                        title = "Pika Labs",
                        subtitle = "Creative video synthesis",
                        route = "videoGeneration",
                        modelType = "pika-labs"
                    ),
                    DropdownOption(
                        title = "LumaLabs Dream Machine",
                        subtitle = "Advanced video AI",
                        route = "videoGeneration",
                        modelType = "lumalabs-dream"
                    )
                )
            ),
            BannerItem(
                title = "Image to Video",
                subtitle = "Transform images into dynamic videos",
                icon = Icons.Outlined.VideoLibrary,
                imageUrl = "https://cdn.pixabay.com/photo/2016/11/29/05/45/astronomy-1867616_1280.jpg",
                gradientColors = listOf(Color(0xFF667EEA), Color(0xFF764BA2)),
                route = "imageToVideo"
            ),
            // BannerItem(
            //     title = "Video Generation (Coming Soon)",
            //     subtitle = "Create stunning videos from text prompts - Feature in development",
            //     icon = Icons.Outlined.VideoLibrary,
            //     imageUrl = "https://cdn.pixabay.com/photo/2019/04/24/21/55/cinema-4153289_1280.jpg",
            //     gradientColors = listOf(Color(0xFFF093FB), Color(0xFFF5576C)),
            //     route = "home" // Redirect to home instead of video generation for now
            // ),
            // BannerItem(
            //     title = "Live Avatar (Coming Soon)",
            //     subtitle = "Interactive AI-powered digital avatars - Feature in development",
            //     icon = Icons.Outlined.VideoCall,
            //     imageUrl = "https://miro.medium.com/v2/resize:fit:1400/0*YpJsxi_-9hBsMc9H.gif",
            //     gradientColors = listOf(Color(0xFF8EC5FC), Color(0xFFE0C3FC)),
            //     route = "home" // Redirect to home instead of live avatar for now
            // )
        )
    }
    
    // Pager state for banner
    val pagerState = rememberPagerState(pageCount = { bannerItems.size })
    
    // Auto-scroll effect
    LaunchedEffect(pagerState) {
        while (true) {
            delay(4000) // Change slide every 4 seconds
            val nextPage = (pagerState.currentPage + 1) % bannerItems.size
            pagerState.animateScrollToPage(
                page = nextPage,
                animationSpec = tween(
                    durationMillis = 800,
                    easing = FastOutSlowInEasing
                )
            )
        }
    }
    
    // Load image history
    val imageHistoryStore = remember { ImageHistoryDataStore(context) }
    val persistedImageHistory by imageHistoryStore.imageHistory.collectAsState(initial = emptyList())
    
    // Load video history
    val videoHistoryStore = remember { VideoHistoryDataStore(context) }
    val persistedVideoHistory by videoHistoryStore.videoHistory.collectAsState(initial = emptyList())
    
    // Load image-to-image history
    val imageToImageHistoryStore = remember { ImageToImageHistoryDataStore(context) }
    val persistedImageToImageHistory by imageToImageHistoryStore.imageHistory.collectAsState(initial = emptyList())
    
    var imageHistory by remember { mutableStateOf(listOf<GeneratedImage>()) }
    var videoHistory by remember { mutableStateOf(listOf<GeneratedVideo>()) }
    var imageToImageHistory by remember { mutableStateOf(listOf<GeneratedImage>()) }
    var selectedImage by remember { mutableStateOf<GeneratedImage?>(null) }
    var selectedVideo by remember { mutableStateOf<GeneratedVideo?>(null) }
    var showFullScreen by remember { mutableStateOf(false) }
    var showVideoPlayer by remember { mutableStateOf(false) }
    
    LaunchedEffect(persistedImageHistory) {
        imageHistory = persistedImageHistory.take(6).mapNotNull { item ->
            val imageData = imageHistoryStore.getImageData(item)
            if (imageData != null) {
                GeneratedImage(
                    id = item.id,
                    prompt = item.prompt,
                    imageData = imageData,
                    timestamp = item.timestamp,
                    model = item.model
                )
            } else null
        }
    }
    
    LaunchedEffect(persistedVideoHistory) {
        videoHistory = persistedVideoHistory.take(6).mapNotNull { item ->
            val videoData = videoHistoryStore.getVideoData(item)
            if (videoData != null) {
                GeneratedVideo(
                    id = item.id,
                    prompt = item.prompt,
                    videoUrl = item.videoUrl,
                    thumbnailPath = item.thumbnailPath,
                    timestamp = item.timestamp,
                    model = item.model
                )
            } else null
        }
    }
    
    LaunchedEffect(persistedImageToImageHistory) {
        imageToImageHistory = persistedImageToImageHistory.take(6).mapNotNull { item ->
            val imageData = imageToImageHistoryStore.getImageData(item)
            if (imageData != null) {
                GeneratedImage(
                    id = item.id,
                    prompt = item.prompt,
                    imageData = imageData,
                    timestamp = item.timestamp,
                    model = "Image2Image - ${item.model}"
                )
            } else null
        }
    }
    
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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header with profile
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = DarkSecondary.copy(alpha = 0.5f),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "OneAI Studio",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Your Creative AI Companion",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    
                    // Profile Icon
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(AccentPurple, AccentPink)
                                )
                            )
                            .clickable { navController.navigate("profile") },
                        contentAlignment = Alignment.Center
                    ) {
                        val auth = FirebaseAuth.getInstance()
                        val userPhotoUrl = remember { auth.currentUser?.photoUrl }
                        val userName = remember { auth.currentUser?.displayName ?: "User" }
                        val userInitial = remember { userName.firstOrNull()?.toString() ?: "U" }
                        
                        if (userPhotoUrl != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(userPhotoUrl)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Text(
                                text = userInitial,
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
            
            // Auto-sliding banner
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    BannerCard(
                        item = bannerItems[page],
                        onMainClick = { navController.navigate(bannerItems[page].route) },
                        onDropdownClick = { option ->
                            if (option.modelType != null) {
                                navController.navigate("${option.route}?modelType=${option.modelType}")
                            } else {
                                navController.navigate(option.route)
                            }
                        }
                    )
                }
                
                // Page indicators
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(bannerItems.size) { index ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (pagerState.currentPage == index) 
                                        Color.White 
                                    else 
                                        Color.White.copy(alpha = 0.3f)
                                )
                        )
                    }
                }
            }
            
            // Recent Generations Section
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Recent Creations",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Your AI-generated masterpieces",
                            fontSize = 12.sp,
                            color = TextSecondary
                        )
                    }
                    
                    TextButton(
                        onClick = { navController.navigate("enhancedImageGenerator") }
                    ) {
                        Text("View All", color = AccentPurple)
                        Icon(
                            Icons.Default.ArrowForward,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                
                if (imageHistory.isEmpty() && videoHistory.isEmpty() && imageToImageHistory.isEmpty()) {
                    EmptyGalleryState(
                        onCreateClick = { navController.navigate("enhancedImageGenerator") }
                    )
                } else {
                    // Combine and sort items by timestamp
                    val allItems = (imageHistory.map { CreationItem.ImageItem(it) } + 
                                   videoHistory.map { CreationItem.VideoItem(it) } +
                                   imageToImageHistory.map { CreationItem.ImageToImageItem(it) })
                        .sortedByDescending { 
                            when (it) {
                                is CreationItem.ImageItem -> it.image.timestamp
                                is CreationItem.VideoItem -> it.video.timestamp
                                is CreationItem.ImageToImageItem -> it.image.timestamp
                            }
                        }
                    
                    LazyVerticalStaggeredGrid(
                        columns = StaggeredGridCells.Fixed(2),
                        modifier = Modifier.heightIn(max = 600.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalItemSpacing = 8.dp
                    ) {
                        items(allItems) { item ->
                            when (item) {
                                is CreationItem.ImageItem -> {
                                    val aspectRatio = remember { 
                                        // Generate random aspect ratios for variety
                                        when (Random.nextInt(3)) {
                                            0 -> 1f // Square
                                            1 -> 0.7f // Portrait
                                            else -> 1.3f // Landscape
                                        }
                                    }
                                    GeneratedImageCard(
                                        image = item.image,
                                        aspectRatio = aspectRatio,
                                        onClick = {
                                            selectedImage = item.image
                                            showFullScreen = true
                                        }
                                    )
                                }
                                is CreationItem.VideoItem -> {
                                    GeneratedVideoCard(
                                        video = item.video,
                                        aspectRatio = 16f/9f, // Standard video aspect ratio
                                        onClick = {
                                            // No longer navigate, just handled within the card
                                        }
                                    )
                                }
                                is CreationItem.ImageToImageItem -> {
                                    val aspectRatio = remember { 
                                        // Generate random aspect ratios for variety
                                        when (Random.nextInt(3)) {
                                            0 -> 1f // Square
                                            1 -> 0.7f // Portrait
                                            else -> 1.3f // Landscape
                                        }
                                    }
                                    GeneratedImageCard(
                                        image = item.image,
                                        aspectRatio = aspectRatio,
                                        onClick = {
                                            selectedImage = item.image
                                            showFullScreen = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                
                
                // Quick Actions section removed completely
            }
            
            Spacer(modifier = Modifier.height(80.dp)) // Space for bottom navigation
        }
    }
    
    // Full screen image viewer
    if (showFullScreen && selectedImage != null) {
        FullScreenImageViewer(
            image = selectedImage!!,
            onDismiss = { showFullScreen = false }
        )
    }
    
    // Video player dialog - removed as we now play inline
}

@Composable
private fun BannerCard(
    item: BannerItem,
    onMainClick: () -> Unit,
    onDropdownClick: (DropdownOption) -> Unit = {}
) {
    var showDropdown by remember { mutableStateOf(false) }
    Card(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { 
                if (item.hasDropdown && item.dropdownOptions.isNotEmpty()) {
                    showDropdown = true
                } else {
                    onMainClick()
                }
            },
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
        ) {
            // Banner Image Background
            AsyncImage(
                model = item.imageUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            
            // Gradient Overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = item.gradientColors.map { it.copy(alpha = 0.8f) },
                            start = Offset(0f, 0f),
                            end = Offset(1000f, 1000f)
                        )
                    )
            )
            
            // Dark gradient for text readability
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 100f
                        )
                    )
            )
            
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = item.title,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = item.subtitle,
                        fontSize = 14.sp,
                        color = Color.White.copy(alpha = 0.9f),
                        lineHeight = 20.sp
                    )
                }
                
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .background(
                            Color.White.copy(alpha = 0.2f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(48.dp)
                    )
                    
                    // Dropdown indicator
                    if (item.hasDropdown) {
                        Icon(
                            imageVector = Icons.Default.ArrowDropDown,
                            contentDescription = "Dropdown",
                            tint = Color.White,
                            modifier = Modifier
                                .size(20.dp)
                                .align(Alignment.BottomEnd)
                                .background(
                                    Color.Black.copy(alpha = 0.7f),
                                    CircleShape
                                )
                                .padding(2.dp)
                        )
                    }
                }
                
            }
            
            // Dropdown menu overlay
            if (showDropdown && item.hasDropdown) {
                DropdownOptionsOverlay(
                    options = item.dropdownOptions,
                    onDismiss = { showDropdown = false },
                    onOptionClick = { option ->
                        showDropdown = false
                        onDropdownClick(option)
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DropdownOptionsOverlay(
    options: List<DropdownOption>,
    onDismiss: () -> Unit,
    onOptionClick: (DropdownOption) -> Unit
) {
    // Full screen dialog for dropdown
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
                .background(Color.Black.copy(alpha = 0.6f))
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { onDismiss() }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .pointerInput(Unit) {
                        // Prevent clicks from propagating to the background
                        detectTapGestures {}
                    },
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground
                ),
                elevation = CardDefaults.cardElevation(
                    defaultElevation = 12.dp
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Choose Model",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        IconButton(
                            onClick = onDismiss,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Close",
                                tint = TextSecondary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                    
                    Text(
                        text = "Select the AI model you want to use",
                        fontSize = 14.sp,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 20.dp)
                    )
                    
                    // Options list
                    options.forEach { option ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 12.dp)
                                .clickable { onOptionClick(option) },
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = DarkSecondary.copy(alpha = 0.8f)
                            ),
                            border = BorderStroke(
                                width = 1.dp,
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        AccentPurple.copy(alpha = 0.3f),
                                        AccentCyan.copy(alpha = 0.3f)
                                    )
                                )
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // Model icon
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            Brush.linearGradient(
                                                colors = when {
                                                    option.title.contains("FLUX") -> listOf(
                                                        AccentPurple.copy(alpha = 0.2f),
                                                        AccentCyan.copy(alpha = 0.2f)
                                                    )
                                                    option.title.contains("DALL") -> listOf(
                                                        AccentCyan.copy(alpha = 0.2f),
                                                        Color(0xFF06B6D4).copy(alpha = 0.2f)
                                                    )
                                                    option.title.contains("Runway") -> listOf(
                                                        AccentPink.copy(alpha = 0.2f),
                                                        Color(0xFFEC4899).copy(alpha = 0.2f)
                                                    )
                                                    else -> listOf(
                                                        AccentPurple.copy(alpha = 0.2f),
                                                        AccentPink.copy(alpha = 0.2f)
                                                    )
                                                }
                                            )
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = when {
                                            option.title.contains("Ultra") -> Icons.Outlined.Stars
                                            option.title.contains("Pro") || option.title.contains("Max") -> Icons.Outlined.WorkspacePremium
                                            option.title.contains("DALL") -> Icons.Outlined.Palette
                                            option.title.contains("Runway") || option.title.contains("Video") -> Icons.Outlined.VideoLibrary
                                            option.title.contains("Stable") -> Icons.Outlined.Landscape
                                            else -> Icons.Outlined.AutoAwesome
                                        },
                                        contentDescription = null,
                                        tint = when {
                                            option.title.contains("FLUX") -> AccentPurple
                                            option.title.contains("DALL") -> AccentCyan
                                            option.title.contains("Runway") -> AccentPink
                                            else -> AccentPurple
                                        },
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                                
                                // Model details
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = option.title,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = TextPrimary
                                    )
                                    
                                    Text(
                                        text = option.subtitle,
                                        fontSize = 12.sp,
                                        color = TextSecondary,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                                
                                // Arrow icon
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = null,
                                    tint = TextSecondary.copy(alpha = 0.6f),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GeneratedImageCard(
    image: GeneratedImage,
    aspectRatio: Float = 1f,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
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
                                Color.Black.copy(alpha = 0.8f)
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
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        Icons.Outlined.AutoAwesome,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = image.model,
                        color = AccentPurple,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = image.prompt,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                        .format(Date(image.timestamp)),
                    color = TextSecondary,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyGalleryState(
    onCreateClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        border = BorderStroke(
            width = 1.dp,
            brush = Brush.linearGradient(
                colors = listOf(
                    AccentPurple.copy(alpha = 0.3f),
                    AccentPink.copy(alpha = 0.3f)
                )
            )
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Outlined.ImageNotSupported,
                contentDescription = null,
                tint = TextSecondary,
                modifier = Modifier.size(48.dp)
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Text(
                "No creations yet",
                color = TextPrimary,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                "Start creating amazing AI art",
                color = TextSecondary,
                fontSize = 14.sp
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Button(
                onClick = onCreateClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple
                )
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Now")
            }
        }
    }
}

@Composable
private fun ComingSoonQuickActionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(100.dp)
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFE082).copy(alpha = 0.8f)
        ),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFFFF9800).copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        Color(0xFFFF9800).copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color(0xFF37474F),
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            
            Text(
                text = title,
                color = Color(0xFF37474F),
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = subtitle,
                color = Color(0xFF6C757D),
                fontSize = 10.sp,
                fontWeight = FontWeight.Normal
            )
        }
    }
}

@Composable
private fun GeneratedVideoCard(
    video: GeneratedVideo,
    aspectRatio: Float = 16f / 9f,
    onClick: () -> Unit
) {
    var isPlaying by remember { mutableStateOf(false) }
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .clickable { 
                isPlaying = !isPlaying
                onClick()
            },
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Box {
            if (isPlaying) {
                // Show video player inline
                InlineVideoPlayer(
                    videoUrl = video.videoUrl,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                // Video thumbnail or placeholder
                if (video.thumbnailPath != null && File(video.thumbnailPath).exists()) {
                    // Display actual video thumbnail
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(File(video.thumbnailPath))
                            .crossfade(true)
                            .build(),
                        contentDescription = "Video thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    // Fallback gradient background
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF4C1D95),
                                        Color(0xFF7C3AED)
                                    )
                                )
                            )
                    )
                }
                
                // Dark overlay for better contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.2f))
                )
                
                // Play button overlay
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .align(Alignment.Center)
                        .background(
                            Color.Black.copy(alpha = 0.7f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play Video",
                        tint = Color.White,
                        modifier = Modifier.size(36.dp)
                    )
                }
                
                // Gradient overlay for text
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.9f)
                                ),
                                startY = 150f
                            )
                        )
                )
                
                // Video info - removed the video icon and text
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(12.dp)
                ) {
                    Text(
                        text = video.model,
                        color = AccentPink,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = video.prompt,
                        color = TextPrimary,
                        fontSize = 12.sp,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 16.sp
                    )
                    
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    Text(
                        text = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
                            .format(Date(video.timestamp)),
                        color = TextSecondary,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

// VideoPlayerDialog removed - videos now play inline

@Composable
private fun InlineVideoPlayer(
    videoUrl: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    val exoPlayer = remember(videoUrl) {
        ExoPlayer.Builder(context).build().apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
    }
    
    DisposableEffect(exoPlayer) {
        onDispose {
            exoPlayer.release()
        }
    }
    
    AndroidView(
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = true
            }
        },
        modifier = modifier
    )
}

@Composable
private fun QuickActionCard(
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(100.dp)
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        border = BorderStroke(
            width = 1.dp,
            color = AccentPurple.copy(alpha = 0.2f)
        )
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        AccentPurple.copy(alpha = 0.1f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = AccentPurple,
                    modifier = Modifier.size(28.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Text(
                text = title,
                color = TextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

data class QuickAction(
    val title: String,
    val icon: ImageVector,
    val route: String
)

@Composable
private fun FullScreenImageViewer(
    image: GeneratedImage,
    onDismiss: () -> Unit
) {
    var showInfo by remember { mutableStateOf(true) }
    
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
                .background(Color.Black)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showInfo = !showInfo }
                    )
                }
        ) {
            // Full screen image
            AsyncImage(
                model = image.imageData,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.Center),
                contentScale = ContentScale.Fit
            )
            
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .background(
                        Color.Black.copy(alpha = 0.5f),
                        CircleShape
                    )
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White
                )
            }
            
            // Image info overlay
            AnimatedVisibility(
                visible = showInfo,
                modifier = Modifier.align(Alignment.BottomCenter),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.9f)
                                ),
                                startY = 0f
                            )
                        )
                        .padding(24.dp)
                ) {
                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                Icons.Outlined.AutoAwesome,
                                contentDescription = null,
                                tint = AccentPurple,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = image.model,
                                color = AccentPurple,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = image.prompt,
                            color = TextPrimary,
                            fontSize = 16.sp,
                            lineHeight = 22.sp
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                        
                        Text(
                            text = SimpleDateFormat("MMMM dd, yyyy • HH:mm", Locale.getDefault())
                                .format(Date(image.timestamp)),
                            color = TextSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}
