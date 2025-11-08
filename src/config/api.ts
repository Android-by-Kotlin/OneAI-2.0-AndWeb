// API Configuration
export const API_CONFIG = {
  // Gemini API Key
  GEMINI_API_KEY: import.meta.env.VITE_GEMINI_API_KEY || '',
  
  // OpenRouter API Key (for Claude, GPT-4, Llama, etc.)
  OPENROUTER_API_KEY: import.meta.env.VITE_OPENROUTER_API_KEY || '',
  
  // Nebius API Key (for FLUX image generation)
  IMAGE_GEN_API_KEY: import.meta.env.VITE_IMAGE_GEN_API_KEY || '',
  
  // Video Generation API Key (Minimax)
  VIDEO_GEN_API_KEY: import.meta.env.VITE_VIDEO_GEN_API_KEY || '',
  VIDEO_GEN_GROUP_ID: import.meta.env.VITE_VIDEO_GEN_GROUP_ID || '',
  
  // ModelsLab API Key (for image transformations)
  MODELSLAB_API_KEY: import.meta.env.VITE_MODELSLAB_API_KEY || '',
  
  // Stability AI API Key (for Stable Diffusion)
  STABILITY_API_KEY: import.meta.env.VITE_STABILITY_API_KEY || '',
  
  // HeyGen API Key (for live avatars)
  HEYGEN_API_KEY: import.meta.env.VITE_HEYGEN_API_KEY || '',
  
  // A4F API Key (for GPT-4o-mini and other models)
  A4F_API_KEY: import.meta.env.VITE_A4F_API_KEY || '',
};

export const API_ENDPOINTS = {
  // Image Generation
  NEBIUS_IMAGE_GEN: 'https://api.studio.nebius.ai/v1/images/generations',
  FLUX_IMAGE_GEN: 'https://api.bfl.ml/v1/flux-pro-1.1',
  MODELSLAB_TEXT_TO_IMAGE: 'https://modelslab.com/api/v6/images/text2img',
  
  // Image to Image
  MODELSLAB_IMAGE_TO_IMAGE: 'https://modelslab.com/api/v6/images/img2img',
  STABILITY_IMAGE_TO_IMAGE: 'https://api.stability.ai/v2beta/stable-image/generate/sd3',
  
  // Video Generation
  VIDEO_GENERATION: 'https://api.modelslab.com/api/v6/video/text2video',
  IMAGE_TO_VIDEO: 'https://api.modelslab.com/api/v6/video/img2video',
  
  // Chat & AI Models
  GEMINI_CHAT: 'https://generativelanguage.googleapis.com/v1beta/models',
  OPENROUTER_CHAT: 'https://openrouter.ai/api/v1/chat/completions',
  
  // Live Avatar (using proxy in development to avoid CORS)
  HEYGEN_STREAMING: import.meta.env.DEV ? '/api/heygen/v2/video/streaming' : 'https://api.heygen.com/v2/video/streaming',
  
  // A4F API
  A4F_CHAT: 'https://api.a4f.co/v1/chat/completions',
};

export const AI_MODELS = {
  TEXT_TO_IMAGE: [
    { id: 'flux.1.1-pro', name: 'FLUX 1.1 Pro', provider: 'nebius' },
    { id: 'provider-2/FLUX.1-kontext-max', name: 'FLUX Kontext Max', provider: 'nebius' },
    { id: 'imagegen-4', name: 'ImageGen-4', provider: 'modelslab' },
    { id: 'sdxl', name: 'Stable Diffusion XL', provider: 'modelslab' },
  ],
  IMAGE_TO_IMAGE: [
    { id: 'nano-banana', name: 'Nano Banana', provider: 'modelslab' },
    { id: 'ghibli', name: 'Ghibli Style', provider: 'modelslab' },
    { id: 'flux-kontext-pro', name: 'FLUX Kontext Pro', provider: 'nebius' },
    { id: 'stability-sd3', name: 'Stable Diffusion 3', provider: 'stability' },
  ],
  CHAT: [
    { id: 'models/gemini-2.0-flash', name: 'Gemini 2.0 Flash', provider: 'google' },
    { id: 'models/gemini-1.5-pro', name: 'Gemini 1.5 Pro', provider: 'google' },
    { id: 'google/gemini-2.0-flash', name: 'Gemini 2.0 Flash (OpenRouter)', provider: 'openrouter' },
    { id: 'anthropic/claude-3.5-sonnet', name: 'Claude 3.5 Sonnet', provider: 'openrouter' },
  ],
};
