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
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logI
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

    fun onPhoneNumberChange(value: String) {
        _uiState.update { it.copy(phoneNumber = value) }
    }

    fun onPasswordChange(value: String) {
        _uiState.update { it.copy(password = value) }
    }

    fun onRetypePasswordChange(value: String) {
        _uiState.update { it.copy(retypePassword = value) }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(passwordVisible = !it.passwordVisible) }
        logD("Password visibility toggled: ${_uiState.value.passwordVisible}")
    }

    fun toggleRetypePasswordVisibility() {
        _uiState.update { it.copy(retypePasswordVisible = !it.retypePasswordVisible) }
        logD("Retype password visibility toggled: ${_uiState.value.retypePasswordVisible}")
    }

    fun onSignUpClick() {
        logI("Sign-up initiated")
        var isValid = true
        _uiState.update {
            it.copy(
                fullNameError = null,
                usernameError = null,
                emailError = null,
                phoneNumberError = null,
                passwordError = null,
                retypePasswordError = null,
                errorMessage = ""
            )
        }

        val state = _uiState.value

        // --- Validation Checks ---
        if (state.fullName.isBlank()) {
            _uiState.update { it.copy(fullNameError = "Full name cannot be empty") }
            logD("Validation failed: Full name is empty")
            isValid = false
        }

        if (state.username.isBlank()) {
            _uiState.update { it.copy(usernameError = "Username cannot be empty") }
            logD("Validation failed: Username is empty")
            isValid = false
        }

        if (state.email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(state.email).matches()) {
            _uiState.update { it.copy(emailError = "Invalid email") }
            logD("Validation failed: Invalid email format")
            isValid = false
        }

        // Phone number validation - basic format check
        if (state.phoneNumber.isBlank()) {
            _uiState.update { it.copy(phoneNumberError = "Phone number cannot be empty") }
            logD("Validation failed: Phone number is empty")
            isValid = false
        } else if (!isValidPhoneNumber(state.phoneNumber)) {
            _uiState.update { it.copy(phoneNumberError = "Invalid phone number format") }
            logD("Validation failed: Invalid phone number format")
            isValid = false
        }

        if (state.password.length < 6) {
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters") }
            logD("Validation failed: Password too short")
            isValid = false
        }

        if (state.password != state.retypePassword) {
            _uiState.update { it.copy(retypePasswordError = "Passwords do not match") }
            logD("Validation failed: Passwords do not match")
            isValid = false
        }
        // --- End Validation Checks ---

        if (!isValid) {
            logI("Sign-up validation failed")
            return
        }

        logD("All validations passed, proceeding with sign-up")

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            logD("Starting sign-up API call for user: ${state.email}")
            try {
                val result = repository.signUp(
                    fullName = state.fullName,
                    username = state.username,
                    email = state.email,
                    phoneNumber = state.phoneNumber,
                    password = state.password
                )

                result.onSuccess {
                    logI("Sign-up successful for user: ${state.email}")
                    _uiEvent.emit(SignUpUiEvent.NavigateToHome)
                }.onFailure { e ->
                    // Handle specific Firebase errors
                    when (e) {
                        is FirebaseAuthUserCollisionException -> {
                            logD("Sign-up failed: Email already in use")
                            _uiState.update { it.copy(emailError = "Email already in use") }
                        }

                        is FirebaseAuthWeakPasswordException -> {
                            logD("Sign-up failed: Weak password")
                            _uiState.update { it.copy(passwordError = "Weak password") }
                        }

                        is FirebaseAuthInvalidCredentialsException -> {
                            logD("Sign-up failed: Invalid email format")
                            _uiState.update { it.copy(emailError = "Invalid email format") }
                        }

                        is FirebaseNetworkException -> {
                            logE("Sign-up failed: Network error")
                            _uiState.update { it.copy(errorMessage = "No internet connection") }
                        }

                        else -> {
                            logE("Unexpected sign-up error: ${e.message}", e)
                            _uiState.update { it.copy(errorMessage = e.message ?: "Sign-up failed due to an unknown error.") }
                        }
                    }
                }
            } catch (e: Exception) {
                logE("Critical error during sign-up launch: ${e.message}", e)
                _uiState.update { it.copy(errorMessage = "A critical error occurred: ${e.message}") }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
                logD("Sign-up process completed, loading state reset")
            }
        }
    }

    /**
     * Validates phone number format
     * Accepts formats like: +1234567890, (123) 456-7890, 123-456-7890, 1234567890
     */
    private fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Remove all non-digit characters to check length
        val digitsOnly = phoneNumber.replace(Regex("[^0-9]"), "")

        // Phone number should have at least 10 digits (can be more for international numbers)
        if (digitsOnly.length < 10) {
            return false
        }

        // Accept various phone number formats
        val phonePattern = Regex("^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$")
        return phonePattern.matches(phoneNumber)
    }
}
