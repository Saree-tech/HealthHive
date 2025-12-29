@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthhive.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.viewmodel.ProfileViewModel

@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onEditClick: () -> Unit,
    onLogoutSuccess: () -> Unit,
    viewModel: ProfileViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var isPushEnabled by remember { mutableStateOf(true) }

    LaunchedEffect(uiState.isLoggedOut) {
        if (uiState.isLoggedOut) onLogoutSuccess()
    }

    Scaffold(
        containerColor = Color(0xFFF8FAFC),
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Profile", fontWeight = FontWeight.Black, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") }
                },
                actions = {
                    TextButton(onClick = onEditClick) {
                        Text("Edit", color = Color(0xFF2D6A4F), fontWeight = FontWeight.Black)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(20.dp)
        ) {
            item {
                ProfileHeaderWithStats(
                    name = uiState.user?.userName ?: "Health Warrior",
                    email = uiState.user?.email ?: "Syncing...",
                    age = uiState.user?.age?.ifBlank { "--" } ?: "--",
                    weight = uiState.user?.weight?.let { "$it kg" } ?: "-- kg",
                    height = uiState.user?.height?.let { "$it cm" } ?: "-- cm"
                )
                Spacer(modifier = Modifier.height(32.dp))
            }

            item { SectionLabel("Medical Identity") }
            item {
                SettingsCard {
                    StaticInfoItem("Blood Type", uiState.user?.bloodType?.ifBlank { "Not Set" } ?: "Not Set", Icons.Default.Bloodtype, Color(0xFFE63946))
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                    StaticInfoItem("Allergies", uiState.user?.allergies?.ifBlank { "None Reported" } ?: "None Reported", Icons.Default.Warning, Color(0xFFFFB703))
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                    StaticInfoItem("Medical History", uiState.user?.medicalHistory?.ifBlank { "No records" } ?: "No records", Icons.Default.History, Color(0xFF2D6A4F))
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }

            item { SectionLabel("Settings") }
            item {
                SettingsCard {
                    ToggleSettingsItem("Push Notifications", Icons.Default.NotificationsActive, Color(0xFF2D6A4F), isPushEnabled) { isPushEnabled = it }
                    HorizontalDivider(Modifier.padding(horizontal = 16.dp), thickness = 0.5.dp, color = Color(0xFFF1F5F9))
                    SettingsItem("Help & Support", Icons.Default.SupportAgent, Color(0xFF64748B)) {}
                }
            }

            item {
                Spacer(modifier = Modifier.height(40.dp))
                LogoutButton { viewModel.logout() }
                Spacer(modifier = Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("HealthHive v1.0.22", fontSize = 12.sp, color = Color.LightGray)
                }
            }
        }
    }
}

@Composable
fun ProfileHeaderWithStats(name: String, email: String, age: String, weight: String, height: String) {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(100.dp), shape = CircleShape, color = Color(0xFFE7F5EC)) {
            Box(contentAlignment = Alignment.Center) {
                Text(name.take(1).uppercase(), fontSize = 40.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D6A4F))
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(name, fontSize = 24.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B4332))
        Text(email, fontSize = 14.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            VerticalStat(age, "Age")
            Box(Modifier.height(30.dp).width(1.dp).background(Color(0xFFE2E8F0)))
            VerticalStat(weight, "Weight")
            Box(Modifier.height(30.dp).width(1.dp).background(Color(0xFFE2E8F0)))
            VerticalStat(height, "Height")
        }
    }
}

@Composable
fun VerticalStat(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontSize = 17.sp, fontWeight = FontWeight.Black, color = Color(0xFF1B4332))
        Text(label, fontSize = 12.sp, color = Color.Gray)
    }
}

@Composable
fun StaticInfoItem(label: String, value: String, icon: ImageVector, iconColor: Color) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).background(iconColor.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = iconColor)
        }
        Spacer(Modifier.width(16.dp))
        Column {
            Text(label, fontSize = 11.sp, color = Color.Gray, fontWeight = FontWeight.Bold)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = Color.Black)
        }
    }
}

@Composable
fun SettingsItem(title: String, icon: ImageVector, iconColor: Color, onClick: () -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().clickable { onClick() }.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).background(iconColor.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = iconColor)
        }
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
    }
}

@Composable
fun ToggleSettingsItem(title: String, icon: ImageVector, iconColor: Color, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).background(iconColor.copy(0.1f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, modifier = Modifier.size(18.dp), tint = iconColor)
        }
        Spacer(Modifier.width(16.dp))
        Text(title, modifier = Modifier.weight(1f), fontSize = 15.sp, fontWeight = FontWeight.Medium)
        Switch(checked = checked, onCheckedChange = onCheckedChange, colors = SwitchDefaults.colors(checkedTrackColor = Color(0xFF2D6A4F)))
    }
}

@Composable
fun SettingsCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) { Column(content = content) }
}

@Composable
fun SectionLabel(text: String) {
    Text(text, modifier = Modifier.padding(start = 8.dp, bottom = 8.dp), fontSize = 13.sp, fontWeight = FontWeight.Black, color = Color(0xFF2D6A4F))
}

@Composable
fun LogoutButton(onLogout: () -> Unit) {
    Button(
        onClick = onLogout,
        modifier = Modifier.fillMaxWidth().height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFEF2F2), contentColor = Color(0xFFEF4444)),
        elevation = null
    ) {
        Icon(Icons.AutoMirrored.Filled.Logout, null)
        Spacer(Modifier.width(8.dp))
        Text("Log Out", fontWeight = FontWeight.Bold)
    }
}