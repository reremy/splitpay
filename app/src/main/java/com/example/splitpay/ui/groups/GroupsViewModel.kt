package com.example.splitpay.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.GroupWithBalance
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.logger.logD
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface GroupsUiEvent {
    object NavigateToCreateGroup : GroupsUiEvent
    data class NavigateToGroupDetail(val groupId: String) : GroupsUiEvent
}

data class GroupsUiState(
    val isLoading: Boolean = true,
    val groupsWithBalances: List<GroupWithBalance> = emptyList(),
    val overallOwedBalance: Double = 0.0,
    val error: String? = null,
    val totalNetBalance: Double = 0.0, // Overall balance across all friends
)

class GroupsViewModel(
    private val groupsRepository: GroupsRepository = GroupsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<GroupsUiEvent>()
    val uiEvent: SharedFlow<GroupsUiEvent> = _uiEvent

    init {
        logD("GroupsViewModel initialized. Starting group collection.")
        collectGroups()
    }

    private fun collectGroups() {
        viewModelScope.launch {
            // TODO: In a real app, this should be a Flow combining Group flow with Expense flow
            // to dynamically calculate the balance. For now, we simulate the balance calculation.
            groupsRepository.getGroups()
                .collect { groups ->
                    // Simplified calculation simulation for UI display
                    val groupsWithBalances = groups.map { group ->
                        // Placeholder: Generate a random balance for demonstration
                        val balance = 0.0

                        // Placeholder: Create a fake breakdown for demonstration
                        val breakdown = if (balance > 0) mapOf("FakeUserUID1" to balance / 2, "FakeUserUID2" to balance / 2) else emptyMap()

                        GroupWithBalance(
                            group = group,
                            userNetBalance = 0.0,
                            simplifiedOwedBreakdown = breakdown
                        )
                    }.sortedByDescending { it.userNetBalance }

                    val overallOwedBalance = groupsWithBalances
                        .filter { it.userNetBalance > 0 }
                        .sumOf { it.userNetBalance }

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            groupsWithBalances = groupsWithBalances,
                            overallOwedBalance = overallOwedBalance,
                            error = null
                        )
                    }
                }
        }
    }

    fun onGroupCardClick(groupId: String) {
        viewModelScope.launch {
            _uiEvent.emit(GroupsUiEvent.NavigateToGroupDetail(groupId))
        }
    }

    fun onCreateGroupClick() {
        viewModelScope.launch {
            _uiEvent.emit(GroupsUiEvent.NavigateToCreateGroup)
        }
    }


}