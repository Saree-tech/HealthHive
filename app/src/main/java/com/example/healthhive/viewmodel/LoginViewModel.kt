// File: com/example/healthhive/viewmodel/LoginViewModel.kt

package com.example.healthhive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Data class representing the current state of the Login Screen UI.
 */
data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isLoginSuccessful: Boolean = false
)

class LoginViewModel(
    // NOTE: This default parameter simplifies usage without Hilt/DI for now.
    private val authService: AuthService = AuthService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState

    /**
     * Attempts to log in the user using the AuthService.
     */
    fun login(email: String, password: String) {
        // Prevent concurrent login attempts
        if (_uiState.value.isLoading) return

        // 1. Start loading and clear previous error
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        viewModelScope.launch {
            try {
                // 2. Call the Data Service
                authService.login(email, password)

                // 3. Update state for successful login
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoginSuccessful = true
                )
            } catch (e: Exception) {
                // 4. Update state for login failure
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "An unknown error occurred during login."
                )
            }
        }
    }

    /**
     * Resets the error and success flags after a transient operation or navigation.
     */
    fun resetUiState() {
        _uiState.value = _uiState.value.copy(
            isLoginSuccessful = false,
            error = null
        )
    }
}