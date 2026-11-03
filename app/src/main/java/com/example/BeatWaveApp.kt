package com.example

import android.app.Application
import com.example.util.CrashReportManager

class BeatWaveApp : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReportManager.install(this)
    }
}
