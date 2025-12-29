// File: com/example/healthhive/navigation/AppNavigation.kt

package com.example.healthhive.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthhive.Routes
import com.example.healthhive.ui.screens.*
import com.example.healthhive.viewmodel.*

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Routes.SPLASH
) {
    // Shared ViewModels for screens that need to sync data
    val profileViewModel: ProfileViewModel = viewModel()
    val vitalsViewModel: VitalsViewModel = viewModel()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- SPLASH ---
        composable(Routes.SPLASH) {
            SplashScreen()
            androidx.compose.runtime.LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2500)
                navController.navigate(Routes.LOGIN) {
                    popUpTo(Routes.SPLASH) { inclusive = true }
                }
            }
        }

        // --- AUTH FLOW ---
        composable(Routes.LOGIN) {
            val loginViewModel: LoginViewModel = viewModel()
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
                onSignUpClick = {
                    navController.navigate(Routes.SIGNUP)
                },
                onForgotPasswordClick = {
                    navController.navigate(Routes.FORGOT_PASSWORD)
                },
                viewModel = loginViewModel
            )
        }

        composable(Routes.FORGOT_PASSWORD) {
            ForgotPasswordScreen(
                onSendResetSuccess = {
                    navController.popBackStack()
                },
                onBackToLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        composable(Routes.SIGNUP) {
            SignUpScreen(
                onSignUpSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.SIGNUP) { inclusive = true }
                    }
                },
                onBackToLoginClick = {
                    navController.popBackStack()
                }
            )
        }

        // --- MAIN FEATURES ---
        composable(Routes.HOME) {
            HomeScreen(
                onNavigateTo = { route -> navController.navigate(route) },
                onSymptomCheckerClick = { navController.navigate(Routes.SYMPTOM_CHECKER) },
                onReportsClick = { navController.navigate(Routes.REPORTS) },
                onSettingsClick = { navController.navigate(Routes.SETTINGS) },
                // REMOVED profileViewModel because HomeScreen doesn't take it as a parameter
                vitalsViewModel = vitalsViewModel
            )
        }

        composable(Routes.REPORTS) {
            ReportGeneratorScreen(
                onBackClick = { navController.popBackStack() },
                vitalsViewModel = vitalsViewModel,
                profileViewModel = profileViewModel
            )
        }

        composable(Routes.SETTINGS) {
            ProfileScreen(
                viewModel = profileViewModel,
                onBack = { navController.popBackStack() },
                onEditClick = { navController.navigate("edit_profile_route") },
                onLogoutSuccess = {
                    navController.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable("edit_profile_route") {
            EditProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        composable(Routes.SYMPTOM_CHECKER) {
            val chatViewModel: SymptomCheckerViewModel = viewModel(
                factory = SymptomCheckerViewModel.Factory(context.applicationContext)
            )
            SymptomCheckerScreen(viewModel = chatViewModel)
        }
    }
}