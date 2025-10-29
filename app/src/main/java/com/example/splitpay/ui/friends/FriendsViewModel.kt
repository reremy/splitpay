package com.example.splitpay.ui.friends

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.BalanceDetail
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.FriendWithBalance
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// Enum for filtering state
enum class FriendFilterType {
    ALL, OUTSTANDING, OWES_YOU, YOU_OWE
}

// UI State for the Friends Screen
data class FriendsUiState(
    val isLoading: Boolean = true,
    val friends: List<FriendWithBalance> = emptyList(), // The full list from repo
    val filteredAndSearchedFriends: List<FriendWithBalance> = emptyList(), // List displayed in UI
    val currentFilter: FriendFilterType = FriendFilterType.ALL,
    val totalNetBalance: Double = 0.0, // Overall balance across all friends
    val error: String? = null,
    val isFilterMenuExpanded: Boolean = false,
    // --- Search State ---
    val isSearchActive: Boolean = false,
    val searchQuery: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModel(
    // Inject Repositories
    private val userRepository: UserRepository = UserRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val groupsRepository: GroupsRepository = GroupsRepository()
) : ViewModel() {

    // --- Private State Flows for UI controls ---
    private val _currentFilter = MutableStateFlow(FriendFilterType.ALL)
    private val _isFilterMenuExpanded = MutableStateFlow(false)
    private val _isSearchActive = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")

    private val currentUserUid: String? = userRepository.getCurrentUser()?.uid

    // --- Data Flows from Repositories ---
    // These flows are assumed to be added to your repositories to provide real-time updates.

    // 1. A flow that emits the user's friend list (as User objects) whenever it changes.
    private val friendsFlow: Flow<List<User>> = if (currentUserUid == null) {
        flowOf(emptyList())
    } else {
        // ASSUMPTION: You create this reactive function in UserRepository
        // e.g., userRepository.getFriendProfilesFlow(currentUserUid)
        // For now, we'll simulate a non-reactive one that loads once:
        userRepository.getFriendsFlow(currentUserUid) // <-- Assuming this is the new reactive func
    }

    // 2. A flow that emits all groups the user is in. (This one exists)
    private val groupsFlow: Flow<List<Group>> = if (currentUserUid == null) {
        flowOf(emptyList())
    } else {
        groupsRepository.getGroups() // This is already reactive
    }

    // 3. Flow for ALL relevant expenses (group + non-group)
    private val allExpensesFlow: Flow<List<Expense>> = if (currentUserUid == null) {
        flowOf(emptyList())
    } else {
        // Combine group expenses and non-group expenses reactively
        groupsFlow.flatMapLatest { groups ->
            // Get flow for non-group expenses
            val nonGroupExpenseFlow = expenseRepository.getNonGroupExpensesFlow(currentUserUid)

            if (groups.isEmpty()) {
                // If no groups, just return the non-group expenses
                nonGroupExpenseFlow
            } else {
                // Create flows for each group's expenses
                val groupExpenseFlows: List<Flow<List<Expense>>> = groups.map { group ->
                    expenseRepository.getExpensesFlowForGroup(group.id)
                }
                // Combine all group flows and the non-group flow
                combine(groupExpenseFlows + nonGroupExpenseFlow) { arrayOfExpenseLists ->
                    // Flatten into a single list
                    arrayOfExpenseLists.asList().flatten()
                }
            }
        }
    }


    // --- Combined State Flow for the UI ---
    val uiState: StateFlow<FriendsUiState> = combine(
        friendsFlow,
        groupsFlow,
        allExpensesFlow,
        _currentFilter,
        _isFilterMenuExpanded,
        _isSearchActive,
        _searchQuery
    ) { flows ->
        // Deconstruct flows array
        val friends = flows[0] as List<User>
        val allGroups = flows[1] as List<Group>
        val allExpenses = flows[2] as List<Expense>
        val currentFilter = flows[3] as FriendFilterType
        val isFilterMenuExpanded = flows[4] as Boolean
        val isSearchActive = flows[5] as Boolean
        val searchQuery = flows[6] as String

        if (currentUserUid == null) {
            FriendsUiState(isLoading = false, error = "User not logged in.")
        } else {
            // --- All calculation logic is now inside the combine block ---

            logD("Recalculating FriendsUiState...")

            // 1. Separate expenses for easier processing
            val groupExpensesMap = allExpenses.filter { it.groupId != null }.groupBy { it.groupId }
            val nonGroupExpenses = allExpenses.filter { it.groupId == null }

            var totalOverallBalance = 0.0

            // 2. Calculate balance for each friend
            val friendsWithBalances = friends.map { friend ->
                val (netBalance, breakdown) = calculateNetBalanceWithFriend(
                    currentUserUid = currentUserUid,
                    friendUid = friend.uid,
                    allUserGroups = allGroups,
                    groupExpensesMap = groupExpensesMap,
                    allNonGroupExpenses = nonGroupExpenses
                )
                totalOverallBalance += netBalance
                FriendWithBalance(
                    uid = friend.uid,
                    username = friend.username.takeIf { it.isNotBlank() } ?: friend.fullName,
                    netBalance = netBalance,
                    balanceBreakdown = breakdown
                )
            }.sortedByDescending { it.netBalance.absoluteValue } // Sort by balance magnitude

            // 3. Apply filtering
            val filtered = when (currentFilter) {
                FriendFilterType.ALL -> friendsWithBalances
                FriendFilterType.OUTSTANDING -> friendsWithBalances.filter { (it.netBalance > 0.01 || it.netBalance < -0.01) }
                FriendFilterType.OWES_YOU -> friendsWithBalances.filter { it.netBalance > 0.01 }
                FriendFilterType.YOU_OWE -> friendsWithBalances.filter { it.netBalance < -0.01 }
            }

            // 4. Apply search
            val filteredAndSearched = if (isSearchActive && searchQuery.isNotBlank()) {
                filtered.filter {
                    it.username.contains(searchQuery, ignoreCase = true)
                }
            } else {
                filtered
            }

            // 5. Construct the final UI state
            FriendsUiState(
                isLoading = false, // Data has been loaded
                friends = friendsWithBalances, // The original full list
                filteredAndSearchedFriends = filteredAndSearched, // The list to display
                currentFilter = currentFilter,
                totalNetBalance = roundToCents(totalOverallBalance), // The true, reactive total
                error = null,
                isFilterMenuExpanded = isFilterMenuExpanded,
                isSearchActive = isSearchActive,
                searchQuery = searchQuery
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FriendsUiState(isLoading = true) // Start in loading state
    )


    // --- Public Event Handlers ---

    fun onDismissFilterMenu() {
        _isFilterMenuExpanded.update { false }
    }

    fun applyFilter(filterType: FriendFilterType) {
        _currentFilter.update { filterType }
        onDismissFilterMenu()
    }

    fun onSearchIconClick() {
        _isSearchActive.update { true }
    }

    fun onSearchCloseClick() {
        _isSearchActive.update { false }
        _searchQuery.update { "" }
    }

    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    fun onFilterIconClick() {
        _isFilterMenuExpanded.update { true }
    }

    // --- Balance Calculation Helpers ---

    /**
     * Non-suspending balance calculation function.
     * It takes the prefetched, reactive lists as parameters and filters them.
     */
    private fun calculateNetBalanceWithFriend(
        currentUserUid: String,
        friendUid: String,
        allUserGroups: List<Group>,
        groupExpensesMap: Map<String?, List<Expense>>,
        allNonGroupExpenses: List<Expense>
    ): Pair<Double, List<BalanceDetail>> {
        val balanceDetails = mutableListOf<BalanceDetail>()
        var netBalance = 0.0
        logD("Calculating balance between $currentUserUid and $friendUid")

        try {
            // --- 1. Process SHARED GROUP Expenses ---
            val sharedGroups = allUserGroups
                .filter { it.members.contains(currentUserUid) && it.members.contains(friendUid) }
            val groupBalances = mutableMapOf<String, Double>() // GroupId to Net Balance Change

            sharedGroups.forEach { group ->
                val expensesInGroup = groupExpensesMap[group.id] ?: emptyList()
                expensesInGroup.forEach { expense ->
                    val balanceChange = calculateBalanceChangeForExpense(expense, currentUserUid, friendUid)
                    netBalance += balanceChange
                    groupBalances[group.id] = (groupBalances[group.id] ?: 0.0) + balanceChange
                }
            }

            // Create BalanceDetail for each shared group with non-zero balance
            sharedGroups.forEach { group ->
                val groupBalance = groupBalances[group.id] ?: 0.0
                if (groupBalance.absoluteValue > 0.01) {
                    balanceDetails.add(BalanceDetail(groupName = group.name, amount = roundToCents(groupBalance)))
                }
            }

            // --- 2. Process NON-GROUP Expenses ---
            // Filter the non-group list for *only* expenses involving this friend
            val nonGroupExpensesWithFriend = allNonGroupExpenses.filter { expense ->
                val involvedUids = mutableSetOf(expense.createdByUid)
                expense.paidBy.forEach { involvedUids.add(it.uid) }
                expense.participants.forEach { involvedUids.add(it.uid) }
                involvedUids.contains(currentUserUid) && involvedUids.contains(friendUid)
            }

            var nonGroupNetBalance = 0.0
            nonGroupExpensesWithFriend.forEach { expense ->
                val balanceChange = calculateBalanceChangeForExpense(expense, currentUserUid, friendUid)
                netBalance += balanceChange
                nonGroupNetBalance += balanceChange
            }

            if (nonGroupNetBalance.absoluteValue > 0.01) {
                balanceDetails.add(BalanceDetail(groupName = "Non-group", amount = roundToCents(nonGroupNetBalance)))
            }

            // --- 3. Return final, rounded balance and details ---
            val finalBalance = roundToCents(netBalance)
            logD("Final calculated balance between $currentUserUid and $friendUid: $finalBalance")
            return Pair(finalBalance, balanceDetails)

        } catch (e: Exception) {
            logE("Error calculating balance between $currentUserUid and $friendUid: ${e.message}")
            return Pair(0.0, emptyList())
        }
    }

    /**
     * Calculates the net change in balance FOR the currentUserUid RELATIVE TO the friendUid
     * for a single expense.
     * Positive result means friendUid owes currentUserUid more.
     * Negative result means currentUserUid owes friendUid more.
     */
    private fun calculateBalanceChangeForExpense(
        expense: Expense,
        currentUserUid: String,
        friendUid: String
    ): Double {
        val paidByCurrentUser = expense.paidBy.find { it.uid == currentUserUid }?.paidAmount ?: 0.0
        val paidByFriend = expense.paidBy.find { it.uid == friendUid }?.paidAmount ?: 0.0

        val shareOwedByCurrentUser = expense.participants.find { it.uid == currentUserUid }?.owesAmount ?: 0.0
        val shareOwedByFriend = expense.participants.find { it.uid == friendUid }?.owesAmount ?: 0.0

        val netContributionCurrentUser = paidByCurrentUser - shareOwedByCurrentUser
        val netContributionFriend = paidByFriend - shareOwedByFriend

        // This calculation determines the net flow *between only these two users*
        val balanceChange = (netContributionCurrentUser - netContributionFriend) / 2.0

        return balanceChange
    }

    // Helper function for rounding
    private fun roundToCents(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}