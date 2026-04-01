# Code Standards & Best Practices

## Overview

This document defines the coding standards for the Instant Translate Chrome Extension. All code must comply with these standards before merge.

**Standards Version:** 1.0.0
**ESLint Config:** Airbnb
**Code Formatter:** Prettier
**Target:** 100% compliance

---

## JavaScript Code Style

### Variable & Function Naming

#### Rules
- **Variables/Functions:** `camelCase`
- **Constants:** `CONSTANT_CASE`
- **Classes/Constructors:** `PascalCase`
- **File Names:** `kebab-case-with-description.js`
- **Private Methods:** prefix with `_privateMethod()`

#### Examples
```javascript
// ✓ CORRECT
const apiKey = chrome.storage.local.get('geminiApiKey');
const MAX_RETRIES = 3;
const translateText = (text) => { /* ... */ };
class GeminiApiClient { /* ... */ }

// ✗ INCORRECT
const api_key = chrome.storage.local.get('geminiApiKey');
const maxRetries = 3;  // Should be CONSTANT_CASE
const TranslateText = (text) => { /* ... */ };
```

### Indentation & Formatting

#### Rules
- **Indentation:** 2 spaces (no tabs)
- **Line Length:** Max 100 characters
- **Semicolons:** Required
- **Quotes:** Single quotes for strings (use double only for HTML attributes)
- **Trailing Commas:** Only in multi-line structures

#### Examples
```javascript
// ✓ CORRECT
const config = {
  apiKey: 'user-key',
  maxRetries: 3,
  timeout: 5000,
};

const messageHandler = (request, sender, sendResponse) => {
  if (request.action === 'translate') {
    performTranslation(request.text).then((result) => {
      sendResponse({ success: true, result });
    });
  }
};

// ✗ INCORRECT
const config = { apiKey: "user-key", maxRetries:3,timeout:5000 }
const messageHandler = (request, sender, sendResponse) => {
    if (request.action === "translate")
    performTranslation(request.text)
      .then(result => sendResponse({ success: true, result }))
}
```

### Arrow Functions vs Function Declarations

#### Rules
- Use arrow functions for callbacks and short functions
- Use function declarations for exported functions
- Avoid `function` keyword except for constructors

#### Examples
```javascript
// ✓ CORRECT - Arrow function for callback
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  // Handle message
});

// ✓ CORRECT - Function declaration for export
export function initializeStorage() {
  // Initialize
}

// ✗ INCORRECT - Unnecessary function keyword
const handler = function(request, sender) {
  // ...
};
```

### Comments & Documentation

#### Rules
- Comment complex logic only (not obvious code)
- Use `//` for single-line comments
- Use `/* */` for multi-line explanations
- Include JSDoc for exported functions

#### Examples
```javascript
// ✓ CORRECT
/**
 * Translates text using Gemini API with retry logic
 * @param {string} text - Text to translate
 * @param {number} maxRetries - Maximum retry attempts
 * @returns {Promise<string>} Translated text
 */
export async function translateWithRetry(text, maxRetries = 3) {
  // Retry logic with exponential backoff
  for (let attempt = 0; attempt < maxRetries; attempt += 1) {
    try {
      const result = await callGeminiAPI(text);
      return result;
    } catch (error) {
      if (attempt === maxRetries - 1) throw error;
      // Wait before retrying
      await new Promise(resolve => setTimeout(resolve, 1000 * (2 ** attempt)));
    }
  }
}

// ✗ INCORRECT
// bad function name
function trans(t, r) {
  // stuff
}
```

### Error Handling

#### Rules
- Always handle promise rejections
- Use try/catch for async operations
- Log errors appropriately
- Provide user-facing error messages

