package com.example.splitpay.ui.expense

sealed interface AddExpenseUiEvent {
    object NavigateBack : AddExpenseUiEvent
    data class SaveSuccess(val isGroupDetail: Boolean) : AddExpenseUiEvent
}