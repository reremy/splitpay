package com.example.splitpay.ui.friendSettleUp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.ExpenseType
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.groups.availableTagsMap
import com.example.splitpay.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// Data class for balance by group
data class GroupBalance(
    val groupId: String,
    val groupName: String,
    val groupIconIdentifier: String?,
    val balance: Double,
    val group: Group?
)

// UI State
data class SelectBalanceUiState(
    val isLoading: Boolean = true,
    val friendName: String = "",
    val totalBalance: Double = 0.0,
    val groupBalances: List<GroupBalance> = emptyList(),
    val error: String? = null
)

// ViewModel
class SelectBalanceToSettleViewModel(
    private val friendId: String,
    private val userRepository: UserRepository = UserRepository(),
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectBalanceUiState())
    val uiState: StateFlow<SelectBalanceUiState> = _uiState.asStateFlow()

    private val currentUserId = userRepository.getCurrentUser()?.uid

    init {
        loadBalances()
    }

    private fun loadBalances() {
        if (currentUserId == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "User not logged in"
            )
            return
        }

        viewModelScope.launch {
            try {
                // Get friend info
                val friend = userRepository.getUserProfile(friendId)
                if (friend == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Friend not found"
                    )
                    return@launch
                }

                // Get all groups both users are in
                val allGroups = groupsRepository.getGroupsSuspend()
                val sharedGroups = allGroups.filter { group: Group ->
                    group.members.contains(currentUserId) && group.members.contains(friendId)
                }

                // Calculate balance for each group
                val groupBalancesList = mutableListOf<GroupBalance>()
                var totalBalance = 0.0

                for (group in sharedGroups) {
                    val expenses = expenseRepository.getExpensesForGroups(listOf(group.id))
                    var groupBalance = 0.0

                    for (expense in expenses) {
                        // Only consider expenses where both users are involved
                        val currentUserInvolved = expense.participants.any { it.uid == currentUserId } ||
                                                 expense.paidBy.any { it.uid == currentUserId }
                        val friendInvolved = expense.participants.any { it.uid == friendId } ||
                                           expense.paidBy.any { it.uid == friendId }

                        if (!currentUserInvolved || !friendInvolved) continue

                        val paidByCurrentUser = expense.paidBy.find { it.uid == currentUserId }?.paidAmount ?: 0.0
                        val paidByFriend = expense.paidBy.find { it.uid == friendId }?.paidAmount ?: 0.0
                        val shareOwedByCurrentUser = expense.participants.find { it.uid == currentUserId }?.owesAmount ?: 0.0
                        val shareOwedByFriend = expense.participants.find { it.uid == friendId }?.owesAmount ?: 0.0

                        // Calculate balance change for this expense between the two users
                        val balanceChange = if (expense.expenseType == ExpenseType.PAYMENT) {
                            // Direct payment between users
                            when {
                                paidByCurrentUser > 0 -> paidByCurrentUser  // I paid the friend
                                paidByFriend > 0 -> -paidByFriend            // The friend paid me
                                else -> 0.0
                            }
                        } else {
                            // Shared expense - calculate based on net contributions
                            val netContributionCurrentUser = paidByCurrentUser - shareOwedByCurrentUser
                            val netContributionFriend = paidByFriend - shareOwedByFriend
                            val numParticipants = expense.participants.size.toDouble().coerceAtLeast(1.0)

                            (netContributionCurrentUser - netContributionFriend) / numParticipants
                        }

                        groupBalance += balanceChange
                    }

                    // Only add if balance is non-zero
                    if (groupBalance.absoluteValue > 0.01) {
                        groupBalancesList.add(
                            GroupBalance(
                                groupId = group.id,
                                groupName = group.name,
                                groupIconIdentifier = group.iconIdentifier,
                                balance = groupBalance,
                                group = group
                            )
                        )
                        totalBalance += groupBalance
                    }
                }

                // Sort by balance magnitude (highest first)
                groupBalancesList.sortByDescending { it.balance.absoluteValue }

                _uiState.value = SelectBalanceUiState(
                    isLoading = false,
                    friendName = friend.fullName.ifBlank { friend.username },
                    totalBalance = totalBalance,
                    groupBalances = groupBalancesList,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error loading balances: ${e.message}"
                )
            }
        }
    }
}

