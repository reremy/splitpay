package com.example.splitpay.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logE
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Represents a member displayed in the UI, including their balance within the group
data class GroupMemberViewData(
    val user: User,
    val balance: Double = 0.0 // Positive: Gets back, Negative: Owes
)

// UI State for the Group Settings Screen
data class GroupSettingsUiState(
    val isLoading: Boolean = true,
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
)

class GroupSettingsViewModel(
    private val groupsRepository: GroupsRepository,
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupSettingsUiState())
    val uiState: StateFlow<GroupSettingsUiState> = _uiState.asStateFlow()

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
                    // Fetch member details based on the latest members list
                    val memberDetails = userRepository.getProfilesForFriends(group.members)
                    // TODO: Calculate each member's balance within this group
                    val membersWithBalance = memberDetails.map { user ->
                        GroupMemberViewData(user = user, balance = 0.0) // Placeholder balance
                    }

                    // Fetch current user's friends (for adding members) - could be optimized
                    val friendIds = userRepository.getCurrentUserFriendIds()
                    val friends = userRepository.getProfilesForFriends(friendIds)

                    // TODO: Calculate current user's balance within this group
                    val currentUserBalance = 0.0 // Placeholder balance

                    // Update the rest of the state
                    _uiState.update {
                        it.copy(
                            isLoading = false, // Loading complete for this update cycle
                            members = membersWithBalance,
                            currentUserFriends = friends,
                            currentUserBalanceInGroup = currentUserBalance,
                            error = null // Clear previous errors on successful update
                        )
                    }
                } catch (e: Exception) {
                    logE("Error loading related data after group update: ${e.message}")
                    _uiState.update { it.copy(isLoading = false, error = "Failed to load member/friend data.") }
                }
            }
        }
    }

    // --- Dialog Visibility Handlers ---
    fun showEditNameDialog(show: Boolean) = _uiState.update { it.copy(showEditNameDialog = show) }
    fun showChangeIconDialog(show: Boolean) = _uiState.update { it.copy(showChangeIconDialog = show) }
    fun showAddMemberDialog(show: Boolean) = _uiState.update { it.copy(showAddMemberDialog = show) }
    fun showRemoveMemberConfirmation(user: User?) = _uiState.update { it.copy(showRemoveMemberConfirmation = user) }
    fun showLeaveGroupConfirmation(show: Boolean) = _uiState.update { it.copy(showLeaveGroupConfirmation = show) }
    fun showDeleteGroupConfirmation(show: Boolean) = _uiState.update { it.copy(showDeleteGroupConfirmation = show) }
    fun clearLeaveGroupError() = _uiState.update { it.copy(leaveGroupErrorMessage = null) }

    // --- Action Handlers (Placeholders - Need Implementation) ---

    fun updateGroupName(newName: String) {
        if (newName.isBlank()) return
        viewModelScope.launch {
            // TODO: Call groupsRepository.updateGroupName(groupId, newName)
            // On success, update group in state or reload data
            showEditNameDialog(false)
        }
    }

    fun updateGroupIcon(newIcon: String) {
        viewModelScope.launch {
            // TODO: Call groupsRepository.updateGroupIcon(groupId, newIcon)
            // On success, update group in state or reload data
            showChangeIconDialog(false)
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

    fun removeMember(memberUid: String) {
        viewModelScope.launch {
            // TODO: Check member balance first if required by rules
            // TODO: Call groupsRepository.removeMemberFromGroup(groupId, memberUid)
            // On success, reload data? (Group listener might handle this)
            showRemoveMemberConfirmation(null) // Dismiss confirmation
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
            // TODO: Call groupsRepository.deleteGroup(groupId)
            // TODO: Call expenseRepository.deleteExpensesForGroup(groupId)
            // On success, navigate back? (Need Navigation event)
            showDeleteGroupConfirmation(false)
        }
    }
}