package com.example.simov_project.ui.addReservation

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.view.get
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.simov_project.R
import com.example.simov_project.ui.auth.AuthViewModel
import com.example.simov_project.ui.location.LocationViewModel
import com.example.simov_project.ui.reservation.ReservationViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions

/**
 * Fragment to create and specify a reservation
 * It´s addReservationViewModel will be destroyed with this fragment
 * Using wdullaer.materialdatetimepicker to pick date and times
 */
class AddReservationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_add_reservation, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val locationViewModel: LocationViewModel by activityViewModels()
        val reservationViewModel: ReservationViewModel by activityViewModels()
        val authViewModel: AuthViewModel by activityViewModels()
        val args: AddReservationFragmentArgs by navArgs()

        val location = locationViewModel.getLocalLocation(args.locationId)!! //Location has to exist
        val addReservationModel = AddReservationViewModel(location, authViewModel.user.value!!)
        val context = requireContext()
        var isInitDateTime = true

        val locationImage: ImageView = view.findViewById(R.id.addReservation_imageView)
        val nameView: TextView = view.findViewById(R.id.addReservation_name_textView)
        val addressView: TextView = view.findViewById(R.id.addReservation_address_textView)
        val timesContainer: GridLayout = view.findViewById(R.id.addReservation_times_container)
        val mapView: MapView = view.findViewById(R.id.addReservation_mapView)
        val dateButton: Button = view.findViewById(R.id.addReservation_date_button)
        val startTimeButton: Button = view.findViewById(R.id.addReservation_startTime_button)
        val endTimeButton: Button = view.findViewById(R.id.addReservation_endTime_button)
        val confirmButton: Button = view.findViewById(R.id.addReservation_confirm_button)
        val pickerContainer: ConstraintLayout =
            view.findViewById(R.id.addReservation_picker_container)

        nameView.text = location.name
        addressView.text = getString(R.string.address_string, location.addressString)
        /**
         * Loop to update operating house.
         * Objects in the layoutView are indexed, but anonymous
         * To change the text, they need to be cast to TextView
         */
        for (day in 0..6) {
            val openView = timesContainer[day + 9] as TextView
            val closeView = timesContainer[day + 17] as TextView

            if (locationViewModel.isDayClosed(location, day)) {
                openView.text = "-"
                closeView.text = ""
            } else {
                openView.text = location.getOpenTime(day)
                closeView.text = location.getCloseTime(day)
            }
        }

        // Set marker and position of mapView
        val latLng = LatLng(location.latitude, location.longtitude)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { googleMap ->
            val marker = MarkerOptions().position(latLng)
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            googleMap.addMarker(marker)
        }

        /**
         * Observing the reservation starts intents
         * After setting the the date a timePicker for startTime starts
         * After setting the startTime a timePicker for endTime starts
         * After date/time are set (else path) all Buttons are visible
         *      and the confirmButton adds the reservation to the repository
         * After the reservation has an ID it´s added to the user and reservationViewModel
         *      and the app navigates to reservations
         */
        addReservationModel.reservation.observe(viewLifecycleOwner, Observer {
            if (it.day != null) {
                when {
                    it.startHour == null -> startTimePick(context, addReservationModel, true)
                    it.endHour == null -> startTimePick(context, addReservationModel, false)
                    it.reservationId != "-1" -> {
                        authViewModel.addReservationId(it.reservationId)
                        locationViewModel.addReservationId(args.locationId, it.reservationId)
                        reservationViewModel.addReservation(it)
                        findNavController().navigate(R.id.action_navigation_to_reservation)
                    }
                    else -> {
                        isInitDateTime = false
                        confirmButton.text = getString(R.string.add_reservation_confirm)
                        pickerContainer.visibility = View.VISIBLE
                    }
                }
                dateButton.text = it.getDateString()
                startTimeButton.text = it.getStartTime()
                endTimeButton.text = it.getEndTime()
            }
        })

        /**
         * Find and set location image
         * If no image found -> start download -> observe images
         */
        locationViewModel.images.value!![location.locationId]?.let {
            locationImage.setImageBitmap(it)
        } ?: kotlin.run {
            locationViewModel.downloadImage(location.locationId)
            locationViewModel.images.observe(viewLifecycleOwner, Observer { images ->
                images[location.locationId]?.let { locationImage.setImageBitmap(it) }
            })
        }

        /**
         * date, start- and endTimeButton are not visible until date/time is initialized
         * confirm button first starts date/timePicking, then confirms the reservation
         */
        dateButton.setOnClickListener {
            startDatePick(context, addReservationModel)
        }

        startTimeButton.setOnClickListener {
            startTimePick(context, addReservationModel, true)
        }

        endTimeButton.setOnClickListener {
            startTimePick(context, addReservationModel, false)
        }

        confirmButton.setOnClickListener {
            if (isInitDateTime) {
                startDatePick(context, addReservationModel)
            } else {
                addReservationModel.addReservation()
            }
        }
    }

    /**
     * Starts a DatePickerDialog
     * @param context needed to set the Button colors
     * @param addReservationModel logic for date picking and persist the reservation
     */
    private fun startDatePick(context: Context, addReservationModel: AddReservationViewModel) {
        val datePickerDialog = addReservationModel.getDatePicker().apply {
            this.setTitle(context.getString(R.string.pickDateTitle))
            this.setCancelColor(ContextCompat.getColor(context, R.color.calendarButtonText))
            this.setOkColor(ContextCompat.getColor(context, R.color.calendarButtonText))
            this.accentColor = (ContextCompat.getColor(context, R.color.calendarAccent))
            this.setCancelText(context.getString(R.string.cancelButton))
            this.setOkText(context.getString(R.string.confirmButton))
        }
        datePickerDialog.show(parentFragmentManager, "DatePickerDialog")
    }

    /**
     * Starts a TimePickerDialog
     * @param context needed to set the Button colors
     * @param addReservationModel logic for time picking and persist the reservation
     * @param isStartTime defines if start or end time is set
     */
    private fun startTimePick(
        context: Context,
        addReservationModel: AddReservationViewModel,
        isStartTime: Boolean
    ) {
        val timePickerDialog = addReservationModel.getTimePicker(isStartTime).apply {
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