package com.example.splitpay.ui.signup

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logE
import kotlinx.coroutines.flow.asSharedFlow

class SignUpViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<SignUpUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun onFullNameChange(value: String) {
        _uiState.update { it.copy(fullName = value) }
    }

    fun onUsernameChange(value: String) {
        _uiState.update { it.copy(username = value) }
    }

    fun onEmailChange(value: String) {
        _uiState.update { it.copy(email = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onRetypePasswordChange(value: String) {
        _uiState.update { it.copy(retypePassword = value) }
    }

    fun onSignUpClick() {
        var isValid = true
        _uiState.update {
            it.copy(
                fullNameError = null,
                usernameError = null,
                emailError = null,
                passwordError = null,
                retypePasswordError = null,
                errorMessage = ""
            )
        }

        val state = _uiState.value

        // --- Validation Checks (Duplicated logic from Validator.kt for simplicity in current structure) ---
        if (state.fullName.isBlank()) {
            _uiState.update { it.copy(fullNameError = "Full name cannot be empty") }
            isValid = false
        }

        if (state.username.isBlank()) {
            _uiState.update { it.copy(usernameError = "Username cannot be empty") }
            isValid = false
        }

        if (state.email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Invalid email") }
            isValid = false
        }

        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            isValid = false
        }

        if (state.password != state.retypePassword) {
            _uiState.update { it.copy(retypePasswordError = "Passwords do not match") }
            isValid = false
        }
        // --- End Validation Checks ---

        if (!isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // The repository method signUp2 returns a Result<Unit> which is good for handling success/failure
                val result = repository.signUp(
                    fullName = state.fullName,
                    username = state.username,
                    email = state.email,
                    password = state.password
                )

                result.onSuccess {
                    _uiEvent.emit(SignUpUiEvent.NavigateToHome)
                }.onFailure { e ->
                    // Handle specific Firebase errors
                    when (e) {
                        is FirebaseAuthUserCollisionException ->
                            _uiState.update { it.copy(emailError = "Email already in use") }

                        is FirebaseAuthWeakPasswordException ->
                            _uiState.update { it.copy(passwordError = "Weak password") }

                        is FirebaseAuthInvalidCredentialsException ->
                            _uiState.update { it.copy(emailError = "Invalid email format") }

                        is FirebaseNetworkException ->
                            _uiState.update { it.copy(errorMessage = "No internet connection") }

                        else -> {
                            // Log the unexpected error and update generic message
                            logE("Unexpected sign-up error: ${e.message}")
                            _uiState.update { it.copy(errorMessage = e.message ?: "Sign-up failed due to an unknown error.") }
                        }
                    }
                }
            } catch (e: Exception) {
                // Catch any exceptions not handled within the try block (less likely, but safer)
                logE("Critical error during sign-up launch: ${e.message}")
                _uiState.update { it.copy(errorMessage = "A critical error occurred: ${e.message}") }
            } finally {
                // Ensure isLoading is reset regardless of success or failure
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
