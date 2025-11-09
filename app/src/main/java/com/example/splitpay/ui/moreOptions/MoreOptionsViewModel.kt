package com.example.splitpay.ui.moreOptions

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logE
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MoreOptionsUiState(
    val isLoading: Boolean = true,
    val group: Group? = null,
    val members: List<User> = emptyList(),
    val selectedPayer: User? = null,
    val selectedRecipient: User? = null,
    val selectionStep: SelectionStep = SelectionStep.SELECT_PAYER,
    val error: String? = null
)

enum class SelectionStep {
    SELECT_PAYER,
    SELECT_RECIPIENT
}

sealed interface MoreOptionsUiEvent {
    object NavigateBack : MoreOptionsUiEvent
    data class NavigateToRecordPayment(val groupId: String, val payerUid: String, val recipientUid: String) : MoreOptionsUiEvent
}

class MoreOptionsViewModel(
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(MoreOptionsUiState())
    val uiState: StateFlow<MoreOptionsUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<MoreOptionsUiEvent>()
    val uiEvent: SharedFlow<MoreOptionsUiEvent> = _uiEvent.asSharedFlow()

    private val groupId: String = savedStateHandle["groupId"] ?: ""

    init {
        if (groupId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Invalid group ID") }
        } else {
            loadGroupAndMembers()
        }
    }

    private fun loadGroupAndMembers() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val group = groupsRepository.getGroupFlow(groupId).firstOrNull()

                if (group == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Group not found") }
                    return@launch
                }

                // Fetch member profiles
                val members = userRepository.getProfilesForFriends(group.members)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        group = group,
                        members = members,
                        error = null
                    )
                }

            } catch (e: Exception) {
                logE("Failed to load group and members: ${e.message}")
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load group: ${e.message}")
                }
            }
        }
    }

    fun onMemberSelected(member: User) {
        val currentState = _uiState.value

        when (currentState.selectionStep) {
            SelectionStep.SELECT_PAYER -> {
                _uiState.update {
                    it.copy(
                        selectedPayer = member,
                        selectionStep = SelectionStep.SELECT_RECIPIENT
                    )
                }
            }
            SelectionStep.SELECT_RECIPIENT -> {
                _uiState.update { it.copy(selectedRecipient = member) }

                // Navigate to RecordPayment
                viewModelScope.launch {
                    _uiEvent.emit(
                        MoreOptionsUiEvent.NavigateToRecordPayment(
                            groupId = groupId,
                            payerUid = currentState.selectedPayer!!.uid,
                            recipientUid = member.uid
                        )
                    )
                }
            }
        }
    }

    fun onBackClick() {
        val currentState = _uiState.value

        when (currentState.selectionStep) {
            SelectionStep.SELECT_PAYER -> {
                // Go back to previous screen
                viewModelScope.launch {
                    _uiEvent.emit(MoreOptionsUiEvent.NavigateBack)
                }
            }
            SelectionStep.SELECT_RECIPIENT -> {
                // Go back to payer selection
                _uiState.update {
                    it.copy(
                        selectedPayer = null,
                        selectedRecipient = null,
                        selectionStep = SelectionStep.SELECT_PAYER
                    )
                }
            }
        }
    }
}
