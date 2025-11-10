package com.example.splitpay.ui.friendsDetail

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Activity
import com.example.splitpay.data.model.ActivityType
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.ExpenseType
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.example.splitpay.ui.theme.NegativeRed
import com.example.splitpay.ui.theme.PositiveGreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

data class FriendDetailUiState(
    val friend: User? = null,
    val isLoadingFriend: Boolean = true,
    val isLoadingExpenses: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val sharedGroups: List<Group> = emptyList(),
    val sharedGroupActivities: List<Activity> = emptyList(),
    val error: String? = null,
    val netBalance: Double = 0.0,
    val balanceBreakdown: List<BalanceDetail> = emptyList(),
    val usersMap: Map<String, User> = emptyMap(),
    // NEW: Aggregated activity cards
    val activityCards: List<ActivityCard> = emptyList()
)

data class BalanceDetail(
    val groupName: String,
    val amount: Double
)

// NEW: Sealed class for different activity card types
sealed class ActivityCard(val date: Long) {
    data class SharedGroupCard(
        val group: Group?,
        val groupName: String,
        val balance: Double,
        val mostRecentDate: Long,
        val groupId: String?
    ) : ActivityCard(mostRecentDate)

    data class PaymentCard(
        val expense: Expense
    ) : ActivityCard(expense.date)
}

