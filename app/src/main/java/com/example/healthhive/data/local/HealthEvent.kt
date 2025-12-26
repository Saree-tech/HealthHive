package com.example.healthhive.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.healthhive.ui.screens.RecurrenceType

@Entity(tableName = "health_events")
data class HealthEvent(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val subtitle: String,
    val time: String,
    val startDate: String,
    val type: String,
    val recurrence: RecurrenceType,
    val datesTaken: List<String> = emptyList(),
    val isSynced: Boolean = false
)