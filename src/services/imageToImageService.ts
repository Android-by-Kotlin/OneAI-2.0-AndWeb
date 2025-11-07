import axios from 'axios';
import { API_CONFIG } from '../config/api';

interface ModelsLabImg2ImgRequest {
  key: string;
  prompt: string;
  model_id: string;
  init_image: string;
  init_image_2?: string;
  width: string;
  height: string;
  samples: string;
  num_inference_steps: string;
  guidance_scale: string;
  strength?: string;
  negative_prompt?: string;
  seed: null;
  webhook: null;
  track_id: null;
}

interface ModelsLabResponse {
  status: string;
  generationTime?: number;
  id?: string;
  output?: string[];
  meta?: any;
  error?: string;
  message?: string;
}

// Convert image file to base64 string
export async function imageToBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const base64 = reader.result as string;
      // Remove data URI prefix and return just the base64 string
      const base64String = base64.split(',')[1];
      resolve(base64String);
    };
    reader.onerror = reject;
    reader.readAsDataURL(file);
  });
}

// Generate image using nano-banana model (single image mode)
export async function transformImageSingle(
  imageFile: File,
  prompt: string,
  strength: number = 0.7,
  guidanceScale: number = 7.5
): Promise<{ imageUrl: string; generationTime?: number }> {
  try {
    // Convert image to base64
    const base64Image = await imageToBase64(imageFile);
    const formattedImage = `data:image/jpeg;base64,${base64Image}`;

    const requestBody: ModelsLabImg2ImgRequest = {
      key: API_CONFIG.MODELSLAB_API_KEY,
      prompt: prompt,
      model_id: 'nano-banana',
      init_image: formattedImage,
      width: '768',
      height: '1024',
      samples: '1',
      num_inference_steps: '25',
      guidance_scale: guidanceScale.toString(),
      strength: strength.toString(),
      negative_prompt: '',
      seed: null,
      webhook: null,
      track_id: null
    };

    const response = await axios.post<ModelsLabResponse>(
      'https://modelslab.com/api/v7/images/image-to-image',
      requestBody,
      {
        headers: {
          'key': API_CONFIG.MODELSLAB_API_KEY,
          'Content-Type': 'application/json'
        },
        timeout: 120000
      }
    );

    if (response.data.status === 'success') {
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
      throw new Error(response.data.error || response.data.message || 'Failed to transform image');
    }
  } catch (error: any) {
    console.error('Image transformation error:', error);
    
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
    
    throw new Error('Failed to transform image. Please try again.');
  }
}

// Generate image using nano-banana model (dual image mode)
export async function transformImageDual(
  imageFile1: File,
  imageFile2: File,
  prompt: string,
  guidanceScale: number = 7.5
): Promise<{ imageUrl: string; generationTime?: number }> {
  try {
    // Convert both images to base64
    const base64Image1 = await imageToBase64(imageFile1);
    const base64Image2 = await imageToBase64(imageFile2);
    
    const formattedImage1 = `data:image/jpeg;base64,${base64Image1}`;
    const formattedImage2 = `data:image/jpeg;base64,${base64Image2}`;

    const requestBody: ModelsLabImg2ImgRequest = {
      key: API_CONFIG.MODELSLAB_API_KEY,
      prompt: prompt,
      model_id: 'nano-banana',
      init_image: formattedImage1,
      init_image_2: formattedImage2,
      width: '768',
      height: '1024',
      samples: '1',
      num_inference_steps: '25',
      guidance_scale: guidanceScale.toString(),
      seed: null,
      webhook: null,
      track_id: null
    };

    const response = await axios.post<ModelsLabResponse>(
      'https://modelslab.com/api/v7/images/image-to-image',
      requestBody,
      {
        headers: {
          'key': API_CONFIG.MODELSLAB_API_KEY,
          'Content-Type': 'application/json'
        },
        timeout: 120000
      }
    );

    if (response.data.status === 'success') {
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
      throw new Error(response.data.error || response.data.message || 'Failed to transform image');
    }
  } catch (error: any) {
    console.error('Dual image transformation error:', error);
    
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
    
    throw new Error('Failed to transform images. Please try again.');
  }
}

// Inpainting with mask
export async function inpaintImage(
  imageFile: File,
  maskCanvas: HTMLCanvasElement,
  prompt: string,
  guidanceScale: number = 7.5,
  strength: number = 0.7
): Promise<{ imageUrl: string; generationTime?: number }> {
  try {
    // Convert original image to base64
    const base64Image = await imageToBase64(imageFile);
    const formattedImage = `data:image/jpeg;base64,${base64Image}`;

    // Convert mask canvas to base64 PNG
    const maskBase64 = maskCanvas.toDataURL('image/png').split(',')[1];
    const formattedMask = `data:image/png;base64,${maskBase64}`;

    const requestBody = {
      key: API_CONFIG.MODELSLAB_API_KEY,
      model_id: 'v51_inpainting',
      prompt: prompt,
      negative_prompt: 'ugly, tiling, poorly drawn hands, poorly drawn feet, poorly drawn face, out of frame, extra limbs, disfigured, deformed, body out of frame, bad anatomy, watermark, signature, cut off, low contrast, underexposed, overexposed, bad art, beginner, amateur, distorted face, blurry, draft, grainy',
      init_image: formattedImage,
      mask_image: formattedMask,
      samples: 1,
      steps: 21,
      safety_checker: 'no',
      guidance_scale: guidanceScale,
      strength: strength,
      scheduler: 'DPMSolverMultistepScheduler',
      tomesd: 'yes',
      use_karras_sigmas: 'yes',
      base64: true,
      webhook: null,
      track_id: null
    };

    const response = await axios.post<ModelsLabResponse>(
      'https://modelslab.com/api/v6/images/inpaint',
      requestBody,
      {
        headers: {
          'key': API_CONFIG.MODELSLAB_API_KEY,
          'Content-Type': 'application/json'
        },
        timeout: 120000
      }
    );

    if (response.data.status === 'success') {
      if (response.data.output && response.data.output.length > 0) {
        return {
          imageUrl: response.data.output[0],
          generationTime: response.data.generationTime
        };
      } else {
        throw new Error('No image was generated');
      }
    } else if (response.data.status === 'processing') {
      if (response.data.id) {
        return await pollForImageResults(response.data.id);
      }
      throw new Error('Image is processing but no request ID was provided');
    } else {
      throw new Error(response.data.error || response.data.message || 'Failed to inpaint image');
    }
  } catch (error: any) {
    console.error('Inpainting error:', error);
    
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
    
    throw new Error('Failed to inpaint image. Please try again.');
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
        if (response.data.output && response.data.output.length > 0) {
          return {
            imageUrl: response.data.output[0],
            generationTime: response.data.generationTime
          };
        }
      } else if (response.data.status === 'error' || response.data.status === 'failed') {
        throw new Error(response.data.error || response.data.message || 'Failed to transform image');
      }
      // Continue polling if still processing
    } catch (error) {
      if (attempt === maxAttempts - 1) {
        throw error;
      }
      // Continue polling on error unless it's the last attempt
    }
  }

  throw new Error('Image transformation timed out. Please try again.');
}
