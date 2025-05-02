// PassengerPagerAdapter.kt
package com.example.saferidesapplication

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter

class PassengerPagerAdapter(activity: FragmentActivity) : FragmentStateAdapter(activity) {
    override fun getItemCount(): Int = 2

    override fun createFragment(position: Int): Fragment {
        return when (position) {
            0 -> RideQueueFragment()       // Queue page
            1 -> RideTimelineFragment()    // Timeline page
            else -> throw IllegalStateException("Invalid position $position")
        }
    }
}



