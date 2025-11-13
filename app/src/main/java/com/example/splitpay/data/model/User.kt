package com.example.splitpay.data.model

/**
 * Represents a user account in the SplitPay application.
 *
 * This model corresponds to documents in the Firestore `/users` collection.
 * Each user has a unique UID from Firebase Authentication, along with profile
 * information and social connections (friends and blocked users).
 *
 * @property uid Unique user ID from Firebase Authentication (document ID)
 * @property fullName User's full name (e.g., "John Doe")
 * @property username Unique username for user discovery and display
 * @property email User's email address (from Firebase Auth)
 * @property phoneNumber User's phone number with country code (e.g., "+1234567890")
 * @property profilePictureUrl Firebase Storage URL for the user's profile picture
 * @property qrCodeUrl Firebase Storage URL for the user's QR code (contains username)
 * @property createdAt Account creation timestamp in milliseconds
 * @property friends List of friend UIDs (bilateral friendship - both users must have each other)
 * @property blockedUsers List of blocked user UIDs (prevents friend requests and visibility)
 * @property deletionScheduledAt Timestamp when account deletion was requested (null if not scheduled).
 *                               Account will be permanently deleted 30 days after this timestamp.
 *                               Logging in before deletion cancels the scheduled deletion.
 */
data class User(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String = "",
    val qrCodeUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val friends: List<String> = emptyList(),
    val blockedUsers: List<String> = emptyList(),
    val deletionScheduledAt: Long? = null
)