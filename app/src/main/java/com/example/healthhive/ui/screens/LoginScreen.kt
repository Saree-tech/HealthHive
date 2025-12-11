// File: com/example/healthhive/ui/screens/LoginScreen.kt

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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.healthhive.R // Assuming your logo and other resources are here

@Composable
fun LoginScreen(
    onLoginClick: () -> Unit,
    onSignUpClick: () -> Unit,
    onForgotPasswordClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    // Use Column for vertical arrangement of elements
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .imePadding(), // Handles keyboard pushing the layout
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(32.dp))

        // 1. Logo
        Image(
            painter = painterResource(id = R.drawable.logo), // Use your actual logo resource ID
            contentDescription = "HealthHive Logo",
            modifier = Modifier.size(150.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 2. Email Field
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 3. Password Field
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))

        // 4. Login Button
        Button(
            onClick = onLoginClick, // Placeholder for actual login logic
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Login")
        }

        Spacer(modifier = Modifier.height(24.dp))

        // 5. Forgot Password Link
        Text(
            text = "Forgot your password?",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.clickable(onClick = onForgotPasswordClick)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 6. Don't Have an Account Link
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Do not have an account? ",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Sign Up",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onSignUpClick)
            )
        }
    }
}