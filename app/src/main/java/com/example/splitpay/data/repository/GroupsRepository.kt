package com.example.splitpay.data.repository

import com.example.splitpay.data.model.Group
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
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
    suspend fun createGroup(groupName: String, iconIdentifier: String, photoUrl: String = ""): Result<Group> { // <--- UPDATED SIGNATURE
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
                iconIdentifier = iconIdentifier,
                photoUrl = photoUrl,
                isArchived = false
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
            .whereEqualTo("isArchived", false)
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

    // --- NEW suspend function to get groups once ---
    /**
     * Fetches the list of groups the current user is a member of once.
     */
    // In GroupsRepository.kt, replace the getGroupsSuspend function with this:

    suspend fun getGroupsSuspend(): List<Group> {
        val currentUser = userRepository.getCurrentUser()
        if (currentUser == null) {
            logE("User not logged in, cannot fetch groups.")
            return emptyList()
        }
        val uid = currentUser.uid

        return try {
            logD("Fetching groups once for user: $uid")
            val querySnapshot = groupsCollection
                .whereArrayContains("members", uid)
                .whereEqualTo("isArchived", false)
                // REMOVED: .orderBy("createdAt", Query.Direction.DESCENDING)
                // We can sort in memory instead
                .get()
                .await()

            val groups = querySnapshot.toObjects(Group::class.java)
                .sortedByDescending { it.createdAt } // Sort in memory instead
            logD("Fetched ${groups.size} groups.")
            groups
        } catch (e: Exception) {
            logE("Error fetching groups once: ${e.message}")
            emptyList()
        }
    }

    /**
     * Gets a Flow that emits the Group document whenever it changes in Firestore.
     * Emits null if the document doesn't exist or an error occurs.
     */
    fun getGroupFlow(groupId: String): Flow<Group?> = callbackFlow {
        if (groupId.isBlank()) {
            trySend(null) // Send null immediately if ID is invalid
            awaitClose { }
            return@callbackFlow
        }

        val docRef = groupsCollection.document(groupId)
        val listenerRegistration = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logE("Listen failed for group $groupId: ${error.message}")
                // Close the flow with the error? Or just send null? Sending null for simplicity.
                trySend(null)
                close(error) // Close the flow on error
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val group = snapshot.toObject(Group::class.java)
                logD("Group data updated for $groupId, emitting...")
                trySend(group) // Emit the latest group data
            } else {
                logD("Group document $groupId does not exist or was deleted.")
                trySend(null) // Emit null if document doesn't exist
            }
        }

        // This is called when the flow collection stops
        awaitClose {
            logD("Stopping Firestore listener for group $groupId.")
            listenerRegistration.remove()
        }
    }

    suspend fun getGroupById(groupId: String): Group? {
        return try {
            val snapshot = groupsCollection.document(groupId).get().await()
            snapshot.toObject(Group::class.java)
        } catch (e: Exception) {
            logE("Error fetching group $groupId: ${e.message}")
            null
        }
    }

    suspend fun updateGroupName(groupId: String, newName: String): Result<Unit> {
        return try {
            groupsCollection.document(groupId).update("name", newName).await()
            logD("Updated group name for $groupId")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error updating group name for $groupId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateGroupIcon(groupId: String, newIconIdentifier: String): Result<Unit> {
        return try {
            groupsCollection.document(groupId).update("iconIdentifier", newIconIdentifier).await()
            logD("Updated group icon for $groupId")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error updating group icon for $groupId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateGroup(
        groupId: String,
        name: String? = null,
        iconIdentifier: String? = null,
        photoUrl: String? = null
    ): Result<Unit> {
        return try {
            val updates = mutableMapOf<String, Any>()

            name?.let { updates["name"] = it }
            iconIdentifier?.let { updates["iconIdentifier"] = it }
            photoUrl?.let { updates["photoUrl"] = it }

            if (updates.isEmpty()) {
                return Result.success(Unit)
            }

            groupsCollection.document(groupId).update(updates).await()
            logD("Updated group $groupId with fields: ${updates.keys}")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error updating group $groupId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun addMembersToGroup(groupId: String, memberUids: List<String>): Result<Unit> {
        if (memberUids.isEmpty()) return Result.success(Unit) // Nothing to add
        return try {
            val groupRef = groupsCollection.document(groupId)
            // Use FieldValue.arrayUnion to add elements without duplicates
            groupRef.update("members", FieldValue.arrayUnion(*memberUids.toTypedArray())).await()
            logD("Added ${memberUids.size} members to group $groupId")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error adding members to group $groupId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun removeMemberFromGroup(groupId: String, memberUid: String): Result<Unit> {
        return try {
            val groupRef = groupsCollection.document(groupId)
            // Use FieldValue.arrayRemove to remove elements
            groupRef.update("members", FieldValue.arrayRemove(memberUid)).await()
            logD("Removed member $memberUid from group $groupId")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error removing member $memberUid from group $groupId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun archiveGroup(groupId: String): Result<Unit> {
        return try {
            groupsCollection.document(groupId).update("isArchived", true).await()
            logD("Archived group $groupId")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error archiving group $groupId: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteGroup(groupId: String): Result<Unit> {
        return try {
            groupsCollection.document(groupId).delete().await()
            logD("Deleted group $groupId")
            // Note: Does not delete associated expenses yet
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error deleting group $groupId: ${e.message}")
            Result.failure(e)
        }
    }
}
