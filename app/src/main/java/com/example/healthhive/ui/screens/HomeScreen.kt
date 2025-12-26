package com.example.healthhive.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
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
import com.example.healthhive.viewmodel.VitalsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateTo: (route: String) -> Unit,
    onSymptomCheckerClick: () -> Unit,
    onReportsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onVitalClick: (vitalType: String) -> Unit = {},
    // Injected VitalsViewModel to observe live Firestore data
    vitalsViewModel: VitalsViewModel = viewModel(),
    homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory())
) {
    val homeUiState by homeViewModel.uiState.collectAsState()
    val vitalsUiState by vitalsViewModel.uiState.collectAsState()

    val displayName = homeUiState.user?.userName?.split(" ")?.get(0) ?: "Warrior"

    // Sync live vitals on screen load
    LaunchedEffect(Unit) {
        vitalsViewModel.startListening("All")
    }

    Scaffold(
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 8.dp) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Default.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = false, onClick = onSymptomCheckerClick, icon = { Icon(Icons.Default.AutoAwesome, null) }, label = { Text("Lumi AI") })
                NavigationBarItem(selected = false, onClick = onReportsClick, icon = { Icon(Icons.Default.Assignment, null) }, label = { Text("Reports") })
                NavigationBarItem(selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Default.Person, null) }, label = { Text("Profile") })
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color(0xFFF0F9F4), Color.White)))
                .padding(paddingValues),
            contentPadding = PaddingValues(20.dp)
        ) {
            // 1. DYNAMIC HEADER
            item {
                HomeHeader(displayName, onSettingsClick)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 2. DYNAMIC HEALTH SCORE GAUGE
            item {
                // Converting 0.0-1.0 float to 0-100 percentage
                HealthScoreCard(score = vitalsUiState.healthScore * 100)
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 3. LUMI AI FEATURE CARD (With live recommendation snippet)
            item {
                MainFeatureCard(
                    title = "Ask Lumi AI",
                    subtitle = if(vitalsUiState.aiRecommendation.length > 60)
                        vitalsUiState.aiRecommendation.take(60) + "..."
                    else vitalsUiState.aiRecommendation,
                    icon = Icons.Default.AutoAwesome,
                    onClick = onSymptomCheckerClick
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 4. LIVE VITALS SECTION (Horizontal scroll of latest entries)
            item {
                SectionHeader(title = "Live Vitals", onAction = { onVitalClick("All") })

                if (vitalsUiState.history.isEmpty() && !vitalsUiState.isLoading) {
                    Text("No vitals logged yet.", color = Color.Gray, modifier = Modifier.padding(vertical = 8.dp))
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // We extract the most recent entry for each unique vital type
                        val latestVitals = vitalsUiState.history
                            .groupBy { it.type }
                            .map { it.value.last() }

                        items(latestVitals) { vital ->
                            VitalCard(
                                label = vital.type,
                                value = "${vital.value.toInt()}",
                                icon = when(vital.type) {
                                    "Heart Rate" -> Icons.Default.Favorite
                                    "Blood Pressure" -> Icons.Default.Speed
                                    "Steps" -> Icons.Default.DirectionsRun
                                    "Sleep" -> Icons.Default.Bedtime
                                    else -> Icons.Default.MonitorHeart
                                },
                                color = Color(0xFF2D6A4F),
                                onClick = { onVitalClick(vital.type) }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 5. QUICK SERVICES
            item {
                SectionHeader(title = "Medical Services", onAction = {})
                Row(modifier = Modifier.fillMaxWidth()) {
                    SmallActionCard("Reports", Icons.Default.Assessment, Color(0xFFE8F5E9), onReportsClick, Modifier.weight(1f))
                    Spacer(Modifier.width(12.dp))
                    SmallActionCard("Calendar", Icons.Default.CalendarMonth, Color(0xFFFFF3E0), { onNavigateTo("calendar") }, Modifier.weight(1f))
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            // 6. HEALTH INSIGHT TIP
            item {
                SectionHeader(title = "Daily Tip", onAction = {})
                NewsCard("Stay Hydrated", "Drinking water improves AI vital accuracy.", Color(0xFF2D6A4F))
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

// --- SUB-COMPONENTS WITH UPGRADED UI ---

@Composable
fun HealthScoreCard(score: Float) {
    val animatedScore by animateFloatAsState(
        targetValue = score,
        animationSpec = tween(durationMillis = 1500)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(32.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                Canvas(modifier = Modifier.size(90.dp)) {
                    drawArc(Color(0xFFF1F8F5), 0f, 360f, false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(Color(0xFF2D6A4F), -90f, (animatedScore / 100) * 360f, false, style = Stroke(10.dp.toPx(), cap = StrokeCap.Round))
                }
                Text("${animatedScore.toInt()}%", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF1B4332))
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text("Overall Health", fontWeight = FontWeight.Bold, fontSize = 20.sp, color = Color(0xFF1B4332))
                Text(
                    if (score > 80) "You're in the green zone!" else "Needs more data points.",
                    color = Color.Gray, fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
fun VitalCard(label: String, value: String, icon: ImageVector, color: Color, onClick: () -> Unit) {
    Card(
        modifier = Modifier.width(135.dp).clickable { onClick() },
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF0F0F0))
    ) {
        Column(Modifier.padding(16.dp)) {
            Surface(shape = CircleShape, color = color.copy(alpha = 0.1f), modifier = Modifier.size(40.dp)) {
                Icon(icon, null, Modifier.padding(10.dp), tint = color)
            }
            Spacer(Modifier.height(12.dp))
            Text(value, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, color = Color.Black)
            Text(label, fontSize = 13.sp, color = Color.Gray)
        }
    }
}

@Composable
fun MainFeatureCard(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1B4332))
    ) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Icon(icon, null, tint = Color(0xFFD8F3DC), modifier = Modifier.size(32.dp))
                Spacer(Modifier.height(12.dp))
                Text(title, color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(subtitle, color = Color.White.copy(0.7f), fontSize = 13.sp, lineHeight = 18.sp)
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.White)
        }
    }
}

// Re-using your existing Header/Section logic but with refined colors
@Composable
fun HomeHeader(name: String, onProfileClick: () -> Unit) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Good morning,", fontSize = 14.sp, color = Color.Gray)
            Text(name, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF1B4332))
        }
        Surface(
            Modifier.size(50.dp).clickable { onProfileClick() },
            shape = CircleShape,
            color = Color(0xFFDFF2F2)
        ) {
            Icon(Icons.Default.Person, null, Modifier.padding(12.dp), tint = Color(0xFF2D6A4F))
        }
    }
}

@Composable
fun SectionHeader(title: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
        Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1B4332))
        Text("View History", color = Color(0xFF2D6A4F), fontSize = 13.sp, fontWeight = FontWeight.Bold, modifier = Modifier.clickable { onAction() })
    }
}

@Composable
fun SmallActionCard(title: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.height(90.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF5F5F5))
    ) {
        Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color.DarkGray, modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(4.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
    }
}

@Composable
fun NewsCard(title: String, desc: String, accentColor: Color) {
    Card(
        Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF5F5F5))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(50.dp).background(accentColor.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Lightbulb, null, tint = accentColor)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(desc, fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}