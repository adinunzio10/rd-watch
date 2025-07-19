package com.rdwatch.androidtv.ui.details.models.advanced

/**
 * Predefined filter presets for common user scenarios
 */
object FilterPresets {
    /**
     * Best quality preset - prioritizes highest quality sources
     */
    val BEST_QUALITY =
        AdvancedSourceFilter(
            qualityFilters =
                QualityFilters(
                    minResolution = VideoResolution.RESOLUTION_1080P,
                    requireHDR = false, // Don't require but prefer
                ),
            codecFilters =
                CodecFilters(
                    allowedCodecs = setOf(VideoCodec.AV1, VideoCodec.HEVC, VideoCodec.H264),
                    preferredCodecs = setOf(VideoCodec.AV1, VideoCodec.HEVC),
                ),
            releaseTypeFilters =
                ReleaseTypeFilters(
                    allowedTypes =
                        setOf(
                            ReleaseType.BLURAY_REMUX,
                            ReleaseType.BLURAY,
                            ReleaseType.WEB_DL,
                            ReleaseType.WEBRIP,
                        ),
                    excludeCAM = true,
                ),
            audioFilters =
                AudioFilters(
                    allowedFormats =
                        setOf(
                            AudioFormat.TRUEHD,
                            AudioFormat.DTS_HD_MA,
                            AudioFormat.DTS_HD,
                            AudioFormat.EAC3,
                            AudioFormat.FLAC,
                        ),
                ),
        )

    /**
     * HDR only preset - filters for HDR content exclusively
     */
    val HDR_ONLY =
        AdvancedSourceFilter(
            qualityFilters =
                QualityFilters(
                    requireHDR = true,
                    minResolution = VideoResolution.RESOLUTION_1080P,
                ),
            codecFilters =
                CodecFilters(
                    allowedCodecs = setOf(VideoCodec.AV1, VideoCodec.HEVC), // Only codecs that support HDR
                    preferHEVC = true,
                ),
            releaseTypeFilters =
                ReleaseTypeFilters(
                    excludeCAM = true,
                ),
        )

    /**
     * 4K UHD preset - 4K content with HDR preference
     */
    val UHD_4K =
        AdvancedSourceFilter(
            qualityFilters =
                QualityFilters(
                    require4KOnly = true,
                    minResolution = VideoResolution.RESOLUTION_4K,
                ),
            codecFilters =
                CodecFilters(
                    preferredCodecs = setOf(VideoCodec.AV1, VideoCodec.HEVC), // Better compression for 4K
                    preferHEVC = true,
                ),
            fileSizeFilters =
                FileSizeFilters(
                    minSizeGB = 15.0, // 4K should be substantial size
                ),
            releaseTypeFilters =
                ReleaseTypeFilters(
                    allowedTypes =
                        setOf(
                            ReleaseType.BLURAY_REMUX,
                            ReleaseType.BLURAY,
                            ReleaseType.WEB_DL,
                        ),
                ),
        )

    /**
     * Cached only preset - instant play sources
     */
    val CACHED_ONLY =
        AdvancedSourceFilter(
            sourceTypeFilters =
                SourceTypeFilters(
                    cachedOnly = true,
                ),
            qualityFilters =
                QualityFilters(
                    minResolution = VideoResolution.RESOLUTION_720P, // Accept lower quality for instant play
                ),
        )

    /**
     * Small file preset - for limited bandwidth/storage
     */
    val SMALL_FILES =
        AdvancedSourceFilter(
            fileSizeFilters =
                FileSizeFilters(
                    maxSizeGB = 5.0,
                    optimalSizeRange = Pair(2.0, 5.0),
                ),
            qualityFilters =
                QualityFilters(
                    maxResolution = VideoResolution.RESOLUTION_1080P, // Reasonable quality for small files
                ),
            codecFilters =
                CodecFilters(
                    preferredCodecs = setOf(VideoCodec.HEVC, VideoCodec.AV1), // Better compression
                    preferHEVC = true,
                ),
            releaseTypeFilters =
                ReleaseTypeFilters(
                    excludedTypes = setOf(ReleaseType.BLURAY_REMUX), // Usually too large
                    excludeCAM = true,
                ),
        )

