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

/**
 * Repository for managing user authentication and profile data.
 *
 * This repository handles:
 * - Firebase Authentication (sign up, login, logout)
 * - User profile management in Firestore
 * - Friend relationships (add, remove, block/unblock)
 * - User search and discovery
 * - Profile updates (name, username, profile picture, QR code)
 */
class UserRepository(
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
){

    private val usersCollection = firestore.collection("users")

    // ========================================
    // Caching Infrastructure
    // ========================================

    /**
     * Cached user profile with timestamp for TTL-based expiration.
     */
    private data class CachedUserProfile(
        val user: User,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(ttlMs: Long = PROFILE_CACHE_TTL): Boolean {
            return System.currentTimeMillis() - timestamp > ttlMs
        }
    }

    /**
     * Cached search result with timestamp for TTL-based expiration.
     */
    private data class CachedSearchResult(
        val results: List<User>,
        val query: String,
        val timestamp: Long = System.currentTimeMillis()
    ) {
        fun isExpired(ttlMs: Long = SEARCH_CACHE_TTL): Boolean {
            return System.currentTimeMillis() - timestamp > ttlMs
        }
    }

    // Cache storage
    private val profileCache = mutableMapOf<String, CachedUserProfile>()
    private val searchResultCache = mutableMapOf<String, CachedSearchResult>()

    companion object {
        private const val PROFILE_CACHE_TTL = 5 * 60 * 1000L // 5 minutes
        private const val SEARCH_CACHE_TTL = 30 * 1000L // 30 seconds
    }

    // ========================================
    // Authentication Functions
    // ========================================

    /**
     * Creates a new user account with email and password.
     *
     * This function performs the following operations:
     * 1. Creates a Firebase Authentication account
     * 2. Sets the user's display name in Firebase Auth
     * 3. Creates a user profile document in Firestore
     *
     * @param fullName User's full name
     * @param username Unique username for the user
     * @param email User's email address
     * @param phoneNumber User's phone number with country code
     * @param password Account password (min 6 characters per Firebase rules)
     * @return Result indicating success or failure with error details
     */
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

            // ✅ ADD: Log instances being used
            logD("🔍 Auth instance: $auth")
            logD("🔍 Firestore instance: $firestore")

            // Create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: run {
                logE("Firebase Auth user creation failed - user object is null")
                return Result.failure(Exception("User creation failed."))
            }

            logI("Firebase Auth account created successfully - UID: ${user.uid}")

            // Set display name
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

            // ✅ ADD: Detailed Firestore logging
            logD("🔍 User data object created: $userData")
            logD("🔍 About to write to Firestore collection 'users', document: ${user.uid}")

            try {
                firestore.collection("users")
                    .document(user.uid)
                    .set(userData)
                    .await()

                logI("✅ Firestore write completed successfully!")

                // ✅ ADD: Immediately verify the write
                logD("🔍 Verifying write by reading back...")
                val verifyDoc = firestore.collection("users")
                    .document(user.uid)
                    .get()
                    .await()

                logD("🔍 Verification - Document exists: ${verifyDoc.exists()}")
                logD("🔍 Verification - Document data: ${verifyDoc.data}")

                if (!verifyDoc.exists()) {
                    logE("❌ CRITICAL: Document write succeeded but immediate read shows it doesn't exist!")
                    return Result.failure(Exception("Firestore write verification failed"))
                }

            } catch (firestoreException: Exception) {
                logE("❌ Firestore operation failed!")
                logE("   Type: ${firestoreException.javaClass.simpleName}")
                logE("   Message: ${firestoreException.message}")
                firestoreException.printStackTrace()
                throw firestoreException
            }

            logI("User profile saved to Firestore successfully - Sign-up complete for: $email")
            Result.success(Unit)

        } catch (e: Exception) {
            logE("Sign-up failed for $email: ${e.message}")
            e.printStackTrace()
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

                // Check if account deletion is scheduled and cancel if within grace period
                val userProfile = getUserProfile(user.uid)
                if (userProfile?.deletionScheduledAt != null) {
                    val deletionScheduledAt = userProfile.deletionScheduledAt
                    val gracePeriodMs = 30L * 24 * 60 * 60 * 1000 // 30 days in milliseconds
                    val now = System.currentTimeMillis()

                    if (now - deletionScheduledAt < gracePeriodMs) {
                        // Within grace period - cancel deletion silently
                        logI("User ${user.uid} logged in during grace period. Cancelling scheduled deletion.")
                        cancelScheduledDeletion(user.uid)
                    } else {
                        // Grace period has expired - account should have been deleted
                        logE("User ${user.uid} attempted login after grace period. Account should be deleted.")
                        // Sign out immediately and throw exception
                        signOut()
                        throw Exception("Account has been deleted.")
                    }
                }
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
     * Gets user profile with caching.
     * Returns cached version if available and not expired.
     *
     * @param uid The user's UID
     * @return User profile or null if not found
     */
    suspend fun getUserProfileCached(uid: String): User? {
        // Check cache first
        val cached = profileCache[uid]
        if (cached != null && !cached.isExpired()) {
            logD("Profile cache hit for $uid")
            return cached.user
        }

        // Cache miss - fetch from Firestore
        logD("Profile cache miss for $uid - fetching from Firestore")
        val user = getUserProfile(uid)

        // Update cache
        if (user != null) {
            profileCache[uid] = CachedUserProfile(user)
        }

        return user
    }

    /**
     * Invalidates profile cache for a specific user or all users.
     * Call this after profile updates.
     *
     * @param uid The user's UID to invalidate, or null to clear entire cache
     */
    fun invalidateProfileCache(uid: String? = null) {
        if (uid == null) {
            profileCache.clear()
            logD("Cleared entire profile cache")
        } else {
            profileCache.remove(uid)
            logD("Invalidated cache for $uid")
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

            // Invalidate cache for this user after successful update
            invalidateProfileCache(uid)

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
     * Gets profiles with caching for batch operations.
     * Returns cached + freshly fetched profiles combined.
     *
     * @param friendUids List of user UIDs to fetch profiles for
     * @return List of User profiles
     */
    suspend fun getProfilesForFriendsCached(friendUids: List<String>): List<User> {
        if (friendUids.isEmpty()) {
            return emptyList()
        }

        val cached = mutableListOf<User>()
        val toFetch = mutableListOf<String>()

        // Separate cached from uncached
        for (uid in friendUids) {
            val cachedEntry = profileCache[uid]
            if (cachedEntry != null && !cachedEntry.isExpired()) {
                cached.add(cachedEntry.user)
            } else {
                toFetch.add(uid)
            }
        }

        // Fetch uncached profiles
        val fresh = if (toFetch.isNotEmpty()) {
            getProfilesForFriends(toFetch)
        } else {
            emptyList()
        }

        // Update cache with fresh data
        fresh.forEach { user ->
            profileCache[user.uid] = CachedUserProfile(user)
        }

        logD("Profile cache: ${cached.size} hits, ${fresh.size} misses (total ${friendUids.size} requested)")
        return cached + fresh
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

    /**
     * Searches for users by username with caching.
     * Returns cached results if available and not expired.
     *
     * @param query The username search query
     * @param limit Maximum number of results to return
     * @return List of User objects matching the query
     */
    suspend fun searchUsersByUsernameCached(query: String, limit: Long = 5): List<User> {
        if (query.isBlank()) {
            return emptyList()
        }

        // Check cache first
        val cached = searchResultCache[query]
        if (cached != null && !cached.isExpired()) {
            logD("Search cache hit for: '$query'")
            return cached.results.take(limit.toInt())
        }

        // Cache miss - perform search
        logD("Search cache miss for: '$query' - fetching from Firestore")
        val results = searchUsersByUsername(query, limit)

        // Update cache
        searchResultCache[query] = CachedSearchResult(results, query)

        return results
    }

    /**
     * Invalidates search result cache.
     * Call this when search criteria or user data changes significantly.
     */
    fun invalidateSearchCache() {
        searchResultCache.clear()
        logD("Cleared search result cache")
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
                        val profiles = getProfilesForFriendsCached(friendUids)
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

    // ========================================
    // Account Deletion Functions
    // ========================================

    /**
     * Result of account deletion validation check.
     */
    data class DeletionValidationResult(
        val canDelete: Boolean,
        val errorMessage: String? = null
    )

    /**
     * Validates if user can delete their account.
     * Checks for:
     * - Active (non-archived) group memberships
     * - Unsettled balances (must be exactly 0.00)
     *
     * @param uid The user's UID
     * @param groupsRepository Repository to check group memberships
     * @param expenseRepository Repository to check balances
     * @return DeletionValidationResult indicating if deletion is allowed
     */
    suspend fun validateAccountDeletion(
        uid: String,
        groupsRepository: com.example.splitpay.data.repository.GroupsRepository,
        expenseRepository: com.example.splitpay.data.repository.ExpenseRepository
    ): DeletionValidationResult {
        try {
            // Check for active group memberships
            val allGroups = groupsRepository.getGroupsSuspend()
            val activeGroups = allGroups.filter { group ->
                group.members.contains(uid) && !group.isArchived
            }

            if (activeGroups.isNotEmpty()) {
                logD("Cannot delete account: User is in ${activeGroups.size} active group(s)")
                return DeletionValidationResult(
                    canDelete = false,
                    errorMessage = "Please leave all groups before deleting your account."
                )
            }

            // Check for unsettled balances
            // Get all expenses involving this user
            val userProfile = getUserProfile(uid)
            val friendUids = userProfile?.friends ?: emptyList()

            // Get all non-group expenses
            for (friendUid in friendUids) {
                val expenses = expenseRepository.getNonGroupExpensesBetweenUsers(uid, friendUid)

                // Calculate balance with this friend
                var balance = 0.0
                expenses.forEach { expense ->
                    val paidByUser = expense.paidBy.find { it.uid == uid }?.paidAmount ?: 0.0
                    val owedByUser = expense.participants.find { it.uid == uid }?.owesAmount ?: 0.0
                    balance += (paidByUser - owedByUser)
                }

                // Check if balance is exactly 0.00
                if (balance != 0.0) {
                    logD("Cannot delete account: Unsettled balance of $balance with friend $friendUid")
                    return DeletionValidationResult(
                        canDelete = false,
                        errorMessage = "Please settle all balances before deleting your account."
                    )
                }
            }

            logD("Account deletion validation passed for user $uid")
            return DeletionValidationResult(canDelete = true)

        } catch (e: Exception) {
            logE("Error validating account deletion: ${e.message}")
            return DeletionValidationResult(
                canDelete = false,
                errorMessage = "Error checking account status. Please try again."
            )
        }
    }

    /**
     * Schedules account for deletion after 30-day grace period.
     * Sets deletionScheduledAt timestamp.
     * User can cancel by logging in within 30 days.
     *
     * @param uid The user's UID
     * @return Result indicating success or failure
     */
    suspend fun scheduleAccountDeletion(uid: String): Result<Unit> {
        return try {
            val deletionTimestamp = System.currentTimeMillis()

            usersCollection.document(uid)
                .update("deletionScheduledAt", deletionTimestamp)
                .await()

            logI("Account deletion scheduled for user $uid at timestamp $deletionTimestamp")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error scheduling account deletion for $uid: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Cancels scheduled account deletion.
     * Called when user logs in during grace period.
     *
     * @param uid The user's UID
     * @return Result indicating success or failure
     */
    suspend fun cancelScheduledDeletion(uid: String): Result<Unit> {
        return try {
            usersCollection.document(uid)
                .update("deletionScheduledAt", null)
                .await()

            logI("Account deletion cancelled for user $uid")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error cancelling account deletion for $uid: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Permanently deletes user account and all associated data.
     * This should only be called by Cloud Function after grace period ends.
     *
     * Steps:
     * 1. Remove user from all friends' friend lists
     * 2. Leave all archived groups
     * 3. Delete user document from Firestore
     * 4. Delete Firebase Auth account
     *
     * @param uid The user's UID
     * @return Result indicating success or failure
     */
    suspend fun permanentlyDeleteAccount(uid: String): Result<Unit> {
        return try {
            logI("Starting permanent account deletion for user $uid")

            // Get user profile
            val user = getUserProfile(uid)
            if (user == null) {
                logE("Cannot delete account: User $uid not found")
                return Result.failure(Exception("User not found"))
            }

            // 1. Remove from all friends' lists
            val friendUids = user.friends
            for (friendUid in friendUids) {
                try {
                    usersCollection.document(friendUid)
                        .update("friends", FieldValue.arrayRemove(uid))
                        .await()
                    logD("Removed $uid from friend $friendUid's friend list")
                } catch (e: Exception) {
                    logE("Error removing $uid from friend $friendUid: ${e.message}")
                }
            }

            // 2. Leave all archived groups (active groups should already be left)
            // Note: Group expenses will show [Deleted User] via the app logic
            // We don't delete the expenses themselves

            // 3. Delete user document from Firestore
            usersCollection.document(uid).delete().await()
            logI("Deleted Firestore document for user $uid")

            // 4. Delete Firebase Auth account would be done by Cloud Function with admin SDK
            // App doesn't have permission to delete other users' auth accounts

            logI("Account permanently deleted for user $uid")
            Result.success(Unit)

        } catch (e: Exception) {
            logE("Error permanently deleting account for $uid: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            val currentUser = getCurrentUser() ?: run {
                logE("Cannot update FCM token: User not signed in")
                return Result.failure(Exception("User not signed in."))
            }

            logD("Updating FCM token for user: ${currentUser.uid}")

            firestore.collection("users")
                .document(currentUser.uid)
                .update("fcmToken", token)
                .await()

            logI("FCM token updated successfully for user: ${currentUser.uid}")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Failed to update FCM token: ${e.message}")
            Result.failure(e)
        }
    }
}