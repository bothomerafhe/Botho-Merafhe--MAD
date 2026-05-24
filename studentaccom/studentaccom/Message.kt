package com.example.studentaccom


data class Message(
    var id: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val listingId: String = "",
    val content: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val isRead: Boolean = false,
    val senderName: String = "",
    val landlordName: String = ""
)