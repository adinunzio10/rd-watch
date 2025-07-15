package com.rdwatch.androidtv.ui.details.models.advanced

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Date

/**
 * User preferences for filtering and their persistence
 */
@Serializable
data class UserFilterPreferences(
    val defaultFilter: SerializableSourceFilter? = null,
    val savedFilters: Map<String, SerializableSourceFilter> = emptyMap(),
    val favoritePresets: Set<String> = emptySet(),
    val quickFilters: List<String> = emptyList(), // Quick access filters
    val lastUsedFilter: SerializableSourceFilter? = null,
    val autoApplyLastFilter: Boolean = false,
    val conflictResolutionEnabled: Boolean = true,
    val preferredFilterCategories: Set<String> = emptySet(),
    val lastModified: Long = System.currentTimeMillis()
) {
    
    /**
     * Convert to runtime filter
     */
    fun toAdvancedSourceFilter(saved: SerializableSourceFilter): AdvancedSourceFilter {
        return AdvancedSourceFilter(
            qualityFilters = QualityFilters(
                minResolution = saved.minResolution?.let { VideoResolution.valueOf(it) },
                maxResolution = saved.maxResolution?.let { VideoResolution.valueOf(it) },
                allowedResolutions = saved.allowedResolutions.map { VideoResolution.valueOf(it) }.toSet(),
                requireHDR = saved.requireHDR,
                require4KOnly = saved.require4KOnly,
                requireDolbyVision = saved.requireDolbyVision,
                requireHDR10Plus = saved.requireHDR10Plus,
                requireHDR10 = saved.requireHDR10,
                minBitrate = saved.minBitrate,
                maxBitrate = saved.maxBitrate,
                requireBitrateInfo = saved.requireBitrateInfo,
                minFrameRate = saved.minFrameRate,
                maxFrameRate = saved.maxFrameRate,
                requireFrameRateInfo = saved.requireFrameRateInfo
            ),
            sourceTypeFilters = SourceTypeFilters(
                allowedProviderTypes = saved.allowedProviderTypes.map { 
                    SourceProviderInfo.ProviderType.valueOf(it) 
                }.toSet(),
                cachedOnly = saved.cachedOnly,
                p2pOnly = saved.p2pOnly,
                directLinksOnly = saved.directLinksOnly,
                allowedDebridServices = saved.allowedDebridServices,
                requireDebridService = saved.requireDebridService,
                allowedRegions = saved.allowedRegions,
                requireRegionInfo = saved.requireRegionInfo
            ),
            healthFilters = HealthFilters(
                minSeeders = saved.minSeeders,
                maxLeechers = saved.maxLeechers,
                minSeederRatio = saved.minSeederRatio,
                minAvailability = saved.minAvailability,
                allowedHealthStatuses = saved.allowedHealthStatuses.map {
                    HealthInfo.HealthStatus.valueOf(it)
                }.toSet(),
                requireSeederInfo = saved.requireSeederInfo,
                requireAvailabilityInfo = saved.requireAvailabilityInfo
            ),
            fileSizeFilters = FileSizeFilters(
                minSizeGB = saved.minSizeGB,
                maxSizeGB = saved.maxSizeGB,
                optimalSizeRange = saved.optimalSizeRange,
                requireSizeInfo = saved.requireSizeInfo
            ),
            codecFilters = CodecFilters(
                allowedCodecs = saved.allowedCodecs.map { VideoCodec.valueOf(it) }.toSet(),
                preferredCodecs = saved.preferredCodecs.map { VideoCodec.valueOf(it) }.toSet(),
                excludedCodecs = saved.excludedCodecs.map { VideoCodec.valueOf(it) }.toSet(),
                preferHEVC = saved.preferHEVC,
                preferAV1 = saved.preferAV1
            ),
            audioFilters = AudioFilters(
                allowedFormats = saved.allowedFormats.map { AudioFormat.valueOf(it) }.toSet(),
                requireDolbyAtmos = saved.requireDolbyAtmos,
                requireDTSX = saved.requireDTSX,
                minChannels = saved.minChannels,
                minAudioBitrate = saved.minAudioBitrate,
                allowedLanguages = saved.allowedLanguages,
                requireChannelInfo = saved.requireChannelInfo,
                requireBitrateInfo = saved.requireAudioBitrateInfo,
                requireLanguageInfo = saved.requireLanguageInfo
            ),
            releaseTypeFilters = ReleaseTypeFilters(
                allowedTypes = saved.allowedReleaseTypes.map { ReleaseType.valueOf(it) }.toSet(),
                excludedTypes = saved.excludedReleaseTypes.map { ReleaseType.valueOf(it) }.toSet(),
                remuxOnly = saved.remuxOnly,
                webDLOnly = saved.webDLOnly,
                excludeCAM = saved.excludeCAM,
                allowedGroups = saved.allowedGroups,
                excludedGroups = saved.excludedGroups,
                requireGroupInfo = saved.requireGroupInfo
            ),
            providerFilters = ProviderFilters(
                allowedProviders = saved.allowedProviders,
                excludedProviders = saved.excludedProviders,
                allowedReliabilityTiers = saved.allowedReliabilityTiers.map {
                    SourceProviderInfo.ProviderReliability.valueOf(it)
                }.toSet(),
                minReliability = saved.minReliability?.let { 
                    SourceProviderInfo.ProviderReliability.valueOf(it) 
                }
            ),
            ageFilters = AgeFilters(
                maxAgeDays = saved.maxAgeDays,
                dateRange = saved.dateRange?.let { (start, end) -> Pair(Date(start), Date(end)) },
                recentOnly = saved.recentOnly,
                requireDateInfo = saved.requireDateInfo
            ),
            filterCombination = saved.filterCombination?.let { 
                FilterCombination.valueOf(it) 
            } ?: FilterCombination.AND,
            conflictResolution = ConflictResolution(
                enabled = saved.conflictResolutionEnabled,
                strategies = saved.conflictResolutionStrategies.map { 
                    ConflictResolutionStrategy.valueOf(it) 
                }
            )
        )
    }
    
    /**
     * Convert from runtime filter for serialization
     */
    fun fromAdvancedSourceFilter(filter: AdvancedSourceFilter): SerializableSourceFilter {
        return SerializableSourceFilter(
            // Quality filters
            minResolution = filter.qualityFilters.minResolution?.name,
            maxResolution = filter.qualityFilters.maxResolution?.name,
            allowedResolutions = filter.qualityFilters.allowedResolutions.map { it.name }.toSet(),
            requireHDR = filter.qualityFilters.requireHDR,
            require4KOnly = filter.qualityFilters.require4KOnly,
            requireDolbyVision = filter.qualityFilters.requireDolbyVision,
            requireHDR10Plus = filter.qualityFilters.requireHDR10Plus,
            requireHDR10 = filter.qualityFilters.requireHDR10,
            minBitrate = filter.qualityFilters.minBitrate,
            maxBitrate = filter.qualityFilters.maxBitrate,
            requireBitrateInfo = filter.qualityFilters.requireBitrateInfo,
            minFrameRate = filter.qualityFilters.minFrameRate,
            maxFrameRate = filter.qualityFilters.maxFrameRate,
            requireFrameRateInfo = filter.qualityFilters.requireFrameRateInfo,
            
            // Source type filters
            allowedProviderTypes = filter.sourceTypeFilters.allowedProviderTypes.map { it.name }.toSet(),
            cachedOnly = filter.sourceTypeFilters.cachedOnly,
            p2pOnly = filter.sourceTypeFilters.p2pOnly,
            directLinksOnly = filter.sourceTypeFilters.directLinksOnly,
            allowedDebridServices = filter.sourceTypeFilters.allowedDebridServices,
            requireDebridService = filter.sourceTypeFilters.requireDebridService,
            allowedRegions = filter.sourceTypeFilters.allowedRegions,
            requireRegionInfo = filter.sourceTypeFilters.requireRegionInfo,
            
            // Health filters
            minSeeders = filter.healthFilters.minSeeders,
            maxLeechers = filter.healthFilters.maxLeechers,
            minSeederRatio = filter.healthFilters.minSeederRatio,
            minAvailability = filter.healthFilters.minAvailability,
            allowedHealthStatuses = filter.healthFilters.allowedHealthStatuses.map { it.name }.toSet(),
            requireSeederInfo = filter.healthFilters.requireSeederInfo,
            requireAvailabilityInfo = filter.healthFilters.requireAvailabilityInfo,
            
            // File size filters
            minSizeGB = filter.fileSizeFilters.minSizeGB,
            maxSizeGB = filter.fileSizeFilters.maxSizeGB,
            optimalSizeRange = filter.fileSizeFilters.optimalSizeRange,
            requireSizeInfo = filter.fileSizeFilters.requireSizeInfo,
            
            // Codec filters
            allowedCodecs = filter.codecFilters.allowedCodecs.map { it.name }.toSet(),
            preferredCodecs = filter.codecFilters.preferredCodecs.map { it.name }.toSet(),
            excludedCodecs = filter.codecFilters.excludedCodecs.map { it.name }.toSet(),
            preferHEVC = filter.codecFilters.preferHEVC,
            preferAV1 = filter.codecFilters.preferAV1,
            
            // Audio filters
            allowedFormats = filter.audioFilters.allowedFormats.map { it.name }.toSet(),
            requireDolbyAtmos = filter.audioFilters.requireDolbyAtmos,
            requireDTSX = filter.audioFilters.requireDTSX,
            minChannels = filter.audioFilters.minChannels,
            minAudioBitrate = filter.audioFilters.minAudioBitrate,
            allowedLanguages = filter.audioFilters.allowedLanguages,
            requireChannelInfo = filter.audioFilters.requireChannelInfo,
            requireAudioBitrateInfo = filter.audioFilters.requireBitrateInfo,
            requireLanguageInfo = filter.audioFilters.requireLanguageInfo,
            
            // Release type filters
            allowedReleaseTypes = filter.releaseTypeFilters.allowedTypes.map { it.name }.toSet(),
            excludedReleaseTypes = filter.releaseTypeFilters.excludedTypes.map { it.name }.toSet(),
            remuxOnly = filter.releaseTypeFilters.remuxOnly,
            webDLOnly = filter.releaseTypeFilters.webDLOnly,
            excludeCAM = filter.releaseTypeFilters.excludeCAM,
            allowedGroups = filter.releaseTypeFilters.allowedGroups,
            excludedGroups = filter.releaseTypeFilters.excludedGroups,
            requireGroupInfo = filter.releaseTypeFilters.requireGroupInfo,
            
            // Provider filters
            allowedProviders = filter.providerFilters.allowedProviders,
            excludedProviders = filter.providerFilters.excludedProviders,
            allowedReliabilityTiers = filter.providerFilters.allowedReliabilityTiers.map { it.name }.toSet(),
            minReliability = filter.providerFilters.minReliability?.name,
            
            // Age filters
            maxAgeDays = filter.ageFilters.maxAgeDays,
            dateRange = filter.ageFilters.dateRange?.let { (start, end) -> Pair(start.time, end.time) },
            recentOnly = filter.ageFilters.recentOnly,
            requireDateInfo = filter.ageFilters.requireDateInfo,
            
            // Meta filters
            filterCombination = filter.filterCombination.name,
            conflictResolutionEnabled = filter.conflictResolution.enabled,
            conflictResolutionStrategies = filter.conflictResolution.strategies.map { it.name }
        )
    }
}

