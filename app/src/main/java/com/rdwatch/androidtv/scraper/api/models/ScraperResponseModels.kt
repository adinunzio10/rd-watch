package com.rdwatch.androidtv.scraper.api.models

import com.squareup.moshi.Json

/**
 * Response models for various scraper services
 * Based on Stremio addon protocol and scraper-specific formats
 */

// ============= Stremio Addon Protocol Models (Torrentio, KnightCrawler) =============

/**
 * Main response from Stremio-compatible scrapers
 */
data class StremioStreamResponse(
    @Json(name = "streams")
    val streams: List<StremioStream> = emptyList(),
)

/**
 * Individual stream from Stremio addon
 */
data class StremioStream(
    @Json(name = "name")
    val name: String? = null,
    @Json(name = "title")
    val title: String? = null,
    @Json(name = "infoHash")
    val infoHash: String? = null,
    @Json(name = "fileIdx")
    val fileIdx: Int? = null,
    @Json(name = "url")
    val url: String? = null,
    @Json(name = "behaviorHints")
    val behaviorHints: BehaviorHints? = null,
    @Json(name = "sources")
    val sources: List<String>? = null,
    @Json(name = "debridService")
    val debridService: String? = null,
    @Json(name = "availability")
    val availability: Double? = null,
) {
    /**
     * Get display title for the stream
     */
    fun getDisplayTitle(): String {
        return title ?: name ?: "Unknown Source"
    }

    /**
     * Check if this is a torrent stream
     */
    fun isTorrent(): Boolean {
        return !infoHash.isNullOrEmpty()
    }

    /**
     * Get stream URL (either direct URL or magnet link)
     */
    fun getStreamUrl(): String? {
        return when {
            !url.isNullOrEmpty() -> url
            isTorrent() -> buildMagnetLink()
            else -> null
        }
    }

    /**
     * Build magnet link from torrent info
     */
    private fun buildMagnetLink(): String? {
        return infoHash?.let { hash ->
            val magnetBuilder = StringBuilder("magnet:?xt=urn:btih:$hash")

            // Add display name if available
            name?.let { magnetBuilder.append("&dn=${java.net.URLEncoder.encode(it, "UTF-8")}") }

            // Add trackers from sources if available
            sources?.forEach { tracker ->
                magnetBuilder.append("&tr=${java.net.URLEncoder.encode(tracker, "UTF-8")}")
            }

            magnetBuilder.toString()
        }
    }
}

/**
 * Behavior hints for stream handling
 */
data class BehaviorHints(
    @Json(name = "bingeGroup")
    val bingeGroup: String? = null,
    @Json(name = "proxyHeaders")
    val proxyHeaders: ProxyHeaders? = null,
    @Json(name = "videoSize")
    val videoSize: Long? = null,
    @Json(name = "filename")
    val filename: String? = null,
)

/**
 * Proxy headers for stream access
 */
data class ProxyHeaders(
    @Json(name = "request")
    val request: Map<String, String>? = null,
    @Json(name = "response")
    val response: Map<String, String>? = null,
)

// ============= Torrent/P2P Specific Models =============

/**
 * Parsed torrent information from stream title
 */
