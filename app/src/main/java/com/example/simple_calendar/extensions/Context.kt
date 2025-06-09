package com.example.simple_calendar.extensions

import android.app.*
import android.accounts.Account
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import androidx.room.util.copy
import com.example.simple_calendar.R
import com.example.simple_calendar.databases.EventsDatabase
import com.example.simple_calendar.helpers.CalDAVHelper
import com.simplemobiletools.commons.helpers.ensureBackgroundThread
import java.util.HashSet

import com.example.simple_calendar.helpers.Config
import com.example.simple_calendar.helpers.EventsHelper
import com.example.simple_calendar.helpers.MyWidgetDateProvider
import com.example.simple_calendar.helpers.MyWidgetListProvider
import com.example.simple_calendar.helpers.MyWidgetMonthlyProvider
import com.example.simple_calendar.helpers.SCHEDULE_CALDAV_REQUEST_CODE
import com.example.simple_calendar.interfaces.EventTypesDao
import com.example.simple_calendar.interfaces.EventsDao
import com.example.simple_calendar.receivers.CalDAVSyncReceiver
import com.simplemobiletools.commons.extensions.showErrorToast
import com.simplemobiletools.commons.extensions.toast
import com.simplemobiletools.commons.helpers.isSPlus
import com.example.simple_calendar.helpers.*
import com.example.simple_calendar.models.Event
import com.example.simple_calendar.receivers.NotificationReceiver
import com.simplemobiletools.commons.extensions.formatSecondsToTimeString
import com.simplemobiletools.commons.helpers.WEEK_SECONDS
import com.simplemobiletools.commons.helpers.YEAR_SECONDS
import org.joda.time.LocalDate

val Context.config: Config get() = Config.newInstance(applicationContext)
val Context.calDAVHelper: CalDAVHelper get() = CalDAVHelper(this)
val Context.eventsHelper: EventsHelper get() = EventsHelper(this)
val Context.eventsDB: EventsDao get() = EventsDatabase.getInstance(applicationContext).EventsDao()
val Context.eventTypesDB: EventTypesDao
    get() = EventsDatabase.getInstance(applicationContext).EventTypesDao()


private fun getPendingIntent(context: Context, event: Event): PendingIntent {
    val activityClass = getActivityToOpen(event.isTask())
    val intent = Intent(context, activityClass)
    intent.putExtra(EVENT_ID, event.id)
    intent.putExtra(EVENT_OCCURRENCE_TS, event.startTS)
    return PendingIntent.getActivity(context, event.id!!.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}


fun Context.updateWidgets() {
    val widgetIDs = AppWidgetManager.getInstance(applicationContext)
        ?.getAppWidgetIds(ComponentName(applicationContext, MyWidgetMonthlyProvider::class.java))
        ?: return
    if (widgetIDs.isNotEmpty()) {
        Intent(applicationContext, MyWidgetMonthlyProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
            sendBroadcast(this)
        }
    }

    updateListWidget()
    updateDateWidget()
}

fun Context.updateListWidget() {
    val widgetIDs = AppWidgetManager.getInstance(applicationContext)
        ?.getAppWidgetIds(ComponentName(applicationContext, MyWidgetListProvider::class.java))
        ?: return

    if (widgetIDs.isNotEmpty()) {
        Intent(applicationContext, MyWidgetListProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
            sendBroadcast(this)
        }
    }
    AppWidgetManager.getInstance(applicationContext)
        ?.notifyAppWidgetViewDataChanged(widgetIDs, R.id.widget_event_list)
}

fun Context.updateDateWidget() {
    val widgetIDs = AppWidgetManager.getInstance(applicationContext)
        ?.getAppWidgetIds(ComponentName(applicationContext, MyWidgetDateProvider::class.java))
        ?: return
    if (widgetIDs.isNotEmpty()) {
        Intent(applicationContext, MyWidgetDateProvider::class.java).apply {
            action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, widgetIDs)
            sendBroadcast(this)
        }
    }
}

