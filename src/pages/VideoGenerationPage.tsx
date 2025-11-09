import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, Video, Loader2, Download, X, Play } from 'lucide-react';
import { generateVideo, pollForVideo } from '../services/videoGenerationService';

const VideoGenerationPage = () => {
  const navigate = useNavigate();
  const [prompt, setPrompt] = useState('');
  const [negativePrompt, setNegativePrompt] = useState('blurry, low quality, distorted, artifacts, bad anatomy');
  const [isGenerating, setIsGenerating] = useState(false);
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [generationTime, setGenerationTime] = useState<number | null>(null);
  const [isPolling, setIsPolling] = useState(false);

  const handleGenerate = async () => {
    if (!prompt.trim()) {
      setError('Please enter a prompt');
      return;
    }

    setIsGenerating(true);
    setError(null);
    setVideoUrl(null);
    setGenerationTime(null);

    try {
      const result = await generateVideo(prompt, negativePrompt);
      
      if (result.videoUrl) {
        // Video generated immediately
        setVideoUrl(result.videoUrl);
        setGenerationTime(result.generationTime || null);
        setIsGenerating(false);
      } else if (result.requestId) {
        // Need to poll for results
        setIsPolling(true);
        const pollResult = await pollForVideo(result.requestId);
        setVideoUrl(pollResult.videoUrl);
        setGenerationTime(pollResult.generationTime || null);
        setIsPolling(false);
        setIsGenerating(false);
      }
    } catch (err: any) {
      console.error('Video generation error:', err);
      setError(err.message || 'Failed to generate video');
      setIsGenerating(false);
      setIsPolling(false);
    }
  };

  const handleDownload = () => {
    if (videoUrl) {
      const link = document.createElement('a');
      link.href = videoUrl;
      link.download = `generated-video-${Date.now()}.mp4`;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
    }
  };

  return (
    <div className="min-h-screen bg-black relative overflow-hidden p-6">
      {/* Ambient gradient background - teal/cyan theme */}
      <div className="fixed inset-0 pointer-events-none z-0">
        {/* Vignette overlay */}
        <div style={{
          position: 'absolute',
          inset: 0,
          background: 'radial-gradient(circle at center, transparent 0%, rgba(0, 0, 0, 0.6) 100%)'
        }} />
        
        {/* Bottom ambient vignette */}
        <div style={{
          position: 'absolute',
          bottom: 0,
          left: 0,
          right: 0,
          height: '60%',
          background: `radial-gradient(ellipse at bottom, rgba(20, 184, 166, 0.35) 0%, rgba(6, 182, 212, 0.25) 40%, transparent 70%)`,
          opacity: 1
        }} />
        
        {/* Left side glow */}
        <div style={{
          position: 'absolute',
          left: 0,
          top: '20%',
          width: '50%',
          height: '60%',
          background: `radial-gradient(ellipse at left, rgba(20, 184, 166, 0.3) 0%, transparent 60%)`,
          opacity: 1
        }} />
        
        {/* Right side glow */}
        <div style={{
          position: 'absolute',
          right: 0,
          top: '30%',
          width: '50%',
          height: '60%',
          background: `radial-gradient(ellipse at right, rgba(6, 182, 212, 0.25) 0%, transparent 60%)`,
          opacity: 1
        }} />
        
        {/* Top subtle glow */}
        <div style={{
          position: 'absolute',
          top: 0,
          left: '20%',
          right: '20%',
          height: '40%',
          background: `radial-gradient(ellipse at top, rgba(13, 148, 136, 0.2) 0%, transparent 60%)`,
          opacity: 1
        }} />
        
        {/* Center depth */}
        <div style={{
          position: 'absolute',
          top: '50%',
          left: '50%',
          transform: 'translate(-50%, -50%)',
          width: '70%',
          height: '70%',
          background: `radial-gradient(circle, rgba(20, 184, 166, 0.15) 0%, transparent 70%)`,
          opacity: 1
        }} />
      </div>
      
      <div className="h-full flex flex-col relative z-10">
        {/* Header */}
        <div className="flex items-center justify-between mb-6">
          <button 
            onClick={() => navigate('/home')} 
            className="flex items-center gap-2 text-gray-300 hover:text-white transition-colors"
          >
            <ArrowLeft className="w-5 h-5" />
            Back
          </button>
          <h1 className="text-3xl font-bold gradient-text">Video Generation</h1>
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
              <p className="text-gray-400 text-sm">Using Seedance T2V Model</p>
            </div>

            {/* Prompt Input */}
            <div>
              <label className="block text-sm font-medium text-gray-300 mb-2">
                Prompt *
              </label>
              <textarea
                value={prompt}
                onChange={(e) => setPrompt(e.target.value)}
                placeholder="A beautiful sunset over mountains..."
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
                placeholder="blurry, low quality, distorted, artifacts..."
                className="w-full px-3 py-2 bg-gray-800/50 border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary/50 resize-none text-sm"
                rows={2}
                disabled={isGenerating}
              />
            </div>

            {/* Spacer to push settings and button to bottom */}
            <div className="flex-1"></div>

            {/* Video Settings Info */}
            <div className="bg-gray-800/30 rounded-lg p-3 space-y-2">
              <h3 className="text-xs font-medium text-gray-300">Settings</h3>
              <div className="grid grid-cols-2 gap-1 text-xs text-gray-400">
                <div>480p</div>
                <div>25 frames</div>
                <div>Seedance</div>
                <div>16 FPS (~1.5s)</div>
              </div>
            </div>

            {/* Generate Button */}
            <button
              onClick={handleGenerate}
              disabled={isGenerating || !prompt.trim()}
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
                  <Video className="w-16 h-16 mx-auto mb-4 opacity-30" />
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

export default VideoGenerationPage;
