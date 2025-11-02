package com.example.splitpay.ui.groups

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.ExpenseType
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.GroupWithBalance
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.flatten
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
    val membersMap: Map<String, User> = emptyMap()
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
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectGroupsAndBalances() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentUid = currentUserUid!!

            try {
                // Combine groups flow and the flow of all relevant expenses
                combine(
                    groupsRepository.getGroups(),
                    groupsRepository.getGroups().flatMapLatest { groups ->
                        if (groups.isEmpty()) {
                            flowOf(emptyList<Expense>())
                        } else {
                            val expenseFlows = groups.map { expenseRepository.getExpensesFlowForGroup(it.id) }
                            combine(expenseFlows) { arrayOfExpenseLists ->
                                arrayOfExpenseLists.asList().flatten()
                            }
                        }
                    }
                ) { groups, allExpenses ->
                    _uiState.update { it.copy(isLoading = true) } // Show loading during recalc

                    // --- CORRECTED BLOCK ---
                    if (groups.isEmpty()) {
                        // Instead of just updating state and returning Unit,
                        // return the empty Pair that collectLatest expects.
                        Pair(emptyList<GroupWithBalance>(), emptyMap<String, User>())
                        // --- END CORRECTION ---
                    } else {
                        // --- Fetch All Member Details Once ---
                        val allMemberUids = groups.flatMap { it.members }.distinct()
                        val membersMap = if (allMemberUids.isNotEmpty()) {
                            userRepository.getProfilesForFriends(allMemberUids).associateBy { it.uid }
                        } else {
                            emptyMap()
                        }
                        // --- End Member Fetch ---

                        val groupExpensesMap = allExpenses.groupBy { it.groupId ?: "" }

                        val groupsWithCalculatedBalances = groups.map { group ->
                            // ... (rest of the calculation logic remains the same) ...
                            val expensesInGroup = groupExpensesMap[group.id] ?: emptyList()
                            var groupNetBalance = 0.0
                            val memberBalancesInGroup = mutableMapOf<String, Double>()

                            expensesInGroup.forEach { expense ->
                                expense.paidBy.forEach { payer ->
                                    memberBalancesInGroup[payer.uid] = (memberBalancesInGroup[payer.uid] ?: 0.0) + payer.paidAmount
                                }
                                expense.participants.forEach { participant ->
                                    memberBalancesInGroup[participant.uid] = (memberBalancesInGroup[participant.uid] ?: 0.0) - participant.owesAmount
                                }
                            }

                            groupNetBalance = roundToCents(memberBalancesInGroup[currentUid] ?: 0.0)

                            val breakdown = mutableMapOf<String, Double>()
                            group.members.forEach { memberUid ->
                                if (memberUid != currentUid) {
                                    var balanceWithMember = 0.0
                                    expensesInGroup.filter { exp ->
                                        val involvedUids = (exp.paidBy.map { it.uid } + exp.participants.map { it.uid }).toSet()
                                        involvedUids.contains(currentUid) && involvedUids.contains(memberUid)
                                    }.forEach { relevantExpense ->
                                        val currentUserPaid = relevantExpense.paidBy.find { it.uid == currentUid }?.paidAmount ?: 0.0
                                        val currentUserOwes = relevantExpense.participants.find { it.uid == currentUid }?.owesAmount ?: 0.0
                                        val memberPaid = relevantExpense.paidBy.find { it.uid == memberUid }?.paidAmount ?: 0.0
                                        val memberOwes = relevantExpense.participants.find { it.uid == memberUid }?.owesAmount ?: 0.0
                                        if (relevantExpense.expenseType == ExpenseType.PAYMENT) {
                                            // This is a direct payment (like a Settle Up)
                                            if (currentUserPaid > 0) {
                                                // I paid the member
                                                balanceWithMember += currentUserPaid
                                            } else if (memberPaid > 0) {
                                                // The member paid me
                                                balanceWithMember -= memberPaid
                                            }
                                        } else {
                                            // This is a shared expense, use the split logic
                                            val currentUserNet = currentUserPaid - currentUserOwes
                                            val memberNet = memberPaid - memberOwes
                                            val numParticipants = relevantExpense.participants.size.toDouble().coerceAtLeast(1.0)
                                            balanceWithMember += (currentUserNet - memberNet) / numParticipants
                                        }
                                    }
                                    val roundedBalanceWithMember = roundToCents(balanceWithMember)
                                    if (roundedBalanceWithMember.absoluteValue > 0.01) {
                                        breakdown[memberUid] = roundedBalanceWithMember
                                    }
                                }
                            }

                            GroupWithBalance(
                                group = group,
                                userNetBalance = groupNetBalance,
                                simplifiedOwedBreakdown = breakdown
                            )
                        }.sortedByDescending { it.userNetBalance.absoluteValue }

                        // Return calculated data and membersMap
                        Pair(groupsWithCalculatedBalances, membersMap)
                    }

                }.collectLatest { (calculatedGroups, membersMap) -> // Collect the Pair
                    // This block will now *always* receive a Pair
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            groupsWithBalances = calculatedGroups,
                            membersMap = membersMap,
                            error = null
                        )
                    }
                }

            } catch (e: Exception) {
                logE("Error collecting groups/balances: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load group balances.") }
            }
        }
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