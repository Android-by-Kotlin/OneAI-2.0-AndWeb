import { useState, useRef, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { motion, AnimatePresence } from 'framer-motion';
import { ArrowLeft, Send, Loader, Trash2, Settings, Image as ImageIcon, X, Plus, MessageSquare, Clock } from 'lucide-react';
import { sendMessage, generateMessageId, AVAILABLE_MODELS, type Message } from '../services/chatService';
import { 
  createChatSession, 
  updateChatSession, 
  getUserChatSessions, 
  deleteChatSession,
  generateChatTitle,
  type ChatSession 
} from '../services/chatHistoryService';
import { useAuth } from '../contexts/AuthContext';

// Typing effect component
const TypingText = ({ text, speed = 5, onUpdate }: { text: string; speed?: number; onUpdate?: () => void }) => {
  const [displayedText, setDisplayedText] = useState('');
  const [currentIndex, setCurrentIndex] = useState(0);

  useEffect(() => {
    if (currentIndex < text.length) {
      const timeout = setTimeout(() => {
        setDisplayedText(prev => prev + text[currentIndex]);
        setCurrentIndex(prev => prev + 1);
        // Trigger scroll update
        if (onUpdate) {
          onUpdate();
        }
      }, speed);
      return () => clearTimeout(timeout);
    }
  }, [currentIndex, text, speed, onUpdate]);

  return <span>{displayedText}</span>;
};

const ChatBotPage = () => {
  const navigate = useNavigate();
  const { user } = useAuth();
  const [messages, setMessages] = useState<Message[]>([]);
  const [inputText, setInputText] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [selectedModel, setSelectedModel] = useState('models/gemini-2.0-flash');
  const [showSettings, setShowSettings] = useState(false);
  const [typingMessageId, setTypingMessageId] = useState<string | null>(null);
  const [selectedImage, setSelectedImage] = useState<string | null>(null);
  const [chatSessions, setChatSessions] = useState<ChatSession[]>([]);
  const [currentSessionId, setCurrentSessionId] = useState<string | null>(null);
  const [showHistory, setShowHistory] = useState(false);
  const [isLoadingHistory, setIsLoadingHistory] = useState(false);
  const messagesEndRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  // Scroll function that can be called during typing
  const scrollDuringTyping = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  // Load chat history once on mount
  useEffect(() => {
    if (user?.uid) {
      loadChatHistory();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [user?.uid]);

  const loadChatHistory = async () => {
    if (!user?.uid) return;
    
    setIsLoadingHistory(true);
    try {
      const sessions = await getUserChatSessions(user.uid);
      setChatSessions(sessions);
    } catch (err) {
      console.error('Error loading chat history:', err);
    } finally {
      setIsLoadingHistory(false);
    }
  };

  const startNewChat = async () => {
    if (!user?.uid) return;

    setMessages([]);
    setCurrentSessionId(null);
    setSelectedImage(null);
    setError(null);
    setShowHistory(false);
  };

  const loadChatSession = (session: ChatSession) => {
    setMessages(session.messages);
    setCurrentSessionId(session.id);
    setSelectedModel(session.model);
    setShowHistory(false);
  };

  const deleteChat = async (sessionId: string) => {
    if (!user?.uid) return;
    
    try {
      await deleteChatSession(user.uid, sessionId);
      setChatSessions(prev => prev.filter(s => s.id !== sessionId));
      if (currentSessionId === sessionId) {
        startNewChat();
      }
    } catch (err) {
      console.error('Error deleting chat:', err);
    }
  };

  const handleImageSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      const reader = new FileReader();
      reader.onloadend = () => {
        setSelectedImage(reader.result as string);
      };
      reader.readAsDataURL(file);
    }
  };

  const removeImage = () => {
    setSelectedImage(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const handleSend = async () => {
    if ((!inputText.trim() && !selectedImage) || isLoading) return;
    
    if (!user?.uid) {
      setError('Please sign in to save chat history');
      return;
    }

    const userMessage: Message = {
      id: generateMessageId(),
      role: 'user',
      content: inputText.trim() || 'Describe this image',
      timestamp: Date.now(),
      model: selectedModel,
      image: selectedImage || undefined
    };

    const updatedMessages = [...messages, userMessage];
    setMessages(updatedMessages);
    const currentImage = selectedImage;
    setInputText('');
    setSelectedImage(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
    setIsLoading(true);
    setError(null);

    try {
      const response = await sendMessage(userMessage.content, selectedModel, messages, currentImage || undefined);
      
      const assistantMessage: Message = {
        id: generateMessageId(),
        role: 'assistant',
        content: response,
        timestamp: Date.now(),
        model: selectedModel
      };

      const finalMessages = [...updatedMessages, assistantMessage];
      setMessages(finalMessages);
      setTypingMessageId(assistantMessage.id);
      
      // Save to Firebase (non-blocking, errors won't break the chat)
      try {
        if (currentSessionId) {
          // Update existing session
          await updateChatSession(user.uid, currentSessionId, finalMessages);
          // Update local state without reloading
          setChatSessions(prev => 
            prev.map(session => 
              session.id === currentSessionId 
                ? { ...session, messages: finalMessages, updatedAt: Date.now() }
                : session
            )
          );
        } else {
          // Create new session
          const title = generateChatTitle(userMessage.content);
          const newSessionId = await createChatSession(user.uid, title, selectedModel);
          setCurrentSessionId(newSessionId);
          await updateChatSession(user.uid, newSessionId, finalMessages);
          // Add to local state without reloading from Firebase
          setChatSessions(prev => [{
            id: newSessionId,
            title,
            model: selectedModel,
            messages: finalMessages,
            createdAt: Date.now(),
            updatedAt: Date.now()
          }, ...prev]);
        }
      } catch (saveError: any) {
        console.error('Failed to save chat history:', saveError);
        // Don't show error to user - chat still works locally
      }
      
      // Clear typing effect after animation completes
      setTimeout(() => {
        setTypingMessageId(null);
      }, response.length * 5 + 500);
    } catch (err: any) {
      setError(err.message || 'Failed to get response');
      console.error('Chat error:', err);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSend();
    }
  };

  const clearChat = () => {
    setMessages([]);
    setError(null);
  };

  const currentModel = AVAILABLE_MODELS.find(m => m.id === selectedModel);

  // Group chats by date
  const groupChatsByDate = (sessions: ChatSession[]) => {
    const now = new Date();
    const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
    const yesterday = new Date(today);
    yesterday.setDate(yesterday.getDate() - 1);
    const lastWeek = new Date(today);
    lastWeek.setDate(lastWeek.getDate() - 7);
    const lastMonth = new Date(today);
    lastMonth.setDate(lastMonth.getDate() - 30);

    const groups: { [key: string]: ChatSession[] } = {
      'Today': [],
      'Yesterday': [],
      'Last 7 days': [],
      'Last 30 days': [],
      'Older': []
    };

    sessions.forEach(session => {
      const sessionDate = new Date(session.updatedAt);
      if (sessionDate >= today) {
        groups['Today'].push(session);
      } else if (sessionDate >= yesterday) {
        groups['Yesterday'].push(session);
      } else if (sessionDate >= lastWeek) {
        groups['Last 7 days'].push(session);
      } else if (sessionDate >= lastMonth) {
        groups['Last 30 days'].push(session);
      } else {
        groups['Older'].push(session);
      }
    });

    return groups;
  };

  const groupedChats = groupChatsByDate(chatSessions);

  return (
    <div className="min-h-screen bg-black flex relative">
      {/* Ambient Light Background */}
      <div className="fixed inset-0 pointer-events-none overflow-hidden">
        {/* Vignette Overlay */}
        <div className="absolute inset-0 bg-gradient-radial from-transparent via-transparent to-black opacity-60" />
        
        {/* Bottom Vignette - Green/Emerald glow */}
        <div className="absolute inset-0 blur-3xl" style={{
          background: 'radial-gradient(ellipse at 50% 120%, rgba(34, 197, 94, 0.35) 0%, rgba(22, 163, 74, 0.2) 30%, transparent 70%)'
        }} />
        
        {/* Left Side Glow */}
        <div className="absolute inset-0 blur-3xl opacity-60" style={{
          background: 'radial-gradient(ellipse at 0% 50%, rgba(34, 197, 94, 0.35) 0%, transparent 60%)'
        }} />
        
        {/* Right Side Glow */}
        <div className="absolute inset-0 blur-3xl opacity-60" style={{
          background: 'radial-gradient(ellipse at 100% 50%, rgba(16, 185, 129, 0.25) 0%, transparent 60%)'
        }} />
        
        {/* Top Subtle Glow */}
        <div className="absolute inset-0 blur-3xl opacity-40" style={{
          background: 'radial-gradient(ellipse at 50% -20%, rgba(22, 163, 74, 0.2) 0%, transparent 50%)'
        }} />
        
        {/* Center Depth Layer */}
        <div className="absolute inset-0 blur-3xl opacity-30" style={{
          background: 'radial-gradient(circle at 50% 50%, rgba(22, 163, 74, 0.2) 0%, transparent 70%)'
        }} />
      </div>
      
      {/* Content with proper z-index */}
      <div className="relative z-10 flex w-full">
      {/* Sidebar */}
      <AnimatePresence>
        {showHistory && (
          <motion.div
            initial={{ width: 0, opacity: 0 }}
            animate={{ width: 260, opacity: 1 }}
            exit={{ width: 0, opacity: 0 }}
            className="glass-dark border-r border-white border-opacity-10 overflow-hidden flex-shrink-0"
          >
            <div className="h-full flex flex-col">
              {/* Sidebar Header */}
              <div className="p-4 border-b border-white border-opacity-10">
                <button
                  onClick={startNewChat}
                  className="w-full flex items-center gap-2 px-4 py-2 bg-primary hover:bg-primary/80 text-white rounded-lg transition-colors"
                >
                  <Plus className="w-5 h-5" />
                  <span className="font-medium">New Chat</span>
                </button>
              </div>

              {/* Chat History List */}
              <div className="flex-1 overflow-y-auto p-2">
                {isLoadingHistory ? (
                  <div className="flex items-center justify-center py-8">
                    <Loader className="w-5 h-5 animate-spin text-primary" />
                  </div>
                ) : chatSessions.length === 0 ? (
                  <div className="text-center py-8 text-gray-400 text-sm px-4">
                    <MessageSquare className="w-12 h-12 mx-auto mb-2 opacity-30" />
                    <p>No chat history yet</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {Object.entries(groupedChats).map(([period, sessions]) => (
                      sessions.length > 0 && (
                        <div key={period}>
                          <h3 className="text-xs font-semibold text-gray-400 px-2 mb-2">{period}</h3>
                          <div className="space-y-1">
                            {sessions.map(session => (
                              <div
                                key={session.id}
                                className={`group relative rounded-lg px-3 py-2 hover:bg-white/10 transition-all cursor-pointer ${
                                  currentSessionId === session.id ? 'bg-white/10' : ''
                                }`}
                                onClick={() => loadChatSession(session)}
                              >
                                <div className="flex items-center justify-between gap-2">
                                  <div className="flex-1 min-w-0">
                                    <p className="text-white text-sm truncate">{session.title}</p>
                                  </div>
                                  <button
                                    onClick={(e) => {
                                      e.stopPropagation();
                                      deleteChat(session.id);
                                    }}
                                    className="opacity-0 group-hover:opacity-100 p-1 hover:bg-red-500/20 rounded transition-all"
                                    title="Delete"
                                  >
                                    <Trash2 className="w-4 h-4 text-gray-400 hover:text-red-400" />
                                  </button>
                                </div>
                              </div>
                            ))}
                          </div>
                        </div>
                      )
                    ))}
                  </div>
                )}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Main Content */}
      <div className="flex-1 flex flex-col min-w-0">
      {/* Header */}
      <div className="glass-dark border-b border-white border-opacity-10 px-4 py-3">
        <div className="max-w-5xl mx-auto flex items-center justify-between">
          <div className="flex items-center gap-4">
            <button
              onClick={() => navigate('/home')}
              className="flex items-center gap-2 text-gray-300 hover:text-white transition-colors"
            >
              <ArrowLeft className="w-5 h-5" />
              <span className="hidden sm:inline">Back</span>
            </button>
            <div>
              <h1 className="text-xl font-bold text-white">AI Chat</h1>
              <p className="text-xs text-gray-400">{currentModel?.name}</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={startNewChat}
              className="p-2 text-gray-400 hover:text-white transition-colors"
              title="New chat"
            >
              <Plus className="w-5 h-5" />
            </button>
            <button
              onClick={() => setShowHistory(!showHistory)}
              className="p-2 text-gray-400 hover:text-white transition-colors"
              title="Chat history"
            >
              <Clock className="w-5 h-5" />
            </button>
            <button
              onClick={clearChat}
              className="p-2 text-gray-400 hover:text-white transition-colors"
              title="Clear chat"
            >
              <Trash2 className="w-5 h-5" />
            </button>
            <button
              onClick={() => setShowSettings(!showSettings)}
              className="p-2 text-gray-400 hover:text-white transition-colors"
              title="Settings"
            >
              <Settings className="w-5 h-5" />
            </button>
          </div>
        </div>
      </div>

      {/* Settings Panel */}
      <AnimatePresence>
        {showSettings && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="glass-dark border-b border-white border-opacity-10 overflow-hidden"
          >
            <div className="max-w-5xl mx-auto p-4">
              <h3 className="text-sm font-semibold text-white mb-3">Select AI Model</h3>
              <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-3">
                {AVAILABLE_MODELS.map(model => (
                  <button
                    key={model.id}
                    onClick={() => {
                      setSelectedModel(model.id);
                      setShowSettings(false);
                    }}
                    className={`text-left p-3 rounded-lg transition-all ${
                      selectedModel === model.id
                        ? 'bg-primary bg-opacity-20 border-2 border-primary'
                        : 'glass border border-white border-opacity-10 hover:border-primary hover:border-opacity-50'
                    }`}
                  >
                    <div className="font-semibold text-white text-sm">{model.name}</div>
                    <div className="text-xs text-gray-400 mt-1">{model.description}</div>
                    <div className="text-xs text-primary mt-1">{model.provider}</div>
                  </button>
                ))}
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* Messages Area */}
      <div className="flex-1 overflow-y-auto px-4 py-6">
        <div className="max-w-5xl mx-auto space-y-4">
          {messages.length === 0 && (
            <div className="text-center py-12">
              <div className="w-16 h-16 bg-gradient-to-br from-primary to-accent rounded-full flex items-center justify-center mx-auto mb-4">
                <span className="text-2xl">💬</span>
              </div>
              <h2 className="text-2xl font-bold text-white mb-2">Start a conversation</h2>
              <p className="text-gray-400">Ask me anything! I'm powered by {currentModel?.name}</p>
            </div>
          )}

          {messages.map((message, index) => (
            <motion.div
              key={message.id}
              initial={{ opacity: 0, y: 20 }}
              animate={{ opacity: 1, y: 0 }}
              transition={{ delay: index * 0.05 }}
              className={`flex ${
                message.role === 'user' ? 'justify-end' : 'justify-start'
              }`}
            >
              <div
                className={`max-w-[80%] rounded-2xl px-4 py-3 ${
                  message.role === 'user'
                    ? 'bg-gradient-to-r from-primary to-accent text-white'
                    : 'glass text-white'
                }`}
              >
                {message.image && (
                  <div className="mb-2">
                    <img 
                      src={message.image} 
                      alt="Uploaded" 
                      className="max-w-full max-h-48 rounded-lg"
                    />
                  </div>
                )}
                <div className="whitespace-pre-wrap break-words">
                  {message.role === 'assistant' && typingMessageId === message.id ? (
                    <TypingText text={message.content} speed={5} onUpdate={scrollDuringTyping} />
                  ) : (
                    message.content
                  )}
                </div>
                <div className="text-xs opacity-60 mt-2">
                  {new Date(message.timestamp).toLocaleTimeString()}
                </div>
              </div>
            </motion.div>
          ))}

          {isLoading && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="flex justify-start"
            >
              <div className="glass rounded-2xl px-4 py-3 flex items-center gap-2">
                <Loader className="w-4 h-4 animate-spin text-primary" />
                <span className="text-gray-300">Thinking...</span>
              </div>
            </motion.div>
          )}

          {error && (
            <motion.div
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              className="glass bg-red-500 bg-opacity-10 border border-red-500 rounded-2xl px-4 py-3"
            >
              <p className="text-red-400 text-sm">{error}</p>
            </motion.div>
          )}

          <div ref={messagesEndRef} />
        </div>
      </div>

      {/* Input Area */}
      <div className="glass-dark border-t border-white border-opacity-10 p-4">
        <div className="max-w-5xl mx-auto">
          {/* Image Preview */}
          {selectedImage && (
            <div className="mb-3 relative inline-block">
              <img 
                src={selectedImage} 
                alt="Preview" 
                className="max-h-32 rounded-lg"
              />
              <button
                onClick={removeImage}
                className="absolute -top-2 -right-2 bg-red-500 hover:bg-red-600 text-white rounded-full p-1 transition-colors"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          )}
          <div className="flex gap-2 items-end">
            <div className="flex-1 glass rounded-2xl">
              <textarea
                ref={inputRef}
                value={inputText}
                onChange={(e) => setInputText(e.target.value)}
                onKeyDown={handleKeyDown}
                placeholder={selectedImage ? "Ask about this image..." : "Type your message..."}
                className="w-full px-4 py-3 bg-transparent text-white placeholder-gray-400 focus:outline-none resize-none"
                rows={1}
                style={{
                  minHeight: '44px',
                  maxHeight: '120px',
                  height: 'auto'
                }}
              />
            </div>
            {/* Image Upload Button - Only show for Gemini models */}
            {selectedModel.includes('gemini') && (
              <>
                <input
                  ref={fileInputRef}
                  type="file"
                  accept="image/*"
                  onChange={handleImageSelect}
                  className="hidden"
                />
                <button
                  onClick={() => fileInputRef.current?.click()}
                  disabled={isLoading}
                  className="px-4 py-3 glass hover:bg-white/10 text-white rounded-2xl transition-colors disabled:opacity-50 disabled:cursor-not-allowed"
                  title="Upload image"
                >
                  <ImageIcon className="w-5 h-5" />
                </button>
              </>
            )}
            <button
              onClick={handleSend}
              disabled={(!inputText.trim() && !selectedImage) || isLoading}
              className="px-6 py-3 bg-gradient-to-r from-primary to-accent text-white rounded-2xl hover:opacity-90 transition-opacity disabled:opacity-50 disabled:cursor-not-allowed flex items-center gap-2"
            >
              <Send className="w-5 h-5" />
              <span className="hidden sm:inline">Send</span>
            </button>
          </div>
        </div>
      </div>
      </div>
      </div>
    </div>
  );
};

export default ChatBotPage;
