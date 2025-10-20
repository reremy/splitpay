package com.example.splitpay.data.model

import com.google.firebase.firestore.Exclude

// Represents a single user's debt or payment details within the expense
data class ExpenseParticipant(
    val uid: String = "",
    val owesAmount: Double = 0.0,
    val initialSplitValue: Double = 0.0, // Amount, percentage, or share count entered by user
)

// Represents a single user who paid part of the expense
data class ExpensePayer(
    val uid: String = "",
    val paidAmount: Double = 0.0
)

data class Expense(
    val id: String = "", // Unique ID (Firestore Document ID)
    val groupId: String = "", // Which group this expense belongs to
    val description: String = "",
    val totalAmount: Double = 0.0,
    val createdByUid: String = "", // The UID of the user who created the expense
    val date: Long = System.currentTimeMillis(),

    val splitType: String = "EQUALLY", // e.g., "EQUALLY", "PERCENTAGES"

    // List of users who paid and how much
    val paidBy: List<ExpensePayer> = emptyList(),

    // List of users in the expense and how much they owe (calculated share)
    val participants: List<ExpenseParticipant> = emptyList(),
)
