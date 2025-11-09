import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { motion } from 'framer-motion';
import {
  Image,
  MessageSquare,
  Palette,
  Wand2,
  User2,
  LogOut,
  Sparkles,
  Mic,
  Clapperboard,
  ArrowRight,
  Zap,
  Star,
} from 'lucide-react';

interface FeatureCard {
  title: string;
  description: string;
  icon: React.ReactNode;
  gradient: string;
  route: string;
}

const HomePage = () => {
  const navigate = useNavigate();
  const { user, logout } = useAuth();

  const features: FeatureCard[] = [
    {
      title: 'Image Generation',
      description: 'Generate stunning images from text prompts using advanced AI models',
      icon: <Image className="w-8 h-8" />,
      gradient: 'from-blue-500 to-cyan-500',
      route: '/image-generator',
    },
    {
      title: 'Image Transform',
      description: 'Transform and enhance your images with AI-powered tools',
      icon: <Palette className="w-8 h-8" />,
      gradient: 'from-purple-500 to-pink-500',
      route: '/image-to-image',
    },
    {
      title: 'AI Chat',
      description: 'Chat with advanced AI models like Gemini and Claude',
      icon: <MessageSquare className="w-8 h-8" />,
      gradient: 'from-green-500 to-emerald-500',
      route: '/chatbot',
    },
    {
      title: 'Video Generation',
      description: 'Create videos from text prompts or animate your images',
      icon: <Clapperboard className="w-8 h-8" />,
      gradient: 'from-teal-500 to-cyan-500',
      route: '/text-to-video',
    },
    {
      title: 'Inpainting',
      description: 'Fill or edit specific areas of your images with AI',
      icon: <Wand2 className="w-8 h-8" />,
      gradient: 'from-indigo-500 to-purple-500',
      route: '/sketch-to-image',
    },
    {
      title: 'Live Avatar',
      description: 'Interact with AI avatars in real-time',
      icon: <Mic className="w-8 h-8" />,
      gradient: 'from-pink-500 to-rose-500',
      route: '/live-avatar',
    },
  ];

  const handleLogout = async () => {
    try {
      await logout();
      navigate('/login');
    } catch (error) {
      console.error('Logout error:', error);
    }
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900 relative overflow-hidden">
      {/* Animated Background Elements */}
      <div className="absolute inset-0 overflow-hidden pointer-events-none">
        <motion.div
          animate={{
            scale: [1, 1.2, 1],
            rotate: [0, 90, 0],
            opacity: [0.3, 0.5, 0.3],
          }}
          transition={{ duration: 20, repeat: Infinity, ease: "linear" }}
          className="absolute -top-40 -right-40 w-96 h-96 bg-purple-500 rounded-full blur-3xl"
        />
        <motion.div
          animate={{
            scale: [1, 1.3, 1],
            rotate: [0, -90, 0],
            opacity: [0.3, 0.6, 0.3],
          }}
          transition={{ duration: 15, repeat: Infinity, ease: "linear" }}
          className="absolute -bottom-40 -left-40 w-96 h-96 bg-blue-500 rounded-full blur-3xl"
        />
        <motion.div
          animate={{
            scale: [1, 1.4, 1],
            opacity: [0.2, 0.4, 0.2],
          }}
          transition={{ duration: 18, repeat: Infinity, ease: "linear" }}
          className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-96 h-96 bg-pink-500 rounded-full blur-3xl"
        />
      </div>

      {/* Header */}
      <header className="glass-dark border-b border-white border-opacity-10 relative z-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <motion.div 
              initial={{ opacity: 0, x: -20 }}
              animate={{ opacity: 1, x: 0 }}
              className="flex items-center gap-3"
            >
              <div className="relative">
                <Sparkles className="w-8 h-8 text-primary" />
                <motion.div
                  animate={{ scale: [1, 1.2, 1], opacity: [0.5, 0, 0.5] }}
                  transition={{ duration: 2, repeat: Infinity }}
                  className="absolute inset-0 bg-primary rounded-full blur-md"
                />
              </div>
              <h1 className="text-2xl font-bold gradient-text">OneAI</h1>
            </motion.div>
            
            <motion.div 
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              className="flex items-center gap-3"
            >
              <button
                onClick={() => navigate('/profile')}
                className="flex items-center gap-2 px-4 py-2 glass rounded-xl hover:bg-white hover:bg-opacity-20 transition-all duration-300 hover:scale-105"
              >
                <User2 className="w-5 h-5" />
                <span className="hidden sm:inline">{user?.displayName || 'Profile'}</span>
              </button>
              
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-red-500 to-pink-500 bg-opacity-20 text-white rounded-xl hover:shadow-lg hover:shadow-red-500/50 transition-all duration-300 hover:scale-105"
              >
                <LogOut className="w-5 h-5" />
                <span className="hidden sm:inline">Logout</span>
              </button>
            </motion.div>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 relative z-10">
        <motion.div
          initial={{ opacity: 0, y: 30 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.8 }}
          className="text-center mb-16"
        >
          {/* Badge */}
          <motion.div
            initial={{ opacity: 0, scale: 0.5 }}
            animate={{ opacity: 1, scale: 1 }}
            transition={{ delay: 0.2 }}
            className="inline-flex items-center gap-2 px-4 py-2 glass rounded-full mb-6 border border-white border-opacity-20"
          >
            <Star className="w-4 h-4 text-yellow-400" />
            <span className="text-sm text-gray-300">Powered by Advanced AI Models</span>
            <Zap className="w-4 h-4 text-primary" />
          </motion.div>

          {/* Main Heading */}
          <h2 className="text-5xl sm:text-6xl md:text-7xl font-bold text-white mb-6 leading-tight">
            Create Amazing Content
            <br />
            with <span className="gradient-text">OneAI</span>
          </h2>
          
          {/* Subheading */}
          <p className="text-xl sm:text-2xl text-gray-300 max-w-3xl mx-auto mb-8 leading-relaxed">
            Generate images, chat with AI, create videos, and transform your ideas into reality with cutting-edge artificial intelligence
          </p>

          {/* CTA Buttons */}
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ delay: 0.4 }}
            className="flex flex-col sm:flex-row items-center justify-center gap-4"
          >
            <button
              onClick={() => navigate('/chatbot')}
              className="group flex items-center gap-2 px-8 py-4 bg-gradient-to-r from-primary to-purple-600 text-white rounded-xl font-semibold text-lg hover:shadow-2xl hover:shadow-primary/50 transition-all duration-300 hover:scale-105"
            >
              Start Creating
              <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
            </button>
            <button
              onClick={() => window.scrollTo({ top: document.body.scrollHeight, behavior: 'smooth' })}
              className="flex items-center gap-2 px-8 py-4 glass rounded-xl text-white font-semibold text-lg hover:bg-white hover:bg-opacity-20 transition-all duration-300 hover:scale-105 border border-white border-opacity-20"
            >
              Explore Features
              <Sparkles className="w-5 h-5" />
            </button>
          </motion.div>
        </motion.div>

        {/* Section Title */}
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          transition={{ delay: 0.6 }}
          className="text-center mb-12"
        >
          <h3 className="text-3xl sm:text-4xl font-bold text-white mb-4">
            Explore Our <span className="gradient-text">AI Tools</span>
          </h3>
          <p className="text-gray-400 text-lg max-w-2xl mx-auto">
            Choose from our powerful AI-powered tools to bring your creative vision to life
          </p>
        </motion.div>

        {/* Features Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 30 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: 0.7 + index * 0.1, duration: 0.5 }}
              whileHover={{ y: -8 }}
            >
              <button
                onClick={() => navigate(feature.route)}
                className="w-full text-left group h-full"
              >
                <div className="glass rounded-2xl p-6 transition-all duration-300 h-full border border-white border-opacity-10 hover:border-opacity-30 hover:shadow-2xl relative overflow-hidden">
                  {/* Hover Gradient Effect */}
                  <div className={`absolute inset-0 bg-gradient-to-br ${feature.gradient} opacity-0 group-hover:opacity-10 transition-opacity duration-300`} />
                  
                  {/* Icon with gradient background */}
                  <motion.div 
                    whileHover={{ rotate: 360 }}
                    transition={{ duration: 0.6 }}
                    className={`w-16 h-16 rounded-xl bg-gradient-to-br ${feature.gradient} flex items-center justify-center mb-4 shadow-lg relative z-10`}
                  >
                    {feature.icon}
                  </motion.div>

                  {/* Content */}
                  <h3 className="text-xl font-bold text-white mb-3 relative z-10">
                    {feature.title}
                  </h3>
                  <p className="text-gray-300 text-sm mb-4 leading-relaxed relative z-10">
                    {feature.description}
                  </p>

                  {/* Arrow indicator */}
                  <div className="flex items-center text-primary group-hover:text-white transition-colors relative z-10">
                    <span className="text-sm font-semibold">Try it now</span>
                    <ArrowRight className="w-4 h-4 ml-2 group-hover:translate-x-2 transition-transform" />
                  </div>
                </div>
              </button>
            </motion.div>
          ))}
        </div>

      </div>
    </div>
  );
};

export default HomePage;
