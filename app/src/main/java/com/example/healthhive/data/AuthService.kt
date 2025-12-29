package com.example.healthhive.data

import android.net.Uri
import com.example.healthhive.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage // Will work after Step 1
import kotlinx.coroutines.tasks.await
import java.lang.Exception

/**
 * Data layer service responsible for Firebase Auth, Firestore, and Storage interactions.
 */
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

    /**
     * Creates a new user account and saves the user data to Firestore.
     */
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
     * Uploads an image to Firebase Storage and returns the download URL string.
     */
    suspend fun uploadProfilePicture(userId: String, imageUri: Uri): String {
        return try {
            // Updated to ensure reference is accessed correctly
            val storageRef = storage.getReference("profile_pictures/$userId.jpg")
            storageRef.putFile(imageUri).await()
            return storageRef.downloadUrl.await().toString()
        } catch (e: Exception) {
            throw Exception("Image upload failed: ${e.message}")
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
            // FIXED: Explicitly specified User class to resolve inference error
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