package com.example.healthhive.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.healthhive.R
import kotlinx.coroutines.launch

data class OnboardingPage(
    val title: String,
    val description: String,
    val imageResId: Int
)

private val pages = listOf(
    OnboardingPage(
        title = "Track Your Health",
        description = "Monitor your vital signs and activity with AI-driven precision.",
        imageResId = R.drawable._1
    ),
    OnboardingPage(
        title = "Secure & Private",
        description = "Your health data is protected with military-grade encryption.",
        imageResId = R.drawable._2
    ),
    OnboardingPage(
        title = "Real-Time Alerts",
        description = "Get timely reminders to stay on top of your wellness goals.",
        imageResId = R.drawable._3
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    // Reusing the Splash Gradient for consistency
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF0F9F9), Color(0xFFFFFFFF))
    )

    Scaffold { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundGradient)
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Main Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .weight(0.8f)
                        .fillMaxWidth()
                ) { index ->
                    OnboardingPageContent(page = pages[index])
                }

                // Bottom UI Area
                Column(
                    modifier = Modifier
                        .weight(0.2f)
                        .fillMaxWidth()
                        .padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    // Modern Animated Indicator
                    OnboardingIndicator(
                        pageSize = pages.size,
                        currentPage = pagerState.currentPage
                    )

                    // Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        if (!isLastPage) {
                            TextButton(onClick = onOnboardingComplete) {
                                Text(
                                    "Skip",
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        color = Color.Gray,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.width(50.dp))
                        }

                        Button(
                            onClick = {
                                if (isLastPage) onOnboardingComplete()
                                else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2D6A4F) // Matching our Splash Green
                            ),
                            modifier = Modifier
                                .height(56.dp)
                                .then(if (isLastPage) Modifier.fillMaxWidth(0.8f) else Modifier.width(140.dp))
                        ) {
                            Text(
                                if (isLastPage) "Get Started" else "Next",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Soft Shadow/Glow effect would go here if using custom canvas,
        // but for now, we'll focus on clean scaling.
        Image(
            painter = painterResource(id = page.imageResId),
            contentDescription = null,
            modifier = Modifier
                .size(280.dp)
                .padding(20.dp)
        )
        Spacer(modifier = Modifier.height(40.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge.copy(
                fontWeight = FontWeight.ExtraBold,
                color = Color(0xFF1B4332),
                letterSpacing = (-1).sp
            ),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge.copy(
                lineHeight = 24.sp,
                color = Color.DarkGray,
                textAlign = TextAlign.Center
            )
        )
    }
}

@Composable
fun OnboardingIndicator(pageSize: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(pageSize) { index ->
            val isSelected = currentPage == index
            val width by animateDpAsState(
                targetValue = if (isSelected) 32.dp else 10.dp,
                animationSpec = tween(durationMillis = 300), label = ""
            )
            val color by animateColorAsState(
                targetValue = if (isSelected) Color(0xFF2D6A4F) else Color(0xFFB7E4C7),
                animationSpec = tween(durationMillis = 300), label = ""
            )

            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .height(10.dp)
                    .width(width)
                    .clip(RoundedCornerShape(10.dp))
                    .background(color)
            )
        }
    }
}