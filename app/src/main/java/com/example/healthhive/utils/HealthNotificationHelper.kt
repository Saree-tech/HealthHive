package com.example.healthhive.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.healthhive.MainActivity
import com.example.healthhive.R

class HealthNotificationHelper(private val context: Context) {

    private val channelId = "health_reminders"

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Health Hive Reminders"
            val descriptionText = "Alerts for medications and doctor appointments"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(channelId, name, importance).apply {
                description = descriptionText
                // Optional: Enable lights or vibration by default
                enableLights(true)
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Shows a notification.
     * @param title The name of the medicine or appointment
     * @param message The dosage or location details
     * @param eventType Set to "APPOINTMENT" or "MEDICATION" to customize the alert
     */
    fun showNotification(title: String, message: String, eventType: String = "MEDICATION") {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(), // Unique request code
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Customizing based on type
        val notificationTitle = if (eventType == "APPOINTMENT") {
            "📅 Appointment Reminder"
        } else {
            "💊 Medication Reminder"
        }

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(notificationTitle)
            .setContentText("$title: $message")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Use current time as ID so multiple notifications can show at once
        val uniqueId = System.currentTimeMillis().toInt()
        notificationManager.notify(uniqueId, builder.build())
    }
}