// API Configuration - Keys from Android app
export const API_CONFIG = {
  // Gemini API Key
  GEMINI_API_KEY: import.meta.env.VITE_GEMINI_API_KEY || 'AIzaSyCrWF3Wq9tN1-0IsWRoVC0MVhE0kS0YXeY',
  
  // OpenRouter API Key (for Claude, GPT-4, Llama, etc.)
  OPENROUTER_API_KEY: import.meta.env.VITE_OPENROUTER_API_KEY || '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',
  
  // Nebius API Key (for FLUX image generation)
  IMAGE_GEN_API_KEY: import.meta.env.VITE_IMAGE_GEN_API_KEY || 'eyJhbGciOiJIUzI1NiIsImtpZCI6IlV6SXJWd1h0dnprLVRvdzlLZWstc0M1akptWXBvX1VaVkxUZlpnMDRlOFUiLCJ0eXAiOiJKV1QifQ.eyJzdWIiOiJnb29nbGUtb2F1dGgyfDExNjU5MDc4MTg4OTE1Mzc2MTEzMiIsInNjb3BlIjoib3BlbmlkIG9mZmxpbmVfYWNjZXNzIiwiaXNzIjoiYXBpX2tleV9pc3N1ZXIiLCJhdWQiOlsiaHR0cHM6Ly9uZWJpdXMtaW5mZXJlbmNlLmV1LmF1dGgwLmNvbS9hcGkvdjIvIl0sImV4cCI6MTkxMTg0NDA3OCwidXVpZCI6IjcyZWMxNjY1LTA0YTctNGMyOS1hMjZkLTA4OWVjY2JmZDc2YSIsIm5hbWUiOiJmbHV4IHNjaG5lbGwiLCJleHBpcmVzX2F0IjoiMjAzMC0wOC0wMVQxOTo0Nzo1OCswMDAwIn0.r7PqUiowcleFXQgfY39us2QshI9Xpr0kGv7T_-0ef8Q',
  
  // Video Generation API Key (Minimax)
  VIDEO_GEN_API_KEY: import.meta.env.VITE_VIDEO_GEN_API_KEY || 'eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJHcm91cE5hbWUiOiJPbSBNYW5kYWwiLCJVc2VyTmFtZSI6Ik9tIE1hbmRhbCIsIkFjY291bnQiOiIiLCJTdWJqZWN0SUQiOiIxOTI3MzQyMTkzNjcyOTgzMDE2IiwiUGhvbmUiOiIiLCJHcm91cElEIjoiMTkyNzM0MjE5MzY2ODc4ODcxMiIsIlBhZ2VOYW1lIjoiIiwiTWFpbCI6Im1heG9obTI0QGdtYWlsLmNvbSIsIkNyZWF0ZVRpbWUiOiIyMDI1LTA1LTI5IDE3OjIyOjMyIiwiVG9rZW5UeXBlIjoxLCJpc3MiOiJtaW5pbWF4In0.f46QK1qY7kgcFXv7vbYmTjmxItmAOweMls6e9UBBtGSOS4l5r6s6WhE-Nlz2dEDJvm1eG0FWXoTDpJ0bPszwArSF0DWGHM75SaeuJMz_YUr26s_jPYxSxsHvOqGDXqNza0hE0VGz8PcU0L5hpmKMb2ywm-gTecIbHal_m0TIuzhZ981lnZV_0zUIgWEiMPwlsV-FUdO40-Tu1uMLOVX2QZgtFfK4hCi3iMe9-tPgHVAj01haTQ9sRYMXhoMhyGVgsBYB1G2d62qzGbX8T4KDRAW0RQS_p887wKijoZ4gJYMUli50U7twJafO9xLZoPMHtKf7big_zez9A2krtKdcOw',
  VIDEO_GEN_GROUP_ID: '1927342193668788712',
  
  // ModelsLab API Key (for image transformations)
  MODELSLAB_API_KEY: import.meta.env.VITE_MODELSLAB_API_KEY || 'TYKbhmIv99CmDFruaavFUEDfHzWiMUWCyNMVluUqPlgKnx3a6oisBpwlhAc9',
  
  // Stability AI API Key (for Stable Diffusion)
  STABILITY_API_KEY: import.meta.env.VITE_STABILITY_API_KEY || '',
  
  // HeyGen API Key (for live avatars)
  HEYGEN_API_KEY: import.meta.env.VITE_HEYGEN_API_KEY || 'sk_V2_hgu_kuyqblDYNUX_JL2Wlbk1rqI4Yfws314pi1ffpKr326Bd',
  
  // A4F API Key (for GPT-4o-mini and other models)
  A4F_API_KEY: import.meta.env.VITE_A4F_API_KEY || 'ddc-a4f-2e9bee7f17d640d7bcba8fb26cf48d46',
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
  HEYGEN_STREAMING: import.meta.env.DEV ? '/api/heygen/v1/streaming' : 'https://api.heygen.com/v1/streaming',
  
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
