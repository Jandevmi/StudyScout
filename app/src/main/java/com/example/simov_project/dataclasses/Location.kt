package com.example.simov_project.dataclasses

import android.util.Log
import com.wdullaer.materialdatetimepicker.time.Timepoint
import java.sql.Time

/**
 * A location always needs id, userId and name
 * Other parameter are optional for now
 *
 * Note that a ID is created after it has been add to Firestore
 * I recommend to create location objects with id "-1" and overwrite the the local object
 * once it has been added to Firestore
 *
 * Opening times are represented in 2 Arrays openTime and closeTime,
 * with 7 fields each for one weekday, starting with Monday
 *
 * Default lat / long = ISEP
 */

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
    var openTimeHour: MutableList<Int> = mutableListOf(0, 0, 0, 0, 0, 0, 0),
    var openTimeMinute: MutableList<Int> = mutableListOf(0, 0, 0, 0, 0, 0, 0),
    var closeTimeHour: MutableList<Int> = mutableListOf(0, 0, 0, 0, 0, 0, 0),
    var closeTimeMinute: MutableList<Int> = mutableListOf(0, 0, 0, 0, 0, 0, 0),
    var iconUri: String? = null,
    var imageUri: String? = null
) {
    // FixMe: Don´t write these times to Firestore
    fun getOpenTime(day: Int): String {
        return createTimeString(openTimeHour[day], openTimeMinute[day])
    }

    fun getCloseTime(day: Int): String {
        return createTimeString(closeTimeHour[day], closeTimeMinute[day])
    }

    fun isDayClosed(day: Int): Boolean {
        return openTimeHour[day] == closeTimeHour[day] &&
                openTimeMinute[day] == closeTimeMinute[day]
    }

    fun isOpen(day: Int, now: Timepoint): Boolean {
        val open = Timepoint(openTimeHour[day], openTimeMinute[day], 0)
        val close = Timepoint(closeTimeHour[day], closeTimeMinute[day], 0)
        return open.toSeconds() < now.toSeconds() && now.toSeconds() < close.toSeconds()
    }

    private fun createTimeString(hour: Int, minute: Int) :String {
        return ("${hour.toString().padStart(2, '0')}:" +
                minute.toString().padStart(2, '0'))
    }
}