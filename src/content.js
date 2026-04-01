// Content script - Injected into web pages
import DOMPurify from 'dompurify';
import {
  getSelectionText,
  getSelectionCoordinates,
} from './utils/dom-selection-helpers';

let currentSelection = null;
let currentOverlay = null;

// Initialize content script
function init() {
  registerHotkey();
  registerContextMenuListener();
  console.log('Instant Translate content script loaded');
}

// Register listener for context menu messages
function registerContextMenuListener() {
  chrome.runtime.onMessage.addListener((message) => {
    if (message.action === 'translateFromContextMenu') {
      // Get current selection coordinates
      const coords = getSelectionCoordinates();
      if (coords) {
        requestTranslation(message.text, coords);
      } else {
        // Fallback: use middle of screen if no selection coordinates
        const fallbackCoords = {
          x: window.innerWidth / 2 - 200,
          y: window.innerHeight / 2,
          height: 0,
        };
        requestTranslation(message.text, fallbackCoords);
      }
    }
  });
}

// Register Ctrl+Shift+E hotkey listener
function registerHotkey() {
  document.addEventListener('keydown', handleHotkey);
}

// Handle hotkey press
function handleHotkey(event) {
  // Check for Ctrl+Shift+E (or Cmd+Shift+E on Mac)
  const modifier = event.metaKey || event.ctrlKey;
  if (modifier && event.shiftKey && event.key.toUpperCase() === 'E') {
    event.preventDefault();
    onTranslateHotkey();
  }
}

// Handle translation hotkey trigger
function onTranslateHotkey() {
  const selection = captureSelection();
  if (!selection) {
    console.log('No text selected');
    return;
  }

  console.log('Translation requested:', selection.text.substring(0, 50));
  requestTranslation(selection.text, selection.coords);
}

// Capture current text selection and coordinates
function captureSelection() {
  const text = getSelectionText();
  if (!text || text.length === 0) return null;

  const coords = getSelectionCoordinates();
  if (!coords) return null;

  currentSelection = {
    text,
    coords,
    timestamp: Date.now(),
  };

  return currentSelection;
}

// Request translation from background worker
async function requestTranslation(text, coords) {
  // Validate text length (max 5000 chars for MVP)
  if (text.length > 5000) {
    showError('Text too long (max 5000 characters)');
    return;
  }

  console.log('Requesting translation...');

  // Get target language (hardcoded to English for MVP)
  const targetLang = await getTargetLanguage();

  // Send message to background service worker
  chrome.runtime.sendMessage(
    {
      action: 'translate',
      text,
      targetLang,
    },
    (response) => {
      if (chrome.runtime.lastError) {
        showError(`Extension error: ${chrome.runtime.lastError.message}`);
        return;
      }
      handleTranslationResponse(response, coords);
    },
  );
}

// Get target language from settings
async function getTargetLanguage() {
  try {
    const result = await chrome.storage.sync.get('targetLanguage');
    return result.targetLanguage || 'en';
  } catch (error) {
    console.error('Failed to get target language:', error);
    return 'en'; // Default to English
  }
}

// Handle translation response from background worker
function handleTranslationResponse(response, coords) {
  if (!response) {
    showErrorToast('No response from background worker');
    return;
  }

  if (response.success) {
    console.log('Translation received:', response.translation);
    console.log('Latency:', response.latency, 'ms');
    showOverlay(response.translation, coords);
  } else {
    showErrorToast(response.error || 'Translation failed');
  }
}

// === OVERLAY UI FUNCTIONS ===

// Show translation overlay near selected text
function showOverlay(translation, coords) {
  // Sanitize translation text to prevent XSS
  const sanitizedText = DOMPurify.sanitize(translation, {
    ALLOWED_TAGS: [], // Strip all HTML, text only
  });

  createHighlightOverlay(sanitizedText, coords);
}

// Create highlight overlay with Shadow DOM
function createHighlightOverlay(translation, coords) {
  // Destroy existing overlay
  destroyOverlay();

  // Create Shadow DOM container
  const host = createShadowHost();

  // Render overlay inside shadow root
  const overlay = renderOverlay(translation);
  host.shadowRoot.appendChild(overlay);

  // Position overlay
  positionOverlay(host, coords);

  // Register dismiss handlers
  registerDismissHandlers(host);

  // Add to page
  document.body.appendChild(host);
  currentOverlay = host;
}

// Create Shadow DOM host element
function createShadowHost() {
  const host = document.createElement('div');
  host.id = 'instant-translate-overlay-host';
  host.attachShadow({ mode: 'open' });

  // Add styles to shadow root
  const style = document.createElement('style');
  style.textContent = getOverlayStyles();
  host.shadowRoot.appendChild(style);

  return host;
}

