package com.example.splitpay.ui.friendSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.groups.availableTagsMap
import com.example.splitpay.ui.theme.*

// ViewModel Factory
class FriendSettingsViewModelFactory(
    private val userRepository: UserRepository,
    private val groupsRepository: GroupsRepository,
    private val expenseRepository: ExpenseRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(FriendSettingsViewModel::class.java)) {
            return FriendSettingsViewModel(
                userRepository,
                groupsRepository,
                expenseRepository,
                savedStateHandle
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendSettingsScreen(
    friendId: String,
    onNavigateBack: () -> Unit,
    onNavigateToGroupDetail: (String) -> Unit,
    userRepository: UserRepository = UserRepository(),
    groupsRepository: GroupsRepository = GroupsRepository(),
    expenseRepository: ExpenseRepository = ExpenseRepository()
) {
    val savedStateHandle = SavedStateHandle(mapOf("friendId" to friendId))
    val factory = FriendSettingsViewModelFactory(
        userRepository,
        groupsRepository,
        expenseRepository,
        savedStateHandle
    )
    val viewModel: FriendSettingsViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    // Handle navigation after successful removal/block
    LaunchedEffect(uiState.shouldNavigateBack) {
        if (uiState.shouldNavigateBack) {
            viewModel.resetNavigationFlag()
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friend Settings", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
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
            uiState.friend != null -> {
                FriendSettingsContent(
                    uiState = uiState,
                    innerPadding = innerPadding,
                    onNavigateToGroupDetail = onNavigateToGroupDetail,
                    onRemoveFriendClick = viewModel::onRemoveFriendClick,
                    onBlockUserClick = viewModel::onBlockUserClick,
                    onReportUserClick = viewModel::onReportUserClick
                )
            }
        }

        // Dialogs
        if (uiState.showRemoveFriendDialog) {
            RemoveFriendDialog(
                friendName = uiState.friend?.username ?: "",
                hasSharedGroups = uiState.sharedGroups.isNotEmpty(),
                onDismiss = viewModel::onDismissRemoveFriendDialog,
                onConfirm = viewModel::confirmRemoveFriend
            )
        }

        if (uiState.showBlockUserDialog) {
            BlockUserDialog(
                friendName = uiState.friend?.username ?: "",
                onDismiss = viewModel::onDismissBlockUserDialog,
                onConfirm = viewModel::confirmBlockUser
            )
        }

        if (uiState.showReportUserDialog) {
            ReportUserDialog(
                onDismiss = viewModel::onDismissReportUserDialog
            )
        }

        // Removal Error Dialog
        uiState.removalError?.let { errorMessage ->
            AlertDialog(
                onDismissRequest = viewModel::clearRemovalError,
                title = { Text("Cannot Remove Friend", color = TextWhite) },
                text = { Text(errorMessage, color = Color.Gray) },
                confirmButton = {
                    TextButton(onClick = viewModel::clearRemovalError) {
                        Text("OK", color = PrimaryBlue)
                    }
                },
                containerColor = Color(0xFF2D2D2D)
            )
        }

        // Success Message Dialog
        uiState.showSuccessMessage?.let { successMessage ->
            AlertDialog(
                onDismissRequest = viewModel::dismissSuccessMessage,
                title = { Text("Success", color = TextWhite) },
                text = { Text(successMessage, color = TextWhite) },
                confirmButton = {
                    TextButton(onClick = viewModel::dismissSuccessMessage) {
                        Text("OK", color = PrimaryBlue)
                    }
                },
                containerColor = Color(0xFF2D2D2D)
            )
        }
    }
}

@Composable
fun FriendSettingsContent(
    uiState: FriendSettingsUiState,
    innerPadding: PaddingValues,
    onNavigateToGroupDetail: (String) -> Unit,
    onRemoveFriendClick: () -> Unit,
    onBlockUserClick: () -> Unit,
    onReportUserClick: () -> Unit
) {
    val friend = uiState.friend ?: return

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        item { Spacer(Modifier.height(8.dp)) }

        // Friend Profile Section
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF2D2D2D)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Profile Picture
                    if (friend.profilePictureUrl.isNotEmpty()) {
                        AsyncImage(
                            model = friend.profilePictureUrl,
                            contentDescription = "${friend.username}'s profile",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .background(PrimaryBlue),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.AccountCircle,
                                contentDescription = "Profile",
                                tint = TextWhite,
                                modifier = Modifier.size(60.dp)
                            )
                        }
                    }

                    // Full Name
                    Text(
                        text = friend.fullName,
                        color = TextWhite,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Username
                    if (friend.username.isNotBlank() && friend.username != friend.fullName) {
                        Text(
                            text = "@${friend.username}",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }

                    // Email
                    if (friend.email.isNotBlank()) {
                        Text(
                            text = friend.email,
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }

        // Shared Groups Section
        if (uiState.sharedGroups.isNotEmpty()) {
            item {
                Column {
                    Text(
                        text = "Shared Groups (${uiState.sharedGroups.size})",
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        uiState.sharedGroups.forEach { group ->
                            SharedGroupCard(
                                group = group,
                                onClick = { onNavigateToGroupDetail(group.id) }
                            )
                        }
                    }
                }
            }
        }

        // Manage Relationships Section
        item {
            Column {
                Text(
                    text = "Manage Relationship",
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Remove from Friends Button
                    Button(
                        onClick = onRemoveFriendClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2D2D2D)
                        )
                    ) {
                        Icon(
                            Icons.Default.PersonRemove,
                            contentDescription = null,
                            tint = TextWhite
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Remove from Friends",
                            color = TextWhite,
                            fontSize = 16.sp
                        )
                    }

                    // Block User Button
                    Button(
                        onClick = onBlockUserClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2D2D2D)
                        )
                    ) {
                        Icon(
                            Icons.Default.Block,
                            contentDescription = null,
                            tint = NegativeRed
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Block User",
                            color = NegativeRed,
                            fontSize = 16.sp
                        )
                    }

                    // Report User Button
                    Button(
                        onClick = onReportUserClick,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2D2D2D)
                        )
                    ) {
                        Icon(
                            Icons.Default.Report,
                            contentDescription = null,
                            tint = Color(0xFFFFA726)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = "Report User",
                            color = Color(0xFFFFA726),
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
fun SharedGroupCard(
    group: Group,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2D2D2D)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group Icon
            val groupIcon = group.iconIdentifier?.let { availableTagsMap[it] } ?: Icons.Default.Group
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
                    text = group.name,
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${group.members.size} members",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }

            // Arrow Icon
            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.Gray
            )
        }
    }
}

