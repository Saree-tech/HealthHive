// File: com.example.healthhive.viewmodel.HomeViewModel.kt
package com.example.healthhive.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.AuthService
import com.example.healthhive.data.LocalCacheManager
import com.example.healthhive.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Data class representing the state of the Home Screen.
 * Includes [selectedMood] to power the new Mood Handler UI.
 */
data class HomeUiState(
    val user: User? = null,
    val vitalsList: List<VitalCardData> = emptyList(),
    val healthScore: Float = 0f,
    val selectedMood: String? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)

data class VitalCardData(
    val type: String,
    val value: String,
    val unit: String,
    val color: Long,
    val iconName: String
)

class HomeViewModel(
    application: Application,
    private val authService: AuthService
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private val cacheManager = LocalCacheManager(application)

    init {
        refreshDashboard()
        listenToLiveVitals()
        observeCachedMood()
    }

    /**
     * Loads the saved mood from DataStore/LocalCache on initialization.
     */
    private fun observeCachedMood() {
        viewModelScope.launch {
            cacheManager.getMood.collect { mood ->
                _uiState.update { it.copy(selectedMood = mood) }
            }
        }
    }

    /**
     * Updates the user's mood both in the UI state and persistent local cache.
     */
    fun updateMood(mood: String) {
        viewModelScope.launch {
            // Immediate UI feedback (MVI Pattern)
            _uiState.update { it.copy(selectedMood = mood) }
            // Persistent storage
            cacheManager.saveMood(mood)
        }
    }

    private fun refreshDashboard() {
        val currentUserId = authService.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            try {
                val userProfile = authService.getUserData(currentUserId)
                _uiState.update { it.copy(user = userProfile, isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage, isLoading = false) }
            }
        }
    }

    private fun listenToLiveVitals() {
        val userId = authService.getCurrentUser()?.uid ?: return

        firestore.collection("users").document(userId).collection("vitals")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(error = error.localizedMessage) }
                    return@addSnapshotListener
                }

                val rawList = snapshot?.documents?.mapNotNull { doc ->
                    val type = doc.getString("type") ?: ""
                    val value = doc.get("value")
                    val valueFloat = when (value) {
                        is Double -> value.toFloat()
                        is Long -> value.toFloat()
                        is String -> value.toFloatOrNull()
                        else -> null
                    }
                    type to valueFloat
                } ?: emptyList()

                val latestUnique = rawList.distinctBy { it.first }
                val mappedVitals = latestUnique.map { (type, value) ->
                    mapToVitalCard(type, value ?: 0f)
                }

                _uiState.update {
                    it.copy(
                        vitalsList = mappedVitals,
                        healthScore = calculateHealthScore(latestUnique),
                        isLoading = false
                    )
                }
            }
    }

    private fun mapToVitalCard(type: String, value: Float): VitalCardData {
        return when (type) {
            "Heart Rate" -> VitalCardData(type, value.toInt().toString(), "bpm", 0xFF2D6A4F, "heart")
            "Blood Pressure" -> VitalCardData(type, value.toInt().toString(), "mmHg", 0xFF2D6A4F, "bp")
            "Steps" -> VitalCardData(type, String.format("%,d", value.toInt()), "steps", 0xFF2D6A4F, "steps")
            else -> VitalCardData(type, value.toString(), "", 0xFF2D6A4F, "monitor")
        }
    }

    private fun calculateHealthScore(vitals: List<Pair<String, Float?>>): Float {
        if (vitals.isEmpty()) return 0.5f // Default "neutral" score
        var points = 0f
        vitals.forEach { (type, value) ->
            if (value == null) return@forEach
            points += when (type) {
                "Heart Rate" -> if (value in 60f..100f) 1f else 0.6f
                "Blood Pressure" -> if (value in 90f..125f) 1f else 0.5f
                else -> 0.8f
            }
        }
        return (points / vitals.size).coerceIn(0f, 1f)
    }

    /**
     * Corrected Factory to ensure [Application] is passed properly from MainActivity/HomeScreen.
     */
    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(app, AuthService()) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}