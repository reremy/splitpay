package com.example.splitpay.data.repository

import android.util.Log
import com.example.splitpay.data.model.Expense
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
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
    suspend fun addExpense(expense: Expense): Result<String> {
        return try {
            val expenseId = if (expense.id.isBlank()) {
                expensesCollection.document().id
            } else {
                expense.id
            }
            val expenseToSave = expense.copy(id = expenseId)
            expensesCollection.document(expenseId).set(expenseToSave).await()
            logD("Expense added successfully with ID: $expenseId")
            Result.success(expenseId)
        } catch (e: Exception) {
            logE("Error adding expense: ${e.message}")
            Result.failure(e)
        }
    }

    // --- NEW: Get Expenses for MULTIPLE Specific Groups ---
    suspend fun getExpensesForGroups(groupIds: List<String>): List<Expense> {
        if (groupIds.isEmpty()) {
            return emptyList()
        }
        return try {
            val groupChunks = groupIds.chunked(10) // Chunk IDs for 'whereIn'
            val expenses = mutableListOf<Expense>()
            for (chunk in groupChunks) {
                if (chunk.isEmpty()) continue
                val querySnapshot = expensesCollection
                    .whereIn("groupId", chunk)
                    .orderBy("date", Query.Direction.DESCENDING) // Optional order
                    .get()
                    .await()
                expenses.addAll(querySnapshot.toObjects(Expense::class.java))
            }
            logD("Fetched ${expenses.size} expenses for ${groupIds.size} groups.")
            expenses
        } catch (e: Exception) {
            logE("Error fetching expenses for multiple groups: ${e.message}")
            emptyList()
        }
    }

    // --- UPDATED: Get Non-Group Expenses Between Two Users ---
    suspend fun getNonGroupExpensesBetweenUsers(currentUserUid: String, friendUid: String): List<Expense> {
        if (currentUserUid.isBlank() || friendUid.isBlank()) return emptyList()
        return try {
            val userIds = listOf(currentUserUid, friendUid)
            // Query 1: Where either user is a participant
            val participantsQuery = expensesCollection
                .whereEqualTo("groupId", null)
                .whereArrayContainsAny("participants.uid", userIds)
                .get()
                .await()
            // Query 2: Where currentUser created it
            val createdByCurrentUserQuery = expensesCollection
                .whereEqualTo("groupId", null)
                .whereEqualTo("createdByUid", currentUserUid)
                .get()
                .await()
            // Query 3: Where friend created it
            val createdByFriendQuery = expensesCollection
                .whereEqualTo("groupId", null)
                .whereEqualTo("createdByUid", friendUid)
                .get()
                .await()

            // Combine results and filter duplicates
            val potentialExpenses = mutableSetOf<Expense>()
            potentialExpenses.addAll(participantsQuery.toObjects())
            potentialExpenses.addAll(createdByCurrentUserQuery.toObjects())
            potentialExpenses.addAll(createdByFriendQuery.toObjects())

            // Local Filtering: Ensure BOTH users are actually involved.
            val filteredExpenses = potentialExpenses.filter { expense ->
                val involvedUids = mutableSetOf(expense.createdByUid)
                expense.paidBy.forEach { involvedUids.add(it.uid) }
                expense.participants.forEach { involvedUids.add(it.uid) }
                involvedUids.contains(currentUserUid) && involvedUids.contains(friendUid)
            }.sortedByDescending { it.date } // Sort by date

            logD("Fetched ${filteredExpenses.size} non-group expenses between $currentUserUid and $friendUid after filtering.")
            filteredExpenses
        } catch (e: Exception) {
            logE("Error fetching non-group expenses between $currentUserUid and $friendUid: ${e.message}")
            emptyList()
        }
    }

    // --- Other potential functions (getExpenseById, update, delete) ... ---

}