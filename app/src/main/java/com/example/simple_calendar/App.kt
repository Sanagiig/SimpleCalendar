package com.example.simple_calendar

import com.simplemobiletools.commons.extensions.checkUseEnglish
import androidx.multidex.MultiDexApplication

class App : MultiDexApplication() {
    override fun onCreate() {
        super.onCreate()
        checkUseEnglish()
    }
}