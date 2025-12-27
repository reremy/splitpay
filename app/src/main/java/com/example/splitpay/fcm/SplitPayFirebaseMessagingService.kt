package com.example.splitpay.fcm

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.splitpay.MainActivity
import com.example.splitpay.R
import com.google.firebase.messaging.FirebaseMessagingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import com.example.splitpay.data.repository.UserRepository
import com.example.splitpay.logger.logD
import com.example.splitpay.logger.logE
import com.example.splitpay.logger.logI
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.launch


class SplitPayFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val userRepository = UserRepository()

    companion object {
        private const val CHANNEL_ID = "splitpay_notifications"
        private const val CHANNEL_NAME = "SplitPay Notifications"
        private const val CHANNEL_DESCRIPTION = "Notifications for expenses, payments, and reminders"
    }

    override fun onNewToken(token: String){
        super.onNewToken(token)
        logI("New FCM token generated: ${token.take(20)}...")

        serviceScope.launch{
            val result = userRepository.updateFcmToken(token)
            if (result.isSuccess){
                logI("FCM token is successfully saved in Firestore")
            } else {
                logE("Failed to save FCM token: ${result.exceptionOrNull()?.message}")
            }
        }
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        logD("FCM message received from: ${message.from}")

        val title = message.notification?.title ?: "SplitPay"
        val body = message.notification?.body ?: "You have a new notification"
        val clickAction = message.data["click_action"] ?: "ACTIVITY_TAB"

        logI("Notification received - Title: $title, Body: $body")

        showNotification(title, body, clickAction)
    }

    private fun showNotification(title: String, body: String, clickAction: String){
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        createNotificationChannel(notificationManager)

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("navigate_to", "activity") // Signal to navigate to Activity tab
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(body)
            //.setSmallIcon(R.drawable.ic_notification) // Replace with your app icon
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
        logI("Notification displayed: $title")
    }

    private fun createNotificationChannel(notificationManager: NotificationManager){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val channel = NotificationChannel(CHANNEL_ID,CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH)
                .apply {
                    description = CHANNEL_DESCRIPTION
                    enableVibration(true)
                }
            notificationManager.createNotificationChannel(channel)
        }
    }




}