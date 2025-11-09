package com.example.splitpay.ui.groups

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Available group tags and their associated icons.
 *
 * Tags are used to categorize groups (e.g., friends, trip, rent, groceries, utilities).
 */
val availableTags = listOf(
    "friends" to Icons.Default.People,
    "trip" to Icons.Default.Flight,
    "rent" to Icons.Default.Home,
    "groceries" to Icons.Default.ShoppingCart,
    "utilities" to Icons.Default.Bolt,
)

/**
 * Map of tag identifiers to their icons for quick lookup.
 */
val availableTagsMap: Map<String, ImageVector> = availableTags.toMap()
