package com.rdwatch.androidtv.scraper.cache

import com.rdwatch.androidtv.scraper.models.ManifestCacheException
import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import kotlinx.coroutines.flow.Flow
import java.util.Date

/**
 * Manifest caching interface with TTL support and cache management
 */
interface ManifestCache {
    
    // Basic cache operations
    suspend fun get(key: String): ManifestResult<ScraperManifest?>
    suspend fun put(key: String, manifest: ScraperManifest, ttlMinutes: Long? = null): ManifestResult<Unit>
    suspend fun remove(key: String): ManifestResult<Unit>
    suspend fun clear(): ManifestResult<Unit>
    
    // Cache metadata
    suspend fun contains(key: String): Boolean
    suspend fun getSize(): Int
    suspend fun getKeys(): Set<String>
    suspend fun getExpiration(key: String): Date?
    suspend fun isExpired(key: String): Boolean
    
    // Bulk operations
    suspend fun putAll(manifests: Map<String, ScraperManifest>, ttlMinutes: Long? = null): ManifestResult<Unit>
    suspend fun getAll(keys: Set<String>): Map<String, ScraperManifest>
    suspend fun removeAll(keys: Set<String>): ManifestResult<Unit>
    
    // Cache warming and preloading
    suspend fun warmCache(manifests: List<ScraperManifest>): ManifestResult<Int>
    suspend fun preloadFromRepository(): ManifestResult<Int>
    
    // Cache maintenance
    suspend fun evictExpired(): ManifestResult<Int>
    suspend fun updateTtl(key: String, ttlMinutes: Long): ManifestResult<Unit>
    suspend fun touch(key: String): ManifestResult<Unit> // Reset expiration
    
    // Statistics and monitoring
    suspend fun getStatistics(): CacheStatistics
    suspend fun resetStatistics(): ManifestResult<Unit>
    
    // Reactive operations
    fun observeCache(): Flow<CacheEvent>
    fun observeKey(key: String): Flow<CacheKeyEvent>
    
    // Memory management
    suspend fun trimToSize(maxSize: Int): ManifestResult<Int>
    suspend fun getMemoryUsage(): MemoryUsage
}

/**
 * Cache statistics for monitoring and optimization
 */
data class CacheStatistics(
    val hitCount: Long,
    val missCount: Long,
    val putCount: Long,
    val evictionCount: Long,
    val size: Int,
    val maxSize: Int,
    val hitRate: Double = if (hitCount + missCount > 0) hitCount.toDouble() / (hitCount + missCount) else 0.0,
    val memoryUsage: MemoryUsage,
    val averageLoadTime: Long,
    val lastClearTime: Date?,
    val oldestEntry: Date?,
    val newestEntry: Date?
)

/**
 * Memory usage information
 */
data class MemoryUsage(
    val usedBytes: Long,
    val maxBytes: Long,
    val entryCount: Int,
    val averageEntrySize: Long = if (entryCount > 0) usedBytes / entryCount else 0L
)

/**
 * Cache events for reactive monitoring
 */
sealed class CacheEvent {
    data class EntryAdded(val key: String, val manifest: ScraperManifest) : CacheEvent()
    data class EntryUpdated(val key: String, val manifest: ScraperManifest) : CacheEvent()
    data class EntryRemoved(val key: String) : CacheEvent()
    data class EntryExpired(val key: String) : CacheEvent()
    data class CacheCleared(val previousSize: Int) : CacheEvent()
    data class CacheWarmed(val loadedCount: Int) : CacheEvent()
    data class EvictionOccurred(val evictedCount: Int, val reason: EvictionReason) : CacheEvent()
}

/**
 * Key-specific cache events
 */
sealed class CacheKeyEvent {
    data class KeyAdded(val manifest: ScraperManifest) : CacheKeyEvent()
    data class KeyUpdated(val manifest: ScraperManifest) : CacheKeyEvent()
    object KeyRemoved : CacheKeyEvent()
    object KeyExpired : CacheKeyEvent()
}

/**
 * Reasons for cache eviction
 */
enum class EvictionReason {
    EXPIRED,
    SIZE_LIMIT,
    MANUAL,
    MEMORY_PRESSURE
}

/**
 * Cache entry with metadata
 */
internal data class CacheEntry(
    val key: String,
    val manifest: ScraperManifest,
    val createdAt: Date,
    val lastAccessedAt: Date,
    val expiresAt: Date?,
    val accessCount: Long = 1,
    val size: Long
) {
    val isExpired: Boolean
        get() = expiresAt?.let { Date().after(it) } ?: false
    
    val age: Long
        get() = Date().time - createdAt.time
    
    val timeSinceLastAccess: Long
        get() = Date().time - lastAccessedAt.time
    
    fun touch(): CacheEntry = copy(
        lastAccessedAt = Date(),
        accessCount = accessCount + 1
    )
    
    fun updateExpiration(ttlMinutes: Long): CacheEntry = copy(
        expiresAt = Date(Date().time + ttlMinutes * 60 * 1000)
    )
}