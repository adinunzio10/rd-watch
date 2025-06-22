package com.rdwatch.androidtv.scraper.cache

import com.rdwatch.androidtv.scraper.models.ManifestResult
import com.rdwatch.androidtv.scraper.models.ScraperManifest
import java.util.Date

/**
 * Cache strategies for different use cases
 */
enum class CacheStrategy {
    /**
     * Cache everything with default TTL
     */
    CACHE_ALL,
    
    /**
     * Cache only frequently accessed manifests
     */
    CACHE_POPULAR,
    
    /**
     * Cache based on manifest priority
     */
    CACHE_PRIORITY,
    
    /**
     * Cache with adaptive TTL based on update frequency
     */
    ADAPTIVE_TTL,
    
    /**
     * No caching
     */
    NO_CACHE
}

/**
 * Cache eviction policies
 */
enum class EvictionPolicy {
    /**
     * Least Recently Used
     */
    LRU,
    
    /**
     * Least Frequently Used
     */
    LFU,
    
    /**
     * First In, First Out
     */
    FIFO,
    
    /**
     * Random eviction
     */
    RANDOM,
    
    /**
     * Time-based eviction (TTL only)
     */
    TTL_ONLY
}

/**
 * Cache configuration
 */
data class CacheConfig(
    val strategy: CacheStrategy = CacheStrategy.CACHE_ALL,
    val evictionPolicy: EvictionPolicy = EvictionPolicy.LRU,
    val maxSize: Int = 100,
    val maxMemoryMB: Int = 50,
    val defaultTtlMinutes: Long = 60,
    val enableStatistics: Boolean = true,
    val enableEventNotifications: Boolean = true,
    val backgroundCleanupIntervalMinutes: Long = 15,
    val preloadOnStartup: Boolean = true,
    val adaptiveTtlConfig: AdaptiveTtlConfig = AdaptiveTtlConfig()
)

/**
 * Configuration for adaptive TTL strategy
 */
data class AdaptiveTtlConfig(
    val minTtlMinutes: Long = 15,
    val maxTtlMinutes: Long = 480, // 8 hours
    val baselineUpdateIntervalMinutes: Long = 60,
    val ttlMultiplier: Double = 1.5,
    val popularityThreshold: Int = 10 // Access count threshold for "popular" items
)

/**
 * Smart cache manager that applies different caching strategies
 */
