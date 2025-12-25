package com.example.healthhive.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthhive.R
import kotlin.random.Random

@Composable
fun SplashScreen() {
    val infiniteTransition = rememberInfiniteTransition(label = "splash_assets")

    // 1. Optimized Floating Animation using graphicsLayer (Hardware Accelerated)
    val floatingOffset by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "logo_float"
    )

    // 2. Static Background Particles (Optimized to prevent recomposition)
    val particles = remember {
        List(15) {
            ParticleData(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 5f + 2f
            )
        }
    }

    // 3. Shimmer Effect for the Brand Text
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "text_shimmer"
    )

    // Entrance Fade-in
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(1000))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF0D1B16), Color(0xFF052B1D))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // --- BACKGROUND LAYER ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawCircle(
                    color = Color(0xFF409167).copy(alpha = 0.15f),
                    radius = p.radius,
                    center = Offset(p.x * size.width, p.y * size.height)
                )
            }
        }

        // --- CONTENT LAYER ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .alpha(alphaAnim.value)
                .graphicsLayer { translationY = floatingOffset } // Efficient translation
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Glow effect behind logo
                Surface(
                    modifier = Modifier
                        .size(160.dp)
                        .blur(35.dp),
                    shape = CircleShape,
                    color = Color(0xFF52B788).copy(alpha = 0.1f)
                ) {}

                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "HealthHive Logo",
                    modifier = Modifier.size(120.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Shimmering Text Logic
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFFD8F3DC),
                    Color(0xFF74C69D),
                    Color(0xFFD8F3DC)
                ),
                start = Offset(shimmerTranslate - 500f, shimmerTranslate - 500f),
                end = Offset(shimmerTranslate, shimmerTranslate)
            )

            Text(
                text = "HealthHive",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp,
                    brush = shimmerBrush
                )
            )

            Text(
                text = "INTELLIGENT WELLNESS",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF95D5B2).copy(alpha = 0.6f),
                    letterSpacing = 6.sp,
                    fontWeight = FontWeight.Medium
                ),
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// Simplified data class for performance
data class ParticleData(val x: Float, val y: Float, val radius: Float)