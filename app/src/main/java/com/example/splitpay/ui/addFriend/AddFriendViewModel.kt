package com.example.splitpay.ui.addfriend

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// UI State for AddFriendScreen
data class AddFriendUiState(
    val searchQuery: String = "",
    val searchResults: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val infoMessage: String? = null // For messages like "No users found" or initial prompt
)

// One-time events from ViewModel to UI
sealed interface AddFriendUiEvent {
    data class NavigateToProfilePreview(val userId: String, val username: String) : AddFriendUiEvent
    data class ShowSnackbar(val message: String) : AddFriendUiEvent // This might be better handled on the screen receiving the result
    object NavigateBack : AddFriendUiEvent
}

class AddFriendViewModel(
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddFriendUiState(infoMessage = "Enter a username to search.")) // Initial prompt
    val uiState: StateFlow<AddFriendUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<AddFriendUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private var searchJob: Job? = null
    private val searchDelayMillis = 300L // Delay before triggering search after typing stops

    fun onSearchQueryChange(query: String) {
        val trimmedQuery = query.trimStart() // Trim leading whitespace only
        _uiState.update { it.copy(searchQuery = trimmedQuery, infoMessage = null, error = null) }
        searchJob?.cancel() // Cancel previous delayed job

        if (trimmedQuery.isBlank()) {
            _uiState.update { it.copy(searchResults = emptyList(), isLoading = false, infoMessage = "Enter a username to search.") } // Reset to initial prompt
            return
        }

        // Debounce: Wait a bit after typing stops before searching
        searchJob = viewModelScope.launch {
            delay(searchDelayMillis)
            performSearch(trimmedQuery)
        }
    }

    private suspend fun performSearch(query: String) {
        _uiState.update { it.copy(isLoading = true, infoMessage = null, error = null) }
        try {
            val results = userRepository.searchUsersByUsername(query, limit = 5)
            logD("Search results for '$query': ${results.size} users found.")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    searchResults = results,
                    // Show message only if query is not blank and results are empty
                    infoMessage = if (results.isEmpty() && query.isNotBlank()) "No users found matching '$query'" else null
                )
            }
        } catch (e: Exception) {
            logE("Error during user search: ${e.message}")
            _uiState.update { it.copy(isLoading = false, searchResults = emptyList(), error = "Search failed. Please try again.") }
        }
    }

    fun onUserSelected(user: User) {
        logD("User selected for preview: ${user.username} (UID: ${user.uid})")
        viewModelScope.launch {
            // Navigate to a preview screen where the actual adding happens
            _uiEvent.emit(AddFriendUiEvent.NavigateToProfilePreview(user.uid, user.username))
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.emit(AddFriendUiEvent.NavigateBack)
        }
    }

    // Note: The addFriend logic will reside in the FriendProfilePreviewViewModel
}