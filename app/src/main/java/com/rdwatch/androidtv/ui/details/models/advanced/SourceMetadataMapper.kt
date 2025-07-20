package com.rdwatch.androidtv.ui.details.models.advanced

import com.rdwatch.androidtv.network.models.UnrestrictLinkResponse
import com.rdwatch.androidtv.scraper.api.models.StremioStream
import com.rdwatch.androidtv.ui.details.models.*
import java.util.Date
import com.rdwatch.androidtv.network.models.TorrentInfo as NetworkTorrentInfo
import com.rdwatch.androidtv.scraper.api.models.TorrentInfo as ScraperTorrentInfo

/**
 * Mapper to convert between different source models and the advanced SourceMetadata
 */
object SourceMetadataMapper {
    /**
     * Convert existing StreamingSource to SourceMetadata
     */
    fun fromStreamingSource(source: StreamingSource): SourceMetadata {
        // Parse additional info from title if available
        val torrentInfo = source.title?.let { ScraperTorrentInfo.fromTitle(it) }

        return SourceMetadata(
            id = source.id,
            provider =
                SourceProviderInfo(
                    id = source.provider.id,
                    name = source.provider.name,
                    displayName = source.provider.displayName,
                    logoUrl = source.provider.logoUrl,
                    type = mapProviderType(source.sourceType.type),
                    reliability = mapReliability(source.sourceType.reliability),
                    capabilities = source.provider.capabilities.toSet(),
                ),
            quality =
                QualityInfo(
                    resolution = mapResolution(source.quality),
                    hdr10 = torrentInfo?.hdr == true || source.quality.name.contains("HDR"),
                    dolbyVision = source.features.supportsDolbyVision,
                    frameRate = null, // Not available in current model
                ),
            codec =
                CodecInfo(
                    type = torrentInfo?.codec?.let { VideoCodec.fromString(it) } ?: VideoCodec.UNKNOWN,
                ),
            audio =
                AudioInfo(
                    format = torrentInfo?.audio?.let { AudioFormat.fromString(it) } ?: AudioFormat.UNKNOWN,
                    dolbyAtmos = source.features.supportsDolbyAtmos,
                ),
            release =
                ReleaseInfo(
                    type = torrentInfo?.source?.let { ReleaseType.fromString(it) } ?: ReleaseType.UNKNOWN,
                    group = null, // Not available in current model
                    edition = null,
                    year = null,
                ),
            file =
                FileInfo(
                    name = source.title,
                    sizeInBytes = parseSizeToBytes(source.size ?: torrentInfo?.size),
                    extension = null,
                    hash = null,
                    addedDate = source.addedDate?.let { parseISODate(it) },
                ),
            health =
                HealthInfo(
                    seeders = source.features.seeders ?: torrentInfo?.seeders,
                    leechers = source.features.leechers ?: torrentInfo?.leechers,
                    availability = null,
                    lastChecked = source.lastUpdated?.let { parseISODate(it) },
                ),
            features =
                FeatureInfo(
                    subtitles = emptyList(), // Not available in current model
                    has3D = torrentInfo?.is3D == true,
                    hasChapters = false,
                    hasMultipleAudioTracks = false,
                    isDirectPlay = source.sourceType.type == SourceType.ScraperSourceType.DIRECT_LINK,
                    requiresTranscoding = false,
                    supportedDevices = emptySet(),
                ),
            availability =
                AvailabilityInfo(
                    isAvailable = source.isAvailable,
                    region = source.region,
                    expiryDate = null,
                    debridService = null,
                    cached = false,
                ),
            metadata = source.metadata + mapOf("originalUrl" to source.url),
        )
    }

