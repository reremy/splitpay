package com.example.splitpay.ui.activityDetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.ActivityType
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ActivityDetailViewModel(
    private val activityRepository: ActivityRepository,
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(ActivityDetailUiState())
    val uiState: StateFlow<ActivityDetailUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ActivityDetailUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val activityId: String? = savedStateHandle["activityId"]
    private val expenseId: String? = savedStateHandle["expenseId"]
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        logD("ActivityDetailViewModel init - activityId: $activityId, expenseId: $expenseId, currentUserId: $currentUserId")

        if (currentUserId == null) {
            logE("Current user is null")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "User not logged in."
                )
            }
        } else if (activityId.isNullOrBlank() && expenseId.isNullOrBlank()) {
            logE("Both activityId and expenseId are null or blank")
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Invalid activity ID or expense ID."
                )
            }
        } else {
            logD("Loading activity details...")
            _uiState.update { it.copy(currentUserId = currentUserId) }
            loadActivityDetails()
        }
    }

    private fun loadActivityDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                // Load activity - either by activityId or by expenseId (entityId)
                logD("Loading activity with activityId: $activityId, expenseId: $expenseId")
                val activityResult = if (!activityId.isNullOrBlank()) {
                    logD("Fetching activity by ID: $activityId")
                    activityRepository.getActivityById(activityId)
                } else if (!expenseId.isNullOrBlank()) {
                    logD("Fetching activity by expense ID: $expenseId")
                    activityRepository.getActivityByEntityId(expenseId)
                } else {
                    logE("No valid ID provided")
                    Result.failure(IllegalArgumentException("No valid ID provided"))
                }

                logD("Activity result: isSuccess=${activityResult.isSuccess}, activity=${activityResult.getOrNull()}")

                var activity = activityResult.getOrNull()

                // If we have an expenseId but no activity, load expense directly and create a minimal activity
                if (activity == null && !expenseId.isNullOrBlank()) {
                    logD("No activity found for expense $expenseId, loading expense directly")
                    val expenseResult = expenseRepository.getExpenseById(expenseId)
                    val expense = expenseResult.getOrNull()

                    if (expense != null) {
                        logD("Loaded expense directly: ${expense.id}")
                        // Create a minimal activity for display purposes
                        activity = com.example.splitpay.data.model.Activity(
                            id = "expense_${expense.id}",
                            activityType = com.example.splitpay.data.model.ActivityType.EXPENSE_ADDED.name,
                            actorUid = expense.createdByUid,
                            actorName = "Unknown",
                            timestamp = expense.date,
                            groupId = expense.groupId,
                            groupName = null,
                            entityId = expense.id,
                            displayText = expense.description,
                            totalAmount = expense.totalAmount,
                            involvedUids = listOf(expense.createdByUid) + expense.paidBy.map { it.uid } + expense.participants.map { it.uid }
                        )

                        // Pre-load the expense into state
                        _uiState.update { it.copy(expense = expense, activity = activity) }
                    }
                }

                if (activity == null) {
                    val errorMsg = "Activity and expense not found"
                    logE(errorMsg)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = errorMsg
                        )
                    }
                    return@launch
                }

                _uiState.update { it.copy(activity = activity) }

                // Load actor user
                val actorUser = userRepository.getUserProfile(activity.actorUid)
                _uiState.update { it.copy(actorUser = actorUser) }

                // Load involved users
                val involvedUsers = activity.involvedUids.mapNotNull { uid ->
                    userRepository.getUserProfile(uid)
                }
                _uiState.update { it.copy(involvedUsers = involvedUsers) }

                // If this is an expense activity, load expense details (if not already loaded)
                if (activity.activityType == ActivityType.EXPENSE_ADDED.name ||
                    activity.activityType == ActivityType.EXPENSE_UPDATED.name ||
                    activity.activityType == ActivityType.EXPENSE_DELETED.name
                ) {
                    // Check if expense was already loaded above
                    var expense = _uiState.value.expense

                    // If not loaded yet, load it now
                    if (expense == null) {
                        activity.entityId?.let { expId ->
                            val expenseResult = expenseRepository.getExpenseById(expId)
                            expense = expenseResult.getOrNull()
                        }
                    }

                    if (expense != null) {
                        // Load payer users
                        val payersMap = expense.paidBy.associate { payer ->
                            payer.uid to userRepository.getUserProfile(payer.uid)
                        }.filterValues { it != null } as Map<String, com.example.splitpay.data.model.User>

                        // Load participant users
                        val participantsMap = expense.participants.associate { participant ->
                            participant.uid to userRepository.getUserProfile(participant.uid)
                        }.filterValues { it != null } as Map<String, com.example.splitpay.data.model.User>

                        _uiState.update {
                            it.copy(
                                expense = expense,
                                payers = payersMap,
                                participants = participantsMap
                            )
                        }
                    }
                }

                _uiState.update { it.copy(isLoading = false) }
                logD("Activity details loaded successfully for ID: $activityId")

            } catch (e: Exception) {
                logE("Error loading activity details: ${e.message}")
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Failed to load activity details: ${e.message}"
                    )
                }
            }
        }
    }

    fun onDeleteClick() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun onDismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun onConfirmDelete() {
        viewModelScope.launch {
            _uiState.update { it.copy(showDeleteDialog = false, isLoading = true) }

            try {
                val activity = _uiState.value.activity
                val expense = _uiState.value.expense

                // Delete the expense if it exists
                if (expense != null) {
                    val deleteExpenseResult = expenseRepository.deleteExpense(expense.id)
                    if (deleteExpenseResult.isFailure) {
                        throw Exception("Failed to delete expense: ${deleteExpenseResult.exceptionOrNull()?.message}")
                    }
                    logD("Expense deleted: ${expense.id}")
                }

                // Delete the activity
                if (activity != null) {
                    val deleteActivityResult = activityRepository.deleteActivity(activity.id)
                    if (deleteActivityResult.isFailure) {
                        throw Exception("Failed to delete activity: ${deleteActivityResult.exceptionOrNull()?.message}")
                    }
                    logD("Activity deleted: ${activity.id}")
                }

                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.emit(ActivityDetailUiEvent.DeleteSuccess)

            } catch (e: Exception) {
                logE("Error deleting activity/expense: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.emit(ActivityDetailUiEvent.ShowError("Failed to delete: ${e.message}"))
            }
        }
    }

    fun onEditClick() {
        viewModelScope.launch {
            val expense = _uiState.value.expense
            if (expense != null) {
                _uiEvent.emit(
                    ActivityDetailUiEvent.NavigateToEditExpense(
                        expenseId = expense.id,
                        groupId = expense.groupId
                    )
                )
            } else {
                _uiEvent.emit(ActivityDetailUiEvent.ShowError("Cannot edit this activity"))
            }
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.emit(ActivityDetailUiEvent.NavigateBack)
        }
    }
}
