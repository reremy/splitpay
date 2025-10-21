package com.example.splitpay.ui.expense

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner // Keep this
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AbstractSavedStateViewModelFactory // Keep this
import androidx.lifecycle.SavedStateHandle // Keep this
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry // Keep this
import com.example.splitpay.data.model.Group
import com.example.splitpay.data.model.Participant
import com.example.splitpay.data.model.Payer
import com.example.splitpay.data.repository.GroupsRepository // Keep this
import com.example.splitpay.data.repository.UserRepository // Keep this
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.ErrorRed
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextPlaceholder
import com.example.splitpay.ui.theme.TextWhite
import kotlin.math.absoluteValue
import kotlin.math.roundToInt

@Composable
fun AddExpenseScreen(
    // *** Keep navBackStackEntry as a parameter ***
    navBackStackEntry: NavBackStackEntry,
    prefilledGroupId: String?,
    onNavigateBack: () -> Unit,
    onSaveSuccess: (AddExpenseUiEvent.SaveSuccess) -> Unit
) {

    // --- USE AbstractSavedStateViewModelFactory ---

    // Get the owner needed for the factory
    val owner = LocalSavedStateRegistryOwner.current

    // Remember the factory instance, tied to the owner and entry
    val factory = remember(owner, navBackStackEntry) {
        // Inherit from AbstractSavedStateViewModelFactory
        object : AbstractSavedStateViewModelFactory(owner, navBackStackEntry.arguments) {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                key: String, // Key for the ViewModel
                modelClass: Class<T>, // Class of the ViewModel
                handle: SavedStateHandle // The correctly populated handle provided by the factory
            ): T {
                // Construct the ViewModel using the provided handle and repositories
                return AddExpenseViewModel(
                    groupsRepository = GroupsRepository(),
                    userRepository = UserRepository(),
                    savedStateHandle = handle // Use the handle provided by the factory
                ) as T
            }
        }
    }

    // Get the ViewModel using the factory and scoping it to the NavBackStackEntry
    val viewModel: AddExpenseViewModel = viewModel(
        viewModelStoreOwner = navBackStackEntry, // Scope to this navigation destination
        factory = factory
    )
    // --- END OF FACTORY SETUP ---


    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            AddExpenseUiEvent.NavigateBack -> onNavigateBack()
            is AddExpenseUiEvent.SaveSuccess -> onSaveSuccess(event)
        }
    }

    Scaffold(
        topBar = {
            AddExpenseTopBar( // Use imported composable
                onNavigateBack = viewModel::onBackClick,
                onSave = viewModel::onSaveExpenseClick,
                isLoading = uiState.isLoading
            )
        },
        bottomBar = {
            AddExpenseBottomBar( // Use imported composable
                selectedGroup = uiState.selectedGroup,
                // Pass the necessary state variables
                initialGroupId = uiState.initialGroupId,
                currentGroupId = uiState.currentGroupId,
                currency = uiState.currency,
                onChooseGroupClick = { viewModel.showGroupSelector(true) }
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
            ParticipantsList(
                participants = uiState.participants,
                // Pass the event handler from the ViewModel
                onParticipantCheckedChange = viewModel::onParticipantCheckedChange
            )


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
            onSelect = { selectedGroup ->
                viewModel.handleGroupSelection(selectedGroup)
            }, // Pass Group object
            onSelectNonGroup = { viewModel.handleGroupSelection(null) } // Pass null for non-group
        )
    }


    // Payer Selector Dialog (Handles single/multi-payer logic)
    if (uiState.isPayerSelectorVisible) {
        PayerSelectorDialog(
            relevantUsers = viewModel.relevantUsersForSelection.collectAsState().value, // Pass the dynamic list
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
                    text = uiState.paidByUsers.firstOrNull()?.name
                        ?: "You", // Default text if list is empty
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
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Change Paid By",
                    tint = Color.Gray
                )
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
                Icon(
                    Icons.Default.ChevronRight,
                    contentDescription = "Change Split",
                    tint = Color.Gray
                )
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
                    text = uiState.currency,
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
                // Check error state based on ViewModel's error message for description
                isError = uiState.error?.contains("description", ignoreCase = true) == true,
                supportingText = if (uiState.description.length > 100) {
                    { Text("Max 100 characters", color = ErrorRed) }
                } else null, // Show general error if needed?
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    errorContainerColor = Color.Transparent,
                    disabledContainerColor = Color.Transparent, // Added
                    cursorColor = PrimaryBlue, // Added
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    errorIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent, // Added
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    errorTextColor = TextWhite, // Added
                    disabledTextColor = Color.Gray, // Added
                    focusedPlaceholderColor = TextPlaceholder, // Added
                    unfocusedPlaceholderColor = TextPlaceholder // Added
                )
            )

        }
    }
}

