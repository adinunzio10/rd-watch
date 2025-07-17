package com.rdwatch.androidtv.ui.details.models.advanced

import java.util.regex.Pattern
import kotlin.math.roundToInt

/**
 * Advanced season pack detection and analysis system
 * Provides comprehensive season pack identification, completeness analysis, and metadata extraction
 */
class SeasonPackDetector {
    companion object {
        // Season pattern regex variants
        private val seasonPatterns =
            listOf(
                Pattern.compile("(?i)s(\\d{1,2})(?:[-\\s]?e(\\d{1,3})(?:[-\\s]?e?(\\d{1,3}))?)?", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?i)season[\\s._-]?(\\d{1,2})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?i)series[\\s._-]?(\\d{1,2})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?i)(?:complete[\\s._-]?)season[\\s._-]?(\\d{1,2})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?i)(?:the[\\s._-]?)complete[\\s._-]?(?:series|season)[\\s._-]?(\\d{1,2})?", Pattern.CASE_INSENSITIVE),
            )

        // Episode range patterns
        private val episodeRangePatterns =
            listOf(
                Pattern.compile("(?i)e(\\d{1,3})[-\\s]?(?:to|-)[-\\s]?e?(\\d{1,3})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?i)episodes?[\\s._-]?(\\d{1,3})[-\\s]?(?:to|-)[-\\s]?(\\d{1,3})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?i)(\\d{1,3})[-\\s]?(\\d{1,3})[\\s._-]?episodes?", Pattern.CASE_INSENSITIVE),
            )

        // Multi-season patterns
        private val multiSeasonPatterns =
            listOf(
                Pattern.compile("(?i)s(\\d{1,2})[-\\s]?(?:to|-)[-\\s]?s?(\\d{1,2})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?i)seasons?[\\s._-]?(\\d{1,2})[-\\s]?(?:to|-)[-\\s]?(\\d{1,2})", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?i)complete[\\s._-]?(?:series|collection)", Pattern.CASE_INSENSITIVE),
            )

        // Complete season indicators
        private val completeSeasonIndicators =
            listOf(
                "complete",
                "full",
                "entire",
                "whole",
                "all episodes",
                "season pack",
                "complete season",
                "full season",
            )

        // Individual episode patterns
        private val singleEpisodePatterns =
            listOf(
                Pattern.compile("(?i)s(\\d{1,2})e(\\d{1,3})(?![\\d-])", Pattern.CASE_INSENSITIVE),
                Pattern.compile("(?i)(\\d{1,2})x(\\d{1,3})(?![\\d-])", Pattern.CASE_INSENSITIVE),
            )

        // Quality pack indicators
        private val qualityPackIndicators =
            listOf(
                "remux pack",
                "bluray pack",
                "web-dl pack",
                "collection",
            )
    }

    /**
     * Analyze filename for season pack information
     */
    fun analyzeSeasonPack(
        filename: String,
        fileSize: Long? = null,
    ): SeasonPackInfo {
        val normalizedName = filename.lowercase()

        // Detect single episode first
        val singleEpisode = detectSingleEpisode(normalizedName)
        if (singleEpisode != null) {
            return SeasonPackInfo(
                isSeasonPack = false,
                isSingleEpisode = true,
                seasonNumbers = listOf(singleEpisode.season),
                episodeRange = EpisodeRange(singleEpisode.episode, singleEpisode.episode),
                totalEpisodes = 1,
                completenessPercentage = 100.0f,
                packType = SeasonPackType.SINGLE_EPISODE,
                confidence = 95.0f,
                metadata =
                    SeasonPackMetadata(
                        hasCompleteIndicator = false,
                        hasQualityPackIndicator = false,
                        estimatedEpisodeCount = 1,
                        averageEpisodeSizeMB = fileSize?.let { (it / (1024 * 1024)).toInt() },
                    ),
            )
        }

        // Detect multi-season pack
        val multiSeason = detectMultiSeason(normalizedName)
        if (multiSeason != null) {
            val seasonRange = multiSeason.first to multiSeason.second
            val estimatedEpisodes = estimateEpisodesForSeasonRange(seasonRange)

            return SeasonPackInfo(
                isSeasonPack = true,
                isMultiSeasonPack = true,
                seasonNumbers = (seasonRange.first..seasonRange.second).toList(),
                totalEpisodes = estimatedEpisodes,
                completenessPercentage = calculateMultiSeasonCompleteness(normalizedName, seasonRange),
                packType = SeasonPackType.MULTI_SEASON,
                confidence = calculateConfidence(normalizedName, hasSeasonIndicator = true, hasCompleteIndicator = true),
                metadata =
                    SeasonPackMetadata(
                        hasCompleteIndicator = hasCompleteIndicator(normalizedName),
                        hasQualityPackIndicator = hasQualityPackIndicator(normalizedName),
                        estimatedEpisodeCount = estimatedEpisodes,
                        seasonRange = seasonRange,
                        averageEpisodeSizeMB = fileSize?.let { (it / (1024 * 1024) / estimatedEpisodes).toInt() },
                    ),
            )
        }

        // Detect single season pack
        val seasonInfo = detectSeason(normalizedName)
        if (seasonInfo != null) {
            val episodeInfo = detectEpisodeRange(normalizedName)
            val isComplete = hasCompleteIndicator(normalizedName) || episodeInfo == null
            val estimatedEpisodes =
                episodeInfo?.let { it.end - it.start + 1 }
                    ?: estimateEpisodesForSeason(seasonInfo)

            return SeasonPackInfo(
                isSeasonPack = episodeInfo == null || (episodeInfo.end - episodeInfo.start) > 3, // More than 3 episodes
                isSingleEpisode = false,
                seasonNumbers = listOf(seasonInfo),
                episodeRange = episodeInfo,
                totalEpisodes = estimatedEpisodes,
                completenessPercentage = if (isComplete) 100.0f else calculateSeasonCompleteness(episodeInfo, seasonInfo),
                packType = if (isComplete) SeasonPackType.COMPLETE_SEASON else SeasonPackType.PARTIAL_SEASON,
                confidence = calculateConfidence(normalizedName, hasSeasonIndicator = true, hasCompleteIndicator = isComplete),
                metadata =
                    SeasonPackMetadata(
                        hasCompleteIndicator = isComplete,
                        hasQualityPackIndicator = hasQualityPackIndicator(normalizedName),
                        estimatedEpisodeCount = estimatedEpisodes,
                        averageEpisodeSizeMB = fileSize?.let { (it / (1024 * 1024) / estimatedEpisodes).toInt() },
                    ),
            )
        }

        // Check for complete series pack without explicit season numbers
        if (hasCompleteSeriesIndicator(normalizedName)) {
            val estimatedEpisodes = estimateEpisodesForCompleteSeries()

            return SeasonPackInfo(
                isSeasonPack = true,
                isCompleteSeriesPack = true,
                isMultiSeasonPack = true,
                totalEpisodes = estimatedEpisodes,
                completenessPercentage = 100.0f,
                packType = SeasonPackType.COMPLETE_SERIES,
                confidence = calculateConfidence(normalizedName, hasSeasonIndicator = false, hasCompleteIndicator = true),
                metadata =
                    SeasonPackMetadata(
                        hasCompleteIndicator = true,
                        hasQualityPackIndicator = hasQualityPackIndicator(normalizedName),
                        estimatedEpisodeCount = estimatedEpisodes,
                        averageEpisodeSizeMB = fileSize?.let { (it / (1024 * 1024) / estimatedEpisodes).toInt() },
                    ),
            )
        }

        // Default: assume single file (episode or movie)
        return SeasonPackInfo(
            isSeasonPack = false,
            isSingleEpisode = false,
            totalEpisodes = 1,
            completenessPercentage = 100.0f,
            packType = SeasonPackType.UNKNOWN,
            confidence = 10.0f,
            metadata =
                SeasonPackMetadata(
                    hasCompleteIndicator = false,
                    hasQualityPackIndicator = hasQualityPackIndicator(normalizedName),
                    estimatedEpisodeCount = 1,
                    averageEpisodeSizeMB = fileSize?.let { (it / (1024 * 1024)).toInt() },
                ),
        )
    }

    /**
     * Detect single episode
     */
    private fun detectSingleEpisode(filename: String): EpisodeInfo? {
        for (pattern in singleEpisodePatterns) {
            val matcher = pattern.matcher(filename)
            if (matcher.find()) {
                val season = matcher.group(1)?.toIntOrNull() ?: continue
                val episode = matcher.group(2)?.toIntOrNull() ?: continue
                return EpisodeInfo(season, episode)
            }
        }
        return null
    }

    /**
     * Detect season number
     */
    private fun detectSeason(filename: String): Int? {
        for (pattern in seasonPatterns) {
            val matcher = pattern.matcher(filename)
            if (matcher.find()) {
                return matcher.group(1)?.toIntOrNull()
            }
        }
        return null
    }

    /**
     * Detect multi-season pack
     */
    private fun detectMultiSeason(filename: String): Pair<Int, Int>? {
        for (pattern in multiSeasonPatterns) {
            val matcher = pattern.matcher(filename)
            if (matcher.find()) {
                val start = matcher.group(1)?.toIntOrNull()
                val end = matcher.group(2)?.toIntOrNull()
                if (start != null && end != null && end > start) {
                    return start to end
                }
            }
        }
        return null
    }

    /**
     * Detect episode range
     */
    private fun detectEpisodeRange(filename: String): EpisodeRange? {
        for (pattern in episodeRangePatterns) {
            val matcher = pattern.matcher(filename)
            if (matcher.find()) {
                val start = matcher.group(1)?.toIntOrNull()
                val end = matcher.group(2)?.toIntOrNull()
                if (start != null && end != null && end >= start) {
                    return EpisodeRange(start, end)
                }
            }
        }
        return null
    }

    /**
     * Check for complete season indicator
     */
    private fun hasCompleteIndicator(filename: String): Boolean {
        return completeSeasonIndicators.any { indicator ->
            filename.contains(indicator, ignoreCase = true)
        }
    }

    /**
     * Check for complete series indicator
     */
    private fun hasCompleteSeriesIndicator(filename: String): Boolean {
        val indicators = listOf("complete series", "complete collection", "complete", "entire series")
        return indicators.any { indicator ->
            filename.contains(indicator, ignoreCase = true)
        }
    }

    /**
     * Check for quality pack indicator
     */
    private fun hasQualityPackIndicator(filename: String): Boolean {
        return qualityPackIndicators.any { indicator ->
            filename.contains(indicator, ignoreCase = true)
        }
    }

    /**
     * Calculate confidence score
     */
    private fun calculateConfidence(
        filename: String,
        hasSeasonIndicator: Boolean,
        hasCompleteIndicator: Boolean,
    ): Float {
        var confidence = 0.0f

        if (hasSeasonIndicator) confidence += 40.0f
        if (hasCompleteIndicator) confidence += 30.0f
        if (hasQualityPackIndicator(filename)) confidence += 15.0f

        // Boost confidence for explicit patterns
        if (filename.contains("season", ignoreCase = true)) confidence += 10.0f
        if (filename.contains("complete", ignoreCase = true)) confidence += 10.0f
        if (filename.contains("pack", ignoreCase = true)) confidence += 5.0f

        return confidence.coerceIn(0.0f, 100.0f)
    }

    /**
     * Estimate episodes for a season
     */
    private fun estimateEpisodesForSeason(seasonNumber: Int): Int {
        // Most TV seasons have between 6-24 episodes
        // Earlier seasons tend to have more episodes
        return when {
            seasonNumber == 1 -> 22 // Typical first season
            seasonNumber <= 5 -> 20 // Early seasons
            seasonNumber <= 10 -> 16 // Mid seasons
            else -> 12 // Later seasons often shorter
        }
    }

    /**
     * Estimate episodes for season range
     */
    private fun estimateEpisodesForSeasonRange(seasonRange: Pair<Int, Int>): Int {
        val totalSeasons = seasonRange.second - seasonRange.first + 1
        return (seasonRange.first..seasonRange.second).sumOf { estimateEpisodesForSeason(it) }
    }

    /**
     * Estimate episodes for complete series
     */
    private fun estimateEpisodesForCompleteSeries(): Int {
        // Average TV series has 5-7 seasons with ~18 episodes each
        return 100 // Conservative estimate
    }

    /**
     * Calculate season completeness percentage
     */
    private fun calculateSeasonCompleteness(
        episodeRange: EpisodeRange?,
        seasonNumber: Int,
    ): Float {
        if (episodeRange == null) return 50.0f // Unknown, assume partial

        val estimatedTotal = estimateEpisodesForSeason(seasonNumber)
        val actualEpisodes = episodeRange.end - episodeRange.start + 1

        return (actualEpisodes.toFloat() / estimatedTotal * 100).coerceIn(0.0f, 100.0f)
    }

    /**
     * Calculate multi-season completeness
     */
    private fun calculateMultiSeasonCompleteness(
        filename: String,
        seasonRange: Pair<Int, Int>,
    ): Float {
        // If it says "complete" assume it's complete
        if (hasCompleteIndicator(filename)) return 100.0f

        // Otherwise estimate based on typical season lengths
        return 85.0f // Assume mostly complete but may be missing some episodes
    }

    /**
     * Get season pack quality score for sorting
     */
    fun getSeasonPackQualityScore(seasonPackInfo: SeasonPackInfo): Int {
        var score = 0

        // Pack type bonus
        score +=
            when (seasonPackInfo.packType) {
                SeasonPackType.COMPLETE_SERIES -> 100
                SeasonPackType.COMPLETE_SEASON -> 80
                SeasonPackType.MULTI_SEASON -> 90
                SeasonPackType.PARTIAL_SEASON -> 60
                SeasonPackType.SINGLE_EPISODE -> 40
                SeasonPackType.UNKNOWN -> 20
            }

        // Completeness bonus
        score += (seasonPackInfo.completenessPercentage * 0.5f).roundToInt()

        // Confidence penalty for uncertain detection
        if (seasonPackInfo.confidence < 50.0f) {
            score -= 20
        }

        // Total episodes bonus (more content is better)
        score +=
            when {
                seasonPackInfo.totalEpisodes >= 100 -> 30
                seasonPackInfo.totalEpisodes >= 50 -> 25
                seasonPackInfo.totalEpisodes >= 20 -> 20
                seasonPackInfo.totalEpisodes >= 10 -> 15
                seasonPackInfo.totalEpisodes >= 5 -> 10
                else -> 0
            }

        return score.coerceIn(0, 300)
    }
}

