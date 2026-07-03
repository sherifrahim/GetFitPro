package com.getfit

import android.app.Application
import com.getfit.di.AppContainer

class GetFitApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.seedOnFirstLaunch()
    }
}
