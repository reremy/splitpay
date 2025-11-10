package com.example.splitpay.ui.groups

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.ExpenseType
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.theme.NegativeRed
import com.example.splitpay.ui.theme.PositiveGreen
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// --- NEW: Define UI State Data Class ---
data class GroupDetailUiState(
    val group: Group? = null,
    val isLoadingGroup: Boolean = true, // Renamed for clarity
    val isLoadingExpenses: Boolean = true, // Separate loading state
    val expenses: List<Expense> = emptyList(), // <-- Add state for expenses
    val error: String? = null,
    val currentUserOverallBalance: Double = 0.0,
    val balanceBreakdown: List<MemberBalanceDetail> = emptyList(),
    val membersMap: Map<String, User> = emptyMap(), // To store member details for names
    val totals: GroupTotals = GroupTotals() // Totals data
    // Add other states later (isCurrentUserAdmin, friendsList, membersList, etc.)
)

data class MemberBalanceDetail(
    val memberName: String,
    val amount: Double // Positive: They owe you, Negative: You owe them
)

data class GroupTotals(
    val totalSpent: Double = 0.0, // Sum of all expenses
    val totalPaidByMembers: Map<String, Double> = emptyMap(), // Amount each member contributed
    val averagePerPerson: Double = 0.0, // Average spending per person
    val totalPendingSettlements: Double = 0.0 // Sum of all unsettled balances
)

