package com.example.splitpay.ui.paymentDetail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.splitpay.data.repository.ActivityRepository
import com.example.splitpay.data.repository.ExpenseRepository
import com.example.splitpay.data.repository.GroupsRepository
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.ui.common.UiEventHandler
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.NegativeRed
import com.example.splitpay.ui.theme.PositiveGreen
import com.example.splitpay.ui.theme.PrimaryBlue
import com.example.splitpay.ui.theme.TextWhite
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// --- ViewModel Factory ---
class PaymentDetailViewModelFactory(
    private val expenseRepository: ExpenseRepository,
    private val userRepository: UserRepository,
    private val groupsRepository: GroupsRepository,
    private val activityRepository: ActivityRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PaymentDetailViewModel::class.java)) {
            return PaymentDetailViewModel(
                expenseRepository,
                userRepository,
                groupsRepository,
                activityRepository,
                savedStateHandle
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentDetailScreen(
    paymentId: String,
    onNavigateBack: () -> Unit,
    onNavigateToEdit: (String, String?) -> Unit, // paymentId, groupId
    expenseRepository: ExpenseRepository = ExpenseRepository(),
    userRepository: UserRepository = UserRepository(),
    groupsRepository: GroupsRepository = GroupsRepository(),
    activityRepository: ActivityRepository = ActivityRepository()
) {
    val savedStateHandle = SavedStateHandle(mapOf("paymentId" to paymentId))
    val factory = PaymentDetailViewModelFactory(
        expenseRepository,
        userRepository,
        groupsRepository,
        activityRepository,
        savedStateHandle
    )
    val viewModel: PaymentDetailViewModel = viewModel(factory = factory)

    val uiState by viewModel.uiState.collectAsState()

    UiEventHandler(viewModel.uiEvent) { event ->
        when (event) {
            PaymentDetailUiEvent.NavigateBack -> onNavigateBack()
            is PaymentDetailUiEvent.NavigateToEdit -> onNavigateToEdit(event.paymentId, event.groupId)
        }
    }

    // Delete confirmation dialog
    if (uiState.showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.dismissDeleteDialog() },
            title = { Text("Delete Payment?", color = TextWhite) },
            text = {
                Text(
                    "This will delete the payment record and undo the balance calculation. This action cannot be undone.",
                    color = Color.Gray
                )
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.deletePayment() },
                    colors = ButtonDefaults.buttonColors(containerColor = NegativeRed)
                ) {
                    Text("Delete")
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Payment Record",
                        color = TextWhite,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onBackClick() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextWhite)
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.onEditClick() }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = TextWhite)
                    }
                    IconButton(onClick = { viewModel.showDeleteDialog() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextWhite)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF2D2D2D))
            )
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                uiState.isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryBlue)
                    }
                }
                uiState.error != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = uiState.error!!,
                            color = NegativeRed,
                            textAlign = TextAlign.Center
                        )
                    }
                }
                uiState.payment != null -> {
                    PaymentDetailContent(uiState = uiState)
                }
            }
        }
    }
}

@Composable
fun PaymentDetailContent(
    uiState: PaymentDetailUiState,
    modifier: Modifier = Modifier
) {
    val payment = uiState.payment ?: return
    val payer = uiState.payer
    val recipient = uiState.recipient

    val payerName = payer?.username?.takeIf { it.isNotBlank() }
        ?: payer?.fullName?.takeIf { it.isNotBlank() }
        ?: "Unknown"

    val recipientName = recipient?.username?.takeIf { it.isNotBlank() }
        ?: recipient?.fullName?.takeIf { it.isNotBlank() }
        ?: "Unknown"

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp)
    ) {
        // Payment title
        Text(
            text = "Payment",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        // Amount (large, centered)
        Text(
            text = "MYR %.2f".format(payment.totalAmount),
            color = PositiveGreen,
            fontSize = 48.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(32.dp))

        // Payer → Recipient
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "From",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))

                // Payer info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (payer?.profilePictureUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = payer.profilePictureUrl,
                            contentDescription = "$payerName's profile",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = payerName,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = payerName,
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(Modifier.height(24.dp))

                Text(
                    text = "To",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
                Spacer(Modifier.height(8.dp))

                // Recipient info
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (recipient?.profilePictureUrl?.isNotEmpty() == true) {
                        AsyncImage(
                            model = recipient.profilePictureUrl,
                            contentDescription = "$recipientName's profile",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = recipientName,
                            tint = Color.Gray,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Text(
                        text = recipientName,
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Date
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Date",
                    color = Color.Gray,
                    fontSize = 14.sp,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(payment.date)),
                    color = TextWhite,
                    fontSize = 14.sp
                )
            }
        }

        // Group (if applicable)
        if (uiState.groupName != null) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2D2D2D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Group",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = uiState.groupName,
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Memo (if exists)
        if (payment.memo.isNotEmpty()) {
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
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = payment.memo,
                        color = TextWhite,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}
