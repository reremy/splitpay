package com.example.splitpay.ui.groups

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Expense
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
    val membersMap: Map<String, User> = emptyMap() // To store member details for names
    // Add other states later (isCurrentUserAdmin, friendsList, membersList, etc.)
)

data class MemberBalanceDetail(
    val memberName: String,
    val amount: Double // Positive: They owe you, Negative: You owe them
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
        if (groupId == "non_group" || groupId.isBlank() || currentUserId == null) {
            // Handle non-group or invalid states separately (maybe show different header?)
            // For now, let's just clear balances and show the placeholder group
            val nonGroupPlaceholder = Group(id = "non_group", name = "Non-group Expenses", iconIdentifier = "info")
            _uiState.update { it.copy(
                isLoadingGroup = false, isLoadingExpenses = false, group = nonGroupPlaceholder,
                currentUserOverallBalance = 0.0, balanceBreakdown = emptyList(), membersMap = emptyMap(), error = null
            )}
            // Start collecting non-group expenses if needed
            collectNonGroupExpenses()
            return
        }

        if (currentGroupId == groupId && dataCollectionJob?.isActive == true) {
            return // Already collecting for this group
        }

        currentGroupId = groupId
        dataCollectionJob?.cancel() // Cancel previous job

        _uiState.update { it.copy(isLoadingGroup = true, isLoadingExpenses = true, error = null) }

        dataCollectionJob = viewModelScope.launch {
            // Combine the group flow and the expenses flow
            combine(
                groupsRepository.getGroupFlow(groupId).filterNotNull(), // Ensure group is not null
                expenseRepository.getExpensesFlowForGroup(groupId)
            ) { group, expenses ->
                // This block executes whenever the group OR expenses change
                _uiState.update { it.copy(isLoadingExpenses = true) } // Indicate recalculation

                // Fetch member details (needed for names)
                val members = userRepository.getProfilesForFriends(group.members)
                val membersMap = members.associateBy { it.uid } // Create UID -> User map

                // --- Calculate Balances ---
                val balances = mutableMapOf<String, Double>() // UID -> Balance within this group
                expenses.forEach { expense ->
                    expense.paidBy.forEach { payer ->
                        balances[payer.uid] = (balances[payer.uid] ?: 0.0) + payer.paidAmount
                    }
                    expense.participants.forEach { participant ->
                        balances[participant.uid] = (balances[participant.uid] ?: 0.0) - participant.owesAmount
                    }
                }

                // Calculate breakdown relative to current user
                val breakdown = mutableListOf<MemberBalanceDetail>()
                var overallBalance = 0.0

                balances.forEach { (uid, netAmount) ->
                    if (uid == currentUserId) {
                        overallBalance = roundToCents(netAmount)
                    } else {
                        // Calculate balance *between* currentUser and this member *within this group*
                        // This involves considering only expenses involving both
                        var balanceWithMember = 0.0
                        expenses.filter { exp ->
                            // Check if both users are involved (paid or participated)
                            val involvedUids = (exp.paidBy.map { it.uid } + exp.participants.map { it.uid }).toSet()
                            involvedUids.contains(currentUserId) && involvedUids.contains(uid)
                        }.forEach { relevantExpense ->
                            val currentUserPaid = relevantExpense.paidBy.find { it.uid == currentUserId }?.paidAmount ?: 0.0
                            val currentUserOwes = relevantExpense.participants.find { it.uid == currentUserId }?.owesAmount ?: 0.0
                            val memberPaid = relevantExpense.paidBy.find { it.uid == uid }?.paidAmount ?: 0.0
                            val memberOwes = relevantExpense.participants.find { it.uid == uid }?.owesAmount ?: 0.0

                            // How much currentUser contributed net vs how much member contributed net for this expense
                            val currentUserNet = currentUserPaid - currentUserOwes
                            val memberNet = memberPaid - memberOwes

                            val numParticipants = relevantExpense.participants.size.toDouble().coerceAtLeast(1.0)
                            balanceWithMember += (currentUserNet - memberNet) / numParticipants
                        }

                        val roundedBalanceWithMember = roundToCents(balanceWithMember)
                        if (roundedBalanceWithMember.absoluteValue > 0.01) {
                            breakdown.add(
                                MemberBalanceDetail(
                                    memberName = membersMap[uid]?.username ?: membersMap[uid]?.fullName ?: "User $uid", // Get name
                                    amount = roundedBalanceWithMember
                                )
                            )
                        }
                    }
                }

                // Update UI State with everything
                Triple(group, expenses, Triple(overallBalance, breakdown.sortedByDescending { it.amount.absoluteValue }, membersMap))
            }.collectLatest { (group, expenses, balanceData) ->
                val (overallBalance, breakdown, membersMap) = balanceData
                _uiState.update {
                    it.copy(
                        isLoadingGroup = false,
                        isLoadingExpenses = false,
                        group = group,
                        expenses = expenses,
                        currentUserOverallBalance = overallBalance,
                        balanceBreakdown = breakdown,
                        membersMap = membersMap,
                        error = null
                    )
                }
            }
        }
    }

    // Separate function to collect non-group expenses if needed by non-group logic
    private fun collectNonGroupExpenses() {
        // This might need adjustment if non-group details require specific calculations
        dataCollectionJob = viewModelScope.launch {
            expenseRepository.getNonGroupExpensesFlow(currentUserId ?: "").collectLatest { expenses ->
                _uiState.update { it.copy(isLoadingExpenses = false, expenses = expenses) }
            }
        }
    }

    // Helper function for rounding
    private fun roundToCents(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }

    // --- Helper to calculate user's net amount for a single expense ---
    fun calculateUserLentBorrowed(expense: Expense): Pair<String, Color> {
        if (currentUserId == null) return "" to Color.Gray // Should not happen if logged in

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
        // --- Refine Payer Summary to use membersMap ---
        val payerUid = expense.paidBy.firstOrNull()?.uid
        val payerName = when {
            payerUid == null -> "N/A"
            payerUid == currentUserId -> "You"
            else -> _uiState.value.membersMap[payerUid]?.username // Use username from map
                ?: _uiState.value.membersMap[payerUid]?.fullName // Fallback to full name
                ?: "User ${payerUid.take(4)}..." // Fallback placeholder
        }

        return when (expense.paidBy.size) {
            0 -> "Error: No payer"
            1 -> "$payerName paid MYR%.2f".format(expense.totalAmount)
            else -> "${expense.paidBy.size} people paid MYR%.2f".format(expense.totalAmount)
        }
    }


    // --- Placeholder functions for menu actions (to be implemented later) ---
    fun onEditNameClicked() { /* TODO */ }
    fun onChangeIconClicked() { /* TODO */ }
    fun onAddMembersClicked() { /* TODO */ }
    fun onRemoveMembersClicked() { /* TODO */ }
    fun onLeaveGroupClicked() { /* TODO */ }
    fun onDeleteGroupClicked() { /* TODO */ }
}