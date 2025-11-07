# Page Templates

Copy and paste these templates for each remaining page:

## Generic Feature Page Template

```tsx
import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowLeft, Sparkles } from 'lucide-react';

const [PAGE_NAME] = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  
  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900 p-4">
      <div className="max-w-6xl mx-auto">
        {/* Header */}
        <div className="flex items-center justify-between mb-8">
          <button 
            onClick={() => navigate('/home')}
            className="flex items-center gap-2 text-gray-300 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            Back
          </button>
          <h1 className="text-3xl font-bold gradient-text">[FEATURE NAME]</h1>
          <div className="w-20"></div> {/* Spacer for centering */}
        </div>

        {/* Main Content */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="glass rounded-2xl p-8"
        >
          <div className="text-center py-20">
            <Sparkles className="w-16 h-16 text-primary mx-auto mb-4" />
            <h2 className="text-2xl font-bold text-white mb-2">
              [FEATURE NAME]
            </h2>
            <p className="text-gray-300 mb-4">
              This feature is under development
            </p>
            <p className="text-sm text-gray-400">
              Check IMPLEMENTATION_GUIDE.md for implementation details
            </p>
          </div>
        </motion.div>
      </div>
    </div>
  );
};

export default [PAGE_NAME];
```

## Copy these for each page:

### ImageGeneratorPage.tsx
Replace [PAGE_NAME] with `ImageGeneratorPage`
Replace [FEATURE NAME] with `Text to Image Generation`

### ImageToImagePage.tsx
Replace [PAGE_NAME] with `ImageToImagePage`
Replace [FEATURE NAME] with `Image Transformation`

### ChatBotPage.tsx
Replace [PAGE_NAME] with `ChatBotPage`
Replace [FEATURE NAME] with `AI Chat Assistant`

### VideoGenerationPage.tsx
Replace [PAGE_NAME] with `VideoGenerationPage`
Replace [FEATURE NAME] with `Video Generation`

### ImageToVideoPage.tsx
Replace [PAGE_NAME] with `ImageToVideoPage`
Replace [FEATURE NAME] with `Image to Video`

### SketchToImagePage.tsx
Replace [PAGE_NAME] with `SketchToImagePage`
Replace [FEATURE NAME] with `Sketch to Image`

### LiveAvatarPage.tsx
Replace [PAGE_NAME] with `LiveAvatarPage`
Replace [FEATURE NAME] with `Live Avatar`
