package com.rdwatch.androidtv.data.dao

import androidx.room.*
import com.rdwatch.androidtv.data.entities.DownloadEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DownloadDao {
    
    @Query("SELECT * FROM downloads ORDER BY generated DESC")
    fun getAllDownloads(): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE id = :downloadId")
    suspend fun getDownloadById(downloadId: String): DownloadEntity?
    
    @Query("SELECT * FROM downloads WHERE contentId = :contentId ORDER BY generated DESC")
    fun getDownloadsByContentId(contentId: Long): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE filename LIKE '%' || :query || '%' ORDER BY generated DESC")
    fun searchDownloads(query: String): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE streamable = 1 ORDER BY generated DESC")
    fun getStreamableDownloads(): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE host = :host ORDER BY generated DESC")
    fun getDownloadsByHost(host: String): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE mimeType LIKE :mimeTypePattern ORDER BY generated DESC")
    fun getDownloadsByMimeType(mimeTypePattern: String): Flow<List<DownloadEntity>>
    
    @Query("SELECT * FROM downloads WHERE mimeType LIKE 'video/%' ORDER BY generated DESC")
    fun getVideoDownloads(): Flow<List<DownloadEntity>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: DownloadEntity)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloads(downloads: List<DownloadEntity>)
    
    @Update
    suspend fun updateDownload(download: DownloadEntity)
    
    @Upsert
    suspend fun upsertDownload(download: DownloadEntity)
    
    @Upsert
    suspend fun upsertDownloads(downloads: List<DownloadEntity>)
    
    @Delete
    suspend fun deleteDownload(download: DownloadEntity)
    
    @Query("DELETE FROM downloads WHERE id = :downloadId")
    suspend fun deleteDownloadById(downloadId: String)
    
    @Query("DELETE FROM downloads WHERE contentId = :contentId")
    suspend fun deleteDownloadsByContentId(contentId: Long)
    
    @Query("UPDATE downloads SET contentId = :contentId WHERE id = :downloadId")
    suspend fun updateDownloadContentId(downloadId: String, contentId: Long)
    
    @Query("SELECT COUNT(*) FROM downloads")
    suspend fun getDownloadCount(): Int
    
    @Query("SELECT SUM(filesize) FROM downloads")
    suspend fun getTotalDownloadSize(): Long?
    
    @Query("DELETE FROM downloads WHERE generated < :beforeDate")
    suspend fun deleteDownloadsBefore(beforeDate: Long)
}