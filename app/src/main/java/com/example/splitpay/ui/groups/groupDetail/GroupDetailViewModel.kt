package com.example.splitpay.ui.groups.groupDetail

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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.get
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

//ui state for the screen
data class GroupDetailUiState(
    val group: Group? = null,
    val isLoadingGroup: Boolean = true,
    val isLoadingExpenses: Boolean = true,
    val expenses: List<Expense> = emptyList(),
    val error: String? = null,
    val currentUserOverallBalance: Double = 0.0,
    val balanceBreakdown: List<MemberBalanceDetail> = emptyList(),
    val membersMap: Map<String, User> = emptyMap(),
    val totals: GroupTotals = GroupTotals(),
    val chartData: ChartData = ChartData()

)

data class MemberBalanceDetail(
    val memberName: String,
    val amount: Double
)

data class GroupTotals(
    val totalSpent: Double = 0.0,
    val totalPaidByMembers: Map<String, Double> = emptyMap(),
    val averagePerPerson: Double = 0.0,
    val totalPendingSettlements: Double = 0.0
)

data class ChartData(
    val categoryBreakdown: List<CategorySpending> = emptyList(),
    val memberContributions: List<MemberContribution> = emptyList(),
    val dailySpending: List<DailySpending> = emptyList()
)

data class CategorySpending(
    val category: String,
    val amount: Double,
    val percentage: Float
)

data class MemberContribution(
    val memberName: String,
    val amount: Double,
    val percentage: Float
)

data class DailySpending(
    val date: Long,
    val amount: Double
)


private data class CalculatedData(
    val overallBalance: Double,
    val breakdown: List<MemberBalanceDetail>,
    val membersMap: Map<String, User>,
    val totals: GroupTotals,
    val chartData: ChartData
)

