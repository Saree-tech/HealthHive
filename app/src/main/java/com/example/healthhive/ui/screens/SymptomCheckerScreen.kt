package com.example.healthhive.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.R
import com.example.healthhive.ui.theme.*
import com.example.healthhive.viewmodel.ChatMessage
import com.example.healthhive.viewmodel.SymptomCheckerViewModel
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
// IMPORTANT: Ensure the following lines are NOT present:
// import com.google.accompanist.insets.LocalWindowInsets
// import com.google.accompanist.insets.imePadding // This is why 'imePadding' failed earlier
// import com.google.accompanist.insets.navigationBarsWithImePadding // This is the current error
// >>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>>
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomCheckerScreen(
    viewModel: SymptomCheckerViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val assistantName = viewModel.getAssistantName()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // 1. Detect Keyboard Visibility (FIXED: Using native Compose Insets API)
    // The error 'Function invocation 'bottom()' expected' is solved by using getBottom(density)
    val isKeyboardVisible = WindowInsets.ime.getBottom(LocalDensity.current) > 0

    // Scroll to the latest message whenever chat history updates
    LaunchedEffect(uiState.chatHistory.size) {
        if (uiState.chatHistory.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(uiState.chatHistory.lastIndex)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Chat with $assistantName") },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = PrimaryBlue)
            )
        },
        bottomBar = {
            ChatInput(
                messageInput = uiState.messageInput,
                onInputChanged = viewModel::updateMessageInput,
                onSend = { viewModel.sendMessage(uiState.messageInput) },
                isLoading = uiState.isLoading
            )
        },
        modifier = Modifier.fillMaxSize()
            // FIXED: Using native WindowInsets padding (resolves navigationBarsWithImePadding error)
            .windowInsetsPadding(WindowInsets.safeDrawing)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                // FIXED: Using native imePadding
                .imePadding()
        ) {
            // 2. Logo/Intro Screen that disappears when the keyboard appears
            AnimatedVisibility(
                visible = !isKeyboardVisible && uiState.chatHistory.size <= 1,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                IntroScreen(assistantName)
            }

            // 3. Chat Log
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                items(uiState.chatHistory) { message ->
                    ChatBubble(message = message, assistantName = assistantName)
                }

                // Add loading indicator as the last item
                if (uiState.isLoading) {
                    item { LoadingBubble() }
                }
            }

            // 4. Reset Button (Optional)
            if (uiState.isAnalysisComplete) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    OutlinedButton(
                        onClick = viewModel::resetAnalysis,
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Start New Symptom Check")
                    }
                }
            }
        }
    }
}

@Composable
fun IntroScreen(assistantName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 250.dp, max = 300.dp)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // NOTE: Check your R.drawable resource name!
        Image(
            painter = painterResource(id = R.drawable.assistant_logo),
            contentDescription = "Assistant Logo",
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Hey! I'm $assistantName, your AI Health Assistant",
            style = MaterialTheme.typography.titleLarge,
            color = PrimaryBlue,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Ready to help you, right here.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

@Composable
fun ChatBubble(message: ChatMessage, assistantName: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        if (!message.isUser) {
            // Assistant Icon (Lumi)
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.LightGray.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(assistantName.first().toString(), fontSize = 16.sp, color = DarkTeal)
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        val backgroundColor = if (message.isUser) PrimaryBlue else LightTeal
        val contentColor = if (message.isUser) Color.White else DarkTeal

        // Chat Bubble Content
        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (message.isUser) 16.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = backgroundColor),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                text = message.text,
                color = contentColor,
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun LoadingBubble() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.Start,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Assistant Icon placeholder
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color.LightGray.copy(alpha = 0.5f)),
            contentAlignment = Alignment.Center
        ) {
            // ... (Assistant initial/icon)
        }
        Spacer(modifier = Modifier.width(8.dp))

        Card(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = 4.dp,
                bottomEnd = 16.dp
            ),
            colors = CardDefaults.cardColors(containerColor = LightTeal),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
        ) {
            Text(
                text = "Lumi is thinking...",
                color = DarkTeal.copy(alpha = 0.6f),
                modifier = Modifier.padding(10.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun ChatInput(
    messageInput: String,
    onInputChanged: (String) -> Unit,
    onSend: () -> Unit,
    isLoading: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = messageInput,
            onValueChange = onInputChanged,
            label = { Text("Enter symptoms or question...") },
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(24.dp),
            maxLines = 4,
            enabled = !isLoading
        )

        Spacer(modifier = Modifier.width(8.dp))

        Button(
            onClick = onSend,
            enabled = messageInput.isNotBlank() && !isLoading,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier.height(56.dp)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.Send,
                contentDescription = "Send Message"
            )
        }
    }
}