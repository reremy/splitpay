package com.example.splitpay.ui.expense

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User // Import User for fetching friends/members
// Removed conflicting imports: com.example.splitpay.data.model.Participant
// Removed conflicting imports: com.example.splitpay.data.model.Payer
import com.example.splitpay.data.model.Expense // Data model for saving
import com.example.splitpay.data.model.ExpensePayer // Data model for saving
import com.example.splitpay.data.model.ExpenseParticipant // Data model for saving
import com.example.splitpay.data.model.Participant // Local UI model
import com.example.splitpay.data.model.Payer // Local UI model
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.domain.usecase.CalculateSplitUseCase
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE // Import logger
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.firstOrNull
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

    // Holds available groups for the dropdown selector
    private val _availableGroups = MutableStateFlow<List<Group>>(emptyList())
    val availableGroups: StateFlow<List<Group>> = _availableGroups

    // Holds the dynamic list of users (group members or friends) for selection dialogs (Payer/Split)
    private val _relevantUsersForSelection = MutableStateFlow<List<Payer>>(emptyList())
    val relevantUsersForSelection: StateFlow<List<Payer>> = _relevantUsersForSelection

    init {
        // Retrieve optional groupId passed via navigation
        val prefilledGroupId: String? = savedStateHandle["groupId"]
        Log.d("AddExpenseDebug", "ViewModel init: prefilledGroupId = $prefilledGroupId")

        _uiState.update {
            it.copy(
                initialGroupId = prefilledGroupId,
                currentGroupId = prefilledGroupId
            )
        }
        Log.d("AddExpenseDebug", "ViewModel init state: initialGroupId = ${_uiState.value.initialGroupId}, currentGroupId = ${_uiState.value.currentGroupId}")

        loadInitialData(prefilledGroupId) // Start loading initial data
    }

    /**
     * Loads initial data: current user, friends/group members, and available groups.
     * Sets up the initial state based on whether a group ID was prefilled.
     */
    private fun loadInitialData(prefilledGroupId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitiallyLoading = true, error = null) } // Start initial loading

            // --- 1. Fetch current user's profile ---
            val currentUser = userRepository.getCurrentUser()
            if (currentUser == null) {
                _uiState.update { it.copy(isInitiallyLoading = false, error = "User not logged in.") }
                return@launch
            }
            val userProfile = userRepository.getUserProfile(currentUser.uid)
            val currentUserName = userProfile?.username?.takeIf { it.isNotBlank() }
                ?: userProfile?.fullName?.takeIf { it.isNotBlank() }
                ?: currentUser.email ?: "You" // Fallback chain
            // Create a Payer object for the current user
            val currentUserPayer = Payer(currentUser.uid, currentUserName, isChecked = true)

            // --- 2. Fetch REAL friends list ---
            val friendProfiles = try {
                val friendIds = userRepository.getCurrentUserFriendIds() // Suspend call
                if (friendIds.isNotEmpty()) userRepository.getProfilesForFriends(friendIds) else emptyList() // Suspend call
            } catch (e: Exception) {
                logE("Failed to fetch friends for AddExpense: ${e.message}")
                emptyList<User>() // Return empty list on error
            }

            // Convert fetched User objects (friends) to Payer objects
            val friendsAsPayers = friendProfiles.map { friendUser ->
                Payer(
                    uid = friendUser.uid,
                    name = friendUser.username.takeIf { it.isNotBlank() } ?: friendUser.fullName // Prefer username
                )
            }
            // Combine current user and real friends for the non-group case
            val userAndFriendsPayers = (listOf(currentUserPayer) + friendsAsPayers).distinctBy { it.uid }


            // --- 3. Handle prefilled group OR default non-group state ---
            var initialRelevantPayers = userAndFriendsPayers // Default to user+friends
            var preloadedGroup: Group? = null

            if (prefilledGroupId != null) {
                // Fetch the specific group details
                preloadedGroup = try {
                    // Use a suspend function if available, otherwise use flow (less efficient for one-time fetch)
                    groupsRepository.getGroupFlow(prefilledGroupId).firstOrNull()
                } catch (e: Exception) {
                    logE("Failed to fetch prefilled group $prefilledGroupId: ${e.message}")
                    null
                }

                if (preloadedGroup != null) {
                    // Fetch member profiles for the prefilled group
                    val memberProfiles = try {
                        if (preloadedGroup.members.isNotEmpty()) {
                            userRepository.getProfilesForFriends(preloadedGroup.members) // Suspend call
                        } else emptyList()
                    } catch (e: Exception) {
                        logE("Failed to fetch group members for AddExpense: ${e.message}")
                        emptyList<User>()
                    }

                    // Convert group members to Payer objects
                    val groupMembersAsPayers = memberProfiles.map { memberUser ->
                        Payer(
                            uid = memberUser.uid,
                            name = memberUser.username.takeIf { it.isNotBlank() } ?: memberUser.fullName
                        )
                    }
                    initialRelevantPayers = groupMembersAsPayers.distinctBy { it.uid } // Set relevant users to group members
                } else {
                    // Group ID passed but not found (log error, fallback to user + friends)
                    logE("Prefilled group ID $prefilledGroupId not found.")
                    // initialRelevantPayers remains userAndFriendsPayers
                }
            }

            // Set the list of users available for selection in dialogs
            _relevantUsersForSelection.value = initialRelevantPayers

            // Initialize participants and default payer based on loaded context
            initializeParticipantsAndPayers(
                group = preloadedGroup, // Pass the loaded group (or null)
                relevantPayers = initialRelevantPayers,
                currentUserPayer = currentUserPayer,
                initialLoad = true // Indicate this is the initial setup
            )

            _uiState.update { it.copy(isInitiallyLoading = false) } // Stop initial loading indicator

            // --- 4. Fetch all available groups for the selector dropdown (runs after initial setup) ---
            // Listen for changes in the user's groups list
            groupsRepository.getGroups().collect { groups ->
                _availableGroups.value = groups
                // Optional: If a prefilled group was loaded, ensure its details (like name) are up-to-date in the UI state
                if (prefilledGroupId != null && _uiState.value.selectedGroup?.id == prefilledGroupId) {
                    groups.find { it.id == prefilledGroupId }?.let { updatedGroup ->
                        if (updatedGroup != _uiState.value.selectedGroup) {
                            _uiState.update { it.copy(selectedGroup = updatedGroup) }
                        }
                    }
                }
            }
        }
    }

    /**
     * Sets the initial `participants` list (defaults to all relevant users checked)
     * and the initial `paidByUsers` list (defaults to current user) in the UI state.
     * Called during initial load and when the group selection changes.
     */
    private fun initializeParticipantsAndPayers(
        group: Group?, // The selected group (null for non-group)
        relevantPayers: List<Payer>, // Users to include as participants (group members or user+friends)
        currentUserPayer: Payer, // The current user as a Payer object
        initialLoad: Boolean = false // Flag to differentiate initial setup from manual group change
    ) {
        if (relevantPayers.isEmpty()) {
            logE("initializeParticipantsAndPayers called with empty relevantPayers list.")
            // Handle this case, perhaps by defaulting to just the current user
            // For now, let's ensure currentUser is always included if relevantPayers is empty
            val fallbackPayers = if(relevantPayers.isEmpty()) listOf(currentUserPayer) else relevantPayers
            initializeParticipantsAndPayers(group, fallbackPayers, currentUserPayer, initialLoad)
            return
        }

        _uiState.update { currentState ->
            // Participants default to ALL relevant users, initially checked
            val initialParticipants = relevantPayers.map { payer ->
                Participant(
                    uid = payer.uid,
                    name = payer.name,
                    isChecked = true, // Default to checked
                    // Set initial split value based on the default split type
                    splitValue = if (currentState.splitType == SplitType.PERCENTAGES) {
                        "%.2f".format(100.0 / relevantPayers.size) // Divide 100% equally
                    } else "1.0" // Default for Shares or fallback
                )
            }

            // Calculate initial owesAmount based on default split
            val calculatedParticipants = calculateSplitUseCase(
                amount = currentState.amount.toDoubleOrNull() ?: 0.0, // Use current amount if any
                participants = initialParticipants,
                splitType = currentState.splitType // Use current split type
            )

            // Payer defaults to the current user paying the full (current) amount
            val defaultPayer = listOf(currentUserPayer.copy(
                amount = currentState.amount.ifEmpty { "0.00" }, // Use current amount or 0.00
                isChecked = true
            ))

            currentState.copy(
                selectedGroup = group,
                currentGroupId = group?.id, // Update currentGroupId
                // Hide selector unless it's the initial load for a specific group
                isGroupSelectorVisible = if (initialLoad && group != null) currentState.isGroupSelectorVisible else false,
                participants = calculatedParticipants, // Set the calculated participants list
                paidByUsers = defaultPayer, // Reset payer to current user
                error = null // Clear previous errors
            )
        }
    }

    /**
     * Handles selection changes from the Group Selector Dialog.
     * Fetches relevant users (group members or friends) and updates the UI state.
     */
    fun handleGroupSelection(group: Group?, initialLoad: Boolean = false) { // Keep initialLoad param if needed elsewhere
        viewModelScope.launch { // Launch coroutine for repository calls
            _uiState.update { it.copy(isLoading = true) } // Show temporary loading

            val currentUser = userRepository.getCurrentUser() ?: run {
                _uiState.update { it.copy(isLoading = false, error = "User not logged in.") }
                return@launch
            }
            // Fetch current user details again for safety/consistency
            val userProfile = userRepository.getUserProfile(currentUser.uid)
            val currentUserName = userProfile?.username?.takeIf { it.isNotBlank() }
                ?: userProfile?.fullName?.takeIf { it.isNotBlank() }
                ?: currentUser.email ?: "You"
            val currentUserPayer = Payer(currentUser.uid, currentUserName, isChecked = true)

            val relevantPayers: List<Payer>

            if (group != null) {
                // --- Group selected ---
                logD("Group selected: ${group.name}, fetching members...")
                val memberProfiles = try {
                    if (group.members.isNotEmpty()) userRepository.getProfilesForFriends(group.members) else emptyList()
                } catch (e: Exception) {
                    logE("Failed to fetch group members on selection: ${e.message}")
                    emptyList<User>()
                }
                relevantPayers = memberProfiles.map { Payer(it.uid, it.username.takeIf { n -> n.isNotBlank() } ?: it.fullName) }
            } else {
                // --- Non-group selected ---
                logD("Non-group selected, fetching friends...")
                val friendProfiles = try {
                    val friendIds = userRepository.getCurrentUserFriendIds()
                    if (friendIds.isNotEmpty()) userRepository.getProfilesForFriends(friendIds) else emptyList()
                } catch (e: Exception) {
                    logE("Failed to fetch friends on non-group selection: ${e.message}")
                    emptyList<User>()
                }
                // Combine user + friends for non-group context
                relevantPayers = (listOf(currentUserPayer) + friendProfiles.map { Payer(it.uid, it.username.takeIf { n -> n.isNotBlank() } ?: it.fullName) }).distinctBy { it.uid }
            }

            // Update the list used by Payer/Split dialogs
            _relevantUsersForSelection.value = relevantPayers.distinctBy { it.uid }

            // Use the helper to update main screen participants and reset payer
            initializeParticipantsAndPayers(
                group = group,
                relevantPayers = _relevantUsersForSelection.value,
                currentUserPayer = currentUserPayer,
                initialLoad = false // Explicitly false as this is a manual selection change
            )

            _uiState.update { it.copy(isLoading = false) } // Hide temporary loading
        }
    }


    // --- User Input Handlers ---
    fun onDescriptionChange(newDescription: String) {
        if (newDescription.length <= 100) {
            _uiState.update { it.copy(description = newDescription, error = null) }
        } else {
            // Optionally show feedback, but prevent update if too long
            // _uiState.update { it.copy(error = "Description cannot exceed 100 characters.") }
        }
    }

    fun onAmountChange(newAmount: String) {
        // Basic filtering for valid number format (allows one '.')
        val cleanAmount = newAmount.filter { it.isDigit() || it == '.' }
        val decimalIndex = cleanAmount.indexOf('.')
        val validAmount = if (decimalIndex != -1) {
            // Ensure only digits before and max 2 digits after '.'
            val integerPart = cleanAmount.substringBefore('.')
            val decimalPart = cleanAmount.substringAfter('.').filter { it.isDigit() }.take(2)
            "$integerPart.$decimalPart"
        } else {
            cleanAmount // No decimal yet
        }

        // Update state only if it's potentially valid number (or empty)
        // This prevents excessive recalculations on invalid input
        if (validAmount.isEmpty() || validAmount.toDoubleOrNull() != null || validAmount == ".") {
            _uiState.update { it.copy(amount = validAmount, error = null) }
            // Recalculate split whenever amount changes
            recalculateSplit(validAmount.toDoubleOrNull() ?: 0.0, uiState.value.splitType)
        }
    }

    fun onDateSelected(newDateMillis: Long) {
        _uiState.update { it.copy(date = newDateMillis, isDatePickerDialogVisible = false) }
    }

    fun onMemoChanged(newMemo: String) {
        _uiState.update { it.copy(memo = newMemo) }
    }

    fun onMemoSaved(finalMemo: String) {
        _uiState.update { it.copy(memo = finalMemo.trim(), isMemoDialogVisible = false) } // Trim and hide dialog
    }

    fun onImageAdded(uriString: String) {
        _uiState.update { it.copy(imageUris = it.imageUris + uriString) }
    }

    fun onImageRemoved(uriString: String) {
        _uiState.update { it.copy(imageUris = it.imageUris - uriString) }
    }

    fun showDatePickerDialog(isVisible: Boolean) {
        _uiState.update { it.copy(isDatePickerDialogVisible = isVisible) }
    }

    fun showMemoDialog(isVisible: Boolean) {
        _uiState.update { it.copy(isMemoDialogVisible = isVisible) }
    }


    /**
     * Recalculates the `owesAmount` for all participants based on the current
     * total amount, split type, and participant selections/values.
     */
    private fun recalculateSplit(newAmount: Double, newSplitType: SplitType) {
        _uiState.update { state ->
            // Use the existing participants list from the state
            val updatedParticipants = calculateSplitUseCase(
                amount = newAmount,
                participants = state.participants, // Pass current list to useCase
                splitType = newSplitType
            )
            state.copy(participants = updatedParticipants) // Update state with results
        }
    }

    // --- Participant Checkbox Handler ---
    fun onParticipantCheckedChange(uid: String, isChecked: Boolean) {
        _uiState.update { state ->
            // Update the isChecked status for the specific participant
            val updatedParticipants = state.participants.map {
                if (it.uid == uid) it.copy(isChecked = isChecked) else it
            }
            // Recalculate immediately after checking/unchecking using the updated list
            val recalculatedParticipants = calculateSplitUseCase(
                amount = state.amount.toDoubleOrNull() ?: 0.0,
                participants = updatedParticipants, // Pass the list with updated isChecked
                splitType = state.splitType
            )
            state.copy(participants = recalculatedParticipants, error = null)
        }
    }


    /**
     * Handles changes to the value input field (amount, percentage, share) for a specific
     * participant in the Split Editor dialog.
     */
    fun onParticipantSplitValueChanged(uid: String, newValue: String) {
        // Basic filtering for valid number format
        val cleanValue = newValue.filter { it.isDigit() || it == '.' }
        val decimalIndex = cleanValue.indexOf('.')
        val validValue = if (decimalIndex != -1) {
            val integerPart = cleanValue.substringBefore('.')
            // Allow more decimals for shares/amount, restrict for percentages
            val decimalPart = cleanValue.substringAfter('.').filter { it.isDigit() }
            if (uiState.value.splitType == SplitType.PERCENTAGES) {
                "$integerPart.${decimalPart.take(2)}" // Max 2 decimal places for %
            } else {
                "$integerPart.${decimalPart.take(2)}" // Also limit amount/adjustments to 2 for currency
            }
        } else {
            cleanValue
        }

        // Update state only if potentially valid number (or empty)
        if (validValue.isEmpty() || validValue.toDoubleOrNull() != null || validValue == ".") {
            _uiState.update { state ->
                // Update the splitValue for the specific participant
                val updatedParticipants = state.participants.map { p ->
                    if (p.uid == uid) p.copy(splitValue = validValue) else p
                }
                // Recalculate the owesAmount based on the new splitValue
                val calculatedParticipants = calculateSplitUseCase(
                    amount = state.amount.toDoubleOrNull() ?: 0.0,
                    participants = updatedParticipants, // Pass updated list to useCase
                    splitType = state.splitType
                )
                state.copy(participants = calculatedParticipants, error = null) // Update state
            }
        }
    }

    // --- Payer Selection Handlers (Use local Payer class) ---
    /**
     * Updates the `paidByUsers` list based on checkbox selections in the Payer Dialog.
     * Resets amounts if switching back to a single payer.
     */
    fun onPayerSelectionChanged(updatedSelectionState: List<Payer>) {
        val checkedPayers = updatedSelectionState.filter { it.isChecked }
        val isMultiPayerNow = checkedPayers.size > 1
        val wasMultiPayer = uiState.value.paidByUsers.size > 1

        _uiState.update { currentState ->
            // Reset amounts if going FROM multi-payer TO single payer
            val finalPayers = if (wasMultiPayer && !isMultiPayerNow && checkedPayers.isNotEmpty()) {
                val singlePayer = checkedPayers.first()
                listOf(singlePayer.copy(amount = currentState.amount.ifEmpty { "0.00" })) // Set single payer amount to total
            } else {
                checkedPayers // Keep amounts as they are (or were just entered)
            }
            currentState.copy(paidByUsers = finalPayers, error = null)
        }
    }

    /**
     * Updates the amount for a specific payer in the multi-payer selection dialog.
     */
    fun onPayerAmountChanged(uid: String, newAmount: String) {
        // Basic filtering for valid currency format
        val cleanAmount = newAmount.filter { it.isDigit() || it == '.' }
        val decimalIndex = cleanAmount.indexOf('.')
        val validAmount = if (decimalIndex != -1) {
            val integerPart = cleanAmount.substringBefore('.')
            val decimalPart = cleanAmount.substringAfter('.').filter { it.isDigit() }.take(2)
            "$integerPart.$decimalPart"
        } else {
            cleanAmount
        }

        // Update state only if potentially valid
        if (validAmount.isEmpty() || validAmount.toDoubleOrNull() != null || validAmount == ".") {
            _uiState.update { state ->
                val updatedPayers = state.paidByUsers.map { payer ->
                    if (payer.uid == uid) payer.copy(amount = validAmount) else payer
                }
                state.copy(paidByUsers = updatedPayers, error = null)
            }
        }
    }

    /** Hides the Payer Selector dialog. */
    fun finalizePayerSelection() {
        // Optional: Add validation here before closing if needed (e.g., ensure amounts sum up)
        _uiState.update { it.copy(isPayerSelectorVisible = false) }
    }

    // --- Split Type Handlers (Use local Participant class) ---
    /**
     * Changes the selected split type and resets participant `splitValue`s accordingly.
     * Recalculates `owesAmount` based on the new type.
     */
    fun onSelectSplitType(type: SplitType) {
        _uiState.update { currentState ->
            val activeCount = currentState.participants.count { p -> p.isChecked }.coerceAtLeast(1)
            // Reset splitValue based on the newly selected type
            val newParticipants = currentState.participants.map { p ->
                val defaultValue = when (type) {
                    SplitType.EQUALLY -> "0.00" // OwesAmount calculated directly
                    SplitType.PERCENTAGES -> "%.2f".format(100.0 / activeCount) // Distribute 100%
                    SplitType.SHARES -> "1.0" // Default to 1 share
                    SplitType.UNEQUALLY, SplitType.ADJUSTMENTS -> "0.00" // Default to 0, user must enter
                }
                p.copy(splitValue = defaultValue) // Apply default value
            }
            // Update state with new type and reset participants
            currentState.copy(
                splitType = type,
                participants = newParticipants,
                error = null
            )
        }
        // Recalculate owesAmount based on the new type and reset splitValues
        recalculateSplit(uiState.value.amount.toDoubleOrNull() ?: 0.0, type)
    }

    /** Hides the Split Editor dialog. */
    fun finalizeSplitTypeSelection() {
        // Optional: Add validation here (e.g., ensure percentages add to 100) before closing
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

        // --- Validation Checks ---
        if (currentUser == null) {
            emitErrorDialog("Error", "You are not logged in.")
            return
        }
        if (state.description.isBlank()) {
            emitErrorDialog("Invalid Expense", "Please enter a description.")
            return
        }
        if (totalAmount <= 0.0) {
            emitErrorDialog("Invalid Expense", "Amount must be greater than zero.")
            return
        }
        if (state.paidByUsers.isEmpty()) {
            emitErrorDialog("Invalid Expense", "At least one person must pay.")
            return
        }
        val activeParticipants = state.participants.filter { it.isChecked }
        if (activeParticipants.isEmpty()) {
            emitErrorDialog("Invalid Expense", "At least one participant must be selected for the split.")
            return
        }

        // --- Payment Balance Validation ---
        // If single payer, amount is totalAmount. If multiple, sum entered amounts.
        val paidByTotal = if (state.paidByUsers.size == 1) totalAmount else state.paidByUsers.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        if ((paidByTotal - totalAmount).absoluteValue > 0.015) { // Increased tolerance slightly for multi-payer rounding
            emitErrorDialog("Payment Error", "The total amount paid (MYR${"%.2f".format(paidByTotal)}) does not match the expense total (MYR${"%.2f".format(totalAmount)}). Please adjust amounts in 'Paid by'.")
            return
        }

        // --- Split Balance Validation (using the final calculated amounts) ---
        // Recalculate one last time to be sure, using the final payer info logic
        val finalPayersForCalc = if (state.paidByUsers.size == 1) {
            state.paidByUsers.map { it.copy(amount = totalAmount.toString()) }
        } else {
            state.paidByUsers
        }

        val finalParticipants = calculateSplitUseCase(totalAmount, state.participants, state.splitType)
        val finalAllocatedSplit = finalParticipants.filter { it.isChecked }.sumOf { it.owesAmount }
        if ((finalAllocatedSplit - totalAmount).absoluteValue > 0.015) { // Check if split sums to total (with tolerance)
            emitErrorDialog("Split Error", "The total split amount (MYR${"%.2f".format(finalAllocatedSplit)}) does not match the expense total (MYR${"%.2f".format(totalAmount)}). Please adjust the split details.")
            return
        }
        // Additional check for specific split types
        if (state.splitType == SplitType.PERCENTAGES) {
            val totalPercent = finalParticipants.filter { it.isChecked }.sumOf { it.splitValue.toDoubleOrNull() ?: 0.0 }
            if ((totalPercent - 100.0).absoluteValue > 0.1) { // Allow slight tolerance for percentages
                emitErrorDialog("Split Error", "Percentages must add up to 100%. Current total: ${"%.2f".format(totalPercent)}%")
                return
            }
        }


        // --- Prepare Data for Repository (Convert UI models to Data models) ---
        val expensePayers = finalPayersForCalc.map { payer -> // Use finalPayersForCalc
            ExpensePayer(
                uid = payer.uid,
                paidAmount = payer.amount.toDoubleOrNull() ?: 0.0 // Ensure correct amount is used
            )
        }

        val expenseParticipants = finalParticipants.filter { it.isChecked }.map { participant ->
            ExpenseParticipant(
                uid = participant.uid,
                owesAmount = participant.owesAmount, // Use the final calculated owesAmount
                initialSplitValue = participant.splitValue.toDoubleOrNull() ?: 0.0 // Store the value entered/calculated
            )
        }

        val newExpense = Expense(
            // id is generated by repository if blank
            groupId = state.currentGroupId, // Use currentGroupId (can be null)
            description = state.description.trim(),
            totalAmount = totalAmount,
            createdByUid = currentUser.uid,
            date = state.date, // Use selected date
            splitType = state.splitType.name, // Save enum name as String
            paidBy = expensePayers,
            participants = expenseParticipants,
            memo = state.memo.trim(),
            imageUrls = emptyList() // TODO: Handle image uploads separately
        )

        // --- Call Repository ---
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) } // Show loading on save button

            // TODO: Implement image upload logic here if needed, get URLs

            val result = expenseRepository.addExpense(newExpense)

            result.onSuccess { expenseId ->
                Log.d("AddExpenseViewModel", "Expense saved successfully with ID: $expenseId")
                // Determine if navigation should go back specifically to group detail
                val isGroupDetailNav = state.initialGroupId != null && state.initialGroupId == state.currentGroupId
                _uiEvent.emit(AddExpenseUiEvent.SaveSuccess(isGroupDetailNav)) // Emit success event
            }.onFailure { exception ->
                logE("Failed to save expense: ${exception.message}")
                // Show specific error dialog on failure
                emitErrorDialog("Save Failed", "Could not save expense: ${exception.message}")
                _uiState.update { it.copy(error = "Failed to save expense.") } // Also update state error if needed
            }
            _uiState.update { it.copy(isLoading = false) } // Ensure loading stops
        }
    }

    /** Helper to emit ShowErrorDialog event */
    private fun emitErrorDialog(title: String, message: String) {
        viewModelScope.launch {
            _uiEvent.emit(AddExpenseUiEvent.ShowErrorDialog(title, message))
        }
    }

    /** Emits NavigateBack event */
    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.emit(AddExpenseUiEvent.NavigateBack)
        }
    }
}