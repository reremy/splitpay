package com.example.splitpay.data.model

/**
 * Defines the different types of activities that can be logged in the feed.
 *
 * The activity type determines how the UI formats and displays each activity entry,
 * including the icon, color scheme, and descriptive text.
 *
 * **Categories:**
 * - **Group Management**: GROUP_CREATED, GROUP_DELETED, MEMBER_ADDED, MEMBER_REMOVED, MEMBER_LEFT
 * - **Expense & Payment**: EXPENSE_ADDED, EXPENSE_UPDATED, EXPENSE_DELETED, PAYMENT_MADE, PAYMENT_UPDATED, PAYMENT_DELETED
 */
enum class ActivityType {
    // Group Management
    /** New group was created */
    GROUP_CREATED,
    /** Group was permanently deleted */
    GROUP_DELETED,
    /** New member was added to a group */
    MEMBER_ADDED,
    /** Member was removed from a group by another user */
    MEMBER_REMOVED,
    /** Member voluntarily left a group */
    MEMBER_LEFT,

    // Expense & Payment
    /** New expense was recorded */
    EXPENSE_ADDED,
    /** Existing expense was edited */
    EXPENSE_UPDATED,
    /** Expense was deleted */
    EXPENSE_DELETED,
    /** Settlement payment was made (reduces balances) */
    PAYMENT_MADE,
    /** Existing payment was edited */
    PAYMENT_UPDATED,
    /** Payment was deleted */
    PAYMENT_DELETED
}

/**
 * Represents an activity feed entry showing a user-relevant event.
 *
 * Activities provide users with a chronological log of all events that affect them,
 * including expenses, payments, and group management actions. Each activity captures
 * the what, who, when, and financial impact of an event.
 *
 * Corresponds to documents in the Firestore `/activities` collection.
 *
 * **Query Pattern:**
 * Activities are queried using `whereArrayContains("involvedUids", currentUserUid)` to
 * fetch only events relevant to the current user.
 *
 * **Example Activities:**
 * - "Alice added 'Dinner' expense $100 to Roommates" (EXPENSE_ADDED)
 * - "Bob paid Alice $50" (PAYMENT_MADE)
 * - "You created Vacation group" (GROUP_CREATED)
 *
 * @property id Unique activity ID (Firestore document ID)
 * @property timestamp Activity timestamp in milliseconds (used for chronological sorting)
 * @property activityType Type of activity (see [ActivityType] enum)
 * @property actorUid UID of the user who performed the action
 * @property actorName Display name of the actor (username or full name)
 * @property involvedUids List of ALL user UIDs affected by this activity (used for efficient querying)
 * @property groupId ID of the related group (null for friend-to-friend activities)
 * @property groupName Name of the related group (for display purposes)
 * @property entityId ID of the primary entity involved (e.g., expense ID, member UID)
 * @property displayText Main subject/description (e.g., expense description, member name)
 * @property totalAmount Total transaction amount (null for non-financial activities)
 * @property financialImpacts Map of user UIDs to their financial impact from this activity (negative = owes, positive = gets back)
 */
data class Activity(
    val id: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val activityType: String = ActivityType.EXPENSE_ADDED.name,
    val actorUid: String = "",
    val actorName: String = "",
    val involvedUids: List<String> = emptyList(),
    val groupId: String? = null,
    val groupName: String? = null,
    val entityId: String? = null,
    val displayText: String? = null,
    val totalAmount: Double? = null,
    val financialImpacts: Map<String, Double>? = null
)