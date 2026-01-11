package com.example.splitpay.ui.expense.addExpense

sealed interface AddExpenseUiEvent {
    object NavigateBack : AddExpenseUiEvent
    data class SaveSuccess(val isGroupDetail: Boolean) : AddExpenseUiEvent
    data class ShowErrorDialog(val title: String, val message: String) : AddExpenseUiEvent // <-- ADD THIS
}