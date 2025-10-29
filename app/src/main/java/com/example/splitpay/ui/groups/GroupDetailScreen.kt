package com.example.splitpay.ui.groups

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Add // Keep this import
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitpay.data.model.Expense // <-- Add import
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.repository.ExpenseRepository // <-- Add import
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository // <-- Add import
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.NegativeRed // NEW IMPORT
import com.example.splitpay.ui.theme.PositiveGreen // NEW IMPORT
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import androidx.lifecycle.SavedStateHandle // <-- Add import
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


// Helper map needed for icon lookup
val availableIconsMap = mapOf(
    "travel" to Icons.Default.TravelExplore,
    "family" to Icons.Default.FamilyRestroom,
    "kitchen" to Icons.Default.Dining,
    "other" to Icons.Default.Group,
    "place" to Icons.Default.Place,
    "info" to Icons.Default.Info // Add info icon mapping
)

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
    groupId: String, // Keep receiving groupId
    navController: NavHostController,
    onNavigateBack: () -> Unit,
    // Provide default repository instances
    groupsRepository: GroupsRepository = GroupsRepository(),
    expenseRepository: ExpenseRepository = ExpenseRepository(),
    userRepository: UserRepository = UserRepository()
) {
    // Use Factory to create ViewModel
    val factory = GroupDetailViewModelFactory(groupsRepository, expenseRepository, userRepository)
    val viewModel: GroupDetailViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()
    val group = uiState.group
    // Use combined loading state or handle separately
    val isLoading = uiState.isLoadingGroup || uiState.isLoadingExpenses

    // --- State for Info Dialog ---
    val showInfoDialog = remember { mutableStateOf(false) }

    // Call the loading function when groupId changes
    LaunchedEffect(groupId) {
        viewModel.loadGroupAndExpenses(groupId)
    }

    // --- Info Dialog Composable ---
    if (showInfoDialog.value) {
        AlertDialog(
            onDismissRequest = { showInfoDialog.value = false },
            title = { Text("Non-Group Expenses Info", color = TextWhite) },
            text = { Text(
                "This section shows expenses shared directly with individuals, not within a group setting. These expenses cannot be modified or deleted from this view.",
                color = Color.Gray
            )},
            confirmButton = {
                Button(
                    onClick = { showInfoDialog.value = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { Text("OK") }
            },
            containerColor = Color(0xFF2D2D2D) // Match theme dark dialog background
        )
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
                    // --- MODIFIED: Conditional Icon ---
                    if (groupId == "non_group") {
                        // Show Info icon for non-group
                        IconButton(onClick = { showInfoDialog.value = true }) { // Show dialog on click
                            Icon(Icons.Default.Info, contentDescription = "Non-Group Info", tint = TextWhite)
                        }
                    } else {
                        // Show Settings icon for regular groups
                        IconButton(onClick = {
                            // Navigate to the group settings screen
                            navController.navigate("group_settings/$groupId")
                        }) {
                            Icon(Icons.Default.Settings, contentDescription = "Group Settings", tint = TextWhite)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2D2D2D)) // Match header color
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
                        viewModel = viewModel
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
fun GroupDetailHeaderDisplay(group: Group, iconIdentifier: String?) { // Added parameter
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2D2D)) // Match TopAppBar color
            .padding(bottom = 16.dp, top = 16.dp), // Add padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Group Icon
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            // --- Use iconIdentifier from map, default to Group icon ---
            val icon = availableIconsMap[iconIdentifier] ?: Icons.Default.Group // Use map
            Icon(icon, contentDescription = "Group Icon", tint = TextWhite, modifier = Modifier.size(40.dp))
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

        // TODO: Calculate and display correct balance (group or non-group)
        // This requires enhancing the ViewModel to calculate non-group balance too.
        val isNonGroup = group.id == "non_group"
        val balanceText = if (isNonGroup) "Your non-group balance: MYR0.00" else "Stevie owes you MYR18.00" // Placeholder
        val balanceColor = if (isNonGroup) Color.Gray else PositiveGreen // Placeholder color

        Text(
            text = balanceText,
            color = balanceColor,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


// --- MODIFIED: Accepts groupId ---
@Composable
fun GroupDetailContent(
    groupId: String, // <-- Added parameter
    group: Group,
    expenses: List<Expense>,
    viewModel: GroupDetailViewModel
) {
    // Determine if this is the special non-group view
    val isNonGroup = groupId == "non_group"

    LazyColumn(
        modifier = Modifier.fillMaxSize()
    ) {
        // --- 1. Custom Header ---
        item {
            // Pass the correct icon identifier from the group object
            GroupDetailHeaderDisplay(group = group, iconIdentifier = group.iconIdentifier)
            Spacer(Modifier.height(16.dp))
        }

        // --- 2. Conditionally hide Action Buttons and Member Status for non-group ---
        if (!isNonGroup) {
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ActionButtonsRow()
                    Spacer(Modifier.height(16.dp))
                }
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    GroupMemberStatus(
                        memberCount = group.members.size,
                        groupName = group.name,
                        onAddMemberClick = { /* TODO: Call ViewModel.onShowAddMemberDialog() */ },
                        onShareLinkClick = { /*TODO*/ }
                    )
                    Spacer(Modifier.height(24.dp))
                }
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
                    color = Color.Gray,
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

                ExpenseActivityCard(
                    expense = expense,
                    payerSummary = payerSummary,
                    userLentBorrowed = lentBorrowedText,
                    userLentBorrowedColor = lentBorrowedColor,
                    modifier = Modifier.padding(horizontal = 16.dp)
                    // TODO: Add onClick navigation to expense detail later
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

    // Determine icon (placeholder)
    val icon = Icons.Default.ShoppingCart // TODO: Choose icon based on category/type?

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
        // Icon
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(0xFF3C3C3C)), // Icon background
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
            Text(payerSummary, color = Color.Gray, fontSize = 12.sp) // Use formatted summary
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
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
            */
        }
    }
}


@Composable
fun ActionButtonsRow() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        ActionButton("Settle Up", {})
        ActionButton("Charts", {})
        ActionButton("Balances", {})
        ActionButton("Total", {})
        ActionButton("Export", {})
    }
}


@Composable
fun ActionButton(label: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3C3C3C)),
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
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(10.dp) // Match example
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Status Box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(if (showAddMemberButtonInStatus) Color(0xFF454545) else Color(0xFF3C3C3C))
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
                        modifier = Modifier.weight(1f).height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add Member", modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add Members", color = TextWhite, fontSize = 14.sp)
                    }
                    Spacer(Modifier.width(10.dp))
                }
                // Share Link Button (always shown?)
                Button(
                    onClick = onShareLinkClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF454545)),
                    // --- Adjust weight if Add Members button is hidden ---
                    modifier = Modifier.weight(if (showAddMemberButtonInStatus) 1f else 2f).height(48.dp) // Takes full width if alone
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Link", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Link", color = TextWhite, fontSize = 14.sp)
                }
            }
        }
    }
}