package com.example.splitpay.ui.groups

import android.R.attr.onClick
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import coil.compose.AsyncImage
import com.example.splitpay.data.model.GroupWithBalance
import com.example.splitpay.data.model.User
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.common.OverallBalanceHeader
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.groups.availableTagsMap
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.ErrorRed
import com.example.splitpay.ui.theme.NegativeRed
import com.example.splitpay.ui.theme.PositiveGreen
import com.example.splitpay.ui.theme.PrimaryBlue
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.absoluteValue

// Utility function to format currency
@Composable
fun formatCurrency(amount: Double): String {
    val formatter = NumberFormat.getCurrencyInstance(Locale("ms", "MY")) // Using Malay/Malaysia locale for MYR format
    formatter.maximumFractionDigits = 2
    formatter.currency = java.util.Currency.getInstance("MYR")
    return formatter.format(amount)
}


@Composable
fun GroupsContent(
    innerPadding: PaddingValues,
    overallBalance: Double, // This is from HomeViewModel, we will ignore it for the header
    viewModel: GroupsViewModel = viewModel(),
    onNavigate: (GroupsUiEvent) -> Unit,
    navController: NavHostController
) {
    val uiState by viewModel.uiState.collectAsState()

    // --- FIX: Relaying the Navigation Event to the main NavHost ---
    UiEventHandler(viewModel.uiEvent) { event ->
        // This relays the specific navigation event (including the groupId)
        // up to HomeScreen3 for execution by the mainNavController.
        when (event) {
            is GroupsUiEvent.NavigateToGroupDetail -> onNavigate(event)
            GroupsUiEvent.NavigateToCreateGroup -> onNavigate(event)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // --- START OF FIX ---
        // Calculate the total balance *for this tab* from the GroupsViewModel's state
        val groupsBalance = uiState.groupsWithBalances.sumOf { it.userNetBalance }
        val nonGroupBalance = uiState.nonGroupBalance
        val groupsTabTotalBalance = groupsBalance + nonGroupBalance
        // --- END OF FIX ---


        // Top Section: Overall Balance
        OverallBalanceHeader(
            // Use the correctly calculated total for *this tab*
            totalBalance = groupsTabTotalBalance,
            // Pass only the top padding from the Scaffold's innerPadding
            //topPadding = innerPadding.calculateTopPadding()
        )

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = Color.Red)
            }
        } else {
            // Main List of Groups and Balances
            LazyColumn(
                modifier = Modifier.weight(1f),
                // Applying padding to the LazyColumn content
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Placeholder for non-group expenses
                item {
                    NonGroupExpensesCard(
                        // --- START OF FIX ---
                        // Pass the balance from the uiState
                        balance = uiState.nonGroupBalance,
                        // --- END OF FIX ---
                        onClick = { navController.navigate(Screen.NonGroupDetail) } // Navigate on click
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(uiState.filteredGroupsWithBalances) { groupWithBalance ->
                    GroupBalanceCard(
                        groupWithBalance = groupWithBalance,
                        membersMap = uiState.membersMap, // <-- PASS membersMap
                        onClick = { viewModel.onGroupCardClick(groupWithBalance.group.id) }
                    )
                }

                // Settled Up Groups Toggle Button
                if (uiState.settledGroupsCount > 0 && !uiState.showSettledGroups) {
                    item {
                        Spacer(Modifier.height(16.dp))
                        SettledUpGroupButton(
                            count = uiState.settledGroupsCount,
                            onClick = viewModel::toggleShowSettledGroups
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GroupBalanceCard(
    groupWithBalance: GroupWithBalance,
    membersMap: Map<String, User>,
    onClick: () -> Unit
) {
    val balance = groupWithBalance.userNetBalance
    val groupName = groupWithBalance.group.name
    val breakdown = groupWithBalance.simplifiedOwedBreakdown

    // Determine the text and color based on net balance
    val balanceText = if (balance > 0.01) "you are owed ${formatCurrency(balance)}"
    else if (balance < -0.01) "you owe ${formatCurrency(-balance)}"
    else "settled up"

    val balanceColor = when {
        balance > 0.01 -> PositiveGreen // Green
        balance < -0.01 -> ErrorRed // Red
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Group Photo or Tag Icon
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (groupWithBalance.group.photoUrl.isNotEmpty()) Color.Transparent else PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    if (groupWithBalance.group.photoUrl.isNotEmpty()) {
                        // Display group photo
                        AsyncImage(
                            model = groupWithBalance.group.photoUrl,
                            contentDescription = "Group Photo",
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        // Display tag icon
                        val tagIcon = availableTagsMap[groupWithBalance.group.iconIdentifier] ?: Icons.Default.Group
                        Icon(
                            tagIcon,
                            contentDescription = "Group Tag",
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Text(
                    text = groupName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )

                Column(horizontalAlignment = Alignment.End) {
                    // Overall Balance Text
                    Text(
                        text = balanceText,
                        color = balanceColor,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // --- Simplified Breakdown ---
                    if (breakdown.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp)) // Small space

                        // Iterate through the breakdown map
                        breakdown.entries.sortedByDescending { it.value.absoluteValue }.forEach { (uid, amount) ->
                            // Look up name from membersMap
                            val name = membersMap[uid]?.username ?: membersMap[uid]?.fullName ?: "User..."

                            // Determine text and color based on amount sign
                            val (text, color) = when {
                                amount > 0.01 -> "$name owes you ${formatCurrency(amount)}" to PositiveGreen
                                amount < -0.01 -> "You owe $name ${formatCurrency(amount.absoluteValue)}" to NegativeRed
                                else -> "" to Color.Gray
                            }

                            if (text.isNotEmpty()) {
                                Text(
                                    text = text,
                                    color = color,
                                    fontSize = 14.sp
                                    // No alignment modifier needed; parent Column handles it
                                )
                            }
                        }
                    }
                }
            }

        }
    }
}

@Composable
fun NonGroupExpensesCard(
    balance: Double, // <-- ADDED PARAMETER
    onClick: () -> Unit
) {
    // --- START OF FIX ---
    // Determine text and color based on the passed-in balance
    val balanceText = if (balance > 0.01) "you are owed ${formatCurrency(balance)}"
    else if (balance < -0.01) "you owe ${formatCurrency(balance.absoluteValue)}"
    else "no expenses"

    val balanceColor = when {
        balance > 0.01 -> PositiveGreen
        balance < -0.01 -> NegativeRed
        else -> Color.Gray
    }
    // --- END OF FIX ---

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // <-- Make Card clickable
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF00C853)), // Distinct color for Non-Group
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "Non-Group Icon",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Text(
                    text = "Non-group expenses",
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(start = 12.dp)
                )
            }
            Spacer(Modifier.height(4.dp))
            // --- START OF FIX ---
            // Display the calculated balance text
            Text(
                text = balanceText,
                color = balanceColor,
                modifier = Modifier.padding(start = 60.dp),
                // Make font bold if there is a balance
                fontWeight = if (balance.absoluteValue > 0.01) FontWeight.Bold else FontWeight.Normal
            )
            // --- END OF FIX ---
        }
    }
}

@Composable
fun SettledUpGroupButton(
    count: Int,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hiding groups that have been settled up over one month.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(
            onClick = onClick,
            shape = RoundedCornerShape(20.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = Color.White
            )
        ) {
            val text = if (count == 1) {
                "Show 1 settled-up group"
            } else {
                "Show $count settled-up groups"
            }
            Text(text)
        }
    }
}