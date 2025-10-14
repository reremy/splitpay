package com.example.splitpay.ui.profile

import com.example.splitpay.ui.signup.SignUpUiEvent

sealed interface ProfileUiEvent {
    object NavigateToWelcome : ProfileUiEvent
}