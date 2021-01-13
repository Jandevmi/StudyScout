package com.example.simov_project.dataclasses

/**
 * A location always needs id, userId and locationId
 * Other parameter are optional for now
 *
 * Note that a ID is created after it has been add to Firestore,
 * thus I recommend create location objects with id -1 and overwrite the the local object
 * once it has been added to Firestore
 */

data class Reservation(
    var reservationId: String = "-1",
    var userId: String = "-1",
    var userName: String = "-1",
    var locationId: String = "-1",
    var locationName: String = "Location name",
    var seatNumber: String = "-1",
    var day: Int? = null,
    var month: Int? = null,
    var year: Int? = null,
    var startHour: Int? = null,
    var startMinute: Int? = null,
    var endHour: Int? = null,
    var endMinute: Int? = null
) {
    fun getDateString(): String? {
        return day?.let {
            "${day.toString().padStart(2, '0')}." +
                    "${month.toString().padStart(2, '0')}.$year"
        }
    }

    fun getIndex(): Int {
        val yearIndicator = (year?.let { it % 2020 } ?: 1) * 100000000
        val monthIndicator = month.toString().padStart(2, '0').toInt() * 1000000
        val dayIndicator = day.toString().padStart(2, '0').toInt() * 10000
        val hourIndicator = startHour.toString().padStart(2, '0').toInt() * 100
        val minuteIndicator = startMinute.toString().padStart(2, '0').toInt()
        return yearIndicator + monthIndicator + dayIndicator + hourIndicator + minuteIndicator
    }

    fun getStartTime(): String? {
        return startHour?.let {
            createTimeString(startHour, startMinute)
        }
    }

    fun getEndTime(): String? {
        return startHour?.let {
            createTimeString(endHour, endMinute)
        }
    }

    private fun createTimeString(hour: Int?, minute: Int?): String {
        return ("${hour.toString().padStart(2, '0')}:" +
                minute.toString().padStart(2, '0'))
    }
}