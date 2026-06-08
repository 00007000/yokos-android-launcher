# ✅ BB10 Hub Launcher Setup Checklist

Use this checklist to track your progress through the setup process.

---

## 📋 Pre-Setup (5 minutes)

- [ ] GitHub account created and logged in
- [ ] Git installed on computer (`git --version`)
- [ ] (Optional) GitHub CLI installed (`gh --version`)
- [ ] (Optional) ADB installed (`adb --version`)
- [ ] Android device ready (Android 13+)
- [ ] Text editor open and ready

---

## 🔧 Step 1: Create GitHub Repository (5 minutes)

### Via Web Interface:
- [ ] Go to github.com and log in
- [ ] Click "+" → "New repository"
- [ ] Name: `yokos-launcher`
- [ ] Description: `BB10 Hub Launcher — Notification hub overlay`
- [ ] Visibility: Public (or Private)
- [ ] Check "Add a README file"
- [ ] Add .gitignore: Android
- [ ] Click "Create repository"
- [ ] Note the repository URL (copy it somewhere)

### Via GitHub CLI:
- [ ] Run `gh repo create yokos-launcher --public --description "..."`

**Status: Repository created at `github.com/YOUR_USERNAME/yokos-launcher`**

---

## 💻 Step 2: Clone Repository Locally (2 minutes)

```bash
git clone https://github.com/YOUR_USERNAME/yokos-launcher.git
cd yokos-launcher
```

- [ ] Repository cloned to local machine
- [ ] Current directory is `yokos-launcher`
- [ ] `git remote -v` shows your repo URL

---

## 📥 Step 3: Download All Files (10 minutes)

From the previous conversation where I provided `present_files`:

- [ ] Download all 22 files to a folder (e.g., `~/bb10-files`)
- [ ] Files downloaded include:
  - [ ] 4 markdown files (documentation)
  - [ ] 3 Kotlin files (.kt)
  - [ ] 3 Gradle config files
  - [ ] 11 resource files (colors, strings, styles, animations)
  - [ ] 1 GitHub Actions workflow (.yml)
  - [ ] 1 ProGuard rules file
  - [ ] 1 .gitignore

**Option A - Manual Copy:**
```bash
# Copy all files to your repo
cp ~/bb10-files/* ~/yokos-launcher/
```

**Option B - Automated:**
```bash
chmod +x setup.sh
./setup.sh
```

- [ ] All files copied to repository
- [ ] setup.sh executed (if using Option B)

---

## 🗂️ Step 4: Verify Directory Structure (5 minutes)

Check these files/directories exist:

```bash
cd ~/yokos-launcher
```

### Root Level Files:
- [ ] `build.gradle` (root)
- [ ] `settings.gradle`
- [ ] `.gitignore`
- [ ] `README.md`
- [ ] `GITHUB_SETUP.md`
- [ ] `QUICK_REFERENCE.md`
- [ ] `FILE_INVENTORY.md`
- [ ] `COMPLETE_SETUP_GUIDE.md` (this file)

### App Directory:
- [ ] `app/build.gradle`
- [ ] `app/proguard-rules.pro`

### Source Code:
- [ ] `app/src/main/java/app/lawnchair/bb10hub/HubNotificationService.kt`
- [ ] `app/src/main/java/app/lawnchair/bb10hub/Bb10HubActivity.kt`
- [ ] `app/src/main/java/app/lawnchair/bb10hub/BB10HubScreen.kt`

### Resources:
- [ ] `app/src/main/res/values/colors.xml`
- [ ] `app/src/main/res/values/strings.xml`
- [ ] `app/src/main/res/values/styles.xml`
- [ ] `app/src/main/res/anim/hub_enter.xml`
- [ ] `app/src/main/res/anim/hub_exit.xml`
- [ ] `app/src/main/res/anim/hub_close_enter.xml`
- [ ] `app/src/main/res/anim/hub_close_exit.xml`

### GitHub Actions:
- [ ] `.github/workflows/build-bb10.yml`

### Manifest:
- [ ] `app/src/main/AndroidManifest.xml`

