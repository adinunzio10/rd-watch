package com.rdwatch.androidtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.rdwatch.androidtv.navigation.PlaybackNavigationHelper
import com.rdwatch.androidtv.presentation.navigation.AppNavigation
import com.rdwatch.androidtv.presentation.navigation.Screen
import com.rdwatch.androidtv.ui.MainViewModel
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
    
    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Schedule periodic cleanup of old playback data
        playbackCleanupManager.scheduleCleanup()
        
        setContent {
            RdwatchTheme {
                val isReady by mainViewModel.isReady.collectAsStateWithLifecycle(initialValue = false)
                val startDestination by mainViewModel.startDestination.collectAsStateWithLifecycle(initialValue = Screen.Home)
                
                if (isReady) {
                    val navController = rememberNavController()
                    AppNavigation(
                        navController = navController,
                        startDestination = startDestination,
                        onAuthenticationSuccess = {
                            mainViewModel.onAuthenticationSuccess()
                            // Navigate to home and clear back stack
                            navController.navigate(Screen.Home) {
                                popUpTo(Screen.Authentication) { inclusive = true }
                            }
                        }
                    )
                } else {
                    // Show loading screen while checking authentication
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
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
        
        // Check authentication state in case tokens expired while app was backgrounded
        mainViewModel.onAppResume()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Handle app termination cleanup
        if (isFinishing) {
            playbackNavigationHelper.handleAppTermination()
        }
    }
}