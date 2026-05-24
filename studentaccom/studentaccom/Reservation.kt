package com.example.studentaccom


data class Reservation(
    var id: String = "",
    val userId: String = "",
    val listingId: String = "",
    val listingTitle: String = "",
    val location: String = "",
    val depositPaid: Double = 0.0,
    val referenceNumber: String = "",
    val paymentMethod: String = "",
    val paymentDate: String = "",
    val status: String = "active"
)