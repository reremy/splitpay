package com.example.splitpay.ui.friends.friendsTab

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.splitpay.ui.theme.DialogBackground
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite

@Composable
fun FriendsTopBarActions(
    onSearchClick: () -> Unit = {},
    onAddFriendClick: () -> Unit,
    onFilterClick: () -> Unit,
    isFilterMenuExpanded: Boolean = false,
    currentFilter: FriendFilterType = FriendFilterType.ALL,
    onDismissFilterMenu: () -> Unit = {},
    onApplyFilter: (FriendFilterType) -> Unit = {}
) {
    // Add Friend Button (leftmost)
    IconButton(onClick = onAddFriendClick) {
        Icon(
            imageVector = Icons.Default.PersonAdd,
            contentDescription = "Add Friend",
            tint = Color.White
        )
    }

    // Search Button (middle)
    IconButton(onClick = onSearchClick) {
        Icon(
            imageVector = Icons.Default.Search,
            contentDescription = "Search Friends",
            tint = Color.White
        )
    }

    // Filter Button with Dropdown (rightmost)
    Box {
        IconButton(onClick = onFilterClick) {
            Icon(
                imageVector = Icons.Default.FilterList,
                contentDescription = "Filter Friends",
                tint = Color.White
            )
        }

        // Dropdown Menu anchored to the filter button
        DropdownMenu(
            expanded = isFilterMenuExpanded,
            onDismissRequest = onDismissFilterMenu,
            modifier = Modifier.background(DialogBackground)
        ) {
            val filterOptions = mapOf(
                FriendFilterType.ALL to "All friends",
                FriendFilterType.OUTSTANDING to "Outstanding balances",
                FriendFilterType.OWES_YOU to "Friends who owe you",
                FriendFilterType.YOU_OWE to "Friends you owe"
            )

            filterOptions.forEach { (type, text) ->
                DropdownMenuItem(
                    text = { Text(text, color = TextWhite) },
                    onClick = { onApplyFilter(type) },
                    leadingIcon = {
                        RadioButton(
                            selected = (type == currentFilter),
                            onClick = { onApplyFilter(type) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrimaryBlue,
                                unselectedColor = Color.Gray
                            )
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                )
            }
        }
    }
}