    /**
     * Convert Stremio stream to SourceMetadata
     */
    fun fromStremioStream(
        stream: StremioStream,
        providerId: String,
        providerName: String,
    ): SourceMetadata {
        val torrentInfo = stream.title?.let { ScraperTorrentInfo.fromTitle(it) }
        val displayTitle = stream.getDisplayTitle()

        return SourceMetadata(
            id = "${providerId}_${stream.infoHash ?: stream.url?.hashCode()}",
            provider =
                SourceProviderInfo(
                    id = providerId,
                    name = providerName,
                    displayName = providerName,
                    logoUrl = null,
                    type =
                        if (stream.isTorrent()) {
                            SourceProviderInfo.ProviderType.TORRENT
                        } else {
                            SourceProviderInfo.ProviderType.DIRECT_STREAM
                        },
                    reliability = SourceProviderInfo.ProviderReliability.GOOD,
                    capabilities = setOf("stream"),
                ),
            quality =
                QualityInfo(
                    resolution =
                        torrentInfo?.quality?.let { VideoResolution.fromString(it) }
                            ?: VideoResolution.UNKNOWN,
                    hdr10 = torrentInfo?.hdr == true,
                    dolbyVision = displayTitle.contains("DV") || displayTitle.contains("DOLBY VISION"),
                    frameRate = null,
                ),
            codec =
                CodecInfo(
                    type = torrentInfo?.codec?.let { VideoCodec.fromString(it) } ?: VideoCodec.UNKNOWN,
                ),
            audio =
                AudioInfo(
                    format = torrentInfo?.audio?.let { AudioFormat.fromString(it) } ?: AudioFormat.UNKNOWN,
                    dolbyAtmos = displayTitle.contains("ATMOS"),
                ),
            release =
                ReleaseInfo(
                    type = torrentInfo?.source?.let { ReleaseType.fromString(it) } ?: ReleaseType.UNKNOWN,
                    group = extractReleaseGroup(displayTitle),
                    edition = null,
                    year = null,
                ),
            file =
                FileInfo(
                    name = stream.behaviorHints?.filename ?: displayTitle,
                    sizeInBytes = stream.behaviorHints?.videoSize ?: parseSizeToBytes(torrentInfo?.size),
                    extension = stream.behaviorHints?.filename?.substringAfterLast('.'),
                    hash = stream.infoHash,
                    addedDate = null,
                ),
            health =
                HealthInfo(
                    seeders = torrentInfo?.seeders,
                    leechers = torrentInfo?.leechers,
                    availability = stream.availability?.toFloat(),
                    lastChecked = Date(),
                ),
            features =
                FeatureInfo(
                    subtitles = emptyList(),
                    has3D = torrentInfo?.is3D == true,
                    hasChapters = false,
                    hasMultipleAudioTracks = false,
                    isDirectPlay = !stream.isTorrent(),
                    requiresTranscoding = false,
                    supportedDevices = emptySet(),
                ),
            availability =
                AvailabilityInfo(
                    isAvailable = true,
                    region = null,
                    expiryDate = null,
                    debridService = stream.debridService,
                    cached = stream.debridService != null,
                ),
            metadata =
                buildMap {
                    stream.sources?.forEach { put("tracker_$size", it) }
                    stream.fileIdx?.let { put("fileIdx", it.toString()) }
                },
        )
    }

