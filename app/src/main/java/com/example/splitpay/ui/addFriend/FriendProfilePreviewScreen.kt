package com.example.splitpay.ui.addfriend

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.PersonAdd // Use PersonAdd for the button
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel // Standard import
import androidx.lifecycle.AbstractSavedStateViewModelFactory // Import factory
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner // Import owner
import androidx.navigation.NavBackStackEntry
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendProfilePreviewScreen(
    // Get SavedStateHandle directly from NavBackStackEntry if needed for factory
    navBackStackEntry: NavBackStackEntry,
    onNavigateBack: () -> Unit,
    showSnackbar: (String) -> Unit // Callback to show snackbar on previous screen
) {
    // --- ViewModel Initialization (remains the same using AbstractSavedStateViewModelFactory) ---
    val owner = LocalSavedStateRegistryOwner.current
    val factory = remember(owner, navBackStackEntry) {
        object : AbstractSavedStateViewModelFactory(owner!!, navBackStackEntry.arguments) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(key: String, modelClass: Class<T>, handle: SavedStateHandle): T {
                return FriendProfilePreviewViewModel(userRepository = UserRepository(), savedStateHandle = handle) as T
            }
        }
    }
    val viewModel: FriendProfilePreviewViewModel = viewModel(viewModelStoreOwner = navBackStackEntry, factory = factory)
    // --- End ViewModel Initialization ---

    val uiState by viewModel.uiState.collectAsState()

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            is FriendProfilePreviewUiEvent.ShowSnackbar -> showSnackbar(event.message)
            FriendProfilePreviewUiEvent.NavigateBack -> onNavigateBack()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.userProfile?.username ?: "User Profile", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = viewModel::onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    // --- Button Logic Update ---
                    val userProfileLoaded = uiState.userProfile != null
                    val alreadyFriends = uiState.isAlreadyFriend
                    val addingFriend = uiState.isAddingFriend

                    when {
                        // Show loading indicator while adding
                        addingFriend -> {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp).padding(end = 16.dp),
                                color = TextWhite,
                                strokeWidth = 2.dp
                            )
                        }
                        // Show disabled "Friends" button if already friends
                        userProfileLoaded && alreadyFriends -> {
                            Button(
                                onClick = {}, // No action
                                enabled = false, // Explicitly disable
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.Transparent, // No background
                                    disabledContainerColor = Color.Transparent, // No background when disabled
                                    contentColor = Color.Gray, // Text color when enabled (not relevant)
                                    disabledContentColor = Color.Gray // Gray text when disabled
                                ),
                                // Remove elevation to make it look flat
                                elevation = null
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray) // Check icon
                                Spacer(Modifier.width(4.dp))
                                Text("Friends")
                            }
                        }
                        // Show enabled "Add Friend" button if profile loaded and not friends yet
                        userProfileLoaded && !alreadyFriends -> {
                            Button(
                                onClick = viewModel::onAddFriendClick,
                                enabled = true, // Explicitly enable
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                            ) {
                                Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Add Friend")
                            }
                        }
                        // Otherwise (e.g., loading profile initially), show nothing or a placeholder
                        else -> {
                            Spacer(Modifier.width(48.dp)) // Placeholder to maintain layout
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        // --- Box and Content (remain mostly the same) ---
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when {
                uiState.isLoading -> {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
                !uiState.error.isNullOrEmpty() -> {
                    Text(uiState.error ?: "An error occurred", color = NegativeRed)
                }
                uiState.userProfile != null -> {
                    val user = uiState.userProfile!!
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        // Placeholder Profile Picture
                        Icon(
                            imageVector = Icons.Default.AccountCircle,
                            contentDescription = "Profile Picture",
                            tint = Color.Gray,
                            modifier = Modifier.size(120.dp).clip(CircleShape)
                        )
                        Spacer(Modifier.height(16.dp))
                        // Username
                        Text(
                            text = user.username,
                            color = TextWhite,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(4.dp))
                        // Full Name (if available and different)
                        if (user.fullName.isNotBlank() && user.fullName != user.username) {
                            Text(
                                text = user.fullName,
                                color = Color.Gray,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                else -> {
                    Text("User profile could not be loaded.", color = Color.Gray)
                }
            }
        }
    }
}

// Helper Icon composable if needed for profile picture
@Composable
fun ProfileIconPlaceholder(modifier: Modifier = Modifier) {
    Icon(
        imageVector = Icons.Default.AccountCircle,
        contentDescription = "Profile Picture",
        tint = Color.Gray,
        modifier = modifier.size(120.dp).clip(CircleShape)
    )
}