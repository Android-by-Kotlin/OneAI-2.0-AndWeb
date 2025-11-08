import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, Upload, Sparkles, Download, Share2, Clock, AlertCircle, Brush, Eraser, RotateCcw } from 'lucide-react';
import { inpaintImage } from '../services/imageToImageService';
import { downloadImage, shareImage } from '../services/imageGenerationService';

const SketchToImagePage = () => {
  const navigate = useNavigate();
  const [prompt, setPrompt] = useState('');
  const [image, setImage] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [generatedImage, setGeneratedImage] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [elapsedTime, setElapsedTime] = useState(0);
  const [generationTime, setGenerationTime] = useState<number | null>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  // Canvas states
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const [isDrawing, setIsDrawing] = useState(false);
  const [brushSize, setBrushSize] = useState(30);
  const [tool, setTool] = useState<'brush' | 'eraser'>('brush');
  
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

  // Load image on canvas
  useEffect(() => {
    if (imagePreview && canvasRef.current) {
      const canvas = canvasRef.current;
      const ctx = canvas.getContext('2d');
      const img = new Image();
      img.onload = () => {
        canvas.width = img.width;
        canvas.height = img.height;
        ctx?.drawImage(img, 0, 0);
      };
      img.src = imagePreview;
    }
  }, [imagePreview]);

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setImage(file);
      const reader = new FileReader();
      reader.onload = () => setImagePreview(reader.result as string);
      reader.readAsDataURL(file);
    }
  };

  // Canvas drawing functions
  const startDrawing = (e: React.MouseEvent<HTMLCanvasElement>) => {
    setIsDrawing(true);
    draw(e);
  };

  const stopDrawing = () => {
    setIsDrawing(false);
  };

  const draw = (e: React.MouseEvent<HTMLCanvasElement>) => {
    if (!isDrawing && e.type !== 'mousedown') return;
    
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d');
    if (!ctx || !canvas) return;

    const rect = canvas.getBoundingClientRect();
    const scaleX = canvas.width / rect.width;
    const scaleY = canvas.height / rect.height;
    const x = (e.clientX - rect.left) * scaleX;
    const y = (e.clientY - rect.top) * scaleY;

    ctx.globalCompositeOperation = tool === 'brush' ? 'source-over' : 'destination-out';
    ctx.fillStyle = 'rgba(255, 255, 255, 0.7)';
    ctx.beginPath();
    ctx.arc(x, y, brushSize, 0, Math.PI * 2);
    ctx.fill();
  };

  const clearCanvas = () => {
    const canvas = canvasRef.current;
    const ctx = canvas?.getContext('2d');
    if (!ctx || !canvas || !imagePreview) return;
    
    const img = new Image();
    img.onload = () => {
      canvas.width = img.width;
      canvas.height = img.height;
      ctx.drawImage(img, 0, 0);
    };
    img.src = imagePreview;
  };

  const handleInpaint = async () => {
    if (!image) {
      setError('Please upload an image');
      return;
    }
    if (!prompt.trim()) {
      setError('Please enter a prompt');
      return;
    }
    if (!canvasRef.current) {
      setError('Canvas not ready');
      return;
    }

    setIsLoading(true);
    setError(null);
    setGeneratedImage(null);
    setGenerationTime(null);

    try {
      // Create mask canvas (white mask on black background)
      const maskCanvas = document.createElement('canvas');
      const originalCanvas = canvasRef.current;
      maskCanvas.width = originalCanvas.width;
      maskCanvas.height = originalCanvas.height;
      const maskCtx = maskCanvas.getContext('2d')!;
      
      // Fill with black
      maskCtx.fillStyle = 'black';
      maskCtx.fillRect(0, 0, maskCanvas.width, maskCanvas.height);
      
      // Get white areas from original canvas
      const originalCtx = originalCanvas.getContext('2d')!;
      const imageData = originalCtx.getImageData(0, 0, originalCanvas.width, originalCanvas.height);
      const maskImageData = maskCtx.createImageData(maskCanvas.width, maskCanvas.height);
      
      for (let i = 0; i < imageData.data.length; i += 4) {
        const r = imageData.data[i];
        const g = imageData.data[i + 1];
        const b = imageData.data[i + 2];
        const a = imageData.data[i + 3];
        
        // If pixel is white/semi-transparent (drawn mask area), make it white in mask
        if ((r > 200 && g > 200 && b > 200) || a < 255) {
          maskImageData.data[i] = 255;
          maskImageData.data[i + 1] = 255;
          maskImageData.data[i + 2] = 255;
          maskImageData.data[i + 3] = 255;
        } else {
          // Otherwise keep it black
          maskImageData.data[i] = 0;
          maskImageData.data[i + 1] = 0;
          maskImageData.data[i + 2] = 0;
          maskImageData.data[i + 3] = 255;
        }
      }
      
      maskCtx.putImageData(maskImageData, 0, 0);
      const result = await inpaintImage(image, maskCanvas, prompt);
      
      setGeneratedImage(result.imageUrl);
      if (result.generationTime) {
        setGenerationTime(result.generationTime);
      }
    } catch (err: any) {
      setError(err.message || 'Failed to inpaint image');
    } finally {
      setIsLoading(false);
    }
  };

  const formatTime = (seconds: number) => {
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
  };

  return (
    <div className="h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900 overflow-hidden flex flex-col">
      {/* Header */}
      <div className="flex items-center justify-between p-4 flex-shrink-0">
        <button onClick={() => navigate('/home')} className="flex items-center gap-2 text-gray-300 hover:text-white transition-colors">
          <ArrowLeft className="w-5 h-5" />Back
        </button>
        <h1 className="text-2xl font-bold gradient-text">Inpainting</h1>
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
                <p className="text-gray-300 mt-6 text-lg">Inpainting...</p>
                <p className="text-gray-400 mt-2">{formatTime(elapsedTime)}</p>
              </div>
            ) : generatedImage ? (
              <div className="relative h-full group">
                <img src={generatedImage} alt="Result" className="w-full h-full object-contain rounded-xl" />
                <div className="absolute top-4 right-4 flex gap-2 opacity-0 group-hover:opacity-100 transition-opacity">
                  <button onClick={() => downloadImage(generatedImage, `inpaint-${Date.now()}.png`)} className="p-3 bg-green-600/80 hover:bg-green-600 rounded-full backdrop-blur-sm transition-colors">
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
                <p className="text-gray-500 text-sm mt-2">Upload, mask, and inpaint</p>
              </div>
            )}
          </motion.div>

          {generationTime && !isLoading && (
            <motion.div initial={{ opacity: 0 }} animate={{ opacity: 1 }} className="flex items-center justify-center gap-2 text-purple-400 text-sm">
              <Clock className="w-4 h-4" />
              <span>Processed in {generationTime.toFixed(1)}s</span>
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

          {/* Canvas Drawing Area */}
          <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} className="glass rounded-xl p-4 flex-shrink-0">
            <div className="flex items-center justify-between mb-3">
              <label className="text-gray-300 text-sm font-medium">Draw Mask</label>
              <button onClick={clearCanvas} className="text-xs text-gray-400 hover:text-white flex items-center gap-1 transition-colors">
                <RotateCcw className="w-3 h-3" />Reset
              </button>
            </div>
            <div className="space-y-3">
              {!imagePreview ? (
                <div className="w-full h-72 rounded-lg border-2 border-dashed border-gray-600 flex flex-col items-center justify-center hover:border-purple-500 transition-colors">
                  <input ref={fileInputRef} type="file" accept="image/*" onChange={handleImageChange} className="hidden" id="file-inpaint" />
                  <label htmlFor="file-inpaint" className="cursor-pointer flex flex-col items-center">
                    <Upload className="w-12 h-12 text-gray-500 mb-2" />
                    <span className="text-sm font-medium text-gray-400">Upload Image</span>
                  </label>
                </div>
              ) : (
                <div className="relative w-full h-72 flex items-center justify-center bg-gray-800/30 rounded-lg">
                  <canvas
                    ref={canvasRef}
                    onMouseDown={startDrawing}
                    onMouseMove={draw}
                    onMouseUp={stopDrawing}
                    onMouseLeave={stopDrawing}
                    className="max-w-full max-h-full border-2 border-purple-500 rounded-lg cursor-crosshair bg-gray-800/50"
                    style={{ display: 'block' }}
                  />
                </div>
              )}
              <div className="flex gap-2">
                <button onClick={() => setTool('brush')} className={`flex-1 p-3 rounded-lg flex items-center justify-center gap-2 transition-all ${tool === 'brush' ? 'bg-purple-500 text-white' : 'bg-gray-700 text-gray-300 hover:bg-gray-600'}`}>
                  <Brush className="w-4 h-4" />
                  <span className="text-sm font-medium">Brush</span>
                </button>
                <button onClick={() => setTool('eraser')} className={`flex-1 p-3 rounded-lg flex items-center justify-center gap-2 transition-all ${tool === 'eraser' ? 'bg-purple-500 text-white' : 'bg-gray-700 text-gray-300 hover:bg-gray-600'}`}>
                  <Eraser className="w-4 h-4" />
                  <span className="text-sm font-medium">Eraser</span>
                </button>
              </div>
              <div className="flex items-center gap-3">
                <span className="text-sm text-gray-400 font-medium">Size:</span>
                <input
                  type="range"
                  min="5"
                  max="50"
                  value={brushSize}
                  onChange={(e) => setBrushSize(Number(e.target.value))}
                  className="flex-1 h-2"
                />
                <span className="text-sm text-gray-300 font-medium min-w-[45px] text-right">{brushSize}px</span>
              </div>
            </div>
          </motion.div>

          {/* Prompt Input */}
          <motion.div initial={{ opacity: 0, x: 20 }} animate={{ opacity: 1, x: 0 }} transition={{ delay: 0.1 }} className="glass rounded-xl p-4 flex-shrink-0">
            <label className="text-gray-300 text-sm font-medium mb-3 block">
              What to fill in masked area
            </label>
            <textarea
              value={prompt}
              onChange={(e) => setPrompt(e.target.value)}
              disabled={isLoading}
              placeholder="Describe what should appear in the painted area..."
              rows={3}
              className="w-full bg-gray-800/50 text-white rounded-lg px-4 py-3 focus:outline-none focus:ring-2 focus:ring-purple-500 resize-none disabled:opacity-50"
            />
          </motion.div>

          {/* Process Button */}
          <motion.button
            initial={{ opacity: 0, x: 20 }}
            animate={{ opacity: 1, x: 0 }}
            transition={{ delay: 0.2 }}
            onClick={handleInpaint}
            disabled={isLoading || !image || !prompt.trim()}
            className="w-full glass hover:bg-white/10 rounded-xl p-4 flex items-center justify-center gap-3 transition-all disabled:opacity-50 disabled:cursor-not-allowed group flex-shrink-0"
          >
            {isLoading ? (
              <>
                <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                <span className="text-white font-semibold">Processing...</span>
              </>
            ) : (
              <>
                <Sparkles className="w-5 h-5 text-purple-400 group-hover:scale-110 transition-transform" />
                <span className="text-white font-semibold">Inpaint Image</span>
              </>
            )}
          </motion.button>
        </div>
      </div>
    </div>
  );
};
export default SketchToImagePage;
