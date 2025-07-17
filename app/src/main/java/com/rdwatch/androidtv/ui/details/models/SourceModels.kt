package com.rdwatch.androidtv.ui.details.models

/**
 * Represents a scraper-based source provider
 */
data class SourceProvider(
    val id: String,
    val name: String,
    val displayName: String,
    val logoUrl: String?,
    val logoResource: Int? = null, // For bundled scraper logos
    val isAvailable: Boolean = true,
    val isEnabled: Boolean = true,
    val capabilities: List<String> = emptyList(),
    val color: String? = null, // Scraper brand color
    val priority: Int = 0, // Higher priority sources appear first
) {
    companion object {
        // Default scraper providers - these will be populated from ScraperManifestManager
        val TORRENTIO =
            SourceProvider(
                id = "torrentio",
                name = "torrentio",
                displayName = "Torrentio",
                logoUrl = null,
                logoResource = null,
                capabilities = listOf("stream", "p2p"),
                color = "#FF6B35",
                priority = 100,
            )

        val KNIGHTCRAWLER =
            SourceProvider(
                id = "knightcrawler",
                name = "knightcrawler",
                displayName = "KnightCrawler",
                logoUrl = null,
                logoResource = null,
                capabilities = listOf("stream", "p2p"),
                color = "#2E86AB",
                priority = 90,
            )

        val CINEMETA =
            SourceProvider(
                id = "cinemeta",
                name = "cinemeta",
                displayName = "Cinemeta",
                logoUrl = null,
                logoResource = null,
                capabilities = listOf("meta", "catalog"),
                color = "#F18F01",
                priority = 80,
            )

        val OPENSUBTITLES =
            SourceProvider(
                id = "opensubtitles",
                name = "opensubtitles",
                displayName = "OpenSubtitles",
                logoUrl = null,
                logoResource = null,
                capabilities = listOf("subtitles"),
                color = "#2E8B57",
                priority = 70,
            )

        val KITSU =
            SourceProvider(
                id = "kitsu",
                name = "kitsu",
                displayName = "Kitsu Anime",
                logoUrl = null,
                logoResource = null,
                capabilities = listOf("meta", "catalog"),
                color = "#F75239",
                priority = 60,
            )

        // Get all default scraper providers
        fun getDefaultProviders(): List<SourceProvider> =
            listOf(
                TORRENTIO,
                KNIGHTCRAWLER,
                CINEMETA,
                OPENSUBTITLES,
                KITSU,
            )
    }
}

/**
 * Video quality levels for streaming sources
 */
enum class SourceQuality(
    val displayName: String,
    val shortName: String,
    val priority: Int, // Higher priority qualities appear first
    val isHighQuality: Boolean = false,
) {
    QUALITY_8K("8K Ultra HD", "8K", 100, true),
    QUALITY_4K("4K Ultra HD", "4K", 90, true),
    QUALITY_4K_HDR("4K HDR", "4K HDR", 95, true),
    QUALITY_1080P("Full HD", "1080p", 80),
    QUALITY_1080P_HDR("Full HD HDR", "1080p HDR", 85, true),
    QUALITY_720P("HD", "720p", 70),
    QUALITY_720P_HDR("HD HDR", "720p HDR", 75, true),
    QUALITY_480P("Standard", "480p", 60),
    QUALITY_360P("Low", "360p", 50),
    QUALITY_240P("Very Low", "240p", 40),
    QUALITY_AUTO("Auto", "Auto", 30),
    ;

    companion object {
        fun fromString(quality: String?): SourceQuality? {
            if (quality == null) return null
            return entries.find {
                it.displayName.equals(quality, ignoreCase = true) ||
                    it.shortName.equals(quality, ignoreCase = true) ||
                    it.name.equals(quality, ignoreCase = true)
            }
        }

        fun getHighQualityOptions(): List<SourceQuality> {
            return entries.filter { it.isHighQuality }.sortedByDescending { it.priority }
        }

        fun getStandardQualityOptions(): List<SourceQuality> {
            return entries.filter { !it.isHighQuality }.sortedByDescending { it.priority }
        }
    }
}

