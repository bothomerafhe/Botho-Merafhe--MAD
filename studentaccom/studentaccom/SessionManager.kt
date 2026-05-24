package com.example.studentaccom



import android.content.Context
import android.content.SharedPreferences
import com.example.studentaccom.FirebaseAuthManager

class SessionManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("StudentNestSession", Context.MODE_PRIVATE)

    companion object {
        const val KEY_USER_NAME  = "user_name"
        const val KEY_STUDENT_ID = "student_id"
        const val KEY_UNIVERSITY = "university"
        const val KEY_USER_EMAIL = "user_email"
    }

    fun saveProfile(firstName: String, lastName: String, studentId: String,
                    university: String, email: String) {
        prefs.edit()
            .putString(KEY_USER_NAME, "$firstName $lastName")
            .putString(KEY_STUDENT_ID, studentId)
            .putString(KEY_UNIVERSITY, university)
            .putString(KEY_USER_EMAIL, email)
            .apply()
    }

    fun isLoggedIn(): Boolean  = FirebaseAuthManager.isLoggedIn
    fun getUserId(): String    = FirebaseAuthManager.currentUserId
    fun getUserName(): String  = prefs.getString(KEY_USER_NAME, "Student") ?: "Student"
    fun getStudentId(): String = prefs.getString(KEY_STUDENT_ID, "") ?: ""
    fun getUniversity(): String = prefs.getString(KEY_UNIVERSITY, "") ?: ""
    fun getUserEmail(): String = prefs.getString(KEY_USER_EMAIL, "") ?: ""

    fun clearSession() {
        prefs.edit().clear().apply()
        FirebaseAuthManager.logout()
    }
}