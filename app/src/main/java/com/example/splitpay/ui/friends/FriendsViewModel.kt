package com.example.splitpay.ui.friends

import android.util.Log.e
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.BalanceDetail // Keep this import
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.FriendWithBalance
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User // Import User
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD // Import logger
import com.example.splitpay.logger.logE // Import logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job // Import Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope // <<< ADD THIS IMPORT
import kotlinx.coroutines.delay // Import delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted // Import SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine // Import combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.stateIn // Import stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.filter
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

class FriendsViewModel(
    // Inject UserRepository
    private val userRepository: UserRepository = UserRepository(),
    private val expenseRepository: ExpenseRepository = ExpenseRepository(), // Added
    private val groupsRepository: GroupsRepository = GroupsRepository()
) : ViewModel() {

    // --- Individual State Flows ---
    private val _isLoading = MutableStateFlow(true)
    private val _friends = MutableStateFlow<List<FriendWithBalance>>(emptyList()) // Holds the full list + balances
    private val _currentFilter = MutableStateFlow(FriendFilterType.ALL)
    private val _isFilterMenuExpanded = MutableStateFlow(false)
    private val _isSearchActive = MutableStateFlow(false)
    private val _searchQuery = MutableStateFlow("")
    private val _error = MutableStateFlow<String?>(null)

    // --- Combined State Flow for the UI ---
    val uiState: StateFlow<FriendsUiState> = combine(
        _isLoading,
        _friends,
        _currentFilter,
        _isFilterMenuExpanded,
        _isSearchActive,
        _searchQuery,
        _error
        // --- Explicit Types Added ---
    ) { flows ->
        val isLoading = flows[0] as Boolean
        val friends = flows[1] as List<FriendWithBalance>
        val currentFilter = flows[2] as FriendFilterType
        val isFilterMenuExpanded = flows[3] as Boolean
        val isSearchActive = flows[4] as Boolean
        val searchQuery = flows[5] as String
        val error = flows[6] as String?


        // Apply filtering first based on the selected dropdown option
        val filtered = when (currentFilter) {
            FriendFilterType.ALL -> friends
            FriendFilterType.OUTSTANDING -> friends.filter { (it.netBalance > 0.01 || it.netBalance < -0.01) } // Use tolerance
            FriendFilterType.OWES_YOU -> friends.filter { it.netBalance > 0.01 } // They owe you (positive balance)
            FriendFilterType.YOU_OWE -> friends.filter { it.netBalance < -0.01 } // You owe them (negative balance)
        }

        // Apply search query on top of the filtered results if search is active
        val filteredAndSearched = if (isSearchActive && searchQuery.isNotBlank()) {
            filtered.filter {
                // Search username (add other fields if needed)
                it.username.contains(searchQuery, ignoreCase = true)
            }
        } else {
            filtered // If search is not active or query is blank, show only filtered results
        }

        // Calculate total balance from the original, unfiltered list
        val totalBalance = friends.sumOf { it.netBalance }

        // Construct the final UI state object
        FriendsUiState(
            isLoading = isLoading,
            friends = friends, // Keep the original full list for reference
            filteredAndSearchedFriends = filteredAndSearched, // The list to actually display
            currentFilter = currentFilter,
            totalNetBalance = totalBalance,
            error = error,
            isFilterMenuExpanded = isFilterMenuExpanded,
            isSearchActive = isSearchActive,
            searchQuery = searchQuery
        )
        // Convert the combined flow into a StateFlow for the UI to collect
    }.stateIn(
        scope = viewModelScope, // Coroutine scope for the flow
        started = SharingStarted.WhileSubscribed(5000), // Standard config
        initialValue = FriendsUiState() // Initial state before combination happens
    )

    private var currentUserUid: String? = userRepository.getCurrentUser()?.uid

    init {
        // Load initial data when the ViewModel is created
        loadFriendsWithRealData() // Changed from mock function name
    }

    // Renamed function to reflect real data fetching
    private fun loadFriendsWithRealData() {
        if (currentUserUid == null) {
            _error.value = "User not logged in."
            _isLoading.value = false
            return
        }
        val currentUid = currentUserUid!! // Safe non-null access

        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                // 1. Fetch friend UIDs and profiles
                val friendUids = userRepository.getCurrentUserFriendIds()
                if (friendUids.isEmpty()) {
                    logD("No friends found for current user.")
                    _friends.value = emptyList() // Update state to show no friends
                    _isLoading.value = false
                    return@launch // Early exit if no friends
                }
                val friendProfiles = userRepository.getProfilesForFriends(friendUids)

                // 2. Fetch all groups the current user is in
                // **FIX:** Use suspend version to fetch groups once
                val allUserGroups = groupsRepository.getGroupsSuspend()
                logD("Fetched ${allUserGroups.size} groups for current user.")


                // 3. Calculate balance for each friend CONCURRENTLY
                val friendsWithBalances = coroutineScope { // <<< CHANGE HERE
                    val deferreds = friendProfiles.map { friend ->
                        async(Dispatchers.IO) { // 'async' is now called within the coroutineScope
                            val (netBalance, breakdown) = calculateNetBalanceWithFriend(currentUid, friend.uid, allUserGroups)
                            FriendWithBalance(
                                uid = friend.uid,
                                username = friend.username.takeIf { it.isNotBlank() } ?: friend.fullName, // Prefer username
                                netBalance = netBalance,
                                // **NEW:** Assign the calculated breakdown
                                balanceBreakdown = breakdown
                            )
                        }
                    }
                    deferreds.awaitAll() // awaitAll remains inside the scope
                }

                _friends.value = friendsWithBalances.sortedByDescending { it.netBalance.absoluteValue } // Sort by balance magnitude
                _isLoading.value = false
                logD("Successfully calculated balances for ${friendsWithBalances.size} friends.")

            } catch (e: Exception) {
                logE("Error loading friends and balances: ${e.message}") // Log exception too
                _error.value = "Failed to load friend balances."
                _isLoading.value = false

            }
        }
    }

    /**
     * Called when the filter dropdown menu is dismissed.
     */
    fun onDismissFilterMenu() {
        _isFilterMenuExpanded.update { false }
    }

    /**
     * Called when a new filter option is selected from the dropdown.
     */
    fun applyFilter(filterType: FriendFilterType) {
        _currentFilter.update { filterType }
        onDismissFilterMenu() // Close the menu after selection
    }

    /**
     * Called when the search icon in the top bar is clicked.
     */
    fun onSearchIconClick() {
        _isSearchActive.update { true }
    }

    /**
     * Called when the close (X) icon in the search bar is clicked.
     */
    fun onSearchCloseClick() {
        _isSearchActive.update { false }
        _searchQuery.update { "" } // Clear the search query
    }

    /**
     * Called when the text in the search bar changes.
     */
    fun onSearchQueryChange(query: String) {
        _searchQuery.update { query }
    }

    /**
     * Called when the filter icon in the top bar is clicked.
     */
    fun onFilterIconClick() {
        _isFilterMenuExpanded.update { true }
    }

    // --- Balance Calculation Function ---
    // **MODIFIED:** Returns Pair<Double, List<BalanceDetail>>
    private suspend fun calculateNetBalanceWithFriend(
        currentUserUid: String,
        friendUid: String,
        allUserGroups: List<Group>
    ): Pair<Double, List<BalanceDetail>> {
        val balanceDetails = mutableListOf<BalanceDetail>()
        var netBalance = 0.0
        logD("Calculating balance between $currentUserUid and $friendUid")

        try {
            // --- 1. Calculate balance from SHARED GROUP expenses ---
            val sharedGroups = allUserGroups
                .filter { it.members.contains(currentUserUid) && it.members.contains(friendUid) }
            val sharedGroupIds = sharedGroups.map { it.id }
            logD("Found ${sharedGroupIds.size} shared groups with $friendUid")

            val groupExpensesDeferred = viewModelScope.async(Dispatchers.IO) { // Fetch group expenses concurrently
                if (sharedGroupIds.isNotEmpty()) expenseRepository.getExpensesForGroups(sharedGroupIds) else emptyList()
            }

            // --- 2. Calculate balance from NON-GROUP expenses ---
            val nonGroupExpensesDeferred = viewModelScope.async(Dispatchers.IO) { // Fetch non-group expenses concurrently
                expenseRepository.getNonGroupExpensesBetweenUsers(currentUserUid, friendUid)
            }

            // Await both expense lists
            val groupExpenses = groupExpensesDeferred.await()
            val nonGroupExpenses = nonGroupExpensesDeferred.await()
            logD("Fetched ${groupExpenses.size} shared group expenses and ${nonGroupExpenses.size} non-group expenses with $friendUid")

            // --- 3. Process SHARED GROUP Expenses ---
            val groupBalances = mutableMapOf<String, Double>() // GroupId to Net Balance Change
            groupExpenses.forEach { expense ->
                val balanceChange = calculateBalanceChangeForExpense(expense, currentUserUid, friendUid)
                netBalance += balanceChange
                val groupId = expense.groupId ?: "error_no_group_id"
                groupBalances[groupId] = (groupBalances[groupId] ?: 0.0) + balanceChange
            }

            // Create BalanceDetail for each shared group with non-zero balance
            sharedGroups.forEach { group ->
                val groupBalance = groupBalances[group.id] ?: 0.0
                if (groupBalance.absoluteValue > 0.01) { // Add tolerance
                    balanceDetails.add(BalanceDetail(groupName = group.name, amount = roundToCents(groupBalance)))
                }
            }


            // --- 4. Process NON-GROUP Expenses ---
            var nonGroupNetBalance = 0.0
            nonGroupExpenses.forEach { expense ->
                val balanceChange = calculateBalanceChangeForExpense(expense, currentUserUid, friendUid)
                netBalance += balanceChange
                nonGroupNetBalance += balanceChange
            }
            // Add a single BalanceDetail for all non-group expenses if balance is non-zero
            if (nonGroupNetBalance.absoluteValue > 0.01) { // Add tolerance
                balanceDetails.add(BalanceDetail(groupName = "Non-group", amount = roundToCents(nonGroupNetBalance)))
            }


            // Round the final balance to 2 decimal places
            val finalBalance = roundToCents(netBalance)
            logD("Final calculated balance between $currentUserUid and $friendUid: $finalBalance")
            // Return both the net balance and the detailed breakdown
            return Pair(finalBalance, balanceDetails)

        } catch (e: Exception) {
            logE("Error calculating balance between $currentUserUid and $friendUid: ${e.message}")
            // Return zero balance and empty breakdown on error
            return Pair(0.0, emptyList())
        }
    }


