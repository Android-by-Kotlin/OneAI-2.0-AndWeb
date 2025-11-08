import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, Video, Loader2, Download, X, Play, Image as ImageIcon, Upload } from 'lucide-react';
import { generateImageToVideo, pollForImageToVideo } from '../services/imageToVideoService';

const ImageToVideoPage = () => {
  const navigate = useNavigate();
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  const [imageUrl, setImageUrl] = useState<string>('');
  const [uploadedImage, setUploadedImage] = useState<string | null>(null);
  const [uploadedFileName, setUploadedFileName] = useState<string | null>(null);
  const [prompt, setPrompt] = useState('');
  const [negativePrompt, setNegativePrompt] = useState('blurry, low quality, distorted, artifacts, bad anatomy');
  const [isGenerating, setIsGenerating] = useState(false);
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [generationTime, setGenerationTime] = useState<number | null>(null);
  const [isPolling, setIsPolling] = useState(false);
  const [isUploading, setIsUploading] = useState(false);

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!file.type.startsWith('image/')) {
      setError('Please upload a valid image file');
      return;
    }

    // Validate file size (max 10MB)
    if (file.size > 10 * 1024 * 1024) {
      setError('Image size should be less than 10MB');
      return;
    }

    setIsUploading(true);
    setError(null);

    try {
      const reader = new FileReader();
      reader.onloadend = () => {
        const base64String = reader.result as string;
        setUploadedImage(base64String);
        setUploadedFileName(file.name);
        setImageUrl(''); // Clear URL input when file is uploaded
        setIsUploading(false);
      };
      reader.onerror = () => {
        setError('Failed to read image file');
        setIsUploading(false);
      };
      reader.readAsDataURL(file);
    } catch (err) {
      setError('Failed to upload image');
      setIsUploading(false);
    }
  };

  const handleRemoveUpload = () => {
    setUploadedImage(null);
    setUploadedFileName(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleGenerate = async () => {
    if (!prompt.trim()) {
      setError('Please enter a prompt');
      return;
    }

    // Use uploaded image or URL
    const finalImageUrl = uploadedImage || imageUrl.trim();

    if (!finalImageUrl) {
      setError('Please upload an image or provide an image URL');
      return;
    }

    setIsGenerating(true);
    setError(null);
    setVideoUrl(null);
    setGenerationTime(null);

    try {
      const result = await generateImageToVideo(finalImageUrl, prompt, negativePrompt);
      
      if (result.videoUrl) {
        setVideoUrl(result.videoUrl);
        setGenerationTime(result.generationTime || null);
        setIsGenerating(false);
      } else if (result.requestId) {
        setIsPolling(true);
        const pollResult = await pollForImageToVideo(result.requestId);
        setVideoUrl(pollResult.videoUrl);
        setGenerationTime(pollResult.generationTime || null);
        setIsPolling(false);
        setIsGenerating(false);
      }
    } catch (err: any) {
      console.error('Image-to-video error:', err);
      setError(err.message || 'Failed to generate video');
      setIsGenerating(false);
      setIsPolling(false);
    }
  };

  const handleDownload = () => {
    if (videoUrl) {
      const link = document.createElement('a');
      link.href = videoUrl;
      link.download = `image-to-video-${Date.now()}.mp4`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900 p-6">
      <div className="h-full flex flex-col">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <button 
            onClick={() => navigate('/home')} 
            className="flex items-center gap-2 text-gray-300 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            Back
          </button>
          <h1 className="text-3xl font-bold gradient-text">Image to Video</h1>
          <div className="w-20"></div>
        </div>

        {/* Main Content */}
        <div className="flex-1 grid grid-cols-1 lg:grid-cols-3 gap-6 pb-6">
          {/* Input Section */}
          <motion.div 
            initial={{ opacity: 0, x: -20 }} 
            animate={{ opacity: 1, x: 0 }}
            className="glass rounded-2xl p-6 space-y-4 lg:col-span-1 flex flex-col"
          >
            <div>
              <div className="flex items-center gap-2 mb-4">
                <Video className="w-6 h-6 text-primary" />
                <h2 className="text-xl font-bold text-white">Create Video</h2>
              </div>
              <p className="text-gray-400 text-sm">Using Seedance I2V Model</p>
            </div>

            {/* Image Upload */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Upload Image *
              </label>
              <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleFileUpload}
                className="hidden"
                disabled={isGenerating || isUploading}
              />
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={isGenerating || isUploading || !!uploadedImage}
                className="w-full py-3 bg-gray-800/50 border-2 border-dashed border-gray-700 rounded-lg text-gray-400 hover:border-primary/50 hover:text-primary transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                {isUploading ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    Uploading...
                  </>
                ) : uploadedImage ? (
                  <>
                    <ImageIcon className="w-5 h-5" />
                    {uploadedFileName}
                  </>
                ) : (
                  <>
                    <Upload className="w-5 h-5" />
                    Click to upload image
                  </>
                )}
              </button>
              <p className="text-xs text-gray-500 mt-1">Max size: 10MB</p>
            </div>

            {/* Uploaded Image Preview */}
            {uploadedImage && (
              <div className="relative">
                <img 
                  src={uploadedImage} 
                  alt="Uploaded" 
                  className="w-full h-32 object-cover rounded-lg"
                />
                <button
                  onClick={handleRemoveUpload}
                  className="absolute top-2 right-2 p-1 bg-red-500/80 hover:bg-red-500 rounded-full text-white transition-colors"
                >
                  <X className="w-4 h-4" />
                </button>
              </div>
            )}

            {/* OR Divider */}
            <div className="flex items-center gap-2">
              <div className="flex-1 h-px bg-gray-700"></div>
              <span className="text-xs text-gray-500">OR</span>
              <div className="flex-1 h-px bg-gray-700"></div>
            </div>

            {/* Image URL Input */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Image URL
              </label>
              <input
                type="text"
                value={imageUrl}
                onChange={(e) => {
                  setImageUrl(e.target.value);
                  if (e.target.value) {
                    handleRemoveUpload(); // Clear upload if URL is entered
                  }
                }}
                placeholder="https://example.com/image.jpg"
                className="w-full px-3 py-2 bg-gray-800/50 border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary/50 text-sm"
                disabled={isGenerating || !!uploadedImage}
              />
              <p className="text-xs text-gray-500 mt-1">Or enter a publicly accessible image URL</p>
            </div>

            {/* URL Image Preview */}
            {imageUrl && !uploadedImage && (
              <div className="relative">
                <img 
                  src={imageUrl} 
                  alt="Input" 
                  className="w-full h-32 object-cover rounded-lg"
                  onError={(e) => {
                    e.currentTarget.style.display = 'none';
                  }}
                />
              </div>
            )}

            {/* Prompt Input */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Prompt *
              </label>
              <textarea
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder="Describe the motion and action..."
                className="w-full px-3 py-2 bg-gray-800/50 border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary/50 resize-none text-sm"
                rows={3}
                disabled={isGenerating}
              />
            </div>

            {/* Negative Prompt */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Negative Prompt
              </label>
              <textarea
                value={negativePrompt}
                onChange={(e) => setNegativePrompt(e.target.value)}
                placeholder="blurry, low quality, distorted..."
                className="w-full px-3 py-2 bg-gray-800/50 border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary/50 resize-none text-sm"
                rows={2}
                disabled={isGenerating}
              />
            </div>

            {/* Spacer */}
            <div className="flex-1"></div>

            {/* Settings Info */}
            <div className="bg-gray-800/30 rounded-lg p-3 space-y-2">
              <h3 className="text-xs font-medium text-gray-300">Settings</h3>
              <div className="text-xs text-gray-400">
                <div>Seedance I2V</div>
                <div>Auto duration</div>
              </div>
            </div>

            {/* Generate Button */}
            <button
              onClick={handleGenerate}
              disabled={isGenerating || !prompt.trim() || (!imageUrl.trim() && !uploadedImage)}
              className="w-full py-2.5 bg-gradient-to-r from-primary to-purple-600 text-white font-medium text-sm rounded-lg hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed flex items-center justify-center gap-2"
            >
              {isGenerating ? (
                <>
                  <Loader2 className="w-5 h-5 animate-spin" />
                  {isPolling ? 'Generating Video...' : 'Submitting...'}
                </>
              ) : (
                <>
                  <Play className="w-5 h-5" />
                  Generate Video
                </>
              )}
            </button>

            {/* Error Message */}
            <AnimatePresence>
              {error && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  className="bg-red-500/10 border border-red-500/50 rounded-lg p-4 flex items-start gap-2"
                >
                  <div className="flex-1 text-red-400 text-sm">{error}</div>
                  <button
                    onClick={() => setError(null)}
                    className="text-red-400 hover:text-red-300"
                  >
                    <X className="w-4 h-4" />
                  </button>
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>

          {/* Preview Section */}
          <motion.div 
            initial={{ opacity: 0, x: 20 }} 
            animate={{ opacity: 1, x: 0 }}
            className="glass rounded-2xl p-6 lg:col-span-2 flex flex-col"
          >
            <h2 className="text-xl font-bold text-white mb-4">Preview</h2>
            
            <div className="flex-1 bg-gray-800/50 rounded-lg overflow-hidden flex items-center justify-center" style={{ minHeight: '500px' }}>
              {isGenerating ? (
                <div className="text-center">
                  <Loader2 className="w-12 h-12 text-primary animate-spin mx-auto mb-4" />
                  <p className="text-gray-400">
                    {isPolling ? 'Generating your video...' : 'Submitting request...'}
                  </p>
                  {isPolling && (
                    <p className="text-gray-500 text-sm mt-2">This may take 1-2 minutes</p>
                  )}
                </div>
              ) : videoUrl ? (
                <div className="w-full h-full relative bg-black">
                  <video
                    key={videoUrl}
                    src={videoUrl}
                    controls
                    autoPlay
                    loop
                    playsInline
                    preload="auto"
                    className="w-full h-full object-contain"
                    onError={(e) => {
                      console.error('Video playback error:', e);
                      setError('Failed to load video. Please try again.');
                    }}
                    onLoadedData={() => {
                      console.log('Video loaded successfully');
                    }}
                  >
                    Your browser does not support the video tag.
                  </video>
                </div>
              ) : (
                <div className="text-center text-gray-500">
                  <ImageIcon className="w-16 h-16 mx-auto mb-4 opacity-30" />
                  <p>Your video will appear here</p>
                  <p className="text-sm mt-2">Provide an image URL and describe the motion</p>
                </div>
              )}
            </div>

            {/* Video Actions */}
            {videoUrl && !isGenerating && (
              <motion.div
                initial={{ opacity: 0, y: 10 }}
                animate={{ opacity: 1, y: 0 }}
                className="mt-4 space-y-3"
              >
                {generationTime && (
                  <div className="text-sm text-gray-400 text-center">
                    Generated in {generationTime.toFixed(1)}s
                  </div>
                )}
                <button
                  onClick={handleDownload}
                  className="w-full py-3 bg-gray-800 hover:bg-gray-700 text-white font-medium rounded-lg transition-colors flex items-center justify-center gap-2"
                >
                  <Download className="w-5 h-5" />
                  Download Video
                </button>
              </motion.div>
            )}
          </motion.div>
        </div>
      </div>
    </div>
  );
};
export default ImageToVideoPage;
