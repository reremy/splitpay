package com.example.splitpay.ui.expense

import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.Participant
import com.example.splitpay.data.model.Payer

data class AddExpenseUiState(
    val description: String = "",
    val amount: String = "", // Use string for input field to handle currency format
    val currency: String = "MYR",
    val paidByUsers: List<Payer> = emptyList(), // Supports multiple payers
    val splitType: SplitType = SplitType.EQUALLY,
    val selectedGroup: Group? = null,
    val participants: List<Participant> = emptyList(),

    val isLoading: Boolean = false,
    val error: String? = null,

    // Flags for modular UIs (Dialogs, BottomSheets)
    val isGroupSelectorVisible: Boolean = false,
    val isPayerSelectorVisible: Boolean = false,
    val isSplitEditorVisible: Boolean = false,

    // --- NEW FIELDS FOR NAVIGATION & DATE/MEMO ---
    val date: Long = System.currentTimeMillis(),
    val memo: String = "",
    val imageUris: List<String> = emptyList(), // Store local URIs
    val isUploadingImages: Boolean = false,

    // State for navigation logic
    val initialGroupId: String? = null,
    val currentGroupId: String? = null // `null` means "Non-group"
)