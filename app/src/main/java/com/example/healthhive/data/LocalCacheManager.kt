package com.example.healthhive.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_settings")

class LocalCacheManager(private val context: Context) {
    private val MOOD_KEY = stringPreferencesKey("daily_mood")

    suspend fun saveMood(mood: String) {
        context.dataStore.edit { it[MOOD_KEY] = mood }
    }

    val getMood: Flow<String?> = context.dataStore.data.map { it[MOOD_KEY] }
}