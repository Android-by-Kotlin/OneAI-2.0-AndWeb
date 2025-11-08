# Security Setup

## API Keys Required

This application requires several API keys to function. For security reasons, these keys are NOT included in the repository.

### Setup Instructions

1. **Copy the example environment file:**
   ```bash
   cp .env.example .env
   ```

2. **Get a new Gemini API Key:**
   - Visit: https://aistudio.google.com/app/apikey
   - Create a new API key
   - Add it to your `.env` file as `VITE_GEMINI_API_KEY`

3. **Get other API keys as needed:**
   - OpenRouter: https://openrouter.ai/keys
   - HeyGen: https://www.heygen.com/
   - ModelsLab: https://modelslab.com/
   - And others as listed in `.env.example`

4. **Never commit the `.env` file:**
   - The `.env` file is already in `.gitignore`
   - Only commit `.env.example` with placeholder values

## Important Security Notes

⚠️ **NEVER** hardcode API keys in source code
⚠️ **NEVER** commit `.env` files to version control
⚠️ If an API key is leaked, revoke it immediately and generate a new one
