package com.example.healthhive.navigation

sealed class Screen(val route: String) {
    data object Login : Screen("login_screen")
    data object Home : Screen("home_screen")
    data object Splash : Screen("splash_screen")
    // Add other screens here as needed, e.g.:
    // data object SignUp : Screen("signup_screen")
}