/**
 * Serializable version of AdvancedSourceFilter for persistence
 */
@Serializable
data class SerializableSourceFilter(
    // Quality filters
    val minResolution: String? = null,
    val maxResolution: String? = null,
    val allowedResolutions: Set<String> = emptySet(),
    val requireHDR: Boolean = false,
    val require4KOnly: Boolean = false,
    val requireDolbyVision: Boolean = false,
    val requireHDR10Plus: Boolean = false,
    val requireHDR10: Boolean = false,
    val minBitrate: Long? = null,
    val maxBitrate: Long? = null,
    val requireBitrateInfo: Boolean = false,
    val minFrameRate: Int? = null,
    val maxFrameRate: Int? = null,
    val requireFrameRateInfo: Boolean = false,
    
    // Source type filters
    val allowedProviderTypes: Set<String> = emptySet(),
    val cachedOnly: Boolean = false,
    val p2pOnly: Boolean = false,
    val directLinksOnly: Boolean = false,
    val allowedDebridServices: Set<String> = emptySet(),
    val requireDebridService: Boolean = false,
    val allowedRegions: Set<String> = emptySet(),
    val requireRegionInfo: Boolean = false,
    
    // Health filters
    val minSeeders: Int? = null,
    val maxLeechers: Int? = null,
    val minSeederRatio: Float? = null,
    val minAvailability: Float? = null,
    val allowedHealthStatuses: Set<String> = emptySet(),
    val requireSeederInfo: Boolean = false,
    val requireAvailabilityInfo: Boolean = false,
    
    // File size filters
    val minSizeGB: Double? = null,
    val maxSizeGB: Double? = null,
    val optimalSizeRange: Pair<Double, Double>? = null,
    val requireSizeInfo: Boolean = false,
    
    // Codec filters
    val allowedCodecs: Set<String> = emptySet(),
    val preferredCodecs: Set<String> = emptySet(),
    val excludedCodecs: Set<String> = emptySet(),
    val preferHEVC: Boolean = false,
    val preferAV1: Boolean = false,
    
    // Audio filters
    val allowedFormats: Set<String> = emptySet(),
    val requireDolbyAtmos: Boolean = false,
    val requireDTSX: Boolean = false,
    val minChannels: Int? = null,
    val minAudioBitrate: Int? = null,
    val allowedLanguages: Set<String> = emptySet(),
    val requireChannelInfo: Boolean = false,
    val requireAudioBitrateInfo: Boolean = false,
    val requireLanguageInfo: Boolean = false,
    
    // Release type filters
    val allowedReleaseTypes: Set<String> = emptySet(),
    val excludedReleaseTypes: Set<String> = emptySet(),
    val remuxOnly: Boolean = false,
    val webDLOnly: Boolean = false,
    val excludeCAM: Boolean = false,
    val allowedGroups: Set<String> = emptySet(),
    val excludedGroups: Set<String> = emptySet(),
    val requireGroupInfo: Boolean = false,
    
    // Provider filters
    val allowedProviders: Set<String> = emptySet(),
    val excludedProviders: Set<String> = emptySet(),
    val allowedReliabilityTiers: Set<String> = emptySet(),
    val minReliability: String? = null,
    
    // Age filters
    val maxAgeDays: Long? = null,
    val dateRange: Pair<Long, Long>? = null,
    val recentOnly: Boolean = false,
    val requireDateInfo: Boolean = false,
    
    // Meta filters
    val filterCombination: String? = null,
    val conflictResolutionEnabled: Boolean = true,
    val conflictResolutionStrategies: List<String> = emptyList()
)

