package com.example.splitpay.ui.blockedUsers

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BlockedUsersUiState(
    val isLoading: Boolean = true,
    val blockedUsers: List<User> = emptyList(),
    val error: String? = null,
    val isUnblocking: String? = null // UID of user being unblocked, or null
)

class BlockedUsersViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(BlockedUsersUiState())
    val uiState: StateFlow<BlockedUsersUiState> = _uiState.asStateFlow()

    init {
        loadBlockedUsers()
    }

    private fun loadBlockedUsers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val currentUserUid = userRepository.getCurrentUser()?.uid
                if (currentUserUid == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "User not logged in."
                        )
                    }
                    return@launch
                }

                val currentUser = userRepository.getUserProfile(currentUserUid)
                if (currentUser == null) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "User profile not found."
                        )
                    }
                    return@launch
                }

                // Get blocked user UIDs
                val blockedUserIds = currentUser.blockedUsers

                if (blockedUserIds.isEmpty()) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            blockedUsers = emptyList()
                        )
                    }
                    return@launch
                }

                // Fetch profiles for blocked users
                val blockedUserProfiles = userRepository.getProfilesForFriends(blockedUserIds)

                logD("Loaded ${blockedUserProfiles.size} blocked users")

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        blockedUsers = blockedUserProfiles,
                        error = null
                    )
                }

            } catch (e: Exception) {
                logE("Error loading blocked users: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load blocked users: ${e.message}"
                    )
                }
            }
        }
    }

    fun unblockUser(userUid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUnblocking = userUid) }

            try {
                val result = userRepository.unblockUser(userUid)

                result.onSuccess {
                    logD("Successfully unblocked user: $userUid")
                    // Reload the list
                    loadBlockedUsers()
                }.onFailure { e ->
                    logE("Failed to unblock user: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isUnblocking = null,
                            error = "Failed to unblock user: ${e.message}"
                        )
                    }
                }

            } catch (e: Exception) {
                logE("Error during user unblocking: ${e.message}")
                _uiState.update {
                    it.copy(
                        isUnblocking = null,
                        error = "An error occurred: ${e.message}"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }
}
