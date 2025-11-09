import { 
  collection, 
  addDoc, 
  updateDoc, 
  deleteDoc,
  doc, 
  query, 
  where, 
  orderBy, 
  getDocs,
  Timestamp,
  serverTimestamp
} from 'firebase/firestore';
import { db } from '../config/firebase';
import { Message } from './chatService';

export interface ChatSession {
  id: string;
  userId: string;
  title: string;
  model: string;
  messages: Message[];
  createdAt: Date;
  updatedAt: Date;
}

const CHATS_COLLECTION = 'chatSessions';

/**
 * Create a new chat session
 */
export async function createChatSession(
  userId: string,
  title: string,
  model: string
): Promise<string> {
  try {
    const chatData = {
      userId,
      title,
      model,
      messages: [],
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    };

    const docRef = await addDoc(collection(db, CHATS_COLLECTION), chatData);
    return docRef.id;
  } catch (error) {
    console.error('Error creating chat session:', error);
    throw new Error('Failed to create chat session');
  }
}

/**
 * Update chat session with new messages
 */
export async function updateChatSession(
  sessionId: string,
  messages: Message[],
  title?: string
): Promise<void> {
  try {
    const chatRef = doc(db, CHATS_COLLECTION, sessionId);
    const updateData: any = {
      messages,
      updatedAt: serverTimestamp()
    };

    if (title) {
      updateData.title = title;
    }

    await updateDoc(chatRef, updateData);
  } catch (error) {
    console.error('Error updating chat session:', error);
    throw new Error('Failed to update chat session');
  }
}

/**
 * Get all chat sessions for a user
 */
export async function getUserChatSessions(userId: string): Promise<ChatSession[]> {
  try {
    const q = query(
      collection(db, CHATS_COLLECTION),
      where('userId', '==', userId),
      orderBy('updatedAt', 'desc')
    );

    const querySnapshot = await getDocs(q);
    const sessions: ChatSession[] = [];

    querySnapshot.forEach((doc) => {
      const data = doc.data();
      sessions.push({
        id: doc.id,
        userId: data.userId,
        title: data.title,
        model: data.model,
        messages: data.messages || [],
        createdAt: data.createdAt?.toDate() || new Date(),
        updatedAt: data.updatedAt?.toDate() || new Date()
      });
    });

    return sessions;
  } catch (error) {
    console.error('Error getting chat sessions:', error);
    throw new Error('Failed to load chat history');
  }
}

/**
 * Delete a chat session
 */
export async function deleteChatSession(sessionId: string): Promise<void> {
  try {
    await deleteDoc(doc(db, CHATS_COLLECTION, sessionId));
  } catch (error) {
    console.error('Error deleting chat session:', error);
    throw new Error('Failed to delete chat session');
  }
}

/**
 * Generate a title from the first message
 */
export function generateChatTitle(firstMessage: string): string {
  const maxLength = 50;
  if (firstMessage.length <= maxLength) {
    return firstMessage;
  }
  return firstMessage.substring(0, maxLength) + '...';
}