/**
 * Manager for filter preferences and persistence
 */
class FilterPreferencesManager {
    private val _preferences = MutableStateFlow(UserFilterPreferences())
    val preferences: StateFlow<UserFilterPreferences> = _preferences.asStateFlow()
    
    private val json = Json { 
        ignoreUnknownKeys = true
        prettyPrint = true
    }
    
    /**
     * Save a custom filter
     */
    fun saveFilter(name: String, filter: AdvancedSourceFilter) {
        val serializable = _preferences.value.fromAdvancedSourceFilter(filter)
        val updated = _preferences.value.copy(
            savedFilters = _preferences.value.savedFilters + (name to serializable),
            lastModified = System.currentTimeMillis()
        )
        _preferences.value = updated
    }
    
    /**
     * Delete a saved filter
     */
    fun deleteFilter(name: String) {
        val updated = _preferences.value.copy(
            savedFilters = _preferences.value.savedFilters - name,
            lastModified = System.currentTimeMillis()
        )
        _preferences.value = updated
    }
    
    /**
     * Set default filter
     */
    fun setDefaultFilter(filter: AdvancedSourceFilter?) {
        val serializable = filter?.let { _preferences.value.fromAdvancedSourceFilter(it) }
        val updated = _preferences.value.copy(
            defaultFilter = serializable,
            lastModified = System.currentTimeMillis()
        )
        _preferences.value = updated
    }
    