#### Examples
```javascript
// ✓ CORRECT
export async function fetchTranslation(text) {
  try {
    const apiKey = await getApiKeyFromStorage();
    if (!apiKey) {
      throw new Error('API key not configured');
    }
    const response = await callGeminiAPI(text, apiKey);
    return response.translation;
  } catch (error) {
    console.error('Translation error:', error.message);
    return { error: 'Translation failed. Please try again.' };
  }
}

// ✗ INCORRECT
function fetchTranslation(text) {
  callGeminiAPI(text).then(r => r.translation);
  // No error handling!
}
```

---

## Module Organization

### File Structure Rules

```
src/
├── background.js              # Service worker (single file, <100 lines)
├── content.js                 # Content script (single file, <150 lines)
├── popup.js                   # Popup logic (single file, <100 lines)
├── manifest.json              # Extension manifest
├── popup.html                 # Popup UI
├── popup.css                  # Popup styles
├── providers/
│   └── gemini-api-client.js   # Gemini API integration
├── utils/
│   ├── chrome-storage-helper.js    # Storage wrapper
│   ├── dom-selection-helpers.js    # DOM utilities
│   └── retry-helper.js             # Retry logic
└── styles/
    └── overlay.css            # Translation overlay styles
```

### File Size Limits
- **Maximum:** 200 lines per file
- **Guideline:** 100 lines preferred
- **Exception:** Configuration files (manifest.json, etc.)

### Import/Export Rules

#### Rules
- Use ES6 `import`/`export` syntax
- One default export per file
- Named exports for utilities
- No circular dependencies

#### Examples
```javascript
// ✓ CORRECT
// In providers/gemini-api-client.js
export async function callGeminiAPI(text, apiKey) {
  // ...
}

// In background.js
import { callGeminiAPI } from './providers/gemini-api-client.js';

// ✗ INCORRECT
// No CommonJS require/module.exports
const { callGeminiAPI } = require('./providers/gemini-api-client.js');
```

---

## Chrome Extension Best Practices

### Message Passing

#### Rules
- Always validate message structure
- Include `action` field in requests
- Validate `sender.url` for security
- Use `sendResponse` for async operations

#### Examples
```javascript
// ✓ CORRECT
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  // Validate message structure
  if (!request.action || typeof request.action !== 'string') {
    sendResponse({ error: 'Invalid request' });
    return;
  }

  // Validate sender origin
  if (!sender.url.startsWith('https://')) {
    sendResponse({ error: 'Unauthorized' });
    return;
  }

  // Handle action
  if (request.action === 'translate') {
    performTranslation(request.text)
      .then(result => sendResponse({ success: true, result }))
      .catch(error => sendResponse({ success: false, error: error.message }));
    return true; // Keep channel open for async response
  }
});

// ✗ INCORRECT
chrome.runtime.onMessage.addListener((request, sender, sendResponse) => {
  // No validation!
  performTranslation(request.text).then(r => sendResponse(r));
});
```

### Storage API Usage

#### Rules
- Always use `chrome.storage.local` for data
- Never store sensitive data in localStorage
- Validate data before retrieval
- Handle storage quota errors

#### Examples
```javascript
// ✓ CORRECT
export async function getApiKey() {
  return new Promise((resolve, reject) => {
    chrome.storage.local.get('geminiApiKey', (items) => {
      if (chrome.runtime.lastError) {
        reject(new Error('Storage access failed'));
        return;
      }
      resolve(items.geminiApiKey || null);
    });
  });
}

// ✗ INCORRECT
function getApiKey() {
  return localStorage.getItem('geminiApiKey'); // WRONG - uses page storage!
}
```

### Content Script Injection

#### Rules
- Keep content scripts minimal
- Use isolated scope (no global pollution)
- Sanitize all DOM operations
- Use DOMPurify for HTML rendering

#### Examples
```javascript
// ✓ CORRECT
function showTranslationOverlay(translation) {
  const overlay = document.createElement('div');
  overlay.className = 'instant-translate-overlay';

  const sanitizedText = DOMPurify.sanitize(translation);
  overlay.textContent = sanitizedText; // Use textContent, not innerHTML

  document.body.appendChild(overlay);
}

// ✗ INCORRECT
function showTranslationOverlay(translation) {
  document.body.innerHTML += `<div>${translation}</div>`; // XSS vulnerability!
}
```

