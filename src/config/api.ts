// API Configuration
export const API_CONFIG = {
  // Note: In production, these should be in environment variables
  GEMINI_API_KEY: import.meta.env.VITE_GEMINI_API_KEY || 'AIzaSyCrWF3Wq9tN1-0IsWRoVC0MVhE0kS0YXeY',
  OPENROUTER_API_KEY: import.meta.env.VITE_OPENROUTER_API_KEY || '',
  IMAGE_GEN_API_KEY: import.meta.env.VITE_IMAGE_GEN_API_KEY || '',
  VIDEO_GEN_API_KEY: import.meta.env.VITE_VIDEO_GEN_API_KEY || '',
  MODELSLAB_API_KEY: import.meta.env.VITE_MODELSLAB_API_KEY || '',
  STABILITY_API_KEY: import.meta.env.VITE_STABILITY_API_KEY || '',
  HEYGEN_API_KEY: import.meta.env.VITE_HEYGEN_API_KEY || '',
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
  
  // Live Avatar
  HEYGEN_STREAMING: 'https://api.heygen.com/v1/streaming',
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
