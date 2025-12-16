package com.example.healthhive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.viewmodel.SymptomCheckerViewModel
import com.example.healthhive.viewmodel.SymptomCheckerUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomCheckerScreen(
    viewModel: SymptomCheckerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val assistantName = viewModel.getAssistantName()

    // Custom colors for the chat bubbles and UI elements
    val LumiPurple = Color(0xFF9370DB) // A friendly, light purple
    val LumiBackground = Color(0xFFF7F4FF) // Very light purple/off-white background

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat with $assistantName") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = LumiBackground)
            )
        },
        bottomBar = {
            if (uiState.result == null || uiState.isInitialAnalysis) {
                ChatInputArea(uiState = uiState, viewModel = viewModel, LumiPurple = LumiPurple)
            } else {
                // Display reset button after initial analysis is complete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(onClick = viewModel::resetAnalysis) {
                        Text("Start New Symptom Check")
                    }
                }
            }
        },
        containerColor = LumiBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Lumi's persona, similar to the image
            LumiChatHeader(assistantName)

            // Pass the Modifier.weight() from the parent Column's scope to ChatMessages
            ChatMessages(
                uiState = uiState,
                assistantName = assistantName,
                LumiPurple = LumiPurple,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Applies the weight correctly from ColumnScope
            )

            // Display error dialog if needed
            if (uiState.error != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.resetAnalysis() },
                    title = { Text("Error") },
                    text = { Text(uiState.error ?: "An unknown error occurred.") },
                    confirmButton = {
                        Button(onClick = { viewModel.resetAnalysis() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun LumiChatHeader(assistantName: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(top = 16.dp, bottom = 24.dp)
    ) {
        // Placeholder for a stylish AI icon (like the abstract crystal in the image)
        Box(
            modifier = Modifier
                .size(64.dp)
                .background(Color(0xFFE0B0FF).copy(alpha = 0.5f), RoundedCornerShape(20.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("🔮", style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Hey! I'm $assistantName, your AI Health Assistant",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF333366) // Dark purple text
        )
        Text(
            text = "Ready to help you, right here.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

@Composable
fun ChatInputArea(
    uiState: SymptomCheckerUiState,
    viewModel: SymptomCheckerViewModel,
    LumiPurple: Color
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(Color.White, RoundedCornerShape(16.dp))
            .padding(8.dp)
    ) {
        // --- Display Selected Symptoms as Chips ---
        if (uiState.selectedSymptoms.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                items(uiState.selectedSymptoms) { symptom ->
                    InputChip(
                        selected = true,
                        onClick = { viewModel.removeSymptom(symptom) },
                        label = { Text(symptom) },
                        trailingIcon = {
                            IconButton(onClick = { viewModel.removeSymptom(symptom) }) {
                                Icon(Icons.Default.Close, contentDescription = "Remove")
                            }
                        },
                        colors = InputChipDefaults.inputChipColors(
                            containerColor = LumiPurple.copy(alpha = 0.1f),
                            labelColor = LumiPurple
                        )
                    )
                }
            }
        }

        // --- Input Field and Send Button (Combined) ---
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = uiState.symptomInput,
                onValueChange = { viewModel.updateSymptomInput(it) },
                label = { Text("Type AI symptoms here...") },
                modifier = Modifier.weight(1f),
                singleLine = true,
                // Using the parameter-less default colors to bypass the dependency conflict
                colors = OutlinedTextFieldDefaults.colors()
            )

            Spacer(modifier = Modifier.width(8.dp))

            // Send Button: Adds the current input to the selected list and triggers analysis
            FloatingActionButton(
                onClick = {
                    val canAnalyze = uiState.selectedSymptoms.isNotEmpty() || uiState.symptomInput.isNotBlank()

                    if (uiState.symptomInput.isNotBlank()) {
                        viewModel.addSymptom(uiState.symptomInput.trim())
                    }
                    if (canAnalyze && !uiState.isLoading) {
                        viewModel.analyzeSymptoms()
                    }
                },
                modifier = Modifier.size(48.dp),
                containerColor = LumiPurple,
                contentColor = Color.White,
                // The 'enabled' parameter is removed, fixing the compilation error at line 195
            ) {
                // Correct: Composable functions (like CircularProgressIndicator and Icon)
                // must be called directly inside this content lambda block.
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(Icons.Default.Send, contentDescription = "Send Message")
                }
            }
        }
    }
}


@Composable
fun ChatMessages(
    uiState: SymptomCheckerUiState,
    assistantName: String,
    LumiPurple: Color,
    modifier: Modifier = Modifier // Accepts the modifier from the parent
) {
    LazyColumn(
        modifier = modifier.padding(horizontal = 16.dp), // Uses the modifier passed down by the parent
        reverseLayout = true // Start from the bottom
    ) {
        // 1. Lumi's Initial Welcome Message (only shown before first analysis)
        if (uiState.isInitialAnalysis && uiState.result == null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                AIChatBubble(
                    message = "Hello! Please tell me your symptoms, and I'll provide a helpful analysis.",
                    LumiPurple = LumiPurple,
                    assistantName = assistantName
                )
            }
        }

        // 2. Lumi's Analysis Result (after analysis)
        if (uiState.result != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                AIChatBubble(
                    message = uiState.result!!,
                    LumiPurple = LumiPurple,
                    assistantName = assistantName
                )
            }
        }

        // 3. User's Input (Simulated message)
        if (uiState.selectedSymptoms.isNotEmpty() && uiState.result != null) {
            item {
                Spacer(modifier = Modifier.height(16.dp))
                UserChatBubble(
                    message = "Symptoms entered: ${uiState.selectedSymptoms.joinToString(", ")}"
                )
            }
        }
    }
}

@Composable
fun AIChatBubble(message: String, LumiPurple: Color, assistantName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Start
    ) {
        // Assistant Initial (L) or Icon
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(LumiPurple, RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(assistantName.first().toString(), color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(modifier = Modifier.width(8.dp))

        Card(
            shape = RoundedCornerShape(topStart = 0.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 16.dp),
            colors = CardDefaults.cardColors(containerColor = LumiPurple.copy(alpha = 0.2f)),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(12.dp),
                color = Color.Black
            )
        }
    }
}

@Composable
fun UserChatBubble(message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.End
    ) {
        Card(
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomStart = 16.dp, bottomEnd = 0.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)), // Light Green/Blueish for user
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = message,
                modifier = Modifier.padding(12.dp),
                color = Color.Black
            )
        }
    }
}