package com.rdwatch.androidtv.ui.details.models.advanced

import java.util.Date

/**
 * Comprehensive metadata for a streaming source
 */
data class SourceMetadata(
    val id: String,
    val provider: SourceProviderInfo,
    val quality: QualityInfo,
    val codec: CodecInfo,
    val audio: AudioInfo,
    val release: ReleaseInfo,
    val file: FileInfo,
    val health: HealthInfo,
    val features: FeatureInfo,
    val availability: AvailabilityInfo,
    val metadata: Map<String, String> = emptyMap(),
) {
    /**
     * Calculate overall quality score for sorting with advanced health integration
     */
    fun getQualityScore(
        healthData: HealthData? = null,
        seasonPackInfo: SeasonPackInfo? = null,
    ): Int {
        var score = 0

        // Quality base score
        score += quality.resolution.baseScore

        // HDR bonuses
        if (quality.hdr10) score += 20
        if (quality.dolbyVision) score += 30
        if (quality.hdr10Plus) score += 25

        // Codec efficiency bonus
        score += codec.type.efficiencyBonus

        // Audio quality bonus
        score += audio.format.qualityBonus
        if (audio.dolbyAtmos) score += 15
        if (audio.dtsX) score += 10

        // Release quality bonus
        score += release.type.qualityBonus

        // Advanced health scoring
        healthData?.let { health ->
            // Use comprehensive health score instead of simple seeder count
            score += (health.overallScore * 0.8).toInt() // Scale to max 80 points

            // Additional bonuses for high reliability
            if (health.predictedReliability >= 90) {
                score += 25
            } else if (health.predictedReliability >= 75) {
                score += 15
            } else if (health.predictedReliability >= 60) {
                score += 10
            }

            // Risk level penalty
            score +=
                when (health.riskLevel) {
                    RiskLevel.MINIMAL -> 20
                    RiskLevel.LOW -> 10
                    RiskLevel.MEDIUM -> 0
                    RiskLevel.HIGH -> -15
                }

            // Provider authority bonus
            score += (health.sourceAuthority * 0.3).toInt()

            // Freshness bonus
            if (health.freshnessIndicator >= 90) {
                score += 10
            } else if (health.freshnessIndicator >= 70) {
                score += 5
            }

            // Success rate bonus
            score += (health.downloadSuccessRate * 20).toInt()
        } ?: run {
            // Fallback to basic health scoring if advanced health not available
            if (health.seeders != null && health.seeders > 0) {
                score +=
                    when {
                        health.seeders > 1000 -> 50
                        health.seeders > 500 -> 40
                        health.seeders > 100 -> 30
                        health.seeders > 50 -> 20
                        health.seeders > 10 -> 10
                        else -> 5
                    }
            }
        }

        // Season pack bonus
        seasonPackInfo?.let { packInfo ->
            score += SeasonPackDetector().getSeasonPackQualityScore(packInfo)
        }

        // Provider reliability bonus
        score += provider.reliability.ordinal * 10

        return score
    }

    /**
     * Get list of quality badges to display with advanced health and season pack information
     */
    fun getQualityBadges(
        healthData: HealthData? = null,
        seasonPackInfo: SeasonPackInfo? = null,
    ): List<QualityBadge> {
        val badges = mutableListOf<QualityBadge>()

        // Resolution badge
        badges.add(
            QualityBadge(
                text = quality.resolution.displayName,
                type = QualityBadge.Type.RESOLUTION,
                priority = 100,
            ),
        )

        // HDR badges
        if (quality.dolbyVision) {
            badges.add(
                QualityBadge(
                    text = "DV",
                    type = QualityBadge.Type.HDR,
                    priority = 95,
                ),
            )
        } else if (quality.hdr10Plus) {
            badges.add(
                QualityBadge(
                    text = "HDR10+",
                    type = QualityBadge.Type.HDR,
                    priority = 94,
                ),
            )
        } else if (quality.hdr10) {
            badges.add(
                QualityBadge(
                    text = "HDR10",
                    type = QualityBadge.Type.HDR,
                    priority = 93,
                ),
            )
        }

        // Codec badge
        badges.add(
            QualityBadge(
                text = codec.type.displayName,
                type = QualityBadge.Type.CODEC,
                priority = 80,
            ),
        )

        // Audio badges
        if (audio.dolbyAtmos) {
            badges.add(
                QualityBadge(
                    text = "Atmos",
                    type = QualityBadge.Type.AUDIO,
                    priority = 70,
                ),
            )
        } else if (audio.dtsX) {
            badges.add(
                QualityBadge(
                    text = "DTS:X",
                    type = QualityBadge.Type.AUDIO,
                    priority = 69,
                ),
            )
        } else {
            badges.add(
                QualityBadge(
                    text = audio.format.displayName,
                    type = QualityBadge.Type.AUDIO,
                    priority = 68,
                ),
            )
        }

        // Release type badge
        badges.add(
            QualityBadge(
                text = release.type.displayName,
                type = QualityBadge.Type.RELEASE,
                priority = 60,
            ),
        )

        // Advanced health indicator
        healthData?.let { health ->
            badges.add(health.getHealthBadge())

            // Risk indicator for high-risk sources
            if (health.riskLevel == RiskLevel.HIGH) {
                badges.add(
                    QualityBadge(
                        text = "High Risk",
                        type = QualityBadge.Type.HEALTH,
                        priority = 35,
                    ),
                )
            }

            // Predicted reliability for very reliable sources
            if (health.predictedReliability >= 95) {
                badges.add(
                    QualityBadge(
                        text = "Ultra Reliable",
                        type = QualityBadge.Type.HEALTH,
                        priority = 52,
                    ),
                )
            } else if (health.predictedReliability >= 85) {
                badges.add(
                    QualityBadge(
                        text = "Very Reliable",
                        type = QualityBadge.Type.HEALTH,
                        priority = 51,
                    ),
                )
            }
        } ?: run {
            // Fallback to basic health indicator for P2P
            if (health.seeders != null && health.seeders > 0) {
                val healthText =
                    when {
                        health.seeders > 100 -> "${health.seeders}S"
                        else -> "${health.seeders}S/${health.leechers ?: 0}L"
                    }
                badges.add(
                    QualityBadge(
                        text = healthText,
                        type = QualityBadge.Type.HEALTH,
                        priority = 50,
                    ),
                )
            }
        }

        // Season pack indicator
        seasonPackInfo?.let { packInfo ->
            packInfo.getSeasonPackBadge()?.let { badge ->
                badges.add(badge)
            }
        }

        return badges.sortedByDescending { it.priority }
    }

    /**
     * Check if source matches filter criteria
     */
    fun matchesFilter(filter: SourceFilter): Boolean {
        // Quality filter
        if (filter.minQuality != null && quality.resolution.ordinal < filter.minQuality.ordinal) {
            return false
        }

        // HDR filter
        if (filter.requireHDR && !quality.hasHDR()) {
            return false
        }

        // Codec filter
        if (filter.codecs.isNotEmpty() && codec.type !in filter.codecs) {
            return false
        }

        // Audio filter
        if (filter.audioFormats.isNotEmpty() && audio.format !in filter.audioFormats) {
            return false
        }

        // Release type filter
        if (filter.releaseTypes.isNotEmpty() && release.type !in filter.releaseTypes) {
            return false
        }

        // Provider filter
        if (filter.providers.isNotEmpty() && provider.id !in filter.providers) {
            return false
        }

        // Size filter
        if (filter.maxSizeGB != null && file.sizeInBytes != null) {
            val sizeInGB = file.sizeInBytes / (1024.0 * 1024.0 * 1024.0)
            if (sizeInGB > filter.maxSizeGB) {
                return false
            }
        }

        // Health filter for P2P
        if (filter.minSeeders != null && health.seeders != null) {
            if (health.seeders < filter.minSeeders) {
                return false
            }
        }

        return true
    }
}

