package com.example.simple_calendar.helpers

import android.content.Context
import com.example.simple_calendar.models.Event
import com.simplemobiletools.commons.helpers.ExportResult
import java.io.OutputStream

class IcsExporter(private val context: Context) {
    enum class ExportResult {
        EXPORT_FAIL, EXPORT_OK, EXPORT_PARTIAL
    }

    fun exportEvents(
        outputStream: OutputStream?,
        events: List<Event>,
        showExportingToast: Boolean,
        callback: (result: ExportResult) -> Unit
    ) {}
}