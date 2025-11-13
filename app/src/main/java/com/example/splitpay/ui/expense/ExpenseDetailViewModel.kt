package com.example.splitpay.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Activity
import com.example.splitpay.data.model.ActivityType
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface ExpenseDetailUiEvent {
    object NavigateBack : ExpenseDetailUiEvent
    data class NavigateToEdit(val expenseId: String, val groupId: String?) : ExpenseDetailUiEvent
}

data class ExpenseDetailUiState(
    val isLoading: Boolean = true,
    val expense: Expense? = null,
    val usersMap: Map<String, User> = emptyMap(), // Map of UID to User for displaying names
    val groupName: String? = null,
    val error: String? = null,
    val showDeleteDialog: Boolean = false
)

class ExpenseDetailViewModel(
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val activityRepository: ActivityRepository = ActivityRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseDetailUiState())
    val uiState: StateFlow<ExpenseDetailUiState> = _uiState

    private val _uiEvent = MutableSharedFlow<ExpenseDetailUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    /**
     * Loads the expense details and related data (user names, group name)
     */
    fun loadExpense(expenseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Fetch the expense
                val expenseResult = expenseRepository.getExpenseById(expenseId)
                val expense = expenseResult.getOrNull()

                if (expense == null) {
                    _uiState.update { it.copy(isLoading = false, error = "Expense not found") }
                    return@launch
                }

                // Collect all unique user IDs from payers and participants
                val allUserIds = (expense.paidBy.map { it.uid } + expense.participants.map { it.uid }).distinct()

                // Fetch user profiles for all involved users (with caching)
                val users = userRepository.getProfilesForFriendsCached(allUserIds)
                val usersMap = users.associateBy { it.uid }

                // Fetch group name if it's a group expense
                var groupName: String? = null
                if (expense.groupId != null && expense.groupId != "non_group") {
                    val group = groupsRepository.getGroupById(expense.groupId)
                    groupName = group?.name
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        expense = expense,
                        usersMap = usersMap,
                        groupName = groupName,
                        error = null
                    )
                }
            } catch (e: Exception) {
                logE("Failed to load expense details: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load expense: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Navigates to edit mode for this expense
     */
    fun onEditClick() {
        val expense = _uiState.value.expense ?: return
        viewModelScope.launch {
            _uiEvent.emit(ExpenseDetailUiEvent.NavigateToEdit(expense.id, expense.groupId))
        }
    }

    /**
     * Shows the delete confirmation dialog
     */
    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    /**
     * Dismisses the delete confirmation dialog
     */
    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    /**
     * Deletes the expense and creates an EXPENSE_DELETED activity
     */
    fun deleteExpense() {
        val expense = _uiState.value.expense ?: return
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteDialog = false) }

            try {
                // Delete the expense from Firestore
                val deleteResult = expenseRepository.deleteExpense(expense.id)

                deleteResult.onSuccess {
                    logD("Expense deleted successfully: ${expense.id}")

                    // Create EXPENSE_DELETED activity
                    try {
                        // Get actor name (with caching)
                        val actorProfile = userRepository.getUserProfileCached(currentUser.uid)
                        val actorName = actorProfile?.username?.takeIf { it.isNotBlank() }
                            ?: actorProfile?.fullName?.takeIf { it.isNotBlank() }
                            ?: "Someone"

                        // Get all involved user IDs
                        val allInvolvedUids = (expense.paidBy.map { it.uid } + expense.participants.map { it.uid }).distinct()

                        // Create activity
                        val activity = Activity(
                            activityType = ActivityType.EXPENSE_DELETED.name,
                            actorUid = currentUser.uid,
                            actorName = actorName,
                            involvedUids = allInvolvedUids,
                            groupId = expense.groupId,
                            groupName = _uiState.value.groupName ?: "Non-group",
                            entityId = expense.id, // Reference the deleted expense ID
                            displayText = expense.description,
                            totalAmount = expense.totalAmount,
                            financialImpacts = null // No financial impact for deletion
                        )
                        activityRepository.logActivity(activity)
                        logD("Logged EXPENSE_DELETED activity for expense ${expense.id}")
                    } catch (e: Exception) {
                        logE("Failed to log EXPENSE_DELETED activity: ${e.message}")
                    }

                    // Navigate back
                    _uiEvent.emit(ExpenseDetailUiEvent.NavigateBack)
                }.onFailure { error ->
                    logE("Failed to delete expense: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to delete expense: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                logE("Error during expense deletion: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to delete expense: ${e.message}"
                    )
                }
            }
        }
    }
}
