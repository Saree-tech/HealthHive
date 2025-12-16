package com.example.healthhive.data

import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.Chat
import com.example.healthhive.BuildConfig

// This class handles all communication with the Gemini API.
class AIService {

    // 1. Assign a name to your assistant (for personality)
    private val assistantName = "Lumi"

    // 2. Define a much more detailed and empathetic system instruction
    private val systemInstructionText = """
        You are $assistantName, a beacon of clarity and support—an advanced, specialized, non-diagnostic AI health assistant. Your purpose is to illuminate the path to informed health decisions.
        
        **Your Personality & Tone (Aligned with the 'Lumi' Theme):**
        * Your tone must be **calm, guiding, and reassuring**. You are a light in uncertainty.
        * Be highly professional and empathetic. Avoid robotic language.
        * **Format your output beautifully** using clear headings and bullet points for easy reading, matching the clean UI aesthetic.
        
        **Your Core Safety Rules (Non-Negotiable):**
        * You are NOT a doctor. You CANNOT provide a diagnosis or prescription. State this clearly.
        * Prioritize patient safety above all else.
        * If symptoms indicate a severe or immediate danger (chest pain, severe bleeding, sudden vision loss, signs of stroke/heart attack), interrupt your analysis and instruct the user to call emergency services immediately. 
        
        **Your Analysis Requirements:**
        1.  **Potential Causes:** Provide 2-3 most probable, non-severe potential causes.
        2.  **Severity Assessment:** Categorize the severity as "Low," "Moderate," or "High" based on the described symptoms.
        3.  **Next Steps (Actionable Advice):**
            * For Low/Moderate Severity: Offer simple, safe home care suggestions (rest, hydration) AND pose 1-2 clarifying questions to narrow down the possible cause (e.g., "Does the pain radiate anywhere else?").
            * For High Severity: Strongly and immediately recommend seeing a doctor or visiting an urgent care center.
        4.  **Final Disclaimer:** Every response must end with a strong recommendation to consult a human healthcare professional for a definitive diagnosis.
    """.trimIndent()

    // 3. Chat variable initialized via lazy delegate to ensure it's created correctly
    private lateinit var chat: Chat

    init {
        // Initialize the chat session when the AIService is first created
        initializeChatSession()
    }

    private fun initializeChatSession() {
        chat = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = BuildConfig.GEMINI_API_KEY,
            systemInstruction = content { text(systemInstructionText) }
        ).startChat()
    }


    /**
     * Sends a message to the Gemini chat session.
     */
    suspend fun sendMessage(userMessage: String): String {

        if (BuildConfig.GEMINI_API_KEY.isEmpty() || BuildConfig.GEMINI_API_KEY == "null") {
            return "ERROR: Gemini API Key is missing. Please check local.properties and app/build.gradle."
        }

        // Use the chat.sendMessage method for conversational memory
        return try {
            val response = chat.sendMessage(userMessage)

            // Return the full text response from the assistant
            response.text ?: "Analysis could not be generated."

        } catch (e: Exception) {
            "Failed to connect to the AI model. Error: ${e.localizedMessage}"
        }
    }

    /**
     * Public method to reset the chat session (called by ViewModel on new symptom check).
     */
    fun resetSession() {
        initializeChatSession()
    }

    fun getAssistantName(): String = assistantName
}