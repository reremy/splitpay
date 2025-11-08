package com.example.splitpay.ui.expense

import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.Participant
import com.example.splitpay.data.model.Payer

// --- UI State Data Class ---
data class AddExpenseUiState(
    val description: String = "",
    val amount: String = "",
    val currency: String = "MYR",
    val paidByUsers: List<Payer> = emptyList(), // Uses local Payer class
    val splitType: SplitType = SplitType.EQUALLY,
    val selectedGroup: Group? = null,
    val participants: List<Participant> = emptyList(), // Uses local Participant class
    val isLoading: Boolean = false, // General loading state (e.g., saving)
    val isInitiallyLoading: Boolean = true, // Separate state for initial data load
    val error: String? = null,
    val isGroupSelectorVisible: Boolean = false,
    val isPayerSelectorVisible: Boolean = false,
    val isSplitEditorVisible: Boolean = false,
    val date: Long = System.currentTimeMillis(),
    val memo: String = "",
    val imageUris: List<String> = emptyList(),
    val isUploadingImages: Boolean = false,
    val initialGroupId: String? = null, // Group ID passed via navigation (if any)
    val currentGroupId: String? = null, // Currently selected group ID (can be null for non-group)
    val isDatePickerDialogVisible: Boolean = false,
    val isMemoDialogVisible: Boolean = false,
    val isEditMode: Boolean = false // True when editing an existing expense
)