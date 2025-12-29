package com.example.healthhive.viewmodel

import android.net.Uri
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

    fun loadUserProfile() {
        val userId = authService.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val document = firestore.collection("users").document(userId).get().await()
                val userData = document.toObject(User::class.java)
                _uiState.update { it.copy(user = userData, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Failed to sync profile.") }
            }
        }
    }

    /**
     * Handles image selection, upload to Storage, and updating Firestore URL.
     */
    fun uploadProfileImage(uri: Uri) {
        val userId = authService.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Upload to Firebase Storage
                val downloadUrl = authService.uploadProfilePicture(userId, uri)

                // 2. Update Firestore with new URL
                val currentUser = _uiState.value.user
                if (currentUser != null) {
                    val updatedUser = currentUser.copy(profilePictureUrl = downloadUrl)
                    firestore.collection("users").document(userId).set(updatedUser).await()

                    // 3. Update local UI state
                    _uiState.update { it.copy(user = updatedUser, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Image upload failed.") }
            }
        }
    }

    fun updateProfile(updatedUser: User, onSuccess: () -> Unit) {
        val uid = authService.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                firestore.collection("users").document(uid).set(updatedUser).await()
                _uiState.update { it.copy(user = updatedUser, isLoading = false) }
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