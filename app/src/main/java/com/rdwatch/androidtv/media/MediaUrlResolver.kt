package com.rdwatch.androidtv.media

import androidx.annotation.VisibleForTesting
import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.network.CircuitBreaker
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.base.Result
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Named

/**
 * Resolves media URLs with intelligent caching and circuit breaker protection.
 *
 * Features:
 * - LRU cache with configurable size limit (default: 100 entries)
 * - Time-based cache expiration (default: 30 minutes)
 * - Thread-safe concurrent operations
 * - Cache invalidation support
 * - Circuit breaker protection for Real-Debrid API calls
 * - Optimized for Torrentio and Real-Debrid URL patterns
 */
class MediaUrlResolver
    @Inject
    constructor(
        private val realDebridRepository: RealDebridContentRepository,
        private val dispatcherProvider: DispatcherProvider,
        @Named("real-debrid") private val realDebridCircuitBreaker: CircuitBreaker,
    ) {
        companion object {
            private const val DEFAULT_CACHE_SIZE = 100
            private const val DEFAULT_CACHE_EXPIRATION_MINUTES = 30L
            private const val TAG = "MediaUrlResolver"
        }

        // Cache configuration
        private val cacheSize = DEFAULT_CACHE_SIZE
        private val cacheExpirationMs = TimeUnit.MINUTES.toMillis(DEFAULT_CACHE_EXPIRATION_MINUTES)

        // LRU Cache implementation using LinkedHashMap
        private val urlCache =
            object : LinkedHashMap<String, CachedUrl>(cacheSize + 1, 0.75f, true) {
                override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedUrl>?): Boolean {
                    return size > cacheSize
                }
            }

        // Concurrent access protection
        private val cacheMutex = Mutex()
        private val resolutionMutex = ConcurrentHashMap<String, Mutex>()

        /**
         * Represents a cached URL with expiration timestamp
         */
        private data class CachedUrl(
            val resolvedUrl: String,
            val timestamp: Long,
            val originalUrl: String,
        ) {
            fun isExpired(expirationMs: Long): Boolean {
                return System.currentTimeMillis() - timestamp > expirationMs
            }
        }

        /**
         * Resolves a media URL with caching. If the URL is in cache and not expired,
         * returns the cached result. Otherwise, performs resolution and caches the result.
         *
         * @param url The original URL to resolve
         * @return Result containing the resolved URL or error
         */
        suspend fun resolveUrl(url: String): Result<String> =
            withContext(dispatcherProvider.io) {
                try {
                    android.util.Log.d(TAG, "Resolving URL: $url")

                    // Check cache first
                    val cachedResult = getCachedUrl(url)
                    if (cachedResult != null) {
                        android.util.Log.d(TAG, "Cache hit for URL: $url -> ${cachedResult.resolvedUrl}")
                        return@withContext Result.Success(cachedResult.resolvedUrl)
                    }

                    android.util.Log.d(TAG, "Cache miss for URL: $url, performing resolution...")

                    // Get or create a mutex for this specific URL to prevent duplicate resolutions
                    val urlMutex = resolutionMutex.computeIfAbsent(url) { Mutex() }

                    val result =
                        urlMutex.withLock {
                            // Double-check cache after acquiring lock (another thread might have resolved it)
                            val doubleCheckCached = getCachedUrl(url)
                            if (doubleCheckCached != null) {
                                android.util.Log.d(TAG, "Double-check cache hit for URL: $url")
                                return@withLock Result.Success(doubleCheckCached.resolvedUrl)
                            }

                            // Perform actual URL resolution
                            performUrlResolution(url)
                        }

                    // Clean up the mutex to prevent memory leaks
                    resolutionMutex.remove(url)

                    // Cache successful results
                    if (result is Result.Success) {
                        cacheUrl(url, result.data)
                        android.util.Log.d(TAG, "Cached resolved URL: $url -> ${result.data}")
                    }

                    result
                } catch (e: Exception) {
                    android.util.Log.e(TAG, "Error resolving URL: $url", e)
                    Result.Error(e)
                }
            }

        /**
         * Performs the actual URL resolution logic based on URL patterns
         */
        private suspend fun performUrlResolution(url: String): Result<String> {
            return when {
                // Handle Torrentio resolve URLs
                url.contains("torrentio.strem.fun/resolve") -> {
                    android.util.Log.d(TAG, "Detected Torrentio resolve URL, attempting unrestriction...")
                    unrestrictWithCircuitBreaker(url)
                }

                // Handle direct Real-Debrid URLs
                url.contains("real-debrid.com") -> {
                    android.util.Log.d(TAG, "Detected Real-Debrid URL, unrestricting...")
                    unrestrictWithCircuitBreaker(url)
                }

                // Return other URLs unchanged
                else -> {
                    android.util.Log.d(TAG, "URL doesn't need resolution, using directly")
                    Result.Success(url)
                }
            }
        }

        /**
         * Unrestrict URL using Real-Debrid API with circuit breaker protection
         */
        private suspend fun unrestrictWithCircuitBreaker(url: String): Result<String> {
            return when (
                val circuitResult =
                    realDebridCircuitBreaker.execute {
                        performRealDebridUnrestriction(url)
                    }
            ) {
                is CircuitBreaker.Result.Success -> {
                    android.util.Log.d(TAG, "Successfully unrestricted URL via circuit breaker: ${circuitResult.value}")
                    circuitResult.value
                }
                is CircuitBreaker.Result.Failure -> {
                    android.util.Log.w(TAG, "Failed to unrestrict URL: ${circuitResult.exception.message}")
                    android.util.Log.d(TAG, "Falling back to original URL for ExoPlayer handling")
                    Result.Success(url) // Return original URL as fallback
                }
                is CircuitBreaker.Result.CircuitOpen -> {
                    android.util.Log.w(TAG, "Real-Debrid circuit breaker is OPEN - using original URL")
                    Result.Success(url) // Return original URL when circuit is open
                }
                is CircuitBreaker.Result.FallbackUsed -> {
                    android.util.Log.d(TAG, "Using fallback from circuit breaker")
                    Result.Success(url) // Fallback should return original URL
                }
            }
        }

        /**
         * Perform Real-Debrid unrestriction (called by circuit breaker)
         */
        private suspend fun performRealDebridUnrestriction(url: String): Result<String> {
            return when (val result = realDebridRepository.unrestrictLink(url)) {
                is Result.Success -> {
                    android.util.Log.d(TAG, "Real-Debrid unrestriction successful: ${result.data}")
                    Result.Success(result.data)
                }
                is Result.Error -> {
                    android.util.Log.e(TAG, "Real-Debrid unrestriction failed: ${result.exception.message}")
                    throw result.exception // Circuit breaker will handle this
                }
                is Result.Loading -> {
                    android.util.Log.d(TAG, "Real-Debrid unrestriction in progress")
                    throw Exception("Real-Debrid unrestriction is still loading") // Should not happen in practice
                }
            }
        }

        /**
         * Retrieves a cached URL if it exists and is not expired
         */
        private suspend fun getCachedUrl(url: String): CachedUrl? =
            cacheMutex.withLock {
                val cached = urlCache[url]
                if (cached != null && !cached.isExpired(cacheExpirationMs)) {
                    // Move to end (most recently used)
                    urlCache.remove(url)
                    urlCache[url] = cached
                    cached
                } else {
                    // Remove expired entry
                    if (cached != null) {
                        urlCache.remove(url)
                        android.util.Log.d(TAG, "Removed expired cache entry for URL: $url")
                    }
                    null
                }
            }

        /**
         * Caches a resolved URL
         */
        private suspend fun cacheUrl(
            originalUrl: String,
            resolvedUrl: String,
        ) = cacheMutex.withLock {
            val cachedUrl =
                CachedUrl(
                    resolvedUrl = resolvedUrl,
                    timestamp = System.currentTimeMillis(),
                    originalUrl = originalUrl,
                )
            urlCache[originalUrl] = cachedUrl
            android.util.Log.d(TAG, "Cached URL resolution: $originalUrl -> $resolvedUrl")
        }

        /**
         * Invalidates a specific URL from the cache
         *
         * @param url The URL to invalidate
         */
        suspend fun invalidateUrl(url: String) =
            cacheMutex.withLock {
                val removed = urlCache.remove(url)
                if (removed != null) {
                    android.util.Log.d(TAG, "Invalidated cache entry for URL: $url")
                }
            }

        /**
         * Invalidates all URLs matching a pattern from the cache
         *
         * @param pattern The pattern to match against cached URLs
         */
        suspend fun invalidateUrlsMatching(pattern: String) =
            cacheMutex.withLock {
                val toRemove = urlCache.keys.filter { it.contains(pattern) }
                toRemove.forEach { url ->
                    urlCache.remove(url)
                    android.util.Log.d(TAG, "Invalidated cache entry for URL matching pattern '$pattern': $url")
                }
                android.util.Log.d(TAG, "Invalidated ${toRemove.size} cache entries matching pattern: $pattern")
            }

        /**
         * Clears all cached URLs
         */
        suspend fun clearCache() =
            cacheMutex.withLock {
                val size = urlCache.size
                urlCache.clear()
                android.util.Log.d(TAG, "Cleared all $size cache entries")
            }

        /**
         * Gets current cache statistics for monitoring and debugging
         */
        suspend fun getCacheStats(): CacheStats =
            cacheMutex.withLock {
                val now = System.currentTimeMillis()
                val expiredCount = urlCache.values.count { it.isExpired(cacheExpirationMs) }
                val validCount = urlCache.size - expiredCount

                CacheStats(
                    totalEntries = urlCache.size,
                    validEntries = validCount,
                    expiredEntries = expiredCount,
                    cacheSize = cacheSize,
                    expirationMinutes = DEFAULT_CACHE_EXPIRATION_MINUTES,
                )
            }

        /**
         * Data class representing cache statistics
         */
        data class CacheStats(
            val totalEntries: Int,
            val validEntries: Int,
            val expiredEntries: Int,
            val cacheSize: Int,
            val expirationMinutes: Long,
        )

        /**
         * Performs cache maintenance by removing expired entries
         * This should be called periodically to prevent memory leaks
         */
        suspend fun performCacheMaintenance() =
            cacheMutex.withLock {
                val now = System.currentTimeMillis()
                val iterator = urlCache.entries.iterator()
                var removedCount = 0

                while (iterator.hasNext()) {
                    val entry = iterator.next()
                    if (entry.value.isExpired(cacheExpirationMs)) {
                        iterator.remove()
                        removedCount++
                    }
                }

                if (removedCount > 0) {
                    android.util.Log.d(TAG, "Cache maintenance: removed $removedCount expired entries")
                }
            }

        /**
         * For testing purposes - allows inspection of cache state
         */
        @VisibleForTesting
        internal suspend fun getCacheSize(): Int =
            cacheMutex.withLock {
                urlCache.size
            }

        /**
         * For testing purposes - checks if a URL is cached
         */
        @VisibleForTesting
        internal suspend fun isCached(url: String): Boolean =
            cacheMutex.withLock {
                val cached = urlCache[url]
                cached != null && !cached.isExpired(cacheExpirationMs)
            }

        /**
         * Get Real-Debrid circuit breaker metrics for monitoring
         */
        fun getRealDebridCircuitBreakerMetrics(): CircuitBreaker.Metrics {
            return realDebridCircuitBreaker.getMetrics()
        }

        /**
         * Reset Real-Debrid circuit breaker
         */
        suspend fun resetRealDebridCircuitBreaker() {
            realDebridCircuitBreaker.reset()
        }

        /**
         * Force Real-Debrid circuit breaker to open (for testing or manual intervention)
         */
        suspend fun forceRealDebridCircuitBreakerOpen() {
            realDebridCircuitBreaker.forceOpen()
        }

        /**
         * Force Real-Debrid circuit breaker to close (for testing or manual intervention)
         */
        suspend fun forceRealDebridCircuitBreakerClose() {
            realDebridCircuitBreaker.forceClose()
        }
    }
