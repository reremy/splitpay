package com.example.splitpay.ui.recordPayment

import android.net.Uri
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
import com.example.splitpay.data.repository.FileStorageRepository
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
    val isDatePickerDialogVisible: Boolean = false,
    val isMemoDialogVisible: Boolean = false,
    // --- Image Support ---
    val selectedImageUri: Uri? = null,
    val uploadedImageUrl: String = "",
    val isUploadingImage: Boolean = false,
    // --- Edit Mode ---
    val isEditMode: Boolean = false,
    val editingPaymentId: String? = null
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
    private val fileStorageRepository: FileStorageRepository = FileStorageRepository(),
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(RecordPaymentUiState())
    val uiState: StateFlow<RecordPaymentUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<RecordPaymentUiEvent>()
    val uiEvent: SharedFlow<RecordPaymentUiEvent> = _uiEvent.asSharedFlow()

    private val groupId: String = savedStateHandle["groupId"] ?: ""
    private val memberUid: String? = savedStateHandle.get<String>("memberUid")
    private val balance: Double = savedStateHandle.get<String>("balance")?.toDoubleOrNull() ?: 0.0
    private val paymentId: String? = savedStateHandle.get<String>("paymentId")
    private val payerUid: String? = savedStateHandle.get<String>("payerUid")
    private val recipientUid: String? = savedStateHandle.get<String>("recipientUid")
    private val currentUserId: String? = FirebaseAuth.getInstance().currentUser?.uid

    init {
        if (paymentId != null) {
            // Edit mode - load existing payment
            loadPaymentForEditing(paymentId)
        } else if (payerUid != null && recipientUid != null) {
            // Custom payer/recipient mode (from More Options)
            loadCustomPayerRecipient(payerUid, recipientUid)
        } else {
            // Standard mode - calculate from balance
            if (currentUserId == null || memberUid.isNullOrBlank() || balance == 0.0) {
                _uiState.update { it.copy(isFetchingDetails = false, error = "Invalid payment details.") }
            } else {
                loadUserDetails()
            }
        }
    }

    private fun loadUserDetails() {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingDetails = true, error = null) }
            try {
                // Determine who is payer and receiver based on balance
                // balance < 0 means "You owe them"
                val isPayerUser = balance < 0
                val payerUidActual = if (isPayerUser) currentUserId!! else memberUid!!
                val receiverUidActual = if (isPayerUser) memberUid!! else currentUserId!!

                // Fetch profiles and group details concurrently
                val payerDeferred = async { userRepository.getUserProfile(payerUidActual) }
                val receiverDeferred = async { userRepository.getUserProfile(receiverUidActual) }
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

    private fun loadCustomPayerRecipient(customPayerUid: String, customRecipientUid: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingDetails = true, error = null) }
            try {
                // Fetch profiles and group details concurrently
                val payerDeferred = async { userRepository.getUserProfile(customPayerUid) }
                val receiverDeferred = async { userRepository.getUserProfile(customRecipientUid) }
                val groupDeferred = async { groupsRepository.getGroupFlow(groupId).firstOrNull() }

                val payer = payerDeferred.await()
                val receiver = receiverDeferred.await()
                val group = groupDeferred.await()

                if (payer == null || receiver == null || group == null) {
                    throw Exception("Could not load user or group profiles.")
                }

                val isPayerUser = customPayerUid == currentUserId

                _uiState.update {
                    it.copy(
                        isFetchingDetails = false,
                        amount = "", // No pre-filled amount for custom selection
                        payer = payer,
                        receiver = receiver,
                        isPayerUser = isPayerUser,
                        group = group
                    )
                }

            } catch (e: Exception) {
                logE("Failed to load custom payer/recipient: ${e.message}")
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

            try {
                // 1. Upload image if selected
                var finalImageUrl = state.uploadedImageUrl
                if (state.selectedImageUri != null) {
                    _uiState.update { it.copy(isUploadingImage = true) }
                    val uploadResult = fileStorageRepository.uploadPaymentImage(state.selectedImageUri)
                    uploadResult.onSuccess { url ->
                        finalImageUrl = url
                    }.onFailure { error ->
                        logE("Image upload failed: ${error.message}")
                        _uiState.update { it.copy(isLoading = false, isUploadingImage = false) }
                        _uiEvent.emit(RecordPaymentUiEvent.ShowError("Failed to upload image: ${error.message}"))
                        return@launch
                    }
                    _uiState.update { it.copy(isUploadingImage = false) }
                }

                // 2. Create/Update the payment expense
                val paymentDescription = if (state.isPayerUser) {
                    "Payment to ${state.receiver.username}"
                } else {
                    "Payment from ${state.payer.username}"
                }

                if (state.isEditMode && state.editingPaymentId != null) {
                    // EDIT MODE - Update existing payment
                    val updatedExpense = Expense(
                        id = state.editingPaymentId,
                        groupId = groupId,
                        description = paymentDescription,
                        totalAmount = totalAmount,
                        createdByUid = currentUserId,
                        date = state.date,
                        splitType = SplitType.UNEQUALLY.name,
                        paidBy = listOf(ExpensePayer(uid = state.payer.uid, paidAmount = totalAmount)),
                        participants = listOf(ExpenseParticipant(uid = state.receiver.uid, owesAmount = totalAmount)),
                        memo = state.memo.trim(),
                        imageUrl = finalImageUrl,
                        expenseType = ExpenseType.PAYMENT
                    )

                    val result = expenseRepository.updateExpense(updatedExpense)
                    result.onSuccess {
                        logD("Payment updated successfully.")

                        // Log PAYMENT_UPDATED activity
                        try {
                            val actorProfile = userRepository.getUserProfile(currentUserId)
                            val actorName = actorProfile?.username ?: "Someone"
                            val payerName = state.payer.username
                            val receiverName = state.receiver.username
                            val group = state.group

                            val activity = Activity(
                                activityType = ActivityType.PAYMENT_UPDATED.name,
                                actorUid = currentUserId,
                                actorName = actorName,
                                involvedUids = group?.members ?: listOf(state.payer.uid, state.receiver.uid),
                                groupId = group?.id,
                                groupName = group?.name,
                                entityId = state.editingPaymentId,
                                displayText = "$payerName|$receiverName", // Store both payer and receiver
                                totalAmount = totalAmount,
                                financialImpacts = null
                            )
                            activityRepository.logActivity(activity)
                            logD("Logged PAYMENT_UPDATED activity")
                        } catch (e: Exception) {
                            logE("Failed to log PAYMENT_UPDATED activity: ${e.message}")
                        }

                        _uiEvent.emit(RecordPaymentUiEvent.SaveSuccess)
                    }.onFailure { e ->
                        logE("Failed to update payment: ${e.message}")
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvent.emit(RecordPaymentUiEvent.ShowError("Failed to update payment: ${e.message}"))
                    }

                } else {
                    // CREATE MODE - Add new payment
                    val newExpense = Expense(
                        groupId = groupId,
                        description = paymentDescription,
                        totalAmount = totalAmount,
                        createdByUid = currentUserId,
                        date = state.date,
                        splitType = SplitType.UNEQUALLY.name,
                        paidBy = listOf(ExpensePayer(uid = state.payer.uid, paidAmount = totalAmount)),
                        participants = listOf(ExpenseParticipant(uid = state.receiver.uid, owesAmount = totalAmount)),
                        memo = state.memo.trim(),
                        imageUrl = finalImageUrl,
                        expenseType = ExpenseType.PAYMENT
                    )

                    val result = expenseRepository.addExpense(newExpense)
                    result.onSuccess { createdExpenseId ->
                        logD("Settlement payment saved successfully.")

                        // Log PAYMENT_MADE activity
                        try {
                            val actor = state.payer
                            val receiver = state.receiver
                            val group = state.group!!

                            val financialImpacts = mapOf(
                                actor.uid to totalAmount,
                                receiver.uid to -totalAmount
                            )

                            val activity = Activity(
                                activityType = ActivityType.PAYMENT_MADE.name,
                                actorUid = actor.uid,
                                actorName = actor.username,
                                involvedUids = group.members,
                                groupId = group.id,
                                groupName = group.name,
                                entityId = createdExpenseId,
                                displayText = receiver.username,
                                totalAmount = totalAmount,
                                financialImpacts = financialImpacts
                            )
                            activityRepository.logActivity(activity)
                            logD("Logged PAYMENT_MADE activity for group ${group.id}")
                        } catch (e: Exception) {
                            logE("Failed to log PAYMENT_MADE activity: ${e.message}")
                        }

                        _uiEvent.emit(RecordPaymentUiEvent.SaveSuccess)
                    }.onFailure { e ->
                        logE("Failed to save settlement: ${e.message}")
                        _uiState.update { it.copy(isLoading = false) }
                        _uiEvent.emit(RecordPaymentUiEvent.ShowError("Failed to save payment: ${e.message}"))
                    }
                }

            } catch (e: Exception) {
                logE("Error in onSavePayment: ${e.message}")
                _uiState.update { it.copy(isLoading = false) }
                _uiEvent.emit(RecordPaymentUiEvent.ShowError("An error occurred: ${e.message}"))
            }
        }
    }

    private fun loadPaymentForEditing(paymentId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isFetchingDetails = true, error = null, isEditMode = true, editingPaymentId = paymentId) }

            try {
                val paymentResult = expenseRepository.getExpenseById(paymentId)
                val payment = paymentResult.getOrNull()

                if (payment == null) {
                    _uiState.update { it.copy(isFetchingDetails = false, error = "Payment not found.") }
                    return@launch
                }

                // Load payer and recipient
                val payerUid = payment.paidBy.firstOrNull()?.uid
                val recipientUid = payment.participants.firstOrNull()?.uid

                if (payerUid == null || recipientUid == null) {
                    _uiState.update { it.copy(isFetchingDetails = false, error = "Invalid payment data.") }
                    return@launch
                }

                // Fetch profiles and group
                val payerDeferred = async { userRepository.getUserProfile(payerUid) }
                val receiverDeferred = async { userRepository.getUserProfile(recipientUid) }
                val groupDeferred = async { payment.groupId?.let { groupsRepository.getGroupFlow(it).firstOrNull() } }

                val payer = payerDeferred.await()
                val receiver = receiverDeferred.await()
                val group = groupDeferred.await()

                if (payer == null || receiver == null) {
                    _uiState.update { it.copy(isFetchingDetails = false, error = "Could not load user profiles.") }
                    return@launch
                }

                val isPayerUser = payerUid == currentUserId

                _uiState.update {
                    it.copy(
                        isFetchingDetails = false,
                        amount = payment.totalAmount.toString(),
                        payer = payer,
                        receiver = receiver,
                        isPayerUser = isPayerUser,
                        group = group,
                        date = payment.date,
                        memo = payment.memo,
                        uploadedImageUrl = payment.imageUrl
                    )
                }

            } catch (e: Exception) {
                logE("Failed to load payment for editing: ${e.message}")
                _uiState.update { it.copy(isFetchingDetails = false, error = e.message) }
            }
        }
    }

    fun onPaymentImageSelected(uri: Uri?) {
        _uiState.update { it.copy(selectedImageUri = uri) }
    }

    fun onRemovePaymentImage() {
        _uiState.update { it.copy(selectedImageUri = null, uploadedImageUrl = "") }
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