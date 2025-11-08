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

// Inpainting with mask - EXACT Android implementation (line 1772-1792)
export async function inpaintImage(
  imageFile: File,
  maskCanvas: HTMLCanvasElement,
  prompt: string,
  guidanceScale: number = 7.5,
  strength: number = 0.7
): Promise<{ imageUrl: string; generationTime?: number }> {
  try {
    console.log('Starting V51 Inpainting (Android method)...');
    
    // Convert original image to base64
    const base64Image = await imageToBase64(imageFile);

    // Convert mask canvas to base64 PNG  
    const maskBase64 = maskCanvas.toDataURL('image/png').split(',')[1];

    console.log('Image data prepared:', {
      imageLength: base64Image.length,
      maskLength: maskBase64.length
    });

    // EXACT Android request body (line 1772-1792)
    const requestBody = {
      key: API_CONFIG.MODELSLAB_API_KEY,
      model_id: 'v51_inpainting',
      prompt: prompt,
      negative_prompt: 'ugly, tiling, poorly drawn hands, poorly drawn feet, poorly drawn face, out of frame, extra limbs, disfigured, deformed, body out of frame, bad anatomy, watermark, signature, cut off, low contrast, underexposed, overexposed, bad art, beginner, amateur, distorted face, blurry, draft, grainy',
      init_image: base64Image,
      mask_image: maskBase64,
      samples: 1,
      steps: 21,
      safety_checker: 'no',
      guidance_scale: guidanceScale,
      strength: strength,
      scheduler: 'DPMSolverMultistepScheduler',
      tomesd: 'yes',
      use_karras_sigmas: 'yes',
      base64: "yes",
      webhook: null,
      track_id: null
    };

    console.log('Sending request to API...', requestBody);

    const response = await axios.post<ModelsLabResponse>(
      'https://modelslab.com/api/v6/images/inpaint',
      requestBody,
      {
        headers: {
          'Content-Type': 'application/json'
        },
        timeout: 180000
      }
    );

    console.log('Inpainting response:', response.data);

    if (response.data.status === 'success') {
      // Prefer proxy_links which gives stable URLs
      if (response.data.proxy_links && response.data.proxy_links.length > 0) {
        let imageUrl = response.data.proxy_links[0];
        
        // If URL ends with .base64, fetch and convert to actual image
        if (imageUrl.endsWith('.base64')) {
          console.log('Fetching .base64 URL:', imageUrl);
          try {
            const imgResponse = await axios.get(imageUrl, { responseType: 'text' });
            const base64Data = imgResponse.data;
            imageUrl = `data:image/png;base64,${base64Data}`;
            console.log('Converted .base64 to data URL');
          } catch (e) {
            console.error('Failed to fetch .base64:', e);
            // Continue with original URL
          }
        }
        
        console.log('Using proxy_link:', imageUrl.substring(0, 50));
        return {
          imageUrl: imageUrl,
          generationTime: response.data.generationTime
        };
      }
      // Fallback to output
      else if (response.data.output && response.data.output.length > 0) {
        let output = response.data.output[0];
        
        console.log('Raw output type:', typeof output);
        console.log('Raw output length:', output?.length);
        console.log('Output starts with:', output?.substring(0, 50));
        
        // Handle different response formats
        let imageUrl: string;
        
        if (output.startsWith('http')) {
          // It's a URL
          imageUrl = output;
          console.log('Image is URL');
        } else if (output.startsWith('data:image')) {
          // Already a data URL
          imageUrl = output;
          console.log('Image is already data URL');
        } else {
          // Raw base64, add data URL prefix
          imageUrl = `data:image/png;base64,${output}`;
          console.log('Converting base64 to data URL');
        }
        
        console.log('Final imageUrl starts with:', imageUrl.substring(0, 50));
        
        return {
          imageUrl: imageUrl,
          generationTime: response.data.generationTime
        };
      } else {
        throw new Error('No image was generated');
      }
    } else if (response.data.status === 'processing') {
      if (response.data.id) {
        return await pollForInpaintResults(response.data.id);
      }
      throw new Error('Image is processing but no request ID was provided');
    } else {
      const errorMsg = response.data.error || response.data.message || 'Unknown error';
      console.error('API returned error:', errorMsg, response.data);
      throw new Error(errorMsg);
    }
  } catch (error: any) {
    console.error('Full inpainting error:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status
    });
    
    if (error.response?.data?.message) {
      throw new Error(error.response.data.message);
    }
    if (error.response?.data?.error) {
      throw new Error(error.response.data.error);
    }
    if (error.response?.status === 500 || error.response?.status === 503) {
      throw new Error('Server error. Try again in few seconds.');
    }
    if (error.response?.status === 429) {
      throw new Error('Too many requests. Please wait.');
    }
    if (error.response?.status === 401 || error.response?.status === 403) {
      throw new Error('Invalid API key.');
    }
    if (error.message && !error.message.includes('Failed to inpaint')) {
      throw error;
    }
    
    throw new Error('Try again in few seconds');
  }
}

// Poll for inpainting results
async function pollForInpaintResults(
  requestId: string,
  maxAttempts: number = 40
): Promise<{ imageUrl: string; generationTime?: number }> {
  console.log('Starting polling for request:', requestId);
  
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    await new Promise(resolve => setTimeout(resolve, 3000)); // Wait 3 seconds

    try {
      const response = await axios.post<ModelsLabResponse>(
        'https://modelslab.com/api/v6/images/fetch',
        {
          key: API_CONFIG.MODELSLAB_API_KEY,
          request_id: requestId
        },
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );

      console.log(`Poll attempt ${attempt + 1}:`, response.data.status);

      if (response.data.status === 'success') {
        // Prefer proxy_links for stable URLs
        if (response.data.proxy_links && response.data.proxy_links.length > 0) {
          let imageUrl = response.data.proxy_links[0];
          
          // If URL ends with .base64, fetch and convert
          if (imageUrl.endsWith('.base64')) {
            try {
              const imgResponse = await axios.get(imageUrl, { responseType: 'text' });
              const base64Data = imgResponse.data;
              imageUrl = `data:image/png;base64,${base64Data}`;
            } catch (e) {
              console.error('Failed to fetch .base64 in poll:', e);
            }
          }
          
          return {
            imageUrl: imageUrl,
            generationTime: response.data.generationTime
          };
        }
        // Fallback to output
        else if (response.data.output && response.data.output.length > 0) {
          let output = response.data.output[0];
          
          // Handle different response formats
          let imageUrl: string;
          
          if (output.startsWith('http')) {
            imageUrl = output;
          } else if (output.startsWith('data:image')) {
            imageUrl = output;
          } else {
            imageUrl = `data:image/png;base64,${output}`;
          }
          
          return {
            imageUrl: imageUrl,
            generationTime: response.data.generationTime
          };
        }
      } else if (response.data.status === 'error' || response.data.status === 'failed') {
        throw new Error(response.data.error || response.data.message || 'Inpainting failed');
      }
      // Continue polling if still processing
    } catch (error: any) {
      console.error(`Poll error on attempt ${attempt + 1}:`, error.message);
      if (attempt === maxAttempts - 1) {
        throw new Error('Request timed out. Please try again.');
      }
      // Continue polling on error unless it's the last attempt
    }
  }

  throw new Error('Inpainting timed out. Please try again.');
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
