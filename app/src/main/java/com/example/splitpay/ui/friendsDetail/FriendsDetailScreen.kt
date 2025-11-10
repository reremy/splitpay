package com.example.splitpay.ui.friendsDetail

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import coil.compose.AsyncImage
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.splitpay.ui.groups.expenseCategoriesMap
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitpay.data.model.ActivityType
import com.example.splitpay.data.model.ExpenseType
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.groups.ExpenseActivityCard
import com.example.splitpay.ui.groups.availableTagsMap
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
                ActionButtonsRow(
                    navController = navController,
                    friendId = friendId,
                    viewModel = viewModel
                )
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

        // Activity List (Shared Groups + Payments)
        when {
            viewModel.uiState.value.isLoadingExpenses -> {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
            }
            viewModel.uiState.value.activityCards.isEmpty() -> {
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
                items(
                    viewModel.uiState.value.activityCards,
                    key = { card ->
                        when (card) {
                            is com.example.splitpay.ui.friendsDetail.ActivityCard.SharedGroupCard -> "group_${card.groupId}_${card.mostRecentDate}"
                            is com.example.splitpay.ui.friendsDetail.ActivityCard.PaymentCard -> "payment_${card.expense.id}"
                        }
                    }
                ) { card ->
                    when (card) {
                        is com.example.splitpay.ui.friendsDetail.ActivityCard.SharedGroupCard -> {
                            SharedGroupActivityCard(
                                card = card,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onClick = {
                                    // Navigate to group detail page
                                    if (card.groupId != null) {
                                        navController.navigate("group_detail/${card.groupId}")
                                    }
                                }
                            )
                        }
                        is com.example.splitpay.ui.friendsDetail.ActivityCard.PaymentCard -> {
                            val (lentBorrowedText, lentBorrowedColor) = viewModel.calculateUserLentBorrowed(card.expense)
                            val payerSummary = viewModel.formatPayerSummary(card.expense)

                            ExpenseActivityCard(
                                expense = card.expense,
                                payerSummary = payerSummary,
                                userLentBorrowed = lentBorrowedText,
                                userLentBorrowedColor = lentBorrowedColor,
                                modifier = Modifier.padding(horizontal = 16.dp),
                                onClick = {
                                    // Navigate to appropriate detail screen based on expense type
                                    if (card.expense.expenseType == ExpenseType.PAYMENT) {
                                        navController.navigate("${Screen.PaymentDetail}/${card.expense.id}")
                                    } else {
                                        // Non-group expense or regular expense
                                        navController.navigate("${Screen.ExpenseDetail}/${card.expense.id}")
                                    }
                                }
                            )
                        }
                    }
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
        // Friend Profile Picture
        if (friend.profilePictureUrl.isNotEmpty()) {
            AsyncImage(
                model = friend.profilePictureUrl,
                contentDescription = "${friend.username}'s profile",
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
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
    friendId: String,
    viewModel: FriendsDetailViewModel
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    // State for charts bottom sheet
    val showChartsSheet = remember { mutableStateOf(false) }
    val chartsSheetState = rememberModalBottomSheetState()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ActionButton("Settle Up") {
            navController.navigate("${Screen.SelectBalanceToSettle}/$friendId")
        }
        ActionButton("Charts") {
            showChartsSheet.value = true
        }
        ActionButton("Reminder") {
            viewModel.showReminderDialog()
        }
        ActionButton("Export") {
            val exportData = viewModel.exportFriendData()
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, exportData)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Export Friend Data")
            context.startActivity(shareIntent)
        }
    }

    // Charts Bottom Sheet
    if (showChartsSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showChartsSheet.value = false },
            sheetState = chartsSheetState,
            containerColor = Color(0xFF2D2D2D)
        ) {
            ChartsSheetContent(
                chartData = uiState.chartData,
                friendName = uiState.friend?.username ?: "Friend"
            )
        }
    }

    // Reminder Dialog
    if (uiState.showReminderDialog) {
        ReminderDialog(
            friendName = uiState.friend?.username ?: "Friend",
            onDismiss = { viewModel.dismissReminderDialog() },
            onSend = { message ->
                // For now, just show a toast or dismiss
                // In a real app, this would send a notification or message
                viewModel.dismissReminderDialog()
            }
        )
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
    card: com.example.splitpay.ui.friendsDetail.ActivityCard.SharedGroupCard,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // Format date
    val dateObject = java.util.Date(card.mostRecentDate)
    val dayFormatter = java.text.SimpleDateFormat("dd", java.util.Locale.getDefault())
    val monthFormatter = java.text.SimpleDateFormat("MMM", java.util.Locale.getDefault())
    val day = dayFormatter.format(dateObject)
    val month = monthFormatter.format(dateObject).uppercase()

    // Format balance text
    val balanceText = when {
        card.balance > 0.01 -> "you lent MYR%.2f".format(card.balance)
        card.balance < -0.01 -> "you owe MYR%.2f".format(card.balance.absoluteValue)
        else -> "settled"
    }
    val balanceColor = when {
        card.balance > 0.01 -> PositiveGreen
        card.balance < -0.01 -> NegativeRed
        else -> Color.Gray
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Date Column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(40.dp)
        ) {
            Text(month, color = Color.Gray, fontSize = 12.sp)
            Text(day, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }

        Spacer(Modifier.width(12.dp))

        // Group Icon based on iconIdentifier
        val groupIcon = card.group?.iconIdentifier?.let { availableTagsMap[it] } ?: Icons.Default.Group
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(PrimaryBlue.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = groupIcon,
                contentDescription = "Group",
                tint = PrimaryBlue,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(Modifier.width(12.dp))

        // Content Column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.groupName,
                color = TextWhite,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "shared group",
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        Spacer(Modifier.width(12.dp))

        // Balance Column
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = balanceText,
                color = balanceColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChartsSheetContent(
    chartData: ChartData,
    friendName: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Charts for $friendName",
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (chartData.categoryBreakdown.isEmpty() && chartData.dailySpending.isEmpty() && chartData.groupBreakdown.isEmpty()) {
            Text(
                text = "No expense data available for charts.",
                color = Color.Gray,
                fontSize = 14.sp,
                modifier = Modifier.padding(vertical = 32.dp),
                textAlign = TextAlign.Center
            )
        } else {
            // 1. Category Breakdown
            if (chartData.categoryBreakdown.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3C3C3C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Spending by Category",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        chartData.categoryBreakdown.forEach { category ->
                            HorizontalBarChart(
                                label = category.category.replaceFirstChar { it.uppercase() },
                                value = category.amount,
                                percentage = category.percentage,
                                color = PrimaryBlue
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 2. Group Breakdown
            if (chartData.groupBreakdown.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3C3C3C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Spending by Group",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        chartData.groupBreakdown.forEach { group ->
                            HorizontalBarChart(
                                label = group.groupName,
                                value = group.amount,
                                percentage = group.percentage,
                                color = PositiveGreen
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // 3. Daily Spending
            if (chartData.dailySpending.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF3C3C3C)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Daily Spending",
                            color = TextWhite,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        DailySpendingChart(
                            dailySpending = chartData.dailySpending,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}

@Composable
fun HorizontalBarChart(
    label: String,
    value: Double,
    percentage: Float,
    color: Color
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextWhite,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "MYR %.2f (%.1f%%)".format(value, percentage),
                color = Color.Gray,
                fontSize = 13.sp
            )
        }
        Spacer(Modifier.height(4.dp))
        // Progress bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color(0xFF4D4D4D))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(percentage / 100f)
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(color)
            )
        }
    }
}

@Composable
fun DailySpendingChart(
    dailySpending: List<DailySpending>,
    modifier: Modifier = Modifier
) {
    val maxAmount = dailySpending.maxOfOrNull { it.amount } ?: 1.0

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dailySpending.takeLast(7).forEach { day ->
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val heightFraction = (day.amount / maxAmount).toFloat().coerceIn(0.1f, 1f)
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height((150 * heightFraction).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(PrimaryBlue)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = SimpleDateFormat("dd", Locale.getDefault()).format(Date(day.date)),
                        color = Color.Gray,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Last ${dailySpending.takeLast(7).size} days",
            color = Color.Gray,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderDialog(
    friendName: String,
    onDismiss: () -> Unit,
    onSend: (String) -> Unit
) {
    val reminderMessage = remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Reminder to $friendName", color = TextWhite) },
        text = {
            Column {
                Text(
                    "Send a friendly reminder to $friendName about outstanding balances.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = reminderMessage.value,
                    onValueChange = { reminderMessage.value = it },
                    label = { Text("Message (optional)", color = Color.Gray) },
                    placeholder = { Text("Add a personal message...", color = Color.Gray) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        focusedBorderColor = PrimaryBlue,
                        unfocusedBorderColor = Color.Gray
                    ),
                    maxLines = 3
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSend(reminderMessage.value) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Send", color = TextWhite)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF2D2D2D)
    )
}