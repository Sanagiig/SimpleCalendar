package com.example.simple_calendar.extensions

import com.example.simple_calendar.helpers.MONTH
import com.example.simple_calendar.helpers.WEEK
import com.example.simple_calendar.helpers.YEAR

fun Int.isXWeeklyRepetition() = this != 0 && this % WEEK == 0

fun Int.isXMonthlyRepetition() = this != 0 && this % MONTH == 0

fun Int.isXYearlyRepetition() = this != 0 && this % YEAR == 0
