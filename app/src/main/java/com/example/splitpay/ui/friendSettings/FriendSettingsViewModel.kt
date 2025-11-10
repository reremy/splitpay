package com.example.splitpay.ui.friendSettings

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ExpenseRepository
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
    val showReportUserDialog: Boolean = false,
    val removalError: String? = null, // Error message when removal is not allowed
    val showSuccessMessage: String? = null, // Success message after removal/block
    val shouldNavigateBack: Boolean = false // Flag to trigger navigation back
)

class FriendSettingsViewModel(
    private val userRepository: UserRepository,
    private val groupsRepository: GroupsRepository,
    private val expenseRepository: ExpenseRepository,
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

    fun confirmRemoveFriend() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, removalError = null) }

            try {
                // 1. Check if there are shared groups
                if (uiState.value.sharedGroups.isNotEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            removalError = "You cannot remove ${uiState.value.friend?.username} because you share ${uiState.value.sharedGroups.size} group(s) together. Please leave or remove them from those groups first.",
                            showRemoveFriendDialog = false
                        )
                    }
                    return@launch
                }

                // 2. Check if there are any non-group expenses with balance
                val nonGroupExpenses = expenseRepository.getNonGroupExpensesBetweenUsers(currentUserId!!, friendId)

                // Calculate balance between current user and friend in non-group expenses
                var balance = 0.0
                for (expense in nonGroupExpenses) {
                    val currentUserPaidAmount = expense.paidBy.find { it.uid == currentUserId }?.paidAmount ?: 0.0
                    val friendPaidAmount = expense.paidBy.find { it.uid == friendId }?.paidAmount ?: 0.0
                    val currentUserOwes = expense.participants.find { it.uid == currentUserId }?.owesAmount ?: 0.0
                    val friendOwes = expense.participants.find { it.uid == friendId }?.owesAmount ?: 0.0

                    // Calculate net balance for this expense
                    balance += (currentUserPaidAmount - currentUserOwes) - (friendPaidAmount - friendOwes)
                }

                if (kotlin.math.abs(balance) > 0.01) {
                    val formattedBalance = "MYR%.2f".format(kotlin.math.abs(balance))
                    val message = if (balance > 0) {
                        "You cannot remove ${uiState.value.friend?.username} because they owe you $formattedBalance in non-group expenses. Please settle up first."
                    } else {
                        "You cannot remove ${uiState.value.friend?.username} because you owe them $formattedBalance in non-group expenses. Please settle up first."
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            removalError = message,
                            showRemoveFriendDialog = false
                        )
                    }
                    return@launch
                }

                // 3. All checks passed, proceed with removal
                val result = userRepository.removeFriend(friendId)

                result.onSuccess {
                    logD("Successfully removed friend: $friendId")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showRemoveFriendDialog = false,
                            showSuccessMessage = "Successfully removed ${uiState.value.friend?.username} from your friends."
                        )
                    }
                }.onFailure { e ->
                    logE("Failed to remove friend: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            removalError = "Failed to remove friend: ${e.message}",
                            showRemoveFriendDialog = false
                        )
                    }
                }

            } catch (e: Exception) {
                logE("Error during friend removal: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        removalError = "An error occurred: ${e.message}",
                        showRemoveFriendDialog = false
                    )
                }
            }
        }
    }

    fun confirmBlockUser() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val result = userRepository.blockUser(friendId)

                result.onSuccess {
                    logD("Successfully blocked user: $friendId")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            showBlockUserDialog = false,
                            showSuccessMessage = "Successfully blocked ${uiState.value.friend?.username}. They can no longer interact with you."
                        )
                    }
                }.onFailure { e ->
                    logE("Failed to block user: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to block user: ${e.message}",
                            showBlockUserDialog = false
                        )
                    }
                }

            } catch (e: Exception) {
                logE("Error during user blocking: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "An error occurred: ${e.message}",
                        showBlockUserDialog = false
                    )
                }
            }
        }
    }

    fun clearRemovalError() {
        _uiState.update { it.copy(removalError = null) }
    }

    fun dismissSuccessMessage() {
        _uiState.update { it.copy(showSuccessMessage = null, shouldNavigateBack = true) }
    }

    fun resetNavigationFlag() {
        _uiState.update { it.copy(shouldNavigateBack = false) }
    }
}
