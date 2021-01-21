package com.example.simov_project.dataclasses

import com.wdullaer.materialdatetimepicker.time.Timepoint

data class Location(
    var locationId: String = "-1", // FixMe: Don´t write to Firestore
    var userId: String = "-1",
    var reservationIds: MutableList<String>? = mutableListOf(),
    var googleId: String? = null,
    var name: String? = null,
    var addressString: String? = null,
    var latitude: Double = 41.1784687,
    var longtitude: Double = -8.608977699999999,
    var maxSeats: Int? = null,
    var bookedSeats: Int? = null,
    var openHour: MutableList<Int> = mutableListOf(0, 0, 0, 0, 0, 0, 0),
    var openMinute: MutableList<Int> = mutableListOf(0, 0, 0, 0, 0, 0, 0),
    var closeHour: MutableList<Int> = mutableListOf(0, 0, 0, 0, 0, 0, 0),
    var closeMinute: MutableList<Int> = mutableListOf(0, 0, 0, 0, 0, 0, 0),
    var iconUri: String? = null,
    var imageUri: String? = null
) {
    // FixMe: Don´t write these times to Firestore
    fun getOpenTime(day: Int): String {
        return createTimeString(openHour[day], openMinute[day])
    }

    fun getCloseTime(day: Int): String {
        return createTimeString(closeHour[day], closeMinute[day])
    }

    fun isDayClosed(day: Int): Boolean {
        return openHour[day] == closeHour[day] &&
                openMinute[day] == closeMinute[day]
    }

    fun isOpen(day: Int, now: Timepoint): Boolean {
        val open = Timepoint(openHour[day], openMinute[day], 0)
        val close = Timepoint(closeHour[day], closeMinute[day], 0)
        return open.toSeconds() < now.toSeconds() && now.toSeconds() < close.toSeconds()
    }

    private fun createTimeString(hour: Int, minute: Int) :String {
        return ("${hour.toString().padStart(2, '0')}:" +
                minute.toString().padStart(2, '0'))
    }
}