**Verification Command:**
```bash
find . -type f \( -name "*.kt" -o -name "*.gradle" -o -name "*.xml" \) | wc -l
# Should show: 15+
```

- [ ] Directory structure verified and correct

---

## 🔐 Step 5: Create Debug Keystore (3 minutes)

From repository root:

```bash
keytool -genkey -v -keystore debug.keystore \
  -keyalg RSA \
  -keysize 2048 \
  -validity 10000 \
  -alias androiddebugkey \
  -storepass android \
  -keypass android \
  -dname "CN=Android Debug,O=Android,C=US"
```

- [ ] Command executed successfully
- [ ] `debug.keystore` file created in repo root
- [ ] Verify with:
  ```bash
  ls -la debug.keystore
  keytool -list -v -keystore debug.keystore -storepass android
  ```
- [ ] Output shows `androiddebugkey` alias
- [ ] Keystore file is ~2-3 KB

---

## 📝 Step 6: Commit to GitHub (5 minutes)

```bash
cd ~/yokos-launcher

# Check status
git status

# Add all files
git add .

# Commit
git commit -m "feat: Initial BB10 Hub Launcher setup

- Add Kotlin source files and Compose UI
- Configure Gradle with JDK 17, SDK 33
- Add GitHub Actions CI/CD workflow
- Include resource files and animations
- Add debug keystore for signing"

# Push to main branch
git push origin main
```

- [ ] `git status` shows correct files
- [ ] `git commit` completes successfully
- [ ] `git push origin main` completes (may take 10 seconds)
- [ ] No errors during push

---

## ⚙️ Step 7: Watch First Build (5-10 minutes)

Go to GitHub web interface:

```
github.com/YOUR_USERNAME/yokos-launcher
```

- [ ] Navigate to **Actions** tab
- [ ] See **"Build BB10 Hub APK"** workflow running
- [ ] Watch live logs show:
  - [ ] ✅ Checkout Repository
  - [ ] ✅ Set up JDK 17 (Zulu)
  - [ ] ✅ Validate Gradle Wrapper
  - [ ] ✅ Build Debug APK (takes ~2-3 min)
  - [ ] ✅ Upload APK to Artifacts

**Wait 3-5 minutes total for build to complete**

- [ ] Green checkmark ✅ appears on workflow
- [ ] Build completed successfully

---

## 📥 Step 8: Download APK (2 minutes)

From GitHub Actions:

```
github.com/YOUR_USERNAME/yokos-launcher/actions
```

- [ ] Click latest workflow run (with green checkmark)
- [ ] Scroll to **"Artifacts"** section
- [ ] Click **"bb10-hub-lawnchair12-debug"** to download (ZIP file)
- [ ] Extract ZIP file:
  ```bash
  unzip bb10-hub-lawnchair12-debug.zip
  ```
- [ ] APK file extracted: `app-debug.apk` (should be ~50-80 MB)

---

## 📱 Step 9: Install on Device (5 minutes)

### Connect Device:
```bash
adb devices
```

- [ ] Device appears in list (ADB connected)
- [ ] Device ID shows (e.g., QV781GJ38K)

### Install APK:
```bash
adb install -r app-debug.apk
```

- [ ] Installation completes with "Success"

### Grant Permissions:
```bash
adb shell cmd notification allow_listener \
  app.lawnchair.debug/app.lawnchair.bb10hub.HubNotificationService
```

- [ ] Command executes without errors

### (Optional) Set as Default Launcher:
```bash
adb shell cmd package set-home-activity \
  app.lawnchair.debug/.LauncherActivity
```

- [ ] Command executes (shows confirmation)

### Verify Installation:
```bash
adb shell pm list packages | grep lawnchair
```

- [ ] Output shows: `package:app.lawnchair.debug`

---

## 🧪 Step 10: Test on Device (5 minutes)

On your physical device:

1. **Unlock device**
2. **Open home screen** (or Lawnchair launcher)
3. **Swipe left** from the home screen

- [ ] Hub overlay slides in from left ✅
- [ ] Tab bar visible at top (All, Instagram, Messages, Phone, Gmail)
- [ ] Notifications display with colored stripe on left
- [ ] Notifications are sorted by time (newest first)

