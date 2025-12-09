// File: com/example/healthhive/viewmodel/SplashViewModel.kt

package com.example.healthhive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.Routes
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SplashViewModel : ViewModel() {

    // Note: You must ensure you initialize Firebase in your application class or activity
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _nextRoute = MutableStateFlow<String?>(null)
    val nextRoute: StateFlow<String?> = _nextRoute

    // In a real app, this value MUST be read from DataStore/SharedPreferences
    // For now, it's simulated to trigger the onboarding flow on the first run.
    private var isOnboardingComplete: Boolean = false

    init {
        viewModelScope.launch {
            // Simulate reading the persistence flag and any initial load time
            delay(500)
            checkNextDestination()
        }
    }

    /**
     * Called by OnboardingScreen when the user hits "Get Started" or "Skip".
     * This marks onboarding as complete and checks authentication.
     */
    fun finishOnboarding() {
        // In a real app, save this state to DataStore/SharedPreferences
        isOnboardingComplete = true

        // Immediately check the final destination after onboarding is done
        checkAuthenticationStatus()
    }

    private fun checkNextDestination() {
        if (!isOnboardingComplete) {
            _nextRoute.value = Routes.ONBOARDING
        } else {
            checkAuthenticationStatus()
        }
    }

    private fun checkAuthenticationStatus() {
        val currentUser = auth.currentUser

        if (currentUser != null) {
            _nextRoute.value = Routes.HOME
        } else {
            _nextRoute.value = Routes.LOGIN
        }
    }
}