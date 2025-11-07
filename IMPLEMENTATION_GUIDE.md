# OneAI Web - Complete Implementation Guide

This guide will help you complete all the remaining pages and features for the OneAI web application.

## ✅ Already Completed

1. **Project Setup**
   - ✅ Vite + React + TypeScript configured
   - ✅ Tailwind CSS with custom utilities
   - ✅ Firebase configuration
   - ✅ Router setup
   - ✅ Authentication context

2. **Pages Created**
   - ✅ SplashScreen.tsx - Animated splash screen with logo
   - ✅ LoginPage.tsx - Complete login/signup with Google auth
   - ✅ HomePage.tsx - Modern glassmorphic home with all feature cards

## 📝 Remaining Pages to Implement

### 1. ProfilePage.tsx

**Location**: `src/pages/ProfilePage.tsx`

**Features**:
- Display user information (name, email, photo)
- Edit profile (update display name, photo)
- Show usage statistics
- Logout button
- Theme settings (if applicable)

**Key Components**:
```typescript
- User avatar/photo display
- Editable text fields
- Save/Cancel buttons
- Stats cards (images generated, videos created, etc.)
```

**Firebase Integration**:
- Read from Firestore: `db.collection('users').doc(userId)`
- Update profile: `updateProfile()` from Firebase Auth
- Store additional data in Firestore

---

### 2. ImageGeneratorPage.tsx

**Location**: `src/pages/ImageGeneratorPage.tsx`

**Features**:
- Text input for prompts
- Model selector (FLUX 1.1 Pro, ImageGen-4, SDXL)
- Generate button
- Loading state with progress
- Display generated image
- Download button
- Save to gallery
- History of generated images

**API Integration**:
```typescript
// Use Nebius API for FLUX models
import { API_ENDPOINTS, API_CONFIG } from '../config/api';
import axios from 'axios';

const generateImage = async (prompt: string, model: string) => {
  const response = await axios.post(
    API_ENDPOINTS.NEBIUS_IMAGE_GEN,
    {
      model: model,
      prompt: prompt,
      n: 1,
      size: "1024x1024"
    },
    {
      headers: {
        'Authorization': `Bearer ${API_CONFIG.IMAGE_GEN_API_KEY}`,
        'Content-Type': 'application/json'
      }
    }
  );
  return response.data;
};
```

**UI Layout**:
1. Top bar: Model selector + settings
2. Center: Prompt input (large textarea)
3. Bottom: Generate button
4. Right panel: Generated image display
5. Bottom panel: History gallery

---

### 3. ImageToImagePage.tsx

**Location**: `src/pages/ImageToImagePage.tsx`

**Features**:
- Image upload (drag & drop or file picker)
- Prompt input for transformation
- Model selector (Nano Banana, Ghibli, FLUX Kontext Pro)
- Strength slider (how much to transform)
- Generate button
- Before/After comparison view
- Download and save buttons
- Brush tool for inpainting (optional advanced feature)

**API Integration**:
```typescript
const transformImage = async (
  imageFile: File, 
  prompt: string, 
  model: string
) => {
  const formData = new FormData();
  formData.append('image', imageFile);
  formData.append('prompt', prompt);
  formData.append('model', model);
  
  const response = await axios.post(
    API_ENDPOINTS.MODELSLAB_IMAGE_TO_IMAGE,
    formData,
    {
      headers: {
        'Authorization': `Bearer ${API_CONFIG.MODELSLAB_API_KEY}`,
      }
    }
  );
  return response.data;
};
```

---

### 4. ChatBotPage.tsx

**Location**: `src/pages/ChatBotPage.tsx`

**Features**:
- Chat interface with message bubbles
- Model selector (Gemini 2.0 Flash, Claude, etc.)
- Text input at bottom
- Send button
- Message history
- Typing indicator
- Code highlighting (if applicable)
- Clear chat button

