// File: com/example/healthhive/ui/screens/OnboardingScreen.kt

package com.example.healthhive.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.healthhive.R // Assuming your drawables are in the 'R' file
import kotlinx.coroutines.launch

// Data class to define the content for each Onboarding page
data class OnboardingPage(
    val title: String,
    val description: String,
    val imageResId: Int // Use your drawable resource IDs here
)

// Define the 3 pages of content
private val pages = listOf(
    OnboardingPage(
        title = "Track Your Health",
        description = "Monitor your steps, heart rate, and activity with precision.",
        imageResId = R.drawable._1
    ),
    OnboardingPage(
        title = "Secure & Private",
        description = "Your health data is safe and secure with Firebase Authentication.",
        imageResId = R.drawable._2
    ),
    OnboardingPage(
        title = "Real-Time Alerts",
        description = "Get timely reminders and notifications to stay on top of your goals.",
        imageResId = R.drawable._3
    )
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(onOnboardingComplete: () -> Unit) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val coroutineScope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.size - 1

    Scaffold(
        bottomBar = {
            OnboardingBottomBar(
                isLastPage = isLastPage,
                onSkipClick = onOnboardingComplete,
                onNextClick = {
                    if (isLastPage) {
                        onOnboardingComplete()
                    } else {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )
        }
    ) { paddingValues ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f) // Ensures the pager takes up available space above the indicator
            ) { pageIndex ->
                OnboardingPageContent(page = pages[pageIndex])
            }

            // PAGER INDICATOR (The navigation dots)
            Row(
                Modifier
                    .wrapContentHeight()
                    .fillMaxWidth()
                    .padding(bottom = 24.dp), // Space between dots and bottom bar
                horizontalArrangement = Arrangement.Center
            ) {
                repeat(pagerState.pageCount) { iteration ->
                    val color = if (pagerState.currentPage == iteration) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .background(color, shape = CircleShape)
                    )
                }
            }
        }
    }
}

// Composable for the content of a single page
@Composable
fun OnboardingPageContent(page: OnboardingPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = page.imageResId),
            contentDescription = page.title,
            modifier = Modifier.size(250.dp) // Slightly increased size for visibility
        )
        Spacer(Modifier.height(48.dp))
        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(16.dp))
        Text(
            text = page.description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(Modifier.height(48.dp)) // Added spacer for better visual balance
    }
}

// Composable for the dynamic button bar
@Composable
fun OnboardingBottomBar(
    isLastPage: Boolean,
    onSkipClick: () -> Unit,
    onNextClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp), // Increased padding
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Skip Button (Visible only on first pages)
        if (!isLastPage) {
            TextButton(onClick = onSkipClick) {
                Text("Skip")
            }
        } else {
            // Spacer ensures the "Get Started" button is correctly positioned
            Spacer(modifier = Modifier.weight(1f))
        }

        // Next / Get Started Button
        Button(
            onClick = onNextClick,
            // When on the last page, the button takes up the remaining width
            modifier = if (isLastPage) Modifier.weight(1f) else Modifier.wrapContentWidth()
        ) {
            Text(if (isLastPage) "Get Started" else "Next")
        }
    }
}