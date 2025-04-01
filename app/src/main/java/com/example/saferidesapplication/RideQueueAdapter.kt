package com.example.saferidesapplication

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.saferidesapplication.network.dto.RideResponse

class RideQueueAdapter(

    private val rides: List<RideResponse>,
    private val currentUserId: String,
    private val isDriverView: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_RIDE = 0
        private const val TYPE_FOOTER = 1
    }

    private val visibleRides = if (rides.size > 5) rides.take(5) else rides
    private val hasFooter = rides.size > 5
    private val hiddenCount = rides.size - visibleRides.size

    inner class RideViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val pickupText: TextView = view.findViewById(R.id.pickupText)
        val dropoffText: TextView = view.findViewById(R.id.dropoffText)
        val passengerCountText: TextView = view.findViewById(R.id.passengerCountText)
        val card: CardView = view.findViewById(R.id.rideCard)
    }



    inner class FooterViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val footerText: TextView = view.findViewById(R.id.footerText)
    }

    override fun getItemViewType(position: Int): Int {
        return if (hasFooter && position == visibleRides.size) TYPE_FOOTER else TYPE_RIDE
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_RIDE) {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_ride_queue, parent, false)
            RideViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_queue_footer, parent, false)
            FooterViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RideViewHolder) {
            val ride = visibleRides[position]
            holder.pickupText.text = "Pickup: ${ride.pickupLocation}"
            holder.dropoffText.text = "Dropoff: ${ride.dropoffLocation}"
            holder.passengerCountText.text = "Passengers: ${ride.passengerCount}"
            if (!isDriverView) {
                // Passenger view: highlight their own ride
                if (ride.passengerId == currentUserId) {
                    holder.card.setCardBackgroundColor(Color.parseColor("#C8E6C9")) // green
                } else {
                    holder.card.setCardBackgroundColor(Color.WHITE)
                }
            } else {
                // Driver view
                when {
                    ride.driverId == currentUserId && ride.status in listOf("assigned", "arrived", "picked_up", "in_progress") -> {
                        holder.card.setCardBackgroundColor(Color.parseColor("#C8E6C9")) // green
                    }
                    position == 0 && ride.status == "queued" -> {
                        holder.card.setCardBackgroundColor(Color.parseColor("#FFECB3")) // light orange for the next ride
                    }
                    else -> {
                        holder.card.setCardBackgroundColor(Color.WHITE)
                    }
                }
            }
        } else if (holder is FooterViewHolder) {
            holder.footerText.text = "+$hiddenCount more rides..."
        }
    }

    override fun getItemCount(): Int {
        return visibleRides.size + if (hasFooter) 1 else 0
    }
}

