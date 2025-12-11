// File: com/example/healthhive/viewmodel/HomeViewModel.kt

package com.example.healthhive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.AuthService
import com.example.healthhive.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(
    private val authService: AuthService = AuthService()
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState

    init {
        fetchUserData()
    }

    /**
     * Fetches the user profile (name) from the database.
     */
    fun fetchUserData() {
        _uiState.value = _uiState.value.copy(isLoading = true, error = null)

        val currentUserId = authService.getCurrentUser()?.uid

        if (currentUserId == null) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                error = "User not authenticated. Please log in again."
            )
            // Log user out or force navigation to login if state is bad
            return
        }

        viewModelScope.launch {
            try {
                val userProfile = authService.getUserData(currentUserId) // <-- Fetch from Firestore
                _uiState.value = _uiState.value.copy(
                    user = userProfile,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Failed to load user profile."
                )
            }
        }
    }
}