@Composable
fun RemoveFriendDialog(
    friendName: String,
    hasSharedGroups: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2D2D2D),
        title = {
            Text(
                text = "Remove from Friends?",
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = if (hasSharedGroups) {
                    "You have shared groups with $friendName. Please delete all shared groups before removing this friend."
                } else {
                    "Are you sure you want to remove $friendName from your friends?"
                },
                color = TextWhite
            )
        },
        confirmButton = {
            if (!hasSharedGroups) {
                TextButton(
                    onClick = onConfirm,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = NegativeRed
                    )
                ) {
                    Text("Remove")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = PrimaryBlue
                )
            ) {
                Text(if (hasSharedGroups) "OK" else "Cancel")
            }
        }
    )
}

@Composable
fun BlockUserDialog(
    friendName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2D2D2D),
        title = {
            Text(
                text = "Block User?",
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Text(
                text = "Are you sure you want to block $friendName? They will no longer be able to interact with you.",
                color = TextWhite
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = NegativeRed
                )
            ) {
                Text("Block")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = PrimaryBlue
                )
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun ReportUserDialog(
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF2D2D2D),
        title = {
            Text(
                text = "Report User",
                color = TextWhite,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Why are you reporting this user?",
                    color = TextWhite,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Non-functional buttons for now
                OutlinedButton(
                    onClick = { /* TODO: Implement */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextWhite
                    )
                ) {
                    Text("Other customer support")
                }

                OutlinedButton(
                    onClick = { /* TODO: Implement */ },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextWhite
                    )
                ) {
                    Text("Report abuse")
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = PrimaryBlue
                )
            ) {
                Text("Cancel")
            }
        }
    )
}
