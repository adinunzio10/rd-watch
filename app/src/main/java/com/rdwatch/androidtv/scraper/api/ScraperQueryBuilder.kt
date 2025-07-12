package com.rdwatch.androidtv.scraper.api

import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.scraper.models.ManifestCapability
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds query URLs for different scraper services based on their manifest configuration
 * Handles scraper-specific URL patterns and query parameters
 */
@Singleton
class ScraperQueryBuilder @Inject constructor() {
    
    /**
     * Build query URL for content based on scraper manifest
     */
    fun buildContentQueryUrl(
        manifest: ScraperManifest,
        contentType: String,
        contentId: String,
        imdbId: String? = null,
        tmdbId: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): String {
        println("DEBUG [ScraperQueryBuilder]: Building URL for ${manifest.name} - contentType: $contentType, contentId: $contentId")
        
        return when (manifest.name.lowercase()) {
            "torrentio" -> buildTorrentioUrl(manifest, contentType, imdbId ?: contentId, seasonNumber, episodeNumber)
            "knightcrawler" -> buildKnightCrawlerUrl(manifest, contentType, imdbId ?: contentId, seasonNumber, episodeNumber)
            "cinemeta" -> buildCinemetaUrl(manifest, contentType, imdbId ?: contentId)
            else -> buildGenericStremioUrl(manifest, contentType, imdbId ?: contentId, seasonNumber, episodeNumber)
        }
    }
    
    /**
     * Build Torrentio-specific URL
     * Format: https://torrentio.strem.fun/[config]/stream/[type]/[id]:[season]:[episode].json
     */
    private fun buildTorrentioUrl(
        manifest: ScraperManifest,
        contentType: String,
        imdbId: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): String {
        val baseUrl = manifest.baseUrl.trimEnd('/')
        val config = extractTorrentioConfig(manifest)
        val type = mapContentType(contentType)
        
        val idPart = when (contentType.lowercase()) {
            "movie" -> imdbId
            "tv", "series" -> {
                if (seasonNumber != null && episodeNumber != null) {
                    "$imdbId:$seasonNumber:$episodeNumber"
                } else {
                    imdbId
                }
            }
            else -> imdbId
        }
        
        val url = "$baseUrl/$config/stream/$type/$idPart.json"
        println("DEBUG [ScraperQueryBuilder]: Torrentio URL: $url")
        return url
    }
    
    /**
     * Build KnightCrawler-specific URL
     * Format: https://knightcrawler.elfhosted.com/stream/[type]/[id].json
     */
    private fun buildKnightCrawlerUrl(
        manifest: ScraperManifest,
        contentType: String,
        imdbId: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): String {
        val baseUrl = manifest.baseUrl.trimEnd('/')
        val type = mapContentType(contentType)
        
        val idPart = when (contentType.lowercase()) {
            "movie" -> imdbId
            "tv", "series" -> {
                if (seasonNumber != null && episodeNumber != null) {
                    "$imdbId:$seasonNumber:$episodeNumber"
                } else {
                    imdbId
                }
            }
            else -> imdbId
        }
        
        val url = "$baseUrl/stream/$type/$idPart.json"
        println("DEBUG [ScraperQueryBuilder]: KnightCrawler URL: $url")
        return url
    }
    
    /**
     * Build Cinemeta-specific URL for metadata
     * Format: https://v3-cinemeta.strem.io/meta/[type]/[id].json
     */
    private fun buildCinemetaUrl(
        manifest: ScraperManifest,
        contentType: String,
        imdbId: String
    ): String {
        val baseUrl = manifest.baseUrl.trimEnd('/')
        val type = mapContentType(contentType)
        
        val url = "$baseUrl/meta/$type/$imdbId.json"
        println("DEBUG [ScraperQueryBuilder]: Cinemeta URL: $url")
        return url
    }
    
    /**
     * Build generic Stremio addon URL
     */
    private fun buildGenericStremioUrl(
        manifest: ScraperManifest,
        contentType: String,
        contentId: String,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): String {
        val baseUrl = manifest.baseUrl.trimEnd('/')
        val type = mapContentType(contentType)
        
        // Determine endpoint based on capabilities
        val endpoint = when {
            manifest.metadata.capabilities.contains(ManifestCapability.STREAM) -> "stream"
            manifest.metadata.capabilities.contains(ManifestCapability.META) -> "meta"
            manifest.metadata.capabilities.contains(ManifestCapability.CATALOG) -> "catalog"
            else -> "stream"
        }
        
        val idPart = when {
            endpoint == "stream" && contentType.lowercase() in listOf("tv", "series") && 
            seasonNumber != null && episodeNumber != null -> "$contentId:$seasonNumber:$episodeNumber"
            else -> contentId
        }
        
        val url = "$baseUrl/$endpoint/$type/$idPart.json"
        println("DEBUG [ScraperQueryBuilder]: Generic Stremio URL: $url")
        return url
    }
    
    /**
     * Extract Torrentio configuration from manifest
     */
    private fun extractTorrentioConfig(manifest: ScraperManifest): String {
        // Default Torrentio configuration
        // This can be extended to read from manifest.config or user preferences
        val defaultConfig = "defaults"
        
        // Check if custom config is specified in manifest
        val customConfig = manifest.configuration.additionalParams["torrentioConfig"]
        
        return customConfig ?: defaultConfig
    }
    
    /**
     * Map content type to Stremio type
     */
    private fun mapContentType(contentType: String): String {
        return when (contentType.lowercase()) {
            "movie", "movies" -> "movie"
            "tv", "series", "tvshow", "tv_show" -> "series"
            else -> contentType.lowercase()
        }
    }
    
    /**
     * Build catalog query URL
     */
    fun buildCatalogQueryUrl(
        manifest: ScraperManifest,
        catalogType: String,
        skip: Int = 0
    ): String {
        val baseUrl = manifest.baseUrl.trimEnd('/')
        val url = "$baseUrl/catalog/$catalogType/skip=$skip.json"
        println("DEBUG [ScraperQueryBuilder]: Catalog URL: $url")
        return url
    }
    
    /**
     * Build search query URL
     */
    fun buildSearchQueryUrl(
        manifest: ScraperManifest,
        query: String,
        contentType: String? = null
    ): String {
        val baseUrl = manifest.baseUrl.trimEnd('/')
        val encodedQuery = java.net.URLEncoder.encode(query, "UTF-8")
        
        val type = contentType?.let { mapContentType(it) } ?: "movie,series"
        val url = "$baseUrl/catalog/$type/search=$encodedQuery.json"
        
        println("DEBUG [ScraperQueryBuilder]: Search URL: $url")
        return url
    }
    
    /**
     * Extract headers needed for scraper request
     */
    fun getRequestHeaders(manifest: ScraperManifest): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        
        // Add API key if specified
        manifest.configuration.additionalParams["apiKey"]?.let { apiKey ->
            headers["Authorization"] = "Bearer $apiKey"
        }
        
        // Add custom headers from manifest configuration
        // For now, we don't have a specific headers field in ManifestConfiguration
        // so we'll use default headers only
        
        return headers
    }
}