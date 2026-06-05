package com.boxbox.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.boxbox.app.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

const val CHANNEL_ID_RACE = "boxbox_race"
const val CHANNEL_ID_LIVE = "boxbox_live"

class BoxBoxMessagingService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        val title = message.notification?.title ?: "BoxBox"
        val body = message.notification?.body ?: ""
        showNotification(this, title, body)
    }
}

class RaceAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra("title") ?: "Race starting soon!"
        val body = intent.getStringExtra("body") ?: "Head to BoxBox to follow along 🏎️"
        showNotification(context, title, body)
    }
}

fun createNotificationChannels(context: Context) {
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.createNotificationChannel(
        NotificationChannel(CHANNEL_ID_RACE, "Race Alerts", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Notifications for upcoming races and sessions"
        }
    )
    nm.createNotificationChannel(
        NotificationChannel(CHANNEL_ID_LIVE, "Live Updates", NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = "Live race control alerts"
        }
    )
}

fun showNotification(context: Context, title: String, body: String, channelId: String = CHANNEL_ID_RACE) {
    val intent = Intent(context, MainActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }
    val pendingIntent = PendingIntent.getActivity(
        context, 0, intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle(title)
        .setContentText(body)
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setContentIntent(pendingIntent)
        .setAutoCancel(true)
        .build()

    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    nm.notify(System.currentTimeMillis().toInt(), notification)
}
