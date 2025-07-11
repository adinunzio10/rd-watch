package com.rdwatch.androidtv.ui.details.adapters

import com.rdwatch.androidtv.scraper.models.ManifestCapability
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.ui.details.models.SourceFeatures
import com.rdwatch.androidtv.ui.details.models.SourceProvider
import com.rdwatch.androidtv.ui.details.models.SourceQuality
import com.rdwatch.androidtv.ui.details.models.SourceType
import com.rdwatch.androidtv.ui.details.models.StreamingSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adapter to convert scraper manifests into UI-compatible source models
 * Bridges the gap between the scraper system and source selection UI
 */
@Singleton
class ScraperSourceAdapter @Inject constructor() {
    
    /**
     * Convert a ScraperManifest to a SourceProvider
     */
    fun manifestToProvider(manifest: ScraperManifest): SourceProvider {
        println("DEBUG [ScraperSourceAdapter]: Creating provider for manifest: ${manifest.displayName}")
        println("DEBUG [ScraperSourceAdapter]: Validation status: ${manifest.metadata.validationStatus.name}")
        println("DEBUG [ScraperSourceAdapter]: Is enabled: ${manifest.isEnabled}")
        
        val isAvailable = manifest.metadata.validationStatus.name == "VALID"
        println("DEBUG [ScraperSourceAdapter]: Provider isAvailable: $isAvailable")
        
        return SourceProvider(
            id = manifest.id,
            name = manifest.name,
            displayName = manifest.displayName,
            logoUrl = manifest.logo,
            logoResource = null, // Could be mapped from manifest metadata
            isAvailable = isAvailable,
            isEnabled = manifest.isEnabled,
            capabilities = manifest.metadata.capabilities.map { it.name.lowercase() },
            color = getScraperColor(manifest),
            priority = manifest.priorityOrder
        )
    }
    
    /**
     * Convert multiple ScraperManifests to SourceProviders
     */
    fun manifestsToProviders(manifests: List<ScraperManifest>): List<SourceProvider> {
        return manifests.map { manifestToProvider(it) }
    }
    
    /**
     * Create a StreamingSource from scraper data
     */
    fun createStreamingSource(
        manifest: ScraperManifest,
        url: String,
        quality: SourceQuality,
        title: String? = null,
        size: String? = null,
        seeders: Int? = null,
        leechers: Int? = null
    ): StreamingSource {
        val provider = manifestToProvider(manifest)
        val sourceId = "${manifest.id}_${quality.name}_${url.hashCode()}"
        
        val sourceIsAvailable = manifest.isEnabled && provider.isAvailable
        println("DEBUG [ScraperSourceAdapter]: Source availability calculation: manifest.isEnabled=${manifest.isEnabled}, provider.isAvailable=${provider.isAvailable}, result=$sourceIsAvailable")
        
        val streamingSource = StreamingSource(
            id = sourceId,
            provider = provider,
            quality = quality,
            url = url,
            isAvailable = sourceIsAvailable,
            features = createSourceFeatures(manifest, seeders, leechers),
            sourceType = determineSourceType(manifest, url),
            title = title,
            size = size,
            metadata = createSourceMetadata(manifest)
        )
        
        println("DEBUG [ScraperSourceAdapter]: Created streaming source - ID: $sourceId, Provider: ${provider.displayName}, Quality: ${quality.displayName}, URL: $url, Available: $sourceIsAvailable")
        
        return streamingSource
    }
    
    /**
     * Create SourceFeatures from ScraperManifest
     */
    private fun createSourceFeatures(
        manifest: ScraperManifest,
        seeders: Int? = null,
        leechers: Int? = null
    ): SourceFeatures {
        val capabilities = manifest.metadata.capabilities
        
        return SourceFeatures(
            supportsDolbyVision = false, // Could be inferred from quality
            supportsDolbyAtmos = false,  // Could be inferred from quality
            supportsP2P = capabilities.contains(ManifestCapability.P2P),
            hasSubtitles = capabilities.contains(ManifestCapability.SUBTITLES),
            hasClosedCaptions = capabilities.contains(ManifestCapability.SUBTITLES),
            supportedLanguages = emptyList(), // Could be extracted from manifest config
            isConfigurable = capabilities.contains(ManifestCapability.CONFIGURABLE),
            seeders = seeders,
            leechers = leechers
        )
    }
    
