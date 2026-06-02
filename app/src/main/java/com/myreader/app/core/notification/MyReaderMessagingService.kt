package com.myreader.app.core.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.myreader.app.R
import com.myreader.app.presentation.MainActivity
import timber.log.Timber

class MyReaderMessagingService : FirebaseMessagingService() {

    companion object {
        const val CHANNEL_ID_BOOKS   = "new_books"
        const val CHANNEL_ID_READING = "reading_reminders"
        const val CHANNEL_ID_GENERAL = "general"
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Timber.d("FCM Token refreshed: $token")
        // Save token to Firestore for targeted notifications
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Timber.d("FCM message received: ${message.data}")

        val title = message.notification?.title ?: message.data["title"] ?: "My Reader"
        val body  = message.notification?.body  ?: message.data["body"]  ?: ""
        val type  = message.data["type"] ?: "general"
        val bookId = message.data["bookId"]

        val channelId = when (type) {
            "new_book"    -> CHANNEL_ID_BOOKS
            "reminder"    -> CHANNEL_ID_READING
            else          -> CHANNEL_ID_GENERAL
        }

        showNotification(title, body, channelId, bookId)
    }

    private fun showNotification(
        title: String, body: String,
        channelId: String, bookId: String? = null,
    ) {
        createChannels()

        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            bookId?.let { putExtra("bookId", it) }
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .build()

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(System.currentTimeMillis().toInt(), notification)
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        listOf(
            NotificationChannel(CHANNEL_ID_BOOKS, "New Books", NotificationManager.IMPORTANCE_DEFAULT)
                .apply { description = "Notifications about newly added books" },
            NotificationChannel(CHANNEL_ID_READING, "Reading Reminders", NotificationManager.IMPORTANCE_LOW)
                .apply { description = "Daily reading habit reminders" },
            NotificationChannel(CHANNEL_ID_GENERAL, "General", NotificationManager.IMPORTANCE_DEFAULT),
        ).forEach { manager.createNotificationChannel(it) }
    }
}
