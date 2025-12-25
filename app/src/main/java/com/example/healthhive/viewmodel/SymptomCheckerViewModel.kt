package com.example.healthhive.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.healthhive.data.AIService
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.PropertyName
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// --- MODELS ---
data class ChatSession(
    val id: String = "",
    val userId: String = "",
    val title: String = "New Chat",
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * FIXED: ChatMessage now explicitly handles the "user" field found in your Firestore logs.
 * We use @get:PropertyName and @set:PropertyName to ensure Firestore can read/write
 * the data even if the field names are slightly different.
 */
data class ChatMessage(
    val text: String = "",

    @get:PropertyName("isUser")
    @set:PropertyName("isUser")
    var isUser: Boolean = true,

    @get:PropertyName("user")
    @set:PropertyName("user")
    var user: String = "", // Matches the Firestore "user" field in your log

    val timestamp: Long = System.currentTimeMillis()
) {
    // Required empty constructor for Firestore's toObject() method
    constructor() : this("", true, "", System.currentTimeMillis())
}

data class SymptomCheckerUiState(
    val messageInput: String = "",
    val chatHistory: List<ChatMessage> = emptyList(),
    val activeSessionId: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val isAnalysisComplete: Boolean = false
)

// --- VIEWMODEL ---
class SymptomCheckerViewModel(
    private val aiService: AIService = AIService()
) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val _uiState = MutableStateFlow(SymptomCheckerUiState())
    val uiState: StateFlow<SymptomCheckerUiState> = _uiState.asStateFlow()

    private var messageListener: ListenerRegistration? = null

    fun getAssistantName(): String = aiService.getAssistantName()

    fun updateMessageInput(input: String) {
        _uiState.update { it.copy(messageInput = input) }
    }

    fun loadSession(sessionId: String) {
        val uid = auth.currentUser?.uid ?: return
        messageListener?.remove()

        _uiState.update { it.copy(activeSessionId = sessionId, isLoading = true) }

        messageListener = db.collection("users").document(uid)
            .collection("sessions").document(sessionId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.update { it.copy(error = "Sync error: ${error.localizedMessage}", isLoading = false) }
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val messages = snapshot.toObjects(ChatMessage::class.java)
                    _uiState.update {
                        it.copy(
                            chatHistory = messages,
                            isLoading = false,
                            isAnalysisComplete = messages.isNotEmpty()
                        )
                    }
                }
            }
    }

    fun resetAnalysis() {
        messageListener?.remove()
        messageListener = null
        aiService.resetSession()

        _uiState.update {
            it.copy(
                activeSessionId = null,
                chatHistory = emptyList(),
                messageInput = "",
                isLoading = false,
                isAnalysisComplete = false,
                error = null
            )
        }
    }

    fun sendMessage(userMessage: String) {
        val trimmedMessage = userMessage.trim()
        val currentUser = auth.currentUser
        val uid = currentUser?.uid ?: return

        if (trimmedMessage.isBlank() || _uiState.value.isLoading) return

        val sid = _uiState.value.activeSessionId ?: createNewSession(trimmedMessage)

        // Populate 'user' with the email/ID to satisfy Firestore's expectation
        val userChatMessage = ChatMessage(
            text = trimmedMessage,
            isUser = true,
            user = currentUser.email ?: "User"
        )

        saveMessageToFirestore(sid, userChatMessage)

        _uiState.update { it.copy(messageInput = "", isLoading = true, error = null) }

        viewModelScope.launch {
            try {
                // Take only the last few messages for context to keep the prompt clean
                val chatContext = _uiState.value.chatHistory.takeLast(10).joinToString("\n") {
                    if (it.isUser) "User: ${it.text}" else "Lumi: ${it.text}"
                }

                val response = aiService.sendMessage("$chatContext\nUser: $trimmedMessage")

                val assistantChatMessage = ChatMessage(
                    text = response,
                    isUser = false,
                    user = "Lumi"
                )

                saveMessageToFirestore(sid, assistantChatMessage)
                _uiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, error = "Lumi failed: ${e.localizedMessage}") }
            }
        }
    }

    private fun createNewSession(firstMessage: String): String {
        val uid = auth.currentUser?.uid ?: ""
        val sessionRef = db.collection("users").document(uid).collection("sessions").document()

        val newSession = ChatSession(
            id = sessionRef.id,
            userId = uid,
            title = if (firstMessage.length > 25) firstMessage.take(22) + "..." else firstMessage,
            timestamp = System.currentTimeMillis()
        )

        sessionRef.set(newSession)
        _uiState.update { it.copy(activeSessionId = sessionRef.id) }
        loadSession(sessionRef.id)

        return sessionRef.id
    }

    fun deleteSession(sessionId: String) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid).collection("sessions").document(sessionId).delete()
        if (_uiState.value.activeSessionId == sessionId) {
            resetAnalysis()
        }
    }

    private fun saveMessageToFirestore(sid: String, message: ChatMessage) {
        val uid = auth.currentUser?.uid ?: return
        db.collection("users").document(uid)
            .collection("sessions").document(sid)
            .collection("messages").add(message)
    }

    override fun onCleared() {
        super.onCleared()
        messageListener?.remove()
    }

    companion object {
        fun Factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return SymptomCheckerViewModel() as T
            }
        }
    }
}