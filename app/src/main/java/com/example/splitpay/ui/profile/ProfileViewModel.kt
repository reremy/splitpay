package com.example.splitpay.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.example.splitpay.logger.logI
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: UserRepository = UserRepository(),
    private val groupsRepository: com.example.splitpay.data.repository.GroupsRepository = com.example.splitpay.data.repository.GroupsRepository(),
    private val expenseRepository: com.example.splitpay.data.repository.ExpenseRepository = com.example.splitpay.data.repository.ExpenseRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<ProfileUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun loadUserProfile() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            logD("Loading user profile")

            try {
                val currentUser = repository.getCurrentUser()
                if (currentUser == null) {
                    logE("Cannot load profile: User not signed in")
                    _uiState.update { it.copy(error = "User not signed in", isLoading = false) }
                    return@launch
                }

                logD("Fetching profile for user: ${currentUser.uid}")
                val userDoc = repository.getUserProfileCached(currentUser.uid)
                if (userDoc != null) {
                    logI("Profile loaded successfully for: ${userDoc.username}")
                    _uiState.update {
                        it.copy(
                            fullName = userDoc.fullName,
                            username = userDoc.username,
                            email = currentUser.email ?: "",
                            phoneNumber = userDoc.phoneNumber,
                            profilePictureUrl = userDoc.profilePictureUrl,
                            qrCodeUrl = userDoc.qrCodeUrl,
                            isLoading = false
                        )
                    }
                } else {
                    logE("User profile not found in Firestore for UID: ${currentUser.uid}")
                    _uiState.update { it.copy(error = "User profile not found", isLoading = false) }
                }

            } catch (e: Exception) {
                logE("Error loading user profile: ${e.message}", e)
                _uiState.update { it.copy(error = e.message ?: "Failed to load profile", isLoading = false) }
            }
        }
    }


    fun toggleQrCodeVisibility() {
        _uiState.update { it.copy(showQrCode = !it.showQrCode) }
        logD("QR code visibility toggled: ${_uiState.value.showQrCode}")
    }

    fun navigateToEditProfile() {
        viewModelScope.launch {
            logD("Navigating to Edit Profile screen")
            _uiEvent.emit(ProfileUiEvent.NavigateToEditProfile)
        }
    }

    fun signOut() {
        viewModelScope.launch {
            logI("Sign out initiated by user")
            _uiState.update { it.copy(isLoggingOut = true, error = null) }

            try {
                logD("Calling repository.signOut()")
                repository.signOut()
                logI("Sign out successful, navigating to Welcome screen")
                _uiEvent.emit(ProfileUiEvent.NavigateToWelcome)
            } catch (e: Exception) {
                logE("Sign out failed: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoggingOut = false,
                        error = e.message ?: "Sign out failed. Please try again."
                    )
                }
            } finally {
                // Only reset loading if we didn't navigate away
                // (if navigation happened, this screen will be destroyed anyway)
                logD("Sign out process completed")
            }
        }
    }

    // ========================================
    // Account Deletion Functions
    // ========================================

    /**
     * Called when user clicks Delete Account button.
     * Shows confirmation dialog.
     */
    fun onDeleteAccountClick() {
        logD("Delete account button clicked - showing confirmation dialog")
        _uiState.update { it.copy(showDeleteConfirmation = true) }
    }

    /**
     * Dismisses the delete confirmation dialog.
     */
    fun onDismissDeleteConfirmation() {
        logD("Delete confirmation dialog dismissed")
        _uiState.update { it.copy(showDeleteConfirmation = false) }
    }

    /**
     * Confirms account deletion.
     * Validates that user can delete account, then schedules deletion.
     */
    fun confirmDeleteAccount() {
        viewModelScope.launch {
            logI("Account deletion confirmed by user - starting validation")
            _uiState.update {
                it.copy(
                    isDeletingAccount = true,
                    showDeleteConfirmation = false,
                    deleteErrorMessage = null
                )
            }

            try {
                val currentUser = repository.getCurrentUser()
                if (currentUser == null) {
                    logE("Cannot delete account: User not signed in")
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            deleteErrorMessage = "User not signed in."
                        )
                    }
                    return@launch
                }

                // Validate account deletion
                logD("Validating account deletion for user: ${currentUser.uid}")
                val validationResult = repository.validateAccountDeletion(
                    uid = currentUser.uid,
                    groupsRepository = groupsRepository,
                    expenseRepository = expenseRepository
                )

                if (!validationResult.canDelete) {
                    // Validation failed - show error
                    logE("Account deletion validation failed: ${validationResult.errorMessage}")
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            deleteErrorMessage = validationResult.errorMessage
                        )
                    }
                    return@launch
                }

                // Validation passed - schedule deletion
                logI("Validation passed - scheduling account deletion")
                val result = repository.scheduleAccountDeletion(currentUser.uid)

                result.onSuccess {
                    logI("Account deletion scheduled successfully - signing out user")
                    // Sign out user and navigate to welcome screen
                    repository.signOut()
                    _uiEvent.emit(ProfileUiEvent.NavigateToWelcome)
                }.onFailure { e ->
                    logE("Failed to schedule account deletion: ${e.message}")
                    _uiState.update {
                        it.copy(
                            isDeletingAccount = false,
                            deleteErrorMessage = "Failed to delete account. Please try again."
                        )
                    }
                }

            } catch (e: Exception) {
                logE("Error during account deletion: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isDeletingAccount = false,
                        deleteErrorMessage = e.message ?: "An error occurred. Please try again."
                    )
                }
            }
        }
    }

    /**
     * Dismisses the delete error message dialog.
     */
    fun dismissDeleteError() {
        logD("Delete error dialog dismissed")
        _uiState.update { it.copy(deleteErrorMessage = null) }
    }

}
