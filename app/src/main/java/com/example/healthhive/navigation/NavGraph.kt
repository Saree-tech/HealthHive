package com.example.healthhive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.healthhive.ui.screens.HomeScreen
import com.example.healthhive.ui.screens.SymptomCheckerScreen
import com.example.healthhive.viewmodel.HomeViewModel
import com.example.healthhive.viewmodel.SymptomCheckerViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay

// Assuming AuthScreen is for internal NavGraph logic
sealed class AuthScreen(val route: String) {
    data object Splash : AuthScreen("splash")
    data object Login : AuthScreen("login")
}

@Composable
fun SetupNavGraph(
    navController: NavHostController,
    chatViewModel: SymptomCheckerViewModel
) {
    NavHost(
        navController = navController,
        startDestination = AuthScreen.Splash.route
    ) {
        // 1. Splash Screen
        composable(AuthScreen.Splash.route) {
            val auth = FirebaseAuth.getInstance()
            LaunchedEffect(Unit) {
                delay(1000)
                if (auth.currentUser != null) {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(AuthScreen.Splash.route) { inclusive = true }
                    }
                } else {
                    // Navigate to Home as fallback or Login if created
                    navController.navigate(Screen.Home.route) {
                        popUpTo(AuthScreen.Splash.route) { inclusive = true }
                    }
                }
            }
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }

        // 2. Home Screen
        composable(Screen.Home.route) {
            HomeScreen(
                // Explicitly typing 'route' to fix inference issues
                onNavigateTo = { route: String -> navController.navigate(route) },
                onSymptomCheckerClick = { navController.navigate(Screen.LumiChat.route) },
                onRecommendationsClick = { navController.navigate(Screen.Recommendations.route) },
                onReportsClick = { navController.navigate(Screen.Reports.route) },
                onTipsClick = { navController.navigate(Screen.Tips.route) },
                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                viewModel = viewModel(factory = HomeViewModel.Factory())
            )
        }

        // 3. Other Screens (Passing navController to fix the parameter error)
        composable(Screen.LumiChat.route) {
            SymptomCheckerScreen(chatViewModel)
        }

        composable(Screen.Recommendations.route) {
            PlaceholderScreen("Recommendations", navController)
        }

        composable(Screen.Reports.route) {
            PlaceholderScreen("History / Reports", navController)
        }

        composable(Screen.Tips.route) {
            PlaceholderScreen("Health Tips", navController)
        }

        composable(Screen.Settings.route) {
            PlaceholderScreen("Settings / Profile", navController)
        }
    }
}