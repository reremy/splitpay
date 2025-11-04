package com.example.splitpay.ui.groups

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.theme.* // Import theme colors

// --- ViewModel Factory ---
class AddGroupMembersViewModelFactory(
    private val groupsRepository: GroupsRepository,
    private val userRepository: UserRepository,
    private val activityRepository: ActivityRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AddGroupMembersViewModel::class.java)) {
            return AddGroupMembersViewModel(
                groupsRepository,
                userRepository,
                activityRepository,
                savedStateHandle
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

// --- Screen Composable ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddGroupMembersScreen(
    groupId: String,
    onNavigateBack: () -> Unit,
    groupsRepository: GroupsRepository = GroupsRepository(),
    userRepository: UserRepository = UserRepository(),
    activityRepository: ActivityRepository = ActivityRepository()
) {
    val savedStateHandle = SavedStateHandle(mapOf("groupId" to groupId))
    val factory = AddGroupMembersViewModelFactory(
        groupsRepository,
        userRepository,
        activityRepository,
        savedStateHandle
    )
    val viewModel: AddGroupMembersViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()

    // Navigate back on success
    LaunchedEffect(uiState.addMembersSuccess) {
        if (uiState.addMembersSuccess) {
            onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { /* Title can be empty or "Add Members" */ },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    // Search Field integrated into TopAppBar
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::onSearchQueryChange,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 0.dp, end = 16.dp) // Adjust padding
                            .height(50.dp), // Control height
                        placeholder = { Text("Search Friend's username", color = TextPlaceholder) },
                        singleLine = true,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color(0xFF3C3C3C),
                            unfocusedContainerColor = Color(0xFF3C3C3C),
                            disabledContainerColor = Color(0xFF3C3C3C),
                            cursorColor = PrimaryBlue,
                            focusedIndicatorColor = Color.Transparent, // No underline
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                        ),
                        shape = RoundedCornerShape(8.dp),
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextPlaceholder) },
                        trailingIcon = {
                            if (uiState.searchQuery.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onSearchQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
                                }
                            }
                        }
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // --- Selected Friends Row ---
            if (uiState.selectedFriends.isNotEmpty()) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.selectedFriends, key = { it.uid }) { friend ->
                        SelectedFriendChip(friend = friend, onRemove = { viewModel.onFriendDeselected(friend) })
                    }
                }
                HorizontalDivider(color = Color(0xFF454545))
            }

            // --- Available Friends List ---
            if (uiState.isLoading && uiState.availableFriends.isEmpty()) { // Show loading only initially
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            } else if (uiState.error != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Error: ${uiState.error}", color = NegativeRed)
                }
            } else if (uiState.availableFriends.isEmpty() && uiState.searchQuery.isNotEmpty()) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("No friends found matching '${uiState.searchQuery}'", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else if (uiState.availableFriends.isEmpty() && uiState.allFriends.isNotEmpty()) {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("All your friends are already in this group or none found.", color = Color.Gray, textAlign = TextAlign.Center)
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f), // Takes remaining space
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.availableFriends, key = { it.uid }) { friend ->
                        val isSelected = uiState.selectedFriends.any { it.uid == friend.uid }
                        FriendSelectItem(
                            friend = friend,
                            isSelected = isSelected,
                            onToggleSelect = {
                                if (isSelected) viewModel.onFriendDeselected(friend)
                                else viewModel.onFriendSelected(friend)
                            }
                        )
                    }
                }
            }


            // --- Done Button ---
            Button(
                onClick = viewModel::onDoneClick,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .height(50.dp),
                enabled = !uiState.isLoading, // Disable while adding
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                if (uiState.isLoading && uiState.addMembersSuccess) { // Show loading indicator on button when submitting
                    CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TextWhite)
                } else {
                    Text("Done", fontSize = 18.sp, color = TextWhite)
                }
            }
        } // End Column
    } // End Scaffold
}

// --- Helper Composables ---

@Composable
fun SelectedFriendChip(friend: User, onRemove: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            Icon(
                Icons.Default.AccountCircle, // Placeholder
                contentDescription = friend.username,
                tint = Color.Gray,
                modifier = Modifier.size(50.dp)
            )
            IconButton(
                onClick = onRemove,
                modifier = Modifier
                    .offset(x = 8.dp, y = (-8).dp) // Position X icon
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.Gray)
            ) {
                Icon(Icons.Default.Close, contentDescription = "Remove", tint = DarkBackground, modifier = Modifier.size(14.dp))
            }
        }
        Text(
            friend.username,
            color = Color.Gray,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun FriendSelectItem(
    friend: User,
    isSelected: Boolean,
    onToggleSelect: () -> Unit
) {
    ListItem(
        headlineContent = { Text(friend.username, color = TextWhite) },
        supportingContent = { if (friend.fullName.isNotBlank()) Text(friend.fullName, color = Color.Gray, fontSize = 12.sp) else null },
        leadingContent = {
            Icon(
                Icons.Default.AccountCircle, // Placeholder
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(40.dp)
            )
        },
        trailingContent = {
            if (isSelected) {
                Icon(Icons.Default.CheckCircle, contentDescription = "Selected", tint = PositiveGreen)
            } else {
                Icon(Icons.Default.RadioButtonUnchecked, contentDescription = "Not Selected", tint = Color.Gray)
            }
        },
        modifier = Modifier
            .clickable(onClick = onToggleSelect)
            .padding(horizontal = 16.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
    HorizontalDivider(color = Color(0xFF454545), modifier = Modifier.padding(start = 72.dp)) // Indent divider
}