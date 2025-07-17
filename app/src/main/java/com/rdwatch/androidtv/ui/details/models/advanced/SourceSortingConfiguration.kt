package com.rdwatch.androidtv.ui.details.models.advanced

/**
 * Configuration options for user-customizable source sorting
 */
data class SourceSortingConfiguration(
    // Primary sorting preferences
    val primarySort: SortCriteria = SortCriteria.SMART,
    val secondarySort: SortCriteria = SortCriteria.QUALITY,
    val tertiarySort: SortCriteria = SortCriteria.HEALTH,
    // Weights for smart sorting (0.0 to 1.0)
    val qualityWeight: Float = 1.0f,
    val healthWeight: Float = 0.8f,
    val sizeWeight: Float = 0.3f,
    val providerWeight: Float = 0.5f,
    val releaseGroupWeight: Float = 0.4f,
    // User preferences
    val userPreferences: UserSortingPreferences = UserSortingPreferences(),
    // Performance settings
    val enableChunkedProcessing: Boolean = true,
    val maxSourcesForFullSort: Int = 200,
    val enablePreFiltering: Boolean = true,
    // Android TV specific optimizations
    val prioritizeCachedSources: Boolean = true,
    val optimizeForRemoteNavigation: Boolean = true,
    val groupSimilarSources: Boolean = true,
) {
    /**
     * Create a custom comparator based on this configuration
     */
    fun createComparator(): Comparator<SourceMetadata> {
        val primary = createCriteriaComparator(primarySort)
        val secondary = createCriteriaComparator(secondarySort)
        val tertiary = createCriteriaComparator(tertiarySort)

        return SourceComparators.chain(primary, secondary, tertiary)
    }

    /**
     * Create comparator for specific criteria
     */
    private fun createCriteriaComparator(criteria: SortCriteria): Comparator<SourceMetadata> {
        return when (criteria) {
            SortCriteria.SMART ->
                SourceComparators.createWeightedComparator(
                    qualityWeight,
                    healthWeight,
                    sizeWeight,
                    providerWeight,
                )
            SortCriteria.QUALITY -> SourceComparators.qualityComparator
            SortCriteria.HEALTH -> SourceComparators.healthComparator
            SortCriteria.SIZE_SMALLEST -> SourceComparators.sizeComparator
            SortCriteria.SIZE_LARGEST -> SourceComparators.sizeLargestFirstComparator
            SortCriteria.PROVIDER -> SourceComparators.providerReliabilityComparator
            SortCriteria.RELEASE_TYPE -> SourceComparators.releaseTypeComparator
            SortCriteria.CACHED_FIRST -> SourceComparators.cachedFirstComparator
            SortCriteria.RECENT -> SourceComparators.recentComparator
            SortCriteria.ANDROID_TV_OPTIMIZED -> SourceComparators.androidTVOptimizedComparator
        }
    }

    /**
     * Get configuration for different user scenarios
     */
    companion object {
        /**
         * Configuration optimized for quality enthusiasts
         */
        fun qualityFocused() =
            SourceSortingConfiguration(
                primarySort = SortCriteria.QUALITY,
                secondarySort = SortCriteria.HEALTH,
                tertiarySort = SortCriteria.SIZE_LARGEST,
                qualityWeight = 1.0f,
                healthWeight = 0.6f,
                sizeWeight = 0.2f,
                userPreferences =
                    UserSortingPreferences(
                        preferredResolution = VideoResolution.RESOLUTION_4K,
                        preferredCodecs = setOf(VideoCodec.HEVC, VideoCodec.AV1),
                        preferHDR = true,
                        preferHighQualityAudio = true,
                    ),
            )

        /**
         * Configuration optimized for Android TV performance
         */
        fun androidTVOptimized() =
            SourceSortingConfiguration(
                primarySort = SortCriteria.ANDROID_TV_OPTIMIZED,
                secondarySort = SortCriteria.QUALITY,
                tertiarySort = SortCriteria.SIZE_SMALLEST,
                prioritizeCachedSources = true,
                optimizeForRemoteNavigation = true,
                groupSimilarSources = true,
                userPreferences =
                    UserSortingPreferences(
                        preferDebrid = true,
                        fileSizePreference = FileSizePreference.OPTIMAL,
                        preferredFileSizeGB = 6.0,
                    ),
            )

        /**
         * Configuration for users with limited bandwidth
         */
        fun bandwidthConstrained() =
            SourceSortingConfiguration(
                primarySort = SortCriteria.SIZE_SMALLEST,
                secondarySort = SortCriteria.QUALITY,
                tertiarySort = SortCriteria.HEALTH,
                sizeWeight = 1.0f,
                qualityWeight = 0.7f,
                userPreferences =
                    UserSortingPreferences(
                        fileSizePreference = FileSizePreference.SMALLEST,
                        preferredResolution = VideoResolution.RESOLUTION_720P,
                    ),
            )

        /**
         * Configuration for P2P enthusiasts
         */
        fun p2pOptimized() =
            SourceSortingConfiguration(
                primarySort = SortCriteria.HEALTH,
                secondarySort = SortCriteria.QUALITY,
                tertiarySort = SortCriteria.RELEASE_TYPE,
                healthWeight = 1.0f,
                qualityWeight = 0.8f,
                releaseGroupWeight = 0.6f,
                userPreferences =
                    UserSortingPreferences(
                        preferP2P = true,
                        preferDebrid = false,
                    ),
            )

        /**
         * Configuration for users who prefer cached/instant sources
         */
        fun instantPlayback() =
            SourceSortingConfiguration(
                primarySort = SortCriteria.CACHED_FIRST,
                secondarySort = SortCriteria.QUALITY,
                tertiarySort = SortCriteria.SIZE_SMALLEST,
                prioritizeCachedSources = true,
                userPreferences =
                    UserSortingPreferences(
                        preferDebrid = true,
                        prioritizeCached = true,
                    ),
            )
    }
}

