package com.example.healthhive.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.viewmodel.ChatMessage
import com.example.healthhive.viewmodel.SymptomCheckerViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SymptomCheckerScreen(
    viewModel: SymptomCheckerViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    val lamiGreen = Color(0xFF2D6A4F)
    val chatBg = Color(0xFFF8FAF9)

    LaunchedEffect(uiState.chatHistory.size) {
        if (uiState.chatHistory.isNotEmpty()) {
            listState.animateScrollToItem(uiState.chatHistory.lastIndex)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp)
                // Removed direct containerColor, ModalDrawerSheet uses defaults or specific surface colors
            ) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Chat History",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                NavigationDrawerItem(
                    label = { Text("Start New Chat", fontWeight = FontWeight.Bold) },
                    selected = false,
                    onClick = {
                        viewModel.resetAnalysis()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(Icons.Default.Add, null) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        unselectedContainerColor = Color(0xFFE7F5EC),
                        unselectedIconColor = lamiGreen,
                        unselectedTextColor = lamiGreen
                    )
                )

                HorizontalDivider(Modifier.padding(vertical = 8.dp))

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    // Mock history - this will later be powered by your database/ViewModel
                    items(listOf("Symptom Check - Today", "Headache Analysis - Dec 21")) { title ->
                        NavigationDrawerItem(
                            label = { Text(title) },
                            selected = false,
                            onClick = { scope.launch { drawerState.close() } },
                            icon = { Icon(Icons.AutoMirrored.Filled.Notes, null, tint = Color.Gray) },
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("Lumi AI", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                            Text("Online", style = MaterialTheme.typography.labelSmall.copy(color = lamiGreen))
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(Icons.Default.Menu, contentDescription = "History")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.resetAnalysis() }) {
                            Icon(Icons.Default.Add, contentDescription = "New Chat", tint = lamiGreen)
                        }
                    },
                    // FIXED: Properly applying colors through the colors parameter
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = Color.White,
                        scrolledContainerColor = Color.White,
                        navigationIconContentColor = Color.Black,
                        titleContentColor = Color.Black,
                        actionIconContentColor = lamiGreen
                    )
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(chatBg)
                    .padding(paddingValues)
                    .imePadding()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.chatHistory.isEmpty()) {
                        LumiEmptyState()
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.chatHistory) { message ->
                            ModernChatBubble(message)
                        }
                        if (uiState.isLoading) {
                            item { TypingIndicator() }
                        }
                    }
                }

                // Floating Input Bar
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    shape = RoundedCornerShape(28.dp),
                    color = Color.White,
                    shadowElevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextField(
                            value = uiState.messageInput,
                            onValueChange = viewModel::updateMessageInput,
                            placeholder = { Text("Ask Lumi anything...") },
                            modifier = Modifier.weight(1f),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            maxLines = 4
                        )

                        IconButton(
                            onClick = { viewModel.sendMessage(uiState.messageInput) },
                            enabled = uiState.messageInput.isNotBlank() && !uiState.isLoading,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(if (uiState.messageInput.isNotBlank()) lamiGreen else Color.LightGray)
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Send",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModernChatBubble(message: ChatMessage) {
    val isUser = message.isUser
    val alignment = if (isUser) Alignment.CenterEnd else Alignment.CenterStart
    val bubbleColor = if (isUser) Color(0xFF2D6A4F) else Color.White
    val textColor = if (isUser) Color.White else Color(0xFF1B4332)

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = alignment) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = bubbleColor,
            shadowElevation = if (isUser) 2.dp else 1.dp
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(14.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp)
            )
        }
    }
}

@Composable
fun LumiEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = Color(0xFFE7F5EC)
        ) {
            Icon(
                Icons.Default.History,
                contentDescription = null,
                modifier = Modifier.padding(20.dp),
                tint = Color(0xFF2D6A4F)
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "How can I help you today?",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        Text(
            "Check symptoms, health trends, or medical info.",
            color = Color.Gray,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun TypingIndicator() {
    Row(
        modifier = Modifier.padding(8.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        repeat(3) {
            Surface(
                modifier = Modifier.size(8.dp),
                shape = CircleShape,
                color = Color(0xFF2D6A4F).copy(alpha = 0.4f)
            ) {}
        }
    }
}