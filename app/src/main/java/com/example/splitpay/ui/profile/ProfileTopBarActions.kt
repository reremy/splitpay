package com.example.splitpay.ui.profile

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import com.example.splitpay.ui.theme.PrimaryBlue

// =========================================================================
// NEW: Profile Top Bar Actions
// =========================================================================
@Composable
fun ProfileTopBarActions(onEditProfile: () -> Unit) {
    IconButton(onClick = onEditProfile) {
        Icon(Icons.Default.Edit, contentDescription = "Edit Profile", tint = PrimaryBlue)
    }
}