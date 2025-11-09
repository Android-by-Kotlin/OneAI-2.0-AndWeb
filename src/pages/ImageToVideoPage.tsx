import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, Video, Loader2, Download, X, Play, Image as ImageIcon } from 'lucide-react';
import { generateImageToVideo, pollForImageToVideo } from '../services/imageToVideoService';

const ImageToVideoPage = () => {
  const navigate = useNavigate();
  
  const [imageUrl, setImageUrl] = useState<string>('');
  const [prompt, setPrompt] = useState('');
  const [isGenerating, setIsGenerating] = useState(false);
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [generationTime, setGenerationTime] = useState<number | null>(null);
  const [isPolling, setIsPolling] = useState(false);
  const [isPortrait, setIsPortrait] = useState(true);
  const [uploadedFile, setUploadedFile] = useState<File | null>(null);

  const handleGenerate = async () => {
    if (!imageUrl.trim()) {
      setError('Please enter an image URL');
      return;
    }

    if (!prompt.trim()) {
      setError('Please enter a prompt');
      return;
    }

    setIsGenerating(true);
    setError(null);
    setVideoUrl(null);
    setGenerationTime(null);

    try {
      const result = await generateImageToVideo(imageUrl.trim(), prompt, '', 'gen4_turbo', isPortrait);
      
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
              <p className="text-gray-400 text-sm">Using Gen4 Turbo Model</p>
            </div>

            {/* Portrait Toggle */}
            <div className="flex items-center justify-between py-2 px-3 bg-gray-800/30 rounded-lg">
              <label className="flex items-center gap-2 cursor-pointer">
                <input
                  type="checkbox"
                  checked={isPortrait}
                  onChange={(e) => setIsPortrait(e.target.checked)}
                  className="w-4 h-4 rounded border-gray-600 text-primary focus:ring-primary focus:ring-offset-gray-900"
                  disabled={isGenerating}
                />
                <span className="text-sm text-gray-300">Portrait</span>
              </label>
            </div>

            {/* Image URL Input */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Image URL *
              </label>
              <input
                type="text"
                value={imageUrl}
                onChange={(e) => setImageUrl(e.target.value)}
                placeholder="https://example.com/image.jpg"
                className="w-full px-3 py-2 bg-gray-800/50 border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary/50 text-sm"
                disabled={isGenerating}
              />
            </div>

            {/* Image Preview */}
            {imageUrl && (
              <div className="relative bg-gray-800/50 rounded-lg" style={{ height: '200px' }}>
                <img 
                  src={imageUrl} 
                  alt="Input" 
                  className="w-full h-full object-contain rounded-lg"
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

            {/* Spacer */}
            <div className="flex-1"></div>

            {/* Generate Button */}
            <button
              onClick={handleGenerate}
              disabled={isGenerating || !imageUrl.trim() || !prompt.trim()}
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
