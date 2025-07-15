package com.rdwatch.androidtv.ui.details.models.advanced

import java.util.Date

/**
 * Advanced filter model with comprehensive filtering options
 */
data class AdvancedSourceFilter(
    val qualityFilters: QualityFilters = QualityFilters(),
    val sourceTypeFilters: SourceTypeFilters = SourceTypeFilters(),
    val healthFilters: HealthFilters = HealthFilters(),
    val fileSizeFilters: FileSizeFilters = FileSizeFilters(),
    val codecFilters: CodecFilters = CodecFilters(),
    val audioFilters: AudioFilters = AudioFilters(),
    val releaseTypeFilters: ReleaseTypeFilters = ReleaseTypeFilters(),
    val providerFilters: ProviderFilters = ProviderFilters(),
    val ageFilters: AgeFilters = AgeFilters(),
    val customFilters: List<CustomFilter> = emptyList(),
    val filterCombination: FilterCombination = FilterCombination.AND,
    val conflictResolution: ConflictResolution = ConflictResolution()
) {
    
    /**
     * Check if filter is empty (no criteria set)
     */
    fun isEmpty(): Boolean {
        return qualityFilters.isEmpty() &&
                sourceTypeFilters.isEmpty() &&
                healthFilters.isEmpty() &&
                fileSizeFilters.isEmpty() &&
                codecFilters.isEmpty() &&
                audioFilters.isEmpty() &&
                releaseTypeFilters.isEmpty() &&
                providerFilters.isEmpty() &&
                ageFilters.isEmpty() &&
                customFilters.isEmpty()
    }
    
    /**
     * Get count of active filters
     */
    fun getActiveFilterCount(): Int {
        var count = 0
        if (!qualityFilters.isEmpty()) count++
        if (!sourceTypeFilters.isEmpty()) count++
        if (!healthFilters.isEmpty()) count++
        if (!fileSizeFilters.isEmpty()) count++
        if (!codecFilters.isEmpty()) count++
        if (!audioFilters.isEmpty()) count++
        if (!releaseTypeFilters.isEmpty()) count++
        if (!providerFilters.isEmpty()) count++
        if (!ageFilters.isEmpty()) count++
        count += customFilters.size
        return count
    }
    
    /**
     * Create a summary of active filters
     */
    fun getSummary(): String {
        val parts = mutableListOf<String>()
        
        if (!qualityFilters.isEmpty()) {
            qualityFilters.minResolution?.let { parts.add("Min: ${it.displayName}") }
            if (qualityFilters.requireHDR) parts.add("HDR")
        }
        
        if (!sourceTypeFilters.isEmpty()) {
            if (sourceTypeFilters.cachedOnly) parts.add("Cached")
            if (sourceTypeFilters.p2pOnly) parts.add("P2P")
        }
        
        if (!healthFilters.isEmpty()) {
            healthFilters.minSeeders?.let { parts.add("≥${it}S") }
        }
        
        if (!fileSizeFilters.isEmpty()) {
            fileSizeFilters.maxSizeGB?.let { parts.add("≤${it}GB") }
        }
        
        return if (parts.isEmpty()) "No filters" else parts.joinToString(", ")
    }
}

/**
 * Quality-related filters
 */
data class QualityFilters(
    val minResolution: VideoResolution? = null,
    val maxResolution: VideoResolution? = null,
    val allowedResolutions: Set<VideoResolution> = emptySet(),
    val requireHDR: Boolean = false,
    val require4KOnly: Boolean = false,
    val requireDolbyVision: Boolean = false,
    val requireHDR10Plus: Boolean = false,
    val requireHDR10: Boolean = false,
    val minBitrate: Long? = null, // in bps
    val maxBitrate: Long? = null,
    val requireBitrateInfo: Boolean = false,
    val minFrameRate: Int? = null,
    val maxFrameRate: Int? = null,
    val requireFrameRateInfo: Boolean = false
) {
    fun isEmpty(): Boolean = minResolution == null && maxResolution == null && 
                            allowedResolutions.isEmpty() && !requireHDR && !require4KOnly &&
                            !requireDolbyVision && !requireHDR10Plus && !requireHDR10 &&
                            minBitrate == null && maxBitrate == null && 
                            minFrameRate == null && maxFrameRate == null
}

/**
 * Source type filters
 */
data class SourceTypeFilters(
    val allowedProviderTypes: Set<SourceProviderInfo.ProviderType> = emptySet(),
    val cachedOnly: Boolean = false,
    val p2pOnly: Boolean = false,
    val directLinksOnly: Boolean = false,
    val allowedDebridServices: Set<String> = emptySet(),
    val requireDebridService: Boolean = false,
    val allowedRegions: Set<String> = emptySet(),
    val requireRegionInfo: Boolean = false
) {
    fun isEmpty(): Boolean = allowedProviderTypes.isEmpty() && !cachedOnly && !p2pOnly && 
                            !directLinksOnly && allowedDebridServices.isEmpty() && 
                            allowedRegions.isEmpty()
}

/**
 * Health-related filters for P2P sources
 */
