package com.example.healthhive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect // <-- NEW IMPORT
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthhive.ui.screens.LoginScreen
import com.example.healthhive.viewmodel.SplashViewModel
import androidx.compose.material3.Text
// Make sure to import all required screens
import com.example.healthhive.ui.screens.HomeScreen
import com.example.healthhive.ui.screens.SplashScreen
import com.example.healthhive.ui.screens.OnboardingScreen
import com.example.healthhive.ui.screens.SignUpScreen
import com.example.healthhive.ui.screens.ForgotPasswordScreen
import com.example.healthhive.ui.theme.HealthHiveTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthHiveTheme {
                val navController = rememberNavController()
                val splashViewModel: SplashViewModel = viewModel()
                val startDestination by splashViewModel.nextRoute.collectAsState()

                NavHost(
                    navController = navController,
                    startDestination = Routes.SPLASH
                ) {
                    // 1. SPLASH SCREEN (Navigation remains correct)
                    composable(Routes.SPLASH) {
                        SplashScreen(
                            onNavigate = {
                                val destination = startDestination
                                if (destination != null) {
                                    navController.popBackStack()
                                    navController.navigate(destination)
                                }
                            }
                        )
                    }

                    // 2. ONBOARDING SCREEN (FIXED NAVIGATION LOGIC)
                    composable(Routes.ONBOARDING) {
                        // We observe the ViewModel's route state change
                        val newDestination by splashViewModel.nextRoute.collectAsState()

                        OnboardingScreen(
                            onOnboardingComplete = {
                                // Only trigger the state update/save action in the ViewModel
                                splashViewModel.finishOnboarding()
                            }
                        )

                        // Use LaunchedEffect to react to the state change and navigate
                        LaunchedEffect(newDestination) {
                            // Navigate only if the new destination is set and is NOT the onboarding screen itself
                            if (newDestination != null && newDestination != Routes.ONBOARDING) {
                                navController.popBackStack()
                                navController.navigate(newDestination!!)
                            }
                        }
                    }

                    // 3. LOGIN SCREEN (Remains the same)
                    composable(Routes.LOGIN) {
                        LoginScreen(
                            onLoginClick = {
                                // TODO: Replace with authenticated navigation after Firebase setup
                                navController.navigate(Routes.HOME)
                            },
                            onSignUpClick = {
                                navController.navigate(Routes.SIGNUP)
                            },
                            onForgotPasswordClick = {
                                navController.navigate(Routes.FORGOT_PASSWORD)
                            }
                        )
                    }

                    // 4. HOME SCREEN (Remains the same)
                    composable(Routes.HOME) {
                        HomeScreen(
                            userName = "HealthHive User",
                            onNavigateTo = { route -> navController.navigate(route) },
                            onSymptomCheckerClick = {
                                navController.navigate(Routes.SYMPTOM_CHECKER)
                            },
                            onRecommendationsClick = {
                                navController.navigate(Routes.RECOMMENDATIONS)
                            },
                            onReportsClick = {
                                navController.navigate(Routes.REPORTS)
                            },
                            onTipsClick = {
                                navController.navigate(Routes.TIPS)
                            },
                            onSettingsClick = {
                                navController.navigate(Routes.SETTINGS)
                            }
                        )
                    }

                    // 5. SIGNUP SCREEN (Remains the same)
                    composable(Routes.SIGNUP) {
                        SignUpScreen(
                            onSignUpClick = {
                                navController.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                            },
                            onBackToLoginClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // 6. FORGOT PASSWORD SCREEN (Remains the same)
                    composable(Routes.FORGOT_PASSWORD) {
                        ForgotPasswordScreen(
                            onSendResetClick = { email ->
                                // TODO: Implement reset email logic
                                navController.popBackStack(Routes.LOGIN, inclusive = false)
                            },
                            onBackToLoginClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // 7. FEATURE PLACEHOLDERS (Remains the same)
                    composable(Routes.SYMPTOM_CHECKER) { Text("AI Symptom Checker Screen is coming soon!") }
                    composable(Routes.RECOMMENDATIONS) { Text("Health Tracker Recommendations Screen is coming soon!") }
                    composable(Routes.REPORTS) { Text("History / Reports Screen is coming soon!") }
                    composable(Routes.TIPS) { Text("Health Tips Screen is coming soon!") }
                    composable(Routes.SETTINGS) { Text("Settings Screen is coming soon!") }
                    composable(Routes.NOTIFICATIONS) { Text("Notifications Screen is coming soon!") }
                }
            }
        }
    }
}