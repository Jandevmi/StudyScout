package com.example.simov_project.ui.addReservation

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.*
import com.example.simov_project.dataclasses.Location
import com.example.simov_project.dataclasses.Reservation
import com.example.simov_project.dataclasses.User
import com.example.simov_project.db.FirebaseProvider
import com.wdullaer.materialdatetimepicker.date.DatePickerDialog;
import com.wdullaer.materialdatetimepicker.time.TimePickerDialog
import com.wdullaer.materialdatetimepicker.time.Timepoint
import kotlinx.coroutines.launch
import java.util.*
import kotlin.math.min

/**
 * This ViewModel uses data from location/user/date + timePicker to create a reservation
 */
class AddReservationViewModel(val location: Location, val user: User) : ViewModel() {

    private val _reservation = MutableLiveData<Reservation>().apply {
        value = Reservation(
            locationId = location.locationId,
            locationName = location.name ?: "Location name not found",
            userId = user.userId,
            userName = user.name
        )
    }
    private val _locationImage = MutableLiveData<Bitmap>()
    val reservation: LiveData<Reservation> = _reservation
    val locationImage: LiveData<Bitmap> = _locationImage

    init {
        if (location.imageUri != null) {
            viewModelScope.launch {
                _locationImage.value =
                    FirebaseProvider.storage.downloadLocationImage(location.locationId, "image")
            }
        }
    }

    fun setLocalReservation(reservation: Reservation) {
        _reservation.value = reservation
    }

    /**
     * Creates a new reservation. The change in _reservation will also add the reservationId to User
     * @param reservation The reservation object to be written to the database
     */
    fun addReservation() {
        val reservation = reservation.value!!
        viewModelScope.launch {
            FirebaseProvider.db.createReservation(reservation)?.let {
                reservation.reservationId = it
                _reservation.value = reservation
            }
        }
    }

    /**
     * @return DateTimePickerDialog:
     * Possible Dates from now to now + MAX_RESERVATION_MONTHS + MAX_RESERVATION_DAYS.
     * Disables Days that are closed in the viewModels location.
     * OnDateSetListener -> sets the date to the viewModels reservation.
     */
    fun getDatePicker(): DatePickerDialog {
        val listener = DatePickerDialog.OnDateSetListener { _, year, monthOfYear, dayOfMonth ->
            val reservation = reservation.value
            reservation?.apply {
                reservation.year = year
                reservation.month = monthOfYear + 1
                reservation.day = dayOfMonth

                // if date changes -> check if reservation times are still possible
                reservation.startHour?.let { startHour ->
                    val calendar = Calendar.getInstance()
                    calendar.set(year, monthOfYear, dayOfMonth)
                    val weekDay = convertDayToCalendarDay(calendar.get(Calendar.DAY_OF_WEEK))
                    if (startHour <= location.openTimeHour[weekDay]) {
                        location.openTimeHour[weekDay]
                        reservation.startMinute?.let { minute ->
                            if (minute <= location.openTimeMinute[weekDay])
                                location.openTimeMinute[weekDay]
                        }
                    }
                    reservation.endHour?.let { endHour ->
                        if (endHour <= location.closeTimeHour[weekDay]) {
                            location.closeTimeHour[weekDay]
                            reservation.endMinute?.let { minute ->
                                if (minute <= location.closeTimeMinute[weekDay])
                                    location.closeTimeMinute[weekDay]
                            }
                        }
                    }
                }
            }
            _reservation.value = reservation
        }
        val minDate = Calendar.getInstance()
        val maxDate = Calendar.getInstance()
        val initDate = Calendar.getInstance()
        reservation.value?.let {
            it.day?.let { _ ->
                initDate.set(it.year!!, it.month!! - 1, it.day!!)
            }
        }
        val datePickerDialog = DatePickerDialog.newInstance(
            listener, initDate
        )
        maxDate.add(Calendar.MONTH, MAX_RESERVATION_MONTHS)
        maxDate.add(Calendar.DAY_OF_MONTH, MAX_RESERVATION_DAYS)
        datePickerDialog.apply {
            this.minDate = minDate
            this.maxDate = maxDate
        }

        // Disable closed days
        while (minDate.before(maxDate)) {
            val day = convertDayToCalendarDay(minDate.get(Calendar.DAY_OF_WEEK))
            if (location.isDayClosed(day)) {
                val disabledDays = arrayOf(minDate)
                datePickerDialog.disabledDays = disabledDays
            }
            minDate.add(Calendar.DAY_OF_MONTH, 1)
        }
        return datePickerDialog
    }

    /**
     * @param isStartTime true -> creates startHour/Minute; false -> creates endHour/Minute
     * @return TimePickerDialog:
     * Possible time are defined by the locations operating hours.
     * Min endTime is the chosen startTime + MIN_RESERVATION_HOURS + MIN_RESERVATION_MINUTES.
     * Interval defined by HOUR_INTERVAL, MINUTE_INTERVAL.
     * OnTimeSetListener -> sets the time to the viewModels reservation.
     */
    fun getTimePicker(isStartTime: Boolean): TimePickerDialog {
        val reservation = reservation.value!!
        reservation.apply {
            val listener = TimePickerDialog.OnTimeSetListener { _, hourOfDay, minute, _ ->
                if (isStartTime) {
                    this.startHour = hourOfDay
                    this.startMinute = minute
                } else {
                    this.endHour = hourOfDay
                    this.endMinute = minute
                }
                _reservation.value = reservation
            }
            val timePickerDialog = TimePickerDialog.newInstance(listener, HOUR_MODE_24)
            val calendar = Calendar.getInstance()
            calendar.set(this.year!!, this.month!!, this.day!!)
            val weekDay = convertDayToLocationDay(calendar.get(Calendar.DAY_OF_WEEK))
            val closeTime =
                Timepoint(location.closeTimeHour[weekDay], location.closeTimeMinute[weekDay], 0)
            val openTime = if (isStartTime)
                Timepoint(location.openTimeHour[weekDay], location.openTimeMinute[weekDay], 0)
            else Timepoint(
                this.startHour!! + MIN_RESERVATION_HOURS,
                this.startMinute!! + MIN_RESERVATION_MINUTES, 0
            )
            val initialTime = if (isStartTime)
                Timepoint(0, 0, 0)
            else closeTime

            // Setup timePickerDialog parameter
            timePickerDialog.apply {
                this.setMinTime(openTime)
                this.setMaxTime(closeTime)
                this.setInitialSelection(initialTime)
                this.setTimeInterval(HOUR_INTERVAL, MINUTE_INTERVAL)
            }
            return timePickerDialog
        }
    }

    private fun convertDayToCalendarDay(calendarDay: Int): Int {
        return if (calendarDay == 1) 6 // sunday
        else calendarDay - 2
    }

    private fun convertDayToLocationDay(calendarDay: Int): Int {
        return if (calendarDay >= 5) calendarDay - 5
        else calendarDay + 2
    }

    companion object {
        const val MAX_RESERVATION_MONTHS = 1
        const val MAX_RESERVATION_DAYS = 0
        const val MIN_RESERVATION_HOURS = 1
        const val MIN_RESERVATION_MINUTES = 0 // does not work if startMinutes is > 25
        const val HOUR_INTERVAL = 1
        const val MINUTE_INTERVAL = 5
        const val HOUR_MODE_24 = true
    }
}