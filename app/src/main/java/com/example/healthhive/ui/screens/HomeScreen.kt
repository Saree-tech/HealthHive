package com.example.healthhive.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.viewmodel.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateTo: (route: String) -> Unit,
    onSymptomCheckerClick: () -> Unit,
    onRecommendationsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onTipsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val displayName = uiState.user?.userName?.split(" ")?.get(0) ?: "User"

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF0F9F9), Color(0xFFFFFFFF))
    )

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = false, onClick = onSymptomCheckerClick, icon = { Icon(Icons.Default.ChatBubbleOutline, null) }, label = { Text("Lumi AI") })
                NavigationBarItem(selected = false, onClick = onReportsClick, icon = { Icon(Icons.Default.Assignment, null) }, label = { Text("Reports") })
                NavigationBarItem(selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") })
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().background(brush = backgroundGradient).padding(paddingValues)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center), color = Color(0xFF2D6A4F))
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(20.dp)
                ) {
                    item {
                        HomeHeader(displayName, onSettingsClick)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        HealthScoreCard(score = 82f)
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        MainFeatureCard(
                            title = "AI Symptom Checker",
                            subtitle = "Chat with Lumi to understand your symptoms.",
                            icon = Icons.Default.AutoAwesome,
                            onClick = onSymptomCheckerClick
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    // --- UPDATED: MEDICAL SERVICES GRID ---
                    item {
                        SectionHeader(title = "Medical Services", onAction = {})
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Row(modifier = Modifier.fillMaxWidth()) {
                                SmallActionCard("Reports", Icons.Default.Assignment, Color(0xFFE8F5E9), onReportsClick, Modifier.weight(1f))
                                Spacer(Modifier.width(12.dp))
                                // ADDED: CALENDAR ACCESS HERE
                                SmallActionCard("Calendar", Icons.Default.CalendarMonth, Color(0xFFFFF3E0), { onNavigateTo("calendar") }, Modifier.weight(1f))
                            }
                            Row(modifier = Modifier.fillMaxWidth()) {
                                SmallActionCard("Plans", Icons.Default.AutoGraph, Color(0xFFE3F2FD), onRecommendationsClick, Modifier.weight(1f))
                                Spacer(Modifier.width(12.dp))
                                Box(modifier = Modifier.weight(1f)) // Empty box to keep grid alignment
                            }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        SectionHeader(title = "Live Vitals", onAction = {})
                        LazyRow(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            item { VitalCard("Steps", "8,432", Icons.Default.DirectionsRun, Color(0xFFE8F5E9)) }
                            item { VitalCard("Heart", "74 bpm", Icons.Default.Favorite, Color(0xFFFCE4EC)) }
                            item { VitalCard("Sleep", "7h 20m", Icons.Default.Bedtime, Color(0xFFE8EAF6)) }
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                    }

                    item {
                        SectionHeader(title = "Health Insights", onAction = onTipsClick)
                        NewsCard("Optimizing Deep Sleep", "Restore your body with these 5 tips.", Color(0xFF2D6A4F))
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}

// --- HELPER COMPONENTS REMAIN THE SAME ---

@Composable
fun SmallActionCard(title: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(80.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            Modifier.fillMaxSize().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Surface(Modifier.size(40.dp), shape = CircleShape, color = color) {
                Icon(icon, null, Modifier.padding(8.dp), tint = Color.DarkGray)
            }
            Spacer(Modifier.width(12.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun HomeHeader(name: String, onProfileClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Welcome back,", style = MaterialTheme.typography.bodyLarge, color = Color.Gray)
            Text(name, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color(0xFF1B4332))
        }
        Surface(
            Modifier.size(48.dp).clickable { onProfileClick() },
            shape = CircleShape,
            color = Color(0xFFDFF2F2)
        ) {
            Icon(Icons.Default.Person, null, Modifier.padding(10.dp), tint = Color(0xFF2D6A4F))
        }
    }
}

@Composable
fun HealthScoreCard(score: Float) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(80.dp)) {
                Canvas(modifier = Modifier.size(80.dp)) {
                    drawArc(Color(0xFFF0F0F0), 0f, 360f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(Color(0xFF2D6A4F), -90f, (score / 100) * 360f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                }
                Text("${score.toInt()}%", fontWeight = FontWeight.Bold, color = Color(0xFF1B4332))
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text("Health Score", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("You're doing better than 80% of users!", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun MainFeatureCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF2D6A4F))
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Icon(icon, null, tint = Color(0xFFB7E4C7), modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(8.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(subtitle, color = Color.White.copy(0.7f), fontSize = 14.sp)
            }
            Icon(Icons.Default.ArrowForwardIos, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
fun VitalCard(label: String, value: String, icon: ImageVector, color: Color) {
    Card(
        Modifier.width(130.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(0.4f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Icon(icon, null, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(8.dp))
            Text(value, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            Text(label, fontSize = 12.sp)
        }
    }
}

@Composable
fun NewsCard(title: String, desc: String, accentColor: Color) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(1.dp)
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(60.dp).background(accentColor.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lightbulb, null, tint = accentColor)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold)
                Text(desc, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1B4332))
        Text("See all", color = Color(0xFF2D6A4F), fontSize = 14.sp, modifier = Modifier.clickable { onAction() })
    }
}