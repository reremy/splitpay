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

enum class GroupFilter {
    ALL_GROUPS,
    OUTSTANDING_BALANCES,
    GROUPS_YOU_OWE,
    GROUPS_THAT_OWE_YOU
}

data class GroupsUiState(
    val isLoading: Boolean = true,
    val groupsWithBalances: List<GroupWithBalance> = emptyList(),
    val filteredGroupsWithBalances: List<GroupWithBalance> = emptyList(),
    // --- START OF FIX 1 ---
    val nonGroupBalance: Double = 0.0, // <-- ADD THIS FIELD
    // --- END OF FIX 1 ---
    val error: String? = null,
    val membersMap: Map<String, User> = emptyMap(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedFilter: GroupFilter = GroupFilter.ALL_GROUPS,
    val showFilterDropdown: Boolean = false,
    val showSettledGroups: Boolean = false, // Toggle to show/hide settled groups
    val settledGroupsCount: Int = 0 // Count of hidden settled groups
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
                // --- START OF FIX 2 ---
                // Combine groups flow, the flow of all RELEVANT group expenses, AND the non-group expenses flow
                combine(
                    groupsRepository.getGroups(), // Flow<List<Group>>
                    groupsRepository.getGroups().flatMapLatest { groups -> // Flow<List<Expense>> (for groups)
                        if (groups.isEmpty()) {
                            flowOf(emptyList<Expense>())
                        } else {
                            val expenseFlows = groups.map { expenseRepository.getExpensesFlowForGroup(it.id) }
                            combine(expenseFlows) { arrayOfExpenseLists ->
                                arrayOfExpenseLists.asList().flatten()
                            }
                        }
                    },
                    expenseRepository.getExpensesFlowForGroup("non_group") // Flow<List<Expense>> (for non-group)
                ) { groups, allGroupExpenses, nonGroupExpenses -> // <-- Added nonGroupExpenses
                    // --- END OF FIX 2 ---
                    _uiState.update { it.copy(isLoading = true) } // Show loading during recalc

                    // --- START OF FIX 3 ---
                    // 1. Calculate Non-Group Balance
                    var calculatedNonGroupBalance = 0.0
                    nonGroupExpenses.forEach { expense ->
                        val userPaid = expense.paidBy.find { it.uid == currentUid }?.paidAmount ?: 0.0
                        val userOwed = expense.participants.find { it.uid == currentUid }?.owesAmount ?: 0.0
                        calculatedNonGroupBalance += (userPaid - userOwed)
                    }
                    val finalNonGroupBalance = roundToCents(calculatedNonGroupBalance)
                    // --- END OF FIX 3 ---


                    // --- 2. Calculate Group Balances (existing logic) ---
                    val (calculatedGroups, membersMap) = if (groups.isEmpty()) {
                        // Return empty pair if no groups
                        Pair(emptyList<GroupWithBalance>(), emptyMap<String, User>())
                    } else {
                        // --- Fetch All Member Details Once (with caching) ---
                        val allMemberUids = groups.flatMap { it.members }.distinct()
                        val membersMap = if (allMemberUids.isNotEmpty()) {
                            userRepository.getProfilesForFriendsCached(allMemberUids).associateBy { it.uid }
                        } else {
                            emptyMap()
                        }
                        // --- End Member Fetch ---

                        val groupExpensesMap = allGroupExpenses.groupBy { it.groupId ?: "" }

                        val groupsWithCalculatedBalances = groups.map { group ->
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
                                            if (currentUserPaid > 0) {
                                                balanceWithMember += currentUserPaid
                                            } else if (memberPaid > 0) {
                                                balanceWithMember -= memberPaid
                                            }
                                        } else {
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

                    // --- START OF FIX 4 ---
                    // 3. Return a Triple containing all data
                    Triple(calculatedGroups, membersMap, finalNonGroupBalance)
                    // --- END OF FIX 4 ---

                }.collectLatest { (calculatedGroups, membersMap, nonGroupBalance) -> // <-- Collect the Triple
                    // --- START OF FIX 5 ---
                    // This block will now *always* receive a Triple
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            groupsWithBalances = calculatedGroups,
                            membersMap = membersMap,
                            nonGroupBalance = nonGroupBalance, // <-- Update the state
                            error = null
                        )
                    }
                    // Apply search and filter after updating groups
                    applySearchAndFilter()
                    // --- END OF FIX 5 ---
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

    // Search and Filter Functions
    fun onSearchIconClick() {
        _uiState.update { it.copy(isSearchActive = true) }
    }

    fun onSearchCloseClick() {
        _uiState.update { it.copy(isSearchActive = false, searchQuery = "") }
        applySearchAndFilter()
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        applySearchAndFilter()
    }

    fun onFilterSelected(filter: GroupFilter) {
        _uiState.update { it.copy(selectedFilter = filter, showFilterDropdown = false) }
        applySearchAndFilter()
    }

    fun toggleFilterDropdown() {
        _uiState.update { it.copy(showFilterDropdown = !it.showFilterDropdown) }
    }

    fun toggleShowSettledGroups() {
        _uiState.update { it.copy(showSettledGroups = !it.showSettledGroups) }
        applySearchAndFilter()
    }

    private fun applySearchAndFilter() {
        val currentState = _uiState.value
        var filteredGroups = currentState.groupsWithBalances

        // Apply search filter
        if (currentState.searchQuery.isNotEmpty()) {
            filteredGroups = filteredGroups.filter { groupWithBalance ->
                groupWithBalance.group.name.contains(currentState.searchQuery, ignoreCase = true)
            }
        }

        // Apply balance filter
        filteredGroups = when (currentState.selectedFilter) {
            GroupFilter.ALL_GROUPS -> filteredGroups
            GroupFilter.OUTSTANDING_BALANCES -> filteredGroups.filter {
                it.userNetBalance.absoluteValue > 0.01
            }
            GroupFilter.GROUPS_YOU_OWE -> filteredGroups.filter {
                it.userNetBalance < -0.01
            }
            GroupFilter.GROUPS_THAT_OWE_YOU -> filteredGroups.filter {
                it.userNetBalance > 0.01
            }
        }

        // Filter settled groups (if toggle is off)
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000 // 30 days in milliseconds
        val currentTime = System.currentTimeMillis()

        val (settledGroups, activeGroups) = filteredGroups.partition { groupWithBalance ->
            val settledDate = groupWithBalance.group.settledDate
            val isBalanceSettled = groupWithBalance.userNetBalance.absoluteValue < 0.01

            // Group is considered "settled and old" if:
            // 1. It has a settledDate
            // 2. The settledDate is more than 30 days ago
            // 3. The balance is currently settled (near zero)
            settledDate != null &&
            (currentTime - settledDate) > thirtyDaysInMillis &&
            isBalanceSettled
        }

        val finalFilteredGroups = if (currentState.showSettledGroups) {
            filteredGroups // Show all groups
        } else {
            activeGroups // Hide settled groups
        }

        _uiState.update {
            it.copy(
                filteredGroupsWithBalances = finalFilteredGroups,
                settledGroupsCount = settledGroups.size
            )
        }
    }
}