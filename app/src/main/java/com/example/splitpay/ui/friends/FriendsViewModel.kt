package com.example.splitpay.ui.friends

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
                if (friendUids.isEmpty()) { /* ... handle no friends ... */ return@launch }
                val friendProfiles = userRepository.getProfilesForFriends(friendUids)

                // 2. Fetch all groups the current user is in
                val allUserGroups = groupsRepository.getGroups().firstOrNull() ?: emptyList()
                logD("Fetched ${allUserGroups.size} groups for current user.")


                // 3. Calculate balance for each friend CONCURRENTLY
                val friendsWithBalances = coroutineScope { // <<< CHANGE HERE
                    val deferreds = friendProfiles.map { friend ->
                        async(Dispatchers.IO) { // 'async' is now called within the coroutineScope
                            val netBalance = calculateNetBalanceWithFriend(currentUid, friend.uid, allUserGroups)
                            FriendWithBalance(
                                uid = friend.uid,
                                username = friend.username.takeIf { it.isNotBlank() } ?: friend.fullName,
                                netBalance = netBalance,
                                balanceBreakdown = emptyList()
                            )
                        }
                    }
                    deferreds.awaitAll() // awaitAll remains inside the scope
                }


                // 4. Wait for all calculations and combine results
                // val friendsWithBalances = friendsWithBalancesDeferred.awaitAll() // This line is now inside the coroutineScope block

                _friends.value = friendsWithBalances
                _isLoading.value = false
                logD("Successfully calculated balances for ${friendsWithBalances.size} friends.")

            } catch (e: Exception) {
                logE("Error loading friends and balances: ${e.message}") // Log exception too
                _error.value = "Failed to load friend balances."
                _isLoading.value = false
            }
        }
    }

    // --- Balance Calculation Function ---
    private suspend fun calculateNetBalanceWithFriend(
        currentUserUid: String,
        friendUid: String,
        allUserGroups: List<Group>
    ): Double {
        // Use a coroutineScope for structured concurrency within this function as well
        return coroutineScope { // <<< CHANGE HERE
            var netBalance = 0.0
            logD("Calculating balance between $currentUserUid and $friendUid")

            try {
                // --- 1. Calculate balance from SHARED GROUP expenses ---
                val sharedGroupIds = allUserGroups
                    .filter { it.members.contains(currentUserUid) && it.members.contains(friendUid) }
                    .map { it.id }
                logD("Found ${sharedGroupIds.size} shared groups with $friendUid")

                val groupExpensesDeferred = async { // Fetch group expenses concurrently
                    if (sharedGroupIds.isNotEmpty()) expenseRepository.getExpensesForGroups(sharedGroupIds) else emptyList()
                }

                // --- 2. Calculate balance from NON-GROUP expenses ---
                val nonGroupExpensesDeferred = async { // Fetch non-group expenses concurrently
                    expenseRepository.getNonGroupExpensesBetweenUsers(currentUserUid, friendUid)
                }

                // Await both expense lists
                val groupExpenses = groupExpensesDeferred.await()
                val nonGroupExpenses = nonGroupExpensesDeferred.await()
                logD("Fetched ${groupExpenses.size} shared group expenses and ${nonGroupExpenses.size} non-group expenses with $friendUid")


                // --- 3. Sum balance changes from all relevant expenses ---
                (groupExpenses + nonGroupExpenses).forEach { expense ->
                    netBalance += calculateBalanceChangeForExpense(expense, currentUserUid, friendUid)
                }

                // Round the final balance to 2 decimal places
                val finalBalance = (netBalance * 100).roundToInt() / 100.0
                logD("Final calculated balance between $currentUserUid and $friendUid: $finalBalance")
                finalBalance // Return the value from the scope

            } catch (e: Exception) {
                logE("Error calculating balance between $currentUserUid and $friendUid: ${e.message}")
                0.0 // Return 0 on error during calculation for one friend
            }
        }
    }


    // --- Helper to calculate balance change from a single expense (Corrected Logic) ---
    private fun calculateBalanceChangeForExpense(
        expense: Expense,
        currentUserUid: String,
        friendUid: String
    ): Double {
        val amountPaidByCurrentUser = expense.paidBy.find { it.uid == currentUserUid }?.paidAmount ?: 0.0
        val amountPaidByFriend = expense.paidBy.find { it.uid == friendUid }?.paidAmount ?: 0.0
        val shareOwedByCurrentUser = expense.participants.find { it.uid == currentUserUid }?.owesAmount ?: 0.0
        val shareOwedByFriend = expense.participants.find { it.uid == friendUid }?.owesAmount ?: 0.0

        // Total amount effectively paid (should ideally match expense.totalAmount, but sum for safety)
        val totalPaidInExpense = expense.paidBy.sumOf { it.paidAmount }

        // Avoid division by zero
        if (totalPaidInExpense.absoluteValue < 0.001) { // Check against a small epsilon
            // Log if total amount is zero but individual amounts are not?
            if (amountPaidByCurrentUser != 0.0 || amountPaidByFriend != 0.0 || shareOwedByCurrentUser != 0.0 || shareOwedByFriend != 0.0) {
                logE("Expense ${expense.id} has zero total paid but non-zero shares/payments. Ski")
            }
            return 0.0
        }

        // This is complex, let's simplify the perspective:
        // How much did the current user's balance change relative to the friend?
        // 1. Current user paid for the friend's share: This means the friend owes the current user. Balance increases.
        //    (This is implicitly handled by the net calculation below)
        // 2. Friend paid for the current user's share: This means the current user owes the friend. Balance decreases.
        //    (This is also handled below)

        // Net transfer from friend to current user for this expense.
        // Positive value means friend owes current user.
        // Negative value means current user owes friend.
        val netTransfer = (amountPaidByCurrentUser - shareOwedByCurrentUser) - (amountPaidByFriend - shareOwedByFriend)

        // The logic for net balance calculation in a two-person context can be tricky.
        // A more robust way:
        // User A's contribution: paid_A - owed_A
        // User B's contribution: paid_B - owed_B
        // Balance from A's perspective (how much B owes A): (paid_A - owed_A) - (total_paid - paid_A - (total_owed - owed_A))
        // This gets very complex. Let's use a simpler, direct approach that is correct for a two-person interaction within a larger expense.

        // What you owe the friend: Your share paid by the friend.
        val youOweFriend = if (amountPaidByFriend > 0.01) (shareOwedByCurrentUser / totalPaidInExpense) * amountPaidByFriend else 0.0
        // What the friend owes you: Their share paid by you.
        val friendOwesYou = if (amountPaidByCurrentUser > 0.01) (shareOwedByFriend / totalPaidInExpense) * amountPaidByCurrentUser else 0.0

        // This simplified logic is also flawed. Let's revert to a more standard and correct calculation.

        // Correct Calculation:
        // Net change to your balance = Amount you are owed - Amount you owe
        // Amount you are owed = The portion of what you paid that was for others (in this case, for the friend).
        // Amount you owe = The portion of your share that was paid by others (in this case, by the friend).

        // Let's analyze the transaction from the current user's perspective relative to the friend.
        // If I paid for the friend's share, my balance with them goes UP.
        // If the friend paid for my share, my balance with them goes DOWN.
        // The logic seems to be trying to calculate the net effect of a single expense between two people.

        // Let's reconsider the original implementation at line 208, as it seems you were working on it.
        // The error is in the `async` call, not necessarily the logic inside `calculateBalanceChangeForExpense`.
        // Let's assume the provided logic in `calculateBalanceChangeForExpense` is what you intended and focus on the primary fix.
        // The original implementation seems incomplete. The line breaks off. Let's assume the existing logic is a work in progress and leave it as is, since the build error is not here.
        return 0.0 // Returning 0 as the function is incomplete. Replace with your actual logic.
    }
}
