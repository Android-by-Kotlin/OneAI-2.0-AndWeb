package max.ohm.oneai.homescreen

import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import max.ohm.oneai.imagegeneration.GeneratedImage
import max.ohm.oneai.imagegeneration.ImageHistoryDataStore
import max.ohm.oneai.imagetoimage.ImageToImageHistoryDataStore
import max.ohm.oneai.imagetoimage.ImageToImageHistoryItem
import max.ohm.oneai.login.LoginState
import max.ohm.oneai.login.LoginViewModel
import max.ohm.oneai.ui.theme.*
import max.ohm.oneai.videogeneration.GeneratedVideo
import max.ohm.oneai.videogeneration.VideoHistoryDataStore
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

// Data classes for AI tools and categories
data class AITool(
    val title: String,
    val subtitle: String,
    val icon: ImageVector,
    val gradient: List<Color>,
    val route: String,
    val category: String,
    val isPro: Boolean = false,
    val isSoon: Boolean = false
)

data class CategorySection(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val tools: List<AITool>
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ModernGlassmorphismHomeScreen(
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
    
    // AI Tools data
    val aiTools = remember {
        listOf(
            CategorySection(
                title = "AI Creativity",
                description = "Create stunning visuals and content",
                icon = Icons.Outlined.Brush,
                tools = listOf(
                    AITool(
                        title = "Image Generation",
                        subtitle = "Transform your ideas into beautiful artwork",
                        icon = Icons.Outlined.Image,
                        gradient = listOf(GradientPurple, GradientPink),
                        route = "enhancedImageGenerator",
                        category = "Generator"
                    ),
                    AITool(
                        title = "Image Editing",
                        subtitle = "Transform and enhance existing images",
                        icon = Icons.Outlined.Transform,
                        gradient = listOf(GradientBlue, GradientCyan),
                        route = "imageToImage",
                        category = "Editor"
                    ),
//                    AITool(
//                        title = "Image to Video",
//                        subtitle = "Transform images into dynamic videos",
//                        icon = Icons.Outlined.VideoLibrary,
//                        gradient = listOf(GradientPurple, GradientPink),
//                        route = "imageToVideo",
//                        category = "Generator"
//                    ),
                      AITool(
                        title = "Chat Assistant",
                        subtitle = "Intelligent conversations powered by AI",
                        icon = Icons.Outlined.Chat,
                        gradient = listOf(GradientIndigo, GradientBlue),
                        route = "chatbot",
                        category = "Assistant"
                    ),
                    AITool(
                        title = "Video Generation",
                        subtitle = "Create stunning videos from text",
                        icon = Icons.Outlined.VideoLibrary,
                        gradient = listOf(GradientOrange, GradientPink),
                        route = "enhancedVideoGeneration",
                        category = "Generator"
                    )
                )
            )
//            CategorySection(
//                title ="AI Conversation",
//                description = "Intelligent chat and voice interactions",
//                icon = Icons.Outlined.Chat,
//                tools = listOf(
//                    AITool(
//                        title = "AI Chat Assistant",
//                        subtitle = "Intelligent conversations powered by AI",
//                        icon = Icons.Outlined.Chat,
//                        gradient = listOf(GradientIndigo, GradientBlue),
//                        route = "chatbot",
//                        category = "Assistant"
//                    ),
//                    AITool(
//                        title = "AI Talk",
//                        subtitle = "Voice conversations with AI",
//                        icon = Icons.Outlined.RecordVoiceOver,
//                        gradient = listOf(GradientGreen, GradientCyan),
//                        route = "aiTalk",
//                        category = "Voice",
//                        isPro = true
//                    )
//                )
//            )
        )
    }
    
    // Load image, image to image, and video history
    val imageHistoryStore = remember { ImageHistoryDataStore(context) }
    val persistedImageHistory by imageHistoryStore.imageHistory.collectAsState(initial = emptyList())
    
    val imageToImageHistoryStore = remember { ImageToImageHistoryDataStore(context) }
    val persistedImageToImageHistory by imageToImageHistoryStore.imageHistory.collectAsState(initial = emptyList())
    
    val videoHistoryStore = remember { VideoHistoryDataStore(context) }
    val persistedVideoHistory by videoHistoryStore.videoHistory.collectAsState(initial = emptyList())
    
    var recentCreations by remember { mutableStateOf(listOf<Any>()) }
    
    LaunchedEffect(persistedImageHistory, persistedImageToImageHistory, persistedVideoHistory) {
        val images = persistedImageHistory.take(3).mapNotNull { item ->
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
        
        val imageToImages = persistedImageToImageHistory.take(3)
        
        val videos = persistedVideoHistory.take(2).mapNotNull { item ->
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
        
        recentCreations = (images + imageToImages + videos).sortedByDescending { 
            when (it) {
                is GeneratedImage -> it.timestamp
                is ImageToImageHistoryItem -> it.timestamp
                is GeneratedVideo -> it.timestamp
                else -> 0L
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        DarkBackground,
                        DarkerBackground,
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
        
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            // Header Section
            item {
                GlassmorphicHeader(navController)
            }
            

            
            // Categories Section
            items(aiTools) { category ->
                CategoryCard(category, navController)
            }
            
            // Recent Creations
            if (recentCreations.isNotEmpty()) {
                item {
                    RecentCreationsSection(recentCreations, navController, imageToImageHistoryStore)
                }
            }
            
            // Features Coming Soon
            item {
                ComingSoonSection()
            }
        }
    }
}

@Composable
private fun GlassmorphicHeader(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Transparent box with only OneAI text
        Card(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Transparent)
                    .padding(20.dp)
            ) {
                Column {
                    Text(
                        text = "OneAI",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        style = androidx.compose.ui.text.TextStyle(
                            shadow = androidx.compose.ui.graphics.Shadow(
                                color = GradientPurple.copy(alpha = 0.3f),
                                offset = Offset(0f, 4f),
                                blurRadius = 8f
                            )
                        )
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "One New Era of AI",
                        fontSize = 14.sp,
                        color = TextSecondary
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.width(16.dp))
        
        // Profile Avatar outside the transparent box
        Box(
            modifier = Modifier
                .size(56.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.linearGradient(
                        colors = listOf(GradientPurple, GradientPink)
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
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun WelcomeSection() {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Type your idea",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        
        // Search/Input Field with glass effect
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = GlassBackground
            ),
            border = BorderStroke(1.dp, GlassBorder)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Outlined.Add,
                    contentDescription = "Add",
                    tint = AccentPurple,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Let's make your dreams come true...",
                    color = TextMuted,
                    fontSize = 16.sp,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    Icons.Outlined.Send,
                    contentDescription = "Send",
                    tint = AccentPurple,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun QuickActionsSection(tools: List<AITool>, navController: NavController) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Choose Style",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
//            TextButton(onClick = { /* See All */ }) {
//                Text("See All", color = AccentPurple, fontSize = 14.sp)
//            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(horizontal = 4.dp)
        ) {
            items(tools) { tool ->
                QuickActionCard(tool, navController)
            }
        }
    }
}

@Composable
private fun QuickActionCard(tool: AITool, navController: NavController) {
    Card(
        modifier = Modifier
            .width(110.dp)
            .height(110.dp)
            .then(
                if (!tool.isSoon) {
                    Modifier.clickable { navController.navigate(tool.route) }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassBackground
        ),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.linearGradient(
                        colors = tool.gradient.map { it.copy(alpha = 0.1f) }
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (tool.isPro) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Pro",
                        tint = AccentOrange,
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.End)
                    )
                } else if (tool.isSoon) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Red,
                        modifier = Modifier
                            .align(Alignment.End)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = "Soon",
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
                
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(
                            brush = Brush.linearGradient(tool.gradient),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = tool.title.split(" ").take(2).joinToString(" "),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextPrimary,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun CategoryCard(category: CategorySection, navController: NavController) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Text(
            text = " Categories",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
       // Spacer(modifier = Modifier.height(4.dp))
//        Text(
//            text = "See All",
//            fontSize = 14.sp,
//            color = AccentPurple
//        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.height(300.dp)
        ) {
            items(category.tools) { tool ->
                CategoryToolCard(tool, navController)
            }
        }
    }
}

@Composable
private fun CategoryToolCard(tool: AITool, navController: NavController) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp)
            .then(
                if (!tool.isSoon) {
                    Modifier.clickable { navController.navigate(tool.route) }
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassBackground
        ),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            // Background gradient
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = tool.gradient.map { it.copy(alpha = 0.15f) },
                            start = Offset(0f, 0f),
                            end = Offset(300f, 300f)
                        )
                    )
            )
            
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(
                                brush = Brush.linearGradient(tool.gradient),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = tool.icon,
                            contentDescription = tool.title,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    
                    if (tool.isPro) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Pro",
                            tint = AccentOrange,
                            modifier = Modifier.size(16.dp)
                        )
                    } else if (tool.isSoon) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.Red,
                            modifier = Modifier.padding(2.dp)
                        ) {
                            Text(
                                text = "Soon",
                                color = Color.White,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
                
                Column {
                    Text(
                        text = tool.title,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = tool.subtitle,
                        fontSize = 12.sp,
                        color = TextSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun RecentCreationsSection(creations: List<Any>, navController: NavController, imageToImageHistoryStore: ImageToImageHistoryDataStore) {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Recent Creations",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(creations) { creation ->
                when (creation) {
                    is GeneratedImage -> {
                        RecentImageCard(creation)
                    }
                    is ImageToImageHistoryItem -> {
                        RecentImageToImageCard(creation, imageToImageHistoryStore)
                    }
                    is GeneratedVideo -> {
                        RecentVideoCard(creation)
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentImageCard(image: GeneratedImage) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassBackground
        ),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = image.imageData,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun RecentImageToImageCard(item: ImageToImageHistoryItem, dataStore: ImageToImageHistoryDataStore) {
    Card(
        modifier = Modifier
            .width(120.dp)
            .height(120.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassBackground
        ),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            val imageData = dataStore.getImageData(item)
            AsyncImage(
                model = imageData,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

@Composable
private fun RecentVideoCard(video: GeneratedVideo) {
    val context = LocalContext.current
    var showVideoPlayer by remember { mutableStateOf(false) }
    
    if (showVideoPlayer) {
        // Full screen video player dialog
        VideoPlayerDialog(
            videoUrl = video.videoUrl,
            onDismiss = { showVideoPlayer = false }
        )
    }
    
    Card(
        modifier = Modifier
            .width(160.dp)
            .height(120.dp)
            .clickable { showVideoPlayer = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = GlassBackground
        ),
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            // Show thumbnail if available, otherwise show a placeholder
            if (video.thumbnailPath != null) {
                AsyncImage(
                    model = video.thumbnailPath,
                    contentDescription = "Video thumbnail",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Placeholder background with gradient
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    GradientPurple.copy(alpha = 0.3f),
                                    GradientPink.copy(alpha = 0.3f)
                                )
                            )
                        )
                )
            }
            
            // Overlay with play button - removed VIDEO badge
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.5f)
                            )
                        )
                    )
            ) {
                // Play button in center
                Icon(
                    Icons.Default.PlayCircleFilled,
                    contentDescription = "Play Video",
                    tint = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
private fun VideoPlayerDialog(
    videoUrl: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = true
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable { onDismiss() }
        ) {
            // Video Player
            DisposableEffect(videoUrl) {
                val exoPlayer = ExoPlayer.Builder(context).build().apply {
                    val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                    setMediaItem(mediaItem)
                    playWhenReady = true
                    prepare()
                }
                
                onDispose {
                    exoPlayer.release()
                }
            }
            
            AndroidView(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        useController = true
                        player = ExoPlayer.Builder(ctx).build().apply {
                            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                            setMediaItem(mediaItem)
                            playWhenReady = true
                            prepare()
                        }
                    }
                }
            )
            
            // Close button
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
            ) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
    }
}

@Composable
private fun ComingSoonSection() {
    Column(
        modifier = Modifier.padding(16.dp)
    ) {
        Text(
            text = "Coming Soon",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(16.dp))
        
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = GlassBackground
            ),
            border = BorderStroke(1.dp, GlassBorderAccent)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        brush = Brush.linearGradient(
                            colors = listOf(
                                AccentViolet.copy(alpha = 0.1f),
                                AccentPink.copy(alpha = 0.1f)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Outlined.Upcoming,
                        contentDescription = "Coming Soon",
                        tint = AccentPurple,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "More AI Features",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextPrimary
                    )
                    Text(
                        text = "Stay tuned for updates",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}
