package com.example.splitpay.data.model

data class Group(
    val id: String = "",
    val name: String = "",
    val createdByUid: String = "",
    // List of UIDs of all users in the group (including the creator)
    val members: List<String> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val iconIdentifier: String = "group",
    val isArchived: Boolean = false
)