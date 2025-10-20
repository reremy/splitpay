package com.example.splitpay.ui.expense

sealed interface AddExpenseUiEvent {
    object NavigateBack : AddExpenseUiEvent
    // --- UPDATED THIS LINE ---
    // It's now a data class that holds the navigation logic
    data class SaveSuccess(val isGroupDetail: Boolean) : AddExpenseUiEvent
}