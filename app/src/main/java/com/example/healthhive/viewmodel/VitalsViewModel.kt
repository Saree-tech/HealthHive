package com.example.healthhive.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.BuildConfig
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
    val aiRecommendation: String = "Lumi is analyzing your health trends...",
    val isLoading: Boolean = false
)

class VitalsViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(VitalsUiState())
    val uiState: StateFlow<VitalsUiState> = _uiState.asStateFlow()

    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private var listenerRegistration: ListenerRegistration? = null

    private val generativeModel = GenerativeModel(
        modelName = "gemini-2.5-flash",
        apiKey = BuildConfig.GEMINI_API_KEY,
        generationConfig = generationConfig { temperature = 0.7f }
    )

    fun startListening(vitalType: String) {
        val userId = auth.currentUser?.uid ?: return
        listenerRegistration?.remove()
        _uiState.update { it.copy(isLoading = true) }

        val collectionRef = firestore.collection("users").document(userId).collection("vitals")
        val query = if (vitalType == "All") {
            collectionRef.orderBy("timestamp", Query.Direction.ASCENDING)
        } else {
            collectionRef.whereEqualTo("type", vitalType).orderBy("timestamp", Query.Direction.ASCENDING)
        }

        listenerRegistration = query.addSnapshotListener { snapshot, error ->
            if (error != null) {
                _uiState.update { it.copy(isLoading = false) }
                return@addSnapshotListener
            }

            val actualHistory = snapshot?.documents?.mapNotNull { doc ->
                val rawValue = doc.get("value")
                val value = when (rawValue) {
                    is Double -> rawValue.toFloat()
                    is Long -> rawValue.toFloat()
                    else -> 0f
                }
                val ts = doc.getLong("timestamp") ?: 0L
                val typeStr = doc.getString("type") ?: ""
                val dateLabel = SimpleDateFormat("dd MMM", Locale.getDefault()).format(Date(ts))
                VitalHistoryEntry(dateLabel, value, ts, typeStr)
            } ?: emptyList()

            _uiState.update { it.copy(history = actualHistory, isLoading = false) }

            if (actualHistory.isNotEmpty()) {
                getGeminiRecommendation(vitalType, actualHistory)
            }
        }
    }

    private fun getGeminiRecommendation(type: String, history: List<VitalHistoryEntry>) {
        viewModelScope.launch {
            val lastEntry = history.last()

            // Branching prompt logic based on selection
            val prompt = if (type == "All") {
                """
                SYSTEM: You are Lumi, a Chief Medical Consultant. 
                CONTEXT: This is an "Overall Health Summary" spanning multiple vital types.
                DATASET: ${history.takeLast(15).joinToString { "${it.type}: ${it.value} (${it.date})" }}
                TASK:
                1. Identify the most critical biometric trend across all logged types.
                2. Provide a detailed, 40-word technical analysis of the user's overall physiological state.
                3. Mention specific readings (e.g., "Your ${history.firstOrNull()?.type} is trending...") 
                4. Conclude with a high-level lifestyle adjustment.
                5. Keep it professional and clinical. DO NOT give a diagnosis.
                """.trimIndent()
            } else {
                """
                SYSTEM: You are Lumi, a precise medical data analyst. 
                USER DATA: The user's current $type is ${lastEntry.value}.
                TASK: 
                1. If the value is dangerously high or low for $type, strongly advise immediate rest.
                2. If the value is normal, give a specific lifestyle tip relevant to $type.
                3. Response MUST be under 20 words.
                4. Start with a direct reaction to ${lastEntry.value}.
                """.trimIndent()
            }

            try {
                val response = generativeModel.generateContent(prompt)
                _uiState.update { it.copy(aiRecommendation = response.text?.trim() ?: "Reviewing trends...") }
            } catch (e: Exception) {
                Log.e("LumiError", "Gemini failed: ${e.message}")
                _uiState.update { it.copy(aiRecommendation = "Lumi is currently observing your health patterns.") }
            }
        }
    }

    fun saveNewEntry(vitalType: String, valueStr: String) {
        val userId = auth.currentUser?.uid ?: return
        val value = valueStr.toFloatOrNull() ?: return
        val data = hashMapOf(
            "type" to vitalType,
            "value" to value,
            "timestamp" to System.currentTimeMillis()
        )
        _uiState.update { it.copy(aiRecommendation = "Lumi is evaluating...") }
        firestore.collection("users").document(userId).collection("vitals").add(data)
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}