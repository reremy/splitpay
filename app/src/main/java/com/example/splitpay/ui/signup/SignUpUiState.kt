package com.example.splitpay.ui.signup

data class SignUpUiState(
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val password: String = "",
    val retypePassword: String = "",
    val fullNameError: String? = null,
    val usernameError: String? = null,
    val emailError: String? = null,
    val phoneNumberError: String? = null,
    val passwordError: String? = null,
    val retypePasswordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String = "",
    val passwordVisible: Boolean = false,
    val retypePasswordVisible: Boolean = false
)