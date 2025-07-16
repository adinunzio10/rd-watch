package com.rdwatch.androidtv.ui.details.models.advanced

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.json.*
import java.io.File
import java.util.Date
import java.util.concurrent.ConcurrentHashMap

/**
 * Advanced health caching and persistence manager
 * Provides multi-level caching, persistence, and performance optimization for health data
 */
class HealthCacheManager(private val context: Context) {
    
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }
    
    // Multi-level caching
    private val memoryCache = ConcurrentHashMap<String, CachedHealthEntry>()
    private val diskCache = DiskCache(context)
    private val preferences: SharedPreferences = context.getSharedPreferences(
        "health_cache_prefs", Context.MODE_PRIVATE
    )
    
    // Cache configuration
    private val memoryCacheMaxSize = 500
    private val diskCacheMaxSizeMB = 50
    private val memoryCacheExpirationMs = 5 * 60 * 1000L // 5 minutes
    private val diskCacheExpirationMs = 60 * 60 * 1000L // 1 hour
    private val persistentCacheExpirationMs = 24 * 60 * 60 * 1000L // 24 hours
    
    // Performance monitoring
    private val cacheStats = CacheStatistics()
    
    // Cache events
    private val _cacheEvents = MutableSharedFlow<CacheEvent>()
    val cacheEvents: SharedFlow<CacheEvent> = _cacheEvents.asSharedFlow()
    
    init {
        startCacheManagement()
    }
    
    /**
     * Store health data with automatic tier management
     */
    suspend fun storeHealthData(sourceId: String, healthData: HealthData) {
        val entry = CachedHealthEntry(
            sourceId = sourceId,
            healthData = healthData,
            createdAt = Date(),
            lastAccessed = Date(),
            accessCount = 1,
            tier = CacheTier.MEMORY
        )
        
        // Store in memory cache
        storeInMemoryCache(sourceId, entry)
        
        // Async disk cache storage
        scope.launch {
            storeToDiskCache(sourceId, entry)
        }
        
        // Async persistent storage for important data
        if (shouldPersist(healthData)) {
            scope.launch {
                storeToPersistentCache(sourceId, entry)
            }
        }
        
        cacheStats.recordStore()
        _cacheEvents.emit(CacheEvent.Stored(sourceId, CacheTier.MEMORY))
    }
    
    /**
     * Retrieve health data with intelligent fallback
     */
    suspend fun getHealthData(sourceId: String): HealthData? {
        val startTime = System.currentTimeMillis()
        
        // Try memory cache first
        memoryCache[sourceId]?.let { entry ->
            if (!isExpired(entry, memoryCacheExpirationMs)) {
                entry.lastAccessed = Date()
                entry.accessCount++
                cacheStats.recordHit(CacheTier.MEMORY, System.currentTimeMillis() - startTime)
                _cacheEvents.emit(CacheEvent.Hit(sourceId, CacheTier.MEMORY))
                return entry.healthData
            } else {
                memoryCache.remove(sourceId)
            }
        }
        
        // Try disk cache
        val diskEntry = loadFromDiskCache(sourceId)
        if (diskEntry != null && !isExpired(diskEntry, diskCacheExpirationMs)) {
            // Promote to memory cache
            diskEntry.lastAccessed = Date()
            diskEntry.accessCount++
            diskEntry.tier = CacheTier.MEMORY
            storeInMemoryCache(sourceId, diskEntry)
            
            cacheStats.recordHit(CacheTier.DISK, System.currentTimeMillis() - startTime)
            _cacheEvents.emit(CacheEvent.Hit(sourceId, CacheTier.DISK))
            return diskEntry.healthData
        }
        
        // Try persistent cache
        val persistentEntry = loadFromPersistentCache(sourceId)
        if (persistentEntry != null && !isExpired(persistentEntry, persistentCacheExpirationMs)) {
            // Promote through cache tiers
            persistentEntry.lastAccessed = Date()
            persistentEntry.accessCount++
            persistentEntry.tier = CacheTier.MEMORY
            
            storeInMemoryCache(sourceId, persistentEntry)
            scope.launch { storeToDiskCache(sourceId, persistentEntry) }
            
            cacheStats.recordHit(CacheTier.PERSISTENT, System.currentTimeMillis() - startTime)
            _cacheEvents.emit(CacheEvent.Hit(sourceId, CacheTier.PERSISTENT))
            return persistentEntry.healthData
        }
        
        // Cache miss
        cacheStats.recordMiss(System.currentTimeMillis() - startTime)
        _cacheEvents.emit(CacheEvent.Miss(sourceId))
        return null
    }
    
    /**
     * Preload health data for multiple sources
     */
    suspend fun preloadHealthData(sourceIds: List<String>): Map<String, HealthData> {
        val results = mutableMapOf<String, HealthData>()
        
        // Batch load from memory cache
        val memoryHits = sourceIds.mapNotNull { sourceId ->
            memoryCache[sourceId]?.let { entry ->
                if (!isExpired(entry, memoryCacheExpirationMs)) {
                    entry.lastAccessed = Date()
                    entry.accessCount++
                    sourceId to entry.healthData
                } else {
                    memoryCache.remove(sourceId)
                    null
                }
            }
        }.toMap()
        
        results.putAll(memoryHits)
        val remainingIds = sourceIds - memoryHits.keys
        
        if (remainingIds.isNotEmpty()) {
            // Batch load from disk cache
            val diskResults = loadBatchFromDiskCache(remainingIds)
            results.putAll(diskResults)
            
            // Promote disk hits to memory
            diskResults.forEach { (sourceId, healthData) ->
                val entry = CachedHealthEntry(
                    sourceId = sourceId,
                    healthData = healthData,
                    createdAt = Date(),
                    lastAccessed = Date(),
                    accessCount = 1,
                    tier = CacheTier.MEMORY
                )
                storeInMemoryCache(sourceId, entry)
            }
        }
        
        cacheStats.recordBatchLoad(sourceIds.size, results.size)
        return results
    }
    
    /**
     * Invalidate cached health data
     */
    suspend fun invalidateHealthData(sourceId: String) {
        memoryCache.remove(sourceId)
        scope.launch {
            diskCache.remove(sourceId)
            removePersistentCacheEntry(sourceId)
        }
        
        _cacheEvents.emit(CacheEvent.Invalidated(sourceId))
    }
    
    /**
     * Clear all cached data
     */
    suspend fun clearAllCache() {
        memoryCache.clear()
        scope.launch {
            diskCache.clear()
            clearPersistentCache()
        }
        
        cacheStats.reset()
        _cacheEvents.emit(CacheEvent.Cleared)
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStatistics(): CacheStatistics = cacheStats.copy()
    
    /**
     * Optimize cache performance
     */
    suspend fun optimizeCache() {
        val startTime = System.currentTimeMillis()
        
        // Remove expired entries from memory
        val expiredMemoryKeys = memoryCache.entries.filter { (_, entry) ->
            isExpired(entry, memoryCacheExpirationMs)
        }.map { it.key }
        
        expiredMemoryKeys.forEach { memoryCache.remove(it) }
        
        // Trim memory cache if too large
        if (memoryCache.size > memoryCacheMaxSize) {
            val toRemove = memoryCache.size - memoryCacheMaxSize
            val lruEntries = memoryCache.entries
                .sortedBy { it.value.lastAccessed.time }
                .take(toRemove)
                .map { it.key }
            
            lruEntries.forEach { memoryCache.remove(it) }
        }
        
        // Async disk cache optimization
        scope.launch {
            diskCache.optimize()
        }
        
        val duration = System.currentTimeMillis() - startTime
        cacheStats.recordOptimization(duration)
        _cacheEvents.emit(CacheEvent.Optimized(duration))
    }
    
    // MARK: - Private Implementation
    
    private fun storeInMemoryCache(sourceId: String, entry: CachedHealthEntry) {
        // Check cache size limit
        if (memoryCache.size >= memoryCacheMaxSize) {
            removeLRUFromMemory()
        }
        
        memoryCache[sourceId] = entry
    }
    
    private fun removeLRUFromMemory() {
        val lruEntry = memoryCache.entries.minByOrNull { it.value.lastAccessed.time }
        lruEntry?.key?.let { memoryCache.remove(it) }
    }
    
    private suspend fun storeToDiskCache(sourceId: String, entry: CachedHealthEntry) {
        try {
            val serialized = json.encodeToString(entry)
            diskCache.store(sourceId, serialized)
        } catch (e: Exception) {
            // Log error but don't fail the operation
        }
    }
    
    private suspend fun loadFromDiskCache(sourceId: String): CachedHealthEntry? {
        return try {
            val serialized = diskCache.load(sourceId) ?: return null
            json.decodeFromString<CachedHealthEntry>(serialized)
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun loadBatchFromDiskCache(sourceIds: List<String>): Map<String, HealthData> {
        return sourceIds.mapNotNull { sourceId ->
            loadFromDiskCache(sourceId)?.let { entry ->
                if (!isExpired(entry, diskCacheExpirationMs)) {
                    sourceId to entry.healthData
                } else {
                    null
                }
            }
        }.toMap()
    }
    
    private suspend fun storeToPersistentCache(sourceId: String, entry: CachedHealthEntry) {
        try {
            val serialized = json.encodeToString(entry)
            preferences.edit()
                .putString("persistent_$sourceId", serialized)
                .putLong("persistent_${sourceId}_timestamp", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            // Log error but don't fail
        }
    }
    
    private suspend fun loadFromPersistentCache(sourceId: String): CachedHealthEntry? {
        return try {
            val serialized = preferences.getString("persistent_$sourceId", null) ?: return null
            json.decodeFromString<CachedHealthEntry>(serialized)
        } catch (e: Exception) {
            null
        }
    }
    
    private suspend fun removePersistentCacheEntry(sourceId: String) {
        preferences.edit()
            .remove("persistent_$sourceId")
            .remove("persistent_${sourceId}_timestamp")
            .apply()
    }
    
    private suspend fun clearPersistentCache() {
        val keys = preferences.all.keys.filter { it.startsWith("persistent_") }
        val editor = preferences.edit()
        keys.forEach { editor.remove(it) }
        editor.apply()
    }
    
    private fun isExpired(entry: CachedHealthEntry, expirationMs: Long): Boolean {
        val age = System.currentTimeMillis() - entry.createdAt.time
        return age > expirationMs
    }
    
    private fun shouldPersist(healthData: HealthData): Boolean {
        // Persist high-quality sources or frequently accessed ones
        return healthData.overallScore >= 80 || 
               healthData.p2pHealth.seeders > 100 ||
               healthData.providerReliability >= 90
    }
    
    private fun startCacheManagement() {
        // Periodic cache optimization
        scope.launch {
            while (true) {
                delay(15 * 60 * 1000L) // Every 15 minutes
                try {
                    optimizeCache()
                } catch (e: Exception) {
                    // Log error but continue
                }
            }
        }
        
        // Periodic statistics reporting
        scope.launch {
            while (true) {
                delay(60 * 60 * 1000L) // Every hour
                try {
                    _cacheEvents.emit(CacheEvent.StatisticsReport(getCacheStatistics()))
                } catch (e: Exception) {
                    // Log error but continue
                }
            }
        }
    }
    
    fun cleanup() {
        scope.cancel()
    }
}

/**
 * Disk cache implementation
 */
private class DiskCache(context: Context) {
    private val cacheDir = File(context.cacheDir, "health_cache")
    
    init {
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }
    
    suspend fun store(key: String, data: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, key.hashCode().toString())
            file.writeText(data)
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    suspend fun load(key: String): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, key.hashCode().toString())
            if (file.exists()) file.readText() else null
        } catch (e: Exception) {
            null
        }
    }
    
    suspend fun remove(key: String) = withContext(Dispatchers.IO) {
        try {
            val file = File(cacheDir, key.hashCode().toString())
            file.delete()
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    suspend fun clear() = withContext(Dispatchers.IO) {
        try {
            cacheDir.listFiles()?.forEach { it.delete() }
        } catch (e: Exception) {
            // Handle error
        }
    }
    
    suspend fun optimize() = withContext(Dispatchers.IO) {
        try {
            // Remove old files
            val cutoffTime = System.currentTimeMillis() - 24 * 60 * 60 * 1000L // 24 hours
            cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < cutoffTime) {
                    file.delete()
                }
            }
        } catch (e: Exception) {
            // Handle error
        }
    }
}

