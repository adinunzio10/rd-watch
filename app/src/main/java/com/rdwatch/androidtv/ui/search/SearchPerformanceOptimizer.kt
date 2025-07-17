package com.rdwatch.androidtv.ui.search

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Performance optimization for search operations
 * Implements caching, debouncing, prefetching, and performance monitoring
 */
@Singleton
class SearchPerformanceOptimizer
    @Inject
    constructor() {
        private val searchCache = ConcurrentHashMap<String, CachedSearchResult>()
        private val queryMetrics = ConcurrentHashMap<String, QueryMetrics>()
        private val performanceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        // Performance thresholds
        private val cacheExpirationMs = 10 * 60 * 1000L // 10 minutes
        private val maxCacheSize = 100
        private val debounceDelayMs = 300L
        private val prefetchThreshold = 3 // Min chars to start prefetching

        /**
         * Optimize search with caching, debouncing, and performance monitoring
         */
        fun optimizeSearch(
            query: String,
            filters: SearchFilters,
            searchFunction: suspend (String, SearchFilters) -> List<SearchResultItem>,
        ): Flow<OptimizedSearchResult> =
            flow {
                val startTime = System.currentTimeMillis()
                val cacheKey = generateCacheKey(query, filters)

                try {
                    // Check cache first
                    searchCache[cacheKey]?.let { cachedResult ->
                        if (!cachedResult.isExpired()) {
                            emit(OptimizedSearchResult.CacheHit(cachedResult.results, cachedResult.timestamp))
                            recordMetrics(query, System.currentTimeMillis() - startTime, true)
                            return@flow
                        }
                    }

                    emit(OptimizedSearchResult.Searching)

                    // Execute search with timeout
                    val results =
                        withTimeout(30000L) { // 30 second timeout
                            searchFunction(query, filters)
                        }

                    val responseTime = System.currentTimeMillis() - startTime

                    // Cache results
                    cacheSearchResults(cacheKey, results)

                    // Record metrics
                    recordMetrics(query, responseTime, false)

                    // Emit results
                    emit(OptimizedSearchResult.Success(results, responseTime))

                    // Start prefetching related queries
                    if (query.length >= prefetchThreshold) {
                        startPrefetching(query, filters, searchFunction)
                    }
                } catch (e: TimeoutCancellationException) {
                    emit(OptimizedSearchResult.Timeout)
                    recordMetrics(query, System.currentTimeMillis() - startTime, false, true)
                } catch (e: Exception) {
                    emit(OptimizedSearchResult.Error(e.message ?: "Search failed"))
                    recordMetrics(query, System.currentTimeMillis() - startTime, false, false, e.message)
                }
            }.flowOn(Dispatchers.IO)

        /**
         * Debounced search to reduce API calls during typing
         */
        fun createDebouncedSearch(
            searchFunction: suspend (String, SearchFilters) -> List<SearchResultItem>,
        ): (String, SearchFilters) -> Flow<OptimizedSearchResult> {
            return { query: String, filters: SearchFilters ->
                flow {
                    emit(query to filters)
                }
                    .debounce(debounceDelayMs)
                    .flatMapLatest { (debouncedQuery, debouncedFilters) ->
                        if (debouncedQuery.isBlank()) {
                            flowOf(OptimizedSearchResult.Empty)
                        } else {
                            optimizeSearch(debouncedQuery, debouncedFilters, searchFunction)
                        }
                    }
            }
        }

        /**
         * Prefetch related search queries
         */
        private fun startPrefetching(
            baseQuery: String,
            filters: SearchFilters,
            searchFunction: suspend (String, SearchFilters) -> List<SearchResultItem>,
        ) {
            performanceScope.launch {
                val prefetchQueries = generatePrefetchQueries(baseQuery)

                prefetchQueries.forEach { prefetchQuery ->
                    val cacheKey = generateCacheKey(prefetchQuery, filters)

                    // Only prefetch if not already cached
                    if (!searchCache.containsKey(cacheKey)) {
                        try {
                            delay(1000) // Small delay to not overwhelm APIs
                            val results = searchFunction(prefetchQuery, filters)
                            cacheSearchResults(cacheKey, results)
                        } catch (e: Exception) {
                            // Ignore prefetch errors
                        }
                    }
                }
            }
        }

        /**
         * Generate potential prefetch queries based on the current query
         */
        private fun generatePrefetchQueries(baseQuery: String): List<String> {
            val queries = mutableListOf<String>()

            // Add variations with common suffixes
            val commonSuffixes = listOf(" movie", " tv show", " series", " season", " episode")
            commonSuffixes.forEach { suffix ->
                if (!baseQuery.contains(suffix, ignoreCase = true)) {
                    queries.add("$baseQuery$suffix")
                }
            }

            // Add partial queries (for autocomplete)
            if (baseQuery.length > 3) {
                for (i in 3..baseQuery.length) {
                    queries.add(baseQuery.substring(0, i))
                }
            }

            return queries.take(5) // Limit prefetch queries
        }

        /**
         * Cache search results with expiration
         */
        private fun cacheSearchResults(
            cacheKey: String,
            results: List<SearchResultItem>,
        ) {
            // Implement LRU eviction if cache is full
            if (searchCache.size >= maxCacheSize) {
                val oldestKey =
                    searchCache.entries
                        .minByOrNull { it.value.timestamp }
                        ?.key

                oldestKey?.let { searchCache.remove(it) }
            }

            searchCache[cacheKey] =
                CachedSearchResult(
                    results = results,
                    timestamp = System.currentTimeMillis(),
                )
        }

        /**
         * Generate cache key from query and filters
         */
        private fun generateCacheKey(
            query: String,
            filters: SearchFilters,
        ): String {
            val filterHash = filters.hashCode()
            return "${query.lowercase().trim()}_$filterHash"
        }

        /**
         * Record performance metrics
         */
        private fun recordMetrics(
            query: String,
            responseTimeMs: Long,
            wasCacheHit: Boolean,
            wasTimeout: Boolean = false,
            error: String? = null,
        ) {
            val key = query.lowercase().trim()
            val existingMetrics = queryMetrics[key] ?: QueryMetrics(query = key)

            val updatedMetrics =
                existingMetrics.copy(
                    totalSearches = existingMetrics.totalSearches + 1,
                    totalResponseTimeMs = existingMetrics.totalResponseTimeMs + responseTimeMs,
                    cacheHits = existingMetrics.cacheHits + (if (wasCacheHit) 1 else 0),
                    timeouts = existingMetrics.timeouts + (if (wasTimeout) 1 else 0),
                    errors = existingMetrics.errors + (if (error != null) 1 else 0),
                    lastSearchTime = System.currentTimeMillis(),
                    lastError = error ?: existingMetrics.lastError,
                )

            queryMetrics[key] = updatedMetrics
        }

        /**
         * Get performance statistics
         */
        fun getPerformanceStats(): PerformanceStats {
            val totalQueries = queryMetrics.values.sumOf { it.totalSearches }
            val totalCacheHits = queryMetrics.values.sumOf { it.cacheHits }
            val totalTimeouts = queryMetrics.values.sumOf { it.timeouts }
            val totalErrors = queryMetrics.values.sumOf { it.errors }

            val averageResponseTime =
                if (totalQueries > 0) {
                    queryMetrics.values.sumOf { it.totalResponseTimeMs } / totalQueries
                } else {
                    0L
                }

            val cacheHitRate =
                if (totalQueries > 0) {
                    (totalCacheHits.toDouble() / totalQueries) * 100
                } else {
                    0.0
                }

            val slowQueries =
                queryMetrics.values
                    .filter { it.getAverageResponseTime() > 5000 } // Slower than 5 seconds
                    .sortedByDescending { it.getAverageResponseTime() }
                    .take(10)

            return PerformanceStats(
                totalQueries = totalQueries,
                averageResponseTimeMs = averageResponseTime,
                cacheHitRate = cacheHitRate,
                totalTimeouts = totalTimeouts,
                totalErrors = totalErrors,
                currentCacheSize = searchCache.size,
                slowQueries = slowQueries.map { it.query },
            )
        }

        /**
         * Get metrics for a specific query
         */
        fun getQueryMetrics(query: String): QueryMetrics? {
            return queryMetrics[query.lowercase().trim()]
        }

        /**
         * Clear performance cache
         */
        fun clearCache() {
            searchCache.clear()
        }

        /**
         * Clear performance metrics
         */
        fun clearMetrics() {
            queryMetrics.clear()
        }

        /**
         * Optimize cache by removing expired entries
         */
        fun optimizeCache() {
            val currentTime = System.currentTimeMillis()
            val expiredKeys =
                searchCache.entries
                    .filter { currentTime - it.value.timestamp > cacheExpirationMs }
                    .map { it.key }

            expiredKeys.forEach { searchCache.remove(it) }
        }

        /**
         * Get cache statistics
         */
        fun getCacheStats(): CacheStats {
            val currentTime = System.currentTimeMillis()
            val expiredEntries =
                searchCache.values.count {
                    currentTime - it.timestamp > cacheExpirationMs
                }

            return CacheStats(
                totalEntries = searchCache.size,
                expiredEntries = expiredEntries,
                hitRate = getPerformanceStats().cacheHitRate,
                averageEntryAge =
                    if (searchCache.isNotEmpty()) {
                        searchCache.values.map { currentTime - it.timestamp }.average().toLong()
                    } else {
                        0L
                    },
            )
        }

        /**
         * Start background optimization tasks
         */
        fun startBackgroundOptimization() {
            performanceScope.launch {
                while (isActive) {
                    delay(60000) // Run every minute
                    optimizeCache()
                }
            }
        }

        /**
         * Shutdown optimization services
         */
        fun shutdown() {
            performanceScope.cancel()
            clearCache()
        }
    }

