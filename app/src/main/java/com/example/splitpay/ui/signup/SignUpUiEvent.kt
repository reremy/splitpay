package com.example.splitpay.ui.signup

sealed interface SignUpUiEvent {
    object NavigateToHome : SignUpUiEvent
    object NavigateToBack : SignUpUiEvent
}