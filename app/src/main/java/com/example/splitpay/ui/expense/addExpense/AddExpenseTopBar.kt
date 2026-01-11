package com.example.splitpay.ui.expense.addExpense

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.TextWhite

// --- Top Bar ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseTopBar(
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    isLoading: Boolean,
    isEditMode: Boolean = false
) {
    TopAppBar(
        title = {
            Text(
                if (isEditMode) "Edit expense" else "Add expense",
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onNavigateBack, enabled = !isLoading) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
        },
        actions = {
            IconButton(onClick = onSave, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = TextWhite
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Save Expense", tint = Color(0xFF66BB6A))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
    )
}