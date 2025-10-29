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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitpay.data.model.GroupWithBalance
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.common.OverallBalanceHeader
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.ErrorRed
import com.example.splitpay.ui.theme.PrimaryBlue
import java.text.NumberFormat
import java.util.Locale

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
    overallBalance: Double,
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
    // ----------------------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Top Section: Overall Balance (Actions/Icons are now in the main AppTopBar)
        OverallBalanceHeader(
            totalBalance = overallBalance,
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
                        onClick = { navController.navigate(Screen.NonGroupDetail) } // Navigate on click
                    )
                    Spacer(Modifier.height(8.dp))
                }

                items(uiState.groupsWithBalances) { groupWithBalance ->
                    // This triggers the onGroupCardClick in ViewModel (GroupsViewModel.kt)
                    GroupBalanceCard(
                        groupWithBalance = groupWithBalance,
                        onClick = { viewModel.onGroupCardClick(groupWithBalance.group.id) }
                    )
                }

                // Placeholder for Settled Up Groups
                item {
                    Spacer(Modifier.height(16.dp))
                    SettledUpGroupButton()
                }
            }
        }
    }
}

@Composable
fun GroupBalanceCard(
    groupWithBalance: GroupWithBalance,
    onClick: () -> Unit
) {
    val balance = groupWithBalance.userNetBalance
    val groupName = groupWithBalance.group.name

    // Determine the text and color based on net balance
    val balanceText = if (balance > 0) "you are owed ${formatCurrency(balance)}"
    else if (balance < 0) "you owe ${formatCurrency(-balance)}"
    else "settled up"

    val balanceColor = when {
        balance > 0 -> Color(0xFF66BB6A) // Green
        balance < 0 -> ErrorRed // Red
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Group Icon/Image Placeholder
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue),
                    contentAlignment = Alignment.Center
                ) {
                    // Placeholder icon for groups without image
                    Icon(
                        Icons.Default.Group,
                        contentDescription = "Group Icon",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }

                Text(
                    text = groupName,
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(start = 12.dp)
                )

                Text(
                    text = balanceText,
                    color = balanceColor,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Simplified Breakdown (Nur A. owes you X, Stevie owes you Y)
            if (balance > 0) {
                Spacer(Modifier.height(8.dp))
                Column(modifier = Modifier.padding(start = 60.dp)) {
                    groupWithBalance.simplifiedOwedBreakdown.forEach { (uid, amount) ->
                        // NOTE: We need user fetching to get the real name, using UID for now
                        Text(
                            text = "User $uid owes you ${formatCurrency(amount)}",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun NonGroupExpensesCard(onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick), // <-- Make Card clickable,
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
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
            Text(
                text = "no expenses",
                color = Color.Gray,
                modifier = Modifier.padding(start = 60.dp)
            )
        }
    }
}

@Composable
fun SettledUpGroupButton() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Hiding groups that have been settled up over one month.",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Button(
            onClick = { /*TODO: Show settled groups*/ },
            shape = RoundedCornerShape(20.dp),
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                containerColor = Color(0xFF2D2D2D),
                contentColor = Color.White
            )
        ) {
            Text("Show 1 settled-up group")
        }
    }
}