/**
 * Complete season pack information
 */
data class SeasonPackInfo(
    val isSeasonPack: Boolean = false,
    val isSingleEpisode: Boolean = false,
    val isMultiSeasonPack: Boolean = false,
    val isCompleteSeriesPack: Boolean = false,
    val seasonNumbers: List<Int> = emptyList(),
    val episodeRange: EpisodeRange? = null,
    val totalEpisodes: Int = 0,
    val completenessPercentage: Float = 0.0f, // 0-100%
    val packType: SeasonPackType = SeasonPackType.UNKNOWN,
    val confidence: Float = 0.0f, // 0-100% confidence in detection
    val metadata: SeasonPackMetadata = SeasonPackMetadata(),
) {
    /**
     * Get display text for season pack
     */
    fun getDisplayText(): String {
        return when {
            isCompleteSeriesPack -> "Complete Series"
            isMultiSeasonPack && seasonNumbers.isNotEmpty() ->
                "Seasons ${seasonNumbers.first()}-${seasonNumbers.last()}"
            isSeasonPack && seasonNumbers.isNotEmpty() -> {
                val seasonText = "Season ${seasonNumbers.first()}"
                if (completenessPercentage < 100.0f) {
                    "$seasonText (${completenessPercentage.roundToInt()}%)"
                } else {
                    seasonText
                }
            }
            isSingleEpisode && seasonNumbers.isNotEmpty() && episodeRange != null ->
                "S${seasonNumbers.first()}E${episodeRange.start}"
            totalEpisodes > 1 -> "$totalEpisodes Episodes"
            else -> "Single File"
        }
    }

    /**
     * Get season pack badge
     */
    fun getSeasonPackBadge(): QualityBadge? {
        if (!isSeasonPack && !isMultiSeasonPack && !isCompleteSeriesPack) return null

        val text =
            when {
                isCompleteSeriesPack -> "Complete Series"
                isMultiSeasonPack -> "Multi-Season"
                completenessPercentage >= 100.0f -> "Complete Season"
                completenessPercentage >= 80.0f -> "Most Episodes"
                else -> "Partial Season"
            }

        return QualityBadge(
            text = text,
            type = QualityBadge.Type.FEATURE,
            priority = 55,
        )
    }
}

/**
 * Season pack type classification
 */
enum class SeasonPackType {
    COMPLETE_SERIES, // All seasons/episodes of a show
    COMPLETE_SEASON, // All episodes of a specific season
    MULTI_SEASON, // Multiple seasons (may not be complete)
    PARTIAL_SEASON, // Some episodes from a season
    SINGLE_EPISODE, // Individual episode
    UNKNOWN, // Cannot determine
}

/**
 * Episode range information
 */
data class EpisodeRange(
    val start: Int,
    val end: Int,
) {
    val count: Int get() = end - start + 1
}

/**
 * Episode information
 */
private data class EpisodeInfo(
    val season: Int,
    val episode: Int,
)

/**
 * Season pack metadata
 */
data class SeasonPackMetadata(
    val hasCompleteIndicator: Boolean = false,
    val hasQualityPackIndicator: Boolean = false,
    val estimatedEpisodeCount: Int = 0,
    val seasonRange: Pair<Int, Int>? = null,
    val averageEpisodeSizeMB: Int? = null,
    val detectedPatterns: List<String> = emptyList(),
)