---

## Testing Standards

### Unit Testing

#### Coverage Requirements
- All exported functions must have tests
- Utility functions: 100% coverage
- Error paths: explicit test cases
- Edge cases: boundary conditions

#### Examples
```javascript
// Test structure
describe('retry-helper.js', () => {
  describe('retryWithBackoff()', () => {
    it('should return result on first attempt', async () => {
      const fn = jest.fn().mockResolvedValue('success');
      const result = await retryWithBackoff(fn, { maxRetries: 3 });
      expect(result).toBe('success');
      expect(fn).toHaveBeenCalledTimes(1);
    });

    it('should retry on failure', async () => {
      const fn = jest.fn()
        .mockRejectedValueOnce(new Error('Timeout'))
        .mockResolvedValueOnce('success');
      const result = await retryWithBackoff(fn, { maxRetries: 3 });
      expect(result).toBe('success');
      expect(fn).toHaveBeenCalledTimes(2);
    });
  });
});
```

### Manual Testing Checklist

Before commit:
- [ ] Keyboard shortcut works (Ctrl+Shift+E)
- [ ] Selected text is extracted correctly
- [ ] Translation appears in overlay
- [ ] Overlay dismisses on click/ESC
- [ ] API key configuration saves
- [ ] Error messages display correctly
- [ ] No ESLint errors: `npm run lint`
- [ ] Code formatted: `npm run format`

---

## Security Standards

### API Key Handling

#### Rules
- **Never** log API keys
- **Never** transmit to content scripts
- **Always** validate before use
- **Always** use HTTPS for API calls

#### Examples
```javascript
// ✓ CORRECT
async function callGeminiAPI(text, apiKey) {
  // Validate input
  if (!apiKey || typeof apiKey !== 'string') {
    throw new Error('Invalid API key');
  }

  // Use HTTPS
  // Note: Using `gemini-2.5-flash` (stable, production-ready, best price-performance)
  // Alternative models: `gemini-2.5-pro` (advanced), `gemini-3-flash-preview` (cutting-edge)
  const response = await fetch(
    'https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent',
    {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'x-goog-api-key': apiKey, // Proper header usage
      },
      body: JSON.stringify({ /* request */ }),
    }
  );

  return response.json();
}

// ✗ INCORRECT
async function callGeminiAPI(text, apiKey) {
  console.log('API Key:', apiKey); // LOGGING SENSITIVE DATA!

  // HTTP request - INSECURE!
  const response = await fetch(
    `http://example.com/api?key=${apiKey}&text=${text}` // KEY IN URL!
  );
}
```

### Content Security

#### Rules
- No inline scripts
- No eval or Function constructors
- Sanitize all HTML rendering
- Validate all external input

#### Examples
```javascript
// ✓ CORRECT
import DOMPurify from 'dompurify';

function renderTranslation(html) {
  const clean = DOMPurify.sanitize(html, {
    ALLOWED_TAGS: ['b', 'i', 'em', 'strong'],
  });
  const div = document.createElement('div');
  div.innerHTML = clean;
  return div;
}

// ✗ INCORRECT
function renderTranslation(html) {
  document.body.innerHTML = html; // Unvalidated HTML!
  eval(someCode); // NEVER DO THIS!
}
```

---

## Linting Rules

### ESLint Configuration (Airbnb)

#### Enforced Rules
```json
{
  "extends": "airbnb-base",
  "env": {
    "browser": true,
    "es2021": true,
    "webextensions": true
  },
  "rules": {
    "no-console": ["warn", { "allow": ["error"] }],
    "no-unused-vars": "error",
    "semi": ["error", "always"],
    "quotes": ["error", "single"],
    "comma-dangle": ["error", "always-multiline"],
    "indent": ["error", 2],
    "max-len": ["warn", { "code": 100 }],
    "no-param-reassign": "warn"
  }
}
```

#### Common Violations
1. **Missing semicolons** - Add at end of statements
2. **Unused variables** - Remove or prefix with `_`
3. **Undefined variables** - Import or define
4. **Improper indentation** - Use 2 spaces
5. **Missing const/let** - Never use `var`

### Running Linter

```bash
# Check all files
npm run lint

