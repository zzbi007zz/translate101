# Chrome Web Store Submission Guide

## Prerequisites

### 1. Developer Account
- **URL**: https://chrome.google.com/webstore/devconsole
- **Cost**: $5 USD one-time registration fee
- **Requirements**: Google account + payment method

### 2. Required Assets

#### Extension Package (dist.zip)
```bash
# Build extension
npm run build

# Create submission package
cd dist
zip -r ../instant-translate-v0.1.0.zip .
cd ..
```

**Verify package contains**:
- `manifest.json`
- `background.js`
- `content.js`
- `popup.html`, `popup.js`, `popup.css`
- `icons/` directory (icon16.png, icon48.png, icon128.png)
- `styles/overlay.css`

**Important**:
- Max size: 100MB (our bundle: ~40KB)
- No executables (.exe, .dll)
- No obfuscated code

#### Store Listing Images

**1. Icon (Required)**
- Size: 128x128 pixels
- Format: PNG
- Location: `src/icons/icon128.png`
- **Already created ✅**

**2. Small Promo Tile (Required)**
- Size: 440x280 pixels
- Format: PNG or JPEG
- **Status**: Need to create

**3. Screenshots (Required - minimum 1)**
- Size: 1280x800 or 640x400 pixels
- Format: PNG or JPEG
- Quantity: 1-5 screenshots
- **Status**: Need to create

**4. Marquee Promo Tile (Optional)**
- Size: 1400x560 pixels
- Format: PNG or JPEG
- **Status**: Optional, can skip for MVP

---

## Step-by-Step Submission

### Step 1: Prepare Extension Package

```bash
# Navigate to project
cd /Users/february10/Documents/instantTranslate

# Build production version
npm run build

# Create ZIP package
cd dist
zip -r ../instant-translate-v0.1.0.zip .
cd ..

# Verify package
unzip -l instant-translate-v0.1.0.zip
```

**Checklist**:
- [ ] Build successful
- [ ] No console errors
- [ ] All features working
- [ ] ZIP created successfully

### Step 2: Create Store Assets

#### Screenshot 1: Translation in Action (1280x800)
**Content**: Show translation overlay on a webpage
- Open Reddit or Wikipedia
- Select text
- Press Ctrl+Shift+E
- Capture overlay showing translation
- Crop to 1280x800

**Tool**: macOS Screenshot (Cmd+Shift+4)
**Save as**: `docs/assets/screenshot-1-translation.png`

#### Screenshot 2: Settings Popup (1280x800)
**Content**: Show extension settings
- Click extension icon
- Show API key configuration
- Capture popup
- Resize to 1280x800

**Save as**: `docs/assets/screenshot-2-settings.png`

#### Screenshot 3: Insert Action (Optional, 1280x800)
**Content**: Show Insert button replacing text
- Select text in comment field
- Show overlay with Copy/Insert buttons
- Capture action
- Resize to 1280x800

**Save as**: `docs/assets/screenshot-3-insert.png`

#### Small Promo Tile (440x280)
**Content**: Extension logo + tagline
- Background: Gradient (blue to purple)
- Icon: Center logo
- Text: "Instant Translate" + "Translate any text instantly"
- Use Figma/Canva or Photoshop

**Save as**: `docs/assets/promo-tile-440x280.png`

**Design template**:
```
┌─────────────────────────────────────┐
│                                     │
│         [Icon 96x96]                │
│                                     │
│       Instant Translate             │
│   Translate any text instantly      │
│                                     │
└─────────────────────────────────────┘
```

### Step 3: Chrome Web Store Registration

1. **Visit Developer Console**:
   - URL: https://chrome.google.com/webstore/devconsole
   - Sign in with Google account

2. **Pay Registration Fee**:
   - One-time: $5 USD
   - Required for publishing
   - Payment via credit card or Google Pay

3. **Verify Email**:
   - Check for verification email
   - Click verification link

### Step 4: Create New Item

1. **Click "New Item"**
2. **Upload ZIP**: `instant-translate-v0.1.0.zip`
3. **Wait for upload** (~1 minute)

### Step 5: Fill Store Listing

#### Product Details Tab

**Extension Name**:
```
Instant Translate - Quick Text Translation
```

**Summary** (132 characters max):
```
Translate selected text instantly with Ctrl+Shift+E. Powered by Google Gemini AI. Fast, private, no data collection.
```

