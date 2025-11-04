package com.example.splitpay.ui.recordPayment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.splitpay.data.model.Activity
import com.example.splitpay.data.model.ActivityType
import com.example.splitpay.data.model.Expense
import com.example.splitpay.data.model.ExpenseParticipant
import com.example.splitpay.data.model.ExpensePayer
import com.example.splitpay.data.model.ExpenseType
import com.example.splitpay.data.model.Group // <-- IMPORT GROUP
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository // <-- IMPORT REPO
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.example.splitpay.ui.expense.SplitType
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull // <-- IMPORT
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.absoluteValue

data class RecordPaymentUiState(
    val isLoading: Boolean = false, // For saving
    val isFetchingDetails: Boolean = true,
    val amount: String = "",
    val payer: User? = null,
    val receiver: User? = null,
    val isPayerUser: Boolean = true, // True if "You paid them", false if "They paid you"
    val error: String? = null,
    // --- NEW STATES for Bottom Bar ---
    val group: Group? = null, // To get the group name
    val date: Long = System.currentTimeMillis(),
    val memo: String = "",
    val imageUris: List<String> = emptyList(), // Placeholder for camera
    val isDatePickerDialogVisible: Boolean = false,
    val isMemoDialogVisible: Boolean = false
)

sealed interface RecordPaymentUiEvent {
    object NavigateBack : RecordPaymentUiEvent // For the back arrow
    object SaveSuccess : RecordPaymentUiEvent // For the checkmark
    data class ShowError(val message: String) : RecordPaymentUiEvent
}