// ViewModel Factory
class SelectBalanceToSettleViewModelFactory(
    private val friendId: String,
    private val userRepository: UserRepository,
    private val groupsRepository: GroupsRepository,
    private val expenseRepository: ExpenseRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectBalanceToSettleViewModel::class.java)) {
            return SelectBalanceToSettleViewModel(
                friendId,
                userRepository,
                groupsRepository,
                expenseRepository
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectBalanceToSettleScreen(
    friendId: String,
    onNavigateBack: () -> Unit,
    onGroupBalanceClick: (String, Double) -> Unit, // groupId, balance
    onMoreOptionsClick: () -> Unit,
    userRepository: UserRepository = UserRepository(),
    groupsRepository: GroupsRepository = GroupsRepository(),
    expenseRepository: ExpenseRepository = ExpenseRepository()
) {
    val factory = SelectBalanceToSettleViewModelFactory(
        friendId,
        userRepository,
        groupsRepository,
        expenseRepository
    )
    val viewModel: SelectBalanceToSettleViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Select a balance to settle with ${uiState.friendName}",
                        color = TextWhite,
                        fontSize = 18.sp,
                        maxLines = 2
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = TextWhite
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = DarkBackground
                )
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        when {
            uiState.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
            uiState.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = NegativeRed,
                        fontSize = 16.sp
                    )
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    // Overall Balance Card
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = DialogBackground
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "Total Balance",
                                    color = Color.Gray,
                                    fontSize = 14.sp
                                )
                                Spacer(Modifier.height(8.dp))

                                val balanceText = when {
                                    uiState.totalBalance > 0.01 -> "You are owed MYR%.2f".format(uiState.totalBalance)
                                    uiState.totalBalance < -0.01 -> "You owe MYR%.2f".format(uiState.totalBalance.absoluteValue)
                                    else -> "Settled up"
                                }
                                val balanceColor = when {
                                    uiState.totalBalance > 0.01 -> PositiveGreen
                                    uiState.totalBalance < -0.01 -> NegativeRed
                                    else -> Color.Gray
                                }

                                Text(
                                    text = balanceText,
                                    color = balanceColor,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Group Balances Section
                    if (uiState.groupBalances.isNotEmpty()) {
                        item {
                            Text(
                                text = "Select a group to settle",
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        items(uiState.groupBalances) { groupBalance ->
                            GroupBalanceCard(
                                groupBalance = groupBalance,
                                onClick = { onGroupBalanceClick(groupBalance.groupId, groupBalance.balance) }
                            )
                        }
                    } else {
                        item {
                            Text(
                                text = "No outstanding balances",
                                color = Color.Gray,
                                fontSize = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 32.dp)
                            )
                        }
                    }

                    // More Options Button
                    item {
                        Button(
                            onClick = onMoreOptionsClick,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = PrimaryBlue
                            )
                        ) {
                            Text(
                                text = "More options",
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun GroupBalanceCard(
    groupBalance: GroupBalance,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DialogBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group Icon
            val groupIcon = groupBalance.groupIconIdentifier?.let { availableTagsMap[it] } ?: Icons.Default.Group
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(PrimaryBlue.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = groupIcon,
                    contentDescription = "Group",
                    tint = PrimaryBlue,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(Modifier.width(16.dp))

            // Group Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = groupBalance.groupName,
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(Modifier.width(16.dp))

            // Balance
            val balanceText = when {
                groupBalance.balance > 0.01 -> "You are owed\nMYR%.2f".format(groupBalance.balance)
                groupBalance.balance < -0.01 -> "You owe\nMYR%.2f".format(groupBalance.balance.absoluteValue)
                else -> "Settled"
            }
            val balanceColor = when {
                groupBalance.balance > 0.01 -> PositiveGreen
                groupBalance.balance < -0.01 -> NegativeRed
                else -> Color.Gray
            }

            Text(
                text = balanceText,
                color = balanceColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.End
            )
        }
    }
}
