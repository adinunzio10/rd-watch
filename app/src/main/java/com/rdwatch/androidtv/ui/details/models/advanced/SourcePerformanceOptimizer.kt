package com.rdwatch.androidtv.ui.details.models.advanced

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*

/**
 * Performance optimizer for source sorting and filtering on Android TV
 * Optimizes for memory usage, processing time, and UI responsiveness
 */
class SourcePerformanceOptimizer(
    private val configuration: PerformanceConfiguration = PerformanceConfiguration(),
) {
    private val sorter = SourceSorter()
    private val analytics = SourceAnalytics()

    /**
     * Process sources with performance optimizations for Android TV
     */
    suspend fun processSourcesOptimized(
        sources: List<SourceMetadata>,
        preferences: UserSortingPreferences = UserSortingPreferences(),
        maxResults: Int = configuration.maxDisplaySources,
    ): ProcessedSourceResult =
        withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()

            try {
                // Pre-filter obviously bad sources early
                val preFiltered = preFilterSources(sources)

                // Use chunked processing for large source lists
                val processed =
                    if (preFiltered.size > configuration.chunkThreshold) {
                        processSourcesInChunks(preFiltered, preferences, maxResults)
                    } else {
                        processSourcesDirect(preFiltered, preferences, maxResults)
                    }

                val processingTime = System.currentTimeMillis() - startTime

                ProcessedSourceResult(
                    sources = processed,
                    totalProcessed = sources.size,
                    finalCount = processed.size,
                    processingTimeMs = processingTime,
                    optimizationsApplied = getOptimizationsApplied(sources.size, preFiltered.size),
                )
            } catch (e: Exception) {
                ProcessedSourceResult(
                    sources = emptyList(),
                    totalProcessed = sources.size,
                    finalCount = 0,
                    processingTimeMs = System.currentTimeMillis() - startTime,
                    error = e.message,
                )
            }
        }

    /**
     * Pre-filter sources to remove obviously unusable ones
     */
    private fun preFilterSources(sources: List<SourceMetadata>): List<SourceMetadata> {
        return sources.filter { source ->
            // Remove dead torrents
            if (source.health.seeders == 0) return@filter false

            // Remove extremely low quality
            if (source.quality.resolution == VideoResolution.RESOLUTION_240P ||
                source.quality.resolution == VideoResolution.UNKNOWN
            ) {
                return@filter false
            }

            // Remove sources without file information if too many sources
            if (sources.size > configuration.strictFilterThreshold &&
                source.file.sizeInBytes == null
            ) {
                return@filter false
            }

            // Remove untrusted providers if too many sources
            if (sources.size > configuration.strictFilterThreshold &&
                source.provider.reliability == SourceProviderInfo.ProviderReliability.POOR
            ) {
                return@filter false
            }

            true
        }
    }

    /**
     * Process sources directly for smaller lists
     */
    private suspend fun processSourcesDirect(
        sources: List<SourceMetadata>,
        preferences: UserSortingPreferences,
        maxResults: Int,
    ): List<SourceMetadata> {
        return sorter.getTopSources(sources, maxResults, preferences)
    }

    /**
     * Process sources in chunks for better memory usage and responsiveness
     */
    private suspend fun processSourcesInChunks(
        sources: List<SourceMetadata>,
        preferences: UserSortingPreferences,
        maxResults: Int,
    ): List<SourceMetadata> =
        withContext(Dispatchers.Default) {
            val chunkSize = configuration.chunkSize
            val chunks = sources.chunked(chunkSize)
            val topSourcesPerChunk = maxResults / chunks.size + 1

            // Process chunks in parallel
            val processedChunks =
                chunks.map { chunk ->
                    async {
                        sorter.getTopSources(chunk, topSourcesPerChunk, preferences)
                    }
                }.awaitAll()

            // Merge and get final top sources
            val merged = processedChunks.flatten()
            sorter.getTopSources(merged, maxResults, preferences)
        }

    /**
     * Create a reactive source processor for real-time updates
     */
    fun createReactiveProcessor(preferences: UserSortingPreferences = UserSortingPreferences()): SourceProcessor {
        return SourceProcessor(sorter, configuration, preferences)
    }

    /**
     * Optimize sources specifically for Android TV UI performance
     */
    suspend fun optimizeForAndroidTV(
        sources: List<SourceMetadata>,
        preferences: UserSortingPreferences = UserSortingPreferences(),
    ): AndroidTVOptimizedResult =
        withContext(Dispatchers.Default) {
            val startTime = System.currentTimeMillis()

            // Separate cached and non-cached for immediate display
            val (cached, nonCached) = sources.partition { it.availability.cached }

            // Process cached sources first (these can display immediately)
            val topCached =
                if (cached.isNotEmpty()) {
                    sorter.getTopSources(cached, configuration.maxCachedSources, preferences)
                } else {
                    emptyList()
                }

            // Process non-cached sources in background
            val topNonCached =
                if (nonCached.isNotEmpty()) {
                    sorter.getTopSources(
                        nonCached,
                        configuration.maxDisplaySources - topCached.size,
                        preferences,
                    )
                } else {
                    emptyList()
                }

            val processingTime = System.currentTimeMillis() - startTime

            AndroidTVOptimizedResult(
                immediatelyAvailable = topCached,
                backgroundProcessed = topNonCached,
                totalProcessingTime = processingTime,
                cacheHitRatio = cached.size.toFloat() / sources.size,
            )
        }

    /**
     * Monitor performance and adjust configuration dynamically
     */
    fun createPerformanceMonitor(): PerformanceMonitor {
        return PerformanceMonitor(configuration)
    }

    /**
     * Get optimizations that were applied
     */
    private fun getOptimizationsApplied(
        originalSize: Int,
        filteredSize: Int,
    ): List<String> {
        val optimizations = mutableListOf<String>()

        if (filteredSize < originalSize) {
            optimizations.add("Pre-filtering removed ${originalSize - filteredSize} sources")
        }

        if (originalSize > configuration.chunkThreshold) {
            optimizations.add("Chunked processing for $originalSize sources")
        }

        return optimizations
    }
}

