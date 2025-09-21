package com.example.splitpay.ui.home

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.graphics.vector.ImageVector

data class HomeUiState(
    val selectedItemIndex: Int = 0,
    val items: List<HomeNavItem> = listOf(
        HomeNavItem("groups_screen", "Groups", Icons.Default.Home),
        HomeNavItem("friends_screen", "Friends", Icons.Default.Person),
        HomeNavItem("activity_screen", "Activity", Icons.Default.List),
        HomeNavItem("profile_screen", "Account", Icons.Default.AccountCircle)
    )
)

data class HomeNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)
