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
            modelName = "gemini-1.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            generationConfig = generationConfig { temperature = 0.4f }
        )
    }

    fun startListening(vitalType: String) {
        val userId = auth.currentUser?.uid ?: return
        listenerRegistration?.remove()
        _uiState.update { it.copy(isLoading = true, aiRecommendation = "") }

        val collectionRef = firestore.collection("users").document(userId).collection("vitals")
        val query = if (vitalType == "All") {
            collectionRef.orderBy("timestamp", Query.Direction.DESCENDING).limit(50)
        } else {
            collectionRef.whereEqualTo("type", vitalType)
                .orderBy("timestamp", Query.Direction.DESCENDING).limit(20)
        }

        listenerRegistration = query.addSnapshotListener { snapshot, _ ->
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

                val score = if (latest.isEmpty()) 0.70f else {
                    val avgNormalized = latest.values.map { (it / 150f).coerceIn(0f, 1f) }.average()
                    avgNormalized.toFloat()
                }

                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(history = sorted, latestVitals = latest, healthScore = score, isLoading = false)
                    }
                }
            }
        }
    }

    fun triggerManualAnalysis(type: String) {
        val history = _uiState.value.history
        if (history.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLumiThinking = true) }
            val prompt = if (type == "All") {
                "Recent vitals: ${history.takeLast(10).joinToString { "${it.type}:${it.value}" }}. Provide exactly 30 words of advice. No symbols."
            } else {
                "Vital recorded: $type at ${history.last().value}. Provide 15 simple words of advice. No symbols."
            }

            try {
                val response = generativeModel.generateContent(prompt)
                val cleanText = response.text?.replace("*", "")?.trim() ?: "Trends are stable."
                _uiState.update { it.copy(aiRecommendation = cleanText, isLumiThinking = false) }
            } catch (e: QuotaExceededException) {
                _uiState.update {
                    it.copy(aiRecommendation = "Lumi is resting. Please retry in 1 minute.", isLumiThinking = false)
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(aiRecommendation = "Lumi is busy. Try again soon.", isLumiThinking = false) }
            }
        }
    }

    fun saveNewEntry(type: String, value: String) {
        val userId = auth.currentUser?.uid ?: return
        val valFloat = value.toFloatOrNull() ?: return
        val data = hashMapOf("type" to type, "value" to valFloat, "timestamp" to System.currentTimeMillis())

        firestore.collection("users").document(userId).collection("vitals").add(data)
            .addOnSuccessListener { triggerManualAnalysis(type) }
    }

    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }
}