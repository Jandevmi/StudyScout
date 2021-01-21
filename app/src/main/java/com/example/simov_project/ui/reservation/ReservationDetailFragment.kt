package com.example.simov_project.ui.reservation

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.simov_project.R
import com.example.simov_project.ui.auth.AuthViewModel
import com.example.simov_project.ui.location.LocationViewModel
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.zxing.integration.android.IntentIntegrator
import java.util.*

class ReservationDetailFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val reservationViewModel: ReservationViewModel by activityViewModels()
        val locationViewModel: LocationViewModel by activityViewModels()
        val authViewModel: AuthViewModel by activityViewModels()
        val args: ReservationDetailFragmentArgs by navArgs()
        val reservationId = args.reservationId
        val reservation =
            reservationViewModel.reservations.value!!.find { it.reservationId == reservationId }!!
        val locationId = reservation.locationId
        val location = locationViewModel.locations.value!!.find { it.locationId == locationId }!!

        val root = inflater.inflate(R.layout.fragment_reservation_detail, container, false)
        val locationImage: ImageView = root.findViewById(R.id.reservationDetail_imageView)
        val addressView: TextView = root.findViewById(R.id.reservationDetail_address_textView)
        val dateView: TextView = root.findViewById(R.id.reservationDetail_date_textView)
        val timeView: TextView = root.findViewById(R.id.reservationDetail_time_textView)
        val mapView: MapView = root.findViewById(R.id.reservationDetail_mapView)
        val deleteButton: ImageView = root.findViewById(R.id.reservationDetail_delete_imageView)
        val calendarButton: ImageView = root.findViewById(R.id.reservationDetail_calendar_imageView)
        val activateButton: Button = root.findViewById(R.id.reservationDetail_activate_button)

        addressView.text = reservation.address
        dateView.text = reservation.getDateString()
        timeView.text = getString(
            R.string.reservation_duration,
            reservation.getStartTime(),
            reservation.getEndTime()
        )

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

        val latLng = LatLng(location.latitude, location.longtitude)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync { googleMap ->
            val marker = MarkerOptions().position(latLng)
            googleMap.moveCamera(CameraUpdateFactory.newLatLng(latLng))
            googleMap.addMarker(marker)
        }

        deleteButton.setOnClickListener {
            val builder = AlertDialog.Builder(this.context)
            builder.setMessage("Delete Reservation at ${reservation.locationName}?")
                .setCancelable(false)
                .setPositiveButton("Delete") { _, _ ->
                    reservationViewModel.deleteReservation(reservationId)
                    locationViewModel.removeReservationId(reservationId, locationId)
                    authViewModel.removeReservationId(reservationId)
                    findNavController().navigate(R.id.action_navigation_reservationDetail_to_reservation)
                }
                .setNegativeButton("Keep") { dialog, _ ->
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        calendarButton.setOnClickListener {
            val startCalendar = Calendar.getInstance()
            val endCalendar = Calendar.getInstance()
            startCalendar.set(
                reservation.year!!,
                reservation.month!! - 1,
                reservation.day!!,
                reservation.startHour!!,
                reservation.startMinute!!
            )
            endCalendar.set(
                reservation.year!!,
                reservation.month!! - 1,
                reservation.day!!,
                reservation.endHour!!,
                reservation.endMinute!!
            )

            val intent = Intent(Intent.ACTION_EDIT)
            intent.type = "vnd.android.cursor.item/event"
            intent.putExtra("beginTime", startCalendar.timeInMillis)
            intent.putExtra("endTime", endCalendar.timeInMillis)
            intent.putExtra("allDay", false)
            intent.putExtra("title", "Study reservation at ${reservation.locationName}")
            ContextCompat.startActivity(requireContext(), intent, null)
        }

        if (reservation.getReservationStatus() == ReservationStatus.ACTIVATE) {
            activateButton.visibility = View.VISIBLE
            activateButton.setOnClickListener {
                reservationViewModel.reservation = reservation
                val scanner = IntentIntegrator.forSupportFragment(this)
                scanner.setDesiredBarcodeFormats(IntentIntegrator.QR_CODE)
                scanner.initiateScan()
                findNavController().navigate(R.id.action_navigation_reservationDetail_to_reservation)
            }
        } else
            activateButton.visibility = View.GONE
        return root
    }


    /**
     * Result from QR code scanner
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val reservationViewModel: ReservationViewModel by activityViewModels()
        val result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        var toastString = "Nothing!"
        if (result != null)
            toastString = if (result.contents == null) "cancelled"
            else reservationViewModel.activateReservation(result.contents)
        else super.onActivityResult(requestCode, resultCode, data)
        Toast.makeText(requireContext(), toastString, Toast.LENGTH_LONG).show()
    }
}