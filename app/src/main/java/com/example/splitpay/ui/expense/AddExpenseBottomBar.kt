package com.example.splitpay.ui.expense

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Receipt
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
    isGroupSelected: Boolean, // NEW
    currency: String,
    onChooseGroupClick: () -> Unit
) {
    BottomAppBar(
        containerColor = DarkBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Choose Group Button
            TextButton(onClick = onChooseGroupClick) {
                Icon(Icons.Default.Group, contentDescription = "Group", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    // --- UPDATED TEXT ---
                    text = selectedGroup?.name ?: "Non-group expense",
                    color = if (isGroupSelected) PrimaryBlue else Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Action Icons (Calendar, Camera, Tags)
            Row {
                IconButton(onClick = { /* TODO: Date picker */ }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = Color.Gray)
                }
                IconButton(onClick = { /* TODO: Receipt camera */ }) {
                    Icon(Icons.Default.Receipt, contentDescription = "Receipt", tint = Color.Gray)
                }
                // --- UPDATED: Icon and ContentDescription ---
                IconButton(onClick = { /* TODO: Memo dialog */ }) {
                    Icon(Icons.Default.Notes, contentDescription = "Memo", tint = Color.Gray)
                }
            }
        }
    }
}