package com.example.saferidesapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.edit
import androidx.fragment.app.Fragment
import com.example.saferidesapplication.network.dto.RideResponse
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RideTimelineFragment : Fragment() {

    private lateinit var steps: Map<String, TextView>
    private lateinit var passengerId: String
    private var rideListener: ListenerRegistration? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ride_timeline, container, false)

        // Map step IDs to views
        steps = mapOf(
            "queued" to view.findViewById(R.id.timelineStepQueued),
            "assigned" to view.findViewById(R.id.timelineStepAssigned),
            "arrived" to view.findViewById(R.id.timelineStepArrived),
            "in_progress" to view.findViewById(R.id.timelineStepPickedUp),
            "completed" to view.findViewById(R.id.timelineStepCompleted)
        )

        passengerId = requireActivity()
            .getSharedPreferences("SafeRidesPrefs", AppCompatActivity.MODE_PRIVATE)
            .getString("passengerId", null) ?: "unknown"

        return view
    }

    override fun onResume() {
        super.onResume()
        listenToRideStatus()
    }

    override fun onPause() {
        super.onPause()
        rideListener?.remove()
    }

    private fun listenToRideStatus() {
        val db = Firebase.firestore
        val ridesRef = db.collection("rides")

        rideListener?.remove()

        rideListener = ridesRef
            .whereEqualTo("passengerId", passengerId)
            .orderBy("timestamp")
            .addSnapshotListener { snapshots, error ->
                if (error != null || snapshots == null) {
                    setRideActiveState(false)
                    showNoRideMessage()
                    return@addSnapshotListener
                }

                val myRide = snapshots.documents.lastOrNull()?.toObject(RideResponse::class.java)

                if (myRide != null && myRide.status in listOf("queued", "assigned", "arrived", "in_progress")) {
                    setRideActiveState(true)
                    showTimelineAndHighlight(myRide.status)
                } else if (myRide != null && myRide.status == "completed") {
                    setRideActiveState(false)
                    showTimelineAndHighlight("completed")

                    // Delay hiding the timeline after 2.5 seconds
                    view?.postDelayed({
                        showNoRideMessage()
                    }, 2500)
                } else {
                    setRideActiveState(false)
                    showNoRideMessage()
                }
            }
    }

    private fun setRideActiveState(isActive: Boolean) {
        requireActivity().getSharedPreferences("SafeRidesPrefs", AppCompatActivity.MODE_PRIVATE).edit {
            putBoolean("hasActiveRide", isActive)
        }
    }


    private fun showTimelineAndHighlight(status: String) {
        view?.findViewById<View>(R.id.timelineLayout)?.visibility = View.VISIBLE
        view?.findViewById<View>(R.id.noRideMessage)?.visibility = View.GONE
        highlightStep(status)
    }

    private fun showNoRideMessage() {
        view?.findViewById<View>(R.id.timelineLayout)?.visibility = View.GONE
        view?.findViewById<View>(R.id.noRideMessage)?.visibility = View.VISIBLE
    }

    private fun highlightStep(currentStatus: String) {
        val order = listOf("queued", "assigned", "arrived", "in_progress", "completed")
        val activeIndex = order.indexOf(currentStatus)

        steps.forEach { (status, textView) ->
            val index = order.indexOf(status)
            when {
                index < activeIndex -> textView.setTextColor(0xFF4CAF50.toInt()) // green
                index == activeIndex -> textView.setTextColor(0xFFFFC107.toInt()) // amber
                else -> textView.setTextColor(0xFFAAAAAA.toInt()) // gray
            }
        }
    }
}
