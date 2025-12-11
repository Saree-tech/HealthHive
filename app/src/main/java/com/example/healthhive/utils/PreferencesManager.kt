// File: com/example/healthhive/utils/PreferencesManager.kt

package com.example.healthhive.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// DataStore instance
// We define it outside the class, at the top level of the file, 
// using the property delegate provided by the androidx.datastore.preferences package.
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferencesManager(private val context: Context) {

    // Key to store the onboarding status
    private val ONBOARDING_KEY = booleanPreferencesKey("onboarding_complete")

    /**
     * Reads the onboarding status from DataStore.
     * @return Flow<Boolean> which is true if onboarding has been completed.
     */
    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data
        .map { preferences ->
            preferences[ONBOARDING_KEY] ?: false
        }

    /**
     * Sets the onboarding status to true.
     */
    suspend fun setOnboardingComplete(completed: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[ONBOARDING_KEY] = completed
        }
    }
}