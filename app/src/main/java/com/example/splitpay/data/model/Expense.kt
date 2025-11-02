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

object ExpenseType {
    const val EXPENSE = "EXPENSE"
    const val PAYMENT = "PAYMENT"
}

data class Expense(
    val id: String = "",
    val groupId: String? = null, // Make nullable for "Non-group"
    val description: String = "",
    val totalAmount: Double = 0.0,
    val createdByUid: String = "",
    val date: Long = System.currentTimeMillis(), // This field already exists!

    val splitType: String = "EQUALLY",
    val paidBy: List<ExpensePayer> = emptyList(),
    val participants: List<ExpenseParticipant> = emptyList(),

    // --- ADD THESE ---
    val memo: String = "",
    val imageUrls: List<String> = emptyList(),

    val expenseType: String = ExpenseType.EXPENSE
)