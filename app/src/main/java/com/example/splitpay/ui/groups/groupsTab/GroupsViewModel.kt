package com.example.splitpay.ui.groups.groupsTab

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.ExpenseType
import com.example.splitpay.data.model.GroupWithBalance
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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
    val nonGroupBalance: Double = 0.0,
    val error: String? = null,
    val membersMap: Map<String, User> = emptyMap(),
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val selectedFilter: GroupFilter = GroupFilter.ALL_GROUPS,
    val showFilterDropdown: Boolean = false,
    val showSettledGroups: Boolean = false,
    val settledGroupsCount: Int = 0
)

class GroupsViewModel(
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val userRepository: UserRepository = UserRepository()
) : ViewModel() {

    //private uistate for VM to change
    private val _uiState = MutableStateFlow(GroupsUiState())
    //public uistate for ui to observe
    val uiState: StateFlow<GroupsUiState> = _uiState

    //private and public same as above but for events
    private val _uiEvent = MutableSharedFlow<GroupsUiEvent>()
    val uiEvent: SharedFlow<GroupsUiEvent> = _uiEvent

    private var currentUserUid: String? = userRepository.getCurrentUser()?.uid // <-- Get current user ID

    // On init, start collecting groups and balances
    init {
        logD("GroupsViewModel initialized. Starting group collection.")
        if (currentUserUid == null) {
            _uiState.update { it.copy(isLoading = false, error = "User not logged in.") }
        } else {
            //the function to collect groups and balances
            collectGroupsAndBalances()
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun collectGroupsAndBalances() {
        viewModelScope.launch {
            //update ui state to loading
            _uiState.update { it.copy(isLoading = true, error = null) }
            val currentUid = currentUserUid!!

            try {
                //combine 3 data streams: (for the bal calc)
                // all groups from group repo, all group expenses and non-group expenses from expense repo,
                combine(
                    //all groups data stream
                    groupsRepository.getGroups(),
                    //all group expenses data stream
                    groupsRepository.getGroups().flatMapLatest { groups ->
                        if (groups.isEmpty()) {
                            //no group, no grouped expenses
                            flowOf(emptyList<Expense>())
                        } else {
                            //get groups, then in each group get its expenses flow, combine all those flows into one flow of all expenses in all groups
                            val expenseFlows = groups.map { expenseRepository.getExpensesFlowForGroup(it.id) }
                            combine(expenseFlows) { arrayOfExpenseLists ->
                                arrayOfExpenseLists.asList().flatten()
                            }
                        }
                    },
                    //non-group expenses data stream
                    expenseRepository.getExpensesFlowForGroup("non_group")
                ) { groups, allGroupExpenses, nonGroupExpenses -> //referring to the 3 data streams
                    _uiState.update { it.copy(isLoading = true) }

                    //calc for total non-group balance starts here
                    var calculatedNonGroupBalance = 0.0
                    //for each of the non-group expenses
                    nonGroupExpenses.forEach { expense ->
                        //find how much the user paid
                        val userPaid = expense.paidBy.find { it.uid == currentUid }?.paidAmount ?: 0.0
                        //find how much the user owed
                        val userOwed = expense.participants.find { it.uid == currentUid }?.owesAmount ?: 0.0
                        //add the difference to the non-group balance, this is done to all non-group expenses
                        calculatedNonGroupBalance += (userPaid - userOwed)
                    }
                    //final amount of non-group bal
                    val finalNonGroupBalance = roundToCents(calculatedNonGroupBalance)



                    //calc for group balances starts here
                    val (calculatedGroups, membersMap) = if (groups.isEmpty()) {
                        Pair(emptyList<GroupWithBalance>(), emptyMap<String, User>())
                    } else {
                        // get all member userId in all groups
                        val allMemberUids = groups.flatMap { it.members }.distinct()
                        // fetch their user profiles in a single batch and convert to map
                        val membersMap = if (allMemberUids.isNotEmpty()) {
                            userRepository.getProfilesForFriendsCached(allMemberUids).associateBy { it.uid }
                        } else {
                            emptyMap()
                        }

                        //take all group expenses and group them according to their group id (Map)
                        val groupExpensesMap = allGroupExpenses.groupBy { it.groupId ?: "" }

                        // transform "groups" into groups with balances
                        val groupsWithCalculatedBalances = groups.map { group ->

                            //create a list of expenses in this group
                            val expensesInGroup = groupExpensesMap[group.id] ?: emptyList()
                            //init group net bal as 0
                            var groupNetBalance = 0.0

                            //create a map pair consist of member and their bal(+ or -)
                            val memberBalancesInGroup = mutableMapOf<String, Double>()

                            //calc for group net balance start here
                            //for each of the expenses in the group, dont care user or not
                            expensesInGroup.forEach { expense ->
                                //from each expense's payer, we need their uid and amount paid
                                expense.paidBy.forEach { payer ->
                                    //we map (declared above) the payer's uid to their total amount paid
                                    memberBalancesInGroup[payer.uid] = (memberBalancesInGroup[payer.uid] ?: 0.0) + payer.paidAmount
                                }
                                //each expense's participants
                                expense.participants.forEach { participant ->
                                    //we map (same map) their uid with the total amount they owe
                                    memberBalancesInGroup[participant.uid] = (memberBalancesInGroup[participant.uid] ?: 0.0) - participant.owesAmount
                                }

                                //at the end, memberBalancesInGroup map has each member's uid with their net bal in the group
                            }

                            //Current user's balance in this group
                            groupNetBalance = roundToCents(memberBalancesInGroup[currentUid] ?: 0.0)

                            // Calculate the breakdown of the user with each members of the group
                            val breakdown = mutableMapOf<String, Double>()
                            //for each member in the group
                            group.members.forEach { memberUid ->
                                //if the member is not the user (we want user vs member)
                                if (memberUid != currentUid) {
                                    //init balance between user and this member as 0
                                    var balanceWithMember = 0.0
                                    //for each expenses in the group, filter by
                                    expensesInGroup.filter { exp ->
                                        //get all uids involved in paying and participating in that expense, and make it to a set (no dupe)
                                        val involvedUids = (exp.paidBy.map { it.uid } + exp.participants.map { it.uid }).toSet()
                                        //the filter: if the expense has the user and the member uid
                                        involvedUids.contains(currentUid) && involvedUids.contains(memberUid)
                                        //for each of them expenses
                                    }.forEach { relevantExpense ->

                                        //hoe much the user paid
                                        val currentUserPaid = relevantExpense.paidBy.find { it.uid == currentUid }?.paidAmount ?: 0.0
                                        //how much the user owed
                                        val currentUserOwes = relevantExpense.participants.find { it.uid == currentUid }?.owesAmount ?: 0.0
                                        //how much member paid
                                        val memberPaid = relevantExpense.paidBy.find { it.uid == memberUid }?.paidAmount ?: 0.0
                                        //how much member owed
                                        val memberOwes = relevantExpense.participants.find { it.uid == memberUid }?.owesAmount ?: 0.0

                                        //this is to sync bal calc with payment made (made as expense type PAYMENT)
                                        if (relevantExpense.expenseType == ExpenseType.PAYMENT) {
                                            //if user made payment to member
                                            if (currentUserPaid > 0) {
                                                //add to user-member bal
                                                balanceWithMember += currentUserPaid
                                            // if member made payment
                                            } else if (memberPaid > 0) {
                                                //subtract from user-member bal
                                                balanceWithMember -= memberPaid
                                            }
                                        //if no payment
                                        } else {
                                            //normal expense bal calc for user
                                            val currentUserNet = currentUserPaid - currentUserOwes
                                            //normal expense bal calc for member
                                            val memberNet = memberPaid - memberOwes
                                            //number of participants in the expense (at least 1 to avoid div by 0)
                                            val numParticipants = relevantExpense.participants.size.toDouble().coerceAtLeast(1.0)

                                            //adjust user-member bal based on their nets and number of participants
                                            balanceWithMember += (currentUserNet - memberNet) / numParticipants
                                        }
                                    }
                                    val roundedBalanceWithMember = roundToCents(balanceWithMember)
                                    if (roundedBalanceWithMember.absoluteValue > 0.01) {
                                        breakdown[memberUid] = roundedBalanceWithMember
                                    }
                                }
                            }

                            // CREATE GroupWithBalance(group, groupNetBalance, breakdown)
                            GroupWithBalance(
                                group = group,
                                userNetBalance = groupNetBalance,
                                simplifiedOwedBreakdown = breakdown
                            )
                        //Sort groups by balance magnitude (largest first)
                        }.sortedByDescending { it.userNetBalance.absoluteValue }

                        //return the pairs
                        Pair(groupsWithCalculatedBalances, membersMap)
                    }

                    //return triple of calculated groups with bal, members map and non-group bal
                    Triple(calculatedGroups, membersMap, finalNonGroupBalance)

                //end of combine (latest data streams)
                }.collectLatest { (calculatedGroups, membersMap, nonGroupBalance) ->
                    //update the ui state with the latest data
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            groupsWithBalances = calculatedGroups,
                            membersMap = membersMap,
                            nonGroupBalance = nonGroupBalance,
                            error = null
                        )
                    }
                    applySearchAndFilter()
                }

            } catch (e: Exception) {
                logE("Error collecting groups/balances: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load group balances.") }
            }
        }
    }


    fun onGroupCardClick(groupId: String) {
        viewModelScope.launch {
            _uiEvent.emit(GroupsUiEvent.NavigateToGroupDetail(groupId))
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
        val thirtyDaysInMillis = 30L * 24 * 60 * 60 * 1000
        val currentTime = System.currentTimeMillis()

        val (settledGroups, activeGroups) = filteredGroups.partition { groupWithBalance ->
            val settledDate = groupWithBalance.group.settledDate
            val isBalanceSettled = groupWithBalance.userNetBalance.absoluteValue < 0.01


            settledDate != null &&
            (currentTime - settledDate) > thirtyDaysInMillis &&
            isBalanceSettled
        }

        val finalFilteredGroups = if (currentState.showSettledGroups) {
            filteredGroups
        } else {
            activeGroups
        }

        _uiState.update {
            it.copy(
                filteredGroupsWithBalances = finalFilteredGroups,
                settledGroupsCount = settledGroups.size
            )
        }
    }
}