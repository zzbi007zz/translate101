# Chrome Web Store Registration - Quick Start

## ✅ What's Ready

### 1. Extension Package
- **File**: `instant-translate-v0.1.0.zip` (64KB)
- **Status**: ✅ Built and ready
- **Location**: Project root directory

### 2. Documentation
- **Privacy Policy**: `PRIVACY.md` ✅
- **URL for store**: `https://github.com/zzbi007zz/translate101/blob/main/PRIVACY.md`
- **Submission Guide**: `docs/chrome-web-store-submission-guide.md` ✅

### 3. Code
- **Version**: 0.1.0
- **Build**: Production optimized
- **Tests**: All features working ✅

---

## 🎯 What You Need to Do Now

### Step 1: Create Store Assets (Visual)

You need to create these images before submitting:

#### A. Small Promo Tile (Required)
- **Size**: 440x280 pixels
- **Format**: PNG or JPEG
- **Content**: Extension logo + "Instant Translate" text
- **Tool**: Canva (free) or Figma
- **Save as**: `docs/assets/promo-tile-440x280.png`

**Quick Canva Template**:
1. Visit canva.com
2. Create 440x280px design
3. Add gradient background (blue to purple)
4. Upload icon from `src/icons/icon128.png`
5. Add text: "Instant Translate"
6. Subtitle: "Translate any text instantly"
7. Download as PNG

#### B. Screenshots (Required - at least 1)
- **Size**: 1280x800 pixels
- **Format**: PNG
- **Quantity**: 1-3 screenshots

**Screenshot 1** - Translation in Action:
1. Open Reddit or Wikipedia
2. Load extension (chrome://extensions)
3. Select Vietnamese text
4. Press Ctrl+Shift+E
5. Take screenshot (Cmd+Shift+4 on Mac)
6. Crop to 1280x800
7. Save as: `docs/assets/screenshot-1-translation.png`

**Screenshot 2** - Settings Popup:
1. Click extension icon
2. Show API key configuration screen
3. Take screenshot
4. Resize to 1280x800
5. Save as: `docs/assets/screenshot-2-settings.png`

**Optional Screenshot 3** - Insert Button:
1. Show translation overlay with Copy/Insert buttons
2. Demonstrate text replacement in comment field
3. Save as: `docs/assets/screenshot-3-insert.png`

### Step 2: Register Chrome Web Store Developer Account

1. **Visit**: https://chrome.google.com/webstore/devconsole
2. **Sign in** with Google account
3. **Pay** $5 USD one-time registration fee
4. **Verify** email (check inbox)

### Step 3: Submit Extension

1. **Click "New Item"** in developer console
2. **Upload**: `instant-translate-v0.1.0.zip`
3. **Fill out store listing** (copy from guide):
   - Name: "Instant Translate - Quick Text Translation"
   - Summary: "Translate selected text instantly with Ctrl+Shift+E. Powered by Google Gemini AI."
   - Description: Copy from `docs/chrome-web-store-submission-guide.md` (section: "Description")
   - Category: Tools
   - Language: English

4. **Upload assets**:
   - Icon: `src/icons/icon128.png`
   - Small promo tile: `docs/assets/promo-tile-440x280.png`
   - Screenshots: `docs/assets/screenshot-*.png`

5. **Privacy tab**:
   - Privacy Policy URL: `https://github.com/zzbi007zz/translate101/blob/main/PRIVACY.md`
   - Permission justifications: Copy from guide
   - Data usage: Does NOT collect user data ✅

6. **Submit for review**

### Step 4: Wait for Review

- **Timeline**: 1-3 business days
- **Notification**: Email when published/rejected
- **If rejected**: Fix issues and resubmit

---

## 📋 Pre-Submission Checklist

Before clicking "Submit for Review", verify:

### Package
- [x] `instant-translate-v0.1.0.zip` created
- [x] Build successful (webpack)
- [x] All features tested

### Assets
- [ ] Small promo tile (440x280) created
- [ ] Screenshot 1 (1280x800) captured
- [ ] Screenshot 2 (1280x800) captured
- [ ] All images optimized

### Documentation
- [x] Privacy policy on GitHub
- [x] README updated
- [x] Version correct (0.1.0)

### Store Listing
- [ ] Name entered
- [ ] Summary < 132 chars
- [ ] Description copy-pasted
- [ ] Category: Tools
- [ ] Privacy URL added
- [ ] Permission justifications filled

### Legal
- [ ] Developer account registered
- [ ] $5 fee paid
- [ ] Email verified

---

## 🚀 Quick Commands

```bash
# If you need to rebuild
npm run build

# Create new package
./prepare-submission.sh

# Check package contents
unzip -l instant-translate-v0.1.0.zip

# Test extension locally
# 1. Open chrome://extensions/
# 2. Enable Developer mode
# 3. Load unpacked → select dist/ folder
```

---

## 📞 Important Links

- **Developer Console**: https://chrome.google.com/webstore/devconsole
- **Privacy Policy**: https://github.com/zzbi007zz/translate101/blob/main/PRIVACY.md
- **Full Guide**: `docs/chrome-web-store-submission-guide.md`
- **Get Gemini API Key**: https://aistudio.google.com/app/apikey

---

## ⏱️ Timeline

1. **Create assets**: 30-60 minutes (Canva + screenshots)
2. **Register account**: 5 minutes
3. **Fill store listing**: 15-30 minutes
4. **Submit**: 2 minutes
5. **Review**: 1-3 business days
6. **Published**: Instant (after approval)

---

## 💡 Tips

1. **Screenshots**: Make them visually appealing
   - Good lighting
   - Clear text
   - Show value proposition

2. **Description**: Highlight benefits, not features
   - ✅ "Translate instantly with one keystroke"
   - ❌ "Uses keyboard shortcut Ctrl+Shift+E"

3. **Reviews**: Respond to all user reviews promptly

4. **Updates**: Plan regular feature updates

---

**Ready to submit?** Start with Step 1 (create assets) 🎨

**Questions?** Check `docs/chrome-web-store-submission-guide.md` for detailed instructions!
