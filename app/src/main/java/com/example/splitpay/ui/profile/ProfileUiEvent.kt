package com.example.splitpay.ui.profile

sealed interface ProfileUiEvent {
    object NavigateToWelcome : ProfileUiEvent
    object NavigateToEditProfile : ProfileUiEvent
}