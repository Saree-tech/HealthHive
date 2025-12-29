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
    val isLumiThinking: Boolean = false
)

class VitalsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig { temperature = 0.4f }
        )
    }

    fun startListening(vitalType: String) {
        val userId = auth.currentUser?.uid ?: return
        listenerRegistration?.remove()

        // IMPORTANT: We do NOT clear aiRecommendation here.
        // This allows the previous response to stay visible when the screen opens.
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
                _uiState.update { it.copy(isLoading = false) }
                return@addSnapshotListener
            }

            viewModelScope.launch(Dispatchers.Default) {
                val entries = snapshot?.documents?.mapNotNull { doc ->
                    val value = (doc.get("value") as? Number)?.toFloat() ?: 0f
                    val ts = doc.getLong("timestamp") ?: 0L
                    val typeStr = doc.getString("type") ?: ""
                    val dateLabel = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(ts))
                    VitalHistoryEntry(dateLabel, value, ts, typeStr)
                } ?: emptyList()

                val sorted = entries.sortedBy { it.timestamp }
                val latest = entries.groupBy { it.type }.mapValues { it.value.first().value }
                val updatedScore = calculateHealthScoreFromMap(latest)

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            history = sorted,
                            latestVitals = latest,
                            healthScore = updatedScore,
                            isLoading = false
                        )
                    }
                }
            }
        }
    }

    private fun calculateHealthScoreFromMap(vitalsMap: Map<String, Float>): Float {
        if (vitalsMap.isEmpty()) return 0.5f
        var points = 0f
        var totalTracked = 0

        vitalsMap.forEach { (type, value) ->
            totalTracked++
            points += when (type) {
                "Heart Rate" -> if (value in 60f..100f) 1.0f else 0.6f
                "Blood Pressure" -> if (value in 90f..125f) 1.0f else 0.5f
                "Steps" -> if (value >= 8000f) 1.0f else 0.7f
                "Sleep" -> if (value in 7f..9f) 1.0f else 0.6f
                else -> 0.8f
            }
        }
        return (points / totalTracked).coerceIn(0f, 1f)
    }

    /**
     * AI Request now ONLY triggers when this is called by a button click.
     */
    fun triggerManualAnalysis(type: String) {
        val currentState = _uiState.value
        val history = currentState.history

        if (history.isEmpty()) return

        viewModelScope.launch {
            // Clear only when a NEW request actually starts
            _uiState.update { it.copy(isLumiThinking = true, aiRecommendation = "") }

            val prompt = if (type == "All") {
                val summaryData = currentState.latestVitals.entries.joinToString { "${it.key}: ${it.value}" }
                "Summarize health for these readings: $summaryData. Provide 30 words of advice. No symbols."
            } else {
                val currentVal = history.lastOrNull()?.value ?: 0f
                "The user recorded a $type reading of $currentVal. Provide 15 words of advice specifically for $type. No symbols."
            }

            try {
                val response = generativeModel.generateContent(prompt)
                val cleanText = response.text?.replace("*", "")?.trim() ?: "Data is stable."
                _uiState.update { it.copy(aiRecommendation = cleanText, isLumiThinking = false) }
            } catch (e: Exception) {
                val errorMsg = if (e is QuotaExceededException) "Lumi is resting. Retry in 1 min." else "Lumi is busy. Try again soon."
                _uiState.update { it.copy(aiRecommendation = errorMsg, isLumiThinking = false) }
            }
        }
    }

    fun saveNewEntry(type: String, value: String) {
        val userId = auth.currentUser?.uid ?: return
        val valFloat = value.toFloatOrNull() ?: return

        val data = hashMapOf(
            "type" to type,
            "value" to valFloat,
            "timestamp" to System.currentTimeMillis()
        )

        // Only saves to Firestore. Does NOT trigger AI anymore.
        firestore.collection("users").document(userId).collection("vitals").add(data)
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}