package com.rdwatch.androidtv.ui.settings

import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel for Settings Screen - manages all application settings
 * Follows MVVM architecture with BaseViewModel pattern
 */
@HiltViewModel
class SettingsViewModel @Inject constructor() : BaseViewModel<SettingsUiState>() {
    
    override fun createInitialState(): SettingsUiState {
        return SettingsUiState()
    }
    
    init {
        loadSettings()
    }
    
    /**
     * Load saved settings from preferences
     */
    private fun loadSettings() {
        launchSafely {
            // TODO: In real implementation, load from SharedPreferences or DataStore
            // For now, using default values
            updateState { 
                it.copy(
                    isLoading = false,
                    isLoaded = true
                )
            }
        }
    }
    
    /**
     * Update video quality setting
     */
    fun updateVideoQuality(quality: VideoQuality) {
        updateState { it.copy(videoQuality = quality) }
        saveSettings()
    }
    
    /**
     * Update playback speed setting
     */
    fun updatePlaybackSpeed(speed: PlaybackSpeed) {
        updateState { it.copy(playbackSpeed = speed) }
        saveSettings()
    }
    
    /**
     * Toggle subtitles setting
     */
    fun toggleSubtitles(enabled: Boolean) {
        updateState { it.copy(subtitlesEnabled = enabled) }
        saveSettings()
    }
    
    /**
     * Toggle auto play setting
     */
    fun toggleAutoPlay(enabled: Boolean) {
        updateState { it.copy(autoPlay = enabled) }
        saveSettings()
    }
    
    /**
     * Toggle dark mode setting
     */
    fun toggleDarkMode(enabled: Boolean) {
        updateState { it.copy(darkMode = enabled) }
        saveSettings()
    }
    
    /**
     * Toggle parental controls setting
     */
    fun toggleParentalControls(enabled: Boolean) {
        updateState { it.copy(parentalControlsEnabled = enabled) }
        saveSettings()
    }
    
    /**
     * Toggle notifications setting
     */
    fun toggleNotifications(enabled: Boolean) {
        updateState { it.copy(notificationsEnabled = enabled) }
        saveSettings()
    }
    
    /**
     * Update data usage limit setting
     */
    fun updateDataUsageLimit(limit: DataUsageLimit) {
        updateState { it.copy(dataUsageLimit = limit) }
        saveSettings()
    }
    
    /**
     * Reset all settings to defaults
     */
    fun resetToDefaults() {
        updateState { 
            SettingsUiState(
                isLoading = false,
                isLoaded = true
            )
        }
        saveSettings()
    }
    
    /**
     * Save settings to persistent storage
     */
    private fun saveSettings() {
        launchSafely {
            // TODO: In real implementation, save to SharedPreferences or DataStore
            // For now, just update state to indicate saving
            updateState { it.copy(isSaving = true) }
            
            // Simulate save delay
            kotlinx.coroutines.delay(300)
            
            updateState { it.copy(isSaving = false) }
        }
    }
    
    override fun handleError(exception: Throwable) {
        updateState { 
            it.copy(
                isLoading = false,
                isSaving = false,
                error = "Settings error: ${exception.message}"
            )
        }
    }
}

/**
 * UI State for Settings Screen
 */
data class SettingsUiState(
    val videoQuality: VideoQuality = VideoQuality.AUTO,
    val playbackSpeed: PlaybackSpeed = PlaybackSpeed.NORMAL,
    val subtitlesEnabled: Boolean = true,
    val autoPlay: Boolean = true,
    val darkMode: Boolean = true,
    val parentalControlsEnabled: Boolean = false,
    val notificationsEnabled: Boolean = true,
    val dataUsageLimit: DataUsageLimit = DataUsageLimit.UNLIMITED,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isLoaded: Boolean = false,
    val error: String? = null
) {
    val hasChanges: Boolean get() = isLoaded && !isSaving
}

// Settings enums - moved from SettingsScreen to be shared
enum class VideoQuality(val displayName: String) {
    AUTO("Auto"),
    HD_1080P("1080p HD"),
    HD_720P("720p HD"),
    SD_480P("480p SD")
}

enum class PlaybackSpeed(val displayName: String) {
    SLOW("0.75x"),
    NORMAL("1.0x"),
    FAST("1.25x"),
    FASTER("1.5x"),
    FASTEST("2.0x")
}

enum class DataUsageLimit(val displayName: String) {
    UNLIMITED("Unlimited"),
    HIGH("High (5GB/month)"),
    MEDIUM("Medium (2GB/month)"),
    LOW("Low (1GB/month)")
}