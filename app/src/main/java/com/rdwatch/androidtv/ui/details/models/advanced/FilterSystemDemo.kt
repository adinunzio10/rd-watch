package com.rdwatch.androidtv.ui.details.models.advanced

import kotlinx.coroutines.runBlocking
import java.util.Date

/**
 * Demonstration and testing of the advanced filtering system
 */
object FilterSystemDemo {
    
    /**
     * Create sample source data for testing
     */
    fun createSampleSources(): List<SourceMetadata> {
        return listOf(
            // High-quality 4K HDR source
            SourceMetadata(
                id = "source1",
                provider = SourceProviderInfo(
                    id = "realdebrid",
                    name = "Real-Debrid",
                    displayName = "Real-Debrid",
                    logoUrl = null,
                    type = SourceProviderInfo.ProviderType.DEBRID,
                    reliability = SourceProviderInfo.ProviderReliability.EXCELLENT
                ),
                quality = QualityInfo(
                    resolution = VideoResolution.RESOLUTION_4K,
                    bitrate = 50_000_000L,
                    hdr10 = true,
                    dolbyVision = true,
                    frameRate = 24
                ),
                codec = CodecInfo(
                    type = VideoCodec.HEVC,
                    profile = "Main 10"
                ),
                audio = AudioInfo(
                    format = AudioFormat.TRUEHD,
                    channels = "7.1",
                    bitrate = 1536,
                    dolbyAtmos = true
                ),
                release = ReleaseInfo(
                    type = ReleaseType.BLURAY_REMUX,
                    group = "RARBG"
                ),
                file = FileInfo(
                    name = "Movie.2023.4K.UHD.BluRay.REMUX.HDR.HEVC.Atmos-RARBG.mkv",
                    sizeInBytes = 75_000_000_000L,
                    addedDate = Date()
                ),
                health = HealthInfo(),
                features = FeatureInfo(hasChapters = true),
                availability = AvailabilityInfo(cached = true, debridService = "real-debrid")
            ),
            
            // Mid-quality 1080p source
            SourceMetadata(
                id = "source2",
                provider = SourceProviderInfo(
                    id = "torrent1",
                    name = "TorrentProvider",
                    displayName = "Torrent Provider",
                    logoUrl = null,
                    type = SourceProviderInfo.ProviderType.TORRENT,
                    reliability = SourceProviderInfo.ProviderReliability.GOOD
                ),
                quality = QualityInfo(
                    resolution = VideoResolution.RESOLUTION_1080P,
                    bitrate = 15_000_000L,
                    frameRate = 24
                ),
                codec = CodecInfo(type = VideoCodec.H264),
                audio = AudioInfo(
                    format = AudioFormat.AC3,
                    channels = "5.1",
                    bitrate = 640
                ),
                release = ReleaseInfo(
                    type = ReleaseType.WEB_DL,
                    group = "SPARKS"
                ),
                file = FileInfo(
                    name = "Movie.2023.1080p.WEB-DL.DD5.1.H.264-SPARKS.mkv",
                    sizeInBytes = 8_500_000_000L,
                    addedDate = Date(System.currentTimeMillis() - 86400000) // 1 day ago
                ),
                health = HealthInfo(
                    seeders = 150,
                    leechers = 25,
                    availability = 1.0f
                ),
                features = FeatureInfo(),
                availability = AvailabilityInfo(cached = false)
            ),
            
            // Small, fast download
            SourceMetadata(
                id = "source3",
                provider = SourceProviderInfo(
                    id = "torrent2",
                    name = "FastProvider",
                    displayName = "Fast Provider",
                    logoUrl = null,
                    type = SourceProviderInfo.ProviderType.TORRENT,
                    reliability = SourceProviderInfo.ProviderReliability.GOOD
                ),
                quality = QualityInfo(
                    resolution = VideoResolution.RESOLUTION_720P,
                    bitrate = 5_000_000L,
                    frameRate = 24
                ),
                codec = CodecInfo(type = VideoCodec.H264),
                audio = AudioInfo(
                    format = AudioFormat.AAC,
                    channels = "2.0",
                    bitrate = 192
                ),
                release = ReleaseInfo(
                    type = ReleaseType.WEBRIP,
                    group = "YTS"
                ),
                file = FileInfo(
                    name = "Movie.2023.720p.WEBRip.x264.AAC-YTS.MX.mp4",
                    sizeInBytes = 1_200_000_000L,
                    addedDate = Date()
                ),
                health = HealthInfo(
                    seeders = 500,
                    leechers = 100,
                    availability = 1.0f
                ),
                features = FeatureInfo(),
                availability = AvailabilityInfo(cached = false)
            ),
            
            // CAM quality (should be filtered out by most presets)
            SourceMetadata(
                id = "source4",
                provider = SourceProviderInfo(
                    id = "unreliable",
                    name = "UnreliableProvider",
                    displayName = "Unreliable Provider",
                    logoUrl = null,
                    type = SourceProviderInfo.ProviderType.TORRENT,
                    reliability = SourceProviderInfo.ProviderReliability.POOR
                ),
                quality = QualityInfo(
                    resolution = VideoResolution.RESOLUTION_480P,
                    frameRate = 30
                ),
                codec = CodecInfo(type = VideoCodec.H264),
                audio = AudioInfo(
                    format = AudioFormat.MP3,
                    channels = "2.0",
                    bitrate = 128
                ),
                release = ReleaseInfo(
                    type = ReleaseType.CAM,
                    group = "UNKNOWN"
                ),
                file = FileInfo(
                    name = "Movie.2023.CAM.480p.x264.MP3-CAM.avi",
                    sizeInBytes = 700_000_000L,
                    addedDate = Date(System.currentTimeMillis() - 172800000) // 2 days ago
                ),
                health = HealthInfo(
                    seeders = 5,
                    leechers = 50,
                    availability = 0.7f
                ),
                features = FeatureInfo(),
                availability = AvailabilityInfo(cached = false)
            )
        )
    }
    
