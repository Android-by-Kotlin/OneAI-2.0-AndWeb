import axios from 'axios';
import { API_CONFIG } from '../config/api';

export interface ImageModel {
  id: string;
  name: string;
  description: string;
}

export const IMAGE_MODELS: ImageModel[] = [
  {
    id: 'nano-banana',
    name: '🍌 Nano Banana',
    description: 'Realistic • Balanced'
  },
  {
    id: 'imagen-4',
    name: 'ImageGen-4 Premium',
    description: 'Premium • Next-Gen Quality'
  }
];

interface ModelsLabRequest {
  key: string;
  prompt: string;
  negative_prompt: string;
  model_id: string;
  scheduler: string;
  safety_checker: string;
  width: string;
  height: string;
  guidance_scale: number;
  num_inference_steps: string;
  seed: string;
  steps: string;
  samples: string;
  full_url: string;
  instant_response: string;
  tomesd: string;
  free_u: string;
  upscale: number;
  multi_lingual: string;
  panorama: string;
  self_attention: string;
  use_karras_sigmas: string;
  algorithm_type: string;
  safety_checker_type: string;
  embeddings: null;
  vae: null;
  lora_model: null;
  enhance_prompt: boolean;
  lora_strength: number;
  clip_skip: number;
  temp: string;
  base64: boolean;
}

interface ModelsLabResponse {
  status: string;
  generationTime?: number;
  id?: string;
  output?: string[];
  meta?: any;
  error?: string;
  nsfw_content_detected?: boolean;
  nsfw_filter?: string;
}

interface A4FImageRequest {
  model: string;
  prompt: string;
  n: number;
  size: string;
}

interface A4FImageResponse {
  data: Array<{
    url: string;
  }>;
}

const NEGATIVE_PROMPT = "(nsfw, naked, nude, show breast, deformed iris, deformed pupils, semi-realistic, cgi, 3d, render, sketch, cartoon, drawing, anime, mutated hands and fingers:1.4), (deformed, distorted, disfigured:1.3), poorly drawn, bad anatomy, wrong anatomy, extra limb, missing limb, floating limbs, disconnected limbs, mutation, mutated, ugly, disgusting, amputation";

export async function generateImageWithModelsLab(
  prompt: string,
  modelId: string = 'nano-banana'
): Promise<{ imageUrl: string; generationTime?: number }> {
  try {
    const requestBody: ModelsLabRequest = {
      key: API_CONFIG.MODELSLAB_API_KEY,
      prompt: prompt,
      negative_prompt: NEGATIVE_PROMPT,
      model_id: modelId === 'nano-banana' ? 'nano-banana' : 'serenity-v2-1-safetensors',
      scheduler: 'DPMSolverSinglestepScheduler',
      safety_checker: 'yes',
      width: '768',
      height: '1024',
      guidance_scale: 7.5,
      num_inference_steps: '31',
      seed: '0',
      steps: '21',
      samples: '1',
      full_url: 'no',
      instant_response: 'no',
      tomesd: 'yes',
      free_u: 'no',
      upscale: 0,
      multi_lingual: 'no',
      panorama: 'no',
      self_attention: 'no',
      use_karras_sigmas: 'yes',
      algorithm_type: '',
      safety_checker_type: 'sensitive_content_text',
      embeddings: null,
      vae: null,
      lora_model: null,
      enhance_prompt: true,
      lora_strength: 1,
      clip_skip: 1,
      temp: 'no',
      base64: false
    };

    const response = await axios.post<ModelsLabResponse>(
      'https://modelslab.com/api/v7/images/text-to-image',
      requestBody,
      {
        headers: {
          'key': API_CONFIG.MODELSLAB_API_KEY,
          'Content-Type': 'application/json'
        },
        timeout: 120000 // 2 minutes timeout
      }
    );

    if (response.data.status === 'success') {
      // Check for NSFW content
      const nsfwDetected = 
        response.data.nsfw_content_detected ||
        response.data.nsfw_filter === 'triggered' ||
        response.data.meta?.nsfw_detected;

      if (nsfwDetected) {
        throw new Error('Please provide safe prompt.');
      }

      if (response.data.output && response.data.output.length > 0) {
        return {
          imageUrl: response.data.output[0],
          generationTime: response.data.generationTime
        };
      } else {
        throw new Error('No image was generated');
      }
    } else if (response.data.status === 'processing') {
      // Poll for results
      if (response.data.id) {
        return await pollForImageResults(response.data.id);
      }
      throw new Error('Image is processing but no request ID was provided');
    } else {
      throw new Error(response.data.error || 'Failed to generate image');
    }
  } catch (error: any) {
    console.error('ModelsLab API error:', error);
    
    if (error.response?.status === 500 || error.response?.status === 503) {
      throw new Error('Model is experiencing heavy traffic. Please try again later.');
    }
    if (error.response?.status === 429) {
      throw new Error('Too many requests. Please wait a moment before trying again.');
    }
    if (error.response?.status === 401) {
      throw new Error('Invalid API key. Please check your configuration.');
    }
    if (error.message) {
      throw error;
    }
    
    throw new Error('Failed to generate image. Please try again.');
  }
}

