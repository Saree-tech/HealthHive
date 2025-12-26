@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthhive.ui.screens

import android.Manifest
import android.app.TimePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.data.local.HealthEvent
import com.example.healthhive.viewmodel.HealthCalendarViewModel
import com.example.healthhive.utils.HealthNotificationHelper
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun HealthCalendarScreen(
    onBack: () -> Unit,
    showProgressFromPrefs: Boolean,
    viewModel: HealthCalendarViewModel = viewModel()
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val allEvents by viewModel.dailyEvents.collectAsState()

    var isMedSheetOpen by remember { mutableStateOf(false) }
    var isApptSheetOpen by remember { mutableStateOf(false) }

    val medications = allEvents.filter { it.type == "MEDICATION" }
    val appointments = allEvents.filter { it.type == "APPOINTMENT" }

    // --- PERMISSION REQUEST LOGIC ---
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notifications disabled. Alarms won't show.", Toast.LENGTH_LONG).show()
        }
    }

    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Health Hub", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // TEST NOTIFICATION BUTTON (Tests both styles)
                    IconButton(onClick = {
                        val helper = HealthNotificationHelper(context)
                        helper.showNotification("Medication Test", "Take your 500mg dose", "MEDICATION")
                        helper.showNotification("Appointment Test", "Visit Dr. Smith at 10AM", "APPOINTMENT")
                    }) {
                        Icon(imageVector = Icons.Default.NotificationsActive, contentDescription = "Test", tint = Color(0xFF2D6A4F))
                    }
                    IconButton(onClick = { isApptSheetOpen = true }) {
                        Icon(imageVector = Icons.Default.Event, contentDescription = "Add Appointment", tint = Color(0xFF2D6A4F))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isMedSheetOpen = true },
                containerColor = Color(0xFF1B4332),
                contentColor = Color.White,
                icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Medication") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            if (showProgressFromPrefs) {
                item { ProfessionalDashboard(medications, selectedDate) }
            }

            item {
                CalendarStrip(selectedDate, allEvents) { viewModel.selectDate(it) }
            }

            item { SectionHeader("Scheduled Checkups", Icons.Default.DateRange) }

            if (appointments.isEmpty()) {
                item { EmptyStateHint("No appointments for this day") }
            } else {
                items(items = appointments, key = { it.id }) { appt ->
                    ProfessionalApptCard(appt) { viewModel.deleteEvent(appt.id) }
                }
            }

            item { SectionHeader("Medication Plan", Icons.Default.MedicalServices) }

            if (medications.isEmpty()) {
                item { EmptyStateHint("No medications scheduled") }
            } else {
                items(items = medications, key = { it.id }) { med ->
                    val isTaken = med.datesTaken.contains(selectedDate)
                    ProfessionalMedCard(
                        med = med,
                        isTaken = isTaken,
                        onToggle = { viewModel.toggleMedicationTaken(med, selectedDate) },
                        onDelete = { viewModel.deleteEvent(med.id) }
                    )
                }
            }
        }

        if (isMedSheetOpen) {
            AddMedSheet(
                onDismiss = { isMedSheetOpen = false },
                onSave = { name, dose ->
                    viewModel.addNewEvent(name, dose, "09:00 AM", selectedDate, "MEDICATION", RecurrenceType.DAILY)
                }
            )
        }

        if (isApptSheetOpen) {
            AddApptSheet(
                onDismiss = { isApptSheetOpen = false },
                onSave = { title, time, type ->
                    viewModel.addNewEvent(title, "Doctor Appointment", time, selectedDate, "APPOINTMENT", type)
                }
            )
        }
    }
}

// --- CALENDAR STRIP ---

@Composable
fun CalendarStrip(selectedDate: String, allEvents: List<HealthEvent>, onSelect: (String) -> Unit) {
    val sdf = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val dayFormat = remember { SimpleDateFormat("EEE", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("dd", Locale.getDefault()) }

    val dates = remember {
        List(14) { i -> Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, i) }.time }
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        items(dates) { date ->
            val dStr = sdf.format(date)
            val isSelected = dStr == selectedDate
            val hasAppointment = allEvents.any { it.type == "APPOINTMENT" && isEventOnDate(it, dStr) }

            Column(
                Modifier
                    .width(64.dp)
                    .height(95.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isSelected) Color(0xFF1B4332) else Color.White)
                    .clickable { onSelect(dStr) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(dayFormat.format(date), color = if (isSelected) Color.White.copy(0.7f) else Color.Gray, fontSize = 12.sp)
                Text(dateFormat.format(date), color = if (isSelected) Color.White else Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                if (hasAppointment) {
                    Box(Modifier.padding(top = 4.dp).size(6.dp).background(if (isSelected) Color(0xFFB7E4C7) else Color(0xFF2D6A4F), CircleShape))
                }
            }
        }
    }
}

