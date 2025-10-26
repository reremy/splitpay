package com.example.splitpay.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.repository.UserRepository
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

            try {
                val currentUser = repository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.update { it.copy(error = "User not signed in", isLoading = false) }
                    return@launch
                }

                val userDoc = repository.getUserProfile(currentUser.uid)
                if (userDoc != null) {
                    _uiState.update {
                        it.copy(
                            fullName = userDoc.fullName,
                            username = userDoc.username,
                            email = currentUser.email ?: "",
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(error = "User profile not found", isLoading = false) }
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Failed to load profile", isLoading = false) }
            }
        }
    }


    fun signOut() {
        viewModelScope.launch {
            try {
                repository.signOut()
                _uiEvent.emit(ProfileUiEvent.NavigateToWelcome)
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.message ?: "Sign out failed") }
            }
        }
    }

}
