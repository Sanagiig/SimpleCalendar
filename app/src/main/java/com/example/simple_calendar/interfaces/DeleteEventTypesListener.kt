package com.example.simple_calendar.interfaces

import com.example.simple_calendar.models.EventType
import java.util.*

interface DeleteEventTypesListener {
    fun deleteEventTypes(eventTypes: ArrayList<EventType>, deleteEvents: Boolean): Boolean
}
