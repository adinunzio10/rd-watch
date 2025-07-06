package com.rdwatch.androidtv.data.dao

import androidx.room.*
// import androidx.paging.PagingSource
import com.rdwatch.androidtv.data.entities.AccountFileEntity
import com.rdwatch.androidtv.ui.filebrowser.models.FileSource
import com.rdwatch.androidtv.ui.filebrowser.models.FileTypeCategory
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for account files
 * 
 * Provides database operations for the account file browser feature,
 * including pagination, filtering, and bulk operations.
 */
@Dao
interface AccountFileDao {
    
    // Basic CRUD operations
    @Query("SELECT * FROM account_files ORDER BY dateAdded DESC")
    fun getAllFiles(): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files WHERE id = :fileId")
    suspend fun getFileById(fileId: String): AccountFileEntity?
    
    @Query("SELECT * FROM account_files WHERE filename = :filename")
    suspend fun getFileByName(filename: String): AccountFileEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: AccountFileEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFiles(files: List<AccountFileEntity>)
    
    @Update
    suspend fun updateFile(file: AccountFileEntity)
    
    @Delete
    suspend fun deleteFile(file: AccountFileEntity)
    
    @Query("DELETE FROM account_files WHERE id = :fileId")
    suspend fun deleteFileById(fileId: String)
    
    @Query("DELETE FROM account_files WHERE id IN (:fileIds)")
    suspend fun deleteFilesByIds(fileIds: List<String>)
    
    // Pagination support - commented out until Paging3 is properly configured
    // @Query("SELECT * FROM account_files ORDER BY dateAdded DESC")
    // fun getAllFilesPaged(): PagingSource<Int, AccountFileEntity>
    
    // @Query("SELECT * FROM account_files WHERE filename LIKE '%' || :query || '%' ORDER BY dateAdded DESC")
    // fun searchFilesPaged(query: String): PagingSource<Int, AccountFileEntity>
    
    // @Query("""
    //     SELECT * FROM account_files 
    //     WHERE (:source IS NULL OR source = :source)
    //     AND (:fileType IS NULL OR fileTypeCategory = :fileType)
    //     AND (:isStreamable IS NULL OR isStreamable = :isStreamable)
    //     ORDER BY dateAdded DESC
    // """)
    // fun getFilteredFilesPaged(
    //     source: FileSource? = null,
    //     fileType: FileTypeCategory? = null,
    //     isStreamable: Boolean? = null
    // ): PagingSource<Int, AccountFileEntity>
    
    // Search operations
    @Query("SELECT * FROM account_files WHERE filename LIKE '%' || :query || '%' ORDER BY dateAdded DESC")
    fun searchFiles(query: String): Flow<List<AccountFileEntity>>
    
    @Query("""
        SELECT * FROM account_files 
        WHERE filename LIKE '%' || :query || '%'
        AND (:source IS NULL OR source = :source)
        AND (:fileType IS NULL OR fileTypeCategory = :fileType)
        ORDER BY dateAdded DESC
    """)
    fun searchFilesWithFilters(
        query: String,
        source: FileSource? = null,
        fileType: FileTypeCategory? = null
    ): Flow<List<AccountFileEntity>>
    
    // Filtering operations
    @Query("SELECT * FROM account_files WHERE source = :source ORDER BY dateAdded DESC")
    fun getFilesBySource(source: FileSource): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files WHERE fileTypeCategory = :fileType ORDER BY dateAdded DESC")
    fun getFilesByType(fileType: FileTypeCategory): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files WHERE isStreamable = 1 ORDER BY dateAdded DESC")
    fun getStreamableFiles(): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files WHERE filesize >= :minSize AND filesize <= :maxSize ORDER BY dateAdded DESC")
    fun getFilesBySize(minSize: Long, maxSize: Long): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files WHERE dateAdded >= :startDate AND dateAdded <= :endDate ORDER BY dateAdded DESC")
    fun getFilesByDateRange(startDate: Long, endDate: Long): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files WHERE parentTorrentId = :torrentId ORDER BY filename ASC")
    fun getFilesByTorrent(torrentId: String): Flow<List<AccountFileEntity>>
    
    // Sorting operations
    @Query("SELECT * FROM account_files ORDER BY filename ASC")
    fun getFilesSortedByNameAsc(): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files ORDER BY filename DESC")
    fun getFilesSortedByNameDesc(): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files ORDER BY filesize ASC")
    fun getFilesSortedBySizeAsc(): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files ORDER BY filesize DESC")
    fun getFilesSortedBySizeDesc(): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files ORDER BY dateAdded ASC")
    fun getFilesSortedByDateAsc(): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files ORDER BY dateAdded DESC")
    fun getFilesSortedByDateDesc(): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files ORDER BY fileTypeCategory ASC, filename ASC")
    fun getFilesSortedByTypeAsc(): Flow<List<AccountFileEntity>>
    
