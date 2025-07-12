package com.rdwatch.androidtv.scraper.api

import com.squareup.moshi.Moshi
import com.rdwatch.androidtv.scraper.api.models.StremioStream
import com.rdwatch.androidtv.scraper.api.models.StremioStreamResponse
import com.rdwatch.androidtv.scraper.api.models.TorrentInfo
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.ui.details.adapters.ScraperSourceAdapter
import com.rdwatch.androidtv.ui.details.models.SourceQuality
import com.rdwatch.androidtv.ui.details.models.StreamingSource
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Maps scraper API responses to StreamingSource objects
 * Handles parsing and quality detection for different scraper formats
 */
@Singleton
class ScraperResponseMapper @Inject constructor(
    private val scraperSourceAdapter: ScraperSourceAdapter,
    private val moshi: Moshi
) {
    
    /**
     * Parse scraper response and convert to StreamingSource list
     */
    fun parseScraperResponse(
        manifest: ScraperManifest,
        responseBody: String,
        contentId: String,
        contentType: String
    ): List<StreamingSource> {
        println("DEBUG [ScraperResponseMapper]: Parsing response for ${manifest.name}, response length: ${responseBody.length}")
        
        return try {
            when (manifest.name.lowercase()) {
                "torrentio", "knightcrawler" -> parseStremioResponse(manifest, responseBody)
                else -> parseGenericStremioResponse(manifest, responseBody)
            }
        } catch (e: Exception) {
            println("DEBUG [ScraperResponseMapper]: Error parsing response: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }
    
    /**
     * Parse Stremio-compatible response (Torrentio, KnightCrawler)
     */
    private fun parseStremioResponse(
        manifest: ScraperManifest,
        responseBody: String
    ): List<StreamingSource> {
        val adapter = moshi.adapter(StremioStreamResponse::class.java)
        val response = adapter.fromJson(responseBody) ?: return emptyList()
        println("DEBUG [ScraperResponseMapper]: Parsed ${response.streams.size} streams from ${manifest.name}")
        
        return response.streams.mapNotNull { stream ->
            try {
                convertStremioStreamToSource(manifest, stream)
            } catch (e: Exception) {
                println("DEBUG [ScraperResponseMapper]: Error converting stream: ${e.message}")
                null
            }
        }
    }
    
    /**
     * Parse generic Stremio response
     */
    private fun parseGenericStremioResponse(
        manifest: ScraperManifest,
        responseBody: String
    ): List<StreamingSource> {
        // Try parsing as Stremio format first
        return try {
            parseStremioResponse(manifest, responseBody)
        } catch (e: Exception) {
            println("DEBUG [ScraperResponseMapper]: Failed to parse as Stremio format, returning empty list")
            emptyList()
        }
    }
    
    /**
     * Convert Stremio stream to StreamingSource
     */
    private fun convertStremioStreamToSource(
        manifest: ScraperManifest,
        stream: StremioStream
    ): StreamingSource? {
        val streamUrl = stream.getStreamUrl() ?: return null
        val title = stream.getDisplayTitle()
        
        // Parse torrent info from title
        val torrentInfo = TorrentInfo.fromTitle(title)
        
        // Determine quality
        val quality = detectQuality(torrentInfo, title)
        
        // Extract size from title or behavior hints
        val size = torrentInfo.size ?: extractSizeFromTitle(title)
        
        // Create the streaming source
        val source = scraperSourceAdapter.createStreamingSource(
            manifest = manifest,
            url = streamUrl,
            quality = quality,
            title = buildSourceTitle(torrentInfo, stream),
            size = size,
            seeders = torrentInfo.seeders,
            leechers = torrentInfo.leechers
        )
        
        println("DEBUG [ScraperResponseMapper]: Created source - ${source.title} [${quality.displayName}] from ${manifest.name}")
        
        return source
    }
    
    /**
     * Detect quality from torrent info and title
     */
    private fun detectQuality(torrentInfo: TorrentInfo, title: String): SourceQuality {
        // First check parsed quality
        return when (torrentInfo.quality?.lowercase()) {
            "2160p", "4k" -> SourceQuality.QUALITY_4K
            "1080p" -> SourceQuality.QUALITY_1080P
            "720p" -> SourceQuality.QUALITY_720P
            "480p" -> SourceQuality.QUALITY_480P
            else -> {
                // Fallback to title parsing
                val upperTitle = title.uppercase()
                when {
                    upperTitle.contains("2160P") || upperTitle.contains("4K") -> SourceQuality.QUALITY_4K
                    upperTitle.contains("1080P") -> SourceQuality.QUALITY_1080P
                    upperTitle.contains("720P") -> SourceQuality.QUALITY_720P
                    upperTitle.contains("480P") -> SourceQuality.QUALITY_480P
                    else -> SourceQuality.QUALITY_AUTO
                }
            }
        }
    }
    
    /**
     * Build a clean source title from torrent info
     */
    private fun buildSourceTitle(torrentInfo: TorrentInfo, stream: StremioStream): String {
        val parts = mutableListOf<String>()
        
        // Add quality
        torrentInfo.quality?.let { parts.add(it) }
        
        // Add source type
        torrentInfo.source?.let { parts.add(it) }
        
        // Add codec
        torrentInfo.codec?.let { parts.add(it) }
        
        // Add HDR
        if (torrentInfo.hdr) parts.add("HDR")
        
        // Add audio
        torrentInfo.audio?.let { parts.add(it) }
        
        // Add seeders/leechers
        if (torrentInfo.seeders != null && torrentInfo.leechers != null) {
            parts.add("👥 ${torrentInfo.seeders}/${torrentInfo.leechers}")
        }
        
        // Add size
        torrentInfo.size?.let { parts.add(it) }
        
        // If we have no parts, use the original title
        return if (parts.isNotEmpty()) {
            parts.joinToString(" • ")
        } else {
            stream.getDisplayTitle()
        }
    }
    
    /**
     * Extract size from title using regex
     */
    private fun extractSizeFromTitle(title: String): String? {
        val sizeRegex = Regex("(\\d+\\.?\\d*)\\s?(GB|MB|TB)", RegexOption.IGNORE_CASE)
        return sizeRegex.find(title)?.value
    }
    
    /**
     * Calculate priority score for sorting sources
     */
    fun calculatePriorityScore(source: StreamingSource): Int {
        var score = 0
        
        // Quality score (higher quality = higher score)
        score += when (source.quality) {
            SourceQuality.QUALITY_4K -> 400
            SourceQuality.QUALITY_1080P -> 300
            SourceQuality.QUALITY_720P -> 200
            SourceQuality.QUALITY_480P -> 100
            else -> 50
        }
        
        // Provider priority
        score += source.provider.priority * 10
        
        // Seeders bonus for P2P sources
        source.features.seeders?.let { seeders ->
            score += when {
                seeders >= 100 -> 50
                seeders >= 50 -> 30
                seeders >= 10 -> 10
                else -> 0
            }
        }
        
        // Availability bonus
        if (source.isAvailable) score += 100
        
        return score
    }
}