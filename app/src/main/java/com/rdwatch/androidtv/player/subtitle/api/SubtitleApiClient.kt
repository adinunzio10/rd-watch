package com.rdwatch.androidtv.player.subtitle.api

import com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchRequest
import com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult

/**
 * Abstract interface for all external subtitle API clients.
 * Ensures consistent behavior across different subtitle providers.
 */
interface SubtitleApiClient {
    /**
     * The subtitle provider this client handles.
     */
    fun getProvider(): SubtitleApiProvider

    /**
     * Whether this client is currently enabled and available.
     */
    fun isEnabled(): Boolean

    /**
     * Search for subtitles using this provider.
     *
     * @param request The search request with content metadata
     * @return List of matching subtitle results
     * @throws SubtitleApiException on API errors
     */
    suspend fun searchSubtitles(request: SubtitleSearchRequest): List<SubtitleSearchResult>

    /**
     * Download a specific subtitle file.
     *
     * @param result The subtitle result to download
     * @return Local file path of the downloaded subtitle
     * @throws SubtitleApiException on download errors
     */
    suspend fun downloadSubtitle(result: SubtitleSearchResult): String

    /**
     * Test if the API is currently accessible.
     * Used for health checks and provider status monitoring.
     *
     * @return true if API is responding correctly
     */
    suspend fun testConnection(): Boolean
}

/**
 * Enumeration of supported subtitle API providers.
 * Based on PRD requirements: Subdl, SubDB, Podnapisi, Addic7ed alternatives.
 */
enum class SubtitleApiProvider(
    val displayName: String,
    val baseUrl: String,
    val requiresAuth: Boolean = false,
) {
    SUBDL("Subdl", "https://api.subdl.com", false),
    SUBDB("SubDB", "http://api.thesubdb.com", false),
    PODNAPISI("Podnapisi", "https://www.podnapisi.net", false),
    ADDIC7ED_ALT("Addic7ed Alternative", "https://addic7ed-api.example.com", false),
    LOCAL_FILES("Local Files", "", false), // For manually added subtitle files
    ;

    companion object {
        /**
         * Get providers that support hash-based subtitle matching.
         * These provide the most accurate results.
         */
        fun getHashBasedProviders(): List<SubtitleApiProvider> {
            return listOf(SUBDB, SUBDL)
        }

        /**
         * Get providers that support text-based search.
         * Used as fallback when hash matching fails.
         */
        fun getTextBasedProviders(): List<SubtitleApiProvider> {
            return listOf(SUBDL, PODNAPISI, ADDIC7ED_ALT)
        }
    }
}

/**
 * Exception thrown by subtitle API clients.
 */
class SubtitleApiException(
    val provider: SubtitleApiProvider,
    message: String,
    cause: Throwable? = null,
) : Exception("${provider.displayName}: $message", cause)

/**
 * Rate limiting status for API providers.
 */
data class RateLimitStatus(
    val requestsRemaining: Int,
    val resetTimeMs: Long,
    val isLimited: Boolean,
)
