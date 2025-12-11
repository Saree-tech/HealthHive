// File: com/example/healthhive/viewmodel/ForgotPasswordViewModel.kt

package com.example.healthhive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.AuthService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.lang.Exception

/**
 * Data class representing the current state of the Forgot Password UI.
 */
data class ForgotPasswordUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isEmailSent: Boolean = false,
    val emailSentMessage: String? = null
)

class ForgotPasswordViewModel(
    private val authService: AuthService = AuthService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState: StateFlow<ForgotPasswordUiState> = _uiState

    /**
     * Attempts to send a password reset email using the AuthService.
     */
    fun sendResetEmail(email: String) {
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your email address.")
            return
        }

        if (_uiState.value.isLoading) return

        _uiState.value = _uiState.value.copy(isLoading = true, error = null, isEmailSent = false)

        viewModelScope.launch {
            try {
                // 1. Call the Data Service to send the email
                authService.sendPasswordResetEmail(email)

                // 2. Update state for successful email send
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isEmailSent = true,
                    emailSentMessage = "Password reset link sent to $email."
                )
            } catch (e: Exception) {
                // 3. Update state for failure
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to send reset email. Check the address."
                )
            }
        }
    }

    /**
     * Resets the error and success flags after a transient operation or navigation.
     */
    fun resetUiState() {
        _uiState.value = _uiState.value.copy(
            isEmailSent = false,
            emailSentMessage = null,
            error = null
        )
    }
}