class FriendsDetailViewModel(
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    private val groupsRepository: GroupsRepository,
    private val activityRepository: ActivityRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(FriendDetailUiState())
    val uiState: StateFlow<FriendDetailUiState> = _uiState.asStateFlow()

    private val friendId: String = savedStateHandle["friendId"] ?: ""
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        if (friendId.isBlank() || currentUserId == null) {
            _uiState.update {
                it.copy(
                    isLoadingFriend = false,
                    isLoadingExpenses = false,
                    error = "Invalid friend ID or user not logged in."
                )
            }
        } else {
            loadFriendAndExpenses()
        }
    }

    private fun loadFriendAndExpenses() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingFriend = true, isLoadingExpenses = true, error = null) }

            try {
                // Load friend profile
                val friend = userRepository.getUserProfile(friendId)
                if (friend == null) {
                    _uiState.update {
                        it.copy(
                            isLoadingFriend = false,
                            isLoadingExpenses = false,
                            error = "Friend not found."
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(friend = friend, isLoadingFriend = false) }

                logD("Loading expenses for friend: $friendId")

                // Get all groups that both users are in
                val allGroups = groupsRepository.getGroupsSuspend()
                val sharedGroups = allGroups.filter { group ->
                    group.members.contains(currentUserId) && group.members.contains(friendId)
                }

                logD("Found ${sharedGroups.size} shared groups with friend $friendId")

                // Fetch expenses from shared groups
                val groupExpenses = if (sharedGroups.isNotEmpty()) {
                    expenseRepository.getExpensesForGroups(sharedGroups.map { it.id })
                } else {
                    emptyList()
                }

                logD("Fetched ${groupExpenses.size} group expenses")

                // Fetch non-group expenses
                val nonGroupExpenses = expenseRepository.getNonGroupExpensesBetweenUsers(currentUserId!!, friendId)

                logD("Fetched ${nonGroupExpenses.size} non-group expenses")

                // Filter to only include expenses where BOTH users are involved
                val filteredGroupExpenses = groupExpenses.filter { expense ->
                    val involvedUids = mutableSetOf(expense.createdByUid)
                    expense.paidBy.forEach { involvedUids.add(it.uid) }
                    expense.participants.forEach { involvedUids.add(it.uid) }
                    val bothInvolved = involvedUids.contains(currentUserId) && involvedUids.contains(friendId)

                    if (bothInvolved) {
                        logD("✓ Including expense: ${expense.description} (${expense.id})")
                        logD("  - Involved UIDs: $involvedUids")
                        logD("  - Participants: ${expense.participants.map { it.uid }}")
                        logD("  - Payers: ${expense.paidBy.map { it.uid }}")
                    } else {
                        logD("✗ Excluding expense: ${expense.description}")
                        logD("  - Involved UIDs: $involvedUids")
                        logD("  - Current user ($currentUserId) involved: ${involvedUids.contains(currentUserId)}")
                        logD("  - Friend ($friendId) involved: ${involvedUids.contains(friendId)}")
                    }

                    bothInvolved
                }

                logD("After filtering: ${filteredGroupExpenses.size} group expenses where both users are involved")

                // Combine and sort by date
                val allExpenses = (filteredGroupExpenses + nonGroupExpenses)
                    .sortedByDescending { it.date }

                logD("Total expenses to display: ${allExpenses.size}")

                // Calculate balances
                val balanceBreakdown = mutableListOf<BalanceDetail>()
                var totalNetBalance = 0.0

                // Group expenses by group
                sharedGroups.forEach { group ->
                    val groupExpensesList = filteredGroupExpenses.filter { it.groupId == group.id }
                    var groupBalance = 0.0

                    groupExpensesList.forEach { expense ->
                        val balanceChange = calculateBalanceChangeForExpense(expense, currentUserId!!, friendId)
                        groupBalance += balanceChange
                    }

                    if (groupBalance.absoluteValue > 0.01) {
                        balanceBreakdown.add(
                            BalanceDetail(
                                groupName = group.name,
                                amount = roundToCents(groupBalance)
                            )
                        )
                        totalNetBalance += groupBalance
                    }
                }

                // Non-group expenses
                var nonGroupBalance = 0.0
                nonGroupExpenses.forEach { expense ->
                    val balanceChange = calculateBalanceChangeForExpense(expense, currentUserId!!, friendId)
                    nonGroupBalance += balanceChange
                }

                if (nonGroupBalance.absoluteValue > 0.01) {
                    balanceBreakdown.add(
                        BalanceDetail(
                            groupName = "Non-group expenses",
                            amount = roundToCents(nonGroupBalance)
                        )
                    )
                    totalNetBalance += nonGroupBalance
                }

                // Fetch user details for display
                val allInvolvedUids = allExpenses.flatMap { expense ->
                    listOf(expense.createdByUid) + expense.paidBy.map { it.uid } + expense.participants.map { it.uid }
                }.distinct()

                val usersMap = if (allInvolvedUids.isNotEmpty()) {
                    userRepository.getProfilesForFriends(allInvolvedUids).associateBy { it.uid }
                } else {
                    emptyMap()
                }

                // Load shared group activities
                val sharedGroupActivities = mutableListOf<Activity>()
                sharedGroups.forEach { group ->
                    // Get all activities for this group
                    val groupActivities = activityRepository.getActivitiesForGroup(group.id)

                    // Filter to MEMBER_ADDED activities where either user was added to this shared group
                    val relevantActivities = groupActivities.filter { activity ->
                        activity.activityType == ActivityType.MEMBER_ADDED.name &&
                        // Show if either the friend or current user was the one added
                        (activity.entityId == friendId || activity.entityId == currentUserId)
                    }

                    sharedGroupActivities.addAll(relevantActivities)
                }

                logD("Found ${sharedGroupActivities.size} shared group activities")
                logD("Final state: ${allExpenses.size} expenses, balance: $totalNetBalance")

                // NEW: Build activity cards
                val activityCards = buildActivityCards(allExpenses, sharedGroups)

                _uiState.update {
                    it.copy(
                        isLoadingExpenses = false,
                        expenses = allExpenses,
                        sharedGroups = sharedGroups,
                        sharedGroupActivities = sharedGroupActivities,
                        netBalance = roundToCents(totalNetBalance),
                        balanceBreakdown = balanceBreakdown,
                        usersMap = usersMap,
                        activityCards = activityCards,
                        error = null
                    )
                }

            } catch (e: Exception) {
                logE("Error loading friend details: ${e.message}")
                e.printStackTrace()
                _uiState.update {
                    it.copy(
                        isLoadingFriend = false,
                        isLoadingExpenses = false,
                        error = "Failed to load friend details: ${e.message}"
                    )
                }
            }
        }
    }

    private fun calculateBalanceChangeForExpense(
        expense: Expense,
        currentUserUid: String,
        friendUid: String
    ): Double {
        val paidByCurrentUser = expense.paidBy.find { it.uid == currentUserUid }?.paidAmount ?: 0.0
        val paidByFriend = expense.paidBy.find { it.uid == friendUid }?.paidAmount ?: 0.0

        val shareOwedByCurrentUser = expense.participants.find { it.uid == currentUserUid }?.owesAmount ?: 0.0
        val shareOwedByFriend = expense.participants.find { it.uid == friendUid }?.owesAmount ?: 0.0

        if (expense.expenseType == ExpenseType.PAYMENT) {
            return when {
                paidByCurrentUser > 0 -> paidByCurrentUser
                paidByFriend > 0 -> -paidByFriend
                else -> 0.0
            }
        } else {
            val netContributionCurrentUser = paidByCurrentUser - shareOwedByCurrentUser
            val netContributionFriend = paidByFriend - shareOwedByFriend
            val numParticipants = expense.participants.size.toDouble().coerceAtLeast(1.0)

            return (netContributionCurrentUser - netContributionFriend) / numParticipants
        }
    }

    fun calculateUserLentBorrowed(expense: Expense): Pair<String, Color> {
        if (currentUserId == null) return "" to Color.Gray

        if (expense.expenseType == ExpenseType.PAYMENT) {
            val payerUid = expense.paidBy.firstOrNull()?.uid
            val receiverUid = expense.participants.firstOrNull()?.uid

            return when (currentUserId) {
                payerUid -> "You paid ${formatCurrency(expense.totalAmount)}" to NegativeRed
                receiverUid -> "You received ${formatCurrency(expense.totalAmount)}" to PositiveGreen
                else -> "Payment" to Color.Gray
            }
        }

        val userPaid = expense.paidBy.find { it.uid == currentUserId }?.paidAmount ?: 0.0
        val userOwed = expense.participants.find { it.uid == currentUserId }?.owesAmount ?: 0.0
        val netAmount = userPaid - userOwed

        return when {
            netAmount > 0.01 -> "you lent MYR%.2f".format(netAmount) to PositiveGreen
            netAmount < -0.01 -> "you borrowed MYR%.2f".format(netAmount.absoluteValue) to NegativeRed
            else -> "settled" to Color.Gray
        }
    }

    fun formatPayerSummary(expense: Expense): String {
        if (expense.expenseType == ExpenseType.PAYMENT) {
            val payerUid = expense.paidBy.firstOrNull()?.uid
            val receiverUid = expense.participants.firstOrNull()?.uid

            val payerName = _uiState.value.usersMap[payerUid]?.username
                ?: (if (payerUid == currentUserId) "You" else "User...")
            val receiverName = _uiState.value.usersMap[receiverUid]?.username
                ?: (if (receiverUid == currentUserId) "you" else "User...")

            return "$payerName paid $receiverName"
        }

        val payerUid = expense.paidBy.firstOrNull()?.uid
        val payerName = when {
            payerUid == null -> "N/A"
            payerUid == currentUserId -> "You"
            else -> _uiState.value.usersMap[payerUid]?.username
                ?: _uiState.value.usersMap[payerUid]?.fullName
                ?: "User ${payerUid.take(4)}..."
        }

        return when (expense.paidBy.size) {
            0 -> "Error: No payer"
            1 -> "$payerName paid MYR%.2f".format(expense.totalAmount)
            else -> "${expense.paidBy.size} people paid MYR%.2f".format(expense.totalAmount)
        }
    }

    private fun formatCurrency(amount: Double): String {
        return "MYR%.2f".format(amount.absoluteValue)
    }

    private fun roundToCents(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }

    /**
     * Builds a list of activity cards (shared group cards + payment cards + individual non-group expense cards)
     * sorted chronologically by date
     */
    private fun buildActivityCards(allExpenses: List<Expense>, sharedGroups: List<Group>): List<ActivityCard> {
        val activityCards = mutableListOf<ActivityCard>()

        // Separate payments from regular expenses
        val payments = allExpenses.filter { it.expenseType == ExpenseType.PAYMENT }
        val regularExpenses = allExpenses.filter { it.expenseType != ExpenseType.PAYMENT }

        // Separate non-group expenses from group expenses
        val nonGroupExpenses = regularExpenses.filter { it.groupId == "non_group" || it.groupId == null }
        val groupExpenses = regularExpenses.filter { it.groupId != "non_group" && it.groupId != null }

        // Group regular group expenses by groupId (excluding non-group)
        val expensesByGroup = groupExpenses.groupBy { it.groupId }

        // Process each group
        expensesByGroup.forEach { (groupId, expenses) ->
            // Calculate net balance for this group (including payment effects)
            var groupBalance = 0.0
            var mostRecentDate = 0L

            expenses.forEach { expense ->
                val balanceChange = calculateBalanceChangeForExpense(expense, currentUserId!!, friendId)
                groupBalance += balanceChange
                if (expense.date > mostRecentDate) {
                    mostRecentDate = expense.date
                }
            }

            // Factor in payments for this group
            val groupPayments = payments.filter { it.groupId == groupId }
            groupPayments.forEach { payment ->
                val balanceChange = calculateBalanceChangeForExpense(payment, currentUserId!!, friendId)
                groupBalance += balanceChange
                if (payment.date > mostRecentDate) {
                    mostRecentDate = payment.date
                }
            }

            // Only add card if balance is non-zero
            if (groupBalance.absoluteValue > 0.01) {
                val group = sharedGroups.find { it.id == groupId }
                val groupName = group?.name ?: "Unknown Group"

                activityCards.add(
                    ActivityCard.SharedGroupCard(
                        group = group,
                        groupName = groupName,
                        balance = roundToCents(groupBalance),
                        mostRecentDate = mostRecentDate,
                        groupId = groupId
                    )
                )
            }
        }

        // Add individual non-group expense cards as PaymentCards
        // (so they show up as individual items instead of being grouped)
        nonGroupExpenses.forEach { expense ->
            activityCards.add(ActivityCard.PaymentCard(expense = expense))
        }

        // Add payment cards
        payments.forEach { payment ->
            activityCards.add(ActivityCard.PaymentCard(expense = payment))
        }

        // Sort by date descending (most recent first)
        return activityCards.sortedByDescending { it.date }
    }
}