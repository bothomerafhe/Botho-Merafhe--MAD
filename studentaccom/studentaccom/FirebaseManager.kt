package com.example.studentaccom

import com.google.firebase.database.*
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await

object FirebaseManager {

    private val db = FirebaseFirestore.getInstance()
    private val rtdb = FirebaseDatabase.getInstance()

    val listingsRef get() = db.collection("listings")
    val usersRef     get() = db.collection("users")
    val reservationsRef get() = db.collection("reservations")
    val alertPrefsRef   get() = db.collection("alert_preferences")
    val messagesRef     get() = db.collection("messages")

    val rtdbUsersRef = rtdb.getReference("users")
    val rtdbListingsRef = rtdb.getReference("listings")
    val rtdbMessagesRef = rtdb.getReference("messages")

    // --- Listings ---

    fun listenToListings(
        onUpdate: (List<Listing>, Exception?) -> Unit
    ): ListenerRegistration {
        return listingsRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                onUpdate(emptyList(), error)
                return@addSnapshotListener
            }
            val listings = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(Listing::class.java)?.also { it.id = doc.id }
            } ?: emptyList()
            onUpdate(listings, null)
        }
    }

    suspend fun getListingById(listingId: String): Listing? {
        return try {
            val doc = listingsRef.document(listingId).get().await()
            doc.toObject(Listing::class.java)?.also { it.id = doc.id }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun reserveListing(listingId: String, userId: String) {
        try {
            listingsRef.document(listingId).update(
                mapOf("isReserved" to true, "reservedByUserId" to userId, "reservedAt" to System.currentTimeMillis())
            ).await()
        } catch (e: Exception) { throw e }
    }

    suspend fun saveReservation(reservation: Reservation) {
        try {
            reservationsRef.document(reservation.referenceNumber).set(reservation).await()
        } catch (e: Exception) { throw e }
    }

    fun listenToReservations(uid: String, onUpdate: (List<Reservation>, Exception?) -> Unit): ListenerRegistration {
        return reservationsRef.whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    onUpdate(emptyList(), error)
                    return@addSnapshotListener
                }
                val list = snapshot?.documents?.mapNotNull { it.toObject(Reservation::class.java) } ?: emptyList()
                onUpdate(list, null)
            }
    }

    // --- Alerts ---

    suspend fun getAlertPreference(uid: String): AlertPreference? {
        return try {
            val doc = alertPrefsRef.document(uid).get().await()
            doc.toObject(AlertPreference::class.java)
        } catch (e: Exception) {
            null
        }
    }

    suspend fun saveAlertPreference(pref: AlertPreference) {
        alertPrefsRef.document(pref.userId).set(pref).await()
    }

    suspend fun getMatchingListings(min: Double, max: Double): List<Listing> {
        return try {
            val snapshot = listingsRef.whereGreaterThanOrEqualTo("price", min)
                .whereLessThanOrEqualTo("price", max).get().await()
            snapshot.toObjects(Listing::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun getListingsByLocation(location: String): List<Listing> {
        return try {
            val snapshot = listingsRef.whereEqualTo("location", location).get().await()
            snapshot.toObjects(Listing::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }

    // --- Chat ---

    fun listenToAllConversations(uid: String, onUpdate: (List<Message>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val allMessages = mutableListOf<Message>()
                for (convoSnap in snapshot.children) {
                    for (msgSnap in convoSnap.children) {
                        val msg = msgSnap.getValue(Message::class.java)
                        if (msg != null && (msg.senderId == uid || msg.receiverId == uid)) {
                            allMessages.add(msg)
                        }
                    }
                }
                onUpdate(allMessages)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rtdbMessagesRef.addValueEventListener(listener)
        return listener
    }

    fun stopListeningAll(listener: ValueEventListener) {
        rtdbMessagesRef.removeEventListener(listener)
    }

    fun listenToConversation(listingId: String, uid: String, onUpdate: (List<Message>) -> Unit): ValueEventListener {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val messages = snapshot.children.mapNotNull { it.getValue(Message::class.java) }
                onUpdate(messages)
            }
            override fun onCancelled(error: DatabaseError) {}
        }
        rtdbMessagesRef.child(listingId).addValueEventListener(listener)
        return listener
    }

    fun stopListening(listingId: String, listener: ValueEventListener) {
        rtdbMessagesRef.child(listingId).removeEventListener(listener)
    }

    suspend fun sendMessage(message: Message) {
        rtdbMessagesRef.child(message.listingId).push().setValue(message).await()
    }

    fun markMessagesRead(listingId: String, uid: String) {
        rtdbMessagesRef.child(listingId).get().addOnSuccessListener { snapshot ->
            for (msgSnap in snapshot.children) {
                val msg = msgSnap.getValue(Message::class.java)
                if (msg != null && msg.receiverId == uid && !msg.isRead) {
                    msgSnap.ref.child("isRead").setValue(true)
                }
            }
        }
    }

    // --- Seeding ---

    suspend fun seedListingsIfEmpty() {
        val snapshot = listingsRef.get().await()
        val houseImages = listOf(
            "https://images.unsplash.com/photo-1570129477492-45c003edd2be?w=800",
            "https://images.unsplash.com/photo-1580587767376-0424adde09e4?w=800",
            "https://images.unsplash.com/photo-1512917774080-9991f1c4c750?w=800",
            "https://images.unsplash.com/photo-1600585154340-be6161a56a0c?w=800",
            "https://images.unsplash.com/photo-1518780664697-55e3ad937233?w=800",
            "https://images.unsplash.com/photo-1568605114967-8130f3a36994?w=800",
            "https://images.unsplash.com/photo-1480074568708-e7b720bb3f09?w=800",
            "https://images.unsplash.com/photo-1449844908441-8829872d2607?w=800",
            "https://images.unsplash.com/photo-1510798831971-661eb04b3739?w=800"
        )

        if (snapshot.isEmpty) {
            val listings = generateSeedListings(houseImages)
            val batch = db.batch()
            listings.forEach { listing -> batch.set(listingsRef.document(), listing) }
            batch.commit().await()
        } else {
            val firstImg = snapshot.documents.firstOrNull()?.getString("imageUrl") ?: ""
            if (!firstImg.startsWith("http")) {
                val batch = db.batch()
                snapshot.documents.forEachIndexed { i, doc ->
                    batch.update(doc.reference, "imageUrl", houseImages[i % houseImages.size])
                    batch.update(doc.reference, "amenities", "WiFi, Water, Kitchen, Security, Parking")
                }
                batch.commit().await()
            }
        }
    }

    private fun generateSeedListings(houseImages: List<String>): List<Listing> {
        val areas = listOf("Gaborone West","Village","Tlokweng","Mogoditshane","Broadhurst","Phakalane")
        val listings = mutableListOf<Listing>()
        for (i in 1..50) {
            listings.add(Listing(
                title = "Kgosi Residence $i",
                price = 1200.0 + (i * 50),
                location = areas[i % areas.size],
                type = if (i % 3 == 0) "Bachelor Flat" else "Single Room",
                description = "Modern student accommodation in ${areas[i % areas.size]}. Highly secure with private entrance.",
                landlordName = "Landlord $i",
                landlordPhone = "71 234 56$i",
                imageUrl = houseImages[i % houseImages.size],
                amenities = "WiFi, Water Included, Kitchen, Parking"
            ))
        }
        return listings
    }
}
