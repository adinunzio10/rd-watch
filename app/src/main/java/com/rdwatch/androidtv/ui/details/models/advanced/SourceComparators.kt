package com.rdwatch.androidtv.ui.details.models.advanced

import com.rdwatch.androidtv.ui.details.models.SourceSortOption

/**
 * Collection of specialized comparators for different sorting scenarios
 */
object SourceComparators {
    /**
     * Comparator for quality-focused sorting
     */
    val qualityComparator =
        compareByDescending<SourceMetadata> { source ->
            source.getQualityScore()
        }.thenByDescending { source ->
            // Secondary sort by resolution pixels for same quality score
            source.quality.resolution.pixels
        }.thenByDescending { source ->
            // Tertiary sort by codec efficiency
            source.codec.type.efficiencyBonus
        }

    /**
     * Comparator for health-focused sorting (P2P sources)
     */
    val healthComparator =
        compareByDescending<SourceMetadata> { source ->
            source.health.seeders ?: -1
        }.thenByDescending { source ->
            // Consider seeder/leecher ratio
            val seeders = source.health.seeders ?: 0
            val leechers = source.health.leechers ?: 1
            if (seeders > 0 && leechers > 0) seeders.toFloat() / leechers else 0f
        }.thenByDescending { source ->
            // Consider availability
            source.health.availability ?: 0f
        }

    /**
     * Comparator for size-focused sorting
     */
    val sizeComparator =
        compareBy<SourceMetadata> { source ->
            source.file.sizeInBytes ?: Long.MAX_VALUE
        }

    val sizeLargestFirstComparator =
        compareByDescending<SourceMetadata> { source ->
            source.file.sizeInBytes ?: 0L
        }

    /**
     * Comparator for provider reliability
     */
    val providerReliabilityComparator =
        compareByDescending<SourceMetadata> { source ->
            source.provider.reliability.ordinal
        }.thenBy { source ->
            source.provider.displayName
        }

    /**
     * Comparator for release type quality
     */
    val releaseTypeComparator =
        compareByDescending<SourceMetadata> { source ->
            source.release.type.qualityBonus
        }.thenBy { source ->
            // Secondary sort by release group quality
            when (source.release.group?.uppercase()) {
                in setOf("RARBG", "SPARKS", "GECKOS") -> 0
                in setOf("YIFY", "EZTV", "ETTV") -> 1
                else -> 2
            }
        }

    /**
     * Comparator for cached sources first
     */
    val cachedFirstComparator =
        compareBy<SourceMetadata> { source ->
            if (source.availability.cached) 0 else 1
        }

    /**
     * Comparator for debrid sources first
     */
    val debridFirstComparator =
        compareBy<SourceMetadata> { source ->
            when (source.provider.type) {
                SourceProviderInfo.ProviderType.DEBRID -> 0
                SourceProviderInfo.ProviderType.DIRECT_STREAM -> 1
                SourceProviderInfo.ProviderType.TORRENT -> 2
                else -> 3
            }
        }

    /**
     * Comparator for recent sources first
     */
    val recentComparator =
        compareByDescending<SourceMetadata> { source ->
            source.file.addedDate?.time ?: 0L
        }

    /**
     * Comparator optimized for Android TV performance
     * Prioritizes sources that will start playing fastest
     */
    val androidTVOptimizedComparator =
        compareBy<SourceMetadata> { source ->
            // Priority 1: Cached debrid sources (instant start)
            if (source.availability.cached && source.provider.type == SourceProviderInfo.ProviderType.DEBRID) {
                0
            } // Priority 2: Direct streams (fast start)
            else if (source.provider.type == SourceProviderInfo.ProviderType.DIRECT_STREAM) {
                1
            } // Priority 3: Non-cached debrid (moderate start)
            else if (source.provider.type == SourceProviderInfo.ProviderType.DEBRID) {
                2
            } // Priority 4: High-seeded torrents (slower start)
            else if ((source.health.seeders ?: 0) > 100) {
                3
            } // Priority 5: Low-seeded torrents (slowest start)
            else {
                4
            }
        }.thenByDescending { source ->
            // Secondary: Quality score
            source.getQualityScore()
        }.thenByDescending { source ->
            // Tertiary: Health for P2P sources
            source.health.seeders ?: 0
        }

