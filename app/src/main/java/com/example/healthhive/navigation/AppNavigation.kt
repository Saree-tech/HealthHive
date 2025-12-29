package com.example.healthhive.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthhive.ui.screens.*
import com.example.healthhive.viewmodel.HealthCalendarViewModel
import com.example.healthhive.viewmodel.ProfileViewModel
import com.example.healthhive.viewmodel.SymptomCheckerViewModel
import com.example.healthhive.viewmodel.LoginViewModel
import com.example.healthhive.viewmodel.VitalsViewModel

@Composable
fun AppNavigation(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Splash.route
) {
    // Top-level ViewModels to share data across screens (Profile & Vitals)
    val profileViewModel: ProfileViewModel = viewModel()
    val vitalsViewModel: VitalsViewModel = viewModel()
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // --- SPLASH ---
        composable(Screen.Splash.route) {
            SplashScreen()
            androidx.compose.runtime.LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(2500)
                navController.navigate(Screen.Login.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }

        // --- AUTH ---
        composable(Screen.Login.route) {
            val loginViewModel: LoginViewModel = viewModel()
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onSignUpClick = {
                    navController.navigate(Screen.Signup.route)
                },
                viewModel = loginViewModel
            )
        }

        // --- HOME SCREEN ---
        // Updated to include both Vitals and Profile ViewModels
        composable(Screen.Home.route) {
            HomeScreen(
                onNavigateTo = { route -> navController.navigate(route) },
                onSymptomCheckerClick = { navController.navigate(Screen.Chat.route) },
                onReportsClick = { navController.navigate("report_generator") },
                onSettingsClick = { navController.navigate(Screen.Profile.route) },
                onVitalClick = { vitalType -> /* Navigation to details if needed */ },
                vitalsViewModel = vitalsViewModel,
                profileViewModel = profileViewModel
            )
        }

        // --- REPORT GENERATOR ---
        // New route that consumes the shared ViewModels for dynamic data
        composable("report_generator") {
            ReportGeneratorScreen(
                onBackClick = { navController.popBackStack() },
                vitalsViewModel = vitalsViewModel,
                profileViewModel = profileViewModel
            )
        }

        // --- CALENDAR ---
        composable(Screen.HealthCalendar.route) {
            val calendarViewModel: HealthCalendarViewModel = viewModel()
            HealthCalendarScreen(
                onBack = { navController.popBackStack() },
                viewModel = calendarViewModel,
                showProgressFromPrefs = true
            )
        }

        // --- PROFILE SYSTEM ---
        composable(Screen.Profile.route) {
            ProfileScreen(
                viewModel = profileViewModel,
                onBack = { navController.popBackStack() },
                onEditClick = { navController.navigate(Screen.EditProfile.route) },
                onLogoutSuccess = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        }

        composable(Screen.EditProfile.route) {
            EditProfileScreen(
                viewModel = profileViewModel,
                onNavigateBack = { navController.popBackStack() }
            )
        }

        // --- CHAT (Lumi AI) ---
        composable(Screen.Chat.route) {
            val chatViewModel: SymptomCheckerViewModel = viewModel(
                factory = SymptomCheckerViewModel.Factory(context.applicationContext)
            )
            SymptomCheckerScreen(viewModel = chatViewModel)
        }
    }
}