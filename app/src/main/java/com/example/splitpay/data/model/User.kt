package com.example.splitpay.data.model

data class User(
    val uid: String = "",
    val fullName: String = "",
    val username: String = "",
    val email: String = "",
    val phoneNumber: String = "",
    val profilePictureUrl: String = "",
    val qrCodeUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val friends: List<String> = emptyList(),
    val blockedUsers: List<String> = emptyList() // List of blocked user UIDs
)