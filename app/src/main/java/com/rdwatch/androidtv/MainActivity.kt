package com.rdwatch.androidtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.work.WorkManager
import com.rdwatch.androidtv.navigation.PlaybackNavigationHelper
import com.rdwatch.androidtv.ui.home.TVHomeScreen
import com.rdwatch.androidtv.ui.theme.RdwatchTheme
import com.rdwatch.androidtv.util.PlaybackCleanupManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main Activity using Jetpack Compose for TV interface
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject
    lateinit var playbackNavigationHelper: PlaybackNavigationHelper
    
    @Inject
    lateinit var playbackCleanupManager: PlaybackCleanupManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Schedule periodic cleanup of old playback data
        playbackCleanupManager.scheduleCleanup(WorkManager.getInstance(this))
        
        setContent {
            RdwatchTheme {
                TVHomeScreen()
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Handle app going to background
        playbackNavigationHelper.handleAppBackground()
    }
    
    override fun onResume() {
        super.onResume()
        // Handle app returning to foreground
        playbackNavigationHelper.handleAppForeground()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Handle app termination cleanup
        if (isFinishing) {
            playbackNavigationHelper.handleAppTermination()
        }
    }
}