package com.example.splitpay.data.repository

import android.util.Log
import com.example.splitpay.data.model.User
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.example.splitpay.logger.logI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
){

    private val usersCollection = firestore.collection("users")

    // --- Authentication Functions ---

    suspend fun signUp(
        fullName: String,
        username: String,
        email: String,
        phoneNumber: String,
        password: String,
        ): Result<Unit> {
        return try {
            logI("Starting sign-up process for user: $email")
            logD("Sign-up details - Username: $username, Phone: ${phoneNumber.take(3)}***")

            // Create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: run {
                logE("Firebase Auth user creation failed - user object is null")
                return Result.failure(Exception("User creation failed."))
            }

            logI("Firebase Auth account created successfully - UID: ${user.uid}")

            // Set the Firebase User's display name using the fullName
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            user.updateProfile(profileUpdates).await()
            logD("Firebase User profile updated with displayName: $fullName")

            // Build user data for Firestore
            val userData = User(
                uid = user.uid,
                fullName = fullName,
                username = username,
                email = email,
                phoneNumber = phoneNumber
            )

            // Store in Firestore
            logD("Saving user profile to Firestore for UID: ${user.uid}")
            firestore.collection("users")
                .document(user.uid)
                .set(userData)
                .await()

            logI("User profile saved to Firestore successfully - Sign-up complete for: $email")
            Result.success(Unit)
        } catch (e: Exception){
            logE("Sign-up failed for $email: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): FirebaseUser? {
        return try {
            logI("Starting sign-in process for user: $email")
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user

            if (user != null) {
                logI("Sign-in successful - UID: ${user.uid}, Email verified: ${user.isEmailVerified}")
            } else {
                logE("Sign-in returned null user despite no exception")
            }

            user
        } catch (e: Exception) {
            logE("Sign-in failed for $email: ${e.message}")
            throw e
        }
    }

    // This is the preferred approach
    suspend fun signOut() {
        try {
            val userEmail = auth.currentUser?.email
            logD("Attempting sign out for $userEmail")

            auth.signOut() // No withContext needed

            logI("Successfully signed out user: $userEmail")
        } catch (e: Exception) {
            logE("Sign out failed: ${e.message}")
            throw e // Re-throw so the ViewModel can catch it
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    // --- User Profile Functions ---

    suspend fun createUserProfile(user: User): Result<Unit> {
        return try {
            // Use set with merge to create or update
            usersCollection.document(user.uid).set(user, SetOptions.merge()).await()
            logD("User profile created/updated in Firestore for ${user.uid}")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error creating/updating user profile for ${user.uid}: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserProfile(uid: String): User? {
        return try {
            val document = usersCollection.document(uid).get().await()
            document.toObject(User::class.java)
        } catch (e: Exception) {
            logE("Error getting user profile for $uid: ${e.message}")
            null
        }
    }

    /**
     * Updates user profile fields in Firestore
     * @param uid The user's UID
     * @param updates Map of field names to new values
     * @return Result indicating success or failure
     */
    suspend fun updateUserProfile(uid: String, updates: Map<String, Any>): Result<Unit> {
        return try {
            logI("Updating profile for user: $uid with ${updates.keys.joinToString()}")
            usersCollection.document(uid).update(updates).await()
            logI("Profile updated successfully for user: $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error updating user profile for $uid: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Updates the user's full name
     */
    suspend fun updateFullName(uid: String, fullName: String): Result<Unit> {
        return updateUserProfile(uid, mapOf("fullName" to fullName))
    }

    /**
     * Updates the user's email
     */
    suspend fun updateEmail(uid: String, email: String): Result<Unit> {
        return updateUserProfile(uid, mapOf("email" to email))
    }

    /**
     * Updates the user's phone number
     */
    suspend fun updatePhoneNumber(uid: String, phoneNumber: String): Result<Unit> {
        return updateUserProfile(uid, mapOf("phoneNumber" to phoneNumber))
    }

    /**
     * Updates the user's profile picture URL
     */
    suspend fun updateProfilePicture(uid: String, profilePictureUrl: String): Result<Unit> {
        return updateUserProfile(uid, mapOf("profilePictureUrl" to profilePictureUrl))
    }

    /**
     * Updates the user's QR code URL
     */
    suspend fun updateQrCode(uid: String, qrCodeUrl: String): Result<Unit> {
        return updateUserProfile(uid, mapOf("qrCodeUrl" to qrCodeUrl))
    }

    // Gets the current user's profile, including the friends list field
    suspend fun getCurrentUserProfileWithFriends(): User? {
        val currentUser = getCurrentUser() ?: return null
        return getUserProfile(currentUser.uid)
    }

    // --- Friend Functions ---

    /**
     * Fetches the list of friend UIDs for the currently logged-in user.
     */
    suspend fun getCurrentUserFriendIds(): List<String> {
        return try {
            val profile = getCurrentUserProfileWithFriends()
            logD("Fetched friend IDs: ${profile?.friends}")
            profile?.friends ?: emptyList() // Return the friends list or an empty list
        } catch (e: Exception) {
            logE("Error getting current user's friend IDs: ${e.message}")
            emptyList() // Return empty list on error
        }
    }

    /**
     * Fetches the User profiles for a given list of friend UIDs.
     * Handles Firestore's 'whereIn' query limit by chunking.
     */
    suspend fun getProfilesForFriends(friendUids: List<String>): List<User> {
        if (friendUids.isEmpty()) {
            return emptyList()
        }

        return try {
            // Chunk the list into queries of 10 UIDs max for 'whereIn'
            val friendChunks = friendUids.chunked(10)
            val profiles = mutableListOf<User>()

            for (chunk in friendChunks) {
                if (chunk.isEmpty()) continue // Skip empty chunks if any
                val querySnapshot = usersCollection.whereIn("uid", chunk).get().await()
                profiles.addAll(querySnapshot.toObjects(User::class.java))
            }

            logD("Fetched profiles for ${profiles.size} friends.")
            profiles
        } catch (e: Exception) {
            logE("Error getting friend profiles: ${e.message}")
            emptyList() // Return empty list on error
        }
    }

    /**
     * Adds a friend relationship reciprocally between the current user and the target friend UID.
     * Uses a WriteBatch for atomicity. Returns failure if users are already friends or on error.
     */
    suspend fun addFriend(friendUidToAdd: String): Result<Unit> {
        val currentUser = getCurrentUser() ?: return Result.failure(Exception("User not logged in."))
        val currentUserUid = currentUser.uid

        // Prevent adding self
        if (currentUserUid == friendUidToAdd) {
            return Result.failure(IllegalArgumentException("You cannot add yourself as a friend."))
        }

        // Optional: Check if already friends (arrayUnion handles this, but explicit check gives better feedback)
        val currentUserProfile = getUserProfile(currentUserUid)
        if (currentUserProfile?.friends?.contains(friendUidToAdd) == true) {
            return Result.failure(IllegalArgumentException("You are already friends with this user."))
        }

        return try {
            // Use a WriteBatch for atomicity
            val batch = firestore.batch()

            // 1. Add friendUidToAdd to the current user's friends list
            val currentUserDocRef = usersCollection.document(currentUserUid)
            batch.update(currentUserDocRef, "friends", FieldValue.arrayUnion(friendUidToAdd))

            // 2. Add currentUserUid to the friend's friends list
            val friendUserDocRef = usersCollection.document(friendUidToAdd)
            batch.update(friendUserDocRef, "friends", FieldValue.arrayUnion(currentUserUid))

            // Commit the batch
            batch.commit().await()

            logD("Successfully added friend: $currentUserUid <-> $friendUidToAdd")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error adding friend $friendUidToAdd: ${e.message}")
            // Consider checking specific Firestore exceptions if needed
            Result.failure(e)
        }
    }

    /**
     * Removes a friend relationship between the current user and another user.
     * Both users' friends lists are updated atomically.
     */
    suspend fun removeFriend(friendUidToRemove: String): Result<Unit> {
        val currentUser = getCurrentUser() ?: return Result.failure(Exception("User not logged in."))
        val currentUserUid = currentUser.uid

        // Prevent removing self
        if (currentUserUid == friendUidToRemove) {
            return Result.failure(IllegalArgumentException("You cannot remove yourself."))
        }

        return try {
            // Use a WriteBatch for atomicity
            val batch = firestore.batch()

            // 1. Remove friendUidToRemove from the current user's friends list
            val currentUserDocRef = usersCollection.document(currentUserUid)
            batch.update(currentUserDocRef, "friends", FieldValue.arrayRemove(friendUidToRemove))

            // 2. Remove currentUserUid from the friend's friends list
            val friendUserDocRef = usersCollection.document(friendUidToRemove)
            batch.update(friendUserDocRef, "friends", FieldValue.arrayRemove(currentUserUid))

            // Commit the batch
            batch.commit().await()

            logD("Successfully removed friend: $currentUserUid <-> $friendUidToRemove")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error removing friend $friendUidToRemove: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Blocks a user. The current user will add the target user to their blockedUsers list.
     * Also removes the friend relationship if it exists.
     */
    suspend fun blockUser(userUidToBlock: String): Result<Unit> {
        val currentUser = getCurrentUser() ?: return Result.failure(Exception("User not logged in."))
        val currentUserUid = currentUser.uid

        // Prevent blocking self
        if (currentUserUid == userUidToBlock) {
            return Result.failure(IllegalArgumentException("You cannot block yourself."))
        }

        return try {
            val batch = firestore.batch()

            // 1. Add userUidToBlock to current user's blockedUsers list
            val currentUserDocRef = usersCollection.document(currentUserUid)
            batch.update(currentUserDocRef, "blockedUsers", FieldValue.arrayUnion(userUidToBlock))

            // 2. Remove friend relationship if exists (both ways)
            batch.update(currentUserDocRef, "friends", FieldValue.arrayRemove(userUidToBlock))

            val blockedUserDocRef = usersCollection.document(userUidToBlock)
            batch.update(blockedUserDocRef, "friends", FieldValue.arrayRemove(currentUserUid))

            // Commit the batch
            batch.commit().await()

            logD("Successfully blocked user: $userUidToBlock")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error blocking user $userUidToBlock: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Unblocks a user by removing them from the current user's blockedUsers list.
     */
    suspend fun unblockUser(userUidToUnblock: String): Result<Unit> {
        val currentUser = getCurrentUser() ?: return Result.failure(Exception("User not logged in."))
        val currentUserUid = currentUser.uid

        return try {
            val currentUserDocRef = usersCollection.document(currentUserUid)
            currentUserDocRef.update("blockedUsers", FieldValue.arrayRemove(userUidToUnblock)).await()

            logD("Successfully unblocked user: $userUidToUnblock")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error unblocking user $userUidToUnblock: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Searches for users by username prefix (case-sensitive).
     * Excludes the current user and blocked users from the results.
     */
    suspend fun searchUsersByUsername(query: String, limit: Long = 5): List<User> {
        if (query.isBlank()) {
            return emptyList()
        }
        val currentUserUid = getCurrentUser()?.uid // Get current user ID to exclude them

        return try {
            // Get current user's blocked list
            val currentUserProfile = if (currentUserUid != null) getUserProfile(currentUserUid) else null
            val blockedByMe = currentUserProfile?.blockedUsers ?: emptyList()

            // Firestore query for username starting with the query string (case-sensitive)
            val endQuery = query + "\uf8ff" // High-codepoint character for prefix matching
            val querySnapshot = usersCollection
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", endQuery)
                .limit(limit + 10) // Fetch extra to account for filtering
                .get()
                .await()

            val users = querySnapshot.toObjects(User::class.java)

            // Filter out:
            // 1. The current user
            // 2. Users I have blocked
            // 3. Users who have blocked me
            val filteredUsers = users.filterNot { user ->
                user.uid == currentUserUid ||
                blockedByMe.contains(user.uid) ||
                (currentUserUid != null && user.blockedUsers.contains(currentUserUid))
            }.take(limit.toInt())

            logD("Username search for '$query' found ${filteredUsers.size} potential friends (after blocking filter).")
            filteredUsers

        } catch (e: Exception) {
            logE("Error searching users by username '$query': ${e.message}")
            emptyList()
        }
        // NOTE: For case-insensitive search, you'd typically add a lowercase 'username_lowercase'
        // field to your User model and query that field instead, converting the input query to lowercase.
    }

    // --- Other Utility Functions ---

    suspend fun checkUsernameExists(username: String): Boolean {
        return try {
            val querySnapshot = usersCollection.whereEqualTo("username", username).limit(1).get().await()
            !querySnapshot.isEmpty
        } catch (e: Exception) {
            logE("Error checking username existence: ${e.message}")
            false // Assume doesn't exist on error
        }
    }

    fun getFriendsFlow(userUid: String): Flow<List<User>> = callbackFlow {
        val docRef = usersCollection.document(userUid)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logE("FriendsFlow listener error: ${error.message}")
                close(error) // Close the flow on error
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val user = snapshot.toObject(User::class.java)
                val friendUids = user?.friends ?: emptyList()
                val blockedByMe = user?.blockedUsers ?: emptyList()

                // --- FIX ---
                // Launch a coroutine from the callbackFlow's scope
                // to call the suspend function
                launch {
                    try {
                        val profiles = getProfilesForFriends(friendUids)
                        // Filter out blocked users (both blocked by me and who blocked me)
                        val filteredProfiles = profiles.filterNot { profile ->
                            blockedByMe.contains(profile.uid) || profile.blockedUsers.contains(userUid)
                        }
                        trySend(filteredProfiles) // Send the filtered result
                    } catch (e: Exception) {
                        logE("Error fetching profiles in getFriendsFlow: ${e.message}")
                        trySend(emptyList()) // Send empty on fetching error
                    }
                }
                // --- END FIX ---

            } else {
                // Handle document not existing or being null
                trySend(emptyList())
            }
        }
        // This is called when the flow collection stops
        awaitClose {
            logD("Stopping Firestore listener for user friends: $userUid")
            listener.remove()
        }
    }
}