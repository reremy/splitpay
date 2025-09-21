package com.example.splitpay.ui.signup

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.UserRepository
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await


class SignUpViewModel(
    private val repository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<SignUpUiEvent>()
    val uiEvent: SharedFlow<SignUpUiEvent> = _uiEvent

    private val currentUser = repository.getCurrentUser()
    private val firestore = FirebaseFirestore.getInstance()

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
                emailError = null,
                usernameError = null,
                passwordError = null,
                retypePasswordError = null,
                fullNameError = null
            )
        }

        val state =_uiState.value

        if (state.fullName.isBlank()) {
            _uiState.update { it.copy(fullNameError = "Full name cannot be empty") }
            isValid = false
        }

        if (state.email.isBlank() || !Patterns.EMAIL_ADDRESS.matcher(state.email).matches()){
            _uiState.update { it.copy(emailError = "Invalid Email")}
            isValid = false
        }

        if (state.username.isBlank() ){
            _uiState.update { it.copy(usernameError = "Username cannot be empty")}
            isValid = false
        }

        if (state.password.length < 6){
            _uiState.update { it.copy(passwordError = "Password must be at least 6 characters")}
            isValid = false
        }

        if (state.password != state.retypePassword){
            _uiState.update { it.copy(retypePasswordError = "Passwords do not match")}
            isValid = false
        }

        if (isValid) {
            viewModelScope.launch {
                _uiState.update { it.copy(isLoading = true, errorMessage = "") }
                try {
                    val result = repository.signUp(state.email, state.password)
                    val firebaseUser = result.user

                    firebaseUser?.updateProfile(
                        UserProfileChangeRequest.Builder()
                            .setDisplayName(state.fullName)
                            .build()
                    )?.await()

                    firebaseUser?.let { user ->
                        val userDoc = User(
                            uid = user.uid,
                            fullName = state.fullName,
                            username = state.username,
                            email = state.email
                        )

                        firestore.collection("users")
                            .document(user.uid)
                            .set(userDoc)
                            .await()
                    }

                    _uiEvent.emit(SignUpUiEvent.NavigateToHome)
                } catch (e: Exception) {
                    when (e) {
                        is FirebaseAuthUserCollisionException -> {
                            _uiState.update { it.copy(emailError = "Email is already in use")}
                        }
                        is FirebaseAuthWeakPasswordException -> {
                            _uiState.update { it.copy(passwordError = "Password is too weak")}
                        }
                        is FirebaseAuthInvalidCredentialsException -> {
                            _uiState.update { it.copy(emailError = "Invalid email format")}
                        }
                        is FirebaseNetworkException -> {
                            _uiState.update { it.copy(emailError = "Check your Internet connection")} //should be a toast
                        }
                        else -> {
                            _uiState.update { it.copy(errorMessage = "Sign up failed: ${e.message}") }
                        }
                    }
                } finally {
                    _uiState.update { it.copy(isLoading = false) }
                }
            }
        }
    }
}


sealed interface SignUpUiEvent {
    object NavigateToHome : SignUpUiEvent
    object NavigateToBack : SignUpUiEvent
}