**API Integration**:
```typescript
// For Gemini
import { GoogleGenerativeAI } from "@google/generative-ai";

const genAI = new GoogleGenerativeAI(API_CONFIG.GEMINI_API_KEY);
const model = genAI.getGenerativeModel({ model: "gemini-2.0-flash" });

const sendMessage = async (message: string) => {
  const result = await model.generateContent(message);
  const response = await result.response;
  return response.text();
};

// For OpenRouter (Claude, etc.)
const sendMessageOpenRouter = async (
  message: string, 
  model: string
) => {
  const response = await axios.post(
    API_ENDPOINTS.OPENROUTER_CHAT,
    {
      model: model,
      messages: [{ role: "user", content: message }]
    },
    {
      headers: {
        'Authorization': `Bearer ${API_CONFIG.OPENROUTER_API_KEY}`,
        'Content-Type': 'application/json'
      }
    }
  );
  return response.data.choices[0].message.content;
};
```

**UI Components**:
- Message bubble component (user vs AI)
- Input field with send button
- Model selector dropdown
- Chat history display

---

### 5. VideoGenerationPage.tsx

**Location**: `src/pages/VideoGenerationPage.tsx`

**Features**:
- Text prompt input
- Video length selector
- Style selector (realistic, animated, etc.)
- Generate button
- Progress indicator (video generation takes time)
- Video player to preview
- Download button
- Save to gallery

**API Integration**:
```typescript
const generateVideo = async (
  prompt: string, 
  duration: number
) => {
  const response = await axios.post(
    API_ENDPOINTS.VIDEO_GENERATION,
    {
      prompt: prompt,
      duration: duration,
      style: "realistic"
    },
    {
      headers: {
        'Authorization': `Bearer ${API_CONFIG.VIDEO_GEN_API_KEY}`,
      }
    }
  );
  return response.data;
};
```

---

### 6. ImageToVideoPage.tsx

**Location**: `src/pages/ImageToVideoPage.tsx`

**Features**:
- Image upload
- Motion prompt (describe how image should move)
- Duration selector
- Generate button
- Video player
- Download button

**API Integration**: Similar to VideoGenerationPage but with image upload

---

### 7. SketchToImagePage.tsx

**Location**: `src/pages/SketchToImagePage.tsx`

**Features**:
- Canvas for drawing/sketching
- Upload sketch option
- Prompt input (describe desired result)
- Style selector
- Generate button
- Result display
- Download/save buttons

**Drawing Canvas**:
```typescript
import { useRef, useState } from 'react';

const SketchCanvas = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  
  // Implement drawing logic
  // Convert canvas to image
  // Send to API
};
```

---

### 8. LiveAvatarPage.tsx

**Location**: `src/pages/LiveAvatarPage.tsx`

**Features**:
- Avatar video display
- Text input for conversation
- Voice input button (optional)
- Conversation history
- Avatar animation during speech

**API Integration**: HeyGen streaming API (complex, may need WebRTC)

---

### 9. ProfilePage.tsx

**Location**: `src/pages/ProfilePage.tsx`

**Features**:
- User info display
- Edit profile form
- Statistics (generations count, etc.)
- Settings
- Logout

---

## 🛠️ Additional Components Needed

### 1. LoadingSpinner Component

**Location**: `src/components/LoadingSpinner.tsx`

```typescript
export const LoadingSpinner = () => (
  <div className="flex items-center justify-center">
    <div className="w-8 h-8 border-4 border-primary border-t-transparent rounded-full animate-spin"></div>
  </div>
);
```

### 2. ImageGallery Component

**Location**: `src/components/ImageGallery.tsx`

Display grid of generated images with click to view full size.

### 3. VideoPlayer Component

**Location**: `src/components/VideoPlayer.tsx`

Custom video player with controls.

### 4. FileUpload Component

**Location**: `src/components/FileUpload.tsx`

Drag & drop file upload with preview.

