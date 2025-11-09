package com.example.splitpay.ui.profile.edit

sealed class EditProfileUiEvent {
    object NavigateBack : EditProfileUiEvent()
    data class ShowMessage(val message: String) : EditProfileUiEvent()
}
