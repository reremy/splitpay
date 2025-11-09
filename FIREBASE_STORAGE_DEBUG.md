# Firebase Storage Upload Error: "object does not exist at location"

## Error Description
You're getting the error: **"Failed to upload profile picture/QR code: object does not exist at location"**

This error means **Firebase Storage is NOT enabled** in your Firebase project.

---

## Solution: Enable Firebase Storage

### Step 1: Go to Firebase Console
1. Open [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **splitpay-75dd3**

### Step 2: Enable Firebase Storage
1. In the left sidebar, click **Build** → **Storage**
2. Click **Get Started** button
3. You'll see a dialog about security rules:
   - Click **Next**
4. Choose a Cloud Storage location:
   - Select **us-central1** (or your preferred region)
   - Click **Done**

### Step 3: Update Security Rules
Once Storage is enabled, update the rules:

1. Click on the **Rules** tab
2. Replace with this:

```
rules_version = '2';

service firebase.storage {
  match /b/{bucket}/o {

    // Profile Pictures - only authenticated users can upload their own profile picture
    match /profile_pictures/{userId}/{allPaths=**} {
      allow read: if true; // Anyone can read profile pictures
      allow write: if request.auth != null && request.auth.uid == userId;
      allow delete: if request.auth != null && request.auth.uid == userId;
    }

    // QR Codes - only authenticated users can upload their own QR code
    match /qr_codes/{userId}/{allPaths=**} {
      allow read: if true; // Anyone can read QR codes
      allow write: if request.auth != null && request.auth.uid == userId;
      allow delete: if request.auth != null && request.auth.uid == userId;
    }

    // Deny all other paths by default
    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
```

3. Click **Publish**

---

## Debugging Steps

### 1. Check Logcat for Detailed Errors

Run this command to see detailed logs:

```bash
adb logcat | grep -E "(FileStorage|Upload|Storage)"
```

Look for these log messages:
- `Storage bucket: splitpay-75dd3.firebasestorage.app`
- `Full storage path: profile_pictures/...`
- `Starting upload to Firebase Storage...`
- `Exception type: ...`

### 2. What the Logs Tell You

**If you see:**
```
Storage bucket: splitpay-75dd3.firebasestorage.app
Full storage path: profile_pictures/USER_ID/profile_USER_ID_123456.jpg
Starting upload to Firebase Storage...
Failed to upload profile picture: object does not exist at location
```

**This means:** Firebase Storage is **NOT ENABLED** in Firebase Console. Follow Step 2 above.

---

**If you see:**
```
Storage bucket: splitpay-75dd3.firebasestorage.app
Full storage path: profile_pictures/USER_ID/profile_USER_ID_123456.jpg
Starting upload to Firebase Storage...
Upload completed successfully - 12345 bytes transferred
Getting download URL...
Failed to upload profile picture: object does not exist at location
```

**This means:**
- The upload succeeded but getting the download URL failed
- **Most likely:** Storage rules are blocking read access
- **Fix:** Update the security rules as shown in Step 3

---

**If you see:**
```
Storage bucket: splitpay-75dd3.firebasestorage.app
Full storage path: profile_pictures/USER_ID/profile_USER_ID_123456.jpg
Starting upload to Firebase Storage...
Failed to upload profile picture: Permission denied
```

**This means:** Storage rules are blocking the upload
- **Fix:** Update the security rules to allow authenticated users to write to their own folder

---

### 3. Verify Storage is Enabled

In Firebase Console → Storage:
- You should see a **Files** tab showing your storage bucket
- The bucket should be: `splitpay-75dd3.firebasestorage.app`
- If you don't see this, Storage is **NOT enabled** - go to Step 2

### 4. Test Again

After enabling Storage and updating rules:

1. **Rebuild and run the app**
   ```bash
   ./gradlew clean build
   ```

2. **Try uploading again**
   - Navigate to Profile → Edit (pencil icon)
   - Select a profile picture
   - Click Save

3. **Check Logcat**
   ```bash
   adb logcat | grep -E "(FileStorage|Upload)"
   ```

You should see:
```
Starting profile picture upload for user: USER_ID
Storage bucket: splitpay-75dd3.firebasestorage.app
Starting upload to Firebase Storage...
Upload completed successfully - XXXXX bytes transferred
Getting download URL...
Profile picture uploaded successfully. URL: https://firebasestorage...
```

---

## Common Issues

### Issue: "Storage bucket is null"
**Cause:** Firebase Storage not initialized properly
**Fix:** Make sure `google-services.json` is up to date. Re-download it from Firebase Console if needed.

### Issue: "Permission denied"
**Cause:** Storage rules are blocking the upload
**Fix:** Update the security rules as shown above

### Issue: "Network error"
**Cause:** No internet connection or Firebase servers unreachable
**Fix:** Check your internet connection

### Issue: "Invalid image URI"
**Cause:** The image picker returned a null or invalid URI
**Fix:** Check the image picker permissions in AndroidManifest.xml

---

## Verify Your Setup

✅ **Checklist:**
- [ ] Firebase Storage is enabled in Firebase Console
- [ ] Storage bucket exists: `splitpay-75dd3.firebasestorage.app`
- [ ] Security rules are published
- [ ] `google-services.json` is up to date
- [ ] App has been rebuilt after enabling Storage
- [ ] User is logged in (check Firebase Auth)
- [ ] Detailed logs show storage bucket name

---

## Still Having Issues?

If it still doesn't work, share the **full Logcat output** when you try to upload:

```bash
adb logcat | grep -E "(FileStorage|Upload|Storage|Exception)" > storage_logs.txt
```

Then check `storage_logs.txt` for the detailed error messages with the new debug logging.
