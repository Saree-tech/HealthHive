// File: com/example/healthhive/ui/screens/ForgotPasswordScreen.kt

package com.example.healthhive.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.healthhive.R

@Composable
fun ForgotPasswordScreen(
    onSendResetClick: (email: String) -> Unit,
    onBackToLoginClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
            .imePadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 1. Logo
        Image(
            painter = painterResource(id = R.drawable.logo), // Use your actual logo resource ID
            contentDescription = "HealthHive Logo",
            modifier = Modifier.size(120.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Reset Password",
            style = MaterialTheme.typography.headlineMedium
        )

        Text(
            text = "Enter your email address to receive a password reset link.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(top = 8.dp),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // 2. Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 3. Send Reset Link Button
        Button(
            onClick = { onSendResetClick(email) },
            enabled = email.isNotBlank(),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Send Reset Link")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 4. Back to Login Link
        Text(
            text = "Back to Login",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onBackToLoginClick)
        )
    }
}