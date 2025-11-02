package com.example.splitpay.ui.settleUp

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.NegativeRed
import com.example.splitpay.ui.theme.PositiveGreen
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite
import kotlin.math.absoluteValue

// --- ViewModel Factory ---
class SettleUpViewModelFactory(
    private val groupsRepository: GroupsRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SettleUpViewModel::class.java)) {
            return SettleUpViewModel(groupsRepository, expenseRepository, userRepository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettleUpScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    onNavigateToRecordPayment: (String, String, Double) -> Unit, // groupId, memberUid, balance
    onNavigateToMoreOptions: (String) -> Unit, // groupId
    // Provide default repository instances
    groupsRepository: GroupsRepository = GroupsRepository(),
    expenseRepository: ExpenseRepository = ExpenseRepository(),
    userRepository: UserRepository = UserRepository()
) {
    // --- Use SavedStateHandle with factory ---
    val savedStateHandle = SavedStateHandle(mapOf("groupId" to groupId))
    val factory = SettleUpViewModelFactory(groupsRepository, expenseRepository, userRepository, savedStateHandle)
    val viewModel: SettleUpViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            SettleUpUiEvent.NavigateBack -> onNavigateBack()
            is SettleUpUiEvent.NavigateToRecordPayment -> {
                onNavigateToRecordPayment(groupId, event.memberUid, event.balance)
            }
            SettleUpUiEvent.NavigateToMoreOptions -> onNavigateToMoreOptions(groupId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settle Up", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onCloseClick() }) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Select a balance to settle",
                color = Color.Gray,
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Error: ${uiState.error}", color = NegativeRed)
                    }
                }
                uiState.balanceBreakdown.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            "Everyone is settled up in this group!",
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(uiState.balanceBreakdown) { memberBalance ->
                            UserBalanceCard(
                                member = memberBalance,
                                onClick = { viewModel.onMemberClick(memberBalance) }
                            )
                        }

                        item {
                            Spacer(Modifier.height(16.dp))
                            TextButton(
                                onClick = { viewModel.onMoreOptionsClick() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("More options", color = PrimaryBlue, fontSize = 16.sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun UserBalanceCard(
    member: MemberBalanceDetail,
    onClick: () -> Unit
) {
    val (text, color) = when {
        member.amount > 0.01 -> "owes you" to PositiveGreen
        member.amount < -0.01 -> "you owe" to NegativeRed
        else -> "settled up" to Color.Gray
    }
    val amount = member.amount.absoluteValue

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Profile",
                tint = Color.Gray,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(member.memberName, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(text, color = color, fontSize = 12.sp)
                Text(
                    "MYR%.2f".format(amount),
                    color = color,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}