    /**
     * Create custom comparator based on sort option
     */
    fun createComparator(sortOption: SourceSortOption): Comparator<SourceMetadata> {
        return when (sortOption) {
            SourceSortOption.QUALITY_SCORE -> qualityComparator
            SourceSortOption.FILE_SIZE -> sizeComparator
            SourceSortOption.SEEDERS -> healthComparator
            SourceSortOption.ADDED_DATE -> recentComparator
            SourceSortOption.PROVIDER -> providerReliabilityComparator
            SourceSortOption.RELEASE_TYPE -> releaseTypeComparator
            SourceSortOption.PRIORITY -> qualityComparator // Use quality for priority sorting
            SourceSortOption.QUALITY -> qualityComparator // Same as QUALITY_SCORE
            SourceSortOption.RELIABILITY -> providerReliabilityComparator
            SourceSortOption.AVAILABILITY -> healthComparator // Use health for availability
        }
    }

    /**
     * Create composite comparator with multiple criteria
     */
    fun createCompositeComparator(
        primary: Comparator<SourceMetadata>,
        secondary: Comparator<SourceMetadata>,
        tertiary: Comparator<SourceMetadata>? = null,
    ): Comparator<SourceMetadata> {
        return if (tertiary != null) {
            primary.thenComparing(secondary).thenComparing(tertiary)
        } else {
            primary.thenComparing(secondary)
        }
    }

    /**
     * Create weighted comparator for multiple criteria
     */
    fun createWeightedComparator(
        qualityWeight: Float = 1.0f,
        healthWeight: Float = 0.8f,
        sizeWeight: Float = 0.3f,
        providerWeight: Float = 0.5f,
    ): Comparator<SourceMetadata> {
        return compareByDescending { source ->
            var score = 0f

            // Quality score with weight
            score += source.getQualityScore() * qualityWeight

            // Health score with weight
            val seeders = source.health.seeders ?: 0
            val healthScore =
                when {
                    seeders > 1000 -> 100
                    seeders > 500 -> 80
                    seeders > 100 -> 60
                    seeders > 50 -> 40
                    seeders > 10 -> 20
                    seeders > 0 -> 10
                    else -> 0
                }
            score += healthScore * healthWeight

            // Size score (prefer reasonable sizes) with weight
            val sizeInGB = source.file.sizeInBytes?.let { it / (1024.0 * 1024.0 * 1024.0) } ?: 0.0
            val sizeScore =
                when {
                    sizeInGB in 2.0..15.0 -> 50 // Good size range
                    sizeInGB in 1.0..25.0 -> 30 // Acceptable range
                    sizeInGB < 1.0 -> 10 // Too small
                    else -> 20 // Too large
                }
            score += sizeScore * sizeWeight

            // Provider reliability with weight
            score += source.provider.reliability.ordinal * 20 * providerWeight

            score
        }
    }

    /**
     * Chain multiple comparators in priority order
     */
    fun chain(vararg comparators: Comparator<SourceMetadata>): Comparator<SourceMetadata> {
        return comparators.reduce { acc, comparator -> acc.thenComparing(comparator) }
    }
}

/**
 * Extension functions for easier comparator usage
 */
fun List<SourceMetadata>.sortByQuality(): List<SourceMetadata> {
    return sortedWith(SourceComparators.qualityComparator)
}

fun List<SourceMetadata>.sortByHealth(): List<SourceMetadata> {
    return sortedWith(SourceComparators.healthComparator)
}

fun List<SourceMetadata>.sortBySize(largestFirst: Boolean = false): List<SourceMetadata> {
    return sortedWith(
        if (largestFirst) {
            SourceComparators.sizeLargestFirstComparator
        } else {
            SourceComparators.sizeComparator
        },
    )
}

fun List<SourceMetadata>.sortCachedFirst(): List<SourceMetadata> {
    return sortedWith(
        SourceComparators.cachedFirstComparator
            .thenComparing(SourceComparators.qualityComparator),
    )
}

fun List<SourceMetadata>.sortForAndroidTV(): List<SourceMetadata> {
    return sortedWith(SourceComparators.androidTVOptimizedComparator)
}

/**
 * Stability helpers for consistent sorting
 */
object SortingStability {
    /**
     * Add stable secondary sort criteria to ensure consistent ordering
     */
    fun makeStable(comparator: Comparator<SourceMetadata>): Comparator<SourceMetadata> {
        return comparator.thenBy { it.id }
    }

    /**
     * Create a deterministic comparator that always produces the same order
     */
    fun createDeterministic(): Comparator<SourceMetadata> {
        return compareBy<SourceMetadata> { it.id }
            .thenBy { it.provider.id }
            .thenBy { it.file.hash ?: "" }
    }
}
