package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.rdwatch.androidtv.data.entities.LibraryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    
    @Query("SELECT * FROM library WHERE user_id = :userId ORDER BY added_at DESC")
    fun getLibraryByUser(userId: Long): Flow<List<LibraryEntity>>
    
    @Query("SELECT * FROM library WHERE user_id = :userId AND content_type = :contentType ORDER BY added_at DESC")
    fun getLibraryByUserAndType(userId: Long, contentType: String): Flow<List<LibraryEntity>>
    
    @Query("SELECT * FROM library WHERE user_id = :userId AND is_favorite = 1 ORDER BY added_at DESC")
    fun getFavoritesByUser(userId: Long): Flow<List<LibraryEntity>>
    
    @Query("SELECT * FROM library WHERE user_id = :userId AND is_downloaded = 1 ORDER BY added_at DESC")
    fun getDownloadedContentByUser(userId: Long): Flow<List<LibraryEntity>>
    
    @Query("SELECT * FROM library WHERE user_id = :userId AND content_id = :contentId")
    suspend fun getLibraryItem(userId: Long, contentId: String): LibraryEntity?
    
    @Query("SELECT * FROM library WHERE user_id = :userId AND content_id = :contentId")
    fun getLibraryItemFlow(userId: Long, contentId: String): Flow<LibraryEntity?>
    
    @Query("""
        SELECT * FROM library 
        WHERE user_id = :userId 
        AND (title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%')
        ORDER BY added_at DESC
    """)
    fun searchLibrary(userId: Long, query: String): Flow<List<LibraryEntity>>
    
    @Query("SELECT DISTINCT content_type FROM library WHERE user_id = :userId ORDER BY content_type")
    suspend fun getContentTypesByUser(userId: Long): List<String>
    
    @Query("SELECT COUNT(*) FROM library WHERE user_id = :userId")
    suspend fun getLibraryCountByUser(userId: Long): Int
    
    @Query("SELECT COUNT(*) FROM library WHERE user_id = :userId AND content_type = :contentType")
    suspend fun getLibraryCountByUserAndType(userId: Long, contentType: String): Int
    
    @Query("SELECT COUNT(*) FROM library WHERE user_id = :userId AND is_favorite = 1")
    suspend fun getFavoritesCountByUser(userId: Long): Int
    
    @Query("SELECT SUM(file_size_bytes) FROM library WHERE user_id = :userId AND is_downloaded = 1")
    suspend fun getTotalDownloadedSizeByUser(userId: Long): Long?
    
    @Query("SELECT COUNT(*) FROM library WHERE user_id = :userId AND content_id = :contentId")
    suspend fun isInLibrary(userId: Long, contentId: String): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryItem(libraryItem: LibraryEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLibraryItems(libraryItems: List<LibraryEntity>)
    
    @Update
    suspend fun updateLibraryItem(libraryItem: LibraryEntity)
    
    @Query("UPDATE library SET is_favorite = :isFavorite WHERE user_id = :userId AND content_id = :contentId")
    suspend fun updateFavoriteStatus(userId: Long, contentId: String, isFavorite: Boolean)
    
    @Query("""
        UPDATE library 
        SET is_downloaded = :isDownloaded, 
            file_path = :filePath, 
            file_size_bytes = :fileSizeBytes,
            updated_at = :updatedAt
        WHERE user_id = :userId AND content_id = :contentId
    """)
    suspend fun updateDownloadStatus(
        userId: Long,
        contentId: String,
        isDownloaded: Boolean,
        filePath: String?,
        fileSizeBytes: Long?,
        updatedAt: java.util.Date
    )
    
    @Delete
    suspend fun deleteLibraryItem(libraryItem: LibraryEntity)
    
    @Query("DELETE FROM library WHERE user_id = :userId AND content_id = :contentId")
    suspend fun deleteLibraryItem(userId: Long, contentId: String)
    
    @Query("DELETE FROM library WHERE user_id = :userId")
    suspend fun deleteAllLibraryForUser(userId: Long)
    
    @Query("DELETE FROM library WHERE is_downloaded = 1 AND file_path IS NULL")
    suspend fun cleanupOrphanedDownloads()
}