/**
 * Provider information
 */
data class SourceProviderInfo(
    val id: String,
    val name: String,
    val displayName: String,
    val logoUrl: String?,
    val type: ProviderType,
    val reliability: ProviderReliability = ProviderReliability.UNKNOWN,
    val capabilities: Set<String> = emptySet(),
) {
    enum class ProviderType {
        TORRENT,
        DIRECT_STREAM,
        DEBRID,
        SUBTITLE,
        METADATA,
    }

    enum class ProviderReliability {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        UNKNOWN,
    }
}

/**
 * Quality information
 */
data class QualityInfo(
    val resolution: VideoResolution,
    val bitrate: Long? = null, // in bps
    val hdr10: Boolean = false,
    val hdr10Plus: Boolean = false,
    val dolbyVision: Boolean = false,
    val frameRate: Int? = null, // e.g., 24, 30, 60
) {
    fun hasHDR(): Boolean = hdr10 || hdr10Plus || dolbyVision

    fun getDisplayText(): String {
        val parts = mutableListOf(resolution.displayName)

        when {
            dolbyVision -> parts.add("Dolby Vision")
            hdr10Plus -> parts.add("HDR10+")
            hdr10 -> parts.add("HDR10")
        }

        frameRate?.let { if (it > 30) parts.add("${it}fps") }

        return parts.joinToString(" ")
    }
}

