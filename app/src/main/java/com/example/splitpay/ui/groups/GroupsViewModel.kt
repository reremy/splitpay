package com.example.splitpay.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.GroupWithBalance
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

sealed interface GroupsUiEvent {
    object NavigateToCreateGroup : GroupsUiEvent
    data class NavigateToGroupDetail(val groupId: String) : GroupsUiEvent
}

data class GroupsUiState(
    val isLoading: Boolean = true,
    val groupsWithBalances: List<GroupWithBalance> = emptyList(),
    val error: String? = null,
    val totalNetBalance: Double = 0.0, // Overall balance across all friends
)

class GroupsViewModel(
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupsUiState())
    val uiState: StateFlow<GroupsUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<GroupsUiEvent>()
    val uiEvent: SharedFlow<GroupsUiEvent> = _uiEvent

    private var currentUserUid: String? = userRepository.getCurrentUser()?.uid // <-- Get current user ID

    init {
        logD("GroupsViewModel initialized. Starting group collection.")
        if (currentUserUid == null) {
            _uiState.update { it.copy(isLoading = false, error = "User not logged in.") }
        } else {
            collectGroupsAndBalances() // Renamed function
        }
    }

    // Renamed function to reflect balance calculation
    private fun collectGroupsAndBalances() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentUid = currentUserUid!! // Safe non-null access here

            try {
                // Combine the flow of groups with fetched expenses
                groupsRepository.getGroups().collect { groups ->
                    _uiState.update { it.copy(isLoading = true) } // Set loading true while calculating

                    if (groups.isEmpty()) {
                        _uiState.update { it.copy(isLoading = false, groupsWithBalances = emptyList(), totalNetBalance = 0.0) }
                        return@collect // Exit if no groups
                    }

                    // Fetch expenses for all groups concurrently
                    val groupExpensesMap = fetchExpensesForGroups(groups.map { it.id })

                    var overallNetBalance = 0.0
                    val groupsWithCalculatedBalances = groups.map { group ->
                        val expensesInGroup = groupExpensesMap[group.id] ?: emptyList()
                        var groupNetBalance = 0.0

                        expensesInGroup.forEach { expense ->
                            // Calculate user's net contribution FOR THIS EXPENSE
                            val paidByUser = expense.paidBy.find { it.uid == currentUid }?.paidAmount ?: 0.0
                            val shareOwedByUser = expense.participants.find { it.uid == currentUid }?.owesAmount ?: 0.0
                            groupNetBalance += (paidByUser - shareOwedByUser)
                        }

                        val roundedGroupNetBalance = roundToCents(groupNetBalance)
                        overallNetBalance += roundedGroupNetBalance // Add group balance to overall total

                        // TODO: Implement simplified breakdown calculation if needed later
                        val breakdown = emptyMap<String, Double>()

                        GroupWithBalance(
                            group = group,
                            userNetBalance = roundedGroupNetBalance, // Assign calculated balance
                            simplifiedOwedBreakdown = breakdown
                        )
                    }.sortedByDescending { it.userNetBalance.absoluteValue } // Sort by balance magnitude

                    val roundedOverallNetBalance = roundToCents(overallNetBalance)

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            groupsWithBalances = groupsWithCalculatedBalances,
                            totalNetBalance = roundedOverallNetBalance, // Update total balance
                            error = null
                        )
                    }
                } // End collect groups
            } catch (e: Exception) {
                logE("Error collecting groups and calculating balances: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load group balances.") }
            }
        } // End launch
    }

    // Helper function to fetch expenses for multiple groups
    private suspend fun fetchExpensesForGroups(groupIds: List<String>): Map<String, List<Expense>> {
        if (groupIds.isEmpty()) return emptyMap()
        return try {
            // Fetch all expenses related to these groups in one go (or chunked)
            val allExpenses = expenseRepository.getExpensesForGroups(groupIds)
            // Group expenses by their groupId
            allExpenses.groupBy { it.groupId ?: "" } // Group by non-null groupId
        } catch (e: Exception) {
            logE("Error fetching expenses for groups: ${e.message}")
            emptyMap() // Return empty map on error
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

    // Helper function for rounding
    private fun roundToCents(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }


}