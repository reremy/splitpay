package com.example.splitpay.data.model

import com.google.firebase.firestore.PropertyName

data class Group(
    val id: String = "",
    val name: String = "",
    val createdByUid: String = "",
    // List of UIDs of all users in the group (including the creator)
    val members: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val iconIdentifier: String = "friends",
    val photoUrl: String = "", // Group photo URL from Firebase Storage
    @get:PropertyName("isArchived")
    val isArchived: Boolean = false,
    val settledDate: Long? = null // Timestamp when group was last fully settled (all balances = 0)
)