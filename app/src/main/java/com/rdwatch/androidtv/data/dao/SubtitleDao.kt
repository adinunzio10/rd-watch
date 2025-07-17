package com.rdwatch.androidtv.data.dao

import androidx.room.*
import com.rdwatch.androidtv.data.entities.*
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiProvider
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing subtitle cache data.
 * Provides efficient access to cached subtitle search results and files.
 */
@Dao
interface SubtitleDao {
    // ============ Cache Operations ============

    /**
     * Get cached search results for a specific search key.
     */
    @Transaction
    @Query(
        """
        SELECT * FROM subtitle_cache 
        WHERE searchKey = :searchKey 
        AND expiresAt > :currentTime
        LIMIT 1
    """,
    )
    suspend fun getCachedSearch(
        searchKey: String,
        currentTime: Long = System.currentTimeMillis(),
    ): SubtitleCacheWithResults?

    /**
     * Insert a new cache entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCacheEntry(cache: SubtitleCacheEntity): Long

    /**
     * Insert multiple subtitle results for a cache entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResults(results: List<SubtitleResultEntity>)

    /**
     * Clean up expired cache entries and their associated data.
     */
    @Query("DELETE FROM subtitle_cache WHERE expiresAt <= :currentTime")
    suspend fun cleanupExpiredCache(currentTime: Long = System.currentTimeMillis())

    /**
     * Get cache statistics for monitoring.
     */
    @Query(
        """
        SELECT 
            COUNT(*) as totalEntries,
            COUNT(CASE WHEN expiresAt > :currentTime THEN 1 END) as validEntries,
            COUNT(CASE WHEN expiresAt <= :currentTime THEN 1 END) as expiredEntries,
            AVG(resultCount) as avgResultsPerSearch
        FROM subtitle_cache
    """,
    )
    suspend fun getCacheStatistics(currentTime: Long = System.currentTimeMillis()): CacheStatistics

    // ============ File Operations ============

    /**
     * Get cached file for a specific content and language.
     */
    @Query(
        """
        SELECT * FROM subtitle_files 
        WHERE contentId = :contentId 
        AND language = :language 
        AND isActive = 1
        ORDER BY downloadTimestamp DESC
        LIMIT 1
    """,
    )
    suspend fun getCachedFile(
        contentId: String,
        language: String,
    ): SubtitleFileEntity?

    /**
     * Get all cached files for specific content.
     */
    @Query(
        """
        SELECT * FROM subtitle_files 
        WHERE contentId = :contentId 
        AND isActive = 1
        ORDER BY language, downloadTimestamp DESC
    """,
    )
    suspend fun getAllCachedFiles(contentId: String): List<SubtitleFileEntity>

    /**
     * Insert a new subtitle file entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubtitleFile(file: SubtitleFileEntity): Long

    /**
     * Update file access information.
     */
    @Update
    suspend fun updateFileAccess(file: SubtitleFileEntity)

    /**
     * Mark a file as inactive instead of deleting.
     */
    @Query("UPDATE subtitle_files SET isActive = 0 WHERE id = :fileId")
    suspend fun deactivateFile(fileId: Long)

    /**
     * Clean up old subtitle files based on age and usage.
     */
    @Query(
        """
        DELETE FROM subtitle_files 
        WHERE downloadTimestamp < :cutoffTime 
        AND accessCount < :minAccessCount
    """,
    )
    suspend fun cleanupOldFiles(
        cutoffTime: Long,
        minAccessCount: Int = 1,
    )

    /**
     * Get files that need cleanup (old, unused).
     */
    @Query(
        """
        SELECT * FROM subtitle_files 
        WHERE downloadTimestamp < :cutoffTime 
        AND lastAccessTime < :lastAccessCutoff
        ORDER BY lastAccessTime ASC
    """,
    )
    suspend fun getFilesForCleanup(
        cutoffTime: Long,
        lastAccessCutoff: Long,
    ): List<SubtitleFileEntity>

    // ============ Provider Statistics ============

    /**
     * Get provider statistics.
     */
    @Query("SELECT * FROM subtitle_provider_stats WHERE provider = :provider")
    suspend fun getProviderStats(provider: SubtitleApiProvider): SubtitleProviderStatsEntity?

    /**
     * Update provider statistics.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updateProviderStats(stats: SubtitleProviderStatsEntity)

    /**
     * Get all provider statistics.
     */
    @Query("SELECT * FROM subtitle_provider_stats ORDER BY totalRequests DESC")
    suspend fun getAllProviderStats(): List<SubtitleProviderStatsEntity>

    /**
     * Get health status of all providers.
     */
    @Query(
        """
        SELECT provider, isEnabled, consecutiveFailures, lastSuccessTime, lastFailureTime
        FROM subtitle_provider_stats
        ORDER BY provider
    """,
    )
    fun getAllProviderHealth(): Flow<List<ProviderHealthStatus>>