---

## 📦 Additional Packages Needed

```bash
npm install @google/generative-ai
npm install react-canvas-draw  # For sketch canvas
npm install react-player       # For video playback
```

---

## 🔑 Environment Variables

Create `.env` file in root:

```env
VITE_GEMINI_API_KEY=your_key_here
VITE_OPENROUTER_API_KEY=your_key_here
VITE_IMAGE_GEN_API_KEY=your_key_here
VITE_VIDEO_GEN_API_KEY=your_key_here
VITE_MODELSLAB_API_KEY=your_key_here
VITE_STABILITY_API_KEY=your_key_here
VITE_HEYGEN_API_KEY=your_key_here
```

---

## 🎨 Common UI Patterns

### Glass Card Container
```typescript
<div className="glass rounded-2xl p-6">
  {/* Content */}
</div>
```

### Gradient Button
```typescript
<button className="px-6 py-3 bg-gradient-to-r from-primary to-accent text-white rounded-lg hover:opacity-90 transition-opacity">
  Generate
</button>
```

### Input Field
```typescript
<input 
  className="w-full px-4 py-3 bg-white bg-opacity-5 border border-white border-opacity-10 rounded-lg text-white placeholder-gray-400 focus:outline-none focus:ring-2 focus:ring-primary"
  placeholder="Enter prompt..."
/>
```

---

## 🚀 Implementation Order (Recommended)

1. **ProfilePage** - Simple, good warm-up
2. **ImageGeneratorPage** - Core feature, test API integration
3. **ChatBotPage** - Important feature, moderate complexity
4. **ImageToImagePage** - Build on image generation knowledge
5. **VideoGenerationPage** - Similar to image generation
6. **ImageToVideoPage** - Combine image and video concepts
7. **SketchToImagePage** - Canvas drawing is complex
8. **LiveAvatarPage** - Most complex, requires WebRTC/streaming

---

## 🧪 Testing Each Feature

1. Test without API keys first (mock responses)
2. Add API keys one at a time
3. Test error handling
4. Test loading states
5. Test mobile responsiveness

---

## 📱 Responsive Design Tips

Use Tailwind responsive classes:
- `sm:` - Small devices (640px+)
- `md:` - Medium devices (768px+)
- `lg:` - Large devices (1024px+)
- `xl:` - Extra large devices (1280px+)

Example:
```typescript
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
```

---

## 🎯 Quick Start

1. Start with a simple page (ProfilePage recommended)
2. Copy the structure from HomePage or LoginPage
3. Add your specific features
4. Test thoroughly
5. Move to next page

**Example ProfilePage Structure**:
```typescript
import { useAuth } from '../contexts/AuthContext';
import { motion } from 'framer-motion';

const ProfilePage = () => {
  const { user, logout } = useAuth();
  
  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900 p-8">
      <div className="max-w-4xl mx-auto">
        <h1 className="text-4xl font-bold gradient-text mb-8">Profile</h1>
        
        <div className="glass rounded-2xl p-8">
          <div className="flex items-center gap-4 mb-8">
            <div className="w-20 h-20 rounded-full bg-gradient-to-r from-primary to-accent flex items-center justify-center text-2xl font-bold">
              {user?.displayName?.[0] || 'U'}
            </div>
            <div>
              <h2 className="text-2xl font-bold text-white">{user?.displayName}</h2>
              <p className="text-gray-300">{user?.email}</p>
            </div>
          </div>
          
          {/* Add more profile details here */}
        </div>
      </div>
    </div>
  );
};

export default ProfilePage;
```

---

## 📚 Resources

- Android Kotlin Code: Reference for API calls and logic
- Tailwind Docs: https://tailwindcss.com/docs
- Framer Motion: https://www.framer.com/motion/
- Firebase Docs: https://firebase.google.com/docs/web/setup

Good luck with the implementation! Start small and build incrementally. 🚀
