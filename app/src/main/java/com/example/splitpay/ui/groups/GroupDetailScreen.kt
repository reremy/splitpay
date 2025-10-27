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
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart // NEW IMPORT (Placeholder)
import androidx.compose.material.icons.filled.SyncAlt // NEW IMPORT (Placeholder)
import androidx.compose.material.icons.filled.TravelExplore
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


// Helper map needed for icon lookup
val availableIconsMap = mapOf(
    "travel" to Icons.Default.TravelExplore,
    "family" to Icons.Default.FamilyRestroom,
    "kitchen" to Icons.Default.Dining,
    "other" to Icons.Default.Group,
    "place" to Icons.Default.Place,
)

// --- ViewModel Factory ---
class GroupDetailViewModelFactory(
    private val groupsRepository: GroupsRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository
    // Potentially add SavedStateHandle if needed
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
            return GroupDetailViewModel(groupsRepository, expenseRepository, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupDetailScreen(
    groupId: String,
    // Remove viewModel default, use factory
    // viewModel: GroupDetailViewModel = viewModel(),
    navController: NavHostController,
    onNavigateBack: () -> Unit,
    // Provide default repositories (consider Hilt later)
    groupsRepository: GroupsRepository = GroupsRepository(),
    expenseRepository: ExpenseRepository = ExpenseRepository(),
    userRepository: UserRepository = UserRepository()
) {
// --- Use Factory to create ViewModel ---
    val factory = GroupDetailViewModelFactory(groupsRepository, expenseRepository, userRepository)
    val viewModel: GroupDetailViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState() // CORRECTED
    val group = uiState.group
    // Use combined loading state or handle separately
    val isLoading = uiState.isLoadingGroup || uiState.isLoadingExpenses

    // --- Call the new loading function ---
    LaunchedEffect(groupId) {
        viewModel.loadGroupAndExpenses(groupId)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
        topBar = {
            // --- Add TopAppBar ---
            TopAppBar(
                title = {
                    Text(
                        group?.name ?: "Group Details", // Show group name or default
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis // Handle long names
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = {
                        // --- Navigate to the new settings screen ---
                        navController.navigate("group_settings/$groupId")
                    }) {
                        Icon(Icons.Default.Settings, contentDescription = "Group Settings", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2D2D2D)) // Match header color
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    navController.navigate("add_expense/$groupId")
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
                // Show loading indicator if either group or expenses are loading initially
                isLoading && group == null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                group != null -> {
                    // Pass group and now expenses down
                    GroupDetailContent(
                        group = group,
                        expenses = uiState.expenses, // Pass expenses list
                        viewModel = viewModel // Pass viewModel for helpers
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

// --- REINTRODUCE GroupDetailHeader Composable ---
// (Or create a similar composable to display icon/name below TopAppBar)
@Composable
fun GroupDetailHeaderDisplay(group: Group) { // Renamed to avoid confusion
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
            val icon = availableIconsMap[group.iconIdentifier] ?: Icons.Default.Group
            Icon(icon, contentDescription = "Group Icon", tint = TextWhite, modifier = Modifier.size(40.dp))
        }

        Spacer(Modifier.height(12.dp))

        // Group Name
        Text(
            text = group.name,
            color = TextWhite,
            fontSize = 24.sp, // Larger font size for prominence
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        // Group Balance (Placeholder from example)
        Text(
            text = "Stevie owes you MYR18.00", // Example text - Replace with actual data later
            color = PositiveGreen,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}


@Composable
fun GroupDetailContent(
    group: Group,
    expenses: List<Expense>, // <-- Receive expenses list
    viewModel: GroupDetailViewModel // <-- Receive viewModel
) {
    // --- CHANGED: Use LazyColumn for the whole screen content ---
    LazyColumn(
        modifier = Modifier.fillMaxSize()
        // No top padding here, header handles it
        // No bottom padding needed as Scaffold padding is on the Box wrapper
    ) {
        // --- 1. Custom Top Bar / Header ---
        item {
            GroupDetailHeaderDisplay(group = group)
            Spacer(Modifier.height(16.dp))
        }

        // --- 2. Action Buttons ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ActionButtonsRow()
                Spacer(Modifier.height(16.dp))
            }
        }

        // --- 3. Group Status / Member Section ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                GroupMemberStatus(
                    memberCount = group.members.size,
                    groupName = group.name,
                    // Pass the ViewModel functions (or keep TODOs for now)
                    onAddMemberClick = { /* TODO: Call ViewModel.onShowAddMemberDialog() */ },
                    onShareLinkClick = { /*TODO*/ }
                )
                Spacer(Modifier.height(24.dp))
            }
        }

        // --- 4. Activities Title ---
        item {
            Text(
                text = "Activities", // Title for the list
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(Modifier.height(8.dp))
        }

        // --- 5. Activity List ---
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

                ExpenseActivityCard( // Use a new composable for real data
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

        // --- 6. Spacer at the bottom ---
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
    // Convert timestamp to Date/Calendar if needed for month/day
    // For simplicity, using placeholder date for now
    val day = "??"; val month = "???" // TODO: Format expense.date

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