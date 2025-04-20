package com.example.saferidesapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saferidesapplication.network.ApiClient
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class RideQueueFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private var pollingJob: Job? = null
    private lateinit var passengerId: String

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_ride_queue, container, false)
        recyclerView = view.findViewById(R.id.rideQueueRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        val sharedPreferences = requireContext().getSharedPreferences("SafeRidesPrefs", 0)
        passengerId = sharedPreferences.getString("userId", null) ?: "unknown"

        startPollingQueue()

        return view
    }

    private fun startPollingQueue() {
        pollingJob?.cancel()
        pollingJob = viewLifecycleOwner.lifecycleScope.launch {
            while (isActive) {
                loadRideQueue()
                delay(5000)
            }
        }
    }

    private fun loadRideQueue() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val ridesResponse = ApiClient.apiService.getAllRides()
                if (ridesResponse.isSuccessful) {
                    val rideList = ridesResponse.body() ?: return@launch
                    val queuedRides = rideList.filter { it.status == "queued" }
                        .sortedBy { it.timestamp }

                    recyclerView.adapter =
                        RideQueueAdapter(queuedRides, passengerId, isDriverView = false)
                }
            } catch (_: Exception) {
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        pollingJob?.cancel()
    }
}
