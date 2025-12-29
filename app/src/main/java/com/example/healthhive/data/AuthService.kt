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

    /**
     * Logs in an existing user with email and password.
     */
    suspend fun login(email: String, password: String): FirebaseUser {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            result.user ?: throw Exception("Login failed: User object is null.")
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Creates a new user, updates their Firebase Auth profile,
     * and initializes their Firestore document.
     */
    suspend fun signup(email: String, password: String, userName: String): FirebaseUser {
        return try {
            // 1. Create the Auth User in Firebase
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: throw Exception("Signup failed: User object is null.")

            // 2. Update the internal Firebase Auth DisplayName
            val profileUpdates = UserProfileChangeRequest.Builder()
                .setDisplayName(userName)
                .build()
            firebaseUser.updateProfile(profileUpdates).await()

            // 3. Create the User Data Model for Firestore
            val newUser = User(
                id = firebaseUser.uid,
                userName = userName,
                email = email,
                registrationTimestamp = System.currentTimeMillis()
            )

            // 4. Save the document to the "users" collection using the UID as the ID
            usersCollection.document(firebaseUser.uid).set(newUser).await()

            firebaseUser
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Fetches a complete User object from Firestore.
     */
    suspend fun getUserData(userId: String): User {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            if (!snapshot.exists()) {
                throw Exception("User profile document not found in Firestore.")
            }
            // Maps the Firestore fields (like userName) to the Kotlin User data class
            snapshot.toObject(User::class.java)
                ?: throw Exception("Failed to parse User profile data.")
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Uploads an image to Firebase Storage and returns the public URL.
     */
    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): String {
        return try {
            val storageRef = storage.reference
                .child("profile_pictures")
                .child("$userId.jpg")

            storageRef.putFile(imageUri).await()
            storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Storage error: ${e.localizedMessage}")
        }
    }

    /**
     * Sends a password reset email to the user.
     */
    suspend fun sendPasswordResetEmail(email: String) {
        try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Gets the currently logged-in Firebase User.
     */
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    /**
     * Logs the user out.
     */
    fun signOut() {
        auth.signOut()
    }
}