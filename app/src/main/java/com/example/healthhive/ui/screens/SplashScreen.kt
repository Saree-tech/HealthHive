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

    // 1. OBVIOUS ANIMATION: Pulsing Scale (Heartbeat effect)
    // Much more visible than a simple translation
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "logo_scale"
    )

    // 2. Subtle Rotation for extra dynamism
    val rotation by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ), label = "logo_rotation"
    )

    // 3. Shimmer Effect for Brand Text (Adjusted colors for White Background)
    val shimmerTranslate by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ), label = "text_shimmer"
    )

    // 4. Background Particles (Optimized for light mode)
    val particles = remember {
        List(20) {
            ParticleData(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                radius = Random.nextFloat() * 4f + 2f,
                speed = Random.nextFloat() * 0.5f + 0.2f
            )
        }
    }

    // Entrance Fade-in
    val alphaAnim = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        alphaAnim.animateTo(1f, tween(1200))
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            // WHITE BACKGROUND with subtle soft grey gradient
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFFFFFFF), Color(0xFFF8FAF9))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // --- BACKGROUND LAYER (Subtle floating dots) ---
        Canvas(modifier = Modifier.fillMaxSize()) {
            particles.forEach { p ->
                drawCircle(
                    color = Color(0xFF2D6A4F).copy(alpha = 0.08f),
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
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    rotationZ = rotation
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                // Glow effect (Adjusted for white background)
                Surface(
                    modifier = Modifier
                        .size(170.dp)
                        .blur(40.dp),
                    shape = CircleShape,
                    color = Color(0xFF2D6A4F).copy(alpha = 0.05f)
                ) {}

                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "HealthHive Logo",
                    modifier = Modifier.size(130.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Shimmering Text Logic (Darker colors to be visible on white)
            val shimmerBrush = Brush.linearGradient(
                colors = listOf(
                    Color(0xFF1B4332), // Dark Green
                    Color(0xFF52B788), // Light Green
                    Color(0xFF1B4332)  // Dark Green
                ),
                start = Offset(shimmerTranslate - 300f, shimmerTranslate - 300f),
                end = Offset(shimmerTranslate, shimmerTranslate)
            )

            Text(
                text = "HealthHive",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 2.sp,
                    brush = shimmerBrush
                )
            )

            Text(
                text = "INTELLIGENT WELLNESS",
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFF2D6A4F).copy(alpha = 0.5f),
                    letterSpacing = 5.sp,
                    fontWeight = FontWeight.Bold
                ),
                modifier = Modifier.padding(top = 10.dp)
            )
        }
    }
}

data class ParticleData(
    val x: Float,
    val y: Float,
    val radius: Float,
    val speed: Float
)