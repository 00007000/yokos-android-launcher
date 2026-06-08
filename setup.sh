#!/bin/bash

# BB10 Hub Launcher - Project Setup Script
# This script organizes all files into the correct directory structure

echo "Setting up BB10 Hub Launcher project..."

# Create directory structure
mkdir -p app/src/main/java/app/lawnchair/bb10hub
mkdir -p app/src/main/res/{values,anim,layout}
mkdir -p .github/workflows

# Move Kotlin source files
echo "Organizing source files..."
[ -f "HubNotificationService.kt" ] && mv HubNotificationService.kt app/src/main/java/app/lawnchair/bb10hub/
[ -f "Bb10HubActivity.kt" ] && mv Bb10HubActivity.kt app/src/main/java/app/lawnchair/bb10hub/
[ -f "BB10HubScreen.kt" ] && mv BB10HubScreen.kt app/src/main/java/app/lawnchair/bb10hub/

# Move resource files
echo "Organizing resource files..."
[ -f "colors.xml" ] && mv colors.xml app/src/main/res/values/
[ -f "strings.xml" ] && mv strings.xml app/src/main/res/values/
[ -f "styles.xml" ] && mv styles.xml app/src/main/res/values/

# Move animation files
[ -f "hub_enter.xml" ] && mv hub_enter.xml app/src/main/res/anim/
[ -f "hub_exit.xml" ] && mv hub_exit.xml app/src/main/res/anim/
[ -f "hub_close_enter.xml" ] && mv hub_close_enter.xml app/src/main/res/anim/
[ -f "hub_close_exit.xml" ] && mv hub_close_exit.xml app/src/main/res/anim/

# Move manifest
echo "Organizing manifest..."
[ -f "AndroidManifest.xml" ] && mv AndroidManifest.xml app/src/main/

# Move gradle files
echo "Organizing gradle configuration..."
[ -f "app_build.gradle" ] && mv app_build.gradle app/build.gradle
[ -f "settings.gradle" ] && [ ! -f "./settings.gradle" ] && cp settings.gradle ./settings.gradle

# Move workflow
echo "Organizing GitHub Actions workflow..."
[ -f "build-bb10.yml" ] && mv build-bb10.yml .github/workflows/

# Move other files
[ -f "proguard-rules.pro" ] && [ ! -f "app/proguard-rules.pro" ] && cp proguard-rules.pro app/
[ -f ".gitignore" ] && [ ! -f "./.gitignore" ] && cp .gitignore ./

# Verify structure
echo ""
echo "Project structure created:"
tree -L 3 app/src/main/ 2>/dev/null || find app/src/main/ -type f | head -20

echo ""
echo "✅ Setup complete!"
echo ""
echo "Next steps:"
echo "1. Ensure debug.keystore is in the root directory"
echo "2. Run: git add ."
echo "3. Run: git commit -m 'Initial BB10 Hub setup'"
echo "4. Run: git push origin main"
echo "5. Check GitHub Actions for build"
