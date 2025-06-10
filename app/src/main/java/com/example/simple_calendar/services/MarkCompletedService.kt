package com.example.simple_calendar.services

import android.app.IntentService
import android.content.Intent
import com.example.simple_calendar.extensions.eventsDB
import com.example.simple_calendar.extensions.updateTaskCompletion
import com.example.simple_calendar.helpers.ACTION_MARK_COMPLETED
import com.example.simple_calendar.helpers.EVENT_ID

class MarkCompletedService : IntentService("MarkCompleted") {

    @Deprecated("Deprecated in Java")
    override fun onHandleIntent(intent: Intent?) {
        if (intent != null && intent.action == ACTION_MARK_COMPLETED) {
            val taskId = intent.getLongExtra(EVENT_ID, 0L)
            val task = eventsDB.getTaskWithId(taskId)
            if (task != null) {
                updateTaskCompletion(task, completed = true)
            }
        }
    }
}
