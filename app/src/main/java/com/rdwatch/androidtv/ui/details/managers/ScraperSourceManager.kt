package com.rdwatch.androidtv.ui.details.managers

import com.rdwatch.androidtv.scraper.ScraperManifestManager
import com.rdwatch.androidtv.scraper.models.ManifestCapability
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.ui.details.adapters.ScraperSourceAdapter
import com.rdwatch.androidtv.ui.details.models.SourceProvider
import com.rdwatch.androidtv.ui.details.models.SourceQuality
import com.rdwatch.androidtv.ui.details.models.StreamingSource
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * High-level manager for scraper-based source operations
 * Coordinates between the scraper system and UI components
 */
@Singleton
class ScraperSourceManager @Inject constructor(
    private val scraperManifestManager: ScraperManifestManager,
    private val scraperSourceAdapter: ScraperSourceAdapter
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
                    scraperSourceAdapter.getEnabledManifests(result.data)
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
                    scraperSourceAdapter.getEnabledManifests(result.data)
                )
            }
            is ManifestResult.Error -> {
                emptyList()
            }
        }
    }
    
    // === Source Operations ===
    
    /**
     * Get sources for content from scrapers
     */
    suspend fun getSourcesForContent(
        contentId: String,
        contentType: String,
        imdbId: String? = null,
        tmdbId: String? = null
    ): List<StreamingSource> {
        println("DEBUG [ScraperSourceManager]: getSourcesForContent called with contentId: $contentId, contentType: $contentType, imdbId: $imdbId, tmdbId: $tmdbId")
        val streamingSources = mutableListOf<StreamingSource>()
        
        // Get streaming manifests
        val streamingManifests = when (val result = scraperManifestManager.getManifestsByCapability(ManifestCapability.STREAM)) {
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
        
        // Query each streaming manifest for sources
        streamingManifests.forEach { manifest ->
            println("DEBUG [ScraperSourceManager]: Querying manifest: ${manifest.name}")
            val sources = queryManifestForSources(manifest, contentId, contentType, imdbId, tmdbId)
            println("DEBUG [ScraperSourceManager]: Manifest ${manifest.name} returned ${sources.size} sources")
            streamingSources.addAll(sources)
        }
        
        // Sort by priority score
        val sortedSources = streamingSources.sortedByDescending { it.getPriorityScore() }
        println("DEBUG [ScraperSourceManager]: Returning ${sortedSources.size} total sources")
        return sortedSources
    }
    
    /**
     * Query a specific manifest for sources
     */
    private suspend fun queryManifestForSources(
        manifest: ScraperManifest,
        contentId: String,
        contentType: String,
        imdbId: String? = null,
        tmdbId: String? = null
    ): List<StreamingSource> {
        // This would be implemented to make actual API calls to the scraper
        // For now, returning sample sources
        return createSampleSourcesForManifest(manifest, contentId, contentType)
    }
    
    /**
     * Create sample sources for a manifest (for testing/development)
     */
    private fun createSampleSourcesForManifest(
        manifest: ScraperManifest,
        contentId: String,
        contentType: String
    ): List<StreamingSource> {
        val sources = mutableListOf<StreamingSource>()
        
        // Create different quality sources
        val qualities = listOf(
            SourceQuality.QUALITY_4K,
            SourceQuality.QUALITY_1080P,
            SourceQuality.QUALITY_720P
        )
        
        qualities.forEach { quality ->
            val source = scraperSourceAdapter.createStreamingSource(
                manifest = manifest,
                url = generateSampleUrl(manifest, contentId, quality),
                quality = quality,
                title = "Sample ${contentType} ${quality.shortName}",
                seeders = if (manifest.metadata.capabilities.contains(ManifestCapability.P2P)) {
                    (50..200).random()
                } else null,
                leechers = if (manifest.metadata.capabilities.contains(ManifestCapability.P2P)) {
                    (5..20).random()
                } else null
            )
            sources.add(source)
        }
        
        return sources
    }
    
    /**
     * Generate sample URL for testing
     */
    private fun generateSampleUrl(
        manifest: ScraperManifest,
        contentId: String,
        quality: SourceQuality
    ): String {
        return when {
            manifest.metadata.capabilities.contains(ManifestCapability.P2P) -> {
                "magnet:?xt=urn:btih:${contentId}_${quality.shortName}"
            }
            else -> {
                "${manifest.baseUrl}/stream/${contentId}/${quality.shortName}"
            }
        }
    }
    
    // === Provider Management ===
    
    /**
     * Enable/disable a source provider
     */
    suspend fun setProviderEnabled(providerId: String, enabled: Boolean) {
        scraperManifestManager.setManifestEnabled(providerId, enabled)
    }
    
    /**
     * Update provider priority
     */
    suspend fun updateProviderPriority(providerId: String, priority: Int) {
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
                    lastUpdateTime = stats.data.lastRefreshTime
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
    val lastUpdateTime: java.util.Date? = null
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
    val minSeeders: Int = 5
)