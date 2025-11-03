package com.example.splitpay.ui.groups

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
fun GroupsTopBarActions(
    onNavigateToCreateGroup: () -> Unit
) {
    IconButton(onClick = onNavigateToCreateGroup) {
        Icon(Icons.Default.Add, contentDescription = "Create Group", tint = Color.White)
    }
    IconButton(onClick = { /* TODO: Search */ }) {
        Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
    }
    IconButton(onClick = { /* TODO: Filter */ }) {
        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Filter/Sort", tint = Color.White)
    }
}


@Composable
fun ActivityTopBarActions() {
    // --- START OF MODIFICATION ---
    // Add Search icon (placeholder)
    IconButton(onClick = { /* TODO: Search Activity */ }) {
        Icon(Icons.Default.Search, contentDescription = "Search Activity", tint = Color.White)
    }
    // --- END OF MODIFICATION ---

    // Example for Activity: maybe just a filter/sort icon
    IconButton(onClick = { /* TODO: Filter Activity */ }) {
        Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Filter Activity", tint = Color.White)
    }
}