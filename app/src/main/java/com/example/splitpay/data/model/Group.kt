package com.example.splitpay.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Represents an expense-sharing group.
 *
 * Groups allow multiple users to collectively manage and split expenses.
 * Each group has members who can add expenses, view balances, and settle up.
 * Groups can be archived (soft delete) when no longer active.
 *
 * Corresponds to documents in the Firestore `/groups` collection.
 *
 * @property id Unique group ID (Firestore document ID)
 * @property name Group name (e.g., "KOKOLTRIP", "Roommates 2024")
 * @property createdByUid UID of the user who created this group
 * @property members List of UIDs of all users in the group (including the creator)
 * @property createdAt Group creation timestamp in milliseconds
 * @property iconIdentifier Icon identifier for UI display (e.g., "friends", "vacation", "home")
 * @property photoUrl Firebase Storage URL for custom group photo (optional)
 * @property isArchived Whether the group is archived (soft delete, hidden from main list)
 * @property settledDate Timestamp when group was last fully settled (all balances = 0), null if never settled
 */
data class Group(
    val id: String = "",
    val name: String = "",
    val createdByUid: String = "",
    val members: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val iconIdentifier: String = "friends",
    val photoUrl: String = "",
    @get:PropertyName("isArchived")
    val isArchived: Boolean = false,
    val settledDate: Long? = null
)