data class HealthFilters(
    val minSeeders: Int? = null,
    val maxLeechers: Int? = null,
    val minSeederRatio: Float? = null, // seeders/leechers ratio
    val minAvailability: Float? = null, // 0.0 to 1.0
    val allowedHealthStatuses: Set<HealthInfo.HealthStatus> = emptySet(),
    val requireSeederInfo: Boolean = false,
    val requireAvailabilityInfo: Boolean = false
) {
    fun isEmpty(): Boolean = minSeeders == null && maxLeechers == null && 
                            minSeederRatio == null && minAvailability == null && 
                            allowedHealthStatuses.isEmpty()
}

/**
 * File size filters
 */
data class FileSizeFilters(
    val minSizeGB: Double? = null,
    val maxSizeGB: Double? = null,
    val optimalSizeRange: Pair<Double, Double>? = null, // (min, max) for quality-to-size ratio
    val requireSizeInfo: Boolean = false
) {
    fun isEmpty(): Boolean = minSizeGB == null && maxSizeGB == null && optimalSizeRange == null
}

/**
 * Codec filters
 */
data class CodecFilters(
    val allowedCodecs: Set<VideoCodec> = emptySet(),
    val preferredCodecs: Set<VideoCodec> = emptySet(), // For scoring boost, not filtering
    val excludedCodecs: Set<VideoCodec> = emptySet(),
    val preferHEVC: Boolean = false,
    val preferAV1: Boolean = false
) {
    fun isEmpty(): Boolean = allowedCodecs.isEmpty() && preferredCodecs.isEmpty() && 
                            excludedCodecs.isEmpty() && !preferHEVC && !preferAV1
}

/**
 * Audio filters
 */
data class AudioFilters(
    val allowedFormats: Set<AudioFormat> = emptySet(),
    val requireDolbyAtmos: Boolean = false,
    val requireDTSX: Boolean = false,
    val minChannels: Int? = null, // Minimum channel count
    val minAudioBitrate: Int? = null, // in kbps
    val allowedLanguages: Set<String> = emptySet(),
    val requireChannelInfo: Boolean = false,
    val requireBitrateInfo: Boolean = false,
    val requireLanguageInfo: Boolean = false
) {
    fun isEmpty(): Boolean = allowedFormats.isEmpty() && !requireDolbyAtmos && !requireDTSX &&
                            minChannels == null && minAudioBitrate == null && 
                            allowedLanguages.isEmpty()
}

/**
 * Release type filters
 */
data class ReleaseTypeFilters(
    val allowedTypes: Set<ReleaseType> = emptySet(),
    val excludedTypes: Set<ReleaseType> = emptySet(),
    val remuxOnly: Boolean = false,
    val webDLOnly: Boolean = false,
    val excludeCAM: Boolean = false,
    val allowedGroups: Set<String> = emptySet(),
    val excludedGroups: Set<String> = emptySet(),
    val requireGroupInfo: Boolean = false
) {
    fun isEmpty(): Boolean = allowedTypes.isEmpty() && excludedTypes.isEmpty() && 
                            !remuxOnly && !webDLOnly && !excludeCAM &&
                            allowedGroups.isEmpty() && excludedGroups.isEmpty()
}

/**
 * Provider filters
 */
data class ProviderFilters(
    val allowedProviders: Set<String> = emptySet(),
    val excludedProviders: Set<String> = emptySet(),
    val allowedReliabilityTiers: Set<SourceProviderInfo.ProviderReliability> = emptySet(),
    val minReliability: SourceProviderInfo.ProviderReliability? = null
) {
    fun isEmpty(): Boolean = allowedProviders.isEmpty() && excludedProviders.isEmpty() && 
                            allowedReliabilityTiers.isEmpty() && minReliability == null
}

/**
 * Age/date filters
 */
data class AgeFilters(
    val maxAgeDays: Long? = null,
    val dateRange: Pair<Date, Date>? = null, // (start, end)
    val recentOnly: Boolean = false, // Last 3 days
    val requireDateInfo: Boolean = false
) {
    fun isEmpty(): Boolean = maxAgeDays == null && dateRange == null && !recentOnly
}

/**
 * Filter combination logic
 */
enum class FilterCombination {
    AND,    // All filters must pass (default)
    OR      // Any filter can pass
}

/**
 * Conflict resolution configuration
 */
data class ConflictResolution(
    val enabled: Boolean = true,
    val strategies: List<ConflictResolutionStrategy> = listOf(
        ConflictResolutionStrategy.RELAX_QUALITY,
        ConflictResolutionStrategy.RELAX_HEALTH,
        ConflictResolutionStrategy.RELAX_SIZE,
        ConflictResolutionStrategy.DISABLE_STRICT
    )
)

/**
 * Strategies for resolving filter conflicts
 */
enum class ConflictResolutionStrategy {
    RELAX_QUALITY,      // Lower quality requirements
    RELAX_HEALTH,       // Lower health requirements  
    RELAX_SIZE,         // Increase size tolerance
    DISABLE_STRICT,     // Disable strict requirements
    EXPAND_PROVIDERS,   // Include more providers
    IGNORE_MISSING_INFO // Don't require metadata fields
}