// File: com/example/healthhive/MainActivity.kt

package com.example.healthhive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthhive.ui.screens.LoginScreen
import com.example.healthhive.viewmodel.SplashViewModel
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
// Import the missing SplashScreen and OnboardingScreen composables
import com.example.healthhive.ui.screens.SplashScreen
import com.example.healthhive.ui.screens.OnboardingScreen // Assuming you have this screen
import com.example.healthhive.ui.theme.HealthHiveTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthHiveTheme {
                val navController = rememberNavController()
                val splashViewModel: SplashViewModel = viewModel()

                // Keep observing the next route from the ViewModel
                val startDestination by splashViewModel.nextRoute.collectAsState()

                // FIX: Remove the 'startDestination?.let { ... }' wrapper.
                // The NavHost must be built immediately using Routes.SPLASH as the start.
                NavHost(
                    navController = navController,
                    startDestination = Routes.SPLASH // <-- Always start at SPLASH
                ) {

                    // 1. SPLASH SCREEN (FIXED NAVIGATION LOGIC)
                    composable(Routes.SPLASH) {
                        SplashScreen(
                            onNavigate = {
                                // Check for the result here. This will only run after the delay.
                                val destination = startDestination

                                if (destination != null) {
                                    // Navigate to the determined route (ONBOARDING, LOGIN, or HOME)
                                    navController.popBackStack()
                                    navController.navigate(destination)
                                }
                            }
                        )
                    }

                    // 2. ONBOARDING SCREEN (assuming existence of OnboardingScreen composable)
                    composable(Routes.ONBOARDING) {
                        OnboardingScreen(
                            onOnboardingComplete = {
                                splashViewModel.finishOnboarding()

                                val destination = splashViewModel.nextRoute.value
                                if (destination != null) {
                                    navController.popBackStack()
                                    navController.navigate(destination)
                                }
                            }
                        )
                    }

                    // 3. LOGIN SCREEN
                    composable(Routes.LOGIN) {
                        LoginScreen(
                            onLoginClick = {
                                // TODO: Implement authentication logic and navigate to Routes.HOME
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

                    // 4. HOME SCREEN
                    composable(Routes.HOME) {
                        androidx.compose.material3.Text("Home Screen Placeholder")
                    }

                    // 5. SIGNUP SCREEN (NEW)
                    composable(Routes.SIGNUP) {
                        // Placeholder UI
                        androidx.compose.material3.Text("Sign Up Screen is under construction!", modifier = Modifier.padding(16.dp))
                    }

                    // 6. FORGOT PASSWORD SCREEN (NEW)
                    composable(Routes.FORGOT_PASSWORD) {
                        // Placeholder UI
                        androidx.compose.material3.Text("Forgot Password Screen is under construction!", modifier = Modifier.padding(16.dp))
                    }
                }
            }
        }
    }
}