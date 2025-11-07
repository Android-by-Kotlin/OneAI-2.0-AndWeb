import { useNavigate } from 'react-router-dom';
import { motion } from 'framer-motion';
import { ArrowLeft, Film } from 'lucide-react';

const ImageToVideoPage = () => {
  const navigate = useNavigate();
  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900 p-4">
      <div className="max-w-6xl mx-auto">
        <div className="flex items-center justify-between mb-8">
          <button onClick={() => navigate('/home')} className="flex items-center gap-2 text-gray-300 hover:text-white transition-colors">
            <ArrowLeft className="w-5 h-5" />Back
          </button>
          <h1 className="text-3xl font-bold gradient-text">Image to Video</h1>
          <div className="w-20"></div>
        </div>
        <motion.div initial={{ opacity: 0, y: 20 }} animate={{ opacity: 1, y: 0 }} className="glass rounded-2xl p-8">
          <div className="text-center py-20">
            <Film className="w-16 h-16 text-primary mx-auto mb-4" />
            <h2 className="text-2xl font-bold text-white mb-2">Image to Video</h2>
            <p className="text-gray-300 mb-4">Feature under development</p>
            <p className="text-sm text-gray-400">Check IMPLEMENTATION_GUIDE.md</p>
          </div>
        </motion.div>
      </div>
    </div>
  );
};
export default ImageToVideoPage;
