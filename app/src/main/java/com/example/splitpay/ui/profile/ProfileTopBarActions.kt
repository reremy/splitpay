package com.example.splitpay.ui.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// =========================================================================
// NEW: Profile Top Bar Actions
// =========================================================================
@Composable
fun ProfileTopBarActions(onEditProfile: () -> Unit) {
    IconButton(onClick = onEditProfile) {
        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = Color.White)
    }
}