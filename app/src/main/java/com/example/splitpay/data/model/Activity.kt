package com.example.splitpay.data.model

/**
 * Defines the different types of activities that can be logged in the feed.
 * This helps the UI decide how to format and display the activity.
 */
enum class ActivityType {
    // Group Management
    GROUP_CREATED,
    GROUP_DELETED,
    MEMBER_ADDED,
    MEMBER_REMOVED,
    MEMBER_LEFT,

    // Expense & Payment
    EXPENSE_ADDED,
    EXPENSE_UPDATED,
    EXPENSE_DELETED,
    PAYMENT_MADE // For "Settle Up" actions
}



/**
 * Represents a single document in the top-level 'activities' collection in Firestore.
 * This model is designed to be flexible enough to store various event types.
 */
data class Activity(
    /** Unique ID of the activity log itself (e.g., a new UUID). */
    val id: String = "",

    /** The time the activity occurred, used for sorting the feed chronologically. */
    val timestamp: Long = System.currentTimeMillis(),

    /** The type of activity, used to determine how to format the display text. */
    val activityType: String = ActivityType.EXPENSE_ADDED.name,

    /** The UID of the user who performed the action (e.g., "You"). */
    val actorUid: String = "",

    /** The display name (username/full name) of the user who performed the action. */
    val actorName: String = "",

    /**
     * A list of ALL user UIDs this activity is relevant to.
     * This is the most important field for querying.
     * For example, in a group expense, this would include the actor AND all group members.
     * The Activity Screen will query Firestore: `whereArrayContains("involvedUids", currentUser.uid)`
     */
    val involvedUids: List<String> = emptyList(),

    /** The ID of the group this activity is related to (nullable for non-group). */
    val groupId: String? = null,

    /** The name of the group (e.g., "KOKOLTRIP"). */
    val groupName: String? = null,

    /** The ID of the primary entity involved (e.g., the Expense ID, the new Member's UID). */
    val entityId: String? = null,

    /**
     * The main subject of the activity (e.g., the expense description "twst",
     * or the added member's name "Nur A.").
     */
    val displayText: String? = null,

    /** The total amount of the expense or payment (e.g., 308.66). */
    val totalAmount: Double? = null,

    /**
     * Stores the financial impact *for each involved user* from this one activity.
     * Key = UID, Value = Net change (e.g., { "user1_uid": -333.33, "user2_uid": 333.33 })
     * This allows the UI to show a personalized "You owe..." or "You get back...".
     */
    val financialImpacts: Map<String, Double>? = null
)