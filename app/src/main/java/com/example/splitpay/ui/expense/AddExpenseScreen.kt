package com.example.splitpay.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.OutlinedTextFieldDefaults.contentPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel // STANDARD IMPORT
import androidx.lifecycle.createSavedStateHandle // NEW IMPORT
import androidx.lifecycle.viewmodel.compose.viewModel // NEW IMPORT
import com.example.splitpay.data.model.Group
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.ErrorRed
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextPlaceholder
import com.example.splitpay.ui.theme.TextWhite
import kotlin.math.roundToInt
import kotlin.math.absoluteValue
import androidx.lifecycle.ViewModelProvider // NEW IMPORT
import androidx.lifecycle.ViewModelStoreOwner // NEW IMPORT
import androidx.compose.ui.platform.LocalContext // NEW IMPORT
import androidx.lifecycle.SavedStateViewModelFactory // NEW IMPORT
import androidx.activity.ComponentActivity // NEW IMPORT
import androidx.activity.compose.LocalActivity
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner // NEW IMPORT
import androidx.lifecycle.AbstractSavedStateViewModelFactory
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.example.splitpay.data.model.Participant
import com.example.splitpay.data.model.Payer
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import kotlin.collections.map

@Composable
fun AddExpenseScreen(
    // --- UPDATED PARAMETERS ---
    prefilledGroupId: String?, // This comes from Navigation.kt
    onNavigateBack: () -> Unit,
    onSaveSuccess: (AddExpenseUiEvent.SaveSuccess) -> Unit // Now expects the event
) {

    // 1. Get the composable-scoped values *outside* the factory
    val owner = LocalSavedStateRegistryOwner.current
    val activity = (LocalActivity.current as ComponentActivity)

    // 2. Use the viewModel() overload that accepts a factory
    val viewModel: AddExpenseViewModel = viewModel(
        factory = object : AbstractSavedStateViewModelFactory(owner, activity.intent?.extras) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                key: String,
                modelClass: Class<T>,
                handle: SavedStateHandle // This handle is now correctly provided to you
            ): T {
                // 3. Create the ViewModel with its dependencies AND the handle
                return AddExpenseViewModel(
                    groupsRepository = GroupsRepository(),
                    userRepository = UserRepository(),
                    savedStateHandle = handle
                ) as T
            }
        }
    )

    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            AddExpenseUiEvent.NavigateBack -> onNavigateBack()
            // --- UPDATED ---
            is AddExpenseUiEvent.SaveSuccess -> onSaveSuccess(event)
        }
    }

    Scaffold(
        topBar = {
            AddExpenseTopBar(
                onNavigateBack = viewModel::onBackClick,
                onSave = viewModel::onSaveExpenseClick,
                isLoading = uiState.isLoading
            )
        },
        bottomBar = {
            AddExpenseBottomBar(
                // --- UPDATED: Use currentGroupId for "Non-group" text ---
                selectedGroup = uiState.selectedGroup,
                isGroupSelected = uiState.currentGroupId != null, // NEW
                currency = uiState.currency,
                onChooseGroupClick = { viewModel.showGroupSelector(true) }
                // TODO: Calendar, Camera, Tags actions pending specific implementation
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // 1. Paid By / Split Selector (The large middle card in inspo)
            PaidSplitSelector(
                uiState = uiState,
                onPaidByClick = { viewModel.showPayerSelector(true) },
                onSplitClick = { viewModel.showSplitEditor(true) }
            )

            Spacer(Modifier.height(16.dp))

            // 2. Amount and Description Fields
            AmountAndDescriptionFields(
                uiState = uiState,
                onAmountChange = viewModel::onAmountChange,
                onDescriptionChange = viewModel::onDescriptionChange
            )

            Spacer(Modifier.height(16.dp))

            // 3. Participants / Group Members
            ParticipantsList(uiState.participants)

            // 4. Error Message
            if (!uiState.error.isNullOrEmpty()) {
                Text(
                    text = uiState.error!!,
                    color = ErrorRed,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
        }
    }

    // --- Modals / Selectors ---

    // Group Selector Dialog
    if (uiState.isGroupSelectorVisible) {
        GroupSelectorDialog(
            groups = viewModel.availableGroups.collectAsState().value,
            onDismiss = { viewModel.showGroupSelector(false) },
            onSelect = viewModel::onSelectGroup,
            // --- NEW: Add handler for "Non-group" selection ---
            onSelectNonGroup = { viewModel.onSelectGroup(null) }
        )
    }

    // Payer Selector Dialog (Handles single/multi-payer logic)
    if (uiState.isPayerSelectorVisible) {
        PayerSelectorDialog(
            allUsers = viewModel.allUsers.collectAsState().value,
            paidByUsers = uiState.paidByUsers,
            totalAmount = uiState.amount.toDoubleOrNull() ?: 0.0,
            onSelectionChanged = viewModel::onPayerSelectionChanged,
            onAmountChanged = viewModel::onPayerAmountChanged,
            onDismiss = { viewModel.showPayerSelector(false) },
            onDone = viewModel::finalizePayerSelection
        )
    }

    // Split Selector Dialog (The editor for choosing split type and setting values)
    if (uiState.isSplitEditorVisible) {
        SplitSelectorEditor(
            uiState = uiState,
            onDismiss = { viewModel.showSplitEditor(false) },
            onDone = viewModel::finalizeSplitTypeSelection,
            onSelectSplitType = viewModel::onSelectSplitType,
            onParticipantValueChange = viewModel::onParticipantSplitValueChanged
        )
    }
}




// --- Middle Paid By / Split Selector ---
@Composable
fun PaidSplitSelector(
    uiState: AddExpenseUiState,
    onPaidByClick: () -> Unit,
    onSplitClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Paid By Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPaidByClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Paid by ",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                Text(
                    text = uiState.paidByUsers.firstOrNull()?.name ?: "You", // FIX: Use name instead of UID
                    color = PrimaryBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                // Indicator for multiple payers
                if (uiState.paidByUsers.size > 1) {
                    Text(
                        text = " +${uiState.paidByUsers.size - 1} others",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = "Change Paid By", tint = Color.Gray)
            }
            Divider(Modifier.padding(vertical = 8.dp), color = Color(0xFF454545))

            // Split Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onSplitClick),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "and split ",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                Text(
                    text = uiState.splitType.label,
                    color = PrimaryBlue,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.weight(1f))
                Icon(Icons.Default.ChevronRight, contentDescription = "Change Split", tint = Color.Gray)
            }
        }
    }
}

