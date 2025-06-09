package com.example.simple_calendar.interfaces

import android.util.SparseArray
import com.example.simple_calendar.models.DayYearly
import java.util.*

interface YearlyCalendar {
    fun updateYearlyCalendar(events: SparseArray<ArrayList<DayYearly>>, hashCode: Int)
}
