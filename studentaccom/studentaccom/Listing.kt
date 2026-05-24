package com.example.studentaccom

data class Listing(
    var id: String = "",
    var title: String = "",
    var price: Double = 0.0,
    var location: String = "",
    var type: String = "",
    var amenities: String = "",
    var availabilityDate: String = "",
    var depositAmount: Double = 0.0,
    var imageUrl: String = "",
    var description: String = "",
    var landlordName: String = "",
    var landlordPhone: String = "",
    var landlordId: String = "",
    var isReserved: Boolean = false,
    var reservedByUserId: String = "",
    var latitude: Double = -24.6583,
    var longitude: Double = 25.9083
)
