package com.rdwatch.androidtv.ui.filebrowser.repository

import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.filebrowser.models.FileItem
import kotlinx.coroutines.flow.Flow

/**
 * Cache information for the file browser
 */
data class CacheInfo(
    val totalSize: Long,
    val itemCount: Int,
    val lastUpdateTime: Long,
    val expirationTime: Long,
    val isExpired: Boolean,
)

/**
 * Repository interface for file browser operations
 * Abstracts the underlying account service (Real-Debrid, Premiumize, etc.)
 */
interface FileBrowserRepository {
    /**
     * Get the root content list (torrents/downloads)
     * @return Flow of Result containing list of FileItems
     */
    fun getRootContent(): Flow<Result<List<FileItem>>>

    /**
     * Get files within a torrent
     * @param torrentId The torrent ID to expand
     * @return Result containing list of files in the torrent
     */
    suspend fun getTorrentFiles(torrentId: String): Result<List<FileItem.File>>

    /**
     * Get a direct playback URL for a file
     * @param fileId The file ID
     * @param torrentId The parent torrent ID (if applicable)
     * @return Result containing the unrestricted playback URL
     */
    suspend fun getPlaybackUrl(
        fileId: String,
        torrentId: String? = null,
    ): Result<String>

    /**
     * Delete items (torrents or downloads)
     * @param itemIds Set of item IDs to delete
     * @return Result indicating success or failure
     */
    suspend fun deleteItems(itemIds: Set<String>): Result<Unit>

    /**
     * Download files for offline access
     * @param fileIds Set of file IDs to download
     * @return Result indicating success or failure
     */
    suspend fun downloadFiles(fileIds: Set<String>): Result<Unit>

    /**
     * Search content by query
     * @param query Search query
     * @return Flow of Result containing filtered FileItems
     */
    fun searchContent(query: String): Flow<Result<List<FileItem>>>

    /**
     * Get content with applied filters and sorting
     * @param filterOptions Filter options to apply
     * @param sortingOptions Sorting options to apply
     * @return Flow of Result containing filtered and sorted FileItems
     */
    fun getFilteredContent(
        filterOptions: com.rdwatch.androidtv.ui.filebrowser.models.FilterOptions,
        sortingOptions: com.rdwatch.androidtv.ui.filebrowser.models.SortingOptions,
    ): Flow<Result<List<FileItem>>>

    /**
     * Get content filtered by file type
     * @param fileTypes Set of file types to include
     * @return Flow of Result containing filtered FileItems
     */
    fun getContentByFileType(fileTypes: Set<com.rdwatch.androidtv.ui.filebrowser.models.FileType>): Flow<Result<List<FileItem>>>

    /**
     * Get content within a date range
     * @param startDate Start date timestamp
     * @param endDate End date timestamp
     * @return Flow of Result containing filtered FileItems
     */
    fun getContentByDateRange(
        startDate: Long,
        endDate: Long,
    ): Flow<Result<List<FileItem>>>

    /**
     * Get content within a size range
     * @param minSize Minimum file size in bytes
     * @param maxSize Maximum file size in bytes
     * @return Flow of Result containing filtered FileItems
     */
    fun getContentBySizeRange(
        minSize: Long,
        maxSize: Long,
    ): Flow<Result<List<FileItem>>>

    /**
     * Refresh content from the server
     * @return Result indicating success or failure
     */
    suspend fun refreshContent(): Result<Unit>

    /**
     * Force refresh content from the server, clearing all caches
     * @return Result indicating success or failure
     */
    suspend fun pullToRefresh(): Result<Unit>

    /**
     * Get detailed information about a specific item
     * @param itemId The item ID
     * @return Result containing the FileItem details
     */
    suspend fun getItemDetails(itemId: String): Result<FileItem>

    /**
     * Check if the service is authenticated
     * @return true if authenticated, false otherwise
     */
    suspend fun isAuthenticated(): Boolean

    /**
     * Bulk delete items with progress tracking
     * @param itemIds Set of item IDs to delete
     * @param onProgress Progress callback
     * @return Result indicating success or failure
     */
    suspend fun bulkDeleteItems(
        itemIds: Set<String>,
        onProgress: ((String, Float) -> Unit)? = null,
    ): Result<Unit>

    /**
     * Bulk download files with progress tracking
     * @param fileIds Set of file IDs to download
     * @param onProgress Progress callback
     * @return Result indicating success or failure
     */
    suspend fun bulkDownloadFiles(
        fileIds: Set<String>,
        onProgress: ((String, Float) -> Unit)? = null,
    ): Result<Unit>

    /**
     * Cancel ongoing operations
     */
    suspend fun cancelOperations(): Result<Unit>

    /**
     * Get cache status and size
     * @return Cache information
     */
    suspend fun getCacheInfo(): CacheInfo

    /**
     * Clear all caches
     */
    suspend fun clearCache(): Result<Unit>
}
