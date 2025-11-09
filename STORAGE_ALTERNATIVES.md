# Storage Alternatives for SplitPay

## Firebase Storage Free Tier (Recommended)

**You DON'T need to pay!** Firebase Storage has a generous free tier:

- **5 GB** total storage
- **1 GB/day** download bandwidth
- **20,000** uploads/day
- **50,000** downloads/day

**Calculation for your app:**
- Average profile picture: ~100 KB
- Average QR code: ~50 KB
- Total per user: ~150 KB
- **Free tier supports: ~33,000 users** (5 GB ÷ 150 KB)

**You only pay if you exceed these limits.**

---

## Alternative 1: Base64 in Firestore (No Additional Service)

Store images as Base64 strings directly in Firestore User documents.

### Pros
✅ No additional service needed
✅ Already using Firestore
✅ Simple implementation
✅ Images backed up with user data

### Cons
❌ Increases Firestore read/write costs
❌ Slower loading than dedicated storage
❌ 1 MB Firestore document size limit
❌ Not ideal for images > 200 KB

### Implementation

**1. Update FileStorageRepository.kt:**

```kotlin
package com.example.splitpay.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.example.splitpay.logger.logI
import java.io.ByteArrayOutputStream

class FileStorageRepository(
    private val context: Context
) {

    /**
     * Converts image URI to Base64 string
     * Compresses to keep under 200 KB
     */
    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String> {
        return try {
            logI("Converting profile picture to Base64 for user: $userId")

            // Read image from URI
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bitmap = BitmapFactory.decodeStream(inputStream)
            inputStream?.close()

            // Compress to JPEG (quality 80)
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()

            // Check size (max 200 KB to stay under Firestore 1 MB limit)
            if (byteArray.size > 200_000) {
                logE("Image too large: ${byteArray.size} bytes. Max 200 KB")
                return Result.failure(Exception("Image must be under 200 KB"))
            }

            // Convert to Base64
            val base64String = "data:image/jpeg;base64," + Base64.encodeToString(byteArray, Base64.NO_WRAP)

            logI("Image converted successfully - ${byteArray.size} bytes")
            Result.success(base64String)

        } catch (e: Exception) {
            logE("Failed to convert image: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun uploadQrCode(userId: String, imageUri: Uri): Result<String> {
        return uploadProfilePicture(userId, imageUri) // Same logic
    }
}
```

**2. Update User display to handle Base64:**

In `UserProfileScreen.kt` and `EditProfileScreen.kt`, Coil already handles Base64 data URIs automatically:

```kotlin
AsyncImage(
    model = uiState.profilePictureUrl, // Works with Base64 data URIs!
    contentDescription = "Profile Picture",
    modifier = Modifier.size(120.dp).clip(CircleShape)
)
```

**3. Update EditProfileViewModel:**

No changes needed! It already saves the URL (now Base64 string) to Firestore.

---

## Alternative 2: Cloudinary (More Generous Free Tier)

Free tier includes:
- **25 GB** storage
- **25 GB/month** bandwidth
- Image transformations (resize, crop, etc.)

### Setup

1. Sign up at [cloudinary.com](https://cloudinary.com/)
2. Get your Cloud Name, API Key, and API Secret
3. Add dependency to `build.gradle.kts`:

```kotlin
implementation("com.cloudinary:cloudinary-android:2.3.1")
```

4. Update `FileStorageRepository.kt`:

```kotlin
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback

class FileStorageRepository(private val context: Context) {

    init {
        MediaManager.init(context, mapOf(
            "cloud_name" to "YOUR_CLOUD_NAME",
            "api_key" to "YOUR_API_KEY",
            "api_secret" to "YOUR_API_SECRET"
        ))
    }

    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String> {
        return suspendCancellableCoroutine { continuation ->
            MediaManager.get()
                .upload(imageUri)
                .option("folder", "profile_pictures/$userId")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        logD("Upload started: $requestId")
                    }

                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {
                        logD("Upload progress: $bytes/$totalBytes")
                    }

                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as String
                        logI("Upload successful: $url")
                        continuation.resume(Result.success(url))
                    }

                    override fun onError(requestId: String, error: ErrorInfo) {
                        logE("Upload failed: ${error.description}")
                        continuation.resume(Result.failure(Exception(error.description)))
                    }

                    override fun onReschedule(requestId: String, error: ErrorInfo) {
                        logD("Upload rescheduled: ${error.description}")
                    }
                })
                .dispatch()
        }
    }
}
```

---

## Alternative 3: Imgur API (Simple, Free)

Free tier: Unlimited uploads (with rate limits)

```kotlin
class FileStorageRepository(private val context: Context) {

    private val imgurClientId = "YOUR_IMGUR_CLIENT_ID"

    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String> {
        return try {
            // Convert image to Base64
            val inputStream = context.contentResolver.openInputStream(imageUri)
            val bytes = inputStream?.readBytes()
            inputStream?.close()
            val base64Image = Base64.encodeToString(bytes, Base64.DEFAULT)

            // Upload to Imgur
            val client = OkHttpClient()
            val requestBody = FormBody.Builder()
                .add("image", base64Image)
                .build()

            val request = Request.Builder()
                .url("https://api.imgur.com/3/image")
                .addHeader("Authorization", "Client-ID $imgurClientId")
                .post(requestBody)
                .build()

            val response = client.newCall(request).execute()
            val json = JSONObject(response.body?.string() ?: "")
            val imageUrl = json.getJSONObject("data").getString("link")

            Result.success(imageUrl)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## Alternative 4: Local Storage Only (No Cloud)

Store images only on the device. No cloud backup.

```kotlin
class FileStorageRepository(private val context: Context) {

    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String> {
        return try {
            // Copy to internal storage
            val fileName = "profile_$userId.jpg"
            val destinationFile = File(context.filesDir, fileName)

            context.contentResolver.openInputStream(imageUri)?.use { input ->
                destinationFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            // Return local file path
            Result.success("file://${destinationFile.absolutePath}")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
```

---

## Recommendation

**Use Firebase Storage (Free Tier)** because:
1. ✅ You're already using Firebase Auth and Firestore
2. ✅ Free tier is generous (5 GB)
3. ✅ Reliable and fast
4. ✅ No external dependencies
5. ✅ Security rules integration
6. ✅ Automatic image optimization
7. ✅ Only pay if you grow significantly

**Only consider alternatives if:**
- You expect 50,000+ users quickly
- You want to avoid Firebase lock-in
- You have specific image processing needs (Cloudinary)
- You want a completely offline app (local storage)

**For your app's scale, Firebase Storage free tier is perfect!** 🎉
