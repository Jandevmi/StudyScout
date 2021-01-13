package com.example.simov_project.db

import android.content.ContentValues.TAG
import android.util.Log
import com.example.simov_project.dataclasses.Location
import com.example.simov_project.dataclasses.Reservation
import com.example.simov_project.dataclasses.User
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.toObject
import kotlinx.coroutines.tasks.await

class DatabaseProvider {

    companion object {
        const val LOCATIONS = "locations"
        const val RESERVATIONS = "reservations"
        const val USER = "user"
    }

    private val db = FirebaseFirestore.getInstance()

    /**
     * Creates or updates new location
     *
     * If the locationId is "-1" a new location will be created.
     * If the location already has an ID, it will be updated
     * @param location The location object to be updated in the database
     * @return The locationID or null if creating/updating failed
     */
    suspend fun updateLocation(location: Location): String? {
        Log.e(TAG, "Writing location")
        return try {
            if (location.locationId == "-1") {
                db.collection(LOCATIONS)
                    .add(location)
                    .await().id
            } else {
                db.collection(LOCATIONS).document(location.locationId)
                    .set(location)
                    .await()
                location.locationId
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error writing location", e)
            null
        }
    }

    /**
     * Creates a new reservation
     * @param reservation The reservation object to be written to the database
     * @return The ID of from the created reservation object. Returns "-1" if not successful
     */
    suspend fun createReservation(reservation: Reservation): String? {
        return try {
            db.collection(RESERVATIONS)
                .add(reservation)
                .await().id
        } catch (e: Exception) {
            Log.e(TAG, "Error writing reservation", e)
            null
        }
    }

    /**
     * Get a location from the database
     * @param locationId ID of the needed location
     * @return Loaction object. It is empty if request is unsuccessful
     */
    suspend fun getLocation(locationId: String): Location {
        return try {
            db.collection(LOCATIONS).document(locationId)
                .get()
                .await().let { doc ->
                    val location = doc.toObject<Location>()!!
                    location.locationId = doc.id
                    location
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading location with ID: $locationId", e)
            Location()
        }
    }

    /**
     * Get all locations from the database
     * @return List of all documents in Firebase collection "locations"
     */
    suspend fun getAllLocations(): List<Location> {
        return try {
            db.collection(LOCATIONS)
                .get()
                .await().let { documentList ->
                    val locationList = mutableListOf<Location>()
                    documentList.forEach { doc ->
                        val location = doc.toObject(Location::class.java)
                        location.locationId = doc.id
                        locationList.add(location)
                    }
                    locationList
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading locations", e)
            emptyList()
        }
    }

    /**
     * Deletes the location
     * @param locationId ID of the location to be deleted
     * @return true if deleting was successful, false if not
     */
    suspend fun deleteLocation(locationId: String): Boolean {
        return try {
            db.collection(LOCATIONS).document(locationId)
                .delete()
                .await().let {
                    true
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting reservation with ID: $locationId", e)
            false
        }
    }

    /**
     * Get all reservations from the database
     * @return List of all documents in Firebase collection "reservations"
     */
    suspend fun getAllReservations(): List<Reservation> {
        return try {
            db.collection(RESERVATIONS)
                .get()
                .await().let { documentList ->
                    val reservationList = mutableListOf<Reservation>()
                    documentList.forEach { doc ->
                        val reservation = doc.toObject(Reservation::class.java)
                        reservation.reservationId = doc.id
                        reservationList.add(reservation)
                    }
                    reservationList
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading reservations", e)
            emptyList()
        }
    }

    /**
     * Get all reservations from the database
     * @return List of all documents in Firebase collection "reservations"
     */
    suspend fun getReservations(reservationIds: List<String>): List<Reservation> {
        return try {
            val reservationList = mutableListOf<Reservation>()
            reservationIds.forEach { reservationId ->
                db.collection(RESERVATIONS).document(reservationId)
                    .get()
                    .await().let { doc ->
                        doc.toObject<Reservation>()?.let {
                            it.reservationId = doc.id
                            reservationList.add(it)
                        }
                    }
            }
            reservationList.sortedBy { it.getIndex() }
        } catch (e: Exception) {
            Log.e(TAG, "Error reading reservations", e)
            emptyList()
        }
    }

    /**
     * Deletes the reservation
     * @param reservationId ID of the reservation to be deleted
     * @return true if deleting was successful, false if not
     */
    suspend fun deleteReservation(reservationId: String): Boolean {
        return try {
            db.collection(RESERVATIONS).document(reservationId)
                .delete()
                .await().let {
                    true
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting reservation with ID: $reservationId", e)
            false
        }
    }

    /**
     * Creates a new user
     * @param user The user object to be written to the database
     * @return The ID of from the created user object. Returns "-1" if not successful
     */
    suspend fun updateUser(user: User): String? {
        return try {
            db.collection(USER).document(user.userId)
                .set(user)
                .await()
            user.userId
        } catch (e: Exception) {
            Log.e(TAG, "Error writing user with email: ${user.email}", e)
            null
        }
    }

    /**
     * Get a location from the database
     * @param userId ID of the needed user
     * @return user object. Returns null if request is unsuccessful
     */
    suspend fun getUser(userId: String): User? {
        return try {
            db.collection(USER).document(userId)
                .get()
                .await().let { doc ->
                    val user = doc.toObject<User>()!!
                    user.userId = doc.id
                    user
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user with userID: $userId", e)
            null
        }
    }
}