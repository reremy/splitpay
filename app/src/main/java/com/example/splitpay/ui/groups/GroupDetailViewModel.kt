package com.example.splitpay.ui.groups

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.Group
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

// --- NEW: Define UI State Data Class ---
data class GroupDetailUiState(
    val group: Group? = null,
    val isLoadingGroup: Boolean = true, // Renamed for clarity
    val isLoadingExpenses: Boolean = true, // Separate loading state
    val expenses: List<Expense> = emptyList(), // <-- Add state for expenses
    val error: String? = null,
    // Add other states later (isCurrentUserAdmin, friendsList, membersList, etc.)
)

class GroupDetailViewModel(
    private val groupsRepository: GroupsRepository,
    private val expenseRepository: ExpenseRepository, // <-- Inject ExpenseRepository
    private val userRepository: UserRepository,     // <-- Inject UserRepository (if needed for names)
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupDetailUiState())
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    private var currentGroupId: String? = null
    private var groupListenerJob: Job? = null
    private var expenseListenerJob: Job? = null // Job for expense listener

    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid // Get current user ID

    private var groupIdToLoad: String? = null

    fun loadGroupAndExpenses(groupId: String) {
        if (groupId.isBlank()) {
            _uiState.update { it.copy(isLoadingGroup = false, isLoadingExpenses = false, error = "Group ID is invalid.") }
            return
        }

        // Prevent restarting listeners if already listening for the same ID (group or non-group)
        if (currentGroupId == groupId && expenseListenerJob?.isActive == true) {
            // For non-group, only expense listener matters
            if (groupId == "non_group") return
            // For regular groups, check group listener too
            if (groupListenerJob?.isActive == true) return
        }

        currentGroupId = groupId
        groupListenerJob?.cancel() // Cancel previous group listener (if any)
        expenseListenerJob?.cancel() // Cancel previous expense listener

        _uiState.update { it.copy(isLoadingGroup = true, isLoadingExpenses = true, error = null) }

        // --- Handle Non-Group Case ---
        if (groupId == "non_group") {
            // Create a mock Group object for display
            val nonGroupPlaceholder = Group(
                id = "non_group",
                name = "Non-group Expenses",
                iconIdentifier = "info", // Use a distinct identifier, maybe map to Icons.Default.Info later
                members = emptyList() // No specific members apply here in the same way
            )
            _uiState.update { it.copy(isLoadingGroup = false, group = nonGroupPlaceholder) } // Set mock group, stop group loading

            // Listener ONLY for Non-Group Expenses
            expenseListenerJob = viewModelScope.launch {
                expenseRepository.getNonGroupExpensesFlow(currentUserId ?: "").collectLatest { expenses ->
                    _uiState.update {
                        it.copy(
                            isLoadingExpenses = false,
                            expenses = expenses,
                            error = null // Clear any previous error
                        )
                    }
                }
            }
        }
        // --- Handle Regular Group Case ---
        else {
            // Listener for REAL Group Details
            groupListenerJob = viewModelScope.launch {
                groupsRepository.getGroupFlow(groupId).collectLatest { group ->
                    _uiState.update {
                        it.copy(
                            isLoadingGroup = false,
                            group = group,
                            error = if (group == null && !it.isLoadingExpenses) "Group not found or deleted." else it.error
                        )
                    }
                }
            }

            // Listener for Group Expenses
            expenseListenerJob = viewModelScope.launch {
                expenseRepository.getExpensesFlowForGroup(groupId).collectLatest { expenses ->
                    _uiState.update {
                        it.copy(
                            isLoadingExpenses = false,
                            expenses = expenses,
                            error = if (it.group == null && !it.isLoadingGroup) "Group not found or deleted." else it.error
                        )
                    }
                }
            }
        }
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
        return when (expense.paidBy.size) {
            0 -> "Error: No payer" // Should not happen
            1 -> {
                val payerUid = expense.paidBy.first().uid
                // TODO: Need a way to get user name from UID here. Pass user list or fetch?
                val payerName = if (payerUid == currentUserId) "You" else "User $payerUid" // Placeholder name
                "%s paid MYR%.2f".format(payerName, expense.totalAmount)
            }
            else -> "%d people paid MYR%.2f".format(expense.paidBy.size, expense.totalAmount)
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