// --- Amount and Description Fields ---
@Composable
fun AmountAndDescriptionFields(
    uiState: AddExpenseUiState,
    onAmountChange: (String) -> Unit,
    onDescriptionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Amount Input
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = uiState.currency, // Currently hardcoded to MYR/MY
                    color = TextWhite,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(end = 8.dp)
                )

                BasicTextField(
                    value = uiState.amount,
                    onValueChange = onAmountChange,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    textStyle = TextStyle(
                        color = Color(0xFF66BB6A), // Green text for amount
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.End
                    ),
                    modifier = Modifier.weight(1f),
                    decorationBox = { innerTextField ->
                        if (uiState.amount.isEmpty()) {
                            Text(
                                text = "0.00",
                                color = TextPlaceholder,
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                        innerTextField()
                    }
                )
            }
            Divider(Modifier.padding(vertical = 8.dp), color = Color(0xFF454545))

            // Description Input
            TextField(
                value = uiState.description,
                onValueChange = onDescriptionChange,
                placeholder = { Text("Enter a description", color = TextPlaceholder) },
                maxLines = 1,
                isError = uiState.description.length > 100,
                supportingText = if (uiState.description.length > 100) {
                    { Text("Max 100 characters", color = ErrorRed) }
                } else null,
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite
                )
            )
        }
    }
}

// --- Participants List ---
@Composable
fun ParticipantsList(participants: List<Participant>) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Participants:",
                color = TextWhite,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            participants.forEach { participant ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = participant.isChecked,
                        onCheckedChange = { /* TODO: handle check/uncheck */ },
                        colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue, uncheckedColor = Color.Gray)
                    )
                    Text(
                        participant.name,
                        color = TextWhite,
                        modifier = Modifier.weight(1f)
                    )
                    // Display calculated amount owed
                    Text(
                        "RM %.2f".format(participant.owesAmount),
                        color = Color.Gray
                    )
                }
            }
            // Add participants field placeholder
            OutlinedTextField(
                value = "",
                onValueChange = { /* TODO: Handle adding more members */ },
                placeholder = { Text("Enter names, emails, or phone #s", color = TextPlaceholder) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF3C3C3C), // Set container color
                    unfocusedContainerColor = Color(0xFF3C3C3C),
                    // Set border colors to transparent for a simple, floating appearance
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    // Set text colors
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    // Set placeholder color
                    unfocusedPlaceholderColor = TextPlaceholder
                ),
                shape = RoundedCornerShape(8.dp)
            )
        }
    }
}