# Check specific file
npx eslint src/background.js

# Auto-fix some issues
npx eslint src/ --fix

# Fix formatting
npm run format
```

---

## Documentation Standards

### README Requirements
- Installation instructions
- Usage guide with examples
- Development setup
- Build commands
- Troubleshooting section

### Code Comments

#### When to Comment
- Complex algorithms
- Non-obvious logic
- Workarounds and hacks
- Important warnings

#### When NOT to Comment
- Self-documenting code
- Obvious variable assignments
- Simple conditionals
- Loop structures

#### Examples
```javascript
// ✓ GOOD - Explains non-obvious behavior
// Retry with exponential backoff to handle rate limiting
await new Promise(resolve => setTimeout(resolve, 1000 * (2 ** attempt)));

// ✗ BAD - Obvious code
const x = 5; // Set x to 5
if (x > 0) { // If x is greater than 0
  doSomething(); // Do something
}
```

---

## Commit Message Standards

### Format

```
<type>(<scope>): <subject>

<body>

<footer>
```

### Types
- `feat:` - New feature
- `fix:` - Bug fix
- `refactor:` - Code restructuring
- `test:` - Test additions
- `docs:` - Documentation
- `style:` - Formatting, no logic change
- `perf:` - Performance improvement

### Examples

```
feat(translation): add retry logic with exponential backoff

- Implement retryWithBackoff utility
- Add MAX_RETRIES constant
- Handle transient failures gracefully

Fixes #123
```

```
fix(overlay): prevent XSS vulnerability

Use DOMPurify for all HTML rendering in overlay.
Removes innerHTML usage in favor of safe DOM APIs.
```

---

## Performance Standards

### Code Optimization

#### Rules
- Avoid synchronous operations
- Use event delegation for listeners
- Minimize DOM queries
- Cache frequently used values

#### Examples
```javascript
// ✓ CORRECT - Async all the way
async function performTranslation() {
  const apiKey = await getApiKey();
  const result = await callGeminiAPI(text, apiKey);
  return result;
}

// ✓ CORRECT - Cache DOM queries
const overlay = document.getElementById('overlay');
const closeBtn = overlay.querySelector('.close');
closeBtn.addEventListener('click', () => overlay.remove());

// ✗ INCORRECT - Synchronous operations
const result = chrome.storage.local.get('key'); // Returns undefined immediately
const text = document.getElementById('id').textContent; // Repeated queries
```

### Bundle Size

#### Targets
- **Background:** <10KB
- **Content:** <15KB
- **Popup:** <5KB
- **Total:** <35KB

#### Monitoring
```bash
# Check bundle size
webpack --mode production --analyze
```

---

## Version Management

### Semantic Versioning

Format: `MAJOR.MINOR.PATCH`

- **MAJOR:** Breaking changes
- **MINOR:** New features (backward compatible)
- **PATCH:** Bug fixes

### Changelog Entries

Each release must update CHANGELOG.md with:
- Version number
- Release date
- New features
- Bug fixes
- Breaking changes

---

## Pull Request Checklist

Before submitting PR:
- [ ] Code passes linter: `npm run lint`
- [ ] Code formatted: `npm run format`
- [ ] All tests passing
- [ ] Manual testing completed
- [ ] No security vulnerabilities
- [ ] Documentation updated
- [ ] Commit messages follow standards
- [ ] No console.log statements (except errors)
- [ ] No hardcoded credentials
- [ ] Bundle size acceptable

---

**Standards Version:** 1.0.0
**Last Updated:** April 1, 2026
**Status:** ENFORCED ✓
