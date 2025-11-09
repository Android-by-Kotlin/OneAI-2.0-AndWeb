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

  // Load chat history on mount
  useEffect(() => {
    if (user?.uid) {
      loadChatHistory();
    }
  }, [user]);

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
    try {
      await deleteChatSession(sessionId);
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
    if ((!inputText.trim() && !selectedImage) || isLoading || !user?.uid) return;

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
      
      // Save to Firebase
      if (currentSessionId) {
        // Update existing session
        await updateChatSession(currentSessionId, finalMessages);
      } else {
        // Create new session
        const title = generateChatTitle(userMessage.content);
        const newSessionId = await createChatSession(user.uid, title, selectedModel);
        setCurrentSessionId(newSessionId);
        await updateChatSession(newSessionId, finalMessages);
        // Reload history to show new chat
        loadChatHistory();
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

  return (
    <div className="min-h-screen bg-gradient-to-br from-gray-900 via-purple-900 to-gray-900 flex flex-col">
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

      {/* Chat History Sidebar */}
      <AnimatePresence>
        {showHistory && (
          <motion.div
            initial={{ height: 0, opacity: 0 }}
            animate={{ height: 'auto', opacity: 1 }}
            exit={{ height: 0, opacity: 0 }}
            className="glass-dark border-b border-white border-opacity-10 overflow-hidden"
          >
            <div className="max-w-5xl mx-auto p-4">
              <h3 className="text-sm font-semibold text-white mb-3">Chat History</h3>
              {isLoadingHistory ? (
                <div className="flex items-center justify-center py-8">
                  <Loader className="w-5 h-5 animate-spin text-primary" />
                </div>
              ) : chatSessions.length === 0 ? (
                <div className="text-center py-8 text-gray-400 text-sm">
                  <MessageSquare className="w-12 h-12 mx-auto mb-2 opacity-30" />
                  <p>No chat history yet</p>
                </div>
              ) : (
                <div className="space-y-2 max-h-96 overflow-y-auto">
                  {chatSessions.map(session => (
                    <div
                      key={session.id}
                      className={`glass rounded-lg p-3 hover:bg-white/10 transition-all cursor-pointer group ${
                        currentSessionId === session.id ? 'border-2 border-primary' : ''
                      }`}
                      onClick={() => loadChatSession(session)}
                    >
                      <div className="flex items-start justify-between gap-2">
                        <div className="flex-1 min-w-0">
                          <h4 className="text-white text-sm font-medium truncate">{session.title}</h4>
                          <p className="text-xs text-gray-400 mt-1">
                            {session.messages.length} messages • {session.model.split('/').pop()}
                          </p>
                          <p className="text-xs text-gray-500 mt-1">
                            {new Date(session.updatedAt).toLocaleDateString()}
                          </p>
                        </div>
                        <button
                          onClick={(e) => {
                            e.stopPropagation();
                            deleteChat(session.id);
                          }}
                          className="p-1 text-gray-400 hover:text-red-400 opacity-0 group-hover:opacity-100 transition-all"
                          title="Delete chat"
                        >
                          <Trash2 className="w-4 h-4" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
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
  );
};

export default ChatBotPage;
