package com.rdwatch.androidtv.player.subtitle.ranking

import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiProvider
import com.rdwatch.androidtv.player.subtitle.models.SubtitleFormat
import com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchRequest
import com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ln
import kotlin.math.min

/**
 * Ranks and prioritizes subtitle search results using a multi-factor scoring algorithm.
 *
 * The ranking system considers:
 * - Match accuracy (hash, IMDB/TMDB ID, title matching)
 * - Provider reliability and performance
 * - Content quality indicators (download count, rating, verification)
 * - User preferences (language, format, hearing impaired)
 * - Technical factors (file size, release group reputation)
 *
 * This ensures users get the most relevant and high-quality subtitles first.
 */
@Singleton
class SubtitleResultRanker
    @Inject
    constructor() {
        companion object {
            // Scoring weights (must sum to 1.0)
            private const val MATCH_ACCURACY_WEIGHT = 0.35f
            private const val PROVIDER_RELIABILITY_WEIGHT = 0.15f
            private const val CONTENT_QUALITY_WEIGHT = 0.25f
            private const val USER_PREFERENCE_WEIGHT = 0.15f
            private const val TECHNICAL_FACTORS_WEIGHT = 0.10f

            // Provider reliability scores (0.0 to 1.0)
            private val PROVIDER_RELIABILITY =
                mapOf(
                    SubtitleApiProvider.SUBDB to 0.95f, // Hash-based, very reliable
                    SubtitleApiProvider.SUBDL to 0.85f, // Good API, reliable results
                    SubtitleApiProvider.PODNAPISI to 0.75f, // Decent quality, European focus
                    SubtitleApiProvider.ADDIC7ED_ALT to 0.70f, // TV-focused, can be inconsistent
                    SubtitleApiProvider.LOCAL_FILES to 1.0f, // Perfect reliability for local files
                )

            // Known high-quality release groups (incomplete list)
            private val QUALITY_RELEASE_GROUPS =
                setOf(
                    "RARBG", "YTS", "ETRG", "FGT", "ION10", "SPARKS", "FLEET", "CMRG",
                    "Scene", "SVA", "DIMENSION", "KILLERS", "AMIABLE", "BLOW", "CasStudio",
                )
        }

        /**
         * Rank and sort subtitle results by relevance and quality.
         *
         * @param results The list of subtitle results to rank
         * @param request The original search request for context
         * @return Sorted list with most relevant results first
         */
        fun rankResults(
            results: List<SubtitleSearchResult>,
            request: SubtitleSearchRequest,
        ): List<SubtitleSearchResult> {
            if (results.isEmpty()) return emptyList()

            // Calculate scores for all results
            val scoredResults =
                results.map { result ->
                    val score = calculateOverallScore(result, request)
                    result.copy(matchScore = score)
                }

            // Sort by score (highest first) and apply secondary sorting
            return scoredResults.sortedWith(
                compareByDescending<SubtitleSearchResult> { it.matchScore }
                    .thenByDescending { it.downloadCount ?: 0 }
                    .thenByDescending { it.rating ?: 0f }
                    .thenBy { it.languageName }
                    .thenBy { it.fileName },
            )
        }

        /**
         * Calculate overall score for a subtitle result.
         *
         * @param result The subtitle result to score
         * @param request The original search request
         * @return Score between 0.0 and 1.0
         */
        private fun calculateOverallScore(
            result: SubtitleSearchResult,
            request: SubtitleSearchRequest,
        ): Float {
            val matchAccuracy = calculateMatchAccuracyScore(result, request)
            val providerReliability = calculateProviderReliabilityScore(result)
            val contentQuality = calculateContentQualityScore(result)
            val userPreference = calculateUserPreferenceScore(result, request)
            val technicalFactors = calculateTechnicalFactorsScore(result, request)

            return (
                matchAccuracy * MATCH_ACCURACY_WEIGHT +
                    providerReliability * PROVIDER_RELIABILITY_WEIGHT +
                    contentQuality * CONTENT_QUALITY_WEIGHT +
                    userPreference * USER_PREFERENCE_WEIGHT +
                    technicalFactors * TECHNICAL_FACTORS_WEIGHT
            ).coerceIn(0f, 1f)
        }

        /**
         * Score based on how well the result matches the search criteria.
         */
        private fun calculateMatchAccuracyScore(
            result: SubtitleSearchResult,
            request: SubtitleSearchRequest,
        ): Float {
            var score = result.matchType.confidence

            // Bonus for exact language match
            if (result.language in request.languages) {
                score += 0.1f
            }

            // Bonus for preferred format
            if (result.format in request.preferredFormats) {
                score += 0.05f
            }

            // Bonus for TV episode matching (season/episode)
            if (request.season != null && request.episode != null) {
                val titleLower = result.fileName.lowercase()
                val seasonEpisode = "s%02de%02d".format(request.season, request.episode)
                val altSeasonEpisode = "${request.season}x%02d".format(request.episode)

                if (titleLower.contains(seasonEpisode) || titleLower.contains(altSeasonEpisode)) {
                    score += 0.1f
                }
            }

            // Bonus for year matching in movies
            if (request.year != null && result.fileName.contains(request.year.toString())) {
                score += 0.05f
            }

            return score.coerceAtMost(1f)
        }

        /**
         * Score based on provider reliability and performance.
         */
        private fun calculateProviderReliabilityScore(result: SubtitleSearchResult): Float {
            return PROVIDER_RELIABILITY[result.provider] ?: 0.5f
        }

        /**
         * Score based on content quality indicators.
         */
        private fun calculateContentQualityScore(result: SubtitleSearchResult): Float {
            var score = 0f

            // Download count scoring (logarithmic scale)
            result.downloadCount?.let { downloads ->
                if (downloads > 0) {
                    // Normalize to 0-1 scale using logarithmic function
                    score += min(ln(downloads.toFloat() + 1) / ln(10000f), 1f) * 0.4f
                }
            }

            // Rating scoring
            result.rating?.let { rating ->
                score += (rating / 5f) * 0.3f
            }

            // Verification bonus
            if (result.isVerified) {
                score += 0.2f
            }

            // Release group quality bonus
            result.releaseGroup?.let { group ->
                if (QUALITY_RELEASE_GROUPS.contains(group.uppercase())) {
                    score += 0.1f
                }
            }

            return score.coerceAtMost(1f)
        }

        /**
         * Score based on user preferences and accessibility needs.
         */
        private fun calculateUserPreferenceScore(
            result: SubtitleSearchResult,
            request: SubtitleSearchRequest,
        ): Float {
            var score = 0f

            // Language preference scoring
            val languageIndex = request.languages.indexOf(result.language)
            if (languageIndex >= 0) {
                // Higher score for languages earlier in preference list
                score += (request.languages.size - languageIndex).toFloat() / request.languages.size * 0.5f
            }

            // Format preference scoring
            val formatIndex = request.preferredFormats.indexOf(result.format)
            if (formatIndex >= 0) {
                score += (request.preferredFormats.size - formatIndex).toFloat() / request.preferredFormats.size * 0.3f
            }

            // Hearing impaired preference
            request.preferredFormats.firstOrNull()?.let { _ ->
                // If user has specific hearing impaired preference, match it
                // For now, we don't penalize either way since preference isn't specified
                score += 0.2f
            }

            return score.coerceAtMost(1f)
        }

        /**
         * Score based on technical factors like file size and format compatibility.
         */
        private fun calculateTechnicalFactorsScore(
            result: SubtitleSearchResult,
            request: SubtitleSearchRequest,
        ): Float {
            var score = 0.5f // Base score

            // Format compatibility with ExoPlayer
            if (result.format in SubtitleFormat.getExoPlayerSupported()) {
                score += 0.3f
            }

            // File size reasonableness (prefer smaller files, but not too small)
            result.fileSize?.let { size ->
                when {
                    size < 10_000 -> score -= 0.2f // Too small, probably incomplete
                    size in 10_000..500_000 -> score += 0.2f // Good size range
                    size > 2_000_000 -> score -= 0.1f // Very large, might be bloated
                }
            }

            // Prefer newer uploads (within last 30 days get bonus)
            result.uploadDate?.let { uploadTime ->
                val thirtyDaysAgo = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L)
                if (uploadTime > thirtyDaysAgo) {
                    score += 0.1f
                }
            }

            // Check for common quality indicators in filename
            val fileName = result.fileName.lowercase()
            when {
                fileName.contains("sync") || fileName.contains("perfect sync") -> score += 0.1f
                fileName.contains("fixed") || fileName.contains("corrected") -> score += 0.05f
                fileName.contains("auto") && fileName.contains("generated") -> score -= 0.1f
            }

            return score.coerceIn(0f, 1f)
        }

        /**
         * Get ranking explanation for debugging and user information.
         *
         * @param result The subtitle result
         * @param request The search request
         * @return Detailed scoring breakdown
         */
        fun getScoreBreakdown(
            result: SubtitleSearchResult,
            request: SubtitleSearchRequest,
        ): ScoreBreakdown {
            return ScoreBreakdown(
                overallScore = calculateOverallScore(result, request),
                matchAccuracy = calculateMatchAccuracyScore(result, request),
                providerReliability = calculateProviderReliabilityScore(result),
                contentQuality = calculateContentQualityScore(result),
                userPreference = calculateUserPreferenceScore(result, request),
                technicalFactors = calculateTechnicalFactorsScore(result, request),
                factors =
                    buildList {
                        add("Match type: ${result.matchType}")
                        add("Provider: ${result.provider.displayName}")
                        result.downloadCount?.let { add("Downloads: $it") }
                        result.rating?.let { add("Rating: $it/5") }
                        if (result.isVerified) add("Verified")
                        result.releaseGroup?.let { add("Release: $it") }
                    },
            )
        }
    }

/**
 * Detailed breakdown of how a subtitle result was scored.
 * Useful for debugging and explaining rankings to users.
 */
data class ScoreBreakdown(
    val overallScore: Float,
    val matchAccuracy: Float,
    val providerReliability: Float,
    val contentQuality: Float,
    val userPreference: Float,
    val technicalFactors: Float,
    val factors: List<String>,
) {
    fun getTopFactors(): List<String> {
        val scores =
            listOf(
                "Match Accuracy" to matchAccuracy,
                "Provider Reliability" to providerReliability,
                "Content Quality" to contentQuality,
                "User Preference" to userPreference,
                "Technical Factors" to technicalFactors,
            )

        return scores
            .sortedByDescending { it.second }
            .take(3)
            .map { "${it.first}: ${(it.second * 100).toInt()}%" }
    }
}
