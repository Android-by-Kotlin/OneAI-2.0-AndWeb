import { useNavigate } from 'react-router-dom';
import { useAuth } from '../contexts/AuthContext';
import { motion } from 'framer-motion';
import { ArrowLeft, User2 } from 'lucide-react';

const ProfilePage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  
  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900 p-8">
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
  );
};

export default ProfilePage;
