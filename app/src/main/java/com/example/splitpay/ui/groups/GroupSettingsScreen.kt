package com.example.splitpay.ui.groups

import android.R.attr.onClick
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ExitToApp
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
import coil.compose.AsyncImage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.* // Import your theme colors
import com.google.firebase.auth.FirebaseAuth
import kotlin.math.absoluteValue

// --- ViewModel Factory (Required because ViewModel has constructor args) ---
class GroupSettingsViewModelFactory(
    private val groupsRepository: GroupsRepository,
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    private val activityRepository: ActivityRepository, // <-- ADD THIS
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(GroupSettingsViewModel::class.java)) {
            return GroupSettingsViewModel(
                groupsRepository,
                userRepository,
                expenseRepository,
                activityRepository,
                savedStateHandle
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- Main Screen Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupSettingsScreen(
    groupId: String, // Receive groupId from navigation
    onNavigateBack: () -> Unit,
    onNavigateToAddMembers: () -> Unit,
    onNavigateToEditGroup: () -> Unit,
    // Provide default repository instances, consider Hilt later
    groupsRepository: GroupsRepository = GroupsRepository(),
    userRepository: UserRepository = UserRepository(),
    expenseRepository: ExpenseRepository = ExpenseRepository(),
    activityRepository: ActivityRepository = ActivityRepository()
) {
    // --- Use SavedStateHandle with factory ---
    val savedStateHandle = SavedStateHandle(mapOf("groupId" to groupId))
    val factory = GroupSettingsViewModelFactory(
        groupsRepository,
        userRepository,
        expenseRepository,
        activityRepository,
        savedStateHandle
    )
    val viewModel: GroupSettingsViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()
    val group = uiState.group
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid // Get current user ID for checks

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            GroupSettingsUiEvent.NavigateBack -> onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Group Settings", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryBlue)
            }
        } else if (uiState.error != null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Error: ${uiState.error}", color = NegativeRed)
            }
        } else if (group != null) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // --- Group Info Section ---
                item {
                    GroupInfoSection(
                        groupName = group.name,
                        iconIdentifier = group.iconIdentifier,
                        photoUrl = group.photoUrl,
                        onEditClick = onNavigateToEditGroup
                    )
                }

                // --- Group Members Section ---
                item {
                    GroupMembersSection(
                        members = uiState.members,
                        currentUserId = currentUserId,
                        isAdmin = uiState.isCurrentUserAdmin,
                        onAddClick = onNavigateToAddMembers,
                        onRemoveClick = { user -> viewModel.showRemoveMemberConfirmation(user) } // Pass user data
                    )
                }

                // --- Advanced Settings Section ---
                item {
                    AdvancedSettingsSection(
                        canLeave = uiState.currentUserBalanceInGroup == 0.0, // Enable based on balance
                        isCurrentUserAdmin = uiState.isCurrentUserAdmin,
                        onLeaveClick = { viewModel.showLeaveGroupConfirmation(true) },
                        onDeleteClick = { viewModel.showDeleteGroupConfirmation(true) }
                    )
                }
            } // End LazyColumn

            // --- DIALOGS ---

            // Edit Name Dialog (Example)
            if (uiState.showEditNameDialog) {
                EditGroupNameDialog(
                    currentName = group.name,
                    onDismiss = { viewModel.showEditNameDialog(false) },
                    onConfirm = { newName -> viewModel.updateGroupName(newName) }
                )
            }

            if (uiState.showChangeIconDialog) {
                ChangeIconDialog(
                    currentIconIdentifier = group.iconIdentifier, // Pass current icon
                    onDismiss = { viewModel.showChangeIconDialog(false) },
                    onIconSelected = { newIconIdentifier ->
                        viewModel.updateGroupIcon(newIconIdentifier) // Call ViewModel function
                        // No need to dismiss here, updateGroupIcon handles it
                    }
                )
            }

            val userToRemove = uiState.showRemoveMemberConfirmation // Get the user object from state
            if (userToRemove != null) {
                RemoveMemberConfirmationDialog(
                    memberName = userToRemove.username.takeIf { it.isNotBlank() } ?: userToRemove.fullName, // Display name
                    onDismiss = { viewModel.showRemoveMemberConfirmation(null) }, // Clear state on dismiss
                    onConfirm = { viewModel.removeMember(userToRemove.uid) } // Call removeMember with UID
                )
            }

            if (uiState.cannotRemoveDialogMessage != null) {
                CannotRemoveMemberDialog(
                    message = uiState.cannotRemoveDialogMessage!!, // Pass the message
                    onDismiss = { viewModel.clearCannotRemoveDialog() } // Call dismiss function
                )
            }

            // TODO: Add RemoveMemberConfirmationDialog (using uiState.showRemoveMemberConfirmation)

            if (uiState.showLeaveGroupConfirmation) {
                LeaveGroupConfirmationDialog(
                    groupName = group.name, // Pass group name for context
                    onDismiss = { viewModel.showLeaveGroupConfirmation(false) },
                    onConfirm = { viewModel.leaveGroup() } // Call leaveGroup function
                )
            }


            // --- ADD: Delete Group Confirmation Dialog ---
            if (uiState.showDeleteGroupConfirmation) {
                DeleteGroupConfirmationDialog(
                    groupName = group.name,
                    isDeleting = uiState.isDeleting,
                    onDismiss = { viewModel.showDeleteGroupConfirmation(false) },
                    onConfirm = { viewModel.deleteGroup() } // <-- Call implemented function
                )
            }

            // Display "Cannot Leave" message if applicable
            if (uiState.leaveGroupErrorMessage != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.clearLeaveGroupError() },
                    title = { Text("Cannot Leave Group") },
                    text = { Text(uiState.leaveGroupErrorMessage!!) },
                    confirmButton = {
                        Button(onClick = { viewModel.clearLeaveGroupError() }) { Text("OK") }
                    },
                    containerColor = Color(0xFF3C3C3C), // Darker dialog
                    titleContentColor = TextWhite,
                    textContentColor = Color.Gray
                )
            }

        } else {
            // Handle case where group is null after loading (should be caught by error state ideally)
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Text("Group not found.", color = Color.Gray)
            }
        }
    }
}


