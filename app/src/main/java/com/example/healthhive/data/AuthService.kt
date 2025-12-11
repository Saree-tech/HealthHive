// File: com/example/healthhive/data/AuthService.kt (UPDATED with Firestore)

package com.example.healthhive.data

import com.example.healthhive.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.FirebaseFirestore // <-- NEW IMPORT
import kotlinx.coroutines.tasks.await
import java.lang.Exception

/**
 * Data layer service responsible for all Firebase Authentication and User Data interactions.
 */
class AuthService {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance() // <-- NEW FIELD
    private val usersCollection = firestore.collection("users") // Collection reference

    // ... (login method remains the same) ...
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
            val user = authResult.user
                ?: throw Exception("Signup failed: User object is null.")

            // 1. Create the User data model
            val newUser = User(
                id = user.uid,
                userName = userName,
                email = email
            )

            // 2. Save the user data to the 'users' collection using the UID as the document ID
            usersCollection.document(user.uid).set(newUser).await() // <-- FIRESTORE SAVE

            return user
        } catch (e: Exception) {
            throw e
        }
    }

    // ... (sendPasswordResetEmail method remains the same) ...
    suspend fun sendPasswordResetEmail(email: String): Void? {
        return try {
            auth.sendPasswordResetEmail(email).await()
        } catch (e: Exception) {
            throw e
        }
    }

    /**
     * Retrieves the complete User profile data from Firestore.
     */
    suspend fun getUserData(userId: String): User {
        return try {
            val snapshot = usersCollection.document(userId).get().await()
            snapshot.toObject(User::class.java)
                ?: throw Exception("User profile data not found.")
        } catch (e: Exception) {
            throw e
        }
    }

    // ... (getCurrentUser and logout methods remain the same) ...
    fun getCurrentUser(): FirebaseUser? {
        return auth.currentUser
    }

    fun logout() {
        auth.signOut()
    }
}