    /**
     * Demonstrate basic filtering functionality
     */
    fun demonstrateBasicFiltering() {
        println("=== Basic Filtering Demo ===")
        
        val sources = createSampleSources()
        val filterSystem = SourceFilterSystem()
        
        // Test HDR only filter
        val hdrFilter = AdvancedSourceFilter(
            qualityFilters = QualityFilters(requireHDR = true)
        )
        
        val hdrResult = filterSystem.filterSources(sources, hdrFilter)
        println("HDR Only Filter:")
        println("  Sources found: ${hdrResult.filteredSources.size}/${sources.size}")
        println("  Processing time: ${hdrResult.processingTimeMs}ms")
        println("  Applied filters: ${hdrResult.appliedFilters.joinToString(", ")}")
        println()
        
        // Test size filter
        val smallSizeFilter = AdvancedSourceFilter(
            fileSizeFilters = FileSizeFilters(maxSizeGB = 5.0)
        )
        
        val sizeResult = filterSystem.filterSources(sources, smallSizeFilter)
        println("Small Size Filter (≤5GB):")
        println("  Sources found: ${sizeResult.filteredSources.size}/${sources.size}")
        println("  Files: ${sizeResult.filteredSources.map { it.file.getFormattedSize() }}")
        println()
        
        // Test cached only filter
        val cachedFilter = AdvancedSourceFilter(
            sourceTypeFilters = SourceTypeFilters(cachedOnly = true)
        )
        
        val cachedResult = filterSystem.filterSources(sources, cachedFilter)
        println("Cached Only Filter:")
        println("  Sources found: ${cachedResult.filteredSources.size}/${sources.size}")
        println("  Cached sources: ${cachedResult.filteredSources.map { it.provider.displayName }}")
        println()
    }
    
