package com.example.simple_calendar.services

import android.app.IntentService
import android.content.Intent
import com.example.simple_calendar.extensions.config
import com.example.simple_calendar.extensions.eventsDB
import com.example.simple_calendar.extensions.rescheduleReminder
import com.example.simple_calendar.helpers.EVENT_ID

class SnoozeService : IntentService("Snooze") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null) {
            val eventId = intent.getLongExtra(EVENT_ID, 0L)
            val event = eventsDB.getEventOrTaskWithId(eventId)
            rescheduleReminder(event, config.snoozeTime)
        }
    }
}