/**
 * Video resolution enum with metadata
 */
enum class VideoResolution(
    val displayName: String,
    val shortName: String,
    val pixels: Int,
    val baseScore: Int,
) {
    RESOLUTION_8K("8K Ultra HD", "8K", 7680 * 4320, 1000),
    RESOLUTION_4K("4K Ultra HD", "4K", 3840 * 2160, 900),
    RESOLUTION_1440P("2K QHD", "1440p", 2560 * 1440, 800),
    RESOLUTION_1080P("Full HD", "1080p", 1920 * 1080, 700),
    RESOLUTION_720P("HD", "720p", 1280 * 720, 600),
    RESOLUTION_480P("SD", "480p", 854 * 480, 500),
    RESOLUTION_360P("Low", "360p", 640 * 360, 400),
    RESOLUTION_240P("Very Low", "240p", 426 * 240, 300),
    UNKNOWN("Unknown", "?", 0, 0),
    ;

    companion object {
        fun fromString(str: String): VideoResolution {
            val normalized = str.uppercase().replace(" ", "")
            return entries.find {
                it.name.contains(normalized) ||
                    it.shortName.equals(normalized, ignoreCase = true) ||
                    normalized.contains(it.shortName.uppercase())
            } ?: UNKNOWN
        }
    }
}

/**
 * Codec information
 */
data class CodecInfo(
    val type: VideoCodec,
    val profile: String? = null, // e.g., "Main", "High", "Main 10"
    val level: String? = null, // e.g., "4.1", "5.0"
) {
    fun getDisplayText(): String {
        val parts = mutableListOf(type.displayName)
        profile?.let { parts.add("($it)") }
        return parts.joinToString(" ")
    }
}

/**
 * Video codec enum with metadata
 */
enum class VideoCodec(
    val displayName: String,
    val shortName: String,
    val efficiencyBonus: Int, // Higher is better
) {
    AV1("AV1", "AV1", 50),
    HEVC("HEVC", "H.265", 40),
    H264("H.264", "H.264", 30),
    VP9("VP9", "VP9", 35),
    MPEG4("MPEG-4", "MPEG4", 20),
    MPEG2("MPEG-2", "MPEG2", 10),
    UNKNOWN("Unknown", "?", 0),
    ;

    companion object {
        fun fromString(str: String): VideoCodec {
            val normalized = str.uppercase()
            return entries.find {
                normalized.contains(it.name) ||
                    normalized.contains(it.shortName.uppercase()) ||
                    (it == HEVC && (normalized.contains("X265") || normalized.contains("H265"))) ||
                    (it == H264 && (normalized.contains("X264") || normalized.contains("AVC")))
            } ?: UNKNOWN
        }
    }
}

