import axios from 'axios';
import { API_CONFIG } from '../config/api';

interface VideoGenerationRequest {
  key: string;
  model_id: string;
  prompt: string;
  negative_prompt?: string;
  aspect_ratio: string;
  resolution: string;
  camera_fixed?: string | null;
  num_frames: string;
  num_inference_steps: string;
  guidance_scale: string;
  fps: string;
  seed?: string | null;
  webhook?: string | null;
  track_id?: string | null;
}

interface VideoGenerationResponse {
  status: string;
  generationTime?: number;
  id?: number | string;
  output?: string[];
  outputUrls?: string[];
  proxy_links?: string[];
  links?: string[];
  fetch_result?: string;
  eta?: number;
  error?: string;
  message?: string;
  meta?: any;
}

interface VideoPollRequest {
  key: string;
  request_id: string;
}

// Generate video using Seedance T2V model
export async function generateVideo(
  prompt: string,
  negativePrompt: string = 'blurry, low quality, distorted, artifacts, bad anatomy'
): Promise<{ videoUrl?: string; requestId?: string; generationTime?: number }> {
  try {
    console.log('Starting Seedance T2V video generation...');
    
    const requestBody: VideoGenerationRequest = {
      key: API_CONFIG.MODELSLAB_API_KEY,
      model_id: 'seedance-t2v',
      prompt: prompt,
      negative_prompt: negativePrompt,
      aspect_ratio: '16:9',
      resolution: '480p',
      camera_fixed: null,
      num_frames: '8',
      num_inference_steps: '20',
      guidance_scale: '7.0',
      fps: '16',
      seed: null,
      webhook: null,
      track_id: null
    };

    console.log('Sending Seedance T2V request to API...', requestBody);

    const response = await axios.post<VideoGenerationResponse>(
      'https://modelslab.com/api/v6/video/text2video',
      requestBody,
      {
        headers: {
          'Content-Type': 'application/json'
        },
        timeout: 180000 // 3 minutes
      }
    );

    console.log('Seedance T2V response:', response.data);

    // Check for immediate success with video URL
    if (response.data.outputUrls && response.data.outputUrls.length > 0) {
      const videoUrl = response.data.outputUrls[0];
      console.log('Seedance T2V video ready immediately:', videoUrl);
      return {
        videoUrl: videoUrl,
        generationTime: response.data.generationTime
      };
    }
    
    // Check for output field (alternative response format)
    if (response.data.output && response.data.output.length > 0) {
      const videoUrl = response.data.output[0];
      console.log('Seedance T2V video from output:', videoUrl);
      return {
        videoUrl: videoUrl,
        generationTime: response.data.generationTime
      };
    }
    
    // Check if we have a fetch_result URL to poll
    if (response.data.fetch_result) {
      console.log('Seedance T2V has fetch_result URL:', response.data.fetch_result);
      // Need to poll using fetch_result endpoint
      return await pollWithFetchUrl(response.data.fetch_result);
    }
    
    // Check for request ID to poll
    if (response.data.id && (response.data.status === 'processing' || response.data.status === 'pending')) {
      console.log('Seedance T2V is processing, request ID:', response.data.id);
      return {
        requestId: String(response.data.id)
      };
    }
    
    // If status indicates processing but no polling mechanism available
    if (response.data.status === 'processing' || response.data.status === 'pending') {
      throw new Error('Video is processing. Please try again later.');
    }
    
    // Error case
    const errorMsg = response.data.error || response.data.message || response.data.status || 'No video URL returned';
    console.error('Seedance T2V unexpected response:', errorMsg);
    throw new Error(errorMsg);
  } catch (error: any) {
    console.error('Full video generation error:', {
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
    if (error.message && !error.message.includes('Failed to generate')) {
      throw error;
    }
    
    throw new Error('Failed to generate video. Try again.');
  }
}

// Poll for video generation results using fetch endpoint
export async function pollForVideo(
  requestId: string,
  maxAttempts: number = 60
): Promise<{ videoUrl: string; generationTime?: number }> {
  console.log('Starting Seedance T2V polling for request:', requestId);
  
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    // Wait 10 seconds between polls (except first attempt)
    if (attempt > 0) {
      await new Promise(resolve => setTimeout(resolve, 10000));
    }

    try {
      const response = await axios.post<VideoGenerationResponse>(
        `https://modelslab.com/api/v6/video/fetch/${requestId}`,
        {
          key: API_CONFIG.MODELSLAB_API_KEY
        },
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );

      console.log(`Seedance T2V poll attempt ${attempt + 1}/${maxAttempts}:`, response.data.status);

      // Check for success with video URLs
      if ((response.data.status === 'success' || response.data.status === 'completed') && 
          (response.data.outputUrls || response.data.output)) {
        const videoUrl = response.data.outputUrls?.[0] || response.data.output?.[0];
        if (videoUrl) {
          console.log('Seedance T2V video ready:', videoUrl);
          return {
            videoUrl: videoUrl,
            generationTime: response.data.generationTime
          };
        }
      }
      
      // Check for failure
      if (response.data.status === 'error' || response.data.status === 'failed') {
        throw new Error(response.data.error || response.data.message || 'Video generation failed');
      }
      
      // Log ETA if available
      if (response.data.eta) {
        console.log(`Seedance T2V ETA: ${response.data.eta}s`);
      }
      
      // Continue polling if still processing
    } catch (error: any) {
      console.error(`Seedance T2V poll error on attempt ${attempt + 1}:`, error.message);
      if (attempt === maxAttempts - 1) {
        throw new Error('Request timed out. Please try again.');
      }
      // Continue polling on error unless it's the last attempt
    }
  }

  throw new Error('Seedance T2V generation timed out. Please try again.');
}

// Poll using fetch_result URL
async function pollWithFetchUrl(
  fetchUrl: string,
  maxAttempts: number = 60
): Promise<{ videoUrl?: string; requestId?: string; generationTime?: number }> {
  console.log('Polling with fetch_result URL:', fetchUrl);
  
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    // Wait 10 seconds between polls (except first attempt)
    if (attempt > 0) {
      await new Promise(resolve => setTimeout(resolve, 10000));
    }

    try {
      const response = await axios.get<VideoGenerationResponse>(fetchUrl);
      
      console.log(`Fetch URL poll attempt ${attempt + 1}/${maxAttempts}:`, response.data.status);

      // Check for success with video URLs
      if ((response.data.status === 'success' || response.data.status === 'completed') && 
          (response.data.outputUrls || response.data.output)) {
        const videoUrl = response.data.outputUrls?.[0] || response.data.output?.[0];
        if (videoUrl) {
          console.log('Video ready from fetch URL:', videoUrl);
          return {
            videoUrl: videoUrl,
            generationTime: response.data.generationTime
          };
        }
      }
      
      // Check for failure
      if (response.data.status === 'error' || response.data.status === 'failed') {
        throw new Error(response.data.error || response.data.message || 'Video generation failed');
      }
      
      // Continue polling if still processing
    } catch (error: any) {
      console.error(`Fetch URL poll error on attempt ${attempt + 1}:`, error.message);
      if (attempt === maxAttempts - 1) {
        throw new Error('Request timed out. Please try again.');
      }
    }
  }

  throw new Error('Video generation timed out. Please try again.');
}