    /**
     * Demonstrate filter presets
     */
    fun demonstrateFilterPresets() {
        println("=== Filter Presets Demo ===")
        
        val sources = createSampleSources()
        val filterSystem = SourceFilterSystem()
        
        val presets = listOf(
            "Best Quality" to FilterPresets.BEST_QUALITY,
            "Small Files" to FilterPresets.SMALL_FILES,
            "Cached Only" to FilterPresets.CACHED_ONLY,
            "High Seeders" to FilterPresets.HIGH_SEEDERS
        )
        
        for ((name, preset) in presets) {
            val result = filterSystem.filterSources(sources, preset)
            println("$name Preset:")
            println("  Sources found: ${result.filteredSources.size}/${sources.size}")
            println("  Summary: ${preset.getSummary()}")
            if (result.filteredSources.isNotEmpty()) {
                val topSource = result.filteredSources.first()
                println("  Top result: ${topSource.file.name}")
                println("  Quality: ${topSource.quality.getDisplayText()}")
            }
            println()
        }
    }
    
    /**
     * Demonstrate performance optimization
     */
    fun demonstratePerformanceOptimization() = runBlocking {
        println("=== Performance Optimization Demo ===")
        
        val sources = createSampleSources()
        val optimizer = FilterPerformanceOptimizer()
        
        // Create a complex filter
        val complexFilter = AdvancedSourceFilter(
            qualityFilters = QualityFilters(
                minResolution = VideoResolution.RESOLUTION_720P,
                requireHDR = false
            ),
            sourceTypeFilters = SourceTypeFilters(
                allowedProviderTypes = setOf(
                    SourceProviderInfo.ProviderType.DEBRID,
                    SourceProviderInfo.ProviderType.TORRENT
                )
            ),
            healthFilters = HealthFilters(
                minSeeders = 10
            ),
            releaseTypeFilters = ReleaseTypeFilters(
                excludeCAM = true
            )
        )
        
        val result = optimizer.optimizedFilter(sources, complexFilter, maxResults = 50)
        
        println("Optimized Filtering:")
        println("  Sources found: ${result.filteredSources.size}")
        println("  Processing time: ${result.processingTimeMs}ms")
        println("  Sources evaluated: ${result.totalSourcesEvaluated}")
        println("  Filters applied: ${result.filtersApplied}")
        println()
        
        // Demonstrate parallel preset filtering
        val presetNames = listOf("Best Quality", "Small Files", "Cached Only")
        val presets = presetNames.mapNotNull { name ->
            FilterPresets.getAllPresets()[name]?.let { name to it }
        }.toMap()
        
        val parallelResults = optimizer.parallelPresetFiltering(sources, presets)
        
        println("Parallel Preset Filtering:")
        for ((presetName, presetResult) in parallelResults) {
            println("  $presetName: ${presetResult.filteredSources.size} sources, ${presetResult.processingTimeMs}ms")
        }
        println()
    }
    
    /**
     * Demonstrate filter preferences and persistence
     */
    fun demonstrateFilterPreferences() {
        println("=== Filter Preferences Demo ===")
        
        val preferencesManager = FilterPreferencesManager()
        
        // Create a custom filter
        val customFilter = AdvancedSourceFilter(
            qualityFilters = QualityFilters(
                minResolution = VideoResolution.RESOLUTION_1080P,
                requireHDR = true
            ),
            audioFilters = AudioFilters(
                requireDolbyAtmos = true
            ),
            fileSizeFilters = FileSizeFilters(
                maxSizeGB = 30.0
            )
        )
        
        // Save the custom filter
        preferencesManager.saveFilter("My HDR Atmos Filter", customFilter)
        
        // Set as default
        preferencesManager.setDefaultFilter(customFilter)
        
        // Add to favorites
        preferencesManager.toggleFavoritePreset("Best Quality")
        preferencesManager.toggleFavoritePreset("HDR Only")
        
        val preferences = preferencesManager.preferences.value
        
        println("Filter Preferences:")
        println("  Saved filters: ${preferences.savedFilters.keys}")
        println("  Favorite presets: ${preferences.favoritePresets}")
        println("  Has default filter: ${preferences.defaultFilter != null}")
        println("  Auto-apply last filter: ${preferences.autoApplyLastFilter}")
        println()
        
        // Export preferences
        val exported = preferencesManager.exportPreferences()
        println("Exported preferences size: ${exported.length} characters")
        println()
    }
    
