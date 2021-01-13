package com.example.simov_project.ui.addLocation

import android.app.Activity
import android.app.AlertDialog
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.navigation.navGraphViewModels
import com.example.simov_project.R
import com.example.simov_project.ui.auth.AuthViewModel
import com.example.simov_project.ui.location.LocationViewModel
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.libraries.places.widget.Autocomplete
import com.google.android.libraries.places.widget.AutocompleteActivity
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode

class AddLocationFragment : Fragment() {

    private lateinit var placesClient: PlacesClient

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        if (!Places.isInitialized()) {
            Places.initialize(requireContext(), resources.getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(requireContext())
        return inflater.inflate(R.layout.fragment_add_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val addLocationViewModel: AddLocationViewModel by navGraphViewModels(R.id.editLocation_nav_graph)
        val locationViewModel: LocationViewModel by activityViewModels()
        val authViewModel: AuthViewModel by activityViewModels()

        // Inflate the layout and bind it´s elements
        val nameView: EditText = view.findViewById(R.id.addLocation_name_editText)
        val addressView: EditText = view.findViewById((R.id.addLocation_address_editText))
        val latitudeView: EditText = view.findViewById(R.id.addLocation_latitude_editText)
        val longitudeView: EditText = view.findViewById(R.id.addLocation_longitude_editText)
        val seatNumberView: EditText = view.findViewById(R.id.addLocation_seats_editText)
        val searchContainer: LinearLayout = view.findViewById(R.id.addLocation_search_container)
        val googleIdView: TextView = view.findViewById(R.id.addLocation_googleId_textView)
        val timesButton: Button = view.findViewById(R.id.addLocation_times_button)
        val deleteButton: ImageView = view.findViewById(R.id.location_delete_imageView)

        val user = authViewModel.user.value!!
        val args: AddLocationFragmentArgs by navArgs()

        addLocationViewModel.location.observe(viewLifecycleOwner, Observer { itLocation ->
            nameView.setText(itLocation.name)
            addressView.setText(itLocation.addressString)
            latitudeView.setText(itLocation.latitude.toString())
            longitudeView.setText(itLocation.longtitude.toString())
            googleIdView.text = itLocation.googleId
            itLocation.maxSeats?.let {
                seatNumberView.setText(it.toString())
            }

            /**
             * If a new location the fragment transaction has to wait for the locationID
             */
            if (args.locationId == "-1" && itLocation.locationId != "-1") {
                authViewModel.addLocationId(itLocation.locationId)
                findNavController().navigate(R.id.action_navigation_addLocation_to_addTimes)
            }
        })

        /**
         * Hide delete Button if new location
         * Copy location attributes if old location
         */
        if (args.locationId == "-1") {
            deleteButton.visibility = View.GONE
        } else {
            deleteButton.visibility = View.VISIBLE
            addLocationViewModel.setLocation(locationViewModel.locations.value!!.find { it.locationId == args.locationId }!!) //Fixme !!
            addLocationViewModel.downloadImages()
        }

        searchContainer.setOnClickListener {
            val fields: List<Place.Field> =
                listOf(Place.Field.ID, Place.Field.NAME, Place.Field.LAT_LNG, Place.Field.ADDRESS)

            // Start the autocomplete intent.
            val intent = Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY, fields)
                //.setTypeFilter(TypeFilter.ESTABLISHMENT).setCountry("de")
                .build(requireContext())
            startActivityForResult(intent, AUTOCOMPLETE_REQUEST_CODE)
        }

        deleteButton.setOnClickListener {
            val location = addLocationViewModel.location.value!!
            val builder = AlertDialog.Builder(this.context)
            builder.setMessage("Delete Location ${location.name}?")
                .setCancelable(false)
                .setPositiveButton("Delete") { _, _ ->
                    if (user.locationIds.remove(location.locationId)) {
                        authViewModel.updateUser(user)
                        locationViewModel.deleteLocation(location.locationId)
                        findNavController().navigate(R.id.action_navigation_addLocation_to_location)
                    }
                }
                .setNegativeButton("Keep") { dialog, _ ->
                    // Dismiss the dialog
                    dialog.dismiss()
                }
            val alert = builder.create()
            alert.show()
        }

        timesButton.setOnClickListener {
            val toastStringId = when {
                nameView.text.isNullOrBlank() -> {
                    nameView.requestFocus()
                    R.string.enter_a_name_for_the_location
                }
                addressView.text.isNullOrBlank() -> {
                    addressView.requestFocus()
                    R.string.enter_the_address
                }
                latitudeView.text.isNullOrBlank() -> {
                    latitudeView.requestFocus()
                    R.string.enter_latitude
                }
                longitudeView.text.isNullOrBlank() -> {
                    longitudeView.requestFocus()
                    R.string.enter_longitude
                }
                seatNumberView.text.isNullOrBlank() -> {
                    seatNumberView.requestFocus()
                    R.string.enter_seat_count
                }
                else -> null
            }
            toastStringId?.let {
                Toast.makeText(requireContext(), getString(it), Toast.LENGTH_SHORT).apply {
                    setGravity(Gravity.BOTTOM, 0, 600)
                    show()
                }
            } ?: kotlin.run {
                addLocationViewModel.location.value!!.apply {
                    userId = user.userId
                    name = nameView.text.toString()
                    addressString = addressView.text.toString()
                    latitude = latitudeView.text.toString().toDouble()
                    longtitude = longitudeView.text.toString().toDouble()
                    maxSeats = seatNumberView.text.toString().toInt()
                    addLocationViewModel.updateFirebaseLocation()
                    if (args.locationId != "-1")
                        findNavController().navigate(R.id.action_navigation_addLocation_to_addTimes)
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        val addLocationViewModel: AddLocationViewModel by navGraphViewModels(R.id.editLocation_nav_graph)
        if (requestCode == AUTOCOMPLETE_REQUEST_CODE) {
            when (resultCode) {
                Activity.RESULT_OK -> {
                    data?.let {
                        val place = Autocomplete.getPlaceFromIntent(data)
                        val newLocation = addLocationViewModel.location.value!!.apply {
                            this.name = place.name
                            this.googleId = place.id
                            this.addressString = place.address
                            this.latitude = place.latLng?.latitude ?: 0.0
                            this.longtitude = place.latLng?.longitude ?: 0.0
                        }
                        addLocationViewModel.updateLocalLocation(newLocation)
                    }
                }
                AutocompleteActivity.RESULT_ERROR -> {
                    // TODO: Handle the error.
                    data?.let {
                        val status = Autocomplete.getStatusFromIntent(data)
                        Log.i(ContentValues.TAG, status.statusMessage)
                    }
                }
                Activity.RESULT_CANCELED -> {
                    // The user canceled the operation.
                }
            }
            return
        }
        super.onActivityResult(requestCode, resultCode, data)
    }


    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
        private const val AUTOCOMPLETE_REQUEST_CODE = 2
    }
}