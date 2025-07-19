package com.rdwatch.androidtv.ui.details.models.advanced

import java.util.Date

/**
 * Advanced filtering system for streaming sources
 * Provides comprehensive filtering with AND/OR operations, presets, and performance optimization
 */
class SourceFilterSystem {
    /**
     * Apply comprehensive filter to sources with performance optimization
     */
    fun filterSources(
        sources: List<SourceMetadata>,
        filter: AdvancedSourceFilter,
    ): FilterResult {
        val startTime = System.currentTimeMillis()

        // Early return for empty filter
        if (filter.isEmpty()) {
            return FilterResult(
                filteredSources = sources,
                appliedFilters = emptyList(),
                processingTimeMs = System.currentTimeMillis() - startTime,
                totalSourcesEvaluated = sources.size,
                filtersApplied = 0,
            )
        }

        // Apply filters with short-circuiting for performance
        val filteredSources =
            sources.filter { source ->
                applyFilterCriteria(source, filter)
            }

        // Resolve any filter conflicts
        val resolvedSources = resolveFilterConflicts(filteredSources, filter)

        val endTime = System.currentTimeMillis()

        return FilterResult(
            filteredSources = resolvedSources,
            appliedFilters = getAppliedFilterDescriptions(filter),
            processingTimeMs = endTime - startTime,
            totalSourcesEvaluated = sources.size,
            filtersApplied = filter.getActiveFilterCount(),
        )
    }

    /**
     * Apply all filter criteria to a single source
     */
    private fun applyFilterCriteria(
        source: SourceMetadata,
        filter: AdvancedSourceFilter,
    ): Boolean {
        // Quality filters
        if (!applyQualityFilters(source, filter.qualityFilters)) return false

        // Source type filters
        if (!applySourceTypeFilters(source, filter.sourceTypeFilters)) return false

        // Health filters
        if (!applyHealthFilters(source, filter.healthFilters)) return false

        // File size filters
        if (!applyFileSizeFilters(source, filter.fileSizeFilters)) return false

        // Codec filters
        if (!applyCodecFilters(source, filter.codecFilters)) return false

        // Audio filters
        if (!applyAudioFilters(source, filter.audioFilters)) return false

        // Release type filters
        if (!applyReleaseTypeFilters(source, filter.releaseTypeFilters)) return false

        // Provider filters
        if (!applyProviderFilters(source, filter.providerFilters)) return false

        // Age filters
        if (!applyAgeFilters(source, filter.ageFilters)) return false

        // Custom filters
        if (!applyCustomFilters(source, filter.customFilters)) return false

        return true
    }

    /**
     * Apply quality-related filters
     */
    private fun applyQualityFilters(
        source: SourceMetadata,
        filters: QualityFilters,
    ): Boolean {
        // Minimum resolution
        filters.minResolution?.let { minRes ->
            if (source.quality.resolution.ordinal < minRes.ordinal) return false
        }

        // Maximum resolution
        filters.maxResolution?.let { maxRes ->
            if (source.quality.resolution.ordinal > maxRes.ordinal) return false
        }

        // Specific resolutions (OR operation)
        if (filters.allowedResolutions.isNotEmpty()) {
            if (source.quality.resolution !in filters.allowedResolutions) return false
        }

        // HDR requirements
        if (filters.requireHDR && !source.quality.hasHDR()) return false
        if (filters.require4KOnly && source.quality.resolution != VideoResolution.RESOLUTION_4K) return false

        // HDR type filters
        if (filters.requireDolbyVision && !source.quality.dolbyVision) return false
        if (filters.requireHDR10Plus && !source.quality.hdr10Plus) return false
        if (filters.requireHDR10 && !source.quality.hdr10) return false

        // Bitrate filters
        filters.minBitrate?.let { minBitrate ->
            source.quality.bitrate?.let { bitrate ->
                if (bitrate < minBitrate) return false
            } ?: if (filters.requireBitrateInfo) return false else Unit
        }

        filters.maxBitrate?.let { maxBitrate ->
            source.quality.bitrate?.let { bitrate ->
                if (bitrate > maxBitrate) return false
            }
        }

        // Frame rate filters
        filters.minFrameRate?.let { minFps ->
            source.quality.frameRate?.let { fps ->
                if (fps < minFps) return false
            } ?: if (filters.requireFrameRateInfo) return false else Unit
        }

        filters.maxFrameRate?.let { maxFps ->
            source.quality.frameRate?.let { fps ->
                if (fps > maxFps) return false
            }
        }

        return true
    }

