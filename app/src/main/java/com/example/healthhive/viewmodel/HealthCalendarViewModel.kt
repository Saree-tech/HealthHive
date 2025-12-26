package com.example.healthhive.viewmodel

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.local.HealthDatabase
import com.example.healthhive.data.local.HealthEvent
import com.example.healthhive.receiver.AlarmReceiver
import com.example.healthhive.ui.screens.RecurrenceType
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HealthCalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val eventDao = HealthDatabase.getDatabase(application).eventDao()
    private val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    private val timeSdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

    private val _selectedDate = MutableStateFlow(sdf.format(Date()))
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val dailyEvents: StateFlow<List<HealthEvent>> = eventDao.getAllEventsFlow(auth.currentUser?.uid ?: "")
        .combine(_selectedDate) { events, date ->
            events.filter { isEventVisibleOnDate(it, date) }
        }
        .flowOn(Dispatchers.IO)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        fetchFromFirestore()
    }

    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    // --- ADD EVENT WITH ALARM LOGIC ---
    fun addNewEvent(title: String, subtitle: String, time: String, date: String, type: String, recurrence: RecurrenceType) {
        val userId = auth.currentUser?.uid ?: return
        val eventId = UUID.randomUUID().toString()

        val newEvent = HealthEvent(
            id = eventId,
            userId = userId,
            title = title,
            subtitle = subtitle,
            time = time,
            startDate = date,
            type = type,
            recurrence = recurrence,
            isSynced = false
        )

        viewModelScope.launch(Dispatchers.IO) {
            // 1. Save locally
            eventDao.insertEvent(newEvent)

            // 2. Schedule Alarm for BOTH Medications and Appointments
            // We removed the "if type == MEDICATION" check so both trigger alerts
            scheduleAlarm(newEvent)

            // 3. Sync to Firebase
            db.collection("health_hub").document(eventId).set(newEvent)
                .addOnSuccessListener {
                    viewModelScope.launch(Dispatchers.IO) {
                        eventDao.insertEvent(newEvent.copy(isSynced = true))
                    }
                }
        }
    }

    fun deleteEvent(eventId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            // Find the event in the local DB to cancel the alarm properly
            val event = eventDao.getEventById(eventId)
            event?.let { cancelAlarm(it) }

            eventDao.deleteEventById(eventId)
            db.collection("health_hub").document(eventId).delete()
        }
    }

    fun toggleMedicationTaken(event: HealthEvent, date: String) {
        val currentDates = event.datesTaken.toMutableList()
        if (currentDates.contains(date)) {
            currentDates.remove(date)
        } else {
            currentDates.add(date)
        }

        val updatedEvent = event.copy(datesTaken = currentDates, isSynced = false)

        viewModelScope.launch(Dispatchers.IO) {
            eventDao.insertEvent(updatedEvent)
            db.collection("health_hub").document(event.id)
                .update("datesTaken", currentDates)
                .addOnSuccessListener {
                    viewModelScope.launch(Dispatchers.IO) {
                        eventDao.insertEvent(updatedEvent.copy(isSynced = true))
                    }
                }
        }
    }

    // --- ALARM MANAGER HELPERS ---

    private fun scheduleAlarm(event: HealthEvent) {
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(getApplication(), AlarmReceiver::class.java).apply {
            putExtra("TITLE", event.title)
            putExtra("SUBTITLE", event.subtitle)
            putExtra("TYPE", event.type) // CRITICAL: Added type so Receiver knows if it's MEDICATION or APPOINTMENT
        }

        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance()
        try {
            val dateObj = sdf.parse(event.startDate) ?: Date()
            val timeObj = timeSdf.parse(event.time) ?: return

            val timeCal = Calendar.getInstance().apply { time = timeObj }

            calendar.time = dateObj
            calendar.set(Calendar.HOUR_OF_DAY, timeCal.get(Calendar.HOUR_OF_DAY))
            calendar.set(Calendar.MINUTE, timeCal.get(Calendar.MINUTE))
            calendar.set(Calendar.SECOND, 0)

            // If the time has already passed for today, don't trigger unless it's a recurring event
            if (calendar.before(Calendar.getInstance()) && event.recurrence != RecurrenceType.ONETIME) {
                calendar.add(Calendar.DATE, 1)
            }

            // Using setExactAndAllowWhileIdle to ensure it fires even in Doze mode
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("Alarm", "Alarm set for ${event.type}: ${event.title} at ${calendar.time}")
        } catch (e: Exception) {
            Log.e("Alarm", "Error parsing date/time", e)
        }
    }

    private fun cancelAlarm(event: HealthEvent) {
        val alarmManager = getApplication<Application>().getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(getApplication(), AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            getApplication(),
            event.id.hashCode(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            Log.d("Alarm", "Alarm cancelled for ${event.title}")
        }
    }

    private fun fetchFromFirestore() {
        val userId = auth.currentUser?.uid ?: return
        db.collection("health_hub")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                viewModelScope.launch(Dispatchers.IO) {
                    snapshot?.documents?.forEach { doc ->
                        try {
                            val event = HealthEvent(
                                id = doc.id,
                                userId = doc.getString("userId") ?: "",
                                title = doc.getString("title") ?: "",
                                subtitle = doc.getString("subtitle") ?: "",
                                time = doc.getString("time") ?: "08:00 AM",
                                startDate = doc.getString("startDate") ?: "",
                                type = doc.getString("type") ?: "MEDICATION",
                                recurrence = RecurrenceType.valueOf(doc.getString("recurrence") ?: "DAILY"),
                                datesTaken = (doc.get("datesTaken") as? List<*>)?.map { it.toString() } ?: emptyList(),
                                isSynced = true
                            )
                            eventDao.insertEvent(event)

                            // Re-schedule alarm if it's missing (e.g., after a fresh install/sync)
                            scheduleAlarm(event)
                        } catch (e: Exception) {
                            Log.e("ViewModel", "Mapping error", e)
                        }
                    }
                }
            }
    }

    private fun isEventVisibleOnDate(event: HealthEvent, targetDate: String): Boolean {
        return try {
            val target = sdf.parse(targetDate) ?: return false
            val base = sdf.parse(event.startDate) ?: return false

            if (target.before(base) && targetDate != event.startDate) return false

            when (event.recurrence) {
                RecurrenceType.ONETIME, RecurrenceType.ONE_TIME -> event.startDate == targetDate
                RecurrenceType.DAILY -> true
                RecurrenceType.WEEKLY -> {
                    val calT = Calendar.getInstance().apply { time = target }
                    val calB = Calendar.getInstance().apply { time = base }
                    calT.get(Calendar.DAY_OF_WEEK) == calB.get(Calendar.DAY_OF_WEEK)
                }
                RecurrenceType.MONTHLY -> {
                    val calT = Calendar.getInstance().apply { time = target }
                    val calB = Calendar.getInstance().apply { time = base }
                    calT.get(Calendar.DAY_OF_MONTH) == calB.get(Calendar.DAY_OF_MONTH)
                }
            }
        } catch (e: Exception) {
            false
        }
    }
}