package com.rdwatch.androidtv.data.dao

import androidx.room.*
import com.rdwatch.androidtv.data.entities.FileHashEntity
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for file hash caching operations.
 */
@Dao
interface FileHashDao {
    /**
     * Get cached hash for a file if it exists and is still valid.
     * Validates file size and last modified timestamp to ensure hash is current.
     */
    @Query(
        """
        SELECT * FROM file_hashes 
        WHERE file_path = :filePath 
        AND file_size = :fileSize 
        AND last_modified = :lastModified
        LIMIT 1
    """,
    )
    suspend fun getCachedHash(
        filePath: String,
        fileSize: Long,
        lastModified: Long,
    ): FileHashEntity?

    /**
     * Get hash by file path only (for lookup without validation).
     */
    @Query("SELECT * FROM file_hashes WHERE file_path = :filePath LIMIT 1")
    suspend fun getHashByPath(filePath: String): FileHashEntity?

    /**
     * Insert or update file hash entry.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateHash(fileHash: FileHashEntity): Long

    /**
     * Update existing hash entry.
     */
    @Update
    suspend fun updateHash(fileHash: FileHashEntity)

    /**
     * Delete hash entry for a specific file path.
     */
    @Query("DELETE FROM file_hashes WHERE file_path = :filePath")
    suspend fun deleteHashByPath(filePath: String)

    /**
     * Delete all cached hashes (for cleanup).
     */
    @Query("DELETE FROM file_hashes")
    suspend fun deleteAllHashes()

    /**
     * Delete stale cache entries older than specified timestamp.
     */
    @Query("DELETE FROM file_hashes WHERE created_at < :timestamp")
    suspend fun deleteOldEntries(timestamp: Long)

    /**
     * Get all cached hashes for monitoring/debugging.
     */
    @Query("SELECT * FROM file_hashes ORDER BY created_at DESC")
    fun getAllHashes(): Flow<List<FileHashEntity>>

    /**
     * Get count of cached hashes.
     */
    @Query("SELECT COUNT(*) FROM file_hashes")
    suspend fun getHashCount(): Int

    /**
     * Check if hash exists for a file with current metadata.
     */
    @Query(
        """
        SELECT COUNT(*) > 0 FROM file_hashes 
        WHERE file_path = :filePath 
        AND file_size = :fileSize 
        AND last_modified = :lastModified
    """,
    )
    suspend fun hashExists(
        filePath: String,
        fileSize: Long,
        lastModified: Long,
    ): Boolean
}
