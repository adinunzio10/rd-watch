package com.rdwatch.androidtv.repository

import com.rdwatch.androidtv.data.entities.ContentEntity
import com.rdwatch.androidtv.network.models.UserInfo
import com.rdwatch.androidtv.repository.base.Result
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for managing Real-Debrid content operations
 */
interface RealDebridContentRepository {
    /**
     * Get all torrents from Real-Debrid as a Flow of ContentEntity list
     * @return Flow of Result containing list of ContentEntity
     */
    fun getTorrents(): Flow<Result<List<ContentEntity>>>

    /**
     * Get all downloads from Real-Debrid as a Flow of ContentEntity list
     * @return Flow of Result containing list of ContentEntity
     */
    fun getDownloads(): Flow<Result<List<ContentEntity>>>

    /**
     * Get detailed information about a specific torrent
     * @param id The torrent ID
     * @return ContentEntity if found, null otherwise
     */
    suspend fun getTorrentInfo(id: String): Result<ContentEntity?>

    /**
     * Unrestrict a link to get the direct playback URL
     * @param link The link to unrestrict
     * @return The direct playback URL
     */
    suspend fun unrestrictLink(link: String): Result<String>

    /**
     * Sync all content from Real-Debrid (torrents and downloads)
     * This fetches and caches all content for offline access
     */
    suspend fun syncContent(): Result<Unit>

    /**
     * Get all content (torrents and downloads combined)
     * @return Flow of Result containing list of all ContentEntity
     */
    fun getAllContent(): Flow<Result<List<ContentEntity>>>

    /**
     * Search content by title
     * @param query The search query
     * @return Flow of Result containing filtered list of ContentEntity
     */
    fun searchContent(query: String): Flow<Result<List<ContentEntity>>>

    /**
     * Delete a torrent from Real-Debrid
     * @param id The torrent ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteTorrent(id: String): Result<Unit>

    /**
     * Delete a download from Real-Debrid
     * @param id The download ID to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteDownload(id: String): Result<Unit>

    /**
     * Get torrents with pagination support
     * @param offset The offset for pagination (optional)
     * @param limit The number of items to return (optional, max 100)
     * @return Result containing list of ContentEntity
     */
    suspend fun getTorrentsPaginated(
        offset: Int? = null,
        limit: Int? = null,
    ): Result<List<ContentEntity>>

    /**
     * Delete multiple torrents from Real-Debrid
     * @param ids List of torrent IDs to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteTorrents(ids: List<String>): Result<Unit>

    /**
     * Get user information including storage usage
     * @return Result containing UserInfo data
     */
    suspend fun getUserInfo(): Result<UserInfo>

    /**
     * Get downloads with pagination support
     * @param offset The offset for pagination (optional)
     * @param limit The number of items to return (optional, max 100)
     * @return Result containing list of ContentEntity
     */
    suspend fun getDownloadsPaginated(
        offset: Int? = null,
        limit: Int? = null,
    ): Result<List<ContentEntity>>

    /**
     * Delete multiple downloads from Real-Debrid
     * @param ids List of download IDs to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteDownloads(ids: List<String>): Result<Unit>
}