    @Query("SELECT * FROM account_files ORDER BY fileTypeCategory DESC, filename ASC")
    fun getFilesSortedByTypeDesc(): Flow<List<AccountFileEntity>>
    
    // Statistics operations
    @Query("SELECT COUNT(*) FROM account_files")
    suspend fun getFileCount(): Int
    
    @Query("SELECT COUNT(*) FROM account_files WHERE source = :source")
    suspend fun getFileCountBySource(source: FileSource): Int
    
    @Query("SELECT COUNT(*) FROM account_files WHERE fileTypeCategory = :fileType")
    suspend fun getFileCountByType(fileType: FileTypeCategory): Int
    
    @Query("SELECT SUM(filesize) FROM account_files")
    suspend fun getTotalFileSize(): Long
    
    @Query("SELECT SUM(filesize) FROM account_files WHERE source = :source")
    suspend fun getTotalFileSizeBySource(source: FileSource): Long
    
    @Query("SELECT SUM(filesize) FROM account_files WHERE fileTypeCategory = :fileType")
    suspend fun getTotalFileSizeByType(fileType: FileTypeCategory): Long
    
    @Query("SELECT AVG(filesize) FROM account_files")
    suspend fun getAverageFileSize(): Double
    
    @Query("SELECT fileTypeCategory, COUNT(*) as count FROM account_files GROUP BY fileTypeCategory")
    suspend fun getFileTypeStats(): List<FileTypeStats>
    
    // Cache management
    @Query("SELECT * FROM account_files WHERE lastUpdated < :expirationTime")
    suspend fun getStaleFiles(expirationTime: Long): List<AccountFileEntity>
    
    @Query("DELETE FROM account_files WHERE lastUpdated < :expirationTime")
    suspend fun deleteStaleFiles(expirationTime: Long)
    
    @Query("SELECT COUNT(*) FROM account_files WHERE lastUpdated >= :freshnessTime")
    suspend fun getFreshFileCount(freshnessTime: Long): Int
    
    @Query("UPDATE account_files SET lastUpdated = :timestamp WHERE id = :fileId")
    suspend fun updateFileTimestamp(fileId: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("UPDATE account_files SET lastUpdated = :timestamp WHERE id IN (:fileIds)")
    suspend fun updateFileTimestamps(fileIds: List<String>, timestamp: Long = System.currentTimeMillis())
    
    // Bulk operations
    @Query("SELECT * FROM account_files WHERE id IN (:fileIds)")
    suspend fun getFilesByIds(fileIds: List<String>): List<AccountFileEntity>
    
    @Query("UPDATE account_files SET isStreamable = :isStreamable WHERE id IN (:fileIds)")
    suspend fun updateStreamableStatus(fileIds: List<String>, isStreamable: Boolean)
    
    @Query("DELETE FROM account_files")
    suspend fun deleteAllFiles()
    
    @Query("DELETE FROM account_files WHERE source = :source")
    suspend fun deleteFilesBySource(source: FileSource)
    
    @Query("DELETE FROM account_files WHERE fileTypeCategory = :fileType")
    suspend fun deleteFilesByType(fileType: FileTypeCategory)
    
    // Torrent-specific operations
    @Query("SELECT DISTINCT parentTorrentId FROM account_files WHERE parentTorrentId IS NOT NULL")
    suspend fun getAllTorrentIds(): List<String>
    
    @Query("SELECT COUNT(*) FROM account_files WHERE parentTorrentId = :torrentId")
    suspend fun getFileCountByTorrent(torrentId: String): Int
    
    @Query("SELECT SUM(filesize) FROM account_files WHERE parentTorrentId = :torrentId")
    suspend fun getTotalSizeByTorrent(torrentId: String): Long
    
    @Query("DELETE FROM account_files WHERE parentTorrentId = :torrentId")
    suspend fun deleteFilesByTorrent(torrentId: String)
    
    @Query("UPDATE account_files SET torrentProgress = :progress WHERE parentTorrentId = :torrentId")
    suspend fun updateTorrentProgress(torrentId: String, progress: Float)
    
    @Query("UPDATE account_files SET torrentStatus = :status WHERE parentTorrentId = :torrentId")
    suspend fun updateTorrentStatus(torrentId: String, status: String)
}

/**
 * Data class for file type statistics results
 */
data class FileTypeStats(
    val fileTypeCategory: String,
    val count: Int
)