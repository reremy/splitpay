package com.example.splitpay.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.splitpay.data.model.Group
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.ErrorRed
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextPlaceholder
import com.example.splitpay.ui.theme.TextWhite
import java.util.*

@Composable
fun AddExpenseScreen(
    viewModel: AddExpenseViewModel = viewModel(),
    onNavigateBack: () -> Unit,
    onSaveSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            AddExpenseUiEvent.NavigateBack -> onNavigateBack()
            AddExpenseUiEvent.SaveSuccess -> onSaveSuccess()
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
                selectedGroup = uiState.selectedGroup,
                currency = uiState.currency,
                onChooseGroupClick = { viewModel.showGroupSelector(true) }
                // Calendar, Camera, Tags actions pending specific implementation
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
                onSplitClick = { viewModel.showSplitSelector(true) }
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
            onSelect = viewModel::onSelectGroup
        )
    }

    // Payer Selector Dialog (Placeholder)
    if (uiState.isPayerSelectorVisible) {
        // This would be a dialog allowing single/multiple payers and amounts
        PlaceholderSelectorDialog(
            title = "Paid By Selector",
            content = "Select who paid for the expense.",
            onDismiss = { viewModel.showPayerSelector(false) }
        )
    }

    // Split Selector Dialog (Placeholder)
    if (uiState.isSplitSelectorVisible) {
        SplitSelectorDialog(
            currentType = uiState.splitType,
            onDismiss = { viewModel.showSplitSelector(false) },
            onSelect = viewModel::onSelectSplitType
        )
    }
}

// --- Top Bar ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseTopBar(
    onNavigateBack: () -> Unit,
    onSave: () -> Unit,
    isLoading: Boolean
) {
    TopAppBar(
        title = { Text("Add expense", color = TextWhite, fontWeight = FontWeight.Bold) },
        navigationIcon = {
            IconButton(onClick = onNavigateBack, enabled = !isLoading) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
            }
        },
        actions = {
            IconButton(onClick = onSave, enabled = !isLoading) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp,
                        color = TextWhite
                    )
                } else {
                    Icon(Icons.Default.Check, contentDescription = "Save Expense", tint = Color(0xFF66BB6A))
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
    )
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
                    text = uiState.paidByUsers.firstOrNull()?.uid ?: "You", // Display first payer's name
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
                    textStyle = LocalTextStyle.current.copy(
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
                    // Display split value placeholder
                    Text(
                        "RM 0.00",
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
                // FIX: Use the unified 'colors' function with parameters specific to OutlinedTextField styling.
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

// --- Bottom Bar ---
@Composable
fun AddExpenseBottomBar(
    selectedGroup: Group?,
    currency: String,
    onChooseGroupClick: () -> Unit
) {
    BottomAppBar(
        containerColor = DarkBackground,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Choose Group Button
            TextButton(onClick = onChooseGroupClick) {
                Icon(Icons.Default.Group, contentDescription = "Group", tint = PrimaryBlue, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(
                    text = selectedGroup?.name ?: "Choose group",
                    color = if (selectedGroup != null) PrimaryBlue else Color.Gray,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Action Icons (Calendar, Camera, Tags)
            Row {
                IconButton(onClick = { /* TODO: Date picker */ }) {
                    Icon(Icons.Default.CalendarToday, contentDescription = "Calendar", tint = Color.Gray)
                }
                IconButton(onClick = { /* TODO: Receipt camera */ }) {
                    Icon(Icons.Default.Receipt, contentDescription = "Receipt", tint = Color.Gray)
                }
                IconButton(onClick = { /* TODO: Tags picker */ }) {
                    Icon(Icons.Default.LocalOffer, contentDescription = "Tags", tint = Color.Gray)
                }
            }
        }
    }
}

// --- Modals ---

@Composable
fun GroupSelectorDialog(
    groups: List<Group>,
    onDismiss: () -> Unit,
    onSelect: (Group) -> Unit
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
fun PlaceholderSelectorDialog(
    title: String,
    content: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, color = TextWhite) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = PrimaryBlue)
            }
        },
        containerColor = Color(0xFF2D2D2D),
        text = {
            Text(content, color = Color.Gray)
        }
    )
}

@Composable
fun SplitSelectorDialog(
    currentType: SplitType,
    onDismiss: () -> Unit,
    onSelect: (SplitType) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Split Method", color = TextWhite) },
        confirmButton = { },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = PrimaryBlue)
            }
        },
        containerColor = Color(0xFF2D2D2D),
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SplitType.entries.forEach { type ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(type) }
                            .padding(vertical = 12.dp, horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(type.label, color = TextWhite, modifier = Modifier.weight(1f))
                        if (type == currentType) {
                            Icon(Icons.Default.Check, contentDescription = "Selected", tint = Color(0xFF66BB6A))
                        }
                    }
                    Divider(color = Color(0xFF454545))
                }
            }
        }
    )
}
