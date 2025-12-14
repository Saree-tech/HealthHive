package com.example.healthhive.viewmodel

data class SymptomCheckerUiState(
    val symptomInput: String = "",
    val selectedSymptoms: List<String> = emptyList(), // Used for chip display
    val result: String? = null,                       // The AI's final analysis
    val isInitialAnalysis: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null                         // For displaying connection or API errors
)