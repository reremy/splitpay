package com.example.splitpay.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen that displays detailed information about an expense.
 * Layout matches AddExpenseScreen for consistency.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String, String?) -> Unit,
    viewModel: ExpenseDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load expense data when screen is first composed
    LaunchedEffect(expenseId) {
        viewModel.loadExpense(expenseId)
    }

    // Handle UI events
    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            ExpenseDetailUiEvent.NavigateBack -> onNavigateBack()
            is ExpenseDetailUiEvent.NavigateToEdit -> onNavigateToEdit(event.expenseId, event.groupId)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Expense Details", color = TextWhite, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    // Edit icon
                    IconButton(onClick = { viewModel.onEditClick() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextWhite)
                    }
                    // Delete icon
                    IconButton(onClick = { viewModel.showDeleteDialog() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBackground)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        when {
            uiState.isLoading -> {
                Box(
                    Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = PrimaryBlue)
                }
            }
            uiState.error != null -> {
                Box(
                    Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${uiState.error}", color = NegativeRed)
                }
            }
            uiState.expense != null -> {
                ExpenseDetailContent(
                    uiState = uiState,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("Delete Expense?", color = TextWhite) },
            text = {
                Text(
                    "This will permanently delete this expense and create a deletion activity. This action cannot be undone.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deleteExpense() },
                    colors = ButtonDefaults.buttonColors(containerColor = NegativeRed)
                ) {
                    Text("Delete", color = TextWhite)
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissDeleteDialog() }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            containerColor = Color(0xFF2D2D2D)
        )
    }
}

@Composable
fun ExpenseDetailContent(
    uiState: ExpenseDetailUiState,
    modifier: Modifier = Modifier
) {
    val expense = uiState.expense ?: return

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Display Selected Date (like AddExpenseScreen)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(expense.date)),
                color = Color.Gray,
                fontSize = 14.sp
            )
        }

        // Paid By / Split Selector Card (similar to AddExpenseScreen)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Paid by section
                Text(
                    text = "Paid by",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                expense.paidBy.forEach { payer ->
                    val payerName = uiState.usersMap[payer.uid]?.username
                        ?: uiState.usersMap[payer.uid]?.fullName
                        ?: "Unknown"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // User avatar placeholder
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(text = payerName, color = TextWhite, fontSize = 15.sp)
                        }
                        Text(
                            text = "MYR %.2f".format(payer.paidAmount),
                            color = PositiveGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Divider(
                    modifier = Modifier.padding(vertical = 12.dp),
                    color = Color(0xFF4D4D4D)
                )

                // Split details section
                Text(
                    text = "Split (${expense.splitType.lowercase().replaceFirstChar { it.uppercase() }})",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
                Spacer(Modifier.height(8.dp))
                expense.participants.forEach { participant ->
                    val participantName = uiState.usersMap[participant.uid]?.username
                        ?: uiState.usersMap[participant.uid]?.fullName
                        ?: "Unknown"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // User avatar placeholder
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(PrimaryBlue)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(text = participantName, color = TextWhite, fontSize = 15.sp)
                        }
                        Text(
                            text = "MYR %.2f".format(participant.owesAmount),
                            color = NegativeRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Amount and Description (like AddExpenseScreen)
        OutlinedTextField(
            value = "MYR %.2f".format(expense.totalAmount),
            onValueChange = {},
            label = { Text("Amount", color = TextPlaceholder) },
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                disabledContainerColor = Color(0xFF3C3C3C),
                disabledTextColor = TextWhite,
                disabledLabelColor = TextPlaceholder
            ),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = expense.description,
            onValueChange = {},
            label = { Text("Description", color = TextPlaceholder) },
            enabled = false,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                disabledContainerColor = Color(0xFF3C3C3C),
                disabledTextColor = TextWhite,
                disabledLabelColor = TextPlaceholder
            ),
            shape = RoundedCornerShape(8.dp)
        )

        // Display Memo if exists
        if (expense.memo.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Memo",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = expense.memo,
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Expense Image (if exists)
        if (expense.imageUrl.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                AsyncImage(
                    model = expense.imageUrl,
                    contentDescription = "Expense Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }

        // Group Information (if it's a group expense)
        if (expense.groupId != null && expense.groupId != "non_group") {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Group",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = uiState.groupName ?: "Unknown Group",
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
