package com.example.splitpay.ui.groups

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Available group tags and their associated icons.
 *
 * Tags are used to categorize groups (e.g., friends, house, family, event, etc.).
 */
val availableTags = listOf(
    "friends" to Icons.Default.People,
    "house" to Icons.Default.Home,
    "family" to Icons.Default.FamilyRestroom,
    "event" to Icons.Default.Event,
    "office" to Icons.Default.Business,
    "couple" to Icons.Default.Favorite,
    "activity" to Icons.Default.LocalActivity,
    "school" to Icons.Default.School,
)

/**
 * Map of tag identifiers to their icons for quick lookup.
 */
val availableTagsMap: Map<String, ImageVector> = availableTags.toMap()

/**
 * Available expense categories and their associated icons.
 * Used for categorizing individual expenses.
 */
val expenseCategories = listOf(
    "food & drink" to Icons.Default.Restaurant,
    "groceries" to Icons.Default.ShoppingCart,
    "rent" to Icons.Default.Home,
    "utilities" to Icons.Default.Bolt,
    "transport" to Icons.Default.DirectionsCar,
    "entertainment" to Icons.Default.Movie,
    "shopping" to Icons.Default.ShoppingBag,
    "subscription" to Icons.Default.Subscriptions,
    "maintenance" to Icons.Default.Build,
    "medical" to Icons.Default.LocalHospital,
    "education" to Icons.Default.School,
    "misc" to Icons.Default.Category,
)

/**
 * Map of expense category identifiers to their icons for quick lookup.
 */
val expenseCategoriesMap: Map<String, ImageVector> = expenseCategories.toMap()
