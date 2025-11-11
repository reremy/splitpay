# Development Setup Guide

Complete guide to setting up the SplitPay development environment.

---

## Prerequisites

### Required Software

1. **Android Studio**
   - Version: Ladybug (2024.2.1) or later
   - Download: [Android Studio](https://developer.android.com/studio)

2. **Java Development Kit (JDK)**
   - Version: JDK 11 or higher
   - Usually bundled with Android Studio
   - Verify: `java -version`

3. **Android SDK**
   - Min SDK: API 24 (Android 7.0)
   - Target SDK: API 36
   - Installed via Android Studio SDK Manager

4. **Git**
   - Version control
   - Download: [Git](https://git-scm.com/downloads)

5. **Firebase Account**
   - Create at [Firebase Console](https://console.firebase.google.com/)
   - Free tier is sufficient for development

### Recommended Tools

- **Android Device or Emulator**
  - Physical device with USB debugging enabled, OR
  - Android Emulator (API 24+)

- **IDE Plugins**
  - Kotlin plugin (bundled with Android Studio)
  - Firebase plugin (optional, for easier Firebase integration)

---

## Step 1: Clone the Repository

```bash
# Clone the repository
git clone <repository-url>
cd splitpay

# Checkout the appropriate branch
git checkout main
```

---

## Step 2: Firebase Setup

### 2.1 Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add project"
3. Enter project name (e.g., "SplitPay Dev")
4. Disable Google Analytics (optional for dev)
5. Click "Create project"

### 2.2 Register Android App

1. In Firebase Console, click "Add app" → Android
2. Enter package name: `com.example.splitpay`
3. Enter app nickname: "SplitPay Android"
4. Click "Register app"

### 2.3 Download Configuration File

1. Download `google-services.json`
2. Place it in: `splitpay/app/google-services.json`

**Important**: This file contains sensitive configuration. It's already in `.gitignore`.

### 2.4 Enable Firebase Services

#### Authentication

1. In Firebase Console → Authentication
2. Click "Get started"
3. Enable "Email/Password" provider
4. Click "Save"

#### Cloud Firestore

1. In Firebase Console → Firestore Database
2. Click "Create database"
3. Select "Start in test mode" (for development)
4. Choose a location (preferably closest to your region)
5. Click "Enable"

**Security Rules for Development**:
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      // DEVELOPMENT ONLY - Allow all authenticated users
      allow read, write: if request.auth != null;
    }
  }
}
```

**Production Security Rules**: See [FIRESTORE_SCHEMA.md](FIRESTORE_SCHEMA.md) for production-ready rules.

#### Firebase Storage

1. In Firebase Console → Storage
2. Click "Get started"
3. Start in test mode
4. Choose same location as Firestore
5. Click "Done"

**Storage Rules for Development**:
```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      // DEVELOPMENT ONLY - Allow authenticated users
      allow read, write: if request.auth != null;
    }
  }
}
```

**Production Storage Rules**: See [FIREBASE_STORAGE_SETUP.md](../FIREBASE_STORAGE_SETUP.md).

#### Firebase Analytics (Optional)

- Enabled by default
- No additional setup required for basic analytics

---

## Step 3: Open Project in Android Studio

1. Launch Android Studio
2. Click "Open"
3. Navigate to the cloned `splitpay` directory
4. Click "OK"

### First-Time Setup

Android Studio will automatically:
- Sync Gradle files
- Download dependencies
- Index the project

**This may take 5-10 minutes on first run.**

### Gradle Sync Issues?

If Gradle sync fails:

1. Check internet connection
2. Verify `google-services.json` is in `app/` folder
3. Try: **File → Invalidate Caches → Invalidate and Restart**
4. Check Gradle JDK: **Settings → Build Tools → Gradle → Gradle JDK** (should be JDK 11+)

---

## Step 4: Configure Build Variants

### Debug Build (Default)

- Used for development
- ProGuard/R8 disabled
- Debugging enabled
- Faster build times

### Release Build

- For production
- ProGuard/R8 enabled (code shrinking)
- Debugging disabled
- Requires signing configuration (see [DEPLOYMENT.md](DEPLOYMENT.md))

**Select Build Variant**: **Build → Select Build Variant → debug**

---

## Step 5: Verify Dependencies

Check that all dependencies are downloaded:

**File → Project Structure → Dependencies**

Key dependencies should include:
- Kotlin stdlib
- AndroidX libraries (Core, Compose, Navigation, etc.)
- Firebase BOM and services
- Coil (image loading)
- Coroutines

If any are missing, sync Gradle again.

---

## Step 6: Set Up an Android Device/Emulator

### Option A: Physical Device

1. Enable Developer Options on your Android device:
   - Settings → About Phone
   - Tap "Build Number" 7 times
2. Enable USB Debugging:
   - Settings → Developer Options → USB Debugging
3. Connect device via USB
4. Accept debugging prompt on device
5. Verify device appears in Android Studio device dropdown

### Option B: Android Emulator

1. **Tools → Device Manager**
2. Click "Create Device"
3. Select a device definition (e.g., Pixel 5)
4. Select a system image:
   - Recommended: API 34 (Android 14) or API 36
   - Download if not already installed
5. Click "Finish"
6. Launch the emulator

**Emulator Tips**:
- Use x86/x86_64 images for better performance
- Enable hardware acceleration (HAXM on Intel, WHPX on Windows)
- Allocate at least 2GB RAM to emulator

---

## Step 7: Build and Run

### First Build

1. Select the device/emulator from the dropdown
2. Click the "Run" button (green play icon) or press **Shift + F10**
3. Android Studio will:
   - Build the APK
   - Install on device/emulator
   - Launch the app

**Expected build time**: 1-3 minutes (first build), 10-30 seconds (incremental)

### Verify App Launch

You should see:
1. Welcome screen with "Get Started" button
2. Login/Sign Up options
3. Ability to create an account

---

## Step 8: Test Firebase Connection

### Create a Test Account

1. Launch the app
2. Tap "Get Started"
3. Tap "Sign Up"
4. Fill in test details:
   - Full Name: Test User
   - Username: testuser
   - Email: test@example.com
   - Phone: +1234567890
   - Password: Test123!
5. Tap "Sign Up"

### Verify in Firebase Console

1. Go to Firebase Console → Authentication
2. You should see the test user in the "Users" tab
3. Go to Firestore Database
4. You should see a new document in the `users` collection

**If successful**: Firebase is properly configured!

---

## Common Setup Issues

### Issue: "google-services.json not found"

**Solution**:
- Verify `google-services.json` is in `app/` directory (not root)
- Sync Gradle again

### Issue: "Default FirebaseApp is not initialized"

**Solution**:
- Check `google-services.json` is correct
- Verify package name in Firebase matches `com.example.splitpay`
- Clean and rebuild: **Build → Clean Project → Build → Rebuild Project**

### Issue: Firestore permission denied

**Solution**:
- Check Firestore security rules (should allow authenticated users in dev)
- Verify user is signed in
- Check Firebase Console → Firestore → Rules

### Issue: Build fails with "OutOfMemoryError"

**Solution**:
Edit `gradle.properties`:
```properties
org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8
```

### Issue: Emulator is very slow

**Solution**:
- Use x86_64 system images (faster than ARM)
- Enable hardware acceleration
- Allocate more RAM to emulator
- Consider using a physical device

### Issue: "SDK location not found"

**Solution**:
Create `local.properties` in project root:
```properties
sdk.dir=/path/to/Android/Sdk
```
(Usually auto-generated by Android Studio)

---

## Environment Variables

### Optional Configuration

For advanced configuration, you can set environment variables:

**gradle.properties** (project-level):
```properties
# JVM memory settings
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m -Dfile.encoding=UTF-8

