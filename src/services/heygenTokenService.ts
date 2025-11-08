import axios from 'axios';
import { API_CONFIG } from '../config/api';

// Generate HeyGen access token
export async function getHeyGenAccessToken(): Promise<string> {
  try {
    console.log('Fetching HeyGen access token...');

    if (!API_CONFIG.HEYGEN_API_KEY) {
      throw new Error('HeyGen API key is not configured');
    }

    const endpoint = import.meta.env.DEV 
      ? '/api/heygen/v1/streaming.create_token'
      : 'https://api.heygen.com/v1/streaming.create_token';

    const response = await axios.post(
      endpoint,
      {},
      {
        headers: {
          'x-api-key': API_CONFIG.HEYGEN_API_KEY,
        },
        timeout: 30000,
      }
    );

    console.log('Access token generated successfully');
    return response.data.data.token;
  } catch (error: any) {
    console.error('Failed to get access token:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status,
    });

    if (error.response?.status === 401 || error.response?.status === 403) {
      throw new Error('Invalid HeyGen API key');
    }

    throw new Error('Failed to generate access token');
  }
}
