package com.example.splitpay.data.repository

import android.util.Log
import com.example.splitpay.data.model.Expense
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.UUID // Import UUID for generating IDs

class ExpenseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val expensesCollection = firestore.collection("expenses")

    /**
     * Adds a new expense document to Firestore.
     * If the expense object doesn't have an ID, a new one is generated.
     */
    suspend fun addExpense(expense: Expense): Result<String> { // Return the ID on success
        return try {
            // Generate a unique ID if one isn't provided
            val expenseId = if (expense.id.isBlank()) {
                expensesCollection.document().id // Let Firestore generate
                // OR Use UUID: UUID.randomUUID().toString()
            } else {
                expense.id
            }

            // Create a map or use the expense object directly (ensure it's Firestore compatible)
            // It's often safer to create a map to avoid potential issues with default values
            // or annotations Firestore might not understand perfectly.
            // However, your Expense data class looks simple enough to store directly.
            // We'll store it directly but add the generated ID.
            val expenseToSave = expense.copy(id = expenseId)

            expensesCollection.document(expenseId)
                .set(expenseToSave) // Use the expense object directly
                .await() // Wait for the operation to complete

            logD("Expense added successfully with ID: $expenseId")
            Result.success(expenseId) // Return the ID

        } catch (e: Exception) {
            logE("Error adding expense: ${e.message}")
            Result.failure(e)
        }
    }

    // --- Future Functions ---
    // suspend fun getExpensesForGroup(groupId: String): Flow<List<Expense>> { ... }
    // suspend fun getExpenseById(expenseId: String): Expense? { ... }
    // suspend fun updateExpense(expense: Expense): Result<Unit> { ... }
    // suspend fun deleteExpense(expenseId: String): Result<Unit> { ... }

}