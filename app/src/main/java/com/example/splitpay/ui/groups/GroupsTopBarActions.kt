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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitpay.ui.theme.PrimaryBlue

@Composable
fun GroupsTopBarActions(
    searchQuery: String,
    selectedFilter: GroupFilter,
    showFilterDropdown: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onFilterSelected: (GroupFilter) -> Unit,
    onToggleFilterDropdown: () -> Unit,
    onNavigateToCreateGroup: () -> Unit
) {
    var showSearchBar by remember { mutableStateOf(false) }

    if (showSearchBar) {
        // Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search groups...", color = Color.Gray) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF2D2D2D),
                    unfocusedContainerColor = Color(0xFF2D2D2D),
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = PrimaryBlue,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                singleLine = true
            )
            IconButton(onClick = {
                showSearchBar = false
                onSearchQueryChange("")
            }) {
                Icon(Icons.Default.Close, contentDescription = "Close Search", tint = Color.White)
            }
        }
    } else {
        // Normal Actions Row
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Create Group Button
            Button(
                onClick = onNavigateToCreateGroup,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryBlue,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Create Group", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            }

            // Search Icon
            IconButton(onClick = { showSearchBar = true }) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            }

            // Filter Icon with Dropdown
            Box {
                IconButton(onClick = onToggleFilterDropdown) {
                    Icon(
                        Icons.AutoMirrored.Filled.Sort,
                        contentDescription = "Filter",
                        tint = if (selectedFilter != GroupFilter.ALL_GROUPS) PrimaryBlue else Color.White
                    )
                }

                DropdownMenu(
                    expanded = showFilterDropdown,
                    onDismissRequest = onToggleFilterDropdown,
                    modifier = Modifier
                        .background(Color(0xFF2D2D2D))
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
                unselectedColor = Color.Gray
            )
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = text,
            color = if (selected) Color.White else Color.Gray,
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
