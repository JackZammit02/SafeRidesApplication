package com.example.saferidesapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.saferidesapplication.network.ApiClient
import kotlinx.coroutines.launch

class RideTimelineFragment : Fragment() {

    private lateinit var steps: Map<String, TextView>
    private lateinit var passengerId: String

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
            "picked_up" to view.findViewById(R.id.timelineStepPickedUp),
            "completed" to view.findViewById(R.id.timelineStepCompleted)
        )

        passengerId = requireActivity()
            .getSharedPreferences("SafeRidesPrefs", AppCompatActivity.MODE_PRIVATE)
            .getString("userId", null) ?: "unknown"

        return view
    }

    override fun onResume() {
        super.onResume()
        fetchAndDisplayRideStatus()
    }

    private fun fetchAndDisplayRideStatus() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getAllRides()
                if (response.isSuccessful) {
                    val rides = response.body() ?: emptyList()
                    val myRide = rides.find {
                        it.passengerId == passengerId &&
                                it.status in listOf("queued", "assigned", "arrived", "picked_up", "completed")
                    }

                    highlightStep(myRide?.status ?: "none")
                }
            } catch (e: Exception) {
                highlightStep("none")
            }
        }
    }

    private fun highlightStep(currentStatus: String) {
        val order = listOf("queued", "assigned", "arrived", "picked_up", "completed")
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
