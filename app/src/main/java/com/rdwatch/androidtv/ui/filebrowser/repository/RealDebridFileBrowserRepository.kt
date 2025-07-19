package com.rdwatch.androidtv.ui.filebrowser.repository

import com.rdwatch.androidtv.core.error.ErrorHandler
import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.network.api.RealDebridApiService
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.repository.base.safeCall
import com.rdwatch.androidtv.ui.filebrowser.mappers.filteredByOptions
import com.rdwatch.androidtv.ui.filebrowser.mappers.sortedByOptions
import com.rdwatch.androidtv.ui.filebrowser.mappers.toFileItem
import com.rdwatch.androidtv.ui.filebrowser.models.FileItem
import com.rdwatch.androidtv.ui.filebrowser.models.FileStatus
import com.rdwatch.androidtv.ui.filebrowser.models.FileType
import com.rdwatch.androidtv.ui.filebrowser.models.FilterOptions
import com.rdwatch.androidtv.ui.filebrowser.models.SortingOptions
import com.rdwatch.androidtv.ui.filebrowser.models.TorrentStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Real-Debrid implementation of FileBrowserRepository
 * Handles file browsing operations for Real-Debrid account with enhanced caching
 */
@Singleton
class RealDebridFileBrowserRepository
    @Inject
    constructor(
        private val apiService: RealDebridApiService,
        private val contentRepository: RealDebridContentRepository,
        private val dispatcherProvider: DispatcherProvider,
        private val errorHandler: ErrorHandler,
    ) : FileBrowserRepository {
        // Enhanced caching layer with file-browser specific caching
        private val fileBrowserCache = MutableStateFlow<CachedData<List<FileItem>>>(CachedData.empty())
        private val torrentFilesCache = ConcurrentHashMap<String, CachedData<List<FileItem.File>>>()
        private val playbackUrlCache = ConcurrentHashMap<String, CachedData<String>>()

        // Cache management
        private val cacheMutex = Mutex()
        private val cacheExpirationMs = TimeUnit.MINUTES.toMillis(5) // 5 minutes TTL

        override fun getRootContent(): Flow<Result<List<FileItem>>> =
            flow {
                emit(Result.Loading)

                // Check cache first
                val cachedData = fileBrowserCache.value
                if (cachedData.isValid(cacheExpirationMs)) {
                    emit(Result.Success(cachedData.data))
                    return@flow
                }

                // Fetch from content repository (which handles its own caching)
                try {
                    contentRepository.getAllContent().collect { result ->
                        when (result) {
                            is Result.Success -> {
                                val fileItems =
                                    result.data.map { contentEntity ->
                                        // Convert ContentEntity to FileItem.Torrent
                                        // Note: This is a simplified conversion - in a real scenario,
                                        // you'd need more detailed mapping based on the actual API response
                                        FileItem.Torrent(
                                            id = contentEntity.realDebridId ?: contentEntity.id.toString(),
                                            name = contentEntity.title,
                                            size = 0L, // Size would come from API
                                            modifiedDate = contentEntity.addedDate.time,
                                            hash = "", // Would come from API
                                            progress = 100f, // Assume downloaded
                                            status = TorrentStatus.DOWNLOADED,
                                            seeders = null,
                                            speed = null,
                                            files = emptyList(),
                                        )
                                    }

                                // Update cache
                                fileBrowserCache.value = CachedData(fileItems, System.currentTimeMillis())
                                emit(Result.Success(fileItems))
                            }
                            is Result.Error -> emit(result)
                            is Result.Loading -> emit(result)
                        }
                    }
                } catch (e: Exception) {
                    emit(Result.Error(e))
                }
            }.flowOn(dispatcherProvider.io).distinctUntilChanged()

        override suspend fun getTorrentFiles(torrentId: String): Result<List<FileItem.File>> =
            withContext(dispatcherProvider.io) {
                // Check cache first
                val cachedFiles = torrentFilesCache[torrentId]
                if (cachedFiles != null && cachedFiles.isValid(cacheExpirationMs)) {
                    return@withContext Result.Success(cachedFiles.data)
                }

                // Fetch from API
                safeCall {
                    val response = apiService.getTorrentInfo(torrentId)
                    if (response.isSuccessful) {
                        val torrentInfo = response.body()
                        if (torrentInfo != null) {
                            val files =
                                torrentInfo.files.map { torrentFile ->
                                    val extension = torrentFile.path.substringAfterLast('.', "").lowercase()
                                    val fileType = FileType.fromExtension(extension)
                                    val isPlayable = fileType == FileType.VIDEO || fileType == FileType.AUDIO

                                    FileItem.File(
                                        id = "$torrentId-${torrentFile.id}",
                                        name = torrentFile.path.substringAfterLast('/'),
                                        size = torrentFile.bytes,
                                        modifiedDate = System.currentTimeMillis(),
                                        mimeType = getMimeTypeFromExtension(extension),
                                        downloadUrl = torrentInfo.links.firstOrNull(),
                                        streamUrl = if (isPlayable) torrentInfo.links.firstOrNull() else null,
                                        isPlayable = isPlayable,
                                        progress = if (torrentFile.selected == 1) 100f else null,
                                        status = if (torrentFile.selected == 1) FileStatus.READY else FileStatus.UNAVAILABLE,
                                    )
                                }

                            // Cache the result
                            torrentFilesCache[torrentId] = CachedData(files, System.currentTimeMillis())
                            files
                        } else {
                            throw Exception("No torrent info in response")
                        }
                    } else {
                        throw Exception("Failed to fetch torrent files: ${response.code()}")
                    }
                }
            }

        override suspend fun getPlaybackUrl(
            fileId: String,
            torrentId: String?,
        ): Result<String> =
            withContext(dispatcherProvider.io) {
                val cacheKey = "${torrentId}_$fileId"

                // Check cache first
                val cachedUrl = playbackUrlCache[cacheKey]
                if (cachedUrl != null && cachedUrl.isValid(cacheExpirationMs)) {
                    return@withContext Result.Success(cachedUrl.data)
                }

                // Get torrent info to find the file's download link
                if (torrentId != null) {
                    val torrentResult =
                        safeCall {
                            val response = apiService.getTorrentInfo(torrentId)
                            if (response.isSuccessful) {
                                val torrentInfo = response.body()
                                if (torrentInfo != null) {
                                    // Find the specific file by ID
                                    val targetFile = torrentInfo.files.find { "$torrentId-${it.id}" == fileId }
                                    if (targetFile != null && targetFile.selected == 1) {
                                        // Get the download link for this file
                                        val linkIndex = torrentInfo.files.indexOf(targetFile)
                                        if (linkIndex < torrentInfo.links.size) {
                                            val link = torrentInfo.links[linkIndex]
                                            // Unrestrict the link to get direct playback URL
                                            contentRepository.unrestrictLink(link)
                                        } else {
                                            Result.Error(Exception("No download link found for file"))
                                        }
                                    } else {
                                        Result.Error(Exception("File not found or not selected"))
                                    }
                                } else {
                                    Result.Error(Exception("No torrent info in response"))
                                }
                            } else {
                                Result.Error(Exception("Failed to fetch torrent info: ${response.code()}"))
                            }
                        }

                    when (torrentResult) {
                        is Result.Success -> {
                            when (val unrestrictResult = torrentResult.data) {
                                is Result.Success -> {
                                    // Cache the result
                                    playbackUrlCache[cacheKey] = CachedData(unrestrictResult.data, System.currentTimeMillis())
                                    Result.Success(unrestrictResult.data)
                                }
                                is Result.Error -> unrestrictResult
                                is Result.Loading -> Result.Error(Exception("Unexpected loading state"))
                            }
                        }
                        is Result.Error -> torrentResult
                        is Result.Loading -> Result.Error(Exception("Unexpected loading state"))
                    }
                } else {
                    Result.Error(Exception("Torrent ID is required for playback URL"))
                }
            }

        override suspend fun deleteItems(itemIds: Set<String>): Result<Unit> =
            withContext(dispatcherProvider.io) {
                safeCall {
                    val torrentIds = mutableListOf<String>()
                    val downloadIds = mutableListOf<String>()

                    // Separate torrent IDs from download IDs based on the item structure
                    // This is a simplified approach - in practice, you'd need to determine
                    // the type based on the actual data structure
                    itemIds.forEach { id ->
                        if (id.contains("-")) {
                            // This is likely a file ID, extract torrent ID
                            val torrentId = id.substringBefore("-")
                            if (!torrentIds.contains(torrentId)) {
                                torrentIds.add(torrentId)
                            }
                        } else {
                            // This is likely a torrent or download ID
                            torrentIds.add(id)
                        }
                    }

                    // Delete torrents
                    if (torrentIds.isNotEmpty()) {
                        val result = contentRepository.deleteTorrents(torrentIds)
                        when (result) {
                            is Result.Success -> {
                                // Invalidate caches
                                invalidateRelatedCaches(torrentIds)
                            }
                            is Result.Error -> throw result.exception
                            is Result.Loading -> {} // Shouldn't happen in suspend function
                        }
                    }

                    // Delete downloads
                    if (downloadIds.isNotEmpty()) {
                        val result = contentRepository.deleteDownloads(downloadIds)
                        when (result) {
                            is Result.Success -> {
                                // Invalidate caches
                                invalidateRelatedCaches(downloadIds)
                            }
                            is Result.Error -> throw result.exception
                            is Result.Loading -> {} // Shouldn't happen in suspend function
                        }
                    }

                    Unit
                }
            }

        override suspend fun downloadFiles(fileIds: Set<String>): Result<Unit> =
            withContext(dispatcherProvider.io) {
                // For Real-Debrid, files are already "downloaded" to their servers
                // This operation would typically involve selecting files in a torrent
                // or adding them to the download queue

                safeCall {
                    fileIds.forEach { fileId ->
                        if (fileId.contains("-")) {
                            val torrentId = fileId.substringBefore("-")
                            val actualFileId = fileId.substringAfter("-")

                            // Select the file in the torrent
                            val response = apiService.selectFiles(torrentId, actualFileId)
                            if (!response.isSuccessful) {
                                throw Exception("Failed to select file $fileId: ${response.code()}")
                            }
                        }
                    }

                    // Invalidate caches to reflect the changes
                    invalidateAllCaches()
                    Unit
                }
            }

        override fun searchContent(query: String): Flow<Result<List<FileItem>>> =
            getRootContent().map { result ->
                when (result) {
                    is Result.Success -> {
                        val filtered =
                            result.data.filter { item ->
                                item.name.contains(query, ignoreCase = true)
                            }
                        Result.Success(filtered)
                    }
                    is Result.Error -> result
                    is Result.Loading -> result
                }
            }.flowOn(dispatcherProvider.io).distinctUntilChanged()

        override fun getFilteredContent(
            filterOptions: FilterOptions,
            sortingOptions: SortingOptions,
        ): Flow<Result<List<FileItem>>> =
            getRootContent().map { result ->
                when (result) {
                    is Result.Success -> {
                        val filtered = result.data.filteredByOptions(filterOptions)
                        val sorted = filtered.sortedByOptions(sortingOptions)
                        Result.Success(sorted)
                    }
                    is Result.Error -> result
                    is Result.Loading -> result
                }
            }.flowOn(dispatcherProvider.io).distinctUntilChanged()

        override fun getContentByFileType(fileTypes: Set<FileType>): Flow<Result<List<FileItem>>> =
            getRootContent().map { result ->
                when (result) {
                    is Result.Success -> {
                        val filtered =
                            result.data.filter { item ->
                                when (item) {
                                    is FileItem.File -> {
                                        val extension = item.name.substringAfterLast('.', "").lowercase()
                                        val fileType = FileType.fromExtension(extension)
                                        fileTypes.contains(fileType)
                                    }
                                    is FileItem.Torrent -> {
                                        // For torrents, check if they contain files of the specified types
                                        item.files.any { file ->
                                            val extension = file.name.substringAfterLast('.', "").lowercase()
                                            val fileType = FileType.fromExtension(extension)
                                            fileTypes.contains(fileType)
                                        }
                                    }
                                    is FileItem.Folder -> true // Always include folders
                                }
                            }
                        Result.Success(filtered)
                    }
                    is Result.Error -> result
                    is Result.Loading -> result
                }
            }.flowOn(dispatcherProvider.io).distinctUntilChanged()

        override fun getContentByDateRange(
            startDate: Long,
            endDate: Long,
        ): Flow<Result<List<FileItem>>> =
            getRootContent().map { result ->
                when (result) {
                    is Result.Success -> {
                        val filtered =
                            result.data.filter { item ->
                                item.modifiedDate in startDate..endDate
                            }
                        Result.Success(filtered)
                    }
                    is Result.Error -> result
                    is Result.Loading -> result
                }
            }.flowOn(dispatcherProvider.io).distinctUntilChanged()

        override fun getContentBySizeRange(
            minSize: Long,
            maxSize: Long,
        ): Flow<Result<List<FileItem>>> =
            getRootContent().map { result ->
                when (result) {
                    is Result.Success -> {
                        val filtered =
                            result.data.filter { item ->
                                item.size in minSize..maxSize
                            }
                        Result.Success(filtered)
                    }
                    is Result.Error -> result
                    is Result.Loading -> result
                }
            }.flowOn(dispatcherProvider.io).distinctUntilChanged()

        override suspend fun refreshContent(): Result<Unit> =
            withContext(dispatcherProvider.io) {
                cacheMutex.withLock {
                    // Clear all caches to force refresh
                    invalidateAllCaches()

                    // Trigger content sync in the content repository
                    contentRepository.syncContent()
                }
            }

        override suspend fun pullToRefresh(): Result<Unit> =
            withContext(dispatcherProvider.io) {
                cacheMutex.withLock {
                    // Force clear all caches
                    invalidateAllCaches()

                    // Force refresh from server
                    try {
                        val result = contentRepository.syncContent()
                        when (result) {
                            is Result.Success -> {
                                // Trigger cache warming by fetching root content
                                getRootContent().collect { rootResult ->
                                    // Just consume the result to warm the cache
                                    if (rootResult is Result.Success) {
                                        // Cache is now warmed
                                    }
                                }
                                Result.Success(Unit)
                            }
                            is Result.Error -> result
                            is Result.Loading -> Result.Success(Unit) // Async operation, consider success
                        }
                    } catch (e: Exception) {
                        Result.Error(e)
                    }
                }
            }

        override suspend fun getItemDetails(itemId: String): Result<FileItem> =
            withContext(dispatcherProvider.io) {
                safeCall {
                    val response = apiService.getTorrentInfo(itemId)
                    if (response.isSuccessful) {
                        val torrentInfo = response.body()
                        if (torrentInfo != null) {
                            torrentInfo.toFileItem()
                        } else {
                            throw Exception("No torrent info in response")
                        }
                    } else {
                        throw Exception("Failed to fetch item details: ${response.code()}")
                    }
                }
            }

        override suspend fun isAuthenticated(): Boolean =
            withContext(dispatcherProvider.io) {
                try {
                    val response = apiService.getUserInfo()
                    response.isSuccessful
                } catch (e: Exception) {
                    false
                }
            }

        override suspend fun bulkDeleteItems(
            itemIds: Set<String>,
            onProgress: ((String, Float) -> Unit)?,
        ): Result<Unit> =
            withContext(dispatcherProvider.io) {
                safeCall {
                    val totalItems = itemIds.size
                    var completedItems = 0
                    val errors = mutableListOf<Exception>()

                    // Process items in batches to avoid overwhelming the API
                    itemIds.chunked(5).forEach { batch ->
                        batch.forEach { itemId ->
                            try {
                                onProgress?.invoke(itemId, completedItems.toFloat() / totalItems)

                                val result = deleteItems(setOf(itemId))
                                when (result) {
                                    is Result.Success -> {
                                        completedItems++
                                        onProgress?.invoke(itemId, completedItems.toFloat() / totalItems)
                                    }
                                    is Result.Error -> {
                                        errors.add(Exception(result.exception))
                                        onProgress?.invoke(itemId, completedItems.toFloat() / totalItems)
                                    }
                                    is Result.Loading -> {
                                        // Shouldn't happen in suspend function
                                    }
                                }
                            } catch (e: Exception) {
                                errors.add(e)
                            }
                        }
                    }

                    // Invalidate caches after bulk operations
                    invalidateAllCaches()

                    if (errors.isNotEmpty()) {
                        throw Exception("Bulk delete completed with ${errors.size} errors: ${errors.first().message}")
                    }

                    Unit
                }
            }

        override suspend fun bulkDownloadFiles(
            fileIds: Set<String>,
            onProgress: ((String, Float) -> Unit)?,
        ): Result<Unit> =
            withContext(dispatcherProvider.io) {
                safeCall {
                    val totalFiles = fileIds.size
                    var completedFiles = 0
                    val errors = mutableListOf<Exception>()

                    // Process files in batches
                    fileIds.chunked(5).forEach { batch ->
                        batch.forEach { fileId ->
                            try {
                                onProgress?.invoke(fileId, completedFiles.toFloat() / totalFiles)

                                val result = downloadFiles(setOf(fileId))
                                when (result) {
                                    is Result.Success -> {
                                        completedFiles++
                                        onProgress?.invoke(fileId, completedFiles.toFloat() / totalFiles)
                                    }
                                    is Result.Error -> {
                                        errors.add(Exception(result.exception))
                                        onProgress?.invoke(fileId, completedFiles.toFloat() / totalFiles)
                                    }
                                    is Result.Loading -> {
                                        // Shouldn't happen in suspend function
                                    }
                                }
                            } catch (e: Exception) {
                                errors.add(e)
                            }
                        }
                    }

                    // Invalidate caches after bulk operations
                    invalidateAllCaches()

                    if (errors.isNotEmpty()) {
                        throw Exception("Bulk download completed with ${errors.size} errors: ${errors.first().message}")
                    }

                    Unit
                }
            }

        override suspend fun cancelOperations(): Result<Unit> =
            withContext(dispatcherProvider.io) {
                safeCall {
                    // Cancel any ongoing operations
                    // This would typically involve canceling coroutines or API calls
                    // For now, we'll just return success
                    Unit
                }
            }

        override suspend fun getCacheInfo(): CacheInfo =
            withContext(dispatcherProvider.io) {
                val rootCacheData = fileBrowserCache.value
                val torrentCacheSize = torrentFilesCache.size
                val playbackCacheSize = playbackUrlCache.size

                CacheInfo(
                    totalSize = (rootCacheData.data.size + torrentCacheSize + playbackCacheSize).toLong(),
                    itemCount = rootCacheData.data.size + torrentCacheSize + playbackCacheSize,
                    lastUpdateTime = rootCacheData.timestamp,
                    expirationTime = rootCacheData.timestamp + cacheExpirationMs,
                    isExpired = !rootCacheData.isValid(cacheExpirationMs),
                )
            }

        override suspend fun clearCache(): Result<Unit> =
            withContext(dispatcherProvider.io) {
                safeCall {
                    cacheMutex.withLock {
                        invalidateAllCaches()
                        Unit
                    }
                }
            }

        // Helper methods for cache management
        private fun invalidateAllCaches() {
            fileBrowserCache.value = CachedData.empty()
            torrentFilesCache.clear()
            playbackUrlCache.clear()
        }

        private fun invalidateRelatedCaches(ids: List<String>) {
            // Invalidate main cache
            fileBrowserCache.value = CachedData.empty()

            // Remove specific entries from torrent files cache
            ids.forEach { id ->
                torrentFilesCache.remove(id)
                // Remove playback URL cache entries that start with this ID
                playbackUrlCache.keys.removeAll { key -> key.startsWith("${id}_") }
            }
        }

        private fun getMimeTypeFromExtension(extension: String): String? {
            return when (extension.lowercase()) {
                // Video
                "mp4" -> "video/mp4"
                "mkv" -> "video/x-matroska"
                "avi" -> "video/x-msvideo"
                "mov" -> "video/quicktime"
                "wmv" -> "video/x-ms-wmv"
                "flv" -> "video/x-flv"
                "webm" -> "video/webm"
                "m4v" -> "video/x-m4v"
                "mpg", "mpeg" -> "video/mpeg"

                // Audio
                "mp3" -> "audio/mpeg"
                "wav" -> "audio/wav"
                "flac" -> "audio/flac"
                "aac" -> "audio/aac"
                "ogg" -> "audio/ogg"
                "wma" -> "audio/x-ms-wma"
                "m4a" -> "audio/mp4"
                "opus" -> "audio/opus"

                // Documents
                "pdf" -> "application/pdf"
                "doc" -> "application/msword"
                "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                "txt" -> "text/plain"
                "odt" -> "application/vnd.oasis.opendocument.text"
                "rtf" -> "application/rtf"

                // Images
                "jpg", "jpeg" -> "image/jpeg"
                "png" -> "image/png"
                "gif" -> "image/gif"
                "bmp" -> "image/bmp"
                "svg" -> "image/svg+xml"
                "webp" -> "image/webp"

                // Archives
                "zip" -> "application/zip"
                "rar" -> "application/x-rar-compressed"
                "7z" -> "application/x-7z-compressed"
                "tar" -> "application/x-tar"
                "gz" -> "application/gzip"
                "bz2" -> "application/x-bzip2"

                // Subtitles
                "srt" -> "application/x-subrip"
                "ass", "ssa" -> "text/x-ssa"
                "vtt" -> "text/vtt"
                "sub" -> "text/x-microdvd"

                else -> null
            }
        }
    }

/**
 * Data class for cached values with expiration - specific to FileBrowser
 */
private data class CachedData<T>(
    val data: T,
    val timestamp: Long,
) {
    companion object {
        fun <T> empty(): CachedData<T> where T : Collection<*> {
            @Suppress("UNCHECKED_CAST")
            return CachedData(emptyList<Any>() as T, 0L)
        }
    }

    fun isValid(expirationMs: Long): Boolean {
        return System.currentTimeMillis() - timestamp < expirationMs
    }
}
