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
    <div className="min-h-screen bg-black text-white">
      {/* Header */}
      <header className="fixed top-0 left-0 right-0 z-50 bg-black bg-opacity-80 backdrop-blur-md border-b border-gray-800">
        <div className="max-w-7xl mx-auto px-6 lg:px-12 py-5">
          <div className="flex items-center justify-between">
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="flex items-center gap-2"
            >
              <Sparkles className="w-6 h-6 text-white" />
              <h1 className="text-xl font-normal text-white">OneAI</h1>
            </motion.div>
            
            <motion.div 
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="flex items-center gap-3"
            >
              <button
                onClick={() => navigate('/profile')}
                className="px-4 py-2 text-sm text-gray-300 hover:text-white transition-colors"
              >
                {user?.displayName || 'Profile'}
              </button>
              
              <button
                onClick={handleLogout}
                className="px-4 py-2 text-sm text-gray-300 hover:text-white transition-colors"
              >
                Logout
              </button>
            </motion.div>
          </div>
        </div>
      </header>

      {/* Hero Section */}
      <section className="pt-32 pb-24 px-6 lg:px-12">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            animate={{ opacity: 1, y: 0 }}
            transition={{ duration: 0.6 }}
            className="text-center"
          >
            <h2 className="text-6xl sm:text-7xl lg:text-8xl font-light text-white mb-8 tracking-tight">
              Start creating
            </h2>
            <p className="text-xl sm:text-2xl text-gray-400 max-w-3xl mx-auto font-light">
              Transform your ideas into reality with cutting-edge AI models. Generate images, chat with AI, create videos, and more.
            </p>
          </motion.div>
        </div>
      </section>

      {/* Capabilities Section */}
      <section className="py-24 px-6 lg:px-12">
        <div className="max-w-7xl mx-auto">
          <motion.div
            initial={{ opacity: 0 }}
            whileInView={{ opacity: 1 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6 }}
            className="mb-20"
          >
            <h3 className="text-5xl sm:text-6xl font-light text-white mb-6">
              Capabilities
            </h3>
            <p className="text-xl text-gray-400 max-w-2xl font-light">
              Explore our suite of AI-powered tools designed to bring your creative vision to life.
            </p>
          </motion.div>

          {/* Features Showcase */}
          <div className="space-y-32">
            {features.map((feature, index) => (
              <motion.div
                key={feature.title}
                initial={{ opacity: 0, y: 40 }}
                whileInView={{ opacity: 1, y: 0 }}
                viewport={{ once: true, margin: "-100px" }}
                transition={{ duration: 0.8, delay: index * 0.1 }}
                className="relative"
              >
                <div className={`grid lg:grid-cols-2 gap-12 items-center ${index % 2 === 1 ? 'lg:grid-flow-dense' : ''}`}>
                  {/* Text Content */}
                  <div className={index % 2 === 1 ? 'lg:col-start-2' : ''}>
                    <div className="mb-6">
                      <div className={`w-12 h-12 rounded-lg bg-gradient-to-br ${feature.gradient} flex items-center justify-center mb-4`}>
                        {feature.icon}
                      </div>
                    </div>
                    <h4 className="text-4xl sm:text-5xl font-light text-white mb-6">
                      {feature.title}
                    </h4>
                    <p className="text-lg text-gray-400 mb-8 leading-relaxed font-light">
                      {feature.description}
                    </p>
                    <button
                      onClick={() => navigate(feature.route)}
                      className="group inline-flex items-center gap-2 text-white hover:text-gray-300 transition-colors"
                    >
                      <span className="text-lg">Try it now</span>
                      <ArrowRight className="w-5 h-5 group-hover:translate-x-1 transition-transform" />
                    </button>
                  </div>

                  {/* Visual Showcase */}
                  <div className={index % 2 === 1 ? 'lg:col-start-1 lg:row-start-1' : ''}>
                    <motion.div
                      whileHover={{ scale: 1.02 }}
                      transition={{ duration: 0.3 }}
                      onClick={() => navigate(feature.route)}
                      className="relative aspect-video rounded-3xl overflow-hidden bg-gradient-to-br from-gray-900 to-gray-800 border border-gray-800 cursor-pointer group"
                    >
                      {/* Dynamic Content based on feature */}
                      {feature.title === 'Image Generation' && (
                        <div className="absolute inset-0">
                          {/* Showcase Image */}
                          <img
                            src="https://prior-crimson-as5dvgjrrf.edgeone.app/text%20to%20image.png"
                            alt="AI Generated Image Example"
                            className="absolute inset-0 w-full h-full object-cover"
                          />
                          {/* Gradient overlay for depth */}
                          <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent opacity-60" />
                        </div>
                      )}

                      {feature.title === 'Image Transform' && (
                        <div className="absolute inset-0">
                          {/* Showcase Image */}
                          <img
                            src="https://blogs-cdn.imagine.art/runaway_refrence_5_4_3e57bb7ea0.webp"
                            alt="AI Image Transform Example"
                            className="absolute inset-0 w-full h-full object-cover"
                          />
                          {/* Gradient overlay for depth */}
                          <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent opacity-60" />
                        </div>
                      )}

                      {feature.title === 'AI Chat' && (
                        <div className="absolute inset-0">
                          {/* Showcase Video */}
                          <video
                            autoPlay
                            loop
                            muted
                            playsInline
                            className="absolute inset-0 w-full h-full object-cover"
                          >
                            <source src="https://www.shutterstock.com/shutterstock/videos/3525274597/preview/stock-footage-cinematic-concept-of-ai-chatbot-interface-generate-answer-prompt-on-productivity-and-fitness.webm" type="video/webm" />
                          </video>
                          {/* Gradient overlay for depth */}
                          <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent opacity-50" />
                        </div>
                      )}

                      {feature.title === 'Video Generation' && (
                        <div className="absolute inset-0">
                          {/* Showcase Video */}
                          <video
                            autoPlay
                            loop
                            muted
                            playsInline
                            className="absolute inset-0 w-full h-full object-cover"
                          >
                            <source src="https://p2-kling.klingai.com/kcdn/cdn-kcdn112452/kling-website/web-page2-6-new.mp4" type="video/mp4" />
                          </video>
                          {/* Gradient overlay for depth */}
                          <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent opacity-50" />
                        </div>
                      )}

                      {feature.title === 'Inpainting' && (
                        <div className="absolute inset-0">
                          {/* Showcase Image */}
                          <img
                            src="https://cdn.pokecut.com/resource/webseo/aiinpainting_intro1_25081220031215.webp"
                            alt="AI Inpainting Example"
                            className="absolute inset-0 w-full h-full object-cover"
                          />
                          {/* Gradient overlay for depth */}
                          <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent opacity-60" />
                        </div>
                      )}

                      {feature.title === 'Live Avatar' && (
                        <div className="absolute inset-0">
                          {/* Avatar Video */}
                          <video
                            autoPlay
                            loop
                            muted
                            playsInline
                            className="absolute inset-0 w-full h-full object-cover"
                          >
                            <source src="https://files2.heygen.ai/avatar/v3/26c9a98335d7429da6ed3215d961e080_39190/preview_video_target.mp4" type="video/mp4" />
                          </video>
                          {/* Subtle overlay for text visibility */}
                          <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent opacity-50" />
                        </div>
                      )}

                      {/* Gradient overlay on hover */}
                      <div className={`absolute inset-0 bg-gradient-to-br ${feature.gradient} opacity-0 group-hover:opacity-10 transition-opacity duration-300`} />
                      
                      {/* Blur effect at edges */}
                      <div className="absolute inset-0 bg-gradient-to-t from-black via-transparent to-transparent opacity-40" />
                    </motion.div>
                  </div>
                </div>
              </motion.div>
            ))}
          </div>
        </div>
      </section>

      {/* Footer CTA */}
      <section className="py-24 px-6 lg:px-12">
        <div className="max-w-4xl mx-auto text-center">
          <motion.div
            initial={{ opacity: 0, y: 20 }}
            whileInView={{ opacity: 1, y: 0 }}
            viewport={{ once: true }}
            transition={{ duration: 0.6 }}
          >
            <h3 className="text-5xl sm:text-6xl font-light text-white mb-8">
              Ready to create?
            </h3>
            <button
              onClick={() => navigate('/chatbot')}
              className="inline-flex items-center gap-2 px-8 py-4 bg-white text-black rounded-full text-lg font-medium hover:bg-gray-200 transition-colors"
            >
              Get started
              <ArrowRight className="w-5 h-5" />
            </button>
          </motion.div>
        </div>
      </section>
    </div>
  );
};

export default HomePage;