/**
 * Additional scraper source features
 */
data class SourceFeatures(
    val supportsDolbyVision: Boolean = false,
    val supportsDolbyAtmos: Boolean = false,
    val supportsP2P: Boolean = false,
    val hasSubtitles: Boolean = true,
    val hasClosedCaptions: Boolean = true,
    val supportedLanguages: List<String> = emptyList(),
    val isConfigurable: Boolean = false,
    val seeders: Int? = null,
    val leechers: Int? = null,
)

/**
 * Source type information for scraper sources
 */
data class SourceType(
    val type: ScraperSourceType,
    val reliability: SourceReliability = SourceReliability.UNKNOWN,
) {
    enum class ScraperSourceType {
        TORRENT,
        DIRECT_LINK,
        MAGNET,
        METADATA,
        SUBTITLES,
    }

    enum class SourceReliability {
        HIGH,
        MEDIUM,
        LOW,
        UNKNOWN,
    }

    fun getDisplayType(): String {
        return when (type) {
            ScraperSourceType.TORRENT -> "Torrent"
            ScraperSourceType.DIRECT_LINK -> "Direct Link"
            ScraperSourceType.MAGNET -> "Magnet"
            ScraperSourceType.METADATA -> "Metadata"
            ScraperSourceType.SUBTITLES -> "Subtitles"
        }
    }

    fun getReliabilityText(): String {
        return when (reliability) {
            SourceReliability.HIGH -> "High Quality"
            SourceReliability.MEDIUM -> "Medium Quality"
            SourceReliability.LOW -> "Low Quality"
            SourceReliability.UNKNOWN -> "Unknown"
        }
    }
}

/**
 * Represents a scraper-based streaming source with provider, quality, and metadata
 */
