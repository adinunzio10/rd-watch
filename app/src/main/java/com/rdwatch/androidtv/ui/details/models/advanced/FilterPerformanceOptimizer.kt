package com.rdwatch.androidtv.ui.details.models.advanced

import kotlinx.coroutines.*
import kotlin.system.measureTimeMillis

/**
 * Performance optimization system for filtering large source lists
 */
class FilterPerformanceOptimizer {
    
    private val filterCache = mutableMapOf<String, FilterResult>()
    private val maxCacheSize = 50
    private val cacheExpiryMs = 5 * 60 * 1000L // 5 minutes
    
    /**
     * Apply filtering with performance optimizations
     */
    suspend fun optimizedFilter(
        sources: List<SourceMetadata>,
        filter: AdvancedSourceFilter,
        maxResults: Int = 100
    ): FilterResult = withContext(Dispatchers.Default) {
        val cacheKey = generateCacheKey(sources, filter)
        
        // Check cache first
        filterCache[cacheKey]?.let { cached ->
            if (System.currentTimeMillis() - cached.processingTimeMs < cacheExpiryMs) {
                return@withContext cached
            }
        }
        
        val result = when {
            sources.size > 10000 -> largeBatchFilter(sources, filter, maxResults)
            sources.size > 1000 -> mediumBatchFilter(sources, filter, maxResults)
            else -> standardFilter(sources, filter)
        }
        
        // Cache result
        updateCache(cacheKey, result)
        
        result
    }
    
    /**
     * Standard filtering for small to medium lists
     */
    private suspend fun standardFilter(
        sources: List<SourceMetadata>,
        filter: AdvancedSourceFilter
    ): FilterResult {
        val filterSystem = SourceFilterSystem()
        return filterSystem.filterSources(sources, filter)
    }
    
    /**
     * Optimized filtering for medium-sized lists
     */
    private suspend fun mediumBatchFilter(
        sources: List<SourceMetadata>,
        filter: AdvancedSourceFilter,
        maxResults: Int
    ): FilterResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        val batchSize = 250
        val batches = sources.chunked(batchSize)
        val filterSystem = SourceFilterSystem()
        
        val results = mutableListOf<SourceMetadata>()
        var totalEvaluated = 0
        
        // Process batches in parallel
        val jobs = batches.map { batch ->
            async {
                val batchResult = filterSystem.filterSources(batch, filter)
                totalEvaluated += batch.size
                batchResult.filteredSources
            }
        }
        
        // Collect results with early termination if we have enough
        for (job in jobs) {
            results.addAll(job.await())
            if (results.size >= maxResults) {
                // Cancel remaining jobs
                jobs.forEach { if (!it.isCompleted) it.cancel() }
                break
            }
        }
        
