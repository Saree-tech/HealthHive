package com.example.healthhive.data

import android.net.Uri
import com.example.healthhive.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
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
            auth.signInWithEmailAndPassword(email, password).await().user
                ?: throw Exception("Login failed: User object is null.")
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun signup(email: String, password: String, userName: String): FirebaseUser {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
                ?: throw Exception("Signup failed: User object is null.")

            val newUser = User(
                id = firebaseUser.uid,
                userName = userName,
                email = email,
                registrationTimestamp = System.currentTimeMillis(),
                age = "",
                bloodType = "",
                allergies = "",
                medicalHistory = "",
                weight = "",
                height = "",
                profilePictureUrl = ""
            )

            usersCollection.document(firebaseUser.uid).set(newUser).await()
            return firebaseUser
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * FIXED: Explicitly creates the reference hierarchy and uses .child()
     * to avoid pathing errors that lead to 404s.
     */
    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): String {
        return try {
            // Use storage.reference to get the root, then build the path
            val storageRef = storage.reference
                .child("profile_pictures")
                .child("$userId.jpg")

            // Upload file
            storageRef.putFile(imageUri).await()

            // Get URL
            val url = storageRef.downloadUrl.await().toString()
            return url
        } catch (e: Exception) {
            // Rethrowing specific message to help debugging
            throw Exception("Storage error: ${e.localizedMessage}")
        }
    }

    suspend fun sendPasswordResetEmail(email: String): Void? {
        return try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            throw e
        }
    }

    suspend fun getUserData(userId: String): User {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            snapshot.toObject(User::class.java)
                ?: throw Exception("User profile data not found.")
        } catch (e: Exception) {
            throw e
        }
    }

    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun signOut() {
        auth.signOut()
    }
}