package com.example.simple_calendar.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.simple_calendar.extensions.config
import com.example.simple_calendar.extensions.recheckCalDAVCalendars
import com.example.simple_calendar.extensions.refreshCalDAVCalendars
import com.example.simple_calendar.extensions.updateWidgets

class CalDAVSyncReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (context.config.caldavSync) {
            context.refreshCalDAVCalendars(context.config.caldavSyncedCalendarIds, false)
        }

        context.recheckCalDAVCalendars(true) {
            context.updateWidgets()
        }
    }
}
