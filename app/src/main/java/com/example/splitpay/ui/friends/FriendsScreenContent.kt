package com.example.splitpay.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle // Placeholder icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection // Import LayoutDirection
import com.example.splitpay.data.model.FriendWithBalance
import com.example.splitpay.ui.common.OverallBalanceHeader
import com.example.splitpay.ui.theme.* // Import your theme colors
import kotlin.math.absoluteValue

// Define DialogBackground if it doesn't exist in Color.kt
val DialogBackground = Color(0xFF2D2D2D) // Example

@Composable
fun FriendsScreenContent(
    innerPadding: PaddingValues,
    // --- Receive ViewModel instance passed from HomeScreen3 ---
    viewModel: FriendsViewModel,
    onFriendClick: (String) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()

    // --- Use a Box to allow anchoring the DropdownMenu ---
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground) // Use your theme background
            // --- REMOVE innerPadding from the main Column ---
        ) {
            // Overall Balance Header - Apply top padding here
            OverallBalanceHeader(
                totalBalance = uiState.totalNetBalance,
                // Pass only the top padding from the Scaffold's innerPadding
                //topPadding = innerPadding.calculateTopPadding()
            )

            // --- FilterChipRow REMOVED ---

            // Friends List Container - Apply remaining padding here
            Box(modifier = Modifier
                .fillMaxSize()
                // Apply horizontal and bottom padding here
                .padding(
                    start = innerPadding.calculateStartPadding(LayoutDirection.Ltr), // Adjust for RTL if needed
                    end = innerPadding.calculateEndPadding(LayoutDirection.Ltr), // Adjust for RTL if needed
                    bottom = innerPadding.calculateBottomPadding()
                )
            ) {
                when {
                    // ... (Loading, Empty states) ...
                    else -> {
                        LazyColumn( /* ... */ ) {
                            items(uiState.filteredAndSearchedFriends, key = { it.uid }) { friend ->
                                FriendListItem(
                                    friend = friend,
                                    // --- Pass the click lambda ---
                                    onFriendClick = { onFriendClick(friend.uid) } // Pass ID back up
                                )
                            }
                        }
                    }
                }
            } // End of Friends List Container Box
        }

        // --- DropdownMenu ---
        DropdownMenu(
            expanded = uiState.isFilterMenuExpanded,
            onDismissRequest = viewModel::onDismissFilterMenu,
            modifier = Modifier
                .background(DialogBackground)
                .align(Alignment.TopEnd)
                // Adjust padding relative to the top bar height if needed
                .padding(end = 4.dp, top = innerPadding.calculateTopPadding() + 4.dp)
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
                    onClick = { viewModel.applyFilter(type) },
                    leadingIcon = {
                        RadioButton(
                            selected = (type == uiState.currentFilter),
                            onClick = { viewModel.applyFilter(type) },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = PrimaryBlue,
                                unselectedColor = Color.Gray
                            )
                        )
                    },
                    contentPadding = PaddingValues(horizontal = 12.dp)
                )
            }
        } // End DropdownMenu
    } // End of outer Box
}




// --- FilterChipRow and FriendFilterChip composables REMOVED ---


@Composable
fun FriendListItem(
    friend: FriendWithBalance,
    onFriendClick: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onFriendClick(friend.uid) }
            .padding(vertical = 12.dp), // Padding applied within LazyColumn item
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Placeholder for Profile Picture
        Icon(
            imageVector = Icons.Default.AccountCircle,
            contentDescription = "Profile",
            tint = Color.Gray,
            modifier = Modifier.size(40.dp)
        )

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(friend.username, color = TextWhite, fontWeight = FontWeight.Medium)

            // Balance Breakdown
            if (friend.balanceBreakdown.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                friend.balanceBreakdown.forEach { detail ->
                    val detailText = when {
                        detail.amount > 0.01 -> "owes you MYR%.2f for '%s'".format(detail.amount, detail.groupName)
                        detail.amount < -0.01 -> "you owe MYR%.2f for '%s'".format(detail.amount.absoluteValue, detail.groupName)
                        else -> null
                    }
                    if (detailText != null) {
                        Text(
                            detailText,
                            color = if (detail.amount > 0) PositiveGreen else NegativeRed,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        Spacer(Modifier.width(16.dp))

        // Net Balance Display
        when {
            friend.netBalance > 0.01 -> {
                Column(horizontalAlignment = Alignment.End) {
                    Text("owes you", fontSize = 12.sp, color = PositiveGreen)
                    Text(
                        "MYR%.2f".format(friend.netBalance),
                        color = PositiveGreen,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            friend.netBalance < -0.01 -> {
                Column(horizontalAlignment = Alignment.End) {
                    Text("you owe", fontSize = 12.sp, color = NegativeRed)
                    Text(
                        "MYR%.2f".format(friend.netBalance.absoluteValue),
                        color = NegativeRed,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            else -> {
                Text("settled up", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
    // Divider is handled by LazyColumn's verticalArrangement
}

// --- Make sure these colors are defined in ui/theme/Color.kt ---
// val PositiveGreen = Color(0xFF66BB6A)
// val NegativeRed = Color(0xFFE57373)