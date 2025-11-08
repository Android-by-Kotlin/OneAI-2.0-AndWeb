import { useState, useEffect, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, Mic, Send, Power, Loader2, AlertCircle, Video as VideoIcon } from 'lucide-react';
import StreamingAvatar, { AvatarQuality, StreamingEvents } from '@heygen/streaming-avatar';
import { getHeyGenAccessToken } from '../services/heygenTokenService';
import { API_CONFIG } from '../config/api';

interface Message {
  id: string;
  text: string;
  sender: 'user' | 'avatar';
  timestamp: Date;
}

const LiveAvatarPage = () => {
  const navigate = useNavigate();
  const videoRef = useRef<HTMLVideoElement>(null);
  const avatarRef = useRef<StreamingAvatar | null>(null);
  
  const [isConnected, setIsConnected] = useState(false);
  const [isConnecting, setIsConnecting] = useState(false);
  const [message, setMessage] = useState('');
  const [messages, setMessages] = useState<Message[]>([]);
  const [isSending, setIsSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [connectionState, setConnectionState] = useState<string>('disconnected');
  const [hasApiKey, setHasApiKey] = useState(false);
  const [stream, setStream] = useState<MediaStream | null>(null);

  // Check if API key is configured
  useEffect(() => {
    setHasApiKey(!!API_CONFIG.HEYGEN_API_KEY);
  }, []);

  // Start streaming session
  const handleConnect = async () => {
    if (!hasApiKey) {
      setError('HeyGen API key is not configured. Please add VITE_HEYGEN_API_KEY to your .env file.');
      return;
    }

    setIsConnecting(true);
    setError(null);

    try {
      // Get access token
      const token = await getHeyGenAccessToken();

      // Initialize avatar
      const avatar = new StreamingAvatar({ token });
      avatarRef.current = avatar;

      // Set up event listeners
      avatar.on(StreamingEvents.AVATAR_START_TALKING, () => {
        console.log('Avatar started talking');
      });

      avatar.on(StreamingEvents.AVATAR_STOP_TALKING, () => {
        console.log('Avatar stopped talking');
      });

      // Use AVATAR_END_MESSAGE to get complete sentences instead of individual characters
      avatar.on(StreamingEvents.AVATAR_END_MESSAGE, (event) => {
        console.log('Avatar end message:', event);
        const text = event.detail?.message || event.detail?.text;
        if (text && text.trim()) {
          const avatarMessage: Message = {
            id: `avatar-${Date.now()}-${Math.random()}`,
            text: text,
            sender: 'avatar',
            timestamp: new Date()
          };
          setMessages(prev => [...prev, avatarMessage]);
        }
      });

      avatar.on(StreamingEvents.STREAM_DISCONNECTED, () => {
        console.log('Stream disconnected');
        setIsConnected(false);
        setConnectionState('disconnected');
      });

      avatar.on(StreamingEvents.STREAM_READY, (event) => {
        console.log('Stream ready:', event);
        setConnectionState('connected');
        setIsConnected(true);
        setIsConnecting(false);
        
        // Set video stream
        const mediaStream = event.detail;
        console.log('MediaStream received:', mediaStream);
        if (mediaStream && videoRef.current) {
          videoRef.current.srcObject = mediaStream;
          videoRef.current.muted = false;
          videoRef.current.play().catch((e) => console.error('Play error:', e));
          setStream(mediaStream);
        }

        // Send welcome message
        setMessages([{
          id: Date.now().toString(),
          text: 'Hello! I\'m your AI avatar. How can I help you today?',
          sender: 'avatar',
          timestamp: new Date()
        }]);
      });

      // Start avatar session
      await avatar.createStartAvatar({
        quality: AvatarQuality.Low,
        avatarName: 'Anna_public_3_20240108', // Valid public avatar
        language: 'en',
      });

    } catch (err: any) {
      console.error('Connection error:', err);
      setError(err.message || 'Failed to connect to avatar');
      setIsConnecting(false);
      setIsConnected(false);
    }
  };

  // Stop streaming session
  const handleDisconnect = async () => {
    if (avatarRef.current) {
      await avatarRef.current.stopAvatar();
      avatarRef.current = null;
    }

    if (videoRef.current) {
      videoRef.current.srcObject = null;
    }

    setIsConnected(false);
    setConnectionState('disconnected');
    setMessages([]);
    setStream(null);
  };

  // Send message to avatar
  const handleSendMessage = async () => {
    if (!message.trim() || !avatarRef.current || isSending) return;

    const userMessage: Message = {
      id: Date.now().toString(),
      text: message.trim(),
      sender: 'user',
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setMessage('');
    setIsSending(true);

    try {
      // Just make the avatar speak - don't echo the text
      await avatarRef.current.speak({ text: userMessage.text });
    } catch (err: any) {
      setError(err.message || 'Failed to send message');
    } finally {
      setIsSending(false);
    }
  };

  // Cleanup on unmount
  useEffect(() => {
    return () => {
      if (avatarRef.current) {
        avatarRef.current.stopAvatar();
      }
    };
  }, []);

  // Update video element when stream changes
  useEffect(() => {
    if (stream && videoRef.current) {
      videoRef.current.srcObject = stream;
      videoRef.current.muted = false;
      // Try to play
      videoRef.current.play().catch(err => {
        console.error('Video play error:', err);
        // If autoplay fails, try with muted first
        videoRef.current!.muted = true;
        videoRef.current!.play().then(() => {
          // Then unmute after playing
          setTimeout(() => {
            if (videoRef.current) {
              videoRef.current.muted = false;
            }
          }, 100);
        });
      });
    }
  }, [stream]);

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
          <h1 className="text-3xl font-bold gradient-text">Live Avatar</h1>
          <div className="w-20"></div>
        </div>

        {/* API Key Warning */}
        {!hasApiKey && (
          <motion.div
            initial={{ opacity: 0, y: -10 }}
            animate={{ opacity: 1, y: 0 }}
            className="mb-4 bg-yellow-500/10 border border-yellow-500/50 rounded-lg p-4 flex items-start gap-3"
          >
            <AlertCircle className="w-5 h-5 text-yellow-500 flex-shrink-0 mt-0.5" />
            <div className="flex-1 text-sm text-yellow-200">
              <p className="font-medium mb-1">HeyGen API Key Required</p>
              <p className="text-yellow-300/80">To use the Live Avatar feature, please add your HeyGen API key to the .env file as VITE_HEYGEN_API_KEY.</p>
            </div>
          </motion.div>
        )}

        {/* Main Content */}
        <div className="flex-1 grid grid-cols-1 lg:grid-cols-3 gap-6 pb-6">
          {/* Video Section */}
          <motion.div 
            initial={{ opacity: 0, x: -20 }} 
            animate={{ opacity: 1, x: 0 }}
            className="glass rounded-2xl p-6 lg:col-span-2 flex flex-col"
          >
            <div className="flex items-center justify-between mb-4">
              <h2 className="text-xl font-bold text-white">Avatar Stream</h2>
              <div className="flex items-center gap-2">
                <span className={`text-xs px-2 py-1 rounded-full ${
                  isConnected ? 'bg-green-500/20 text-green-400' : 'bg-gray-500/20 text-gray-400'
                }`}>
                  {connectionState}
                </span>
                {!isConnected ? (
                  <button
                    onClick={handleConnect}
                    disabled={isConnecting || !hasApiKey}
                    className="flex items-center gap-2 px-4 py-2 bg-gradient-to-r from-primary to-purple-600 text-white font-medium text-sm rounded-lg hover:opacity-90 transition-all disabled:opacity-50 disabled:cursor-not-allowed"
                  >
                    {isConnecting ? (
                      <>
                        <Loader2 className="w-4 h-4 animate-spin" />
                        Connecting...
                      </>
                    ) : (
                      <>
                        <Power className="w-4 h-4" />
                        Connect
                      </>
                    )}
                  </button>
                ) : (
                  <button
                    onClick={handleDisconnect}
                    className="flex items-center gap-2 px-4 py-2 bg-red-500/20 text-red-400 font-medium text-sm rounded-lg hover:bg-red-500/30 transition-colors"
                  >
                    <Power className="w-4 h-4" />
                    Disconnect
                  </button>
                )}
              </div>
            </div>

            {/* Video Display */}
            <div className="flex-1 bg-gray-800/50 rounded-lg overflow-hidden relative" style={{ minHeight: '500px' }}>
              {/* Always render video element */}
              <video
                ref={videoRef}
                autoPlay
                playsInline
                muted={false}
                className={`w-full h-full object-contain ${
                  isConnected ? 'block' : 'hidden'
                }`}
              />
              
              {/* Loading/Placeholder states */}
              {!isConnected && !isConnecting && (
                <div className="absolute inset-0 flex items-center justify-center text-center text-gray-500">
                  <div>
                    <VideoIcon className="w-16 h-16 mx-auto mb-4 opacity-30" />
                    <p>Click Connect to start your avatar session</p>
                  </div>
                </div>
              )}
              
              {isConnecting && (
                <div className="absolute inset-0 flex items-center justify-center text-center">
                  <div>
                    <Loader2 className="w-12 h-12 text-primary animate-spin mx-auto mb-4" />
                    <p className="text-gray-400">Connecting to avatar...</p>
                  </div>
                </div>
              )}
            </div>

            {/* Error Display */}
            <AnimatePresence>
              {error && (
                <motion.div
                  initial={{ opacity: 0, y: -10 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0 }}
                  className="mt-4 bg-red-500/10 border border-red-500/50 rounded-lg p-3 text-red-400 text-sm"
                >
                  {error}
                </motion.div>
              )}
            </AnimatePresence>
          </motion.div>

          {/* Chat Section */}
          <motion.div 
            initial={{ opacity: 0, x: 20 }} 
            animate={{ opacity: 1, x: 0 }}
            className="glass rounded-2xl p-6 flex flex-col"
          >
            <div className="flex items-center gap-2 mb-4">
              <Mic className="w-6 h-6 text-primary" />
              <h2 className="text-xl font-bold text-white">Conversation</h2>
            </div>

            {/* Messages */}
            <div className="flex-1 overflow-y-auto space-y-3 mb-4" style={{ minHeight: '400px', maxHeight: '500px' }}>
              {messages.length === 0 ? (
                <div className="flex items-center justify-center h-full text-gray-500 text-sm">
                  No messages yet
                </div>
              ) : (
                messages.map((msg) => (
                  <div
                    key={msg.id}
                    className={`flex ${msg.sender === 'user' ? 'justify-end' : 'justify-start'}`}
                  >
                    <div
                      className={`max-w-[80%] px-4 py-2 rounded-lg ${
                        msg.sender === 'user'
                          ? 'bg-primary text-white'
                          : 'bg-gray-700 text-gray-100'
                      }`}
                    >
                      <p className="text-sm">{msg.text}</p>
                      <span className="text-xs opacity-70 mt-1 block">
                        {msg.timestamp.toLocaleTimeString()}
                      </span>
                    </div>
                  </div>
                ))
              )}
            </div>

            {/* Input */}
            <div className="flex gap-2">
              <input
                type="text"
                value={message}
                onChange={(e) => setMessage(e.target.value)}
                onKeyPress={(e) => e.key === 'Enter' && handleSendMessage()}
                placeholder="Type a message..."
                disabled={!isConnected || isSending}
                className="flex-1 px-4 py-2 bg-gray-800/50 border border-gray-700 rounded-lg text-white placeholder-gray-500 focus:outline-none focus:ring-2 focus:ring-primary/50 text-sm disabled:opacity-50"
              />
              <button
                onClick={handleSendMessage}
                disabled={!isConnected || !message.trim() || isSending}
                className="p-2 bg-primary hover:opacity-90 text-white rounded-lg transition-opacity disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {isSending ? (
                  <Loader2 className="w-5 h-5 animate-spin" />
                ) : (
                  <Send className="w-5 h-5" />
                )}
              </button>
            </div>
          </motion.div>
        </div>
      </div>
    </div>
  );
};
export default LiveAvatarPage;
