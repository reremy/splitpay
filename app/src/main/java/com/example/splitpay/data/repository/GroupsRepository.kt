package com.example.splitpay.data.repository

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
    suspend fun createGroup(groupName: String, iconIdentifier: String): Result<Group> { // <--- UPDATED SIGNATURE
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
                createdAt = System.currentTimeMillis(),
                iconIdentifier = iconIdentifier // <--- ADDED ICON
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
            // FIX: Use trySend for synchronous non-suspending flow emission
            trySend(emptyList())
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

    /**
     * Gets a single Group document by ID.
     */
    suspend fun getGroupById(groupId: String): Group? {
        return try {
            val snapshot = groupsCollection.document(groupId).get().await()
            val group = snapshot.toObject(Group::class.java)

            // CRITICAL MOCK/FIX: If the group is not found in Firestore (because the mock environment
            // doesn't persist properly or we haven't loaded it), we fall back to a reasonable mock
            // derived from the ID to prevent crash.
            if (group == null) {
                logE("Group not found in Firestore for ID: $groupId. Using fallback mock.")
                return Group(
                    id = groupId,
                    name = "Fallback Group ${groupId.take(4)}",
                    createdByUid = userRepository.getCurrentUser()?.uid ?: "",
                    members = listOf(userRepository.getCurrentUser()?.uid ?: ""),
                    iconIdentifier = "group"
                )
            }
            group
        } catch (e: Exception) {
            logE("Failed to fetch group by ID: $groupId. Error: ${e.message}")
            null
        }
    }
}
