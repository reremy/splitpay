package com.example.splitpay.ui.friends

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle // Placeholder icon
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection // Import LayoutDirection
import coil.compose.AsyncImage
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(16.dp),
                spotColor = Color.Black.copy(alpha = 0.25f)
            )
            .clickable { onFriendClick(friend.uid) },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D2D)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture with AsyncImage
            if (friend.profilePictureUrl?.isNotEmpty() == true) {
                AsyncImage(
                    model = friend.profilePictureUrl,
                    contentDescription = "${friend.username}'s profile",
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Profile",
                    tint = Color.Gray,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = friend.username,
                    color = TextWhite,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )

                // Balance Breakdown
                if (friend.balanceBreakdown.isNotEmpty()) {
                    Spacer(Modifier.height(6.dp))
                    friend.balanceBreakdown.take(2).forEach { detail ->
                        val detailText = when {
                            detail.amount > 0.01 -> "owes you MYR%.2f for '%s'".format(detail.amount, detail.groupName)
                            detail.amount < -0.01 -> "you owe MYR%.2f for '%s'".format(detail.amount.absoluteValue, detail.groupName)
                            else -> null
                        }
                        if (detailText != null) {
                            Text(
                                text = detailText,
                                color = if (detail.amount > 0) PositiveGreen else NegativeRed,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    // Show "and more" if there are more than 2
                    if (friend.balanceBreakdown.size > 2) {
                        Text(
                            text = "and ${friend.balanceBreakdown.size - 2} more...",
                            color = Color.Gray,
                            fontSize = 12.sp,
                            fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                        )
                    }
                }
            }

            Spacer(Modifier.width(16.dp))

            // Net Balance Display
            when {
                friend.netBalance > 0.01 -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "owes you",
                            fontSize = 13.sp,
                            color = PositiveGreen.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "MYR%.2f".format(friend.netBalance),
                            color = PositiveGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                friend.netBalance < -0.01 -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "you owe",
                            fontSize = 13.sp,
                            color = NegativeRed.copy(alpha = 0.8f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "MYR%.2f".format(friend.netBalance.absoluteValue),
                            color = NegativeRed,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                else -> {
                    Text(
                        text = "settled up",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

// --- Make sure these colors are defined in ui/theme/Color.kt ---
// val PositiveGreen = Color(0xFF66BB6A)
// val NegativeRed = Color(0xFFE57373)