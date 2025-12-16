@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthhive

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding // <-- Fixed: Missing import
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold // <-- Fixed: Missing import
import androidx.compose.material3.Surface
import androidx.compose.material3.Text // <-- Fixed: Missing import
import androidx.compose.material3.TopAppBar // <-- Fixed: Missing import
import androidx.compose.runtime.Composable // <-- Fixed: Missing import
import androidx.compose.ui.Alignment // <-- Fixed: Missing import
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign // <-- Fixed: Missing import
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthhive.ui.screens.HomeScreen
import com.example.healthhive.ui.screens.SymptomCheckerScreen
import com.example.healthhive.ui.theme.HealthHiveTheme
import com.example.healthhive.viewmodel.HomeViewModel
import com.example.healthhive.viewmodel.SymptomCheckerViewModel

// 1. Define all Navigation Routes
sealed class Screen(val route: String) {
    data object Home : Screen("home")
    data object LumiChat : Screen("lumi_chat")
    data object Recommendations : Screen("recommendations")
    data object Reports : Screen("reports")
    data object Tips : Screen("tips")
    data object Settings : Screen("settings")
    // Add other screens as needed
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            HealthHiveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    // ViewModel for the Chat, kept alive across the activity/navigation session
                    val chatViewModel: SymptomCheckerViewModel = viewModel(
                        factory = SymptomCheckerViewModel.Factory(applicationContext)
                    )

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Home.route
                    ) {
                        // Home Screen definition
                        composable(Screen.Home.route) {
                            HomeScreen(
                                // Provide the universal navigation function
                                onNavigateTo = { route -> navController.navigate(route) },

                                // Provide specific navigation functions for the buttons
                                onSymptomCheckerClick = { navController.navigate(Screen.LumiChat.route) },
                                onRecommendationsClick = { navController.navigate(Screen.Recommendations.route) },
                                onReportsClick = { navController.navigate(Screen.Reports.route) },
                                onTipsClick = { navController.navigate(Screen.Tips.route) },
                                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                // Use the factory for HomeViewModel
                                viewModel = viewModel(factory = HomeViewModel.Factory())
                            )
                        }

                        // Lumi Chat Screen
                        composable(Screen.LumiChat.route) {
                            SymptomCheckerScreen(chatViewModel)
                        }

                        // Placeholder Composables for other routes
                        composable(Screen.Recommendations.route) { PlaceholderScreen("Recommendations") }
                        composable(Screen.Reports.route) { PlaceholderScreen("History / Reports") }
                        composable(Screen.Tips.route) { PlaceholderScreen("Health Tips") }
                        composable(Screen.Settings.route) { PlaceholderScreen("Settings / Profile") }
                    }
                }
            }
        }
    }
}

// Simple Placeholder Screen for unbuilt navigation targets
@Composable
fun PlaceholderScreen(title: String) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) }
    ) { paddingValues ->
        Box(
            modifier = Modifier.fillMaxSize().padding(paddingValues),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "This is the $title Screen. Navigation successful!",
                textAlign = TextAlign.Center
            )
        }
    }
}