/**
 * @file AuthManager.kt
 * @brief Klasa zarządzająca procesami uwierzytelniania i danymi użytkowników w Firebase.
 */

package com.example.projekt

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.tasks.await

/**
 * @class AuthManager
 * @brief Menedżer autoryzacji korzystający z Firebase Auth oraz Firestore.
 * 
 * Klasa zapewnia metody do logowania, rejestracji, wylogowywania oraz pobierania 
 * informacji o profilu użytkownika (np. imię) z bazy danych Firestore.
 */
class AuthManager {

    private val auth: FirebaseAuth = Firebase.auth
    private val db: FirebaseFirestore = Firebase.firestore

    /**
     * @brief Pobiera aktualnie zalogowanego użytkownika Firebase.
     * @return Obiekt FirebaseUser lub null, jeśli nikt nie jest zalogowany.
     */
    fun getCurrentUser() = auth.currentUser

    /**
     * @brief Loguje użytkownika za pomocą e-maila i hasła.
     * @param email Adres e-mail użytkownika.
     * @param password Hasło użytkownika.
     * @return null w przypadku sukcesu, lub komunikat błędu w przypadku niepowodzenia.
     */
    suspend fun login(email: String, password: String): String? {
        return try {
            auth.signInWithEmailAndPassword(email, password).await()
            null // Success
        } catch (e: Exception) {
            e.message // Return error message on failure
        }
    }

    /**
     * @brief Rejestruje nowego użytkownika i tworzy jego profil w Firestore.
     * @param name Imię użytkownika wyświetlane w aplikacji.
     * @param email Adres e-mail.
     * @param password Hasło.
     * @return null w przypadku sukcesu, lub komunikat błędu.
     */
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

    /**
     * @brief Pobiera imię użytkownika z bazy Firestore na podstawie UID.
     * @param uid Unikalny identyfikator użytkownika z Firebase Auth.
     * @return Nazwa użytkownika lub null w przypadku błędu.
     */
    suspend fun getUserName(uid: String): String? {
        return try {
            val document = db.collection("users").document(uid).get().await()
            document.getString("name")
        } catch (e: Exception) {
            null
        }
    }

    /**
     * @brief Wylogowuje aktualnego użytkownika z aplikacji.
     */
    fun logout() {
        auth.signOut()
    }
}