private fun isEventOnDate(event: HealthEvent, targetDate: String): Boolean {
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return try {
        val target = sdf.parse(targetDate) ?: return false
        val start = sdf.parse(event.startDate) ?: return false
        if (target.before(start) && targetDate != event.startDate) return false

        when (event.recurrence) {
            RecurrenceType.DAILY -> true
            RecurrenceType.WEEKLY -> {
                val calT = Calendar.getInstance().apply { time = target }
                val calS = Calendar.getInstance().apply { time = start }
                calT.get(Calendar.DAY_OF_WEEK) == calS.get(Calendar.DAY_OF_WEEK)
            }
            RecurrenceType.MONTHLY -> {
                val calT = Calendar.getInstance().apply { time = target }
                val calS = Calendar.getInstance().apply { time = start }
                calT.get(Calendar.DAY_OF_MONTH) == calS.get(Calendar.DAY_OF_MONTH)
            }
            else -> event.startDate == targetDate
        }
    } catch (e: Exception) { false }
}

// --- UI CARDS ---

@Composable
fun ProfessionalDashboard(meds: List<HealthEvent>, selectedDate: String) {
    val completed = meds.count { it.datesTaken.contains(selectedDate) }
    val total = meds.size
    val progress = if (total > 0) completed.toFloat() / total else 0f

    Card(Modifier.padding(16.dp), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B4332))) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Daily Progress", color = Color.White.copy(0.7f), fontSize = 14.sp)
                Text("$completed / $total Meds", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            CircularProgressIndicator(progress = { progress }, color = Color(0xFFB7E4C7), trackColor = Color.White.copy(0.1f))
        }
    }
}

@Composable
fun ProfessionalMedCard(med: HealthEvent, isTaken: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle) {
                Icon(imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, contentDescription = null, tint = if (isTaken) Color(0xFF2D6A4F) else Color.Gray)
            }
            Column(Modifier.weight(1f)) {
                Text(med.title, fontWeight = FontWeight.Bold, color = if (isTaken) Color.Gray else Color.Black)
                Text(med.subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(0.3f))
            }
        }
    }
}

@Composable
fun ProfessionalApptCard(appt: HealthEvent, onDismiss: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = Icons.Default.MedicalServices, contentDescription = null, tint = Color(0xFF059669))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(appt.title, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                Text("${appt.time} • ${appt.recurrence}", fontSize = 12.sp, color = Color(0xFF059669))
            }
            IconButton(onClick = onDismiss) {
                Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(0.3f))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp), tint = Color.Gray)
        Text(title, Modifier.padding(start = 8.dp).weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun EmptyStateHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.LightGray, fontSize = 14.sp)
    }
}

// --- UPDATED BOTTOM SHEETS (Keyboard & Scroll Fix) ---

@Composable
fun AddMedSheet(onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(24.dp)
                .imePadding() // Pushes content above keyboard
                .navigationBarsPadding()
                .verticalScroll(scrollState) // Allows scrolling when keyboard is open
        ) {
            Text("New Medication", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Medicine Name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            OutlinedTextField(value = dose, onValueChange = { dose = it }, label = { Text("Dose (e.g. 500mg)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
            Button(
                onClick = { if (name.isNotBlank()) { onSave(name, dose); onDismiss() } },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4332))
            ) { Text("Save Medication") }
        }
    }
}

@Composable
fun AddApptSheet(onDismiss: () -> Unit, onSave: (String, String, RecurrenceType) -> Unit) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("10:00 AM") }
    var type by remember { mutableStateOf(RecurrenceType.ONETIME) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            Modifier
                .padding(24.dp)
                .imePadding()
                .navigationBarsPadding()
                .verticalScroll(scrollState)
        ) {
            Text("Schedule Checkup", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Reason / Doctor") }, modifier = Modifier.fillMaxWidth(), singleLine = true)

            Surface(
                onClick = {
                    TimePickerDialog(context, { _, h, m ->
                        val ampm = if (h < 12) "AM" else "PM"
                        val hour = if (h % 12 == 0) 12 else h % 12
                        time = String.format("%02d:%02d %s", hour, m, ampm)
                    }, 10, 0, false).show()
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF1F3F4)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.AccessTime, contentDescription = null, tint = Color.Gray)
                    Spacer(Modifier.width(8.dp))
                    Text("Time: $time")
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(RecurrenceType.ONETIME, RecurrenceType.WEEKLY, RecurrenceType.MONTHLY).forEach { rType ->
                    FilterChip(
                        selected = type == rType,
                        onClick = { type = rType },
                        label = { Text(rType.name.lowercase().replaceFirstChar { it.uppercase() }) }
                    )
                }
            }

            Button(
                onClick = { if (title.isNotBlank()) { onSave(title, time, type); onDismiss() } },
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp, bottom = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4332))
            ) { Text("Schedule") }
        }
    }
}