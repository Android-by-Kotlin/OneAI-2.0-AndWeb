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
      num_frames: '25',
      num_inference_steps: '20',
      guidance_scale: '7.0',
      fps: '5',
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

    // Check for success status with video URLs (immediate success)
    if (response.data.status === 'success' || response.data.status === 'completed') {
      // Check all possible URL fields
      const videoUrl = response.data.proxy_links?.[0] || 
                      response.data.links?.[0] || 
                      response.data.outputUrls?.[0] || 
                      response.data.output?.[0];
      
      if (videoUrl) {
        console.log('Seedance T2V video ready immediately (success status):', videoUrl);
        return {
          videoUrl: videoUrl,
          generationTime: response.data.generationTime
        };
      }
    }
    
    // Check for video URLs even without explicit success status
    if (response.data.outputUrls && response.data.outputUrls.length > 0) {
      const videoUrl = response.data.outputUrls[0];
      console.log('Seedance T2V video ready immediately:', videoUrl);
      return {
        videoUrl: videoUrl,
        generationTime: response.data.generationTime
      };
    }
    
    if (response.data.output && response.data.output.length > 0) {
      const videoUrl = response.data.output[0];
      console.log('Seedance T2V video from output:', videoUrl);
      return {
        videoUrl: videoUrl,
        generationTime: response.data.generationTime
      };
    }
    
    if (response.data.proxy_links && response.data.proxy_links.length > 0) {
      const videoUrl = response.data.proxy_links[0];
      console.log('Seedance T2V video from proxy_links:', videoUrl);
      return {
        videoUrl: videoUrl,
        generationTime: response.data.generationTime
      };
    }
    
    if (response.data.links && response.data.links.length > 0) {
      const videoUrl = response.data.links[0];
      console.log('Seedance T2V video from links:', videoUrl);
      return {
        videoUrl: videoUrl,
        generationTime: response.data.generationTime
      };
    }
    
    // Check for request ID to poll (queued, processing, or pending)
    if (response.data.id && (response.data.status === 'queued' || response.data.status === 'processing' || response.data.status === 'pending')) {
      console.log('Seedance T2V is queued/processing, request ID:', response.data.id);
      return {
        requestId: String(response.data.id)
      };
    }
    
    // If status indicates processing but no polling mechanism available
    if (response.data.status === 'queued' || response.data.status === 'processing' || response.data.status === 'pending') {
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
          (response.data.outputUrls || response.data.output || response.data.proxy_links || response.data.links)) {
        const videoUrl = response.data.outputUrls?.[0] || response.data.output?.[0] || 
                        response.data.proxy_links?.[0] || response.data.links?.[0];
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
      
      // If error is 405 Method Not Allowed, stop immediately
      if (error.response?.status === 405) {
        console.error('GET method not supported for this endpoint. Stopping poll.');
        throw new Error('Unable to check video status. The video may be ready at the provided URL.');
      }
      
      // Stop on final attempt
      if (attempt === maxAttempts - 1) {
        throw new Error('Request timed out. Please try again.');
      }
      
      // Stop on repeated errors after 5 attempts
      if (attempt >= 5 && error.response?.status) {
        console.error('Multiple polling errors. Stopping.');
        throw new Error('Unable to check video status. Please try again later.');
      }
      
      // Continue polling on error unless stopped above
    }
  }

  throw new Error('Seedance T2V generation timed out. Please try again.');
}

// Poll using fetch_result URL (NOT USED - GET method not supported)
// Keeping for reference but this endpoint doesn't support GET requests
async function pollWithFetchUrl(
  fetchUrl: string,
  maxAttempts: number = 10
): Promise<{ videoUrl?: string; requestId?: string; generationTime?: number }> {
  console.log('Polling with fetch_result URL:', fetchUrl);
  console.warn('Note: This endpoint may not support GET requests');
  
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
          (response.data.outputUrls || response.data.output || response.data.proxy_links || response.data.links)) {
        const videoUrl = response.data.outputUrls?.[0] || response.data.output?.[0] || 
                        response.data.proxy_links?.[0] || response.data.links?.[0];
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
      
      // If GET method not supported (405), stop immediately
      if (error.response?.status === 405) {
        console.error('GET method not supported. Stopping fetch_result polling.');
        throw new Error('fetch_result endpoint does not support GET method');
      }
      
      // Stop after 3 failed attempts
      if (attempt >= 2) {
        throw new Error('fetch_result polling failed. Video may still be processing.');
      }
    }
  }

  throw new Error('Video generation timed out. Please try again.');
}