// --- UI Sections ---

@Composable
fun GroupInfoSection(
    groupName: String,
    iconIdentifier: String,
    photoUrl: String,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Group Photo or Tag Icon
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(CircleShape)
                .background(if (photoUrl.isNotEmpty()) Color.Transparent else PrimaryBlue),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl.isNotEmpty()) {
                // Display group photo
                AsyncImage(
                    model = photoUrl,
                    contentDescription = "Group Photo",
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                // Display tag icon
                Icon(
                    imageVector = availableTagsMap[iconIdentifier] ?: Icons.Default.Group,
                    contentDescription = "Group Tag",
                    tint = TextWhite,
                    modifier = Modifier.size(32.dp)
                )
            }
        }
        Spacer(Modifier.width(16.dp))
        // Name and Type
        Column(modifier = Modifier.weight(1f)) {
            Text(groupName, color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(
                iconIdentifier.replaceFirstChar { it.uppercase() }, // Simple category name
                color = Color.Gray,
                fontSize = 14.sp
            )
        }
        // Edit Button
        IconButton(onClick = onEditClick) {
            Icon(Icons.Default.Edit, contentDescription = "Edit Group Info", tint = Color.Gray)
        }
    }
}

@Composable
fun GroupMembersSection(
    members: List<GroupMemberViewData>,
    currentUserId: String?,
    isAdmin: Boolean,
    onAddClick: () -> Unit,
    onRemoveClick: (User) -> Unit // Pass the User object
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Group Members", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                members.forEach { memberData ->
                    MemberListItem(
                        memberData = memberData,
                        isCurrentUser = memberData.user.uid == currentUserId,
                        showRemoveButton = isAdmin && memberData.user.uid != currentUserId, // Admin can remove others
                        onRemoveClick = { onRemoveClick(memberData.user) }
                )
                    HorizontalDivider(color = Color(0xFF454545))
            }
                // Add People Button
                ListItem(
                    headlineContent = { Text("Add people", color = PrimaryBlue) },
                    leadingContent = { Icon(Icons.Default.Add, contentDescription = "Add People", tint = PrimaryBlue) },
                    modifier = Modifier.clickable(onClick = onAddClick),
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                // TODO: Add "Invite via Link" button if needed
            }
        }
    }
}

