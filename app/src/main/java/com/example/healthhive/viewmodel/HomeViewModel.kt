package com.example.healthhive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.AuthService
import com.example.healthhive.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VitalCardData(
    val type: String,
    val value: String,
    val unit: String,
    val color: Long,
    val iconName: String
)

data class HomeUiState(
    val user: User? = null,
    val vitalsList: List<VitalCardData> = emptyList(),
    val healthScore: Float = 0f, // 0.0 to 1.0
    val isLoading: Boolean = true,
    val error: String? = null
)

class HomeViewModel(
    private val authService: AuthService
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    private val firestore = FirebaseFirestore.getInstance()

    init {
        refreshDashboard()
        listenToLiveVitals()
    }

    private fun refreshDashboard() {
        val currentUserId = authService.getCurrentUser()?.uid ?: return
        viewModelScope.launch {
            try {
                val userProfile = authService.getUserData(currentUserId)
                _uiState.update { it.copy(user = userProfile) }
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    private fun listenToLiveVitals() {
        val userId = authService.getCurrentUser()?.uid ?: return

        // Listen for the most recent entries in the vitals collection
        firestore.collection("users").document(userId).collection("vitals")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .limit(10)
            .addSnapshotListener { snapshot, error ->
                if (error != null) return@addSnapshotListener

                val rawList = snapshot?.documents?.mapNotNull { doc ->
                    val type = doc.getString("type") ?: ""
                    val value = doc.get("value").toString()
                    val ts = doc.getLong("timestamp") ?: 0L
                    type to value.toFloatOrNull()
                } ?: emptyList()

                // Filter to get only the LATEST unique vital types for the dashboard
                val latestMap = rawList.distinctBy { it.first }

                val mappedVitals = latestMap.map { (type, value) ->
                    mapToVitalCard(type, value ?: 0f)
                }

                val score = calculateHealthScore(latestMap)

                _uiState.update {
                    it.copy(
                        vitalsList = mappedVitals,
                        healthScore = score,
                        isLoading = false
                    )
                }
            }
    }

    private fun mapToVitalCard(type: String, value: Float): VitalCardData {
        return when (type) {
            "Heart Rate" -> VitalCardData(type, value.toInt().toString(), "bpm", 0xFFFCE4EC, "heart")
            "Blood Pressure" -> VitalCardData(type, value.toInt().toString(), "mmHg", 0xFFE0F2F1, "heart")
            "Steps" -> VitalCardData(type, String.format("%,d", value.toInt()), "steps", 0xFFE8F5E9, "steps")
            else -> VitalCardData(type, value.toString(), "", 0xFFF5F5F5, "monitor")
        }
    }

    private fun calculateHealthScore(vitals: List<Pair<String, Float?>>): Float {
        if (vitals.isEmpty()) return 0.5f
        var points = 0f
        var totalTracked = 0

        vitals.forEach { (type, value) ->
            if (value == null) return@forEach
            totalTracked++
            points += when (type) {
                "Heart Rate" -> if (value in 60f..100f) 1f else 0.6f
                "Blood Pressure" -> if (value in 90f..125f) 1f else 0.5f
                else -> 0.8f // Default contribution for other logs
            }
        }
        return (points / totalTracked).coerceIn(0f, 1f)
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