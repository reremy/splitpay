package com.example.splitpay.ui.groups

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.repository.GroupsRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

sealed interface CreateGroupUiEvent {
    // Pass the newly created Group ID to navigate to its detail page
    data class GroupCreated(val groupId: String) : CreateGroupUiEvent
    object NavigateBack : CreateGroupUiEvent
}

data class CreateGroupUiState(
    val groupName: String = "",
    val selectedIcon: String = "group", // Default icon
    val isLoading: Boolean = false,
    val error: String? = null
)
class CreateGroupViewModel(
    private val repository: GroupsRepository = GroupsRepository()
) : ViewModel() {

    private val _uiState = mutableStateOf(CreateGroupUiState())
    val uiState: State<CreateGroupUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<CreateGroupUiEvent>()
    val uiEvent: SharedFlow<CreateGroupUiEvent> = _uiEvent.asSharedFlow()

    fun onGroupNameChange(newName: String) {
        _uiState.value = _uiState.value.copy(groupName = newName, error = null)
    }

    fun onIconSelected(iconIdentifier: String) {
        _uiState.value = _uiState.value.copy(selectedIcon = iconIdentifier)
    }

    fun onCreateGroupClick() {
        val name = _uiState.value.groupName.trim()
        val icon = _uiState.value.selectedIcon

        if (name.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Group name cannot be empty")
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            // Pass the new iconIdentifier to the repository
            repository.createGroup(name,    icon)
                .onSuccess { newGroup ->
                    // Navigate to the newly created group's detail page
                    _uiEvent.emit(CreateGroupUiEvent.GroupCreated(newGroup.id))
                }
                .onFailure { error ->
                    _uiState.value = _uiState.value.copy(
                        error = error.message ?: "Failed to create group",
                        isLoading = false
                    )
                }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }
}