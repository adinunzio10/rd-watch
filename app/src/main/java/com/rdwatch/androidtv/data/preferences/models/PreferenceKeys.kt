package com.rdwatch.androidtv.data.preferences.models

import androidx.datastore.preferences.core.*

/**
 * Centralized preference keys for DataStore.
 * Organized by category for easy management and discoverability.
 */
object PreferenceKeys {
    
    // Playback Settings
    val VIDEO_QUALITY = stringPreferencesKey("video_quality")
    val PLAYBACK_SPEED = floatPreferencesKey("playback_speed")
    val SUBTITLES_ENABLED = booleanPreferencesKey("subtitles_enabled")
    val SUBTITLE_LANGUAGE = stringPreferencesKey("subtitle_language")
    val AUTO_PLAY = booleanPreferencesKey("auto_play")
    val AUTO_PLAY_DELAY = intPreferencesKey("auto_play_delay_seconds")
    val EXTERNAL_PLAYER_PACKAGE = stringPreferencesKey("external_player_package")
    val PREFERRED_AUDIO_LANGUAGE = stringPreferencesKey("preferred_audio_language")
    
    // Display Settings
    val THEME_MODE = stringPreferencesKey("theme_mode")
    val CONTENT_LAYOUT = stringPreferencesKey("content_layout")
    val SHOW_QUALITY_BADGES = booleanPreferencesKey("show_quality_badges")
    val OVERSCAN_MARGIN = intPreferencesKey("overscan_margin_dp")
    
    // Network Settings
    val BANDWIDTH_LIMIT_MBPS = intPreferencesKey("bandwidth_limit_mbps")
    val CONNECTION_TIMEOUT_SECONDS = intPreferencesKey("connection_timeout_seconds")
    val RETRY_ATTEMPTS = intPreferencesKey("retry_attempts")
    val PREFER_CDN = stringPreferencesKey("prefer_cdn")
    
    // Storage Settings
    val CACHE_SIZE_LIMIT_MB = longPreferencesKey("cache_size_limit_mb")
    val DOWNLOAD_LOCATION = stringPreferencesKey("download_location")
    val AUTO_DELETE_WATCHED = booleanPreferencesKey("auto_delete_watched")
    val KEEP_SUBTITLES_DAYS = intPreferencesKey("keep_subtitles_days")
    
    // Scraper Settings
    val DEFAULT_SCRAPER_MANIFEST = stringPreferencesKey("default_scraper_manifest")
    val SCRAPER_TIMEOUT_SECONDS = intPreferencesKey("scraper_timeout_seconds")
    val ENABLE_ADULT_SCRAPERS = booleanPreferencesKey("enable_adult_scrapers")
    val SCRAPER_CONCURRENCY = intPreferencesKey("scraper_concurrency")
    
    // Account Settings
    val SHOW_PREMIUM_DAYS = booleanPreferencesKey("show_premium_days")
    val AUTO_REFRESH_TOKEN = booleanPreferencesKey("auto_refresh_token")
    val DEVICE_NAME = stringPreferencesKey("device_name")
    
    // Parental Control Settings
    val PARENTAL_CONTROLS_ENABLED = booleanPreferencesKey("parental_controls_enabled")
    val PARENTAL_PIN_HASH = stringPreferencesKey("parental_pin_hash")
    val PARENTAL_PIN_SALT = stringPreferencesKey("parental_pin_salt")
    val PARENTAL_MAX_RATING = stringPreferencesKey("parental_max_rating")
    val REQUIRE_PIN_FOR_SETTINGS = booleanPreferencesKey("require_pin_for_settings")
    
    // Notification Settings
    val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
    val NOTIFY_NEW_EPISODES = booleanPreferencesKey("notify_new_episodes")
    val NOTIFY_DOWNLOAD_COMPLETE = booleanPreferencesKey("notify_download_complete")
    
    // Analytics Settings
    val ANALYTICS_ENABLED = booleanPreferencesKey("analytics_enabled")
    val CRASH_REPORTING_ENABLED = booleanPreferencesKey("crash_reporting_enabled")
    
    // App Metadata
    val SETTINGS_VERSION = intPreferencesKey("settings_version")
    val FIRST_LAUNCH_COMPLETED = booleanPreferencesKey("first_launch_completed")
    val LAST_BACKUP_TIMESTAMP = longPreferencesKey("last_backup_timestamp")
}