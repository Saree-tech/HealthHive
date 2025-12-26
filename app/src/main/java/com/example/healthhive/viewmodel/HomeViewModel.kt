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

data class VitalCardData(
    val type: String,
    val value: String,
    val unit: String,
    val trend: String,
    val color: Long,
    val iconName: String
)

data class HomeUiState(
    val user: User? = null,
    val vitalsList: List<VitalCardData> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init { refreshDashboard() }

    fun refreshDashboard() {
        val currentUserId = authService.getCurrentUser()?.uid
        if (currentUserId == null) {
            _uiState.update { it.copy(isLoading = false, error = "Session expired.") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                val userProfile = authService.getUserData(currentUserId)
                val latestVitals = fetchVitalsFromSource(currentUserId)
                _uiState.update {
                    it.copy(user = userProfile, vitalsList = latestVitals, isLoading = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = e.localizedMessage) }
            }
        }
    }

    private fun fetchVitalsFromSource(userId: String): List<VitalCardData> {
        return listOf(
            VitalCardData("Heart Rate", "74", "bpm", "Normal", 0xFFFCE4EC, "heart"),
            VitalCardData("Steps", "8,432", "steps", "+12%", 0xFFE8F5E9, "steps"),
            VitalCardData("Sleep", "7h 20m", "quality", "Good", 0xFFE8EAF6, "sleep")
        )
    }

    companion object {
        fun Factory(): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(AuthService()) as T
            }
        }
    }
}