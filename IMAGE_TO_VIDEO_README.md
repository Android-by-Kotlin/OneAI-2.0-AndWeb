# Image-to-Video Generation Feature

## Overview
A professional Image-to-Video generation feature integrated into the OneAI Android app using the ModelsLab API. This feature allows users to transform static images into dynamic videos using AI.

## Features Implemented

### 1. **API Integration** (`ModelsLabVideoService.kt`)
- RESTful API integration with ModelsLab's image-to-video endpoint
- Automatic polling for video generation status
- Full error handling and timeout management
- Support for both URL and local image inputs

### 2. **Data Models** (`ImageToVideoModels.kt`)
- `ImageToVideoRequest` - API request structure
- `ImageToVideoResponse` - API response parsing
- `ImageToVideoState` - UI state management (Idle, Loading, Processing, Success, Error)
- `ImageToVideoHistoryItem` - History tracking

### 3. **ViewModel** (`ImageToVideoViewModel.kt`)
- Reactive state management using Kotlin Flow
- Automatic status polling with 5-second intervals
- Video generation history tracking
- Real-time UI updates
- Comprehensive error handling

### 4. **Professional UI** (`ImageToVideoScreen.kt`)
- Modern dark theme matching the app design
- Image upload via file picker or URL input
- Large textarea for prompt input
- Real-time status indicators (Loading, Processing, Error)
- Video preview dialog with ExoPlayer integration
- Gradient buttons and glassmorphic cards
- Smooth animations and transitions

### 5. **Navigation Integration**
- Route added to `AppNavigation.kt`: `"imageToVideo"`
- Accessible from both home screens:
  - `ModernGlassmorphismHomeScreen.kt`
  - `EnhancedHomeScreen.kt` (banner carousel)

## API Endpoint
```
POST https://modelslab.com/api/v7/video-fusion/image-to-video
```

### Request Format
```json
{
  "model_id": "seedance-i2v",
  "init_image": "https://example.com/image.jpg",
  "prompt": "a woman sitting in car, cars passing by, girl talking with us",
  "key": "YOUR_API_KEY"
}
```

### Response Handling
The feature handles three response states:
1. **Success** - Video URL returned immediately
2. **Processing** - Request ID returned, automatic polling begins
3. **Error** - Error message displayed to user

## File Structure
```
app/src/main/java/max/ohm/oneai/imagetovideo/
├── ImageToVideoModels.kt      # Data models and states
├── ModelsLabVideoService.kt   # API service
├── ImageToVideoViewModel.kt   # Business logic & state management
└── ImageToVideoScreen.kt      # UI composables
```

## UI Flow

### 1. **Input Section**
- **Reference Image Upload**
  - Tap to open image picker
  - Or paste image URL
  - Preview selected image
  - Remove/change image option

### 2. **Prompt Input**
- Multi-line text field
- Placeholder example provided
- Character limit appropriate for API

### 3. **Generation Process**
- **Loading State**: Shows "Generating..." with spinner
- **Processing State**: Shows "Processing..." with ETA updates
- **Success State**: Opens video player dialog
- **Error State**: Displays error message with retry option

### 4. **Video Result**
- Full-screen video player dialog
- Play/pause controls
- Download option (TODO)
- Close button

## Color Scheme
- **Dark Background**: `#0A0E27`
- **Dark Card**: `#141833`
- **Accent Purple**: `#8B5CF6`
- **Accent Pink**: `#EC4899`
- **Text Primary**: `#FFFFFF`
- **Text Secondary**: `#B8BCC8`

## Key Technologies
- **Jetpack Compose** - Modern Android UI
- **Kotlin Coroutines** - Asynchronous programming
- **Retrofit** - HTTP client
- **Coil** - Image loading
- **ExoPlayer** - Video playback
- **Material3** - Material Design components

## Usage Example

### Navigate to Feature
```kotlin
navController.navigate("imageToVideo")
```

### From Home Screen
Users can access the feature via:
- Banner carousel (EnhancedHomeScreen)
- Category card (ModernGlassmorphismHomeScreen)

## Testing Checklist
- [ ] Upload image from gallery
- [ ] Enter image URL
- [ ] Submit with valid prompt
- [ ] Test error handling (invalid URL, API errors)
- [ ] Test video playback
- [ ] Test navigation back
- [ ] Test on different screen sizes
- [ ] Test with slow network connection

## Future Enhancements
1. **Download Functionality** - Save generated videos locally
2. **History Persistence** - DataStore integration for viewing past generations
3. **Advanced Options** - Video duration, resolution, style settings
4. **Batch Processing** - Generate multiple videos at once
5. **Sharing** - Share videos directly to social media

## API Key Security
⚠️ **Important**: The API key is currently hardcoded in `ModelsLabVideoService.kt`. For production:
1. Move API key to `local.properties`
2. Use BuildConfig to access it
3. Consider implementing backend proxy for additional security

## Dependencies
All required dependencies are already included in `app/build.gradle.kts`:
- Retrofit & Gson
- OkHttp Logging Interceptor
- Coil Compose
- Media3 ExoPlayer
- Kotlin Coroutines

## Support
For API-related issues, refer to ModelsLab documentation:
https://modelslab.com/docs

## License
This implementation is part of the OneAI project.

