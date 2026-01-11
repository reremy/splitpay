package com.example.splitpay.ui.recordPayment

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import coil.compose.AsyncImage
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.splitpay.data.model.User
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.FileStorageRepository
import com.example.splitpay.data.repository.GroupsRepository // <-- IMPORT
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.expense.addExpense.AddExpenseBottomBar // <-- IMPORT
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.NegativeRed
import com.example.splitpay.ui.theme.PositiveGreen
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.SurfaceDark
import com.example.splitpay.ui.theme.TextPlaceholder
import com.example.splitpay.ui.theme.TextWhite

// --- ViewModel Factory ---
class RecordPaymentViewModelFactory(
    private val userRepository: UserRepository,
    private val expenseRepository: ExpenseRepository,
    private val groupsRepository: GroupsRepository,
    private val activityRepository: ActivityRepository,
    private val fileStorageRepository: FileStorageRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(RecordPaymentViewModel::class.java)) {
            return RecordPaymentViewModel(
                userRepository,
                expenseRepository,
                groupsRepository,
                activityRepository,
                fileStorageRepository,
                savedStateHandle
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordPaymentScreen(
    groupId: String,
    memberUid: String,
    balance: String,
    paymentId: String? = null,
    payerUid: String? = null,
    recipientUid: String? = null,
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit,
    // Provide default repository instances
    userRepository: UserRepository = UserRepository(),
    expenseRepository: ExpenseRepository = ExpenseRepository(),
    groupsRepository: GroupsRepository = GroupsRepository(),
    activityRepository: ActivityRepository = ActivityRepository(),
    fileStorageRepository: FileStorageRepository = FileStorageRepository()
) {
    // --- Use SavedStateHandle with factory ---
    val savedStateHandle = SavedStateHandle(
        mapOf(
            "groupId" to groupId,
            "memberUid" to memberUid,
            "balance" to balance
        ).plus(
            listOfNotNull(
                paymentId?.let { "paymentId" to it },
                payerUid?.let { "payerUid" to it },
                recipientUid?.let { "recipientUid" to it }
            ).toMap()
        )
    )
    val factory = RecordPaymentViewModelFactory(
        userRepository,
        expenseRepository,
        groupsRepository,
        activityRepository,
        fileStorageRepository,
        savedStateHandle
    )
    val viewModel: RecordPaymentViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    // --- NEW: Date Picker State ---
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = uiState.date
    )
    val showDatePicker = remember { mutableStateOf(false) }
    LaunchedEffect(uiState.isDatePickerDialogVisible) {
        showDatePicker.value = uiState.isDatePickerDialogVisible
    }

    // --- NEW: Memo Dialog State ---
    val showMemo = remember { mutableStateOf(false) }
    val memoTextState = rememberSaveable { mutableStateOf(uiState.memo) }
    LaunchedEffect(uiState.isMemoDialogVisible) {
        showMemo.value = uiState.isMemoDialogVisible
        if (uiState.isMemoDialogVisible) {
            memoTextState.value = uiState.memo
        }
    }

    // --- NEW: Image Picker Launcher ---
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        viewModel.onPaymentImageSelected(uri)
    }

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            RecordPaymentUiEvent.NavigateBack -> onNavigateBack()
            RecordPaymentUiEvent.SaveSuccess -> onSaveSuccess()
            is RecordPaymentUiEvent.ShowError -> {
                Toast.makeText(context, event.message, Toast.LENGTH_SHORT).show()
            }
        }
    }

    // --- NEW: Date Picker Dialog Composable ---
    if (showDatePicker.value) {
        DatePickerDialog(
            onDismissRequest = { viewModel.showDatePickerDialog(false) },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let {
                        viewModel.onDateSelected(it)
                    }
                }) {
                    Text("OK", color = PrimaryBlue)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDatePickerDialog(false) }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = MaterialTheme.colorScheme.surface) // Dark background
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MaterialTheme.colorScheme.surface, // Ensure inner container is also dark
                    titleContentColor = TextWhite,
                    headlineContentColor = TextWhite,
                    weekdayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    subheadContentColor = TextWhite, // Month/Year selector text
                    yearContentColor = TextWhite,
                    currentYearContentColor = PrimaryBlue,
                    selectedYearContentColor = TextWhite,
                    selectedYearContainerColor = PrimaryBlue,
                    dayContentColor = TextWhite,
                    disabledDayContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                    selectedDayContentColor = DarkBackground, // Text color on selected day
                    disabledSelectedDayContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    selectedDayContainerColor = PrimaryBlue, // Background of selected day
                    todayContentColor = PrimaryBlue, // Color for today's number (if not selected)
                    todayDateBorderColor = PrimaryBlue, // Border for today
                    dayInSelectionRangeContentColor = PrimaryBlue,
                    dayInSelectionRangeContainerColor = PrimaryBlue.copy(alpha=0.2f)
                )
            )
        }
    }

    // --- NEW: Memo Dialog Composable ---
    if (showMemo.value) {
        AlertDialog(
            onDismissRequest = { viewModel.showMemoDialog(false) },
            title = { Text("Add Memo", color = TextWhite) },
            text = {
                OutlinedTextField(
                    value = memoTextState.value,
                    onValueChange = { memoTextState.value = it },
                    placeholder = { Text("Enter memo details...", color = TextPlaceholder) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors( // Use colors for consistency
                        focusedContainerColor = SurfaceDark,
                        unfocusedContainerColor = SurfaceDark,
                        disabledContainerColor = SurfaceDark,
                        cursorColor = PrimaryBlue,
                        focusedIndicatorColor = PrimaryBlue,
                        unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        disabledIndicatorColor = Color.Transparent,
                        focusedTextColor = TextWhite,
                        unfocusedTextColor = TextWhite,
                        disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        focusedPlaceholderColor = TextPlaceholder,
                        unfocusedPlaceholderColor = TextPlaceholder
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.onMemoSaved(memoTextState.value) }, // Call new ViewModel function
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { Text("Done") }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showMemoDialog(false) }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant) }
            },
            containerColor = MaterialTheme.colorScheme.surface // Dialog background
        )
    }

    // --- Balance Warning Dialog ---
    if (uiState.showBalanceWarning) {
        AlertDialog(
            onDismissRequest = viewModel::onDismissBalanceWarning,
            title = { Text("Amount Exceeds Balance", color = TextWhite) },
            text = {
                Text(
                    text = "The payment amount (MYR%.2f) exceeds the outstanding balance (MYR%.2f). Do you want to proceed anyway?".format(
                        uiState.amount.toDoubleOrNull() ?: 0.0,
                        uiState.outstandingBalance
                    ),
                    color = TextWhite
                )
            },
            confirmButton = {
                Button(
                    onClick = viewModel::onConfirmExceedBalance,
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
                ) { Text("Proceed") }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onDismissBalanceWarning) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (uiState.isEditMode) "Edit Payment" else "Record Payment", color = TextWhite) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.onSavePayment() },
                        enabled = !uiState.isLoading && !uiState.isFetchingDetails
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = TextWhite, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.Check, contentDescription = "Save Payment", tint = PositiveGreen)
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        // --- ADDED BOTTOM BAR ---
        bottomBar = {
            AddExpenseBottomBar(
                selectedGroup = uiState.group,
                initialGroupId = null, // Not relevant here, group is fixed
                currentGroupId = groupId,
                currency = "MYR", // Hardcoded for now
                onChooseGroupClick = {}, // <-- DISABLES button click
                onCalendarClick = { viewModel.showDatePickerDialog(true) },
                onCameraClick = { imagePickerLauncher.launch("image/*") }, // Wire up image picker
                onMemoClick = { viewModel.showMemoDialog(true) }
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->

        when {
            uiState.isFetchingDetails -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
            uiState.error != null -> {
                Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                    Text("Error: ${uiState.error}", color = NegativeRed)
                }
            }
            uiState.payer != null && uiState.receiver != null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding) // <-- This now accounts for TopBar AND BottomBar
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {

                    PaymentUserDisplay(
                        payer = uiState.payer!!,
                        receiver = uiState.receiver!!,
                        isPayerUser = uiState.isPayerUser
                    )

                    Spacer(Modifier.height(48.dp))

                    AmountInput(
                        amount = uiState.amount,
                        onAmountChange = viewModel::onAmountChange
                    )

                    Spacer(Modifier.height(24.dp))

                    // Display Payment Image Preview (if selected or already uploaded)
                    if (uiState.selectedImageUri != null || uiState.uploadedImageUrl.isNotEmpty()) {
                        PaymentImagePreview(
                            imageUri = uiState.selectedImageUri,
                            imageUrl = uiState.uploadedImageUrl,
                            isUploading = uiState.isUploadingImage,
                            onRemoveClick = { viewModel.onRemovePaymentImage() },
                            onReplaceClick = { imagePickerLauncher.launch("image/*") }
                        )
                        Spacer(Modifier.height(24.dp))
                    }

                    Text(
                        text = "This feature does not move money.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun PaymentUserDisplay(payer: User, receiver: User, isPayerUser: Boolean) {
    val payerName = if (isPayerUser) "You" else payer.username
    val receiverName = if (isPayerUser) receiver.username else "You"

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically, // <-- Centered for arrow alignment
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- Payer Column ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(80.dp)
            ) {
                // Profile photo or fallback icon
                if (payer.profilePictureUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = payer.profilePictureUrl,
                        contentDescription = "$payerName's profile",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = payerName,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(60.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = payerName,
                    color = TextWhite,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // --- Arrow (Larger and Centered) ---
            Icon(
                Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "pays",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp) // Increased from 24dp to 40dp
            )

            // --- Receiver Column ---
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.width(80.dp)
            ) {
                // Profile photo or fallback icon
                if (receiver.profilePictureUrl?.isNotEmpty() == true) {
                    AsyncImage(
                        model = receiver.profilePictureUrl,
                        contentDescription = "$receiverName's profile",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Default.AccountCircle,
                        contentDescription = receiverName,
                        tint = PositiveGreen,
                        modifier = Modifier.size(60.dp)
                    )
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    text = receiverName,
                    color = TextWhite,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "$payerName paid $receiverName",
            color = TextWhite,
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun AmountInput(amount: String, onAmountChange: (String) -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "MYR",
                color = TextPlaceholder,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            BasicTextField(
                value = amount,
                onValueChange = onAmountChange,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                textStyle = TextStyle(
                    color = PositiveGreen,
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Start
                ),
                modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
                decorationBox = { innerTextField ->
                    if (amount.isEmpty()) {
                        Text(
                            text = "0.00",
                            color = TextPlaceholder,
                            fontSize = 48.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    innerTextField()
                }
            )
        }
        Spacer(Modifier.height(8.dp))
        HorizontalDivider(color = PositiveGreen, thickness = 2.dp)
    }
}

// --- Payment Image Preview ---
@Composable
fun PaymentImagePreview(
    imageUri: Uri?,
    imageUrl: String,
    isUploading: Boolean,
    onRemoveClick: () -> Unit,
    onReplaceClick: () -> Unit
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        colors = androidx.compose.material3.CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Payment Image",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                // X button to remove image
                IconButton(
                    onClick = onRemoveClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(NegativeRed),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Remove image",
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // Image display - clickable to replace
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable(onClick = onReplaceClick)
                    .background(SurfaceDark),
                contentAlignment = Alignment.Center
            ) {
                when {
                    isUploading -> {
                        // Show loading indicator while uploading
                        CircularProgressIndicator(color = PrimaryBlue, modifier = Modifier.size(40.dp))
                    }
                    imageUri != null -> {
                        // Show newly selected image (not yet uploaded)
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Selected payment image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    imageUrl.isNotEmpty() -> {
                        // Show existing uploaded image
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = "Payment image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }

            // Hint text
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Tap image to replace",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}