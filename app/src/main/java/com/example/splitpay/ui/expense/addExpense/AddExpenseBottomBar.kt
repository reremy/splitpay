package com.example.splitpay.ui.expense.addExpense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material3.BottomAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.splitpay.data.model.Group
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.PrimaryBlue

// --- Bottom Bar ---
@Composable
fun AddExpenseBottomBar(
    selectedGroup: Group?,
    initialGroupId: String?,
    currentGroupId: String?,
    currency: String,
    onChooseGroupClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onCameraClick: () -> Unit,
    onMemoClick: () -> Unit,
    onCategoryClick: () -> Unit = {},
    isEditMode: Boolean = false
) {

    val (buttonText, buttonColor) = when {
        // 1. A specific group object has been selected/loaded.
        selectedGroup != null -> selectedGroup.name to PrimaryBlue

        // 2. "Non-group" has been explicitly selected (currentGroupId is null)
        // AND it wasn't the initial state (initialGroupId was non-null when starting).
        currentGroupId == null && initialGroupId != null -> "Non-group expense" to PrimaryBlue

        // 3. User manually selected "Non-group" (currentGroupId is null) when starting from scratch (initialGroupId was also null).
        // We know initialGroupId must be null if we reach here and currentGroupId is null.
        // We also know selectedGroup is null from condition 1 failing.
        currentGroupId == null -> "Non-group expense" to PrimaryBlue

        // 4. Initial state: No group loaded/selected yet, OR group is loading.
        else -> "Choose group" to Color.Gray
    }

    BottomAppBar(
        containerColor = DarkBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Choose Group Button (disabled in edit mode)
            TextButton(
                onClick = onChooseGroupClick,
                enabled = !isEditMode
            ) {
                Icon(
                    Icons.Default.Group,
                    contentDescription = "Group",
                    tint = if (isEditMode) Color.Gray else PrimaryBlue,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = buttonText,
                    color = if (isEditMode) Color.Gray else buttonColor,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Action Icons (Calendar, Camera, Category, Memo)
            Row {
                IconButton(onClick = onCalendarClick) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = Color.Gray)
                }
                IconButton(onClick = onCameraClick) {
                    Icon(Icons.Default.CameraAlt, contentDescription = "Add expense image", tint = Color.Gray)
                }
                IconButton(onClick = onCategoryClick) {
                    Icon(Icons.Default.Category, contentDescription = "Category", tint = Color.Gray)
                }
                IconButton(onClick = onMemoClick) {
                    Icon(Icons.Default.Notes, contentDescription = "Memo", tint = Color.Gray)
                }
            }
        }
    }
}