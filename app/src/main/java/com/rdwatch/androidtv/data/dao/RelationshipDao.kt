package com.rdwatch.androidtv.data.dao

import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.rdwatch.androidtv.data.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface RelationshipDao {
    @Transaction
    @Query("SELECT * FROM users WHERE user_id = :userId")
    fun getUserWithWatchProgress(userId: Long): Flow<UserWithWatchProgress?>

    @Transaction
    @Query("SELECT * FROM users WHERE user_id = :userId")
    fun getUserWithLibrary(userId: Long): Flow<UserWithLibrary?>

    @Transaction
    @Query("SELECT * FROM users WHERE user_id = :userId")
    fun getUserWithSearchHistory(userId: Long): Flow<UserWithSearchHistory?>

    @Transaction
    @Query("SELECT * FROM content WHERE id = :contentId")
    suspend fun getContentWithTorrentsAndDownloads(contentId: Long): ContentWithRealDebridData?
}

data class UserWithWatchProgress(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "user_id",
    )
    val watchProgress: List<WatchProgressEntity>,
)

data class UserWithLibrary(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "user_id",
    )
    val library: List<LibraryEntity>,
)

data class UserWithSearchHistory(
    @Embedded val user: UserEntity,
    @Relation(
        parentColumn = "user_id",
        entityColumn = "user_id",
    )
    val searchHistory: List<SearchHistoryEntity>,
)

data class ContentWithRealDebridData(
    @Embedded val content: ContentEntity,
    @Relation(
        parentColumn = "realDebridId",
        entityColumn = "id",
    )
    val torrents: List<TorrentEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "contentId",
    )
    val downloads: List<DownloadEntity>,
)
