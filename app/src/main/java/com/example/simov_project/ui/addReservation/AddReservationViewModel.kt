package com.example.simov_project.ui.addReservation

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

/**
 * This ViewModel uses data from location/user/date + timePicker to create a reservation
 */
class AddReservationViewModel(val location: Location, val user: User) : ViewModel() {

    private val _reservation = MutableLiveData<Reservation>().apply {
        value = Reservation(
            locationId = location.locationId,
            locationName = location.name ?: "Location name not found",
            address = location.addressString ?: "Address not found",
            userId = user.userId,
            userName = user.name
        )
    }
    val reservation: LiveData<Reservation> = _reservation

    /**
     * Creates a new reservation. The change in _reservation will also add the reservationId to User
     * @param reservation The reservation object to be written to the database
     */
    fun addReservation() {
        val reservation = reservation.value!!
        viewModelScope.launch {
            FirebaseProvider.db.updateReservation(reservation)?.let {
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
                    if (startHour <= location.openHour[weekDay]) {
                        location.openHour[weekDay]
                        reservation.startMinute?.let { minute ->
                            if (minute <= location.openMinute[weekDay])
                                location.openMinute[weekDay]
                        }
                    }
                    reservation.endHour?.let { endHour ->
                        if (endHour <= location.closeHour[weekDay]) {
                            location.closeHour[weekDay]
                            reservation.endMinute?.let { minute ->
                                if (minute <= location.closeMinute[weekDay])
                                    location.closeMinute[weekDay]
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
            val closeTime: Timepoint
            var openTime: Timepoint
            var initialTime: Timepoint
            val calendar = Calendar.getInstance()
            calendar.set(this.year!!, this.month!!, this.day!!)
            val weekDay = convertDayToLocationDay(calendar.get(Calendar.DAY_OF_WEEK))

            closeTime = Timepoint(location.closeHour[weekDay], location.closeMinute[weekDay])
            initialTime =
                Timepoint(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), 0)
            if (isStartTime) {
                openTime = Timepoint(location.openHour[weekDay], location.openMinute[weekDay], 0)
                if (initialTime.toSeconds() > openTime.toSeconds())
                    openTime = initialTime
            } else {
                openTime =
                    Timepoint(this.startHour!! + MIN_RESERVATION_HOURS, this.startMinute!!, 0)
                initialTime = closeTime
            }


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