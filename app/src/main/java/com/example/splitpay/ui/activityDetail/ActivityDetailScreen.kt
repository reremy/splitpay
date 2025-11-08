package com.example.splitpay.ui.activityDetail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitpay.data.model.Activity
import com.example.splitpay.data.model.ActivityType
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.NegativeRed
import com.example.splitpay.ui.theme.PositiveGreen
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.absoluteValue

// --- ViewModel Factory ---
class ActivityDetailViewModelFactory(
    private val activityRepository: ActivityRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ActivityDetailViewModel::class.java)) {
            return ActivityDetailViewModel(activityRepository, expenseRepository, userRepository, savedStateHandle) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityDetailScreen(
    activityId: String? = null,
    expenseId: String? = null,
    navController: NavHostController,
    onNavigateBack: () -> Unit,
    activityRepository: ActivityRepository = ActivityRepository(),
    expenseRepository: ExpenseRepository = ExpenseRepository(),
    userRepository: UserRepository = UserRepository()
) {
    val savedStateHandle = SavedStateHandle(mapOf(
        "activityId" to activityId,
        "expenseId" to expenseId
    ))
    val factory = ActivityDetailViewModelFactory(activityRepository, expenseRepository, userRepository, savedStateHandle)
    val viewModel: ActivityDetailViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()

    // Handle UI Events
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ActivityDetailUiEvent.NavigateBack -> onNavigateBack()
                is ActivityDetailUiEvent.NavigateToEditExpense -> {
                    navController.navigate("add_expense?groupId=${event.groupId ?: ""}&expenseId=${event.expenseId}")
                }
                is ActivityDetailUiEvent.ShowError -> {
                    // TODO: Show snackbar or error dialog
                }
                is ActivityDetailUiEvent.DeleteSuccess -> onNavigateBack()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Details", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackClick() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    // Camera icon (placeholder for now)
                    IconButton(onClick = { /* TODO: Add camera functionality */ }) {
                        Icon(
                            imageVector = Icons.Default.CameraAlt,
                            contentDescription = "Camera",
                            tint = TextWhite
                        )
                    }

                    // Delete icon (only show for expense activities)
                    if (uiState.activity?.activityType == ActivityType.EXPENSE_ADDED.name ||
                        uiState.activity?.activityType == ActivityType.EXPENSE_UPDATED.name
                    ) {
                        IconButton(onClick = { viewModel.onDeleteClick() }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete",
                                tint = TextWhite
                            )
                        }
                    }

                    // Edit icon (only show for expense activities)
                    if (uiState.activity?.activityType == ActivityType.EXPENSE_ADDED.name ||
                        uiState.activity?.activityType == ActivityType.EXPENSE_UPDATED.name
                    ) {
                        IconButton(onClick = { viewModel.onEditClick() }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit",
                                tint = TextWhite
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PrimaryBlue
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(DarkBackground)
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = PrimaryBlue
                    )
                }
                uiState.error != null -> {
                    Text(
                        text = uiState.error ?: "Unknown error",
                        color = NegativeRed,
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(16.dp),
                        textAlign = TextAlign.Center
                    )
                }
                uiState.activity != null -> {
                    ActivityDetailContent(
                        activity = uiState.activity!!,
                        expense = uiState.expense,
                        actorUser = uiState.actorUser,
                        payers = uiState.payers,
                        participants = uiState.participants,
                        currentUserId = uiState.currentUserId
                    )
                }
            }
        }
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.onDismissDeleteDialog() },
            title = { Text("Delete expense?") },
            text = {
                Text("Are you sure you want to delete this expense? This will remove this expense for ALL people involved, not just you.")
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onConfirmDelete() },
                    colors = ButtonDefaults.buttonColors(containerColor = NegativeRed)
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.onDismissDeleteDialog() }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ActivityDetailContent(
    activity: Activity,
    expense: Expense?,
    actorUser: User?,
    payers: Map<String, User>,
    participants: Map<String, User>,
    currentUserId: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        // Activity Metadata Section
        ActivityMetadataSection(activity = activity, actorUser = actorUser)

        Spacer(modifier = Modifier.height(16.dp))

        // Content based on activity type
        when (activity.activityType) {
            ActivityType.EXPENSE_ADDED.name, ActivityType.EXPENSE_UPDATED.name -> {
                if (expense != null) {
                    ExpenseDetailSection(
                        expense = expense,
                        payers = payers,
                        participants = participants,
                        currentUserId = currentUserId
                    )
                }
            }
            ActivityType.PAYMENT_MADE.name -> {
                PaymentDetailSection(activity = activity)
            }
            ActivityType.MEMBER_ADDED.name, ActivityType.MEMBER_REMOVED.name, ActivityType.MEMBER_LEFT.name -> {
                MemberActivityDetailSection(activity = activity)
            }
            ActivityType.GROUP_CREATED.name, ActivityType.GROUP_DELETED.name -> {
                GroupActivityDetailSection(activity = activity)
            }
            else -> {
                GenericActivityDetailSection(activity = activity)
            }
        }
    }
}

