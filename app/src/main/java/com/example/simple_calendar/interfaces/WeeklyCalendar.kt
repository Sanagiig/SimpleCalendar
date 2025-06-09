package com.example.simple_calendar.interfaces

import com.example.simple_calendar.models.Event

interface WeeklyCalendar {
    fun updateWeeklyCalendar(events: ArrayList<Event>)
}
