package com.rdwatch.androidtv.data.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entity for tracking episode-specific watch progress
 * Extends the basic WatchProgressEntity to support season/episode tracking for TV shows
 */
@Entity(
    tableName = "episode_progress",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["tmdb_show_id"]),
        Index(value = ["season_number"]),
        Index(value = ["episode_number"]),
        Index(value = ["updated_at"]),
        Index(value = ["user_id", "tmdb_show_id", "season_number", "episode_number"], unique = true),
    ],
)
data class EpisodeProgressEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "progress_id")
    val progressId: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "tmdb_show_id")
    val tmdbShowId: Int,
    @ColumnInfo(name = "season_number")
    val seasonNumber: Int,
    @ColumnInfo(name = "episode_number")
    val episodeNumber: Int,
    @ColumnInfo(name = "episode_title")
    val episodeTitle: String? = null,
    @ColumnInfo(name = "progress_seconds")
    val progressSeconds: Long,
    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Long,
    @ColumnInfo(name = "watch_percentage")
    val watchPercentage: Float,
    @ColumnInfo(name = "is_completed")
    val isCompleted: Boolean = false,
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date,
    @ColumnInfo(name = "device_info")
    val deviceInfo: String? = null,
    @ColumnInfo(name = "video_source_url")
    val videoSourceUrl: String? = null,
) {
    /**
     * Get formatted episode identifier (S01E05)
     */
    fun getFormattedEpisodeId(): String {
        return "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
    }

    /**
     * Check if episode is substantially watched (>= 90%)
     */
    fun isSubstantiallyWatched(): Boolean = watchPercentage >= 0.9f

    /**
     * Check if episode has minimal progress (< 5%)
     */
    fun hasMinimalProgress(): Boolean = watchPercentage < 0.05f

    /**
     * Check if episode is partially watched (has progress but not completed)
     */
    fun isPartiallyWatched(): Boolean = watchPercentage > 0f && !isCompleted

    /**
     * Get human-readable progress description
     */
    fun getProgressDescription(): String {
        return when {
            isCompleted -> "Completed"
            watchPercentage >= 0.9f -> "Nearly finished (${(watchPercentage * 100).toInt()}%)"
            watchPercentage >= 0.5f -> "Half watched (${(watchPercentage * 100).toInt()}%)"
            watchPercentage > 0f -> "Started (${(watchPercentage * 100).toInt()}%)"
            else -> "Not started"
        }
    }
}

/**
 * Entity for tracking show-level watch progress and next episode suggestions
 */
@Entity(
    tableName = "show_progress",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["user_id"]),
        Index(value = ["tmdb_show_id"]),
        Index(value = ["updated_at"]),
        Index(value = ["next_episode_air_date"]),
        Index(value = ["user_id", "tmdb_show_id"], unique = true),
    ],
)
data class ShowProgressEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "progress_id")
    val progressId: Long = 0,
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "tmdb_show_id")
    val tmdbShowId: Int,
    @ColumnInfo(name = "show_title")
    val showTitle: String,
    @ColumnInfo(name = "next_season_number")
    val nextSeasonNumber: Int? = null,
    @ColumnInfo(name = "next_episode_number")
    val nextEpisodeNumber: Int? = null,
    @ColumnInfo(name = "next_episode_title")
    val nextEpisodeTitle: String? = null,
    @ColumnInfo(name = "next_episode_air_date")
    val nextEpisodeAirDate: Date? = null,
    @ColumnInfo(name = "last_watched_season")
    val lastWatchedSeason: Int? = null,
    @ColumnInfo(name = "last_watched_episode")
    val lastWatchedEpisode: Int? = null,
    @ColumnInfo(name = "total_episodes_watched")
    val totalEpisodesWatched: Int = 0,
    @ColumnInfo(name = "total_runtime_seconds")
    val totalRuntimeSeconds: Long = 0,
    @ColumnInfo(name = "show_completion_percentage")
    val showCompletionPercentage: Float = 0f,
    @ColumnInfo(name = "is_show_completed")
    val isShowCompleted: Boolean = false,
    @ColumnInfo(name = "watch_order_preference")
    val watchOrderPreference: WatchOrderType = WatchOrderType.CHRONOLOGICAL,
    @ColumnInfo(name = "skip_special_episodes")
    val skipSpecialEpisodes: Boolean = false,
    @ColumnInfo(name = "auto_play_enabled")
    val autoPlayEnabled: Boolean = true,
    @ColumnInfo(name = "created_at")
    val createdAt: Date,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date,
) {
    /**
     * Check if there's a next episode available
     */
    fun hasNextEpisode(): Boolean = nextSeasonNumber != null && nextEpisodeNumber != null

    /**
     * Get formatted next episode identifier
     */
    fun getNextEpisodeId(): String? {
        return if (hasNextEpisode()) {
            "S${nextSeasonNumber.toString().padStart(2, '0')}E${nextEpisodeNumber.toString().padStart(2, '0')}"
        } else {
            null
        }
    }

    /**
     * Get formatted last watched episode identifier
     */
    fun getLastWatchedEpisodeId(): String? {
        return if (lastWatchedSeason != null && lastWatchedEpisode != null) {
            "S${lastWatchedSeason.toString().padStart(2, '0')}E${lastWatchedEpisode.toString().padStart(2, '0')}"
        } else {
            null
        }
    }

    /**
     * Check if show is actively being watched (has progress)
     */
    fun isActivelyWatching(): Boolean = totalEpisodesWatched > 0

    /**
     * Get human-readable watch status
     */
    fun getWatchStatus(): String {
        return when {
            isShowCompleted -> "Completed"
            !isActivelyWatching() -> "Not started"
            hasNextEpisode() -> "In progress - Next: ${getNextEpisodeId()}"
            else -> "Caught up"
        }
    }
}

/**
 * Watch order preferences for different types of shows
 */
enum class WatchOrderType {
    CHRONOLOGICAL, // Follow season/episode order (default)
    RELEASE_DATE, // Follow air date order
    CUSTOM, // User-defined order (for anthologies)
    SKIP_SPECIALS, // Skip season 0 episodes
}

/**
 * Entity for auto-play countdown settings and state
 */
@Entity(
    tableName = "auto_play_settings",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [
        Index(value = ["user_id"], unique = true),
    ],
)
data class AutoPlaySettingsEntity(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: Long,
    @ColumnInfo(name = "enabled")
    val enabled: Boolean = true,
    @ColumnInfo(name = "countdown_seconds")
    val countdownSeconds: Int = 10,
    @ColumnInfo(name = "skip_intro_enabled")
    val skipIntroEnabled: Boolean = false,
    @ColumnInfo(name = "skip_outro_enabled")
    val skipOutroEnabled: Boolean = false,
    @ColumnInfo(name = "binge_mode_enabled")
    val bingeModeEnabled: Boolean = false,
    @ColumnInfo(name = "notification_enabled")
    val notificationEnabled: Boolean = true,
    @ColumnInfo(name = "auto_mark_watched_threshold")
    val autoMarkWatchedThreshold: Float = 0.9f,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Date = Date(),
) {
    /**
     * Check if auto-play is effectively enabled
     */
    fun isAutoPlayActive(): Boolean = enabled && countdownSeconds > 0

    /**
     * Get formatted countdown duration
     */
    fun getFormattedCountdown(): String = "${countdownSeconds}s"
}
