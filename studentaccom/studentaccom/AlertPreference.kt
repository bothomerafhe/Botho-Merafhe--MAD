package com.example.studentaccom

data class AlertPreference(
    var id: String = "",
    val userId: String = "",
    val minPrice: Double = 0.0,
    val maxPrice: Double = 10000.0,
    val preferredLocation: String = "",
    val availabilityDate: String = "",
    val priceAlertEnabled: Boolean = true,
    val locationAlertEnabled: Boolean = true,
    val dateAlertEnabled: Boolean = false
)