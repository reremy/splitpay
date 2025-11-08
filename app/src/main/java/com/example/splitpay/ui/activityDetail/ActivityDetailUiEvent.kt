package com.example.splitpay.ui.activityDetail

sealed interface ActivityDetailUiEvent {
    object NavigateBack : ActivityDetailUiEvent
    data class NavigateToEditExpense(val expenseId: String, val groupId: String?) : ActivityDetailUiEvent
    data class ShowError(val message: String) : ActivityDetailUiEvent
    object DeleteSuccess : ActivityDetailUiEvent
}
