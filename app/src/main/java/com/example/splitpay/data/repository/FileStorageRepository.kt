package com.example.splitpay.data.repository

import android.net.Uri
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.example.splitpay.logger.logI
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import kotlinx.coroutines.tasks.await

class FileStorageRepository(
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
) {

    private val storageRef: StorageReference = storage.reference

    /**
     * Uploads a profile picture for a user
     * @param userId The user's UID
     * @param imageUri The local URI of the image to upload
     * @return The download URL of the uploaded image, or null if failed
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
     * Uploads a QR code image for a user
     * @param userId The user's UID
     * @param imageUri The local URI of the QR code image to upload
     * @return The download URL of the uploaded image, or null if failed
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
     * Uploads a group photo
     * @param groupId The group's ID
     * @param imageUri The local URI of the image to upload
     * @return The download URL of the uploaded image
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
     * Uploads an expense image
     * @param expenseId The expense's ID
     * @param imageUri The local URI of the image to upload
     * @return The download URL of the uploaded image
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
     * Uploads a payment image to Firebase Storage
     * @param imageUri The URI of the image to upload
     * @return The download URL of the uploaded image
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
     * Deletes a file from Firebase Storage given its URL
     * @param fileUrl The full download URL of the file to delete
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