# Kotlin
kotlin.code.style=official

# AndroidX
android.useAndroidX=true
android.enableJetifier=true

# Build performance
org.gradle.parallel=true
org.gradle.caching=true
```

---

## Debugging Tools

### Android Studio Debugger

1. Set breakpoints in code (click left margin)
2. Run in debug mode: **Run → Debug 'app'** or **Shift + F9**
3. Use debugging panel to inspect variables

### Logcat

View app logs:
- **View → Tool Windows → Logcat**
- Filter by package: `com.example.splitpay`
- Filter by log level: Debug, Info, Warn, Error

### Database Inspector

View Firestore data in real-time:
- Use Firebase Console → Firestore Database
- Or use **App Inspection → Database Inspector** (Android Studio)

### Layout Inspector

Inspect Compose UI hierarchy:
- **Tools → Layout Inspector**
- Select running app
- Explore component tree and properties

---

## Next Steps

After successful setup:

1. **Explore the codebase**: See [ARCHITECTURE.md](ARCHITECTURE.md)
2. **Understand data models**: See [DATA_MODELS.md](DATA_MODELS.md)
3. **Review features**: See [FEATURES.md](FEATURES.md)
4. **Write tests**: See [TESTING.md](TESTING.md)
5. **Start contributing**: See [CONTRIBUTING.md](../CONTRIBUTING.md)

---

## Development Workflow

### Daily Workflow

1. Pull latest changes: `git pull origin main`
2. Sync Gradle if needed
3. Run tests: `./gradlew test`
4. Make changes
5. Test locally on device/emulator
6. Commit and push

### Before Committing

1. Run lint checks: **Analyze → Inspect Code**
2. Format code: **Code → Reformat Code** (Ctrl+Alt+L)
3. Run tests: `./gradlew test`
4. Test on emulator/device

---

## Additional Resources

- [Android Developer Guide](https://developer.android.com/guide)
- [Jetpack Compose Documentation](https://developer.android.com/jetpack/compose)
- [Firebase Documentation](https://firebase.google.com/docs)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Material Design 3](https://m3.material.io/)

---

## Getting Help

If you encounter issues:

1. Check this documentation
2. Search existing GitHub issues
3. Check Firebase Console for errors
4. Review Logcat output
5. Ask in project discussions/chat
6. Create a new GitHub issue with:
   - Clear description
   - Steps to reproduce
   - Error messages/logs
   - Environment details (OS, Android Studio version, etc.)

---

**Happy coding!** 🚀
