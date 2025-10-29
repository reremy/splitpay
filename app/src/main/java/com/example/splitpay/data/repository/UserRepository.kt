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
        password: String,
        ): Result<Unit> {
        return try {

            logD("Attempting sign-up for user: $email")
            //create Firebase Auth account
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user ?: return Result.failure(Exception("User creation failed."))

            logI("User created in Firebase Auth: ${user.uid}")

            // Set the Firebase User's display name using the fullName
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(fullName)
                .build()
            user.updateProfile(profileUpdates).await() // <-- ADDED: Updates displayName
            logD("Firebase User profile updated with displayName: $fullName") // Logger: Profile updated


            //build user data for firestore
            val userData = User(
                uid = user.uid,
                fullName = fullName,
                username = username,
                email = email
            )

            //store in Firestore
            firestore.collection("users")
                .document(user.uid)
                .set(userData)
                .await()

            logI("User profile saved to Firestore.")
            Result.success(Unit)
        } catch (e: Exception){
            logE("Sign-up failed for $email: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signIn(email: String, password: String): FirebaseUser? {
        return try {
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            authResult.user

        } catch (e: Exception) {
            throw e
        }
    }
    //return FirebaseUser if successful, caught exception if not.

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
     * Searches for users by username prefix (case-sensitive).
     * Excludes the current user from the results.
     */
    suspend fun searchUsersByUsername(query: String, limit: Long = 5): List<User> {
        if (query.isBlank()) {
            return emptyList()
        }
        val currentUserUid = getCurrentUser()?.uid // Get current user ID to exclude them

        return try {
            // Firestore query for username starting with the query string (case-sensitive)
            val endQuery = query + "\uf8ff" // High-codepoint character for prefix matching
            val querySnapshot = usersCollection
                .whereGreaterThanOrEqualTo("username", query)
                .whereLessThanOrEqualTo("username", endQuery)
                .limit(limit + 1) // Fetch one extra to potentially exclude self later
                .get()
                .await()

            val users = querySnapshot.toObjects(User::class.java)
            // Filter out the current user and take the desired limit
            val filteredUsers = users.filterNot { it.uid == currentUserUid }.take(limit.toInt())
            logD("Username search for '$query' found ${filteredUsers.size} potential friends.")
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

                // --- FIX ---
                // Launch a coroutine from the callbackFlow's scope
                // to call the suspend function
                launch {
                    try {
                        val profiles = getProfilesForFriends(friendUids)
                        trySend(profiles) // Send the result
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