package com.example.healthhive.viewmodel

data class SymptomCheckerUiState(
    val messageInput: String = "", // Current text being typed by the user
    val chatHistory: List<ChatMessage> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAnalysisComplete: Boolean = false // Flag to show if an analysis has been done
)