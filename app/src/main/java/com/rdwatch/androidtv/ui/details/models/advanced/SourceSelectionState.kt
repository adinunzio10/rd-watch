package com.rdwatch.androidtv.ui.details.models.advanced

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import com.rdwatch.androidtv.ui.details.models.SourceSortOption
import com.rdwatch.androidtv.ui.details.models.TVEpisode

/**
 * UI state for advanced source selection
 */
data class SourceSelectionState(
    val sources: List<SourceMetadata> = emptyList(),
    val filteredSources: List<SourceMetadata> = emptyList(),
    val selectedSource: SourceMetadata? = null,
    val selectedEpisode: TVEpisode? = null,
    val filter: SourceFilter = SourceFilter(),
    val sortOption: SourceSortOption = SourceSortOption.QUALITY_SCORE,
    val isLoading: Boolean = false,
    val error: String? = null,
    val expandedGroups: Set<String> = emptySet(), // For grouping by provider/quality
    val viewMode: ViewMode = ViewMode.GRID
) {
    enum class ViewMode {
        GRID,      // Grid view with cards
        LIST,      // Detailed list view
        COMPACT    // Compact list for many sources
    }
    
    /**
     * Get sources grouped by provider
     */
    fun getSourcesByProvider(): Map<SourceProviderInfo, List<SourceMetadata>> {
        return filteredSources.groupBy { it.provider }
    }
    
    /**
     * Get sources grouped by quality
     */
    fun getSourcesByQuality(): Map<VideoResolution, List<SourceMetadata>> {
        return filteredSources.groupBy { it.quality.resolution }
            .toSortedMap(compareByDescending { it.baseScore })
    }
    
    /**
     * Get quick filter suggestions based on available sources
     */
    fun getQuickFilters(): QuickFilters {
        return QuickFilters(
            hasHDR = sources.any { it.quality.hasHDR() },
            has4K = sources.any { it.quality.resolution == VideoResolution.RESOLUTION_4K },
            has1080p = sources.any { it.quality.resolution == VideoResolution.RESOLUTION_1080P },
            hasDolbyVision = sources.any { it.quality.dolbyVision },
            hasDolbyAtmos = sources.any { it.audio.dolbyAtmos },
            hasP2P = sources.any { it.provider.type == SourceProviderInfo.ProviderType.TORRENT },
            hasCached = sources.any { it.availability.cached },
            providers = sources.map { it.provider }.distinctBy { it.id },
            codecs = sources.map { it.codec.type }.distinct().filter { it != VideoCodec.UNKNOWN },
            releaseTypes = sources.map { it.release.type }.distinct().filter { it != ReleaseType.UNKNOWN }
        )
    }
    
    /**
     * Get statistics about sources
     */
    fun getStatistics(): SourceStatistics {
        return SourceStatistics(
            totalSources = sources.size,
            filteredSources = filteredSources.size,
            cachedSources = sources.count { it.availability.cached },
            p2pSources = sources.count { it.provider.type == SourceProviderInfo.ProviderType.TORRENT },
            hdrSources = sources.count { it.quality.hasHDR() },
            highQualitySources = sources.count { 
                it.quality.resolution.baseScore >= VideoResolution.RESOLUTION_1080P.baseScore 
            },
            averageFileSize = sources.mapNotNull { it.file.sizeInBytes }.average().takeIf { it.isFinite() },
            providerCount = sources.map { it.provider.id }.distinct().size
        )
    }
}

/**
 * Quick filter options based on available sources
 */
data class QuickFilters(
    val hasHDR: Boolean,
    val has4K: Boolean,
    val has1080p: Boolean,
    val hasDolbyVision: Boolean,
    val hasDolbyAtmos: Boolean,
    val hasP2P: Boolean,
    val hasCached: Boolean,
    val providers: List<SourceProviderInfo>,
    val codecs: List<VideoCodec>,
    val releaseTypes: List<ReleaseType>
)

/**
 * Statistics about available sources
 */
data class SourceStatistics(
    val totalSources: Int,
    val filteredSources: Int,
    val cachedSources: Int,
    val p2pSources: Int,
    val hdrSources: Int,
    val highQualitySources: Int,
    val averageFileSize: Double?,
    val providerCount: Int
)

