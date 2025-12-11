// File: com/example/healthhive/data/model/User.kt

package com.example.healthhive.data.model

/**
 * Data model for a user document stored in Firestore.
 * The 'id' corresponds to the Firebase Auth UID.
 */
data class User(
    val id: String = "",
    val userName: String = "",
    val email: String = "",
    val registrationTimestamp: Long = System.currentTimeMillis()
    // Add other profile fields here later (e.g., dateOfBirth, gender, weight)
)