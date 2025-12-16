package com.example.healthhive.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.AIService
import com.example.healthhive.data.ChatDataStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SymptomCheckerViewModel(
    private val aiService: AIService,
    private val dataStore: ChatDataStore // Dependency Injection of DataStore
) : ViewModel() {

    private val _uiState = MutableStateFlow(SymptomCheckerUiState())
    val uiState: StateFlow<SymptomCheckerUiState> = _uiState.asStateFlow()

    init {
        // Load messages from DataStore on startup
        loadChatHistory()
    }

    fun getAssistantName(): String = aiService.getAssistantName()

    fun updateMessageInput(input: String) {
        _uiState.update { it.copy(messageInput = input) }
    }

    private fun loadChatHistory() {
        viewModelScope.launch {
            val loadedHistory = dataStore.loadHistory()

            // If history is empty, add the initial welcome message
            val finalHistory = if (loadedHistory.isEmpty()) {
                listOf(
                    ChatMessage(
                        text = "Hey! I'm ${getAssistantName()}, your AI Health Assistant. What symptoms are you experiencing today?",
                        isUser = false
                    )
                )
            } else {
                loadedHistory
            }

            _uiState.update { it.copy(chatHistory = finalHistory) }
        }
    }

    fun sendMessage(userMessage: String) {
        val trimmedMessage = userMessage.trim()
        if (trimmedMessage.isBlank() || _uiState.value.isLoading) return

        // 1. Add user message to history
        val userChatMessage = ChatMessage(text = trimmedMessage, isUser = true)
        val historyBeforeSend = _uiState.value.chatHistory + userChatMessage

        _uiState.update {
            it.copy(
                chatHistory = historyBeforeSend,
                messageInput = "",
                isLoading = true,
                error = null
            )
        }

        viewModelScope.launch {
            try {
                // 2. Fix Robotic Repetition: Send the full history for context
                val chatContext = historyBeforeSend.map {
                    if (it.isUser) "User: ${it.text}" else "${getAssistantName()}: ${it.text}"
                }.joinToString(separator = "\n")

                val fullPrompt = "The following is our chat history. Please respond to the last user message, maintaining a friendly tone but being informative:\n\n$chatContext"


                val response = aiService.sendMessage(fullPrompt)

                // 3. Add AI response to history and save
                val assistantChatMessage = ChatMessage(text = response, isUser = false)
                _uiState.update { currentState ->
                    val finalHistory = currentState.chatHistory + assistantChatMessage

                    viewModelScope.launch { dataStore.saveHistory(finalHistory) }

                    currentState.copy(
                        chatHistory = finalHistory,
                        isLoading = false,
                        error = null,
                        isAnalysisComplete = true
                    )
                }
            } catch (e: Exception) {
                // 4. Handle error response and save history
                _uiState.update { currentState ->
                    val errorText = "Failed to connect to the AI model. Please check your internet connection or try again later. Error: ${e.localizedMessage}"
                    val errorChatMessage = ChatMessage(text = errorText, isUser = false)
                    val finalHistory = currentState.chatHistory + errorChatMessage

                    viewModelScope.launch { dataStore.saveHistory(finalHistory) }

                    currentState.copy(
                        chatHistory = finalHistory,
                        isLoading = false,
                        error = errorText
                    )
                }
            }
        }
    }

    fun resetAnalysis() {
        viewModelScope.launch {
            aiService.resetSession()
            dataStore.clearHistory() // Clear DataStore
            loadChatHistory() // Reload the initial welcome message

            _uiState.update {
                it.copy(
                    error = null,
                    isAnalysisComplete = false,
                    messageInput = ""
                )
            }
        }
    }

    // Factory for ViewModel (required because we now pass 'Context' to the constructor)
    companion object {
        fun Factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val dataStore = ChatDataStore(context)
                val aiService = AIService() // AIService is still created here
                return SymptomCheckerViewModel(aiService, dataStore) as T
            }
        }
    }
}