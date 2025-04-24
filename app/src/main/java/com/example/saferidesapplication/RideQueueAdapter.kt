package com.example.saferidesapplication

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.example.saferidesapplication.network.dto.RideResponse
import androidx.core.graphics.toColorInt

class RideQueueAdapter(
    private val currentUserId: String,
    private val isDriverView: Boolean
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_RIDE = 0
        private const val TYPE_FOOTER = 1
    }

    private val visibleRides: MutableList<RideResponse> = mutableListOf()
    private var hasFooter = false
    private var hiddenCount = 0

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

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is RideViewHolder) {
            val ride = visibleRides[position]
            holder.pickupText.text = "Pickup: ${ride.pickupLocation}"
            holder.dropoffText.text = "Dropoff: ${ride.dropoffLocation}"
            holder.passengerCountText.text = "Passengers: ${ride.passengerCount}"
            if (!isDriverView) {
                if (ride.passengerId == currentUserId) {
                    holder.card.setCardBackgroundColor("#C8E6C9".toColorInt()) // green
                } else {
                    holder.card.setCardBackgroundColor(Color.WHITE)
                }
            } else {
                when {
                    ride.driverId == currentUserId && ride.status in listOf("assigned", "arrived", "in_progress") -> {
                        holder.card.setCardBackgroundColor("#C8E6C9".toColorInt()) // green
                    }
                    else -> {
                        val isFirstQueued = ride.status == "queued" &&
                                visibleRides.indexOfFirst { it.status == "queued" } == position

                        if (isFirstQueued) {
                            holder.card.setCardBackgroundColor("#FFECB3".toColorInt()) // yellow
                        } else {
                            holder.card.setCardBackgroundColor(Color.WHITE)
                        }
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

    fun updateData(newRides: List<RideResponse>) {
        visibleRides.clear()

        if (isDriverView) {
            val assignedToDriver = newRides.filter {
                it.driverId == currentUserId && it.status in listOf("assigned", "arrived", "in_progress")
            }
            val queued = newRides.filter { it.status == "queued" }
            val shownQueued = if (queued.size > 8) queued.take(8) else queued

            hasFooter = queued.size > 8
            hiddenCount = queued.size - shownQueued.size

            visibleRides.addAll(assignedToDriver + shownQueued)
        } else {
            val passengerRides = newRides.filter {
                it.passengerId == currentUserId && it.status !in listOf("completed", "cancelled")
            }.sortedBy { it.timestamp }

            visibleRides.addAll(passengerRides)
            hasFooter = false
            hiddenCount = 0
        }
    }

}
