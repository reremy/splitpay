package com.example.splitpay.ui.profile

data class ProfileUiState(
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String = "",
    val qrCodeUrl: String = "",
    val isLoading: Boolean = true,
    val isLoggingOut: Boolean = false,
    val showQrCode: Boolean = false,
    val error: String? = null,
    // Account deletion related fields
    val isDeletingAccount: Boolean = false,
    val showDeleteConfirmation: Boolean = false,
    val deleteErrorMessage: String? = null,
)
