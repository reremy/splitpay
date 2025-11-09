package com.example.splitpay.ui.profile.edit

import android.net.Uri

data class EditProfileUiState(
    val fullName: String = "",
    val username: String = "", // Read-only, just for display
    val email: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String = "",
    val qrCodeUrl: String = "",
    val profilePictureUri: Uri? = null, // Temporary URI before upload
    val qrCodeUri: Uri? = null, // Temporary URI before upload
    val fullNameError: String? = null,
    val emailError: String? = null,
    val phoneNumberError: String? = null,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isUploadingProfilePicture: Boolean = false,
    val isUploadingQrCode: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)
