package com.rdwatch.androidtv.data.dao

import androidx.room.*
import com.rdwatch.androidtv.data.entities.TorrentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TorrentDao {
    @Query("SELECT * FROM torrents ORDER BY added DESC")
    fun getAllTorrents(): Flow<List<TorrentEntity>>

    @Query("SELECT * FROM torrents WHERE id = :torrentId")
    suspend fun getTorrentById(torrentId: String): TorrentEntity?

    @Query("SELECT * FROM torrents WHERE hash = :hash")
    suspend fun getTorrentByHash(hash: String): TorrentEntity?

    @Query("SELECT * FROM torrents WHERE status = :status ORDER BY added DESC")
    fun getTorrentsByStatus(status: String): Flow<List<TorrentEntity>>

    @Query("SELECT * FROM torrents WHERE filename LIKE '%' || :query || '%' ORDER BY added DESC")
    fun searchTorrents(query: String): Flow<List<TorrentEntity>>

    @Query("SELECT * FROM torrents WHERE status IN ('downloading', 'queued') ORDER BY added DESC")
    fun getActiveTorrents(): Flow<List<TorrentEntity>>

    @Query("SELECT * FROM torrents WHERE status = 'downloaded' ORDER BY added DESC")
    fun getCompletedTorrents(): Flow<List<TorrentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTorrent(torrent: TorrentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTorrents(torrents: List<TorrentEntity>)

    @Update
    suspend fun updateTorrent(torrent: TorrentEntity)

    @Upsert
    suspend fun upsertTorrent(torrent: TorrentEntity)

    @Upsert
    suspend fun upsertTorrents(torrents: List<TorrentEntity>)

    @Delete
    suspend fun deleteTorrent(torrent: TorrentEntity)

    @Query("DELETE FROM torrents WHERE id = :torrentId")
    suspend fun deleteTorrentById(torrentId: String)

    @Query("UPDATE torrents SET progress = :progress, status = :status WHERE id = :torrentId")
    suspend fun updateTorrentProgress(
        torrentId: String,
        progress: Float,
        status: String,
    )

    @Query("UPDATE torrents SET speed = :speed, seeders = :seeders WHERE id = :torrentId")
    suspend fun updateTorrentStats(
        torrentId: String,
        speed: Long,
        seeders: Int,
    )

    @Query("DELETE FROM torrents WHERE status = 'error' AND added < :beforeDate")
    suspend fun deleteErroredTorrentsBefore(beforeDate: Long)

    @Query("SELECT COUNT(*) FROM torrents WHERE status IN ('downloading', 'queued')")
    suspend fun getActiveTorrentCount(): Int
}
