package com.example.splitpay.ui.expense

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
// Removed conflicting imports: com.example.splitpay.data.model.Participant
// Removed conflicting imports: com.example.splitpay.data.model.Payer
import com.example.splitpay.data.model.Expense // Data model for saving
import com.example.splitpay.data.model.ExpensePayer // Data model for saving
import com.example.splitpay.data.model.ExpenseParticipant // Data model for saving
import com.example.splitpay.data.model.Participant
import com.example.splitpay.data.model.Payer
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.domain.usecase.CalculateSplitUseCase
import com.example.splitpay.logger.logE // Import logger if needed
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.filter
import kotlin.collections.find
import kotlin.collections.map
import kotlin.math.roundToInt
import kotlin.math.absoluteValue



class AddExpenseViewModel(
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val calculateSplitUseCase = CalculateSplitUseCase()

    // --- StateFlows and SharedFlow ---
    private val _uiState = MutableStateFlow(AddExpenseUiState())
    val uiState: StateFlow<AddExpenseUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<AddExpenseUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val _availableGroups = MutableStateFlow<List<Group>>(emptyList())
    val availableGroups: StateFlow<List<Group>> = _availableGroups

    // Holds the dynamic list of users (group members or friends) for selection dialogs
    private val _relevantUsersForSelection = MutableStateFlow<List<Payer>>(emptyList())
    val relevantUsersForSelection: StateFlow<List<Payer>> = _relevantUsersForSelection

    init {
        val prefilledGroupId: String? = savedStateHandle["groupId"]
        Log.d("AddExpenseDebug", "ViewModel init: prefilledGroupId = $prefilledGroupId")

        _uiState.update {
            it.copy(
                initialGroupId = prefilledGroupId,
                currentGroupId = prefilledGroupId
            )
        }
        Log.d("AddExpenseDebug", "ViewModel init: initialGroupId = ${_uiState.value.initialGroupId}, currentGroupId = ${_uiState.value.currentGroupId}")

        loadInitialData(prefilledGroupId)
    }

    private fun loadInitialData(prefilledGroupId: String?) {
        viewModelScope.launch {
            // --- 1. Fetch current user's profile FIRST ---
            val currentUser = userRepository.getCurrentUser()
            if (currentUser == null) {
                _uiState.update { it.copy(error = "User not logged in.") }
                return@launch
            }
            // Fetch from Firestore for username (Corrected typo: userRepository)
            val userProfile = userRepository.getUserProfile(currentUser.uid)
            val currentUserName = userProfile?.username?.takeIf { it.isNotBlank() }
                ?: userProfile?.fullName?.takeIf { it.isNotBlank() }
                ?: currentUser.email ?: "You" // Fallback chain
            val currentUserPayer =
                Payer(currentUser.uid, currentUserName, isChecked = true) // Uses local Payer

            // --- 2. Mock friends list (TODO: Replace with real friends fetch) ---
            val mockFriends = listOf(
                Payer("uid_A", "Person A"), // Uses local Payer
                Payer("uid_B", "Person B")  // Uses local Payer
            )
            val userAndFriends = listOf(currentUserPayer) + mockFriends

            // --- 3. Handle prefilled group OR default state ---
            if (prefilledGroupId != null) {
                // TODO: Add groupsRepository.getGroupById(groupId) function
                // val group = groupsRepository.getGroupById(prefilledGroupId)
                val group: Group? = _availableGroups.value.find { it.id == prefilledGroupId } // Temporary find from collected list
                if (group != null) {
                    // This will set participants AND relevant users
                    handleGroupSelection(group, initialLoad = true)
                } else {
                    // Group ID passed but not found (handle error or default)
                    Log.e("AddExpenseVM", "Prefilled group ID $prefilledGroupId not found.")
                    _relevantUsersForSelection.value = userAndFriends
                    setDefaultParticipantsAndPayers(currentUserPayer)
                }
            } else {
                // No prefilled group, default to user + friends
                _relevantUsersForSelection.value = userAndFriends
                setDefaultParticipantsAndPayers(currentUserPayer)
            }

            // --- 4. Fetch all groups for the selector dropdown (runs after initial setup) ---
            groupsRepository.getGroups().collect { groups ->
                _availableGroups.value = groups
                // If a prefilled group was loaded earlier, ensure its details are up-to-date
                if (prefilledGroupId != null && _uiState.value.selectedGroup != null) {
                    groups.find { it.id == prefilledGroupId }?.let { updatedGroup ->
                        if (updatedGroup != _uiState.value.selectedGroup) {
                            _uiState.update { it.copy(selectedGroup = updatedGroup) }
                        }
                    }
                }
            }
        }
    }

    // Helper to set default payer/participant (current user only) for non-group start
    private fun setDefaultParticipantsAndPayers(currentUserPayer: Payer) { // Uses local Payer
        if (uiState.value.participants.isEmpty()) { // Only set if not already populated
            _uiState.update { currentState ->
                // Uses local Participant
                val initialParticipants = listOf(
                    Participant(
                        currentUserPayer.uid,
                        currentUserPayer.name,
                        splitValue = "1.0"
                    )
                )
                currentState.copy(
                    // Uses local Payer
                    paidByUsers = listOf(currentUserPayer.copy(amount = "0.00", isChecked = true)),
                    participants = calculateSplitUseCase( // <-- USECASE CALLED HERE
                        amount = currentState.amount.toDoubleOrNull() ?: 0.0,
                        participants = initialParticipants,
                        splitType = currentState.splitType
                    )
                )
            }
        }
    }

    // --- Renamed 'onSelectGroup' ---
    fun handleGroupSelection(group: Group?, initialLoad: Boolean = false) {
        // --- 1. Get current user + mock friends (TODO: Replace with real data) ---
        // Ensure currentUser details are fetched and valid first from loadInitialData
        val currentUser = _relevantUsersForSelection.value.firstOrNull { it.uid == userRepository.getCurrentUser()?.uid }
        val mockFriends = listOf(
            Payer("uid_A", "Person A"), // Uses local Payer
            Payer("uid_B", "Person B")  // Uses local Payer
        )

        // --- 2. Determine the list of relevant users for selection ---
        val newRelevantUsers = if (group != null) {
            // Group selected: List should be all group members
            // TODO: Fetch actual member details (names) from Firestore based on group.members list of UIDs
            group.members.mapNotNull { memberUid ->
                // Find user details from current list (which includes user + friends initially)
                _relevantUsersForSelection.value.find { it.uid == memberUid }
                    ?: mockFriends.find { it.uid == memberUid } // Or check mock friends if not current user
                    ?: Payer(
                        memberUid,
                        "Member $memberUid"
                    ) // Fallback if user details aren't loaded yet
            }.distinctBy { it.uid }
        } else {
            // Non-group selected: List should be user + friends
            (currentUser?.let { listOf(it) } ?: emptyList()) + mockFriends
        }.distinctBy { it.uid } // Ensure uniqueness for non-group too

        _relevantUsersForSelection.value = newRelevantUsers // Update the list for the dialog

        // --- 3. Determine the list of *participants* for the main screen ---
        // Defaults to ALL relevant users, checked
        val participantsList = newRelevantUsers.map { payer -> // Uses local Participant
            Participant(
                uid = payer.uid,
                name = payer.name,
                isChecked = true,
                splitValue = if (uiState.value.splitType == SplitType.PERCENTAGES && newRelevantUsers.isNotEmpty()) {
                    "%.2f".format(100.0 / newRelevantUsers.size)
                } else "1.0"
            )
        }

        // --- 4. Update the UI State ---
        _uiState.update { currentState ->
            val calculatedParticipants = calculateSplitUseCase(
                amount = currentState.amount.toDoubleOrNull() ?: 0.0,
                participants = participantsList,
                splitType = currentState.splitType
            )
            // Reset payer to current user whenever the group changes
            val defaultPayer = currentUser?.copy(amount = "0.00", isChecked = true)?.let { listOf(it) } ?: emptyList() // Uses local Payer

            currentState.copy(
                selectedGroup = group,
                currentGroupId = group?.id,
                isGroupSelectorVisible = if (initialLoad) currentState.isGroupSelectorVisible else false,
                participants = calculatedParticipants, // Set main participants list
                paidByUsers = defaultPayer, // Reset payer to current user
                error = null
            )
        }
    }


    // --- User Input Handlers ---
    fun onDescriptionChange(newDescription: String) {
        if (newDescription.length <= 100) {
            _uiState.update { it.copy(description = newDescription, error = null) }
        } else {
            _uiState.update { it.copy(error = "Description cannot exceed 100 characters.") }
        }
    }

    fun onAmountChange(newAmount: String) {
        val cleanAmount = newAmount.filter { it.isDigit() || it == '.' }
        val decimalIndex = cleanAmount.indexOf('.')
        val validAmount = if (decimalIndex != -1) {
            val integerPart = cleanAmount.substringBefore('.')
            val decimalPart = cleanAmount.substringAfter('.').filter { it.isDigit() }.take(2)
            "$integerPart.$decimalPart"
        } else {
            cleanAmount
        }
        _uiState.update { it.copy(amount = validAmount, error = null) }
        recalculateSplit(validAmount.toDoubleOrNull() ?: 0.0, uiState.value.splitType)
    }

    fun onDateSelected(newDateMillis: Long) {
        _uiState.update { it.copy(date = newDateMillis) }
    }

    fun onMemoChanged(newMemo: String) {
        _uiState.update { it.copy(memo = newMemo) }
    }

    fun onImageAdded(uriString: String) {
        _uiState.update { it.copy(imageUris = it.imageUris + uriString) }
    }

    fun onImageRemoved(uriString: String) {
        _uiState.update { it.copy(imageUris = it.imageUris - uriString) }
    }


    private fun recalculateSplit(newAmount: Double, newSplitType: SplitType) {
        _uiState.update { state ->
            val updatedParticipants = calculateSplitUseCase(
                amount = newAmount,
                participants = state.participants, // Use current participants list
                splitType = newSplitType
            )
            state.copy(participants = updatedParticipants)
        }
    }

    // --- Participant Checkbox Handler ---
    fun onParticipantCheckedChange(uid: String, isChecked: Boolean) {
        _uiState.update { state ->
            val updatedParticipants = state.participants.map {
                if (it.uid == uid) it.copy(isChecked = isChecked) else it
            }
            // Recalculate immediately after checking/unchecking
            val recalculatedParticipants = calculateSplitUseCase(
                amount = state.amount.toDoubleOrNull() ?: 0.0,
                participants = updatedParticipants,
                splitType = state.splitType
            )
            state.copy(participants = recalculatedParticipants, error = null)
        }
    }


    fun onParticipantSplitValueChanged(uid: String, newValue: String) {
        val cleanValue = newValue.filter { it.isDigit() || it == '.' }
        val decimalIndex = cleanValue.indexOf('.')
        val validValue = if (decimalIndex != -1) {
            val integerPart = cleanValue.substringBefore('.')
            val decimalPart = cleanValue.substringAfter('.').filter { it.isDigit() }
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
            val calculatedParticipants = calculateSplitUseCase(
                amount = state.amount.toDoubleOrNull() ?: 0.0,
                participants = updatedParticipants,
                splitType = state.splitType
            )
            state.copy(participants = calculatedParticipants, error = null)
        }
    }

    // --- Payer Selection Handlers (Use local Payer class) ---
    fun onPayerSelectionChanged(updatedSelectionState: List<Payer>) {
        val checkedPayers = updatedSelectionState.filter { it.isChecked }
        _uiState.update {
            it.copy(paidByUsers = checkedPayers, error = null)
        }
        // Reset amounts if going back to single payer
        if (checkedPayers.size == 1) {
            _uiState.update {
                it.copy(paidByUsers = it.paidByUsers.map { p -> p.copy(amount = "0.00") })
            }
        }
    }

    fun onPayerAmountChanged(uid: String, newAmount: String) {
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
            state.copy(paidByUsers = updatedPayers, error = null)
        }
    }

    fun finalizePayerSelection() {
        _uiState.update { it.copy(isPayerSelectorVisible = false) }
    }

    // --- Split Type Handlers (Use local Participant class) ---
    fun onSelectSplitType(type: SplitType) {
        _uiState.update {
            val activeCount = it.participants.count { p -> p.isChecked }.coerceAtLeast(1)
            val newParticipants = it.participants.map { p ->
                val defaultValue = when (type) {
                    SplitType.EQUALLY -> "0.00"
                    SplitType.PERCENTAGES -> "%.2f".format(100.0 / activeCount)
                    else -> "1.0"
                }
                p.copy(splitValue = defaultValue)
            }
            it.copy(
                splitType = type,
                participants = newParticipants,
                error = null
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

    // --- Save Logic ---
    fun onSaveExpenseClick() {
        val state = uiState.value
        val totalAmount = state.amount.toDoubleOrNull() ?: 0.0
        val currentUser = userRepository.getCurrentUser()

        // --- Basic Validation ---
        if (currentUser == null) { /* ... error handling ... */ return }
        if (state.description.isBlank()) { /* ... error handling ... */ return }
        if (totalAmount <= 0.0) { /* ... error handling ... */ return }
        if (state.paidByUsers.isEmpty()) { /* ... error handling ... */ return }
        val activeParticipants = state.participants.filter { it.isChecked }
        if (activeParticipants.isEmpty()) { /* ... error handling ... */ return }

        // --- Payment Validation ---
        val paidByTotal = if (state.paidByUsers.size == 1) totalAmount else state.paidByUsers.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        if ((paidByTotal - totalAmount).absoluteValue > 0.01) { /* ... error handling ... */ return }

        // --- Split Validation ---
        val finalParticipants = calculateSplitUseCase(totalAmount, state.participants, state.splitType)
        val finalAllocatedSplit = finalParticipants.filter { it.isChecked }.sumOf { it.owesAmount }
        if ((finalAllocatedSplit - totalAmount).absoluteValue > 0.01) { /* ... error handling ... */ return }

        // --- Prepare Data for Repository (Convert local UI models to data models) ---
        val expensePayers = state.paidByUsers.map { payer -> // uses local Payer
            ExpensePayer( // Creates data model ExpensePayer
                uid = payer.uid,
                paidAmount = if (state.paidByUsers.size == 1) totalAmount else payer.amount.toDoubleOrNull() ?: 0.0
            )
        }

        val expenseParticipants = finalParticipants.filter { it.isChecked }.map { participant -> // uses local Participant
            ExpenseParticipant( // Creates data model ExpenseParticipant
                uid = participant.uid,
                owesAmount = participant.owesAmount,
                initialSplitValue = participant.splitValue.toDoubleOrNull() ?: 0.0
            )
        }

        val newExpense = Expense( // Creates data model Expense
            groupId = state.currentGroupId,
            description = state.description.trim(),
            totalAmount = totalAmount,
            createdByUid = currentUser.uid,
            date = state.date,
            splitType = state.splitType.name,
            paidBy = expensePayers,
            participants = expenseParticipants,
            memo = state.memo.trim(),
            imageUrls = emptyList() // TODO: Handle image uploads
        )

        // --- Call Repository ---
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // TODO: Implement image upload logic here

            val result = expenseRepository.addExpense(newExpense)

            result.onSuccess { expenseId ->
                Log.d("AddExpenseViewModel", "Expense saved with ID: $expenseId")
                val isGroupDetailNav = (state.initialGroupId != null) && (state.initialGroupId == state.currentGroupId)
                _uiEvent.emit(AddExpenseUiEvent.SaveSuccess(isGroupDetailNav))
            }.onFailure { exception ->
                logE("Failed to save expense: ${exception.message}")
                _uiState.update { it.copy(error = "Failed to save expense: ${exception.message}") }
            }
            _uiState.update { it.copy(isLoading = false) } // Ensure loading stops
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.emit(AddExpenseUiEvent.NavigateBack)
        }
    }
}