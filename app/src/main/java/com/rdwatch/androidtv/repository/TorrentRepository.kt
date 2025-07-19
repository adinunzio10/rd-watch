package com.rdwatch.androidtv.repository

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import com.rdwatch.androidtv.data.dao.TorrentDao
import com.rdwatch.androidtv.data.entities.TorrentEntity
import com.rdwatch.androidtv.data.paging.RealDebridPagingSourceFactory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing torrent data with both local database and paging operations
 */
@Singleton
class TorrentRepository
    @Inject
    constructor(
        private val torrentDao: TorrentDao,
        private val pagingSourceFactory: RealDebridPagingSourceFactory,
    ) {
        companion object {
            private const val PAGE_SIZE = 20
        }

        /**
         * Get paginated torrents from Real-Debrid API
         */
        fun getTorrentsPaginated(filter: String? = null): Flow<PagingData<TorrentEntity>> {
            return Pager(
                config =
                    PagingConfig(
                        pageSize = PAGE_SIZE,
                        enablePlaceholders = false,
                        prefetchDistance = 3,
                        initialLoadSize = PAGE_SIZE * 2,
                    ),
                pagingSourceFactory = { pagingSourceFactory.create(filter) },
            ).flow
        }

        /**
         * Get all torrents from local database
         */
        fun getAllTorrentsLocal(): Flow<List<TorrentEntity>> {
            return torrentDao.getAllTorrents()
        }

        /**
         * Get torrents by status from local database
         */
        fun getTorrentsByStatus(status: String): Flow<List<TorrentEntity>> {
            return torrentDao.getTorrentsByStatus(status)
        }

        /**
         * Get active torrents (downloading, queued) from local database
         */
        fun getActiveTorrents(): Flow<List<TorrentEntity>> {
            return torrentDao.getActiveTorrents()
        }

        /**
         * Get completed torrents from local database
         */
        fun getCompletedTorrents(): Flow<List<TorrentEntity>> {
            return torrentDao.getCompletedTorrents()
        }

        /**
         * Search torrents in local database
         */
        fun searchTorrents(query: String): Flow<List<TorrentEntity>> {
            return torrentDao.searchTorrents(query)
        }

        /**
         * Get torrent by ID from local database
         */
        suspend fun getTorrentById(id: String): TorrentEntity? {
            return torrentDao.getTorrentById(id)
        }

        /**
         * Get active torrent count from local database
         */
        suspend fun getActiveTorrentCount(): Int {
            return torrentDao.getActiveTorrentCount()
        }

        /**
         * Insert or update torrents in local database
         */
        suspend fun upsertTorrents(torrents: List<TorrentEntity>) {
            torrentDao.upsertTorrents(torrents)
        }

        /**
         * Delete torrent from local database
         */
        suspend fun deleteTorrent(torrent: TorrentEntity) {
            torrentDao.deleteTorrent(torrent)
        }

        /**
         * Update torrent progress in local database
         */
        suspend fun updateTorrentProgress(
            torrentId: String,
            progress: Float,
            status: String,
        ) {
            torrentDao.updateTorrentProgress(torrentId, progress, status)
        }
    }
