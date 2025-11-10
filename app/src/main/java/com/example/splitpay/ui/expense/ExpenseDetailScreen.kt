package com.example.splitpay.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Category
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.MaterialTheme
import coil.compose.AsyncImage
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.groups.expenseCategoriesMap
import com.example.splitpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen that displays detailed information about an expense.
 * Order: Description, Amount, Date, Category, Group, Split, Image, Memo
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            },
            containerColor = MaterialTheme.colorScheme.surface
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
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // 1. DESCRIPTION (centered, large, prominent)
        Text(
            text = expense.description,
            color = TextWhite,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
        )

        Spacer(Modifier.height(16.dp))

        // 2. AMOUNT (centered, very large)
        Text(
            text = "MYR %.2f".format(expense.totalAmount),
            color = TextWhite,
            fontSize = 36.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // 3. DATE (centered, gray)
        Text(
            text = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(expense.date)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(24.dp))

        // 4. CATEGORY
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Category:",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )

                // Category icon and name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = expenseCategoriesMap[expense.category] ?: Icons.Default.Category,
                        contentDescription = expense.category,
                        tint = PrimaryBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = expense.category.replaceFirstChar { it.uppercase() },
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))

        // 5. GROUP (if it's a group expense)
        if (expense.groupId != null && expense.groupId != "non_group") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Group:",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = uiState.groupName ?: "Unknown Group",
                        color = TextWhite,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // 6. SPLIT (Paid by + Participants with profile photos)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Paid by section
                Text(
                    text = "Paid by",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                expense.paidBy.forEach { payer ->
                    val user = uiState.usersMap[payer.uid]
                    val payerName = user?.username
                        ?: user?.fullName
                        ?: "Unknown"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Profile photo
                            if (user?.profilePictureUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = user.profilePictureUrl,
                                    contentDescription = "$payerName's profile",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = payerName,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
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
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outline
                )

                // Split details section
                Text(
                    text = "Split (${expense.splitType.lowercase().replaceFirstChar { it.uppercase() }})",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                expense.participants.forEach { participant ->
                    val user = uiState.usersMap[participant.uid]
                    val participantName = user?.username
                        ?: user?.fullName
                        ?: "Unknown"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            // Profile photo
                            if (user?.profilePictureUrl?.isNotEmpty() == true) {
                                AsyncImage(
                                    model = user.profilePictureUrl,
                                    contentDescription = "$participantName's profile",
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            } else {
                                Icon(
                                    Icons.Default.AccountCircle,
                                    contentDescription = participantName,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
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

        // 7. EXPENSE IMAGE (if exists)
        if (expense.imageUrl.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Expense Image",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(12.dp))
                    AsyncImage(
                        model = expense.imageUrl,
                        contentDescription = "Expense Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }

        // 8. MEMO (if exists)
        if (expense.memo.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Memo",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
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

        Spacer(Modifier.height(24.dp))
    }
}
