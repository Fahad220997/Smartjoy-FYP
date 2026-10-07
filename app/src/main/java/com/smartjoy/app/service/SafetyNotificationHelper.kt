package com.smartjoy.app.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.smartjoy.app.R

object SafetyNotificationHelper {
    private const val DANGER_CHANNEL_ID = "danger_zone_alerts"
    private const val TRACKING_CHANNEL_ID = "child_safety_alerts"
    
    private const val DANGER_NOTIFICATION_ID = 2001
    private const val TRACKING_NOTIFICATION_ID = 1001

    fun showDangerAlert(context: Context, zoneName: String) {
        ensureChannel(context, DANGER_CHANNEL_ID, context.getString(R.string.danger_channel_name))
        val notification = NotificationCompat.Builder(context, DANGER_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.danger_notification_title))
            .setContentText(context.getString(R.string.danger_notification_text, zoneName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .build()
        
        try {
            NotificationManagerCompat.from(context).notify(DANGER_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }

    fun showOutOfRangeAlert(context: Context, deviceName: String) {
        ensureChannel(context, TRACKING_CHANNEL_ID, context.getString(R.string.tracking_channel_name))
        val notification = NotificationCompat.Builder(context, TRACKING_CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle(context.getString(R.string.tracking_notification_title))
            .setContentText(context.getString(R.string.tracking_notification_text, deviceName))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(TRACKING_NOTIFICATION_ID, notification)
        } catch (_: SecurityException) {}
    }

    private fun ensureChannel(context: Context, id: String, name: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(id, name, importance).apply {
                enableVibration(true)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }
}