    /**
     * Determine source type from manifest and URL
     */
    private fun determineSourceType(manifest: ScraperManifest, url: String): SourceType {
        val sourceType = when {
            url.startsWith("magnet:") -> SourceType.ScraperSourceType.MAGNET
            url.contains(".torrent") -> SourceType.ScraperSourceType.TORRENT
            manifest.metadata.capabilities.contains(ManifestCapability.STREAM) -> SourceType.ScraperSourceType.DIRECT_LINK
            manifest.metadata.capabilities.contains(ManifestCapability.META) -> SourceType.ScraperSourceType.METADATA
            manifest.metadata.capabilities.contains(ManifestCapability.SUBTITLES) -> SourceType.ScraperSourceType.SUBTITLES
            else -> SourceType.ScraperSourceType.DIRECT_LINK
        }
        
        val reliability = when (manifest.metadata.validationStatus.name) {
            "VALID" -> SourceType.SourceReliability.HIGH
            "OUTDATED" -> SourceType.SourceReliability.MEDIUM
            "INVALID", "ERROR" -> SourceType.SourceReliability.LOW
            else -> SourceType.SourceReliability.UNKNOWN
        }
        
        return SourceType(sourceType, reliability)
    }
    
    /**
     * Create source metadata from ScraperManifest
     */
    private fun createSourceMetadata(manifest: ScraperManifest): Map<String, String> {
        return mapOf(
            "scraper_id" to manifest.id,
            "scraper_name" to manifest.name,
            "scraper_version" to manifest.version,
            "scraper_author" to (manifest.author ?: "unknown"),
            "validation_status" to manifest.metadata.validationStatus.name,
            "capabilities" to manifest.metadata.capabilities.joinToString(",") { it.name },
            "base_url" to manifest.baseUrl,
            "source_url" to manifest.sourceUrl
        )
    }
    
    /**
     * Get color for scraper based on its type and capabilities
     */
    private fun getScraperColor(manifest: ScraperManifest): String? {
        val capabilities = manifest.metadata.capabilities
        
        return when {
            capabilities.contains(ManifestCapability.STREAM) && capabilities.contains(ManifestCapability.P2P) -> "#FF6B35" // Orange for P2P streaming
            capabilities.contains(ManifestCapability.STREAM) -> "#2E86AB" // Blue for direct streaming
            capabilities.contains(ManifestCapability.META) -> "#F18F01" // Yellow for metadata
            capabilities.contains(ManifestCapability.SUBTITLES) -> "#2E8B57" // Green for subtitles
            capabilities.contains(ManifestCapability.CATALOG) -> "#8B5CF6" // Purple for catalog
            else -> "#6B7280" // Gray for unknown
        }
    }
    
    /**
     * Filter manifests by capability
     */
    fun getManifestsByCapability(
        manifests: List<ScraperManifest>,
        capability: ManifestCapability
    ): List<ScraperManifest> {
        return manifests.filter { manifest ->
            manifest.metadata.capabilities.contains(capability)
        }
    }
    
    /**
     * Get streaming-capable manifests
     */
    fun getStreamingManifests(manifests: List<ScraperManifest>): List<ScraperManifest> {
        return getManifestsByCapability(manifests, ManifestCapability.STREAM)
    }
    
    /**
     * Get metadata-capable manifests
     */
    fun getMetadataManifests(manifests: List<ScraperManifest>): List<ScraperManifest> {
        return getManifestsByCapability(manifests, ManifestCapability.META)
    }
    
    /**
     * Get subtitle-capable manifests
     */
    fun getSubtitleManifests(manifests: List<ScraperManifest>): List<ScraperManifest> {
        return getManifestsByCapability(manifests, ManifestCapability.SUBTITLES)
    }
    
    /**
     * Sort manifests by priority
     */
    fun sortManifestsByPriority(manifests: List<ScraperManifest>): List<ScraperManifest> {
        return manifests.sortedBy { it.priorityOrder }
    }
    
    /**
     * Filter enabled manifests
     */
    fun getEnabledManifests(manifests: List<ScraperManifest>): List<ScraperManifest> {
        return manifests.filter { it.isEnabled }
    }
    
    /**
     * Create sample sources from manifests for testing
     */
    fun createSampleSourcesFromManifests(manifests: List<ScraperManifest>): List<StreamingSource> {
        return manifests.flatMap { manifest ->
            listOf(
                createStreamingSource(
                    manifest = manifest,
                    url = "magnet:?xt=urn:btih:sample",
                    quality = SourceQuality.QUALITY_1080P,
                    title = "Sample Movie 1080p",
                    seeders = 150,
                    leechers = 5
                ),
                createStreamingSource(
                    manifest = manifest,
                    url = "https://example.com/sample.mp4",
                    quality = SourceQuality.QUALITY_720P,
                    title = "Sample Movie 720p",
                    size = "1.5 GB"
                )
            )
        }
    }
}