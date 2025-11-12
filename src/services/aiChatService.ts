import { GoogleGenerativeAI } from '@google/generative-ai';
import { API_CONFIG } from '../config/api';

let chatSession: any = null;
let genAI: GoogleGenerativeAI | null = null;

// Initialize Gemini AI
export function initializeAI() {
  try {
    if (!API_CONFIG.GEMINI_API_KEY) {
      throw new Error('Gemini API key is not configured');
    }

    genAI = new GoogleGenerativeAI(API_CONFIG.GEMINI_API_KEY);
    
    // Try gemini-2.0-flash-exp first, fallback to gemini-1.5-flash if it fails
    const model = genAI.getGenerativeModel({ 
      model: 'gemini-1.5-flash',
      systemInstruction: 'You are a helpful AI avatar assistant. Keep your responses concise and conversational, around 1-2 sentences. Be friendly and natural.'
    });
    
    chatSession = model.startChat({
      history: [],
    });
    
    console.log('AI initialized successfully');
    return chatSession;
  } catch (error: any) {
    console.error('Failed to initialize AI:', error);
    throw new Error(`Failed to initialize AI: ${error.message}`);
  }
}

// Get AI response for a message
export async function getAIResponse(message: string): Promise<string> {
  try {
    if (!API_CONFIG.GEMINI_API_KEY) {
      throw new Error('Gemini API key is not configured. Please add VITE_GEMINI_API_KEY to your .env file.');
    }

    if (!chatSession) {
      console.log('Chat session not initialized, initializing now...');
      initializeAI();
    }

    if (!chatSession) {
      throw new Error('Failed to initialize chat session');
    }

    console.log('Sending message to AI:', message);
    const result = await chatSession.sendMessage(message);
    const response = result.response;
    const text = response.text();
    console.log('AI response received:', text);
    return text;
  } catch (error: any) {
    console.error('AI response error:', error);
    
    // Provide more specific error messages
    if (error.message?.includes('API_KEY_INVALID')) {
      throw new Error('Invalid Gemini API key. Please check your API key in .env file.');
    } else if (error.message?.includes('RATE_LIMIT')) {
      throw new Error('Rate limit exceeded. Please try again later.');
    } else if (error.message?.includes('quota')) {
      throw new Error('API quota exceeded. Please check your Gemini API quota.');
    } else {
      throw new Error(`Failed to get AI response: ${error.message || 'Unknown error'}`);
    }
  }
}

// Reset conversation
export function resetConversation() {
  chatSession = null;
}
