package com.example.healthhive.ui.screens

import android.app.TimePickerDialog
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName
import java.text.SimpleDateFormat
import java.util.*

// --- MODELS ---

enum class RecurrenceType { ONETIME, ONE_TIME, DAILY, WEEKLY, MONTHLY }

@IgnoreExtraProperties
data class MedicationData(
    val id: String = "",
    val userId: String = "",
    val name: String = "",
    val dosage: String = "",
    val time: String = "08:00 AM",
    val datesTaken: List<String> = emptyList(),
    val recurrence: RecurrenceType = RecurrenceType.DAILY,

    @get:PropertyName("isTaken") @set:PropertyName("isTaken")
    var isTaken: Boolean = false,

    @get:PropertyName("dateAdded") @set:PropertyName("dateAdded")
    var dateAdded: Long = System.currentTimeMillis()
)

@IgnoreExtraProperties
data class Appointment(
    val id: String = "",
    val userId: String = "",
    val title: String = "",
    val date: String = "", // We keep this as String for your logic
    val time: String = "10:00 AM",
    val type: RecurrenceType = RecurrenceType.ONETIME
)

// --- MAIN SCREEN ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HealthCalendarScreen(onBack: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    val sdf = remember { SimpleDateFormat("yyyyMMdd", Locale.getDefault()) }
    val today = remember { sdf.format(Date()) }

    var selectedDate by remember { mutableStateOf(today) }
    var medications by remember { mutableStateOf<List<MedicationData>>(emptyList()) }
    var appointments by remember { mutableStateOf<List<Appointment>>(emptyList()) }

    var isMedSheetOpen by remember { mutableStateOf(false) }
    var isApptSheetOpen by remember { mutableStateOf(false) }

    val dailyAppts by remember(appointments, selectedDate) {
        derivedStateOf { appointments.filter { isApptVisibleOnDate(it, selectedDate) } }
    }

    val medicationSplit by remember(medications, selectedDate) {
        derivedStateOf { medications.partition { it.datesTaken.contains(selectedDate) } }
    }

    DisposableEffect(userId) {
        if (userId.isEmpty()) return@DisposableEffect onDispose {}

        val medListener = db.collection("medications")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snap, _ ->
                medications = snap?.toObjects(MedicationData::class.java) ?: emptyList()
            }

        val apptListener = db.collection("appointments")
            .whereEqualTo("userId", userId)
            .addSnapshotListener { snap, error ->
                if (error != null) {
                    Log.e("Firestore", "Error listening to appts", error)
                    return@addSnapshotListener
                }

                // SAFE MAPPING FIX:
                // Instead of snap?.toObjects(), we manually map to catch the Long vs String error
                val list = mutableListOf<Appointment>()
                snap?.documents?.forEach { doc ->
                    try {
                        // Check if 'date' is a Long and convert it to String if necessary
                        val rawDate = doc.get("date")
                        val dateStr = when (rawDate) {
                            is Long -> rawDate.toString()
                            is String -> rawDate
                            else -> ""
                        }

                        val appt = Appointment(
                            id = doc.id,
                            userId = doc.getString("userId") ?: "",
                            title = doc.getString("title") ?: "",
                            date = dateStr,
                            time = doc.getString("time") ?: "10:00 AM",
                            type = RecurrenceType.valueOf(doc.getString("type") ?: "ONETIME")
                        )
                        list.add(appt)
                    } catch (e: Exception) {
                        Log.e("Firestore", "Failed to map document ${doc.id}", e)
                    }
                }
                appointments = list
            }

        onDispose {
            medListener.remove()
            apptListener.remove()
        }
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Health Hub", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, "Back") }
                },
                actions = {
                    IconButton(onClick = { isApptSheetOpen = true }) {
                        Icon(Icons.Default.Event, tint = Color(0xFF2D6A4F), contentDescription = "Add Appt")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { isMedSheetOpen = true },
                containerColor = Color(0xFF1B4332),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Add, null) },
                text = { Text("Add Medication") }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            contentPadding = PaddingValues(bottom = 80.dp)
        ) {
            item { ProfessionalDashboard(medications, selectedDate) }

            item { CalendarStrip(selectedDate, appointments) { selectedDate = it } }

            item { SectionHeader("Scheduled Checkups", dailyAppts.size, Icons.Default.DateRange) }
            if (dailyAppts.isEmpty()) {
                item { EmptyStateHint("No appointments for this day") }
            } else {
                items(dailyAppts, key = { it.id }) { appt ->
                    ProfessionalApptCard(appt) {
                        db.collection("appointments").document(appt.id).delete()
                    }
                }
            }

            val (takenMeds, pendingMeds) = medicationSplit
            item { SectionHeader("Medication Plan", medications.size, Icons.Default.MedicalServices) }

            items(pendingMeds, key = { it.id }) { med ->
                ProfessionalMedCard(med, isTaken = false,
                    onToggle = {
                        db.collection("medications").document(med.id)
                            .update("datesTaken", FieldValue.arrayUnion(selectedDate))
                    },
                    onDelete = { db.collection("medications").document(med.id).delete() }
                )
            }
            items(takenMeds, key = { it.id }) { med ->
                ProfessionalMedCard(med, isTaken = true,
                    onToggle = {
                        db.collection("medications").document(med.id)
                            .update("datesTaken", FieldValue.arrayRemove(selectedDate))
                    },
                    onDelete = { db.collection("medications").document(med.id).delete() }
                )
            }
        }

        if (isMedSheetOpen) AddMedSheet(onDismiss = { isMedSheetOpen = false }, db, userId)
        if (isApptSheetOpen) AddApptSheet(onDismiss = { isApptSheetOpen = false }, db, userId, selectedDate)
    }
}