class RecordPaymentViewModel(
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    private val groupsRepository: GroupsRepository,
    private val activityRepository: ActivityRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordPaymentUiState())
    val uiState: StateFlow<RecordPaymentUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<RecordPaymentUiEvent>()
    val uiEvent: SharedFlow<RecordPaymentUiEvent> = _uiEvent.asSharedFlow()

    private val groupId: String = savedStateHandle["groupId"] ?: ""
    private val memberUid: String = savedStateHandle["memberUid"] ?: ""
    private val balance: Double = savedStateHandle.get<String>("balance")?.toDoubleOrNull() ?: 0.0
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        if (currentUserId == null || memberUid.isBlank() || balance == 0.0) {
            _uiState.update { it.copy(isFetchingDetails = false, error = "Invalid payment details.") }
        } else {
            loadUserDetails()
        }
    }

    private fun loadUserDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingDetails = true, error = null) }
            try {
                // Determine who is payer and receiver based on balance
                // balance < 0 means "You owe them"
                val isPayerUser = balance < 0
                val payerUid = if (isPayerUser) currentUserId!! else memberUid
                val receiverUid = if (isPayerUser) memberUid else currentUserId!!

                // Fetch profiles and group details concurrently
                val payerDeferred = async { userRepository.getUserProfile(payerUid) }
                val receiverDeferred = async { userRepository.getUserProfile(receiverUid) }
                val groupDeferred = async { groupsRepository.getGroupFlow(groupId).firstOrNull() } // <-- FETCH GROUP

                val payer = payerDeferred.await()
                val receiver = receiverDeferred.await()
                val group = groupDeferred.await() // <-- AWAIT GROUP

                if (payer == null || receiver == null || group == null) {
                    throw Exception("Could not load user or group profiles.")
                }

                _uiState.update {
                    it.copy(
                        isFetchingDetails = false,
                        amount = balance.absoluteValue.toString(), // Pre-fill with full balance
                        payer = payer,
                        receiver = receiver,
                        isPayerUser = isPayerUser,
                        group = group // <-- STORE GROUP
                    )
                }

            } catch (e: Exception) {
                logE("Failed to load user details for payment: ${e.message}")
                _uiState.update { it.copy(isFetchingDetails = false, error = e.message) }
            }
        }
    }

    fun onAmountChange(newAmount: String) {
        // Basic filtering for valid number format (allows one '.')
        val cleanAmount = newAmount.filter { it.isDigit() || it == '.' }
        val decimalIndex = cleanAmount.indexOf('.')
        val validAmount = if (decimalIndex != -1) {
            val integerPart = cleanAmount.substringBefore('.')
            val decimalPart = cleanAmount.substringAfter('.').filter { it.isDigit() }.take(2)
            "$integerPart.$decimalPart"
        } else {
            cleanAmount // No decimal yet
        }

        if (validAmount.isEmpty() || validAmount.toDoubleOrNull() != null || validAmount == ".") {
            _uiState.update { it.copy(amount = validAmount) }
        }
    }

    fun onSavePayment() {
        val state = _uiState.value
        val totalAmount = state.amount.toDoubleOrNull() ?: 0.0

        if (state.isLoading || state.isFetchingDetails || state.payer == null || state.receiver == null || currentUserId == null) return

        if (totalAmount <= 0) {
            viewModelScope.launch { _uiEvent.emit(RecordPaymentUiEvent.ShowError("Amount must be greater than zero.")) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            // Create the "payment" expense
            val paymentDescription = if (state.isPayerUser) {
                "Payment to ${state.receiver.username}"
            } else {
                "Payment from ${state.payer.username}"
            }

            val newExpense = Expense(
                groupId = groupId,
                description = paymentDescription,
                totalAmount = totalAmount,
                createdByUid = currentUserId,
                date = state.date, // <-- USE STATE DATE
                splitType = SplitType.UNEQUALLY.name, // Treat it as a direct transfer
                paidBy = listOf(ExpensePayer(uid = state.payer.uid, paidAmount = totalAmount)),
                participants = listOf(ExpenseParticipant(uid = state.receiver.uid, owesAmount = totalAmount)),
                memo = state.memo.trim(), // <-- USE STATE MEMO
                imageUrls = emptyList(), // TODO: Handle image uploads later
                expenseType = ExpenseType.PAYMENT
            )

            val result = expenseRepository.addExpense(newExpense)
            result.onSuccess {
                logD("Settlement payment saved successfully.")
                viewModelScope.launch {
                    try {
                        // The Payer is the "actor"
                        val actor = state.payer
                        // The Receiver is the "other person" in the text
                        val receiver = state.receiver
                        val group = state.group!! // We checked for null at the start

                        // Calculate financial impact for balance sheet
                        // Payer's balance is restored (e.g., -500 -> 0), so +Amount
                        // Receiver's balance is restored (e.g., +500 -> 0), so -Amount
                        val financialImpacts = mapOf(
                            actor.uid to totalAmount,
                            receiver.uid to -totalAmount
                        )

                        val activity = Activity(
                            activityType = ActivityType.PAYMENT_MADE.name,
                            actorUid = actor.uid,
                            actorName = actor.username,
                            involvedUids = group.members, // All group members can see
                            groupId = group.id,
                            groupName = group.name,
                            displayText = receiver.username, // The "other person"
                            financialImpacts = financialImpacts
                        )
                        activityRepository.logActivity(activity)
                        logD("Logged PAYMENT_MADE activity for group ${group.id}")
                    } catch (e: Exception) {
                        logE("Failed to log PAYMENT_MADE activity: ${e.message}")
                    }
                }

                _uiEvent.emit(RecordPaymentUiEvent.SaveSuccess) // Go back on success
            }.onFailure { e ->
                logE("Failed to save settlement: ${e.message}")
                _uiState.update { it.copy(isLoading = false, error = "Failed to save payment.") }
                viewModelScope.launch { _uiEvent.emit(RecordPaymentUiEvent.ShowError("Failed to save payment: ${e.message}")) }
            }
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.emit(RecordPaymentUiEvent.NavigateBack)
        }
    }

    // Event Handlers for Bottom Bar ---
    fun showDatePickerDialog(isVisible: Boolean) {
        _uiState.update { it.copy(isDatePickerDialogVisible = isVisible) }
    }

    fun onDateSelected(newDateMillis: Long) {
        _uiState.update { it.copy(date = newDateMillis, isDatePickerDialogVisible = false) }
    }

    fun showMemoDialog(isVisible: Boolean) {
        _uiState.update { it.copy(isMemoDialogVisible = isVisible) }
    }

    fun onMemoSaved(finalMemo: String) {
        _uiState.update { it.copy(memo = finalMemo.trim(), isMemoDialogVisible = false) }
    }
}