    /**
     * Apply source type filters
     */
    private fun applySourceTypeFilters(
        source: SourceMetadata,
        filters: SourceTypeFilters,
    ): Boolean {
        // Provider type filters
        if (filters.allowedProviderTypes.isNotEmpty()) {
            if (source.provider.type !in filters.allowedProviderTypes) return false
        }

        // Cached only filter
        if (filters.cachedOnly && !source.availability.cached) return false

        // P2P only filter
        if (filters.p2pOnly && source.provider.type != SourceProviderInfo.ProviderType.TORRENT) return false

        // Direct links only filter
        if (filters.directLinksOnly && source.provider.type != SourceProviderInfo.ProviderType.DIRECT_STREAM) return false

        // Debrid service filters
        if (filters.allowedDebridServices.isNotEmpty()) {
            source.availability.debridService?.let { service ->
                if (service !in filters.allowedDebridServices) return false
            } ?: if (filters.requireDebridService) return false else Unit
        }

        // Region filters
        if (filters.allowedRegions.isNotEmpty()) {
            source.availability.region?.let { region ->
                if (region !in filters.allowedRegions) return false
            } ?: if (filters.requireRegionInfo) return false else Unit
        }

        return true
    }

    /**
     * Apply health-related filters for P2P sources
     */
    private fun applyHealthFilters(
        source: SourceMetadata,
        filters: HealthFilters,
    ): Boolean {
        // Minimum seeders
        filters.minSeeders?.let { minSeeders ->
            source.health.seeders?.let { seeders ->
                if (seeders < minSeeders) return false
            } ?: if (filters.requireSeederInfo) return false else Unit
        }

        // Maximum leechers
        filters.maxLeechers?.let { maxLeechers ->
            source.health.leechers?.let { leechers ->
                if (leechers > maxLeechers) return false
            }
        }

        // Seeder/Leecher ratio
        filters.minSeederRatio?.let { minRatio ->
            val seeders = source.health.seeders ?: return if (filters.requireSeederInfo) false else true
            val leechers = source.health.leechers ?: 0

            if (leechers == 0) {
                // No leechers means infinite ratio - should pass
                return true
            }

            val ratio = seeders.toFloat() / leechers
            if (ratio < minRatio) return false
        }

        // Availability percentage
        filters.minAvailability?.let { minAvail ->
            source.health.availability?.let { availability ->
                if (availability < minAvail) return false
            } ?: if (filters.requireAvailabilityInfo) return false else Unit
        }

        // Health status filter
        if (filters.allowedHealthStatuses.isNotEmpty()) {
            val healthStatus = source.health.getHealthStatus()
            if (healthStatus !in filters.allowedHealthStatuses) return false
        }

        return true
    }

    /**
     * Apply file size filters
     */
    private fun applyFileSizeFilters(
        source: SourceMetadata,
        filters: FileSizeFilters,
    ): Boolean {
        source.file.sizeInBytes?.let { sizeBytes ->
            val sizeGB = sizeBytes / (1024.0 * 1024.0 * 1024.0)

            // Size range filters
            filters.minSizeGB?.let { minSize ->
                if (sizeGB < minSize) return false
            }

            filters.maxSizeGB?.let { maxSize ->
                if (sizeGB > maxSize) return false
            }

            // Optimal size range (for quality-to-size ratio)
            filters.optimalSizeRange?.let { (min, max) ->
                if (sizeGB < min || sizeGB > max) return false
            }
        } ?: if (filters.requireSizeInfo) return false else Unit

        return true
    }

