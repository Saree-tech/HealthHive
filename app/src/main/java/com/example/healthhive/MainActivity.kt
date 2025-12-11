// File: com/example/healthhive/MainActivity.kt (FULLY SYNCHRONIZED AND CORRECT)

package com.example.healthhive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
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
                    startDestination = Routes.SPLASH // Starts here
                ) {
                    // 1. SPLASH SCREEN (Correct)
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

                    // 2. ONBOARDING SCREEN (Correct)
                    composable(Routes.ONBOARDING) {
                        val newDestination by splashViewModel.nextRoute.collectAsState()

                        OnboardingScreen(
                            onOnboardingComplete = {
                                splashViewModel.finishOnboarding()
                            }
                        )

                        LaunchedEffect(newDestination) {
                            if (newDestination != null && newDestination != Routes.ONBOARDING) {
                                navController.popBackStack()
                                navController.navigate(newDestination!!)
                            }
                        }
                    }

                    // 3. LOGIN SCREEN (Correct)
                    composable(Routes.LOGIN) {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate(Routes.HOME) {
                                    popUpTo(Routes.SPLASH) { inclusive = true }
                                }
                            },
                            onSignUpClick = {
                                navController.navigate(Routes.SIGNUP)
                            },
                            onForgotPasswordClick = {
                                navController.navigate(Routes.FORGOT_PASSWORD)
                            }
                        )
                    }

                    // 4. HOME SCREEN (Ready for VM integration next)
                    composable(Routes.HOME) {
                        HomeScreen(
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

                    // 5. SIGNUP SCREEN (FIXED: onSignUpClick -> onSignUpSuccess)
                    composable(Routes.SIGNUP) {
                        SignUpScreen(
                            onSignUpSuccess = { // <-- FIXED PARAMETER NAME
                                // Navigate to HOME upon successful registration
                                navController.navigate(Routes.HOME) { popUpTo(Routes.SPLASH) { inclusive = true } }
                            },
                            onBackToLoginClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // 6. FORGOT PASSWORD SCREEN (FIXED: onSendResetClick -> onSendResetSuccess)
                    composable(Routes.FORGOT_PASSWORD) {
                        ForgotPasswordScreen(
                            onSendResetSuccess = { // <-- FIXED PARAMETER NAME
                                // Navigate back to login screen on successful email send
                                navController.popBackStack(Routes.LOGIN, inclusive = false)
                            },
                            onBackToLoginClick = {
                                navController.popBackStack()
                            }
                        )
                    }

                    // 7. FEATURE PLACEHOLDERS (Correct)
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