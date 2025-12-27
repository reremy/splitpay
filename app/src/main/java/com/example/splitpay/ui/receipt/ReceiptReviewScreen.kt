package com.example.splitpay.ui.receipt

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.example.splitpay.data.model.ReceiptLineItem
import com.example.splitpay.ui.theme.DarkBackground
import com.example.splitpay.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptReviewScreen(
    groupId: String,
    navController: NavHostController,
    viewModel: ReceiptScannerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val receipt = uiState.parsedReceipt ?: return

    Scaffold(
        containerColor = DarkBackground,
        topBar = {
            TopAppBar(
                title = { Text("Review Receipt", color = TextWhite) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Navigate to AddExpense with pre-filled data
                    navController.navigate("add_expense?groupId=$groupId&fromReceipt=true")
                }
            ) {
                Icon(Icons.Default.Check, "Confirm")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Total section
            item {
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Total: RM %.2f".format(receipt.total),
                            style = MaterialTheme.typography.headlineMedium
                        )
                        receipt.subtotal?.let {
                            Text("Subtotal: RM %.2f".format(it))
                        }
                        receipt.tax?.let {
                            Text("Tax: RM %.2f".format(it))
                        }
                        receipt.serviceCharge?.let {
                            Text("Service Charge: RM %.2f".format(it))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Line items header
            item {
                Text(
                    "Items (${receipt.lineItems.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextWhite
                )
                Spacer(Modifier.height(8.dp))
            }

            // Line items list
            items(receipt.lineItems) { item ->
                LineItemCard(item)
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
fun LineItemCard(item: ReceiptLineItem) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                // ✅ Safe handling - description is non-nullable in data model
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge
                )

                // ✅ Safe comparison - quantity is non-nullable with default = 1
                if (item.quantity > 1) {
                    Text(
                        text = "${item.quantity} x RM %.2f".format(item.price),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // ✅ Safe multiplication - both are non-nullable
            Text(
                text = "RM %.2f".format(item.price * item.quantity),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}