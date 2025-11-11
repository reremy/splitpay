package com.example.splitpay.data.repository

import android.net.Uri
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.example.splitpay.logger.logI
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing file uploads and downloads in Firebase Storage.
 *
 * This repository handles all image-related operations including:
 * - Uploading profile pictures, QR codes, group photos, and expense/payment receipts
 * - Generating unique filenames with timestamps to prevent collisions
 * - Obtaining public download URLs for uploaded files
 * - Deleting files from storage
 *
 * **Storage Structure:**
 * ```
 * gs://[bucket]/
 * ├── profile_pictures/[userId]/profile_[userId]_[timestamp].jpg
 * ├── qr_codes/[userId]/qr_[userId]_[timestamp].jpg
 * ├── group_photos/[groupId]/group_[groupId]_[timestamp].jpg
 * ├── expense_images/[expenseId]/expense_[expenseId]_[timestamp].jpg
 * └── payment_images/payment_[timestamp].jpg
 * ```
 *
 * **File Naming Convention:**
 * All filenames include timestamps (System.currentTimeMillis()) to ensure uniqueness
 * and allow multiple versions of the same image type per entity.
 */
class FileStorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    private val storageRef: StorageReference = storage.reference

    /**
     * Uploads a profile picture for a user to Firebase Storage.
     *
     * The image is stored in a user-specific directory at:
     * `profile_pictures/[userId]/profile_[userId]_[timestamp].jpg`
     *
     * This allows users to change their profile picture multiple times, with each
     * version stored separately. The old profile picture URL should be deleted
     * from Firestore when updating to a new one.
     *
     * @param userId The user's UID (from Firebase Authentication)
     * @param imageUri The local URI of the image to upload (e.g., from gallery or camera)
     * @return Result with the public download URL on success, or exception on failure
     */
    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): Result<String> {
        return try {
            logI("Starting profile picture upload for user: $userId")
            logD("Storage bucket: ${storage.reference.bucket}")
            logD("Image URI: $imageUri")

            val fileName = "profile_${userId}_${System.currentTimeMillis()}.jpg"
            val profilePicRef = storageRef.child("profile_pictures/$userId/$fileName")

            logD("Full storage path: ${profilePicRef.path}")
            logD("Storage bucket: ${profilePicRef.bucket}")

            // Upload the file
            logD("Starting upload to Firebase Storage...")
            val uploadTask = profilePicRef.putFile(imageUri).await()
            logI("Upload completed successfully - ${uploadTask.bytesTransferred} bytes transferred")

            // Get the download URL
            logD("Getting download URL...")
            val downloadUrl = profilePicRef.downloadUrl.await().toString()
            logI("Profile picture uploaded successfully. URL: ${downloadUrl.take(50)}...")

            Result.success(downloadUrl)
        } catch (e: Exception) {
            logE("Failed to upload profile picture for user $userId: ${e.message}", e)
            logE("Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Uploads a QR code image for a user to Firebase Storage.
     *
     * QR codes are generated client-side and contain the user's username for
     * easy friend discovery via QR scanning. The image is stored at:
     * `qr_codes/[userId]/qr_[userId]_[timestamp].jpg`
     *
     * Each user typically has one QR code, but regenerating the QR code
     * (e.g., when username changes) will create a new file.
     *
     * @param userId The user's UID (from Firebase Authentication)
     * @param imageUri The local URI of the generated QR code image
     * @return Result with the public download URL on success, or exception on failure
     */
    suspend fun uploadQrCode(userId: String, imageUri: Uri): Result<String> {
        return try {
            logI("Starting QR code upload for user: $userId")
            logD("Storage bucket: ${storage.reference.bucket}")
            logD("Image URI: $imageUri")

            val fileName = "qr_${userId}_${System.currentTimeMillis()}.jpg"
            val qrCodeRef = storageRef.child("qr_codes/$userId/$fileName")

            logD("Full storage path: ${qrCodeRef.path}")
            logD("Storage bucket: ${qrCodeRef.bucket}")

            // Upload the file
            logD("Starting upload to Firebase Storage...")
            val uploadTask = qrCodeRef.putFile(imageUri).await()
            logI("Upload completed successfully - ${uploadTask.bytesTransferred} bytes transferred")

            // Get the download URL
            logD("Getting download URL...")
            val downloadUrl = qrCodeRef.downloadUrl.await().toString()
            logI("QR code uploaded successfully. URL: ${downloadUrl.take(50)}...")

            Result.success(downloadUrl)
        } catch (e: Exception) {
            logE("Failed to upload QR code for user $userId: ${e.message}", e)
            logE("Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Uploads a custom group photo to Firebase Storage.
     *
     * Group photos provide visual identification for groups beyond the default icon.
     * The image is stored at: `group_photos/[groupId]/group_[groupId]_[timestamp].jpg`
     *
     * Users can change the group photo multiple times. The old photo URL should be
     * deleted when updating to a new one to save storage space.
     *
     * @param groupId The group's unique ID (Firestore document ID)
     * @param imageUri The local URI of the image to upload (e.g., from gallery)
     * @return Result with the public download URL on success, or exception on failure
     */
    suspend fun uploadGroupPhoto(groupId: String, imageUri: Uri): Result<String> {
        return try {
            logI("Starting group photo upload for group: $groupId")
            logD("Image URI: $imageUri")

            val fileName = "group_${groupId}_${System.currentTimeMillis()}.jpg"
            val groupPhotoRef = storageRef.child("group_photos/$groupId/$fileName")

            logD("Full storage path: ${groupPhotoRef.path}")

            // Upload the file
            logD("Starting upload to Firebase Storage...")
            val uploadTask = groupPhotoRef.putFile(imageUri).await()
            logI("Upload completed successfully - ${uploadTask.bytesTransferred} bytes transferred")

            // Get the download URL
            logD("Getting download URL...")
            val downloadUrl = groupPhotoRef.downloadUrl.await().toString()
            logI("Group photo uploaded successfully. URL: ${downloadUrl.take(50)}...")

            Result.success(downloadUrl)
        } catch (e: Exception) {
            logE("Failed to upload group photo for group $groupId: ${e.message}", e)
            logE("Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Uploads an expense receipt/bill image to Firebase Storage.
     *
     * Receipt images provide proof of purchase and help users remember what
     * an expense was for. The image is stored at:
     * `expense_images/[expenseId]/expense_[expenseId]_[timestamp].jpg`
     *
     * Each expense typically has one receipt image. If the expense is deleted,
     * the associated image should also be deleted to save storage space.
     *
     * @param expenseId The expense's unique ID (Firestore document ID)
     * @param imageUri The local URI of the receipt image to upload
     * @return Result with the public download URL on success, or exception on failure
     */
    suspend fun uploadExpenseImage(expenseId: String, imageUri: Uri): Result<String> {
        return try {
            logI("Starting expense image upload for expense: $expenseId")
            logD("Image URI: $imageUri")

            val fileName = "expense_${expenseId}_${System.currentTimeMillis()}.jpg"
            val expenseImageRef = storageRef.child("expense_images/$expenseId/$fileName")

            logD("Full storage path: ${expenseImageRef.path}")

            // Upload the file
            logD("Starting upload to Firebase Storage...")
            val uploadTask = expenseImageRef.putFile(imageUri).await()
            logI("Upload completed successfully - ${uploadTask.bytesTransferred} bytes transferred")

            // Get the download URL
            logD("Getting download URL...")
            val downloadUrl = expenseImageRef.downloadUrl.await().toString()
            logI("Expense image uploaded successfully. URL: ${downloadUrl.take(50)}...")

            Result.success(downloadUrl)
        } catch (e: Exception) {
            logE("Failed to upload expense image for expense $expenseId: ${e.message}", e)
            logE("Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Uploads a payment proof/receipt image to Firebase Storage.
     *
     * Payment images serve as proof that a settlement payment was made
     * (e.g., screenshot of bank transfer, photo of cash exchange).
     * The image is stored at: `payment_images/payment_[timestamp].jpg`
     *
     * Note: Unlike other upload methods, this doesn't use an entity ID since
     * the payment might not exist in Firestore yet. The timestamp serves as
     * a unique identifier.
     *
     * @param imageUri The local URI of the payment proof image to upload
     * @return Result with the public download URL on success, or exception on failure
     */
    suspend fun uploadPaymentImage(imageUri: Uri): Result<String> {
        return try {
            logI("Starting payment image upload")
            logD("Image URI: $imageUri")

            val paymentId = "payment_${System.currentTimeMillis()}"
            val fileName = "${paymentId}.jpg"
            val paymentImageRef = storageRef.child("payment_images/$fileName")

            logD("Full storage path: ${paymentImageRef.path}")

            // Upload the file
            logD("Starting upload to Firebase Storage...")
            val uploadTask = paymentImageRef.putFile(imageUri).await()
            logI("Upload completed successfully - ${uploadTask.bytesTransferred} bytes transferred")

            // Get the download URL
            logD("Getting download URL...")
            val downloadUrl = paymentImageRef.downloadUrl.await().toString()
            logI("Payment image uploaded successfully. URL: ${downloadUrl.take(50)}...")

            Result.success(downloadUrl)
        } catch (e: Exception) {
            logE("Failed to upload payment image: ${e.message}", e)
            logE("Exception type: ${e.javaClass.simpleName}")
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Deletes a file from Firebase Storage given its download URL.
     *
     * This is typically called when:
     * - A user changes their profile picture (delete old one)
     * - A group photo is updated (delete old one)
     * - An expense or payment is deleted (cleanup associated image)
     *
     * If the URL is empty, this method succeeds without making a network call.
     * If the file doesn't exist, Firebase Storage returns a failure.
     *
     * @param fileUrl The full public download URL of the file to delete (e.g., "https://firebasestorage.googleapis.com/...")
     * @return Result indicating success or failure (including if file not found)
     */
    suspend fun deleteFile(fileUrl: String): Result<Unit> {
        return try {
            if (fileUrl.isEmpty()) {
                return Result.success(Unit)
            }

            logD("Attempting to delete file: ${fileUrl.take(50)}...")
            val fileRef = storage.getReferenceFromUrl(fileUrl)
            fileRef.delete().await()
            logI("File deleted successfully")

            Result.success(Unit)
        } catch (e: Exception) {
            logE("Failed to delete file: ${e.message}", e)
            Result.failure(e)
        }
    }
}
