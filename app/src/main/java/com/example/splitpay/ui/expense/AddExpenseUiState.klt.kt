package com.example.splitpay.ui.expense

import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.Participant
import com.example.splitpay.data.model.Payer

data class AddExpenseUiState(
    val description: String = "",
    val amount: String = "",
    val currency: String = "MYR",
    val paidByUsers: List<Payer> = emptyList(), // Uses local Payer class
    val splitType: SplitType = SplitType.EQUALLY,
    val selectedGroup: Group? = null,
    val participants: List<Participant> = emptyList(), // Uses local Participant class
    val isLoading: Boolean = false,
    val error: String? = null,
    val isGroupSelectorVisible: Boolean = false,
    val isPayerSelectorVisible: Boolean = false,
    val isSplitEditorVisible: Boolean = false,
    val date: Long = System.currentTimeMillis(),
    val memo: String = "",
    val imageUris: List<String> = emptyList(),
    val isUploadingImages: Boolean = false,
    val initialGroupId: String? = null,
    val currentGroupId: String? = null
)