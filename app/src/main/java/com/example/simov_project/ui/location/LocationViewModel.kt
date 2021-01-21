package com.example.simov_project.ui.location

import android.graphics.Bitmap
import androidx.lifecycle.*
import com.example.simov_project.dataclasses.Location
import com.example.simov_project.db.FirebaseProvider
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Holds the values and logic for the locationFragment
 * @property locations Locations from firebase to be displayed in recyclerView
 * @property icons a hashMap of locationId and the icon as bitmap for the location
 * @property images a hashMap of locationId and the image as bitmap for the location
 * @property distances a hashMap of locationId and the distance between the user and the location
 */
class LocationViewModel : ViewModel() {

    private val _locations = MutableLiveData<List<Location>>().apply { value = listOf() }
    private val _icons = MutableLiveData<HashMap<String, Bitmap>>().apply {
        value = hashMapOf()
    }
    private val _images = MutableLiveData<HashMap<String, Bitmap>>().apply {
        value = hashMapOf()
    }
    private val _distances = MutableLiveData<HashMap<String, Float>>().apply {
        value = hashMapOf()
    }
    val locations: LiveData<List<Location>> = _locations
    val icons: LiveData<HashMap<String, Bitmap>> = _icons
    val images: LiveData<HashMap<String, Bitmap>> = _images
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

    /**
     * Downloads the icon for each location
     */
    private fun downloadIcons() {
        locations.value!!.forEach { location ->
            viewModelScope.launch {
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
    }

    /**
     * Downloads an image
     * @param locationId the location to download the image for
     */
    fun downloadImage(locationId: String) {
        val location = locations.value!!.find { it.locationId == locationId }
        location?.let {
            if (it.imageUri != null) {
                viewModelScope.launch {
                    FirebaseProvider.storage.downloadLocationImage(location.locationId, "image")
                        ?.let { bitmap ->
                            val images = _images.value!!
                            images[locationId] = bitmap
                            _images.value = images
                        }
                }
            }
        }
    }

    /**
     * Calculate distance to each location
     * @param latitude of the user
     * @param longitude of the user
     */
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
            it.openHour[day] == it.closeHour[day] &&
                    it.openMinute[day] == it.closeMinute[day]
        }
    }
}