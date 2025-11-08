# Video Generation Display Fixes for Seedance Model

## Problem
Videos generated successfully via the Seedance API but not playing in the Android app. The videos display correctly on the API website.

## Root Causes Identified

### 1. **API Response Field Mismatch**
- The ModelsLab API returns video URLs in the `output` field (as an array)
- The app was only checking the `outputUrls` field
- The data models had a helper method `getVideoUrls()` but it wasn't being used consistently

### 2. **URL Encoding Issues**
- Video URLs contain special characters that need proper encoding when passed as navigation parameters
- URLs were not being encoded before navigation and decoded after navigation

### 3. **Network Security Configuration**
- Video hosting domains (ModelsLab, R2.dev) were not explicitly allowed in the network security config
- This could cause SSL/TLS issues when loading videos over HTTPS

## Fixes Applied

### 1. **Fixed API Response Parsing** (`NewVideoGenerationViewModel.kt`)
Updated all Seedance-related video URL retrieval to use the `getVideoUrls()` helper method:

```kotlin
// Before:
if (!responseBody.outputUrls.isNullOrEmpty()) {
    val videoUrl = responseBody.outputUrls.first()
    // ...
}

// After:
val videoUrls = responseBody.getVideoUrls()
if (!videoUrls.isNullOrEmpty()) {
    val videoUrl = videoUrls.first()
    // ...
}
```

This fix was applied to:
- `generateWithSeedanceI2V()` - Initial response handling
- `pollSeedanceI2VStatus()` - Polling status check
- `generateWithSeedanceT2V()` - Initial response handling
- `pollSeedanceT2VStatus()` - Polling status check
- `pollSeedanceT2VWithFetchUrl()` - Fetch URL polling

### 2. **Fixed URL Navigation** (`NewVideoGenerationScreen.kt` & `AppNavigation.kt`)

**In NewVideoGenerationScreen.kt:**
```kotlin
// Before:
navController.navigate("videoPlayer?videoUrl=${state.videoUrl}")

// After:
val encodedUrl = java.net.URLEncoder.encode(state.videoUrl, "UTF-8")
navController.navigate("videoPlayer?videoUrl=$encodedUrl")
```

**In AppNavigation.kt:**
```kotlin
// Added URL decoding:
val encodedUrl = backStackEntry.arguments?.getString("videoUrl") ?: ""
val videoUrl = java.net.URLDecoder.decode(encodedUrl, "UTF-8")
VideoPlayerScreen(navController = navController, videoUrl = videoUrl)
```

### 3. **Enhanced VideoPlayerScreen** (`VideoPlayerScreen.kt`)

Added comprehensive error handling and logging:

```kotlin
// Added imports for error handling
import androidx.media3.common.Player
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState

// Added error state and listener
val snackbarHostState = remember { SnackbarHostState() }
var errorMessage by remember { mutableStateOf<String?>(null) }

// Added Player.Listener with detailed state tracking
addListener(object : Player.Listener {
    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
        android.util.Log.e("VideoPlayerScreen", "Playback error: ${error.message}", error)
        errorMessage = "Failed to play video: ${error.message}"
    }
    
    override fun onPlaybackStateChanged(playbackState: Int) {
        // Log all state changes for debugging
    }
})
```

### 4. **Updated Network Security Config** (`network_security_config.xml`)

Added domains for video hosting:

```xml
<domain-config cleartextTrafficPermitted="true">
    <domain includeSubdomains="true">api.minimax.chat</domain>
    <domain includeSubdomains="true">commondatastorage.googleapis.com</domain>
    <domain includeSubdomains="true">modelslab.com</domain>
    <domain includeSubdomains="true">r2.dev</domain>
    <domain includeSubdomains="true">pub-3626123a908346a7a8be8d9295f44e26.r2.dev</domain>
</domain-config>
```

## Testing Checklist

After applying these fixes, test the following scenarios:

- [ ] Generate video with Seedance T2V model
- [ ] Generate video with Seedance I2V model
- [ ] Verify video plays in VideoPlayerScreen
- [ ] Verify video plays in EnhancedVideoGenerationScreen (inline player)
- [ ] Test with different network conditions (WiFi, mobile data)
- [ ] Verify error messages display correctly if video fails to load
- [ ] Check logcat for detailed error information

## Expected Behavior

1. **Successful Generation:**
   - Video generates successfully via ModelsLab API
   - Video URL is extracted from either `output` or `outputUrls` field
   - URL is properly encoded for navigation
   - ExoPlayer receives the decoded URL
   - Video loads and plays automatically

2. **Error Handling:**
   - Network errors display user-friendly messages
   - Playback errors show in a Snackbar
   - Detailed logs available in logcat for debugging
   - Buffering state shows loading indicator

3. **Supported Models:**
   - `seedance-t2v` (Text to Video)
   - `seedance-i2v` (Image to Video)
   - Other models (MiniMax, CogVideoX) continue to work as before

## Additional Notes

- The `VideoPlayer` composable in `VideoGenerationScreen.kt` already had good error handling for HTTP requests
- The main issue was the data parsing and URL encoding, not the player itself
- All existing functionality for other video models (MiniMax, CogVideoX) is preserved
- The fixes are backward compatible with existing video history

## Dependencies

Ensure these dependencies are present in `build.gradle.kts`:

```kotlin
implementation("androidx.media3:media3-exoplayer:1.2.0")
implementation("androidx.media3:media3-ui:1.2.0")
implementation("androidx.media3:media3-common:1.2.0")
```

## Permissions

Required permissions in `AndroidManifest.xml`:

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```

Both are already present in the manifest.