// --- REMAINING UI COMPONENTS STAY THE SAME ---
@Composable
fun CalendarStrip(selectedDate: String, appointments: List<Appointment>, onSelect: (String) -> Unit) {
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
            val hasEvent = appointments.any { isApptVisibleOnDate(it, dStr) }

            Column(
                Modifier
                    .width(64.dp).height(90.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(if (isSelected) Color(0xFF1B4332) else Color.White)
                    .clickable { onSelect(dStr) }
                    .padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(dayFormat.format(date), color = if (isSelected) Color.White.copy(0.7f) else Color.Gray, fontSize = 12.sp)
                Text(dateFormat.format(date), color = if (isSelected) Color.White else Color.Black, fontSize = 20.sp, fontWeight = FontWeight.Bold)

                if (hasEvent) {
                    Box(Modifier.padding(top = 4.dp).size(6.dp).background(if (isSelected) Color(0xFFB7E4C7) else Color(0xFF2D6A4F), CircleShape))
                }
            }
        }
    }
}

@Composable
fun ProfessionalDashboard(meds: List<MedicationData>, selectedDate: String) {
    val completed = meds.count { it.datesTaken.contains(selectedDate) }
    val total = meds.size
    val progress = if (total > 0) completed.toFloat() / total else 0f

    Card(Modifier.padding(16.dp), shape = RoundedCornerShape(28.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B4332))) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("Daily Progress", color = Color.White.copy(0.7f), fontSize = 14.sp)
                Text("$completed / $total Meds", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            CircularProgressIndicator(progress = progress, color = Color(0xFFB7E4C7), trackColor = Color.White.copy(0.1f))
        }
    }
}

@Composable
fun ProfessionalMedCard(med: MedicationData, isTaken: Boolean, onToggle: () -> Unit, onDelete: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (isTaken) Icons.Default.CheckCircle else Icons.Default.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = if (isTaken) Color(0xFF2D6A4F) else Color.Gray
                )
            }
            Column(Modifier.weight(1f)) {
                Text(med.name, fontWeight = FontWeight.Bold, color = if (isTaken) Color.Gray else Color.Black)
                Text(med.dosage, fontSize = 12.sp, color = Color.Gray)
            }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.3f)) }
        }
    }
}

@Composable
fun ProfessionalApptCard(appt: Appointment, onDismiss: () -> Unit) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.MedicalServices, null, tint = Color(0xFF059669))
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(appt.title, fontWeight = FontWeight.Bold, color = Color(0xFF064E3B))
                Text("${appt.time} • ${appt.type.name}", fontSize = 12.sp, color = Color(0xFF059669))
            }
            IconButton(onClick = onDismiss) { Icon(Icons.Default.Delete, null, tint = Color.Red.copy(0.3f)) }
        }
    }
}

@Composable fun SectionHeader(title: String, count: Int, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, Modifier.size(18.dp), tint = Color.Gray)
        Text(title, Modifier.padding(start = 8.dp).weight(1f), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.Gray)
    }
}

@Composable fun EmptyStateHint(text: String) {
    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
        Text(text, color = Color.LightGray, fontSize = 14.sp)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddMedSheet(onDismiss: () -> Unit, db: FirebaseFirestore, userId: String) {
    var name by remember { mutableStateOf("") }
    var dose by remember { mutableStateOf("") }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).padding(bottom = 32.dp).navigationBarsPadding()) {
            Text("New Medication", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Medicine Name") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = dose, onValueChange = { dose = it }, label = { Text("Dose (e.g. 500mg)") }, modifier = Modifier.fillMaxWidth())
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val ref = db.collection("medications").document()
                        val newMed = MedicationData(id = ref.id, userId = userId, name = name, dosage = dose)
                        ref.set(newMed)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4332))
            ) { Text("Save Medication") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddApptSheet(onDismiss: () -> Unit, db: FirebaseFirestore, userId: String, selectedDate: String) {
    var title by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("10:00 AM") }
    var type by remember { mutableStateOf(RecurrenceType.ONETIME) }
    val context = LocalContext.current

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(24.dp).padding(bottom = 32.dp).navigationBarsPadding()) {
            Text("Schedule Checkup", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Reason / Doctor") }, modifier = Modifier.fillMaxWidth())

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
                Text("Time: $time", Modifier.padding(16.dp))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(RecurrenceType.ONETIME, RecurrenceType.WEEKLY, RecurrenceType.MONTHLY).forEach { rType ->
                    FilterChip(
                        selected = type == rType,
                        onClick = { type = rType },
                        label = { Text(rType.name.lowercase()) }
                    )
                }
            }

            Button(
                onClick = {
                    if (title.isNotBlank()) {
                        val ref = db.collection("appointments").document()
                        val newAppt = Appointment(id = ref.id, userId = userId, title = title, date = selectedDate, time = time, type = type)
                        ref.set(newAppt)
                        onDismiss()
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4332))
            ) { Text("Schedule") }
        }
    }
}

fun isApptVisibleOnDate(appt: Appointment, targetDate: String): Boolean {
    if (appt.date.isBlank() || targetDate.isBlank()) return false
    val sdf = SimpleDateFormat("yyyyMMdd", Locale.getDefault())
    return try {
        val target = sdf.parse(targetDate) ?: return false
        val base = sdf.parse(appt.date) ?: return false
        if (target.before(base) && targetDate != appt.date) return false

        when (appt.type) {
            RecurrenceType.ONETIME, RecurrenceType.ONE_TIME -> appt.date == targetDate
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
    } catch (e: Exception) { false }
}