/**
 * Audio information
 */
data class AudioInfo(
    val format: AudioFormat,
    val channels: String? = null, // e.g., "5.1", "7.1", "2.0"
    val bitrate: Int? = null, // in kbps
    val language: String? = null,
    val dolbyAtmos: Boolean = false,
    val dtsX: Boolean = false,
) {
    fun getDisplayText(): String {
        val parts = mutableListOf<String>()

        when {
            dolbyAtmos -> parts.add("Dolby Atmos")
            dtsX -> parts.add("DTS:X")
            else -> parts.add(format.displayName)
        }

        channels?.let { parts.add(it) }

        return parts.joinToString(" ")
    }
}

/**
 * Audio format enum with metadata
 */
enum class AudioFormat(
    val displayName: String,
    val shortName: String,
    val qualityBonus: Int,
) {
    TRUEHD("TrueHD", "TrueHD", 50),
    DTS_HD_MA("DTS-HD MA", "DTS-HD", 45),
    DTS_HD("DTS-HD", "DTS-HD", 40),
    EAC3("E-AC3", "DD+", 35),
    AC3("AC3", "DD", 30),
    DTS("DTS", "DTS", 35),
    FLAC("FLAC", "FLAC", 40),
    PCM("PCM", "PCM", 45),
    AAC("AAC", "AAC", 25),
    MP3("MP3", "MP3", 20),
    UNKNOWN("Unknown", "?", 0),
    ;

    companion object {
        fun fromString(str: String): AudioFormat {
            val normalized = str.uppercase()
            return entries.find {
                normalized.contains(it.name.replace("_", "-")) ||
                    normalized.contains(it.shortName.uppercase())
            } ?: UNKNOWN
        }
    }
}

/**
 * Release information
 */
data class ReleaseInfo(
    val type: ReleaseType,
    val group: String? = null, // Release group name
    val edition: String? = null, // e.g., "Director's Cut", "Extended"
    val year: Int? = null,
)

/**
 * Release type enum with metadata
 */
enum class ReleaseType(
    val displayName: String,
    val shortName: String,
    val qualityBonus: Int,
) {
    BLURAY_REMUX("BluRay REMUX", "REMUX", 100),
    BLURAY("BluRay", "BluRay", 90),
    WEB_DL("WEB-DL", "WEB-DL", 85),
    WEBRIP("WebRip", "WebRip", 80),
    HDTV("HDTV", "HDTV", 70),
    DVDRIP("DVDRip", "DVD", 60),
    HDCAM("HD CAM", "HDCAM", 40),
    CAM("CAM", "CAM", 30),
    SCREENER("Screener", "SCR", 35),
    UNKNOWN("Unknown", "?", 0),
    ;

    companion object {
        fun fromString(str: String): ReleaseType {
            val normalized = str.uppercase().replace("-", "").replace(" ", "")
            return entries.find {
                normalized.contains(it.name.replace("_", "")) ||
                    normalized.contains(it.shortName.uppercase().replace("-", ""))
            } ?: UNKNOWN
        }
    }
}

/**
 * File information
 */
data class FileInfo(
    val name: String? = null,
    val sizeInBytes: Long? = null,
    val extension: String? = null,
    val hash: String? = null, // Could be torrent hash or file hash
    val addedDate: Date? = null,
) {
    fun getFormattedSize(): String? {
        return sizeInBytes?.let {
            when {
                it >= 1_000_000_000_000L -> String.format("%.2f TB", it / 1_000_000_000_000.0)
                it >= 1_000_000_000L -> String.format("%.2f GB", it / 1_000_000_000.0)
                it >= 1_000_000L -> String.format("%.2f MB", it / 1_000_000.0)
                it >= 1_000L -> String.format("%.2f KB", it / 1_000.0)
                else -> "$it B"
            }
        }
    }
}

/**
 * Health information for P2P sources
 */
