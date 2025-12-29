package com.example.healthhive.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.healthhive.notifications.HealthNotificationHelper

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val title = intent?.getStringExtra("TITLE") ?: return
        val subtitle = intent.getStringExtra("SUBTITLE") ?: ""

        Log.d("HealthAlarm", "Receiver triggered for: $title")

        HealthNotificationHelper.showNotification(
            context = context,
            title = title,
            message = subtitle
        )
    }
}