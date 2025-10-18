package com.example.splitpay.ui.groups

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.FamilyRestroom
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
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
    onNavigateBack: () -> Unit
) {
    val group by viewModel.group.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    // Load group details on first composition
    LaunchedEffect(groupId) {
        viewModel.loadGroup(groupId)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
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
            // Handle error or group not found
            Text(
                text = "Group not found or an error occurred.",
                color = Color.Red,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@Composable
fun GroupDetailContent(
    group: Group,
    onNavigateBack: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // --- Custom Top Bar / Header ---
        GroupDetailHeader(group = group, onNavigateBack = onNavigateBack)

        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // --- Action Buttons ---
            ActionButtonsRow()
            Spacer(Modifier.height(32.dp))

            // --- Group Status / Member Section ---
            GroupMemberStatus(
                memberCount = group.members.size,
                groupName = group.name,
                onAddMemberClick = { /*TODO*/ },
                onShareLinkClick = { /*TODO*/ }
            )
            Spacer(Modifier.height(16.dp))

            // --- Balances/Activity (Placeholder) ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = "Group Balances (Placeholder)",
                        color = TextWhite,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Total spent: RM 0.00. You are all settled up!",
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun GroupDetailHeader(group: Group, onNavigateBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF2D2D2D))
            .padding(vertical = 16.dp)
            .padding(top = 16.dp), // Extra top padding to clear status bar area
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Button
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to Groups", tint = TextWhite)
            }
        }

        // Group Icon
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            val icon = availableIconsMap[group.iconIdentifier] ?: Icons.Default.Group
            Icon(icon, contentDescription = "Group Icon", tint = TextWhite, modifier = Modifier.size(52.dp))
        }

        Spacer(Modifier.height(16.dp))

        // Group Name
        Text(
            text = group.name,
            color = TextWhite,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))

        // Group Balance (Placeholder)
        Text(
            text = "Total Balance: RM 0.00",
            color = Color(0xFF66BB6A),
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
        shape = RoundedCornerShape(8.dp)
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