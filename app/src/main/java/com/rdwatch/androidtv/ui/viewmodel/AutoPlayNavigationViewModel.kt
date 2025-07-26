package com.rdwatch.androidtv.ui.viewmodel

import androidx.lifecycle.ViewModel
import com.rdwatch.androidtv.autoplay.AutoPlayNavigationBridge
import com.rdwatch.androidtv.data.repository.NextEpisodeResult
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * ViewModel wrapper for AutoPlayNavigationBridge to integrate with Compose navigation
 */
@HiltViewModel
class AutoPlayNavigationViewModel
    @Inject
    constructor(
        private val autoPlayNavigationBridge: AutoPlayNavigationBridge,
    ) : ViewModel() {
        /**
         * Handle auto-play navigation with full episode resolution
         */
        fun handleAutoPlayNavigation(
            nextEpisode: NextEpisodeResult,
            showTitle: String,
            autoPlayController: AutoPlayController,
            fallbackNavigationCallback: ((tmdbShowId: Int, seasonNumber: Int, episodeNumber: Int) -> Unit)? = null,
        ) {
            autoPlayNavigationBridge.handleAutoPlayNavigation(
                nextEpisode = nextEpisode,
                showTitle = showTitle,
                autoPlayController = autoPlayController,
                fallbackNavigationCallback = fallbackNavigationCallback,
            )
        }

        /**
         * Legacy auto-play navigation using only callback-based approach
         */
        fun handleLegacyAutoPlayNavigation(
            nextEpisode: NextEpisodeResult,
            navigationCallback: (tmdbShowId: Int, seasonNumber: Int, episodeNumber: Int) -> Unit,
        ) {
            autoPlayNavigationBridge.handleLegacyAutoPlayNavigation(
                nextEpisode = nextEpisode,
                navigationCallback = navigationCallback,
            )
        }

        /**
         * Check if enhanced auto-play navigation is supported
         */
        fun isEnhancedNavigationSupported(): Boolean {
            return autoPlayNavigationBridge.isEnhancedNavigationSupported()
        }
    }
