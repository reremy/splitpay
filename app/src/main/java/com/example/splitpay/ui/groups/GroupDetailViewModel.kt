package com.example.splitpay.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.repository.GroupsRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- NEW: Define UI State Data Class ---
data class GroupDetailUiState(
    val group: Group? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    //val isSettingsMenuVisible: Boolean = false,
    // Add other states later (isCurrentUserAdmin, friendsList, membersList, etc.)
)

class GroupDetailViewModel(
    private val repository: GroupsRepository = GroupsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private var groupIdToLoad: String? = null
    private var groupListenerJob: Job? = null

    /**
     * Call this function from the UI (e.g., LaunchedEffect) to start loading/listening.
     * Pass the groupId from navigation arguments.
     */
    fun loadAndListenForGroupUpdates(groupId: String) {
        if (groupId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Group ID is invalid.") }
            return
        }
        // If already listening for the same group, do nothing
        if (groupIdToLoad == groupId && groupListenerJob?.isActive == true) {
            return
        }
        groupIdToLoad = groupId
        groupListenerJob?.cancel() // Cancel previous listener if any

        groupListenerJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) } // Set loading state
            repository.getGroupFlow(groupId).collectLatest { group ->
                if (group == null) {
                    _uiState.update { it.copy(isLoading = false, group = null, error = "Group not found or deleted.") }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            group = group,
                            error = null // Clear error on successful fetch/update
                        )
                    }
                    // TODO: Determine if current user is admin based on group.createdByUid
                    // TODO: Fetch expenses for this group to display activity list/balances later
                }
            }
        }
    }


    // --- Placeholder functions for menu actions (to be implemented later) ---
    fun onEditNameClicked() { /* TODO */ }
    fun onChangeIconClicked() { /* TODO */ }
    fun onAddMembersClicked() { /* TODO */ }
    fun onRemoveMembersClicked() { /* TODO */ }
    fun onLeaveGroupClicked() { /* TODO */ }
    fun onDeleteGroupClicked() { /* TODO */ }
}