class GroupDetailViewModel(
    private val groupsRepository: GroupsRepository,
    private val expenseRepository: ExpenseRepository, // <-- Inject ExpenseRepository
    private val userRepository: UserRepository,     // <-- Inject UserRepository (if needed for names)
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private var currentGroupId: String? = null
    private var dataCollectionJob: Job? = null
    private var expenseListenerJob: Job? = null // Job for expense listener

    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid // Get current user ID

    private var groupIdToLoad: String? = null

    fun loadGroupAndExpenses(groupId: String) {
        if (groupId.isBlank() || currentUserId == null) {
            _uiState.update { it.copy(
                isLoadingGroup = false, isLoadingExpenses = false, group = null,
                currentUserOverallBalance = 0.0, balanceBreakdown = emptyList(), membersMap = emptyMap(), error = "Invalid Group or User"
            )}
            return
        }

        if (groupId == "non_group") {
            val nonGroupPlaceholder = Group(id = "non_group", name = "Non-group Expenses", iconIdentifier = "info")
            _uiState.update { it.copy(
                isLoadingGroup = false,
                isLoadingExpenses = true,
                group = nonGroupPlaceholder,
                currentUserOverallBalance = 0.0, balanceBreakdown = emptyList(), membersMap = emptyMap(), error = null
            )}

        } else if (currentGroupId == groupId && dataCollectionJob?.isActive == true) {
            return // Already collecting for this group
        } else {
            _uiState.update { it.copy(isLoadingGroup = true, isLoadingExpenses = true, error = null) }
        }

        currentGroupId = groupId
        dataCollectionJob?.cancel() // Cancel previous job

        dataCollectionJob = viewModelScope.launch {
            val groupSourceFlow = if (groupId == "non_group") {
                flowOf(uiState.value.group)
            } else {
                groupsRepository.getGroupFlow(groupId)
            }

            combine(
                groupSourceFlow.filterNotNull(),
                expenseRepository.getExpensesFlowForGroup(groupId)
            ) { group, expenses ->
                _uiState.update { it.copy(isLoadingExpenses = true) } // Indicate recalculation

                // --- START OF FIX (Reactivity Bug) ---
                // The order of operations is changed here.

                // 1. Calculate Balances FIRST to find out *who* is involved
                val balances = mutableMapOf<String, Double>()
                expenses.forEach { expense ->
                    expense.paidBy.forEach { payer ->
                        balances[payer.uid] = (balances[payer.uid] ?: 0.0) + payer.paidAmount
                    }
                    expense.participants.forEach { participant ->
                        balances[participant.uid] = (balances[participant.uid] ?: 0.0) - participant.owesAmount
                    }
                }

                // 2. Determine which UIDs we need to fetch profiles for
                val memberUidsToFetch = if (groupId == "non_group") {
                    balances.keys.toList() // Get all UIDs from the calculated balances
                } else {
                    group.members // Use the official group members list
                }

                // 3. Fetch member details for *only* the relevant UIDs
                val membersMap = if (memberUidsToFetch.isNotEmpty()) {
                    try {
                        userRepository.getProfilesForFriends(memberUidsToFetch).associateBy { it.uid }
                    } catch (e: Exception) {
                        emptyMap<String, User>()
                    }
                } else {
                    emptyMap<String, User>()
                }
                // --- END OF FIX ---


                // 4. Calculate Balances Breakdown
                val breakdown = mutableListOf<MemberBalanceDetail>()
                var overallBalance = 0.0

                // Iterate over the UIDs we just fetched
                memberUidsToFetch.forEach { uid ->
                    if (uid == currentUserId) {
                        overallBalance = roundToCents(balances[uid] ?: 0.0)
                    } else {
                        // Calculate balance *between* currentUser and this member
                        var balanceWithMember = 0.0
                        expenses.filter { exp ->
                            val involvedUids = (exp.paidBy.map { it.uid } + exp.participants.map { it.uid }).toSet()
                            involvedUids.contains(currentUserId) && involvedUids.contains(uid)
                        }.forEach { relevantExpense ->
                            val currentUserPaid = relevantExpense.paidBy.find { it.uid == currentUserId }?.paidAmount ?: 0.0
                            val currentUserOwes = relevantExpense.participants.find { it.uid == currentUserId }?.owesAmount ?: 0.0
                            val memberPaid = relevantExpense.paidBy.find { it.uid == uid }?.paidAmount ?: 0.0
                            val memberOwes = relevantExpense.participants.find { it.uid == uid }?.owesAmount ?: 0.0

                            if (relevantExpense.expenseType == ExpenseType.PAYMENT) {
                                if (currentUserPaid > 0) {
                                    balanceWithMember += currentUserPaid
                                } else if (memberPaid > 0) {
                                    balanceWithMember -= memberPaid
                                }
                            } else {
                                val currentUserNet = currentUserPaid - currentUserOwes
                                val memberNet = memberPaid - memberOwes
                                val numParticipants = if (groupId == "non_group" && relevantExpense.participants.size == 2) {
                                    2.0
                                } else {
                                    relevantExpense.participants.size.toDouble().coerceAtLeast(1.0)
                                }
                                balanceWithMember += (currentUserNet - memberNet) / numParticipants
                            }
                        }

                        val roundedBalanceWithMember = roundToCents(balanceWithMember)
                        if (roundedBalanceWithMember.absoluteValue > 0.01) {
                            breakdown.add(
                                MemberBalanceDetail(
                                    // This will now work, because membersMap is populated
                                    memberName = membersMap[uid]?.username ?: membersMap[uid]?.fullName ?: "User $uid",
                                    amount = roundedBalanceWithMember
                                )
                            )
                        }
                    }
                }

                // 5. Calculate Totals
                val totals = calculateTotals(expenses, balances, memberUidsToFetch.size)

                // Update UI State with everything
                Triple(group, expenses, Triple(overallBalance, breakdown.sortedByDescending { it.amount.absoluteValue }, membersMap), totals)
            }.collectLatest { (groupFromFlow, expenses, balanceData, totals) ->
                val (overallBalance, breakdown, membersMap) = balanceData
                _uiState.update {
                    it.copy(
                        isLoadingGroup = false,
                        isLoadingExpenses = false,
                        group = if (groupId == "non_group") it.group else groupFromFlow,
                        expenses = expenses,
                        currentUserOverallBalance = overallBalance,
                        balanceBreakdown = breakdown,
                        membersMap = membersMap,
                        totals = totals,
                        error = null
                    )
                }
            }
        }
    }

    // Helper function for rounding
    private fun roundToCents(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }

    // Helper function to calculate totals
    private fun calculateTotals(
        expenses: List<Expense>,
        balances: Map<String, Double>,
        memberCount: Int
    ): GroupTotals {
        // Calculate total spent (sum of all expense amounts, excluding payments)
        val totalSpent = expenses
            .filter { it.expenseType != ExpenseType.PAYMENT }
            .sumOf { it.totalAmount }

        // Calculate total paid by each member
        val totalPaidByMembers = mutableMapOf<String, Double>()
        expenses.forEach { expense ->
            expense.paidBy.forEach { payer ->
                totalPaidByMembers[payer.uid] = (totalPaidByMembers[payer.uid] ?: 0.0) + payer.paidAmount
            }
        }

        // Calculate average per person
        val averagePerPerson = if (memberCount > 0) {
            roundToCents(totalSpent / memberCount)
        } else {
            0.0
        }

        // Calculate total pending settlements (sum of absolute values of unsettled balances)
        val totalPendingSettlements = balances.values
            .filter { it.absoluteValue > 0.01 }
            .sumOf { it.absoluteValue } / 2.0 // Divide by 2 because each debt is counted twice

        return GroupTotals(
            totalSpent = roundToCents(totalSpent),
            totalPaidByMembers = totalPaidByMembers.mapValues { roundToCents(it.value) },
            averagePerPerson = averagePerPerson,
            totalPendingSettlements = roundToCents(totalPendingSettlements)
        )
    }

    // --- Helper to calculate user's net amount for a single expense ---
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

    // --- Helper to format payer summary ---
    fun formatPayerSummary(expense: Expense): String {
        if (expense.expenseType == ExpenseType.PAYMENT) {
            val payerUid = expense.paidBy.firstOrNull()?.uid
            val receiverUid = expense.participants.firstOrNull()?.uid

            val payerName = _uiState.value.membersMap[payerUid]?.username
                ?: (if (payerUid == currentUserId) "You" else "User...")
            val receiverName = _uiState.value.membersMap[receiverUid]?.username
                ?: (if (receiverUid == currentUserId) "you" else "User...")

            return "$payerName paid $receiverName"
        }

        val payerUid = expense.paidBy.firstOrNull()?.uid
        val payerName = when {
            payerUid == null -> "N/A"
            payerUid == currentUserId -> "You"
            else -> _uiState.value.membersMap[payerUid]?.username
                ?: _uiState.value.membersMap[payerUid]?.fullName
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

    // --- Placeholder functions for menu actions (to be implemented later) ---
    fun onEditNameClicked() { /* TODO */ }
    fun onChangeIconClicked() { /* TODO */ }
    fun onAddMembersClicked() { /* TODO */ }
    fun onRemoveMembersClicked() { /* TODO */ }
    fun onLeaveGroupClicked() { /* TODO */ }
    fun onDeleteGroupClicked() { /* TODO */ }
}