package com.example.simov_project.ui.location

import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.NavHostFragment.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.example.simov_project.R
import com.example.simov_project.dataclasses.Location
import com.wdullaer.materialdatetimepicker.time.Timepoint
import kotlinx.android.synthetic.main.recylcerview_location.view.*
import java.util.*
import kotlin.collections.HashMap

/**
 * This adopter represents the logic for the recyclerView in locations
 * In onCreateViewHolder the layout for the cells is inflated with R.layout.recylcerview_location
 * In onBindViewHolder the elements of the layout get values
 */
class LocationAdapter(
    private val locations: List<Location>,
    private val icons: HashMap<String, Bitmap>,
    private val distances: HashMap<String, Float>,
    private val fragment: LocationFragment,
    private val userLocationOwner: Boolean
) : RecyclerView.Adapter<CustomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CustomViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val cellForRow = layoutInflater.inflate(R.layout.recylcerview_location, parent, false)
        return CustomViewHolder(cellForRow)
    }

    override fun onBindViewHolder(holder: CustomViewHolder, position: Int) {
        val location = locations[position]
        val addressString = location.addressString
        val context = holder.view.context
        holder.view.addReservation_name_textView.text = location.name
        holder.view.location_address_textview.text = addressString


        holder.view.setOnClickListener {
            val action = if (userLocationOwner)
                LocationFragmentDirections.actionNavigationLocationToAddLocation(location.locationId)
            else LocationFragmentDirections.actionNavigationToReservationAdd(location.locationId)
            findNavController(fragment).navigate(action)
        }

        val calendar = Calendar.getInstance()
        val now = Timepoint(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE))
        val calendarDay = calendar.get(Calendar.DAY_OF_WEEK)
        val day = if (calendarDay == 1) 6 // sunday
        else calendarDay - 2
        holder.view.location_open_textView.apply {
            if (location.isOpen(day, now)) {
                text = context.getString(R.string.open)
                setTextColor(resources.getColor(R.color.openGreen))
            } else {
                text = context.getString(R.string.closed)
                setTextColor(resources.getColor(R.color.closeRed))
            }
        }

        distances[location.locationId]?.let {
            holder.view.location_distance_textview.text = "Distance: $it km"
        }

        icons[location.locationId]?.let {
            holder.view.location_icon_imageView.setImageBitmap(it)
        } ?: holder.view.location_icon_imageView.setImageResource(R.mipmap.ic_picture)

    }

    override fun getItemCount(): Int {
        return locations.size
    }
}

class CustomViewHolder(val view: View) : RecyclerView.ViewHolder(view) {}