/**
 * Available sorting criteria
 */
enum class SortCriteria {
    SMART, // Intelligent weighted sorting
    QUALITY, // Video/audio quality
    HEALTH, // P2P health (seeders/leechers)
    SIZE_SMALLEST, // File size (smallest first)
    SIZE_LARGEST, // File size (largest first)
    PROVIDER, // Provider reliability
    RELEASE_TYPE, // Release type quality
    CACHED_FIRST, // Cached sources first
    RECENT, // Recently added
    ANDROID_TV_OPTIMIZED, // Optimized for Android TV
}

/**
 * Preset sorting configurations for quick selection
 */
enum class SortingPreset(val displayName: String, val description: String) {
    SMART("Smart", "Intelligent sorting based on all factors"),
    QUALITY_FIRST("Quality First", "Prioritize highest quality sources"),
    FAST_PLAYBACK("Fast Playback", "Prioritize sources that start quickly"),
    SMALL_FILES("Small Files", "Prefer smaller file sizes"),
    HIGH_SEEDERS("High Seeders", "Prefer well-seeded torrents"),
    CACHED_ONLY("Cached Only", "Show only cached sources first"),
    NEWEST_FIRST("Newest First", "Show recently added sources first"),
    ANDROID_TV("Android TV", "Optimized for TV interface"),
    ;

    /**
     * Get configuration for this preset
     */
    fun getConfiguration(): SourceSortingConfiguration {
        return when (this) {
            SMART -> SourceSortingConfiguration()
            QUALITY_FIRST -> SourceSortingConfiguration.qualityFocused()
            FAST_PLAYBACK -> SourceSortingConfiguration.instantPlayback()
            SMALL_FILES -> SourceSortingConfiguration.bandwidthConstrained()
            HIGH_SEEDERS -> SourceSortingConfiguration.p2pOptimized()
            CACHED_ONLY -> SourceSortingConfiguration.instantPlayback()
            NEWEST_FIRST ->
                SourceSortingConfiguration(
                    primarySort = SortCriteria.RECENT,
                    secondarySort = SortCriteria.QUALITY,
                )
            ANDROID_TV -> SourceSortingConfiguration.androidTVOptimized()
        }
    }
}

/**
 * Advanced filter and sort combinations
 */
data class FilterSortCombination(
    val filter: SourceFilter,
    val sortingConfig: SourceSortingConfiguration,
    val name: String,
    val description: String,
) {
    companion object {
        /**
         * Get predefined filter/sort combinations
         */
        fun getPredefinedCombinations(): List<FilterSortCombination> {
            return listOf(
                FilterSortCombination(
                    filter =
                        SourceFilter(
                            minQuality = VideoResolution.RESOLUTION_1080P,
                            requireHDR = true,
                        ),
                    sortingConfig = SourceSortingConfiguration.qualityFocused(),
                    name = "4K HDR Premium",
                    description = "Best quality 4K HDR sources",
                ),
                FilterSortCombination(
                    filter =
                        SourceFilter(
                            requireCached = true,
                        ),
                    sortingConfig = SourceSortingConfiguration.instantPlayback(),
                    name = "Instant Play",
                    description = "Cached sources for immediate playback",
                ),
                FilterSortCombination(
                    filter =
                        SourceFilter(
                            maxSizeGB = 5f,
                            minQuality = VideoResolution.RESOLUTION_720P,
                        ),
                    sortingConfig = SourceSortingConfiguration.bandwidthConstrained(),
                    name = "Mobile Friendly",
                    description = "Small, efficient files for mobile viewing",
                ),
                FilterSortCombination(
                    filter =
                        SourceFilter(
                            minSeeders = 50,
                            releaseTypes = setOf(ReleaseType.BLURAY, ReleaseType.WEB_DL),
                        ),
                    sortingConfig = SourceSortingConfiguration.p2pOptimized(),
                    name = "P2P Premium",
                    description = "High-quality, well-seeded torrents",
                ),
            )
        }
    }
}
