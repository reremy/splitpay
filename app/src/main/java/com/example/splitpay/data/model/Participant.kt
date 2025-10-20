package com.example.splitpay.data.model

data class Participant(
    val uid: String,
    val name: String,
    val isChecked: Boolean = true,
    val splitValue: String = "0.00", // Value used for unequal split (amount, percentage, or share count)
    val owesAmount: Double = 0.0 // The calculated amount the participant owes
)
