package com.example.healthhive.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    val isSummaryMode = vitalType == "All"

    LaunchedEffect(vitalType) {
        viewModel.startListening(vitalType)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (isSummaryMode) "Full Health Analysis" else vitalType,
                        fontWeight = FontWeight.Bold
                    )
                },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!isSummaryMode) {
                // 1. THE LIVE GRAPH (Only for specific vitals)
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
                        VitalsChart(uiState.history)
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            } else {
                // 1. SUMMARY HEADER (Only for 'All')
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = Color(0xFF1B4332)
                )
                Text(
                    "Lumi Deep Insights",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4332),
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    "Comprehensive review of your recent biometric trends.",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 24.dp)
                )
            }

            // 2. AI ANALYSIS CARD
            Text(
                if (isSummaryMode) "Health Report" else "Lumi's Analysis",
                modifier = Modifier.fillMaxWidth(),
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1B4332)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F9F4)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Text(
                    text = uiState.aiRecommendation,
                    modifier = Modifier.padding(20.dp),
                    fontSize = 16.sp,
                    color = Color(0xFF2D6A4F),
                    lineHeight = 24.sp
                )
            }

            // 3. RECOMMENDATIONS (Shown in Summary Mode)
            if (isSummaryMode) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Next Steps",
                    modifier = Modifier.fillMaxWidth(),
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B4332)
                )

                val recommendations = listOf(
                    "Maintain consistent logging for higher AI accuracy.",
                    "Review these trends with your physician during check-ups.",
                    "Ensure you are resting 5 minutes before vital captures."
                )

                recommendations.forEach { tip ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        // FIXED: Using standard BorderStroke for mobile
                        border = BorderStroke(1.dp, Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "• $tip",
                            modifier = Modifier.padding(12.dp),
                            fontSize = 14.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 4. INPUT SECTION (Hidden in Summary Mode)
            if (!isSummaryMode) {
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
            val spacing = if (dataPoints.size > 1) width / (dataPoints.size - 1) else 0f

            val path = Path()
            dataPoints.forEachIndexed { index, value ->
                val x = index * spacing
                val y = height - ((value - minVal) / range * height)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                drawCircle(color = Color(0xFF2D6A4F), radius = 8f, center = Offset(x, y))
            }
            drawPath(path = path, color = Color(0xFF409167), style = Stroke(width = 5f))
        }
        Text(
            text = "Latest: ${dataPoints.last()}",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd)
        )
    }
}