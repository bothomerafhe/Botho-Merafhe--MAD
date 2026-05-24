package com.example.studentaccom


data class User(
    var uid: String = "",
    val studentId: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val email: String = "",
    val university: String = "",
    val role: String = "student"
)
