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
    private val repository: UserRepository = UserRepository()
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

}
