package com.example.splitpay.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.absoluteValue

// --- Data Models for Internal State ---
enum class SplitType(val label: String) {
    EQUALLY("Equally"),
    UNEQUALLY("Unequally (by amount)"),
    PERCENTAGES("By Percentages"),
    SHARES("By Shares"),
    ADJUSTMENTS("By Adjustments")
}

data class Participant(
    val uid: String,
    val name: String,
    val isChecked: Boolean = true,
    val splitValue: String = "0.00", // Value used for unequal split (amount, percentage, or share count)
    val owesAmount: Double = 0.0 // The calculated amount the participant owes
)

data class Payer(
    val uid: String,
    val name: String,
    val amount: String = "0.00", // Amount paid by this user (String for input)
    val isChecked: Boolean = false // Used in Payer selection UI
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
    val isSplitEditorVisible: Boolean = false,
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

    private val _availableGroups = MutableStateFlow<List<Group>>(emptyList())
    val availableGroups: StateFlow<List<Group>> = _availableGroups

    private val _allUsers = MutableStateFlow<List<Payer>>(emptyList())
    val allUsers: StateFlow<List<Payer>> = _allUsers

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            groupsRepository.getGroups().collect { groups ->
                _availableGroups.value = groups
            }

            val currentUser = userRepository.getCurrentUser()
            val currentUserName = currentUser?.displayName ?: "You"
            val currentUid = currentUser?.uid ?: "current_user"

            // Mock all potential payers/participants (should be real friends list)
            val mockUsers = listOf(
                Payer(currentUid, currentUserName, isChecked = true),
                Payer("uid_A", "Person A"),
                Payer("uid_B", "Person B")
            )
            _allUsers.value = mockUsers

            if (currentUser != null) {
                // Initialize state with only the current user as the payer and participant
                _uiState.update {
                    val initialParticipants = listOf(Participant(currentUid, currentUserName, splitValue = (100.0).toString()))
                    it.copy(
                        paidByUsers = listOf(Payer(currentUid, currentUserName, amount = "0.00", isChecked = true)),
                        participants = calculateSplit(
                            amount = it.amount.toDoubleOrNull() ?: 0.0,
                            participants = initialParticipants,
                            splitType = it.splitType
                        )
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
        val cleanAmount = newAmount.replace(Regex("[^0-9.]"), "")
        val decimalIndex = cleanAmount.indexOf('.')

        val validAmount = if (decimalIndex >= 0 && cleanAmount.length - decimalIndex > 3) {
            cleanAmount.substring(0, decimalIndex + 3)
        } else {
            cleanAmount
        }

        _uiState.update { it.copy(amount = validAmount) }
        recalculateSplit(validAmount.toDoubleOrNull() ?: 0.0, uiState.value.splitType)
    }

    // --- Core Logic: Split Recalculation ---

    // Function to calculate the distribution based on split type
    private fun calculateSplit(amount: Double, participants: List<Participant>, splitType: SplitType): List<Participant> {
        val activeParticipants = participants.filter { it.isChecked }
        if (activeParticipants.isEmpty() || amount <= 0) return participants

        // Helper for rounding to 2 decimal places
        fun roundToCents(value: Double) = (value * 100.0).roundToInt() / 100.0

        return when (splitType) {
            SplitType.EQUALLY -> {
                val share = amount / activeParticipants.size
                participants.map { p ->
                    if (p.isChecked) p.copy(owesAmount = roundToCents(share)) else p.copy(owesAmount = 0.0)
                }
            }

            SplitType.UNEQUALLY, SplitType.ADJUSTMENTS -> {
                // UNEQUALLY/ADJUSTMENTS: Assumes splitValue holds the exact amount owed.
                participants.map { p ->
                    val owes = p.splitValue.toDoubleOrNull() ?: 0.0
                    p.copy(owesAmount = roundToCents(owes))
                }
            }

            SplitType.PERCENTAGES -> {
                val totalPercent = activeParticipants.sumOf { it.splitValue.toDoubleOrNull() ?: 0.0 }
                if (totalPercent == 0.0) return participants.map { it.copy(owesAmount = 0.0) }

                participants.map { p ->
                    if (p.isChecked) {
                        val percent = p.splitValue.toDoubleOrNull() ?: 0.0
                        val owes = (percent / 100.0) * amount
                        p.copy(owesAmount = roundToCents(owes))
                    } else {
                        p.copy(owesAmount = 0.0)
                    }
                }
            }

            SplitType.SHARES -> {
                val totalShares = activeParticipants.sumOf { it.splitValue.toDoubleOrNull() ?: 0.0 }
                if (totalShares == 0.0) return participants.map { it.copy(owesAmount = 0.0) }

                val perShareCost = amount / totalShares

                participants.map { p ->
                    if (p.isChecked) {
                        val shares = p.splitValue.toDoubleOrNull() ?: 0.0
                        val owes = shares * perShareCost
                        p.copy(owesAmount = roundToCents(owes))
                    } else {
                        p.copy(owesAmount = 0.0)
                    }
                }
            }
        }
    }

    private fun recalculateSplit(newAmount: Double, newSplitType: SplitType) {
        _uiState.update { state ->
            val updatedParticipants = calculateSplit(
                amount = newAmount,
                participants = state.participants,
                splitType = newSplitType
            )
            state.copy(participants = updatedParticipants)
        }
    }

    fun onParticipantSplitValueChanged(uid: String, newValue: String) {
        _uiState.update { state ->
            val updatedParticipants = state.participants.map { p ->
                if (p.uid == uid) p.copy(splitValue = newValue) else p
            }
            // Recalculate split immediately after input changes
            val calculatedParticipants = calculateSplit(
                amount = state.amount.toDoubleOrNull() ?: 0.0,
                participants = updatedParticipants,
                splitType = state.splitType
            )
            state.copy(participants = calculatedParticipants)
        }
    }

    // --- Selectors ---

    fun onSelectGroup(group: Group) {
        // When a group is selected, update the group and participants (default: all members)
        val participantsFromGroup = _allUsers.value.filter { group.members.contains(it.uid) }.map { payer ->
            // Use Payer object properties to create Participant
            Participant(
                uid = payer.uid,
                name = payer.name,
                isChecked = true,
                // Default split value: 1.0 for share/unequal, 100/N for percentage
                splitValue = if (uiState.value.splitType == SplitType.PERCENTAGES) (100.0 / group.members.size).toString() else "1.0"
            )
        }

        _uiState.update {
            // FIX APPLIED HERE: Only calculate and assign once.
            val calculatedParticipants = calculateSplit(
                amount = it.amount.toDoubleOrNull() ?: 0.0,
                participants = participantsFromGroup,
                splitType = it.splitType
            )
            it.copy(
                selectedGroup = group,
                isGroupSelectorVisible = false,
                participants = calculatedParticipants
            )
        }
    }

    fun onPayerSelectionChanged(updatedPayerList: List<Payer>) {
        _uiState.update {
            it.copy(paidByUsers = updatedPayerList.filter { p -> p.isChecked })
        }
    }

    fun onPayerAmountChanged(uid: String, newAmount: String) {
        _uiState.update { state ->
            val updatedPayers = state.paidByUsers.map { payer ->
                if (payer.uid == uid) payer.copy(amount = newAmount) else payer
            }
            state.copy(paidByUsers = updatedPayers)
        }
    }

    fun finalizePayerSelection() {
        _uiState.update { it.copy(isPayerSelectorVisible = false) }
    }

    fun onSelectSplitType(type: SplitType) {
        _uiState.update {
            // When changing split type, reset splitValue to a reasonable default (e.g., 1 for shares, or 100/N for %)
            val newParticipants = it.participants.map { p ->
                if (type == SplitType.EQUALLY) {
                    p.copy(splitValue = "0.00")
                } else if (type == SplitType.PERCENTAGES) {
                    val defaultPercent = (100.0 / it.participants.size)
                    p.copy(splitValue = "%.2f".format(defaultPercent))
                } else { // UNEQUALLY, SHARES, ADJUSTMENTS
                    p.copy(splitValue = "1.0")
                }
            }
            it.copy(
                splitType = type,
                participants = newParticipants
            )
        }
        recalculateSplit(uiState.value.amount.toDoubleOrNull() ?: 0.0, type)
    }

    fun finalizeSplitTypeSelection() {
        _uiState.update { it.copy(isSplitEditorVisible = false) }
    }

    // --- UI Visibility Handlers ---

    fun showGroupSelector(isVisible: Boolean) {
        _uiState.update { it.copy(isGroupSelectorVisible = isVisible) }
    }

    fun showPayerSelector(isVisible: Boolean) {
        _uiState.update { it.copy(isPayerSelectorVisible = isVisible) }
    }

    fun showSplitEditor(isVisible: Boolean) {
        _uiState.update { it.copy(isSplitEditorVisible = isVisible) }
    }

    fun onSaveExpenseClick() {
        val totalAmount = uiState.value.amount.toDoubleOrNull() ?: 0.0
        val paidAmount = uiState.value.paidByUsers.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val allocatedSplit = uiState.value.participants.sumOf { it.owesAmount }

        if (uiState.value.description.isBlank() || totalAmount <= 0.0) {
            _uiState.update { it.copy(error = "Please fill in description and amount.") }
            return
        }

        if (uiState.value.paidByUsers.isEmpty()) {
            _uiState.update { it.copy(error = "Please select a payer.") }
            return
        }

        // 1. Validate Paid Amount matches Total Amount (if multi-payer)
        if (uiState.value.paidByUsers.size > 1 && paidAmount != totalAmount) {
            _uiState.update { it.copy(error = "Total paid amount must equal expense amount.") }
            return
        }

        // 2. Validate Split Allocation matches Total Amount
        if (allocatedSplit.roundToInt() != totalAmount.roundToInt()) {
            _uiState.update { it.copy(error = "Split allocation (RM %.2f) must match total expense.".format(allocatedSplit)) }
            return
        }

        // 3. Perform mock save
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
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