// --- Helper to calculate balance change from a single expense ---
    /**
     * Calculates the net change in balance FOR the currentUserUid RELATIVE TO the friendUid
     * for a single expense.
     * Positive result means friendUid owes currentUserUid more after this expense.
     * Negative result means currentUserUid owes friendUid more after this expense.
     */
    private fun calculateBalanceChangeForExpense(
        expense: Expense,
        currentUserUid: String,
        friendUid: String
    ): Double {
        // Find how much each person paid
        val paidByCurrentUser = expense.paidBy.find { it.uid == currentUserUid }?.paidAmount ?: 0.0
        val paidByFriend = expense.paidBy.find { it.uid == friendUid }?.paidAmount ?: 0.0

        // Find how much each person's share was
        val shareOwedByCurrentUser = expense.participants.find { it.uid == currentUserUid }?.owesAmount ?: 0.0
        val shareOwedByFriend = expense.participants.find { it.uid == friendUid }?.owesAmount ?: 0.0

        // Calculate the net contribution of each person for this specific expense
        // Positive means they paid more than their share (lent money to the pool)
        // Negative means they paid less than their share (borrowed from the pool)
        val netContributionCurrentUser = paidByCurrentUser - shareOwedByCurrentUser
        val netContributionFriend = paidByFriend - shareOwedByFriend

        // The change in balance FROM the current user's perspective relative to the friend:
        // If my net contribution is positive (I lent) and friend's is negative (they borrowed), I am owed more (positive change).
        // If my net contribution is negative (I borrowed) and friend's is positive (they lent), I owe more (negative change).
        // Essentially, we want to find how much of the friend's debt was covered by the current user's surplus,
        // or how much of the current user's debt was covered by the friend's surplus.

        // Simpler way: Calculate the difference in their net contributions.
        // If (My Net) > (Friend Net), the difference is how much more I contributed than the friend.
        // This difference should ideally be balanced by others, but in a two-person context,
        // it reflects the net flow between them.

        // Let's test with examples:
        // Ex1: I pay 60, split 20 each (Me +20, Friend -20). Diff = +40. Friend owes me 20. Change should be +20?
        // Ex2: Friend pays 60, split 20 each (Me -20, Friend +20). Diff = -40. I owe friend 20. Change should be -20?
        // Ex3: I pay 30, Friend pays 30, split 30 each (Me 0, Friend 0). Diff = 0. Change should be 0.
        // Ex4: I pay 40, Friend pays 20, split 30 each (Me +10, Friend -10). Diff = +20. Friend owes me 10. Change should be +10?

        // It looks like (My Net Contribution - Friend Net Contribution) / 2 gives the correct balance change between the two.
        val balanceChange = (netContributionCurrentUser - netContributionFriend) / 2.0

        // Log calculation details for debugging
        // logD("Expense '${expense.description}': MyNet=${netContributionCurrentUser}, FriendNet=${netContributionFriend}, Change=${balanceChange}")

        return balanceChange
    }

    // Helper function for rounding
    private fun roundToCents(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}
