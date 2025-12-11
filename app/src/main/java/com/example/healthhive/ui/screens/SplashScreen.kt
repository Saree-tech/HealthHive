// File: com/example/healthhive/ui/screens/SplashScreen.kt

package com.example.healthhive.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.example.healthhive.R // Import R to access drawable resources
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onNavigate: () -> Unit) { // <-- Simplified signature
    // 1. Wait for a short duration (simulating initialization)
    LaunchedEffect(key1 = true) {
        // Delay for 1.5 seconds
        delay(1500)

        // This triggers the navigation check in MainActivity.kt
        onNavigate()
    }

    // 2. The UI for the Splash Screen
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE6F3E6)), // Light green background
        contentAlignment = Alignment.Center
    ) {
        // --- LOGO IMPLEMENTATION ---
        Image(
            painter = painterResource(id = R.drawable.logo), // <-- **Change this to your actual logo resource ID**
            contentDescription = "HealthHive Logo",
            modifier = Modifier.size(250.dp) // Adjust size as needed
        )
        // -------------------------
    }
}