    /**
     * Apply codec filters
     */
    private fun applyCodecFilters(
        source: SourceMetadata,
        filters: CodecFilters,
    ): Boolean {
        // Allowed codecs
        if (filters.allowedCodecs.isNotEmpty()) {
            if (source.codec.type !in filters.allowedCodecs) return false
        }

        // Preferred codecs (higher priority)
        if (filters.preferredCodecs.isNotEmpty()) {
            // This is handled in scoring, not filtering
        }

        // HEVC preference
        if (filters.preferHEVC && source.codec.type != VideoCodec.HEVC) {
            // Allow but penalize in scoring
        }

        // AV1 preference
        if (filters.preferAV1 && source.codec.type != VideoCodec.AV1) {
            // Allow but penalize in scoring
        }

        // Exclude specific codecs
        if (filters.excludedCodecs.isNotEmpty()) {
            if (source.codec.type in filters.excludedCodecs) return false
        }

        return true
    }

    /**
     * Apply audio filters
     */
    private fun applyAudioFilters(
        source: SourceMetadata,
        filters: AudioFilters,
    ): Boolean {
        // Allowed audio formats
        if (filters.allowedFormats.isNotEmpty()) {
            if (source.audio.format !in filters.allowedFormats) return false
        }

        // Dolby Atmos requirement
        if (filters.requireDolbyAtmos && !source.audio.dolbyAtmos) return false

        // DTS:X requirement
        if (filters.requireDTSX && !source.audio.dtsX) return false

        // Channel count filters
        filters.minChannels?.let { minChannels ->
            source.audio.channels?.let { channels ->
                val channelCount = extractChannelCount(channels)
                if (channelCount < minChannels) return false
            } ?: if (filters.requireChannelInfo) return false else Unit
        }

        // Audio bitrate filters
        filters.minAudioBitrate?.let { minBitrate ->
            source.audio.bitrate?.let { bitrate ->
                if (bitrate < minBitrate) return false
            } ?: if (filters.requireBitrateInfo) return false else Unit
        }

        // Language filters
        if (filters.allowedLanguages.isNotEmpty()) {
            source.audio.language?.let { language ->
                if (language !in filters.allowedLanguages) return false
            } ?: if (filters.requireLanguageInfo) return false else Unit
        }

        return true
    }

    /**
     * Apply release type filters
     */
    private fun applyReleaseTypeFilters(
        source: SourceMetadata,
        filters: ReleaseTypeFilters,
    ): Boolean {
        // Allowed release types
        if (filters.allowedTypes.isNotEmpty()) {
            if (source.release.type !in filters.allowedTypes) return false
        }

        // Excluded release types
        if (filters.excludedTypes.isNotEmpty()) {
            if (source.release.type in filters.excludedTypes) return false
        }

        // Remux only
        if (filters.remuxOnly && source.release.type != ReleaseType.BLURAY_REMUX) return false

        // Web-DL only
        if (filters.webDLOnly && source.release.type != ReleaseType.WEB_DL) return false

        // Exclude CAM releases
        if (filters.excludeCAM && source.release.type in setOf(ReleaseType.CAM, ReleaseType.HDCAM)) return false

        // Release group filters
        if (filters.allowedGroups.isNotEmpty()) {
            source.release.group?.let { group ->
                if (group.uppercase() !in filters.allowedGroups.map { it.uppercase() }) return false
            } ?: if (filters.requireGroupInfo) return false else Unit
        }

        if (filters.excludedGroups.isNotEmpty()) {
            source.release.group?.let { group ->
                if (group.uppercase() in filters.excludedGroups.map { it.uppercase() }) return false
            }
        }

        return true
    }

    /**
     * Apply provider filters
     */
    private fun applyProviderFilters(
        source: SourceMetadata,
        filters: ProviderFilters,
    ): Boolean {
        // Allowed providers
        if (filters.allowedProviders.isNotEmpty()) {
            if (source.provider.id !in filters.allowedProviders) return false
        }

        // Excluded providers
        if (filters.excludedProviders.isNotEmpty()) {
            if (source.provider.id in filters.excludedProviders) return false
        }

        // Reliability tiers
        if (filters.allowedReliabilityTiers.isNotEmpty()) {
            if (source.provider.reliability !in filters.allowedReliabilityTiers) return false
        }

        // Minimum reliability
        filters.minReliability?.let { minReliability ->
            if (source.provider.reliability.ordinal < minReliability.ordinal) return false
        }

        return true
    }

