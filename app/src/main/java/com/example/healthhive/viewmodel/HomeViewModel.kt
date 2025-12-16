package com.example.healthhive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.AuthService
import com.example.healthhive.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// DEFINITION 1: HomeUiState is defined here
data class HomeUiState(
    val user: User? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

// DEFINITION 2: HomeViewModel class and logic
class HomeViewModel(
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        fetchUserData()
    }

    /**
     * Fetches the user profile (name) from the database.
     */
    fun fetchUserData() {
        _uiState.update { it.copy(isLoading = true, error = null) }

        val currentUserId = authService.getCurrentUser()?.uid

        if (currentUserId == null) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "User not authenticated. Please log in again."
                )
            }
            return
        }

        viewModelScope.launch {
            try {
                val userProfile = authService.getUserData(currentUserId) // Fetch from Firestore
                _uiState.update {
                    it.copy(
                        user = userProfile,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.localizedMessage ?: "Failed to load user profile."
                    )
                }
            }
        }
    }

    // FIX: The Factory definition is correctly placed inside the companion object
    companion object {
        fun Factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                // Instantiate the real AuthService here
                val authService = AuthService()
                return HomeViewModel(authService) as T
            }
        }
    }
}