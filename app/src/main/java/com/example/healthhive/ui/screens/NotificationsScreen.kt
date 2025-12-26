package com.example.healthhive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(onBackClick: () -> Unit) {
    val lamiGreen = Color(0xFF2D6A4F)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notifications", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF8FAF9)),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 6.3: Hydrate Tips Card from Proposal
            item {
                NotificationHighlightCard(
                    title = "7 Hydrate Tips to Boost Your Energy",
                    description = "Hydrate regularly to maintain body functions and nourishment. Drinking water early in the morning boosts metabolism.",
                    author = "Rick C.K.",
                    color = lamiGreen
                )
            }

            item {
                Text(
                    "Recent Alerts",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(sampleNotifications) { notification ->
                NotificationItem(notification)
            }
        }
    }
}

@Composable
fun NotificationHighlightCard(title: String, description: String, author: String, color: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = color)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Icon(Icons.Default.WaterDrop, contentDescription = null, tint = Color.White)
            Spacer(Modifier.height(16.dp))
            Text(title, color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(description, color = Color.White.copy(alpha = 0.8f), fontSize = 14.sp)
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("- $author -", color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                Button(
                    onClick = { /* Read More Action */ },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = color),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Read More")
                }
            }
        }
    }
}

@Composable
fun NotificationItem(notification: NotificationData) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 1.dp
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(40.dp).background(Color(0xFFE8F5E9), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.NotificationsActive, null, tint = Color(0xFF2D6A4F), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(notification.title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(notification.time, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

data class NotificationData(val title: String, val time: String)
val sampleNotifications = listOf(
    NotificationData("Time to log your heart rate", "Just now"),
    NotificationData("You reached your 8,000 steps goal!", "2 hours ago"),
    NotificationData("Check your sleep analysis for last night", "5 hours ago")
)