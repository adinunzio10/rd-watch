package com.rdwatch.androidtv.autoplay

import com.rdwatch.androidtv.data.repository.NextEpisodeResult
import com.rdwatch.androidtv.data.repository.TMDbTVRepository
import com.rdwatch.androidtv.ui.details.models.ContentAction
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import com.rdwatch.androidtv.ui.details.models.ContentMetadata
import com.rdwatch.androidtv.ui.details.models.ContentType
import com.rdwatch.androidtv.ui.details.models.StreamingSource
import com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata
import com.rdwatch.androidtv.ui.details.repository.SourceAggregationRepository
import com.rdwatch.androidtv.util.DebugLogger
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for handling auto-play episode resolution and source fetching
 * Integrates with the existing source aggregation system to resolve episode URLs
 */
@Singleton
class EpisodeAutoPlayService
    @Inject
    constructor(
        private val tmdbTVRepository: TMDbTVRepository,
        private val sourceAggregationRepository: SourceAggregationRepository,
    ) {
        /**
         * Resolve sources for the next episode in auto-play sequence
         * @param nextEpisode The episode to resolve sources for
         * @param showTitle The show title for logging
         * @return List of available sources or empty list if none found
         */
        suspend fun resolveEpisodeSources(
            nextEpisode: NextEpisodeResult,
            showTitle: String,
        ): List<SourceMetadata> {
            DebugLogger.d(
                "EpisodeAutoPlayService",
                "Auto-play source resolution for $showTitle S${nextEpisode.seasonNumber}E${nextEpisode.episodeNumber}",
            )

            return try {
                // Create ContentDetail for the episode
                val contentDetail = createEpisodeContentDetail(nextEpisode, showTitle)

                // Get sources from aggregation repository
                val sources =
                    sourceAggregationRepository.getSources(
                        contentDetail = contentDetail,
                        forceRefresh = false, // Use cached sources for faster auto-play
                    ).firstOrNull() ?: emptyList()

                DebugLogger.i(
                    "EpisodeAutoPlayService",
                    "Found ${sources.size} sources for episode ${nextEpisode.getDisplayText()}",
                )

                sources
            } catch (e: Exception) {
                DebugLogger.e(
                    "EpisodeAutoPlayService",
                    "Error resolving sources for episode ${nextEpisode.getDisplayText()}",
                    e,
                )
                emptyList()
            }
        }

        /**
         * Get the best available source for auto-play
         * Prioritizes cached/debrid sources for faster playback
         */
        suspend fun getBestSourceForAutoPlay(sources: List<SourceMetadata>): SourceMetadata? {
            if (sources.isEmpty()) return null

            return try {
                // Priority order for auto-play:
                // 1. Cached debrid sources (fastest)
                // 2. High quality sources with good health
                // 3. Any available source

                val cachedSources =
                    sources.filter { source ->
                        try {
                            sourceAggregationRepository.isSourceCached(source)
                        } catch (e: Exception) {
                            DebugLogger.w("EpisodeAutoPlayService", "Error checking cache status for source", e)
                            false
                        }
                    }

                if (cachedSources.isNotEmpty()) {
                    val bestCached = cachedSources.maxByOrNull { it.getQualityScore() }
                    DebugLogger.d("EpisodeAutoPlayService", "Selected cached source: ${bestCached?.id}")
                    return bestCached
                }

                // Fall back to best available source
                val bestSource =
                    sources
                        .maxByOrNull { it.getQualityScore() }
                        ?: sources.firstOrNull()

                DebugLogger.d("EpisodeAutoPlayService", "Selected source: ${bestSource?.id}")
                bestSource
            } catch (e: Exception) {
                DebugLogger.e("EpisodeAutoPlayService", "Error selecting best source for auto-play", e)
                sources.firstOrNull()
            }
        }

        /**
         * Resolve streaming URL for the selected source
         */
        suspend fun resolveStreamingUrl(source: SourceMetadata): String? {
            return try {
                val streamingUrl = sourceAggregationRepository.getStreamingUrl(source)
                DebugLogger.d("EpisodeAutoPlayService", "Resolved streaming URL for source: ${source.id}")
                streamingUrl
            } catch (e: Exception) {
                DebugLogger.e("EpisodeAutoPlayService", "Error resolving streaming URL", e)
                null
            }
        }

        /**
         * Complete episode resolution for auto-play
         * Returns the resolved streaming URL or null if resolution fails
         */
        suspend fun resolveEpisodeForAutoPlay(
            nextEpisode: NextEpisodeResult,
            showTitle: String,
        ): AutoPlayEpisodeResult {
            DebugLogger.d(
                "EpisodeAutoPlayService",
                "Starting complete episode resolution for ${nextEpisode.getDisplayText()}",
            )

            try {
                // Step 1: Get all available sources
                val sources = resolveEpisodeSources(nextEpisode, showTitle)
                if (sources.isEmpty()) {
                    DebugLogger.w("EpisodeAutoPlayService", "No sources found for episode")
                    return AutoPlayEpisodeResult.NoSourcesFound(nextEpisode)
                }

                // Step 2: Select best source for auto-play
                val bestSource = getBestSourceForAutoPlay(sources)
                if (bestSource == null) {
                    DebugLogger.w("EpisodeAutoPlayService", "No suitable source found for auto-play")
                    return AutoPlayEpisodeResult.NoSuitableSource(nextEpisode, sources)
                }

                // Step 3: Resolve streaming URL
                val streamingUrl = resolveStreamingUrl(bestSource)
                if (streamingUrl.isNullOrEmpty()) {
                    DebugLogger.w("EpisodeAutoPlayService", "Failed to resolve streaming URL")
                    return AutoPlayEpisodeResult.UrlResolutionFailed(nextEpisode, bestSource)
                }

                DebugLogger.i(
                    "EpisodeAutoPlayService",
                    "Successfully resolved episode for auto-play: ${nextEpisode.getDisplayText()}",
                )

                return AutoPlayEpisodeResult.Success(
                    nextEpisode = nextEpisode,
                    source = bestSource,
                    streamingUrl = streamingUrl,
                )
            } catch (e: Exception) {
                DebugLogger.e("EpisodeAutoPlayService", "Error in complete episode resolution", e)
                return AutoPlayEpisodeResult.ResolutionError(nextEpisode, e.message ?: "Unknown error")
            }
        }

        /**
         * Create ContentDetail object for episode source resolution
         */
        private suspend fun createEpisodeContentDetail(
            nextEpisode: NextEpisodeResult,
            showTitle: String,
        ): ContentDetail {
            // Create a minimal ContentDetail implementation for episode source resolution
            return object : ContentDetail {
                override val id: String = "${nextEpisode.tmdbShowId}:${nextEpisode.seasonNumber}:${nextEpisode.episodeNumber}"
                override val title: String = showTitle
                override val description: String? = nextEpisode.episodeTitle
                override val backgroundImageUrl: String? = null
                override val cardImageUrl: String? = null
                override val contentType: ContentType = ContentType.TV_EPISODE
                override val metadata: ContentMetadata =
                    ContentMetadata(
                        season = nextEpisode.seasonNumber,
                        episode = nextEpisode.episodeNumber,
                    )
                override val actions: List<ContentAction> = emptyList()
                override val videoUrl: String? = null
                override val sources: List<StreamingSource> = emptyList()
            }
        }
    }

/**
 * Result of auto-play episode resolution
 */
sealed class AutoPlayEpisodeResult {
    data class Success(
        val nextEpisode: NextEpisodeResult,
        val source: SourceMetadata,
        val streamingUrl: String,
    ) : AutoPlayEpisodeResult()

    data class NoSourcesFound(
        val nextEpisode: NextEpisodeResult,
    ) : AutoPlayEpisodeResult()

    data class NoSuitableSource(
        val nextEpisode: NextEpisodeResult,
        val availableSources: List<SourceMetadata>,
    ) : AutoPlayEpisodeResult()

    data class UrlResolutionFailed(
        val nextEpisode: NextEpisodeResult,
        val source: SourceMetadata,
    ) : AutoPlayEpisodeResult()

    data class ResolutionError(
        val nextEpisode: NextEpisodeResult,
        val errorMessage: String,
    ) : AutoPlayEpisodeResult()
}
