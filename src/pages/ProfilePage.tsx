import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { motion } from 'framer-motion';
import { ArrowLeft, User2 } from 'lucide-react';

const ProfilePage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  
  return (
    <div className="min-h-screen bg-black text-white relative p-8">
      {/* Ambient Light Background */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        {/* Vignette Overlay */}
        <div className="absolute inset-0 bg-gradient-radial from-transparent via-transparent to-black opacity-60" />
        
        {/* Bottom Vignette - Purple glow */}
        <div className="absolute inset-0 blur-3xl" style={{
          background: 'radial-gradient(ellipse at 50% 120%, rgba(139, 92, 246, 0.35) 0%, rgba(147, 51, 234, 0.2) 30%, transparent 70%)'
        }} />
        
        {/* Left Side Glow */}
        <div className="absolute inset-0 blur-3xl opacity-60" style={{
          background: 'radial-gradient(ellipse at 0% 50%, rgba(139, 92, 246, 0.35) 0%, transparent 60%)'
        }} />
        
        {/* Right Side Glow */}
        <div className="absolute inset-0 blur-3xl opacity-60" style={{
          background: 'radial-gradient(ellipse at 100% 50%, rgba(59, 130, 246, 0.25) 0%, transparent 60%)'
        }} />
        
        {/* Top Subtle Glow */}
        <div className="absolute inset-0 blur-3xl opacity-40" style={{
          background: 'radial-gradient(ellipse at 50% -20%, rgba(147, 51, 234, 0.2) 0%, transparent 50%)'
        }} />
        
        {/* Center Depth Layer */}
        <div className="absolute inset-0 blur-3xl opacity-30" style={{
          background: 'radial-gradient(circle at 50% 50%, rgba(147, 51, 234, 0.2) 0%, transparent 70%)'
        }} />
      </div>
      
      {/* Content with proper z-index */}
      <div className="relative z-10">
      <div className="max-w-4xl mx-auto">
        <button 
          onClick={() => navigate('/home')}
          className="flex items-center gap-2 text-gray-300 hover:text-white mb-8 transition-colors"
        >
          <ArrowLeft className="w-5 h-5" />
          Back to Home
        </button>

        <motion.div
          initial={{ opacity: 0, y: 20 }}
          animate={{ opacity: 1, y: 0 }}
        >
          <h1 className="text-4xl font-bold gradient-text mb-8">Profile</h1>
          
          <div className="glass rounded-2xl p-8">
            <div className="flex items-center gap-4 mb-8">
              <div className="w-20 h-20 rounded-full bg-gradient-to-r from-primary to-accent flex items-center justify-center text-2xl font-bold">
                {user?.displayName?.[0]?.toUpperCase() || <User2 />}
              </div>
              <div>
                <h2 className="text-2xl font-bold text-white">{user?.displayName || 'User'}</h2>
                <p className="text-gray-300">{user?.email}</p>
              </div>
            </div>
            
            <div className="space-y-4">
              <div className="glass-dark rounded-lg p-4">
                <h3 className="text-lg font-semibold text-white mb-2">Account Info</h3>
                <p className="text-gray-300">User ID: {user?.uid?.substring(0, 16)}...</p>
                <p className="text-gray-300 mt-2">Member since: {user?.metadata.creationTime}</p>
              </div>

              <div className="glass-dark rounded-lg p-4">
                <h3 className="text-lg font-semibold text-white mb-2">Usage Statistics</h3>
                <p className="text-gray-300">Feature coming soon...</p>
              </div>
            </div>
          </div>
        </motion.div>
      </div>
      </div>
    </div>
  );
};

export default ProfilePage;
