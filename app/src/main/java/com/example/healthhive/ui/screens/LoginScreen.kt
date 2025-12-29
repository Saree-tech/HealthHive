package com.example.healthhive.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.R
import com.example.healthhive.viewmodel.LoginViewModel
import com.example.healthhive.viewmodel.LoginUiState
import com.example.healthhive.viewmodel.ForgotPasswordViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginSuccess: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit, // Added to navigate to your separate ForgotPasswordScreen
    viewModel: LoginViewModel = viewModel(),
    forgotPasswordViewModel: ForgotPasswordViewModel = viewModel()
) {
    val uiState: LoginUiState by viewModel.uiState.collectAsState()
    val forgotUiState by forgotPasswordViewModel.uiState.collectAsState()

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var showForgotDialog by remember { mutableStateOf(false) }
    var resetEmail by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation on Success
    LaunchedEffect(uiState.isLoginSuccessful) {
        if (uiState.isLoginSuccessful) {
            viewModel.resetUiState()
            onLoginSuccess()
        }
    }

    // Error & Success Handling
    LaunchedEffect(uiState.error, forgotUiState.error, forgotUiState.isEmailSent) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetUiState()
        }
        forgotUiState.error?.let {
            snackbarHostState.showSnackbar(it)
            forgotPasswordViewModel.resetUiState()
        }
        if (forgotUiState.isEmailSent) {
            snackbarHostState.showSnackbar(forgotUiState.emailSentMessage ?: "Reset email sent!")
            forgotPasswordViewModel.resetUiState()
            showForgotDialog = false
        }
    }

    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF0F9F9), Color(0xFFFFFFFF))
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(brush = backgroundGradient)
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 30.dp)
                    .imePadding(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "Logo",
                    modifier = Modifier.size(120.dp)
                )

                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B4332)
                    )
                )

                Text(
                    text = "Log in to continue your wellness journey",
                    style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
                )

                Spacer(modifier = Modifier.height(40.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Email Address") },
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF2D6A4F)) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2D6A4F),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color(0xFF2D6A4F)) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2D6A4F),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    singleLine = true
                )

                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                    Text(
                        text = "Forgot Password?",
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .clickable {
                                // Choice: Open Dialog OR Navigate to separate screen
                                // showForgotDialog = true
                                onForgotPasswordClick()
                            },
                        style = MaterialTheme.typography.bodyMedium.copy(
                            color = Color(0xFF2D6A4F),
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = { viewModel.login(email, password) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                    enabled = !uiState.isLoading
                ) {
                    if (uiState.isLoading) {
                        // FIXED: Modifier.size instead of size parameter
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Login", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Row {
                    Text("Don't have an account? ", color = Color.Gray)
                    Text(
                        text = "Sign Up",
                        modifier = Modifier.clickable { onSignUpClick() },
                        color = Color(0xFF2D6A4F),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }

    // --- FORGOT PASSWORD DIALOG (Kept for reference, logic fixed) ---
    if (showForgotDialog) {
        AlertDialog(
            onDismissRequest = { if (!forgotUiState.isLoading) showForgotDialog = false },
            title = { Text("Reset Password", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Enter your email to receive a password reset link.")
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = resetEmail,
                        onValueChange = { resetEmail = it },
                        label = { Text("Email") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        singleLine = true
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { forgotPasswordViewModel.sendResetEmail(resetEmail) },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                    enabled = !forgotUiState.isLoading
                ) {
                    if (forgotUiState.isLoading) {
                        // FIXED: Modifier.size instead of size parameter
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Send Link")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showForgotDialog = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = Color.White
        )
    }
}