data class TorrentInfo(
    val quality: String? = null,
    val codec: String? = null,
    val audio: String? = null,
    val size: String? = null,
    val seeders: Int? = null,
    val leechers: Int? = null,
    val source: String? = null,
    val tracker: String? = null, // Release group/tracker name (YTS, EZTV, RARBG, etc.)
    val hdr: Boolean = false,
    val is3D: Boolean = false,
) {
    companion object {
        /**
         * Parse torrent info from stream title
         */
        fun fromTitle(title: String): TorrentInfo {
            val upperTitle = title.uppercase()

            // Extract quality
            val quality =
                when {
                    upperTitle.contains("2160P") || upperTitle.contains("4K") -> "2160p"
                    upperTitle.contains("1080P") -> "1080p"
                    upperTitle.contains("720P") -> "720p"
                    upperTitle.contains("480P") -> "480p"
                    else -> null
                }

            // Extract codec
            val codec =
                when {
                    upperTitle.contains("X265") || upperTitle.contains("HEVC") -> "HEVC"
                    upperTitle.contains("X264") || upperTitle.contains("H264") -> "H264"
                    upperTitle.contains("AV1") -> "AV1"
                    else -> null
                }

            // Extract audio
            val audio =
                when {
                    upperTitle.contains("ATMOS") -> "Atmos"
                    upperTitle.contains("DTS-HD") -> "DTS-HD"
                    upperTitle.contains("TRUEHD") -> "TrueHD"
                    upperTitle.contains("DTS") -> "DTS"
                    upperTitle.contains("AC3") -> "AC3"
                    upperTitle.contains("AAC") -> "AAC"
                    else -> null
                }

            // Extract size (e.g., "5.2GB")
            val sizeRegex = Regex("(\\d+\\.?\\d*)\\s?(GB|MB)", RegexOption.IGNORE_CASE)
            val size = sizeRegex.find(title)?.value

            // Extract seeders/leechers (e.g., "👥 150/10")
            val seedersRegex = Regex("👥\\s?(\\d+)/(\\d+)")
            val seedersMatch = seedersRegex.find(title)
            val seeders = seedersMatch?.groupValues?.get(1)?.toIntOrNull()
            val leechers = seedersMatch?.groupValues?.get(2)?.toIntOrNull()

            // Check for HDR
            val hdr =
                upperTitle.contains("HDR") || upperTitle.contains("HDR10") ||
                    upperTitle.contains("DOLBY VISION") || upperTitle.contains("DV")

            // Check for 3D
            val is3D = upperTitle.contains("3D")

            // Extract source
            val source =
                when {
                    upperTitle.contains("BLURAY") || upperTitle.contains("BLU-RAY") -> "BluRay"
                    upperTitle.contains("WEBRIP") -> "WebRip"
                    upperTitle.contains("WEB-DL") || upperTitle.contains("WEBDL") -> "WEB-DL"
                    upperTitle.contains("HDTV") -> "HDTV"
                    upperTitle.contains("DVDRIP") -> "DVDRip"
                    upperTitle.contains("CAM") -> "CAM"
                    else -> null
                }

            // Extract tracker/release group (YTS, EZTV, RARBG, etc.)
            val tracker = extractTracker(title)

            return TorrentInfo(
                quality = quality,
                codec = codec,
                audio = audio,
                size = size,
                seeders = seeders,
                leechers = leechers,
                source = source,
                tracker = tracker,
                hdr = hdr,
                is3D = is3D,
            )
        }

        /**
         * Extract tracker/release group name from torrent title
         * Common patterns:
         * - Movie.Name.2024.1080p.BluRay.x264-RARBG
         * - Show.S01E01.720p.HDTV.x264-KILLERS
         * - Movie.Name.2024.4K.WEB-DL.x265-YTS
         */
        private fun extractTracker(title: String): String? {
            // Pattern 1: Tracker after dash (most common)
            // Example: Movie.Name.2024.1080p.BluRay.x264-RARBG
            val dashPattern = Regex("-(\\w+)(?:\\[|\\.|$)", RegexOption.IGNORE_CASE)
            dashPattern.find(title)?.let { match ->
                val tracker = match.groupValues[1].uppercase()
                return normalizeTracker(tracker)
            }

            // Pattern 2: Known trackers in brackets or at end
            // Example: Movie.Name.2024.1080p[YTS.MX] or Movie.Name.YTS
            val knownTrackers =
                listOf(
                    "YTS", "YIFY", "YTS.MX", "YTS.LT", "YTS.AM",
                    "EZTV", "ETTV", "EZTV.AG", "EZTV.RE",
                    "RARBG", "RARBG.TO", "RARBG.CC",
                    "1337X", "LEET", "1337X.TO",
                    "THEPIRATEBAY", "TPB",
                    "TORRENTGALAXY", "TGX",
                    "KICKASS", "KAT",
                )

            val upperTitle = title.uppercase()
            for (tracker in knownTrackers) {
                if (upperTitle.contains(tracker)) {
                    return normalizeTracker(tracker)
                }
            }

            // Pattern 3: Scene groups (usually all caps at end)
            val scenePattern = Regex("\\b([A-Z]{2,10})(?:\\[|\\.|$)")
            scenePattern.findAll(title).lastOrNull()?.let { match ->
                val group = match.groupValues[1]
                // Filter out common false positives
                val commonWords = setOf("PROPER", "REPACK", "INTERNAL", "LIMITED", "UNRATED", "EXTENDED")
                if (group !in commonWords && group.length >= 3) {
                    return group
                }
            }

            return null
        }

        /**
         * Normalize tracker names to consistent format
         */
        private fun normalizeTracker(tracker: String): String {
            return when (tracker.uppercase()) {
                "YIFY", "YTS.MX", "YTS.LT", "YTS.AM" -> "YTS"
                "ETTV", "EZTV.AG", "EZTV.RE" -> "EZTV"
                "RARBG.TO", "RARBG.CC" -> "RARBG"
                "1337X.TO", "LEET" -> "1337x"
                "THEPIRATEBAY" -> "TPB"
                "TORRENTGALAXY" -> "TGX"
                "KICKASS" -> "KAT"
                else -> tracker.uppercase()
            }
        }
    }
}

// ============= Generic Scraper Response =============

/**
 * Generic scraper response wrapper
 */
data class GenericScraperResponse(
    val sources: List<GenericSource> = emptyList(),
    val metadata: Map<String, Any>? = null,
)

/**
 * Generic source model that can be adapted from various scrapers
 */
data class GenericSource(
    val url: String,
    val title: String,
    val quality: String? = null,
    val type: String? = null,
    val size: String? = null,
    val seeders: Int? = null,
    val leechers: Int? = null,
    val metadata: Map<String, Any>? = null,
)