data class HealthInfo(
    val seeders: Int? = null,
    val leechers: Int? = null,
    val downloadSpeed: Long? = null, // bytes per second
    val uploadSpeed: Long? = null, // bytes per second
    val availability: Float? = null, // 0.0 to 1.0
    val lastChecked: Date? = null,
) {
    fun getHealthStatus(): HealthStatus {
        return when {
            seeders == null -> HealthStatus.UNKNOWN
            seeders == 0 -> HealthStatus.DEAD
            seeders < 5 -> HealthStatus.POOR
            seeders < 20 -> HealthStatus.FAIR
            seeders < 50 -> HealthStatus.GOOD
            else -> HealthStatus.EXCELLENT
        }
    }

    enum class HealthStatus {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        DEAD,
        UNKNOWN,
    }
}

/**
 * Feature information
 */
data class FeatureInfo(
    val subtitles: List<SubtitleInfo> = emptyList(),
    val has3D: Boolean = false,
    val hasChapters: Boolean = false,
    val hasMultipleAudioTracks: Boolean = false,
    val isDirectPlay: Boolean = false,
    val requiresTranscoding: Boolean = false,
    val supportedDevices: Set<String> = emptySet(),
)

/**
 * Subtitle information
 */
data class SubtitleInfo(
    val language: String,
    val languageCode: String, // ISO code
    val type: SubtitleType,
    val isForced: Boolean = false,
    val isDefault: Boolean = false,
) {
    enum class SubtitleType {
        EMBEDDED,
        EXTERNAL,
        CLOSED_CAPTION,
    }
}

/**
 * Availability information
 */
data class AvailabilityInfo(
    val isAvailable: Boolean = true,
    val region: String? = null,
    val expiryDate: Date? = null,
    val debridService: String? = null, // e.g., "real-debrid", "premiumize"
    val cached: Boolean = false, // For debrid services
)

/**
 * Quality badge for UI display
 */
data class QualityBadge(
    val text: String,
    val type: Type,
    val priority: Int = 0,
) {
    enum class Type {
        RESOLUTION,
        HDR,
        CODEC,
        AUDIO,
        RELEASE,
        HEALTH,
        FEATURE,
        PROVIDER,
    }
}

/**
 * Filter criteria for sources (Legacy - for backward compatibility)
 */
data class SourceFilter(
    val minQuality: VideoResolution? = null,
    val requireHDR: Boolean = false,
    val codecs: Set<VideoCodec> = emptySet(),
    val audioFormats: Set<AudioFormat> = emptySet(),
    val releaseTypes: Set<ReleaseType> = emptySet(),
    val providers: Set<String> = emptySet(),
    val maxSizeGB: Float? = null,
    val minSeeders: Int? = null,
    val requireCached: Boolean = false, // For debrid services
) {
    /**
     * Convert legacy SourceFilter to AdvancedSourceFilter
     */
    fun toAdvancedFilter(): AdvancedSourceFilter {
        return AdvancedSourceFilter(
            qualityFilters =
                QualityFilters(
                    minResolution = minQuality,
                    requireHDR = requireHDR,
                ),
            sourceTypeFilters =
                SourceTypeFilters(
                    cachedOnly = requireCached,
                ),
            codecFilters =
                CodecFilters(
                    allowedCodecs = codecs,
                ),
            audioFilters =
                AudioFilters(
                    allowedFormats = audioFormats,
                ),
            releaseTypeFilters =
                ReleaseTypeFilters(
                    allowedTypes = releaseTypes,
                ),
            providerFilters =
                ProviderFilters(
                    allowedProviders = providers,
                ),
            fileSizeFilters =
                FileSizeFilters(
                    maxSizeGB = maxSizeGB?.toDouble(),
                ),
            healthFilters =
                HealthFilters(
                    minSeeders = minSeeders,
                ),
        )
    }
}

/**
 * Sorting options for sources
 */
// SourceSortOption moved to com.rdwatch.androidtv.ui.details.models.SourceSortOption
