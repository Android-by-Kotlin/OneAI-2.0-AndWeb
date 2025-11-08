package max.ohm.oneai.videogeneration

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.navigation.NavController
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    navController: NavController,
    videoUrl: String
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    // Log the video URL for debugging
    android.util.Log.d("VideoPlayerScreen", "Playing video URL: $videoUrl")
    
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            // Create MediaItem with proper configuration
            val mediaItem = MediaItem.Builder()
                .setUri(videoUrl)
                .build()
            
            setMediaItem(mediaItem)
            
            // Add error listener
            addListener(object : Player.Listener {
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    android.util.Log.e("VideoPlayerScreen", "Playback error: ${error.message}", error)
                    errorMessage = "Failed to play video: ${error.message}"
                }
                
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            android.util.Log.d("VideoPlayerScreen", "Video ready to play")
                        }
                        Player.STATE_BUFFERING -> {
                            android.util.Log.d("VideoPlayerScreen", "Video buffering...")
                        }
                        Player.STATE_ENDED -> {
                            android.util.Log.d("VideoPlayerScreen", "Video playback ended")
                        }
                        Player.STATE_IDLE -> {
                            android.util.Log.d("VideoPlayerScreen", "Player idle")
                        }
                    }
                }
            })
            
            prepare()
            playWhenReady = true
        }
    }
    
    // Show error in Snackbar
    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Generated Video", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            Icons.Default.ArrowBack, 
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = { context ->
                    PlayerView(context).apply {
                        player = exoPlayer
                        useController = true
                        setBackgroundColor(android.graphics.Color.BLACK)
                        keepScreenOn = true
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black)
            )
        }
    }
}
