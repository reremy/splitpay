package com.example.splitpay.ui.addfriend

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// UI State for FriendProfilePreviewScreen
data class FriendProfilePreviewUiState(
    val userProfile: User? = null,
    val isLoading: Boolean = true,
    val isAddingFriend: Boolean = false, // Separate loading state for add action
    val error: String? = null,
    val isAlreadyFriend: Boolean = false
)

// Events specific to the preview screen
sealed interface FriendProfilePreviewUiEvent {
    data class ShowSnackbar(val message: String) : FriendProfilePreviewUiEvent
    object NavigateBack : FriendProfilePreviewUiEvent // To go back to search or friends list
}

class FriendProfilePreviewViewModel(
    private val userRepository: UserRepository = UserRepository(),
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendProfilePreviewUiState())
    val uiState: StateFlow<FriendProfilePreviewUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<FriendProfilePreviewUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    // Get the userId passed via navigation arguments
    private val userId: String? = savedStateHandle["userId"]
    private var currentUserUid: String? = userRepository.getCurrentUser()?.uid

    init {
        loadUserProfileAndCheckFriendship()
    }

    private fun loadUserProfileAndCheckFriendship() {
        if (userId == null || currentUserUid == null) {
            _uiState.update { it.copy(isLoading = false, error = "User ID not found or not logged in.") }
            logE("FriendProfilePreviewViewModel: userId ($userId) or currentUserUid ($currentUserUid) is null.")
            return
        }

        // Prevent showing profile preview for self
        if (userId == currentUserUid) {
            _uiState.update { it.copy(isLoading = false, error = "Cannot add yourself as a friend.") }
            return
        }


        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, isAlreadyFriend = false) } // Reset state

            // Fetch both profiles concurrently (slightly more efficient)
            val targetProfileDeferred = viewModelScope.async { userRepository.getUserProfile(userId) }
            val currentUserProfileDeferred = viewModelScope.async { userRepository.getCurrentUserProfileWithFriends() } // Fetches friends list

            val targetProfile = targetProfileDeferred.await()
            val currentUserProfile = currentUserProfileDeferred.await()

            if (targetProfile != null) {
                // Check if the target user's ID is in the current user's friend list
                val alreadyFriends = currentUserProfile?.friends?.contains(userId) == true
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        userProfile = targetProfile,
                        isAlreadyFriend = alreadyFriends // <-- Update friendship status
                    )
                }
            } else {
                _uiState.update { it.copy(isLoading = false, error = "Could not load user profile.") }
            }
        }
    }

    fun onAddFriendClick() {
        val userToAdd = uiState.value.userProfile ?: return
        // Prevent adding if already friends or if adding self (double check)
        if (uiState.value.isAlreadyFriend || userToAdd.uid == currentUserUid) {
            viewModelScope.launch { _uiEvent.emit(FriendProfilePreviewUiEvent.ShowSnackbar("Already friends or cannot add self.")) }
            return
        }
        val friendUid = userToAdd.uid

        viewModelScope.launch {
            _uiState.update { it.copy(isAddingFriend = true) }
            val result = userRepository.addFriend(friendUid)
            result.onSuccess {
                logD("Friend added successfully via preview screen.")
                // --- Update state to reflect friendship ---
                _uiState.update { it.copy(isAlreadyFriend = true) }
                // --- Show specific success message ---
                _uiEvent.emit(FriendProfilePreviewUiEvent.ShowSnackbar("You and ${userToAdd.username} are now friends!"))
                // Optional: Navigate back immediately, or let user see disabled button
                delay(1500) // Keep message visible briefly
                _uiEvent.emit(FriendProfilePreviewUiEvent.NavigateBack) // Navigate back
            }.onFailure { exception ->
                logE("Failed to add friend from preview: ${exception.message}")
                val message = when (exception) {
                    is IllegalArgumentException -> exception.message ?: "Cannot add friend."
                    else -> "Failed to add friend. Please try again."
                }
                _uiEvent.emit(FriendProfilePreviewUiEvent.ShowSnackbar(message))
            }
            _uiState.update { it.copy(isAddingFriend = false) }
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.emit(FriendProfilePreviewUiEvent.NavigateBack)
        }
    }
}