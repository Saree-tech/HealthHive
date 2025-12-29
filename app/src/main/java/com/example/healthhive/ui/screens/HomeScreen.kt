@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthhive.ui.screens

import android.app.Application
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.healthhive.data.HealthArticle
import com.example.healthhive.viewmodel.HomeViewModel
import com.example.healthhive.viewmodel.VitalsViewModel
import com.example.healthhive.viewmodel.ProfileViewModel
import com.example.healthhive.viewmodel.HealthNewsViewModel

@Composable
fun HomeScreen(
    onNavigateTo: (route: String) -> Unit,
    onSymptomCheckerClick: () -> Unit,
    onReportsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onVitalClick: (vitalType: String) -> Unit = {},
    vitalsViewModel: VitalsViewModel = viewModel(),
    profileViewModel: ProfileViewModel = viewModel(),
    newsViewModel: HealthNewsViewModel = viewModel()
) {
    val context = LocalContext.current.applicationContext as Application
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(context))
    val uriHandler = LocalUriHandler.current

    // Reactive state observation
    val homeUiState by homeViewModel.uiState.collectAsState()
    val profileState by profileViewModel.uiState.collectAsState()
    val vitalsUiState by vitalsViewModel.uiState.collectAsState()
    val newsState by newsViewModel.uiState.collectAsState()

    val displayName = profileState.user?.userName?.split(" ")?.firstOrNull() ?: "Warrior"
    val profilePicUrl = profileState.user?.profilePictureUrl ?: ""

    // Initial data fetch
    LaunchedEffect(Unit) {
        vitalsViewModel.startListening("All")
    }

    Scaffold(
        containerColor = Color(0xFFF8FAF9),
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 12.dp) {
                NavigationBarItem(
                    selected = true,
                    onClick = {},
                    icon = { Icon(Icons.Rounded.Home, null) },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onSymptomCheckerClick,
                    icon = { Icon(Icons.Rounded.AutoAwesome, null) },
                    label = { Text("Lumi AI") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onReportsClick,
                    icon = { Icon(Icons.Rounded.Assignment, null) },
                    label = { Text("Reports") }
                )
                NavigationBarItem(
                    selected = false,
                    onClick = onSettingsClick,
                    icon = { Icon(Icons.Rounded.Person, null) },
                    label = { Text("Profile") }
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            // 1. Header
            item {
                EnhancedHeader(
                    name = displayName,
                    profilePicUrl = profilePicUrl,
                    onProfileClick = onSettingsClick
                )
            }

            // 2. Mood Tracker
            item {
                MoodHandlerSection(
                    selectedMood = homeUiState.selectedMood,
                    onMoodSelected = { homeViewModel.updateMood(it) }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }

            // 3. Health Score Card
            item {
                PremiumHealthScoreCard(score = vitalsUiState.healthScore * 100)
            }

            // 4. Lumi AI Feature Card
            item {
                LumiAIActionCard(
                    recommendation = vitalsUiState.aiRecommendation,
                    onClick = onSymptomCheckerClick
                )
            }

            // 5. Vitals Section
            item {
                SectionHeader(title = "Vitals", onAction = { onVitalClick("All") })

                if (vitalsUiState.isLoading && vitalsUiState.latestVitals.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2D6A4F), strokeWidth = 2.dp)
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        val categories = listOf("Heart Rate", "Blood Pressure", "Steps", "Sleep")
                        items(categories) { type ->
                            val value = vitalsUiState.latestVitals[type]
                            ModernVitalCard(
                                label = type,
                                value = if (value != null) value.toInt().toString() else "--",
                                type = type,
                                isPlaceholder = value == null,
                                onClick = { onVitalClick(type) }
                            )
                        }
                    }
                }
            }

            // 6. Services Grid
            item {
                SectionHeader(title = "Health Services", onAction = {})
                Row(
                    modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ServiceTile("Analytics", Icons.Rounded.BarChart, Color(0xFFE3F2FD), onReportsClick, Modifier.weight(1f))
                    ServiceTile("Schedule", Icons.Rounded.EventAvailable, Color(0xFFFFF3E0), { onNavigateTo("calendar") }, Modifier.weight(1f))
                }
            }

            // 7. Live Health News Section
            item {
                Spacer(modifier = Modifier.height(24.dp))
                SectionHeader(title = "Live Health Insights", onAction = { newsViewModel.fetchNews() })
            }

            // News Feed Logic
            when {
                newsState.isLoading -> {
                    item {
                        Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = Color(0xFF2D6A4F))
                        }
                    }
                }
                newsState.errorMessage != null -> {
                    item {
                        Text(
                            text = newsState.errorMessage!!,
                            modifier = Modifier.padding(24.dp),
                            color = Color.Red,
                            fontSize = 14.sp
                        )
                    }
                }
                else -> {
                    items(newsState.articles) { article ->
                        DynamicInsightCard(
                            article = article,
                            onClick = { uriHandler.openUri(article.url) }
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

// --- REWRITTEN SUB-COMPONENTS ---

@Composable
fun EnhancedHeader(name: String, profilePicUrl: String, onProfileClick: () -> Unit) {
    val initial = if (name.isNotBlank()) name.take(1).uppercase() else "W"
    Row(
        modifier = Modifier.fillMaxWidth().padding(24.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text("Welcome back,", fontSize = 14.sp, color = Color.Gray)
            Text(name, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B4332))
        }
        Box(
            modifier = Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFD8F3DC)).clickable { onProfileClick() },
            contentAlignment = Alignment.Center
        ) {
            if (profilePicUrl.isNotBlank()) {
                AsyncImage(model = profilePicUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Text(text = initial, fontWeight = FontWeight.Bold, color = Color(0xFF2D6A4F), fontSize = 20.sp)
            }
        }
    }
}

@Composable
fun ModernVitalCard(label: String, value: String, type: String, isPlaceholder: Boolean, onClick: () -> Unit) {
    val icon = when (type) {
        "Heart Rate" -> Icons.Rounded.Favorite
        "Blood Pressure" -> Icons.Rounded.Speed
        "Steps" -> Icons.Rounded.DirectionsRun
        "Sleep" -> Icons.Rounded.Bedtime
        else -> Icons.Rounded.MonitorHeart
    }
    Surface(
        onClick = onClick,
        modifier = Modifier.width(150.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, if (isPlaceholder) Color(0xFFEEEEEE) else Color(0xFFF1F1F1))
    ) {
        Column(Modifier.padding(20.dp)) {
            Box(Modifier.size(40.dp).background(if (isPlaceholder) Color(0xFFF5F5F5) else Color(0xFFF0F9F4), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (isPlaceholder) Color.LightGray else Color(0xFF2D6A4F))
            }
            Spacer(Modifier.height(16.dp))
            Text(text = value, fontWeight = FontWeight.Black, fontSize = 26.sp, color = if (isPlaceholder) Color.LightGray else Color.Black)
            Text(text = label, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
            if (isPlaceholder) {
                Text(text = "Tap to add", fontSize = 10.sp, color = Color(0xFF2D6A4F), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 4.dp))
            }
        }
    }
}

@Composable
fun PremiumHealthScoreCard(score: Float) {
    val animatedScore by animateFloatAsState(targetValue = score, animationSpec = tween(1500, easing = FastOutSlowInEasing), label = "ScoreAnimation")
    Card(modifier = Modifier.padding(20.dp).fillMaxWidth(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                Canvas(modifier = Modifier.size(90.dp)) {
                    drawArc(color = Color(0xFFF1F8F5), startAngle = 0f, sweepAngle = 360f, useCenter = false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(color = Color(0xFF2D6A4F), startAngle = -90f, sweepAngle = (animatedScore / 100) * 360f, useCenter = false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                }
                Text(text = "${animatedScore.toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B4332))
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text("Health Score", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1B4332))
                Text(text = if (score >= 80) "Peak condition!" else "Log more data to improve.", color = Color.Gray, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun MoodHandlerSection(selectedMood: String?, onMoodSelected: (String) -> Unit) {
    val moods = listOf("🤩" to "Great", "😊" to "Good", "😐" to "Fine", "😔" to "Bad", "😴" to "Tired")
    Column(Modifier.padding(horizontal = 20.dp)) {
        Text("Daily Check-in", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color(0xFF1B4332))
        Spacer(Modifier.height(12.dp))
        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            items(moods) { (emoji, label) ->
                val isSelected = selectedMood == label
                Surface(onClick = { onMoodSelected(label) }, shape = RoundedCornerShape(16.dp), color = if (isSelected) Color(0xFF2D6A4F) else Color.White, border = BorderStroke(1.dp, Color(0xFFEEEEEE))) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(emoji, fontSize = 20.sp)
                        if (isSelected) {
                            Spacer(Modifier.width(4.dp))
                            Text(label, fontSize = 12.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun LumiAIActionCard(recommendation: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.padding(20.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF1B4332))) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).background(Color(0xFF2D6A4F), CircleShape), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White)
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Lumi Health AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(text = recommendation.ifBlank { "Analyzing your health trends..." }, color = Color.White.copy(0.7f), fontSize = 12.sp, maxLines = 1)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.White)
        }
    }
}

@Composable
fun DynamicInsightCard(article: HealthArticle, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(Color.White),
        border = BorderStroke(1.dp, Color(0xFFF5F5F5))
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = article.imageUrl, contentDescription = null, modifier = Modifier.size(70.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF0F9F4)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(text = article.source.name.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D6A4F))
                Text(text = article.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF1B4332), maxLines = 2, lineHeight = 20.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun ServiceTile(title: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier.height(100.dp), shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFF5F5F5))) {
        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color(0xFF2D6A4F))
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1B4332))
        }
    }
}

@Composable
fun SectionHeader(title: String, onAction: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF1B4332))
        TextButton(onClick = onAction) { Text("See All", color = Color(0xFF2D6A4F), fontSize = 13.sp) }
    }
}