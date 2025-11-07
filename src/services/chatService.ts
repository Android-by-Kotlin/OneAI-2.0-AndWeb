import { GoogleGenerativeAI } from '@google/generative-ai';
import axios from 'axios';
import { API_CONFIG, API_ENDPOINTS } from '../config/api';

export interface Message {
  id: string;
  role: 'user' | 'assistant';
  content: string;
  timestamp: number;
  model?: string;
}

export interface ChatModel {
  id: string;
  name: string;
  provider: 'google' | 'openrouter' | 'a4f';
  description: string;
}

export const AVAILABLE_MODELS: ChatModel[] = [
  {
    id: 'models/gemini-2.0-flash',
    name: 'Gemini 2.0 Flash',
    provider: 'google',
    description: 'Fast and efficient model for general tasks'
  },
  {
    id: 'models/gemini-1.5-pro',
    name: 'Gemini 1.5 Pro',
    provider: 'google',
    description: 'Advanced model for complex reasoning'
  },
  {
    id: 'google/gemini-2.0-flash-exp:free',
    name: 'Gemini 2.0 Flash (OpenRouter)',
    provider: 'openrouter',
    description: 'Via OpenRouter - Free tier'
  },
  {
    id: 'anthropic/claude-3.5-sonnet',
    name: 'Claude 3.5 Sonnet',
    provider: 'openrouter',
    description: 'Anthropic\'s most capable model'
  },
  {
    id: 'openai/gpt-4-turbo',
    name: 'GPT-4 Turbo',
    provider: 'openrouter',
    description: 'OpenAI\'s most advanced model'
  },
  {
    id: 'meta-llama/llama-3.1-70b-instruct',
    name: 'Llama 3.1 70B',
    provider: 'openrouter',
    description: 'Meta\'s open source model'
  },
  {
    id: 'provider-5/gpt-4o-mini',
    name: 'GPT-4o Mini',
    provider: 'a4f',
    description: 'Fast and efficient GPT-4o mini via A4F'
  },
  {
    id: 'provider-5/gpt-4o',
    name: 'GPT-4o',
    provider: 'a4f',
    description: 'Latest GPT-4o model via A4F'
  },
  {
    id: 'provider-5/o1-mini',
    name: 'O1 Mini',
    provider: 'a4f',
    description: 'Reasoning-focused O1 mini via A4F'
  },
  {
    id: 'provider-6/llama-3.2-1b-instruct',
    name: 'Llama 3.2 1B',
    provider: 'a4f',
    description: 'Fast and lightweight Llama 3.2 1B via A4F'
  },
  {
    id: 'provider-5/deepseek-r1-0528-qwen3-8b',
    name: 'DeepSeek R1 Qwen3 8B',
    provider: 'a4f',
    description: 'Advanced reasoning model DeepSeek R1 via A4F'
  },
  {
    id: 'provider-1/qwen3-32b',
    name: 'Qwen3 32B',
    provider: 'a4f',
    description: 'Large Qwen3 32B model via A4F'
  },
  {
    id: 'provider-5/qwq-32b',
    name: 'QwQ 32B',
    provider: 'a4f',
    description: 'QwQ 32B reasoning model via A4F'
  }
];

// Initialize Gemini AI with API key from config
const genAI = new GoogleGenerativeAI(API_CONFIG.GEMINI_API_KEY);

/**
 * Clean response text by removing thinking tags
 */
function cleanResponseText(text: string): string {
  return text
    .replace(/<think>.*?<\/think>/gis, '')
    .replace(/<thinking>.*?<\/thinking>/gis, '')
    .trim() || 'Processing your request...';
}

/**
 * Send message to Gemini model
 */
async function sendToGemini(
  message: string,
  modelId: string,
  conversationHistory: Message[] = []
): Promise<string> {
  try {
    const model = genAI.getGenerativeModel({ model: modelId });
    
    // Build conversation history for context
    const history = conversationHistory.map(msg => ({
      role: msg.role === 'user' ? 'user' : 'model',
      parts: [{ text: msg.content }]
    }));

    // Start chat with history
    const chat = model.startChat({
      history: history.length > 0 ? history : undefined,
    });

    const result = await chat.sendMessage(message);
    const response = await result.response;
    const text = response.text();
    
    return cleanResponseText(text);
  } catch (error: any) {
    console.error('Gemini API error:', error);
    
    if (error.message?.includes('API key')) {
      throw new Error('Invalid API key. Please check your Gemini API key configuration.');
    }
    if (error.message?.includes('quota')) {
      throw new Error('API quota exceeded. Please try again later or use a different model.');
    }
    if (error.message?.includes('blocked')) {
      throw new Error('Content was blocked by safety filters. Please rephrase your message.');
    }
    
    throw new Error(`Gemini error: ${error.message || 'Unknown error occurred'}`);
  }
}

