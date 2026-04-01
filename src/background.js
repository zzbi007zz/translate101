// Background service worker - Manifest V3
import { translateWithGemini } from './providers/gemini-api-client';
import { getApiKey } from './utils/chrome-storage-helper';
import { retryWithBackoff } from './utils/retry-helper';

// Service worker lifecycle events
chrome.runtime.onInstalled.addListener(() => {
  console.log('Instant Translate extension installed');
});

self.addEventListener('activate', () => {
  console.log('Service worker activated');
});

// Message handler for translation requests
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  if (request.action === 'translate') {
    handleTranslation(request, sendResponse);
    return true; // Keep channel open for async response
  }
  return false;
});

async function handleTranslation(request, sendResponse) {
  const startTime = Date.now();

  try {
    // Validate request
    if (!request.text || !request.targetLang) {
      sendResponse({
        success: false,
        error: 'Missing text or targetLang',
        code: 'INVALID_REQUEST',
      });
      return;
    }

    // Get API key from storage
    const apiKey = await getApiKey('gemini');
    if (!apiKey) {
      sendResponse({
        success: false,
        error: 'No API key configured. Open extension settings.',
        code: 'NO_API_KEY',
      });
      return;
    }

    // Translate with retry logic
    const translation = await retryWithBackoff(
      () => translateWithGemini(request.text, request.targetLang, apiKey),
    );

    // Success response
    sendResponse({
      success: true,
      translation,
      provider: 'gemini',
      latency: Date.now() - startTime,
    });
  } catch (error) {
    // Error handling with user-friendly messages
    const errorResponse = mapErrorToResponse(error);
    errorResponse.latency = Date.now() - startTime;
    sendResponse(errorResponse);
  }
}

function mapErrorToResponse(error) {
  const message = error.message || '';

  if (message.includes('401') || message.includes('403')) {
    return {
      success: false,
      error: 'Invalid API key. Check settings.',
      code: 'AUTH_FAILED',
    };
  }

  if (message.includes('429')) {
    return {
      success: false,
      error: 'Rate limit exceeded. Try again later.',
      code: 'RATE_LIMIT',
    };
  }

  if (error.name === 'AbortError') {
    return {
      success: false,
      error: 'Request timeout. Check your internet connection.',
      code: 'TIMEOUT',
    };
  }

  if (message.includes('NetworkError') || message.includes('Failed to fetch')) {
    return {
      success: false,
      error: 'Network error. Check your internet connection.',
      code: 'NETWORK_ERROR',
    };
  }

  return {
    success: false,
    error: `Translation failed: ${message}`,
    code: 'UNKNOWN',
  };
}
