package com.example.splitpay.ui.expense

import android.util.Log
import androidx.lifecycle.SavedStateHandle // NEW IMPORT
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.Participant // Keep this
import com.example.splitpay.data.model.Payer // Keep this
import com.example.splitpay.data.model.Expense // NEW IMPORT
import com.example.splitpay.data.model.ExpensePayer // NEW IMPORT
import com.example.splitpay.data.model.ExpenseParticipant // NEW IMPORT
import com.example.splitpay.data.repository.ExpenseRepository // NEW IMPORT
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logE // Import logger if needed
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import kotlin.math.absoluteValue


class AddExpenseViewModel(
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val userRepository: UserRepository = UserRepository(),
    // --- NEW: Add ExpenseRepository ---
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    // --- StateFlows and SharedFlow remain the same ---
    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState
    private val _uiEvent = MutableSharedFlow<AddExpenseUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()
    private val _availableGroups = MutableStateFlow<List<Group>>(emptyList())
    val availableGroups: StateFlow<List<Group>> = _availableGroups
    private val _relevantUsersForSelection = MutableStateFlow<List<Payer>>(emptyList()) // Will hold friends + group members
    val relevantUsersForSelection: StateFlow<List<Payer>> = _relevantUsersForSelection

    init {
        val prefilledGroupId: String? = savedStateHandle["groupId"]
        Log.d("AddExpenseDebug", "ViewModel init: prefilledGroupId = $prefilledGroupId")

        _uiState.update {
            it.copy(
                initialGroupId = prefilledGroupId,
                currentGroupId = prefilledGroupId
                // We'll set selectedGroup later in loadInitialData
            )
        }
        Log.d("AddExpenseDebug", "ViewModel init: initialGroupId = ${_uiState.value.initialGroupId}, currentGroupId = ${_uiState.value.currentGroupId}")

        loadInitialData(prefilledGroupId)
    }

    private fun loadInitialData(prefilledGroupId: String?) {
        viewModelScope.launch {
            // --- Fetch current user info ONCE ---
            val currentUser = userRepository.getCurrentUser()
            val currentUserName = currentUser?.displayName?.takeIf { it.isNotBlank() } ?: currentUser?.email ?: "You" // Use email as fallback
            val currentUid = currentUser?.uid ?: "current_user_placeholder" // Should not happen if logged in
            val currentUserPayer = Payer(currentUid, currentUserName, isChecked = true) // Default state

            // --- Mock friends list (Replace with actual fetch later) ---
            val mockFriends = listOf(
                Payer("uid_A", "Person A"),
                Payer("uid_B", "Person B")
            )

            // --- Load relevant users based on prefilled group ---
            if (prefilledGroupId != null) {
                // Fetch the specific group (assuming groupsRepository.getGroupById exists)
                val group = groupsRepository.getGroupById(prefilledGroupId) // Need to make this suspend or change repo
                if (group != null) {
                    // TODO: Fetch actual member details (names) based on group.members UIDs
                    val groupMembersAsPayers = group.members.mapNotNull { memberUid ->
                        // Find user details (mock for now)
                        (listOf(currentUserPayer) + mockFriends).find { it.uid == memberUid }
                    }
                    _relevantUsersForSelection.value = groupMembersAsPayers
                    // Also call onSelectGroup to set initial participants
                    handleGroupSelection(group, initialLoad = true)
                } else {
                    // Group not found, default to user + friends
                    _relevantUsersForSelection.value = listOf(currentUserPayer) + mockFriends
                    setDefaultParticipantsAndPayers(currentUserPayer) // Set defaults if group load fails
                }
            } else {
                // No prefilled group, default to user + friends
                _relevantUsersForSelection.value = listOf(currentUserPayer) + mockFriends
                setDefaultParticipantsAndPayers(currentUserPayer) // Set defaults for non-group start
            }

            // Fetch available groups for the dropdown selector (can run concurrently)
            groupsRepository.getGroups().collect { groups ->
                _availableGroups.value = groups
            }
        }
    }

    //Helper to set default payer/participant when no group is pre-selected
    private fun setDefaultParticipantsAndPayers(currentUserPayer: Payer) {
        if (uiState.value.participants.isEmpty()) { // Only if not already set
            _uiState.update { currentState ->
                val initialParticipants = listOf(Participant(currentUserPayer.uid, currentUserPayer.name, splitValue = "1.0"))
                currentState.copy(
                    paidByUsers = listOf(currentUserPayer.copy(amount = "0.00", isChecked = true)),
                    participants = calculateSplit(
                        amount = currentState.amount.toDoubleOrNull() ?: 0.0,
                        participants = initialParticipants,
                        splitType = currentState.splitType
                    )
                )
            }
        }
    }

    // --- Modified onSelectGroup ---
    fun onSelectGroup(group: Group?, initialLoad: Boolean = false) {
        // --- Fetch relevant users based on selection ---
        val currentUser = _relevantUsersForSelection.value.firstOrNull() // Assume current user is loaded
        // --- Mock friends list (Replace with actual fetch later) ---
        val mockFriends = listOf(
            Payer("uid_A", "Person A"),
            Payer("uid_B", "Person B")
        )

        val newRelevantUsers = if (group != null) {
            // TODO: Fetch actual member details based on group.members UIDs
            group.members.mapNotNull { memberUid ->
                (_relevantUsersForSelection.value + mockFriends).find { it.uid == memberUid } // Find from current/friends list
            }.distinctBy { it.uid } // Ensure uniqueness
        } else {
            // Non-group: User + Friends
            (currentUser?.let { listOf(it) } ?: emptyList()) + mockFriends
        }
        _relevantUsersForSelection.value = newRelevantUsers

        // --- Determine participants based on the new relevant users ---
        val participantsList = newRelevantUsers.map { payer ->
            Participant(
                uid = payer.uid,
                name = payer.name,
                isChecked = true, // Default all participants to checked
                splitValue = if (uiState.value.splitType == SplitType.PERCENTAGES && newRelevantUsers.isNotEmpty()) {
                    "%.2f".format(100.0 / newRelevantUsers.size)
                } else "1.0"
            )
        }

        // --- Update state ---
        _uiState.update { currentState ->
            val calculatedParticipants = calculateSplit(
                amount = currentState.amount.toDoubleOrNull() ?: 0.0,
                participants = participantsList,
                splitType = currentState.splitType
            )
            // Reset payer to current user when group changes
            val defaultPayer = currentUser?.copy(amount = "0.00", isChecked = true)?.let { listOf(it) } ?: emptyList()

            currentState.copy(
                selectedGroup = group,
                currentGroupId = group?.id,
                isGroupSelectorVisible = if (initialLoad) currentState.isGroupSelectorVisible else false,
                participants = calculatedParticipants, // Update participants list
                paidByUsers = defaultPayer, // Reset payer
                error = null
            )
        }
    }


    // --- User Input Handlers ---
    fun onDescriptionChange(newDescription: String) {
        if (newDescription.length <= 100) { // Limit description length
            _uiState.update { it.copy(description = newDescription, error = null) } // Clear error on change
        } else {
            _uiState.update { it.copy(error = "Description cannot exceed 100 characters.") }
        }
    }

    fun onAmountChange(newAmount: String) {
        // Allow only numbers and one decimal point, max two decimal places
        val cleanAmount = newAmount.filter { it.isDigit() || it == '.' }
        val decimalIndex = cleanAmount.indexOf('.')
        val validAmount = if (decimalIndex != -1) {
            val integerPart = cleanAmount.substringBefore('.')
            val decimalPart = cleanAmount.substringAfter('.').filter { it.isDigit() }.take(2)
            "$integerPart.$decimalPart"
        } else {
            cleanAmount
        }

        _uiState.update { it.copy(amount = validAmount, error = null) } // Clear error on change
        recalculateSplit(validAmount.toDoubleOrNull() ?: 0.0, uiState.value.splitType)
    }

    // --- NEW Handlers for Date, Memo, Images ---
    fun onDateSelected(newDateMillis: Long) {
        _uiState.update { it.copy(date = newDateMillis) }
    }

    fun onMemoChanged(newMemo: String) {
        _uiState.update { it.copy(memo = newMemo) }
    }

    fun onImageAdded(uriString: String) {
        _uiState.update { it.copy(imageUris = it.imageUris + uriString) }
        // TODO: Show image previews in the UI
    }

    fun onImageRemoved(uriString: String) {
        _uiState.update { it.copy(imageUris = it.imageUris - uriString) }
    }


    // --- Core Logic: Split Recalculation ---
    private fun calculateSplit(amount: Double, participants: List<Participant>, splitType: SplitType): List<Participant> {
        val activeParticipants = participants.filter { it.isChecked }
        if (activeParticipants.isEmpty() || amount <= 0) return participants.map { it.copy(owesAmount = 0.0) } // Reset all if no one is checked or amount is zero

        // Helper for rounding to 2 decimal places to avoid floating point issues
        fun roundToCents(value: Double) = (value * 100.0).roundToInt() / 100.0

        val calculatedParticipants = when (splitType) {
            SplitType.EQUALLY -> {
                val share = roundToCents(amount / activeParticipants.size)
                participants.map { p ->
                    if (p.isChecked) p.copy(owesAmount = share) else p.copy(owesAmount = 0.0)
                }
            }
            SplitType.UNEQUALLY, SplitType.ADJUSTMENTS -> {
                participants.map { p ->
                    val owes = if (p.isChecked) roundToCents(p.splitValue.toDoubleOrNull() ?: 0.0) else 0.0
                    p.copy(owesAmount = owes)
                }
            }
            SplitType.PERCENTAGES -> {
                val totalPercent = activeParticipants.sumOf { it.splitValue.toDoubleOrNull() ?: 0.0 }
                if (totalPercent == 0.0) return participants.map { it.copy(owesAmount = 0.0) } // Avoid division by zero

                participants.map { p ->
                    if (p.isChecked) {
                        val percent = p.splitValue.toDoubleOrNull() ?: 0.0
                        // Calculate share based on percentage of total amount
                        val owes = roundToCents((percent / 100.0) * amount)
                        p.copy(owesAmount = owes)
                    } else {
                        p.copy(owesAmount = 0.0)
                    }
                }
            }
            SplitType.SHARES -> {
                val totalShares = activeParticipants.sumOf { it.splitValue.toDoubleOrNull() ?: 0.0 }
                if (totalShares == 0.0) return participants.map { it.copy(owesAmount = 0.0) } // Avoid division by zero

                // Calculate cost per share carefully to minimize rounding errors later
                val costPerShare = amount / totalShares

                participants.map { p ->
                    if (p.isChecked) {
                        val shares = p.splitValue.toDoubleOrNull() ?: 0.0
                        val owes = roundToCents(shares * costPerShare)
                        p.copy(owesAmount = owes)
                    } else {
                        p.copy(owesAmount = 0.0)
                    }
                }
            }
        }

        // --- Adjustment for rounding errors ---
        // Calculate the difference between the total amount and the sum of calculated shares
        val currentTotalOwed = calculatedParticipants.filter { it.isChecked }.sumOf { it.owesAmount }
        val difference = roundToCents(amount - currentTotalOwed)

        // If there's a small difference (usually +/- 0.01 due to rounding)
        // Add/subtract it from the first active participant to make the total match exactly
        if (difference != 0.0 && activeParticipants.isNotEmpty()) {
            val firstActiveParticipantIndex = calculatedParticipants.indexOfFirst { it.isChecked }
            if (firstActiveParticipantIndex != -1) {
                val adjustedParticipant = calculatedParticipants[firstActiveParticipantIndex]
                val adjustedList = calculatedParticipants.toMutableList()
                adjustedList[firstActiveParticipantIndex] = adjustedParticipant.copy(
                    owesAmount = roundToCents(adjustedParticipant.owesAmount + difference)
                )
                return adjustedList.toList() // Return the adjusted list
            }
        }

        return calculatedParticipants // Return the original list if no adjustment needed
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

    fun onParticipantCheckedChange(uid: String, isChecked: Boolean) {
        _uiState.update { state ->
            val updatedParticipants = state.participants.map {
                if (it.uid == uid) it.copy(isChecked = isChecked) else it
            }
            // Recalculate immediately after checking/unchecking
            val recalculatedParticipants = calculateSplit(
                amount = state.amount.toDoubleOrNull() ?: 0.0,
                participants = updatedParticipants,
                splitType = state.splitType
            )
            state.copy(participants = recalculatedParticipants, error = null) // Clear error on change
        }
    }


    fun onParticipantSplitValueChanged(uid: String, newValue: String) {
        // Allow only numbers and one decimal point for split values too
        val cleanValue = newValue.filter { it.isDigit() || it == '.' }
        val decimalIndex = cleanValue.indexOf('.')
        val validValue = if (decimalIndex != -1) {
            val integerPart = cleanValue.substringBefore('.')
            val decimalPart = cleanValue.substringAfter('.').filter { it.isDigit() } // Allow more decimals here if needed by type
            // Limit decimals for percentages specifically
            if (uiState.value.splitType == SplitType.PERCENTAGES) {
                "$integerPart.${decimalPart.take(2)}"
            } else {
                "$integerPart.$decimalPart"
            }
        } else {
            cleanValue
        }

        _uiState.update { state ->
            val updatedParticipants = state.participants.map { p ->
                if (p.uid == uid) p.copy(splitValue = validValue) else p
            }
            // Recalculate split immediately after input changes
            val calculatedParticipants = calculateSplit(
                amount = state.amount.toDoubleOrNull() ?: 0.0,
                participants = updatedParticipants,
                splitType = state.splitType
            )
            state.copy(participants = calculatedParticipants, error = null) // Clear error on change
        }
    }

    // --- Selectors ---

    fun handleGroupSelection(group: Group?, initialLoad: Boolean = false) {
        // TODO: Fetch real members/friends based on group selection
        // For now, use _allUsers, filtering if a group is selected
        val participantsList = if (group != null) {
            _relevantUsersForSelection.value
                .filter { p -> group.members.contains(p.uid) } // Filter by group members
                .map { payer ->
                    Participant(
                        uid = payer.uid,
                        name = payer.name,
                        isChecked = true,
                        splitValue = if (uiState.value.splitType == SplitType.PERCENTAGES && group.members.isNotEmpty()) {
                            // Calculate default percentage for EQUAL split
                            "%.2f".format(100.0 / group.members.size)
                        } else "1.0" // Default share/amount
                    )
                }
        } else { // Non-group selected
            val currentUser = _relevantUsersForSelection.value.firstOrNull() // Default to current user
            currentUser?.let {
                listOf(Participant(it.uid, it.name, isChecked = true, splitValue = "1.0"))
            } ?: emptyList()
        }

        _uiState.update { currentState ->
            // Recalculate splits based on the new participant list and current amount/type
            val calculatedParticipants = calculateSplit(
                amount = currentState.amount.toDoubleOrNull() ?: 0.0,
                participants = participantsList,
                splitType = currentState.splitType
            )
            currentState.copy(
                selectedGroup = group,
                currentGroupId = group?.id, // null if group is null (Non-group)
                isGroupSelectorVisible = if (initialLoad) currentState.isGroupSelectorVisible else false, // Don't hide if initial load
                participants = calculatedParticipants, // Update participants list
                error = null // Clear error on change
            )
        }
    }


    fun onPayerSelectionChanged(updatedSelectionState: List<Payer>) {
        // The list passed might contain unchecked users, filter them out for the state
        val checkedPayers = updatedSelectionState.filter { it.isChecked }

        _uiState.update {
            it.copy(
                paidByUsers = checkedPayers,
                error = null // Clear error on change
            )
        }
        // If switching back to single payer, reset their amount in UI state (optional)
        if (checkedPayers.size == 1) {
            _uiState.update {
                it.copy(paidByUsers = it.paidByUsers.map { p -> p.copy(amount = "0.00") })
            }
        }
    }


    fun onPayerAmountChanged(uid: String, newAmount: String) {
        // Validate amount input for payers too
        val cleanAmount = newAmount.filter { it.isDigit() || it == '.' }
        val decimalIndex = cleanAmount.indexOf('.')
        val validAmount = if (decimalIndex != -1) {
            val integerPart = cleanAmount.substringBefore('.')
            val decimalPart = cleanAmount.substringAfter('.').filter { it.isDigit() }.take(2)
            "$integerPart.$decimalPart"
        } else {
            cleanAmount
        }

        _uiState.update { state ->
            val updatedPayers = state.paidByUsers.map { payer ->
                if (payer.uid == uid) payer.copy(amount = validAmount) else payer
            }
            state.copy(paidByUsers = updatedPayers, error = null) // Clear error on change
        }
    }

    fun finalizePayerSelection() {
        _uiState.update { it.copy(isPayerSelectorVisible = false) }
    }

    fun onSelectSplitType(type: SplitType) {
        _uiState.update {
            val activeCount = it.participants.count { p -> p.isChecked }.coerceAtLeast(1) // Avoid division by zero
            val newParticipants = it.participants.map { p ->
                val defaultValue = when (type) {
                    SplitType.EQUALLY -> "0.00"
                    SplitType.PERCENTAGES -> "%.2f".format(100.0 / activeCount)
                    else -> "1.0" // UNEQUALLY, SHARES, ADJUSTMENTS default to 1 share/amount
                }
                p.copy(splitValue = defaultValue)
            }
            it.copy(
                splitType = type,
                participants = newParticipants,
                error = null // Clear error on change
            )
        }
        // Recalculate owesAmount based on the new type and default splitValues
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

    // --- Save Logic ---
    fun onSaveExpenseClick() {
        val state = uiState.value // Get a consistent snapshot of the state
        val totalAmount = state.amount.toDoubleOrNull() ?: 0.0
        val currentUser = userRepository.getCurrentUser()

        // --- Basic Validation ---
        if (currentUser == null) {
            _uiState.update { it.copy(error = "User not logged in.") }
            return
        }
        if (state.description.isBlank()) {
            _uiState.update { it.copy(error = "Please enter a description.") }
            return
        }
        if (totalAmount <= 0.0) {
            _uiState.update { it.copy(error = "Please enter a valid amount.") }
            return
        }
        if (state.paidByUsers.isEmpty()) {
            _uiState.update { it.copy(error = "Please select who paid.") }
            return
        }
        val activeParticipants = state.participants.filter { it.isChecked }
        if (activeParticipants.isEmpty()) {
            _uiState.update { it.copy(error = "Please select at least one participant to split with.") }
            return
        }

        // --- Payment Validation ---
        val paidByTotal = if (state.paidByUsers.size == 1) {
            totalAmount // Single payer paid the total
        } else {
            state.paidByUsers.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        }
        // Use a tolerance for floating point comparison
        if ((paidByTotal - totalAmount).absoluteValue > 0.01) { // Use extension property
            _uiState.update { it.copy(error = "Total paid amount (RM %.2f) must equal expense amount (RM %.2f).".format(paidByTotal, totalAmount)) }
            return
        }

        // --- Split Validation (using the already calculated owesAmount) ---
        // Recalculate one last time to be sure, applying rounding adjustments
        val finalParticipants = calculateSplit(totalAmount, state.participants, state.splitType)
        val finalAllocatedSplit = finalParticipants.filter { it.isChecked }.sumOf { it.owesAmount }

        if ((finalAllocatedSplit - totalAmount).absoluteValue > 0.01) { // Use extension property
            // This error *shouldn't* happen if calculateSplit adjustment works, but check defensively
            logE("Split calculation mismatch: Total $totalAmount vs Allocated $finalAllocatedSplit")
            _uiState.update { it.copy(error = "Split allocation (RM %.2f) doesn't match total expense (RM %.2f). Please check values.".format(finalAllocatedSplit, totalAmount)) }
            // Update state with recalculated values so user can see the issue if needed
            _uiState.update { it.copy(participants = finalParticipants) }
            return
        }

        // --- Prepare Data for Repository ---
        val expensePayers = state.paidByUsers.map { payer ->
            ExpensePayer(
                uid = payer.uid,
                paidAmount = if (state.paidByUsers.size == 1) totalAmount else payer.amount.toDoubleOrNull() ?: 0.0
            )
        }

        val expenseParticipants = finalParticipants.filter { it.isChecked }.map { participant ->
            ExpenseParticipant(
                uid = participant.uid,
                owesAmount = participant.owesAmount, // Use the final calculated amount
                initialSplitValue = participant.splitValue.toDoubleOrNull() ?: 0.0 // Store original input
            )
        }

        val newExpense = Expense(
            // ID generated by repository
            groupId = state.currentGroupId, // null for non-group
            description = state.description.trim(),
            totalAmount = totalAmount,
            createdByUid = currentUser.uid,
            date = state.date,
            splitType = state.splitType.name,
            paidBy = expensePayers,
            participants = expenseParticipants,
            memo = state.memo.trim(),
            imageUrls = emptyList() // TODO: Handle image uploads separately
        )

        // --- Call Repository ---
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // TODO: Implement image upload logic here before saving expense
            // 1. Set isUploadingImages = true
            // 2. Upload images in state.imageUris using StorageRepository
            // 3. Get download URLs
            // 4. Update newExpense object with imageUrls
            // 5. Set isUploadingImages = false

            val result = expenseRepository.addExpense(newExpense)

            result.onSuccess { expenseId ->
                Log.d("AddExpenseViewModel", "Expense saved with ID: $expenseId")
                val isGroupDetailNav = (state.initialGroupId != null) && (state.initialGroupId == state.currentGroupId)
                _uiEvent.emit(AddExpenseUiEvent.SaveSuccess(isGroupDetailNav))
                // Reset state after successful save (optional, depends on desired UX)
                // _uiState.value = AddExpenseUiState() // Basic reset
            }.onFailure { exception ->
                logE("Failed to save expense: ${exception.message}")
                _uiState.update { it.copy(error = "Failed to save expense: ${exception.message}") }
            }
            // Ensure loading is always turned off
            _uiState.update { it.copy(isLoading = false) }
        }
    }


    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.emit(AddExpenseUiEvent.NavigateBack)
        }
    }
}