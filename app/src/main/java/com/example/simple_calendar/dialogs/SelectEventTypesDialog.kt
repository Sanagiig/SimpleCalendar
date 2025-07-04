package com.example.simple_calendar.dialogs

import androidx.appcompat.app.AlertDialog
import com.example.simple_calendar.activities.SimpleActivity
import com.example.simple_calendar.databinding.DialogFilterEventTypesBinding
import com.example.simple_calendar.extensions.eventsHelper
import com.simplemobiletools.commons.extensions.getAlertDialogBuilder
import com.simplemobiletools.commons.extensions.setupDialogStuff
import com.simplemobiletools.commons.extensions.viewBinding

class SelectEventTypesDialog(val activity: SimpleActivity, selectedEventTypes: Set<String>, val callback: (HashSet<String>) -> Unit) {
    private var dialog: AlertDialog? = null
    private val binding by activity.viewBinding(DialogFilterEventTypesBinding::inflate)

    init {
        activity.eventsHelper.getEventTypes(activity, false) {
//            binding.filterEventTypesList.adapter = FilterEventTypeAdapter(activity, it, selectedEventTypes)
//
//            activity.getAlertDialogBuilder()
//                .setPositiveButton(com.simplemobiletools.commons.R.string.ok) { _, _ -> confirmEventTypes() }
//                .setNegativeButton(com.simplemobiletools.commons.R.string.cancel, null)
//                .apply {
//                    activity.setupDialogStuff(binding.root, this) { alertDialog ->
//                        dialog = alertDialog
//                    }
//                }
        }
    }

    private fun confirmEventTypes() {
//        val adapter = binding.filterEventTypesList.adapter as FilterEventTypeAdapter
//        val selectedItems = adapter.getSelectedItemsList()
//            .map { it.toString() }
//            .toHashSet()
//        callback(selectedItems)
//        dialog?.dismiss()
    }
}
