package max.ohm.oneai.chatbot

import max.ohm.oneai.BuildConfig

// Gemini API Key is now securely stored in local.properties
// The key is read from BuildConfig at compile time and never exposed in the repository
internal val GEMINI_API_KEY: String = BuildConfig.GEMINI_API_KEY
