package com.example.splitpay.ui.welcome

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch


class WelcomeViewModel : ViewModel() {
    private val _uiEvent = MutableSharedFlow<WelcomeUiEvent>()
    val uiEvent: SharedFlow<WelcomeUiEvent> = _uiEvent

    fun onSignUpClick() {
        viewModelScope.launch {
            _uiEvent.emit(WelcomeUiEvent.NavigateToSignUp)
        }
    }

    fun onLogInClick() {
        viewModelScope.launch {
            _uiEvent.emit(WelcomeUiEvent.NavigateToLogIn)
        }
    }
}

sealed interface WelcomeUiEvent {
    object NavigateToSignUp : WelcomeUiEvent
    object NavigateToLogIn : WelcomeUiEvent
}