    /**
     * Apply age/date filters
     */
    private fun applyAgeFilters(
        source: SourceMetadata,
        filters: AgeFilters,
    ): Boolean {
        source.file.addedDate?.let { addedDate ->
            val now = Date()

            // Maximum age
            filters.maxAgeDays?.let { maxDays ->
                val ageInDays = (now.time - addedDate.time) / (24 * 60 * 60 * 1000)
                if (ageInDays > maxDays) return false
            }

            // Date range
            filters.dateRange?.let { (startDate, endDate) ->
                if (addedDate.before(startDate) || addedDate.after(endDate)) return false
            }

            // Recent releases only
            if (filters.recentOnly) {
                val threeDaysAgo = Date(now.time - 3 * 24 * 60 * 60 * 1000)
                if (addedDate.before(threeDaysAgo)) return false
            }
        } ?: if (filters.requireDateInfo) return false else Unit

        return true
    }

    /**
     * Apply custom filters
     */
    private fun applyCustomFilters(
        source: SourceMetadata,
        filters: List<CustomFilter>,
    ): Boolean {
        for (filter in filters) {
            if (!filter.apply(source)) return false
        }
        return true
    }

    /**
     * Resolve filter conflicts and apply conflict resolution strategies
     */
    private fun resolveFilterConflicts(
        sources: List<SourceMetadata>,
        filter: AdvancedSourceFilter,
    ): List<SourceMetadata> {
        // If no sources pass filters, try relaxing some constraints
        if (sources.isEmpty() && filter.conflictResolution.enabled) {
            return attemptConflictResolution(sources, filter)
        }

        return sources
    }

    /**
     * Attempt to resolve filter conflicts by relaxing constraints
     */
    private fun attemptConflictResolution(
        originalSources: List<SourceMetadata>,
        filter: AdvancedSourceFilter,
    ): List<SourceMetadata> {
        val strategies = filter.conflictResolution.strategies

        for (strategy in strategies) {
            when (strategy) {
                ConflictResolutionStrategy.RELAX_QUALITY -> {
                    val relaxedFilter =
                        filter.copy(
                            qualityFilters =
                                filter.qualityFilters.copy(
                                    minResolution =
                                        filter.qualityFilters.minResolution?.let {
                                            VideoResolution.entries.getOrNull(it.ordinal - 1)
                                        },
                                    requireHDR = false,
                                ),
                        )
                    val relaxedResult = filterSources(originalSources, relaxedFilter).filteredSources
                    if (relaxedResult.isNotEmpty()) return relaxedResult
                }

                ConflictResolutionStrategy.RELAX_HEALTH -> {
                    val relaxedFilter =
                        filter.copy(
                            healthFilters =
                                filter.healthFilters.copy(
                                    minSeeders = filter.healthFilters.minSeeders?.let { it / 2 },
                                    minAvailability = filter.healthFilters.minAvailability?.let { it * 0.8f },
                                ),
                        )
                    val relaxedResult = filterSources(originalSources, relaxedFilter).filteredSources
                    if (relaxedResult.isNotEmpty()) return relaxedResult
                }

                ConflictResolutionStrategy.RELAX_SIZE -> {
                    val relaxedFilter =
                        filter.copy(
                            fileSizeFilters =
                                filter.fileSizeFilters.copy(
                                    maxSizeGB = filter.fileSizeFilters.maxSizeGB?.let { it * 1.5 },
                                    minSizeGB = filter.fileSizeFilters.minSizeGB?.let { it * 0.7 },
                                ),
                        )
                    val relaxedResult = filterSources(originalSources, relaxedFilter).filteredSources
                    if (relaxedResult.isNotEmpty()) return relaxedResult
                }

                ConflictResolutionStrategy.DISABLE_STRICT -> {
                    // Disable all strict requirements
                    val relaxedFilter =
                        filter.copy(
                            qualityFilters = filter.qualityFilters.copy(requireHDR = false),
                            sourceTypeFilters = filter.sourceTypeFilters.copy(cachedOnly = false),
                            healthFilters = filter.healthFilters.copy(requireSeederInfo = false),
                            fileSizeFilters = filter.fileSizeFilters.copy(requireSizeInfo = false),
                        )
                    val relaxedResult = filterSources(originalSources, relaxedFilter).filteredSources
                    if (relaxedResult.isNotEmpty()) return relaxedResult
                }

                ConflictResolutionStrategy.EXPAND_PROVIDERS -> {
                    // Allow more provider types
                    val relaxedFilter =
                        filter.copy(
                            providerFilters =
                                filter.providerFilters.copy(
                                    allowedProviders = emptySet(), // Allow all providers
                                    excludedProviders = emptySet(),
                                ),
                        )
                    val relaxedResult = filterSources(originalSources, relaxedFilter).filteredSources
                    if (relaxedResult.isNotEmpty()) return relaxedResult
                }

                ConflictResolutionStrategy.IGNORE_MISSING_INFO -> {
                    // Don't require metadata fields
                    val relaxedFilter =
                        filter.copy(
                            qualityFilters = filter.qualityFilters.copy(requireBitrateInfo = false),
                            healthFilters =
                                filter.healthFilters.copy(
                                    requireSeederInfo = false,
                                    requireAvailabilityInfo = false,
                                ),
                            fileSizeFilters = filter.fileSizeFilters.copy(requireSizeInfo = false),
                            audioFilters =
                                filter.audioFilters.copy(
                                    requireChannelInfo = false,
                                    requireBitrateInfo = false,
                                    requireLanguageInfo = false,
                                ),
                            releaseTypeFilters = filter.releaseTypeFilters.copy(requireGroupInfo = false),
                            ageFilters = filter.ageFilters.copy(requireDateInfo = false),
                        )
                    val relaxedResult = filterSources(originalSources, relaxedFilter).filteredSources
                    if (relaxedResult.isNotEmpty()) return relaxedResult
                }
            }
        }

        return emptyList()
    }

