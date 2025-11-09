package com.example.splitpay.ui.paymentDetail

import androidx.lifecycle.SavedStateHandle
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
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface PaymentDetailUiEvent {
    object NavigateBack : PaymentDetailUiEvent
    data class NavigateToEdit(val paymentId: String, val groupId: String?) : PaymentDetailUiEvent
}

data class PaymentDetailUiState(
    val isLoading: Boolean = true,
    val payment: Expense? = null,
    val payer: User? = null,
    val recipient: User? = null,
    val groupName: String? = null,
    val error: String? = null,
    val showDeleteDialog: Boolean = false
)

class PaymentDetailViewModel(
    private val expenseRepository: ExpenseRepository = ExpenseRepository(),
    private val userRepository: UserRepository = UserRepository(),
    private val groupsRepository: GroupsRepository = GroupsRepository(),
    private val activityRepository: ActivityRepository = ActivityRepository(),
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentDetailUiState())
    val uiState: StateFlow<PaymentDetailUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<PaymentDetailUiEvent>()
    val uiEvent: SharedFlow<PaymentDetailUiEvent> = _uiEvent.asSharedFlow()

    private val paymentId: String = savedStateHandle["paymentId"] ?: ""

    init {
        if (paymentId.isBlank()) {
            _uiState.update { it.copy(isLoading = false, error = "Payment ID missing.") }
        } else {
            loadPayment()
        }
    }

    private fun loadPayment() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            try {
                val paymentResult = expenseRepository.getExpenseById(paymentId)
                val payment = paymentResult.getOrNull()

                if (payment == null) {
                    _uiState.update {
                        it.copy(isLoading = false, error = "Payment not found.")
                    }
                    return@launch
                }

                // Load payer and recipient info
                val payerUid = payment.paidBy.firstOrNull()?.uid
                val recipientUid = payment.participants.firstOrNull()?.uid

                val payer = payerUid?.let { userRepository.getUserProfile(it) }
                val recipient = recipientUid?.let { userRepository.getUserProfile(it) }

                // Load group name if payment is in a group
                val groupName = payment.groupId?.let { groupId ->
                    groupsRepository.getGroup(groupId).getOrNull()?.name
                }

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        payment = payment,
                        payer = payer,
                        recipient = recipient,
                        groupName = groupName,
                        error = null
                    )
                }
            } catch (e: Exception) {
                logE("Error loading payment: ${e.message}")
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to load payment.")
                }
            }
        }
    }

    fun onEditClick() {
        val payment = _uiState.value.payment ?: return
        viewModelScope.launch {
            _uiEvent.emit(PaymentDetailUiEvent.NavigateToEdit(payment.id, payment.groupId))
        }
    }

    fun showDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = true) }
    }

    fun dismissDeleteDialog() {
        _uiState.update { it.copy(showDeleteDialog = false) }
    }

    fun deletePayment() {
        val payment = _uiState.value.payment ?: return
        val currentUser = FirebaseAuth.getInstance().currentUser ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, showDeleteDialog = false) }

            try {
                val deleteResult = expenseRepository.deleteExpense(payment.id)

                deleteResult.onSuccess {
                    logD("Payment deleted successfully: ${payment.id}")

                    // Create PAYMENT_DELETED activity
                    val actorProfile = userRepository.getUserProfile(currentUser.uid)
                    val actorName = actorProfile?.username?.takeIf { it.isNotBlank() }
                        ?: actorProfile?.fullName?.takeIf { it.isNotBlank() }
                        ?: "Someone"

                    val payer = _uiState.value.payer
                    val recipient = _uiState.value.recipient
                    val recipientName = recipient?.username?.takeIf { it.isNotBlank() }
                        ?: recipient?.fullName?.takeIf { it.isNotBlank() }
                        ?: "Someone"

                    val allInvolvedUids = (payment.paidBy.map { it.uid } + payment.participants.map { it.uid }).distinct()

                    val activity = Activity(
                        activityType = ActivityType.PAYMENT_DELETED.name,
                        actorUid = currentUser.uid,
                        actorName = actorName,
                        involvedUids = allInvolvedUids,
                        groupId = payment.groupId,
                        groupName = _uiState.value.groupName,
                        entityId = payment.id,
                        displayText = recipientName, // Store recipient name for display
                        totalAmount = payment.totalAmount,
                        financialImpacts = null
                    )
                    activityRepository.logActivity(activity)

                    _uiEvent.emit(PaymentDetailUiEvent.NavigateBack)
                }.onFailure { error ->
                    logE("Error deleting payment: ${error.message}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Failed to delete payment: ${error.message}"
                        )
                    }
                }
            } catch (e: Exception) {
                logE("Exception deleting payment: ${e.message}")
                _uiState.update {
                    it.copy(isLoading = false, error = "Failed to delete payment.")
                }
            }
        }
    }

    fun onBackClick() {
        viewModelScope.launch {
            _uiEvent.emit(PaymentDetailUiEvent.NavigateBack)
        }
    }
}
