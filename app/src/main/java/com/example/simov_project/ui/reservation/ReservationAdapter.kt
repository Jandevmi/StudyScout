package com.example.simov_project.ui.reservation

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat.startActivity
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
    private val reservationFragment: ReservationFragment,
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
                        ReservationFragmentDirections.actionNavigationReservationToReservationAdd(
                            reservation.locationId
                        )
                    findNavController().navigate(action)
                }

                this.reservation_calendar_imageView.setOnClickListener {
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

                    val i = Intent(Intent.ACTION_EDIT)
                    i.type = "vnd.android.cursor.item/event"
                    i.putExtra("beginTime", startCalendar.timeInMillis)
                    i.putExtra("endTime", endCalendar.timeInMillis)
                    i.putExtra("allDay", false)
                    i.putExtra("title", "Study reservation at ${reservation.locationName}")
                    startActivity(this.context, i, null)
                }


                this.reservation_delete_imageView.setOnClickListener {
                    val builder = AlertDialog.Builder(this.context)
                    builder.setMessage("Delete Reservation at ${reservation.locationName}?")
                        .setCancelable(false)
                        .setPositiveButton("Delete") { _, _ ->
                            reservationFragment.deleteReservation(
                                reservation.reservationId,
                                reservation.locationId
                            )
                        }
                        .setNegativeButton("Keep") { dialog, _ ->
                            // Dismiss the dialog
                            dialog.dismiss()
                        }
                    val alert = builder.create()
                    alert.show()
                }
            }
        }
    }

    override fun getItemCount(): Int {
        return reservations.size
    }
}

class CustomViewHolder(val view: View) : RecyclerView.ViewHolder(view) {}