// --- Modals ---

@Composable
fun GroupSelectorDialog(
    groups: List<Group>,
    onDismiss: () -> Unit,
    onSelect: (Group) -> Unit,
    onSelectNonGroup: () -> Unit // NEW
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Group", color = TextWhite) },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PrimaryBlue)
            }
        },
        containerColor = Color(0xFF2D2D2D),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // --- NEW: Non-group option ---
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelectNonGroup() }
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Non-group expense", color = TextWhite, modifier = Modifier.weight(1f))
                    Icon(Icons.Default.Person, contentDescription = "Non-group", tint = Color.Gray)
                }
                Divider(color = Color(0xFF454545))

                // Existing groups
                groups.forEach { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(group) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(group.name, color = TextWhite, modifier = Modifier.weight(1f))
                        Icon(Icons.Default.Group, contentDescription = "Group Icon", tint = Color.Gray)
                    }
                    Divider(color = Color(0xFF454545))
                }
            }
        }
    )
}

@Composable
fun PayerSelectorDialog(
    allUsers: List<Payer>,
    paidByUsers: List<Payer>,
    totalAmount: Double,
    onSelectionChanged: (List<Payer>) -> Unit,
    onAmountChanged: (uid: String, amount: String) -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    // Merge all users with current paidBy state for selection UI
    val selectedUsersMap = paidByUsers.associateBy { it.uid }
    val selectionState by remember(paidByUsers, allUsers) {
        mutableStateOf(allUsers.map { user ->
            selectedUsersMap[user.uid]?.copy(isChecked = true) ?: user.copy(isChecked = false, amount = "0.00")
        })
    }

    // Recalculate total paid amount whenever selection changes
    val totalPaid = selectionState.filter { it.isChecked }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val isMultiPayer = selectionState.count { it.isChecked } > 1

    // --- UPDATED: Balance logic ---
    val finalTotalPaid = if (isMultiPayer) totalPaid else totalAmount
    val isAmountBalanced = finalTotalPaid.roundToInt() == totalAmount.roundToInt()


    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Who Paid?", color = TextWhite) },
        confirmButton = {
            Button(
                onClick = onDone,
                enabled = (isMultiPayer && isAmountBalanced) || (!isMultiPayer && selectionState.any { it.isChecked }),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Done")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = Color.Gray) }
        },
        containerColor = Color(0xFF2D2D2D),
        text = {
            Column(Modifier.heightIn(max = 400.dp)) {
                // Multi-Payer Status Message
                if (isMultiPayer) {
                    Text(
                        text = if (totalAmount > 0.0 && !isAmountBalanced) {
                            "Total paid (RM %.2f) does not match expense (RM %.2f)".format(totalPaid, totalAmount)
                        } else {
                            "Total Paid: RM %.2f".format(totalPaid)
                        },
                        color = if (isAmountBalanced) Color(0xFF66BB6A) else ErrorRed,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // List of Users
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    selectionState.forEach { payer ->
                        PayerListItem(
                            payer = payer,
                            isMultiPayer = isMultiPayer,
                            onCheckChanged = { isChecked ->
                                // Logic to maintain selection for multi-payer
                                val newPayerList = selectionState.map {
                                    if (it.uid == payer.uid) it.copy(isChecked = isChecked)
                                    // If single payer mode, uncheck others
                                    else if (!isMultiPayer && isChecked) it.copy(isChecked = false)
                                    else it
                                }
                                onSelectionChanged(newPayerList)
                            },
                            onAmountChanged = { amount ->
                                onAmountChanged(payer.uid, amount)
                            }
                        )
                    }
                }
            }
        }
    )
}

@Composable
fun PayerListItem(
    payer: Payer,
    isMultiPayer: Boolean,
    onCheckChanged: (Boolean) -> Unit,
    onAmountChanged: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckChanged(!payer.isChecked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = payer.isChecked,
            onCheckedChange = onCheckChanged,
            colors = CheckboxDefaults.colors(checkedColor = PrimaryBlue)
        )
        Text(payer.name, color = TextWhite, modifier = Modifier.weight(1f).padding(start = 8.dp))

        // Amount Input for Multi-Payer
        if (payer.isChecked && isMultiPayer) {
            OutlinedTextField(
                value = payer.amount,
                onValueChange = onAmountChanged,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.width(80.dp),
                textStyle = TextStyle(textAlign = TextAlign.End, color = TextWhite, fontSize = 14.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF3C3C3C),
                    unfocusedContainerColor = Color(0xFF3C3C3C),
                    focusedIndicatorColor = PrimaryBlue,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                shape = RoundedCornerShape(8.dp),
                //contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
            )
        }
    }
}

