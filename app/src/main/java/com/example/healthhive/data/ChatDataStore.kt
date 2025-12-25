package com.example.healthhive.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
// IMPORT MODELS
import com.example.healthhive.viewmodel.ChatMessage
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.lang.reflect.Type

// DataStore instance for chat history
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "chat_history")

class ChatDataStore(private val context: Context) {

    private val CHAT_HISTORY_KEY = stringPreferencesKey("chat_messages_json")
    private val gson = Gson()

    // Fixed: Explicitly defining the Type to help Gson and the compiler
    private val typeToken: Type = object : TypeToken<List<ChatMessage>>() {}.type

    /**
     * Loads the chat history as a List<ChatMessage> from DataStore.
     * Returns an empty list if no data exists.
     */
    suspend fun loadHistory(): List<ChatMessage> {
        val json = context.dataStore.data.map { preferences ->
            preferences[CHAT_HISTORY_KEY] ?: "[]"
        }.first()

        return try {
            // Added explicit type check
            val result: List<ChatMessage>? = gson.fromJson(json, typeToken)
            result ?: emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Saves the current List<ChatMessage> to DataStore.
     */
    suspend fun saveHistory(history: List<ChatMessage>) {
        val json = gson.toJson(history)
        context.dataStore.edit { preferences ->
            preferences[CHAT_HISTORY_KEY] = json
        }
    }

    /**
     * Clears all saved chat history.
     */
    suspend fun clearHistory() {
        context.dataStore.edit { preferences ->
            preferences.remove(CHAT_HISTORY_KEY)
        }
    }
}