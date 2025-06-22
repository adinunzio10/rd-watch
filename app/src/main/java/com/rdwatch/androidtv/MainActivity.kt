package com.rdwatch.androidtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.rdwatch.androidtv.ui.home.TVHomeScreen
import com.rdwatch.androidtv.ui.theme.RdwatchTheme

/**
 * Main Activity using Jetpack Compose for TV interface
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            RdwatchTheme {
                TVHomeScreen()
            }
        }
    }
}