fun Context.refreshCalDAVCalendars(ids: String, showToasts: Boolean) {
    val uri = CalendarContract.Calendars.CONTENT_URI
    val accounts = HashSet<Account>()
    val calendars = calDAVHelper.getCalDAVCalendars(ids, showToasts)
    calendars.forEach {
        accounts.add(Account(it.accountName, it.accountType))
    }

    Bundle().apply {
        putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true)
        if (showToasts) {
            // Assume this is a manual synchronisation when we showToasts to the user (swipe refresh, MainMenu-> refresh caldav calendars, ...)
            putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true)
        }
        accounts.forEach {
            ContentResolver.requestSync(it, uri.authority, this)
        }
    }
}

// same as Context.queryCursor but inlined to allow non-local returns
inline fun Context.queryCursorInlined(
    uri: Uri,
    projection: Array<String>,
    selection: String? = null,
    selectionArgs: Array<String>? = null,
    sortOrder: String? = null,
    showErrors: Boolean = false,
    callback: (cursor: Cursor) -> Unit
) {
    try {
        val cursor = contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
        cursor?.use {
            if (cursor.moveToFirst()) {
                do {
                    callback(cursor)
                } while (cursor.moveToNext())
            }
        }
    } catch (e: Exception) {
        if (showErrors) {
            showErrorToast(e)
        }
    }
}

fun Context.scheduleCalDAVSync(activate: Boolean) {
    val syncIntent = Intent(applicationContext, CalDAVSyncReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        applicationContext,
        SCHEDULE_CALDAV_REQUEST_CODE,
        syncIntent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val alarmManager = getAlarmManager()
    alarmManager.cancel(pendingIntent)

    if (activate) {
        val syncCheckInterval = 2 * AlarmManager.INTERVAL_HOUR
        try {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + syncCheckInterval,
                syncCheckInterval,
                pendingIntent
            )
        } catch (ignored: Exception) {
        }
    }
}

fun Context.recheckCalDAVCalendars(scheduleNextCalDAVSync: Boolean, callback: () -> Unit) {
    if (config.caldavSync) {
        ensureBackgroundThread {
            calDAVHelper.refreshCalendars(false, scheduleNextCalDAVSync, callback)
            updateWidgets()
        }
    }
}

fun Context.getAlarmManager() = getSystemService(Context.ALARM_SERVICE) as AlarmManager


fun Context.scheduleNextEventReminder(event: Event, showToasts: Boolean) {
    val validReminders = event.getReminders().filter { it.type == REMINDER_NOTIFICATION }
    if (validReminders.isEmpty()) {
        if (showToasts) {
            toast(com.simplemobiletools.commons.R.string.saving)
        }
        return
    }

    val now = getNowSeconds()
    val reminderSeconds = validReminders.reversed().map { it.minutes * 60 }
    val isTask = event.isTask()
    eventsHelper.getEvents(now, now + YEAR, event.id!!, false) { events ->
        if (events.isNotEmpty()) {
            for (curEvent in events) {
                if (isTask && curEvent.isTaskCompleted()) {
                    // skip scheduling reminders for completed tasks
                    continue
                }

                for (curReminder in reminderSeconds) {
                    if (curEvent.getEventStartTS() - curReminder > now) {
                        scheduleEventIn(
                            (curEvent.getEventStartTS() - curReminder) * 1000L,
                            curEvent,
                            showToasts
                        )
                        return@getEvents
                    }
                }
            }
        }

        if (showToasts) {
            toast(com.simplemobiletools.commons.R.string.saving)
        }
    }
}