    /**
     * Demonstrate conflict resolution
     */
    fun demonstrateConflictResolution() {
        println("=== Conflict Resolution Demo ===")
        
        val sources = createSampleSources()
        val filterSystem = SourceFilterSystem()
        
        // Create a filter that will conflict (require 4K + small size)
        val conflictingFilter = AdvancedSourceFilter(
            qualityFilters = QualityFilters(
                require4KOnly = true,
                requireHDR = true
            ),
            fileSizeFilters = FileSizeFilters(
                maxSizeGB = 2.0 // Too small for 4K HDR
            ),
            healthFilters = HealthFilters(
                minSeeders = 1000 // Very high requirement
            ),
            conflictResolution = ConflictResolution(
                enabled = true,
                strategies = listOf(
                    ConflictResolutionStrategy.RELAX_SIZE,
                    ConflictResolutionStrategy.RELAX_HEALTH,
                    ConflictResolutionStrategy.RELAX_QUALITY
                )
            )
        )
        
        val result = filterSystem.filterSources(sources, conflictingFilter)
        
        println("Conflicting Filter Results:")
        println("  Original filter summary: ${conflictingFilter.getSummary()}")
        println("  Sources found: ${result.filteredSources.size}")
        println("  Conflict resolution attempted: ${result.filteredSources.isNotEmpty()}")
        if (result.filteredSources.isNotEmpty()) {
            println("  Resolved sources: ${result.filteredSources.map { it.quality.resolution.displayName }}")
        }
        println()
    }
    
    /**
     * Run all demonstrations
     */
    fun runAllDemos() {
        demonstrateBasicFiltering()
        demonstrateFilterPresets()
        demonstratePerformanceOptimization()
        demonstrateFilterPreferences()
        demonstrateConflictResolution()
        
        println("=== Filter System Demo Complete ===")
        println("The advanced filtering system provides:")
        println("✓ Comprehensive filtering across 9 categories")
        println("✓ 10+ predefined filter presets")
        println("✓ Performance optimization for large source lists")
        println("✓ User preference persistence and management")
        println("✓ Conflict resolution with multiple strategies")
        println("✓ Backward compatibility with legacy SourceFilter")
        println("✓ Export/import of filter configurations")
        println("✓ Parallel processing for multiple presets")
    }
}

/**
 * Usage example for integration
 */
class FilterSystemUsageExample {
    
    private val sourceSelectionManager = SourceSelectionManager()
    private val filterPreferences = FilterPreferencesManager()
    
    suspend fun exampleUsage() {
        // Apply a preset filter
        sourceSelectionManager.applyFilterPreset("Best Quality")
        
        // Create and apply a custom filter
        val customFilter = AdvancedSourceFilter(
            qualityFilters = QualityFilters(
                minResolution = VideoResolution.RESOLUTION_1080P,
                requireHDR = true
            ),
            sourceTypeFilters = SourceTypeFilters(
                cachedOnly = true
            )
        )
        
        sourceSelectionManager.applyAdvancedFilter(customFilter, maxResults = 50)
        
        // Save the custom filter for later use
        filterPreferences.saveFilter("My Custom Filter", customFilter)
        
        // Compare multiple presets
        val comparison = sourceSelectionManager.applyMultiplePresets(
            listOf("Best Quality", "Small Files", "TV Optimized")
        )
        
        // The results are now available in the state
        val currentState = sourceSelectionManager.state.value
        println("Filtered sources: ${currentState.filteredSources.size}")
    }
}