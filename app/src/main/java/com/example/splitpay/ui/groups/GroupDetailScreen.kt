package com.example.splitpay.ui.groups

import android.R.attr.onClick
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable // Import clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn // NEW IMPORT
import androidx.compose.foundation.lazy.items // NEW IMPORT
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add // Keep this import
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.GroupAdd
import androidx.compose.material.icons.filled.GroupRemove
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart // NEW IMPORT (Placeholder)
import androidx.compose.material.icons.filled.SyncAlt // NEW IMPORT (Placeholder)
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitpay.data.model.Expense // <-- Add import
import com.example.splitpay.data.model.ExpenseType
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ExpenseRepository // <-- Add import
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository // <-- Add import
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.groups.expenseCategoriesMap // Import expense categories map
import com.example.splitpay.ui.theme.CardBackground
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.DialogBackground
import com.example.splitpay.ui.theme.NegativeRed
import com.example.splitpay.ui.theme.PositiveGreen
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.SurfaceDark
import com.example.splitpay.ui.theme.TextWhite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.SavedStateHandle // <-- Add import
import coil.compose.AsyncImage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.absoluteValue



// --- ViewModel Factory ---
class GroupDetailViewModelFactory(
    private val groupsRepository: GroupsRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            // Pass repositories to ViewModel constructor
            return GroupDetailViewModel(groupsRepository, expenseRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    navController: NavHostController,
    onNavigateBack: () -> Unit,
    groupsRepository: GroupsRepository = GroupsRepository(),
    expenseRepository: ExpenseRepository = ExpenseRepository(),
    userRepository: UserRepository = UserRepository()
) {
    // Use Factory to create ViewModel
    val factory = GroupDetailViewModelFactory(groupsRepository, expenseRepository, userRepository)
    val viewModel: GroupDetailViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()
    val group = uiState.group

    val isLoading = uiState.isLoadingGroup || uiState.isLoadingExpenses

    // --- State for Info Dialog ---
    val showInfoDialog = remember { mutableStateOf(false) }

    // --- State for Totals Bottom Sheet ---
    val showTotalsSheet = remember { mutableStateOf(false) }
    val totalsSheetState = rememberModalBottomSheetState()

    // --- State for Balances Bottom Sheet ---
    val showBalancesSheet = remember { mutableStateOf(false) }
    val balancesSheetState = rememberModalBottomSheetState()

    // --- State for Charts Bottom Sheet ---
    val showChartsSheet = remember { mutableStateOf(false) }
    val chartsSheetState = rememberModalBottomSheetState()

    // Call the loading function when groupId changes
    LaunchedEffect(groupId) {
        viewModel.loadGroupAndExpenses(groupId)
    }

    // --- Info Dialog Composable ---
    if (showInfoDialog.value) {
        AlertDialog(
            onDismissRequest = { showInfoDialog.value = false },
            title = { Text("Non-Group Expenses Info", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text(
                "This section shows expenses shared directly with individuals, not within a group setting. These expenses cannot be modified or deleted from this view.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )},
            confirmButton = {
                Button(
                    onClick = { showInfoDialog.value = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) { Text("OK") }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // --- Totals Bottom Sheet ---
    if (showTotalsSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showTotalsSheet.value = false },
            sheetState = totalsSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            TotalsSheetContent(
                totals = uiState.totals,
                membersMap = uiState.membersMap,
                groupName = group?.name ?: "Group"
            )
        }
    }

    // --- Balances Bottom Sheet ---
    if (showBalancesSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showBalancesSheet.value = false },
            sheetState = balancesSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            BalancesSheetContent(
                balanceBreakdown = uiState.balanceBreakdown,
                membersMap = uiState.membersMap,
                groupName = group?.name ?: "Group"
            )
        }
    }

    // --- Charts Bottom Sheet ---
    if (showChartsSheet.value) {
        ModalBottomSheet(
            onDismissRequest = { showChartsSheet.value = false },
            sheetState = chartsSheetState,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            ChartsSheetContent(
                chartData = uiState.chartData,
                groupName = group?.name ?: "Group"
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        // Use group name from state, handles non-group placeholder name
                        text = uiState.group?.name ?: "Details",
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

                    if (groupId == "non_group") {
                        // Show Info icon for non-group
                        IconButton(onClick = { showInfoDialog.value = true }) { // Show dialog on click
                            Icon(Icons.Default.Info, contentDescription = "Non-Group Info", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    } else {
                        // Show Settings icon for regular groups
                        IconButton(onClick = {
                            // Navigate to the group settings screen
                            navController.navigate("group_settings/$groupId")
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Group Settings", tint = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        floatingActionButton = {
            // Keep FAB enabled, allowing adding non-group expenses from here or group expenses
            FloatingActionButton(
                onClick = {
                    if (groupId == "non_group") {
                        // Navigate to add expense, passing null for groupId to indicate non-group
                        navController.navigate(Screen.AddExpense)
                    } else {
                        // Navigate to add expense, prefilling the current group ID
                        navController.navigate("${Screen.AddExpense}?groupId=$groupId")
                    }
                },
                containerColor = PositiveGreen // Use theme color
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Expense", tint = Color.Black) // Changed tint for visibility
            }
        }
    ) { innerPadding ->
        // --- Main Content Box ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            when {
                // Show loading indicator if either is loading initially
                isLoading && group == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                group != null -> {
                    // Pass group, expenses, and now groupId down
                    GroupDetailContent(
                        groupId = groupId, // Pass the ID
                        group = group,
                        expenses = uiState.expenses,
                        viewModel = viewModel,
                        navController = navController, // <-- PASS NAVCONTROLLER
                        showTotalsSheet = showTotalsSheet,
                        showBalancesSheet = showBalancesSheet,
                        showChartsSheet = showChartsSheet
                    )
                }
                else -> { // Error state or group is null after loading
                    Text(
                        text = uiState.error ?: "Group not found.",
                        color = NegativeRed,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        } // End Main Content Box
    } // End Scaffold
}

// --- MODIFIED: Accepts iconIdentifier string ---
@Composable
fun GroupDetailHeaderDisplay(
    group: Group,
    iconIdentifier: String?,
    overallBalance: Double,
    balanceBreakdown: List<MemberBalanceDetail>,
    membersMap: Map<String, User> // Add membersMap parameter
) { // Added parameter
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface) // Match TopAppBar color
            .padding(bottom = 16.dp, top = 16.dp), // Add padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Group Photo or Tag Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(if (group.photoUrl.isNotEmpty()) Color.Transparent else PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            if (group.photoUrl.isNotEmpty()) {
                // Display group photo
                AsyncImage(
                    model = group.photoUrl,
                    contentDescription = "Group Photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Display tag icon
                val icon = availableTagsMap[iconIdentifier] ?: Icons.Default.Group
                Icon(icon, contentDescription = "Group Tag", tint = TextWhite, modifier = Modifier.size(40.dp))
            }
        }

        Spacer(Modifier.height(12.dp))

        // Group Name
        Text(
            text = group.name, // Display name from (potentially mock) group object
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        // Stacked Member Photos
        if (group.id != "non_group") {
            StackedMemberPhotos(
                memberIds = group.members,
                membersMap = membersMap
            )
            Spacer(Modifier.height(16.dp))
        } else {
            Spacer(Modifier.height(16.dp))
        }

        // --- Overall Balance ---
        val overallBalanceText = when {
            overallBalance > 0.01 -> "Overall, you are owed ${formatCurrency(overallBalance)}"
            overallBalance < -0.01 -> "Overall, you owe ${formatCurrency(overallBalance.absoluteValue)}"
            else -> "You are settled up in this group"
        }
        val overallBalanceColor = when {
            overallBalance > 0.01 -> PositiveGreen
            overallBalance < -0.01 -> NegativeRed
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }

        Text(
            text = overallBalanceText,
            color = overallBalanceColor,
            fontSize = 20.sp, // Slightly larger font for overall balance
            fontWeight = FontWeight.Bold // Make it bold
        )

        // --- Balance Breakdown ---
        // Only show breakdown if there are details and it's not the non-group placeholder
        if (balanceBreakdown.isNotEmpty() && group.id != "non_group") {
            Spacer(Modifier.height(8.dp))
            balanceBreakdown.forEach { detail ->
                val breakdownText = when {
                    // Positive amount means they owe you
                    detail.amount > 0.01 -> "${detail.memberName} owes you ${formatCurrency(detail.amount)}"
                    // Negative amount means you owe them
                    detail.amount < -0.01 -> "You owe ${detail.memberName} ${formatCurrency(detail.amount.absoluteValue)}"
                    else -> null // Should not happen due to filtering in VM, but good practice
                }
                val breakdownColor = if (detail.amount > 0) PositiveGreen else NegativeRed

                if (breakdownText != null) {
                    Text(
                        text = breakdownText,
                        color = breakdownColor,
                        fontSize = 14.sp, // Smaller font for breakdown
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        }
    }
}


// --- MODIFIED: Accepts groupId ---
@Composable
fun GroupDetailContent(
    groupId: String, // <-- Added parameter
    group: Group,
    expenses: List<Expense>,
    viewModel: GroupDetailViewModel,
    navController: NavHostController, // <-- Added NavController
    showTotalsSheet: androidx.compose.runtime.MutableState<Boolean>,
    showBalancesSheet: androidx.compose.runtime.MutableState<Boolean>,
    showChartsSheet: androidx.compose.runtime.MutableState<Boolean>
) {

    val uiState by viewModel.uiState.collectAsState()
    // Determine if this is the special non-group view
    val isNonGroup = groupId == "non_group"

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // --- 1. Custom Header ---
        item {
            // Pass the correct icon identifier from the group object
            GroupDetailHeaderDisplay(
                group = group,
                iconIdentifier = group.iconIdentifier,
                overallBalance = uiState.currentUserOverallBalance, // <-- Pass overall balance
                balanceBreakdown = uiState.balanceBreakdown,      // <-- Pass breakdown list
                membersMap = uiState.membersMap                    // <-- Pass membersMap
            )
            Spacer(Modifier.height(16.dp))
        }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                // --- PASS NAVCONTROLLER AND GROUPID ---
                ActionButtonsRow(
                    navController = navController,
                    groupId = groupId,
                    showTotalsSheet = showTotalsSheet,
                    showBalancesSheet = showBalancesSheet,
                    showChartsSheet = showChartsSheet,
                    viewModel = viewModel
                )
                Spacer(Modifier.height(16.dp))
            }
        }

        // --- 2. Single Member Message (if applicable) ---
        if (groupId != "non_group" && group.members.size == 1) {
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "There is only one member in this group",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = {
                                // Navigate to add group members screen
                                navController.navigate("add_group_members/$groupId")
                            },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add Members", modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Add Members", color = TextWhite, fontSize = 14.sp)
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }

        // --- 3. Activities Title ---
        item {
            Text(
                // Adjust title based on whether it's non-group or regular group
                text = if (isNonGroup) "Non-Group Activities" else "Activities",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        // --- 4. Activity List ---
        if (expenses.isEmpty() && !viewModel.uiState.value.isLoadingExpenses) { // Check loading state too
            item {
                Text(
                    "No expenses recorded yet.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            items(expenses, key = { it.id }) { expense ->
                // Calculate lent/borrowed status using ViewModel helper
                val (lentBorrowedText, lentBorrowedColor) = viewModel.calculateUserLentBorrowed(expense)
                // Format payer summary using ViewModel helper
                val payerSummary = viewModel.formatPayerSummary(expense)

                //val userNon_GroupParticipation = expense.participants.contains(currentUserId)

                ExpenseActivityCard(
                    expense = expense,
                    payerSummary = payerSummary,
                    userLentBorrowed = lentBorrowedText,
                    userLentBorrowedColor = lentBorrowedColor,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    onClick = {
                        // Navigate to the correct detail page based on expense type
                        if (expense.expenseType == ExpenseType.PAYMENT) {
                            navController.navigate("${Screen.PaymentDetail}/${expense.id}")
                        } else {
                            navController.navigate("${Screen.ExpenseDetail}/${expense.id}")
                        }
                    }
                )
                Spacer(Modifier.height(8.dp))
            }
        }

        // --- 5. Spacer at the bottom ---
        item {
            Spacer(Modifier.height(72.dp)) // Spacer for FAB
        }
    }
}

// --- NEW: Composable for displaying a real Expense ---
@Composable
fun ExpenseActivityCard(
    expense: Expense,
    payerSummary: String,
    userLentBorrowed: String,
    userLentBorrowedColor: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    // Convert timestamp (Long) to Date object
    val dateObject = Date(expense.date)
    // Format day ("dd")
    val dayFormatter = SimpleDateFormat("dd", Locale.getDefault())
    val day = dayFormatter.format(dateObject)
    // Format month abbreviation ("MMM")
    val monthFormatter = SimpleDateFormat("MMM", Locale.getDefault())
    val month = monthFormatter.format(dateObject).uppercase() // e.g., "OCT"

    // Determine icon based on expense category
    val icon = expenseCategoriesMap[expense.category] ?: Icons.Default.Category

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
            Text(month, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(day, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
        }
        Spacer(Modifier.width(12.dp))
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(SurfaceDark), // Icon background
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = expense.description,
                tint = TextWhite,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(Modifier.width(12.dp))

        // Description and Payer
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center
        ) {
            Text(expense.description, color = TextWhite, fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(payerSummary, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp) // Use formatted summary
        }

        Spacer(Modifier.width(8.dp))

        // User's Share (Lent/Borrowed)
        Column(horizontalAlignment = Alignment.End) {
            // Display calculated lent/borrowed text and use its color
            Text(
                userLentBorrowed,
                color = userLentBorrowedColor,
                fontSize = 14.sp, // Made slightly larger
                fontWeight = FontWeight.Medium
            )
            // Optionally show the user's share amount again if needed (might be redundant)
            /*
            val userOwed = expense.participants.find { it.uid == currentUserId }?.owesAmount ?: 0.0
            if (userOwed > 0.01) {
                Text(
                    "Your share: MYR%.2f".format(userOwed),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
            */
        }
    }
}


@Composable
fun ActionButtonsRow(
    navController: NavHostController, // <-- Add NavController
    groupId: String, // <-- Add groupId
    showTotalsSheet: androidx.compose.runtime.MutableState<Boolean>,
    showBalancesSheet: androidx.compose.runtime.MutableState<Boolean>,
    showChartsSheet: androidx.compose.runtime.MutableState<Boolean>,
    viewModel: GroupDetailViewModel
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        // --- MODIFIED ONCLICK ---
        ActionButton(
            "Settle Up",
            { navController.navigate("${Screen.SettleUp}/$groupId") }
        )
        if(groupId != "non_group"){
            ActionButton(
                "Scan",
                {
                    navController.navigate("${Screen.ReceiptScanner}/$groupId")
                }
            )
        }

        ActionButton("Charts", { showChartsSheet.value = true })
        ActionButton("Balances", { showBalancesSheet.value = true })
        ActionButton("Total", { showTotalsSheet.value = true })
        ActionButton("Export", {
            val exportData = viewModel.exportGroupData()
            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, exportData)
                type = "text/plain"
            }
            val shareIntent = Intent.createChooser(sendIntent, "Export Group Data")
            context.startActivity(shareIntent)
        })
    }
}


@Composable
fun ActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = SurfaceDark),
        // Adjust padding if needed for 5 buttons
        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(label, fontSize = 14.sp, color = TextWhite)
    }
}

@Composable
fun GroupMemberStatus(
    memberCount: Int,
    groupName: String,
    onAddMemberClick: () -> Unit,
    onShareLinkClick: () -> Unit
) {
    val showAddMemberButtonInStatus = memberCount <= 1

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(10.dp) // Match example
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (showAddMemberButtonInStatus) SurfaceDark else SurfaceDark)
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                val message = if (showAddMemberButtonInStatus) {
                    "You're the only person in '$groupName'. Add members to split expenses."
                } else {
                    "$memberCount members in this group."
                }
                Text(message, color = TextWhite, fontSize = 16.sp, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (showAddMemberButtonInStatus) {
                    Button(
                        onClick = onAddMemberClick, // Use passed lambda
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Member", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Members", color = TextWhite, fontSize = 14.sp)
                    }
                }
            }
        }
    }
}
// --- Stacked Member Photos Component ---
@Composable
fun StackedMemberPhotos(
    memberIds: List<String>,
    membersMap: Map<String, User>,
    maxVisible: Int = 5 // Show max 5 photos, then "+X" for remaining
) {
    val members = memberIds.mapNotNull { membersMap[it] }

    if (members.isEmpty()) return

    Row(
        modifier = Modifier.height(32.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val visibleMembers = members.take(maxVisible)
        val remainingCount = members.size - visibleMembers.size

        visibleMembers.forEachIndexed { index, member ->
            Box(
                modifier = Modifier
                    .offset(x = (index * -8).dp) // Overlap by 8dp
                    .size(32.dp)
                    .clip(CircleShape)
                    .border(2.dp, DarkBackground, CircleShape), // Border to separate overlapping photos
                contentAlignment = Alignment.Center
            ) {
                if (member.profilePictureUrl.isNotEmpty()) {
                    AsyncImage(
                        model = member.profilePictureUrl,
                        contentDescription = member.username,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = member.username,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }
            }
        }

        // Show "+X" indicator if there are more members
        if (remainingCount > 0) {
            Box(
                modifier = Modifier
                    .offset(x = (visibleMembers.size * -8).dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(2.dp, DarkBackground, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+$remainingCount",
                    color = TextWhite,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

// --- Totals Bottom Sheet Content ---
@Composable
fun TotalsSheetContent(
    totals: GroupTotals,
    membersMap: Map<String, User>,
    groupName: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Text(
            text = "Totals for $groupName",
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Total Spent Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Total Spent",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "MYR %.2f".format(totals.totalSpent),
                    color = TextWhite,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Sum of all expenses in this group",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Average Per Person Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Average Per Person",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "MYR %.2f".format(totals.averagePerPerson),
                    color = PrimaryBlue,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Total spent divided by ${if (totals.totalPaidByMembers.isNotEmpty()) totals.totalPaidByMembers.size else 0} ${if (totals.totalPaidByMembers.size == 1) "member" else "members"}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Total Pending Settlements Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Pending Settlements",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "MYR %.2f".format(totals.totalPendingSettlements),
                    color = if (totals.totalPendingSettlements > 0) NegativeRed else PositiveGreen,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = if (totals.totalPendingSettlements > 0) "Total amount yet to be settled" else "All settled up!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Total Added by Members Card
        if (totals.totalPaidByMembers.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Contributions by Member",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    // Sort by amount (highest to lowest)
                    totals.totalPaidByMembers.entries
                        .sortedByDescending { it.value }
                        .forEach { (uid, amount) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Member name
                                val memberName = membersMap[uid]?.username
                                    ?: membersMap[uid]?.fullName
                                    ?: "Unknown"

                                Text(
                                    text = memberName,
                                    color = TextWhite,
                                    fontSize = 15.sp,
                                    modifier = Modifier.weight(1f)
                                )

                                // Amount contributed
                                Text(
                                    text = "MYR %.2f".format(amount),
                                    color = PositiveGreen,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            if (uid != totals.totalPaidByMembers.entries.sortedByDescending { it.value }.last().key) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 4.dp),
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                        }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// --- Balances Bottom Sheet Content ---
@Composable
fun BalancesSheetContent(
    balanceBreakdown: List<MemberBalanceDetail>,
    membersMap: Map<String, User>,
    groupName: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Text(
            text = "Balances for $groupName",
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (balanceBreakdown.isEmpty()) {
            // All settled up message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "All Settled Up!",
                        color = PositiveGreen,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Everyone's balanced in this group",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Balance breakdown list
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    balanceBreakdown.forEachIndexed { index, detail ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Member profile and name
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                // Find the user object for this member
                                val memberUid = membersMap.entries.find {
                                    it.value.username == detail.memberName || it.value.fullName == detail.memberName
                                }?.key
                                val user = memberUid?.let { membersMap[it] }

                                // Profile photo
                                if (user?.profilePictureUrl?.isNotEmpty() == true) {
                                    AsyncImage(
                                        model = user.profilePictureUrl,
                                        contentDescription = "${detail.memberName}'s profile",
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        Icons.Default.AccountCircle,
                                        contentDescription = detail.memberName,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(40.dp)
                                    )
                                }

                                Spacer(Modifier.width(12.dp))

                                Column {
                                    Text(
                                        text = detail.memberName,
                                        color = TextWhite,
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Medium
                                    )

                                    // Descriptive text
                                    val descriptionText = when {
                                        detail.amount > 0.01 -> "owes you"
                                        detail.amount < -0.01 -> "you owe"
                                        else -> "settled"
                                    }

                                    Text(
                                        text = descriptionText,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontSize = 13.sp
                                    )
                                }
                            }

                            // Amount
                            Text(
                                text = "MYR %.2f".format(detail.amount.absoluteValue),
                                color = if (detail.amount > 0) PositiveGreen else NegativeRed,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Divider between items (not after the last item)
                        if (index < balanceBreakdown.size - 1) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// --- Charts Bottom Sheet Content ---
@Composable
fun ChartsSheetContent(
    chartData: ChartData,
    groupName: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Title
        Text(
            text = "Charts for $groupName",
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        if (chartData.categoryBreakdown.isEmpty()) {
            // No data message
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "No Data Available",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Add some expenses to see charts",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Category Breakdown Chart
            Text(
                text = "Spending by Category",
                color = TextWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
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

            Spacer(Modifier.height(24.dp))

            // Member Contributions Chart
            if (chartData.memberContributions.isNotEmpty()) {
                Text(
                    text = "Contributions by Member",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        chartData.memberContributions.forEach { member ->
                            HorizontalBarChart(
                                label = member.memberName,
                                value = member.amount,
                                percentage = member.percentage,
                                color = PositiveGreen
                            )
                            Spacer(Modifier.height(12.dp))
                        }
                    }
                }
            }

            Spacer(Modifier.height(24.dp))

            // Daily Spending Chart
            if (chartData.dailySpending.isNotEmpty()) {
                Text(
                    text = "Spending Over Time",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SurfaceDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    DailySpendingChart(
                        dailySpending = chartData.dailySpending,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

// --- Horizontal Bar Chart Component ---
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
                color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                .background(MaterialTheme.colorScheme.outline)
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

// --- Daily Spending Chart Component ---
@Composable
fun DailySpendingChart(
    dailySpending: List<DailySpending>,
    modifier: Modifier = Modifier
) {
    val maxAmount = dailySpending.maxOfOrNull { it.amount } ?: 1.0

    Column(modifier = modifier) {
        // Simple bar chart
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            dailySpending.takeLast(7).forEach { day -> // Show last 7 days
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Bar
                    val heightFraction = (day.amount / maxAmount).toFloat().coerceIn(0.1f, 1f)
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height((150 * heightFraction).dp)
                            .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp))
                            .background(PrimaryBlue)
                    )
                    Spacer(Modifier.height(4.dp))
                    // Date label
                    Text(
                        text = SimpleDateFormat("dd", Locale.getDefault()).format(Date(day.date)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        // Legend
        Text(
            text = "Last ${dailySpending.takeLast(7).size} days",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )
    }
}
