package com.example.simov_project.ui.location

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simov_project.R
import com.example.simov_project.ui.auth.AuthViewModel
import com.google.android.gms.location.LocationServices

class LocationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_location, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val locationViewModel: LocationViewModel by activityViewModels()
        val authViewModel: AuthViewModel by activityViewModels()
        val locationList: RecyclerView = view.findViewById(R.id.location_recyclerView)
        val loadingView: ProgressBar = view.findViewById(R.id.progress_loader)
        val user = authViewModel.user.value

        // Init the the recyclerView and bind it to viewModel
        locationList.layoutManager = LinearLayoutManager(requireContext())
        locationViewModel.locations.observe(viewLifecycleOwner, Observer { itLocations ->

            if (itLocations.isEmpty())
                loadingView.visibility = View.VISIBLE
            else {
                loadingView.visibility = View.GONE
                // Start sorting if there was an change in locations
                val locationIds = mutableSetOf<String>()
                itLocations.forEach { locationIds.add(it.locationId) }
                if (locationViewModel.distances.value!!.keys.toSet() != locationIds)
                    calculateDistances()
            }

            user?.let {
                locationList.swapAdapter(
                    LocationAdapter(
                        itLocations,
                        locationViewModel.icons.value!!,
                        locationViewModel.distances.value!!,
                        this,
                        user.locationOwner
                    ), true
                )
            }
        })

        locationViewModel.icons.observe(viewLifecycleOwner, Observer { itIcons ->
            user?.let {
                locationList.swapAdapter(
                    LocationAdapter(
                        locationViewModel.locations.value!!,
                        itIcons,
                        locationViewModel.distances.value!!,
                        this,
                        user.locationOwner
                    ), true
                )
            }
        })
    }

    private fun calculateDistances(){
        val locationViewModel: LocationViewModel by activityViewModels()
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireContext())
        if (ActivityCompat.checkSelfPermission(requireContext(),
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            fusedLocationClient.lastLocation.addOnSuccessListener {
                locationViewModel.calculateDistances(it.latitude, it.longitude)
            }
        }
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

}