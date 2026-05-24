package com.example.studentaccom

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.tasks.await

object FirebaseAuthManager {

    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val currentUserId: String
        get() = auth.currentUser?.uid ?: ""

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    suspend fun register(
        email: String,
        password: String,
        firstName: String,
        lastName: String,
        studentId: String,
        university: String
    ): Result<User> {
        return try {
            // 1. Create user in Firebase Auth
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Failed to get UID")
            
            val user = User(
                uid = uid,
                studentId = studentId,
                firstName = firstName,
                lastName = lastName,
                email = email,
                university = university,
                role = "student"
            )

            // 2. Save user profile to Realtime Database
            // We use the reference from FirebaseManager
            FirebaseManager.rtdbUsersRef.child(uid).setValue(user).await()
            
            // 3. Also save to Firestore for backup/compatibility
            try {
                FirebaseManager.usersRef.document(uid).set(user).await()
            } catch (e: Exception) {
                Log.e("AuthManager", "Firestore save failed: ${e.message}")
            }

            Result.success(user)
        } catch (e: Exception) {
            Log.e("AuthManager", "Registration Error: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<User> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid ?: throw Exception("Login failed")
            
            // Fetch from Realtime Database
            val rtdbSnapshot = FirebaseManager.rtdbUsersRef.child(uid).get().await()
            var user = rtdbSnapshot.getValue(User::class.java)
            
            // Fallback to Firestore
            if (user == null) {
                val doc = FirebaseManager.usersRef.document(uid).get().await()
                user = doc.toObject(User::class.java)
            }
            
            if (user == null) {
                user = User(uid = uid, email = email, firstName = "Student")
            }
            
            Result.success(user.also { it.uid = uid })
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun logout() {
        auth.signOut()
    }
}
