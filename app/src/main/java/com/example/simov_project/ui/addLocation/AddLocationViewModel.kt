package com.example.simov_project.ui.addLocation

import android.graphics.Bitmap
import android.widget.ImageView
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.simov_project.dataclasses.Location
import com.example.simov_project.db.FirebaseProvider
import com.example.simov_project.ui.addReservation.AddReservationViewModel
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import com.wdullaer.materialdatetimepicker.time.Timepoint
import kotlinx.coroutines.launch

/**
 * Holds the values and logic for the addLocationFragment
 * @property location location that is created or edited
 * @property icon of the location that is used in recyclerViews
 * @property image of the location hat is used in location / reservation detail page
 * @property loading is > 0 during upload. Loading symbol is shown when > 0
 */
class AddLocationViewModel : ViewModel() {

    private val _location = MutableLiveData<Location>().apply { value = Location() }
    private val _loading = MutableLiveData<Int>().apply { value = 0 }
    private val _icon = MutableLiveData<Bitmap>()
    private val _image = MutableLiveData<Bitmap>()
    val location: LiveData<Location> = _location
    val loading: LiveData<Int> = _loading
    val icon: LiveData<Bitmap> = _icon
    val image: LiveData<Bitmap> = _image

    /**
     * Set / update the location as viewModel location
     * @param location the location object to be set
     */
    fun updateLocalLocation(location: Location) {
        _location.value = location
    }

    /**
     * Updates the viewModel location to Firebase
     */
    fun updateFirebaseLocation() {
        location.value?.let {
            viewModelScope.launch {
                FirebaseProvider.db.updateLocation(it)?.let { id ->
                    if (it.locationId == "-1") {
                        val newLocation = it.copy(locationId = id)
                        _location.value = newLocation
                    }
                }
            }
        }
    }

    /**
     * Downloads the old image / icon for the location
     * Stored as LiveData<Bitmap>
     */
    fun downloadImages() {
        viewModelScope.launch {
            if (location.value!!.iconUri != null)
                _icon.value =
                    FirebaseProvider.storage.downloadLocationImage(
                        location.value!!.locationId,
                        "icon"
                    )
            if (location.value!!.imageUri != null)
                _image.value =
                    FirebaseProvider.storage.downloadLocationImage(
                        location.value!!.locationId,
                        "image"
                    )
        }
    }

    /**
     * Uploads images from LiveData<Bitmap> to Firebase Storage
     * Saves the URI in the Location property
     * LiveData++ at download start, LiveData-- when download is finished
     */
    fun uploadImage(filename: String, imageView: ImageView) {
        viewModelScope.launch {
            _loading.value = loading.value!! + 1
            when (filename) {
                "icon" -> _location.value!!.iconUri = FirebaseProvider.storage.uploadLocationImage(
                    location.value!!.locationId,
                    filename,
                    imageView
                )
                "image" -> _location.value!!.imageUri =
                    FirebaseProvider.storage.uploadLocationImage(
                        location.value!!.locationId,
                        filename,
                        imageView
                    )
            }
            _loading.value = loading.value!! - 1
        }
    }

    /**
     * Returns a TimePickerDialog to change operating times of location
     * @param isStartTime true -> change openHour / openMinute
     *                    false -> change closeHour / closeMinute
     * @param day day of week to be changed ( 0 - 6 = Monday - Sunday, 7 = all days)
     * @return instance of TimePickerDialog. Listener manipulates this location.
     */
    fun getTimePicker(isStartTime: Boolean, day: Int): TimePickerDialog {
        val location = _location.value!!
        location.apply {
            val listener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute, _ ->
                if (day == 7) {
                    for (allDays in 0..6) {
                        if (isStartTime) {
                            this.openHour[allDays] = hourOfDay
                            this.openMinute[allDays] = minute
                        } else {
                            this.closeHour[allDays] = hourOfDay
                            this.closeMinute[allDays] = minute
                        }
                    }
                } else {
                    if (isStartTime) {
                        this.openHour[day] = hourOfDay
                        this.openMinute[day] = minute
                    } else {
                        this.closeHour[day] = hourOfDay
                        this.closeMinute[day] = minute
                    }
                }
                _location.value = location
            }

            val timePickerDialog = TimePickerDialog.newInstance(listener, true)
            // Setup timePickerDialog parameter
            val minTime = when {
                isStartTime -> Timepoint(0, 0, 0)
                day == 7 -> Timepoint(location.openHour[0], location.openHour[0], 0)
                else -> Timepoint(location.openHour[day], location.openHour[day], 0)
            }
            val initialTime = when {
                day == 7 && isStartTime -> Timepoint(
                    location.openHour[0],
                    location.openHour[0],
                    0
                )
                day == 7 -> Timepoint(location.closeHour[0], location.closeHour[0], 0)
                isStartTime -> Timepoint(location.openHour[day], location.openHour[day], 0)
                else -> Timepoint(location.closeHour[day], location.closeHour[day], 0)
            }

            // Setup timePickerDialog parameter ToDo: Rework this
            timePickerDialog.apply {
                //this.setMinTime(minTime)
                this.setInitialSelection(initialTime)
                this.setTimeInterval(
                    AddReservationViewModel.HOUR_INTERVAL,
                    AddReservationViewModel.MINUTE_INTERVAL
                )
            }
            return timePickerDialog
        }
    }
}