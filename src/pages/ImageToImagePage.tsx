import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, Upload, Sparkles, Download, Share2, Clock, AlertCircle, X, Plus } from 'lucide-react';
import { transformImageSingle, transformImageDual } from '../services/imageToImageService';
import { downloadImage, shareImage } from '../services/imageGenerationService';

const ImageToImagePage = () => {
  const navigate = useNavigate();
  const [prompt, setPrompt] = useState('');
  const [image1, setImage1] = useState<File | null>(null);
  const [image1Preview, setImage1Preview] = useState<string | null>(null);
  const [image2, setImage2] = useState<File | null>(null);
  const [image2Preview, setImage2Preview] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [generatedImage, setGeneratedImage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [elapsedTime, setElapsedTime] = useState(0);
  const [generationTime, setGenerationTime] = useState<number | null>(null);
  const fileInput1Ref = useRef<HTMLInputElement>(null);
  const fileInput2Ref = useRef<HTMLInputElement>(null);
  
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isLoading) {
      const startTime = Date.now();
      interval = setInterval(() => {
        setElapsedTime(Math.floor((Date.now() - startTime) / 1000));
      }, 1000);
    } else {
      setElapsedTime(0);
    }
    return () => clearInterval(interval);
  }, [isLoading]);

  const handleImage1Change = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setImage1(file);
      const reader = new FileReader();
      reader.onload = () => setImage1Preview(reader.result as string);
      reader.readAsDataURL(file);
    }
  };

  const handleImage2Change = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setImage2(file);
      const reader = new FileReader();
      reader.onload = () => setImage2Preview(reader.result as string);
      reader.readAsDataURL(file);
    }
  };

  const handleTransform = async () => {
    if (!image1) {
      setError('Please select at least one image');
      return;
    }
    if (!prompt.trim()) {
      setError('Please enter a prompt');
      return;
    }

    setIsLoading(true);
    setError(null);
    setGeneratedImage(null);
    setGenerationTime(null);

    try {
      let result;
      if (image2) {
        // Dual image mode
        result = await transformImageDual(image1, image2, prompt);
      } else {
        // Single image mode
        result = await transformImageSingle(image1, prompt);
      }
      
      setGeneratedImage(result.imageUrl);
      if (result.generationTime) {
        setGenerationTime(result.generationTime);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to transform image');
    } finally {
      setIsLoading(false);
    }
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  const removeImage2 = () => {
    setImage2(null);
    setImage2Preview(null);
    if (fileInput2Ref.current) {
      fileInput2Ref.current.value = '';
    }
  };

  return (
    <div className="h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900 overflow-hidden flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between p-4 flex-shrink-0">
        <button onClick={() => navigate('/home')} className="flex items-center gap-2 text-gray-300 hover:text-white transition-colors">
          <ArrowLeft className="w-5 h-5" />Back
        </button>
        <h1 className="text-2xl font-bold gradient-text">Image Transform</h1>
        <div className="w-20"></div>
      </div>

      {/* Main Content */}
      <div className="flex-1 grid grid-cols-1 lg:grid-cols-2 gap-4 p-4 pt-0 overflow-hidden">
        {/* Left Side - Result */}
        <div className="flex flex-col gap-3 min-h-0">
          <motion.div initial={{ opacity: 0, x: -20 }} animate={{ opacity: 1, x: 0 }} className="glass rounded-2xl p-2 flex-1 relative overflow-hidden">
            {isLoading ? (
              <div className="absolute inset-0 flex flex-col items-center justify-center">
                <div className="relative">
                  <div className="w-20 h-20 border-4 border-purple-500/30 border-t-purple-500 rounded-full animate-spin"></div>
                  <Sparkles className="w-10 h-10 text-purple-400 absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2" />
                </div>
                <p className="text-gray-300 mt-6 text-lg">Transforming image...</p>
                <p className="text-gray-400 mt-2">{formatTime(elapsedTime)}</p>
              </div>
            ) : generatedImage ? (
              <div className="relative h-full group">
                <img src={generatedImage} alt="Transformed" className="w-full h-full object-contain rounded-xl" />
                <div className="absolute top-4 right-4 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button onClick={() => downloadImage(generatedImage, `transform-${Date.now()}.png`)} className="p-3 bg-green-600/80 hover:bg-green-600 rounded-full backdrop-blur-sm transition-colors">
                    <Download className="w-5 h-5 text-white" />
                  </button>
                  <button onClick={() => shareImage(generatedImage, prompt)} className="p-3 bg-blue-600/80 hover:bg-blue-600 rounded-full backdrop-blur-sm transition-colors">
                    <Share2 className="w-5 h-5 text-white" />
                  </button>
                </div>
              </div>
            ) : (
              <div className="absolute inset-0 flex flex-col items-center justify-center">
                <Upload className="w-16 h-16 text-gray-600 mb-4" />
                <p className="text-gray-400 text-lg">Result will appear here</p>
                <p className="text-gray-500 text-sm mt-2">Upload image(s) and transform</p>
              </div>
            )}
          </motion.div>

          {generationTime && !isLoading && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-center justify-center gap-2 text-purple-400 text-sm">
              <Clock className="w-4 h-4" />
              <span>Transformed in {generationTime.toFixed(1)}s</span>
            </motion.div>
          )}
        </div>

        {/* Right Side - Controls */}
        <div className="flex flex-col gap-3 min-h-0">
          {/* Error Display */}
          <AnimatePresence>
            {error && (
              <motion.div initial={{ opacity: 0, y: -10 }} animate={{ opacity: 1, y: 0 }} exit={{ opacity: 0, y: -10 }} className="glass-error rounded-xl p-3 flex items-start gap-3 flex-shrink-0">
                <AlertCircle className="w-5 h-5 text-red-400 flex-shrink-0 mt-0.5" />
                <div className="flex-1 min-w-0">
                  <p className="text-red-400 font-medium text-sm">Error</p>
                  <p className="text-red-300 text-xs mt-1">{error}</p>
                </div>
                <button onClick={() => setError(null)} className="text-red-400 hover:text-red-300 text-xl leading-none">×</button>
              </motion.div>
            )}
          </AnimatePresence>

          {/* Image Upload Section */}
          <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} className="glass rounded-xl p-4 flex-shrink-0">
            <label className="text-gray-300 text-sm font-medium mb-2 block">Input Images</label>
            <div className="grid grid-cols-2 gap-2">
              {/* Image 1 */}
              <div className="relative">
                <input ref={fileInput1Ref} type="file" accept="image/*" onChange={handleImage1Change} className="hidden" id="file1" />
                <label htmlFor="file1" className="cursor-pointer block">
                  {image1Preview ? (
                    <div className="relative aspect-square rounded-lg overflow-hidden border-2 border-purple-500">
                      <img src={image1Preview} alt="Preview 1" className="w-full h-full object-cover" />
                    </div>
                  ) : (
                    <div className="aspect-square rounded-lg border-2 border-dashed border-gray-600 hover:border-purple-500 flex flex-col items-center justify-center transition-colors">
                      <Upload className="w-8 h-8 text-gray-500 mb-2" />
                      <span className="text-xs text-gray-500">Image 1</span>
                    </div>
                  )}
                </label>
              </div>

              {/* Image 2 (Optional) */}
              <div className="relative">
                {image2Preview ? (
                  <div className="relative aspect-square rounded-lg overflow-hidden border-2 border-purple-500">
                    <img src={image2Preview} alt="Preview 2" className="w-full h-full object-cover" />
                    <button onClick={removeImage2} className="absolute top-1 right-1 p-1 bg-red-600 hover:bg-red-700 rounded-full">
                      <X className="w-4 h-4 text-white" />
                    </button>
                  </div>
                ) : (
                  <>
                    <input ref={fileInput2Ref} type="file" accept="image/*" onChange={handleImage2Change} className="hidden" id="file2" />
                    <label htmlFor="file2" className="cursor-pointer block">
                      <div className="aspect-square rounded-lg border-2 border-dashed border-gray-700 hover:border-purple-500 flex flex-col items-center justify-center transition-colors">
                        <Plus className="w-8 h-8 text-gray-600 mb-2" />
                        <span className="text-xs text-gray-600">Image 2</span>
                        <span className="text-xs text-gray-700 mt-1">(Optional)</span>
                      </div>
                    </label>
                  </>
                )}
              </div>
            </div>
          </motion.div>

          {/* Prompt Input */}
          <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.1 }} className="glass rounded-xl p-4 flex-1 min-h-0 flex flex-col">
            <label className="text-gray-300 text-sm font-medium mb-2 block flex-shrink-0">Transformation Prompt</label>
            <textarea value={prompt} onChange={(e) => setPrompt(e.target.value)} disabled={isLoading} placeholder="Describe how you want to transform the image..." className="w-full flex-1 bg-gray-800/50 text-white rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none disabled:opacity-50" />
          </motion.div>

          {/* Transform Button */}
          <motion.button initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.2 }} onClick={handleTransform} disabled={isLoading || !image1 || !prompt.trim()} className="w-full glass hover:bg-white/10 rounded-xl p-4 flex items-center justify-center gap-3 transition-all disabled:opacity-50 disabled:cursor-not-allowed group flex-shrink-0">
            {isLoading ? (
              <>
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                <span className="text-white font-semibold">Transforming...</span>
              </>
            ) : (
              <>
                <Sparkles className="w-5 h-5 text-purple-400 group-hover:scale-110 transition-transform" />
                <span className="text-white font-semibold">Transform Image</span>
              </>
            )}
          </motion.button>

          <p className="text-center text-gray-500 text-xs flex-shrink-0">⚠️ Processing may take time for dual images</p>
        </div>
      </div>
    </div>
  );
};
export default ImageToImagePage;