fun Context.scheduleEventIn(notifyAtMillis: Long, event: Event, showToasts: Boolean) {
    val now = System.currentTimeMillis()
    if (notifyAtMillis < now) {
        if (showToasts) {
            toast(com.simplemobiletools.commons.R.string.saving)
        }
        return
    }

    val newNotifyAtMillis = notifyAtMillis + 1000
    if (showToasts) {
        val secondsTillNotification = (newNotifyAtMillis - now) / 1000
        val msg = String.format(
            getString(com.simplemobiletools.commons.R.string.time_remaining),
            formatSecondsToTimeString(secondsTillNotification.toInt())
        )
        toast(msg)
    }

    val pendingIntent = getNotificationIntent(event)
    setExactAlarm(newNotifyAtMillis, pendingIntent)
}

fun Context.getNotificationIntent(event: Event): PendingIntent {
    val intent = Intent(this, NotificationReceiver::class.java)
    intent.putExtra(EVENT_ID, event.id)
    intent.putExtra(EVENT_OCCURRENCE_TS, event.startTS)
    return PendingIntent.getBroadcast(this, event.id!!.toInt(), intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
}


fun Context.notifyEvent(originalEvent: Event) {
    var event = originalEvent.copy()
    val currentSeconds = getNowSeconds()

    var eventStartTS = if (event.getIsAllDay()) Formatter.getDayStartTS(Formatter.getDayCodeFromTS(event.startTS)) else event.startTS
    // make sure refer to the proper repeatable event instance with "Tomorrow", or the specific date
    if (event.repeatInterval != 0 && eventStartTS - event.reminder1Minutes * 60 < currentSeconds) {
        val events = eventsHelper.getRepeatableEventsFor(currentSeconds - WEEK_SECONDS, currentSeconds + YEAR_SECONDS, event.id!!)
        for (currEvent in events) {
            eventStartTS = if (currEvent.getIsAllDay()) Formatter.getDayStartTS(Formatter.getDayCodeFromTS(currEvent.startTS)) else currEvent.startTS
            val firstReminderMinutes =
                arrayOf(currEvent.reminder3Minutes, currEvent.reminder2Minutes, currEvent.reminder1Minutes).filter { it != REMINDER_OFF }.max()
            if (eventStartTS - firstReminderMinutes * 60 > currentSeconds) {
                break
            }

            event = currEvent
        }
    }

    val pendingIntent = getPendingIntent(applicationContext, event)
    val startTime = Formatter.getTimeFromTS(applicationContext, event.startTS)
    val endTime = Formatter.getTimeFromTS(applicationContext, event.endTS)
    val startDate = Formatter.getDateFromTS(event.startTS)

    val displayedStartDate = when (startDate) {
        LocalDate.now() -> ""
        LocalDate.now().plusDays(1) -> getString(com.simplemobiletools.commons.R.string.tomorrow)
        else -> "${Formatter.getDateFromCode(this, Formatter.getDayCodeFromTS(event.startTS))},"
    }

    val timeRange = if (event.getIsAllDay()) getString(R.string.all_day) else getFormattedEventTime(startTime, endTime)
    val descriptionOrLocation = if (config.replaceDescription) event.location else event.description
    val content = "$displayedStartDate $timeRange $descriptionOrLocation".trim()
    ensureBackgroundThread {
        if (event.isTask()) eventsHelper.updateIsTaskCompleted(event)
        val notification = getNotification(pendingIntent, event, content)
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        try {
            if (notification != null) {
                notificationManager.notify(event.id!!.toInt(), notification)
            }
        } catch (e: Exception) {
            showErrorToast(e)
        }
    }
}



fun Context.setExactAlarm(
    triggerAtMillis: Long,
    operation: PendingIntent,
    type: Int = AlarmManager.RTC_WAKEUP
) {
    val alarmManager = getAlarmManager()
    try {
        if (isSPlus() && alarmManager.canScheduleExactAlarms() || !isSPlus()) {
            alarmManager.setExactAndAllowWhileIdle(type, triggerAtMillis, operation)
        } else {
            alarmManager.setAndAllowWhileIdle(type, triggerAtMillis, operation)
        }
    } catch (e: Exception) {
        showErrorToast(e)
    }
}