/**
 * Cached health entry with metadata
 */
@Serializable
data class CachedHealthEntry(
    val sourceId: String,
    val healthData: HealthData,
    @Serializable(with = DateSerializer::class)
    val createdAt: Date,
    @Serializable(with = DateSerializer::class)
    var lastAccessed: Date,
    var accessCount: Int,
    var tier: CacheTier
)

/**
 * Cache tiers for data organization
 */
@Serializable
enum class CacheTier {
    MEMORY,
    DISK,
    PERSISTENT
}

/**
 * Cache events for monitoring
 */
sealed class CacheEvent {
    data class Stored(val sourceId: String, val tier: CacheTier) : CacheEvent()
    data class Hit(val sourceId: String, val tier: CacheTier) : CacheEvent()
    data class Miss(val sourceId: String) : CacheEvent()
    data class Invalidated(val sourceId: String) : CacheEvent()
    data class Optimized(val durationMs: Long) : CacheEvent()
    data class StatisticsReport(val statistics: CacheStatistics) : CacheEvent()
    object Cleared : CacheEvent()
}

/**
 * Cache statistics for performance monitoring
 */
@Serializable
data class CacheStatistics(
    var memoryHits: Int = 0,
    var diskHits: Int = 0,
    var persistentHits: Int = 0,
    var misses: Int = 0,
    var stores: Int = 0,
    var invalidations: Int = 0,
    var optimizations: Int = 0,
    var batchLoads: Int = 0,
    var batchHits: Int = 0,
    @Serializable(with = DateSerializer::class)
    var lastReset: Date = Date(),
    var averageRetrievalTimeMs: Long = 0L,
    var totalRetrievalTimeMs: Long = 0L,
    var retrievalCount: Int = 0
) {
    fun recordHit(tier: CacheTier, durationMs: Long) {
        when (tier) {
            CacheTier.MEMORY -> memoryHits++
            CacheTier.DISK -> diskHits++
            CacheTier.PERSISTENT -> persistentHits++
        }
        recordRetrievalTime(durationMs)
    }
    
    fun recordMiss(durationMs: Long) {
        misses++
        recordRetrievalTime(durationMs)
    }
    
    fun recordStore() {
        stores++
    }
    
    fun recordBatchLoad(requested: Int, hits: Int) {
        batchLoads++
        batchHits += hits
    }
    
    fun recordOptimization(durationMs: Long) {
        optimizations++
    }
    
    private fun recordRetrievalTime(durationMs: Long) {
        totalRetrievalTimeMs += durationMs
        retrievalCount++
        averageRetrievalTimeMs = if (retrievalCount > 0) totalRetrievalTimeMs / retrievalCount else 0L
    }
    
    fun getHitRate(): Float {
        val totalRequests = memoryHits + diskHits + persistentHits + misses
        return if (totalRequests > 0) {
            (memoryHits + diskHits + persistentHits).toFloat() / totalRequests
        } else 0f
    }
    
    fun getMemoryHitRate(): Float {
        val totalRequests = memoryHits + diskHits + persistentHits + misses
        return if (totalRequests > 0) memoryHits.toFloat() / totalRequests else 0f
    }
    
    fun reset() {
        memoryHits = 0
        diskHits = 0
        persistentHits = 0
        misses = 0
        stores = 0
        invalidations = 0
        optimizations = 0
        batchLoads = 0
        batchHits = 0
        lastReset = Date()
        averageRetrievalTimeMs = 0L
        totalRetrievalTimeMs = 0L
        retrievalCount = 0
    }
    
    fun copy(): CacheStatistics {
        return CacheStatistics(
            memoryHits, diskHits, persistentHits, misses, stores,
            invalidations, optimizations, batchLoads, batchHits,
            lastReset, averageRetrievalTimeMs, totalRetrievalTimeMs, retrievalCount
        )
    }
}

/**
 * Date serializer for kotlinx.serialization
 */
object DateSerializer : KSerializer<Date> {
    override val descriptor = PrimitiveSerialDescriptor("Date", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Date) = encoder.encodeLong(value.time)
    override fun deserialize(decoder: Decoder): Date = Date(decoder.decodeLong())
}