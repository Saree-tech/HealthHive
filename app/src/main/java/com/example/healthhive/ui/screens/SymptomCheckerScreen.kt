@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthhive.ui.screens

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.R
import com.example.healthhive.viewmodel.ChatSession
import com.example.healthhive.viewmodel.ChatMessage
import com.example.healthhive.viewmodel.SymptomCheckerViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

/**
 * Observes network connectivity status.
 */
fun observeConnectivity(context: Context) = callbackFlow {
    val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) { trySend(true) }
        override fun onLost(network: Network) { trySend(false) }
    }
    val request = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .build()
    connectivityManager.registerNetworkCallback(request, callback)

    val activeNetwork = connectivityManager.activeNetwork
    val capabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
    trySend(capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true)

    awaitClose { connectivityManager.unregisterNetworkCallback(callback) }
}

@Composable
fun SymptomCheckerScreen(
    viewModel: SymptomCheckerViewModel = viewModel(),
    onBackClick: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = LocalContext.current

    val isOnline by observeConnectivity(context).collectAsState(initial = true)

    val lamiGreen = Color(0xFF2D6A4F)
    val chatBg = Color(0xFFF8FAF9)

    val db = FirebaseFirestore.getInstance()
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
    var chatSessions by remember { mutableStateOf<List<ChatSession>>(emptyList()) }
    var sessionToDelete by remember { mutableStateOf<ChatSession?>(null) }

    LaunchedEffect(userId) {
        if (userId.isEmpty()) return@LaunchedEffect
        db.collection("users").document(userId).collection("sessions")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, _ ->
                chatSessions = snap?.toObjects(ChatSession::class.java) ?: emptyList()
            }
    }

    // FIX: Force scroll whenever list size changes OR loading starts
    LaunchedEffect(uiState.chatHistory.size, uiState.isLoading) {
        if (uiState.chatHistory.isNotEmpty() || uiState.isLoading) {
            val totalItems = listState.layoutInfo.totalItemsCount
            if (totalItems > 0) {
                listState.animateScrollToItem(totalItems - 1)
            }
        }
    }

    if (sessionToDelete != null) {
        AlertDialog(
            onDismissRequest = { sessionToDelete = null },
            title = { Text("Delete Chat History?") },
            text = { Text("This will permanently delete '${sessionToDelete?.title}' from your records.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.deleteSession(sessionToDelete!!.id)
                    sessionToDelete = null
                }) { Text("Delete", color = Color.Red, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { sessionToDelete = null }) { Text("Cancel") }
            }
        )
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet(modifier = Modifier.width(310.dp)) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Chat History",
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )

                NavigationDrawerItem(
                    label = { Text("Start New Chat", fontWeight = FontWeight.Bold) },
                    selected = uiState.activeSessionId == null,
                    onClick = {
                        viewModel.resetAnalysis()
                        scope.launch { drawerState.close() }
                    },
                    icon = { Icon(imageVector = Icons.Default.Add, contentDescription = null) },
                    modifier = Modifier.padding(horizontal = 12.dp),
                    colors = NavigationDrawerItemDefaults.colors(
                        selectedContainerColor = Color(0xFFE7F5EC),
                        selectedIconColor = lamiGreen,
                        selectedTextColor = lamiGreen
                    )
                )

                HorizontalDivider(Modifier.padding(vertical = 8.dp), thickness = 0.5.dp)

                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(chatSessions) { session ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            NavigationDrawerItem(
                                label = { Text(session.title, maxLines = 1) },
                                selected = uiState.activeSessionId == session.id,
                                onClick = {
                                    viewModel.loadSession(session.id)
                                    scope.launch { drawerState.close() }
                                },
                                icon = { Icon(imageVector = Icons.AutoMirrored.Filled.Notes, contentDescription = null) },
                                modifier = Modifier.weight(1f),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = Color(0xFFE7F5EC),
                                    selectedIconColor = lamiGreen,
                                    selectedTextColor = lamiGreen
                                )
                            )
                            IconButton(onClick = { sessionToDelete = session }) {
                                Icon(
                                    imageVector = Icons.Default.DeleteOutline,
                                    contentDescription = "Delete",
                                    tint = Color.Gray,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    ) {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Image(
                                painter = painterResource(id = R.drawable.assistant_logo),
                                contentDescription = "Lumi Logo",
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(lamiGreen.copy(alpha = 0.1f)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(horizontalAlignment = Alignment.Start) {
                                Text("Lumi AI", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
                                Text(
                                    text = if (isOnline) "Online" else "Offline",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        color = if (isOnline) lamiGreen else Color.Red
                                    )
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(imageVector = Icons.Default.Menu, contentDescription = "Menu")
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.resetAnalysis() }) {
                            Icon(imageVector = Icons.Default.Add, contentDescription = "New Chat", tint = lamiGreen)
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
                )
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier.fillMaxSize().background(chatBg).padding(paddingValues).imePadding()
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (uiState.chatHistory.isEmpty() && !uiState.isLoading) {
                        LumiEmptyState(lamiGreen)
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
                            // Using a key makes it easier for LazyColumn to track this item
                            item(key = "typing_indicator") {
                                TypingIndicator(lamiGreen)
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
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
                            placeholder = { Text("Message Lumi...") },
                            modifier = Modifier.weight(1f),
                            enabled = isOnline,
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                                disabledContainerColor = Color.Transparent
                            )
                        )

                        IconButton(
                            onClick = { viewModel.sendMessage(uiState.messageInput) },
                            enabled = uiState.messageInput.isNotBlank() && !uiState.isLoading && isOnline,
                            modifier = Modifier.clip(CircleShape)
                                .background(if (uiState.messageInput.isNotBlank() && isOnline) lamiGreen else Color.LightGray)
                        ) {
                            Icon(
                                imageVector = if (isOnline) Icons.AutoMirrored.Filled.Send else Icons.Default.WifiOff,
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
                topStart = 20.dp, topEnd = 20.dp,
                bottomStart = if (isUser) 20.dp else 4.dp,
                bottomEnd = if (isUser) 4.dp else 20.dp
            ),
            color = bubbleColor,
            shadowElevation = 1.dp
        ) {
            Text(
                text = if (isUser) AnnotatedString(message.text) else parseMarkdown(message.text),
                modifier = Modifier.padding(14.dp),
                color = textColor,
                style = MaterialTheme.typography.bodyLarge.copy(lineHeight = 22.sp)
            )
        }
    }
}

fun parseMarkdown(text: String): AnnotatedString {
    return buildAnnotatedString {
        val cleanText = text.replace(Regex("(?m)^#+\\s*"), "• ")
        val boldRegex = Regex("\\*\\*(.*?)\\*\\*")
        var lastIndex = 0

        boldRegex.findAll(cleanText).forEach { match ->
            append(cleanText.substring(lastIndex, match.range.first))
            pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
            append(match.groupValues[1])
            pop()
            lastIndex = match.range.last + 1
        }
        append(cleanText.substring(lastIndex))
    }
}

@Composable
fun LumiEmptyState(brandColor: Color) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = brandColor.copy(alpha = 0.05f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.assistant_logo),
                contentDescription = "Lumi Logo Large",
                modifier = Modifier.padding(20.dp).clip(CircleShape),
                contentScale = ContentScale.Fit
            )
        }
        Spacer(Modifier.height(24.dp))
        Text("I'm Lumi, your health assistant", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1B4332))
        Text("How can I help you today?", color = Color.Gray, fontSize = 15.sp)
    }
}

@Composable
fun TypingIndicator(color: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "typing")

    Row(
        modifier = Modifier
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp)
            .background(Color.White, RoundedCornerShape(20.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(3) { index ->
            val delay = index * 200
            val yOffset by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = -8f,
                animationSpec = infiniteRepeatable(
                    animation = tween(450, delayMillis = delay, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "dotBounce"
            )

            Box(
                modifier = Modifier
                    .size(8.dp)
                    .offset(y = yOffset.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.6f))
            )
        }
    }
}