/**
 * Reactive source processor for real-time updates
 */
class SourceProcessor(
    private val sorter: SourceSorter,
    private val configuration: PerformanceConfiguration,
    private val preferences: UserSortingPreferences,
) {
    private val _sources = MutableStateFlow<List<SourceMetadata>>(emptyList())
    private val _isProcessing = MutableStateFlow(false)

    val sources: StateFlow<List<SourceMetadata>> = _sources.asStateFlow()
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    /**
     * Process sources with debouncing to avoid excessive processing
     */
    fun updateSources(newSources: List<SourceMetadata>) {
        MainScope().launch {
            _isProcessing.value = true

            // Debounce rapid updates
            delay(configuration.debounceMs)

            val processed =
                withContext(Dispatchers.Default) {
                    sorter.getTopSources(newSources, configuration.maxDisplaySources, preferences)
                }

            _sources.value = processed
            _isProcessing.value = false
        }
    }

    /**
     * Add new sources incrementally
     */
    fun addSources(additionalSources: List<SourceMetadata>) {
        val current = _sources.value
        val combined = current + additionalSources
        updateSources(combined)
    }
}

/**
 * Performance monitoring for adaptive optimization
 */
class PerformanceMonitor(
    private val configuration: PerformanceConfiguration,
) {
    private val processingTimes = mutableListOf<Long>()
    private val maxHistorySize = 50

    fun recordProcessingTime(timeMs: Long) {
        processingTimes.add(timeMs)
        if (processingTimes.size > maxHistorySize) {
            processingTimes.removeAt(0)
        }

        // Adaptive optimization based on performance
        if (getAverageProcessingTime() > configuration.targetProcessingTimeMs) {
            suggestOptimizations()
        }
    }

    fun getAverageProcessingTime(): Double {
        return if (processingTimes.isNotEmpty()) {
            processingTimes.average()
        } else {
            0.0
        }
    }

    fun getPerformanceMetrics(): ProcessingMetrics {
        return ProcessingMetrics(
            averageProcessingTime = getAverageProcessingTime(),
            minProcessingTime = processingTimes.minOrNull()?.toDouble() ?: 0.0,
            maxProcessingTime = processingTimes.maxOrNull()?.toDouble() ?: 0.0,
            sampleSize = processingTimes.size,
        )
    }

    private fun suggestOptimizations(): List<String> {
        val suggestions = mutableListOf<String>()

        if (getAverageProcessingTime() > configuration.targetProcessingTimeMs * 2) {
            suggestions.add("Consider reducing max display sources")
            suggestions.add("Enable more aggressive pre-filtering")
        }

        return suggestions
    }
}

/**
 * Configuration for performance optimization
 */
data class PerformanceConfiguration(
    // Max sources to show in UI
    val maxDisplaySources: Int = 50,
    // Max cached sources to prioritize
    val maxCachedSources: Int = 20,
    // When to use chunked processing
    val chunkThreshold: Int = 100,
    // Size of each processing chunk
    val chunkSize: Int = 25,
    // When to apply strict filtering
    val strictFilterThreshold: Int = 200,
    // Debounce time for reactive updates
    val debounceMs: Long = 300,
    // Target processing time
    val targetProcessingTimeMs: Long = 1000,
)

/**
 * Result of optimized source processing
 */
data class ProcessedSourceResult(
    val sources: List<SourceMetadata>,
    val totalProcessed: Int,
    val finalCount: Int,
    val processingTimeMs: Long,
    val optimizationsApplied: List<String> = emptyList(),
    val error: String? = null,
)

/**
 * Android TV specific optimization result
 */
data class AndroidTVOptimizedResult(
    // Cached sources for immediate display
    val immediatelyAvailable: List<SourceMetadata>,
    // Non-cached sources
    val backgroundProcessed: List<SourceMetadata>,
    val totalProcessingTime: Long,
    val cacheHitRatio: Float,
)

/**
 * Processing metrics for performance monitoring
 */
data class ProcessingMetrics(
    val averageProcessingTime: Double,
    val minProcessingTime: Double,
    val maxProcessingTime: Double,
    val sampleSize: Int,
)

/**
 * Extension functions for easy performance optimization
 */
suspend fun List<SourceMetadata>.optimizeForAndroidTV(preferences: UserSortingPreferences = UserSortingPreferences()): AndroidTVOptimizedResult {
    val optimizer = SourcePerformanceOptimizer()
    return optimizer.optimizeForAndroidTV(this, preferences)
}

suspend fun List<SourceMetadata>.processOptimized(
    preferences: UserSortingPreferences = UserSortingPreferences(),
    maxResults: Int = 50,
): ProcessedSourceResult {
    val optimizer = SourcePerformanceOptimizer()
    return optimizer.processSourcesOptimized(this, preferences, maxResults)
}
