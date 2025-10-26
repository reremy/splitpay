package com.example.splitpay.ui.addfriend

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.splitpay.data.model.User
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFriendScreen(
    viewModel: AddFriendViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToProfilePreview: (String, String) -> Unit // Takes userId and username
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle one-time events
    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            is AddFriendUiEvent.NavigateToProfilePreview -> onNavigateToProfilePreview(event.userId, event.username)
            is AddFriendUiEvent.ShowSnackbar -> {
                // Snackbar should ideally be shown on the screen that receives the result (e.g., FriendsScreen)
                // For now, just log it.
                Log.d("AddFriendScreen", "Snackbar Event: ${event.message}")
                // If you want to show it here (e.g., on immediate failure), use LaunchedEffect:
                // LaunchedEffect(snackbarHostState) { snackbarHostState.showSnackbar(event.message) }
            }
            AddFriendUiEvent.NavigateBack -> onNavigateBack()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) }, // Include SnackbarHost
        topBar = {
            TopAppBar(
                title = { Text("Add Friend", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = viewModel::onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
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
                .padding(horizontal = 16.dp, vertical = 8.dp) // Apply padding consistently
        ) {
            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::onSearchQueryChange,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Search by username", color = TextPlaceholder) },
                placeholder = { Text("Start typing username...", color = TextPlaceholder) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF3C3C3C),
                    unfocusedContainerColor = Color(0xFF3C3C3C),
                    disabledContainerColor = Color(0xFF3C3C3C),
                    cursorColor = PrimaryBlue,
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    disabledTextColor = Color.Gray,
                    focusedLabelColor = PrimaryBlue,
                    unfocusedLabelColor = TextPlaceholder,
                    focusedPlaceholderColor = TextPlaceholder,
                    unfocusedPlaceholderColor = TextPlaceholder
                ),
                shape = RoundedCornerShape(8.dp),
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSearchQueryChange("") }) { // Clear query
                            Icon(Icons.Default.Clear, contentDescription = "Clear search", tint = Color.Gray)
                        }
                    }
                }
            )

            Spacer(Modifier.height(16.dp))

            // Results / Loading / Info Message Box
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter // Align content to the top below search bar
            ) {
                when {
                    // Show loading indicator
                    uiState.isLoading -> {
                        CircularProgressIndicator(modifier = Modifier.padding(top = 32.dp), color = PrimaryBlue)
                    }
                    // Show info message (e.g., "No users found" or initial prompt)
                    !uiState.infoMessage.isNullOrEmpty() -> {
                        // val message = uiState.infoMessage // Local variable is good practice but didn't work
                        Text(
                            // --- FIX: Use non-null assertion or elvis operator ---
                            text = uiState.infoMessage!!, // Force non-null (safe here after check)
                            // OR alternative: text = uiState.infoMessage ?: "", // Provide empty default
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                    !uiState.error.isNullOrEmpty() -> {
                        // val errorText = uiState.error // Local variable is good practice but didn't work
                        Text(
                            // --- FIX: Use non-null assertion or elvis operator ---
                            text = uiState.error!!, // Force non-null (safe here after check)
                            // OR alternative: text = uiState.error ?: "", // Provide empty default
                            color = NegativeRed,
                            modifier = Modifier.padding(top = 32.dp)
                        )
                    }
                    // Show results only if query is not blank, not loading, and results exist
                    uiState.searchQuery.isNotBlank() && uiState.searchResults.isNotEmpty() -> {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth(), // Take full width
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(uiState.searchResults, key = { it.uid }) { user ->
                                SearchResultItem(user = user, onClick = { viewModel.onUserSelected(user) })
                                Divider(color = Color(0xFF454545), modifier = Modifier.padding(horizontal = 8.dp)) // Divider between items
                            }
                        }
                    }
                    // Default empty state when query is blank and not loading (already handled by infoMessage)
                    else -> {
                        // Optionally show nothing or keep the initial prompt via infoMessage
                    }
                }
            } // End Box
        } // End Column
    } // End Scaffold
}

@Composable
fun SearchResultItem(user: User, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp), // Add horizontal padding for consistency
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.AccountCircle, // Placeholder icon
            contentDescription = "Profile",
            tint = Color.Gray,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) { // Allow text column to take available space
            Text(
                text = user.username,
                color = TextWhite,
                fontWeight = FontWeight.Medium,
                maxLines = 1 // Prevent wrapping if too long
            )
            // Show full name only if it exists and is different from username
            if (user.fullName.isNotBlank() && user.fullName != user.username) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = user.fullName,
                    color = Color.Gray,
                    fontSize = 12.sp,
                    maxLines = 1 // Prevent wrapping
                )
            }
        }
    }
}