    /**
     * Convert Real-Debrid torrent info to SourceMetadata
     */
    fun fromRealDebridTorrent(
        torrent: NetworkTorrentInfo,
        providerId: String = "real-debrid",
    ): SourceMetadata {
        val scraperTorrentInfo = torrent.filename.let { ScraperTorrentInfo.fromTitle(it) }

        return SourceMetadata(
            id = torrent.id,
            provider =
                SourceProviderInfo(
                    id = providerId,
                    name = "Real-Debrid",
                    displayName = "Real-Debrid",
                    logoUrl = null,
                    type = SourceProviderInfo.ProviderType.DEBRID,
                    reliability = SourceProviderInfo.ProviderReliability.EXCELLENT,
                    capabilities = setOf("stream", "download", "torrent"),
                ),
            quality =
                QualityInfo(
                    resolution =
                        scraperTorrentInfo.quality?.let { VideoResolution.fromString(it) }
                            ?: VideoResolution.UNKNOWN,
                    hdr10 = scraperTorrentInfo.hdr,
                    dolbyVision = torrent.filename.contains("DV") || torrent.filename.contains("DOLBY VISION"),
                    frameRate = null,
                ),
            codec =
                CodecInfo(
                    type = scraperTorrentInfo.codec?.let { VideoCodec.fromString(it) } ?: VideoCodec.UNKNOWN,
                ),
            audio =
                AudioInfo(
                    format = scraperTorrentInfo.audio?.let { AudioFormat.fromString(it) } ?: AudioFormat.UNKNOWN,
                    dolbyAtmos = torrent.filename.contains("ATMOS"),
                ),
            release =
                ReleaseInfo(
                    type = scraperTorrentInfo.source?.let { ReleaseType.fromString(it) } ?: ReleaseType.UNKNOWN,
                    group = extractReleaseGroup(torrent.filename),
                    edition = null,
                    year = null,
                ),
            file =
                FileInfo(
                    name = torrent.filename,
                    sizeInBytes = torrent.bytes,
                    extension = torrent.filename.substringAfterLast('.'),
                    hash = torrent.hash,
                    addedDate = parseRealDebridDate(torrent.added),
                ),
            health =
                HealthInfo(
                    seeders = torrent.seeders,
                    leechers = null,
                    downloadSpeed = torrent.speed,
                    uploadSpeed = null,
                    availability = torrent.progress / 100f,
                    lastChecked = Date(),
                ),
            features =
                FeatureInfo(
                    subtitles = emptyList(),
                    has3D = scraperTorrentInfo.is3D,
                    hasChapters = false,
                    hasMultipleAudioTracks = false,
                    isDirectPlay = false,
                    requiresTranscoding = false,
                    supportedDevices = emptySet(),
                ),
            availability =
                AvailabilityInfo(
                    isAvailable = torrent.status == "downloaded",
                    region = null,
                    expiryDate = null,
                    debridService = "real-debrid",
                    cached = true,
                ),
            metadata =
                mapOf(
                    // TODO: Fix compilation errors with torrent properties
                    // "status" -> torrent.status,
                    // "host" -> torrent.host,
                    // "progress" -> torrent.progress.toString()
                ),
        )
    }

    /**
     * Convert unrestricted link to SourceMetadata
     */
    fun fromUnrestrictedLink(
        link: UnrestrictLinkResponse,
        torrentInfo: NetworkTorrentInfo? = null,
    ): SourceMetadata {
        val scraperTorrentInfo = link.filename.let { ScraperTorrentInfo.fromTitle(it) }

        return SourceMetadata(
            id = link.id,
            provider =
                SourceProviderInfo(
                    id = "real-debrid",
                    name = "Real-Debrid",
                    displayName = "Real-Debrid (${link.host})",
                    logoUrl = null,
                    type = SourceProviderInfo.ProviderType.DEBRID,
                    reliability = SourceProviderInfo.ProviderReliability.EXCELLENT,
                    capabilities = setOf("stream", "download"),
                ),
            quality =
                QualityInfo(
                    resolution =
                        scraperTorrentInfo.quality?.let { VideoResolution.fromString(it) }
                            ?: VideoResolution.UNKNOWN,
                    hdr10 = scraperTorrentInfo.hdr,
                    dolbyVision = link.filename.contains("DV") || link.filename.contains("DOLBY VISION"),
                    frameRate = null,
                ),
            codec =
                CodecInfo(
                    type = scraperTorrentInfo.codec?.let { VideoCodec.fromString(it) } ?: VideoCodec.UNKNOWN,
                ),
            audio =
                AudioInfo(
                    format = scraperTorrentInfo.audio?.let { AudioFormat.fromString(it) } ?: AudioFormat.UNKNOWN,
                    dolbyAtmos = link.filename.contains("ATMOS"),
                ),
            release =
                ReleaseInfo(
                    type = scraperTorrentInfo.source?.let { ReleaseType.fromString(it) } ?: ReleaseType.UNKNOWN,
                    group = extractReleaseGroup(link.filename),
                    edition = null,
                    year = null,
                ),
            file =
                FileInfo(
                    name = link.filename,
                    sizeInBytes = link.filesize,
                    extension = link.filename.substringAfterLast('.'),
                    hash = null,
                    addedDate = null,
                ),
            health =
                HealthInfo(
                    seeders = torrentInfo?.seeders,
                    leechers = null,
                    downloadSpeed = null,
                    uploadSpeed = null,
                    availability = 1.0f, // Unrestricted links are fully available
                    lastChecked = Date(),
                ),
            features =
                FeatureInfo(
                    subtitles = emptyList(),
                    has3D = scraperTorrentInfo.is3D,
                    hasChapters = false,
                    hasMultipleAudioTracks = false,
                    isDirectPlay = link.streamable == 1,
                    requiresTranscoding = link.streamable != 1,
                    supportedDevices = emptySet(),
                ),
            availability =
                AvailabilityInfo(
                    isAvailable = true,
                    region = null,
                    expiryDate = null,
                    debridService = "real-debrid",
                    cached = true,
                ),
            metadata =
                mapOf(
                    "mimeType" to (link.mimeType ?: ""),
                    // TODO: Fix compilation errors with link properties
                    // "host" to link.host,
                    "downloadUrl" to link.download,
                    "streamUrl" to link.link,
                ),
        )
    }

