package com.example.splitpay.ui.activityDetail

import com.example.splitpay.data.model.Activity
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.User

data class ActivityDetailUiState(
    val isLoading: Boolean = true,
    val activity: Activity? = null,
    val expense: Expense? = null,
    val actorUser: User? = null,
    val involvedUsers: List<User> = emptyList(),
    val payers: Map<String, User> = emptyMap(), // uid -> User
    val participants: Map<String, User> = emptyMap(), // uid -> User
    val currentUserId: String = "",
    val error: String? = null,
    val showDeleteDialog: Boolean = false
)