    // ============ Search and Analysis ============

    /**
     * Search cached results by content metadata.
     */
    @Transaction
    @Query(
        """
        SELECT sc.* FROM subtitle_cache sc
        WHERE (sc.contentTitle LIKE '%' || :query || '%' 
            OR sc.contentId LIKE '%' || :query || '%')
        AND sc.expiresAt > :currentTime
        ORDER BY sc.timestamp DESC
        LIMIT :limit
    """,
    )
    suspend fun searchCachedContent(
        query: String,
        currentTime: Long = System.currentTimeMillis(),
        limit: Int = 50,
    ): List<SubtitleCacheWithResults>

    /**
     * Get popular languages from cached results.
     */
    @Query(
        """
        SELECT sr.language, sr.languageName, COUNT(*) as count
        FROM subtitle_results sr
        INNER JOIN subtitle_cache sc ON sr.cacheId = sc.id
        WHERE sc.expiresAt > :currentTime
        GROUP BY sr.language, sr.languageName
        ORDER BY count DESC
        LIMIT :limit
    """,
    )
    suspend fun getPopularLanguages(
        currentTime: Long = System.currentTimeMillis(),
        limit: Int = 10,
    ): List<LanguagePopularity>

    /**
     * Get provider performance metrics.
     */
    @Query(
        """
        SELECT 
            sr.provider,
            COUNT(*) as totalResults,
            AVG(sr.matchScore) as avgMatchScore,
            AVG(sr.downloadCount) as avgDownloadCount,
            COUNT(CASE WHEN sr.isVerified = 1 THEN 1 END) as verifiedCount
        FROM subtitle_results sr
        INNER JOIN subtitle_cache sc ON sr.cacheId = sc.id
        WHERE sc.expiresAt > :currentTime
        GROUP BY sr.provider
        ORDER BY avgMatchScore DESC, totalResults DESC
    """,
    )
    suspend fun getProviderPerformanceMetrics(currentTime: Long = System.currentTimeMillis()): List<ProviderPerformanceMetrics>

    // ============ Maintenance Operations ============

    /**
     * Get database size information for cache management.
     */
    @Query(
        """
        SELECT 
            (SELECT COUNT(*) FROM subtitle_cache) as cacheEntries,
            (SELECT COUNT(*) FROM subtitle_results) as resultEntries,
            (SELECT COUNT(*) FROM subtitle_files) as fileEntries,
            (SELECT COUNT(*) FROM subtitle_provider_stats) as providerEntries
    """,
    )
    suspend fun getDatabaseSize(): DatabaseSizeInfo

    /**
     * Clear all subtitle cache data (nuclear option).
     */
    @Transaction
    suspend fun clearAllCache() {
        clearProviderStats()
        clearAllFiles()
        clearAllResults()
        clearAllCacheEntries()
    }

    @Query("DELETE FROM subtitle_provider_stats")
    suspend fun clearProviderStats()

    @Query("DELETE FROM subtitle_files")
    suspend fun clearAllFiles()

    @Query("DELETE FROM subtitle_results")
    suspend fun clearAllResults()

    @Query("DELETE FROM subtitle_cache")
    suspend fun clearAllCacheEntries()
}

// ============ Data Classes for Query Results ============

/**
 * Cache entry with associated results.
 */
data class SubtitleCacheWithResults(
    @Embedded val cache: SubtitleCacheEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "cacheId",
    )
    val results: List<SubtitleResultEntity>,
)

/**
 * Cache statistics for monitoring.
 */
data class CacheStatistics(
    val totalEntries: Int,
    val validEntries: Int,
    val expiredEntries: Int,
    val avgResultsPerSearch: Double,
)

/**
 * Provider health status.
 */
data class ProviderHealthStatus(
    val provider: SubtitleApiProvider,
    val isEnabled: Boolean,
    val consecutiveFailures: Int,
    val lastSuccessTime: Long?,
    val lastFailureTime: Long?,
)

/**
 * Language popularity metrics.
 */
data class LanguagePopularity(
    val language: String,
    val languageName: String,
    val count: Int,
)

/**
 * Provider performance metrics.
 */
data class ProviderPerformanceMetrics(
    val provider: SubtitleApiProvider,
    val totalResults: Int,
    val avgMatchScore: Double,
    val avgDownloadCount: Double?,
    val verifiedCount: Int,
)

/**
 * Database size information.
 */
data class DatabaseSizeInfo(
    val cacheEntries: Int,
    val resultEntries: Int,
    val fileEntries: Int,
    val providerEntries: Int,
) {
    val totalEntries: Int get() = cacheEntries + resultEntries + fileEntries + providerEntries
}
