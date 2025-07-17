package com.rdwatch.androidtv.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import com.rdwatch.androidtv.data.preferences.models.ThemeMode
import com.rdwatch.androidtv.data.repository.SettingsRepository

private val DarkColorScheme =
    darkColorScheme(
        primary = Color(0xFF2196F3),
        secondary = Color(0xFF03DAC5),
        tertiary = Color(0xFF3700B3),
        background = Color(0xFF121212),
        surface = Color(0xFF1E1E1E),
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onTertiary = Color.White,
        onBackground = Color.White,
        onSurface = Color.White,
    )

private val LightColorScheme =
    lightColorScheme(
        primary = Color(0xFF2196F3),
        secondary = Color(0xFF03DAC5),
        tertiary = Color(0xFF3700B3),
        background = Color(0xFFFFFBFE),
        surface = Color(0xFFFFFBFE),
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onBackground = Color(0xFF1C1B1F),
        onSurface = Color(0xFF1C1B1F),
    )

/**
 * Main theme composable with dynamic theme switching support
 * @param themeMode Optional theme mode override. If not provided, will use system preference
 * @param dynamicTheme If true, theme will automatically update based on user preferences
 * @param content The content to display with this theme
 */
@Composable
fun RdwatchTheme(
    themeMode: ThemeMode? = null,
    dynamicTheme: Boolean = true,
    content: @Composable () -> Unit,
) {
    val isSystemDarkTheme = isSystemInDarkTheme()

    // Determine if dark theme should be used
    val darkTheme =
        when {
            themeMode != null -> {
                // Use provided theme mode
                when (themeMode) {
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                    ThemeMode.SYSTEM -> isSystemDarkTheme
                }
            }
            else -> {
                // Default to system theme
                isSystemDarkTheme
            }
        }

    val colorScheme =
        when {
            darkTheme -> DarkColorScheme
            else -> LightColorScheme
        }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content,
    )
}

/**
 * Theme composable that observes theme preferences from SettingsRepository
 * This version is used when you want the theme to react to preference changes
 */
@Composable
fun RdwatchTheme(
    settingsRepository: SettingsRepository,
    content: @Composable () -> Unit,
) {
    val themeMode by settingsRepository.themeMode.collectAsState(initial = ThemeMode.SYSTEM)

    RdwatchTheme(
        themeMode = themeMode,
        dynamicTheme = true,
        content = content,
    )
}
