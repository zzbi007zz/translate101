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