data class StreamingSource(
    val id: String,
    val provider: SourceProvider,
    val quality: SourceQuality,
    val url: String,
    val isAvailable: Boolean = true,
    val features: SourceFeatures = SourceFeatures(),
    val sourceType: SourceType = SourceType(SourceType.ScraperSourceType.DIRECT_LINK),
    val region: String? = null,
    val title: String? = null,
    val size: String? = null,
    val addedDate: String? = null, // ISO date string
    val lastUpdated: String? = null, // ISO date string
    val metadata: Map<String, String> = emptyMap(),
) {
    /**
     * Check if this source is currently available
     */
    fun isCurrentlyAvailable(): Boolean {
        val result = isAvailable && provider.isAvailable
        println(
            "DEBUG [StreamingSource.isCurrentlyAvailable]: ${provider.displayName} - isAvailable: $isAvailable, provider.isAvailable: ${provider.isAvailable}, result: $result",
        )
        return result
    }

    /**
     * Get display text for availability
     */
    fun getAvailabilityText(): String {
        return when {
            !isAvailable -> "Not available"
            !provider.isAvailable -> "Scraper unavailable"
            !provider.isEnabled -> "Scraper disabled"
            else -> "Available"
        }
    }

    /**
     * Get priority score for sorting sources
     */
    fun getPriorityScore(): Int {
        var score = provider.priority + quality.priority

        // Bonus for high-quality features
        if (features.supportsDolbyVision) score += 10
        if (features.supportsDolbyAtmos) score += 5

        // Bonus for P2P sources with good seeder count
        if (features.supportsP2P && features.seeders != null) {
            score +=
                when {
                    features.seeders!! > 100 -> 15
                    features.seeders!! > 50 -> 10
                    features.seeders!! > 10 -> 5
                    else -> 0
                }
        }

        // Bonus for reliability
        score +=
            when (sourceType.reliability) {
                SourceType.SourceReliability.HIGH -> 20
                SourceType.SourceReliability.MEDIUM -> 10
                SourceType.SourceReliability.LOW -> 0
                SourceType.SourceReliability.UNKNOWN -> 0
            }

        return score
    }

    /**
     * Check if this source is a P2P source
     */
    fun isP2P(): Boolean {
        return features.supportsP2P || sourceType.type in
            listOf(
                SourceType.ScraperSourceType.TORRENT,
                SourceType.ScraperSourceType.MAGNET,
            )
    }

    /**
     * Check if this source is reliable
     */
    fun isReliable(): Boolean {
        return sourceType.reliability in
            listOf(
                SourceType.SourceReliability.HIGH,
                SourceType.SourceReliability.MEDIUM,
            )
    }

    /**
     * Get all quality badges for this source
     */
    fun getQualityBadges(): List<String> {
        val badges = mutableListOf<String>()

        // Main quality badge
        badges.add(quality.shortName)

        // Additional quality features
        if (features.supportsDolbyVision) badges.add("Dolby Vision")
        if (features.supportsDolbyAtmos) badges.add("Dolby Atmos")

        // P2P indicators
        if (features.supportsP2P) {
            badges.add("P2P")
            features.seeders?.let { seeders ->
                if (seeders > 0) badges.add("${seeders}S")
            }
        }

        // Source type
        badges.add(sourceType.getDisplayType())

        return badges
    }

    companion object {
        /**
         * Create a sample scraper source for testing
         */
        fun createSample(
            provider: SourceProvider = SourceProvider.TORRENTIO,
            quality: SourceQuality = SourceQuality.QUALITY_4K,
            sourceType: SourceType = SourceType(SourceType.ScraperSourceType.TORRENT, SourceType.SourceReliability.HIGH),
        ): StreamingSource {
            return StreamingSource(
                id = "${provider.id}_${quality.name}",
                provider = provider,
                quality = quality,
                url = "magnet:?xt=urn:btih:example",
                sourceType = sourceType,
                features =
                    SourceFeatures(
                        supportsDolbyVision = quality.isHighQuality,
                        supportsDolbyAtmos = quality.isHighQuality,
                        supportsP2P =
                            sourceType.type in
                                listOf(
                                    SourceType.ScraperSourceType.TORRENT,
                                    SourceType.ScraperSourceType.MAGNET,
                                ),
                        hasSubtitles = true,
                        hasClosedCaptions = true,
                        seeders = if (sourceType.type == SourceType.ScraperSourceType.TORRENT) 150 else null,
                    ),
            )
        }

        /**
         * Create sample scraper sources for testing
         */
        fun createSampleSources(): List<StreamingSource> {
            return listOf(
                createSample(
                    SourceProvider.TORRENTIO,
                    SourceQuality.QUALITY_4K_HDR,
                    SourceType(SourceType.ScraperSourceType.TORRENT, SourceType.SourceReliability.HIGH),
                ),
                createSample(
                    SourceProvider.KNIGHTCRAWLER,
                    SourceQuality.QUALITY_4K,
                    SourceType(SourceType.ScraperSourceType.TORRENT, SourceType.SourceReliability.HIGH),
                ),
                createSample(
                    SourceProvider.TORRENTIO,
                    SourceQuality.QUALITY_1080P_HDR,
                    SourceType(SourceType.ScraperSourceType.DIRECT_LINK, SourceType.SourceReliability.MEDIUM),
                ),
                createSample(
                    SourceProvider.CINEMETA,
                    SourceQuality.QUALITY_1080P,
                    SourceType(SourceType.ScraperSourceType.METADATA, SourceType.SourceReliability.HIGH),
                ),
                createSample(
                    SourceProvider.OPENSUBTITLES,
                    SourceQuality.QUALITY_1080P,
                    SourceType(SourceType.ScraperSourceType.SUBTITLES, SourceType.SourceReliability.HIGH),
                ),
            )
        }
    }
}
