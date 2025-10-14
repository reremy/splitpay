package com.example.splitpay.data.repository

import androidx.core.app.PendingIntentCompat.send
import com.example.splitpay.data.model.Group
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class GroupsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userRepository: UserRepository = UserRepository()
) {
    private val groupsCollection = firestore.collection("groups")

    /**
     * Creates a new expense group in Firestore.
     * The creator is automatically added as the first member.
     */
    suspend fun createGroup(groupName: String): Result<Group> {
        val currentUser = userRepository.getCurrentUser()
        if (currentUser == null) {
            return Result.failure(Exception("User not authenticated."))
        }

        return try {
            val documentRef = groupsCollection.document()
            val newGroup = Group(
                id = documentRef.id,
                name = groupName,
                createdByUid = currentUser.uid,
                // Creator is the first member
                members = listOf(currentUser.uid),
                createdAt = System.currentTimeMillis()
            )

            documentRef.set(newGroup, SetOptions.merge()).await()
            logD("Group created successfully with ID: ${newGroup.id}")
            Result.success(newGroup)
        } catch (e: Exception) {
            logE("Failed to create group: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Streams groups the current user is a member of.
     */
    fun getGroups(): Flow<List<Group>> = callbackFlow {
        val currentUserUid = userRepository.getCurrentUser()?.uid ?: run {
            logE("User not logged in. Cannot fetch groups.")
            send(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        // Listen for groups where the current user's UID is present in the 'members' array
        val listenerRegistration = groupsCollection
            .whereArrayContains("members", currentUserUid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logE("Error fetching groups: ${error.message}")
                    close(error) // Close the flow on error
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val groups = snapshot.toObjects<Group>()
                    logD("Fetched ${groups.size} groups for user $currentUserUid")
                    trySend(groups)
                }
            }

        // The callbackFlow scope is suspended until awaitClose is called.
        // This block is executed when the flow collector is cancelled or completes.
        awaitClose {
            logD("Stopping Firestore listener for groups.")
            listenerRegistration.remove()
        }
    }
}