class GroupDetailViewModel(
    private val groupsRepository: GroupsRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private var currentGroupId: String? = null
    private var dataCollectionJob: Job? = null

    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid


    //run in the screen
    //fetches group info, expenses, balances, members, update the screen after
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

        // If we're already loading the requested group, do nothing
        } else if (currentGroupId == groupId && dataCollectionJob?.isActive == true) {
            return
        } else {
            _uiState.update { it.copy(isLoadingGroup = true, isLoadingExpenses = true, error = null) }
        }

        currentGroupId = groupId //set current group id
        dataCollectionJob?.cancel() //cancel previous group loading if any

        dataCollectionJob = viewModelScope.launch {
            //create data flow for group (non_group, or group from repository)
            val groupSourceFlow = if (groupId == "non_group") {
                //use existing placeholder group for non_group we created above
                flowOf(uiState.value.group)
            } else {
                //get group fx from grp repo
                groupsRepository.getGroupFlow(groupId)
            }

            //combine the flows of group and expenses
            combine(
                groupSourceFlow.filterNotNull(), //flow for the group
                expenseRepository.getExpensesFlowForGroup(groupId) //expenses flow for the group
            ) { group, expenses ->
                _uiState.update { it.copy(isLoadingExpenses = true) }

                val expensesToUse = if (groupId == "non_group") {
                    //for non-group, only show expenses that the current user paid/owed
                    expenses.filter { expense ->
                        val isInPaidBy = expense.paidBy.any { it.uid == currentUserId }
                        val isInParticipants = expense.participants.any { it.uid == currentUserId }
                        isInPaidBy || isInParticipants
                    }
                } else {
                    // show expenses of the group (not non-group)
                    expenses
                }


                //calc balances for each member in the group based on expenses
                val balances = mutableMapOf<String, Double>()
                expensesToUse.forEach { expense ->
                    expense.paidBy.forEach { payer ->
                        balances[payer.uid] = (balances[payer.uid] ?: 0.0) + payer.paidAmount
                    }
                    expense.participants.forEach { participant ->
                        balances[participant.uid] = (balances[participant.uid] ?: 0.0) - participant.owesAmount
                    }
                //at the end, the map 'balances' will have each member's uid and their net balance
                }


                //memberUidsToFetch - list of member uids to fetch profiles for
                val memberUidsToFetch = if (groupId == "non_group") {
                    balances.keys.toList()
                } else {
                    group.members
                }


                //fetch member profiles from user repo
                val membersMap = if (memberUidsToFetch.isNotEmpty()) {
                    try {
                        userRepository.getProfilesForFriendsCached(memberUidsToFetch).associateBy { it.uid }
                    } catch (e: Exception) {
                        emptyMap<String, User>()
                    }
                } else {
                    emptyMap<String, User>()
                }


                //calculate overall balance and breakdown with each member
                val breakdown = mutableListOf<MemberBalanceDetail>()
                var overallBalance = 0.0


                //foe each of the group's members in memberUidsToFetch
                memberUidsToFetch.forEach { uid ->
                    //if the uid == current user
                    if (uid == currentUserId) {
                        overallBalance = roundToCents(balances[uid] ?: 0.0)
                    } else {
                        //calculate balance between current user with member of the group
                        var balanceWithMember = 0.0
                        expensesToUse.filter { exp ->
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

                                    memberName = membersMap[uid]?.username ?: membersMap[uid]?.fullName ?: "User $uid",
                                    amount = roundedBalanceWithMember
                                )
                            )
                        }
                    }
                }


                val totals = calculateTotals(expensesToUse, balances, memberUidsToFetch.size)


                val chartData = calculateChartData(expensesToUse, membersMap)


                val calculatedData = CalculatedData(
                    overallBalance = overallBalance,
                    breakdown = breakdown.sortedByDescending { it.amount.absoluteValue },
                    membersMap = membersMap,
                    totals = totals,
                    chartData = chartData
                )


                Pair(group, Pair(expensesToUse, calculatedData))
            }.collectLatest { (groupFromFlow, expensesAndData) ->
                val (expenses, calculatedData) = expensesAndData
                _uiState.update {
                    it.copy(
                        isLoadingGroup = false,
                        isLoadingExpenses = false,
                        group = if (groupId == "non_group") it.group else groupFromFlow,
                        expenses = expenses,
                        currentUserOverallBalance = calculatedData.overallBalance,
                        balanceBreakdown = calculatedData.breakdown,
                        membersMap = calculatedData.membersMap,
                        totals = calculatedData.totals,
                        chartData = calculatedData.chartData,
                        error = null
                    )
                }
            }
        }
    }


    private fun roundToCents(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }


    private fun calculateTotals(
        expenses: List<Expense>,
        balances: Map<String, Double>,
        memberCount: Int
    ): GroupTotals {

        val totalSpent = expenses
            .filter { it.expenseType != ExpenseType.PAYMENT }
            .sumOf { it.totalAmount }


        val totalPaidByMembers = mutableMapOf<String, Double>()
        expenses.forEach { expense ->
            expense.paidBy.forEach { payer ->
                totalPaidByMembers[payer.uid] = (totalPaidByMembers[payer.uid] ?: 0.0) + payer.paidAmount
            }
        }


        val averagePerPerson = if (memberCount > 0) {
            roundToCents(totalSpent / memberCount)
        } else {
            0.0
        }


        val totalPendingSettlements = balances.values
            .filter { it.absoluteValue > 0.01 }
            .sumOf { it.absoluteValue } / 2.0

        return GroupTotals(
            totalSpent = roundToCents(totalSpent),
            totalPaidByMembers = totalPaidByMembers.mapValues { roundToCents(it.value) },
            averagePerPerson = averagePerPerson,
            totalPendingSettlements = roundToCents(totalPendingSettlements)
        )
    }


    private fun calculateChartData(
        expenses: List<Expense>,
        membersMap: Map<String, User>
    ): ChartData {

        val actualExpenses = expenses.filter { it.expenseType != ExpenseType.PAYMENT }

        if (actualExpenses.isEmpty()) {
            return ChartData()
        }


        val categoryTotals = mutableMapOf<String, Double>()
        actualExpenses.forEach { expense ->
            categoryTotals[expense.category] = (categoryTotals[expense.category] ?: 0.0) + expense.totalAmount
        }

        val totalSpent = categoryTotals.values.sum()
        val categoryBreakdown = categoryTotals.entries
            .sortedByDescending { it.value }
            .map { (category, amount) ->
                CategorySpending(
                    category = category,
                    amount = roundToCents(amount),
                    percentage = if (totalSpent > 0) (amount / totalSpent * 100).toFloat() else 0f
                )
            }


        val memberTotals = mutableMapOf<String, Double>()
        actualExpenses.forEach { expense ->
            expense.paidBy.forEach { payer ->
                memberTotals[payer.uid] = (memberTotals[payer.uid] ?: 0.0) + payer.paidAmount
            }
        }

        val totalContributions = memberTotals.values.sum()
        val memberContributions = memberTotals.entries
            .sortedByDescending { it.value }
            .map { (uid, amount) ->
                val memberName = membersMap[uid]?.username
                    ?: membersMap[uid]?.fullName
                    ?: "Unknown"
                MemberContribution(
                    memberName = memberName,
                    amount = roundToCents(amount),
                    percentage = if (totalContributions > 0) (amount / totalContributions * 100).toFloat() else 0f
                )
            }


        val dailyTotals = mutableMapOf<Long, Double>()
        actualExpenses.forEach { expense ->

            val dayStart = expense.date - (expense.date % (24 * 60 * 60 * 1000))
            dailyTotals[dayStart] = (dailyTotals[dayStart] ?: 0.0) + expense.totalAmount
        }

        val dailySpending = dailyTotals.entries
            .sortedBy { it.key }
            .map { (date, amount) ->
                DailySpending(
                    date = date,
                    amount = roundToCents(amount)
                )
            }

        return ChartData(
            categoryBreakdown = categoryBreakdown,
            memberContributions = memberContributions,
            dailySpending = dailySpending
        )
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


    fun exportGroupData(): String {
        val state = _uiState.value
        val group = state.group ?: return "No group data available"
        val expenses = state.expenses
        val totals = state.totals
        val balanceBreakdown = state.balanceBreakdown
        val membersMap = state.membersMap

        val sb = StringBuilder()


        sb.appendLine("=".repeat(50))
        sb.appendLine("GROUP EXPENSE REPORT")
        sb.appendLine("=".repeat(50))
        sb.appendLine()


        sb.appendLine("Group: ${group.name}")
        if (group.id != "non_group") {
            sb.appendLine("Members: ${group.members.size}")
            sb.appendLine()
            group.members.forEach { uid ->
                val member = membersMap[uid]
                val name = member?.username ?: member?.fullName ?: "Unknown"
                sb.appendLine("  • $name")
            }
        }
        sb.appendLine()


        sb.appendLine("-".repeat(50))
        sb.appendLine("SUMMARY")
        sb.appendLine("-".repeat(50))
        sb.appendLine("Total Spent: MYR %.2f".format(totals.totalSpent))
        sb.appendLine("Average Per Person: MYR %.2f".format(totals.averagePerPerson))
        sb.appendLine("Pending Settlements: MYR %.2f".format(totals.totalPendingSettlements))
        sb.appendLine()


        sb.appendLine("-".repeat(50))
        sb.appendLine("YOUR BALANCE")
        sb.appendLine("-".repeat(50))
        val overallBalance = state.currentUserOverallBalance
        when {
            overallBalance > 0.01 -> sb.appendLine("Overall, you are owed MYR %.2f".format(overallBalance))
            overallBalance < -0.01 -> sb.appendLine("Overall, you owe MYR %.2f".format(overallBalance.absoluteValue))
            else -> sb.appendLine("You are settled up!")
        }
        sb.appendLine()


        if (balanceBreakdown.isNotEmpty()) {
            sb.appendLine("Balance Breakdown:")
            balanceBreakdown.forEach { detail ->
                when {
                    detail.amount > 0.01 -> sb.appendLine("  • ${detail.memberName} owes you MYR %.2f".format(detail.amount))
                    detail.amount < -0.01 -> sb.appendLine("  • You owe ${detail.memberName} MYR %.2f".format(detail.amount.absoluteValue))
                }
            }
            sb.appendLine()
        }


        if (totals.totalPaidByMembers.isNotEmpty()) {
            sb.appendLine("-".repeat(50))
            sb.appendLine("CONTRIBUTIONS")
            sb.appendLine("-".repeat(50))
            totals.totalPaidByMembers.entries
                .sortedByDescending { it.value }
                .forEach { (uid, amount) ->
                    val memberName = membersMap[uid]?.username
                        ?: membersMap[uid]?.fullName
                        ?: "Unknown"
                    sb.appendLine("  $memberName: MYR %.2f".format(amount))
                }
            sb.appendLine()
        }


        sb.appendLine("-".repeat(50))
        sb.appendLine("EXPENSES (${expenses.size})")
        sb.appendLine("-".repeat(50))
        expenses.forEach { expense ->
            sb.appendLine()
            sb.appendLine(expense.description)
            sb.appendLine("  Amount: MYR %.2f".format(expense.totalAmount))
            sb.appendLine("  Date: ${
                SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(
                    Date(expense.date)
                )}")
            sb.appendLine("  Category: ${expense.category}")


            if (expense.paidBy.isNotEmpty()) {
                sb.append("  Paid by: ")
                expense.paidBy.forEach { payer ->
                    val payerName = membersMap[payer.uid]?.username
                        ?: membersMap[payer.uid]?.fullName
                        ?: "Unknown"
                    sb.append("$payerName (MYR %.2f) ".format(payer.paidAmount))
                }
                sb.appendLine()
            }


            if (expense.participants.isNotEmpty()) {
                sb.appendLine("  Split between:")
                expense.participants.forEach { participant ->
                    val participantName = membersMap[participant.uid]?.username
                        ?: membersMap[participant.uid]?.fullName
                        ?: "Unknown"
                    sb.appendLine("    - $participantName: MYR %.2f".format(participant.owesAmount))
                }
            }

            if (expense.memo.isNotEmpty()) {
                sb.appendLine("  Memo: ${expense.memo}")
            }
        }

        sb.appendLine()
        sb.appendLine("=".repeat(50))
        sb.appendLine("End of Report")
        sb.appendLine("=".repeat(50))

        return sb.toString()
    }

}