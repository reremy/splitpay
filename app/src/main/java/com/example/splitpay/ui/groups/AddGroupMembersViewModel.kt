package com.example.splitpay.ui.groups

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

// UI State for AddGroupMembersScreen
data class AddGroupMembersUiState(
    val isLoading: Boolean = true,
    val group: Group? = null, // To know current members
    val allFriends: List<User> = emptyList(), // Full list of user's friends
    val availableFriends: List<User> = emptyList(), // Filtered list based on search/existing members
    val selectedFriends: List<User> = emptyList(), // Friends currently staged for adding
    val searchQuery: String = "",
    val error: String? = null,
    val addMembersSuccess: Boolean = false // Flag to trigger navigation back
)

class AddGroupMembersViewModel(
    private val groupsRepository: GroupsRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddGroupMembersUiState())
    val uiState: StateFlow<AddGroupMembersUiState> = _uiState.asStateFlow()

    private val groupId: String = savedStateHandle["groupId"] ?: ""
    private var searchJob: Job? = null

    init {
        if (groupId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Group ID is missing.") }
        } else {
            loadInitialData()
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                // Fetch group details to know current members
                val group = groupsRepository.getGroupFlow(groupId).firstOrNull()
                if (group == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Group not found.") }
                    return@launch
                }

                // Fetch all friends
                val friendIds = userRepository.getCurrentUserFriendIds()
                val friends = userRepository.getProfilesForFriends(friendIds)

                // Filter out friends already in the group
                val currentMemberIds = group.members.toSet()
                val available = friends.filterNot { currentMemberIds.contains(it.uid) }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        group = group,
                        allFriends = friends, // Store full list
                        availableFriends = available // Initially show all available
                    )
                }
            } catch (e: Exception) {
                logE("Error loading data for AddGroupMembers: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load friend list.") }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(300L) // Debounce
            filterAvailableFriends()
        }
    }

    private fun filterAvailableFriends() {
        _uiState.update { state ->
            val groupMemberIds = state.group?.members?.toSet() ?: emptySet()
            val available = state.allFriends.filterNot { groupMemberIds.contains(it.uid) }

            val filtered = if (state.searchQuery.isBlank()) {
                available
            } else {
                available.filter {
                    it.username.contains(state.searchQuery, ignoreCase = true) ||
                            it.fullName.contains(state.searchQuery, ignoreCase = true)
                }
            }
            state.copy(availableFriends = filtered)
        }
    }

    fun onFriendSelected(friend: User) {
        _uiState.update { state ->
            if (state.selectedFriends.any { it.uid == friend.uid }) {
                state // Already selected, do nothing (or could deselect here)
            } else {
                state.copy(selectedFriends = state.selectedFriends + friend)
            }
        }
    }

    fun onFriendDeselected(friend: User) {
        _uiState.update { state ->
            state.copy(selectedFriends = state.selectedFriends.filterNot { it.uid == friend.uid })
        }
    }

    fun onDoneClick() {
        val friendsToAdd = uiState.value.selectedFriends
        if (friendsToAdd.isEmpty()) {
            // Maybe show a message? Or just navigate back.
            _uiState.update { it.copy(addMembersSuccess = true) } // Navigate back even if none selected
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) } // Show loading indicator on button?
            val result = groupsRepository.addMembersToGroup(groupId, friendsToAdd.map { it.uid })
            result.onSuccess {
                logD("Successfully added members to group $groupId")
                _uiState.update { it.copy(isLoading = false, addMembersSuccess = true) }
            }.onFailure { e ->
                logE("Failed to add members: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Failed to add members.") }
            }
        }
    }
}