### Test Actions:
- [ ] Tap a notification to expand
- [ ] 3 action buttons appear (✓ Mark Read, ⏱ Snooze, ✕ Dismiss)
- [ ] Click tab to filter notifications
- [ ] Swipe right or tap back to close hub

**Status: ✅ BB10 Hub is working on your device!**

---

## 🔄 Step 11: Development Workflow Setup (1 minute)

You're now ready to develop! Here's your workflow:

```bash
# 1. Make code changes
nano app/src/main/java/app/lawnchair/bb10hub/BB10HubScreen.kt

# 2. Commit and push
git add .
git commit -m "feat: Describe your changes"
git push origin main

# 3. Wait for GitHub build (~3-5 min)
# 4. Download APK from Actions → Artifacts
# 5. Install: adb install -r app-debug.apk
# 6. Test on device
```

- [ ] Understand the workflow (no local build needed)
- [ ] Know where to get latest APK (GitHub Artifacts)
- [ ] Know how to test on device (adb install + swipe left)

---

## 📊 Final Verification (2 minutes)

Run these commands to verify everything is set up:

```bash
# Check git setup
git remote -v  # Shows your repo
git log --oneline  # Shows your commits

# Check files organized correctly
find app/src -name "*.kt" | wc -l  # Should be 3
find app/src/main/res -name "*.xml" | wc -l  # Should be 11

# Check GitHub
# Visit: github.com/YOUR_USERNAME/yokos-launcher
# Click Actions tab - should show 1 successful build
```

- [ ] `git remote -v` shows your repository URL
- [ ] At least 1 commit visible in `git log`
- [ ] 3 Kotlin files found in app/src
- [ ] 11+ XML resource files found
- [ ] GitHub Actions shows ✅ Build 1 successful
- [ ] APK downloaded and installed on device
- [ ] Hub works on device (swipe left opens it)

---

## ✨ You're Done! 🎉

You have successfully:

✅ Created GitHub repository  
✅ Organized all source files  
✅ Created signing keystore  
✅ Pushed code to GitHub  
✅ GitHub Actions auto-built APK  
✅ Downloaded and installed APK  
✅ Tested on device  
✅ Set up development workflow  

---

## 🚀 Next Steps

Now that everything is set up:

1. **Make improvements** to the code
2. **Push to GitHub** and let it build
3. **Download APK** from Artifacts
4. **Test on device** and iterate

You now have a complete development pipeline with:
- Zero local build power needed
- Automatic CI/CD builds
- Easy artifact downloads
- Quick device testing

### Useful Commands to Remember

```bash
# Show git history
git log --oneline -10

# See what changed
git diff

# Check device connection
adb devices

# Install APK
adb install -r app-debug.apk

# View logs
adb logcat | grep "BB10\|Hub"

# Open hub on device
# Swipe left from home screen
```

---

## 📞 Troubleshooting Reference

| Problem | Solution |
|---------|----------|
| Build fails in GitHub | Check Actions logs at `Actions` tab → Failed run |
| APK won't install | `adb uninstall app.lawnchair.debug` then retry |
| No notifications appear | `adb shell cmd notification allow_listener app.lawnchair.debug/...` |
| Hub doesn't open | Set as default launcher: `adb shell cmd package set-home-activity app.lawnchair.debug/.LauncherActivity` |
| Forgot repository URL | `git remote -v` |
| Need to see file structure | `find . -type f -name "*.kt" -o -name "*.xml"` |

---

## 📚 Reference Documents

You now have these files in your repository:

1. **COMPLETE_SETUP_GUIDE.md** (this file) — Detailed step-by-step
2. **QUICK_REFERENCE.md** — Daily development commands
3. **GITHUB_SETUP.md** — GitHub-specific workflow
4. **README.md** — Project overview
5. **FILE_INVENTORY.md** — Where each file goes
6. **QUICK_REFERENCE.md** — Emergency commands

Read them when you need help!

---

**Happy coding! 🚀**

Last updated: [Current Date]
BB10 Hub Launcher - Complete Setup
