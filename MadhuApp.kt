package com.foss.madhu

import android.app.Application

/**
 * Application class — instantiates the [AppComponent] (manual DI root)
 * exactly once for the lifetime of the process.
 */
class MadhuApp : Application() {

    lateinit var appComponent: AppComponent
        private set

    override fun onCreate() {
        super.onCreate()
        appComponent = AppComponent(applicationContext)
    }
}
