package com.example.splitpay.data.repository

import com.example.splitpay.data.model.Activity
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Repository for managing activity feed data in Firestore.
 *
 * The activity feed provides users with a chronological log of all events relevant to them,
 * including:
 * - Expense and payment transactions
 * - Group management events (created, deleted, member changes)
 * - Financial impact summaries for each activity
 *
 * **Key Design Pattern:**
 * Activities use the `involvedUids` array to enable efficient user-specific queries.
 * When an activity is created, all affected user IDs are stored in this array, allowing
 * Firestore to quickly find activities for a specific user via `whereArrayContains`.
 */
class ActivityRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val activitiesCollection = firestore.collection("activities")

    /**
     * Logs a new activity to the activity feed.
     *
     * **Usage Pattern:**
     * This is typically called after creating/updating expenses, payments, or making
     * group changes. It's a "fire-and-forget" operation - failures are logged but
     * don't block the main operation.
     *
     * The function automatically generates a unique ID for the activity.
     *
     * @param activity The activity to log (ID will be auto-generated if blank)
     * @return Result indicating success or failure
     */
    suspend fun logActivity(activity: Activity): Result<Unit> {
        return try {
            val documentRef = activitiesCollection.document()
            // Copy the activity, assigning the new unique ID from the document reference
            val activityToSave = activity.copy(id = documentRef.id)

            documentRef.set(activityToSave).await()
            logD("Activity logged successfully: ${activityToSave.id} (Type: ${activity.activityType})")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error logging activity: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Creates a real-time Flow of activities relevant to a specific user.
     *
     * **Query Strategy:**
     * Uses `whereArrayContains("involvedUids", userUid)` to find all activities where
     * the user is involved (as creator, participant, payer, or group member).
     *
     * **Performance Note:**
     * Limited to the 50 most recent activities by default to avoid excessive data transfer.
     * Pagination can be added for loading older activities.
     *
     * @param userUid The UID of the current user
     * @param limit Maximum number of activities to return (default: 50)
     * @return Flow emitting lists of activities in real-time
     */
    fun getActivityFeedFlow(userUid: String, limit: Long = 50): Flow<List<Activity>> = callbackFlow {
        if (userUid.isBlank()) {
            logE("User UID is blank, cannot fetch activity feed.")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        logD("Starting activity feed listener for user: $userUid")

        // Query all activities where user is in the involvedUids array
        val query = activitiesCollection
            .whereArrayContains("involvedUids", userUid)
            .limit(limit)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logE("Activity feed listener error for $userUid: ${error.message}")
                close(error) // Close flow on error
                return@addSnapshotListener
            }

            if (snapshot != null) {
                val activities = snapshot.toObjects(Activity::class.java)
                logD("Emitting ${activities.size} activities for user $userUid")
                trySend(activities) // Emit the latest list
            } else {
                logD("Activity snapshot was null for user $userUid")
                trySend(emptyList()) // Emit empty list if snapshot is null
            }
        }

        // Unregister listener when flow is cancelled
        awaitClose {
            logD("Stopping activity feed listener for $userUid")
            listenerRegistration.remove()
        }
    }

    /**
     * Retrieves a single activity by its unique ID.
     *
     * @param activityId The unique ID of the activity
     * @return Result with the Activity object (or null if not found), or the exception on failure
     */
    suspend fun getActivityById(activityId: String): Result<Activity?> {
        return try {
            if (activityId.isBlank()) {
                return Result.failure(IllegalArgumentException("Activity ID cannot be blank"))
            }
            val document = activitiesCollection.document(activityId).get().await()
            val activity = document.toObject(Activity::class.java)
            logD("Fetched activity with ID: $activityId")
            Result.success(activity)
        } catch (e: Exception) {
            logE("Error fetching activity by ID $activityId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Permanently deletes an activity from the feed.
     *
     * **Warning:** This operation cannot be undone and will remove the activity
     * from all users' feeds.
     *
     * @param activityId The ID of the activity to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteActivity(activityId: String): Result<Unit> {
        return try {
            if (activityId.isBlank()) {
                return Result.failure(IllegalArgumentException("Activity ID cannot be blank"))
            }
            activitiesCollection.document(activityId).delete().await()
            logD("Activity deleted successfully with ID: $activityId")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error deleting activity $activityId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Finds an activity by the ID of its related entity (e.g., expense ID).
     *
     * **Use Case:**
     * When navigating from an expense card to its activity detail, this function
     * retrieves the activity that was logged when the expense was created.
     *
     * @param entityId The ID of the related entity (expense, payment, group, etc.)
     * @return Result with the first matching Activity (or null if not found)
     */
    suspend fun getActivityByEntityId(entityId: String): Result<Activity?> {
        return try {
            if (entityId.isBlank()) {
                return Result.failure(IllegalArgumentException("Entity ID cannot be blank"))
            }
            val querySnapshot = activitiesCollection
                .whereEqualTo("entityId", entityId)
                .limit(1)
                .get()
                .await()

            val activity = if (!querySnapshot.isEmpty) {
                querySnapshot.documents[0].toObject(Activity::class.java)
            } else {
                null
            }
            logD("Fetched activity with entityId: $entityId")
            Result.success(activity)
        } catch (e: Exception) {
            logE("Error fetching activity by entityId $entityId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Fetches all activities related to a specific group.
     *
     * Returns activities such as:
     * - Group creation
     * - Member additions/removals
     * - Expenses within the group
     * - Payments within the group
     *
     * Results are sorted by timestamp (newest first).
     *
     * @param groupId The ID of the group
     * @return List of activities for the group, or empty list on error
     */
    suspend fun getActivitiesForGroup(groupId: String): List<Activity> {
        return try {
            if (groupId.isBlank()) {
                logE("Group ID is blank")
                return emptyList()
            }

            val querySnapshot = activitiesCollection
                .whereEqualTo("groupId", groupId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .await()

            val activities = querySnapshot.toObjects(Activity::class.java)
            logD("Fetched ${activities.size} activities for group $groupId")
            activities
        } catch (e: Exception) {
            logE("Error fetching activities for group $groupId: ${e.message}")
            emptyList()
        }
    }
}