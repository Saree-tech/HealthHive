// File: com/example/healthhive/viewmodel/SplashViewModel.kt (UPDATED)

package com.example.healthhive.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.Routes
import com.example.healthhive.utils.PreferencesManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// IMPORTANT: ViewModel must be AndroidViewModel to access Application Context for DataStore
class SplashViewModel(application: Application) : AndroidViewModel(application) {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val prefsManager = PreferencesManager(application.applicationContext)

    private val _nextRoute = MutableStateFlow<String?>(null)
    val nextRoute: StateFlow<String?> = _nextRoute

    init {
        viewModelScope.launch {
            // Read the persistent state before proceeding
            val isComplete = prefsManager.isOnboardingComplete.first()

            // Simulate splash delay (or initial load time)
            // delay(500)

            checkNextDestination(isComplete)
        }
    }

    /**
     * Called by OnboardingScreen when the user hits "Get Started" or "Skip".
     * This marks onboarding as complete and checks authentication.
     */
    fun finishOnboarding() {
        viewModelScope.launch {
            // 1. Save state to DataStore
            prefsManager.setOnboardingComplete(true)

            // 2. Immediately check the final destination after onboarding is done
            checkAuthenticationStatus()
        }
    }

    private fun checkNextDestination(isOnboardingComplete: Boolean) {
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