// --- Split Selector Implementation ---

@Composable
fun SplitSelectorEditor(
    uiState: AddExpenseUiState,
    onDismiss: () -> Unit,
    onDone: () -> Unit,
    onSelectSplitType: (SplitType) -> Unit,
    onParticipantValueChange: (uid: String, value: String) -> Unit
) {
    Scaffold(
        topBar = {
            SplitEditorTopBar(
                title = "Split ${uiState.splitType.label}",
                onDismiss = onDismiss,
                onDone = onDone
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. Split Type Selector (Dropdown/Segmented Control)
            SplitTypeChooser(
                currentType = uiState.splitType,
                onSelect = onSelectSplitType
            )
            Spacer(Modifier.height(16.dp))

            // 2. Participants List for editing split values
            SplitParticipantsList(
                participants = uiState.participants,
                totalAmount = uiState.amount.toDoubleOrNull() ?: 0.0,
                splitType = uiState.splitType,
                onValueChange = onParticipantValueChange
            )
            // 3. Balance/Summary (Total amount vs Sum of splits)
            SplitSummary(uiState.participants, uiState.amount.toDoubleOrNull() ?: 0.0)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitEditorTopBar(
    title: String,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    TopAppBar(
        title = { Text(title, color = TextWhite, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
            }
        },
        actions = {
            Button(onClick = onDone, colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)) {
                Text("Done")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2D2D2D))
    )
}

@Composable
fun SplitTypeChooser(
    currentType: SplitType,
    onSelect: (SplitType) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(8.dp)) {
            SplitType.entries.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(type) }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(type.label, color = TextWhite, modifier = Modifier.weight(1f))
                    if (type == currentType) {
                        Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF66BB6A))
                    }
                }
                if (type != SplitType.entries.last()) {
                    Divider(color = Color(0xFF454545))
                }
            }
        }
    }
}

@Composable
fun SplitParticipantsList(
    participants: List<Participant>,
    totalAmount: Double,
    splitType: SplitType,
    onValueChange: (uid: String, value: String) -> Unit
) {
    val unit = when (splitType) {
        SplitType.PERCENTAGES -> "%"
        SplitType.SHARES -> " shares"
        SplitType.EQUALLY, SplitType.ADJUSTMENTS, SplitType.UNEQUALLY -> " RM"
    }

    // Determine if input is needed for the current split type
    val isInputNeeded = splitType != SplitType.EQUALLY && splitType != SplitType.ADJUSTMENTS

    LazyColumn(
        modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        items(participants) { participant ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Participant Name
                    Text(participant.name, color = TextWhite, modifier = Modifier.weight(1f))

                    // Split Input / Display
                    if (isInputNeeded) {
                        // Input field for editing split value (amount, percentage, or shares)
                        OutlinedTextField(
                            value = participant.splitValue,
                            onValueChange = { onValueChange(participant.uid, it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.width(80.dp),
                            textStyle = TextStyle(textAlign = TextAlign.End, color = TextWhite, fontSize = 14.sp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color(0xFF3C3C3C),
                                unfocusedContainerColor = Color(0xFF3C3C3C),
                                focusedIndicatorColor = PrimaryBlue,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(8.dp),
                            // FIX 1: Removed contentPadding parameter as it caused the candidate error
                            trailingIcon = { if (unit != " RM") Text(unit, color = Color.Gray) }
                        )
                    } else { // EQUALLY or ADJUSTMENTS/UNEQUALLY (Display calculated amount owed)
                        Text("RM %.2f".format(participant.owesAmount), color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun SplitSummary(
    participants: List<Participant>,
    totalAmount: Double
) {
    val totalOwed = participants.sumOf { it.owesAmount }
    val isBalanced = totalOwed.roundToInt() == totalAmount.roundToInt()
    val difference = totalAmount - totalOwed

    Card(
        modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Expense:", color = Color.Gray)
                Text("RM %.2f".format(totalAmount), color = TextWhite)
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Allocated:", color = Color.Gray)
                Text(
                    "RM %.2f".format(totalOwed),
                    color = if (isBalanced) Color(0xFF66BB6A) else ErrorRed
                )
            }
            if (!isBalanced) {
                Text(
                    "Difference: RM %.2f".format(difference.absoluteValue),
                    color = ErrorRed,
                    fontSize = 12.sp,
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}