    /**
     * High seeder P2P preset - reliable torrents
     */
    val HIGH_SEEDERS =
        AdvancedSourceFilter(
            sourceTypeFilters =
                SourceTypeFilters(
                    p2pOnly = true,
                ),
            healthFilters =
                HealthFilters(
                    minSeeders = 50,
                    minSeederRatio = 2.0f,
                    minAvailability = 0.8f,
                    allowedHealthStatuses =
                        setOf(
                            HealthInfo.HealthStatus.EXCELLENT,
                            HealthInfo.HealthStatus.GOOD,
                        ),
                ),
            providerFilters =
                ProviderFilters(
                    minReliability = SourceProviderInfo.ProviderReliability.GOOD,
                ),
        )

    /**
     * TV optimized preset - good for Android TV playback
     */
    val TV_OPTIMIZED =
        AdvancedSourceFilter(
            qualityFilters =
                QualityFilters(
                    minResolution = VideoResolution.RESOLUTION_1080P,
                    maxResolution = VideoResolution.RESOLUTION_4K, // Don't go beyond TV capabilities
                ),
            codecFilters =
                CodecFilters(
                    allowedCodecs = setOf(VideoCodec.H264, VideoCodec.HEVC), // Most compatible
                    preferHEVC = true,
                ),
            sourceTypeFilters =
                SourceTypeFilters(
                    cachedOnly = true, // Prefer instant play for TV
                ),
            fileSizeFilters =
                FileSizeFilters(
                    maxSizeGB = 25.0, // Reasonable for TV storage
                    optimalSizeRange = Pair(5.0, 15.0),
                ),
            audioFilters =
                AudioFilters(
                    allowedFormats =
                        setOf(
                            AudioFormat.EAC3,
                            AudioFormat.AC3,
                            AudioFormat.AAC,
                            AudioFormat.DTS,
                        ),
                    // TV-compatible formats
                    minChannels = 2,
                ),
            releaseTypeFilters =
                ReleaseTypeFilters(
                    excludeCAM = true,
                ),
        )

    /**
     * Audiophile preset - best audio quality
     */
    val AUDIOPHILE =
        AdvancedSourceFilter(
            audioFilters =
                AudioFilters(
                    allowedFormats =
                        setOf(
                            AudioFormat.TRUEHD,
                            AudioFormat.DTS_HD_MA,
                            AudioFormat.FLAC,
                            AudioFormat.PCM,
                        ),
                    minChannels = 6, // At least 5.1
                    minAudioBitrate = 1000, // High bitrate audio
                ),
            qualityFilters =
                QualityFilters(
                    minResolution = VideoResolution.RESOLUTION_1080P,
                ),
            releaseTypeFilters =
                ReleaseTypeFilters(
                    allowedTypes =
                        setOf(
                            ReleaseType.BLURAY_REMUX,
                            ReleaseType.BLURAY,
                        ), // Best audio tracks
                ),
            fileSizeFilters =
                FileSizeFilters(
                    minSizeGB = 10.0, // Larger files for better audio
                ),
        )

    /**
     * Quick download preset - fast to download and watch
     */
    val QUICK_DOWNLOAD =
        AdvancedSourceFilter(
            fileSizeFilters =
                FileSizeFilters(
                    maxSizeGB = 3.0,
                ),
            healthFilters =
                HealthFilters(
                    minSeeders = 100, // Very healthy for fast download
                    allowedHealthStatuses = setOf(HealthInfo.HealthStatus.EXCELLENT),
                ),
            qualityFilters =
                QualityFilters(
                    maxResolution = VideoResolution.RESOLUTION_1080P,
                ),
            codecFilters =
                CodecFilters(
                    preferredCodecs = setOf(VideoCodec.H264), // Fastest to encode/decode
                ),
            releaseTypeFilters =
                ReleaseTypeFilters(
                    allowedTypes = setOf(ReleaseType.WEBRIP, ReleaseType.WEB_DL),
                    excludeCAM = true,
                ),
        )

