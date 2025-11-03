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
}