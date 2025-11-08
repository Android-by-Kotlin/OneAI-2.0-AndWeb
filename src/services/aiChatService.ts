import { GoogleGenerativeAI } from '@google/generative-ai';
import { API_CONFIG } from '../config/api';

let chatSession: any = null;

// Initialize Gemini AI
export function initializeAI() {
  try {
    const genAI = new GoogleGenerativeAI(API_CONFIG.GEMINI_API_KEY);
    const model = genAI.getGenerativeModel({ 
      model: 'gemini-2.0-flash-exp',
      systemInstruction: 'You are a helpful AI avatar assistant. Keep your responses concise and conversational, around 1-2 sentences. Be friendly and natural.'
    });
    
    chatSession = model.startChat({
      history: [],
    });
    
    return chatSession;
  } catch (error) {
    console.error('Failed to initialize AI:', error);
    throw error;
  }
}

// Get AI response for a message
export async function getAIResponse(message: string): Promise<string> {
  try {
    if (!chatSession) {
      initializeAI();
    }

    const result = await chatSession.sendMessage(message);
    const response = result.response;
    return response.text();
  } catch (error: any) {
    console.error('AI response error:', error);
    throw new Error('Failed to get AI response');
  }
}

// Reset conversation
export function resetConversation() {
  chatSession = null;
}
