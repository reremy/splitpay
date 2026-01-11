package com.example.splitpay.ui.friends.friendSettleUp

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.theme.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

// UI State
data class SelectPayerFriendUiState(
    val isLoading: Boolean = true,
    val currentUser: User? = null,
    val friend: User? = null,
    val error: String? = null
)

// ViewModel
class SelectPayerFriendViewModel(
    private val friendId: String,
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SelectPayerFriendUiState())
    val uiState: StateFlow<SelectPayerFriendUiState> = _uiState.asStateFlow()

    init {
        loadUsers()
    }

    private fun loadUsers() {
        viewModelScope.launch {
            try {
                val currentUserId = userRepository.getCurrentUser()?.uid
                if (currentUserId == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "User not logged in"
                    )
                    return@launch
                }

                val currentUser = userRepository.getUserProfile(currentUserId)
                val friend = userRepository.getUserProfile(friendId)

                if (currentUser == null || friend == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        error = "Failed to load users"
                    )
                    return@launch
                }

                _uiState.value = SelectPayerFriendUiState(
                    isLoading = false,
                    currentUser = currentUser,
                    friend = friend,
                    error = null
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = "Error: ${e.message}"
                )
            }
        }
    }
}

// ViewModel Factory
class SelectPayerFriendViewModelFactory(
    private val friendId: String,
    private val userRepository: UserRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SelectPayerFriendViewModel::class.java)) {
            return SelectPayerFriendViewModel(friendId, userRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SelectPayerFriendScreen(
    friendId: String,
    onNavigateBack: () -> Unit,
    onPayerSelected: (String, String) -> Unit, // payerUid, recipientUid
    userRepository: UserRepository = UserRepository()
) {
    val factory = SelectPayerFriendViewModelFactory(friendId, userRepository)
    val viewModel: SelectPayerFriendViewModel = viewModel(factory = factory)
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Who paid?",
                        color = TextWhite,
                        fontSize = 20.sp
                    )
                },
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
            else -> {
                val users = listOfNotNull(uiState.currentUser, uiState.friend)

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(Modifier.height(8.dp)) }

                    item {
                        Text(
                            text = "Select who made the payment",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }

                    items(users) { user ->
                        val isCurrentUser = user.uid == uiState.currentUser?.uid
                        val recipient = if (isCurrentUser) uiState.friend else uiState.currentUser

                        UserSelectionCard(
                            user = user,
                            isCurrentUser = isCurrentUser,
                            onClick = {
                                recipient?.let { recip ->
                                    onPayerSelected(user.uid, recip.uid)
                                }
                            }
                        )
                    }

                    item { Spacer(Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
fun UserSelectionCard(
    user: User,
    isCurrentUser: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = DialogBackground
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Profile Picture
            if (user.profilePictureUrl.isNotEmpty()) {
                AsyncImage(
                    model = user.profilePictureUrl,
                    contentDescription = "${user.username}'s profile",
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryBlue.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = "Profile",
                        tint = PrimaryBlue,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(Modifier.width(16.dp))

            // User Name
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isCurrentUser) "You" else user.fullName.ifBlank { user.username },
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                if (!isCurrentUser && user.username.isNotBlank()) {
                    Text(
                        text = "@${user.username}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
