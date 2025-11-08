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

class ActivityRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val activitiesCollection = firestore.collection("activities")

    /**
     * Saves a new activity document to Firestore.
     * This is a "fire-and-forget" operation from the ViewModel's perspective.
     * It generates a new ID for the activity.
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
     * Gets a real-time Flow of the user's activity feed.
     * Queries all activities where the user's UID is in the 'involvedUids' array.
     *
     * @param userUid The UID of the current user.
     * @param limit The maximum number of activities to fetch (e.g., the 50 most recent).
     * @return A Flow emitting a list of Activity objects.
     */
    fun getActivityFeedFlow(userUid: String, limit: Long = 50): Flow<List<Activity>> = callbackFlow {
        if (userUid.isBlank()) {
            logE("User UID is blank, cannot fetch activity feed.")
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        logD("Starting activity feed listener for user: $userUid")

        // This is the key query based on our data model
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
     * Gets a single activity by ID
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
     * Deletes a single activity by ID
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
     * Gets an activity by entity ID (e.g., expense ID)
     * Useful when navigating from expense cards to activity details
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
     * Gets all activities for a specific group
     * Useful for displaying group-related activities (member additions, etc.)
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