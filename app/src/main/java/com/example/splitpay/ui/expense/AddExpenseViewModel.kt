package com.example.splitpay.ui.expense

import android.util.Log
import androidx.lifecycle.SavedStateHandle // NEW IMPORT
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.Participant
import com.example.splitpay.data.model.Payer
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.filter
import kotlin.collections.firstOrNull
import kotlin.collections.map
import kotlin.math.roundToInt
import kotlin.math.absoluteValue

class AddExpenseViewModel(
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val savedStateHandle: SavedStateHandle
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
        // Read the prefilled groupId from the navigation arguments
        val prefilledGroupId: String? = savedStateHandle["groupId"]
        Log.d("AddExpenseDebug", "ViewModel init: prefilledGroupId = $prefilledGroupId") // <-- ADD THIS

        _uiState.update {
            it.copy(
                initialGroupId = prefilledGroupId,
                currentGroupId = prefilledGroupId
                // We'll set selectedGroup later in loadInitialData
            )
        }
        Log.d("AddExpenseDebug", "ViewModel init: initialGroupId = ${_uiState.value.initialGroupId}, currentGroupId = ${_uiState.value.currentGroupId}") // <-- ADD THIS

        loadInitialData(prefilledGroupId)
    }

    private fun loadInitialData(prefilledGroupId: String?) {
        viewModelScope.launch {
            // --- THIS LOGIC IS REPLACED ---
            // We no longer use mock data
            // val currentUser = userRepository.getCurrentUser() ... (etc)

            // 1. Fetch all available groups
            groupsRepository.getGroups().collect { groups ->
                _availableGroups.value = groups

                // 2. If a groupId was passed, find and select it
                if (prefilledGroupId != null) {
                    val group = groups.find { it.id == prefilledGroupId }
                    if (group != null) {
                        // This will trigger onSelectGroup to populate participants
                        onSelectGroup(group)
                    }
                }
            }

            // TODO: Replace this with a real fetch from UserRepository
            // For now, we will keep the mock users for selection
            val currentUser = userRepository.getCurrentUser()
            val currentUserName = currentUser?.displayName ?: "You"
            val currentUid = currentUser?.uid ?: "current_user"

            val mockUsers = listOf(
                Payer(currentUid, currentUserName, isChecked = true),
                Payer("uid_A", "Person A"),
                Payer("uid_B", "Person B")
            )
            _allUsers.value = mockUsers

            // If no group was prefilled, set default participants (current user)
            if (prefilledGroupId == null && uiState.value.participants.isEmpty()) {
                _uiState.update {
                    val initialParticipants = listOf(
                        Participant(
                            currentUid,
                            currentUserName,
                            splitValue = "1.0"
                        )
                    )
                    it.copy(
                        paidByUsers = listOf(
                            Payer(
                                currentUid,
                                currentUserName,
                                amount = "0.00",
                                isChecked = true
                            )
                        ),
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
        if (activeParticipants.isEmpty() || amount <= 0) return participants.map { it.copy(owesAmount = 0.0) } // Reset all if no one is checked

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
                    val owes = if (p.isChecked) p.splitValue.toDoubleOrNull() ?: 0.0 else 0.0
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

    // UPDATED to handle 'null' for "Non-group"
    fun onSelectGroup(group: Group?) {
        // When a group is selected, update the group and participants (default: all members)
        // TODO: Replace _allUsers.value with a real fetch of group members + friends
        val participantsFromGroup = if (group != null) {
            _allUsers.value.filter { group.members.contains(it.uid) }.map { payer ->
                // Use Payer object properties to create Participant
                Participant(
                    uid = payer.uid,
                    name = payer.name,
                    isChecked = true,
                    // Default split value: 1.0 for share/unequal, 100/N for percentage
                    splitValue = if (uiState.value.splitType == SplitType.PERCENTAGES) (100.0 / group.members.size).toString() else "1.0"
                )
            }
        } else {
            // "Non-group" selected. Default to just the current user.
            // You can expand this logic to show a friend picker later.
            val currentUser = _allUsers.value.firstOrNull() // Assuming current user is first
            if (currentUser != null) {
                listOf(
                    Participant(
                        currentUser.uid,
                        currentUser.name,
                        isChecked = true,
                        splitValue = "1.0"
                    )
                )
            } else {
                emptyList()
            }
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
                currentGroupId = group?.id, // This is now 'null' for non-group
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
            val activeCount = it.participants.count { p -> p.isChecked }
            val newParticipants = it.participants.map { p ->
                if (type == SplitType.EQUALLY) {
                    p.copy(splitValue = "0.00")
                } else if (type == SplitType.PERCENTAGES) {
                    val defaultPercent = if (activeCount > 0) (100.0 / activeCount) else 0.0
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
        val state = uiState.value // Get a snapshot of the state
        val totalAmount = state.amount.toDoubleOrNull() ?: 0.0
        val paidAmount = state.paidByUsers.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        val allocatedSplit = state.participants.filter { it.isChecked }.sumOf { it.owesAmount }

        if (state.description.isBlank() || totalAmount <= 0.0) {
            _uiState.update { it.copy(error = "Please fill in description and amount.") }
            return
        }

        if (state.paidByUsers.isEmpty()) {
            _uiState.update { it.copy(error = "Please select a payer.") }
            return
        }

        // 1. Validate Paid Amount matches Total Amount (if multi-payer)
        val paidByTotal = if (state.paidByUsers.size == 1) {
            totalAmount // If single payer, they paid the total
        } else {
            state.paidByUsers.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        }

        if (paidByTotal.roundToInt() != totalAmount.roundToInt()) {
            _uiState.update { it.copy(error = "Total paid amount (RM %.2f) must equal expense amount (RM %.2f).".format(paidByTotal, totalAmount)) }
            return
        }

        // 2. Validate Split Allocation matches Total Amount
        if (allocatedSplit.roundToInt() != totalAmount.roundToInt()) {
            _uiState.update { it.copy(error = "Split allocation (RM %.2f) must match total expense.".format(allocatedSplit)) }
            return
        }

        // TODO:
        // 1. Upload images from state.imageUris to Firebase Storage
        // 2. Get download URLs
        // 3. Create Expense object with all data
        // 4. Save to ExpenseRepository

        // --- MOCK SAVE with NAVIGATION LOGIC ---
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            kotlinx.coroutines.delay(1000) // Simulating network call

            // Determine which navigation event to send
            val isGroupDetailNav = (state.initialGroupId != null) &&
                    (state.initialGroupId == state.currentGroupId)

            _uiEvent.emit(AddExpenseUiEvent.SaveSuccess(isGroupDetailNav))
            _uiState.update { it.copy(isLoading = false) }
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.emit(AddExpenseUiEvent.NavigateBack)
        }
    }
}