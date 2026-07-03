package com.wilddeck.app

import android.app.Application
import com.wilddeck.app.data.CrashReporter

class WildDeckApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashReporter.install(this)
    }
}
