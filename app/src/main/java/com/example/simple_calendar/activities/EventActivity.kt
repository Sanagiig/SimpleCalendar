package com.example.simple_calendar.activities

import android.app.Activity
import android.app.DatePickerDialog
import android.app.TimePickerDialog
import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract.Attendees
import android.provider.CalendarContract.Colors
import android.provider.ContactsContract.CommonDataKinds
import android.provider.ContactsContract.CommonDataKinds.StructuredName
import android.provider.ContactsContract.Data
import android.text.TextUtils
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.WindowManager
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import android.widget.RelativeLayout
import com.google.android.material.timepicker.MaterialTimePicker
import com.google.android.material.timepicker.MaterialTimePicker.INPUT_MODE_CLOCK
import com.google.android.material.timepicker.TimeFormat
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import com.simplemobiletools.commons.dialogs.ColorPickerDialog
import com.simplemobiletools.commons.dialogs.ConfirmationAdvancedDialog
import com.simplemobiletools.commons.dialogs.PermissionRequiredDialog
import com.simplemobiletools.commons.dialogs.RadioGroupDialog
import com.simplemobiletools.commons.extensions.*
import com.simplemobiletools.commons.helpers.*
import com.simplemobiletools.commons.models.RadioItem
import com.simplemobiletools.commons.views.MyAutoCompleteTextView
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import java.util.TimeZone
import java.util.regex.Pattern

import com.example.simple_calendar.helpers.*
import com.example.simple_calendar.models.Attendee
import com.example.simple_calendar.models.Event
import com.example.simple_calendar.models.EventType
import com.example.simple_calendar.databinding.ActivityEventBinding

class EventActivity : SimpleActivity() {
    private val LAT_LON_PATTERN =
        "^[-+]?([1-8]?\\d(\\.\\d+)?|90(\\.0+)?)([,;])\\s*[-+]?(180(\\.0+)?|((1[0-7]\\d)|([1-9]?\\d))(\\.\\d+)?)\$"
    private val SELECT_TIME_ZONE_INTENT = 1

    private var mIsAllDayEvent = false
    private var mReminder1Minutes = REMINDER_OFF
    private var mReminder2Minutes = REMINDER_OFF
    private var mReminder3Minutes = REMINDER_OFF
    private var mReminder1Type = REMINDER_NOTIFICATION
    private var mReminder2Type = REMINDER_NOTIFICATION
    private var mReminder3Type = REMINDER_NOTIFICATION
    private var mRepeatInterval = 0
    private var mRepeatLimit = 0L
    private var mRepeatRule = 0
    private var mEventTypeId = REGULAR_EVENT_TYPE_ID
    private var mEventOccurrenceTS = 0L
    private var mLastSavePromptTS = 0L
    private var mEventCalendarId = STORED_LOCALLY_ONLY
    private var mWasContactsPermissionChecked = false
    private var mWasCalendarChanged = false
    private var mAttendees = ArrayList<Attendee>()
    private var mAttendeeAutoCompleteViews = ArrayList<MyAutoCompleteTextView>()
    private var mAvailableContacts = ArrayList<Attendee>()
    private var mSelectedContacts = ArrayList<Attendee>()
    private var mAvailability = Attendees.AVAILABILITY_BUSY
    private var mStoredEventTypes = ArrayList<EventType>()
    private var mOriginalTimeZone = DateTimeZone.getDefault().id
    private var mOriginalStartTS = 0L
    private var mOriginalEndTS = 0L
    private var mIsNewEvent = true
    private var mEventColor = 0

    private lateinit var mEventStartDateTime: DateTime
    private lateinit var mEventEndDateTime: DateTime
    private lateinit var mEvent: Event

    private val binding by viewBinding(ActivityEventBinding::inflate)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }
}
