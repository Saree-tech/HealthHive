package com.example.healthhive.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.Chat
import com.example.healthhive.BuildConfig
import android.util.Log
import kotlinx.serialization.SerializationException

class AIService {

    private val assistantName = "Lumi"

    private val systemInstructionText = """
        You are $assistantName, a friendly and intuitive AI health companion. Your vibe is calm, supportive, and clear.
        
        **Tone and Voice:**
        - Be empathetic. Acknowledge pain or discomfort personally.
        - Keep it light. Avoid overly clinical jargon.
        
        **Safety & Disclaimers:**
        - DO NOT repeat "I am not a doctor" in every message.
        - State your non-diagnostic nature ONLY once at the very beginning of a session.
        - If symptoms sound dangerous (chest pain, difficulty breathing), tell them to call emergency services immediately.
        
        **Formatting:**
        - Use **bold text** for key points.
        - Use bullet points (-) for lists.
        - Keep paragraphs short for mobile.
    """.trimIndent()

    private var chat: Chat? = null

    init {
        initializeChatSession()
    }

    private fun initializeChatSession() {
        try {
            val apiKey = BuildConfig.GEMINI_API_KEY
            if (apiKey.isEmpty() || apiKey == "null") {
                Log.e("AIService", "GEMINI_API_KEY is missing")
                return
            }

            // "gemini-1.5-flash" is the most stable string for AI Studio keys.
            val model = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = apiKey,
                systemInstruction = content { text(systemInstructionText) }
            )
            chat = model.startChat()
            Log.d("AIService", "Chat session initialized with gemini-1.5-flash")
        } catch (e: Exception) {
            Log.e("AIService", "Initialization failed: ${e.message}", e)
        }
    }

    suspend fun sendMessage(userMessage: String): String {
        if (BuildConfig.GEMINI_API_KEY.isEmpty() || BuildConfig.GEMINI_API_KEY == "null") {
            return "Lumi is offline: API Key missing."
        }

        if (chat == null) initializeChatSession()

        return try {
            val response = chat?.sendMessage(userMessage)
            response?.text ?: "I'm listening, but I couldn't quite process that. Could you try again?"
        } catch (e: Exception) {
            val errorMsg = e.localizedMessage ?: ""
            Log.e("AIService", "Error during sendMessage: $errorMsg", e)

            when {
                // This specifically catches the 'details' field error from your Logcat
                e is SerializationException || errorMsg.contains("MissingFieldException") -> {
                    "Lumi had a synchronization error with the server. Please try again."
                }
                errorMsg.contains("404") -> {
                    "Model not found. Ensure your API Key from AI Studio is valid and has access to Gemini 1.5 Flash."
                }
                errorMsg.contains("safety") -> {
                    "I cannot discuss that for safety reasons. Please consult a professional."
                }
                else -> {
                    "Connection issue: ${e.javaClass.simpleName}. Please check your internet."
                }
            }
        }
    }

    fun resetSession() {
        initializeChatSession()
    }

    fun getAssistantName(): String = assistantName
}