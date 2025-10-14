package com.example.splitpay.ui.login

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.repository.UserRepositoryTry
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.example.splitpay.logger.logI
import com.example.splitpay.logger.logW
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch


class LoginViewModel(
    private val repository: UserRepositoryTry = UserRepositoryTry()
) : ViewModel() {

    //private val currentUser = repository.getCurrentUser()

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<LoginUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onEmailChange(newEmail: String) {
        _uiState.update { it.copy(email = newEmail) }
    }

    fun onPasswordChange(newPassword: String) {
        _uiState.update { it.copy(password = newPassword) }
    }

    fun onLoginClick() {
        val state = _uiState.value

        _uiState.update {
            it.copy(
                emailError = null,
                passwordError = null,
                generalError = null,
                loginSuccess = false
            )
        }

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

        if (hasError) {
            logD("Login validation failed. Errors on screen.")
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            logD("Starting login request for: ${state.email}")
            try {
                val user = repository.signIn2(state.email, state.password)
                if (user != null){
                    logI("Login successful! Navigating to home.")
                    _uiState.update { it.copy(loginSuccess = true) }
                    _uiEvent.emit(LoginUiEvent.NavigateToHome)
                } else {
                    logW("Login returned null user, but no exception was thrown.")
                    _uiState.update { it.copy(generalError = "Login failed") }
                }
            } catch (e: Exception) {
                logE("Login failed with exception: ${e.message}")
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
