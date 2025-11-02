package com.example.splitpay.ui.settleUp

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.ExpenseType
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

// Re-defined here for this ViewModel. Ideally, this would be in a shared model file.
data class MemberBalanceDetail(
    val memberUid: String,
    val memberName: String,
    val amount: Double // Positive: They owe you, Negative: You owe them
)

data class SettleUpUiState(
    val isLoading: Boolean = true,
    val group: Group? = null,
    val balanceBreakdown: List<MemberBalanceDetail> = emptyList(),
    val error: String? = null
)

sealed interface SettleUpUiEvent {
    object NavigateBack : SettleUpUiEvent
    data class NavigateToRecordPayment(val memberUid: String, val balance: Double) : SettleUpUiEvent
    object NavigateToMoreOptions : SettleUpUiEvent
}

class SettleUpViewModel(
    private val groupsRepository: GroupsRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettleUpUiState())
    val uiState: StateFlow<SettleUpUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<SettleUpUiEvent>()
    val uiEvent: SharedFlow<SettleUpUiEvent> = _uiEvent.asSharedFlow()

    private val groupId: String = savedStateHandle["groupId"] ?: ""
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        if (groupId.isBlank() || currentUserId == null) {
            _uiState.update { it.copy(isLoading = false, error = "Group ID missing or user not logged in.") }
        } else {
            loadBalances()
        }
    }

    private fun loadBalances() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Combine group details and expenses
            try {
                groupsRepository.getGroupFlow(groupId).collectLatest { group ->
                    if (group == null) {
                        _uiState.update { it.copy(isLoading = false, error = "Group not found.") }
                        return@collectLatest
                    }

                    expenseRepository.getExpensesFlowForGroup(groupId).collectLatest { expenses ->
                        _uiState.update { it.copy(isLoading = true) } // Show loading for recalc

                        val members = userRepository.getProfilesForFriends(group.members)
                        val membersMap = members.associateBy { it.uid }

                        val breakdown = mutableListOf<MemberBalanceDetail>()

                        group.members.forEach { memberUid ->
                            if (memberUid != currentUserId) {
                                // Calculate balance *between* currentUser and this member *within this group*
                                var balanceWithMember = 0.0
                                expenses.filter { exp ->
                                    val involvedUids = (exp.paidBy.map { it.uid } + exp.participants.map { it.uid }).toSet()
                                    involvedUids.contains(currentUserId) && involvedUids.contains(memberUid)
                                }.forEach { relevantExpense ->
                                    val currentUserPaid = relevantExpense.paidBy.find { it.uid == currentUserId }?.paidAmount ?: 0.0
                                    val currentUserOwes = relevantExpense.participants.find { it.uid == currentUserId }?.owesAmount ?: 0.0
                                    val memberPaid = relevantExpense.paidBy.find { it.uid == memberUid }?.paidAmount ?: 0.0
                                    val memberOwes = relevantExpense.participants.find { it.uid == memberUid }?.owesAmount ?: 0.0

                                    // --- *** THIS IS THE CORRECTED LOGIC *** ---
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
                                    breakdown.add(
                                        MemberBalanceDetail(
                                            memberUid = memberUid,
                                            memberName = membersMap[memberUid]?.username ?: membersMap[memberUid]?.fullName ?: "User $memberUid",
                                            amount = roundedBalanceWithMember
                                        )
                                    )
                                }
                            }
                        }

                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                group = group,
                                balanceBreakdown = breakdown.sortedByDescending { it.amount.absoluteValue },
                                error = null
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                logE("Error loading balances for SettleUp: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Failed to load balances.") }
            }
        }
    }

    fun onMemberClick(member: MemberBalanceDetail) {
        viewModelScope.launch {
            _uiEvent.emit(SettleUpUiEvent.NavigateToRecordPayment(member.memberUid, member.amount))
        }
    }

    fun onMoreOptionsClick() {
        viewModelScope.launch {
            _uiEvent.emit(SettleUpUiEvent.NavigateToMoreOptions)
        }
    }

    fun onCloseClick() {
        viewModelScope.launch {
            _uiEvent.emit(SettleUpUiEvent.NavigateBack)
        }
    }

    private fun roundToCents(value: Double): Double {
        return (value * 100.0).roundToInt() / 100.0
    }
}