package com.rdwatch.androidtv.data.preferences.models

import androidx.datastore.preferences.core.Preferences

/**
 * Defines preference categories and their associated keys for organization
 * and bulk operations.
 */
object PreferenceCategories {
    /**
     * Playback related preferences
     */
    val playbackKeys =
        listOf(
            PreferenceKeys.VIDEO_QUALITY,
            PreferenceKeys.PLAYBACK_SPEED,
            PreferenceKeys.SUBTITLES_ENABLED,
            PreferenceKeys.SUBTITLE_LANGUAGE,
            PreferenceKeys.AUTO_PLAY,
            PreferenceKeys.AUTO_PLAY_DELAY,
            PreferenceKeys.EXTERNAL_PLAYER_PACKAGE,
            PreferenceKeys.PREFERRED_AUDIO_LANGUAGE,
        )

    /**
     * Display related preferences
     */
    val displayKeys =
        listOf(
            PreferenceKeys.THEME_MODE,
            PreferenceKeys.CONTENT_LAYOUT,
            PreferenceKeys.SHOW_QUALITY_BADGES,
            PreferenceKeys.OVERSCAN_MARGIN,
        )

    /**
     * Network related preferences
     */
    val networkKeys =
        listOf(
            PreferenceKeys.BANDWIDTH_LIMIT_MBPS,
            PreferenceKeys.CONNECTION_TIMEOUT_SECONDS,
            PreferenceKeys.RETRY_ATTEMPTS,
            PreferenceKeys.PREFER_CDN,
        )

    /**
     * Storage related preferences
     */
    val storageKeys =
        listOf(
            PreferenceKeys.CACHE_SIZE_LIMIT_MB,
            PreferenceKeys.DOWNLOAD_LOCATION,
            PreferenceKeys.AUTO_DELETE_WATCHED,
            PreferenceKeys.KEEP_SUBTITLES_DAYS,
        )

    /**
     * Scraper related preferences
     */
    val scraperKeys =
        listOf(
            PreferenceKeys.DEFAULT_SCRAPER_MANIFEST,
            PreferenceKeys.SCRAPER_TIMEOUT_SECONDS,
            PreferenceKeys.ENABLE_ADULT_SCRAPERS,
            PreferenceKeys.SCRAPER_CONCURRENCY,
        )

    /**
     * Account related preferences
     */
    val accountKeys =
        listOf(
            PreferenceKeys.SHOW_PREMIUM_DAYS,
            PreferenceKeys.AUTO_REFRESH_TOKEN,
            PreferenceKeys.DEVICE_NAME,
        )

    /**
     * Parental control related preferences
     */
    val parentalKeys =
        listOf(
            PreferenceKeys.PARENTAL_CONTROLS_ENABLED,
            PreferenceKeys.PARENTAL_PIN_HASH,
            PreferenceKeys.PARENTAL_PIN_SALT,
            PreferenceKeys.PARENTAL_MAX_RATING,
            PreferenceKeys.REQUIRE_PIN_FOR_SETTINGS,
        )

    /**
     * Notification related preferences
     */
    val notificationKeys =
        listOf(
            PreferenceKeys.NOTIFICATIONS_ENABLED,
            PreferenceKeys.NOTIFY_NEW_EPISODES,
            PreferenceKeys.NOTIFY_DOWNLOAD_COMPLETE,
        )

    /**
     * Analytics related preferences
     */
    val analyticsKeys =
        listOf(
            PreferenceKeys.ANALYTICS_ENABLED,
            PreferenceKeys.CRASH_REPORTING_ENABLED,
        )

    /**
     * Get all preference keys for a given category
     */
    fun getKeysForCategory(category: SettingsCategory): List<Preferences.Key<*>> {
        return when (category) {
            SettingsCategory.PLAYBACK -> playbackKeys
            SettingsCategory.DISPLAY -> displayKeys
            SettingsCategory.NETWORK -> networkKeys
            SettingsCategory.STORAGE -> storageKeys
            SettingsCategory.SCRAPERS -> scraperKeys
            SettingsCategory.ACCOUNT -> accountKeys
            SettingsCategory.PARENTAL -> parentalKeys
            SettingsCategory.NOTIFICATIONS -> notificationKeys
            SettingsCategory.ABOUT -> emptyList() // About section has no preferences
        }
    }

    /**
     * Get all preference keys that should be included in backup
     */
    fun getBackupKeys(): List<Preferences.Key<*>> {
        return playbackKeys + displayKeys + networkKeys + storageKeys +
            scraperKeys + accountKeys + notificationKeys + analyticsKeys
        // Note: Parental controls are excluded from backup for security
    }

    /**
     * Get preference keys that should be reset on sign out
     */
    fun getSignOutResetKeys(): List<Preferences.Key<*>> {
        return accountKeys +
            listOf(
                PreferenceKeys.DOWNLOAD_LOCATION,
                PreferenceKeys.DEVICE_NAME,
            )
    }
}

/**
 * Default values for preferences
 */
object PreferenceDefaults {
    // Playback
    const val VIDEO_QUALITY = "auto"
    const val PLAYBACK_SPEED = 1.0f
    const val SUBTITLES_ENABLED = true
    const val SUBTITLE_LANGUAGE = "en"
    const val AUTO_PLAY = true
    const val AUTO_PLAY_DELAY_SECONDS = 10
    const val EXTERNAL_PLAYER_PACKAGE = ""
    const val PREFERRED_AUDIO_LANGUAGE = "en"

    // Display
    const val THEME_MODE = "system"
    const val CONTENT_LAYOUT = "grid"
    const val SHOW_QUALITY_BADGES = true
    const val OVERSCAN_MARGIN_DP = 32

    // Network
    const val BANDWIDTH_LIMIT_MBPS = 0 // Unlimited
    const val CONNECTION_TIMEOUT_SECONDS = 30
    const val RETRY_ATTEMPTS = 3
    const val PREFER_CDN = ""

    // Storage
    const val CACHE_SIZE_LIMIT_MB = 1024L // 1GB
    const val DOWNLOAD_LOCATION = ""
    const val AUTO_DELETE_WATCHED = false
    const val KEEP_SUBTITLES_DAYS = 30

    // Scrapers
    const val DEFAULT_SCRAPER_MANIFEST = ""
    const val SCRAPER_TIMEOUT_SECONDS = 15
    const val ENABLE_ADULT_SCRAPERS = false
    const val SCRAPER_CONCURRENCY = 3

    // Account
    const val SHOW_PREMIUM_DAYS = true
    const val AUTO_REFRESH_TOKEN = true
    const val DEVICE_NAME = ""

    // Parental
    const val PARENTAL_CONTROLS_ENABLED = false
    const val PARENTAL_PIN_HASH = ""
    const val PARENTAL_PIN_SALT = ""
    const val PARENTAL_MAX_RATING = "UNRATED"
    const val REQUIRE_PIN_FOR_SETTINGS = false

    // Notifications
    const val NOTIFICATIONS_ENABLED = true
    const val NOTIFY_NEW_EPISODES = true
    const val NOTIFY_DOWNLOAD_COMPLETE = true

    // Analytics
    const val ANALYTICS_ENABLED = true
    const val CRASH_REPORTING_ENABLED = true

    // App Metadata
    const val SETTINGS_VERSION = 1
    const val FIRST_LAUNCH_COMPLETED = false
    const val LAST_BACKUP_TIMESTAMP = 0L
}
