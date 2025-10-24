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
import androidx.compose.material.icons.filled.Add // Keep this import
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart // NEW IMPORT (Placeholder)
import androidx.compose.material.icons.filled.SyncAlt // NEW IMPORT (Placeholder)
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.navigation.Screen
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.NegativeRed // NEW IMPORT
import com.example.splitpay.ui.theme.PositiveGreen // NEW IMPORT
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


// Helper map needed for icon lookup
val availableIconsMap = mapOf(
    "travel" to Icons.Default.TravelExplore,
    "family" to Icons.Default.FamilyRestroom,
    "kitchen" to Icons.Default.Dining,
    "other" to Icons.Default.Group,
    "place" to Icons.Default.Place,
)


@Composable
fun GroupDetailScreen(
    groupId: String,
    viewModel: GroupDetailViewModel = viewModel(),
    navController: NavHostController,
    onNavigateBack: () -> Unit
) {
    val group by viewModel.group.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = DarkBackground,
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                // --- APPLY PADDING HERE ---
                // We apply padding to the Box so the FAB doesn't overlap the list bottom
                .padding(innerPadding)
                .background(DarkBackground)
        ) {
            if (isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (group != null) {
                val currentGroup = group!!
                GroupDetailContent(
                    group = currentGroup,
                    onNavigateBack = onNavigateBack
                )
            } else {
                Text(
                    text = "Group not found or an error occurred.",
                    color = NegativeRed, // Use theme color
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

// --- MOCK DATA FOR ACTIVITY LIST ---
data class ActivityItem(
    val id: String,
    val day: String,
    val month: String,
    val icon: ImageVector,
    val description: String,
    val paidBy: String, // "You paid RM16.00" or "Nur A. paid RM18.00"
    val userShare: String, // "you lent RM4.00" or "you borrowed RM2.00"
    val userShareColor: Color
)

val mockActivities = listOf(
    ActivityItem(
        id = "1", day = "13", month = "Jun", icon = Icons.Default.SyncAlt,
        description = "test subject 2",
        paidBy = "Nur A. paid Ramizan MYR18.00",
        userShare = "you lent MYR4.00",
        userShareColor = PositiveGreen
    ),
    ActivityItem(
        id = "2", day = "13", month = "Jun", icon = Icons.Default.Dining,
        description = "Chinese Tea Ping",
        paidBy = "You paid MYR4.00",
        userShare = "you lent MYR4.00",
        userShareColor = PositiveGreen
    ),
    ActivityItem(
        id = "3", day = "13", month = "Jun", icon = Icons.Default.ShoppingCart,
        description = "Nasi Ayam Geprek Butter",
        paidBy = "You paid MYR16.00",
        userShare = "you lent MYR16.00",
        userShareColor = PositiveGreen
    ),
    ActivityItem(
        id = "4", day = "13", month = "Jun", icon = Icons.Default.ShoppingCart,
        description = "test subject",
        paidBy = "You paid MYR16.00",
        userShare = "you borrowed MYR2.00",
        userShareColor = NegativeRed // Example of borrowing
    )
)
// --- END OF MOCK DATA ---


@Composable
fun GroupDetailContent(
    group: Group,
    onNavigateBack: () -> Unit,
) {
    // --- CHANGED: Use LazyColumn for the whole screen content ---
    LazyColumn(
        modifier = Modifier.fillMaxSize()
        // No top padding here, header handles it
        // No bottom padding needed as Scaffold padding is on the Box wrapper
    ) {
        // --- 1. Custom Top Bar / Header ---
        item {
            GroupDetailHeader(group = group, onNavigateBack = onNavigateBack)
        }

        // --- 2. Action Buttons ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                ActionButtonsRow()
                Spacer(Modifier.height(32.dp))
            }
        }

        // --- 3. Group Status / Member Section ---
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                GroupMemberStatus(
                    memberCount = group.members.size,
                    groupName = group.name,
                    onAddMemberClick = { /*TODO*/ },
                    onShareLinkClick = { /*TODO*/ }
                )
                Spacer(Modifier.height(24.dp)) // Space before activity list
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
        items(mockActivities, key = { it.id }) { activity ->
            ActivityCard(
                activity = activity,
                modifier = Modifier.padding(horizontal = 16.dp) // Apply padding to each card
            )
            Spacer(Modifier.height(8.dp)) // Space between cards
        }

        // --- 6. Spacer at the bottom ---
        item {
            Spacer(Modifier.height(72.dp)) // Spacer for FAB
        }
    }
}

// --- NEW COMPOSABLE: ActivityCard ---
@Composable
fun ActivityCard(
    activity: ActivityItem,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
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
            Text(activity.month, color = Color.Gray, fontSize = 12.sp)
            Text(activity.day, color = TextWhite, fontSize = 16.sp, fontWeight = FontWeight.Medium)
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
                imageVector = activity.icon,
                contentDescription = activity.description,
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
            Text(activity.description, color = TextWhite, fontSize = 16.sp)
            Spacer(Modifier.height(2.dp))
            Text(activity.paidBy, color = Color.Gray, fontSize = 12.sp)
        }

        Spacer(Modifier.width(8.dp))

        // User's Share (Lent/Borrowed)
        Column(horizontalAlignment = Alignment.End) {
            Text(
                activity.userShare,
                color = activity.userShareColor,
                fontSize = 12.sp
            )
            // This part from your image seems to be the user's *total share* of the expense,
            // not just the lent/borrowed amount.
            Text(
                "MYR${activity.userShare.filter { it.isDigit() || it == '.' }}", // Extract amount
                color = activity.userShareColor,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}


@Composable
fun GroupDetailHeader(group: Group, onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2D2D))
            .padding(bottom = 16.dp, top = 16.dp), // Adjusted padding
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack, modifier = Modifier.size(48.dp).padding(end = 16.dp)) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
            // Spacer to help center the title, assuming back button is the only item on left
            Spacer(Modifier.weight(1f))
            Text(
                text = group.name,
                color = TextWhite,
                fontSize = 20.sp, // Slightly smaller title to fit
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.weight(1f))
            // Placeholder for potential right-side actions
            Spacer(Modifier.width(48.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Group Icon (Smaller)
        Box(
            modifier = Modifier
                .size(80.dp) // Smaller icon
                .clip(CircleShape)
                .background(PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            val icon = availableIconsMap[group.iconIdentifier] ?: Icons.Default.Group
            Icon(icon, contentDescription = "Group Icon", tint = TextWhite, modifier = Modifier.size(40.dp)) // Smaller icon
        }

        Spacer(Modifier.height(12.dp))

        // Group Balance (Placeholder from example)
        Text(
            text = "Stevie owes you MYR18.00", // Example text
            color = PositiveGreen, // Use theme color
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
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
    val isOnlyUser = memberCount == 1

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
                    .background(if (isOnlyUser) Color(0xFF454545) else Color(0xFF3C3C3C))
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                val message = if (isOnlyUser) {
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
                Button(
                    onClick = onAddMemberClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Member", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Members", color = TextWhite, fontSize = 14.sp)
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = onShareLinkClick,
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF454545)),
                    modifier = Modifier.weight(1f).height(48.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share Link", modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Share Link", color = TextWhite, fontSize = 14.sp)
                }
            }
        }
    }
}