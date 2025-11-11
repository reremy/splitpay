package com.example.splitpay.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.material3.MaterialTheme
import com.example.splitpay.ui.theme.NegativeRed
import com.example.splitpay.ui.theme.PositiveGreen
import com.example.splitpay.ui.theme.PrimaryBlue

@Composable
fun GroupsTopBarActions(
    selectedFilter: GroupFilter,
    showFilterDropdown: Boolean,
    onSearchIconClick: () -> Unit,
    onFilterSelected: (GroupFilter) -> Unit,
    onToggleFilterDropdown: () -> Unit,
    onNavigateToCreateGroup: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Create Group Icon
        IconButton(onClick = onNavigateToCreateGroup) {
            Icon(Icons.Default.Add, contentDescription = "Create Group", tint = Color.White)
        }

        // Search Icon
        IconButton(onClick = onSearchIconClick) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
        }

        // Filter Icon with Dropdown
        Box {
            val filterIconColor = when (selectedFilter) {
                GroupFilter.ALL_GROUPS -> Color.White
                GroupFilter.OUTSTANDING_BALANCES -> Color(0xFFFF9800) // Orange (mix of red and green)
                GroupFilter.GROUPS_YOU_OWE -> NegativeRed
                GroupFilter.GROUPS_THAT_OWE_YOU -> PositiveGreen
            }

            IconButton(onClick = onToggleFilterDropdown) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Filter",
                    tint = filterIconColor
                )
            }

            DropdownMenu(
                expanded = showFilterDropdown,
                onDismissRequest = onToggleFilterDropdown,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        "Filter Groups",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )

                    FilterOption(
                        text = "All Groups",
                        selected = selectedFilter == GroupFilter.ALL_GROUPS,
                        onClick = { onFilterSelected(GroupFilter.ALL_GROUPS) }
                    )
                    FilterOption(
                        text = "Outstanding Balances",
                        selected = selectedFilter == GroupFilter.OUTSTANDING_BALANCES,
                        onClick = { onFilterSelected(GroupFilter.OUTSTANDING_BALANCES) }
                    )
                    FilterOption(
                        text = "Groups You Owe",
                        selected = selectedFilter == GroupFilter.GROUPS_YOU_OWE,
                        onClick = { onFilterSelected(GroupFilter.GROUPS_YOU_OWE) }
                    )
                    FilterOption(
                        text = "Groups That Owe You",
                        selected = selectedFilter == GroupFilter.GROUPS_THAT_OWE_YOU,
                        onClick = { onFilterSelected(GroupFilter.GROUPS_THAT_OWE_YOU) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterOption(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = selected,
            onClick = onClick,
            colors = RadioButtonDefaults.colors(
                selectedColor = PrimaryBlue,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = if (selected) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            fontWeight = if (selected) FontWeight.Medium else FontWeight.Normal
        )
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
