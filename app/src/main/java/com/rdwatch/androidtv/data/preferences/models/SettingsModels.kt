package com.rdwatch.androidtv.data.preferences.models

/**
 * Data models for settings that require more complex representation
 * than simple primitive types.
 */

/**
 * Theme mode options for the application
 */
enum class ThemeMode(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    SYSTEM("system"),
    ;

    companion object {
        fun fromValue(value: String): ThemeMode = values().find { it.value == value } ?: SYSTEM
    }
}

/**
 * Content layout options for browse screens
 */
enum class ContentLayout(val value: String) {
    GRID("grid"),
    LIST("list"),
    COMPACT_GRID("compact_grid"),
    ;

    companion object {
        fun fromValue(value: String): ContentLayout = values().find { it.value == value } ?: GRID
    }
}

/**
 * Video quality preferences with bitrate hints
 */
enum class VideoQualityPreference(
    val value: String,
    val displayName: String,
    val maxResolution: String,
    val bitrateHint: Int? = null,
) {
    AUTO("auto", "Auto", "Best Available"),
    UHD_4K("4k", "4K UHD", "2160p", 25000),
    FHD_1080P("1080p", "1080p HD", "1080p", 8000),
    HD_720P("720p", "720p HD", "720p", 4000),
    SD_480P("480p", "480p SD", "480p", 2000),
    DATA_SAVER("data_saver", "Data Saver", "360p", 1000),
    ;

    companion object {
        fun fromValue(value: String): VideoQualityPreference = values().find { it.value == value } ?: AUTO
    }
}

/**
 * Playback speed options
 */
enum class PlaybackSpeedPreference(val value: Float, val displayName: String) {
    SPEED_0_5X(0.5f, "0.5x"),
    SPEED_0_75X(0.75f, "0.75x"),
    SPEED_1X(1.0f, "Normal"),
    SPEED_1_25X(1.25f, "1.25x"),
    SPEED_1_5X(1.5f, "1.5x"),
    SPEED_1_75X(1.75f, "1.75x"),
    SPEED_2X(2.0f, "2x"),
    ;

    companion object {
        fun fromValue(value: Float): PlaybackSpeedPreference = values().find { it.value == value } ?: SPEED_1X
    }
}

/**
 * Bandwidth limit options
 */
enum class BandwidthLimit(val mbps: Int, val displayName: String) {
    UNLIMITED(0, "Unlimited"),
    LIMIT_100(100, "100 Mbps"),
    LIMIT_50(50, "50 Mbps"),
    LIMIT_25(25, "25 Mbps"),
    LIMIT_10(10, "10 Mbps"),
    LIMIT_5(5, "5 Mbps"),
    ;

    companion object {
        fun fromMbps(mbps: Int): BandwidthLimit = values().find { it.mbps == mbps } ?: UNLIMITED
    }
}

/**
 * Parental control rating limits
 */
enum class ParentalRating(val value: String, val displayName: String, val minAge: Int) {
    G("G", "G - General Audiences", 0),
    PG("PG", "PG - Parental Guidance", 8),
    PG13("PG-13", "PG-13 - Parents Strongly Cautioned", 13),
    R("R", "R - Restricted", 17),
    NC17("NC-17", "NC-17 - Adults Only", 18),
    UNRATED("UNRATED", "Unrated Content", 18),
    ;

    companion object {
        fun fromValue(value: String): ParentalRating = values().find { it.value == value } ?: UNRATED
    }
}

/**
 * Represents a complete settings backup
 */
data class SettingsBackup(
    val version: Int,
    val timestamp: Long,
    val deviceName: String?,
    val preferences: Map<String, Any>,
)

/**
 * Settings category for organization in UI
 */
enum class SettingsCategory(val displayName: String, val icon: String) {
    PLAYBACK("Playback", "play_arrow"),
    DISPLAY("Display", "display_settings"),
    NETWORK("Network", "wifi"),
    STORAGE("Storage", "storage"),
    SCRAPERS("Scrapers", "search"),
    ACCOUNT("Account", "account_circle"),
    PARENTAL("Parental Controls", "child_care"),
    NOTIFICATIONS("Notifications", "notifications"),
    ABOUT("About", "info"),
}

/**
 * External player info
 */
data class ExternalPlayer(
    val packageName: String,
    val displayName: String,
    val supportsStreaming: Boolean = true,
    val supportsLocalFiles: Boolean = true,
)

/**
 * Common external players for Android TV
 */
val EXTERNAL_PLAYERS =
    listOf(
        ExternalPlayer("com.mxtech.videoplayer.ad", "MX Player", true, true),
        ExternalPlayer("com.mxtech.videoplayer.pro", "MX Player Pro", true, true),
        ExternalPlayer("org.videolan.vlc", "VLC", true, true),
        ExternalPlayer("com.kodi.android", "Kodi", true, true),
        ExternalPlayer("com.semperpax.spmc", "SPMC", true, true),
        ExternalPlayer("", "Built-in Player", true, true),
    )
