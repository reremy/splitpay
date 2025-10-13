package com.example.splitpay.ui.welcome

sealed interface WelcomeUiEvent {
    object NavigateToSignUp : WelcomeUiEvent
    object NavigateToLogIn : WelcomeUiEvent
}