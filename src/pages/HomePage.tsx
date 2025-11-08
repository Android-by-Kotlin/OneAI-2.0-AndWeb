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
      title: 'Text to Image',
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
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900">
      {/* Header */}
      <header className="glass-dark border-b border-white border-opacity-10">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <Sparkles className="w-8 h-8 text-primary" />
              <h1 className="text-2xl font-bold gradient-text">OneAI</h1>
            </div>
            
            <div className="flex items-center gap-4">
              <button
                onClick={() => navigate('/profile')}
                className="flex items-center gap-2 px-4 py-2 glass rounded-lg hover:bg-white hover:bg-opacity-20 transition-colors"
              >
                <User2 className="w-5 h-5" />
                <span className="hidden sm:inline">{user?.displayName || 'Profile'}</span>
              </button>
              
              <button
                onClick={handleLogout}
                className="flex items-center gap-2 px-4 py-2 bg-red-500 bg-opacity-20 text-red-400 rounded-lg hover:bg-opacity-30 transition-colors"
              >
                <LogOut className="w-5 h-5" />
                <span className="hidden sm:inline">Logout</span>
              </button>
            </div>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-12">
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          className="text-center mb-12"
        >
          <h2 className="text-4xl sm:text-5xl md:text-6xl font-bold text-white mb-4">
            Welcome to <span className="gradient-text">OneAI</span>
          </h2>
          <p className="text-xl text-gray-300 max-w-3xl mx-auto">
            Transform your ideas into visual masterpieces with the power of AI
          </p>
        </motion.div>

        {/* Features Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {features.map((feature, index) => (
            <motion.div
              key={feature.title}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.1 }}
            >
              <button
                onClick={() => navigate(feature.route)}
                className="w-full text-left group"
              >
                <div className="glass rounded-2xl p-6 hover:scale-105 transition-transform duration-300 h-full">
                  {/* Icon with gradient background */}
                  <div className={`w-16 h-16 rounded-xl bg-gradient-to-br ${feature.gradient} flex items-center justify-center mb-4 group-hover:scale-110 transition-transform`}>
                    {feature.icon}
                  </div>

                  {/* Content */}
                  <h3 className="text-xl font-bold text-white mb-2">
                    {feature.title}
                  </h3>
                  <p className="text-gray-300 text-sm">
                    {feature.description}
                  </p>

                  {/* Arrow indicator */}
                  <div className="mt-4 flex items-center text-primary group-hover:translate-x-2 transition-transform">
                    <span className="text-sm font-medium">Get Started</span>
                    <svg
                      className="w-4 h-4 ml-2"
                      fill="none"
                      stroke="currentColor"
                      viewBox="0 0 24 24"
                    >
                      <path
                        strokeLinecap="round"
                        strokeLinejoin="round"
                        strokeWidth={2}
                        d="M9 5l7 7-7 7"
                      />
                    </svg>
                  </div>
                </div>
              </button>
            </motion.div>
          ))}
        </div>

        {/* Stats Section */}
        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ delay: 0.8 }}
          className="mt-12 glass rounded-2xl p-8"
        >
          <div className="grid grid-cols-1 sm:grid-cols-3 gap-8 text-center">
            <div>
              <div className="text-3xl font-bold gradient-text mb-2">Fast</div>
              <p className="text-gray-300">Generate images in under 6 seconds</p>
            </div>
            <div>
              <div className="text-3xl font-bold gradient-text mb-2">Multiple Models</div>
              <p className="text-gray-300">Access to cutting-edge AI models</p>
            </div>
            <div>
              <div className="text-3xl font-bold gradient-text mb-2">High Quality</div>
              <p className="text-gray-300">Professional-grade outputs</p>
            </div>
          </div>
        </motion.div>
      </div>

      {/* Footer */}
      <footer className="mt-auto py-8 text-center text-gray-400">
        <p className="text-sm">
          OneAI - Made with ❤️ | Transform Ideas into Visual Masterpieces
        </p>
      </footer>
    </div>
  );
};

export default HomePage;
