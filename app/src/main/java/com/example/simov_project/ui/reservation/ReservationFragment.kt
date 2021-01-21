package com.example.simov_project.ui.reservation

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.simov_project.R
import com.example.simov_project.ui.auth.AuthViewModel
import com.example.simov_project.ui.location.LocationViewModel

class ReservationFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val reservationViewModel: ReservationViewModel by activityViewModels()
        val locationViewModel: LocationViewModel by activityViewModels()
        val authViewModel: AuthViewModel by activityViewModels()
        val root = inflater.inflate(R.layout.fragment_reservation, container, false)
        val user = authViewModel.user.value!!
        val reservationList: RecyclerView = root.findViewById(R.id.reservation_recyclerView)
        val loadingView: ProgressBar = root.findViewById(R.id.progress_loader)
        val emptyReservationsView: TextView = root.findViewById(R.id.reservation_empty_textView)

        // Init the the recyclerView and bind it to viewModel
        reservationList.layoutManager = LinearLayoutManager(requireContext())
        reservationViewModel.reservations.observe(viewLifecycleOwner, Observer { itReservations ->
            // Show loading or a text "No reservations yet"
            if (itReservations.isEmpty())
                if (user.reservationIds.isEmpty()) {
                    loadingView.visibility = View.GONE
                    emptyReservationsView.visibility = View.VISIBLE
                } else {
                    loadingView.visibility = View.VISIBLE
                    emptyReservationsView.visibility = View.GONE
                }
            else {
                emptyReservationsView.visibility = View.GONE
                loadingView.visibility = View.GONE
            }
            reservationList.adapter =
                ReservationAdapter(
                    reservationViewModel.reservations.value!!,
                    locationViewModel.icons.value!!,
                    user.locationOwner
                )
        })

        locationViewModel.icons.observe(viewLifecycleOwner, Observer { itIcons ->
            reservationList.swapAdapter(
                ReservationAdapter(
                    reservationViewModel.reservations.value!!,
                    itIcons,
                    user.locationOwner
                ), true
            )
        })

        if (!user.locationOwner)
            reservationViewModel.getReservationsForUser(user.reservationIds)

        return root
    }
}