// Get overlay CSS styles
function getOverlayStyles() {
  return `
    .overlay-container {
      position: fixed;
      background: #fef08a;
      color: #1f2937;
      padding: 12px 36px 12px 16px;
      border-radius: 8px;
      box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1), 0 2px 4px rgba(0, 0, 0, 0.06);
      font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
      font-size: 14px;
      line-height: 1.5;
      max-width: 400px;
      z-index: 2147483647;
      opacity: 0;
      transform: translateY(-4px);
      transition: opacity 0.2s, transform 0.2s;
      pointer-events: auto;
    }

    .overlay-container.visible {
      opacity: 1;
      transform: translateY(0);
    }

    .overlay-translation {
      word-wrap: break-word;
    }

    .overlay-close {
      position: absolute;
      top: 8px;
      right: 8px;
      background: transparent;
      border: none;
      font-size: 20px;
      color: #6b7280;
      cursor: pointer;
      padding: 0;
      width: 20px;
      height: 20px;
      line-height: 1;
    }

    .overlay-close:hover {
      color: #1f2937;
    }
  `;
}

// Render overlay HTML structure
function renderOverlay(translation) {
  const container = document.createElement('div');
  container.className = 'overlay-container';

  const textDiv = document.createElement('div');
  textDiv.className = 'overlay-translation';
  textDiv.textContent = translation;

  const closeBtn = document.createElement('button');
  closeBtn.className = 'overlay-close';
  closeBtn.textContent = '×';
  closeBtn.title = 'Close (ESC)';
  closeBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    destroyOverlay();
  });

  container.appendChild(textDiv);
  container.appendChild(closeBtn);

  return container;
}

// Position overlay near selection coordinates
function positionOverlay(host, coords) {
  const container = host.shadowRoot.querySelector('.overlay-container');

  // Default position: above selection
  let top = coords.y - 10; // 10px gap
  let left = coords.x;

  // Wait for overlay to render to get dimensions
  requestAnimationFrame(() => {
    const rect = container.getBoundingClientRect();

    // Check if overlay fits above selection
    if (top - rect.height < 0) {
      // Position below selection
      top = coords.y + coords.height + 10;
    } else {
      // Position above
      top = top - rect.height;
    }

    // Check horizontal bounds
    if (left + rect.width > window.innerWidth) {
      left -= rect.width - window.innerWidth + 10;
    }

    // Ensure left position is not negative
    if (left < 10) {
      left = 10;
    }

    container.style.top = `${top}px`;
    container.style.left = `${left}px`;

    // Make visible after positioning
    setTimeout(() => container.classList.add('visible'), 10);
  });
}

// Register handlers to dismiss overlay
function registerDismissHandlers(host) {
  // ESC key handler
  const escHandler = (e) => {
    if (e.key === 'Escape') {
      destroyOverlay();
      document.removeEventListener('keydown', escHandler);
    }
  };
  document.addEventListener('keydown', escHandler);

  // Click outside handler
  const clickHandler = (e) => {
    if (!host.contains(e.target)) {
      destroyOverlay();
      document.removeEventListener('click', clickHandler);
    }
  };

  // Delay to prevent immediate dismissal from selection click
  setTimeout(() => {
    document.addEventListener('click', clickHandler);
  }, 100);

  // Store handlers for cleanup
  host._dismissHandlers = { escHandler, clickHandler };
}

// Destroy overlay and clean up event listeners
function destroyOverlay() {
  if (!currentOverlay) return;

  // Remove event listeners
  if (currentOverlay._dismissHandlers) {
    document.removeEventListener(
      'keydown',
      currentOverlay._dismissHandlers.escHandler,
    );
    document.removeEventListener(
      'click',
      currentOverlay._dismissHandlers.clickHandler,
    );
  }

  // Remove from DOM
  currentOverlay.remove();
  currentOverlay = null;
}

// Show error toast notification
function showErrorToast(message) {
  console.error('Translation error:', message);

  // Remove existing toast if any
  const existingToast = document.getElementById('instant-translate-error-toast');
  if (existingToast) {
    existingToast.remove();
  }

  const toast = document.createElement('div');
  toast.id = 'instant-translate-error-toast';
  toast.style.cssText = `
    position: fixed;
    bottom: 20px;
    right: 20px;
    background: #ef4444;
    color: white;
    padding: 12px 16px;
    border-radius: 8px;
    box-shadow: 0 4px 6px rgba(0, 0, 0, 0.1);
    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif;
    font-size: 14px;
    z-index: 2147483647;
    animation: slideIn 0.3s ease-out;
  `;
  toast.textContent = message;

  document.body.appendChild(toast);

  // Auto-remove after 4 seconds
  setTimeout(() => {
    toast.style.animation = 'slideOut 0.3s ease-in';
    setTimeout(() => toast.remove(), 300);
  }, 4000);
}

// Show error message (deprecated, use showErrorToast)
function showError(message) {
  showErrorToast(message);
}

// Clear selection state
function clearSelectionState() {
  currentSelection = null;
}

// Cleanup on page unload
window.addEventListener('beforeunload', () => {
  document.removeEventListener('keydown', handleHotkey);
  destroyOverlay();
  clearSelectionState();
});

// Initialize
init();
