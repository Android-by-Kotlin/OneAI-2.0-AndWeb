import axios from 'axios';
import { API_CONFIG, API_ENDPOINTS } from '../config/api';

interface StreamingSession {
  session_id: string;
  sdp: {
    type: string;
    sdp: string;
  };
  ice_servers: RTCIceServer[];
}

interface StreamingSessionResponse {
  data: StreamingSession;
  code: number;
  message: string;
}

interface TaskResponse {
  data: {
    task_id: string;
    session_id: string;
  };
  code: number;
  message: string;
}

// Create a new streaming session with HeyGen
export async function createStreamingSession(
  avatarId: string = 'default',
  quality: string = 'high'
): Promise<StreamingSession> {
  try {
    console.log('Creating HeyGen streaming session...');
    
    if (!API_CONFIG.HEYGEN_API_KEY) {
      throw new Error('HeyGen API key is not configured. Please add VITE_HEYGEN_API_KEY to your environment variables.');
    }

    const response = await axios.post<StreamingSessionResponse>(
      `${API_ENDPOINTS.HEYGEN_STREAMING}/new`,
      {
        quality: quality,
        avatar_name: avatarId,
        voice: {
          voice_id: 'en-US-Neural2-F',
        },
      },
      {
        headers: {
          'X-Api-Key': API_CONFIG.HEYGEN_API_KEY,
          'Content-Type': 'application/json',
        },
        timeout: 30000,
      }
    );

    console.log('Streaming session created:', response.data);
    return response.data.data;
  } catch (error: any) {
    console.error('Failed to create streaming session:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status,
    });

    if (error.response?.status === 401 || error.response?.status === 403) {
      throw new Error('Invalid HeyGen API key. Please check your configuration.');
    }
    if (error.response?.status === 429) {
      throw new Error('Rate limit exceeded. Please try again later.');
    }
    if (error.message.includes('API key is not configured')) {
      throw error;
    }

    throw new Error('Failed to create streaming session. Please try again.');
  }
}

// Send a task to the avatar (make it speak)
export async function sendAvatarTask(
  sessionId: string,
  text: string,
  taskType: 'talk' | 'repeat' = 'talk'
): Promise<string> {
  try {
    console.log('Sending task to avatar...');

    if (!API_CONFIG.HEYGEN_API_KEY) {
      throw new Error('HeyGen API key is not configured.');
    }

    const response = await axios.post<TaskResponse>(
      `${API_ENDPOINTS.HEYGEN_STREAMING}/task`,
      {
        session_id: sessionId,
        text: text,
        task_type: taskType,
      },
      {
        headers: {
          'X-Api-Key': API_CONFIG.HEYGEN_API_KEY,
          'Content-Type': 'application/json',
        },
        timeout: 30000,
      }
    );

    console.log('Task sent successfully:', response.data);
    return response.data.data.task_id;
  } catch (error: any) {
    console.error('Failed to send avatar task:', {
      message: error.message,
      response: error.response?.data,
      status: error.response?.status,
    });

    throw new Error('Failed to send message to avatar. Please try again.');
  }
}

// Stop the streaming session
export async function stopStreamingSession(sessionId: string): Promise<void> {
  try {
    console.log('Stopping streaming session...');

    if (!API_CONFIG.HEYGEN_API_KEY) {
      return; // Silently return if no API key
    }

    await axios.post(
      `${API_ENDPOINTS.HEYGEN_STREAMING}/stop`,
      {
        session_id: sessionId,
      },
      {
        headers: {
          'X-Api-Key': API_CONFIG.HEYGEN_API_KEY,
          'Content-Type': 'application/json',
        },
        timeout: 10000,
      }
    );

    console.log('Streaming session stopped successfully');
  } catch (error: any) {
    console.error('Failed to stop streaming session:', error.message);
    // Don't throw error on stop failure
  }
}

// WebRTC Connection Manager
export class WebRTCConnection {
  private peerConnection: RTCPeerConnection | null = null;
  private onTrackCallback: ((stream: MediaStream) => void) | null = null;
  private onConnectionStateChange: ((state: RTCPeerConnectionState) => void) | null = null;

  constructor() {
    this.peerConnection = null;
  }

  async initialize(
    sdp: { type: string; sdp: string },
    iceServers: RTCIceServer[]
  ): Promise<string> {
    try {
      // Create peer connection
      this.peerConnection = new RTCPeerConnection({
        iceServers: iceServers,
      });

      // Handle incoming tracks (video/audio from avatar)
      this.peerConnection.ontrack = (event) => {
        console.log('Received track:', event.track.kind);
        if (this.onTrackCallback && event.streams[0]) {
          this.onTrackCallback(event.streams[0]);
        }
      };

      // Handle connection state changes
      this.peerConnection.onconnectionstatechange = () => {
        const state = this.peerConnection?.connectionState;
        console.log('Connection state:', state);
        if (this.onConnectionStateChange && state) {
          this.onConnectionStateChange(state);
        }
      };

      // Handle ICE candidates
      this.peerConnection.onicecandidate = (event) => {
        if (event.candidate) {
          console.log('New ICE candidate:', event.candidate);
        }
      };

      // Set remote description (offer from HeyGen)
      await this.peerConnection.setRemoteDescription(
        new RTCSessionDescription({
          type: sdp.type as RTCSdpType,
          sdp: sdp.sdp,
        })
      );

      // Create answer
      const answer = await this.peerConnection.createAnswer();
      await this.peerConnection.setLocalDescription(answer);

      console.log('WebRTC connection initialized');
      return answer.sdp || '';
    } catch (error) {
      console.error('Failed to initialize WebRTC connection:', error);
      throw new Error('Failed to establish video connection');
    }
  }

  onTrack(callback: (stream: MediaStream) => void) {
    this.onTrackCallback = callback;
  }

  onStateChange(callback: (state: RTCPeerConnectionState) => void) {
    this.onConnectionStateChange = callback;
  }

  close() {
    if (this.peerConnection) {
      this.peerConnection.close();
      this.peerConnection = null;
      console.log('WebRTC connection closed');
    }
  }

  getConnectionState(): RTCPeerConnectionState | null {
    return this.peerConnection?.connectionState || null;
  }
}