/**
 * Manager for source selection state with advanced sorting
 */
class SourceSelectionManager {
    private val _state = MutableStateFlow(SourceSelectionState())
    val state: StateFlow<SourceSelectionState> = _state.asStateFlow()
    
    // Advanced sorting and filtering components
    private val sourceSorter = SourceSorter()
    private val performanceOptimizer = SourcePerformanceOptimizer()
    private val filterSystem = SourceFilterSystem()
    private val filterOptimizer = FilterPerformanceOptimizer()
    private var userPreferences = UserSortingPreferences()
    
    /**
     * Update sources and apply current filter with smart sorting
     */
    suspend fun updateSources(sources: List<SourceMetadata>) {
        _state.value = _state.value.copy(isLoading = true)
        
        try {
            // Use performance optimizer for large source lists
            val processedResult = performanceOptimizer.processSourcesOptimized(
                sources = sources,
                preferences = userPreferences,
                maxResults = 50
            )
            
            val filteredSources = filterSources(processedResult.sources, _state.value.filter)
            
            _state.value = _state.value.copy(
                sources = processedResult.sources,
                filteredSources = filteredSources,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Failed to process sources: ${e.message}"
            )
        }
    }
    
    /**
     * Update sources optimized for Android TV
     */
    suspend fun updateSourcesForAndroidTV(sources: List<SourceMetadata>) {
        _state.value = _state.value.copy(isLoading = true)
        
        try {
            val optimizedResult = performanceOptimizer.optimizeForAndroidTV(sources, userPreferences)
            val allSources = optimizedResult.immediatelyAvailable + optimizedResult.backgroundProcessed
            val filteredSources = filterSources(allSources, _state.value.filter)
            
            _state.value = _state.value.copy(
                sources = allSources,
                filteredSources = filteredSources,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Failed to optimize sources: ${e.message}"
            )
        }
    }
    
    /**
     * Update user sorting preferences
     */
    fun updateUserPreferences(preferences: UserSortingPreferences) {
        userPreferences = preferences
        // Re-sort with new preferences
        val currentState = _state.value
        val resorted = sourceSorter.sortSources(currentState.sources, preferences)
        val refiltered = filterSources(resorted, currentState.filter)
        
        _state.value = currentState.copy(
            sources = resorted,
            filteredSources = refiltered
        )
    }
    
    /**
     * Apply filter to sources (Legacy method)
     */
    fun applyFilter(filter: SourceFilter) {
        val filteredSources = filterSources(_state.value.sources, filter)
        _state.value = _state.value.copy(
            filter = filter,
            filteredSources = filteredSources
        )
    }
    
