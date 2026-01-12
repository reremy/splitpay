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

/**
 * Repository for managing expense-sharing groups in Firestore.
 *
 * This repository handles:
 * - Creating and editing groups
 * - Managing group membership (add/remove members)
 * - Querying user's groups (active and archived)
 * - Archiving and deleting groups
 *
 * **Group Structure:**
 * Groups contain multiple members and are used to organize shared expenses.
 * Each group has an `iconIdentifier` for UI display and can have a custom photo.
 */
class GroupsRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val userRepository: UserRepository = UserRepository()
) {
    private val groupsCollection = firestore.collection("groups")

    /**
     * Creates a new expense-sharing group.
     *
     * The creator is automatically added as the first member of the group.
     * Additional members can be added later using `addMembersToGroup()`.
     *
     * @param groupName The name of the group (e.g., "Weekend Trip")
     * @param iconIdentifier Identifier for the group icon (e.g., "trip", "home", "friends")
     * @param photoUrl Optional URL to a custom group photo in Firebase Storage
     * @return Result with the created Group object, or an exception on failure
     */
    suspend fun createGroup(groupName: String, iconIdentifier: String, photoUrl: String = ""): Result<Group> {
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
     * Creates a real-time Flow of active groups where the current user is a member.
     *
     * **Query Details:**
     * - Uses `whereArrayContains("members", currentUserUid)` to find groups containing the user
     * - Filters out archived groups (`isArchived = false`)
     * - Automatically updates when groups are added, modified, or archived
     *
     * @return Flow emitting lists of active Group objects
     */
    fun getGroups(): Flow<List<Group>> = callbackFlow {
        val currentUserUid = userRepository.getCurrentUser()?.uid ?: run {
            logE("User not logged in. Cannot fetch groups.")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        // Listen for all groups where current user is a member
        val listenerRegistration = groupsCollection
            .whereArrayContains("members", currentUserUid)
            .whereEqualTo("isArchived", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    logE("Error fetching groups: ${error.message}")
                    close(error)
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val groups = snapshot.toObjects<Group>()
                    logD("Fetched ${groups.size} groups for user $currentUserUid")
                    trySend(groups)
                }
            }

        awaitClose {
            logD("Stopping Firestore listener for groups.")
            listenerRegistration.remove()
        }
    }

    /**
     * Fetches the list of active groups for the current user (one-time fetch).
     *
     * Unlike `getGroups()` which provides a real-time stream, this function
     * performs a single query and returns the results immediately.
     *
     * **Note:** Results are sorted by creation date (newest first) in memory
     * to avoid requiring a Firestore composite index.
     *
     * @return List of Group objects, or empty list if user is not logged in or on error
     */
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
                .get()
                .await()

            // Sort in memory to avoid needing a composite index on (members + isArchived + createdAt)
            val groups = querySnapshot.toObjects(Group::class.java)
                .sortedByDescending { it.createdAt }
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
