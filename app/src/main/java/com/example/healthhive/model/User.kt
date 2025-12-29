package com.example.healthhive.data.model

/**
 * Merged Data model for a user document stored in Firestore.
 * IMPORTANT: Field names must match Firestore keys exactly.
 */
data class User(
    val id: String = "",
    val userName: String = "",
    val email: String = "",
    val registrationTimestamp: Long = System.currentTimeMillis(),
    val age: String = "",
    val bloodType: String = "",
    val allergies: String = "",
    val medicalHistory: String = "",
    val weight: String = "",
    val height: String = "",
    val profilePictureUrl: String = ""
)