class SmartCacheManager(
    private val cache: ManifestCache,
    private val config: CacheConfig
) {
    
    private val accessCounts = mutableMapOf<String, Int>()
    private val lastUpdateTimes = mutableMapOf<String, Date>()
    
    /**
     * Smart get with strategy-based caching
     */
    suspend fun get(key: String): ManifestResult<ScraperManifest?> {
        return when (config.strategy) {
            CacheStrategy.NO_CACHE -> ManifestResult.Success(null)
            else -> {
                trackAccess(key)
                cache.get(key)
            }
        }
    }
    
    /**
     * Smart put with strategy-based decisions
     */
    suspend fun put(key: String, manifest: ScraperManifest): ManifestResult<Unit> {
        return when (config.strategy) {
            CacheStrategy.NO_CACHE -> ManifestResult.Success(Unit)
            CacheStrategy.CACHE_ALL -> cache.put(key, manifest, calculateTtl(key, manifest))
            CacheStrategy.CACHE_POPULAR -> putIfPopular(key, manifest)
            CacheStrategy.CACHE_PRIORITY -> putIfHighPriority(key, manifest)
            CacheStrategy.ADAPTIVE_TTL -> cache.put(key, manifest, calculateAdaptiveTtl(key, manifest))
        }
    }
    
    /**
     * Update manifest with smart caching decisions
     */
    suspend fun update(key: String, manifest: ScraperManifest): ManifestResult<Unit> {
        lastUpdateTimes[key] = Date()
        return put(key, manifest)
    }
    
    /**
     * Check if item should be cached based on strategy
     */
    private suspend fun shouldCache(key: String, manifest: ScraperManifest): Boolean {
        return when (config.strategy) {
            CacheStrategy.NO_CACHE -> false
            CacheStrategy.CACHE_ALL -> true
            CacheStrategy.CACHE_POPULAR -> isPopular(key)
            CacheStrategy.CACHE_PRIORITY -> isHighPriority(manifest)
            CacheStrategy.ADAPTIVE_TTL -> true
        }
    }
    
    private suspend fun putIfPopular(key: String, manifest: ScraperManifest): ManifestResult<Unit> {
        return if (isPopular(key)) {
            cache.put(key, manifest, calculateTtl(key, manifest))
        } else {
            ManifestResult.Success(Unit)
        }
    }
    
    private suspend fun putIfHighPriority(key: String, manifest: ScraperManifest): ManifestResult<Unit> {
        return if (isHighPriority(manifest)) {
            cache.put(key, manifest, calculateTtl(key, manifest))
        } else {
            ManifestResult.Success(Unit)
        }
    }
    
    private fun isPopular(key: String): Boolean {
        val accessCount = accessCounts[key] ?: 0
        return accessCount >= config.adaptiveTtlConfig.popularityThreshold
    }
    
    private fun isHighPriority(manifest: ScraperManifest): Boolean {
        return manifest.priorityOrder <= 10 && manifest.isEnabled
    }
    
    private fun trackAccess(key: String) {
        accessCounts[key] = (accessCounts[key] ?: 0) + 1
    }
    
    private fun calculateTtl(key: String, manifest: ScraperManifest): Long {
        return when (config.strategy) {
            CacheStrategy.ADAPTIVE_TTL -> calculateAdaptiveTtl(key, manifest)
            else -> config.defaultTtlMinutes
        }
    }
    
    private fun calculateAdaptiveTtl(key: String, manifest: ScraperManifest): Long {
        val adaptiveConfig = config.adaptiveTtlConfig
        val accessCount = accessCounts[key] ?: 0
        val lastUpdate = lastUpdateTimes[key]
        
        var ttl = adaptiveConfig.baselineUpdateIntervalMinutes
        
        // Increase TTL for popular items
        if (accessCount >= adaptiveConfig.popularityThreshold) {
            ttl = (ttl * adaptiveConfig.ttlMultiplier).toLong()
        }
        
        // Decrease TTL for recently updated items
        lastUpdate?.let { updateTime ->
            val timeSinceUpdate = Date().time - updateTime.time
            val hoursSinceUpdate = timeSinceUpdate / (1000 * 60 * 60)
            
            if (hoursSinceUpdate < 1) {
                ttl = (ttl * 0.5).toLong() // Reduce TTL for very recently updated items
            }
        }
        
        // Apply priority-based adjustments
        when {
            manifest.priorityOrder <= 5 -> ttl = (ttl * 1.5).toLong()
            manifest.priorityOrder > 50 -> ttl = (ttl * 0.8).toLong()
        }
        
        // Ensure TTL is within bounds
        return ttl.coerceIn(adaptiveConfig.minTtlMinutes, adaptiveConfig.maxTtlMinutes)
    }
    
    /**
     * Get cache recommendation for a manifest
     */
    fun getCacheRecommendation(key: String, manifest: ScraperManifest): CacheRecommendation {
        val shouldCacheResult = when (config.strategy) {
            CacheStrategy.NO_CACHE -> false
            CacheStrategy.CACHE_ALL -> true
            CacheStrategy.CACHE_POPULAR -> isPopular(key)
            CacheStrategy.CACHE_PRIORITY -> isHighPriority(manifest)
            CacheStrategy.ADAPTIVE_TTL -> true
        }
        
        val recommendedTtl = calculateTtl(key, manifest)
        val accessCount = accessCounts[key] ?: 0
        
        val reason = when {
            !shouldCacheResult && config.strategy == CacheStrategy.NO_CACHE -> "Caching disabled"
            !shouldCacheResult && config.strategy == CacheStrategy.CACHE_POPULAR -> "Not popular enough (${accessCount} accesses)"
            !shouldCacheResult && config.strategy == CacheStrategy.CACHE_PRIORITY -> "Low priority (${manifest.priorityOrder})"
            shouldCacheResult -> "Meets caching criteria"
            else -> "Unknown"
        }
        
        return CacheRecommendation(
            shouldCache = shouldCacheResult,
            recommendedTtlMinutes = recommendedTtl,
            reason = reason,
            accessCount = accessCount,
            isPopular = isPopular(key),
            isHighPriority = isHighPriority(manifest)
        )
    }
    
    /**
     * Get usage statistics
     */
    fun getUsageStatistics(): UsageStatistics {
        val totalAccesses = accessCounts.values.sum()
        val popularItems = accessCounts.count { it.value >= config.adaptiveTtlConfig.popularityThreshold }
        val mostAccessedKey = accessCounts.maxByOrNull { it.value }?.key
        val averageAccesses = if (accessCounts.isNotEmpty()) totalAccesses.toDouble() / accessCounts.size else 0.0
        
        return UsageStatistics(
            totalAccesses = totalAccesses,
            uniqueKeys = accessCounts.size,
            popularItems = popularItems,
            mostAccessedKey = mostAccessedKey,
            averageAccesses = averageAccesses,
            recentUpdates = lastUpdateTimes.size
        )
    }
    
    /**
     * Reset usage tracking
     */
    fun resetUsageTracking() {
        accessCounts.clear()
        lastUpdateTimes.clear()
    }
    
    /**
     * Cleanup old tracking data
     */
    fun cleanupOldTracking(olderThanHours: Long = 24) {
        val cutoffTime = Date(Date().time - olderThanHours * 60 * 60 * 1000)
        
        val keysToRemove = lastUpdateTimes.entries
            .filter { it.value.before(cutoffTime) }
            .map { it.key }
        
        keysToRemove.forEach { key ->
            lastUpdateTimes.remove(key)
            accessCounts.remove(key)
        }
    }
}

/**
 * Cache recommendation result
 */
data class CacheRecommendation(
    val shouldCache: Boolean,
    val recommendedTtlMinutes: Long,
    val reason: String,
    val accessCount: Int,
    val isPopular: Boolean,
    val isHighPriority: Boolean
)

/**
 * Usage statistics for smart caching
 */
data class UsageStatistics(
    val totalAccesses: Int,
    val uniqueKeys: Int,
    val popularItems: Int,
    val mostAccessedKey: String?,
    val averageAccesses: Double,
    val recentUpdates: Int
)