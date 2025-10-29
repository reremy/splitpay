package com.example.splitpay.ui.groups

import android.util.Log.e
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// Represents a member displayed in the UI, including their balance within the group
data class GroupMemberViewData(
    val user: User,
    val balance: Double = 0.0 // Positive: Gets back, Negative: Owes
)

sealed interface GroupSettingsUiEvent {
    object NavigateBack : GroupSettingsUiEvent
}

// UI State for the Group Settings Screen
data class GroupSettingsUiState(
    val isLoading: Boolean = true,
    val isDeleting: Boolean = false,
    val group: Group? = null,
    val members: List<GroupMemberViewData> = emptyList(),
    val currentUserFriends: List<User> = emptyList(),
    val currentUserBalanceInGroup: Double = 0.0,
    val isCurrentUserAdmin: Boolean = false,
    val error: String? = null,
    // Dialog visibility states
    val showEditNameDialog: Boolean = false,
    val showChangeIconDialog: Boolean = false,
    val showAddMemberDialog: Boolean = false,
    val showRemoveMemberConfirmation: User? = null, // Store user to remove
    val showLeaveGroupConfirmation: Boolean = false,
    val showDeleteGroupConfirmation: Boolean = false,
    val leaveGroupErrorMessage: String? = null, // For "cannot leave due to balance"
    val cannotRemoveDialogMessage: String? = null // Message for the "Cannot Remove" dialog
)

