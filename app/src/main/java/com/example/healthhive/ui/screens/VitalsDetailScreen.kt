package com.example.healthhive.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VitalsDetailScreen(
    vitalType: String,
    onBack: () -> Unit,
    viewModel: VitalsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var inputValue by remember { mutableStateOf("") }

    LaunchedEffect(vitalType) {
        viewModel.startListening(vitalType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if(vitalType == "All") "Overall Health" else vitalType, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. THE LIVE GRAPH CARD
            Card(
                modifier = Modifier.fillMaxWidth().height(250.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(4.dp)
            ) {
                if (uiState.isLoading) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2D6A4F))
                    }
                } else if (uiState.history.isEmpty()) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data available", color = Color.Gray)
                    }
                } else {
                    // This function is now defined below!
                    VitalsChart(uiState.history)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. LUMI PROFESSIONAL VERDICT
            Text(
                "Lumi's Analysis",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B4332)
            )

            Card(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9F4)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = uiState.aiRecommendation,
                    modifier = Modifier.padding(16.dp),
                    fontSize = 15.sp,
                    color = Color(0xFF2D6A4F),
                    lineHeight = 22.sp
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            // 3. INPUT SECTION
            if (vitalType != "All") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it },
                        label = { Text("Enter Value") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Button(
                        onClick = {
                            if (inputValue.isNotBlank()) {
                                viewModel.saveNewEntry(vitalType, inputValue)
                                inputValue = ""
                            }
                        },
                        modifier = Modifier.height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B4332))
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

/**
 * A Custom Line Chart using Jetpack Compose Canvas
 */
@Composable
fun VitalsChart(history: List<VitalHistoryEntry>) {
    val dataPoints = history.map { it.value }
    if (dataPoints.isEmpty()) return

    val maxVal = dataPoints.maxOrNull() ?: 1f
    val minVal = dataPoints.minOrNull() ?: 0f
    val range = if (maxVal - minVal == 0f) 1f else maxVal - minVal

    Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val spacing = width / (dataPoints.size - 1).coerceAtLeast(1)

            val path = Path()
            dataPoints.forEachIndexed { index, value ->
                // Normalize value to fit within height (inverted because 0 is top)
                val x = index * spacing
                val y = height - ((value - minVal) / range * height)

                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)

                // Draw data point circles
                drawCircle(
                    color = Color(0xFF2D6A4F),
                    radius = 8f,
                    center = Offset(x, y)
                )
            }

            // Draw the connecting line
            drawPath(
                path = path,
                color = Color(0xFF409167),
                style = Stroke(width = 5f)
            )
        }

        // Show the latest value as a label
        Text(
            text = "Latest: ${dataPoints.last()}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}