async function pollForImageResults(
  requestId: string,
  maxAttempts: number = 30
): Promise<{ imageUrl: string; generationTime?: number }> {
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    await new Promise(resolve => setTimeout(resolve, 2000)); // Wait 2 seconds

    try {
      const response = await axios.post<ModelsLabResponse>(
        'https://modelslab.com/api/v7/images/fetch',
        {
          key: API_CONFIG.MODELSLAB_API_KEY,
          request_id: requestId
        },
        {
          headers: {
            'key': API_CONFIG.MODELSLAB_API_KEY,
            'Content-Type': 'application/json'
          }
        }
      );

      if (response.data.status === 'success') {
        // Check for NSFW content
        const nsfwDetected = 
          response.data.nsfw_content_detected ||
          response.data.nsfw_filter === 'triggered' ||
          response.data.meta?.nsfw_detected;

        if (nsfwDetected) {
          throw new Error('Please provide safe prompt.');
        }

        if (response.data.output && response.data.output.length > 0) {
          return {
            imageUrl: response.data.output[0],
            generationTime: response.data.generationTime
          };
        }
      } else if (response.data.status === 'error' || response.data.status === 'failed') {
        throw new Error(response.data.error || 'Failed to generate image');
      }
      // Continue polling if still processing
    } catch (error) {
      if (attempt === maxAttempts - 1) {
        throw error;
      }
      // Continue polling on error unless it's the last attempt
    }
  }

  throw new Error('Image generation timed out. Please try again.');
}

export async function generateImageWithA4F(
  prompt: string,
  modelId: string = 'imagen-4'
): Promise<{ imageUrl: string }> {
  try {
    const requestBody: A4FImageRequest = {
      model: 'provider-4/imagen-4',
      prompt: prompt,
      n: 1,
      size: '1080x1080'
    };

    const response = await axios.post<A4FImageResponse>(
      'https://api.a4f.co/v1/images/generations',
      requestBody,
      {
        headers: {
          'Authorization': `Bearer ${API_CONFIG.A4F_API_KEY}`,
          'Content-Type': 'application/json'
        },
        timeout: 120000
      }
    );

    if (response.data.data && response.data.data.length > 0) {
      return {
        imageUrl: response.data.data[0].url
      };
    } else {
      throw new Error('No image was generated');
    }
  } catch (error: any) {
    console.error('A4F API error:', error);
    
    if (error.response?.status === 500 || error.response?.status === 503) {
      throw new Error('ImageGen-4 is experiencing heavy traffic. Please try again later.');
    }
    if (error.response?.status === 429) {
      throw new Error('Too many requests. Please wait a moment before trying again.');
    }
    if (error.response?.status === 401) {
      throw new Error('Invalid API key. Please check your configuration.');
    }
    
    throw new Error('Failed to generate image. Please try again.');
  }
}

export function downloadImage(imageUrl: string, filename: string = 'generated-image.png') {
  const link = document.createElement('a');
  link.href = imageUrl;
  link.download = filename;
  link.target = '_blank';
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
}

export function shareImage(imageUrl: string, prompt: string) {
  if (navigator.share) {
    navigator.share({
      title: 'Generated Image',
      text: prompt,
      url: imageUrl
    }).catch(err => console.log('Error sharing:', err));
  } else {
    // Fallback: copy URL to clipboard
    navigator.clipboard.writeText(imageUrl).then(() => {
      alert('Image URL copied to clipboard!');
    });
  }
}
