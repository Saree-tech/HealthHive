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

    fun loadUserProfile() {
        val userId = authService.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val document = firestore.collection("users").document(userId).get().await()
                if (document.exists()) {
                    val userData = document.toObject(User::class.java)
                    _uiState.update { it.copy(user = userData, isLoading = false) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Load failed") }
            }
        }
    }

    fun uploadProfileImage(uri: Uri) {
        val userId = authService.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // 1. Upload to Storage and get Download URL
                val downloadUrl = authService.uploadProfilePicture(userId, uri)

                if (downloadUrl != null) {
                    // 2. Update Firestore immediately so the link is permanent
                    firestore.collection("users").document(userId)
                        .update("profilePictureUrl", downloadUrl).await()

                    // 3. Update local state
                    val updatedUser = _uiState.value.user?.copy(profilePictureUrl = downloadUrl)
                    _uiState.update { it.copy(user = updatedUser, isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "Upload Error: ${e.message}")
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