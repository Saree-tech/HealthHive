package com.example.healthhive.viewmodel

/**
 * Represents a single message in the chat history.
 */
data class ChatMessage(
    val id: Long = System.currentTimeMillis(),
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)