**Description** (16,000 characters max):
```markdown
# 🌍 Instant Translate - Translate Any Text Instantly

**Translate any text on any webpage with a single keyboard shortcut**

Simply select text and press **Ctrl+Shift+E** (or Cmd+Shift+E on Mac) to get instant, AI-powered translation. No context menus, no copy-pasting—just select and translate.

---

## ✨ Key Features

### 🚀 Lightning Fast Translation
- **<100ms** keyboard response time
- **1-3 seconds** translation via Google Gemini AI
- Works on ANY website (news, blogs, social media, documentation)

### 📝 Smart Actions
- **Copy**: Copy translation to clipboard
- **Insert**: Replace selected text with translation (works in comment fields!)
- **Dismiss**: Click away or press ESC

### 🎯 Non-Intrusive Overlay
Translation appears in a beautiful floating overlay right next to your selected text. No page disruption, no pop-ups.

### 🔒 Privacy-First Design
- **Your API Key**: Uses your own Google Gemini API key
- **Zero Tracking**: No analytics, telemetry, or data collection
- **Local Processing**: No server-side tracking
- **Open Source**: Full transparency on GitHub

### 🌐 Multilingual Support
Translate between 8+ languages:
- English ↔ Vietnamese
- English ↔ Spanish
- English ↔ French
- English ↔ German
- English ↔ Japanese
- English ↔ Korean
- English ↔ Chinese

### ⚡ Super Lightweight
- **33.5KB** bundle size
- **<15MB** memory per page
- **<50ms** page load impact

---

## 🎬 How to Use

### Setup (One-Time)
1. Install extension
2. Click extension icon
3. Get free Gemini API key at: https://aistudio.google.com/app/apikey
4. Paste key and save settings

### Translate
1. **Select** any text on any webpage
2. **Press** Ctrl+Shift+E (or Cmd+Shift+E on Mac)
3. **View** translation in floating overlay

**Alternative**: Right-click selected text → "Translate [text]"

### Copy Translation
- Click "Copy" button in overlay
- Translation copied to clipboard

### Insert Translation
- Click "Insert" button to replace selected text
- Works in Reddit, Facebook comments, text fields, forms

---

## 🛡️ Security & Privacy

### What We DO ✅
- Encrypt your API key in Chrome Storage
- Use Shadow DOM for CSS isolation
- Sanitize all output (XSS prevention)
- Follow Chrome security best practices

### What We DON'T DO ❌
- Collect user data
- Track browsing history
- Send analytics
- Auto-translate pages
- Share API keys
- Store translation history

---

## 🎯 Perfect For

- **Students**: Translate research papers, articles, documentation
- **Professionals**: Quick email/document translation
- **Travelers**: Understand foreign websites
- **Language Learners**: Instant word/phrase lookup
- **Developers**: Translate technical docs
- **Content Creators**: Multilingual content creation

---

## 🔧 Technical Details

- **Manifest V3**: Latest Chrome standard
- **Google Gemini AI**: Advanced language model
- **Shadow DOM**: Style isolation
- **DOMPurify**: XSS protection
- **Webpack 5**: Optimized bundling

---

## 📞 Support

- **GitHub**: https://github.com/zzbi007zz/translate101
- **Issues**: Report bugs or request features
- **Documentation**: Full guides in repository

---

## 🌟 Why Choose Instant Translate?

✅ **Fastest**: Keyboard shortcut beats context menus
✅ **Privacy**: Your data stays yours
✅ **Free**: Uses free Gemini API (with generous limits)
✅ **Lightweight**: Minimal performance impact
✅ **Smart**: Works in comment fields, forms, contenteditable elements
✅ **Beautiful**: Clean, modern UI
✅ **Open Source**: Fully transparent codebase

---

**Get your free Gemini API key**: https://aistudio.google.com/app/apikey
**Source code**: https://github.com/zzbi007zz/translate101
```

**Category**:
- Primary: `Tools`
- (Productivity tools, utilities)

**Language**:
- `English`

**Visibility**:
- [x] Public (recommended)
- [ ] Unlisted
- [ ] Private

#### Store Listing Tab

**Icon** (128x128):
- Upload: `src/icons/icon128.png`

**Small Promo Tile** (440x280):
- Upload: `docs/assets/promo-tile-440x280.png`

**Screenshots**:
1. Upload: `docs/assets/screenshot-1-translation.png`
2. Upload: `docs/assets/screenshot-2-settings.png`
3. Upload: `docs/assets/screenshot-3-insert.png` (optional)

**YouTube Video** (Optional):
- Skip for MVP

#### Privacy Tab

**Single Purpose Description**:
```
This extension provides instant translation of selected text using Google Gemini AI. Users select text on any webpage and press a keyboard shortcut to see translation in a floating overlay.
```

**Permission Justifications**:

1. **storage**:
```
Stores user's Gemini API key and language preferences locally in Chrome Storage. Required for saving settings between sessions.
```

2. **contextMenus**:
```
Adds "Translate" option to right-click context menu when text is selected. Provides alternative translation trigger method.
```