    /**
     * Extract channel count from channel string (e.g., "5.1" -> 6, "7.1" -> 8)
     */
    private fun extractChannelCount(channels: String): Int {
        return when (channels.trim()) {
            "2.0", "Stereo" -> 2
            "5.1" -> 6
            "7.1" -> 8
            "7.1.4", "Atmos" -> 12
            else -> {
                // Try to extract number before first dot
                channels.split(".").firstOrNull()?.toIntOrNull() ?: 2
            }
        }
    }

    /**
     * Get descriptions of applied filters for UI display
     */
    private fun getAppliedFilterDescriptions(filter: AdvancedSourceFilter): List<String> {
        val descriptions = mutableListOf<String>()

        // Quality filter descriptions
        filter.qualityFilters.let { qf ->
            qf.minResolution?.let { descriptions.add("Min: ${it.displayName}") }
            if (qf.requireHDR) descriptions.add("HDR Required")
            if (qf.require4KOnly) descriptions.add("4K Only")
        }

        // Source type descriptions
        filter.sourceTypeFilters.let { stf ->
            if (stf.cachedOnly) descriptions.add("Cached Only")
            if (stf.p2pOnly) descriptions.add("P2P Only")
            if (stf.directLinksOnly) descriptions.add("Direct Links Only")
        }

        // Health filter descriptions
        filter.healthFilters.let { hf ->
            hf.minSeeders?.let { descriptions.add("Min Seeders: $it") }
            hf.minAvailability?.let { descriptions.add("Min Availability: ${(it * 100).toInt()}%") }
        }

        // File size descriptions
        filter.fileSizeFilters.let { fsf ->
            fsf.maxSizeGB?.let { descriptions.add("Max Size: ${it}GB") }
            fsf.minSizeGB?.let { descriptions.add("Min Size: ${it}GB") }
        }

        return descriptions
    }
}

/**
 * Result of filtering operation
 */
data class FilterResult(
    val filteredSources: List<SourceMetadata>,
    val appliedFilters: List<String>,
    val processingTimeMs: Long,
    val totalSourcesEvaluated: Int,
    val filtersApplied: Int,
)

/**
 * Custom filter interface for extensibility
 */
interface CustomFilter {
    val name: String
    val description: String

    fun apply(source: SourceMetadata): Boolean
}