    // Helper functions

    private fun mapProviderType(type: SourceType.ScraperSourceType): SourceProviderInfo.ProviderType {
        return when (type) {
            SourceType.ScraperSourceType.TORRENT -> SourceProviderInfo.ProviderType.TORRENT
            SourceType.ScraperSourceType.DIRECT_LINK -> SourceProviderInfo.ProviderType.DIRECT_STREAM
            SourceType.ScraperSourceType.MAGNET -> SourceProviderInfo.ProviderType.TORRENT
            SourceType.ScraperSourceType.METADATA -> SourceProviderInfo.ProviderType.METADATA
            SourceType.ScraperSourceType.SUBTITLES -> SourceProviderInfo.ProviderType.SUBTITLE
        }
    }

    private fun mapReliability(reliability: SourceType.SourceReliability): SourceProviderInfo.ProviderReliability {
        return when (reliability) {
            SourceType.SourceReliability.HIGH -> SourceProviderInfo.ProviderReliability.EXCELLENT
            SourceType.SourceReliability.MEDIUM -> SourceProviderInfo.ProviderReliability.GOOD
            SourceType.SourceReliability.LOW -> SourceProviderInfo.ProviderReliability.FAIR
            SourceType.SourceReliability.UNKNOWN -> SourceProviderInfo.ProviderReliability.UNKNOWN
        }
    }

    private fun mapResolution(quality: SourceQuality): VideoResolution {
        return when (quality) {
            SourceQuality.QUALITY_8K -> VideoResolution.RESOLUTION_8K
            SourceQuality.QUALITY_4K, SourceQuality.QUALITY_4K_HDR -> VideoResolution.RESOLUTION_4K
            SourceQuality.QUALITY_1080P, SourceQuality.QUALITY_1080P_HDR -> VideoResolution.RESOLUTION_1080P
            SourceQuality.QUALITY_720P, SourceQuality.QUALITY_720P_HDR -> VideoResolution.RESOLUTION_720P
            SourceQuality.QUALITY_480P -> VideoResolution.RESOLUTION_480P
            SourceQuality.QUALITY_360P -> VideoResolution.RESOLUTION_360P
            SourceQuality.QUALITY_240P -> VideoResolution.RESOLUTION_240P
            SourceQuality.QUALITY_AUTO -> VideoResolution.UNKNOWN
        }
    }

    private fun parseSizeToBytes(sizeStr: String?): Long? {
        if (sizeStr == null) return null

        val regex = Regex("(\\d+\\.?\\d*)\\s?(TB|GB|MB|KB|B)", RegexOption.IGNORE_CASE)
        val match = regex.find(sizeStr) ?: return null

        val value = match.groupValues[1].toDoubleOrNull() ?: return null
        val unit = match.groupValues[2].uppercase()

        return when (unit) {
            "TB" -> (value * 1_000_000_000_000L).toLong()
            "GB" -> (value * 1_000_000_000L).toLong()
            "MB" -> (value * 1_000_000L).toLong()
            "KB" -> (value * 1_000L).toLong()
            "B" -> value.toLong()
            else -> null
        }
    }

    private fun parseISODate(dateStr: String): Date? {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'").parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun parseRealDebridDate(dateStr: String): Date? {
        return try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").parse(dateStr)
        } catch (e: Exception) {
            parseISODate(dateStr)
        }
    }

    private fun extractReleaseGroup(filename: String): String? {
        // Common pattern: Title.Year.Quality.Source-GROUP
        val groupRegex = Regex("-([A-Za-z0-9]+)(?:\\[|\\.|$)")
        return groupRegex.find(filename)?.groupValues?.get(1)
    }
}