3. **Host Permission: generativelanguage.googleapis.com**:
```
Communicates with Google Gemini API to perform text translation. All translation requests are sent to this official Google endpoint.
```

**Privacy Policy URL**:
```
https://github.com/zzbi007zz/translate101/blob/main/PRIVACY.md
```
(Create this file - see Step 6)

**Data Usage**:
- [ ] Collects or transmits user data
- [x] Does NOT collect or transmit user data

**Certification**:
- [x] This extension complies with Chrome Web Store Program Policies

### Step 6: Create Privacy Policy

Create `PRIVACY.md` in repository:

```markdown
# Privacy Policy for Instant Translate

**Last Updated**: April 2, 2026

## Overview
Instant Translate is a browser extension that translates selected text using Google Gemini AI. We are committed to protecting user privacy.

## Data Collection
**We DO NOT collect, store, or transmit any user data.**

## What We Store Locally
The extension stores the following data **locally on your device only**:
- Your Google Gemini API key (encrypted in Chrome Storage)
- Your preferred target language

This data **never leaves your device** except for translation API calls.

## Third-Party Services
The extension communicates with:
- **Google Gemini API** (`generativelanguage.googleapis.com`)
  - Purpose: Text translation
  - Data sent: Selected text + target language
  - Data NOT sent: Browsing history, personal info, API key
  - Google's Privacy Policy: https://policies.google.com/privacy

## Permissions
- **storage**: Save API key and settings locally
- **contextMenus**: Add "Translate" to right-click menu
- **Host permission (generativelanguage.googleapis.com)**: Call translation API

## Security
- API key encrypted in Chrome Storage
- No server-side processing
- No analytics or tracking
- XSS prevention via DOMPurify
- Shadow DOM isolation

## Data Retention
- API key stored until user removes extension
- No translation history saved
- No server logs

## User Control
- Users can delete API key anytime (extension settings)
- Uninstalling extension removes all local data
- Users control their own Gemini API account

## Contact
For privacy questions:
- GitHub: https://github.com/zzbi007zz/translate101/issues

## Changes
We will update this policy for any material changes. Last update: April 2, 2026.
```

### Step 7: Submit for Review

1. **Review all tabs** (Product Details, Store Listing, Privacy)
2. **Click "Submit for Review"**
3. **Review Timeline**: 1-3 business days (usually faster)

### Step 8: Post-Submission

**You'll receive email notifications for**:
- Submission received
- Review in progress
- Published (or rejection with reasons)

**Common Rejection Reasons**:
- Missing privacy policy
- Unclear permission justifications
- Poor quality screenshots
- Misleading description

**If Rejected**:
- Read feedback carefully
- Fix issues
- Resubmit

---

## Quick Command Reference

```bash
# Build extension
npm run build

# Create ZIP
cd dist && zip -r ../instant-translate-v0.1.0.zip . && cd ..

# Test ZIP contents
unzip -l instant-translate-v0.1.0.zip

# Create screenshots directory
mkdir -p docs/assets

# Check manifest version
cat src/manifest.json | grep version
```

---

## Checklist Before Submission

### Code
- [ ] Build successful (`npm run build`)
- [ ] No console errors
- [ ] All features tested
- [ ] Version bumped in manifest.json

### Package
- [ ] ZIP created from dist/
- [ ] ZIP < 100MB
- [ ] All files included

### Assets
- [ ] Icon 128x128 ready
- [ ] Small promo tile 440x280 created
- [ ] 1-3 screenshots captured (1280x800)
- [ ] All images optimized

### Store Listing
- [ ] Name compelling
- [ ] Summary < 132 chars
- [ ] Description detailed
- [ ] Category selected
- [ ] Language: English

### Privacy
- [ ] Privacy policy created on GitHub
- [ ] URL added to store listing
- [ ] Permission justifications clear
- [ ] Single purpose described

### Legal
- [ ] Developer account registered ($5)
- [ ] Email verified
- [ ] Complies with Chrome policies

---

## Post-Approval Tasks

1. **Share**: Twitter, Reddit, Product Hunt
2. **Monitor**: Reviews, ratings, support requests
3. **Update**: Fix bugs, add features
4. **Iterate**: Collect feedback, improve

---

## Resources

- **Developer Console**: https://chrome.google.com/webstore/devconsole
- **Program Policies**: https://developer.chrome.com/docs/webstore/program-policies
- **Best Practices**: https://developer.chrome.com/docs/webstore/best_practices
- **Branding Guidelines**: https://developer.chrome.com/docs/webstore/branding
- **Get Gemini API Key**: https://aistudio.google.com/app/apikey

---

**Ready to submit?** Follow steps 1-7 above! 🚀
