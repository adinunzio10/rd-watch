package com.rdwatch.androidtv.ui.theme

import androidx.compose.ui.unit.dp

/**
 * UI Constants for Android TV application
 * Centralized configuration for timeouts, dimensions, and UI behavior
 */
object UIConstants {
    /**
     * Timeout configurations for various UI operations
     */
    object Timeouts {
        /** Video loading timeout in milliseconds */
        const val VIDEO_LOADING_TIMEOUT_MS = 15000L

        /** Player controls auto-hide delay in milliseconds */
        const val PLAYER_CONTROLS_AUTO_HIDE_MS = 5000L

        /** Network request timeout in milliseconds */
        const val NETWORK_REQUEST_TIMEOUT_MS = 30000L

        /** Search debounce delay in milliseconds */
        const val SEARCH_DEBOUNCE_MS = 300L
    }

    /**
     * Dimensions optimized for Android TV 10-foot experience
     */
    object Dimensions {
        /** Standard content width for dialogs and modals */
        val STANDARD_CONTENT_WIDTH = 600.dp

        /** Minimum dialog width */
        val MIN_DIALOG_WIDTH = 400.dp

        /** Maximum dialog width */
        val MAX_DIALOG_WIDTH = 800.dp

        /** Episode grid height - responsive to screen size */
        val EPISODE_GRID_HEIGHT = 480.dp // Reduced from fixed 600.dp for better TV experience

        /** Maximum height for bottom sheets and overlays */
        val MAX_OVERLAY_HEIGHT = 600.dp

        /** Standard card width for content items */
        val STANDARD_CARD_WIDTH = 200.dp

        /** Standard card height for content items */
        val STANDARD_CARD_HEIGHT = 300.dp

        /** Focus border width for TV navigation */
        val FOCUS_BORDER_WIDTH = 3.dp

        /** Standard padding for TV-safe areas */
        val TV_SAFE_PADDING = 48.dp

        /** Grid spacing for content grids */
        val GRID_SPACING = 16.dp

        /** Standard corner radius for cards */
        val CARD_CORNER_RADIUS = 8.dp
    }

    /**
     * Player-specific configurations
     */
    object Player {
        /** Default auto-hide delay for player controls */
        const val DEFAULT_AUTO_HIDE_DELAY_MS = 5000L

        /** Extended auto-hide delay for slower interactions */
        const val EXTENDED_AUTO_HIDE_DELAY_MS = 8000L

        /** Short auto-hide delay for quick interactions */
        const val SHORT_AUTO_HIDE_DELAY_MS = 3000L

        /** Seek increment in seconds */
        const val SEEK_INCREMENT_SECONDS = 10

        /** Long seek increment in seconds */
        const val LONG_SEEK_INCREMENT_SECONDS = 30
    }

    /**
     * Animation durations for smooth TV experience
     */
    object Animations {
        /** Standard animation duration in milliseconds */
        const val STANDARD_DURATION_MS = 300

        /** Fast animation duration in milliseconds */
        const val FAST_DURATION_MS = 150

        /** Slow animation duration in milliseconds */
        const val SLOW_DURATION_MS = 500

        /** Focus animation duration in milliseconds */
        const val FOCUS_DURATION_MS = 200
    }

    /**
     * Content loading and pagination
     */
    object Content {
        /** Default page size for content loading */
        const val DEFAULT_PAGE_SIZE = 20

        /** Large page size for efficient loading */
        const val LARGE_PAGE_SIZE = 50

        /** Small page size for initial loads */
        const val SMALL_PAGE_SIZE = 10

        /** Maximum retry attempts for content loading */
        const val MAX_RETRY_ATTEMPTS = 3
    }

    /**
     * Responsive dimension calculations based on screen size
     */
    object Responsive {
        /** Calculate episode grid height based on screen density */
        fun getEpisodeGridHeight(screenHeightDp: Float): androidx.compose.ui.unit.Dp {
            return when {
                screenHeightDp >= 1080 -> 600.dp // Large screens
                screenHeightDp >= 720 -> 480.dp // Medium screens
                else -> 360.dp // Small screens
            }
        }

        /** Calculate dialog width based on screen width */
        fun getDialogWidth(screenWidthDp: Float): androidx.compose.ui.unit.Dp {
            return when {
                screenWidthDp >= 1920 -> Dimensions.MAX_DIALOG_WIDTH
                screenWidthDp >= 1280 -> Dimensions.STANDARD_CONTENT_WIDTH
                else -> Dimensions.MIN_DIALOG_WIDTH
            }
        }

        /** Calculate grid columns based on screen width */
        fun getGridColumns(screenWidthDp: Float): Int {
            return when {
                screenWidthDp >= 1920 -> 8 // 4K screens
                screenWidthDp >= 1280 -> 6 // 1080p screens
                screenWidthDp >= 960 -> 4 // 720p screens
                else -> 3 // Smaller screens
            }
        }
    }
}
