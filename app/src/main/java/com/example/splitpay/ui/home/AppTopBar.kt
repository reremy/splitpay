package com.example.splitpay.ui.home

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow

// AppTopBar - Centralized TopAppBar Component
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(
    title: String,
    scrollBehavior: androidx.compose.material3.TopAppBarScrollBehavior,
    // Define a composable lambda for actions that takes the title for context
    actions: @Composable (androidx.compose.foundation.layout.RowScope.() -> Unit)
) {
    TopAppBar(
        title = {
            Text(text = title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        actions = actions, // Inject the customized actions here
        scrollBehavior = scrollBehavior,

        colors = TopAppBarDefaults.topAppBarColors(
            // 1. TOP BAR BACKGROUND COLOR
            containerColor = Color(0xFF1E1E1E), // Example hardcoded background (DarkBackground)

            // FIX: Explicitly set the Scrolled Container Color to the same dark color.
            scrolledContainerColor = Color(0xFF1E1E1E), // <-- ADDED/FIXED THIS LINE

            // 2. TITLE TEXT COLOR
            titleContentColor = Color(0xFFFFFFFF), // Example title text color (TextWhite)

            // 3. ACTION ICON COLORS (Menu, Search, Filter, etc.)
            actionIconContentColor = Color(0xFFFFFFFF) // Example icon color (TextWhite)
        )
    )
}