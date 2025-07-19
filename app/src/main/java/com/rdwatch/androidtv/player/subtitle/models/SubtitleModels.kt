package com.rdwatch.androidtv.player.subtitle.models

import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiProvider

/**
 * Request object for subtitle searches.
 * Contains all necessary metadata for accurate subtitle matching.
 */
data class SubtitleSearchRequest(
    // Primary identifiers (most accurate)
    val imdbId: String? = null,
    val tmdbId: String? = null,
    val fileHash: String? = null,
    // Content metadata
    val title: String,
    val year: Int? = null,
    val type: ContentType,
    // TV-specific metadata
    val season: Int? = null,
    val episode: Int? = null,
    val episodeTitle: String? = null,
    // Preferences
    val languages: List<String> = listOf("en"), // ISO 639-1 codes
    val preferredFormats: List<SubtitleFormat> = listOf(SubtitleFormat.SRT, SubtitleFormat.VTT),
    // Technical metadata
    val fileSize: Long? = null,
    val fileName: String? = null,
    val duration: Long? = null, // in milliseconds
) {
    /**
     * Create a cache key for this search request.
     * Used for caching search results.
     */
    fun getCacheKey(): String {
        val primaryId = imdbId ?: tmdbId ?: fileHash ?: "${title}_$year"
        val episodeInfo = if (type == ContentType.TV_EPISODE) "_s${season}e$episode" else ""
        val languageInfo = languages.sorted().joinToString(",")
        return "${primaryId}${episodeInfo}_$languageInfo".replace("[^a-zA-Z0-9_,-]".toRegex(), "_")
    }

    /**
     * Check if this request has reliable identifiers for accurate matching.
     */
    fun hasReliableIdentifiers(): Boolean {
        return imdbId != null || tmdbId != null || fileHash != null
    }
}

/**
 * Result from a subtitle search operation.
 */
data class SubtitleSearchResult(
    val id: String, // Provider-specific identifier
    val provider: SubtitleApiProvider,
    val language: String, // ISO 639-1 code
    val languageName: String, // Human-readable language name
    val format: SubtitleFormat,
    val downloadUrl: String,
    // Metadata
    val fileName: String,
    val fileSize: Long? = null,
    val downloadCount: Int? = null,
    val rating: Float? = null, // 0.0 to 5.0
    val uploadDate: Long? = null, // timestamp
    val uploader: String? = null,
    // Match confidence
    val matchScore: Float = 0.0f, // 0.0 to 1.0, calculated by ranking system
    val matchType: MatchType,
    // Content verification
    val contentHash: String? = null,
    val isVerified: Boolean = false,
    val hearingImpaired: Boolean? = null,
    // Additional metadata
    val releaseGroup: String? = null,
    val version: String? = null,
    val comments: String? = null,
) {
    /**
     * Create a unique identifier for caching this result.
     */
    fun getCacheId(): String {
        return "${provider.name}_${id}_${language}_${format.extension}"
    }

    /**
     * Check if this result is suitable for the given request.
     */
    fun isCompatibleWith(request: SubtitleSearchRequest): Boolean {
        return language in request.languages && format in request.preferredFormats
    }
}

/**
 * Type of content being searched for subtitles.
 */
enum class ContentType {
    MOVIE,
    TV_EPISODE,
    UNKNOWN,
}

/**
 * Supported subtitle formats.
 */
enum class SubtitleFormat(
    val extension: String,
    val mimeType: String,
    val description: String,
) {
    SRT("srt", "text/srt", "SubRip"),
    VTT("vtt", "text/vtt", "WebVTT"),
    ASS("ass", "text/ass", "Advanced SSA"),
    SSA("ssa", "text/ssa", "SubStation Alpha"),
    SUB("sub", "text/sub", "MicroDVD"),
    ;

    companion object {
        /**
         * Get format by file extension.
         */
        fun fromExtension(extension: String): SubtitleFormat? {
            return values().find { it.extension.equals(extension, ignoreCase = true) }
        }

        /**
         * Get formats supported by ExoPlayer.
         */
        fun getExoPlayerSupported(): List<SubtitleFormat> {
            return listOf(SRT, VTT, ASS, SSA)
        }
    }
}

/**
 * Type of match between search request and result.
 * Used for ranking and confidence scoring.
 */
enum class MatchType(val confidence: Float) {
    HASH_MATCH(1.0f), // Perfect file hash match
    IMDB_MATCH(0.9f), // IMDB ID match
    TMDB_MATCH(0.9f), // TMDB ID match
    TITLE_YEAR_MATCH(0.7f), // Title and year match
    TITLE_MATCH(0.5f), // Title-only match
    FUZZY_MATCH(0.3f), // Fuzzy text matching
    MANUAL_MATCH(0.0f), // Manually selected by user
}

/**
 * Status of subtitle file download and processing.
 */
data class SubtitleFileInfo(
    val filePath: String,
    val format: SubtitleFormat,
    val language: String,
    val fileSize: Long,
    val isEmbedded: Boolean = false,
    val isExternal: Boolean = true,
    val cacheTimestamp: Long = System.currentTimeMillis(),
    val source: SubtitleApiProvider? = null,
    val originalResult: SubtitleSearchResult? = null,
) {
    /**
     * Check if this cached file is still valid.
     */
    fun isExpired(maxAgeMs: Long = 24 * 60 * 60 * 1000): Boolean { // 24 hours default
        return System.currentTimeMillis() - cacheTimestamp > maxAgeMs
    }
}

/**
 * Configuration for subtitle search and download behavior.
 */
data class SubtitleSearchConfig(
    val enableHashMatching: Boolean = true,
    val enableFuzzyMatching: Boolean = true,
    val maxResults: Int = 20,
    val timeoutMs: Long = 10000,
    val retryAttempts: Int = 2,
    val cacheExpirationHours: Int = 24,
    val preferredProviders: List<SubtitleApiProvider> = emptyList(),
    val excludedProviders: List<SubtitleApiProvider> = emptyList(),
    val autoDownloadBest: Boolean = false,
    val hearingImpairedPreference: Boolean? = null, // null = no preference
)
