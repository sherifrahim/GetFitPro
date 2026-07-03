package com.getfit

import android.app.Application

class GetFitApp : Application() {
    // AppContainer (manual DI) is wired here in Phase 4.
    override fun onCreate() {
        super.onCreate()
    }
}
