package com.example.healthhive.viewmodel

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.AuthService
import com.example.healthhive.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class ProfileUiState(
    val user: User? = null,
    val isLoading: Boolean = false,
    val isLoggedOut: Boolean = false,
    val errorMessage: String? = null
)

class ProfileViewModel(
    private val authService: AuthService = AuthService(),
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUserProfile()
    }

    /**
     * Changed to a SnapshotListener so the Profile screen updates
     * immediately when Home or Edit changes the data.
     */
    fun loadUserProfile() {
        val userId = authService.getCurrentUser()?.uid ?: return
        firestore.collection("users").document(userId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(errorMessage = "Sync failed") }
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    val userData = snapshot.toObject(User::class.java)
                    _uiState.update { it.copy(user = userData, isLoading = false) }
                }
            }
    }

    fun uploadProfileImage(uri: Uri) {
        val userId = authService.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // AuthService already handles the Firestore update in our previous fix
                authService.uploadProfilePicture(userId, uri)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }

    /**
     * FIX: Use update() with a Map to prevent overwriting other fields
     * like profilePictureUrl or registrationTimestamp.
     */
    // Inside ProfileViewModel.kt
    fun updateProfile(
        name: String,
        age: String,
        weight: String,
        height: String,
        bloodType: String,
        allergies: String,
        history: String,
        onSuccess: () -> Unit
    ) {
        val uid = authService.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val updates = mapOf(
                    "userName" to name,
                    "age" to age,
                    "weight" to weight,
                    "height" to height,
                    "bloodType" to bloodType,
                    "allergies" to allergies,
                    "medicalHistory" to history
                )
                authService.updateUserProfile(uid, updates)
                _uiState.update { it.copy(isLoading = false) }
                onSuccess()
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Update failed.") }
            }
        }
    }

    fun logout() {
        authService.signOut()
        _uiState.update { it.copy(isLoggedOut = true) }
    }
}