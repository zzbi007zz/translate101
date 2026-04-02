#!/bin/bash
# Chrome Web Store Submission Package Creator
# Usage: ./prepare-submission.sh

set -e

echo "🚀 Preparing Chrome Web Store Submission Package"
echo ""

# Step 1: Clean build
echo "📦 Step 1: Building extension..."
npm run build
echo "✅ Build complete"
echo ""

# Step 2: Create ZIP package
echo "📦 Step 2: Creating ZIP package..."
cd dist
ZIP_NAME="instant-translate-v0.1.0.zip"
zip -r ../$ZIP_NAME .
cd ..
echo "✅ Package created: $ZIP_NAME"
echo ""

# Step 3: Verify ZIP contents
echo "📦 Step 3: Verifying package..."
unzip -l $ZIP_NAME | head -20
echo ""
ZIP_SIZE=$(du -h $ZIP_NAME | cut -f1)
echo "✅ Package size: $ZIP_SIZE"
echo ""

# Step 4: Check manifest version
echo "📦 Step 4: Checking manifest..."
MANIFEST_VERSION=$(cat src/manifest.json | grep '"version"' | cut -d'"' -f4)
echo "✅ Version: $MANIFEST_VERSION"
echo ""

# Step 5: Create assets directory
echo "📦 Step 5: Preparing assets directory..."
mkdir -p docs/assets
echo "✅ Assets directory ready: docs/assets/"
echo ""

# Summary
echo "✅ Submission package ready!"
echo ""
echo "📋 Next Steps:"
echo "1. Create store assets:"
echo "   - Small promo tile (440x280): docs/assets/promo-tile-440x280.png"
echo "   - Screenshot 1 (1280x800): docs/assets/screenshot-1-translation.png"
echo "   - Screenshot 2 (1280x800): docs/assets/screenshot-2-settings.png"
echo ""
echo "2. Register Chrome Web Store Developer account:"
echo "   - URL: https://chrome.google.com/webstore/devconsole"
echo "   - Cost: \$5 USD one-time fee"
echo ""
echo "3. Upload and submit:"
echo "   - Package: $ZIP_NAME"
echo "   - Privacy Policy: https://github.com/zzbi007zz/translate101/blob/main/PRIVACY.md"
echo ""
echo "📚 Full guide: docs/chrome-web-store-submission-guide.md"
