package com.rdwatch.androidtv.data.dao

import androidx.room.*
import com.rdwatch.androidtv.data.entities.FileTypeStatsEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for file type statistics
 * 
 * This DAO manages aggregated statistics for different file types,
 * providing insights into storage usage patterns and file distribution.
 */
@Dao
interface FileTypeStatsDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM file_type_stats ORDER BY fileType ASC")
    fun getAllFileTypeStats(): Flow<List<FileTypeStatsEntity>>
    
    @Query("SELECT * FROM file_type_stats WHERE id = :id")
    suspend fun getFileTypeStatsById(id: String): FileTypeStatsEntity?
    
    @Query("SELECT * FROM file_type_stats WHERE fileType = :fileType")
    suspend fun getFileTypeStatsByType(fileType: String): FileTypeStatsEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileTypeStats(stats: FileTypeStatsEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFileTypeStats(statsList: List<FileTypeStatsEntity>)
    
    @Update
    suspend fun updateFileTypeStats(stats: FileTypeStatsEntity)
    
    @Upsert
    suspend fun upsertFileTypeStats(stats: FileTypeStatsEntity)
    
    @Upsert
    suspend fun upsertFileTypeStats(statsList: List<FileTypeStatsEntity>)
    
    @Delete
    suspend fun deleteFileTypeStats(stats: FileTypeStatsEntity)
    
    @Query("DELETE FROM file_type_stats WHERE id = :id")
    suspend fun deleteFileTypeStatsById(id: String)
    
    @Query("DELETE FROM file_type_stats WHERE fileType = :fileType")
    suspend fun deleteFileTypeStatsByType(fileType: String)
    
    // Statistics queries
    @Query("SELECT SUM(fileCount) FROM file_type_stats")
    suspend fun getTotalFileCount(): Int
    
    @Query("SELECT SUM(totalSizeBytes) FROM file_type_stats")
    suspend fun getTotalSizeBytes(): Long
    
    @Query("SELECT AVG(totalSizeBytes / CASE WHEN fileCount > 0 THEN fileCount ELSE 1 END) FROM file_type_stats WHERE fileCount > 0")
    suspend fun getAverageFileSizeAcrossTypes(): Double
    
    @Query("SELECT COUNT(DISTINCT fileType) FROM file_type_stats WHERE fileCount > 0")
    suspend fun getActiveFileTypeCount(): Int
    
    @Query("SELECT * FROM file_type_stats WHERE fileCount > 0 ORDER BY fileCount DESC")
    fun getFileTypeStatsByCountDesc(): Flow<List<FileTypeStatsEntity>>
    
    @Query("SELECT * FROM file_type_stats WHERE fileCount > 0 ORDER BY totalSizeBytes DESC")
    fun getFileTypeStatsBySizeDesc(): Flow<List<FileTypeStatsEntity>>
    
    @Query("SELECT * FROM file_type_stats WHERE fileCount > 0 ORDER BY (totalSizeBytes / fileCount) DESC")
    fun getFileTypeStatsByAverageSizeDesc(): Flow<List<FileTypeStatsEntity>>
    
    // Top N queries
    @Query("SELECT * FROM file_type_stats WHERE fileCount > 0 ORDER BY fileCount DESC LIMIT :limit")
    suspend fun getTopFileTypesByCount(limit: Int = 5): List<FileTypeStatsEntity>
    
    @Query("SELECT * FROM file_type_stats WHERE fileCount > 0 ORDER BY totalSizeBytes DESC LIMIT :limit")
    suspend fun getTopFileTypesBySize(limit: Int = 5): List<FileTypeStatsEntity>
    
    @Query("SELECT * FROM file_type_stats WHERE fileCount > 0 ORDER BY (totalSizeBytes / fileCount) DESC LIMIT :limit")
    suspend fun getTopFileTypesByAverageSize(limit: Int = 5): List<FileTypeStatsEntity>
    
    // Update operations
    @Query("UPDATE file_type_stats SET fileCount = :fileCount, lastUpdated = :timestamp WHERE fileType = :fileType")
    suspend fun updateFileCount(fileType: String, fileCount: Int, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE file_type_stats SET totalSizeBytes = :totalSize, lastUpdated = :timestamp WHERE fileType = :fileType")
    suspend fun updateTotalSize(fileType: String, totalSize: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE file_type_stats SET 
        fileCount = :fileCount, 
        totalSizeBytes = :totalSize, 
        lastUpdated = :timestamp 
        WHERE fileType = :fileType
    """)
    suspend fun updateFileTypeStats(
        fileType: String,
        fileCount: Int,
        totalSize: Long,
        timestamp: Long = System.currentTimeMillis()
    )
    
    // Increment operations
    @Query("UPDATE file_type_stats SET fileCount = fileCount + :increment, lastUpdated = :timestamp WHERE fileType = :fileType")
    suspend fun incrementFileCount(fileType: String, increment: Int = 1, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE file_type_stats SET totalSizeBytes = totalSizeBytes + :sizeBytes, lastUpdated = :timestamp WHERE fileType = :fileType")
    suspend fun incrementTotalSize(fileType: String, sizeBytes: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE file_type_stats SET 
        fileCount = fileCount + :fileIncrement,
        totalSizeBytes = totalSizeBytes + :sizeIncrement,
        lastUpdated = :timestamp
        WHERE fileType = :fileType
    """)
    suspend fun incrementStats(
        fileType: String,
        fileIncrement: Int = 1,
        sizeIncrement: Long,
        timestamp: Long = System.currentTimeMillis()
    )
    
    // Decrement operations
    @Query("UPDATE file_type_stats SET fileCount = MAX(0, fileCount - :decrement), lastUpdated = :timestamp WHERE fileType = :fileType")
    suspend fun decrementFileCount(fileType: String, decrement: Int = 1, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE file_type_stats SET totalSizeBytes = MAX(0, totalSizeBytes - :sizeBytes), lastUpdated = :timestamp WHERE fileType = :fileType")
    suspend fun decrementTotalSize(fileType: String, sizeBytes: Long, timestamp: Long = System.currentTimeMillis())
    
    @Query("""
        UPDATE file_type_stats SET 
        fileCount = MAX(0, fileCount - :fileDecrement),
        totalSizeBytes = MAX(0, totalSizeBytes - :sizeDecrement),
        lastUpdated = :timestamp
        WHERE fileType = :fileType
    """)
    suspend fun decrementStats(
        fileType: String,
        fileDecrement: Int = 1,
        sizeDecrement: Long,
        timestamp: Long = System.currentTimeMillis()
    )
    
    // Cache management
    @Query("SELECT * FROM file_type_stats WHERE lastUpdated < :expirationTime")
    suspend fun getStaleStats(expirationTime: Long): List<FileTypeStatsEntity>
    
    @Query("DELETE FROM file_type_stats WHERE lastUpdated < :expirationTime")
    suspend fun deleteStaleStats(expirationTime: Long)
    
    @Query("SELECT COUNT(*) FROM file_type_stats WHERE lastUpdated >= :freshnessTime")
    suspend fun getFreshStatsCount(freshnessTime: Long): Int
    
    @Query("UPDATE file_type_stats SET lastUpdated = :timestamp WHERE fileType = :fileType")
    suspend fun updateTimestamp(fileType: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE file_type_stats SET lastUpdated = :timestamp")
    suspend fun updateAllTimestamps(timestamp: Long = System.currentTimeMillis())
    
    // Conditional operations
    @Query("SELECT EXISTS(SELECT 1 FROM file_type_stats WHERE fileType = :fileType)")
    suspend fun existsByFileType(fileType: String): Boolean
    
    @Query("SELECT fileCount FROM file_type_stats WHERE fileType = :fileType")
    suspend fun getFileCountByType(fileType: String): Int?
    
    @Query("SELECT totalSizeBytes FROM file_type_stats WHERE fileType = :fileType")
    suspend fun getTotalSizeByType(fileType: String): Long?
    
    @Query("SELECT (totalSizeBytes / CASE WHEN fileCount > 0 THEN fileCount ELSE 1 END) FROM file_type_stats WHERE fileType = :fileType")
    suspend fun getAverageFileSizeByType(fileType: String): Long?
    
    // Bulk operations
    @Query("DELETE FROM file_type_stats")
    suspend fun deleteAllStats()
    
    @Query("DELETE FROM file_type_stats WHERE fileCount = 0")
    suspend fun deleteEmptyStats()
    
    @Query("UPDATE file_type_stats SET fileCount = 0, totalSizeBytes = 0, lastUpdated = :timestamp")
    suspend fun resetAllStats(timestamp: Long = System.currentTimeMillis())
    
    // Aggregation queries
    @Query("""
        SELECT 
        fileType,
        fileCount,
        totalSizeBytes,
        (totalSizeBytes * 100.0 / (SELECT SUM(totalSizeBytes) FROM file_type_stats WHERE totalSizeBytes > 0)) as sizePercentage
        FROM file_type_stats 
        WHERE fileCount > 0 
        ORDER BY totalSizeBytes DESC
    """)
    suspend fun getFileTypeDistribution(): List<FileTypeDistribution>
    
    @Query("SELECT fileType FROM file_type_stats WHERE fileCount > 0 ORDER BY fileCount DESC")
    suspend fun getMostCommonFileTypes(): List<String>
    
    @Query("SELECT fileType FROM file_type_stats WHERE totalSizeBytes > 0 ORDER BY totalSizeBytes DESC")
    suspend fun getLargestFileTypes(): List<String>
    
    /**
     * Ensures a file type entry exists, creating it if necessary
     */
    suspend fun ensureFileTypeExists(fileType: String) {
        if (!existsByFileType(fileType)) {
            insertFileTypeStats(FileTypeStatsEntity.forFileType(fileType))
        }
    }
    
    /**
     * Adds a file to statistics, creating the entry if it doesn't exist
     */
    suspend fun addFileToStats(fileType: String, fileSize: Long) {
        ensureFileTypeExists(fileType)
        incrementStats(fileType, 1, fileSize)
    }
    
    /**
     * Removes a file from statistics
     */
    suspend fun removeFileFromStats(fileType: String, fileSize: Long) {
        if (existsByFileType(fileType)) {
            decrementStats(fileType, 1, fileSize)
        }
    }
}

/**
 * Data class for file type distribution results
 */
data class FileTypeDistribution(
    val fileType: String,
    val fileCount: Int,
    val totalSizeBytes: Long,
    val sizePercentage: Double
)