/**
 * Send message to OpenRouter (for Claude, GPT-4, Llama, etc.)
 */
async function sendToOpenRouter(
  message: string,
  modelId: string,
  conversationHistory: Message[] = []
): Promise<string> {
  try {
    const apiKey = API_CONFIG.OPENROUTER_API_KEY;
    
    if (!apiKey || apiKey === '') {
      throw new Error('OpenRouter API key not configured');
    }

    // Build messages array with history
    const messages = [
      ...conversationHistory.map(msg => ({
        role: msg.role,
        content: msg.content
      })),
      {
        role: 'user' as const,
        content: message
      }
    ];

    const response = await axios.post(
      API_ENDPOINTS.OPENROUTER_CHAT,
      {
        model: modelId,
        messages: messages,
      },
      {
        headers: {
          'Authorization': `Bearer ${apiKey}`,
          'HTTP-Referer': window.location.origin,
          'X-Title': 'OneAI Web',
          'Content-Type': 'application/json'
        }
      }
    );

    if (response.data?.choices?.[0]?.message?.content) {
      return cleanResponseText(response.data.choices[0].message.content);
    }
    
    throw new Error('Invalid response from OpenRouter');
  } catch (error: any) {
    console.error('OpenRouter API error:', error);
    
    if (error.response?.status === 401) {
      throw new Error('Invalid OpenRouter API key. Please check your configuration.');
    }
    if (error.response?.status === 429) {
      throw new Error('Rate limit exceeded. Please try again later.');
    }
    if (error.response?.data?.error?.message) {
      throw new Error(`OpenRouter error: ${error.response.data.error.message}`);
    }
    
    throw new Error(`OpenRouter error: ${error.message || 'Unknown error occurred'}`);
  }
}

/**
 * Send message to A4F (for GPT-4o, GPT-4o-mini, O1, etc.)
 */
async function sendToA4F(
  message: string,
  modelId: string,
  conversationHistory: Message[] = []
): Promise<string> {
  try {
    const apiKey = API_CONFIG.A4F_API_KEY;
    
    if (!apiKey || apiKey === '') {
      throw new Error('A4F API key not configured');
    }

    // Build messages array with history
    const messages = [
      ...conversationHistory.map(msg => ({
        role: msg.role,
        content: msg.content
      })),
      {
        role: 'user' as const,
        content: message
      }
    ];

    const response = await axios.post(
      API_ENDPOINTS.A4F_CHAT,
      {
        model: modelId,
        messages: messages,
      },
      {
        headers: {
          'Authorization': `Bearer ${apiKey}`,
          'Content-Type': 'application/json'
        }
      }
    );

    if (response.data?.choices?.[0]?.message?.content) {
      return cleanResponseText(response.data.choices[0].message.content);
    }
    
    throw new Error('Invalid response from A4F');
  } catch (error: any) {
    console.error('A4F API error:', error);
    
    if (error.response?.status === 401) {
      throw new Error('Invalid A4F API key. Please check your configuration.');
    }
    if (error.response?.status === 429) {
      throw new Error('Rate limit exceeded. Please try again later.');
    }
    if (error.response?.data?.error?.message) {
      throw new Error(`A4F error: ${error.response.data.error.message}`);
    }
    
    throw new Error(`A4F error: ${error.message || 'Unknown error occurred'}`);
  }
}

/**
 * Main function to send message to selected model
 */
export async function sendMessage(
  message: string,
  modelId: string,
  conversationHistory: Message[] = []
): Promise<string> {
  const model = AVAILABLE_MODELS.find(m => m.id === modelId);
  
  if (!model) {
    throw new Error(`Model ${modelId} not found`);
  }

  if (model.provider === 'google') {
    return sendToGemini(message, modelId, conversationHistory);
  } else if (model.provider === 'openrouter') {
    return sendToOpenRouter(message, modelId, conversationHistory);
  } else if (model.provider === 'a4f') {
    return sendToA4F(message, modelId, conversationHistory);
  }
  
  throw new Error(`Unknown provider: ${model.provider}`);
}

/**
 * Generate a unique ID for messages
 */
export function generateMessageId(): string {
  return `msg_${Date.now()}_${Math.random().toString(36).substr(2, 9)}`;
}
