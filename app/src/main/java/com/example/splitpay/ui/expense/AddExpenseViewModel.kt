package com.example.splitpay.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User // Assuming we have a User model
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.DecimalFormat

// --- Data Models for Internal State ---
enum class SplitType(val label: String) {
    EQUALLY("Equally"),
    UNEQUALLY("Unequally"),
    PERCENTAGES("By Percentages"),
    SHARES("By Shares"),
    ADJUSTMENTS("By Adjustments")
}

data class Participant(
    val uid: String,
    val name: String,
    val isChecked: Boolean = true,
    val splitValue: Double = 0.0 // Could be amount, percentage, or share count
)

data class Payer(
    val uid: String,
    val amount: Double
)

// --- UI State ---
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
    val isSplitSelectorVisible: Boolean = false,
)

// --- UI Events ---
sealed interface AddExpenseUiEvent {
    object NavigateBack : AddExpenseUiEvent
    object SaveSuccess : AddExpenseUiEvent
}


class AddExpenseViewModel(
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<AddExpenseUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    // Placeholder for all groups available to the user
    private val _availableGroups = MutableStateFlow<List<Group>>(emptyList())
    val availableGroups: StateFlow<List<Group>> = _availableGroups

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // 1. Load groups
            groupsRepository.getGroups().collect { groups ->
                _availableGroups.value = groups
            }

            // 2. Set default participants (just the current user for now)
            val currentUser = userRepository.getCurrentUser()
            if (currentUser != null) {
                val initialPayer = Payer(currentUser.uid, 0.0)
                val initialParticipant = Participant(currentUser.uid, currentUser.displayName ?: "You")

                _uiState.update {
                    it.copy(
                        paidByUsers = listOf(initialPayer),
                        participants = listOf(initialParticipant)
                    )
                }
            }
        }
    }

    // --- User Input Handlers ---

    fun onDescriptionChange(newDescription: String) {
        if (newDescription.length <= 100) {
            _uiState.update { it.copy(description = newDescription) }
        }
    }

    fun onAmountChange(newAmount: String) {
        // Validate as numeric with max 2 decimals
        val cleanAmount = newAmount.replace(Regex("[^0-9.]"), "")
        val decimalIndex = cleanAmount.indexOf('.')

        val validAmount = if (decimalIndex >= 0 && cleanAmount.length - decimalIndex > 3) {
            cleanAmount.substring(0, decimalIndex + 3)
        } else {
            cleanAmount
        }

        _uiState.update { it.copy(amount = validAmount) }
    }

    // --- Selectors ---

    fun onSelectGroup(group: Group) {
        // When a group is selected, update the group and participants (default: all members)
        val participantsFromGroup = group.members.map { uid ->
            // In a real app, fetch user details using UID here. For now, mock name.
            Participant(uid, "User ${uid.take(4)}")
        }

        _uiState.update {
            it.copy(
                selectedGroup = group,
                isGroupSelectorVisible = false,
                participants = participantsFromGroup
            )
        }
    }

    fun onSelectPayer(payers: List<Payer>) {
        // Update the paidByUsers list and hide the selector
        _uiState.update {
            it.copy(
                paidByUsers = payers,
                isPayerSelectorVisible = false
            )
        }
    }

    fun onSelectSplitType(type: SplitType) {
        // Update the split type and hide the selector
        _uiState.update {
            it.copy(
                splitType = type,
                isSplitSelectorVisible = false
            )
        }
    }

    // --- UI Visibility Handlers ---

    fun showGroupSelector(isVisible: Boolean) {
        _uiState.update { it.copy(isGroupSelectorVisible = isVisible) }
    }

    fun showPayerSelector(isVisible: Boolean) {
        _uiState.update { it.copy(isPayerSelectorVisible = isVisible) }
    }

    fun showSplitSelector(isVisible: Boolean) {
        _uiState.update { it.copy(isSplitSelectorVisible = isVisible) }
    }

    fun onSaveExpenseClick() {
        // 1. Basic validation check
        if (uiState.value.description.isBlank() || uiState.value.amount.toDoubleOrNull() == 0.0) {
            _uiState.update { it.copy(error = "Please fill in description and amount.") }
            return
        }

        // 2. Perform mock save (actual implementation deferred)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            // In a real app: groupsRepository.addExpense(expenseModel)

            // Simulate success
            kotlinx.coroutines.delay(1000)

            _uiEvent.emit(AddExpenseUiEvent.SaveSuccess)
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.emit(AddExpenseUiEvent.NavigateBack)
        }
    }
}
