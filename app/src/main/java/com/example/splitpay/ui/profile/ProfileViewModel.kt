package com.example.splitpay.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState

    fun loadUserProfile() {
        val currentUser = repository.getCurrentUser()
        if (currentUser == null) {
            _uiState.value = ProfileUiState(isLoading = false, error = "No user logged in")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val profile = repository.getUserProfile(currentUser.uid)
            if (profile != null) {
                _uiState.value = ProfileUiState(
                    fullName = profile.fullName,
                    username = profile.username,
                    email = profile.email,
                    isLoading = false
                )
            } else {
                _uiState.value = ProfileUiState(
                    isLoading = false,
                    error = "Failed to load profile"
                )
            }
        }
    }

    fun signOut() {
        repository.signOut()
        _uiState.value = _uiState.value.copy(signedOut = true)
    }
}
