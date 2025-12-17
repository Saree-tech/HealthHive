@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthhive

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.healthhive.data.AuthService
import com.example.healthhive.ui.screens.HomeScreen
import com.example.healthhive.ui.screens.SymptomCheckerScreen
import com.example.healthhive.ui.screens.OnboardingScreen
import com.example.healthhive.ui.theme.HealthHiveTheme
import com.example.healthhive.viewmodel.HomeViewModel
import com.example.healthhive.viewmodel.SymptomCheckerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Onboarding : Screen("onboarding")
    data object Auth : Screen("auth")
    data object Home : Screen("home")
    data object LumiChat : Screen("lumi_chat")
    data object Recommendations : Screen("recommendations")
    data object Reports : Screen("reports")
    data object Tips : Screen("tips")
    data object Settings : Screen("settings")
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authService = AuthService()

        try {
            setContent {
                HealthHiveTheme {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        val navController = rememberNavController()
                        val context = LocalContext.current

                        val chatViewModel: SymptomCheckerViewModel = viewModel(
                            factory = SymptomCheckerViewModel.Factory(applicationContext)
                        )

                        NavHost(
                            navController = navController,
                            startDestination = Screen.Splash.route
                        ) {
                            // 1. SPLASH SCREEN
                            composable(Screen.Splash.route) {
                                SplashScreen(navController, authService)
                            }

                            // 2. ONBOARDING SCREEN (Fixed parameter name here)
                            composable(Screen.Onboarding.route) {
                                OnboardingScreen(
                                    onOnboardingComplete = {
                                        val sharedPrefs = context.getSharedPreferences("health_hive_prefs", Context.MODE_PRIVATE)
                                        sharedPrefs.edit().putBoolean("onboarding_complete", true).apply()

                                        navController.navigate(Screen.Auth.route) {
                                            popUpTo(Screen.Onboarding.route) { inclusive = true }
                                        }
                                    }
                                )
                            }

                            // 3. AUTH SCREEN
                            composable(Screen.Auth.route) {
                                AuthScreen(navController, authService)
                            }

                            // 4. HOME SCREEN
                            composable(Screen.Home.route) {
                                HomeScreen(
                                    onNavigateTo = { route -> navController.navigate(route) },
                                    onSymptomCheckerClick = { navController.navigate(Screen.LumiChat.route) },
                                    onRecommendationsClick = { navController.navigate(Screen.Recommendations.route) },
                                    onReportsClick = { navController.navigate(Screen.Reports.route) },
                                    onTipsClick = { navController.navigate(Screen.Tips.route) },
                                    onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                    viewModel = viewModel(factory = HomeViewModel.Factory())
                                )
                            }

                            composable(Screen.LumiChat.route) {
                                SymptomCheckerScreen(chatViewModel)
                            }

                            composable(Screen.Recommendations.route) { PlaceholderScreen("Recommendations") }
                            composable(Screen.Reports.route) { PlaceholderScreen("History / Reports") }
                            composable(Screen.Tips.route) { PlaceholderScreen("Health Tips") }
                            composable(Screen.Settings.route) { PlaceholderScreen("Settings / Profile") }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

@Composable
fun SplashScreen(navController: NavController, authService: AuthService) {
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        delay(2000)

        val sharedPrefs = context.getSharedPreferences("health_hive_prefs", Context.MODE_PRIVATE)
        val isOnboardingComplete = sharedPrefs.getBoolean("onboarding_complete", false)
        val currentUser = authService.getCurrentUser()

        when {
            currentUser != null -> {
                navController.navigate(Screen.Home.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            !isOnboardingComplete -> {
                navController.navigate(Screen.Onboarding.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
            else -> {
                navController.navigate(Screen.Auth.route) {
                    popUpTo(Screen.Splash.route) { inclusive = true }
                }
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            // Note: Make sure R.drawable.logo exists in your project
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "HealthHive Logo",
                modifier = Modifier.size(120.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text("HealthHive", style = MaterialTheme.typography.displayMedium)
        }
    }
}

@Composable
fun AuthScreen(navController: NavController, authService: AuthService) {
    var isLogin by remember { mutableStateOf(true) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var userName by remember { mutableStateOf("") }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var isLoading by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = if (isLogin) "Welcome Back" else "Create Account", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(24.dp))

        if (!isLogin) {
            OutlinedTextField(value = userName, onValueChange = { userName = it }, label = { Text("Full Name") }, modifier = Modifier.fillMaxWidth())
            Spacer(modifier = Modifier.height(8.dp))
        }

        OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, modifier = Modifier.fillMaxWidth())
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password, onValueChange = { password = it }, label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(), modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        try {
                            if (isLogin) authService.login(email, password)
                            else authService.signup(email, password, userName)
                            navController.navigate(Screen.Home.route) { popUpTo(Screen.Auth.route) { inclusive = true } }
                        } catch (e: Exception) {
                            Toast.makeText(context, e.localizedMessage ?: "Auth Failed", Toast.LENGTH_LONG).show()
                        } finally { isLoading = false }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text(if (isLogin) "Login" else "Sign Up") }
            TextButton(onClick = { isLogin = !isLogin }) {
                Text(if (isLogin) "Don't have an account? Sign Up" else "Already have an account? Login")
            }
        }
    }
}

@Composable
fun PlaceholderScreen(title: String) {
    Scaffold(
        topBar = { TopAppBar(title = { Text(title) }) }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues), contentAlignment = Alignment.Center) {
            Text(text = "This is the $title Screen.", textAlign = TextAlign.Center)
        }
    }
}