// --- Participants List ---
@Composable
fun ParticipantsList(
    participants: List<Participant>,
    onParticipantCheckedChange: (uid: String, isChecked: Boolean) -> Unit // Add handler
) {
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
            // Use LazyColumn if the list can be very long
            participants.forEach { participant ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        // Make the whole row clickable to toggle checkbox
                        .clickable { onParticipantCheckedChange(participant.uid, !participant.isChecked) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = participant.isChecked,
                        // Pass the handler to the Checkbox as well
                        onCheckedChange = { isChecked -> onParticipantCheckedChange(participant.uid, isChecked) },
                        colors = CheckboxDefaults.colors(
                            checkedColor = PrimaryBlue,
                            uncheckedColor = Color.Gray
                        )
                    )
                    Text(
                        participant.name,
                        color = TextWhite,
                        modifier = Modifier.weight(1f).padding(start = 8.dp) // Add padding for better spacing
                    )
                    // Display calculated amount owed
                    Text(
                        // Format currency consistently
                        "RM %.2f".format(participant.owesAmount),
                        color = if (participant.isChecked) Color.Gray else Color.DarkGray // Dim if unchecked
                    )
                }
            }
            // Add participants field placeholder (Functionality TBD)
            OutlinedTextField(
                value = "",
                onValueChange = { /* TODO: Handle adding more members by searching/pasting */ },
                placeholder = { Text("Add participants (e.g., search name)", color = TextPlaceholder) },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF3C3C3C),
                    unfocusedContainerColor = Color(0xFF3C3C3C),
                    disabledContainerColor = Color(0xFF3C3C3C),
                    cursorColor = PrimaryBlue,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    disabledTextColor = Color.Gray,
                    focusedPlaceholderColor = TextPlaceholder,
                    unfocusedPlaceholderColor = TextPlaceholder
                ),
                shape = RoundedCornerShape(8.dp),
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search Participants", tint = TextPlaceholder)} // Added search icon
            )
        }
    }
}


// --- Modals ---

@Composable
fun GroupSelectorDialog(
    groups: List<Group>,
    onDismiss: () -> Unit,
    onSelect: (Group) -> Unit, // Changed signature
    onSelectNonGroup: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Group", color = TextWhite) },
        confirmButton = { }, // No confirm needed, selection is immediate
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray) // Changed color for consistency
            }
        },
        containerColor = Color(0xFF2D2D2D),
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) { // Use LazyColumn for potentially long lists
                // Non-group option first
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelectNonGroup()
                                onDismiss() // Dismiss dialog on selection
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Person, contentDescription = "Non-group", tint = Color.Gray, modifier = Modifier.padding(end = 16.dp)) // Add icon
                        Text("Non-group expense", color = TextWhite, modifier = Modifier.weight(1f))
                    }
                    Divider(color = Color(0xFF454545))
                }

                // Existing groups
                items(groups) { group ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(group) // Pass the selected Group object
                                onDismiss() // Dismiss dialog on selection
                            }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // TODO: Display group icon based on group.iconIdentifier
                        Icon(Icons.Default.Group, contentDescription = "Group Icon", tint = Color.Gray, modifier = Modifier.padding(end = 16.dp))
                        Text(group.name, color = TextWhite, modifier = Modifier.weight(1f))

                    }
                    // Only add divider if not the last item? Might need index check. For now, always add.
                    Divider(color = Color(0xFF454545))
                }
            }
        }
    )
}

