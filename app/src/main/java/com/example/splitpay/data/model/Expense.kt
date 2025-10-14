package com.example.splitpay.data.model

import com.google.firebase.firestore.Exclude

data class Expense(
    val id: String = "", // Unique ID (Firestore Document ID)
    val groupId: String = "", // Which group this expense belongs to
    val description: String = "", // e.g., "Dinner at Kani House"
    val amount: Double = 0.0,
    val paidByUid: String = "", // The UID of the user who paid the full amount
    val date: Long = System.currentTimeMillis(),

    // Key: UID of the member who owes, Value: Amount this member owes the payer.
    // E.g., { "uid_friend": 50.0 }
    val individualShares: Map<String, Double> = emptyMap(),

    // An optional field to store the payer's share (if they owe themselves)
    @get:Exclude val payerShare: Double = 0.0,

    // A simplified flag for tracking when one member pays another back (settlement)
    val isSettlement: Boolean = false
)