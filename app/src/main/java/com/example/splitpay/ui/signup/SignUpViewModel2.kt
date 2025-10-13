package com.example.splitpay.ui.signup

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.example.splitpay.data.repository.UserRepositoryTry
class SignUpViewModel2(
    private val repository: UserRepositoryTry = UserRepositoryTry()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<SignUpUiEvent>()
    val uiEvent: SharedFlow<SignUpUiEvent> = _uiEvent

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

        if (!isValid) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val result = repository.signUp2(
                    fullName = state.fullName,
                    username = state.username,
                    email = state.email,
                    password = state.password
                )

                result.onSuccess {
                    _uiEvent.emit(SignUpUiEvent.NavigateToHome)
                }.onFailure { e ->
                    when (e) {
                        is FirebaseAuthUserCollisionException ->
                            _uiState.update { it.copy(emailError = "Email already in use") }

                        is FirebaseAuthWeakPasswordException ->
                            _uiState.update { it.copy(passwordError = "Weak password") }

                        is FirebaseAuthInvalidCredentialsException ->
                            _uiState.update { it.copy(emailError = "Invalid email format") }

                        is FirebaseNetworkException ->
                            _uiState.update { it.copy(errorMessage = "No internet connection") }

                        else ->
                            _uiState.update { it.copy(errorMessage = e.message ?: "Sign-up failed") }
                    }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }
}
