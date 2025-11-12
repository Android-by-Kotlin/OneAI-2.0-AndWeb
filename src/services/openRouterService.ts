import axios from 'axios';
import { API_CONFIG, API_ENDPOINTS } from '../config/api';

interface Message {
  role: 'user' | 'assistant' | 'system';
  content: string;
}

let conversationHistory: Message[] = [
  {
    role: 'system',
    content: 'You are a helpful AI avatar assistant. Keep your responses concise and conversational, around 1-2 sentences. Be friendly and natural.'
  }
];

// Helper function to wait
const wait = (ms: number) => new Promise(resolve => setTimeout(resolve, ms));

// Try multiple models with fallback
const MODELS_TO_TRY = [
  'google/gemini-2.0-flash-exp:free',
  'meta-llama/llama-3.2-3b-instruct:free',
  'qwen/qwen-2-7b-instruct:free',
];

export async function getOpenRouterResponse(userMessage: string): Promise<string> {
  if (!API_CONFIG.OPENROUTER_API_KEY) {
    throw new Error('OpenRouter API key is not configured');
  }

  // Add user message to history
  conversationHistory.push({
    role: 'user',
    content: userMessage
  });

  let lastError: any = null;

  // Try each model in order
  for (let i = 0; i < MODELS_TO_TRY.length; i++) {
    const model = MODELS_TO_TRY[i];
    
    try {
      console.log(`Attempting with model: ${model}`);
      
      const response = await axios.post(
        API_ENDPOINTS.OPENROUTER_CHAT,
        {
          model: model,
          messages: conversationHistory
        },
        {
          headers: {
            'Authorization': `Bearer ${API_CONFIG.OPENROUTER_API_KEY}`,
            'HTTP-Referer': window.location.origin,
            'X-Title': 'OneAI Live Avatar',
            'Content-Type': 'application/json'
          },
          timeout: 30000 // 30 second timeout
        }
      );

      const aiResponse = response.data.choices[0].message.content;

      // Add AI response to history
      conversationHistory.push({
        role: 'assistant',
        content: aiResponse
      });

      // Keep only last 10 messages to avoid token limits
      if (conversationHistory.length > 11) {
        conversationHistory = [
          conversationHistory[0], // Keep system message
          ...conversationHistory.slice(-10)
        ];
      }

      console.log(`Success with model: ${model}`);
      return aiResponse;
      
    } catch (error: any) {
      lastError = error;
      console.error(`Error with model ${model}:`, error.response?.data || error.message);
      
      // If rate limited and not the last model, wait and try next
      if (error.response?.status === 429 && i < MODELS_TO_TRY.length - 1) {
        console.log('Rate limited, trying next model...');
        await wait(1000); // Wait 1 second before trying next model
        continue;
      }
      
      // If it's the last model or a different error, handle it
      if (i === MODELS_TO_TRY.length - 1) {
        // Remove the user message we just added since we failed
        conversationHistory.pop();
        
        if (error.response?.status === 401) {
          throw new Error('Invalid OpenRouter API key');
        } else if (error.response?.status === 429) {
          throw new Error('All models are rate limited. Please wait a moment and try again.');
        } else if (error.code === 'ECONNABORTED') {
          throw new Error('Request timeout. Please try again.');
        } else {
          throw new Error(`Failed to get AI response: ${error.message}`);
        }
      }
    }
  }

  // Should never reach here, but just in case
  conversationHistory.pop();
  throw new Error('Failed to get response from any AI model');
}

export function resetOpenRouterConversation() {
  conversationHistory = [
    {
      role: 'system',
      content: 'You are a helpful AI avatar assistant. Keep your responses concise and conversational, around 1-2 sentences. Be friendly and natural.'
    }
  ];
}
