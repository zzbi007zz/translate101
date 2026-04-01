# System Architecture - Instant Translate

## Overview

Instant Translate is a **Manifest V3 Chrome Extension** that provides real-time text translation using Google's Gemini API. The architecture follows Chrome's extension security model with service workers, content scripts, and isolated messaging.

**Version:** 1.0.0 MVP
**Bundle Size:** 31.8KB (minified)
**Status:** Complete ✓

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                      CHROME BROWSER                         │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────────┐         ┌──────────────────┐          │
│  │  CONTENT SCRIPT  │         │  BACKGROUND      │          │
│  │  (content.js)    │◄────────┤  SERVICE WORKER  │          │
│  │                  │         │  (background.js) │          │
│  │ • Listens to     │         │                  │          │
│  │   keyboard       │         │ • Handles Gemini │          │
│  │ • Gets selection │         │   API calls      │          │
│  │ • Injects overlay│────────►│ • Manages API    │          │
│  │ • Renders result │         │   key storage    │          │
│  └──────────────────┘         │ • Message router │          │
│           △                   └──────────────────┘          │
│           │                            △                    │
│           └────────────────────────────┤                    │
│                    Message Channel      │                   │
│                                         │                   │
│           ┌───────────────────────────────────────┐         │
│           │     POPUP UI (popup.html/js)          │         │
│           │                                       │         │
│           │ • API key configuration               │         │
│           │ • Extension settings                  │         │
│           │ • Status display                      │         │
│           └───────────────────────────────────────┘         │
│                          ▲                                  │
│                          │                                  │
│                    Chrome Storage API                       │
│                          │                                  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │         LOCAL STORAGE (Encrypted by Chrome)         │  │
│  │                                                      │  │
│  │ • geminiApiKey (user-configured)                   │  │
│  │ • translationCache (recent translations)           │  │
│  │ • extensionSettings                                │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
└─────────────────────────────────────────────────────────────┘
                            │
                            ▼
        ┌───────────────────────────────────────┐
        │   GOOGLE GEMINI API GATEWAY           │
        │   https://generativelanguage.         │
        │     googleapis.com/                   │
        │                                       │
        │ • Text analysis & translation         │
        │ • Multiple language detection         │
        │ • Rate-limited, API-key authenticated │
        └───────────────────────────────────────┘
```

---

## Component Details

### 1. **Background Service Worker** (`background.js`)

Runs persistently (or on-demand in Manifest V3) to handle:

**Responsibilities:**
- Listen for messages from content script
- Handle API key retrieval from Chrome Storage
- Make HTTP requests to Gemini API
- Handle errors and retry logic
- Route responses back to content script

**Key Functions:**
```javascript
chrome.runtime.onMessage.addListener() // Listen for translation requests
callGeminiAPI()                        // Make API call with retry
handleStorageAccess()                  // Get API key securely
```

**Security Features:**
- API key never exposed to content script
- All API calls made from privileged context
- Message validation before processing

---

### 2. **Content Script** (`content.js`)

Injected into every web page to handle:

**Responsibilities:**
- Monitor keyboard input (Ctrl+Shift+E)
- Get selected text from page
- Send translation request to background worker
- Inject and manage overlay UI
- Handle overlay events

**Key Functions:**
```javascript
document.addEventListener('keydown')  // Detect keyboard shortcut
window.getSelection()                 // Extract selected text
chrome.runtime.sendMessage()           // Send to background worker
createAndShowOverlay()                 // Render translation result
```

**Security Features:**
- No access to API keys
- Cannot modify extension settings
- Runs in isolated context (no access to page's `window`)
- DOMPurify sanitizes all rendered content

---

### 3. **Popup UI** (`popup.html`, `popup.js`, `popup.css`)

User interface for extension configuration:

**Responsibilities:**
- Allow user to enter/update API key
- Display current extension status
- Show recent translations (optional feature)
- Provide quick settings access

**Key Features:**
- Form validation for API key
- Visual feedback on save
- Secure storage via Chrome Storage API
- Responsive design

---

### 4. **API Provider** (`providers/gemini-api-client.js`)

Abstraction layer for Gemini API:

**Responsibilities:**
- Format translation requests
- Handle API authentication
- Parse Gemini responses
- Manage retries on failure

**Key Functions:**
```javascript
translateText(text, sourceLanguage, targetLanguage)
detectLanguage(text)
formatPrompt(text)
```

---

### 5. **Utility Modules**

#### **Chrome Storage Helper** (`utils/chrome-storage-helper.js`)
- Wrapper around `chrome.storage.local`
- Get/set encrypted storage securely
- Default value handling

#### **DOM Selection Helpers** (`utils/dom-selection-helpers.js`)
- Extract selected text safely
- Get selection coordinates
- Clear selection state

#### **Retry Helper** (`utils/retry-helper.js`)
- Exponential backoff logic
- Max retry configuration
- Error categorization (retriable vs fatal)

---

## Data Flow

### Translation Request Flow

```
User selects text
    ↓
