package com.example.splitpay.ui.activity.activityTab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.splitpay.ui.theme.DialogBackground
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextPlaceholder
import com.example.splitpay.ui.theme.TextWhite

@Composable
fun ActivityTopBarActions(
    isSearchActive: Boolean = false,
    searchQuery: String = "",
    onSearchClick: () -> Unit = {},
    onSearchQueryChange: (String) -> Unit = {},
    onSearchDismiss: () -> Unit = {},
    onFilterClick: () -> Unit = {},
    isFilterMenuExpanded: Boolean = false,
    onDismissFilterMenu: () -> Unit = {},
    activityFilter: ActivityFilterType = ActivityFilterType.ALL,
    timePeriodFilter: TimePeriodFilter = TimePeriodFilter.ALL_TIME,
    sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    onActivityFilterChange: (ActivityFilterType) -> Unit = {},
    onTimePeriodFilterChange: (TimePeriodFilter) -> Unit = {},
    onSortOrderChange: (SortOrder) -> Unit = {}
) {
    if (isSearchActive) {
        // Show search bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = 8.dp),
            horizontalArrangement = Arrangement.End
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                placeholder = { Text("Search activities...", color = TextPlaceholder) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surface,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    cursorColor = PrimaryBlue,
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                shape = RoundedCornerShape(8.dp)
            )
            Spacer(Modifier.width(4.dp))
            IconButton(onClick = onSearchDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close search", tint = Color.White)
            }
        }
    } else {
        // Show search and filter icons
        IconButton(onClick = onSearchClick) {
            Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
        }

        Box {
            IconButton(onClick = onFilterClick) {
                Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = "Filter", tint = Color.White)
            }

            // Filter dropdown menu
            DropdownMenu(
                expanded = isFilterMenuExpanded,
                onDismissRequest = onDismissFilterMenu,
                modifier = Modifier
                    .background(DialogBackground)
                    .width(250.dp)
            ) {
                // Activity Type Section
                Text(
                    text = "Activity Type",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                ActivityFilterType.values().forEach { filter ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (filter) {
                                    ActivityFilterType.ALL -> "All Activities"
                                    ActivityFilterType.EXPENSES -> "Expenses Only"
                                    ActivityFilterType.PAYMENTS -> "Payments Only"
                                    ActivityFilterType.GROUP_ACTIVITIES -> "Group Activities"
                                },
                                color = TextWhite
                            )
                        },
                        onClick = {
                            onActivityFilterChange(filter)
                            onDismissFilterMenu()
                        },
                        leadingIcon = {
                            RadioButton(
                                selected = filter == activityFilter,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryBlue,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                // Time Period Section
                Text(
                    text = "Time Period",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                TimePeriodFilter.values().forEach { filter ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (filter) {
                                    TimePeriodFilter.ALL_TIME -> "All Time"
                                    TimePeriodFilter.TODAY -> "Today"
                                    TimePeriodFilter.THIS_WEEK -> "This Week"
                                    TimePeriodFilter.THIS_MONTH -> "This Month"
                                },
                                color = TextWhite
                            )
                        },
                        onClick = {
                            onTimePeriodFilterChange(filter)
                            onDismissFilterMenu()
                        },
                        leadingIcon = {
                            RadioButton(
                                selected = filter == timePeriodFilter,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryBlue,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    )
                }

                Divider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 8.dp))

                // Sort Order Section
                Text(
                    text = "Sort By",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )

                SortOrder.values().forEach { order ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = when (order) {
                                    SortOrder.NEWEST_FIRST -> "Newest First"
                                    SortOrder.OLDEST_FIRST -> "Oldest First"
                                },
                                color = TextWhite
                            )
                        },
                        onClick = {
                            onSortOrderChange(order)
                            onDismissFilterMenu()
                        },
                        leadingIcon = {
                            RadioButton(
                                selected = order == sortOrder,
                                onClick = null,
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = PrimaryBlue,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                        },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
