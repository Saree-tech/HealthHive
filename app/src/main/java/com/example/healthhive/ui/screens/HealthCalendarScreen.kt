@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthhive.ui.screens

import android.Manifest
import android.app.DatePickerDialog
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.data.local.HealthEvent
import com.example.healthhive.viewmodel.HealthCalendarViewModel
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

    var isMedDialogOpen by remember { mutableStateOf(false) }
    var isApptDialogOpen by remember { mutableStateOf(false) }

    val medications = allEvents.filter { it.type == "MEDICATION" }
    val appointments = allEvents.filter { it.type == "APPOINTMENT" }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (!isGranted) {
            Toast.makeText(context, "Notifications disabled.", Toast.LENGTH_SHORT).show()
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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { isApptDialogOpen = true }) {
                        Icon(Icons.Default.Event, "Add Appointment", tint = Color(0xFF2D6A4F))
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isMedDialogOpen = true },
                containerColor = Color(0xFF1B4332),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
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

        if (isMedDialogOpen) {
            AddMedDialog(
                currentDate = selectedDate,
                onDismiss = { isMedDialogOpen = false },
                onSave = { name, dose, date ->
                    viewModel.addNewEvent(name, dose, "09:00 AM", date, "MEDICATION", RecurrenceType.DAILY)
                }
            )
        }

        if (isApptDialogOpen) {
            AddApptDialog(
                currentDate = selectedDate,
                onDismiss = { isApptDialogOpen = false },
                onSave = { title, time, date, type ->
                    viewModel.addNewEvent(title, "Doctor Appointment", time, date, "APPOINTMENT", type)
                }
            )
        }
    }
}

// --- FULL SCREEN INPUT DIALOGS ---

@Composable
fun AddMedDialog(currentDate: String, onDismiss: () -> Unit, onSave: (String, String, String) -> Unit) {
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }
    var dateString by remember { mutableStateOf(currentDate) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column(Modifier.fillMaxSize()) {
                SmallTopAppBar(
                    title = { Text("New Medication", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                    actions = {
                        TextButton(onClick = { if (name.isNotBlank()) { onSave(name, dose, dateString); onDismiss() } }) {
                            Text("SAVE", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B4332))
                        }
                    }
                )

                Column(Modifier.padding(24.dp).verticalScroll(scrollState).imePadding()) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Medicine Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.MedicalInformation, null, tint = Color(0xFF1B4332)) }
                    )
                    Spacer(Modifier.height(16.dp))
                    OutlinedTextField(
                        value = dose,
                        onValueChange = { dose = it },
                        label = { Text("Dose (e.g. 500mg)") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.Science, null, tint = Color(0xFF1B4332)) }
                    )

                    Spacer(Modifier.height(24.dp))
                    DatePickerField(dateString) { dateString = it }
                }
            }
        }
    }
}

@Composable
fun AddApptDialog(currentDate: String, onDismiss: () -> Unit, onSave: (String, String, String, RecurrenceType) -> Unit) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("10:00 AM") }
    var dateString by remember { mutableStateOf(currentDate) }
    var type by remember { mutableStateOf(RecurrenceType.ONETIME) }
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(modifier = Modifier.fillMaxSize(), color = Color.White) {
            Column(Modifier.fillMaxSize()) {
                SmallTopAppBar(
                    title = { Text("Schedule Appointment", fontWeight = FontWeight.Bold) },
                    navigationIcon = { IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, null) } },
                    actions = {
                        TextButton(onClick = { if (title.isNotBlank()) { onSave(title, time, dateString, type); onDismiss() } }) {
                            Text("DONE", fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B4332))
                        }
                    }
                )

                Column(Modifier.padding(24.dp).verticalScroll(scrollState).imePadding()) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Reason / Doctor Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        leadingIcon = { Icon(Icons.Default.PersonSearch, null, tint = Color(0xFF1B4332)) }
                    )

                    Spacer(Modifier.height(24.dp))
                    DatePickerField(dateString) { dateString = it }

                    Surface(
                        onClick = {
                            TimePickerDialog(context, { _, h, m ->
                                val ampm = if (h < 12) "AM" else "PM"
                                val hour = if (h % 12 == 0) 12 else h % 12
                                time = String.format("%02d:%02d %s", hour, m, ampm)
                            }, 10, 0, false).show()
                        },
                        modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF1F5F9)
                    ) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AccessTime, null, tint = Color(0xFF1B4332))
                            Spacer(Modifier.width(12.dp))
                            Text("Time: $time", fontWeight = FontWeight.Medium)
                        }
                    }

                    Spacer(Modifier.height(16.dp))
                    Text("Recurrence", fontSize = 14.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
                    Row(Modifier.padding(vertical = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(RecurrenceType.ONETIME, RecurrenceType.WEEKLY, RecurrenceType.MONTHLY).forEach { rType ->
                            FilterChip(
                                selected = type == rType,
                                onClick = { type = rType },
                                label = { Text(rType.name.lowercase().replaceFirstChar { it.uppercase() }) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DatePickerField(currentDateStr: String, onDateSelected: (String) -> Unit) {
    val context = LocalContext.current
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    val displaySdf = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault())

    val date = sdf.parse(currentDateStr) ?: Date()
    val calendar = Calendar.getInstance().apply { time = date }

    Surface(
        onClick = {
            DatePickerDialog(context, { _, year, month, day ->
                val newCal = Calendar.getInstance().apply { set(year, month, day) }
                onDateSelected(sdf.format(newCal.time))
            }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show()
        },
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = Color(0xFFF1F5F9)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CalendarToday, null, tint = Color(0xFF1B4332))
            Spacer(Modifier.width(12.dp))
            Text("Date: ${displaySdf.format(date)}", fontWeight = FontWeight.Medium)
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

// --- UI COMPONENTS ---

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
                Icon(if (isTaken) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked, null, tint = if (isTaken) Color(0xFF2D6A4F) else Color.Gray)
            }
            Column(Modifier.weight(1f)) {
                Text(med.title, fontWeight = FontWeight.Bold, color = if (isTaken) Color.Gray else Color.Black)
                Text(med.subtitle, fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(0.3f))
            }
        }
    }
}

@Composable
fun ProfessionalApptCard(appt: HealthEvent, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MedicalServices, null, tint = Color(0xFF059669))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(appt.title, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                Text("${appt.time} • ${appt.recurrence}", fontSize = 12.sp, color = Color(0xFF059669))
            }
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(0.3f))
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, icon: ImageVector) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, modifier = Modifier.size(18.dp), tint = Color.Gray)
        Text(title, Modifier.padding(start = 8.dp).weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable
fun EmptyStateHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.LightGray, fontSize = 14.sp)
    }
}