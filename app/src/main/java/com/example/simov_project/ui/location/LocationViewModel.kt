package com.example.simov_project.ui.location

import android.graphics.Bitmap
import androidx.lifecycle.*
import com.example.simov_project.dataclasses.Location
import com.example.simov_project.db.FirebaseProvider
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Holds the values and logic for the locationFragment
 * @property _locations Locations from firebase that can be changed inside the viewModel
 * @property locations Locations from firebase that can be accessed from outside, used to display locations
 */
class LocationViewModel : ViewModel() {

    private val _locations = MutableLiveData<List<Location>>().apply { value = listOf() }
    private val _icons = MutableLiveData<HashMap<String, Bitmap>>().apply {
        value = hashMapOf()
    }
    private val _distances = MutableLiveData<HashMap<String, Float>>().apply {
        value = hashMapOf()
    }
    val locations: LiveData<List<Location>> = _locations
    val icons: LiveData<HashMap<String, Bitmap>> = _icons
    val distances: LiveData<HashMap<String, Float>> = _distances

    init {
        getAllFirebaseLocations()
    }


    fun getLocalLocation(locationId: String): Location? {
        return locations.value!!.find { it.locationId == locationId }
    }

    /**
     * Get all locations from the database and assign it to locations
     */
    fun getAllFirebaseLocations() {
        viewModelScope.launch {
            val locations = FirebaseProvider.db.getAllLocations()
            _locations.value = locations
            downloadIcons()
        }
    }

    private suspend fun downloadIcons() {
        locations.value!!.forEach { location ->
            if (location.iconUri != null) {
                FirebaseProvider.storage.downloadLocationImage(location.locationId, "icon")
                    ?.let { bitmap ->
                        val icons = icons.value!!
                        icons[location.locationId] = bitmap
                        _icons.value = icons
                    }
            }
        }
    }

    fun calculateDistances(latitude: Double, longitude: Double) {
        val distances = hashMapOf<String, Float>()
        locations.value!!.forEach { location ->
            val target = android.location.Location("Target")
            target.longitude = location.longtitude
            target.latitude = location.latitude
            val gps = android.location.Location("gps")
            gps.longitude = longitude
            gps.latitude = latitude
            distances[location.locationId] =
                (target.distanceTo(gps) / 100).roundToInt().toFloat() / 10
        }
         val sortedLocations = locations.value!!.sortedBy { distances[it.locationId] }
        _distances.value = distances
        _locations.value = sortedLocations
    }

    /**
     * Get the locations from the database and assign it to locations
     * @param locationIds
     */
    fun getFirebaseLocationsForUser(locationIds: List<String>) {
        val locations = mutableListOf<Location>()
        viewModelScope.launch {
            locationIds.forEach {
                locations.add(FirebaseProvider.db.getLocation(it))
            }
            _locations.value = locations
            downloadIcons()
        }
    }

    /**
     * Deletes the location
     */
    fun deleteLocation(locationId: String) {
        viewModelScope.launch {
            if (FirebaseProvider.db.deleteLocation(locationId)) {
                _locations.value = locations.value!!.filter { oldLocations ->
                    oldLocations.locationId != locationId
                }
            }
        }
    }

    /**
     * Adds the reservationID to the location locally and to Firebase
     * @param reservationId The ID to be added
     */
    fun addReservationId(locationId: String, reservationId: String) {
        viewModelScope.launch {
            val newLocation = locations.value!!.find { it.locationId == locationId }!! // FixMe
            newLocation.reservationIds?.add(reservationId)
            FirebaseProvider.db.updateLocation(newLocation)
        }
    }

    /**
     * Removes the reservationID to the location locally and to Firebase
     * @param reservationId The ID to be removed
     */
    fun removeReservationId(reservationId: String, locationId: String) {
        viewModelScope.launch {
            val newLocation = locations.value!!.find { it.locationId == locationId }
            newLocation?.let {
                it.reservationIds?.apply {
                    remove(reservationId)
                    distinct()
                }
                FirebaseProvider.db.updateLocation(it)
            }
        }
    }

    /**
     * Checks if the location is open at that day
     * @param pos The position of the location in locations array and locationAdapter
     * @param day Day of month, starting with 0 = Monday to 6 = Sunday
     */
    fun isDayClosed(location: Location, day: Int): Boolean {
        return location.let {
            it.openTimeHour[day] == it.closeTimeHour[day] &&
                    it.openTimeMinute[day] == it.closeTimeMinute[day]
        }
    }
}