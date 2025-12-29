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
import com.example.healthhive.data.model.HealthArticle
import com.example.healthhive.viewmodel.HomeViewModel
import com.example.healthhive.viewmodel.VitalsViewModel
import com.example.healthhive.viewmodel.HealthNewsViewModel

@Composable
fun HomeScreen(
    onNavigateTo: (route: String) -> Unit,
    onSymptomCheckerClick: () -> Unit,
    onReportsClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onVitalClick: (vitalType: String) -> Unit = {},
    vitalsViewModel: VitalsViewModel = viewModel(),
    newsViewModel: HealthNewsViewModel = viewModel()
) {
    val context = LocalContext.current.applicationContext as Application
    val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(context))
    val uriHandler = LocalUriHandler.current

    val homeUiState by homeViewModel.uiState.collectAsState()
    val vitalsUiState by vitalsViewModel.uiState.collectAsState()
    val newsState by newsViewModel.uiState.collectAsState()

    val displayName = homeUiState.user?.userName?.takeIf { it.isNotBlank() }?.split(" ")?.firstOrNull() ?: "Warrior"
    val profilePicUrl = homeUiState.user?.profilePictureUrl ?: ""

    LaunchedEffect(Unit) {
        vitalsViewModel.startListening("All")
        homeViewModel.refreshDashboard()
    }

    Scaffold(
        containerColor = Color(0xFFF8FAF9),
        bottomBar = {
            NavigationBar(containerColor = Color.White, tonalElevation = 12.dp) {
                NavigationBarItem(selected = true, onClick = {}, icon = { Icon(Icons.Rounded.Home, null) }, label = { Text("Home") })
                NavigationBarItem(selected = false, onClick = onSymptomCheckerClick, icon = { Icon(Icons.Rounded.AutoAwesome, null) }, label = { Text("Lumi AI") })
                NavigationBarItem(selected = false, onClick = onReportsClick, icon = { Icon(Icons.Rounded.Assignment, null) }, label = { Text("Reports") })
                NavigationBarItem(selected = false, onClick = onSettingsClick, icon = { Icon(Icons.Rounded.Person, null) }, label = { Text("Profile") })
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentPadding = PaddingValues(bottom = 24.dp)
        ) {
            item { EnhancedHeader(name = displayName, profilePicUrl = profilePicUrl, onProfileClick = onSettingsClick) }

            item {
                MoodHandlerSection(selectedMood = homeUiState.selectedMood, onMoodSelected = { homeViewModel.updateMood(it) })
                Spacer(Modifier.height(16.dp))
            }

            item { PremiumHealthScoreCard(score = vitalsUiState.healthScore * 100) }

            item { LumiAIActionCard(recommendation = vitalsUiState.aiRecommendation, onClick = onSymptomCheckerClick) }

            item {
                SectionHeader(title = "Vitals", onAction = { onVitalClick("All") })
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val categories = listOf("Heart Rate", "Blood Pressure", "Steps", "Sleep")
                    items(categories) { type ->
                        val value = vitalsUiState.latestVitals[type]
                        ModernVitalCard(
                            label = type,
                            value = value?.toInt()?.toString() ?: "--",
                            type = type,
                            isPlaceholder = value == null,
                            onClick = { onVitalClick(type) }
                        )
                    }
                }
            }

            item {
                SectionHeader(title = "Health Services", onAction = {})
                Row(Modifier.padding(horizontal = 20.dp).fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ServiceTile("Analytics", Icons.Rounded.BarChart, Color(0xFFE3F2FD), onReportsClick, Modifier.weight(1f))
                    ServiceTile("Schedule", Icons.Rounded.EventAvailable, Color(0xFFFFF3E0), { onNavigateTo("calendar") }, Modifier.weight(1f))
                }
            }

            item {
                Spacer(Modifier.height(24.dp))
                SectionHeader(title = "Global Health Insights", onAction = { newsViewModel.fetchNews() })
            }

            // FIXED NEWS LOGIC: Handles loading, error, and list items correctly
            if (newsState.isLoading) {
                item {
                    Box(Modifier.fillMaxWidth().padding(40.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF2D6A4F))
                    }
                }
            } else if (newsState.errorMessage != null) {
                item {
                    Text(text = newsState.errorMessage!!, Modifier.padding(24.dp), color = Color.Red)
                }
            } else {
                items(newsState.articles) { article ->
                    DynamicInsightCard(
                        article = article,
                        onClick = { if (article.url.isNotBlank()) uriHandler.openUri(article.url) }
                    )
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

// --- REMAINING UI COMPONENTS ---

@Composable
fun EnhancedHeader(name: String, profilePicUrl: String, onProfileClick: () -> Unit) {
    val initial = if (name.isNotBlank()) name.take(1).uppercase() else "W"
    Row(Modifier.fillMaxWidth().padding(24.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
        Column {
            Text("Welcome back,", fontSize = 14.sp, color = Color.Gray)
            Text(name, fontSize = 26.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B4332))
        }
        Box(Modifier.size(50.dp).clip(CircleShape).background(Color(0xFFD8F3DC)).clickable { onProfileClick() }, contentAlignment = Alignment.Center) {
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
    Surface(onClick = onClick, modifier = Modifier.width(150.dp), shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, if (isPlaceholder) Color(0xFFEEEEEE) else Color(0xFFF1F1F1))) {
        Column(Modifier.padding(20.dp)) {
            Box(Modifier.size(40.dp).background(if (isPlaceholder) Color(0xFFF5F5F5) else Color(0xFFF0F9F4), CircleShape), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = if (isPlaceholder) Color.LightGray else Color(0xFF2D6A4F))
            }
            Spacer(Modifier.height(16.dp))
            Text(text = value, fontWeight = FontWeight.Black, fontSize = 26.sp, color = if (isPlaceholder) Color.LightGray else Color.Black)
            Text(text = label, fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
fun PremiumHealthScoreCard(score: Float) {
    val animatedScore by animateFloatAsState(targetValue = score, animationSpec = tween(1500), label = "")
    Card(Modifier.padding(20.dp).fillMaxWidth(), shape = RoundedCornerShape(32.dp), colors = CardDefaults.cardColors(Color.White), elevation = CardDefaults.cardElevation(2.dp)) {
        Row(Modifier.padding(24.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(90.dp)) {
                Canvas(Modifier.size(90.dp)) {
                    drawArc(Color(0xFFF1F8F5), 0f, 360f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                    drawArc(Color(0xFF2D6A4F), -90f, (animatedScore / 100) * 360f, false, style = Stroke(8.dp.toPx(), cap = StrokeCap.Round))
                }
                Text("${animatedScore.toInt()}%", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B4332))
            }
            Spacer(Modifier.width(20.dp))
            Column {
                Text("Health Score", fontWeight = FontWeight.ExtraBold, fontSize = 18.sp, color = Color(0xFF1B4332))
                Text(if (score >= 80) "Peak condition!" else "Log more data.", color = Color.Gray, fontSize = 13.sp)
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
            Box(Modifier.size(44.dp).background(Color(0xFF2D6A4F), CircleShape), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.AutoAwesome, null, tint = Color.White) }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text("Lumi Health AI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text(recommendation.ifBlank { "Analyzing trends..." }, color = Color.White.copy(0.7f), fontSize = 12.sp, maxLines = 1)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.White)
        }
    }
}

@Composable
fun DynamicInsightCard(article: HealthArticle, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.padding(horizontal = 20.dp).fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(Color.White), border = BorderStroke(1.dp, Color(0xFFF5F5F5))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(model = article.imageUrl, contentDescription = null, modifier = Modifier.size(70.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFF0F9F4)), contentScale = ContentScale.Crop)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(article.source.name.uppercase(), fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF2D6A4F))
                Text(article.title, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp, color = Color(0xFF1B4332), maxLines = 2, lineHeight = 20.sp)
            }
            Icon(Icons.Rounded.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

@Composable
fun ServiceTile(title: String, icon: ImageVector, color: Color, onClick: () -> Unit, modifier: Modifier) {
    Surface(onClick = onClick, modifier = modifier.height(100.dp), shape = RoundedCornerShape(24.dp), color = Color.White, border = BorderStroke(1.dp, Color(0xFFF5F5F5))) {
        Column(Modifier.fillMaxSize(), Arrangement.Center, Alignment.CenterHorizontally) {
            Icon(icon, null, tint = Color(0xFF2D6A4F))
            Spacer(Modifier.height(8.dp))
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color(0xFF1B4332))
        }
    }
}

@Composable
fun SectionHeader(title: String, onAction: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), Arrangement.SpaceBetween, Alignment.CenterVertically) {
        Text(title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color(0xFF1B4332))
        TextButton(onClick = onAction) { Text("See All", color = Color(0xFF2D6A4F), fontSize = 13.sp) }
    }
}