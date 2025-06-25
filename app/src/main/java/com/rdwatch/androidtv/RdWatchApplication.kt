package com.rdwatch.androidtv

import android.app.Application
// import com.rdwatch.androidtv.util.RefreshManager
import dagger.hilt.android.HiltAndroidApp
// import javax.inject.Inject

@HiltAndroidApp
class RdWatchApplication : Application() {
    
    // @Inject
    // lateinit var refreshManager: RefreshManager
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize periodic sync for Real-Debrid content
        // refreshManager.schedulePeriodicSync()
    }
}