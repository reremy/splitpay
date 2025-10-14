package com.example.splitpay.data.model

/**
 * A composite model used specifically for displaying an item on the main Groups list screen.
 * It holds the Group data and the current user's net balance within that group.
 */
data class GroupWithBalance(
    val group: Group,
    // The current user's net balance for this group.
    // Positive = You are owed money.
    // Negative = You owe money.
    val userNetBalance: Double,
    // List of simplified balances showing who owes the current user in this group.
    // Key: UID of the person who owes, Value: Amount they owe.
    // This is optional for a simple UI but useful for the breakdown section (like "Nur A. owes you...")
    val simplifiedOwedBreakdown: Map<String, Double> = emptyMap()
)