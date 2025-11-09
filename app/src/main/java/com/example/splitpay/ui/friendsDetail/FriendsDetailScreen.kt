package com.example.splitpay.ui.friendsDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitpay.data.model.ActivityType
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.groups.ExpenseActivityCard
import com.example.splitpay.ui.theme.*
import kotlin.math.absoluteValue

// ViewModel Factory
class FriendsDetailViewModelFactory(
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    private val groupsRepository: GroupsRepository,
    private val activityRepository: ActivityRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendsDetailViewModel::class.java)) {
            return FriendsDetailViewModel(
                userRepository,
                expenseRepository,
                groupsRepository,
                activityRepository,
                savedStateHandle
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsDetailScreen(
    friendId: String,
    navController: NavHostController,
    onNavigateBack: () -> Unit,
    userRepository: UserRepository = UserRepository(),
    expenseRepository: ExpenseRepository = ExpenseRepository(),
    groupsRepository: GroupsRepository = GroupsRepository(),
    activityRepository: ActivityRepository = ActivityRepository()
) {
    val savedStateHandle = SavedStateHandle(mapOf("friendId" to friendId))
    val factory = FriendsDetailViewModelFactory(
        userRepository,
        expenseRepository,
        groupsRepository,
        activityRepository,
        savedStateHandle
    )
    val viewModel: FriendsDetailViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()
    val friend = uiState.friend
    val isLoading = uiState.isLoadingFriend || uiState.isLoadingExpenses

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.friend?.username ?: "Friend Details",
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        navController.navigate("${Screen.FriendSettings}/$friendId")
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Friend Settings", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2D2D2D))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate(Screen.AddExpense)
                },
                containerColor = PositiveGreen
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Color.Black)
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            when {
                isLoading && friend == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                friend != null -> {
                    FriendDetailContent(
                        friendId = friendId,
                        friend = friend,
                        netBalance = uiState.netBalance,
                        balanceBreakdown = uiState.balanceBreakdown,
                        expenses = uiState.expenses,
                        viewModel = viewModel,
                        navController = navController
                    )
                }
                else -> {
                    Text(
                        text = uiState.error ?: "Friend not found.",
                        color = NegativeRed,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
fun FriendDetailContent(
    friendId: String,
    friend: com.example.splitpay.data.model.User,
    netBalance: Double,
    balanceBreakdown: List<BalanceDetail>,
    expenses: List<com.example.splitpay.data.model.Expense>,
    viewModel: FriendsDetailViewModel,
    navController: NavHostController
) {
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        // Header
        item {
            FriendDetailHeader(
                friend = friend,
                netBalance = netBalance,
                balanceBreakdown = balanceBreakdown
            )
            Spacer(Modifier.height(16.dp))
        }

        // Action Buttons
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ActionButtonsRow(navController = navController, friendId = friendId)
                Spacer(Modifier.height(24.dp))
            }
        }

        // Activities Title
        item {
            Text(
                text = "Activities",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        // Shared Group Activities
        val sharedGroupActivities = viewModel.uiState.value.sharedGroupActivities
        items(sharedGroupActivities, key = { "activity_${it.id}" }) { activity ->
            SharedGroupActivityCard(
                activity = activity,
                modifier = Modifier.padding(horizontal = 16.dp),
                onClick = {
                    // Navigate to ExpenseDetail for expense-related activities
                    val activityType = try {
                        ActivityType.valueOf(activity.activityType)
                    } catch (e: Exception) {
                        null
                    }

                    when (activityType) {
                        ActivityType.EXPENSE_ADDED -> {
                            // Navigate to Expense Detail using entityId
                            if (activity.entityId != null) {
                                navController.navigate("${Screen.ExpenseDetail}/${activity.entityId}")
                            }
                        }
                        ActivityType.EXPENSE_UPDATED,
                        ActivityType.EXPENSE_DELETED -> {
                            // Edit and delete activities do nothing when clicked
                        }
                        else -> {
                            // For other activity types, navigate to generic activity detail
                            navController.navigate("${Screen.ActivityDetail}?activityId=${activity.id}")
                        }
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
        }

        // Activity List (Expenses)
        when {
            viewModel.uiState.value.isLoadingExpenses -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        // This is the loading spinner
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
            }
            expenses.isEmpty() && viewModel.uiState.value.sharedGroupActivities.isEmpty() -> {
                item {
                    Text(
                        "No activities yet with this friend.",
                        color = Color.Gray,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        textAlign = TextAlign.Center
                    )
                }
            }
            else -> {
                items(expenses, key = { "expense_${it.id}" }) { expense ->
                    val (lentBorrowedText, lentBorrowedColor) = viewModel.calculateUserLentBorrowed(expense)
                    val payerSummary = viewModel.formatPayerSummary(expense)

                    ExpenseActivityCard(
                        expense = expense,
                        payerSummary = payerSummary,
                        userLentBorrowed = lentBorrowedText,
                        userLentBorrowedColor = lentBorrowedColor,
                        modifier = Modifier.padding(horizontal = 16.dp),
                        onClick = {
                            navController.navigate("${Screen.ExpenseDetail}/${expense.id}")
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                }
            }
        }

        // Spacer at the bottom
        item {
            Spacer(Modifier.height(72.dp))
        }
    }
}

@Composable
fun FriendDetailHeader(
    friend: com.example.splitpay.data.model.User,
    netBalance: Double,
    balanceBreakdown: List<BalanceDetail>
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2D2D))
            .padding(bottom = 16.dp, top = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Friend Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.AccountCircle,
                contentDescription = "Friend Icon",
                tint = TextWhite,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        // Friend Name
        Text(
            text = friend.username,
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(16.dp))

        // Overall Balance
        val overallBalanceText = when {
            netBalance > 0.01 -> "Overall, you are owed MYR%.2f".format(netBalance)
            netBalance < -0.01 -> "Overall, you owe MYR%.2f".format(netBalance.absoluteValue)
            else -> "You are settled up with this friend"
        }
        val overallBalanceColor = when {
            netBalance > 0.01 -> PositiveGreen
            netBalance < -0.01 -> NegativeRed
            else -> Color.Gray
        }

        Text(
            text = overallBalanceText,
            color = overallBalanceColor,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )

        // Balance Breakdown
        if (balanceBreakdown.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            balanceBreakdown.forEach { detail ->
                val breakdownText = when {
                    detail.amount > 0.01 -> "${friend.username} owes you MYR%.2f in ${detail.groupName}".format(detail.amount)
                    detail.amount < -0.01 -> "You owe ${friend.username} MYR%.2f in ${detail.groupName}".format(detail.amount.absoluteValue)
                    else -> null
                }
                val breakdownColor = if (detail.amount > 0) PositiveGreen else NegativeRed

                if (breakdownText != null) {
                    Text(
                        text = breakdownText,
                        color = breakdownColor,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}

@Composable
fun ActionButtonsRow(
    navController: NavHostController,
    friendId: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ActionButton("Settle Up") {
            navController.navigate("${Screen.SettleUpFriend}/$friendId")
        }
        ActionButton("Charts") {}
        ActionButton("Balances") {}
        ActionButton("Total") {}
        ActionButton("Export") {}
    }
}

@Composable
fun ActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C3C3C)),
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 14.sp, color = TextWhite)
    }
}

@Composable
fun SharedGroupActivityCard(
    activity: com.example.splitpay.data.model.Activity,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2A2A2A))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = "Group",
                tint = PrimaryBlue,
                modifier = Modifier.size(28.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Content
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = activity.groupName ?: "Unknown Group",
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "shared group",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
    }
}