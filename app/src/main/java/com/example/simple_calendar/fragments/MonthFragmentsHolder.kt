package com.example.simple_calendar.fragments

import androidx.fragment.app.Fragment
import com.example.simple_calendar.interfaces.NavigationListener
import org.joda.time.DateTime

class MonthFragmentsHolder:  MyFragmentHolder(), NavigationListener{

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

    override fun goLeft() {
        TODO("Not yet implemented")
    }

    override fun goRight() {
        TODO("Not yet implemented")
    }

    override fun goToDateTime(dateTime: DateTime) {
        TODO("Not yet implemented")
    }
}