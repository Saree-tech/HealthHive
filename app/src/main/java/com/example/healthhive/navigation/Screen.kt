package com.example.healthhive.navigation

import com.example.healthhive.Routes

/**
 * Maps the static Routes to a Navigation Screen hierarchy.
 */
sealed class Screen(val route: String) {
    data object Splash : Screen(Routes.SPLASH)
    data object Login : Screen(Routes.LOGIN)
    data object Signup : Screen(Routes.SIGNUP)
    data object Home : Screen(Routes.HOME)

    // Adding new specific routes for our features
    data object HealthCalendar : Screen("health_calendar_route")
    data object Profile : Screen("profile_route")
    data object EditProfile : Screen("edit_profile_route")
    data object Chat : Screen("chat_route")
}