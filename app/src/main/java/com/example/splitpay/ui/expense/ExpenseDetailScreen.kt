package com.example.splitpay.ui.expense

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.splitpay.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen that displays detailed information about an expense.
 * Shows all information that was entered when the expense was created.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseDetailScreen(
    expenseId: String,
    onNavigateBack: () -> Unit,
    viewModel: ExpenseDetailViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Load expense data when screen is first composed
    LaunchedEffect(expenseId) {
        viewModel.loadExpense(expenseId)
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Amount Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "MYR %.2f".format(expense.totalAmount),
                    color = TextWhite,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = expense.description,
                    color = Color.Gray,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = SimpleDateFormat("EEE, dd MMM yyyy", Locale.getDefault()).format(Date(expense.date)),
                    color = Color.Gray,
                    fontSize= 14.sp
                )
            }
        }

        // Expense Image (if exists)
        if (expense.imageUrl.isNotEmpty()) {
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

        // Paid By Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Paid by",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                expense.paidBy.forEach { payer ->
                    val payerName = uiState.usersMap[payer.uid]?.username
                        ?: uiState.usersMap[payer.uid]?.fullName
                        ?: "Unknown"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = payerName, color = Color.Gray, fontSize = 15.sp)
                        Text(
                            text = "MYR %.2f".format(payer.paidAmount),
                            color = PositiveGreen,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Split Details Section
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Split details (${expense.splitType.lowercase().replaceFirstChar { it.uppercase() }})",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))
                expense.participants.forEach { participant ->
                    val participantName = uiState.usersMap[participant.uid]?.username
                        ?: uiState.usersMap[participant.uid]?.fullName
                        ?: "Unknown"
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = participantName, color = Color.Gray, fontSize = 15.sp)
                        Text(
                            text = "MYR %.2f".format(participant.owesAmount),
                            color = NegativeRed,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        // Memo (if exists)
        if (expense.memo.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Memo",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = expense.memo,
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Group Information (if it's a group expense)
        if (expense.groupId != null && expense.groupId != "non_group") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Group",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = uiState.groupName ?: "Unknown Group",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