    /**
     * Update last used filter
     */
    fun updateLastUsedFilter(filter: AdvancedSourceFilter) {
        val serializable = _preferences.value.fromAdvancedSourceFilter(filter)
        val updated = _preferences.value.copy(
            lastUsedFilter = serializable,
            lastModified = System.currentTimeMillis()
        )
        _preferences.value = updated
    }
    
    /**
     * Add/remove favorite preset
     */
    fun toggleFavoritePreset(presetName: String) {
        val favorites = _preferences.value.favoritePresets.toMutableSet()
        if (presetName in favorites) {
            favorites.remove(presetName)
        } else {
            favorites.add(presetName)
        }
        
        val updated = _preferences.value.copy(
            favoritePresets = favorites,
            lastModified = System.currentTimeMillis()
        )
        _preferences.value = updated
    }
    
    /**
     * Update quick filters list
     */
    fun updateQuickFilters(quickFilters: List<String>) {
        val updated = _preferences.value.copy(
            quickFilters = quickFilters,
            lastModified = System.currentTimeMillis()
        )
        _preferences.value = updated
    }
    
    /**
     * Toggle auto-apply last filter
     */
    fun setAutoApplyLastFilter(enabled: Boolean) {
        val updated = _preferences.value.copy(
            autoApplyLastFilter = enabled,
            lastModified = System.currentTimeMillis()
        )
        _preferences.value = updated
    }
    
