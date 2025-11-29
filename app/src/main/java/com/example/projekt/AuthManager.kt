package com.example.projekt

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

class AuthManager {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    fun getCurrentUser() = auth.currentUser

    suspend fun login(email: String, password: String): String? {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            null // Success
        } catch (e: Exception) {
            e.message // Return error message on failure
        }
    }

    suspend fun register(name: String, email: String, password: String): String? {
        return try {
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user
            if (firebaseUser != null) {
                val user = hashMapOf(
                    "name" to name,
                    "email" to email
                )
                db.collection("users").document(firebaseUser.uid).set(user).await()
            }
            null // Success
        } catch (e: Exception) {
            e.message // Return error message on failure
        }
    }

    suspend fun getUserName(uid: String): String? {
        return try {
            val document = db.collection("users").document(uid).get().await()
            document.getString("name")
        } catch (e: Exception) {
            null
        }
    }

    fun logout() {
        auth.signOut()
    }
}