    /**
     * Archive quality preset - long-term storage
     */
    val ARCHIVE_QUALITY =
        AdvancedSourceFilter(
            qualityFilters =
                QualityFilters(
                    minResolution = VideoResolution.RESOLUTION_1080P,
                ),
            releaseTypeFilters =
                ReleaseTypeFilters(
                    allowedTypes =
                        setOf(
                            ReleaseType.BLURAY_REMUX,
                            ReleaseType.BLURAY,
                        ),
                ),
            codecFilters =
                CodecFilters(
                    allowedCodecs = setOf(VideoCodec.AV1, VideoCodec.HEVC), // Future-proof codecs
                    preferAV1 = true,
                ),
            fileSizeFilters =
                FileSizeFilters(
                    minSizeGB = 8.0, // Substantial quality
                ),
            audioFilters =
                AudioFilters(
                    allowedFormats =
                        setOf(
                            AudioFormat.TRUEHD,
                            AudioFormat.DTS_HD_MA,
                            AudioFormat.FLAC,
                        ),
                ),
        )

    /**
     * Get all available presets
     */
    fun getAllPresets(): Map<String, AdvancedSourceFilter> {
        return mapOf(
            "Best Quality" to BEST_QUALITY,
            "HDR Only" to HDR_ONLY,
            "4K UHD" to UHD_4K,
            "Cached Only" to CACHED_ONLY,
            "Small Files" to SMALL_FILES,
            "High Seeders" to HIGH_SEEDERS,
            "TV Optimized" to TV_OPTIMIZED,
            "Audiophile" to AUDIOPHILE,
            "Quick Download" to QUICK_DOWNLOAD,
            "Archive Quality" to ARCHIVE_QUALITY,
        )
    }

    /**
     * Get preset by category
     */
    fun getPresetsByCategory(): Map<PresetCategory, List<Pair<String, AdvancedSourceFilter>>> {
        return mapOf(
            PresetCategory.QUALITY to
                listOf(
                    "Best Quality" to BEST_QUALITY,
                    "HDR Only" to HDR_ONLY,
                    "4K UHD" to UHD_4K,
                    "Archive Quality" to ARCHIVE_QUALITY,
                ),
            PresetCategory.SIZE to
                listOf(
                    "Small Files" to SMALL_FILES,
                    "Quick Download" to QUICK_DOWNLOAD,
                ),
            PresetCategory.SPEED to
                listOf(
                    "Cached Only" to CACHED_ONLY,
                    "High Seeders" to HIGH_SEEDERS,
                    "Quick Download" to QUICK_DOWNLOAD,
                ),
            PresetCategory.DEVICE to
                listOf(
                    "TV Optimized" to TV_OPTIMIZED,
                ),
            PresetCategory.AUDIO to
                listOf(
                    "Audiophile" to AUDIOPHILE,
                ),
        )
    }

    /**
     * Create a custom preset based on user device capabilities
     */
    fun createDeviceOptimizedPreset(
        maxResolution: VideoResolution = VideoResolution.RESOLUTION_4K,
        supportsHDR: Boolean = true,
        supportedCodecs: Set<VideoCodec> = setOf(VideoCodec.H264, VideoCodec.HEVC),
        maxFileSizeGB: Double = 20.0,
        preferCached: Boolean = true,
    ): AdvancedSourceFilter {
        return AdvancedSourceFilter(
            qualityFilters =
                QualityFilters(
                    maxResolution = maxResolution,
                    requireHDR = false, // Don't require but allow if supported
                ),
            codecFilters =
                CodecFilters(
                    allowedCodecs = supportedCodecs,
                    preferHEVC = VideoCodec.HEVC in supportedCodecs,
                ),
            fileSizeFilters =
                FileSizeFilters(
                    maxSizeGB = maxFileSizeGB,
                ),
            sourceTypeFilters =
                SourceTypeFilters(
                    cachedOnly = preferCached,
                ),
            releaseTypeFilters =
                ReleaseTypeFilters(
                    excludeCAM = true,
                ),
        )
    }
}

/**
 * Categories for organizing presets
 */
enum class PresetCategory(val displayName: String) {
    QUALITY("Quality"),
    SIZE("File Size"),
    SPEED("Speed"),
    DEVICE("Device"),
    AUDIO("Audio"),
}
