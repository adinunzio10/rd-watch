package com.rdwatch.androidtv.data.dao

import androidx.room.*
import com.rdwatch.androidtv.data.entities.StorageUsageEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for storage usage statistics
 * 
 * This DAO manages storage usage information from Real-Debrid account.
 * Typically contains a single row cache with current statistics.
 */
@Dao
interface StorageUsageDao {
    
    // Basic operations
    @Query("SELECT * FROM storage_usage WHERE id = 'current'")
    suspend fun getCurrentStorageUsage(): StorageUsageEntity?
    
    @Query("SELECT * FROM storage_usage WHERE id = 'current'")
    fun getCurrentStorageUsageFlow(): Flow<StorageUsageEntity?>
    
    @Query("SELECT * FROM storage_usage ORDER BY lastUpdated DESC LIMIT 1")
    suspend fun getLatestStorageUsage(): StorageUsageEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorageUsage(storageUsage: StorageUsageEntity)
    
    @Update
    suspend fun updateStorageUsage(storageUsage: StorageUsageEntity)
    
    @Upsert
    suspend fun upsertStorageUsage(storageUsage: StorageUsageEntity)
    
    @Delete
    suspend fun deleteStorageUsage(storageUsage: StorageUsageEntity)
    
    @Query("DELETE FROM storage_usage WHERE id = :id")
    suspend fun deleteStorageUsageById(id: String)
    
    // Cache management
    @Query("SELECT * FROM storage_usage WHERE lastUpdated >= :freshnessTime")
    suspend fun getFreshStorageUsage(freshnessTime: Long): StorageUsageEntity?
    
    @Query("SELECT COUNT(*) FROM storage_usage WHERE lastUpdated >= :freshnessTime")
    suspend fun hasFreshData(freshnessTime: Long): Int
    
    @Query("UPDATE storage_usage SET lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun updateTimestamp(timestamp: Long = System.currentTimeMillis())
    
    @Query("DELETE FROM storage_usage WHERE lastUpdated < :expirationTime")
    suspend fun deleteStaleData(expirationTime: Long)
    
    // Statistics queries
    @Query("SELECT totalSpaceBytes FROM storage_usage WHERE id = 'current'")
    suspend fun getTotalSpace(): Long?
    
    @Query("SELECT usedSpaceBytes FROM storage_usage WHERE id = 'current'")
    suspend fun getUsedSpace(): Long?
    
    @Query("SELECT freeSpaceBytes FROM storage_usage WHERE id = 'current'")
    suspend fun getFreeSpace(): Long?
    
    @Query("SELECT fileCount FROM storage_usage WHERE id = 'current'")
    suspend fun getFileCount(): Int?
    
    @Query("SELECT torrentCount FROM storage_usage WHERE id = 'current'")
    suspend fun getTorrentCount(): Int?
    
    @Query("SELECT downloadCount FROM storage_usage WHERE id = 'current'")
    suspend fun getDownloadCount(): Int?
    
    // Update specific fields
    @Query("UPDATE storage_usage SET totalSpaceBytes = :totalSpace, lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun updateTotalSpace(totalSpace: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE storage_usage SET usedSpaceBytes = :usedSpace, lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun updateUsedSpace(usedSpace: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE storage_usage SET freeSpaceBytes = :freeSpace, lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun updateFreeSpace(freeSpace: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE storage_usage SET fileCount = :fileCount, lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun updateFileCount(fileCount: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE storage_usage SET torrentCount = :torrentCount, lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun updateTorrentCount(torrentCount: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE storage_usage SET downloadCount = :downloadCount, lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun updateDownloadCount(downloadCount: Int, timestamp: Long = System.currentTimeMillis())
    
    // Bulk updates
    @Query("""
        UPDATE storage_usage SET 
        totalSpaceBytes = :totalSpace,
        usedSpaceBytes = :usedSpace,
        freeSpaceBytes = :freeSpace,
        lastUpdated = :timestamp
        WHERE id = 'current'
    """)
    suspend fun updateSpaceUsage(
        totalSpace: Long,
        usedSpace: Long,
        freeSpace: Long,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("""
        UPDATE storage_usage SET 
        fileCount = :fileCount,
        torrentCount = :torrentCount,
        downloadCount = :downloadCount,
        lastUpdated = :timestamp
        WHERE id = 'current'
    """)
    suspend fun updateCounts(
        fileCount: Int,
        torrentCount: Int,
        downloadCount: Int,
        timestamp: Long = System.currentTimeMillis()
    )
    
    @Query("""
        UPDATE storage_usage SET 
        totalSpaceBytes = :totalSpace,
        usedSpaceBytes = :usedSpace,
        freeSpaceBytes = :freeSpace,
        fileCount = :fileCount,
        torrentCount = :torrentCount,
        downloadCount = :downloadCount,
        lastUpdated = :timestamp
        WHERE id = 'current'
    """)
    suspend fun updateAllStats(
        totalSpace: Long,
        usedSpace: Long,
        freeSpace: Long,
        fileCount: Int,
        torrentCount: Int,
        downloadCount: Int,
        timestamp: Long = System.currentTimeMillis()
    )
    
    // Increment operations
    @Query("UPDATE storage_usage SET fileCount = fileCount + :increment, lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun incrementFileCount(increment: Int = 1, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE storage_usage SET torrentCount = torrentCount + :increment, lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun incrementTorrentCount(increment: Int = 1, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE storage_usage SET downloadCount = downloadCount + :increment, lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun incrementDownloadCount(increment: Int = 1, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE storage_usage SET usedSpaceBytes = usedSpaceBytes + :bytes, lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun incrementUsedSpace(bytes: Long, timestamp: Long = System.currentTimeMillis())
    
    // Decrement operations
    @Query("UPDATE storage_usage SET fileCount = MAX(0, fileCount - :decrement), lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun decrementFileCount(decrement: Int = 1, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE storage_usage SET torrentCount = MAX(0, torrentCount - :decrement), lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun decrementTorrentCount(decrement: Int = 1, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE storage_usage SET downloadCount = MAX(0, downloadCount - :decrement), lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun decrementDownloadCount(decrement: Int = 1, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE storage_usage SET usedSpaceBytes = MAX(0, usedSpaceBytes - :bytes), lastUpdated = :timestamp WHERE id = 'current'")
    suspend fun decrementUsedSpace(bytes: Long, timestamp: Long = System.currentTimeMillis())
    
    // Utility operations
    @Query("DELETE FROM storage_usage")
    suspend fun deleteAllStorageUsage()
    
    @Query("SELECT COUNT(*) FROM storage_usage")
    suspend fun getStorageUsageCount(): Int
    
    @Query("SELECT EXISTS(SELECT 1 FROM storage_usage WHERE id = 'current')")
    suspend fun hasCurrentStorageUsage(): Boolean
    
    @Query("SELECT lastUpdated FROM storage_usage WHERE id = 'current'")
    suspend fun getLastUpdatedTime(): Long?
    
    /**
     * Checks if current storage usage data is fresh
     */
    suspend fun isDataFresh(ttlMillis: Long = 600_000): Boolean { // 10 minutes default TTL
        val currentUsage = getCurrentStorageUsage() ?: return false
        return currentUsage.isFresh(ttlMillis)
    }
    
    /**
     * Gets fresh storage usage or null if stale
     */
    suspend fun getFreshStorageUsageOrNull(ttlMillis: Long = 600_000): StorageUsageEntity? {
        val currentUsage = getCurrentStorageUsage() ?: return null
        return if (currentUsage.isFresh(ttlMillis)) currentUsage else null
    }
}