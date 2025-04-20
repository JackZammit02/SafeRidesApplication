package com.example.saferidesapplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saferidesapplication.network.dto.RideResponse
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class RideQueueFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: RideQueueAdapter
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

        adapter = RideQueueAdapter(passengerId, isDriverView = false)

        recyclerView.adapter = adapter

        listenToRideQueueUpdates()

        return view
    }

    private fun listenToRideQueueUpdates() {
        val db = Firebase.firestore

        db.collection("rides")
            .whereEqualTo("status", "queued")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener

                val queuedRides = snapshot.toObjects(RideResponse::class.java)
                    .sortedBy { it.timestamp }

                adapter.updateData(queuedRides)
                adapter.notifyDataSetChanged()
            }
    }
}
