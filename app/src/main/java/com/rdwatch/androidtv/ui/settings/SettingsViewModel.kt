package com.rdwatch.androidtv.ui.settings

import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.auth.AuthRepository
import com.rdwatch.androidtv.data.preferences.models.*
import com.rdwatch.androidtv.data.repository.SettingsRepository
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

/**
 * ViewModel for Settings Screen - manages all application settings
 * Follows MVVM architecture with BaseViewModel pattern
 */
@HiltViewModel
class SettingsViewModel
    @Inject
    constructor(
        private val authRepository: AuthRepository,
        private val settingsRepository: SettingsRepository,
    ) : BaseViewModel<SettingsUiState>() {
        override fun createInitialState(): SettingsUiState {
            return SettingsUiState()
        }

        init {
            loadSettings()
            observeSettingsChanges()
        }

        /**
         * Load saved settings from preferences
         */
        private fun loadSettings() {
            launchSafely {
                updateState { copy(isLoading = true) }

                // Initial load will happen through the Flow observers
                updateState {
                    copy(
                        isLoading = false,
                        isLoaded = true,
                    )
                }
            }
        }

        /**
         * Observe settings changes from repository
         */
        private fun observeSettingsChanges() {
            // Observe video quality
            settingsRepository.videoQuality
                .onEach { quality ->
                    updateState { copy(videoQuality = quality.toUiModel()) }
                }
                .launchIn(viewModelScope)

            // Observe playback speed
            settingsRepository.playbackSpeed
                .onEach { speed ->
                    updateState { copy(playbackSpeed = speed.toUiModel()) }
                }
                .launchIn(viewModelScope)

            // Observe subtitles enabled
            settingsRepository.subtitlesEnabled
                .onEach { enabled ->
                    updateState { copy(subtitlesEnabled = enabled) }
                }
                .launchIn(viewModelScope)

            // Observe auto play
            settingsRepository.autoPlay
                .onEach { enabled ->
                    updateState { copy(autoPlay = enabled) }
                }
                .launchIn(viewModelScope)

            // Observe theme mode
            settingsRepository.themeMode
                .onEach { mode ->
                    updateState { copy(darkMode = mode == ThemeMode.DARK) }
                }
                .launchIn(viewModelScope)

            // Observe parental controls
            settingsRepository.parentalControlsEnabled
                .onEach { enabled ->
                    updateState { copy(parentalControlsEnabled = enabled) }
                }
                .launchIn(viewModelScope)

            // Observe notifications
            settingsRepository.notificationsEnabled
                .onEach { enabled ->
                    updateState { copy(notificationsEnabled = enabled) }
                }
                .launchIn(viewModelScope)

            // Observe bandwidth limit
            settingsRepository.bandwidthLimit
                .onEach { limit ->
                    updateState { copy(dataUsageLimit = limit.toUiModel()) }
                }
                .launchIn(viewModelScope)
        }

        /**
         * Update video quality setting
         */
        fun updateVideoQuality(quality: VideoQuality) {
            launchSafely {
                settingsRepository.updateVideoQuality(quality.toPreferenceModel())
            }
        }

        /**
         * Update playback speed setting
         */
        fun updatePlaybackSpeed(speed: PlaybackSpeed) {
            launchSafely {
                settingsRepository.updatePlaybackSpeed(speed.toPreferenceModel())
            }
        }

        /**
         * Toggle subtitles setting
         */
        fun toggleSubtitles(enabled: Boolean) {
            launchSafely {
                settingsRepository.updateSubtitlesEnabled(enabled)
            }
        }

        /**
         * Toggle auto play setting
         */
        fun toggleAutoPlay(enabled: Boolean) {
            launchSafely {
                settingsRepository.updateAutoPlay(enabled)
            }
        }

        /**
         * Toggle dark mode setting
         */
        fun toggleDarkMode(enabled: Boolean) {
            launchSafely {
                val newMode = if (enabled) ThemeMode.DARK else ThemeMode.LIGHT
                settingsRepository.updateThemeMode(newMode)
            }
        }

        /**
         * Toggle parental controls setting
         */
        fun toggleParentalControls(enabled: Boolean) {
            launchSafely {
                settingsRepository.updateParentalControlsEnabled(enabled)
            }
        }

        /**
         * Toggle notifications setting
         */
        fun toggleNotifications(enabled: Boolean) {
            launchSafely {
                settingsRepository.updateNotificationsEnabled(enabled)
            }
        }

        /**
         * Update data usage limit setting
         */
        fun updateDataUsageLimit(limit: DataUsageLimit) {
            launchSafely {
                settingsRepository.updateBandwidthLimit(limit.toPreferenceModel())
            }
        }

        /**
         * Reset all settings to defaults
         */
        fun resetToDefaults() {
            launchSafely {
                updateState { copy(isLoading = true) }
                settingsRepository.resetToDefaults()
                updateState { copy(isLoading = false) }
            }
        }

        /**
         * Sign out the current user
         */
        fun signOut() {
            launchSafely {
                updateState { copy(isLoading = true) }
                settingsRepository.resetForSignOut()
                authRepository.logout()
                updateState { copy(isLoading = false) }
            }
        }

        override fun handleError(exception: Throwable) {
            updateState {
                copy(
                    isLoading = false,
                    isSaving = false,
                    error = "Settings error: ${exception.message}",
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
    val error: String? = null,
) {
    val hasChanges: Boolean get() = isLoaded && !isSaving
}

// Settings enums - moved from SettingsScreen to be shared
enum class VideoQuality(val displayName: String) {
    AUTO("Auto"),
    HD_1080P("1080p HD"),
    HD_720P("720p HD"),
    SD_480P("480p SD"),
}

enum class PlaybackSpeed(val displayName: String) {
    SLOW("0.75x"),
    NORMAL("1.0x"),
    FAST("1.25x"),
    FASTER("1.5x"),
    FASTEST("2.0x"),
}

enum class DataUsageLimit(val displayName: String) {
    UNLIMITED("Unlimited"),
    HIGH("High (5GB/month)"),
    MEDIUM("Medium (2GB/month)"),
    LOW("Low (1GB/month)"),
}

// Extension functions to convert between UI models and preference models

private fun VideoQualityPreference.toUiModel(): VideoQuality =
    when (this) {
        VideoQualityPreference.AUTO -> VideoQuality.AUTO
        VideoQualityPreference.FHD_1080P -> VideoQuality.HD_1080P
        VideoQualityPreference.HD_720P -> VideoQuality.HD_720P
        VideoQualityPreference.SD_480P -> VideoQuality.SD_480P
        else -> VideoQuality.AUTO
    }

private fun VideoQuality.toPreferenceModel(): VideoQualityPreference =
    when (this) {
        VideoQuality.AUTO -> VideoQualityPreference.AUTO
        VideoQuality.HD_1080P -> VideoQualityPreference.FHD_1080P
        VideoQuality.HD_720P -> VideoQualityPreference.HD_720P
        VideoQuality.SD_480P -> VideoQualityPreference.SD_480P
    }

private fun PlaybackSpeedPreference.toUiModel(): PlaybackSpeed =
    when (this) {
        PlaybackSpeedPreference.SPEED_0_75X -> PlaybackSpeed.SLOW
        PlaybackSpeedPreference.SPEED_1X -> PlaybackSpeed.NORMAL
        PlaybackSpeedPreference.SPEED_1_25X -> PlaybackSpeed.FAST
        PlaybackSpeedPreference.SPEED_1_5X -> PlaybackSpeed.FASTER
        PlaybackSpeedPreference.SPEED_2X -> PlaybackSpeed.FASTEST
        else -> PlaybackSpeed.NORMAL
    }

private fun PlaybackSpeed.toPreferenceModel(): PlaybackSpeedPreference =
    when (this) {
        PlaybackSpeed.SLOW -> PlaybackSpeedPreference.SPEED_0_75X
        PlaybackSpeed.NORMAL -> PlaybackSpeedPreference.SPEED_1X
        PlaybackSpeed.FAST -> PlaybackSpeedPreference.SPEED_1_25X
        PlaybackSpeed.FASTER -> PlaybackSpeedPreference.SPEED_1_5X
        PlaybackSpeed.FASTEST -> PlaybackSpeedPreference.SPEED_2X
    }

private fun BandwidthLimit.toUiModel(): DataUsageLimit =
    when (this.mbps) {
        0 -> DataUsageLimit.UNLIMITED
        100 -> DataUsageLimit.HIGH
        50 -> DataUsageLimit.MEDIUM
        25 -> DataUsageLimit.LOW
        else -> DataUsageLimit.UNLIMITED
    }

private fun DataUsageLimit.toPreferenceModel(): BandwidthLimit =
    when (this) {
        DataUsageLimit.UNLIMITED -> BandwidthLimit.UNLIMITED
        DataUsageLimit.HIGH -> BandwidthLimit.LIMIT_100
        DataUsageLimit.MEDIUM -> BandwidthLimit.LIMIT_50
        DataUsageLimit.LOW -> BandwidthLimit.LIMIT_25
    }
