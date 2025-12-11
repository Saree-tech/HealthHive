// File: com/example/healthhive/ui/screens/ForgotPasswordScreen.kt (UPDATED WITH MVVM)

package com.example.healthhive.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.viewmodel.ForgotPasswordViewModel
import com.example.healthhive.viewmodel.ForgotPasswordUiState

@Composable
fun ForgotPasswordScreen(
    onSendResetSuccess: () -> Unit, // Changed parameter name
    onBackToLoginClick: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel() // Inject the ViewModel
) {
    // Collect the UI state
    val uiState: ForgotPasswordUiState by viewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // --- EFFECT HANDLING (Snackbar and Navigation) ---

    // 1. Success Message & Navigation
    LaunchedEffect(uiState.isEmailSent) {
        if (uiState.isEmailSent) {
            // Show success message
            snackbarHostState.showSnackbar(
                message = uiState.emailSentMessage ?: "Reset email sent.",
                actionLabel = "OK"
            )
            // Navigate back to login screen after showing message
            viewModel.resetUiState()
            onSendResetSuccess()
        }
    }

    // 2. Error Display
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            snackbarHostState.showSnackbar(
                message = errorMessage,
                actionLabel = "Dismiss"
            )
            viewModel.resetUiState()
        }
    }

    // --- UI STRUCTURE ---
    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(64.dp))

            Text(
                text = "Reset Password",
                style = MaterialTheme.typography.headlineLarge,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            Text(
                text = "Enter your email address to receive a password reset link.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(bottom = 48.dp)
            )

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email Address") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Send Reset Button
            Button(
                onClick = {
                    viewModel.sendResetEmail(email)
                },
                enabled = !uiState.isLoading && email.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Send Reset Link")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Back to Login Link
            Text(
                text = "Back to Login",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onBackToLoginClick)
            )
        }
    }
}