@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthhive.ui.screens

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.viewmodel.ProfileViewModel
import com.example.healthhive.viewmodel.VitalsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun ReportGeneratorScreen(
    onBackClick: () -> Unit,
    vitalsViewModel: VitalsViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel()
) {
    val vitalsState by vitalsViewModel.uiState.collectAsState()
    val profileState by profileViewModel.uiState.collectAsState()
    val context = LocalContext.current

    var isGenerating by remember { mutableStateOf(false) }
    var reportReady by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val userName = profileState.user?.userName ?: "User"
    val currentDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date())
    val reportId = "HH-${System.currentTimeMillis().toString().takeLast(6)}"

    val latestHeartRate = vitalsState.history.findLast { it.type == "Heart Rate" }?.value?.toInt()?.toString() ?: "--"
    val latestSteps = vitalsState.history.findLast { it.type == "Steps" }?.value?.toInt()?.toString() ?: "--"
    val latestSleep = vitalsState.history.findLast { it.type == "Sleep" }?.value?.toString() ?: "--"
    val aiRecommendation = vitalsState.aiRecommendation.ifBlank { "No specific anomalies detected in this period." }

    // PDF Save Launcher
    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            generatePdfFile(
                context = context,
                uri = it,
                userName = userName,
                reportId = reportId,
                date = currentDate,
                heartRate = latestHeartRate,
                steps = latestSteps,
                sleep = latestSleep,
                aiInsight = aiRecommendation
            )
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Health Report Engine", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = Color(0xFFF8FAF9)
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (!reportReady && !isGenerating) {
                    item {
                        ProfessionalInitialState(onGenerate = {
                            scope.launch {
                                isGenerating = true
                                delay(3000)
                                isGenerating = false
                                reportReady = true
                            }
                        })
                    }
                }

                if (isGenerating) {
                    item {
                        AnalysisLoader()
                    }
                }

                if (reportReady) {
                    item {
                        MedicalReportPreview(
                            userName = userName,
                            date = currentDate,
                            reportId = reportId,
                            heartRate = latestHeartRate,
                            steps = latestSteps,
                            sleep = latestSleep,
                            aiRecommendation = aiRecommendation,
                            onExport = {
                                savePdfLauncher.launch("HealthHive_Report_${reportId}.pdf")
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- PDF GENERATION LOGIC ---

fun generatePdfFile(
    context: Context,
    uri: Uri,
    userName: String,
    reportId: String,
    date: String,
    heartRate: String,
    steps: String,
    sleep: String,
    aiInsight: String
) {
    val pdfDocument = PdfDocument()
    val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 Size
    val page = pdfDocument.startPage(pageInfo)
    val canvas: Canvas = page.canvas
    val paint = Paint()
    val titlePaint = Paint().apply {
        isFakeBoldText = true
        textSize = 20f
        color = android.graphics.Color.BLACK
    }
    val bodyPaint = Paint().apply {
        textSize = 14f
        color = android.graphics.Color.DKGRAY
    }

    // Drawing the Report
    canvas.drawText("HealthHive Official Medical Report", 40f, 50f, titlePaint)
    canvas.drawText("Patient Name: $userName", 40f, 100f, bodyPaint)
    canvas.drawText("Report ID: $reportId", 40f, 120f, bodyPaint)
    canvas.drawText("Date: $date", 40f, 140f, bodyPaint)

    canvas.drawLine(40f, 160f, 555f, 160f, paint)

    canvas.drawText("BIOMETRIC SUMMARY:", 40f, 200f, titlePaint.apply { textSize = 16f })
    canvas.drawText("- Heart Rate: $heartRate BPM", 50f, 230f, bodyPaint)
    canvas.drawText("- Average Sleep: $sleep hrs", 50f, 255f, bodyPaint)
    canvas.drawText("- Daily Steps: $steps", 50f, 280f, bodyPaint)

    canvas.drawText("LUMI AI CLINICAL INSIGHT:", 40f, 340f, titlePaint)

    // Simple text wrapping for AI insight
    val margin = 40f
    val maxWidth = 500f
    var yPos = 370f
    val words = aiInsight.split(" ")
    var line = ""
    for (word in words) {
        if (paint.measureText("$line $word") < maxWidth) {
            line += "$word "
        } else {
            canvas.drawText(line, margin, yPos, bodyPaint)
            yPos += 20f
            line = "$word "
        }
    }
    canvas.drawText(line, margin, yPos, bodyPaint)

    pdfDocument.finishPage(page)

    try {
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            pdfDocument.writeTo(outputStream)
            Toast.makeText(context, "Report Saved Successfully", Toast.LENGTH_SHORT).show()
        }
    } catch (e: Exception) {
        e.printStackTrace()
        Toast.makeText(context, "Failed to save PDF", Toast.LENGTH_SHORT).show()
    } finally {
        pdfDocument.close()
    }
}

// --- UI COMPONENTS (Initial State, Loader, Preview, etc.) ---
// [Keep the rest of the UI components from the previous code here]

@Composable
fun ProfessionalInitialState(onGenerate: () -> Unit) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(modifier = Modifier.height(40.dp))
        Box(
            modifier = Modifier.size(100.dp).background(Color(0xFFE8F5E9), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Analytics, null, modifier = Modifier.size(50.dp), tint = Color(0xFF2D6A4F))
        }
        Spacer(modifier = Modifier.height(32.dp))
        Text("Clinical Data Summary", fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B4332))
        Text(
            "Generate a professional health summary by aggregating your biometric data and Lumi AI insights.",
            textAlign = TextAlign.Center, color = Color.Gray, modifier = Modifier.padding(16.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        ReportFeatureRow(Icons.Rounded.Verified, "Medical-grade Data Formatting")
        ReportFeatureRow(Icons.Rounded.AutoAwesome, "Lumi AI Executive Summary")
        Spacer(modifier = Modifier.height(48.dp))
        Button(
            onClick = onGenerate,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
        ) {
            Text("Compile & Generate Report", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun AnalysisLoader() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 100.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = Color(0xFF2D6A4F), strokeWidth = 4.dp)
        Spacer(Modifier.height(32.dp))
        Text("Aggregating Biometrics...", fontWeight = FontWeight.Bold)
        Column(Modifier.padding(top = 40.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            LoadingStep("Syncing Vital History...", true)
            LoadingStep("Scanning for anomalies...", true)
            LoadingStep("Formatting clinical PDF...", false)
        }
    }
}

@Composable
fun MedicalReportPreview(
    userName: String,
    date: String,
    reportId: String,
    heartRate: String,
    steps: String,
    sleep: String,
    aiRecommendation: String,
    onExport: () -> Unit
) {
    Column {
        Text("Report Preview", fontWeight = FontWeight.Black, fontSize = 18.sp, modifier = Modifier.padding(bottom = 16.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(Color.White),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE0E0E0))
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Column {
                        Text(userName, fontWeight = FontWeight.Bold, color = Color(0xFF2D6A4F))
                        Text("Ref: $reportId", fontSize = 10.sp, color = Color.Gray)
                    }
                    Text(date, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(Modifier.padding(vertical = 16.dp))
                Text("VITAL SIGNS SUMMARY", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    ReportStatVertical("Heart Rate", "$heartRate BPM")
                    ReportStatVertical("Sleep", "$sleep hrs")
                    ReportStatVertical("Steps", steps)
                }
                Spacer(Modifier.height(24.dp))
                Text("AI CLINICAL INSIGHT", fontSize = 11.sp, fontWeight = FontWeight.Black, color = Color.Gray)
                Spacer(Modifier.height(8.dp))
                Box(modifier = Modifier.fillMaxWidth().background(Color(0xFFF1F8F5), RoundedCornerShape(8.dp)).padding(12.dp)) {
                    Text(aiRecommendation, fontSize = 13.sp, color = Color(0xFF1B4332))
                }
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onExport,
            modifier = Modifier.fillMaxWidth().height(60.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4332))
        ) {
            Icon(Icons.Rounded.PictureAsPdf, null)
            Spacer(Modifier.width(12.dp))
            Text("Export Official PDF", fontWeight = FontWeight.ExtraBold)
        }
    }
}

@Composable
fun ReportFeatureRow(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
        Icon(icon, null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, fontSize = 14.sp, color = Color.DarkGray)
    }
}

@Composable
fun LoadingStep(label: String, isDone: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = if (isDone) Icons.Rounded.CheckCircle else Icons.Rounded.Circle,
            contentDescription = null,
            tint = if (isDone) Color(0xFF2D6A4F) else Color.LightGray,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(12.dp))
        Text(label, fontSize = 14.sp, color = if (isDone) Color.Black else Color.Gray)
    }
}

@Composable
fun ReportStatVertical(label: String, value: String) {
    Column {
        Text(label, fontSize = 11.sp, color = Color.Gray)
        Text(value, fontWeight = FontWeight.Bold, fontSize = 15.sp)
    }
}