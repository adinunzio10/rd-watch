package com.rdwatch.androidtv.player.state

import com.rdwatch.androidtv.util.DebugLogger
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing parsed episode metadata
 */
data class EpisodeMetadata(
    val tmdbShowId: Int,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val episodeTitle: String? = null,
    val showTitle: String? = null,
)

/**
 * Service for detecting and parsing episode metadata from various content identifiers.
 * Handles multiple content ID formats used throughout the application.
 */
@Singleton
class EpisodeDetectionService
    @Inject
    constructor() {
        /**
         * Parse episode metadata from a content ID or URL
         *
         * Supported formats:
         * - "{tmdbShowId}:{seasonNumber}:{episodeNumber}" (from PlaybackViewModel)
         * - URLs containing show/season/episode information
         * - Other episode identification patterns
         */
        fun parseEpisodeMetadata(
            contentId: String,
            title: String? = null,
            mediaUrl: String? = null,
        ): EpisodeMetadata? {
            return try {
                // Try parsing the standard episode content ID format first
                parseStandardEpisodeFormat(contentId)
                    ?: parseFromTitle(title)
                    ?: parseFromUrl(mediaUrl ?: contentId)
            } catch (e: Exception) {
                DebugLogger.w("EpisodeDetectionService", "Failed to parse episode metadata from contentId: $contentId", e)
                null
            }
        }

        /**
         * Parse standard episode format: "{tmdbShowId}:{seasonNumber}:{episodeNumber}"
         */
        private fun parseStandardEpisodeFormat(contentId: String): EpisodeMetadata? {
            val parts = contentId.split(":")
            return if (parts.size == 3) {
                try {
                    val tmdbShowId = parts[0].toInt()
                    val seasonNumber = parts[1].toInt()
                    val episodeNumber = parts[2].toInt()

                    // Validate the parsed values
                    if (tmdbShowId > 0 && seasonNumber >= 0 && episodeNumber > 0) {
                        EpisodeMetadata(
                            tmdbShowId = tmdbShowId,
                            seasonNumber = seasonNumber,
                            episodeNumber = episodeNumber,
                        )
                    } else {
                        DebugLogger.w("EpisodeDetectionService", "Invalid episode values: show=$tmdbShowId, season=$seasonNumber, episode=$episodeNumber")
                        null
                    }
                } catch (e: NumberFormatException) {
                    DebugLogger.d("EpisodeDetectionService", "Standard format parse failed for: $contentId")
                    null
                }
            } else {
                null
            }
        }

        /**
         * Parse episode information from title string
         * Examples: "Show Name - S01E05: Episode Title", "S1E1", etc.
         */
        private fun parseFromTitle(title: String?): EpisodeMetadata? {
            if (title.isNullOrBlank()) return null

            return try {
                // Pattern for "S01E05" or "S1E1" format
                val seasonEpisodeRegex = Regex("""S(\d+)E(\d+)""", RegexOption.IGNORE_CASE)
                val match = seasonEpisodeRegex.find(title)

                if (match != null) {
                    val seasonNumber = match.groupValues[1].toInt()
                    val episodeNumber = match.groupValues[2].toInt()

                    // Extract show title if possible (text before " - S")
                    val showTitle = title.substringBefore(" - S").takeIf { it.isNotBlank() && it != title }

                    // Extract episode title if possible (text after ": ")
                    val episodeTitle = title.substringAfter(": ").takeIf { it.isNotBlank() && it != title }

                    // We don't have TMDb ID from title, so return partial metadata
                    // This would need to be enhanced with a show lookup service
                    DebugLogger.d("EpisodeDetectionService", "Parsed from title: S${seasonNumber}E$episodeNumber, but no TMDb ID available")
                    null
                } else {
                    null
                }
            } catch (e: Exception) {
                DebugLogger.w("EpisodeDetectionService", "Failed to parse episode from title: $title", e)
                null
            }
        }

        /**
         * Parse episode information from URL or file path
         */
        private fun parseFromUrl(url: String): EpisodeMetadata? {
            if (url.isBlank()) return null

            return try {
                // Look for season/episode patterns in URLs
                val patterns =
                    listOf(
                        Regex("""[/\s]S(\d+)E(\d+)[/\s\.]""", RegexOption.IGNORE_CASE),
                        Regex("""season[.\s-](\d+).*episode[.\s-](\d+)""", RegexOption.IGNORE_CASE),
                        Regex("""(\d+)x(\d+)"""), // Format like "1x05"
                    )

                for (pattern in patterns) {
                    val match = pattern.find(url)
                    if (match != null) {
                        val seasonNumber = match.groupValues[1].toInt()
                        val episodeNumber = match.groupValues[2].toInt()

                        DebugLogger.d("EpisodeDetectionService", "Parsed from URL: S${seasonNumber}E$episodeNumber, but no TMDb ID available")
                        // We don't have TMDb ID from URL, so return null for now
                        // This could be enhanced with a show matching service
                        return null
                    }
                }

                null
            } catch (e: Exception) {
                DebugLogger.w("EpisodeDetectionService", "Failed to parse episode from URL: $url", e)
                null
            }
        }

        /**
         * Check if a content ID likely represents an episode
         */
        fun isEpisodeContent(
            contentId: String,
            title: String? = null,
        ): Boolean {
            return parseEpisodeMetadata(contentId, title) != null
        }

        /**
         * Create a standard episode content ID from metadata
         */
        fun createEpisodeContentId(
            tmdbShowId: Int,
            seasonNumber: Int,
            episodeNumber: Int,
        ): String {
            return "$tmdbShowId:$seasonNumber:$episodeNumber"
        }

        /**
         * Extract show title from a full episode title
         * Example: "Breaking Bad - S01E01: Pilot" -> "Breaking Bad"
         */
        fun extractShowTitle(fullTitle: String): String? {
            return fullTitle.substringBefore(" - S").takeIf {
                it.isNotBlank() && it != fullTitle
            }
        }

        /**
         * Extract episode title from a full episode title
         * Example: "Breaking Bad - S01E01: Pilot" -> "Pilot"
         */
        fun extractEpisodeTitle(fullTitle: String): String? {
            return fullTitle.substringAfter(": ").takeIf {
                it.isNotBlank() && it != fullTitle
            }
        }

        /**
         * Format episode for display
         */
        fun formatEpisodeDisplay(
            seasonNumber: Int,
            episodeNumber: Int,
            episodeTitle: String? = null,
        ): String {
            val seasonEpisode = "S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"
            return if (episodeTitle != null) {
                "$seasonEpisode • $episodeTitle"
            } else {
                seasonEpisode
            }
        }
    }
