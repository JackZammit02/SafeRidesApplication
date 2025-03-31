package com.example.saferidesapplication

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.saferidesapplication.network.ApiClient
import kotlinx.coroutines.*
import retrofit2.HttpException


class QueuePreviewActivity : ComponentActivity() {
    private lateinit var recyclerView: RecyclerView
    private var pollingJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_queue_preview)

        title = "Queue Preview"

        recyclerView = findViewById(R.id.queueRecyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        val backButton: Button = findViewById(R.id.backButton)
        backButton.setOnClickListener { finish() }

        startPollingQueue()

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
    }

    private fun startPollingQueue() {
        pollingJob = lifecycleScope.launch {
            while (isActive) {
                loadRideQueue()
                delay(5000) // Poll every 5 seconds
            }
        }
    }

    private fun loadRideQueue() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.apiService.getAllRides()
                if (response.isSuccessful && response.body() != null) {
                    val queuedRides = response.body()!!.filter { it.status == "queued" }
                        .sortedByDescending { it.timestamp }
                    recyclerView.adapter = RideQueueAdapter(queuedRides, "", isDriverView = false)
                } else {
                    Toast.makeText(this@QueuePreviewActivity, "Could not load queue", Toast.LENGTH_SHORT).show()
                }
            } catch (e: HttpException) {
                Toast.makeText(this@QueuePreviewActivity, "HTTP error: ${e.message()}", Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                Toast.makeText(this@QueuePreviewActivity, "Error: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        pollingJob?.cancel()
    }
}
