// File: com/example/healthhive/ui/screens/HomeScreen.kt (FINAL MODIFIED VERSION)

package com.example.healthhive.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
// IMPORTANT: MonitorHeart and Assignment are here:
import androidx.compose.material.icons.filled.Assignment
import androidx.compose.material.icons.filled.MonitorHeart
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Lightbulb // New icon for Tips
import androidx.compose.material.icons.filled.Settings // Re-adding Settings icon
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.healthhive.R

@Composable
fun HomeScreen(
    // >>> CORRECTION: ADD THE MISSING USERNAME PARAMETER HERE <<<
    userName: String,
    onNavigateTo: (route: String) -> Unit,
    // Add specific routes/functions for the four main features
    onSymptomCheckerClick: () -> Unit,
    onRecommendationsClick: () -> Unit,
    onReportsClick: () -> Unit,
    onTipsClick: () -> Unit, // New function for the 'Tips' card
    onSettingsClick: () -> Unit // Function for the 5th card/Settings
) {
    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(48.dp))

            // 1. Profile Avatar (Placeholder)
            Icon(
                imageVector = Icons.Filled.Person,
                contentDescription = "Profile Avatar",
                modifier = Modifier.size(72.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. Welcome Header (UPDATED TO USE THE PARAMETER)
            Text(
                text = "Hello, $userName", // <-- CORRECTED: Now displays the passed name
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 32.dp)
            )

            // 3. 2x2 Feature Grid
            // Row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FeatureCard(
                    title = "Symptom Checker",
                    icon = Icons.Filled.MonitorHeart,
                    onClick = onSymptomCheckerClick,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                FeatureCard(
                    title = "Health Tracker Recommendations",
                    icon = Icons.Filled.Favorite,
                    onClick = onRecommendationsClick,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 2 (Now contains Reports and Tips)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                FeatureCard(
                    title = "History / Reports",
                    icon = Icons.Filled.Assignment,
                    onClick = onReportsClick,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                FeatureCard(
                    title = "Health Tips",
                    icon = Icons.Filled.Lightbulb,
                    onClick = onTipsClick,
                    modifier = Modifier.weight(1f).padding(start = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 3 (For Settings/Profile or other overflow)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                FeatureCard(
                    title = "Settings / Profile",
                    icon = Icons.Filled.Settings,
                    onClick = onSettingsClick,
                    modifier = Modifier
                        .fillMaxWidth(0.5f)
                        .padding(horizontal = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Arrow at the bottom
            Icon(
                imageVector = Icons.Filled.ArrowForward,
                contentDescription = "Quick Action",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .size(48.dp)
                    .clickable { /* Handle quick action navigation here if needed */ }
            )
        }
    }
}

@Composable
fun FeatureCard(title: String, icon: ImageVector, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}