    /**
     * Apply advanced filter to sources with performance optimization
     */
    suspend fun applyAdvancedFilter(
        filter: AdvancedSourceFilter,
        maxResults: Int = 100
    ) {
        _state.value = _state.value.copy(isLoading = true)
        
        try {
            val filterResult = filterOptimizer.optimizedFilter(
                sources = _state.value.sources,
                filter = filter,
                maxResults = maxResults
            )
            
            _state.value = _state.value.copy(
                filteredSources = filterResult.filteredSources,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            _state.value = _state.value.copy(
                isLoading = false,
                error = "Filter failed: ${e.message}"
            )
        }
    }
    
    /**
     * Apply filter preset
     */
    suspend fun applyFilterPreset(presetName: String) {
        FilterPresets.getAllPresets()[presetName]?.let { preset ->
            applyAdvancedFilter(preset)
        }
    }
    
    /**
     * Apply multiple filter presets in parallel for comparison
     */
    suspend fun applyMultiplePresets(presetNames: List<String>): Map<String, List<SourceMetadata>> {
        val presets = presetNames.mapNotNull { name ->
            FilterPresets.getAllPresets()[name]?.let { name to it }
        }.toMap()
        
        val results = filterOptimizer.parallelPresetFiltering(
            sources = _state.value.sources,
            presets = presets,
            maxResultsPerPreset = 20
        )
        
        return results.mapValues { it.value.filteredSources }
    }
    
    /**
     * Apply sort option with smart sorting
     */
    fun applySortOption(sortOption: SourceSortOption) {
        val comparator = SourceComparators.createComparator(sortOption)
        val sortedSources = _state.value.sources.sortedWith(comparator)
        val filteredSources = filterSources(sortedSources, _state.value.filter)
        
        _state.value = _state.value.copy(
            sortOption = sortOption,
            sources = sortedSources,
            filteredSources = filteredSources
        )
    }
    
    /**
     * Apply smart sorting with user preferences
     */
    fun applySmartSort() {
        val smartSorted = sourceSorter.sortSources(_state.value.sources, userPreferences)
        val filteredSources = filterSources(smartSorted, _state.value.filter)
        
        _state.value = _state.value.copy(
            sources = smartSorted,
            filteredSources = filteredSources
        )
    }
    
    /**
     * Get grouped and sorted sources for display
     */
    fun getGroupedSources(): Map<ReleaseGroup, List<SourceMetadata>> {
        return sourceSorter.groupAndSortSources(_state.value.filteredSources, userPreferences)
    }
    
    /**
     * Get source analytics
     */
    fun getSourceAnalytics(): SourceCollectionAnalysis {
        val analytics = SourceAnalytics()
        return analytics.analyzeSourceCollection(_state.value.sources)
    }
    
    /**
     * Select a source
     */
    fun selectSource(source: SourceMetadata) {
        _state.value = _state.value.copy(selectedSource = source)
    }
    
    /**
     * Toggle group expansion
     */
    fun toggleGroup(groupId: String) {
        val expandedGroups = _state.value.expandedGroups.toMutableSet()
        if (groupId in expandedGroups) {
            expandedGroups.remove(groupId)
        } else {
            expandedGroups.add(groupId)
        }
        _state.value = _state.value.copy(expandedGroups = expandedGroups)
    }
    
    /**
     * Change view mode
     */
    fun setViewMode(viewMode: SourceSelectionState.ViewMode) {
        _state.value = _state.value.copy(viewMode = viewMode)
    }
    
    /**
     * Set loading state
     */
    fun setLoading(isLoading: Boolean) {
        _state.value = _state.value.copy(isLoading = isLoading)
    }
    
    /**
     * Set error state
     */
    fun setError(error: String?) {
        _state.value = _state.value.copy(error = error, isLoading = false)
    }
    
    /**
     * Clear selection
     */
    fun clearSelection() {
        _state.value = _state.value.copy(selectedSource = null)
    }
    
    /**
     * Reset filters
     */
    fun resetFilters() {
        applyFilter(SourceFilter())
    }
    
    /**
     * Apply quick filter preset
     */
    fun applyQuickFilter(preset: QuickFilterPreset) {
        val filter = when (preset) {
            QuickFilterPreset.BEST_QUALITY -> SourceFilter(
                minQuality = VideoResolution.RESOLUTION_1080P,
                requireHDR = false,
                codecs = setOf(VideoCodec.AV1, VideoCodec.HEVC, VideoCodec.H264),
                releaseTypes = setOf(ReleaseType.BLURAY_REMUX, ReleaseType.BLURAY, ReleaseType.WEB_DL)
            )
            QuickFilterPreset.HDR_ONLY -> SourceFilter(
                requireHDR = true
            )
            QuickFilterPreset.CACHED_ONLY -> SourceFilter(
                requireCached = true
            )
            QuickFilterPreset.SMALL_SIZE -> SourceFilter(
                maxSizeGB = 5f
            )
            QuickFilterPreset.P2P_HIGH_SEEDERS -> SourceFilter(
                minSeeders = 20
            )
        }
        applyFilter(filter)
    }
    
    // Private helper functions
    
    private fun filterSources(sources: List<SourceMetadata>, filter: SourceFilter): List<SourceMetadata> {
        return sources.filter { it.matchesFilter(filter) }
    }
    
}

/**
 * Quick filter presets
 */
enum class QuickFilterPreset {
    BEST_QUALITY,
    HDR_ONLY,
    CACHED_ONLY,
    SMALL_SIZE,
    P2P_HIGH_SEEDERS
}