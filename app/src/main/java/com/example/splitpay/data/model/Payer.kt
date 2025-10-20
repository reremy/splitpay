package com.example.splitpay.data.model

data class Payer(
    val uid: String,
    val name: String,
    val amount: String = "0.00", // Amount paid by this user (String for input)
    val isChecked: Boolean = false // Used in Payer selection UI
)
