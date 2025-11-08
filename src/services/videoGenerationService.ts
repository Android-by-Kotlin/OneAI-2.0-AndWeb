import axios from 'axios';
import { API_CONFIG } from '../config/api';

interface VideoGenerationRequest {
  key: string;
  model_id: string;
  prompt: string;
  negative_prompt?: string;
  height: number;
  width: number;
  frames: number;
  webhook?: string | null;
  track_id?: string | null;
}

interface VideoGenerationResponse {
  status: string;
  generationTime?: number;
  id?: string;
  output?: string[];
  proxy_links?: string[];
  links?: string[];
  error?: string;
  message?: string;
  meta?: any;
}

interface VideoPollRequest {
  key: string;
  request_id: string;
}

// Generate video using CogVideoX model
export async function generateVideo(
  prompt: string,
  negativePrompt: string = 'low quality, worst quality, deformed, distorted',
  width: number = 720,
  height: number = 480,
  frames: number = 49
): Promise<{ videoUrl?: string; requestId?: string; generationTime?: number }> {
  try {
    console.log('Starting CogVideoX video generation...');
    
    const requestBody: VideoGenerationRequest = {
      key: API_CONFIG.MODELSLAB_API_KEY,
      model_id: 'cogvideox',
      prompt: prompt,
      negative_prompt: negativePrompt,
      height: height,
      width: width,
      frames: frames,
      webhook: null,
      track_id: null
    };

    console.log('Sending request to API...', requestBody);

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

    console.log('Video generation response:', response.data);

    if (response.data.status === 'success') {
      // Check for proxy_links first (stable CDN URLs)
      if (response.data.proxy_links && response.data.proxy_links.length > 0) {
        const videoUrl = response.data.proxy_links[0];
        console.log('Video URL from proxy_links:', videoUrl);
        return {
          videoUrl: videoUrl,
          generationTime: response.data.generationTime
        };
      }
      // Fallback to output
      else if (response.data.output && response.data.output.length > 0) {
        const videoUrl = response.data.output[0];
        console.log('Video URL from output:', videoUrl);
        return {
          videoUrl: videoUrl,
          generationTime: response.data.generationTime
        };
      }
      // Fallback to links
      else if (response.data.links && response.data.links.length > 0) {
        const videoUrl = response.data.links[0];
        console.log('Video URL from links:', videoUrl);
        return {
          videoUrl: videoUrl,
          generationTime: response.data.generationTime
        };
      } else {
        throw new Error('No video URL in response');
      }
    } else if (response.data.status === 'processing') {
      // Video is processing, need to poll
      if (response.data.id) {
        console.log('Video is processing, request ID:', response.data.id);
        return {
          requestId: response.data.id
        };
      }
      throw new Error('Video is processing but no request ID provided');
    } else {
      const errorMsg = response.data.error || response.data.message || 'Unknown error';
      console.error('API returned error:', errorMsg, response.data);
      throw new Error(errorMsg);
    }
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

// Poll for video generation results
export async function pollForVideo(
  requestId: string,
  maxAttempts: number = 40
): Promise<{ videoUrl: string; generationTime?: number }> {
  console.log('Starting polling for request:', requestId);
  
  for (let attempt = 0; attempt < maxAttempts; attempt++) {
    await new Promise(resolve => setTimeout(resolve, 5000)); // Wait 5 seconds

    try {
      const response = await axios.post<VideoGenerationResponse>(
        'https://modelslab.com/api/v6/video/fetch',
        {
          key: API_CONFIG.MODELSLAB_API_KEY,
          request_id: requestId
        } as VideoPollRequest,
        {
          headers: {
            'Content-Type': 'application/json'
          }
        }
      );

      console.log(`Poll attempt ${attempt + 1}:`, response.data.status);

      if (response.data.status === 'success') {
        // Prefer proxy_links for stable CDN URLs
        if (response.data.proxy_links && response.data.proxy_links.length > 0) {
          return {
            videoUrl: response.data.proxy_links[0],
            generationTime: response.data.generationTime
          };
        }
        // Fallback to output
        else if (response.data.output && response.data.output.length > 0) {
          return {
            videoUrl: response.data.output[0],
            generationTime: response.data.generationTime
          };
        }
        // Fallback to links
        else if (response.data.links && response.data.links.length > 0) {
          return {
            videoUrl: response.data.links[0],
            generationTime: response.data.generationTime
          };
        }
      } else if (response.data.status === 'error' || response.data.status === 'failed') {
        throw new Error(response.data.error || response.data.message || 'Video generation failed');
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

  throw new Error('Video generation timed out. Please try again.');
}
