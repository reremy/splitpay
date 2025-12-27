package com.example.splitpay.data.model

data class ReceiptData(
    val merchantName: String? = null,
    val date: String? = null,
    val lineItems: List<ReceiptLineItem> = emptyList(),
    val subtotal: Double? = null,
    val tax: Double? = null,
    val total: Double? = null,
    val serviceCharge: Double? = null,
    val discount: Double? = null,
    val rawText: String? = ""
)

data class ReceiptLineItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val description: String,  // Non-nullable
    val price: Double,         // Non-nullable
    val quantity: Int = 1,     // Non-nullable with default
    val assignedUserIds: List<String> = emptyList()
)


