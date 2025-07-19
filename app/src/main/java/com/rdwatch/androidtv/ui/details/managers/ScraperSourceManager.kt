package com.rdwatch.androidtv.ui.details.managers

import com.rdwatch.androidtv.scraper.ScraperManifestManager
import com.rdwatch.androidtv.scraper.api.ScraperApiClient
import com.rdwatch.androidtv.scraper.api.ScraperApiResponse
import com.rdwatch.androidtv.scraper.api.ScraperQueryBuilder
import com.rdwatch.androidtv.scraper.api.ScraperResponseMapper
import com.rdwatch.androidtv.scraper.models.ManifestCapability
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.ui.details.adapters.ScraperSourceAdapter
import com.rdwatch.androidtv.ui.details.models.SourceProvider
import com.rdwatch.androidtv.ui.details.models.SourceQuality
import com.rdwatch.androidtv.ui.details.models.StreamingSource
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level manager for scraper-based source operations
 * Coordinates between the scraper system and UI components
 */
@Singleton
class ScraperSourceManager
    @Inject
    constructor(
        private val scraperManifestManager: ScraperManifestManager,
        private val scraperSourceAdapter: ScraperSourceAdapter,
        private val scraperApiClient: ScraperApiClient,
        private val scraperQueryBuilder: ScraperQueryBuilder,
        private val scraperResponseMapper: ScraperResponseMapper,
    ) {
        // === Source Provider Operations ===

        /**
         * Get all available source providers from scrapers
         */
        suspend fun getAvailableProviders(): List<SourceProvider> {
            return when (val result = scraperManifestManager.getEnabledManifests()) {
                is ManifestResult.Success -> {
                    scraperSourceAdapter.manifestsToProviders(result.data)
                }
                is ManifestResult.Error -> {
                    // Return default providers if scrapers fail to load
                    SourceProvider.getDefaultProviders()
                }
            }
        }

        /**
         * Get streaming-capable source providers
         */
        suspend fun getStreamingProviders(): List<SourceProvider> {
            return when (val result = scraperManifestManager.getManifestsByCapability(ManifestCapability.STREAM)) {
                is ManifestResult.Success -> {
                    scraperSourceAdapter.manifestsToProviders(
                        scraperSourceAdapter.getEnabledManifests(result.data),
                    )
                }
                is ManifestResult.Error -> {
                    emptyList()
                }
            }
        }

        /**
         * Get metadata-capable source providers
         */
        suspend fun getMetadataProviders(): List<SourceProvider> {
            return when (val result = scraperManifestManager.getManifestsByCapability(ManifestCapability.META)) {
                is ManifestResult.Success -> {
                    scraperSourceAdapter.manifestsToProviders(
                        scraperSourceAdapter.getEnabledManifests(result.data),
                    )
                }
                is ManifestResult.Error -> {
                    emptyList()
                }
            }
        }

        // === Source Operations ===

        /**
         * Get sources for TV episode from scrapers
         */
        suspend fun getSourcesForTVEpisode(
            tvShowId: String,
            seasonNumber: Int,
            episodeNumber: Int,
            imdbId: String? = null,
            tmdbId: String? = null,
        ): List<StreamingSource> =
            coroutineScope {
                println(
                    "DEBUG [ScraperSourceManager]: getSourcesForTVEpisode called - tvShowId: $tvShowId, season: $seasonNumber, episode: $episodeNumber",
                )

                // Get streaming manifests
                val streamingManifests =
                    when (val result = scraperManifestManager.getManifestsByCapability(ManifestCapability.STREAM)) {
                        is ManifestResult.Success -> {
                            println("DEBUG [ScraperSourceManager]: Found ${result.data.size} streaming manifests for TV episode")
                            scraperSourceAdapter.getEnabledManifests(result.data)
                        }
                        is ManifestResult.Error -> {
                            println(
                                "DEBUG [ScraperSourceManager]: Error getting streaming manifests for TV episode: ${result.exception.message}",
                            )
                            emptyList()
                        }
                    }

                println("DEBUG [ScraperSourceManager]: Enabled streaming manifests for TV episode: ${streamingManifests.size}")

                // Query all manifests concurrently for the specific episode
                val sourceLists =
                    streamingManifests.map { manifest ->
                        async {
                            println("DEBUG [ScraperSourceManager]: Querying manifest for TV episode: ${manifest.name}")
                            try {
                                val sources =
                                    queryManifestForTVEpisode(
                                        manifest = manifest,
                                        tvShowId = tvShowId,
                                        seasonNumber = seasonNumber,
                                        episodeNumber = episodeNumber,
                                        imdbId = imdbId,
                                        tmdbId = tmdbId,
                                    )
                                println(
                                    "DEBUG [ScraperSourceManager]: Manifest ${manifest.name} returned ${sources.size} sources for TV episode",
                                )
                                sources
                            } catch (e: Exception) {
                                println("DEBUG [ScraperSourceManager]: Error querying ${manifest.name} for TV episode: ${e.message}")
                                emptyList()
                            }
                        }
                    }.awaitAll()

                // Flatten all source lists and sort by priority
                val allSources = sourceLists.flatten()
                val sortedSources = allSources.sortedByDescending { scraperResponseMapper.calculatePriorityScore(it) }

                println("DEBUG [ScraperSourceManager]: Returning ${sortedSources.size} total sources for TV episode")
                sortedSources
            }

        /**
         * Get sources for content from scrapers
         */
        suspend fun getSourcesForContent(
            contentId: String,
            contentType: String,
            imdbId: String? = null,
            tmdbId: String? = null,
        ): List<StreamingSource> =
            coroutineScope {
                println(
                    "DEBUG [ScraperSourceManager]: getSourcesForContent called with contentId: $contentId, contentType: $contentType, imdbId: $imdbId, tmdbId: $tmdbId",
                )

                // Get streaming manifests
                val streamingManifests =
                    when (val result = scraperManifestManager.getManifestsByCapability(ManifestCapability.STREAM)) {
                        is ManifestResult.Success -> {
                            println("DEBUG [ScraperSourceManager]: Found ${result.data.size} streaming manifests")
                            scraperSourceAdapter.getEnabledManifests(result.data)
                        }
                        is ManifestResult.Error -> {
                            println("DEBUG [ScraperSourceManager]: Error getting streaming manifests: ${result.exception.message}")
                            emptyList()
                        }
                    }

                println("DEBUG [ScraperSourceManager]: Enabled streaming manifests: ${streamingManifests.size}")

                // Query all manifests concurrently
                val sourceLists =
                    streamingManifests.map { manifest ->
                        async {
                            println("DEBUG [ScraperSourceManager]: Querying manifest: ${manifest.name}")
                            try {
                                val sources = queryManifestForSources(manifest, contentId, contentType, imdbId, tmdbId)
                                println("DEBUG [ScraperSourceManager]: Manifest ${manifest.name} returned ${sources.size} sources")
                                sources
                            } catch (e: Exception) {
                                println("DEBUG [ScraperSourceManager]: Error querying ${manifest.name}: ${e.message}")
                                emptyList()
                            }
                        }
                    }.awaitAll()

                // Flatten all source lists and sort by priority
                val allSources = sourceLists.flatten()
                val sortedSources = allSources.sortedByDescending { scraperResponseMapper.calculatePriorityScore(it) }

                println("DEBUG [ScraperSourceManager]: Returning ${sortedSources.size} total sources")
                sortedSources
            }

        /**
         * Query a specific manifest for TV episode sources
         */
        private suspend fun queryManifestForTVEpisode(
            manifest: ScraperManifest,
            tvShowId: String,
            seasonNumber: Int,
            episodeNumber: Int,
            imdbId: String? = null,
            tmdbId: String? = null,
        ): List<StreamingSource> {
            println("DEBUG [ScraperSourceManager]: Making real API call to ${manifest.name} for TV episode")

            // Build the query URL with season and episode parameters
            val queryUrl =
                scraperQueryBuilder.buildContentQueryUrl(
                    manifest = manifest,
                    contentType = "series",
                    contentId = imdbId ?: tvShowId,
                    imdbId = imdbId,
                    tmdbId = tmdbId,
                    seasonNumber = seasonNumber,
                    episodeNumber = episodeNumber,
                )

            // Get request headers
            val headers = scraperQueryBuilder.getRequestHeaders(manifest)

            // Make the API call
            val response = scraperApiClient.makeScraperRequest(queryUrl, headers)

            return when (response) {
                is ScraperApiResponse.Success -> {
                    println("DEBUG [ScraperSourceManager]: API call successful for ${manifest.name} TV episode")
                    // Parse the response with episode-specific content ID
                    val episodeContentId = "$tvShowId:$seasonNumber:$episodeNumber"
                    scraperResponseMapper.parseScraperResponse(
                        manifest = manifest,
                        responseBody = response.data,
                        contentId = episodeContentId,
                        contentType = "series",
                    )
                }
                is ScraperApiResponse.Error -> {
                    println("DEBUG [ScraperSourceManager]: API call failed for ${manifest.name} TV episode: ${response.message}")
                    // For development/testing, return sample sources if API fails
                    if (response.statusCode == 0 || response.message.contains("timeout", ignoreCase = true)) {
                        println("DEBUG [ScraperSourceManager]: Falling back to sample sources for ${manifest.name} TV episode")
                        createSampleSourcesForTVEpisode(manifest, tvShowId, seasonNumber, episodeNumber)
                    } else {
                        emptyList()
                    }
                }
            }
        }

        /**
         * Query a specific manifest for sources
         */
        private suspend fun queryManifestForSources(
            manifest: ScraperManifest,
            contentId: String,
            contentType: String,
            imdbId: String? = null,
            tmdbId: String? = null,
        ): List<StreamingSource> {
            println("DEBUG [ScraperSourceManager]: Making real API call to ${manifest.name}")

            // Build the query URL
            val queryUrl =
                scraperQueryBuilder.buildContentQueryUrl(
                    manifest = manifest,
                    contentType = contentType,
                    contentId = contentId,
                    imdbId = imdbId,
                    tmdbId = tmdbId,
                )

            // Get request headers
            val headers = scraperQueryBuilder.getRequestHeaders(manifest)

            // Make the API call
            val response = scraperApiClient.makeScraperRequest(queryUrl, headers)

            return when (response) {
                is ScraperApiResponse.Success -> {
                    println("DEBUG [ScraperSourceManager]: API call successful for ${manifest.name}")
                    // Parse the response
                    scraperResponseMapper.parseScraperResponse(
                        manifest = manifest,
                        responseBody = response.data,
                        contentId = contentId,
                        contentType = contentType,
                    )
                }
                is ScraperApiResponse.Error -> {
                    println("DEBUG [ScraperSourceManager]: API call failed for ${manifest.name}: ${response.message}")
                    // For development/testing, return sample sources if API fails
                    if (response.statusCode == 0 || response.message.contains("timeout", ignoreCase = true)) {
                        println("DEBUG [ScraperSourceManager]: Falling back to sample sources for ${manifest.name}")
                        createSampleSourcesForManifest(manifest, contentId, contentType)
                    } else {
                        emptyList()
                    }
                }
            }
        }

        /**
         * Create sample sources for TV episode (for testing/development)
         */
        private fun createSampleSourcesForTVEpisode(
            manifest: ScraperManifest,
            tvShowId: String,
            seasonNumber: Int,
            episodeNumber: Int,
        ): List<StreamingSource> {
            println(
                "DEBUG [ScraperSourceManager]: Creating sample sources for TV episode: ${manifest.name} - S${seasonNumber}E$episodeNumber",
            )
            val sources = mutableListOf<StreamingSource>()

            // Create different quality sources for the episode
            val qualities =
                listOf(
                    SourceQuality.QUALITY_4K,
                    SourceQuality.QUALITY_1080P,
                    SourceQuality.QUALITY_720P,
                )

            val episodeContentId = "$tvShowId:$seasonNumber:$episodeNumber"

            qualities.forEach { quality ->
                val url = generateSampleTVEpisodeUrl(manifest, tvShowId, seasonNumber, episodeNumber, quality)
                println("DEBUG [ScraperSourceManager]: Creating TV episode source for quality: ${quality.displayName}, URL: $url")

                val source =
                    scraperSourceAdapter.createStreamingSource(
                        manifest = manifest,
                        url = url,
                        quality = quality,
                        title = "${manifest.displayName} S${seasonNumber}E$episodeNumber ${quality.shortName}",
                        seeders =
                            if (manifest.metadata.capabilities.contains(ManifestCapability.P2P)) {
                                (30..150).random() // Generally fewer seeders for TV episodes
                            } else {
                                null
                            },
                        leechers =
                            if (manifest.metadata.capabilities.contains(ManifestCapability.P2P)) {
                                (2..15).random()
                            } else {
                                null
                            },
                    )
                sources.add(source)
                println("DEBUG [ScraperSourceManager]: Added TV episode source: ${source.id}")
            }

            println("DEBUG [ScraperSourceManager]: Created ${sources.size} sources for TV episode: ${manifest.name}")
            return sources
        }

        /**
         * Create sample sources for a manifest (for testing/development)
         */
        private fun createSampleSourcesForManifest(
            manifest: ScraperManifest,
            contentId: String,
            contentType: String,
        ): List<StreamingSource> {
            println("DEBUG [ScraperSourceManager]: Creating sample sources for manifest: ${manifest.name}")
            val sources = mutableListOf<StreamingSource>()

            // Create different quality sources
            val qualities =
                listOf(
                    SourceQuality.QUALITY_4K,
                    SourceQuality.QUALITY_1080P,
                    SourceQuality.QUALITY_720P,
                )

            qualities.forEach { quality ->
                val url = generateSampleUrl(manifest, contentId, quality)
                println("DEBUG [ScraperSourceManager]: Creating source for quality: ${quality.displayName}, URL: $url")

                val source =
                    scraperSourceAdapter.createStreamingSource(
                        manifest = manifest,
                        url = url,
                        quality = quality,
                        title = "${manifest.displayName} ${quality.shortName}",
                        seeders =
                            if (manifest.metadata.capabilities.contains(ManifestCapability.P2P)) {
                                (50..200).random()
                            } else {
                                null
                            },
                        leechers =
                            if (manifest.metadata.capabilities.contains(ManifestCapability.P2P)) {
                                (5..20).random()
                            } else {
                                null
                            },
                    )
                sources.add(source)
                println("DEBUG [ScraperSourceManager]: Added source: ${source.id}")
            }

            println("DEBUG [ScraperSourceManager]: Created ${sources.size} sources for manifest: ${manifest.name}")
            return sources
        }

        /**
         * Generate sample URL for TV episode testing
         */
        private fun generateSampleTVEpisodeUrl(
            manifest: ScraperManifest,
            tvShowId: String,
            seasonNumber: Int,
            episodeNumber: Int,
            quality: SourceQuality,
        ): String {
            val episodeContentId = "$tvShowId:$seasonNumber:$episodeNumber"
            return when {
                manifest.metadata.capabilities.contains(ManifestCapability.P2P) -> {
                    "magnet:?xt=urn:btih:${episodeContentId}_${quality.shortName}"
                }
                else -> {
                    "${manifest.baseUrl}/stream/series/$episodeContentId/${quality.shortName}"
                }
            }
        }

        /**
         * Generate sample URL for testing
         */
        private fun generateSampleUrl(
            manifest: ScraperManifest,
            contentId: String,
            quality: SourceQuality,
        ): String {
            return when {
                manifest.metadata.capabilities.contains(ManifestCapability.P2P) -> {
                    "magnet:?xt=urn:btih:${contentId}_${quality.shortName}"
                }
                else -> {
                    "${manifest.baseUrl}/stream/$contentId/${quality.shortName}"
                }
            }
        }

        // === Provider Management ===

        /**
         * Enable/disable a source provider
         */
        suspend fun setProviderEnabled(
            providerId: String,
            enabled: Boolean,
        ) {
            scraperManifestManager.setManifestEnabled(providerId, enabled)
        }

        /**
         * Update provider priority
         */
        suspend fun updateProviderPriority(
            providerId: String,
            priority: Int,
        ) {
            scraperManifestManager.updateManifestPriority(providerId, priority)
        }

        /**
         * Refresh all providers
         */
        suspend fun refreshProviders() {
            scraperManifestManager.refreshAllManifests()
        }

        /**
         * Add new provider from URL
         */
        suspend fun addProviderFromUrl(url: String): Boolean {
            return when (val result = scraperManifestManager.addManifestFromUrl(url)) {
                is ManifestResult.Success -> true
                is ManifestResult.Error -> false
            }
        }

        /**
         * Remove provider
         */
        suspend fun removeProvider(providerId: String): Boolean {
            return when (val result = scraperManifestManager.removeManifest(providerId)) {
                is ManifestResult.Success -> true
                is ManifestResult.Error -> false
            }
        }

        // === Reactive Operations ===

        /**
         * Observe available providers
         */
        fun observeAvailableProviders(): Flow<List<SourceProvider>> {
            return scraperManifestManager.observeEnabledManifests().map { result ->
                when (result) {
                    is ManifestResult.Success -> {
                        scraperSourceAdapter.manifestsToProviders(result.data)
                    }
                    is ManifestResult.Error -> {
                        SourceProvider.getDefaultProviders()
                    }
                }
            }
        }

        /**
         * Observe streaming providers
         */
        fun observeStreamingProviders(): Flow<List<SourceProvider>> {
            return scraperManifestManager.observeEnabledManifests().map { result ->
                when (result) {
                    is ManifestResult.Success -> {
                        val streamingManifests = scraperSourceAdapter.getStreamingManifests(result.data)
                        scraperSourceAdapter.manifestsToProviders(streamingManifests)
                    }
                    is ManifestResult.Error -> {
                        emptyList()
                    }
                }
            }
        }

        // === Statistics and Monitoring ===

        /**
         * Get source statistics
         */
        suspend fun getSourceStatistics(): SourceStatistics {
            val stats = scraperManifestManager.getStatistics()

            return when (stats) {
                is ManifestResult.Success -> {
                    val repoStats = stats.data.repositoryStats
                    SourceStatistics(
                        totalProviders = repoStats?.totalManifests ?: 0,
                        enabledProviders = repoStats?.enabledManifests ?: 0,
                        streamingProviders = repoStats?.manifestsByCapability?.get(ManifestCapability.STREAM) ?: 0,
                        metadataProviders = repoStats?.manifestsByCapability?.get(ManifestCapability.META) ?: 0,
                        lastUpdateTime = stats.data.lastRefreshTime,
                    )
                }
                is ManifestResult.Error -> {
                    SourceStatistics()
                }
            }
        }

        // === Sample Data for Testing ===

        /**
         * Create sample sources for testing
         */
        suspend fun createSampleSources(): List<StreamingSource> {
            return when (val result = scraperManifestManager.getAllManifests()) {
                is ManifestResult.Success -> {
                    scraperSourceAdapter.createSampleSourcesFromManifests(result.data)
                }
                is ManifestResult.Error -> {
                    StreamingSource.createSampleSources()
                }
            }
        }

        /**
         * Get sample providers
         */
        fun getSampleProviders(): List<SourceProvider> {
            return SourceProvider.getDefaultProviders()
        }
    }

/**
 * Statistics about sources and providers
 */
data class SourceStatistics(
    val totalProviders: Int = 0,
    val enabledProviders: Int = 0,
    val streamingProviders: Int = 0,
    val metadataProviders: Int = 0,
    val lastUpdateTime: java.util.Date? = null,
)

/**
 * Result wrapper for source operations
 */
sealed class SourceResult<out T> {
    data class Success<T>(val data: T) : SourceResult<T>()

    data class Error(val message: String, val cause: Throwable? = null) : SourceResult<Nothing>()
}

/**
 * Configuration for source operations
 */
data class SourceConfig(
    val maxSourcesPerProvider: Int = 10,
    val timeoutSeconds: Int = 30,
    val retryAttempts: Int = 3,
    val prioritizeHighQuality: Boolean = true,
    val prioritizeP2P: Boolean = false,
    val minSeeders: Int = 5,
)
