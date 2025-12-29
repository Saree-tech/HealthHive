// File: com/example/healthhive/data/AuthService.kt
package com.example.healthhive.data

import android.net.Uri
import com.example.healthhive.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await

class AuthService {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val storage: FirebaseStorage = FirebaseStorage.getInstance()
    private val usersCollection = firestore.collection("users")

    suspend fun login(email: String, password: String): FirebaseUser {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user ?: throw Exception("Login failed: User object is null.")
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun signup(email: String, password: String, userName: String): FirebaseUser {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Signup failed.")

            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            val newUser = User(
                id = firebaseUser.uid,
                userName = userName,
                email = email,
                registrationTimestamp = System.currentTimeMillis()
            )

            usersCollection.document(firebaseUser.uid).set(newUser).await()
            firebaseUser
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * NEW/CRITICAL: Updates existing user data in Firestore.
     * This is what your EditProfile screen needs to call.
     */
    suspend fun updateUserProfile(userId: String, updates: Map<String, Any>) {
        try {
            usersCollection.document(userId).update(updates).await()

            // If the name was updated, also update Firebase Auth internal profile
            if (updates.containsKey("userName")) {
                val profileUpdates = UserProfileChangeRequest.Builder()
                    .setDisplayName(updates["userName"] as String)
                    .build()
                auth.currentUser?.updateProfile(profileUpdates)?.await()
            }
        } catch (e: Exception) {
            throw Exception("Failed to update profile: ${e.localizedMessage}")
        }
    }

    suspend fun getUserData(userId: String): User {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            if (!snapshot.exists()) {
                throw Exception("User profile document not found.")
            }
            snapshot.toObject(User::class.java) ?: throw Exception("Failed to parse User.")
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): String {
        return try {
            val storageRef = storage.reference
                .child("profile_pictures")
                .child("$userId.jpg")

            storageRef.putFile(imageUri).await()
            val downloadUrl = storageRef.downloadUrl.await().toString()

            // Logic Check: After uploading to storage, we MUST update the Firestore URL
            updateUserProfile(userId, mapOf("profilePictureUrl" to downloadUrl))

            downloadUrl
        } catch (e: Exception) {
            throw Exception("Storage error: ${e.localizedMessage}")
        }
    }

    suspend fun sendPasswordResetEmail(email: String) {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            throw e
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun signOut() {
        auth.signOut()
    }
}