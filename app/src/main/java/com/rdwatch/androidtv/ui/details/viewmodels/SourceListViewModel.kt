package com.rdwatch.androidtv.ui.details.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.ui.details.models.advanced.*
import com.rdwatch.androidtv.ui.details.repository.SourceAggregationRepository
import com.rdwatch.androidtv.ui.details.repository.SourceAggregationRepositoryImpl
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay

/**
 * ViewModel for managing source list state, filtering, sorting, and data operations
 * Integrates with the Backend Agent's advanced sorting algorithms
 */
class SourceListViewModel(
    private val sourceRepository: SourceAggregationRepository = SourceAggregationRepositoryImpl()
) : ViewModel() {
    
    // State management using the advanced selection manager
    private val selectionManager = SourceSelectionManager()
    
    // Expose state from the selection manager
    val state: StateFlow<SourceSelectionState> = selectionManager.state
    
    // Additional UI state
    private val _isBottomSheetVisible = MutableStateFlow(false)
    val isBottomSheetVisible: StateFlow<Boolean> = _isBottomSheetVisible.asStateFlow()
    
    private val _currentMovieId = MutableStateFlow<String?>(null)
    val currentMovieId: StateFlow<String?> = _currentMovieId.asStateFlow()
    
    // User preferences for smart sorting
    private val _userPreferences = MutableStateFlow(UserSortingPreferences())
    val userPreferences: StateFlow<UserSortingPreferences> = _userPreferences.asStateFlow()
    
    // Source events for analytics and optimization
    private val sourceEvents = mutableListOf<SourceSelectionEvents.SourceEvent>()
    
    init {
        // Initialize with default sorting preferences
        setupDefaultPreferences()
    }
    
    /**
     * Load sources for a specific movie/content
     */
    fun loadSources(movieId: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            _currentMovieId.value = movieId
            selectionManager.setLoading(true)
            
            try {
                // Simulate loading delay for TV UI responsiveness
                if (!forceRefresh) {
                    delay(200)
                }
                
                // Fetch sources from repository
                val sources = if (sourceRepository is SourceAggregationRepositoryImpl) {
                    sourceRepository.getSourcesForContent(movieId)
                } else {
                    // Fallback for interface - create basic ContentDetail
                    val contentDetail = com.rdwatch.androidtv.ui.details.repository.ContentDetail(
                        id = movieId,
                        title = "Content",
                        type = "movie",
                        year = null,
                        imdbId = null,
                        tmdbId = null
                    )
                    val sourcesList = mutableListOf<SourceMetadata>()
                    sourceRepository.getSources(contentDetail).collect { sourcesList.addAll(it) }
                    sourcesList
                }
                
                // Process with advanced sorting optimized for Android TV
                selectionManager.updateSourcesForAndroidTV(sources)
                
                // Track successful load event
                trackEvent(SourceSelectionEvents.SourceLoadEvent(
                    contentId = movieId,
                    sourceCount = sources.size,
                    loadTimeMs = System.currentTimeMillis()
                ))
                
            } catch (e: Exception) {
                selectionManager.setError("Failed to load sources: ${e.message}")
                
                // Track error event
                trackEvent(SourceSelectionEvents.SourceErrorEvent(
                    contentId = movieId,
                    error = e.message ?: "Unknown error",
                    timestamp = System.currentTimeMillis()
                ))
            }
        }
    }
    
    /**
     * Refresh sources with pull-to-refresh
     */
    fun refreshSources() {
        _currentMovieId.value?.let { movieId ->
            loadSources(movieId, forceRefresh = true)
        }
    }
    
    /**
     * Show the source selection bottom sheet
     */
    fun showSourceSelection(movieId: String) {
        _isBottomSheetVisible.value = true
        if (_currentMovieId.value != movieId || state.value.sources.isEmpty()) {
            loadSources(movieId)
        }
    }
    
    /**
     * Hide the source selection bottom sheet
     */
    fun hideSourceSelection() {
        _isBottomSheetVisible.value = false
        selectionManager.clearSelection()
    }
    
    /**
     * Select a source
     */
    fun selectSource(source: SourceMetadata) {
        selectionManager.selectSource(source)
        
        // Track selection event
        trackEvent(SourceSelectionEvents.SourceSelectedEvent(
            sourceId = source.id,
            provider = source.provider.name,
            quality = source.quality.resolution.displayName,
            selectionTimeMs = System.currentTimeMillis()
        ))
    }
    
    /**
     * Apply filter with user preference learning
     */
    fun applyFilter(filter: SourceFilter) {
        selectionManager.applyFilter(filter)
        
        // Learn from user filter preferences
        learnFromFilterPreferences(filter)
        
        // Track filter event
        trackEvent(SourceSelectionEvents.SourceFilterEvent(
            filterType = getFilterType(filter),
            filterValue = getFilterValue(filter),
            timestamp = System.currentTimeMillis()
        ))
    }
    
    /**
     * Apply sort option with smart learning
     */
    fun applySortOption(sortOption: SourceSortOption) {
        selectionManager.applySortOption(sortOption)
        
        // Update user preferences
        updateUserSortPreference(sortOption)
        
        // Track sort event
        trackEvent(SourceSelectionEvents.SourceSortEvent(
            sortType = sortOption.name,
            timestamp = System.currentTimeMillis()
        ))
    }
    
    /**
     * Apply quick filter preset
     */
    fun applyQuickFilter(preset: QuickFilterPreset) {
        selectionManager.applyQuickFilter(preset)
        
        // Track quick filter usage
        trackEvent(SourceSelectionEvents.SourceFilterEvent(
            filterType = "quick_filter",
            filterValue = preset.name,
            timestamp = System.currentTimeMillis()
        ))
    }
    
    /**
     * Toggle group expansion
     */
    fun toggleGroup(groupId: String) {
        selectionManager.toggleGroup(groupId)
    }
    
    /**
     * Change view mode
     */
    fun changeViewMode(viewMode: SourceSelectionState.ViewMode) {
        selectionManager.setViewMode(viewMode)
        
        // Track view mode preference
        trackEvent(SourceSelectionEvents.SourceViewEvent(
            viewMode = viewMode.name,
            timestamp = System.currentTimeMillis()
        ))
    }
    
    /**
     * Play a source
     */
    fun playSource(source: SourceMetadata) {
        // Track play event
        trackEvent(SourceSelectionEvents.SourcePlayEvent(
            sourceId = source.id,
            provider = source.provider.name,
            quality = source.quality.resolution.displayName,
            playTimeMs = System.currentTimeMillis()
        ))
        
        // Learn from successful play
        learnFromPlaySuccess(source)
        
        // Hide bottom sheet after selection
        hideSourceSelection()
    }
    
    /**
     * Download a source
     */
    fun downloadSource(source: SourceMetadata) {
        // Track download event
        trackEvent(SourceSelectionEvents.SourceDownloadEvent(
            sourceId = source.id,
            provider = source.provider.name,
            quality = source.quality.resolution.displayName,
            timestamp = System.currentTimeMillis()
        ))
        
        // In a real implementation, this would trigger the download manager
        // For now, we just track the user's preference
        learnFromDownloadPreference(source)
    }
    
    /**
     * Add source to playlist
     */
    fun addToPlaylist(source: SourceMetadata) {
        // Track playlist event
        trackEvent(SourceSelectionEvents.SourcePlaylistEvent(
            sourceId = source.id,
            provider = source.provider.name,
            quality = source.quality.resolution.displayName,
            timestamp = System.currentTimeMillis()
        ))
        
        // Learn from playlist addition
        learnFromPlaylistAddition(source)
    }
    
    /**
     * Get source analytics for debugging and optimization
     */
    fun getSourceAnalytics(): SourceCollectionAnalysis {
        return selectionManager.getSourceAnalytics()
    }
    
    /**
     * Get grouped sources with smart sorting
     */
    fun getGroupedSources(): Map<ReleaseGroup, List<SourceMetadata>> {
        return selectionManager.getGroupedSources()
    }
    
    /**
     * Clear all filters and reset to default state
     */
    fun clearFilters() {
        selectionManager.resetFilters()
    }
    
    /**
     * Apply smart sorting based on user preferences and context
     */
    fun applySmartSort() {
        selectionManager.applySmartSort()
    }
    
    // Private helper methods
    
    private fun setupDefaultPreferences() {
        val defaultPreferences = UserSortingPreferences(
            preferredQuality = VideoResolution.RESOLUTION_1080P,
            preferredCodecs = listOf(VideoCodec.HEVC, VideoCodec.H264),
            preferredReleaseTypes = listOf(ReleaseType.WEB_DL, ReleaseType.BLURAY),
            preferredProviders = emptyList(), // Will be learned from usage
            qualityWeight = 0.3f,
            reliabilityWeight = 0.25f,
            sizeWeight = 0.15f,
            speedWeight = 0.15f,
            popularityWeight = 0.15f,
            preferCached = true,
            maxSizeGB = null, // No size limit by default
            minSeeders = 5 // Minimum seeders for P2P
        )
        
        _userPreferences.value = defaultPreferences
        selectionManager.updateUserPreferences(defaultPreferences)
    }
    
    private fun learnFromFilterPreferences(filter: SourceFilter) {
        val currentPrefs = _userPreferences.value
        
        // Learn quality preferences
        filter.minQuality?.let { minQuality ->
            if (minQuality.ordinal > currentPrefs.preferredQuality.ordinal) {
                updateUserPreferences(currentPrefs.copy(preferredQuality = minQuality))
            }
        }
        
        // Learn codec preferences
        if (filter.codecs.isNotEmpty()) {
            val updatedCodecs = (currentPrefs.preferredCodecs + filter.codecs).distinct()
            updateUserPreferences(currentPrefs.copy(preferredCodecs = updatedCodecs))
        }
        
        // Learn release type preferences
        if (filter.releaseTypes.isNotEmpty()) {
            val updatedReleaseTypes = (currentPrefs.preferredReleaseTypes + filter.releaseTypes).distinct()
            updateUserPreferences(currentPrefs.copy(preferredReleaseTypes = updatedReleaseTypes))
        }
        
        // Learn size preferences
        filter.maxSizeGB?.let { maxSize ->
            updateUserPreferences(currentPrefs.copy(maxSizeGB = maxSize))
        }
        
        // Learn seeder preferences
        filter.minSeeders?.let { minSeeders ->
            updateUserPreferences(currentPrefs.copy(minSeeders = minSeeders))
        }
        
        // Learn caching preferences
        if (filter.requireCached) {
            updateUserPreferences(currentPrefs.copy(preferCached = true))
        }
    }
    
    private fun learnFromPlaySuccess(source: SourceMetadata) {
        val currentPrefs = _userPreferences.value
        
        // Boost preference for this provider
        val updatedProviders = currentPrefs.preferredProviders.toMutableList()
        if (!updatedProviders.contains(source.provider.id)) {
            updatedProviders.add(source.provider.id)
            updateUserPreferences(currentPrefs.copy(preferredProviders = updatedProviders))
        }
        
        // Learn quality preference
        if (source.quality.resolution.ordinal >= currentPrefs.preferredQuality.ordinal) {
            updateUserPreferences(currentPrefs.copy(preferredQuality = source.quality.resolution))
        }
        
        // Learn codec preference
        if (!currentPrefs.preferredCodecs.contains(source.codec.type)) {
            val updatedCodecs = (currentPrefs.preferredCodecs + source.codec.type).distinct()
            updateUserPreferences(currentPrefs.copy(preferredCodecs = updatedCodecs))
        }
    }
    
    private fun learnFromDownloadPreference(source: SourceMetadata) {
        val currentPrefs = _userPreferences.value
        
        // Users who download likely prefer higher quality
        if (source.quality.resolution.ordinal > currentPrefs.preferredQuality.ordinal) {
            updateUserPreferences(currentPrefs.copy(preferredQuality = source.quality.resolution))
        }
        
        // Increase size weight for users who download
        updateUserPreferences(currentPrefs.copy(sizeWeight = currentPrefs.sizeWeight * 1.1f))
    }
    
    private fun learnFromPlaylistAddition(source: SourceMetadata) {
        val currentPrefs = _userPreferences.value
        
        // Users who add to playlists likely prefer reliability
        updateUserPreferences(currentPrefs.copy(reliabilityWeight = currentPrefs.reliabilityWeight * 1.1f))
    }
    
    private fun updateUserSortPreference(sortOption: SourceSortOption) {
        val currentPrefs = _userPreferences.value
        
        // Adjust weights based on sort preference
        val updatedPrefs = when (sortOption) {
            SourceSortOption.QUALITY_SCORE -> currentPrefs.copy(qualityWeight = 0.4f)
            SourceSortOption.FILE_SIZE -> currentPrefs.copy(sizeWeight = 0.3f)
            SourceSortOption.SEEDERS -> currentPrefs.copy(speedWeight = 0.3f)
            SourceSortOption.PROVIDER -> currentPrefs.copy(reliabilityWeight = 0.3f)
            else -> currentPrefs
        }
        
        updateUserPreferences(updatedPrefs)
    }
    
    private fun updateUserPreferences(preferences: UserSortingPreferences) {
        _userPreferences.value = preferences
        selectionManager.updateUserPreferences(preferences)
    }
    
    private fun trackEvent(event: SourceSelectionEvents.SourceEvent) {
        sourceEvents.add(event)
        
        // Keep only recent events to prevent memory bloat
        if (sourceEvents.size > 1000) {
            sourceEvents.removeAll(sourceEvents.take(500))
        }
    }
    
    private fun getFilterType(filter: SourceFilter): String {
        return when {
            filter.minQuality != null -> "quality"
            filter.requireHDR -> "hdr"
            filter.codecs.isNotEmpty() -> "codec"
            filter.audioFormats.isNotEmpty() -> "audio"
            filter.releaseTypes.isNotEmpty() -> "release_type"
            filter.providers.isNotEmpty() -> "provider"
            filter.maxSizeGB != null -> "size"
            filter.minSeeders != null -> "seeders"
            filter.requireCached -> "cached"
            else -> "unknown"
        }
    }
    
    private fun getFilterValue(filter: SourceFilter): String {
        return when {
            filter.minQuality != null -> filter.minQuality.name
            filter.requireHDR -> "true"
            filter.codecs.isNotEmpty() -> filter.codecs.joinToString(",") { it.name }
            filter.audioFormats.isNotEmpty() -> filter.audioFormats.joinToString(",") { it.name }
            filter.releaseTypes.isNotEmpty() -> filter.releaseTypes.joinToString(",") { it.name }
            filter.providers.isNotEmpty() -> filter.providers.joinToString(",")
            filter.maxSizeGB != null -> filter.maxSizeGB.toString()
            filter.minSeeders != null -> filter.minSeeders.toString()
            filter.requireCached -> "true"
            else -> "unknown"
        }
    }
    
    /**
     * Get usage statistics for debugging
     */
    fun getUsageStatistics(): SourceUsageStatistics {
        return SourceUsageStatistics(
            totalEvents = sourceEvents.size,
            playEvents = sourceEvents.count { it is SourceSelectionEvents.SourcePlayEvent },
            downloadEvents = sourceEvents.count { it is SourceSelectionEvents.SourceDownloadEvent },
            filterEvents = sourceEvents.count { it is SourceSelectionEvents.SourceFilterEvent },
            sortEvents = sourceEvents.count { it is SourceSelectionEvents.SourceSortEvent },
            averageSelectionTime = calculateAverageSelectionTime(),
            mostUsedProviders = getMostUsedProviders(),
            preferredQualities = getPreferredQualities()
        )
    }
    
    private fun calculateAverageSelectionTime(): Long {
        val selectionEvents = sourceEvents.filterIsInstance<SourceSelectionEvents.SourceSelectedEvent>()
        return if (selectionEvents.isNotEmpty()) {
            selectionEvents.map { it.selectionTimeMs }.average().toLong()
        } else 0L
    }
    
    private fun getMostUsedProviders(): List<String> {
        return sourceEvents.filterIsInstance<SourceSelectionEvents.SourcePlayEvent>()
            .groupBy { it.provider }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(5)
            .map { it.first }
    }
    
    private fun getPreferredQualities(): List<String> {
        return sourceEvents.filterIsInstance<SourceSelectionEvents.SourcePlayEvent>()
            .groupBy { it.quality }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }
}

/**
 * Usage statistics for debugging and optimization
 */
data class SourceUsageStatistics(
    val totalEvents: Int,
    val playEvents: Int,
    val downloadEvents: Int,
    val filterEvents: Int,
    val sortEvents: Int,
    val averageSelectionTime: Long,
    val mostUsedProviders: List<String>,
    val preferredQualities: List<String>
)