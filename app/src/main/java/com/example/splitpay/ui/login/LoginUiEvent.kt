// LoginUiEvent.kt
package com.example.splitpay.ui.login

sealed class LoginUiEvent {
    object NavigateToHome : LoginUiEvent()
    object NavigateToBack : LoginUiEvent()}
