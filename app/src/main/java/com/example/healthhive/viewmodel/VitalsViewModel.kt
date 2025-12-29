package com.example.healthhive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.QuotaExceededException
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

data class VitalHistoryEntry(
    val date: String,
    val value: Float,
    val timestamp: Long = 0L,
    val type: String = ""
)

data class VitalsUiState(
    val history: List<VitalHistoryEntry> = emptyList(),
    val latestVitals: Map<String, Float> = emptyMap(),
    val aiRecommendation: String = "",
    val healthScore: Float = 0f,
    val isLoading: Boolean = false,
    val isLumiThinking: Boolean = false,
    val errorMessage: String? = null
)

class VitalsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    // Corrected Model Name to a stable version
    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig { temperature = 0.4f }
        )
    }

    /**
     * Listens to Firestore changes. The state is updated only when data is ready,
     * preventing the screen from clearing while new data is being saved.
     */
    fun startListening(vitalType: String) {
        val userId = auth.currentUser?.uid ?: return

        // Clean up existing listener before starting a new one
        listenerRegistration?.remove()

        _uiState.update { it.copy(isLoading = true) }

        val collectionRef = firestore.collection("users").document(userId).collection("vitals")

        val query = if (vitalType == "All") {
            collectionRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
        } else {
            collectionRef.whereEqualTo("type", vitalType)
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(20)
        }

        listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                return@addSnapshotListener
            }

            // Process data in a background thread to keep UI smooth
            viewModelScope.launch(Dispatchers.Default) {
                val entries = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val value = (doc.get("value") as? Number)?.toFloat() ?: 0f
                        val ts = doc.getLong("timestamp") ?: 0L
                        val typeStr = doc.getString("type") ?: ""
                        val dateLabel = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(ts))
                        VitalHistoryEntry(dateLabel, value, ts, typeStr)
                    } catch (e: Exception) {
                        null
                    }
                } ?: emptyList()

                // Sort by time and group to find the "Latest" for each type
                val sortedHistory = entries.sortedByDescending { it.timestamp }
                val latestMap = entries.groupBy { it.type }
                    .mapValues { it.value.maxByOrNull { entry -> entry.timestamp }?.value ?: 0f }

                val updatedScore = calculateHealthScoreFromMap(latestMap)

                // Push all updates to UI thread at once to prevent disappearing elements
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            history = sortedHistory,
                            latestVitals = latestMap,
                            healthScore = updatedScore,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    private fun calculateHealthScoreFromMap(vitalsMap: Map<String, Float>): Float {
        if (vitalsMap.isEmpty()) return 0.0f
        var points = 0f
        vitalsMap.forEach { (type, value) ->
            points += when (type) {
                "Heart Rate" -> if (value in 60f..100f) 1.0f else 0.6f
                "Blood Pressure" -> if (value in 90f..125f) 1.0f else 0.5f
                "Steps" -> if (value >= 8000f) 1.0f else 0.7f
                "Sleep" -> if (value in 7f..9f) 1.0f else 0.6f
                else -> 0.8f
            }
        }
        return (points / vitalsMap.size).coerceIn(0f, 1f)
    }

    fun triggerManualAnalysis(type: String) {
        val currentState = _uiState.value
        if (currentState.history.isEmpty()) return

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isLumiThinking = true) }

            val prompt = if (type == "All") {
                val summary = currentState.latestVitals.entries.joinToString { "${it.key}: ${it.value}" }
                "Health Summary: $summary. Advice in 30 words, no symbols."
            } else {
                val lastVal = currentState.latestVitals[type] ?: 0f
                "User has $type of $lastVal. Provide 15 words of specific advice. No symbols."
            }

            try {
                val response = generativeModel.generateContent(prompt)
                val text = response.text?.replace("*", "")?.trim() ?: "Data stable."
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(aiRecommendation = text, isLumiThinking = false) }
                }
            } catch (e: Exception) {
                val msg = if (e is QuotaExceededException) "Lumi is resting." else "Lumi is busy."
                withContext(Dispatchers.Main) {
                    _uiState.update { it.copy(aiRecommendation = msg, isLumiThinking = false) }
                }
            }
        }
    }

    fun saveNewEntry(type: String, value: String) {
        val userId = auth.currentUser?.uid ?: return
        val valFloat = value.toFloatOrNull() ?: return

        viewModelScope.launch(Dispatchers.IO) {
            val data = hashMapOf(
                "type" to type,
                "value" to valFloat,
                "timestamp" to System.currentTimeMillis()
            )
            firestore.collection("users").document(userId).collection("vitals").add(data)
        }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}