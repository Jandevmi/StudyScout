package com.example.simov_project.ui.reservation

import android.util.Log
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simov_project.dataclasses.Reservation
import com.example.simov_project.dataclasses.User
import com.example.simov_project.db.FirebaseProvider
import kotlinx.coroutines.launch

class ReservationViewModel : ViewModel() {

    private val _reservations = MutableLiveData<List<Reservation>>().apply { value = listOf() }
    private var reservationIds = mutableListOf<String>()
    val reservations: LiveData<List<Reservation>> = _reservations

    fun getReservationsForUser(reservationIds: MutableList<String>) {
        if (this.reservationIds != reservationIds){
            this.reservationIds = reservationIds
            updateReservations()
        }
    }

    /**
     * Deletes the reservation and calls updateReservations() if successful
     * @param reservationId ID of the reservation to be deleted
     */
    fun deleteReservation(reservationId: String) {
        viewModelScope.launch {
            if (FirebaseProvider.db.deleteReservation(reservationId)){
                reservationIds.remove(reservationId)
                updateReservations()
            }
        }
    }

    /**
     * Gets all reservations from Firebase and updates the LiveData
     */
    private fun updateReservations() {
        viewModelScope.launch {
            _reservations.value = FirebaseProvider.db.getReservations(reservationIds)
        }
    }

    /**
     * Adds the reservation to this viewModel. Only local!
     * @param reservation The reservation object to be added to the viewModel
     */
    fun addReservation(reservation: Reservation) {
        _reservations.value = _reservations.value?.plus(reservation)
    }

    fun getReservationsForLocations(locationIds: List<String>) {
        val locationReservationIds = mutableListOf<String>()
        viewModelScope.launch {
            locationIds.forEach { locationId ->
                val location = FirebaseProvider.db.getLocation(locationId)
                location.reservationIds?.forEach { reservationId ->
                    locationReservationIds.add(reservationId)
                }
            }
            reservationIds = locationReservationIds
            updateReservations()
        }
    }
}