        FilterResult(
            filteredSources = results.take(maxResults),
            appliedFilters = getFilterDescriptions(filter),
            processingTimeMs = System.currentTimeMillis() - startTime,
            totalSourcesEvaluated = totalEvaluated,
            filtersApplied = filter.getActiveFilterCount()
        )
    }
    
    /**
     * Highly optimized filtering for large lists
     */
    private suspend fun largeBatchFilter(
        sources: List<SourceMetadata>,
        filter: AdvancedSourceFilter,
        maxResults: Int
    ): FilterResult = withContext(Dispatchers.Default) {
        val startTime = System.currentTimeMillis()
        
        // Pre-filter with most selective criteria first
        val preFiltered = applyPreFiltering(sources, filter)
        
        if (preFiltered.size <= 1000) {
            // If pre-filtering reduced size significantly, use standard approach
            return@withContext standardFilter(preFiltered, filter).copy(
                totalSourcesEvaluated = sources.size,
                processingTimeMs = System.currentTimeMillis() - startTime
            )
        }
        
        // Use streaming approach for very large lists
        val result = streamingFilter(preFiltered, filter, maxResults)
        
        result.copy(
            totalSourcesEvaluated = sources.size,
            processingTimeMs = System.currentTimeMillis() - startTime
        )
    }
    
    /**
     * Pre-filter with most selective criteria to reduce dataset
     */
    private fun applyPreFiltering(
        sources: List<SourceMetadata>,
        filter: AdvancedSourceFilter
    ): List<SourceMetadata> {
        var filtered = sources
        
        // Apply most selective filters first for performance
        
        // 1. Cached filter (usually very selective)
        if (filter.sourceTypeFilters.cachedOnly) {
            filtered = filtered.filter { it.availability.cached }
        }
        
        // 2. Provider type filter
        if (filter.sourceTypeFilters.allowedProviderTypes.isNotEmpty()) {
            filtered = filtered.filter { 
                it.provider.type in filter.sourceTypeFilters.allowedProviderTypes 
            }
        }
        
        // 3. Resolution filter (often very selective)
        filter.qualityFilters.minResolution?.let { minRes ->
            filtered = filtered.filter { it.quality.resolution.ordinal >= minRes.ordinal }
        }
        
        // 4. Size filter (can be very selective)
        filter.fileSizeFilters.maxSizeGB?.let { maxSize ->
            filtered = filtered.filter { source ->
                source.file.sizeInBytes?.let { sizeBytes ->
                    val sizeGB = sizeBytes / (1024.0 * 1024.0 * 1024.0)
                    sizeGB <= maxSize
                } ?: !filter.fileSizeFilters.requireSizeInfo
            }
        }
        
        // 5. Health filter (for P2P)
        filter.healthFilters.minSeeders?.let { minSeeders ->
            filtered = filtered.filter { source ->
                source.health.seeders?.let { it >= minSeeders } 
                    ?: !filter.healthFilters.requireSeederInfo
            }
        }
        
        return filtered
    }
    
    /**
     * Streaming filter for very large datasets
     */
    private suspend fun streamingFilter(
        sources: List<SourceMetadata>,
        filter: AdvancedSourceFilter,
        maxResults: Int
    ): FilterResult = withContext(Dispatchers.Default) {
        val filterSystem = SourceFilterSystem()
        val results = mutableListOf<SourceMetadata>()
        val batchSize = 100
        var processed = 0
        
        for (batch in sources.chunked(batchSize)) {
            val batchResult = filterSystem.filterSources(batch, filter)
            results.addAll(batchResult.filteredSources)
            processed += batch.size
            
            // Early termination if we have enough results
            if (results.size >= maxResults) {
                break
            }
            
            // Yield to prevent blocking
            if (processed % 500 == 0) {
                yield()
            }
        }
        
        FilterResult(
            filteredSources = results.take(maxResults),
            appliedFilters = getFilterDescriptions(filter),
            processingTimeMs = 0, // Will be set by caller
            totalSourcesEvaluated = processed,
            filtersApplied = filter.getActiveFilterCount()
        )
    }
    
    /**
     * Parallel filtering for multiple filter presets
     */
    suspend fun parallelPresetFiltering(
        sources: List<SourceMetadata>,
        presets: Map<String, AdvancedSourceFilter>,
        maxResultsPerPreset: Int = 50
    ): Map<String, FilterResult> = withContext(Dispatchers.Default) {
        
        val jobs = presets.map { (name, filter) ->
            async {
                name to optimizedFilter(sources, filter, maxResultsPerPreset)
            }
        }
        
        jobs.awaitAll().toMap()
    }
    
    /**
     * Filter with result prioritization
     */
    suspend fun prioritizedFilter(
        sources: List<SourceMetadata>,
        filter: AdvancedSourceFilter,
        maxResults: Int = 100,
        sortPreferences: UserSortingPreferences = UserSortingPreferences()
    ): FilterResult = withContext(Dispatchers.Default) {
        
        // Apply filter first
        val filterResult = optimizedFilter(sources, filter, maxResults * 2) // Get more for sorting
        
        // Sort the filtered results
        val sorter = SourceSorter()
        val sortedSources = sorter.sortSources(filterResult.filteredSources, sortPreferences)
        
        filterResult.copy(
            filteredSources = sortedSources.take(maxResults)
        )
    }
    
    /**
     * Progressive filtering with user feedback
     */
    suspend fun progressiveFilter(
        sources: List<SourceMetadata>,
        filter: AdvancedSourceFilter,
        progressCallback: suspend (Int, Int) -> Unit
    ): FilterResult = withContext(Dispatchers.Default) {
        val filterSystem = SourceFilterSystem()
        val results = mutableListOf<SourceMetadata>()
        val batchSize = 200
        val batches = sources.chunked(batchSize)
        var processed = 0
        
        for ((index, batch) in batches.withIndex()) {
            val batchResult = filterSystem.filterSources(batch, filter)
            results.addAll(batchResult.filteredSources)
            processed += batch.size
            
            // Report progress
            progressCallback(processed, sources.size)
            
            // Yield every few batches
            if (index % 5 == 0) {
                yield()
            }
        }
        
        FilterResult(
            filteredSources = results,
            appliedFilters = getFilterDescriptions(filter),
            processingTimeMs = 0,
            totalSourcesEvaluated = sources.size,
            filtersApplied = filter.getActiveFilterCount()
        )
    }
    
    /**
     * Filter with quality-based early termination
     */
    suspend fun qualityThresholdFilter(
        sources: List<SourceMetadata>,
        filter: AdvancedSourceFilter,
        qualityThreshold: Int = 800, // Stop when we find high quality sources
        minResults: Int = 10
    ): FilterResult = withContext(Dispatchers.Default) {
        val filterSystem = SourceFilterSystem()
        val results = mutableListOf<SourceMetadata>()
        val highQualityResults = mutableListOf<SourceMetadata>()
        val batchSize = 100
        var processed = 0
        
        for (batch in sources.chunked(batchSize)) {
            val batchResult = filterSystem.filterSources(batch, filter)
            results.addAll(batchResult.filteredSources)
            
            // Check for high quality sources
            for (source in batchResult.filteredSources) {
                if (source.getQualityScore() >= qualityThreshold) {
                    highQualityResults.add(source)
                }
            }
            
            processed += batch.size
            
            // Early termination if we have enough high quality results
            if (highQualityResults.size >= minResults && processed > sources.size / 4) {
                break
            }
        }
        
        FilterResult(
            filteredSources = if (highQualityResults.size >= minResults) {
                highQualityResults
            } else {
                results
            },
            appliedFilters = getFilterDescriptions(filter),
            processingTimeMs = 0,
            totalSourcesEvaluated = processed,
            filtersApplied = filter.getActiveFilterCount()
        )
    }
    
    /**
     * Generate cache key for filter result
     */
    private fun generateCacheKey(sources: List<SourceMetadata>, filter: AdvancedSourceFilter): String {
        val sourcesHash = sources.map { it.id }.hashCode()
        val filterHash = filter.hashCode()
        return "$sourcesHash-$filterHash"
    }
    
    /**
     * Update cache with size management
     */
    private fun updateCache(key: String, result: FilterResult) {
        // Remove old entries if cache is full
        if (filterCache.size >= maxCacheSize) {
            val oldestKey = filterCache.keys.first()
            filterCache.remove(oldestKey)
        }
        
        filterCache[key] = result
    }
    
    /**
     * Clear filter cache
     */
    fun clearCache() {
        filterCache.clear()
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return CacheStats(
            size = filterCache.size,
            maxSize = maxCacheSize,
            hitCount = 0, // Would need to track this
            missCount = 0  // Would need to track this
        )
    }
    
    /**
     * Get filter descriptions for result
     */
    private fun getFilterDescriptions(filter: AdvancedSourceFilter): List<String> {
        // Simplified version - full implementation would be more comprehensive
        val descriptions = mutableListOf<String>()
        
        if (!filter.qualityFilters.isEmpty()) descriptions.add("Quality filters")
        if (!filter.sourceTypeFilters.isEmpty()) descriptions.add("Source type filters")
        if (!filter.healthFilters.isEmpty()) descriptions.add("Health filters")
        if (!filter.fileSizeFilters.isEmpty()) descriptions.add("File size filters")
        
        return descriptions
    }
}

/**
 * Cache statistics
 */
data class CacheStats(
    val size: Int,
    val maxSize: Int,
    val hitCount: Long,
    val missCount: Long
) {
    val hitRate: Double get() = if (hitCount + missCount > 0) {
        hitCount.toDouble() / (hitCount + missCount)
    } else 0.0
}

/**
 * Performance metrics for filtering operations
 */
data class FilterPerformanceMetrics(
    val totalSources: Int,
    val filteredSources: Int,
    val processingTimeMs: Long,
    val memoryUsageMB: Double,
    val cacheHitRate: Double,
    val optimizationUsed: String
) {
    val filteringRate: Double get() = totalSources.toDouble() / (processingTimeMs / 1000.0)
    val reductionRatio: Double get() = filteredSources.toDouble() / totalSources
}