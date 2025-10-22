package com.example.splitpay.data.model

data class User(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val friends: List<String> = emptyList()
)