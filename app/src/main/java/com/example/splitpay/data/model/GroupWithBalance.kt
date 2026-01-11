package com.example.splitpay.data.model

/**
 * A composite model used specifically for displaying an item on the main Groups list screen.
 * It holds the Group data and the current user's net balance within that group.
 */
data class GroupWithBalance(
    val group: Group,
    val userNetBalance: Double,
    val simplifiedOwedBreakdown: Map<String, Double> = emptyMap()
)