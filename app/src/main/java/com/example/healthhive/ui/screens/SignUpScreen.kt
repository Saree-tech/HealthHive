// File: com/example/healthhive/ui/screens/SignUpScreen.kt (UPDATED WITH MVVM)

package com.example.healthhive.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.R
import com.example.healthhive.viewmodel.SignUpViewModel
import com.example.healthhive.viewmodel.SignUpUiState

@Composable
fun SignUpScreen(
    onSignUpSuccess: () -> Unit, // Renamed for clarity: handles successful auth
    onBackToLoginClick: () -> Unit,
    viewModel: SignUpViewModel = viewModel() // Inject the ViewModel
) {
    // Collect the UI state from the ViewModel
    val uiState: SignUpUiState by viewModel.uiState.collectAsState()

    var userName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    // State for local password mismatch error
    var passwordMismatchError by remember { mutableStateOf(false) }

    // --- EFFECT HANDLING (Snackbar and Navigation) ---
    val snackbarHostState = remember { SnackbarHostState() }

    // 1. Navigation on Success
    LaunchedEffect(uiState.isSignUpSuccessful) {
        if (uiState.isSignUpSuccessful) {
            // Reset state to avoid re-triggering navigation
            viewModel.resetUiState()
            onSignUpSuccess()
        }
    }

    // 2. Error Display (VM errors)
    LaunchedEffect(uiState.error) {
        uiState.error?.let { errorMessage ->
            // Show VM error message and then clear it
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
                .padding(paddingValues) // Apply padding from Scaffold
                .padding(horizontal = 24.dp)
                .imePadding(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(32.dp))

            // 1. Logo and Header
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "HealthHive Logo",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Create Your Account",
                style = MaterialTheme.typography.headlineMedium
            )

            Spacer(modifier = Modifier.height(24.dp))

            // --- Input Fields ---

            // User Name Field
            OutlinedTextField(
                value = userName,
                onValueChange = { userName = it },
                label = { Text("Your Name") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Email Field
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Password Field
            OutlinedTextField(
                value = password,
                onValueChange = { password = it; passwordMismatchError = false },
                label = { Text("Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Confirm Password Field
            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it; passwordMismatchError = false },
                label = { Text("Confirm Password") },
                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                isError = passwordMismatchError,
                supportingText = {
                    if (passwordMismatchError) {
                        Text("Passwords do not match")
                    }
                },
                trailingIcon = {
                    val image = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff
                    IconButton(onClick = { passwordVisible = !passwordVisible }) {
                        Icon(imageVector  = image, contentDescription = "Toggle password visibility")
                    }
                },
                enabled = !uiState.isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 5. Sign Up Button
            Button(
                onClick = {
                    if (password != confirmPassword) {
                        passwordMismatchError = true
                        return@Button
                    }
                    // Call the ViewModel's signup function
                    viewModel.signup(userName, email, password)
                },
                enabled = !uiState.isLoading && userName.isNotBlank() && email.isNotBlank() && password.isNotBlank() && confirmPassword.isNotBlank(),
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
                    Text("Sign Up")
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 6. Back to Login Link
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Already have an account? ",
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "Login",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable(onClick = onBackToLoginClick)
                )
            }
        }
    }
}