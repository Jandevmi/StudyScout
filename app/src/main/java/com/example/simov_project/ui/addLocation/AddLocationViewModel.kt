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
import com.google.firebase.ktx.Firebase
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import com.wdullaer.materialdatetimepicker.time.Timepoint
import kotlinx.coroutines.launch

/**
 * Holds the values and logic for the locationFragment
 * @property _location new Location that can be changed inside the viewModel
 * @property location new Location that can be accessed from outside, used to create a location
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
     * Set the location as viewModel location
     * @param location the location object to be set
     */
    fun setLocation(location: Location) {
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

    fun updateLocalLocation(location: Location) {
        _location.value = location
    }

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

    fun uploadImage(filename: String, imageView: ImageView) {
        viewModelScope.launch {
            _loading.value!!.plus(1)
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
            _loading.value!!.minus(1)
        }
    }

    fun getTimePicker(isStartTime: Boolean, day: Int): TimePickerDialog {
        val location = _location.value!!
        location.apply {
            val listener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute, _ ->
                if (day == 7) {
                    for (allDays in 0..6) {
                        if (isStartTime) {
                            this.openTimeHour[allDays] = hourOfDay
                            this.openTimeMinute[allDays] = minute
                        } else {
                            this.closeTimeHour[allDays] = hourOfDay
                            this.closeTimeMinute[allDays] = minute
                        }
                    }
                } else {
                    if (isStartTime) {
                        this.openTimeHour[day] = hourOfDay
                        this.openTimeMinute[day] = minute
                    } else {
                        this.closeTimeHour[day] = hourOfDay
                        this.closeTimeMinute[day] = minute
                    }
                }
                _location.value = location
            }

            val timePickerDialog = TimePickerDialog.newInstance(listener, true)
            // Setup timePickerDialog parameter
            val minTime = when {
                isStartTime -> Timepoint(0, 0, 0)
                day == 7 -> Timepoint(location.openTimeHour[0], location.openTimeHour[0], 0)
                else -> Timepoint(location.openTimeHour[day], location.openTimeHour[day], 0)
            }
            val initialTime = when {
                day == 7 && isStartTime -> Timepoint(
                    location.openTimeHour[0],
                    location.openTimeHour[0],
                    0
                )
                day == 7 -> Timepoint(location.closeTimeHour[0], location.closeTimeHour[0], 0)
                isStartTime -> Timepoint(location.openTimeHour[day], location.openTimeHour[day], 0)
                else -> Timepoint(location.closeTimeHour[day], location.closeTimeHour[day], 0)
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