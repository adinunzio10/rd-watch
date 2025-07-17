package com.rdwatch.androidtv.data.dao

import androidx.room.*
import com.rdwatch.androidtv.data.entities.ContentEntity
import com.rdwatch.androidtv.data.entities.ContentSource
import kotlinx.coroutines.flow.Flow

@Dao
interface ContentDao {
    @Query("SELECT * FROM content ORDER BY addedDate DESC")
    fun getAllContent(): Flow<List<ContentEntity>>

    @Query("SELECT * FROM content WHERE source = :source ORDER BY addedDate DESC")
    fun getContentBySource(source: ContentSource): Flow<List<ContentEntity>>

    @Query("SELECT * FROM content WHERE id = :contentId")
    suspend fun getContentById(contentId: Long): ContentEntity?

    @Query("SELECT * FROM content WHERE title LIKE '%' || :query || '%' ORDER BY addedDate DESC")
    fun searchContent(query: String): Flow<List<ContentEntity>>

    @Query("SELECT * FROM content WHERE isFavorite = 1 ORDER BY addedDate DESC")
    fun getFavoriteContent(): Flow<List<ContentEntity>>

    @Query("SELECT * FROM content WHERE isWatched = 1 ORDER BY lastPlayedDate DESC")
    fun getWatchedContent(): Flow<List<ContentEntity>>

    @Query("SELECT * FROM content ORDER BY lastPlayedDate DESC LIMIT :limit")
    fun getRecentlyPlayed(limit: Int = 10): Flow<List<ContentEntity>>

    @Query("SELECT * FROM content WHERE source = :source AND title LIKE '%' || :query || '%'")
    fun searchContentBySource(
        query: String,
        source: ContentSource,
    ): Flow<List<ContentEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertContent(content: ContentEntity): Long

    @Update
    suspend fun updateContent(content: ContentEntity)

    @Upsert
    suspend fun upsertContent(content: ContentEntity)

    @Upsert
    suspend fun upsertContent(contentList: List<ContentEntity>)

    @Delete
    suspend fun deleteContent(content: ContentEntity)

    @Query("DELETE FROM content WHERE id = :contentId")
    suspend fun deleteContentById(contentId: Long)

    @Query("UPDATE content SET isFavorite = :isFavorite WHERE id = :contentId")
    suspend fun updateFavoriteStatus(
        contentId: Long,
        isFavorite: Boolean,
    )

    @Query("UPDATE content SET isWatched = :isWatched WHERE id = :contentId")
    suspend fun updateWatchedStatus(
        contentId: Long,
        isWatched: Boolean,
    )

    @Query("UPDATE content SET lastPlayedDate = :date, playCount = playCount + 1 WHERE id = :contentId")
    suspend fun updatePlayedInfo(
        contentId: Long,
        date: Long = System.currentTimeMillis(),
    )
}