@Composable
fun ActivityMetadataSection(activity: Activity, actorUser: User?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = getActivityIcon(activity.activityType),
                    contentDescription = null,
                    tint = PrimaryBlue,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = getActivityTypeLabel(activity.activityType),
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "by ${actorUser?.displayName ?: activity.actorName}",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Timestamp
            Text(
                text = formatTimestamp(activity.timestamp),
                color = Color.Gray,
                fontSize = 12.sp
            )

            // Group name if applicable
            if (activity.groupName != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Group: ${activity.groupName}",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
fun ExpenseDetailSection(
    expense: Expense,
    payers: Map<String, User>,
    participants: Map<String, User>,
    currentUserId: String
) {
    // Description and Amount
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = expense.description,
                color = TextWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "MYR %.2f".format(expense.totalAmount),
                color = PrimaryBlue,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Who Paid Section
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Who Paid",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            expense.paidBy.forEach { payer ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = payers[payer.uid]?.displayName ?: "Unknown",
                        color = Color.LightGray
                    )
                    Text(
                        text = "MYR %.2f".format(payer.paidAmount),
                        color = PositiveGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Split Details Section
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Split Details (${expense.splitType})",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            expense.participants.forEach { participant ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = participants[participant.uid]?.displayName ?: "Unknown",
                            color = if (participant.uid == currentUserId) PrimaryBlue else Color.LightGray,
                            fontWeight = if (participant.uid == currentUserId) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                    Text(
                        text = "MYR %.2f".format(participant.owesAmount),
                        color = NegativeRed,
                        fontWeight = FontWeight.Bold
                    )
                }
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 4.dp),
                    color = Color.DarkGray
                )
            }
        }
    }

    // Memo if exists
    if (expense.memo.isNotBlank()) {
        Spacer(modifier = Modifier.height(16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Notes",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = expense.memo,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun PaymentDetailSection(activity: Activity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Payment Made",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (activity.totalAmount != null) {
                Text(
                    text = "Amount: MYR %.2f".format(activity.totalAmount),
                    color = PositiveGreen,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            if (activity.displayText != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = activity.displayText,
                    color = Color.LightGray
                )
            }
        }
    }
}

@Composable
fun MemberActivityDetailSection(activity: Activity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = getActivityTypeLabel(activity.activityType),
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (activity.displayText != null) {
                Text(
                    text = "Member: ${activity.displayText}",
                    color = Color.LightGray,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun GroupActivityDetailSection(activity: Activity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = getActivityTypeLabel(activity.activityType),
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (activity.groupName != null) {
                Text(
                    text = "Group: ${activity.groupName}",
                    color = Color.LightGray,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun GenericActivityDetailSection(activity: Activity) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2A2A2A)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Activity Details",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            if (activity.displayText != null) {
                Text(
                    text = activity.displayText,
                    color = Color.LightGray
                )
            }
        }
    }
}

// Helper Functions
fun getActivityIcon(activityType: String): ImageVector {
    return when (activityType) {
        ActivityType.EXPENSE_ADDED.name, ActivityType.EXPENSE_UPDATED.name -> Icons.Default.Receipt
        ActivityType.EXPENSE_DELETED.name -> Icons.Default.Delete
        ActivityType.PAYMENT_MADE.name -> Icons.Default.Payments
        ActivityType.MEMBER_ADDED.name -> Icons.Default.PersonAdd
        ActivityType.MEMBER_REMOVED.name, ActivityType.MEMBER_LEFT.name -> Icons.Default.PersonRemove
        ActivityType.GROUP_CREATED.name -> Icons.Default.GroupAdd
        ActivityType.GROUP_DELETED.name -> Icons.Default.Delete
        else -> Icons.Default.Info
    }
}

fun getActivityTypeLabel(activityType: String): String {
    return when (activityType) {
        ActivityType.EXPENSE_ADDED.name -> "Expense Added"
        ActivityType.EXPENSE_UPDATED.name -> "Expense Updated"
        ActivityType.EXPENSE_DELETED.name -> "Expense Deleted"
        ActivityType.PAYMENT_MADE.name -> "Payment Made"
        ActivityType.MEMBER_ADDED.name -> "Member Added"
        ActivityType.MEMBER_REMOVED.name -> "Member Removed"
        ActivityType.MEMBER_LEFT.name -> "Member Left"
        ActivityType.GROUP_CREATED.name -> "Group Created"
        ActivityType.GROUP_DELETED.name -> "Group Deleted"
        else -> "Activity"
    }
}

fun formatTimestamp(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    val dayInMillis = 24 * 60 * 60 * 1000

    return when {
        diff < dayInMillis -> "Today at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))}"
        diff < 2 * dayInMillis -> "Yesterday at ${SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))}"
        else -> SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(timestamp))
    }
}
