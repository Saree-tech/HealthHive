package com.example.healthhive.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LockReset
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthhive.viewmodel.ForgotPasswordViewModel
import com.example.healthhive.viewmodel.ForgotPasswordUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ForgotPasswordScreen(
    onSendResetSuccess: () -> Unit,
    onBackToLoginClick: () -> Unit,
    viewModel: ForgotPasswordViewModel = viewModel()
) {
    val uiState: ForgotPasswordUiState by viewModel.uiState.collectAsState()
    var email by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }

    // Navigation and Snackbar Logic
    LaunchedEffect(uiState.isEmailSent) {
        if (uiState.isEmailSent) {
            snackbarHostState.showSnackbar(
                message = uiState.emailSentMessage ?: "Reset email sent successfully.",
                duration = SnackbarDuration.Short
            )
            viewModel.resetUiState()
            onSendResetSuccess()
        }
    }

    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.resetUiState()
        }
    }

    // Theme Background
    val backgroundGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFFF0F9F9), Color(0xFFFFFFFF))
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color.Transparent,
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBackToLoginClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFF1B4332))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
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
                verticalArrangement = Arrangement.Top
            ) {
                Spacer(modifier = Modifier.height(40.dp))

                // Security Icon Illustration
                Surface(
                    modifier = Modifier.size(100.dp),
                    shape = RoundedCornerShape(30.dp),
                    color = Color(0xFFDFF2F2)
                ) {
                    Icon(
                        imageVector = Icons.Default.LockReset,
                        contentDescription = null,
                        modifier = Modifier.padding(20.dp).size(60.dp),
                        tint = Color(0xFF2D6A4F)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Forgot Password?",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B4332)
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "No worries! Enter your email below and we will send you a link to reset your password.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        color = Color.Gray,
                        lineHeight = 22.sp
                    ),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(48.dp))

                // Email Input with Icon
                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    placeholder = { Text("Enter your email") },
                    leadingIcon = {
                        Icon(Icons.Default.Email, contentDescription = null, tint = Color(0xFF2D6A4F))
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFF2D6A4F),
                        unfocusedBorderColor = Color(0xFFE0E0E0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    singleLine = true,
                    enabled = !uiState.isLoading
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Reset Button
                Button(
                    onClick = { viewModel.sendResetEmail(email) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D6A4F)),
                    enabled = !uiState.isLoading && email.isNotBlank()
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text("Send Reset Link", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Footer
                Text(
                    text = "Remembered your password? Log in",
                    modifier = Modifier.clickable { onBackToLoginClick() },
                    style = MaterialTheme.typography.bodyMedium.copy(
                        color = Color(0xFF2D6A4F),
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}