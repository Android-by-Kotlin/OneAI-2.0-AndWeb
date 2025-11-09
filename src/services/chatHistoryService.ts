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
  serverTimestamp,
  setDoc
} from 'firebase/firestore';
import { db } from '../config/firebase';
import { type Message } from './chatService';

// Re-export Message type for convenience
export type { Message } from './chatService';

export interface ChatSession {
  id: string;
  userId: string;
  title: string;
  model: string;
  messages: Message[];
  createdAt: Date;
  updatedAt: Date;
}

// Match Android app structure: users/{userId}/chats/{chatId}
const USERS_COLLECTION = 'users';
const CHATS_COLLECTION = 'chats';
const MESSAGES_COLLECTION = 'messages';

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
      title,
      model,
      createdAt: serverTimestamp(),
      updatedAt: serverTimestamp()
    };

    // Match Android structure: users/{userId}/chats/{chatId}
    const chatRef = doc(collection(db, USERS_COLLECTION, userId, CHATS_COLLECTION));
    await setDoc(chatRef, chatData);
    return chatRef.id;
  } catch (error) {
    console.error('Error creating chat session:', error);
    throw new Error('Failed to create chat session');
  }
}

/**
 * Update chat session with new messages
 */
export async function updateChatSession(
  userId: string,
  sessionId: string,
  messages: Message[],
  title?: string
): Promise<void> {
  try {
    // Match Android structure: users/{userId}/chats/{chatId}
    const chatRef = doc(db, USERS_COLLECTION, userId, CHATS_COLLECTION, sessionId);
    const updateData: any = {
      updatedAt: serverTimestamp()
    };

    if (title) {
      updateData.title = title;
    }

    await updateDoc(chatRef, updateData);
    
    // Update messages subcollection
    // Delete old messages
    const messagesRef = collection(db, USERS_COLLECTION, userId, CHATS_COLLECTION, sessionId, MESSAGES_COLLECTION);
    const oldMessages = await getDocs(messagesRef);
    for (const msgDoc of oldMessages.docs) {
      await deleteDoc(msgDoc.ref);
    }
    
    // Add new messages
    for (const message of messages) {
      await addDoc(messagesRef, {
        text: message.content,
        role: message.role,
        timestamp: Timestamp.fromMillis(message.timestamp),
        image: message.image || null
      });
    }
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
    // Match Android structure: users/{userId}/chats
    const q = query(
      collection(db, USERS_COLLECTION, userId, CHATS_COLLECTION),
      orderBy('updatedAt', 'desc')
    );

    const querySnapshot = await getDocs(q);
    const sessions: ChatSession[] = [];

    // Load each chat with its messages from subcollection
    for (const chatDoc of querySnapshot.docs) {
      const data = chatDoc.data();
      
      // Load messages from subcollection
      const messagesRef = collection(db, USERS_COLLECTION, userId, CHATS_COLLECTION, chatDoc.id, MESSAGES_COLLECTION);
      const messagesQuery = query(messagesRef, orderBy('timestamp', 'asc'));
      const messagesSnapshot = await getDocs(messagesQuery);
      
      const messages: Message[] = messagesSnapshot.docs.map(msgDoc => {
        const msgData = msgDoc.data();
        return {
          id: msgDoc.id,
          role: msgData.role,
          content: msgData.text,
          timestamp: msgData.timestamp?.toMillis() || Date.now(),
          model: data.model || '',
          image: msgData.image || undefined
        };
      });
      
      sessions.push({
        id: chatDoc.id,
        userId: userId,
        title: data.title,
        model: data.model || '',
        messages: messages,
        createdAt: data.createdAt?.toDate() || new Date(),
        updatedAt: data.updatedAt?.toDate() || new Date()
      });
    }

    return sessions;
  } catch (error) {
    console.error('Error getting chat sessions:', error);
    throw new Error('Failed to load chat history');
  }
}

/**
 * Delete a chat session
 */
export async function deleteChatSession(userId: string, sessionId: string): Promise<void> {
  try {
    // Match Android structure: users/{userId}/chats/{chatId}
    // First delete all messages
    const messagesRef = collection(db, USERS_COLLECTION, userId, CHATS_COLLECTION, sessionId, MESSAGES_COLLECTION);
    const messagesSnapshot = await getDocs(messagesRef);
    for (const msgDoc of messagesSnapshot.docs) {
      await deleteDoc(msgDoc.ref);
    }
    
    // Then delete the chat document
    await deleteDoc(doc(db, USERS_COLLECTION, userId, CHATS_COLLECTION, sessionId));
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
