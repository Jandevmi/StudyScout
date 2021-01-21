package com.example.simov_project.ui.addLocation

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.navGraphViewModels
import com.example.simov_project.R

/**
 * Fragment to specify the operating hours of the location
 */
class AddLocationTimesFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_add_location_times, container, false)
        var isStartTime = false
        var endTimeDay = 0

        val context = requireContext()
        val addLocationViewModel: AddLocationViewModel by navGraphViewModels(R.id.editLocation_nav_graph)
        val timesContainer: GridLayout = root.findViewById(R.id.addLocationTimes_container)
        val imageButton: Button = root.findViewById(R.id.addLocation_image_button)
        val columnCount = timesContainer.columnCount

        addLocationViewModel.location.observe(viewLifecycleOwner, Observer {
            for (day in 0..6) {
                val openView = timesContainer[(day + 1) * columnCount + 1] as TextView
                val closeView = timesContainer[(day + 1) * columnCount + 2] as TextView
                openView.text = it.getOpenTime(day)
                closeView.text = it.getCloseTime(day)
            }
            // start end time pick
            if (isStartTime) {
                isStartTime = false
                startTimePick(context, addLocationViewModel, endTimeDay, false)
            }
        })

        /**
         * Set onClick listeners for the time buttons
         * day 7 is all button
         */
        for (day in 0..7) {
            val buttonNumber = if (day == 7) 0 else (day + 1) * columnCount
            val checkButtonNumber = (day + 1) * columnCount + 3
            val timePickerButton = timesContainer[buttonNumber] as Button
            timePickerButton.setOnClickListener {
                isStartTime = true
                endTimeDay = day
                startTimePick(context, addLocationViewModel, day, true)
            }
            if (day != 7) { // No close all button
                val closeButton = timesContainer[checkButtonNumber] as ImageButton
                closeButton.setOnClickListener {
                    val location = addLocationViewModel.location.value!!
                    location.openHour[day] = 0
                    location.openMinute[day] = 0
                    location.closeHour[day] = 0
                    location.closeHour[day] = 0
                    addLocationViewModel.updateLocalLocation(location)
                }
            }
        }

        // Update location and navigate to addImage
        imageButton.setOnClickListener {
            addLocationViewModel.updateFirebaseLocation()
            findNavController().navigate(R.id.action_navigation_addLocationTimes_to_addLocationImage)
        }

        return root
    }

    /**
     * Starts a TimePickerDialog
     * @param context needed to set the Button colors
     * @param addLocationViewModel logic for time picking and persist the reservation
     * @param day day of week
     * @param isStartTime defines if start or end time is set
     */
    private fun startTimePick(
        context: Context,
        addLocationViewModel: AddLocationViewModel,
        day: Int,
        isStartTime: Boolean
    ) {
        val timePickerDialog = addLocationViewModel.getTimePicker(isStartTime, day).apply {
            this.setCancelColor(ContextCompat.getColor(context, R.color.calendarButtonText))
            this.setOkColor(ContextCompat.getColor(context, R.color.calendarButtonText))
            this.accentColor = (ContextCompat.getColor(context, R.color.calendarAccent))
            this.setCancelText(context.getString(R.string.cancelButton))
            this.setOkText(context.getString(R.string.confirmButton))
            this.title = if (isStartTime)
                context.getString(R.string.pickStartTimeTitle)
            else context.getString(R.string.pickEndTimeTitle)
        }
        if (isStartTime)
            timePickerDialog.show(parentFragmentManager, "StartTimePicker")
        else
            timePickerDialog.show(parentFragmentManager, "EndTimePicker")
    }
}

