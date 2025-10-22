package com.example.splitpay.ui.friends

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList // Import FilterList
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun FriendsTopBarActions(
    onSearchClick: () -> Unit = {},
    onAddFriendClick: () -> Unit = {},
    onFilterClick: () -> Unit // <-- ADD Filter click lambda
) {
    // Filter Button
    IconButton(onClick = onFilterClick) { // <-- Use the lambda
        Icon(
            imageVector = Icons.Default.FilterList, // <-- Use FilterList icon
            contentDescription = "Filter Friends",
            tint = Color.White // Or your theme color
        )
    }

    // Existing Search Button (Placeholder)
    IconButton(onClick = onSearchClick) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search Friends",
            tint = Color.White // Or your theme color
        )
    }
    // Existing Add Friend Button (Placeholder)
    IconButton(onClick = onAddFriendClick) {
        Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = "Add Friend",
            tint = Color.White // Or your theme color
        )
    }
}