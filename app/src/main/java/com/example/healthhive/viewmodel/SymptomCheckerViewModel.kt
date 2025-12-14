package com.example.healthhive.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.AIService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SymptomCheckerViewModel(
    private val aiService: AIService = AIService()
) : ViewModel() {

    // Internal state flow for mutation
    private val _uiState = MutableStateFlow(SymptomCheckerUiState())
    // Public immutable state flow for the UI to collect
    val uiState: StateFlow<SymptomCheckerUiState> = _uiState.asStateFlow()

    fun getAssistantName(): String = aiService.getAssistantName()

    fun updateSymptomInput(input: String) {
        _uiState.update { it.copy(symptomInput = input) }
    }

    fun addSymptom(symptom: String) {
        if (symptom.isNotBlank()) {
            _uiState.update {
                it.copy(
                    selectedSymptoms = it.selectedSymptoms + symptom.trim(),
                    symptomInput = ""
                )
            }
        }
    }

    fun removeSymptom(symptom: String) {
        _uiState.update {
            it.copy(selectedSymptoms = it.selectedSymptoms.filter { s -> s != symptom })
        }
    }

    fun analyzeSymptoms() {
        // Ensure we don't proceed if already loading or if no symptoms are entered
        if (_uiState.value.isLoading || _uiState.value.selectedSymptoms.isEmpty()) return

        _uiState.update {
            it.copy(
                isLoading = true,
                error = null,
                isInitialAnalysis = false // Analysis has begun
            )
        }

        val prompt = "Analyze the following list of symptoms: ${_uiState.value.selectedSymptoms.joinToString(", ")}"

        viewModelScope.launch {
            try {
                val response = aiService.sendMessage(prompt)
                _uiState.update {
                    it.copy(
                        result = response,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        result = null,
                        isLoading = false,
                        error = "Analysis failed due to a network or API error: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun resetAnalysis() {
        aiService.resetSession()
        _uiState.update {
            SymptomCheckerUiState(isInitialAnalysis = true) // Reset to initial state
        }
    }
}