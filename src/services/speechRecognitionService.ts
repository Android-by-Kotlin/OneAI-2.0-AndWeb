// Speech Recognition Service using Web Speech API
export class SpeechRecognitionService {
  private recognition: any;
  private isListening: boolean = false;
  private onResultCallback: ((text: string) => void) | null = null;
  private onErrorCallback: ((error: string) => void) | null = null;
  private onEndCallback: (() => void) | null = null;

  constructor() {
    // Check if browser supports speech recognition
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    
    if (!SpeechRecognition) {
      console.error('Speech recognition not supported in this browser');
      return;
    }

    this.recognition = new SpeechRecognition();
    this.recognition.continuous = false; // Stop after one result
    this.recognition.interimResults = false; // Only final results
    this.recognition.lang = 'en-US';
    this.recognition.maxAlternatives = 1;

    // Set up event listeners
    this.recognition.onresult = (event: any) => {
      const transcript = event.results[0][0].transcript;
      console.log('Speech recognized:', transcript);
      if (this.onResultCallback) {
        this.onResultCallback(transcript);
      }
    };

    this.recognition.onerror = (event: any) => {
      console.error('Speech recognition error:', event.error);
      this.isListening = false;
      if (this.onErrorCallback) {
        this.onErrorCallback(event.error);
      }
    };

    this.recognition.onend = () => {
      console.log('Speech recognition ended');
      this.isListening = false;
      if (this.onEndCallback) {
        this.onEndCallback();
      }
    };
  }

  // Check if speech recognition is supported
  isSupported(): boolean {
    return !!this.recognition;
  }

  // Start listening
  start(
    onResult: (text: string) => void,
    onError?: (error: string) => void,
    onEnd?: () => void
  ) {
    if (!this.recognition) {
      if (onError) onError('Speech recognition not supported');
      return;
    }

    if (this.isListening) {
      console.log('Already listening');
      return;
    }

    this.onResultCallback = onResult;
    this.onErrorCallback = onError || null;
    this.onEndCallback = onEnd || null;

    try {
      this.recognition.start();
      this.isListening = true;
      console.log('Speech recognition started');
    } catch (error) {
      console.error('Failed to start speech recognition:', error);
      if (onError) onError('Failed to start listening');
    }
  }

  // Stop listening
  stop() {
    if (this.recognition && this.isListening) {
      this.recognition.stop();
      this.isListening = false;
    }
  }

  // Check if currently listening
  getIsListening(): boolean {
    return this.isListening;
  }

  // Cleanup
  destroy() {
    if (this.recognition) {
      this.stop();
      this.onResultCallback = null;
      this.onErrorCallback = null;
      this.onEndCallback = null;
    }
  }
}
