package com.presagetech.internal_demo

import android.app.Application
import timber.log.Timber

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}
