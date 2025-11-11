package com.example.splitpay.data.model

import com.google.firebase.firestore.Exclude

/**
 * Represents a participant's share in an expense.
 *
 * @property uid The user's unique ID
 * @property owesAmount Final calculated amount this user owes (in currency units)
 * @property initialSplitValue Original input value from user (interpretation depends on splitType):
 *   - EQUALLY: equal share amount
 *   - EXACT_AMOUNTS: exact amount entered
 *   - PERCENTAGES: percentage value (0-100)
 *   - SHARES: number of shares
 */
data class ExpenseParticipant(
    val uid: String = "",
    val owesAmount: Double = 0.0,
    val initialSplitValue: Double = 0.0
)

/**
 * Represents a user who paid for (part of) an expense.
 *
 * Supports multiple payers for complex split scenarios where multiple people
 * contribute to paying the total amount.
 *
 * @property uid The user's unique ID
 * @property paidAmount Amount this user paid (in currency units)
 */
data class ExpensePayer(
    val uid: String = "",
    val paidAmount: Double = 0.0
)

/**
 * Expense type constants to distinguish between regular expenses and settlement payments.
 */
object ExpenseType {
    /** Regular expense that increases balances */
    const val EXPENSE = "EXPENSE"
    /** Settlement payment that reduces balances */
    const val PAYMENT = "PAYMENT"
}

/**
 * Represents an expense or payment transaction.
 *
 * This model is used for both:
 * 1. Regular expenses (sharing costs among group members or friends)
 * 2. Settlement payments (recording payments to settle balances)
 *
 * Corresponds to documents in the Firestore `/expenses` collection.
 *
 * @property id Unique expense ID (Firestore document ID)
 * @property groupId Group ID for group expenses, "non_group" for friend-to-friend, null for legacy
 * @property description Brief description of the expense (e.g., "Dinner at restaurant")
 * @property totalAmount Total expense amount in currency units
 * @property createdByUid UID of the user who created this expense
 * @property date Expense date timestamp in milliseconds
 * @property splitType How the expense is split: "EQUALLY", "EXACT_AMOUNTS", "PERCENTAGES", "SHARES"
 * @property paidBy List of users who paid, with amounts (supports multiple payers)
 * @property participants List of users who owe money, with calculated amounts
 * @property memo Optional additional notes about the expense
 * @property imageUrl Firebase Storage URL for receipt/bill image (optional)
 * @property category Expense category: "food", "transport", "utilities", "misc", etc.
 * @property expenseType Type: ExpenseType.EXPENSE (regular) or ExpenseType.PAYMENT (settlement)
 */
data class Expense(
    val id: String = "",
    val groupId: String? = null,
    val description: String = "",
    val totalAmount: Double = 0.0,
    val createdByUid: String = "",
    val date: Long = System.currentTimeMillis(),

    val splitType: String = "EQUALLY",
    val paidBy: List<ExpensePayer> = emptyList(),
    val participants: List<ExpenseParticipant> = emptyList(),

    val memo: String = "",
    val imageUrl: String = "",
    val category: String = "misc",

    val expenseType: String = ExpenseType.EXPENSE
)