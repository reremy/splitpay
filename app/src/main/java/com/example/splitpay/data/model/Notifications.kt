package com.example.splitpay.data.model

data class Notifications(
    val id: String = "",
    val userId: String = "",
    val activityId: String = "",
    val title: String = "",
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val type: String = NotificationType.ACTIVITY.name
)

enum class NotificationType{
    ACTIVITY,
    REMINDER
}
