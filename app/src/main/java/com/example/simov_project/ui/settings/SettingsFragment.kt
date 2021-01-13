package com.example.simov_project.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.example.simov_project.R
import com.example.simov_project.ui.auth.AuthViewModel
import com.example.simov_project.ui.location.LocationFragmentDirections
import com.example.simov_project.ui.location.LocationViewModel
import com.example.simov_project.ui.reservation.ReservationViewModel

class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val root = inflater.inflate(R.layout.fragment_settings, container, false)
        val authViewModel: AuthViewModel by activityViewModels()
        val reservationViewModel: ReservationViewModel by activityViewModels()
        val locationViewModel: LocationViewModel by activityViewModels()
        val emailView: TextView = root.findViewById(R.id.settings_email_textView)
        val logoutButton: Button = root.findViewById(R.id.settings_logout_button)
        val ownerButton: Button = root.findViewById(R.id.settings_owner_button)
        val testButton: Button = root.findViewById(R.id.settings_test_button)
        val locationAddButton: Button = root.findViewById(R.id.settings_add_location_button)
        val user = authViewModel.user.value!!
        emailView.text = "Your email: " + user.email

        logoutButton.setOnClickListener {
            if(authViewModel.logout())
                findNavController().navigate(R.id.action_navigation_to_login)
        }


        if (user.locationOwner){
            locationAddButton.visibility = View.VISIBLE
            ownerButton.text = getString(R.string.owner_off)
        }
        else {
            locationAddButton.visibility = View.GONE
            ownerButton.text = getString(R.string.owner_on)
        }

        testButton.setOnClickListener {
            findNavController().navigate(R.id.action_navigtion_settings_to_maps)
        }

        ownerButton.setOnClickListener {
            user.locationOwner = !user.locationOwner
            authViewModel.updateUser(user)
            if (user.locationOwner){
                locationAddButton.visibility = View.VISIBLE
                ownerButton.text = getString(R.string.owner_off)
                reservationViewModel.getReservationsForLocations(user.locationIds)
                locationViewModel.getFirebaseLocationsForUser(user.locationIds)
            }
            else {
                locationAddButton.visibility = View.GONE
                ownerButton.text = getString(R.string.owner_on)
                reservationViewModel.getReservationsForUser(user.reservationIds)
                locationViewModel.getAllFirebaseLocations()
            }
        }

        locationAddButton.setOnClickListener {
            val action = SettingsFragmentDirections.actionNavigationSettingsToAddLocationFragment(locationId = "-1")
            NavHostFragment.findNavController(this).navigate(action)
        }

        return root
    }

    //override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
    //    setPreferencesFromResource(R.xml.root_preferences, rootKey)
    //}
}