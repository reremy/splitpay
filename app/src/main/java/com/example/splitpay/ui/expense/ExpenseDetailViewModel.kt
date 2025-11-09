package com.example.splitpay.ui.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logE
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExpenseDetailUiState(
    val isLoading: Boolean = true,
    val expense: Expense? = null,
    val usersMap: Map<String, User> = emptyMap(), // Map of UID to User for displaying names
    val groupName: String? = null,
    val error: String? = null
)

class ExpenseDetailViewModel(
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val groupsRepository: GroupsRepository = GroupsRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExpenseDetailUiState())
    val uiState: StateFlow<ExpenseDetailUiState> = _uiState

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

                // Fetch user profiles for all involved users
                val users = userRepository.getProfilesForFriends(allUserIds)
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
}
