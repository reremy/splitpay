package com.example.splitpay.ui.friendSettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FriendSettingsUiState(
    val friend: User? = null,
    val isLoading: Boolean = true,
    val sharedGroups: List<Group> = emptyList(),
    val error: String? = null,
    val showRemoveFriendDialog: Boolean = false,
    val showBlockUserDialog: Boolean = false,
    val showReportUserDialog: Boolean = false
)

class FriendSettingsViewModel(
    private val userRepository: UserRepository,
    private val groupsRepository: GroupsRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendSettingsUiState())
    val uiState: StateFlow<FriendSettingsUiState> = _uiState.asStateFlow()

    private val friendId: String = savedStateHandle["friendId"] ?: ""
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        if (friendId.isBlank() || currentUserId == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Invalid friend ID or user not logged in."
                )
            }
        } else {
            loadFriendSettings()
        }
    }

    private fun loadFriendSettings() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load friend profile
                val friend = userRepository.getUserProfile(friendId)
                if (friend == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Friend not found."
                        )
                    }
                    return@launch
                }

                // Get all groups that both users are in
                val allGroups = groupsRepository.getGroupsSuspend()
                val sharedGroups = allGroups.filter { group ->
                    group.members.contains(currentUserId) && group.members.contains(friendId)
                }

                logD("Loaded friend settings: ${friend.username}, ${sharedGroups.size} shared groups")

                _uiState.update {
                    it.copy(
                        friend = friend,
                        sharedGroups = sharedGroups,
                        isLoading = false,
                        error = null
                    )
                }

            } catch (e: Exception) {
                logE("Error loading friend settings: ${e.message}")
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load friend settings: ${e.message}"
                    )
                }
            }
        }
    }

    // Dialog handlers
    fun onRemoveFriendClick() {
        _uiState.update { it.copy(showRemoveFriendDialog = true) }
    }

    fun onDismissRemoveFriendDialog() {
        _uiState.update { it.copy(showRemoveFriendDialog = false) }
    }

    fun onBlockUserClick() {
        _uiState.update { it.copy(showBlockUserDialog = true) }
    }

    fun onDismissBlockUserDialog() {
        _uiState.update { it.copy(showBlockUserDialog = false) }
    }

    fun onReportUserClick() {
        _uiState.update { it.copy(showReportUserDialog = true) }
    }

    fun onDismissReportUserDialog() {
        _uiState.update { it.copy(showReportUserDialog = false) }
    }

    // TODO: Implement actual removal logic
    fun confirmRemoveFriend() {
        logD("Remove friend confirmed (not implemented yet)")
        onDismissRemoveFriendDialog()
    }

    // TODO: Implement actual blocking logic
    fun confirmBlockUser() {
        logD("Block user confirmed (not implemented yet)")
        onDismissBlockUserDialog()
    }
}
