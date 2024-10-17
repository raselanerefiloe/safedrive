package com.drivesolutions.safedrive.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.drivesolutions.safedrive.NotificationActivity
import com.drivesolutions.safedrive.R

fun sendNotification(context: Context, reportType: String, description: String, location: String) {
    val channelId = "road_reports_channel"

    // Intent to open your app when the notification is clicked
    val intent = Intent(context, NotificationActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    }

    val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

    // Build the notification
    val notificationBuilder = NotificationCompat.Builder(context, channelId)
        .setSmallIcon(R.drawable.ic_notification) // Replace with your notification icon
        .setContentTitle("New Road Report: $reportType")
        .setContentText(description)
        .setStyle(NotificationCompat.BigTextStyle().bigText(description)) // For long descriptions
        .setPriority(NotificationCompat.PRIORITY_HIGH)
        .setAutoCancel(true)
        .setContentIntent(pendingIntent)
        .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
        .setVibrate(longArrayOf(0, 1000, 500, 1000))

    // Get the NotificationManager
    val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    // Notify
    notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
}
