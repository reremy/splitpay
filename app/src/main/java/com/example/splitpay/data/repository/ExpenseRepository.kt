package com.example.splitpay.data.repository

import android.util.Log
import com.example.splitpay.data.model.Expense
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
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

    fun getExpensesFlowForGroup(groupId: String): Flow<List<Expense>> = callbackFlow {
        if (groupId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        logD("Starting expense listener for group: $groupId")
        val query = expensesCollection
            .whereEqualTo("groupId", groupId)
            .orderBy("date", Query.Direction.DESCENDING) // Order by most recent first

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logE("Expense listener error for group $groupId: ${error.message}")
                close(error) // Close flow on error
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val expenses = snapshot.toObjects(Expense::class.java)
                logD("Emitting ${expenses.size} expenses for group $groupId")
                trySend(expenses) // Emit the latest list
            } else {
                logD("Expense snapshot was null for group $groupId")
                trySend(emptyList()) // Emit empty list if snapshot is null
            }
        }

        // Unregister listener when flow is cancelled
        awaitClose {
            logD("Stopping expense listener for group $groupId")
            listenerRegistration.remove()
        }
    }

    /**
     * Gets a real-time flow of all non-group expenses where the user is a participant.
     */
    fun getNonGroupExpensesFlow(currentUserUid: String): Flow<List<Expense>> = callbackFlow {
        if (currentUserUid.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        logD("Starting non-group expense listener for user: $currentUserUid")

        // Query: Get non-group expenses where the user is a participant.
        // This is the most critical query for balance calculations.
        val query = expensesCollection
            .whereEqualTo("groupId", null)
            .whereArrayContains("participants.uid", currentUserUid)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logE("Non-group expense listener error for $currentUserUid: ${error.message}")
                close(error) // Close flow on error
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val expenses = snapshot.toObjects(Expense::class.java)
                logD("Emitting ${expenses.size} non-group expenses for user $currentUserUid")
                trySend(expenses) // Emit the latest list
            } else {
                logD("Non-group expense snapshot was null for user $currentUserUid")
                trySend(emptyList()) // Emit empty list if snapshot is null
            }
        }

        // Unregister listener when flow is cancelled
        awaitClose {
            logD("Stopping non-group expense listener for $currentUserUid")
            listener.remove()
        }

        // NOTE: This implementation doesn't listen for expenses where the user
        // *paid* but wasn't a participant. This is a complex query to do
        // reactively and can be added later if that edge case is needed.
    }

    // --- Function to delete expenses (needed for Group Delete later) ---
    suspend fun deleteExpensesForGroup(groupId: String): Result<Unit> {
        return try {
            val querySnapshot = expensesCollection.whereEqualTo("groupId", groupId).get().await()
            if (querySnapshot.isEmpty) {
                logD("No expenses found to delete for group $groupId")
                return Result.success(Unit) // Nothing to delete
            }

            val batch = firestore.batch()
            querySnapshot.documents.forEach { doc ->
                batch.delete(doc.reference)
            }
            batch.commit().await()
            logD("Deleted ${querySnapshot.size()} expenses for group $groupId")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error deleting expenses for group $groupId: ${e.message}")
            Result.failure(e)
        }
    }

    fun getAllExpensesForUserFlow(userUid: String): Flow<List<Expense>> = callbackFlow {
        val query = expensesCollection
            .whereArrayContains("participants.uid", userUid) // Or a more complex query if needed

        val listener = query.addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                trySend(snapshot.toObjects())
            }
            // Handle error
        }
        awaitClose { listener.remove() }
    }



    // --- Other potential functions (getExpenseById, update, delete) ... ---

    /**
     * Gets a single expense by ID
     */
    suspend fun getExpenseById(expenseId: String): Result<Expense?> {
        return try {
            if (expenseId.isBlank()) {
                return Result.failure(IllegalArgumentException("Expense ID cannot be blank"))
            }
            val document = expensesCollection.document(expenseId).get().await()
            val expense = document.toObject(Expense::class.java)
            logD("Fetched expense with ID: $expenseId")
            Result.success(expense)
        } catch (e: Exception) {
            logE("Error fetching expense by ID $expenseId: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Updates an existing expense
     */
    suspend fun updateExpense(expense: Expense): Result<Unit> {
        return try {
            if (expense.id.isBlank()) {
                return Result.failure(IllegalArgumentException("Expense ID cannot be blank"))
            }
            expensesCollection.document(expense.id).set(expense).await()
            logD("Expense updated successfully with ID: ${expense.id}")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error updating expense: ${e.message}")
            Result.failure(e)
        }
    }

    /**
     * Deletes a single expense by ID
     */
    suspend fun deleteExpense(expenseId: String): Result<Unit> {
        return try {
            if (expenseId.isBlank()) {
                return Result.failure(IllegalArgumentException("Expense ID cannot be blank"))
            }
            expensesCollection.document(expenseId).delete().await()
            logD("Expense deleted successfully with ID: $expenseId")
            Result.success(Unit)
        } catch (e: Exception) {
            logE("Error deleting expense $expenseId: ${e.message}")
            Result.failure(e)
        }
    }

}