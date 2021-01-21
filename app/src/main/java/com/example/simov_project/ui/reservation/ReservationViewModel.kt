package com.example.simov_project.ui.reservation

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simov_project.dataclasses.Reservation
import com.example.simov_project.db.FirebaseProvider
import kotlinx.coroutines.launch

/**
 * Holds data and logic for reservations
 * @property reservations LiveData List of Reservations to be shown with Adapter
 * @property reservationIds List of ids for the Reservations
 * @property reservation Single reservation for ReservationDetailFragment
 */
class ReservationViewModel : ViewModel() {

    private val _reservations = MutableLiveData<List<Reservation>>().apply { value = listOf() }
    private var reservationIds = mutableListOf<String>()
    var reservation = Reservation()
    val reservations: LiveData<List<Reservation>> = _reservations


    /**
     * Gets all reservations from Firebase and updates the LiveData
     */
    private fun updateReservations() {
        viewModelScope.launch {
            _reservations.value = FirebaseProvider.db.getReservations(reservationIds)
        }
    }

    /**
     * Activates the reservation after QR-code scan
     * @param locationId result from the QR-code scan, must fit to the locationId of reservation
     */
    fun activateReservation(locationId: String): String {
        reservation.let {
            return if (locationId != it.locationId) {
                "Wrong QR-Code"
            } else {
                viewModelScope.launch {
                    it.activated = true
                    FirebaseProvider.db.updateReservation(it)
                    updateReservations()
                }
                "Activated!"
            }
        }
    }

    /**
     * Update reservationsIds and start download
     * If the user is not locationOwner
     */
    fun getReservationsForUser(reservationIds: MutableList<String>) {
        if (this.reservationIds != reservationIds) {
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
            if (FirebaseProvider.db.deleteReservation(reservationId)) {
                reservationIds.remove(reservationId)
                updateReservations()
            }
        }
    }

    /**
     * Adds the reservation to this viewModel. Only local!
     * @param reservation The reservation object to be added to the viewModel
     */
    fun addReservation(reservation: Reservation) {
        _reservations.value = _reservations.value?.plus(reservation)
    }

    /**
     * Update reservationsIds and start download
     * If the user is locationOwner
     */
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