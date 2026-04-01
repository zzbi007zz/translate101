// Popup UI logic for settings management
import {
  getApiKey,
  setApiKey,
  getSetting,
  setSetting,
} from './utils/chrome-storage-helper';

// DOM elements
let apiKeyInput;
let targetLangSelect;
let toggleKeyBtn;
let testBtn;
let saveBtn;
let statusMessage;
let settingsForm;

// Initialize popup
// Fix: Handle ES module deferred loading - check if DOM is already ready
function init() {
  initializeElements();
  loadSettings();
  registerEventListeners();
}

// Check DOM ready state and execute immediately if already loaded
if (document.readyState === 'loading') {
  document.addEventListener('DOMContentLoaded', init);
} else {
  // DOM already loaded, execute immediately
  init();
}

function initializeElements() {
  apiKeyInput = document.getElementById('api-key');
  targetLangSelect = document.getElementById('target-lang');
  toggleKeyBtn = document.getElementById('toggle-key');
  testBtn = document.getElementById('test-btn');
  saveBtn = document.getElementById('save-btn');
  statusMessage = document.getElementById('status-message');
  settingsForm = document.getElementById('settings-form');
}

// Load current settings from Chrome Storage
async function loadSettings() {
  try {
    // Load API key
    const apiKey = await getApiKey('gemini');
    if (apiKey) {
      apiKeyInput.value = apiKey;
    }

    // Load target language
    const targetLang = await getSetting('targetLanguage', 'en');
    targetLangSelect.value = targetLang;
  } catch (error) {
    console.error('Failed to load settings:', error);
    showStatus('Failed to load settings', 'error');
  }
}

// Register event listeners
function registerEventListeners() {
  // Toggle API key visibility
  toggleKeyBtn.addEventListener('click', toggleApiKeyVisibility);

  // Test translation
  testBtn.addEventListener('click', handleTestTranslation);

  // Save settings
  settingsForm.addEventListener('submit', handleSaveSettings);
}

// Toggle API key visibility
function toggleApiKeyVisibility() {
  if (apiKeyInput.type === 'password') {
    apiKeyInput.type = 'text';
    toggleKeyBtn.textContent = '🙈';
  } else {
    apiKeyInput.type = 'password';
    toggleKeyBtn.textContent = '👁️';
  }
}

// Handle test translation button click
async function handleTestTranslation() {
  const apiKey = apiKeyInput.value.trim();

  if (!apiKey) {
    showStatus('Please enter an API key first', 'error');
    return;
  }

  // Disable button during test
  testBtn.disabled = true;
  testBtn.textContent = 'Testing...';

  try {
    // Save API key temporarily for test
    await setApiKey('gemini', apiKey);

    // Send test translation request to background worker
    const response = await chrome.runtime.sendMessage({
      action: 'translate',
      text: 'Hello, world!',
      targetLang: 'vi',
    });

    if (response.success) {
      showStatus(
        `✓ Test successful! Translation: "${response.translation}"`,
        'success',
      );
    } else {
      showStatus(`✗ Test failed: ${response.error}`, 'error');
    }
  } catch (error) {
    console.error('Test translation error:', error);
    showStatus(`✗ Test failed: ${error.message}`, 'error');
  } finally {
    // Re-enable button
    testBtn.disabled = false;
    testBtn.textContent = 'Test Translation';
  }
}

// Handle save settings form submission
async function handleSaveSettings(event) {
  event.preventDefault();

  const apiKey = apiKeyInput.value.trim();
  const targetLang = targetLangSelect.value;

  // Validate API key
  if (!apiKey) {
    showStatus('Please enter an API key', 'error');
    return;
  }

  if (!apiKey.startsWith('AIza')) {
    showStatus('Invalid API key format. Should start with "AIza"', 'error');
    return;
  }

  // Disable button during save
  saveBtn.disabled = true;
  saveBtn.textContent = 'Saving...';

  try {
    // Save settings to Chrome Storage
    await setApiKey('gemini', apiKey);
    await setSetting('targetLanguage', targetLang);

    showStatus('✓ Settings saved successfully!', 'success');

    // Auto-hide success message after 3 seconds
    setTimeout(() => {
      hideStatus();
    }, 3000);
  } catch (error) {
    console.error('Failed to save settings:', error);
    showStatus(`✗ Failed to save settings: ${error.message}`, 'error');
  } finally {
    // Re-enable button
    saveBtn.disabled = false;
    saveBtn.textContent = 'Save Settings';
  }
}

// Show status message
function showStatus(message, type = 'info') {
  statusMessage.textContent = message;
  statusMessage.className = `status-message ${type}`;
}

// Hide status message
function hideStatus() {
  statusMessage.className = 'status-message';
}