/**
 * Cached search result with timestamp
 */
private data class CachedSearchResult(
    val results: List<SearchResultItem>,
    val timestamp: Long,
) {
    fun isExpired(expirationMs: Long = 10 * 60 * 1000L): Boolean {
        return System.currentTimeMillis() - timestamp > expirationMs
    }
}

/**
 * Performance metrics for a query
 */
data class QueryMetrics(
    val query: String,
    val totalSearches: Int = 0,
    val totalResponseTimeMs: Long = 0,
    val cacheHits: Int = 0,
    val timeouts: Int = 0,
    val errors: Int = 0,
    val lastSearchTime: Long = 0,
    val lastError: String? = null,
) {
    fun getAverageResponseTime(): Long {
        return if (totalSearches > 0) totalResponseTimeMs / totalSearches else 0L
    }

    fun getSuccessRate(): Double {
        return if (totalSearches > 0) {
            ((totalSearches - timeouts - errors).toDouble() / totalSearches) * 100
        } else {
            0.0
        }
    }
}

/**
 * Overall performance statistics
 */
data class PerformanceStats(
    val totalQueries: Int,
    val averageResponseTimeMs: Long,
    val cacheHitRate: Double,
    val totalTimeouts: Int,
    val totalErrors: Int,
    val currentCacheSize: Int,
    val slowQueries: List<String>,
)

/**
 * Cache statistics
 */
data class CacheStats(
    val totalEntries: Int,
    val expiredEntries: Int,
    val hitRate: Double,
    val averageEntryAge: Long,
)

/**
 * Optimized search result types
 */
sealed class OptimizedSearchResult {
    data object Empty : OptimizedSearchResult()

    data object Searching : OptimizedSearchResult()

    data class CacheHit(val results: List<SearchResultItem>, val cacheTime: Long) : OptimizedSearchResult()

    data class Success(val results: List<SearchResultItem>, val responseTime: Long) : OptimizedSearchResult()

    data object Timeout : OptimizedSearchResult()

    data class Error(val message: String) : OptimizedSearchResult()
}

/**
 * Search performance configuration
 */
data class PerformanceConfig(
    val cacheExpirationMinutes: Int = 10,
    val maxCacheSize: Int = 100,
    val debounceDelayMs: Long = 300L,
    val searchTimeoutMs: Long = 30000L,
    val prefetchEnabled: Boolean = true,
    val prefetchThreshold: Int = 3,
    val backgroundOptimizationEnabled: Boolean = true,
)
