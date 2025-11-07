import { initializeApp } from 'firebase/app';
import { getAuth, GoogleAuthProvider } from 'firebase/auth';
import { getFirestore } from 'firebase/firestore';
import { getStorage } from 'firebase/storage';

const firebaseConfig = {
  apiKey: "AIzaSyCY4X9YsdJ1rAcALTW_biRti7Yd3hBNneU",
  authDomain: "oneai-747a7.firebaseapp.com",
  projectId: "oneai-747a7",
  storageBucket: "oneai-747a7.firebasestorage.app",
  messagingSenderId: "827202302425",
  appId: "1:827202302425:android:5903f76527a3320b92e9c8"
};

// Initialize Firebase
const app = initializeApp(firebaseConfig);

// Initialize services
export const auth = getAuth(app);
export const db = getFirestore(app);
export const storage = getStorage(app);
export const googleProvider = new GoogleAuthProvider();

export default app;
