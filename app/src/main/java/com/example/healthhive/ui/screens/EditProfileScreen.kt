@file:OptIn(ExperimentalMaterial3Api::class)

package com.example.healthhive.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.healthhive.data.model.User
import com.example.healthhive.viewmodel.ProfileViewModel

@Composable
fun EditProfileScreen(
    viewModel: ProfileViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()

    // Form states
    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var bloodType by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var medicalHistory by remember { mutableStateOf("") }

    // Update local state when user data is loaded from ViewModel
    LaunchedEffect(uiState.user) {
        uiState.user?.let {
            name = it.userName
            age = it.age
            weight = it.weight
            height = it.height
            bloodType = it.bloodType
            allergies = it.allergies
            medicalHistory = it.medicalHistory
        }
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
        onResult = { uri ->
            uri?.let { viewModel.uploadProfileImage(it) }
        }
    )

    Scaffold(
        containerColor = Color.White,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Edit Profile", fontWeight = FontWeight.Black, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) { Icon(Icons.Rounded.Close, "Cancel") }
                },
                actions = {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp, color = Color(0xFF2D6A4F))
                        Spacer(Modifier.width(16.dp))
                    } else {
                        TextButton(onClick = {
                            val updated = uiState.user?.copy(
                                userName = name,
                                age = age,
                                weight = weight,
                                height = height,
                                bloodType = bloodType,
                                allergies = allergies,
                                medicalHistory = medicalHistory
                            ) ?: User()
                            viewModel.updateProfile(updated) { onNavigateBack() }
                        }) {
                            Text("SAVE", fontWeight = FontWeight.Black, color = Color(0xFF2D6A4F))
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Profile Image Section
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                        )
                    },
                contentAlignment = Alignment.BottomEnd
            ) {
                val imageUrl = uiState.user?.profilePictureUrl

                if (!imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Profile Picture",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = "Default Profile",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color(0xFFF1F5F9)),
                        tint = Color.Gray
                    )
                }

                // Camera overlay icon
                Surface(
                    shape = CircleShape,
                    color = Color(0xFF2D6A4F),
                    modifier = Modifier.size(32.dp),
                    border = androidx.compose.foundation.BorderStroke(2.dp, Color.White)
                ) {
                    Icon(
                        Icons.Default.PhotoCamera,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }

            if (uiState.isLoading) {
                Text("Uploading...", fontSize = 12.sp, color = Color(0xFF2D6A4F))
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Form Fields
            Column(verticalArrangement = Arrangement.spacedBy(20.dp), modifier = Modifier.fillMaxWidth()) {
                ProfileSectionTitle("Basic Information")
                OutlinedTextField(
                    value = name, onValueChange = { name = it },
                    label = { Text("Full Name") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = Color.Gray) }
                )

                ProfileSectionTitle("Physical Statistics")
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(value = age, onValueChange = { age = it }, label = { Text("Age") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight (kg)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                    OutlinedTextField(value = height, onValueChange = { height = it }, label = { Text("Height (cm)") }, modifier = Modifier.weight(1f), shape = RoundedCornerShape(16.dp))
                }

                ProfileSectionTitle("Medical Identity")
                OutlinedTextField(
                    value = bloodType, onValueChange = { bloodType = it },
                    label = { Text("Blood Group") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Default.Bloodtype, null, tint = Color(0xFFE63946)) }
                )

                OutlinedTextField(
                    value = allergies, onValueChange = { allergies = it },
                    label = { Text("Allergies") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 2,
                    leadingIcon = { Icon(Icons.Default.Warning, null, tint = Color(0xFFFFB703)) }
                )

                ProfileSectionTitle("Health History")
                OutlinedTextField(
                    value = medicalHistory, onValueChange = { medicalHistory = it },
                    label = { Text("Conditions / Surgeries") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    minLines = 4,
                    leadingIcon = { Icon(Icons.Default.History, null, tint = Color(0xFF2D6A4F)) }
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// THIS WAS MISSING - ADD THIS AT THE BOTTOM OF YOUR FILE

@Composable
fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium.copy(
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2D6A4F)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}