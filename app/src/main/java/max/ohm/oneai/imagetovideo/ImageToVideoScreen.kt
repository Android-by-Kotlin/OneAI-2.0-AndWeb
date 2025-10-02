package max.ohm.oneai.imagetovideo

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import androidx.compose.ui.draw.blur
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import android.app.Application
import android.util.Log

// Modern color palette
private val DarkBackground = Color(0xFF0A0E27)
private val DarkCard = Color(0xFF141833)
private val AccentPurple = Color(0xFF8B5CF6)
private val AccentPink = Color(0xFFEC4899)
private val AccentGreen = Color(0xFF10B981)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFFB8BCC8)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToVideoScreen(
    navController: NavController? = null
) {
    val context = LocalContext.current
    val application = remember(context) {
        context.applicationContext as Application
    }
    val viewModel: ImageToVideoViewModel = viewModel(
        factory = ImageToVideoViewModelFactory(application)
    )
    val state by viewModel.state.collectAsState()
    val referenceImageUri by viewModel.referenceImageUri.collectAsState()
    val prompt by viewModel.prompt.collectAsState()
    val isPolling by viewModel.isPolling.collectAsState()
    
    var showVideoDialog by remember { mutableStateOf(false) }
    var generatedVideoUrl by remember { mutableStateOf<String?>(null) }
    var imageUrl by remember { mutableStateOf("") }
    
    // Image picker launcher
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            viewModel.setReferenceImage(it)
        }
    }
    
    // Handle state changes
    LaunchedEffect(state) {
        when (val currentState = state) {
            is ImageToVideoState.Success -> {
                generatedVideoUrl = currentState.videoUrl
                showVideoDialog = true
                Log.d("ImageToVideoScreen", "Video generation successful, showing dialog with URL: ${currentState.videoUrl}")
            }
            is ImageToVideoState.Error -> {
                Log.e("ImageToVideoScreen", "Error: ${currentState.message}")
            }
            is ImageToVideoState.Processing -> {
                Log.d("ImageToVideoScreen", "Processing video...")
            }
            else -> {}
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                AccentPurple.copy(alpha = 0.3f),
                                DarkBackground
                            )
                        )
                    )
                    .padding(horizontal = 20.dp, vertical = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = { navController?.popBackStack() },
                        modifier = Modifier
                            .size(40.dp)
                            .background(DarkCard.copy(alpha = 0.8f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.width(16.dp))
                    
                    Column {
                        Text(
                            text = "Image to Video",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = "Bring your images to life",
                            fontSize = 14.sp,
                            color = TextSecondary
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Input Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                // Reference Image Section
                Text(
                    text = "Reference Image URL",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                // Info message about using URLs
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = AccentPurple.copy(alpha = 0.1f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = AccentPurple,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Please use a direct image URL (e.g., from imgur.com). Local files are not supported.",
                            fontSize = 13.sp,
                            color = TextPrimary.copy(alpha = 0.9f)
                        )
                    }
                }
                
                // Info card showing we only support URLs
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkCard
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Image",
                            modifier = Modifier.size(64.dp),
                            tint = AccentPurple
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Enter Image URL Below",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Direct image URLs only (e.g., from imgur.com)",
                            fontSize = 13.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Image URL Input
                OutlinedTextField(
                    value = imageUrl,
                    onValueChange = { imageUrl = it },
                    label = { Text("Or paste image URL (Required if no image selected)", color = TextSecondary) },
                    placeholder = { Text("https://example.com/image.jpg", color = TextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(DarkCard, RoundedCornerShape(16.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = DarkCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentPurple
                    ),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = null,
                            tint = TextSecondary
                        )
                    }
                )
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Prompt Section
                Text(
                    text = "Prompt",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary,
                    modifier = Modifier.padding(bottom = 12.dp)
                )
                
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { viewModel.updatePrompt(it) },
                    label = { Text("Describe the motion and scene", color = TextSecondary) },
                    placeholder = { Text("a woman sitting in car, cars passing by, girl talking with us", color = TextSecondary.copy(alpha = 0.5f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .background(DarkCard, RoundedCornerShape(16.dp)),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentPurple,
                        unfocusedBorderColor = DarkCard,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = AccentPurple
                    ),
                    shape = RoundedCornerShape(16.dp),
                    maxLines = 5
                )
                
                Spacer(modifier = Modifier.height(32.dp))
                
                // Generate Button
                Button(
                    onClick = {
                        // Only use URL input, not local files
                        if (imageUrl.isNotBlank()) {
                            viewModel.generateVideo(imageUrl.trim(), prompt)
                        } else if (referenceImageUri != null) {
                            // Show error for local files
                            viewModel.generateVideo("", "")
                        } else {
                            viewModel.generateVideo("", prompt)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Transparent
                    ),
                    enabled = state !is ImageToVideoState.Loading && state !is ImageToVideoState.Processing,
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    colors = listOf(AccentPurple, AccentPink)
                                ),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state is ImageToVideoState.Loading || state is ImageToVideoState.Processing) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = if (state is ImageToVideoState.Processing) "Processing..." else "Generating...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        } else {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    tint = Color.White
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Generate Video",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // Status Messages
                AnimatedVisibility(
                    visible = state is ImageToVideoState.Error,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFFEF4444).copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Error,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = (state as? ImageToVideoState.Error)?.message ?: "",
                                color = Color(0xFFEF4444),
                                fontSize = 14.sp
                            )
                        }
                    }
                }
                
                AnimatedVisibility(
                    visible = state is ImageToVideoState.Processing,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = AccentPurple.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = AccentPurple,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Video is being processed...",
                                    color = TextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    text = "This may take a few minutes",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
        
        // Video Result Dialog
        if (showVideoDialog && generatedVideoUrl != null) {
            VideoResultDialog(
                videoUrl = generatedVideoUrl!!,
                onDismiss = {
                    showVideoDialog = false
                    viewModel.resetState()
                },
                onDownload = {
                    // TODO: Implement download functionality
                }
            )
        }
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun VideoResultDialog(
    videoUrl: String,
    onDismiss: () -> Unit,
    onDownload: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkCard,
        shape = RoundedCornerShape(24.dp),
        title = {
            Text(
                text = "Video Generated Successfully!",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Black)
                ) {
                    ImageToVideoPlayer(
                        videoUrl = videoUrl,
                        autoplay = true,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Your video is ready!",
                    fontSize = 14.sp,
                    color = TextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentPurple
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Close", color = Color.White)
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDownload,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentPurple)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = null,
                        tint = AccentPurple,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Download", color = AccentPurple)
                }
            }
        }
    )
}

