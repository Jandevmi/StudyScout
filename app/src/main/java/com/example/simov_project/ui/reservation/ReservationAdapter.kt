package com.example.simov_project.ui.reservation

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.simov_project.R
import com.example.simov_project.dataclasses.Reservation
import kotlinx.android.synthetic.main.recylcerview_location.view.*
import kotlinx.android.synthetic.main.recylcerview_reservation.view.*
import java.util.*


class ReservationAdapter(
    private val reservations: List<Reservation>,
    private val icons: HashMap<String, Bitmap>,
    private val locationOwner: Boolean
) :
    RecyclerView.Adapter<CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow = layoutInflater.inflate(R.layout.recylcerview_reservation, parent, false)
        return CustomViewHolder(cellForRow)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        reservations[position].let { reservation ->
            holder.view.apply {
                val durationString = this.context.getString(
                    R.string.reservation_duration,
                    reservation.getStartTime(),
                    reservation.getEndTime()
                )
                this.reservation_name_textView.text = if (locationOwner) reservation.userName
                else reservation.locationName
                this.reservation_date_textView.text = reservation.getDateString()
                this.reservation_duration_textView.text = durationString

                icons[reservation.locationId]?.let {
                    this.reservation_icon_imageView.setImageBitmap(it)
                } ?: holder.view.location_icon_imageView.setImageResource(R.mipmap.ic_picture)

                this.setOnClickListener {
                    val action =
                        ReservationFragmentDirections.actionNavigationReservationToReservationDetail(
                            reservation.reservationId
                        )
                    findNavController().navigate(action)
                }

                val reservationStatus = reservation.getReservationStatus()
                this.reservation_status_textView.apply {
                    val textColorId = when (reservationStatus){
                        ReservationStatus.ACTIVE -> R.color.openGreen
                        ReservationStatus.ACTIVATE -> R.color.colorPrimary
                        ReservationStatus.CANCELLED -> R.color.closeRed
                        ReservationStatus.RESERVED -> R.color.colorPrimaryDark
                        else -> R.color.colorDarkText
                    }
                    text = reservationStatus.status
                    setTextColor(context.getColor(textColorId))
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return reservations.size
    }
}

class CustomViewHolder(val view: View) : RecyclerView.ViewHolder(view) {}