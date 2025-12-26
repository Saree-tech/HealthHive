@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthhive.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.viewmodel.VitalsViewModel
import com.example.healthhive.viewmodel.VitalHistoryEntry

@Composable
fun VitalsDetailScreen(
    vitalType: String,
    onBack: () -> Unit,
    viewModel: VitalsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputValue by remember { mutableStateOf("") }
    val isSummaryMode = vitalType == "All"

    LaunchedEffect(vitalType) {
        viewModel.startListening(vitalType)
    }

    Scaffold(
        containerColor = Color(0xFFF8FAF9),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(if (isSummaryMode) "Health Dashboard" else vitalType, fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, tint = Color(0xFF1B4332))
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.padding(padding).fillMaxSize().padding(horizontal = 20.dp).verticalScroll(rememberScrollState())
        ) {
            if (isSummaryMode) {
                SummaryDashboard(uiState.latestVitals)
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = { viewModel.triggerManualAnalysis("All") },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                    enabled = !uiState.isLumiThinking
                ) {
                    Icon(Icons.Default.AutoAwesome, null)
                    Spacer(Modifier.width(8.dp))
                    Text("Generate Lumi Report", fontWeight = FontWeight.Bold)
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth().height(260.dp),
                    shape = RoundedCornerShape(32.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Box(Modifier.padding(20.dp).fillMaxSize()) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(Modifier.align(Alignment.Center), color = Color(0xFF2D6A4F))
                        } else if (uiState.history.isEmpty()) {
                            Text("No history recorded", Modifier.align(Alignment.Center), color = Color.Gray)
                        } else {
                            VitalsChart(uiState.history)
                        }
                    }
                }
            }

            if (uiState.isLumiThinking || uiState.aiRecommendation.isNotEmpty()) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Lumi's Analysis", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B4332))
                Card(
                    modifier = Modifier.padding(vertical = 12.dp).fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(2.dp)
                ) {
                    Column(Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(shape = CircleShape, color = Color(0xFFE8F5E9), modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.AutoAwesome, null, Modifier.padding(6.dp), tint = Color(0xFF2D6A4F))
                            }
                            Spacer(Modifier.width(12.dp))
                            if (uiState.isLumiThinking) {
                                LinearProgressIndicator(Modifier.weight(1f), color = Color(0xFF2D6A4F))
                            } else {
                                Text("AI Insight", fontWeight = FontWeight.Bold, color = Color(0xFF2D6A4F))
                            }
                        }
                        if (!uiState.isLumiThinking && uiState.aiRecommendation.isNotEmpty()) {
                            Spacer(Modifier.height(12.dp))
                            Text(text = uiState.aiRecommendation, fontSize = 15.sp, color = Color(0xFF1B4332), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            if (!isSummaryMode) {
                Spacer(modifier = Modifier.height(24.dp))
                Text("Log New Data", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B4332))
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = inputValue,
                    onValueChange = { inputValue = it },
                    placeholder = { Text("Enter value...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFF2D6A4F))
                )
                Button(
                    onClick = { if(inputValue.isNotBlank()){ viewModel.saveNewEntry(vitalType, inputValue); inputValue = "" } },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp).height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4332))
                ) {
                    Text("Save Record", fontWeight = FontWeight.Bold)
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun SummaryDashboard(latest: Map<String, Float>) {
    Column {
        Text("Latest Readings", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B4332))
        Spacer(Modifier.height(12.dp))
        val items = latest.toList()
        items.chunked(2).forEach { rowItems ->
            Row(Modifier.fillMaxWidth()) {
                rowItems.forEach { item ->
                    Card(Modifier.weight(1f).padding(4.dp), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                        Column(Modifier.padding(16.dp)) {
                            Icon(Icons.Default.MonitorHeart, null, tint = Color(0xFF409167))
                            Text(item.first, fontSize = 12.sp, color = Color.Gray)
                            Text(item.second.toString(), fontSize = 20.sp, fontWeight = FontWeight.Black)
                        }
                    }
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun VitalsChart(history: List<VitalHistoryEntry>) {
    val points = history.map { it.value }
    if (points.size < 2) return
    val max = points.maxOrNull() ?: 100f
    val min = points.minOrNull() ?: 0f
    val range = (max - min).coerceAtLeast(1f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        val spacing = size.width / (points.size - 1).coerceAtLeast(1)
        val path = Path()
        points.forEachIndexed { i, v ->
            val x = i * spacing
            val y = size.height - ((v - min) / range * size.height)
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
            drawCircle(Color(0xFF2D6A4F), 6f, Offset(x, y))
        }
        drawPath(path, Color(0xFF409167), style = Stroke(5f))
    }
}