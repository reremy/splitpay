package com.example.splitpay.ui.expense

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Activity
import com.example.splitpay.data.model.ActivityType
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User // Import User for fetching friends/members
// Removed conflicting imports: com.example.splitpay.data.model.Participant
// Removed conflicting imports: com.example.splitpay.data.model.Payer
import com.example.splitpay.data.model.Expense // Data model for saving
import com.example.splitpay.data.model.ExpensePayer // Data model for saving
import com.example.splitpay.data.model.ExpenseParticipant // Data model for saving
import com.example.splitpay.data.model.Participant // Local UI model
import com.example.splitpay.data.model.Payer // Local UI model
import android.net.Uri
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.FileStorageRepository
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
    private val fileStorageRepository: FileStorageRepository = FileStorageRepository(),
    private val activityRepository: ActivityRepository,
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

    // --- NEW: Placeholder for Non-Group ---
    private val nonGroupPlaceholder = Group(id = "non_group", name = "Non-group Expenses", iconIdentifier = "info")


    private val expenseId: String? = savedStateHandle["expenseId"]
    private val isEditMode: Boolean = !expenseId.isNullOrBlank()

    init {
        // Retrieve optional groupId passed via navigation
        val prefilledGroupId: String? = savedStateHandle["groupId"]
        Log.d("AddExpenseDebug", "ViewModel init: prefilledGroupId = $prefilledGroupId, expenseId = $expenseId, isEditMode = $isEditMode")

        _uiState.update {
            it.copy(
                initialGroupId = prefilledGroupId,
                // --- MODIFICATION HERE ---
                // If prefilledGroupId is null OR "non_group", default to "non_group"
                currentGroupId = if (prefilledGroupId == null || prefilledGroupId == "non_group") "non_group" else prefilledGroupId,
                isEditMode = isEditMode
            )
        }
        Log.d("AddExpenseDebug", "ViewModel init state: initialGroupId = ${_uiState.value.initialGroupId}, currentGroupId = ${_uiState.value.currentGroupId}, isEditMode = $isEditMode")

        if (isEditMode) {
            loadExpenseForEditing(expenseId!!, prefilledGroupId)
        } else {
            loadInitialData(prefilledGroupId) // Start loading initial data
        }
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
            var preloadedGroup: Group? = null // Default to null (which will become placeholder)

            // --- START OF FIX 1 ---
            if (prefilledGroupId != null) {
                if (prefilledGroupId == "non_group") {
                    // It's non-group, set the placeholder
                    preloadedGroup = nonGroupPlaceholder
                    // initialRelevantPayers is already userAndFriendsPayers, which is correct
                } else {
                    // It's a real group ID, try to fetch it
                    preloadedGroup = try {
                        groupsRepository.getGroupFlow(prefilledGroupId).firstOrNull()
                    } catch (e: Exception) {
                        logE("Failed to fetch prefilled group $prefilledGroupId: ${e.message}")
                        null
                    }

                    if (preloadedGroup != null) {
                        // Successfully fetched real group, get its members
                        val memberProfiles = try {
                            if (preloadedGroup.members.isNotEmpty()) {
                                userRepository.getProfilesForFriends(preloadedGroup.members) // Suspend call
                            } else emptyList()
                        } catch (e: Exception) {
                            logE("Failed to fetch group members for AddExpense: ${e.message}")
                            emptyList<User>()
                        }
                        val groupMembersAsPayers = memberProfiles.map { memberUser ->
                            Payer(
                                uid = memberUser.uid,
                                name = memberUser.username.takeIf { it.isNotBlank() } ?: memberUser.fullName
                            )
                        }
                        initialRelevantPayers = groupMembersAsPayers.distinctBy { it.uid }
                    } else {
                        // Group ID passed but not found, log error and fallback to non-group
                        logE("Prefilled group ID $prefilledGroupId not found. Falling back to non-group.")
                        preloadedGroup = nonGroupPlaceholder // Fallback to placeholder
                        // initialRelevantPayers remains userAndFriendsPayers
                    }
                }
            } else {
                // No prefilled ID, default to non-group
                preloadedGroup = nonGroupPlaceholder
            }
            // --- END OF FIX 1 ---


            // Set the list of users available for selection in dialogs
            _relevantUsersForSelection.value = initialRelevantPayers

            // Initialize participants and default payer based on loaded context
            initializeParticipantsAndPayers(
                group = preloadedGroup, // Pass the loaded group (or placeholder)
                relevantPayers = initialRelevantPayers,
                currentUserPayer = currentUserPayer,
                initialLoad = true // Indicate this is the initial setup
            )

            _uiState.update { it.copy(isInitiallyLoading = false) } // Stop initial loading indicator

            // --- 4. Fetch all available groups for the selector dropdown (runs after initial setup) ---
            groupsRepository.getGroups().collect { groups ->
                _availableGroups.value = groups
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
     * Loads an existing expense for editing.
     * Pre-fills all form fields with the expense data.
     */
    private fun loadExpenseForEditing(expenseId: String, prefilledGroupId: String?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isInitiallyLoading = true, error = null) }

            try {
                // --- 1. Fetch the expense ---
                val expenseResult = expenseRepository.getExpenseById(expenseId)
                val expense = expenseResult.getOrNull()

                if (expense == null) {
                    _uiState.update {
                        it.copy(
                            isInitiallyLoading = false,
                            error = "Expense not found."
                        )
                    }
                    return@launch
                }

                logD("Loaded expense for editing: ${expense.id}, groupId: ${expense.groupId}")

                // --- 2. Fetch current user ---
                val currentUser = userRepository.getCurrentUser()
                if (currentUser == null) {
                    _uiState.update { it.copy(isInitiallyLoading = false, error = "User not logged in.") }
                    return@launch
                }
                val userProfile = userRepository.getUserProfile(currentUser.uid)
                val currentUserName = userProfile?.username?.takeIf { it.isNotBlank() }
                    ?: userProfile?.fullName?.takeIf { it.isNotBlank() }
                    ?: currentUser.email ?: "You"
                val currentUserPayer = Payer(currentUser.uid, currentUserName, isChecked = true)

                // --- 3. Load group or friends based on expense.groupId ---
                var loadedGroup: Group? = null
                val relevantUsers: List<User>

                if (expense.groupId == "non_group" || expense.groupId == null) {
                    // Non-group expense, load friends
                    loadedGroup = nonGroupPlaceholder
                    val friendIds = userRepository.getCurrentUserFriendIds()
                    relevantUsers = if (friendIds.isNotEmpty()) {
                        userRepository.getProfilesForFriends(friendIds)
                    } else {
                        emptyList()
                    }
                } else {
                    // Group expense, load group and members
                    loadedGroup = try {
                        groupsRepository.getGroupFlow(expense.groupId!!).firstOrNull()
                    } catch (e: Exception) {
                        logE("Failed to fetch group ${expense.groupId}: ${e.message}")
                        null
                    }

                    if (loadedGroup != null) {
                        relevantUsers = try {
                            if (loadedGroup.members.isNotEmpty()) {
                                userRepository.getProfilesForFriends(loadedGroup.members)
                            } else emptyList()
                        } catch (e: Exception) {
                            logE("Failed to fetch group members: ${e.message}")
                            emptyList()
                        }
                    } else {
                        logE("Group not found for expense: ${expense.groupId}")
                        _uiState.update {
                            it.copy(
                                isInitiallyLoading = false,
                                error = "Group not found for this expense."
                            )
                        }
                        return@launch
                    }
                }

                // --- 4. Create user maps for quick lookup ---
                val userMap = relevantUsers.associateBy { it.uid }

                // --- 5. Convert ExpensePayers to UI Payers ---
                val uiPayers = expense.paidBy.mapNotNull { expensePayer ->
                    val user = userMap[expensePayer.uid]
                    if (user != null) {
                        Payer(
                            uid = user.uid,
                            name = user.username.takeIf { it.isNotBlank() } ?: user.fullName,
                            amount = expensePayer.paidAmount.toString(),
                            isChecked = true
                        )
                    } else if (expensePayer.uid == currentUser.uid) {
                        // Include current user even if not in group anymore
                        Payer(
                            uid = currentUser.uid,
                            name = currentUserName,
                            amount = expensePayer.paidAmount.toString(),
                            isChecked = true
                        )
                    } else {
                        logE("User not found for payer: ${expensePayer.uid}")
                        null
                    }
                }

                // --- 6. Determine split type from expense ---
                val splitType = try {
                    SplitType.valueOf(expense.splitType)
                } catch (e: Exception) {
                    logE("Unknown split type: ${expense.splitType}, defaulting to EQUALLY")
                    SplitType.EQUALLY
                }

                // --- 7. Create all relevant users as potential participants ---
                val allPotentialUsers = if (expense.groupId == "non_group" || expense.groupId == null) {
                    (listOf(currentUserPayer) + relevantUsers.map { user ->
                        Payer(user.uid, user.username.takeIf { it.isNotBlank() } ?: user.fullName)
                    }).distinctBy { it.uid }
                } else {
                    relevantUsers.map { user ->
                        Payer(user.uid, user.username.takeIf { it.isNotBlank() } ?: user.fullName)
                    }.distinctBy { it.uid }
                }

                _relevantUsersForSelection.value = allPotentialUsers

                // --- 8. Convert ExpenseParticipants to UI Participants ---
                val participantUids = expense.participants.map { it.uid }.toSet()
                val uiParticipants = allPotentialUsers.map { potentialUser ->
                    val expenseParticipant = expense.participants.find { it.uid == potentialUser.uid }
                    if (expenseParticipant != null) {
                        Participant(
                            uid = potentialUser.uid,
                            name = potentialUser.name,
                            isChecked = true,
                            splitValue = when (splitType) {
                                SplitType.EQUALLY -> "0.00"
                                SplitType.PERCENTAGES -> expenseParticipant.initialSplitValue.toString()
                                SplitType.SHARES -> expenseParticipant.initialSplitValue.toString()
                                SplitType.UNEQUALLY -> expenseParticipant.owesAmount.toString()
                                SplitType.ADJUSTMENTS -> expenseParticipant.owesAmount.toString()
                            },
                            owesAmount = expenseParticipant.owesAmount
                        )
                    } else {
                        // User not in original split, unchecked
                        Participant(
                            uid = potentialUser.uid,
                            name = potentialUser.name,
                            isChecked = false,
                            splitValue = "0.00",
                            owesAmount = 0.0
                        )
                    }
                }

                // --- 9. Update UI state with all loaded data ---
                _uiState.update {
                    it.copy(
                        isInitiallyLoading = false,
                        description = expense.description,
                        amount = expense.totalAmount.toString(),
                        selectedGroup = loadedGroup,
                        currentGroupId = expense.groupId ?: "non_group",
                        initialGroupId = expense.groupId ?: "non_group",
                        paidByUsers = uiPayers,
                        participants = uiParticipants,
                        splitType = splitType,
                        date = expense.date,
                        memo = expense.memo,
                        imageUris = expense.imageUrls,
                        error = null
                    )
                }

                // --- 10. Fetch all available groups for the selector dropdown ---
                groupsRepository.getGroups().collect { groups ->
                    _availableGroups.value = groups
                }

                logD("Expense loaded successfully for editing")

            } catch (e: Exception) {
                logE("Error loading expense for editing: ${e.message}")
                _uiState.update {
                    it.copy(
                        isInitiallyLoading = false,
                        error = "Failed to load expense: ${e.message}"
                    )
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
        group: Group?, // The selected group (or placeholder for non-group)
        relevantPayers: List<Payer>, // Users to include as participants (group members or user+friends)
        currentUserPayer: Payer, // The current user as a Payer object
        initialLoad: Boolean = false // Flag to differentiate initial setup from manual group change
    ) {
        if (relevantPayers.isEmpty()) {
            logE("initializeParticipantsAndPayers called with empty relevantPayers list.")
            val fallbackPayers = if(relevantPayers.isEmpty()) listOf(currentUserPayer) else relevantPayers
            initializeParticipantsAndPayers(group, fallbackPayers, currentUserPayer, initialLoad)
            return
        }

        _uiState.update { currentState ->
            val initialParticipants = relevantPayers.map { payer ->
                Participant(
                    uid = payer.uid,
                    name = payer.name,
                    isChecked = true,
                    splitValue = if (currentState.splitType == SplitType.PERCENTAGES) {
                        "%.2f".format(100.0 / relevantPayers.size)
                    } else "1.0"
                )
            }

            val calculatedParticipants = calculateSplitUseCase(
                amount = currentState.amount.toDoubleOrNull() ?: 0.0,
                participants = initialParticipants,
                splitType = currentState.splitType
            )

            val defaultPayer = listOf(currentUserPayer.copy(
                amount = currentState.amount.ifEmpty { "0.00" },
                isChecked = true
            ))

            // --- START OF FIX 2 ---
            // If group is null, it means "Non-group" was just selected. Use the placeholder.
            // If group is not null, it's either a real group or the placeholder from init.
            val displayGroup = group ?: nonGroupPlaceholder
            // --- END OF FIX 2 ---

            currentState.copy(
                selectedGroup = displayGroup, // <-- Use the placeholder if group was null
                currentGroupId = displayGroup.id, // <-- Get ID from placeholder or real group
                isGroupSelectorVisible = if (initialLoad && group != null && group.id != "non_group") currentState.isGroupSelectorVisible else false, // Hide selector for non-group init
                participants = calculatedParticipants,
                paidByUsers = defaultPayer,
                error = null
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
            val userProfile = userRepository.getUserProfile(currentUser.uid)
            val currentUserName = userProfile?.username?.takeIf { it.isNotBlank() }
                ?: userProfile?.fullName?.takeIf { it.isNotBlank() }
                ?: currentUser.email ?: "You"
            val currentUserPayer = Payer(currentUser.uid, currentUserName, isChecked = true)

            val relevantPayers: List<Payer>
            // --- START OF FIX 3 ---
            val groupForState: Group? // This will be the real group or the placeholder

            if (group != null) {
                // --- Group selected ---
                groupForState = group // Use the real group
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
                groupForState = nonGroupPlaceholder // Use the placeholder
                logD("Non-group selected, fetching friends...")
                val friendProfiles = try {
                    val friendIds = userRepository.getCurrentUserFriendIds()
                    if (friendIds.isNotEmpty()) userRepository.getProfilesForFriends(friendIds) else emptyList()
                } catch (e: Exception) {
                    logE("Failed to fetch friends on non-group selection: ${e.message}")
                    emptyList<User>()
                }
                relevantPayers = (listOf(currentUserPayer) + friendProfiles.map { Payer(it.uid, it.username.takeIf { n -> n.isNotBlank() } ?: it.fullName) }).distinctBy { it.uid }
            }
            // --- END OF FIX 3 ---

            _relevantUsersForSelection.value = relevantPayers.distinctBy { it.uid }

            // Use the helper to update main screen participants and reset payer
            initializeParticipantsAndPayers(
                group = groupForState, // <-- Pass real group or placeholder
                relevantPayers = _relevantUsersForSelection.value,
                currentUserPayer = currentUserPayer,
                initialLoad = false
            )

            _uiState.update { it.copy(isLoading = false) } // Hide temporary loading
        }
    }


    // --- User Input Handlers ---
    fun onDescriptionChange(newDescription: String) {
        if (newDescription.length <= 100) {
            _uiState.update { it.copy(description = newDescription, error = null) }
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

        if (validAmount.isEmpty() || validAmount.toDoubleOrNull() != null || validAmount == ".") {
            _uiState.update { it.copy(amount = validAmount, error = null) }
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
        _uiState.update { it.copy(memo = finalMemo.trim(), isMemoDialogVisible = false) }
    }

    /**
     * Handles expense image selection from camera/gallery
     */
    fun onExpenseImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    /**
     * Removes the selected expense image
     */
    fun onRemoveExpenseImage() {
        _uiState.update { it.copy(selectedImageUri = null, uploadedImageUrl = "") }
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
            val updatedParticipants = calculateSplitUseCase(
                amount = newAmount,
                participants = state.participants,
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
            val recalculatedParticipants = calculateSplitUseCase(
                amount = state.amount.toDoubleOrNull() ?: 0.0,
                participants = updatedParticipants,
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
        val cleanValue = newValue.filter { it.isDigit() || it == '.' }
        val decimalIndex = cleanValue.indexOf('.')
        val validValue = if (decimalIndex != -1) {
            val integerPart = cleanValue.substringBefore('.')
            val decimalPart = cleanValue.substringAfter('.').filter { it.isDigit() }
            if (uiState.value.splitType == SplitType.PERCENTAGES) {
                "$integerPart.${decimalPart.take(2)}"
            } else {
                "$integerPart.${decimalPart.take(2)}"
            }
        } else {
            cleanValue
        }

        if (validValue.isEmpty() || validValue.toDoubleOrNull() != null || validValue == ".") {
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
            val finalPayers = if (wasMultiPayer && !isMultiPayerNow && checkedPayers.isNotEmpty()) {
                val singlePayer = checkedPayers.first()
                listOf(singlePayer.copy(amount = currentState.amount.ifEmpty { "0.00" }))
            } else {
                checkedPayers
            }
            currentState.copy(paidByUsers = finalPayers, error = null)
        }
    }

    /**
     * Updates the amount for a specific payer in the multi-payer selection dialog.
     */
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
            val newParticipants = currentState.participants.map { p ->
                val defaultValue = when (type) {
                    SplitType.EQUALLY -> "0.00"
                    SplitType.PERCENTAGES -> "%.2f".format(100.0 / activeCount)
                    SplitType.SHARES -> "1.0"
                    SplitType.UNEQUALLY, SplitType.ADJUSTMENTS -> "0.00"
                }
                p.copy(splitValue = defaultValue)
            }
            currentState.copy(
                splitType = type,
                participants = newParticipants,
                error = null
            )
        }
        recalculateSplit(uiState.value.amount.toDoubleOrNull() ?: 0.0, type)
    }

    /** Hides the Split Editor dialog. */
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

        val paidByTotal = if (state.paidByUsers.size == 1) totalAmount else state.paidByUsers.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
        if ((paidByTotal - totalAmount).absoluteValue > 0.015) {
            emitErrorDialog("Payment Error", "The total amount paid (MYR${"%.2f".format(paidByTotal)}) does not match the expense total (MYR${"%.2f".format(totalAmount)}). Please adjust amounts in 'Paid by'.")
            return
        }

        val finalPayersForCalc = if (state.paidByUsers.size == 1) {
            state.paidByUsers.map { it.copy(amount = totalAmount.toString()) }
        } else {
            state.paidByUsers
        }

        val finalParticipants = calculateSplitUseCase(totalAmount, state.participants, state.splitType)
        val finalAllocatedSplit = finalParticipants.filter { it.isChecked }.sumOf { it.owesAmount }
        if ((finalAllocatedSplit - totalAmount).absoluteValue > 0.015) {
            emitErrorDialog("Split Error", "The total split amount (MYR${"%.2f".format(finalAllocatedSplit)}) does not match the expense total (MYR${"%.2f".format(totalAmount)}). Please adjust the split details.")
            return
        }
        if (state.splitType == SplitType.PERCENTAGES) {
            val totalPercent = finalParticipants.filter { it.isChecked }.sumOf { it.splitValue.toDoubleOrNull() ?: 0.0 }
            if ((totalPercent - 100.0).absoluteValue > 0.1) {
                emitErrorDialog("Split Error", "Percentages must add up to 100%. Current total: ${"%.2f".format(totalPercent)}%")
                return
            }
        }


        val expensePayers = finalPayersForCalc.map { payer ->
            ExpensePayer(
                uid = payer.uid,
                paidAmount = payer.amount.toDoubleOrNull() ?: 0.0
            )
        }

        val expenseParticipants = finalParticipants.filter { it.isChecked }.map { participant ->
            ExpenseParticipant(
                uid = participant.uid,
                owesAmount = participant.owesAmount,
                initialSplitValue = participant.splitValue.toDoubleOrNull() ?: 0.0
            )
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Upload expense image if selected
            var imageUrl = state.uploadedImageUrl // Use already uploaded URL if exists
            if (state.selectedImageUri != null && imageUrl.isEmpty()) {
                _uiState.update { it.copy(isUploadingImage = true) }

                // Generate temporary expense ID for upload
                val tempExpenseId = expenseRepository.generateExpenseId()

                val uploadResult = fileStorageRepository.uploadExpenseImage(tempExpenseId, state.selectedImageUri)
                uploadResult.fold(
                    onSuccess = { url ->
                        logD("Expense image uploaded successfully: $url")
                        imageUrl = url
                        _uiState.update { it.copy(uploadedImageUrl = url, isUploadingImage = false) }
                    },
                    onFailure = { error ->
                        logE("Failed to upload expense image: ${error.message}")
                        _uiState.update { it.copy(isUploadingImage = false, isLoading = false) }
                        emitErrorDialog("Upload Failed", "Failed to upload expense image: ${error.message}")
                        return@launch
                    }
                )
            }

            // This is still correct from our last fix.
            // state.currentGroupId will be "non_group" or a real ID.
            val newExpense = Expense(
                groupId = state.currentGroupId,
                description = state.description.trim(),
                totalAmount = totalAmount,
                createdByUid = currentUser.uid,
                date = state.date,
                splitType = state.splitType.name,
                paidBy = expensePayers,
                participants = expenseParticipants,
                memo = state.memo.trim(),
                imageUrl = imageUrl // Use uploaded image URL
            )

            // --- Determine if we're creating or updating ---
            if (isEditMode && expenseId != null) {
                // --- UPDATE MODE ---
                val expenseToUpdate = newExpense.copy(id = expenseId)
                val result = expenseRepository.updateExpense(expenseToUpdate)

                result.onSuccess {
                    viewModelScope.launch {
                        try {
                            // Get actor name
                            val actorProfile = userRepository.getUserProfile(currentUser.uid)
                            val actorName = actorProfile?.username?.takeIf { it.isNotBlank() }
                                ?: actorProfile?.fullName?.takeIf { it.isNotBlank() }
                                ?: "Someone"

                            // Get all users involved
                            val allInvolvedUids = _relevantUsersForSelection.value.map { it.uid }

                            // Calculate financial impact
                            val financialImpacts = mutableMapOf<String, Double>()
                            expensePayers.forEach { payer ->
                                financialImpacts[payer.uid] = (financialImpacts[payer.uid] ?: 0.0) + payer.paidAmount
                            }
                            expenseParticipants.forEach { participant ->
                                financialImpacts[participant.uid] = (financialImpacts[participant.uid] ?: 0.0) - participant.owesAmount
                            }

                            // Log EXPENSE_UPDATED activity
                            val activity = Activity(
                                activityType = ActivityType.EXPENSE_UPDATED.name,
                                actorUid = currentUser.uid,
                                actorName = actorName,
                                involvedUids = allInvolvedUids,
                                groupId = state.currentGroupId,
                                groupName = state.selectedGroup?.name ?: "Non-group",
                                entityId = expenseId, // Reference the expense being updated
                                displayText = expenseToUpdate.description,
                                totalAmount = totalAmount,
                                financialImpacts = financialImpacts
                            )
                            activityRepository.logActivity(activity)
                            logD("Logged EXPENSE_UPDATED activity for expense $expenseId")

                        } catch (e: Exception) {
                            logE("Failed to log EXPENSE_UPDATED activity: ${e.message}")
                        }
                    }
                    Log.d("AddExpenseViewModel", "Expense updated successfully: $expenseId")
                    val isGroupDetailNav = state.initialGroupId != null && state.initialGroupId == state.currentGroupId
                    _uiEvent.emit(AddExpenseUiEvent.SaveSuccess(isGroupDetailNav))
                }.onFailure { exception ->
                    logE("Failed to update expense: ${exception.message}")
                    emitErrorDialog("Update Failed", "Could not update expense: ${exception.message}")
                    _uiState.update { it.copy(error = "Failed to update expense.") }
                }

            } else {
                // --- CREATE MODE ---
                val result = expenseRepository.addExpense(newExpense)

                result.onSuccess { createdExpenseId ->
                    viewModelScope.launch {
                        try {
                            // Get actor name
                            val actorProfile = userRepository.getUserProfile(currentUser.uid)
                            val actorName = actorProfile?.username?.takeIf { it.isNotBlank() }
                                ?: actorProfile?.fullName?.takeIf { it.isNotBlank() }
                                ?: "Someone"

                            // Get all users involved (group members or friends in non-group)
                            // _relevantUsersForSelection is the correct source of truth here
                            val allInvolvedUids = _relevantUsersForSelection.value.map { it.uid }

                            // Calculate financial impact from the lists we just created
                            val financialImpacts = mutableMapOf<String, Double>()
                            expensePayers.forEach { payer ->
                                financialImpacts[payer.uid] = (financialImpacts[payer.uid] ?: 0.0) + payer.paidAmount
                            }
                            expenseParticipants.forEach { participant ->
                                financialImpacts[participant.uid] = (financialImpacts[participant.uid] ?: 0.0) - participant.owesAmount
                            }

                            val activity = Activity(
                                activityType = ActivityType.EXPENSE_ADDED.name,
                                actorUid = currentUser.uid,
                                actorName = actorName,
                                involvedUids = allInvolvedUids,
                                groupId = state.currentGroupId, // e.g., "non_group" or real ID
                                groupName = state.selectedGroup?.name
                                    ?: "Non-group", // Get name from state
                                entityId = createdExpenseId, // Reference the new expense
                                displayText = newExpense.description, // The expense description
                                totalAmount = totalAmount,
                                financialImpacts = financialImpacts
                            )
                            activityRepository.logActivity(activity)
                            logD("Logged EXPENSE_ADDED activity for ${state.currentGroupId}")

                        } catch (e: Exception) {
                            logE("Failed to log EXPENSE_ADDED activity: ${e.message}")
                            // Do not block UI flow, just log the error
                        }
                    }
                    Log.d("AddExpenseViewModel", "Expense saved successfully with ID: $createdExpenseId")
                    val isGroupDetailNav = state.initialGroupId != null && state.initialGroupId == state.currentGroupId
                    _uiEvent.emit(AddExpenseUiEvent.SaveSuccess(isGroupDetailNav))
                }.onFailure { exception ->
                    logE("Failed to save expense: ${exception.message}")
                    emitErrorDialog("Save Failed", "Could not save expense: ${exception.message}")
                    _uiState.update { it.copy(error = "Failed to save expense.") }
                }
            }

            _uiState.update { it.copy(isLoading = false) }
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