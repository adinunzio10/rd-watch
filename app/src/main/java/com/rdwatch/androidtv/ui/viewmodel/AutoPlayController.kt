package com.rdwatch.androidtv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.auth.UserSessionManager
import com.rdwatch.androidtv.data.entities.AutoPlaySettingsEntity
import com.rdwatch.androidtv.data.repository.NextEpisodeRepository
import com.rdwatch.androidtv.data.repository.NextEpisodeResult
import com.rdwatch.androidtv.util.DebugLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State representing auto-play countdown status
 */
data class AutoPlayState(
    val showCountdown: Boolean = false,
    val nextEpisode: NextEpisodeResult? = null,
    val showTitle: String = "",
    val posterUrl: String? = null,
    val countdownSeconds: Int = 10,
    val isLoading: Boolean = false,
    val error: String? = null,
)

/**
 * Controller for managing auto-play countdown logic and state
 * Handles episode completion detection, next episode calculation, and user interactions
 */
@HiltViewModel
class AutoPlayController
    @Inject
    constructor(
        private val nextEpisodeRepository: NextEpisodeRepository,
        private val userSessionManager: UserSessionManager,
    ) : ViewModel() {
        private val _autoPlayState = MutableStateFlow(AutoPlayState())
        val autoPlayState: StateFlow<AutoPlayState> = _autoPlayState.asStateFlow()

        /**
         * Check if auto-play should be triggered for the completed episode
         * Called when an episode reaches completion threshold (90%+ watched)
         */
        fun checkAutoPlayEligibility(
            tmdbShowId: Int,
            seasonNumber: Int,
            episodeNumber: Int,
            showTitle: String,
            posterUrl: String? = null,
        ) {
            DebugLogger.d("AutoPlayController", "Checking auto-play eligibility for $showTitle S${seasonNumber}E$episodeNumber")

            viewModelScope.launch {
                try {
                    _autoPlayState.value = _autoPlayState.value.copy(isLoading = true, error = null)

                    val currentUserId =
                        try {
                            userSessionManager.getCurrentUserId()
                        } catch (e: Exception) {
                            DebugLogger.w("AutoPlayController", "No current user, skipping auto-play check")
                            _autoPlayState.value = _autoPlayState.value.copy(isLoading = false)
                            return@launch
                        }

                    // Get next episode from repository
                    val nextEpisode = nextEpisodeRepository.getNextEpisode(currentUserId, tmdbShowId)

                    if (nextEpisode == null) {
                        DebugLogger.i("AutoPlayController", "No next episode available for $showTitle")
                        _autoPlayState.value =
                            _autoPlayState.value.copy(
                                isLoading = false,
                                showCountdown = false,
                            )
                        return@launch
                    }

                    // Check if auto-play is enabled for this episode
                    if (!nextEpisode.canAutoPlay) {
                        DebugLogger.i("AutoPlayController", "Auto-play is disabled for $showTitle")
                        _autoPlayState.value =
                            _autoPlayState.value.copy(
                                isLoading = false,
                                showCountdown = false,
                            )
                        return@launch
                    }

                    // Get user's auto-play settings
                    val autoPlaySettings = nextEpisodeRepository.getAutoPlaySettings(currentUserId)
                    val countdownSeconds = autoPlaySettings?.countdownSeconds ?: 10

                    DebugLogger.i("AutoPlayController", "Showing auto-play countdown for next episode: ${nextEpisode.getDisplayText()}")

                    // Show countdown with next episode info
                    _autoPlayState.value =
                        AutoPlayState(
                            showCountdown = true,
                            nextEpisode = nextEpisode,
                            showTitle = showTitle,
                            posterUrl = posterUrl,
                            countdownSeconds = countdownSeconds,
                            isLoading = false,
                            error = null,
                        )
                } catch (e: Exception) {
                    DebugLogger.e("AutoPlayController", "Error checking auto-play eligibility", e)
                    _autoPlayState.value =
                        _autoPlayState.value.copy(
                            isLoading = false,
                            error = "Failed to check next episode: ${e.message}",
                            showCountdown = false,
                        )
                }
            }
        }

        /**
         * User clicked Play Now or countdown reached zero
         * Triggers immediate playback of the next episode
         */
        fun playNextEpisode(onNavigateToEpisode: (NextEpisodeResult) -> Unit) {
            val currentState = _autoPlayState.value
            val nextEpisode = currentState.nextEpisode

            if (nextEpisode == null) {
                DebugLogger.w("AutoPlayController", "No next episode to play")
                _autoPlayState.value =
                    _autoPlayState.value.copy(
                        showCountdown = false,
                        error = "Next episode not available",
                    )
                return
            }

            // Validate episode data before navigation
            if (nextEpisode.tmdbShowId <= 0 || nextEpisode.seasonNumber < 0 || nextEpisode.episodeNumber <= 0) {
                DebugLogger.e("AutoPlayController", "Invalid next episode data: ${nextEpisode.getDisplayText()}")
                _autoPlayState.value =
                    _autoPlayState.value.copy(
                        showCountdown = false,
                        error = "Invalid episode data",
                    )
                return
            }

            DebugLogger.i("AutoPlayController", "Playing next episode: ${nextEpisode.getDisplayText()}")

            try {
                // Hide countdown first
                hideCountdown()

                // Trigger navigation with error handling
                onNavigateToEpisode(nextEpisode)
            } catch (e: Exception) {
                DebugLogger.e("AutoPlayController", "Error navigating to next episode", e)
                _autoPlayState.value =
                    _autoPlayState.value.copy(
                        error = "Navigation failed: ${e.message}",
                    )
            }
        }

        /**
         * User cancelled the auto-play countdown
         * Hides the countdown and returns to normal playback end state
         */
        fun cancelAutoPlay() {
            DebugLogger.i("AutoPlayController", "Auto-play cancelled by user")
            hideCountdown()
        }

        /**
         * Hide the countdown overlay
         */
        fun hideCountdown() {
            _autoPlayState.value = AutoPlayState()
        }

        /**
         * Reset any error state and clear the countdown
         * Useful for recovery from error states
         */
        fun resetState() {
            DebugLogger.d("AutoPlayController", "Resetting auto-play state")
            _autoPlayState.value = AutoPlayState()
        }

        /**
         * Check if auto-play is currently active (showing countdown or processing)
         */
        fun isAutoPlayActive(): Boolean {
            val state = _autoPlayState.value
            return state.showCountdown || state.isLoading
        }

        /**
         * Update episode progress and potentially trigger auto-play check
         * This method should be called from playback progress tracking
         */
        fun updateEpisodeProgress(
            tmdbShowId: Int,
            seasonNumber: Int,
            episodeNumber: Int,
            progressSeconds: Long,
            durationSeconds: Long,
            showTitle: String,
            posterUrl: String? = null,
            episodeTitle: String? = null,
            deviceInfo: String? = null,
        ) {
            // Input validation
            if (tmdbShowId <= 0 || seasonNumber < 0 || episodeNumber <= 0) {
                DebugLogger.w("AutoPlayController", "Invalid episode identifiers: show=$tmdbShowId, season=$seasonNumber, episode=$episodeNumber")
                return
            }

            if (progressSeconds < 0 || durationSeconds <= 0) {
                DebugLogger.w("AutoPlayController", "Invalid progress values: progress=$progressSeconds, duration=$durationSeconds")
                return
            }

            viewModelScope.launch {
                try {
                    val currentUserId =
                        try {
                            userSessionManager.getCurrentUserId()
                        } catch (e: Exception) {
                            DebugLogger.w("AutoPlayController", "No current user for progress tracking")
                            return@launch
                        }

                    // Update progress in repository
                    nextEpisodeRepository.updateEpisodeProgress(
                        userId = currentUserId,
                        tmdbShowId = tmdbShowId,
                        seasonNumber = seasonNumber,
                        episodeNumber = episodeNumber,
                        progressSeconds = progressSeconds,
                        durationSeconds = durationSeconds,
                        episodeTitle = episodeTitle,
                        deviceInfo = deviceInfo,
                    )

                    // Check if episode is now completed and should trigger auto-play
                    val watchPercentage =
                        if (durationSeconds > 0) {
                            (progressSeconds.toFloat() / durationSeconds.toFloat()).coerceIn(0f, 1f)
                        } else {
                            0f
                        }

                    // Get auto-play settings to check completion threshold
                    val autoPlaySettings = nextEpisodeRepository.getAutoPlaySettings(currentUserId)
                    val completionThreshold = autoPlaySettings?.autoMarkWatchedThreshold ?: 0.9f

                    // Only trigger if above threshold, not already showing countdown, and not already completed
                    if (watchPercentage >= completionThreshold &&
                        !_autoPlayState.value.showCountdown &&
                        !_autoPlayState.value.isLoading
                    ) {
                        DebugLogger.d("AutoPlayController", "Episode completed (${"%.1f".format(watchPercentage * 100)}%), checking auto-play eligibility")
                        checkAutoPlayEligibility(
                            tmdbShowId = tmdbShowId,
                            seasonNumber = seasonNumber,
                            episodeNumber = episodeNumber,
                            showTitle = showTitle,
                            posterUrl = posterUrl,
                        )
                    }
                } catch (e: Exception) {
                    DebugLogger.e("AutoPlayController", "Error updating episode progress", e)
                    // Clear any loading state on error
                    if (_autoPlayState.value.isLoading) {
                        _autoPlayState.value =
                            _autoPlayState.value.copy(
                                isLoading = false,
                                error = "Failed to track progress: ${e.message}",
                            )
                    }
                }
            }
        }

        /**
         * Get auto-play settings for the current user
         */
        suspend fun getAutoPlaySettings(): AutoPlaySettingsEntity? {
            return try {
                val currentUserId = userSessionManager.getCurrentUserId()
                nextEpisodeRepository.getAutoPlaySettings(currentUserId)
            } catch (e: Exception) {
                DebugLogger.e("AutoPlayController", "Error getting auto-play settings", e)
                null
            }
        }

        /**
         * Update auto-play settings for the current user
         */
        fun updateAutoPlaySettings(settings: AutoPlaySettingsEntity) {
            viewModelScope.launch {
                try {
                    nextEpisodeRepository.updateAutoPlaySettings(settings)
                    DebugLogger.d("AutoPlayController", "Auto-play settings updated")
                } catch (e: Exception) {
                    DebugLogger.e("AutoPlayController", "Error updating auto-play settings", e)
                }
            }
        }

        /**
         * Check if the current episode is eligible for auto-play based on show progress
         */
        private suspend fun isEpisodeAutoPlayEligible(
            userId: Long,
            tmdbShowId: Int,
        ): Boolean {
            return try {
                val showsEligible = nextEpisodeRepository.getShowsEligibleForAutoPlay(userId)
                showsEligible.any { it.tmdbShowId == tmdbShowId }
            } catch (e: Exception) {
                DebugLogger.e("AutoPlayController", "Error checking episode auto-play eligibility", e)
                false
            }
        }
    }
