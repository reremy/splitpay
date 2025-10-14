package com.example.splitpay.ui.profile

data class ProfileUiState(
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
)
