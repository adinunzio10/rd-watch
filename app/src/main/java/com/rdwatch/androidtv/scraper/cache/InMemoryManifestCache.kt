package com.rdwatch.androidtv.scraper.cache

import com.rdwatch.androidtv.scraper.models.ManifestCacheException
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import com.rdwatch.androidtv.scraper.repository.ManifestRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory manifest cache implementation with LRU eviction and TTL support
 */
@Singleton
class InMemoryManifestCache
    @Inject
    constructor(
        private val repository: ManifestRepository,
    ) : ManifestCache {
        private val cache = ConcurrentHashMap<String, CacheEntry>()
        private val mutex = Mutex()

        // Configuration
        private var maxSize: Int = 100
        private var defaultTtlMinutes: Long = 60
        private var maxMemoryBytes: Long = 50 * 1024 * 1024 // 50 MB

        // Statistics
        private var hitCount: Long = 0
        private var missCount: Long = 0
        private var putCount: Long = 0
        private var evictionCount: Long = 0
        private var lastClearTime: Date? = null

        // Event streams
        private val cacheEventFlow = MutableSharedFlow<CacheEvent>()
        private val keyEventFlows = ConcurrentHashMap<String, MutableSharedFlow<CacheKeyEvent>>()

        override suspend fun get(key: String): ManifestResult<ScraperManifest?> {
            return try {
                mutex.withLock {
                    val entry = cache[key]

                    if (entry == null) {
                        missCount++
                        return@withLock ManifestResult.Success(null)
                    }

                    if (entry.isExpired) {
                        cache.remove(key)
                        evictionCount++
                        emitCacheEvent(CacheEvent.EntryExpired(key))
                        emitKeyEvent(key, CacheKeyEvent.KeyExpired)
                        missCount++
                        return@withLock ManifestResult.Success(null)
                    }

                    // Update access time and count
                    val touchedEntry = entry.touch()
                    cache[key] = touchedEntry
                    hitCount++

                    ManifestResult.Success(touchedEntry.manifest)
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Failed to get from cache: ${e.message}",
                        cause = e,
                        cacheKey = key,
                    ),
                )
            }
        }

        override suspend fun put(
            key: String,
            manifest: ScraperManifest,
            ttlMinutes: Long?,
        ): ManifestResult<Unit> {
            return try {
                mutex.withLock {
                    val now = Date()
                    val ttl = ttlMinutes ?: defaultTtlMinutes
                    val expiresAt = if (ttl > 0) Date(now.time + ttl * 60 * 1000) else null

                    val manifestSize = estimateSize(manifest)
                    val entry =
                        CacheEntry(
                            key = key,
                            manifest = manifest,
                            createdAt = now,
                            lastAccessedAt = now,
                            expiresAt = expiresAt,
                            size = manifestSize,
                        )

                    val wasUpdate = cache.containsKey(key)

                    // Check memory limits before adding
                    if (!wasUpdate && shouldEvictForMemory(manifestSize)) {
                        evictLeastRecentlyUsed(1)
                    }

                    // Check size limits
                    if (!wasUpdate && cache.size >= maxSize) {
                        evictLeastRecentlyUsed(1)
                    }

                    cache[key] = entry
                    putCount++

                    if (wasUpdate) {
                        emitCacheEvent(CacheEvent.EntryUpdated(key, manifest))
                        emitKeyEvent(key, CacheKeyEvent.KeyUpdated(manifest))
                    } else {
                        emitCacheEvent(CacheEvent.EntryAdded(key, manifest))
                        emitKeyEvent(key, CacheKeyEvent.KeyAdded(manifest))
                    }

                    ManifestResult.Success(Unit)
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Failed to put to cache: ${e.message}",
                        cause = e,
                        cacheKey = key,
                    ),
                )
            }
        }

        override suspend fun remove(key: String): ManifestResult<Unit> {
            return try {
                mutex.withLock {
                    val removed = cache.remove(key)
                    if (removed != null) {
                        emitCacheEvent(CacheEvent.EntryRemoved(key))
                        emitKeyEvent(key, CacheKeyEvent.KeyRemoved)
                    }
                    ManifestResult.Success(Unit)
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Failed to remove from cache: ${e.message}",
                        cause = e,
                        cacheKey = key,
                    ),
                )
            }
        }

        override suspend fun clear(): ManifestResult<Unit> {
            return try {
                mutex.withLock {
                    val previousSize = cache.size
                    cache.clear()
                    lastClearTime = Date()

                    // Clear all key event flows
                    keyEventFlows.clear()

                    emitCacheEvent(CacheEvent.CacheCleared(previousSize))
                    ManifestResult.Success(Unit)
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Failed to clear cache: ${e.message}",
                        cause = e,
                    ),
                )
            }
        }

        override suspend fun contains(key: String): Boolean {
            return mutex.withLock {
                val entry = cache[key]
                entry != null && !entry.isExpired
            }
        }

        override suspend fun getSize(): Int {
            return cache.size
        }

        override suspend fun getKeys(): Set<String> {
            return cache.keys.toSet()
        }

        override suspend fun getExpiration(key: String): Date? {
            return cache[key]?.expiresAt
        }

        override suspend fun isExpired(key: String): Boolean {
            return cache[key]?.isExpired ?: false
        }

        override suspend fun putAll(
            manifests: Map<String, ScraperManifest>,
            ttlMinutes: Long?,
        ): ManifestResult<Unit> {
            return try {
                var successCount = 0
                val errors = mutableListOf<Exception>()

                manifests.forEach { (key, manifest) ->
                    when (val result = put(key, manifest, ttlMinutes)) {
                        is ManifestResult.Success -> successCount++
                        is ManifestResult.Error -> errors.add(result.exception)
                    }
                }

                if (errors.isEmpty()) {
                    ManifestResult.Success(Unit)
                } else {
                    ManifestResult.Error(
                        ManifestCacheException(
                            "Partial failure in putAll: ${errors.size} errors out of ${manifests.size} items",
                            cause = errors.firstOrNull(),
                        ),
                    )
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Failed putAll operation: ${e.message}",
                        cause = e,
                    ),
                )
            }
        }

        override suspend fun getAll(keys: Set<String>): Map<String, ScraperManifest> {
            val results = mutableMapOf<String, ScraperManifest>()

            keys.forEach { key ->
                when (val result = get(key)) {
                    is ManifestResult.Success -> result.data?.let { results[key] = it }
                    is ManifestResult.Error -> { /* Skip errors in bulk operation */ }
                }
            }

            return results
        }

        override suspend fun removeAll(keys: Set<String>): ManifestResult<Unit> {
            return try {
                keys.forEach { key ->
                    remove(key) // Ignore individual errors
                }
                ManifestResult.Success(Unit)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Failed removeAll operation: ${e.message}",
                        cause = e,
                    ),
                )
            }
        }

        override suspend fun warmCache(manifests: List<ScraperManifest>): ManifestResult<Int> {
            return try {
                var loadedCount = 0

                manifests.forEach { manifest ->
                    val key = generateCacheKey(manifest)
                    when (put(key, manifest)) {
                        is ManifestResult.Success -> loadedCount++
                        is ManifestResult.Error -> { /* Continue with other manifests */ }
                    }
                }

                emitCacheEvent(CacheEvent.CacheWarmed(loadedCount))
                ManifestResult.Success(loadedCount)
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Cache warming failed: ${e.message}",
                        cause = e,
                    ),
                )
            }
        }

        override suspend fun preloadFromRepository(): ManifestResult<Int> {
            return try {
                when (val manifestsResult = repository.getEnabledManifests()) {
                    is ManifestResult.Success -> warmCache(manifestsResult.data)
                    is ManifestResult.Error ->
                        ManifestResult.Error(
                            ManifestCacheException(
                                "Failed to preload from repository: ${manifestsResult.exception.message}",
                                cause = manifestsResult.exception,
                            ),
                        )
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Preload operation failed: ${e.message}",
                        cause = e,
                    ),
                )
            }
        }

        override suspend fun evictExpired(): ManifestResult<Int> {
            return try {
                mutex.withLock {
                    val expiredKeys =
                        cache.entries
                            .filter { it.value.isExpired }
                            .map { it.key }

                    expiredKeys.forEach { key ->
                        cache.remove(key)
                        emitCacheEvent(CacheEvent.EntryExpired(key))
                        emitKeyEvent(key, CacheKeyEvent.KeyExpired)
                    }

                    evictionCount += expiredKeys.size

                    if (expiredKeys.isNotEmpty()) {
                        emitCacheEvent(CacheEvent.EvictionOccurred(expiredKeys.size, EvictionReason.EXPIRED))
                    }

                    ManifestResult.Success(expiredKeys.size)
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Failed to evict expired entries: ${e.message}",
                        cause = e,
                    ),
                )
            }
        }

        override suspend fun updateTtl(
            key: String,
            ttlMinutes: Long,
        ): ManifestResult<Unit> {
            return try {
                mutex.withLock {
                    val entry = cache[key]
                    if (entry != null) {
                        cache[key] = entry.updateExpiration(ttlMinutes)
                        ManifestResult.Success(Unit)
                    } else {
                        ManifestResult.Error(
                            ManifestCacheException(
                                "Key not found for TTL update: $key",
                                cacheKey = key,
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Failed to update TTL: ${e.message}",
                        cause = e,
                        cacheKey = key,
                    ),
                )
            }
        }

        override suspend fun touch(key: String): ManifestResult<Unit> {
            return try {
                mutex.withLock {
                    val entry = cache[key]
                    if (entry != null && !entry.isExpired) {
                        cache[key] = entry.touch()
                        ManifestResult.Success(Unit)
                    } else {
                        ManifestResult.Error(
                            ManifestCacheException(
                                "Key not found or expired for touch: $key",
                                cacheKey = key,
                            ),
                        )
                    }
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Failed to touch entry: ${e.message}",
                        cause = e,
                        cacheKey = key,
                    ),
                )
            }
        }

        override suspend fun getStatistics(): CacheStatistics {
            return mutex.withLock {
                val entries = cache.values
                val totalMemory = entries.sumOf { it.size }

                CacheStatistics(
                    hitCount = hitCount,
                    missCount = missCount,
                    putCount = putCount,
                    evictionCount = evictionCount,
                    size = cache.size,
                    maxSize = maxSize,
                    memoryUsage =
                        MemoryUsage(
                            usedBytes = totalMemory,
                            maxBytes = maxMemoryBytes,
                            entryCount = cache.size,
                        ),
                    averageLoadTime = 0L, // TODO: Implement load time tracking
                    lastClearTime = lastClearTime,
                    oldestEntry = entries.minByOrNull { it.createdAt }?.createdAt,
                    newestEntry = entries.maxByOrNull { it.createdAt }?.createdAt,
                )
            }
        }

        override suspend fun resetStatistics(): ManifestResult<Unit> {
            return try {
                mutex.withLock {
                    hitCount = 0
                    missCount = 0
                    putCount = 0
                    evictionCount = 0
                    lastClearTime = null
                    ManifestResult.Success(Unit)
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Failed to reset statistics: ${e.message}",
                        cause = e,
                    ),
                )
            }
        }

        override fun observeCache(): Flow<CacheEvent> {
            return cacheEventFlow.asSharedFlow()
        }

        override fun observeKey(key: String): Flow<CacheKeyEvent> {
            return keyEventFlows.getOrPut(key) { MutableSharedFlow() }.asSharedFlow()
        }

        override suspend fun trimToSize(maxSize: Int): ManifestResult<Int> {
            return try {
                mutex.withLock {
                    val currentSize = cache.size
                    val toEvict = currentSize - maxSize

                    if (toEvict > 0) {
                        val evicted = evictLeastRecentlyUsed(toEvict)
                        ManifestResult.Success(evicted)
                    } else {
                        ManifestResult.Success(0)
                    }
                }
            } catch (e: Exception) {
                ManifestResult.Error(
                    ManifestCacheException(
                        "Failed to trim cache: ${e.message}",
                        cause = e,
                    ),
                )
            }
        }

        override suspend fun getMemoryUsage(): MemoryUsage {
            return mutex.withLock {
                val entries = cache.values
                val totalMemory = entries.sumOf { it.size }

                MemoryUsage(
                    usedBytes = totalMemory,
                    maxBytes = maxMemoryBytes,
                    entryCount = cache.size,
                )
            }
        }

        // Configuration methods
        fun configure(
            maxSize: Int,
            defaultTtlMinutes: Long,
            maxMemoryBytes: Long,
        ) {
            this.maxSize = maxSize
            this.defaultTtlMinutes = defaultTtlMinutes
            this.maxMemoryBytes = maxMemoryBytes
        }

        // Private helper methods
        private fun evictLeastRecentlyUsed(count: Int): Int {
            val entries = cache.entries.sortedBy { it.value.lastAccessedAt }
            var evicted = 0

            for (entry in entries) {
                if (evicted >= count) break

                cache.remove(entry.key)
                emitCacheEvent(CacheEvent.EntryRemoved(entry.key))
                emitKeyEvent(entry.key, CacheKeyEvent.KeyRemoved)
                evicted++
            }

            evictionCount += evicted

            if (evicted > 0) {
                emitCacheEvent(CacheEvent.EvictionOccurred(evicted, EvictionReason.SIZE_LIMIT))
            }

            return evicted
        }

        private fun shouldEvictForMemory(newEntrySize: Long): Boolean {
            val currentMemory = cache.values.sumOf { it.size }
            return currentMemory + newEntrySize > maxMemoryBytes
        }

        private fun estimateSize(manifest: ScraperManifest): Long {
            // Rough estimation of manifest size in bytes
            var size = 0L

            size += manifest.id.length * 2 // UTF-16 characters
            size += manifest.name.length * 2
            size += manifest.displayName.length * 2
            size += manifest.version.length * 2
            size += (manifest.description?.length ?: 0) * 2
            size += (manifest.author?.length ?: 0) * 2
            size += manifest.baseUrl.length * 2
            size += manifest.sourceUrl.length * 2

            // Rough estimate for nested objects
            size += 1000 // Base overhead for configuration and metadata

            return size
        }

        private fun generateCacheKey(manifest: ScraperManifest): String {
            return "${manifest.id}:${manifest.version}"
        }

        private fun emitCacheEvent(event: CacheEvent) {
            cacheEventFlow.tryEmit(event)
        }

        private fun emitKeyEvent(
            key: String,
            event: CacheKeyEvent,
        ) {
            keyEventFlows[key]?.tryEmit(event)
        }
    }