User presses Ctrl+Shift+E
    ↓
Content script detects keydown event
    ↓
Content script extracts selected text
    ↓
Content script sends message: chrome.runtime.sendMessage({
  action: 'translate',
  text: 'hello world',
  sourceLanguage: 'en',
  targetLanguage: 'es'
})
    ↓
Background service worker receives message
    ↓
Background retrieves API key from Chrome Storage
    ↓
Background calls Gemini API with retry logic
    ↓
Gemini API returns translated text
    ↓
Background sends response back to content script
    ↓
Content script creates overlay with translation
    ↓
Overlay displays on page (non-intrusive)
    ↓
User can click overlay or press ESC to dismiss
```

---

## Security Model

### Manifest V3 Security

1. **Service Worker Sandboxing**
   - Background process runs in isolated context
   - Cannot directly access page DOM
   - All page interaction via message passing

2. **Content Script Isolation**
   - Runs in its own global scope
   - Cannot access extension's privileged APIs
   - Cannot access other content scripts

3. **API Key Protection**
   - Stored in Chrome Storage (encrypted at rest)
   - Never sent to content script
   - API calls made only from background worker
   - Not logged or transmitted insecurely

4. **CORS & Host Permissions**
   - Extension only requests access to Gemini API
   - Content script matches `<all_urls>` but has no special permissions
   - Background has `host_permissions` for Gemini only

5. **Content Sanitization**
   - All user-facing HTML rendered via DOMPurify
   - No innerHTML without sanitization
   - XSS prevention through DOM APIs

---

## Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Build Tool | Webpack | 5.105.4 |
| Bundling | ES Modules | Native |
| Code Quality | ESLint + Prettier | 8.57.1 |
| Sanitization | DOMPurify | 3.3.3 |
| API Integration | Fetch API | Native |
| Storage | Chrome Storage API | Native (v3) |
| Styling | CSS3 | Native |

---

## Performance Characteristics

### Bundle Size
- **Total:** 31.8KB (minified)
- **background.js:** ~8KB
- **content.js:** ~12KB
- **popup.js:** ~4KB
- **Assets:** ~7KB (icons, styles)

### Runtime Performance
- **Message latency:** <100ms (local storage access)
- **API call time:** 1-3s (Gemini API roundtrip)
- **Overlay render:** <50ms
- **Memory footprint:** <15MB per page

---

## Configuration & Storage

### Chrome Storage Structure
```javascript
{
  geminiApiKey: "string",           // User's API key (encrypted)
  cacheEnabled: true,               // Feature flag
  autoDetectLanguage: true,         // Language detection preference
  targetLanguage: "es",             // Default target language
  recentTranslations: [             // Cache of recent results
    {
      original: "hello",
      translated: "hola",
      timestamp: 1711900000
    }
  ]
}
```

---

## Error Handling

### Retriable Errors
- Network timeout (up to 3 retries)
- Temporary API errors (429, 5xx)
- Rate limiting

### Fatal Errors
- Invalid API key
- Invalid request format
- Unauthorized access
- User cancellation

### User Feedback
- Error messages displayed in overlay
- Retry indication
- Configuration prompts

---

## Testing Strategy

### Unit Testing
- Utility function tests (storage, DOM helpers, retry logic)
- API client tests (format, parsing, error handling)

### Integration Testing
- Message passing between scripts
- End-to-end translation flow
- Storage persistence

### Manual Testing
- Keyboard shortcut recognition
- Overlay positioning and styling
- Multiple language pairs
- Error scenarios (no API key, network down)

---

## Future Extensibility

### Planned Enhancements
1. Multiple API providers (OpenAI, Azure, etc.)
2. Translation history with export
3. Keyboard shortcut customization
4. Language pair presets
5. Batch translation
6. Context menu integration

### Architectural Considerations
- Provider pattern allows easy addition of new APIs
- Modular utility functions support feature expansion
- Message protocol supports new action types
- Storage structure is version-aware for migrations

---

## Deployment & Updates

### Chrome Web Store
- Manifest V3 compliant
- Privacy policy required
- Icons and screenshots
- Version bumping via package.json

### Update Mechanism
- Chrome auto-updates installed extensions
- New features released via version bump
- Breaking changes documented in changelog

---

**Document Version:** 1.0.0
**Last Updated:** April 1, 2026
**Status:** MVP Complete ✓
