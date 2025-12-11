// File: com/example/healthhive/viewmodel/SignUpViewModel.kt

package com.example.healthhive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.Exception

/**
 * Data class representing the current state of the Sign Up Screen UI.
 */
data class SignUpUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isSignUpSuccessful: Boolean = false
)

class SignUpViewModel(
    private val authService: AuthService = AuthService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(SignUpUiState())
    val uiState: StateFlow<SignUpUiState> = _uiState

    /**
     * Attempts to create a new user account using the AuthService.
     */
    fun signup(userName: String, email: String, password: String) {
        // Basic validation: Prevent execution if passwords don't match or fields are empty
        if (userName.isBlank() || email.isBlank() || password.isBlank()) {
            _uiState.value = _uiState.value.copy(
                error = "All fields are required.",
                isLoading = false
            )
            return
        }

        // Prevent concurrent signup attempts
        if (_uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // 1. Call the Data Service to create the account
                // NOTE: The AuthService handles saving the userName to Firebase Auth profile
                authService.signup(email, password, userName)

                // 2. Update state for successful registration
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSignUpSuccessful = true
                )
            } catch (e: Exception) {
                // 3. Update state for registration failure
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    // Firebase Auth errors (e.g., 'email already in use', 'weak password')
                    error = e.localizedMessage ?: "An unknown error occurred during sign up."
                )
            }
        }
    }

    /**
     * Resets the error and success flags after a transient operation or navigation.
     */
    fun resetUiState() {
        _uiState.value = _uiState.value.copy(
            isSignUpSuccessful = false,
            error = null
        )
    }
}