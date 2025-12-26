@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthhive

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.healthhive.data.AuthService
import com.example.healthhive.ui.screens.*
import com.example.healthhive.ui.theme.HealthHiveTheme
import com.example.healthhive.viewmodel.HomeViewModel
import com.example.healthhive.viewmodel.SymptomCheckerViewModel
import com.example.healthhive.viewmodel.VitalsViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.random.Random

// --- SEALED CLASS FOR TYPE-SAFE NAVIGATION ---
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
    data object Calendar : Screen("calendar")
    data object VitalsDetail : Screen("vitals_detail/{vitalType}") {
        fun createRoute(vitalType: String) = "vitals_detail/$vitalType"
    }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val authService = AuthService()

        setContent {
            HealthHiveTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    val context = LocalContext.current

                    // Shared ViewModels for state consistency
                    val chatViewModel: SymptomCheckerViewModel = viewModel(
                        factory = SymptomCheckerViewModel.Factory(applicationContext)
                    )
                    // We instantiate VitalsViewModel here to be shared if needed,
                    // though each screen can also call viewModel() to get the same instance.
                    val vitalsViewModel: VitalsViewModel = viewModel()

                    NavHost(
                        navController = navController,
                        startDestination = Screen.Splash.route
                    ) {
                        // 1. SPLASH SCREEN
                        composable(Screen.Splash.route) {
                            SplashScreen(navController, authService)
                        }

                        // 2. ONBOARDING SCREEN
                        composable(Screen.Onboarding.route) {
                            OnboardingScreen(onOnboardingComplete = {
                                context.getSharedPreferences("health_hive_prefs", Context.MODE_PRIVATE)
                                    .edit().putBoolean("onboarding_complete", true).apply()
                                navController.navigate(Screen.Auth.route) {
                                    popUpTo(Screen.Onboarding.route) { inclusive = true }
                                }
                            })
                        }

                        // 3. AUTHENTICATION (LOGIN/SIGNUP)
                        composable(Screen.Auth.route) {
                            AuthScreen(navController, authService)
                        }

                        // 4. DYNAMIC HOME SCREEN
                        composable(Screen.Home.route) {
                            val homeViewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory())
                            HomeScreen(
                                onNavigateTo = { route -> navController.navigate(route) },
                                onSymptomCheckerClick = { navController.navigate(Screen.LumiChat.route) },
                                onReportsClick = { navController.navigate(Screen.Reports.route) },
                                onSettingsClick = { navController.navigate(Screen.Settings.route) },
                                onVitalClick = { type ->
                                    navController.navigate(Screen.VitalsDetail.createRoute(type))
                                },
                                vitalsViewModel = vitalsViewModel,
                                homeViewModel = homeViewModel
                            )
                        }

                        // 5. LUMI AI CHAT
                        composable(Screen.LumiChat.route) {
                            SymptomCheckerScreen(chatViewModel)
                        }

                        // 6. HEALTH CALENDAR
                        composable(Screen.Calendar.route) {
                            HealthCalendarScreen(onBack = { navController.popBackStack() })
                        }

                        // 7. DYNAMIC VITALS DETAIL (Passes "Heart Rate", "Steps", etc)
                        composable(
                            route = Screen.VitalsDetail.route,
                            arguments = listOf(navArgument("vitalType") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val vitalType = backStackEntry.arguments?.getString("vitalType") ?: "Health Vital"
                            VitalsDetailScreen(
                                vitalType = vitalType,
                                onBack = { navController.popBackStack() },
                                viewModel = vitalsViewModel
                            )
                        }

                        // 8. OTHER SERVICES
                        composable(Screen.Tips.route) {
                            NotificationsScreen(onBackClick = { navController.popBackStack() })
                        }
                        composable(Screen.Recommendations.route) { PlaceholderScreen("Recommendations", navController) }
                        composable(Screen.Reports.route) { PlaceholderScreen("History / Reports", navController) }
                        composable(Screen.Settings.route) { PlaceholderScreen("Settings / Profile", navController) }
                    }
                }
            }
        }
    }
}

// --- SPLASH SCREEN WITH LOGIC ---
@Composable
fun SplashScreen(navController: NavController, authService: AuthService) {
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -10f, targetValue = 10f,
        animationSpec = infiniteRepeatable(tween(2500, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "float"
    )

    LaunchedEffect(Unit) {
        delay(2500) // Visual branding delay
        val sharedPrefs = context.getSharedPreferences("health_hive_prefs", Context.MODE_PRIVATE)
        val isOnboardingComplete = sharedPrefs.getBoolean("onboarding_complete", false)
        val currentUser = authService.getCurrentUser()

        val destination = when {
            currentUser != null -> Screen.Home.route
            !isOnboardingComplete -> Screen.Onboarding.route
            else -> Screen.Auth.route
        }
        navController.navigate(destination) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0D1B16)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer { translationY = floatAnim }
        ) {
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(130.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "HealthHive",
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFD8F3DC),
                    letterSpacing = 2.sp
                )
            )
        }
    }
}

// --- AUTH SCREEN ---
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
        modifier = Modifier
            .fillMaxSize()
            .padding(30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isLogin) "Welcome Back" else "Create Account",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1B4332)
        )
        Spacer(Modifier.height(32.dp))

        if (!isLogin) {
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Full Name") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(Modifier.height(32.dp))

        if (isLoading) {
            CircularProgressIndicator(color = Color(0xFF2D6A4F))
        } else {
            Button(
                onClick = {
                    isLoading = true
                    scope.launch {
                        try {
                            if (isLogin) authService.login(email, password)
                            else authService.signup(email, password, userName)

                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Auth.route) { inclusive = true }
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, e.localizedMessage ?: "Auth Failed", Toast.LENGTH_SHORT).show()
                        } finally {
                            isLoading = false
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F))
            ) {
                Text(if (isLogin) "Login" else "Get Started")
            }

            TextButton(onClick = { isLogin = !isLogin }) {
                Text(
                    text = if (isLogin) "New here? Sign Up" else "Have an account? Login",
                    color = Color(0xFF2D6A4F)
                )
            }
        }
    }
}

// --- PLACEHOLDER ---
@Composable
fun PlaceholderScreen(title: String, navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("$title coming soon!")
        }
    }
}