package com.rdwatch.androidtv.autoplay

import com.rdwatch.androidtv.data.repository.NextEpisodeResult
import com.rdwatch.androidtv.navigation.PlaybackNavigationHelper
import com.rdwatch.androidtv.ui.viewmodel.AutoPlayController
import com.rdwatch.androidtv.util.DebugLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Bridge class that coordinates between AutoPlayController and PlaybackNavigationHelper
 * Provides a unified interface for auto-play navigation that works with both
 * the new enhanced system and existing callback-based navigation
 */
@Singleton
class AutoPlayNavigationBridge
    @Inject
    constructor(
        private val playbackNavigationHelper: PlaybackNavigationHelper,
    ) {
        /**
         * Handle auto-play navigation with full episode resolution
         * This is the preferred method that integrates with the new episode resolution service
         */
        fun handleAutoPlayNavigation(
            nextEpisode: NextEpisodeResult,
            showTitle: String,
            autoPlayController: AutoPlayController,
            fallbackNavigationCallback: ((tmdbShowId: Int, seasonNumber: Int, episodeNumber: Int) -> Unit)? = null,
        ) {
            DebugLogger.d(
                "AutoPlayNavigationBridge",
                "Handling auto-play navigation for ${nextEpisode.getDisplayText()}",
            )

            // Use the enhanced navigation with episode resolution
            playbackNavigationHelper.navigateToNextEpisode(
                nextEpisode = nextEpisode,
                showTitle = showTitle,
                onSuccess = { streamingUrl, episodeTitle ->
                    DebugLogger.i(
                        "AutoPlayNavigationBridge",
                        "Auto-play navigation successful: $episodeTitle",
                    )
                    autoPlayController.onAutoPlayNavigationSuccess(streamingUrl, episodeTitle)
                },
                onError = { errorMessage ->
                    DebugLogger.w(
                        "AutoPlayNavigationBridge",
                        "Enhanced auto-play failed: $errorMessage. Attempting fallback navigation.",
                    )

                    // Notify controller of the error
                    autoPlayController.onAutoPlayNavigationError(errorMessage)

                    // Attempt fallback navigation if available
                    if (fallbackNavigationCallback != null) {
                        try {
                            DebugLogger.d(
                                "AutoPlayNavigationBridge",
                                "Attempting fallback navigation for ${nextEpisode.getDisplayText()}",
                            )
                            fallbackNavigationCallback.invoke(
                                nextEpisode.tmdbShowId,
                                nextEpisode.seasonNumber,
                                nextEpisode.episodeNumber,
                            )
                        } catch (e: Exception) {
                            DebugLogger.e(
                                "AutoPlayNavigationBridge",
                                "Fallback navigation also failed",
                                e,
                            )
                        }
                    } else {
                        DebugLogger.w(
                            "AutoPlayNavigationBridge",
                            "No fallback navigation available",
                        )
                    }
                },
            )
        }

        /**
         * Legacy auto-play navigation using only callback-based approach
         * This is used when the enhanced episode resolution is not available
         */
        fun handleLegacyAutoPlayNavigation(
            nextEpisode: NextEpisodeResult,
            navigationCallback: (tmdbShowId: Int, seasonNumber: Int, episodeNumber: Int) -> Unit,
        ) {
            DebugLogger.d(
                "AutoPlayNavigationBridge",
                "Using legacy auto-play navigation for ${nextEpisode.getDisplayText()}",
            )

            try {
                navigationCallback.invoke(
                    nextEpisode.tmdbShowId,
                    nextEpisode.seasonNumber,
                    nextEpisode.episodeNumber,
                )
            } catch (e: Exception) {
                DebugLogger.e(
                    "AutoPlayNavigationBridge",
                    "Legacy auto-play navigation failed",
                    e,
                )
            }
        }

        /**
         * Check if enhanced auto-play navigation is supported
         */
        fun isEnhancedNavigationSupported(): Boolean {
            return playbackNavigationHelper.isAutoPlayNavigationSupported()
        }
    }
