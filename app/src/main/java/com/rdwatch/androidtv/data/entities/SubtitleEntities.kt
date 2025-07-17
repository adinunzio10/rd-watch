package com.rdwatch.androidtv.data.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiProvider
import com.rdwatch.androidtv.player.subtitle.models.MatchType
import com.rdwatch.androidtv.player.subtitle.models.SubtitleFormat

/**
 * Entity for caching subtitle search results.
 * Reduces API calls and improves performance for repeated searches.
 */
@Entity(
    tableName = "subtitle_cache",
    indices = [
        Index(value = ["searchKey"], unique = true),
        Index(value = ["contentId"]),
        Index(value = ["timestamp"]),
    ],
)
data class SubtitleCacheEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // Search identification
    val searchKey: String, // Unique key for this search
    val contentId: String, // IMDB/TMDB ID or content hash
    val contentTitle: String,
    val contentYear: Int? = null,
    val season: Int? = null,
    val episode: Int? = null,
    // Cache metadata
    val timestamp: Long = System.currentTimeMillis(),
    val expiresAt: Long,
    val resultCount: Int,
    // Search parameters
    val languages: String, // Comma-separated language codes
    val hasFileHash: Boolean = false,
    val hasImdbId: Boolean = false,
    val hasTmdbId: Boolean = false,
) {
    fun isExpired(): Boolean = System.currentTimeMillis() > expiresAt
}

/**
 * Entity for individual subtitle search results.
 * Stores detailed information about each subtitle found.
 */
@Entity(
    tableName = "subtitle_results",
    indices = [
        Index(value = ["cacheId"]),
        Index(value = ["provider"]),
        Index(value = ["language"]),
        Index(value = ["matchScore"]),
        Index(value = ["downloadCount"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = SubtitleCacheEntity::class,
            parentColumns = ["id"],
            childColumns = ["cacheId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SubtitleResultEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // Foreign key to cache entry
    val cacheId: Long,
    // Provider information
    val providerId: String, // Provider-specific ID
    val provider: SubtitleApiProvider,
    val downloadUrl: String,
    // Subtitle metadata
    val language: String, // ISO 639-1 code
    val languageName: String,
    val format: SubtitleFormat,
    val fileName: String,
    val fileSize: Long? = null,
    // Quality indicators
    val downloadCount: Int? = null,
    val rating: Float? = null,
    val matchScore: Float = 0.0f,
    val matchType: MatchType,
    // Additional metadata
    val uploadDate: Long? = null,
    val uploader: String? = null,
    val isVerified: Boolean = false,
    val hearingImpaired: Boolean? = null,
    val releaseGroup: String? = null,
    val version: String? = null,
    val comments: String? = null,
    val contentHash: String? = null,
)

/**
 * Entity for cached subtitle files.
 * Tracks downloaded subtitle files to avoid re-downloading.
 */
@Entity(
    tableName = "subtitle_files",
    indices = [
        Index(value = ["resultId"], unique = true),
        Index(value = ["filePath"], unique = true),
        Index(value = ["contentId"]),
        Index(value = ["language"]),
        Index(value = ["downloadTimestamp"]),
    ],
    foreignKeys = [
        ForeignKey(
            entity = SubtitleResultEntity::class,
            parentColumns = ["id"],
            childColumns = ["resultId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
)
data class SubtitleFileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    // Foreign key to result
    val resultId: Long,
    // File information
    val filePath: String, // Local file path
    val originalFileName: String,
    val contentId: String, // For quick lookup
    val language: String,
    val format: SubtitleFormat,
    val fileSize: Long,
    // Download metadata
    val downloadTimestamp: Long = System.currentTimeMillis(),
    val provider: SubtitleApiProvider,
    val downloadUrl: String? = null,
    // File status
    val isActive: Boolean = true, // Can be disabled without deletion
    val checksum: String? = null, // For integrity verification
    val lastAccessTime: Long = System.currentTimeMillis(),
    val accessCount: Int = 0,
) {
    fun isExpired(maxAgeMs: Long): Boolean {
        return System.currentTimeMillis() - downloadTimestamp > maxAgeMs
    }

    fun updateAccess(): SubtitleFileEntity {
        return copy(
            lastAccessTime = System.currentTimeMillis(),
            accessCount = accessCount + 1,
        )
    }
}

/**
 * Entity for tracking subtitle API provider usage and status.
 * Used for rate limiting and health monitoring.
 */
@Entity(
    tableName = "subtitle_provider_stats",
    indices = [
        Index(value = ["provider"], unique = true),
        Index(value = ["lastUpdate"]),
    ],
)
data class SubtitleProviderStatsEntity(
    @PrimaryKey
    val provider: SubtitleApiProvider,
    // Usage statistics
    val totalRequests: Long = 0,
    val successfulRequests: Long = 0,
    val failedRequests: Long = 0,
    val lastRequestTime: Long? = null,
    // Rate limiting
    val requestsInCurrentWindow: Int = 0,
    val windowResetTime: Long = 0,
    // Health status
    val isEnabled: Boolean = true,
    val lastSuccessTime: Long? = null,
    val lastFailureTime: Long? = null,
    val lastFailureReason: String? = null,
    val consecutiveFailures: Int = 0,
    // Performance metrics
    val averageResponseTimeMs: Long = 0,
    val lastUpdate: Long = System.currentTimeMillis(),
) {
    fun getSuccessRate(): Float {
        return if (totalRequests > 0) {
            successfulRequests.toFloat() / totalRequests.toFloat()
        } else {
            0.0f
        }
    }

    fun isHealthy(): Boolean {
        return isEnabled && consecutiveFailures < 5 && getSuccessRate() > 0.5f
    }
}