class GroupSettingsViewModel(
    private val groupsRepository: GroupsRepository,
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupSettingsUiState())
    val uiState: StateFlow<GroupSettingsUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<GroupSettingsUiEvent>()
    val uiEvent: SharedFlow<GroupSettingsUiEvent> = _uiEvent.asSharedFlow()

    private val groupId: String = savedStateHandle["groupId"] ?: ""
    private var currentUserUid: String? = userRepository.getCurrentUser()?.uid

    private var dataLoadingJob: Job? = null // To manage the collection coroutine

    init {
        if (groupId.isBlank() || currentUserUid == null) {
            _uiState.update { it.copy(isLoading = false, error = "Group ID missing or user not logged in.") }
        } else {
            // --- Start listening for group changes ---
            listenForGroupUpdates()
        }
    }

    private fun listenForGroupUpdates() {
        dataLoadingJob?.cancel() // Cancel previous listener if any
        dataLoadingJob = viewModelScope.launch {
            groupsRepository.getGroupFlow(groupId).collectLatest { group ->
                // This block runs every time the group data changes in Firestore
                if (group == null) {
                    // Handle group deletion or not found
                    _uiState.update { it.copy(isLoading = false, group = null, error = "Group not found or was deleted.") }
                    return@collectLatest // Stop processing for this emission
                }

                // Group data received, update basic state and fetch related data
                _uiState.update { it.copy(isLoading = true, group = group, isCurrentUserAdmin = (group.createdByUid == currentUserUid)) }

                try {
                    // --- Fetch Expenses for THIS group ---
                    // Note: If ExpenseRepository provided a flow, we'd combine it earlier.
                    // Assuming a suspend function getExpensesForGroup exists for now.
                    // If not, we might need to adapt this or use the existing flow differently.
                    // For simplicity, let's assume a one-time fetch or use the flow inside.
                    // Using the flow approach:
                    expenseRepository.getExpensesFlowForGroup(groupId).collectLatest { groupExpenses ->

                        // Fetch member details (can run concurrently with expense fetch if needed)
                        val memberDetailsDeferred = async { userRepository.getProfilesForFriends(group.members) }
                        val friendIdsDeferred = async { userRepository.getCurrentUserFriendIds() } // For Add Friend dialog later

                        val memberDetails = memberDetailsDeferred.await()
                        val friendIds = friendIdsDeferred.await() // Await friend IDs
                        val friendsDeferred = async { userRepository.getProfilesForFriends(friendIds) } // Fetch friend profiles


                        // --- Calculate Balances ---
                        var calculatedCurrentUserBalance = 0.0
                        val balances = mutableMapOf<String, Double>() // Map UID to balance within this group

                        groupExpenses.forEach { expense ->
                            expense.paidBy.forEach { payer ->
                                balances[payer.uid] = (balances[payer.uid] ?: 0.0) + payer.paidAmount
                            }
                            expense.participants.forEach { participant ->
                                balances[participant.uid] = (balances[participant.uid] ?: 0.0) - participant.owesAmount
                            }
                        }

                        val membersWithBalance = memberDetails.map { user ->
                            val balance = roundToCents(balances[user.uid] ?: 0.0)
                            // Update overall current user balance calculation
                            if (user.uid == currentUserUid) {
                                calculatedCurrentUserBalance = balance
                            }
                            GroupMemberViewData(user = user, balance = balance)
                        }

                        val friends = friendsDeferred.await() // Await friend profiles

                        // Update the state with calculated balances and friends
                        _uiState.update {
                            it.copy(
                                isLoading = false, // Loading complete
                                members = membersWithBalance,
                                currentUserFriends = friends, // For Add Dialog later
                                currentUserBalanceInGroup = calculatedCurrentUserBalance, // Use calculated value
                                error = null
                            )
                        }
                    } // End expense collection
                } catch (e: Exception) {
                    logE("Error loading related data or calculating balances: ${e.message}")
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load details or calculate balances.") }
                }
            } // End group collection
        }
    }

    // --- Helper function for rounding ---
    private fun roundToCents(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }

        // --- Dialog Visibility Handlers ---
    fun showEditNameDialog(show: Boolean) = _uiState.update { it.copy(showEditNameDialog = show) }
    fun showChangeIconDialog(show: Boolean) = _uiState.update { it.copy(showChangeIconDialog = show) }
    fun showAddMemberDialog(show: Boolean) = _uiState.update { it.copy(showAddMemberDialog = show) }
    fun showRemoveMemberConfirmation(user: User?) = _uiState.update { it.copy(showRemoveMemberConfirmation = user) }
    fun showLeaveGroupConfirmation(show: Boolean) = _uiState.update { it.copy(showLeaveGroupConfirmation = show) }
    fun showDeleteGroupConfirmation(show: Boolean) = _uiState.update { it.copy(showDeleteGroupConfirmation = show) }
    fun clearLeaveGroupError() = _uiState.update { it.copy(leaveGroupErrorMessage = null) }

    fun clearCannotRemoveDialog() {
        _uiState.update { it.copy(cannotRemoveDialogMessage = null) }
    }

    // --- Action Handlers (Placeholders - Need Implementation) ---

    fun updateGroupName(newName: String) {
        val trimmedName = newName.trim()
        if (trimmedName.isBlank()) {
            _uiState.update { it.copy(error = "Group name cannot be empty.") } // Show error if blank
            return
        }
        // Dismiss dialog immediately for better UX
        showEditNameDialog(false)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // Show loading briefly
            val result = groupsRepository.updateGroupName(groupId, trimmedName) // <-- CALL REPOSITORY
            result.onSuccess {
                logD("Successfully updated group name for $groupId")
                // The listener `listenForGroupUpdates` should automatically refresh the state
                _uiState.update { it.copy(isLoading = false, error = null) } // Clear loading and error
            }.onFailure { e ->
                logE("Failed to update group name: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Failed to update name: ${e.message}") }
            }
        }
    }

    fun updateGroupIcon(newIconIdentifier: String) {
        if (newIconIdentifier.isBlank()) return // Should not happen with current UI

        // Dismiss dialog immediately
        showChangeIconDialog(false)
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // Show loading briefly
            val result = groupsRepository.updateGroupIcon(groupId, newIconIdentifier) // <-- CALL REPOSITORY
            result.onSuccess {
                logD("Successfully updated group icon for $groupId")
                // The listener should refresh the state
                _uiState.update { it.copy(isLoading = false, error = null) }
            }.onFailure { e ->
                logE("Failed to update group icon: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Failed to update icon: ${e.message}") }
            }
        }
    }

    fun addMembers(selectedFriendUids: List<String>) {
        if (selectedFriendUids.isEmpty()) return
        viewModelScope.launch {
            // TODO: Call groupsRepository.addMembersToGroup(groupId, selectedFriendUids)
            // On success, reload data? (Group listener might handle this)
            showAddMemberDialog(false)
        }
    }

    fun removeMember(memberUidToRemove: String) {
        val memberToRemove = uiState.value.members.find { it.user.uid == memberUidToRemove }

        // Rule: Check balance before removing
        if (memberToRemove != null && memberToRemove.balance.absoluteValue > 0.01) {
            // --- INSTEAD of setting general error, set the specific dialog message ---
            val message = "You can't remove ${memberToRemove.user.username} until their debts (MYR${roundToCents(memberToRemove.balance)}) are settled up."
            _uiState.update { it.copy(
                cannotRemoveDialogMessage = message, // Set message for the specific dialog
                showRemoveMemberConfirmation = null // Dismiss confirmation dialog
            )}
            return // Stop removal process
        }

        // Proceed with removal if balance is settled or member info wasn't found
        // Dismiss the confirmation dialog *before* starting the async operation
        _uiState.update { it.copy(showRemoveMemberConfirmation = null) }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) } // Show brief loading
            val result = groupsRepository.removeMemberFromGroup(groupId, memberUidToRemove) // <-- CALL REPOSITORY

            result.onSuccess {
                logD("Successfully removed member $memberUidToRemove from group $groupId")
                // Listener should automatically update the member list in the UI
                _uiState.update { it.copy(isLoading = false, error = null) }
            }.onFailure { e ->
                logE("Failed to remove member $memberUidToRemove: ${e.message}")
                // Set general error for repository failures
                _uiState.update { it.copy(isLoading = false, error = "Failed to remove member: ${e.message}") }
            }
            // No need to dismiss confirmation dialog again here
        }
    }

    fun leaveGroup() {
        val balance = uiState.value.currentUserBalanceInGroup
        if (balance != 0.0) { // Using != 0.0, adjust tolerance if needed
            _uiState.update { it.copy(leaveGroupErrorMessage = "You can't leave the group. You have outstanding debts with other group members.") }
            showLeaveGroupConfirmation(false) // Hide confirmation if it was shown
            return
        }
        viewModelScope.launch {
            // Balance is zero, proceed
            // TODO: Call groupsRepository.removeMemberFromGroup(groupId, currentUserUid!!)
            // On success, navigate back? (Need Navigation event)
            showLeaveGroupConfirmation(false)
        }
    }

    fun deleteGroup() {
        viewModelScope.launch {
            _uiState.update { it.copy(isDeleting = true) }
            try {
                // First, delete all associated expenses
                val expenseResult = expenseRepository.deleteExpensesForGroup(groupId)
                if (expenseResult.isFailure) {
                    throw expenseResult.exceptionOrNull() ?: Exception("Failed to delete expenses.")
                }

                // Second, delete the group itself
                val groupResult = groupsRepository.deleteGroup(groupId)
                if (groupResult.isFailure) {
                    throw groupResult.exceptionOrNull() ?: Exception("Failed to delete group.")
                }

                // On complete success, emit navigation event
                _uiEvent.emit(GroupSettingsUiEvent.NavigateBack)

            } catch (e: Exception) {
                logE("Error deleting group $groupId: ${e.message}")
                _uiState.update { it.copy(error = "Error deleting group: ${e.message}") }
            } finally {
                // Hide dialog and reset loading state
                _uiState.update { it.copy(isDeleting = false, showDeleteGroupConfirmation = false) }
            }
        }
    }
}