@Composable
fun MemberListItem(
    memberData: GroupMemberViewData,
    isCurrentUser: Boolean,
    showRemoveButton: Boolean,
    onRemoveClick: () -> Unit
) {
    ListItem(
        headlineContent = {
            Text(
                text = "${memberData.user.fullName}${if (isCurrentUser) " (you)" else ""}",
                color = TextWhite,
                fontWeight = FontWeight.Medium
            )
        },
        supportingContent = { Text(memberData.user.email, color = Color.Gray, fontSize = 12.sp) },
        leadingContent = {
            if (memberData.user.profilePictureUrl.isNotEmpty()) {
                AsyncImage(
                    model = memberData.user.profilePictureUrl,
                    contentDescription = "${memberData.user.username}'s profile picture",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    Icons.Default.AccountCircle,
                    contentDescription = "Member",
                    tint = Color.Gray,
                    modifier = Modifier.size(40.dp)
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Balance Status
                val balance = memberData.balance
                val (text, color) = when {
                    balance > 0.01 -> "Gets back" to PositiveGreen
                    balance < -0.01 -> "Owes" to NegativeRed
                    else -> "Settled" to Color.Gray
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(text, color = color, fontSize = 12.sp)
                    if (balance.absoluteValue > 0.01) {
                        Text(
                            "MYR%.2f".format(balance.absoluteValue),
                            color = color,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                // Remove Button (only shown if applicable)
                if (showRemoveButton) {
                    IconButton(onClick = onRemoveClick) {
                        Icon(Icons.Default.RemoveCircleOutline, contentDescription = "Remove Member", tint = NegativeRed)
                    }
                }
            }
        },
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
}

@Composable
fun AdvancedSettingsSection(
    canLeave: Boolean,
    isCurrentUserAdmin: Boolean,
    onLeaveClick: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text("Advanced Settings", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                // Leave Group
                ListItem(
                    headlineContent = {
                        Text(
                            "Leave group",
                            color = if (canLeave) NegativeRed else Color.Gray // Dim if disabled
                        )
                    },
                    leadingContent = {
                        Icon(
                            Icons.AutoMirrored.Filled.ExitToApp,
                            contentDescription = "Leave Group",
                            tint = if (canLeave) NegativeRed else Color.Gray
                        )
                    },
                    modifier = Modifier.clickable(enabled = canLeave, onClick = onLeaveClick), // Disable click if can't leave
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                )
                if (!canLeave) {
                    Text(
                        "You can't leave the group. You have outstanding debts with other group members.",
                        color = Color.Gray,
                        fontSize = 12.sp,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp)
                    )
                }

                // Delete Group (Admin only)
                if (isCurrentUserAdmin) {
                    HorizontalDivider(color = Color(0xFF454545))
                    ListItem(
                        headlineContent = { Text("Delete group", color = NegativeRed) },
                        leadingContent = { Icon(Icons.Default.Delete, contentDescription = "Delete Group", tint = NegativeRed) },
                        modifier = Modifier.clickable(onClick = onDeleteClick),
                        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                    )
                }
            }
        }
    }
}


// --- DIALOG COMPOSABLES (Add implementations here) ---

@Composable
fun EditGroupNameDialog(
    currentName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit
) {
    var newName by remember { mutableStateOf(currentName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Group Name", color = TextWhite) },
        text = {
            OutlinedTextField(
                value = newName,
                onValueChange = { newName = it },
                label = { Text("Group Name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors( // Use colors for consistency
                    focusedContainerColor = Color(0xFF3C3C3C),
                    unfocusedContainerColor = Color(0xFF3C3C3C),
                    disabledContainerColor = Color(0xFF3C3C3C),
                    cursorColor = PrimaryBlue,
                    focusedIndicatorColor = PrimaryBlue, // Or Color.Transparent
                    unfocusedIndicatorColor = Color.Gray, // Or Color.Transparent
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    disabledTextColor = Color.Gray,
                    focusedLabelColor = PrimaryBlue,
                    unfocusedLabelColor = TextPlaceholder
                )
            )
        },
        confirmButton = {
            Button(onClick = { onConfirm(newName) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        },
        containerColor = Color(0xFF3C3C3C) // Darker dialog
    )
}

@Composable
fun ChangeIconDialog(
    currentIconIdentifier: String,
    onDismiss: () -> Unit,
    onIconSelected: (String) -> Unit // Passes back the selected identifier string
) {
    var selectedIcon by remember { mutableStateOf(currentIconIdentifier) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Change Group Tag", color = TextWhite) },
        text = {
            // Re-use the TagOption composable from CreateGroupScreen logic
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(availableTags.size) { index ->
                    val (identifier, icon) = availableTags[index]
                    TagOption(
                        icon = icon,
                        label = identifier.replaceFirstChar { it.uppercase() },
                        identifier = identifier,
                        isSelected = selectedIcon == identifier,
                        onSelect = { selectedIcon = identifier } // Update local selection state
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onIconSelected(selectedIcon) }, // Pass the locally selected icon back
                enabled = selectedIcon != currentIconIdentifier // Only enable if changed
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        },
        containerColor = Color(0xFF3C3C3C) // Darker dialog background
    )
}

// TODO: @Composable fun AddMemberDialog(...) { ... }

@Composable
fun RemoveMemberConfirmationDialog(
    memberName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
    // isRemoving: Boolean, // Optional: for showing loading state on button
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remove Member?", color = TextWhite) },
        text = {
            Text(
                "Are you sure you want to remove $memberName from this group?",
                color = Color.Gray
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                // enabled = !isRemoving,
                colors = ButtonDefaults.buttonColors(containerColor = NegativeRed) // Use red for destructive action
            ) {
                // if (isRemoving) { /* Show loading indicator */ } else { Text("Remove") }
                Text("Remove")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss /*, enabled = !isRemoving */) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF3C3C3C) // Darker dialog
    )
}

@Composable
fun CannotRemoveMemberDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Cannot Remove Member", color = TextWhite) },
        text = { Text(message, color = Color.Gray) }, // Display the message from state
        confirmButton = {
            Button(
                onClick = onDismiss, // OK button simply dismisses
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) { Text("OK") }
        },
        containerColor = Color(0xFF3C3C3C) // Darker dialog
    )
}

@Composable
fun LeaveGroupConfirmationDialog(
    groupName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
    // isLoading: Boolean // Optional: for showing loading state on button
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Leave Group?", color = TextWhite) },
        text = {
            Text(
                "Are you sure you want to leave '$groupName'?",
                color = Color.Gray
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                // enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = NegativeRed) // Red for potentially leaving debts (though checked)
            ) {
                // if (isLoading) { /* Show loading indicator */ } else { Text("Leave") }
                Text("Leave")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss /*, enabled = !isLoading */) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF3C3C3C) // Darker dialog
    )
}


// --- ADD: Delete Group Confirmation Dialog ---
@Composable
fun DeleteGroupConfirmationDialog(
    groupName: String,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Group?", color = TextWhite) },
        text = {
            Text(
                "Are you sure you want to delete '$groupName'? This will permanently delete all associated expenses. This action cannot be undone.",
                color = Color.Gray
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting, // Disable button while deleting
                colors = ButtonDefaults.buttonColors(containerColor = NegativeRed)
            ) {
                if (isDeleting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = TextWhite,
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isDeleting) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF3C3C3C) // Darker dialog
    )
}