// Use the same VideoPlayer implementation as text-to-video generation
private const val SCREEN_TAG = "ImageToVideoScreen"
private const val APP_USER_AGENT = "OneAI Android App"

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun ImageToVideoPlayer(
    videoUrl: String,
    autoplay: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isBuffering = remember { mutableStateOf(true) }
    val playerErrorMessage = remember { mutableStateOf<String?>(null) }

    Log.d(SCREEN_TAG, "ImageToVideoPlayer recomposing. URL: $videoUrl, Autoplay: $autoplay")

    val exoPlayer = remember(videoUrl) {
        Log.d(SCREEN_TAG, "Creating new ExoPlayer instance for URL: $videoUrl")
        val player = if (videoUrl.startsWith("http") || videoUrl.startsWith("https")) {
            // For HTTP URLs, use custom data source factory
            val httpDataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(APP_USER_AGENT)
                .setAllowCrossProtocolRedirects(true)
                .setConnectTimeoutMs(30000) // Increase timeout to 30 seconds
                .setReadTimeoutMs(30000)    // Increase read timeout to 30 seconds
                
            val mediaSourceFactory = ProgressiveMediaSource.Factory(httpDataSourceFactory)
            ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
        } else {
            // For local files, use default player
            ExoPlayer.Builder(context).build()
        }
        player
            .apply {
                Log.d(SCREEN_TAG, "Setting media item: $videoUrl")
                try {
                    val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
                    setMediaItem(mediaItem)
                    addListener(object : Player.Listener {
                        override fun onPlaybackStateChanged(playbackState: Int) {
                            isBuffering.value = playbackState == Player.STATE_BUFFERING
                            if (playbackState == Player.STATE_ENDED) {
                                this@apply.seekTo(0)
                                this@apply.playWhenReady = false // Don't auto-restart like text-to-video
                            }
                            Log.d(SCREEN_TAG, "ExoPlayer state changed: $playbackState, isBuffering: ${isBuffering.value}")
                        }
                        override fun onIsPlayingChanged(isPlayingNow: Boolean) { 
                            Log.d(SCREEN_TAG, "ExoPlayer onIsPlayingChanged (actual): $isPlayingNow") 
                        }
                        override fun onPlayerError(error: PlaybackException) {
                            Log.e(SCREEN_TAG, "ExoPlayer error for $videoUrl. Code: ${error.errorCodeName}, Message: ${error.message}", error)
                            
                            // Provide more specific error messages based on error codes
                            val errorMessage = when (error.errorCode) {
                                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS -> {
                                    "HTTP Error: The server returned an error status code (2004).\n" +
                                    "This may be due to an invalid URL or authentication issue.\n" +
                                    "Please try generating the video again."
                                }
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED -> 
                                    "Network connection failed. Please check your internet connection."
                                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> 
                                    "Network connection timed out. The server may be busy or unreachable."
                                PlaybackException.ERROR_CODE_IO_INVALID_HTTP_CONTENT_TYPE -> 
                                    "Invalid content type. The URL may not point to a valid video file."
                                PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND -> 
                                    "Video file not found. The URL may be incorrect or the file has been removed."
                                else -> "Video Error: ${error.errorCodeName} (${error.errorCode})\n${error.localizedMessage?.take(100)}"
                            }
                            
                            playerErrorMessage.value = errorMessage
                            isBuffering.value = false
                        }
                    })
                    playWhenReady = autoplay
                    Log.d(SCREEN_TAG, "Preparing ExoPlayer for $videoUrl, playWhenReady=$autoplay")
                    prepare()
                } catch (e: Exception) {
                    Log.e(SCREEN_TAG, "Exception setting up ExoPlayer: ${e.message}", e)
                    playerErrorMessage.value = "Error setting up video player: ${e.message}"
                    isBuffering.value = false
                }
            }
    }

    DisposableEffect(exoPlayer) {
        onDispose {
            Log.d(SCREEN_TAG, "Releasing ExoPlayer instance for URL: $videoUrl")
            exoPlayer.release()
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize()
        )
        
        if (isBuffering.value) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        playerErrorMessage.value?.let { message ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.7f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    message, 
                    color = Color.White, 
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