@Composable
fun PayerSelectorDialog(
    relevantUsers: List<Payer>,
    paidByUsers: List<Payer>,
    totalAmount: Double,
    onSelectionChanged: (List<Payer>) -> Unit,
    onAmountChanged: (uid: String, amount: String) -> Unit,
    onDismiss: () -> Unit,
    onDone: () -> Unit
) {
    val currentSelectionMap = paidByUsers.associateBy { it.uid }
    // Remember the state within the dialog
    val selectionState = remember {
        mutableStateListOf<Payer>().apply {
            addAll(relevantUsers.map { user ->
                currentSelectionMap[user.uid]?.copy(isChecked = true) ?: user.copy(isChecked = false, amount = "0.00")
            })
        }
    }

    // Derived state based on the dialog's internal selectionState
    val isMultiPayer = selectionState.count { it.isChecked } > 1
    val totalPaidInDialog = selectionState.filter { it.isChecked }.sumOf { it.amount.toDoubleOrNull() ?: 0.0 }
    val finalTotalPaidCheck = if (isMultiPayer) totalPaidInDialog else totalAmount
    val isAmountBalanced = finalTotalPaidCheck.roundToInt() == totalAmount.roundToInt()
    val enableDoneButton = (isMultiPayer && isAmountBalanced) || (!isMultiPayer && selectionState.any { it.isChecked })

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Who Paid?", color = TextWhite) },
        confirmButton = {
            Button(
                onClick = onDone,
                enabled = enableDoneButton, // Use derived state
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
            Column(Modifier.heightIn(max = 400.dp)) { // Limit dialog height
                // Multi-Payer Status Message
                if (isMultiPayer) {
                    Text(
                        // Use totalPaidInDialog for display
                        text = if (totalAmount > 0.0 && !isAmountBalanced) {
                            "Total paid (RM %.2f) doesn't match expense (RM %.2f)".format(totalPaidInDialog, totalAmount)
                        } else {
                            "Total Paid: RM %.2f".format(totalPaidInDialog)
                        },
                        color = if (isAmountBalanced) Color(0xFF66BB6A) else ErrorRed,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                }

                // List of Users using LazyColumn
                LazyColumn(Modifier.weight(1f)) { // Allow list to take available space
                    items(selectionState.size) { index ->
                        val payer = selectionState[index]
                        PayerListItem(
                            payer = payer,
                            isMultiPayer = isMultiPayer, // Pass derived state
                            onCheckChanged = { isChecked ->
                                // Update local dialog state
                                selectionState[index] = payer.copy(isChecked = isChecked)
                                // Notify ViewModel immediately of the change in selection list
                                onSelectionChanged(selectionState.toList())
                            },
                            onAmountChanged = { newAmount ->
                                // Update local dialog state
                                selectionState[index] = payer.copy(amount = newAmount)
                                // Notify ViewModel of specific amount change
                                onAmountChanged(payer.uid, newAmount)
                                // Notify ViewModel of selection list change (needed if balance check depends on it)
                                onSelectionChanged(selectionState.toList())
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
            // Make row clickable only if needed, Checkbox handles its own click
            // .clickable { onCheckChanged(!payer.isChecked) }
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = payer.isChecked,
            onCheckedChange = onCheckChanged, // Directly use the lambda
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
                modifier = Modifier.width(90.dp), // Slightly wider?
                textStyle = TextStyle(textAlign = TextAlign.End, color = TextWhite, fontSize = 14.sp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF3C3C3C),
                    unfocusedContainerColor = Color(0xFF3C3C3C),
                    disabledContainerColor = Color(0xFF3C3C3C),
                    cursorColor = PrimaryBlue,
                    focusedIndicatorColor = PrimaryBlue, // Show indicator when focused
                    unfocusedIndicatorColor = Color.Transparent,
                    disabledIndicatorColor = Color.Transparent,
                    errorIndicatorColor = ErrorRed, // Show error indicator
                    errorCursorColor = ErrorRed, // Error cursor color
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    disabledTextColor = Color.Gray,
                    errorTextColor = ErrorRed
                ),
                shape = RoundedCornerShape(8.dp),
                // Add prefix/suffix if desired
                prefix = { Text("RM", color = Color.Gray, fontSize = 14.sp) }
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
    // Use ModalBottomSheetLayout or similar for better UX? For now, Scaffold.
    Scaffold(
        topBar = {
            SplitEditorTopBar(
                title = "Split ${uiState.splitType.label}",
                onDismiss = onDismiss,
                onDone = {
                    // TODO: Add validation within the editor before calling onDone?
                    onDone() // Call ViewModel's finalize function
                }
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
                participants = uiState.participants.filter { it.isChecked }, // Only show checked participants
                totalAmount = uiState.amount.toDoubleOrNull() ?: 0.0,
                splitType = uiState.splitType,
                onValueChange = onParticipantValueChange
            )
            Spacer(Modifier.height(16.dp)) // Add space before summary

            // 3. Balance/Summary (Total amount vs Sum of splits)
            SplitSummary(
                participants = uiState.participants.filter { it.isChecked }, // Use filtered list for summary
                totalAmount = uiState.amount.toDoubleOrNull() ?: 0.0,
                splitType = uiState.splitType // Pass split type for context
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SplitEditorTopBar(
    title: String,
    onDismiss: () -> Unit,
    onDone: () -> Unit // Consider adding validation check param: onDone: (isValid: Boolean) -> Unit
) {
    TopAppBar(
        title = { Text(title, color = TextWhite, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.Gray)
            }
        },
        actions = {
            Button(
                onClick = onDone,
                // enabled = validation check from ViewModel?
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryBlue)
            ) {
                Text("Done")
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2D2D2D)) // Slightly different color?
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
        Column(Modifier.padding(vertical = 4.dp)) { // Reduced vertical padding
            SplitType.entries.forEach { type ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onSelect(type) }
                        .padding(horizontal = 16.dp, vertical = 12.dp), // Consistent padding
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // TODO: Add icons for each split type?
                    Text(type.label, color = TextWhite, modifier = Modifier.weight(1f))
                    if (type == currentType) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = "Selected",
                            tint = Color(0xFF66BB6A)
                        )
                    }
                }
                if (type != SplitType.entries.last()) {
                    Divider(color = Color(0xFF454545), modifier = Modifier.padding(horizontal = 16.dp)) // Indent divider
                }
            }
        }
    }
}

@Composable
fun SplitParticipantsList(
    participants: List<Participant>, // Expecting already filtered list
    totalAmount: Double,
    splitType: SplitType,
    onValueChange: (uid: String, value: String) -> Unit
) {
    val unitLabel = when (splitType) {
        SplitType.PERCENTAGES -> "%"
        SplitType.SHARES -> if (participants.size == 1) " share" else " shares" // Handle pluralization
        else -> null // For RM, we use prefix/suffix in TextField
    }

    // Only show input fields if needed
    val isInputEnabled = splitType != SplitType.EQUALLY

    // Use LazyColumn for performance if list can be long
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 300.dp), // Max height to prevent taking too much space
        verticalArrangement = Arrangement.spacedBy(8.dp) // Space between items
    ) {
        items(participants, key = { it.uid }) { participant -> // Use key for better performance
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF3C3C3C)), // Slightly lighter card?
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween // Space between name and input/value
                ) {
                    // Participant Name
                    Text(
                        participant.name,
                        color = TextWhite,
                        modifier = Modifier.weight(1f).padding(end = 8.dp), // Add padding
                        maxLines = 1 // Prevent wrapping
                    )

                    // Split Input / Display
                    if (isInputEnabled) {
                        OutlinedTextField(
                            value = participant.splitValue,
                            onValueChange = { onValueChange(participant.uid, it) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), // Use Decimal
                            singleLine = true,
                            modifier = Modifier.width(100.dp), // Wider input
                            textStyle = TextStyle(textAlign = TextAlign.End, color = TextWhite, fontSize = 14.sp),
                            colors = TextFieldDefaults.colors( // Use same colors as Payer input
                                focusedContainerColor = Color(0xFF3C3C3C),
                                unfocusedContainerColor = Color(0xFF3C3C3C),
                                disabledContainerColor = Color(0xFF3C3C3C),
                                cursorColor = PrimaryBlue,
                                focusedIndicatorColor = PrimaryBlue,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledIndicatorColor = Color.Transparent,
                                errorIndicatorColor = ErrorRed,
                                errorCursorColor = ErrorRed,
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                disabledTextColor = Color.Gray,
                                errorTextColor = ErrorRed
                            ),
                            shape = RoundedCornerShape(8.dp),
                            // Use prefix for RM, suffix for %/shares
                            prefix = if (unitLabel == null) { { Text("RM", color = Color.Gray, fontSize = 14.sp) } } else null,
                            suffix = if (unitLabel != null) { { Text(unitLabel, color = Color.Gray, fontSize = 14.sp) } } else null,
                            // TODO: Add error state indication based on ViewModel validation
                            // isError = participant.splitValueError != null
                        )
                    } else { // EQUALLY (Display calculated amount owed)
                        Text(
                            "RM %.2f".format(participant.owesAmount),
                            color = Color.Gray,
                            fontSize = 14.sp // Consistent font size
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SplitSummary(
    participants: List<Participant>, // Expecting filtered list
    totalAmount: Double,
    splitType: SplitType // Added for context
) {
    // Calculate totals based *only* on the participants shown (who are checked)
    val totalOwed = participants.sumOf { it.owesAmount }
    // Calculate total input value for relevant types
    val totalInputValue = when (splitType) {
        SplitType.PERCENTAGES, SplitType.SHARES, SplitType.UNEQUALLY, SplitType.ADJUSTMENTS ->
            participants.sumOf { it.splitValue.toDoubleOrNull() ?: 0.0 }
        else -> 0.0 // Not applicable for EQUALLY
    }

    // Use tolerance for balance check
    val isBalanced = (totalOwed - totalAmount).absoluteValue < 0.01
    val difference = totalAmount - totalOwed

    // Specific check for percentages
    val isPercentCorrect = splitType != SplitType.PERCENTAGES || (totalInputValue - 100.0).absoluteValue < 0.01


    Card(
        modifier = Modifier.fillMaxWidth(), // Removed top padding, handle outside
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            // Row for Total Expense
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Expense:", color = Color.Gray)
                Text("RM %.2f".format(totalAmount), color = TextWhite, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(4.dp))

            // Row for Total Allocated (Owed)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total Allocated:", color = Color.Gray)
                Text(
                    "RM %.2f".format(totalOwed),
                    color = if (isBalanced) Color(0xFF66BB6A) else ErrorRed,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Show Remaining/Difference only if not balanced
            if (!isBalanced) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(if (difference > 0) "Remaining:" else "Over allocated:", color = ErrorRed)
                    Text(
                        "RM %.2f".format(difference.absoluteValue),
                        color = ErrorRed,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Show Total Input Value (Percentages/Shares) if relevant
            if (splitType == SplitType.PERCENTAGES || splitType == SplitType.SHARES) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val label = if (splitType == SplitType.PERCENTAGES) "Total %:" else "Total Shares:"
                    Text(label, color = Color.Gray)
                    Text(
                        if (splitType == SplitType.PERCENTAGES) "%.2f %%".format(totalInputValue) else "%.2f".format(totalInputValue),
                        // Highlight if percentage total is not 100%
                        color = if (splitType == SplitType.PERCENTAGES && !isPercentCorrect) ErrorRed else TextWhite,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}


