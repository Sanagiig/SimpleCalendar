package com.example.simple_calendar.fragments

import androidx.fragment.app.Fragment
import com.example.simple_calendar.interfaces.NavigationListener
import com.simplemobiletools.commons.interfaces.RefreshRecyclerViewListener
import org.joda.time.DateTime

class EventListFragment :  MyFragmentHolder(), RefreshRecyclerViewListener {
    override val viewType: Int
        get() = TODO("Not yet implemented")

    override fun goToToday() {
        TODO("Not yet implemented")
    }

    override fun showGoToDateDialog() {
        TODO("Not yet implemented")
    }

    override fun refreshEvents() {
        TODO("Not yet implemented")
    }

    override fun shouldGoToTodayBeVisible(): Boolean {
        TODO("Not yet implemented")
    }

    override fun getNewEventDayCode(): String {
        TODO("Not yet implemented")
    }

    override fun printView() {
        TODO("Not yet implemented")
    }

    override fun getCurrentDate(): DateTime? {
        TODO("Not yet implemented")
    }

    override fun refreshItems() {
        TODO("Not yet implemented")
    }
}