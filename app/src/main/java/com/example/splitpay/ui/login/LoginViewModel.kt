package com.example.splitpay.ui.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.repository.UserRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class LoginViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val currentUser = repository.getCurrentUser()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    fun onEmailChange(newEmail: String) {
        _uiState.update { it.copy(email = newEmail) }
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.update { it.copy(password = newPassword) }
    }

    fun onLoginClick() {
        _uiState.update {
            it.copy(
                emailError = null,
                passwordError = null,
                generalError = null,
                loginSuccess = false
            )
        }

        val state = _uiState.value
        var hasError = false

        if (state.email.isBlank()) {
            _uiState.update { it.copy(emailError = "Email cannot be empty") }
            hasError = true
        } else if (!Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Invalid email format") }
            hasError = true
        }

        if (state.password.isBlank()) {
            _uiState.update { it.copy(passwordError = "Password cannot be empty") }
            hasError = true
        }

        if (hasError) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, generalError = null) }
            try {
                repository.signUp(state.email, state.password)
                _uiState.update { it.copy(loginSuccess = true) }
            } catch (e: Exception) {
                when (e) {
                    is FirebaseAuthInvalidUserException ->
                        _uiState.update { it.copy(emailError = "No user found with this email") }
                    is FirebaseAuthInvalidCredentialsException ->
                        _uiState.update { it.copy(passwordError = "Invalid email or password") }
                    is FirebaseNetworkException ->
                        _uiState.update { it.copy(generalError = "Check your internet connection") }
                    else ->
                        _uiState.update { it.copy(generalError = "Login failed: ${e.message}") }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
