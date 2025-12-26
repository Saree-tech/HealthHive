package com.example.healthhive.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.healthhive.utils.HealthNotificationHelper

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 1. Extract data passed from the ViewModel/AlarmManager
        val title = intent.getStringExtra("TITLE") ?: "Health Alert"
        val subtitle = intent.getStringExtra("SUBTITLE") ?: ""

        // 2. Extract the TYPE (Medication or Appointment)
        // Default to "MEDICATION" if nothing is passed
        val type = intent.getStringExtra("TYPE") ?: "MEDICATION"

        // 3. Trigger the notification using the helper instance
        val notificationHelper = HealthNotificationHelper(context)
        notificationHelper.showNotification(
            title = title,
            message = subtitle,
            eventType = type
        )
    }
}