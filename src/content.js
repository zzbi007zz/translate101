// Content script - Injected into web pages
import DOMPurify from 'dompurify';
import {
  getSelectionText,
  getSelectionCoordinates,
} from './utils/dom-selection-helpers';

let currentSelection = null;
let currentOverlay = null;
let activeFormElement = null; // Track the active input/textarea for insertion

// Initialize content script
function init() {
  registerMessageListener();
  console.log('Instant Translate content script loaded');
}

// Register listener for both hotkey and context menu messages
function registerMessageListener() {
  chrome.runtime.onMessage.addListener((message) => {
    if (message.action === 'translateFromHotkey') {
      // Get current selection and translate
      const selection = captureSelection();
      if (selection) {
        requestTranslation(selection.text, selection.coords);
      }
    } else if (message.action === 'translateFromContextMenu') {
      // Context menu provides text, get coordinates
      const coords = getSelectionCoordinates();
      if (coords) {
        requestTranslation(message.text, coords);
      } else {
        // Fallback positioning
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

// Capture current text selection and coordinates
function captureSelection() {
  const text = getSelectionText();
  if (!text || text.length === 0) return null;

  const coords = getSelectionCoordinates();
  if (!coords) return null;

  // Track active form element for insertion feature
  const activeEl = document.activeElement;
  if (
    activeEl &&
    (activeEl.tagName === 'INPUT' || activeEl.tagName === 'TEXTAREA') &&
    activeEl.type !== 'password'
  ) {
    activeFormElement = activeEl;
  } else {
    activeFormElement = null;
  }

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
      margin-bottom: 8px;
    }

    .overlay-actions {
      display: flex;
      gap: 6px;
      margin-top: 8px;
    }

    .overlay-btn {
      padding: 4px 10px;
      border: none;
      border-radius: 4px;
      font-size: 12px;
      font-weight: 500;
      cursor: pointer;
      transition: background 0.2s;
    }

    .overlay-btn-copy {
      background: #3b82f6;
      color: white;
    }

    .overlay-btn-copy:hover {
      background: #2563eb;
    }

    .overlay-btn-insert {
      background: #10b981;
      color: white;
    }

    .overlay-btn-insert:hover {
      background: #059669;
    }

    .overlay-btn:disabled {
      background: #9ca3af;
      cursor: not-allowed;
      opacity: 0.5;
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

  // Action buttons container
  const actionsDiv = document.createElement('div');
  actionsDiv.className = 'overlay-actions';

  // Copy button
  const copyBtn = document.createElement('button');
  copyBtn.className = 'overlay-btn overlay-btn-copy';
  copyBtn.textContent = 'Copy';
  copyBtn.title = 'Copy translation to clipboard';
  copyBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    copyToClipboard(translation);
  });

  // Insert button (only show if we have an active form element)
  const insertBtn = document.createElement('button');
  insertBtn.className = 'overlay-btn overlay-btn-insert';
  insertBtn.textContent = 'Insert';
  insertBtn.title = 'Replace selected text with translation';
  if (!activeFormElement) {
    insertBtn.disabled = true;
    insertBtn.title = 'Insert only works in text fields';
  }
  insertBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    insertTranslation(translation);
  });

  actionsDiv.appendChild(copyBtn);
  actionsDiv.appendChild(insertBtn);

  // Close button
  const closeBtn = document.createElement('button');
  closeBtn.className = 'overlay-close';
  closeBtn.textContent = '×';
  closeBtn.title = 'Close (ESC)';
  closeBtn.addEventListener('click', (e) => {
    e.stopPropagation();
    destroyOverlay();
  });

  container.appendChild(textDiv);
  container.appendChild(actionsDiv);
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

// Show success toast notification
function showSuccessToast(message) {
  // Remove existing toast if any
  const existingToast = document.getElementById('instant-translate-success-toast');
  if (existingToast) {
    existingToast.remove();
  }

  const toast = document.createElement('div');
  toast.id = 'instant-translate-success-toast';
  toast.style.cssText = `
    position: fixed;
    bottom: 20px;
    right: 20px;
    background: #10b981;
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

  // Auto-remove after 2 seconds
  setTimeout(() => {
    toast.style.animation = 'slideOut 0.3s ease-in';
    setTimeout(() => toast.remove(), 300);
  }, 2000);
}

// Copy translation to clipboard
async function copyToClipboard(text) {
  try {
    await navigator.clipboard.writeText(text);
    showSuccessToast('Copied to clipboard!');
  } catch (error) {
    console.error('Failed to copy:', error);
    showErrorToast('Failed to copy to clipboard');
  }
}

// Insert translation into active form element
function insertTranslation(translation) {
  if (!activeFormElement) {
    showErrorToast('No text field selected');
    return;
  }

  try {
    const start = activeFormElement.selectionStart;
    const end = activeFormElement.selectionEnd;
    const currentValue = activeFormElement.value;

    // Replace selected text with translation
    const newValue =
      currentValue.substring(0, start) +
      translation +
      currentValue.substring(end);

    activeFormElement.value = newValue;

    // Set cursor position after inserted text
    const newCursorPos = start + translation.length;
    activeFormElement.selectionStart = newCursorPos;
    activeFormElement.selectionEnd = newCursorPos;

    // Focus the element
    activeFormElement.focus();

    // Trigger input event for frameworks that listen to it
    activeFormElement.dispatchEvent(new Event('input', { bubbles: true }));
    activeFormElement.dispatchEvent(new Event('change', { bubbles: true }));

    showSuccessToast('Translation inserted!');
    destroyOverlay();
  } catch (error) {
    console.error('Failed to insert:', error);
    showErrorToast('Failed to insert translation');
  }
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
  destroyOverlay();
  clearSelectionState();
});

// Initialize
init();
