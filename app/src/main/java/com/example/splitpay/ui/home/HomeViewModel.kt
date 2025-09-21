package com.example.splitpay.ui.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update

class HomeViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    fun onItemSelected(index: Int) {
        _uiState.update { it.copy(selectedItemIndex = index) }
    }

    fun selectedRoute(): String {
        return _uiState.value.items[_uiState.value.selectedItemIndex].route
    }
}
