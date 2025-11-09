# Firestore Setup for Chat History

## Issue
If you see "Failed to create chat session" error, it means Firestore security rules need to be configured.

## Solution

### Option 1: Update Firestore Rules (Recommended for Development)

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **oneai-747a7**
3. Go to **Firestore Database** → **Rules**
4. Replace the rules with:

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Allow authenticated users to read/write their own chat sessions
    match /chatSessions/{sessionId} {
      allow read, write: if request.auth != null && 
                           (resource == null || resource.data.userId == request.auth.uid);
    }
    
    // Allow users to query their own sessions
    match /chatSessions/{sessionId} {
      allow list: if request.auth != null &&
                     request.query.limit <= 100;
    }
  }
}
```

5. Click **Publish**

### Option 2: Test Mode (Quick but Insecure - Use only for development)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## Important Notes

- Chat functionality works even if Firebase fails
- Chat history is optional - messages work locally
- Make sure you're signed in with Google Auth
- The app will automatically retry on next message

## Verify Setup

1. Sign in to the app
2. Send a message
3. Check browser console for any Firebase errors
4. Click the history icon to see if chats are saved
