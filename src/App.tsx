import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { AuthProvider, useAuth } from './contexts/AuthContext';
import SplashScreen from './pages/SplashScreen';
import LoginPage from './pages/LoginPage';
import HomePage from './pages/HomePage';
import ProfilePage from './pages/ProfilePage';
import ImageGeneratorPage from './pages/ImageGeneratorPage';
import ImageToImagePage from './pages/ImageToImagePage';
import ChatBotPage from './pages/ChatBotPage';
import VideoGenerationPage from './pages/VideoGenerationPage';
import ImageToVideoPage from './pages/ImageToVideoPage';
import SketchToImagePage from './pages/Inpainting';
import LiveAvatarPage from './pages/LiveAvatarPage';

// Protected Route Component
const ProtectedRoute = ({ children }: { children: React.ReactNode }) => {
  const { user, loading } = useAuth();

  if (loading) {
    return <SplashScreen />;
  }

  if (!user) {
    return <Navigate to="/login" replace />;
  }

  return <>{children}</>;
};

function AppRoutes() {
  return (
    <Router>
      <Routes>
        <Route path="/" element={<SplashScreen />} />
        <Route path="/login" element={<LoginPage />} />
        <Route
          path="/home"
          element={
            <ProtectedRoute>
              <HomePage />  
            </ProtectedRoute>
          }
        />
        <Route
          path="/profile"
          element={
            <ProtectedRoute>
              <ProfilePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/image-generator"
          element={
            <ProtectedRoute>
              <ImageGeneratorPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/image-to-image"
          element={
            <ProtectedRoute>
              <ImageToImagePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/chatbot"
          element={
            <ProtectedRoute>
              <ChatBotPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/video-generation"
          element={
            <ProtectedRoute>
              <VideoGenerationPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/image-to-video"
          element={
            <ProtectedRoute>
              <ImageToVideoPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/sketch-to-image"
          element={
            <ProtectedRoute>
              <SketchToImagePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/live-avatar"
          element={
            <ProtectedRoute>
              <LiveAvatarPage />
            </ProtectedRoute>
          }
        />
      </Routes>
    </Router>
  );
}

function App() {
  return (
    <AuthProvider>
      <AppRoutes />
    </AuthProvider>
  );
}

export default App;
