package com.example.splitpay.data.model

// Represents a friend shown in the Friends list, including their balance relative to the current user
data class FriendWithBalance(
    val uid: String,
    val username: String,
    val netBalance: Double, // Positive if they owe you, negative if you owe them
    val balanceBreakdown: List<BalanceDetail> = emptyList() // Optional breakdown by group
)

// Details for the balance breakdown
data class BalanceDetail(
    val groupName: String,
    val amount: Double // Positive if they owe you for this group, negative if you owe them
)