    /**
     * Toggle conflict resolution
     */
    fun setConflictResolutionEnabled(enabled: Boolean) {
        val updated = _preferences.value.copy(
            conflictResolutionEnabled = enabled,
            lastModified = System.currentTimeMillis()
        )
        _preferences.value = updated
    }
    
    /**
     * Get default filter or last used filter
     */
    fun getInitialFilter(): AdvancedSourceFilter? {
        val prefs = _preferences.value
        return when {
            prefs.autoApplyLastFilter && prefs.lastUsedFilter != null -> 
                prefs.toAdvancedSourceFilter(prefs.lastUsedFilter)
            prefs.defaultFilter != null -> 
                prefs.toAdvancedSourceFilter(prefs.defaultFilter)
            else -> null
        }
    }
    
    /**
     * Get all saved filters
     */
    fun getSavedFilters(): Map<String, AdvancedSourceFilter> {
        return _preferences.value.savedFilters.mapValues { (_, filter) ->
            _preferences.value.toAdvancedSourceFilter(filter)
        }
    }
    
    /**
     * Get favorite presets with their filters
     */
    fun getFavoritePresets(): Map<String, AdvancedSourceFilter> {
        val allPresets = FilterPresets.getAllPresets()
        return _preferences.value.favoritePresets.mapNotNull { presetName ->
            allPresets[presetName]?.let { presetName to it }
        }.toMap()
    }
    
    /**
     * Export preferences to JSON string
     */
    fun exportPreferences(): String {
        return json.encodeToString(UserFilterPreferences.serializer(), _preferences.value)
    }
    
    /**
     * Import preferences from JSON string
     */
    fun importPreferences(jsonString: String): Boolean {
        return try {
            val imported = json.decodeFromString(UserFilterPreferences.serializer(), jsonString)
            _preferences.value = imported.copy(lastModified = System.currentTimeMillis())
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Reset all preferences to default
     */
    fun resetPreferences() {
        _preferences.value = UserFilterPreferences()
    }
}