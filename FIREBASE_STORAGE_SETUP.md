# Firebase Storage Setup Guide

## Issue: "Failed to upload profile picture"

The upload failure is caused by Firebase Storage security rules that prevent unauthorized uploads. By default, Firebase Storage denies all read and write operations.

## Solution: Update Firebase Storage Rules

### Step 1: Open Firebase Console
1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Select your project: **SplitPay**

### Step 2: Navigate to Storage Rules
1. In the left sidebar, click **Storage**
2. Click on the **Rules** tab at the top

### Step 3: Update Security Rules

Replace the existing rules with the following:

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

### Step 4: Publish Rules
1. Click the **Publish** button
2. Confirm the changes

## What These Rules Do

1. **Profile Pictures Path** (`/profile_pictures/{userId}/`)
   - ✅ Anyone can read (view) profile pictures
   - ✅ Only authenticated users can upload to their own folder (userId must match auth.uid)
   - ✅ Only the owner can delete their profile pictures

2. **QR Codes Path** (`/qr_codes/{userId}/`)
   - ✅ Anyone can read (view) QR codes
   - ✅ Only authenticated users can upload to their own folder
   - ✅ Only the owner can delete their QR codes

3. **All Other Paths**
   - ❌ Denied by default for security

## Optional: Add File Size and Type Validation

For additional security, you can add file size and type restrictions:

```
rules_version = '2';

service firebase.storage {
  match /b/{bucket}/o {

    // Profile Pictures with validation
    match /profile_pictures/{userId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null
                   && request.auth.uid == userId
                   && request.resource.size < 5 * 1024 * 1024  // Max 5MB
                   && request.resource.contentType.matches('image/.*'); // Only images
      allow delete: if request.auth != null && request.auth.uid == userId;
    }

    // QR Codes with validation
    match /qr_codes/{userId}/{allPaths=**} {
      allow read: if true;
      allow write: if request.auth != null
                   && request.auth.uid == userId
                   && request.resource.size < 5 * 1024 * 1024  // Max 5MB
                   && request.resource.contentType.matches('image/.*'); // Only images
      allow delete: if request.auth != null && request.auth.uid == userId;
    }

    match /{allPaths=**} {
      allow read, write: if false;
    }
  }
}
```

## Testing After Setup

1. Build and run the app
2. Navigate to Profile → Edit Profile (pencil icon)
3. Try uploading a profile picture
4. Try uploading a QR code
5. Click **Save Changes**

You should now see uploads completing successfully!

## Troubleshooting

### Still getting errors?
1. Check that you published the rules
2. Verify the user is logged in (check Firebase Auth)
3. Check Logcat for detailed error messages:
   ```
   adb logcat | grep -i "storage"
   ```

### Permission denied errors?
- Make sure the userId in the path matches the authenticated user's UID
- Verify the rules were published correctly

### Network errors?
- Check your internet connection
- Verify Firebase Storage is enabled in your project
- Check if you've exceeded Firebase free tier limits
