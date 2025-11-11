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

/**
 * Repository for managing expense and payment data in Firestore.
 *
 * This repository handles all expense-related operations including:
 * - Creating, reading, updating, and deleting expenses and payments
 * - Querying expenses by group or user
 * - Real-time listening to expense changes via Flows
 * - Distinguishing between regular expenses (EXPENSE) and settlement payments (PAYMENT)
 * - Handling both group and non-group (friend-to-friend) expenses
 */
class ExpenseRepository(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) {
    private val expensesCollection = firestore.collection("expenses")

    /**
     * Generates a new unique expense ID without creating a Firestore document.
     *
     * Useful when you need to know the expense ID in advance, such as:
     * - Uploading related files (receipt images) to Storage with the expense ID in the path
     * - Referencing the expense in other operations before it's fully created
     *
     * @return A unique Firestore-generated document ID
     */
    fun generateExpenseId(): String {
        return expensesCollection.document().id
    }

    /**
     * Creates a new expense in Firestore.
     *
     * If the expense object's ID is blank, a new ID is automatically generated.
     * Otherwise, the provided ID is used (useful when pre-generating IDs for file uploads).
     *
     * @param expense The expense object to save
     * @return Result with the expense ID on success, or the exception on failure
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

    /**
     * Fetches expenses for multiple groups in a single operation.
     *
     * Firestore's `whereIn` operator is limited to 10 items per query, so this function
     * automatically chunks the group IDs into batches of 10 and combines the results.
     *
     * @param groupIds List of group IDs to fetch expenses for
     * @return List of all expenses from all specified groups, sorted by date (newest first)
     */
    suspend fun getExpensesForGroups(groupIds: List<String>): List<Expense> {
        if (groupIds.isEmpty()) {
            return emptyList()
        }
        return try {
            // Firestore 'whereIn' is limited to 10 items, so chunk the IDs
            val groupChunks = groupIds.chunked(10)
            val expenses = mutableListOf<Expense>()

            for (chunk in groupChunks) {
                if (chunk.isEmpty()) continue
                val querySnapshot = expensesCollection
                    .whereIn("groupId", chunk)
                    .orderBy("date", Query.Direction.DESCENDING)
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

    /**
     * Fetches all friend-to-friend expenses between two users.
     *
     * This function handles both new and legacy data:
     * - New: Expenses with groupId == "non_group"
     * - Legacy: Expenses with groupId == null
     *
     * **Important:** Client-side filtering is applied to ensure BOTH users are involved
     * (either as creator, payer, or participant) since Firestore doesn't support complex
     * "AND" queries on arrays.
     *
     * @param currentUserUid The current user's UID
     * @param friendUid The friend's UID
     * @return List of expenses involving both users, sorted by date (newest first)
     */
    suspend fun getNonGroupExpensesBetweenUsers(currentUserUid: String, friendUid: String): List<Expense> {
        if (currentUserUid.isBlank() || friendUid.isBlank()) return emptyList()
        return try {
            // Query 1: Get expenses with groupId == "non_group"
            val nonGroupQuery = expensesCollection
                .whereEqualTo("groupId", "non_group")
                .get()
                .await()

            // Query 2: Get expenses with groupId == null (backward compatibility)
            val nullGroupQuery = expensesCollection
                .whereEqualTo("groupId", null)
                .get()
                .await()

            // Combine results and remove duplicates
            val potentialExpenses = mutableSetOf<Expense>()
            potentialExpenses.addAll(nonGroupQuery.toObjects())
            potentialExpenses.addAll(nullGroupQuery.toObjects())

            // Filter to ensure BOTH users are actually involved in the expense
            // An expense involves a user if they are the creator, a payer, or a participant
            val filteredExpenses = potentialExpenses.filter { expense ->
                val involvedUids = mutableSetOf(expense.createdByUid)
                expense.paidBy.forEach { involvedUids.add(it.uid) }
                expense.participants.forEach { involvedUids.add(it.uid) }
                involvedUids.contains(currentUserUid) && involvedUids.contains(friendUid)
            }.sortedByDescending { it.date }

            logD("Fetched ${filteredExpenses.size} non-group expenses between $currentUserUid and $friendUid after filtering.")
            filteredExpenses
        } catch (e: Exception) {
            logE("Error fetching non-group expenses between $currentUserUid and $friendUid: ${e.message}")
            emptyList()
        }
    }

    /**
     * Creates a real-time Flow of expenses for a specific group.
     *
     * This Flow automatically updates when:
     * - New expenses are added to the group
     * - Existing expenses are modified
     * - Expenses are deleted
     *
     * The listener is automatically cleaned up when the Flow is cancelled.
     *
     * @param groupId The group ID to listen to (can be "non_group" for friend-to-friend expenses)
     * @return Flow emitting lists of expenses, sorted by date (newest first)
     */
    fun getExpensesFlowForGroup(groupId: String): Flow<List<Expense>> = callbackFlow {
        if (groupId.isBlank()) {
            trySend(emptyList())
            awaitClose { }
            return@callbackFlow
        }

        logD("Starting expense listener for group: $groupId")
        val query = expensesCollection
            .whereEqualTo("groupId", groupId)
            .orderBy("date", Query.Direction.DESCENDING)

        val listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                logE("Expense listener error for group $groupId: ${error.message}")
                close(error)
                return@addSnapshotListener
            }
            if (snapshot != null) {
                val expenses = snapshot.toObjects(Expense::class.java)
                logD("Emitting ${expenses.size} expenses for group $groupId")
                trySend(expenses)
            } else {
                logD("Expense snapshot was null for group $groupId")
                trySend(emptyList())
            }
        }

        awaitClose {
            logD("Stopping expense listener for group $groupId")
            listenerRegistration.remove()
        }
    }

    /**
     * Gets a real-time flow of all non-group expenses where the user is a participant.
     * NOTE: This only gets expenses with groupId == null. For groupId == "non_group", use getExpensesFlowForGroup("non_group")
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

    /**
     * Deletes all expenses associated with a group.
     *
     * Uses Firestore batch operations to delete all expenses in a single atomic operation.
     * This is typically called when a group is being deleted.
     *
     * @param groupId The ID of the group whose expenses should be deleted
     * @return Result indicating success or failure
     */
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

    /**
     * Creates a real-time Flow of all expenses where the user is a participant.
     *
     * **Note:** This query has limitations:
     * - Firestore doesn't support querying nested arrays directly
     * - This will miss expenses where the user paid but isn't a participant
     *
     * @param userUid The user's UID
     * @return Flow emitting all expenses involving the user
     */
    fun getAllExpensesForUserFlow(userUid: String): Flow<List<Expense>> = callbackFlow {
        val query = expensesCollection
            .whereArrayContains("participants.uid", userUid)

        val listener = query.addSnapshotListener { snapshot, error ->
            if (snapshot != null) {
                trySend(snapshot.toObjects())
            }
        }
        awaitClose { listener.remove() }
    }

    /**
     * Retrieves a single expense by its ID.
     *
     * @param expenseId The unique ID of the expense
     * @return Result with the Expense object (or null if not found), or the exception on failure
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
     * Updates an existing expense in Firestore.
     *
     * Completely replaces the existing expense document with the new data.
     * The expense ID must not be blank.
     *
     * @param expense The updated expense object (must have a valid ID)
     * @return Result indicating success or failure
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
     * Permanently deletes an expense from Firestore.
     *
     * **Warning:** This operation cannot be undone.
     *
     * @param expenseId The ID of the expense to delete
     * @return Result indicating success or failure
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