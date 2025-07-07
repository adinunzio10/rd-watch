package com.rdwatch.androidtv.test.fake

import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.filebrowser.models.*
import com.rdwatch.androidtv.ui.filebrowser.repository.CacheInfo
import com.rdwatch.androidtv.ui.filebrowser.repository.FileBrowserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Fake implementation of FileBrowserRepository for testing.
 * Provides predictable test data without network dependencies.
 */
@Singleton
class FakeFileBrowserRepository @Inject constructor() : FileBrowserRepository {

    private val testFiles = mutableListOf<FileItem>()
    private val testTorrents = mutableListOf<FileItem.Torrent>()
    private val testPlaybackUrls = mutableMapOf<String, String>()
    private val testTorrentFiles = mutableMapOf<String, List<FileItem.File>>()
    
    private var shouldReturnError = false
    private var shouldReturnEmpty = false
    private var isAuthenticated = true
    private var networkDelay = 0L
    private var loadingState = false
    private val deletedItems = mutableSetOf<String>()
    private var searchResults = emptyList<FileItem>()
    private var refreshCount = 0
    private var operationProgress = mutableMapOf<String, Float>()
    private var shouldCancelOperations = false
    private var cacheInfo = CacheInfo(0L, 0, 0L, 0L, false)

    init {
        setupTestData()
    }

    private fun setupTestData() {
        // Clear existing data
        testFiles.clear()
        testTorrents.clear()
        testPlaybackUrls.clear()
        testTorrentFiles.clear()
        
        // Test video files
        val testVideoFiles = listOf(
            FileItem.File(
                id = "file1",
                name = "Test Movie 1.mp4",
                size = 1024L * 1024L * 1024L, // 1GB
                modifiedDate = System.currentTimeMillis() - 86400000L, // 1 day ago
                mimeType = "video/mp4",
                downloadUrl = "https://example.com/download/file1",
                streamUrl = "https://example.com/stream/file1",
                isPlayable = true,
                status = FileStatus.READY
            ),
            FileItem.File(
                id = "file2",
                name = "Test Movie 2.mkv",
                size = 2048L * 1024L * 1024L, // 2GB
                modifiedDate = System.currentTimeMillis() - 172800000L, // 2 days ago
                mimeType = "video/x-matroska",
                downloadUrl = "https://example.com/download/file2",
                streamUrl = "https://example.com/stream/file2",
                isPlayable = true,
                status = FileStatus.READY
            ),
            FileItem.File(
                id = "file3",
                name = "Test Movie 3.avi",
                size = 700L * 1024L * 1024L, // 700MB
                modifiedDate = System.currentTimeMillis() - 259200000L, // 3 days ago
                mimeType = "video/x-msvideo",
                downloadUrl = "https://example.com/download/file3",
                streamUrl = "https://example.com/stream/file3",
                isPlayable = true,
                status = FileStatus.DOWNLOADING,
                progress = 0.75f
            )
        )
        
        // Test audio files
        val testAudioFiles = listOf(
            FileItem.File(
                id = "audio1",
                name = "Test Music 1.mp3",
                size = 5L * 1024L * 1024L, // 5MB
                modifiedDate = System.currentTimeMillis() - 86400000L,
                mimeType = "audio/mpeg",
                downloadUrl = "https://example.com/download/audio1",
                streamUrl = "https://example.com/stream/audio1",
                isPlayable = true,
                status = FileStatus.READY
            ),
            FileItem.File(
                id = "audio2",
                name = "Test Music 2.flac",
                size = 25L * 1024L * 1024L, // 25MB
                modifiedDate = System.currentTimeMillis() - 172800000L,
                mimeType = "audio/flac",
                downloadUrl = "https://example.com/download/audio2",
                streamUrl = "https://example.com/stream/audio2",
                isPlayable = true,
                status = FileStatus.READY
            )
        )
        
        // Test subtitle files
        val testSubtitleFiles = listOf(
            FileItem.File(
                id = "sub1",
                name = "Test Movie 1.srt",
                size = 50L * 1024L, // 50KB
                modifiedDate = System.currentTimeMillis() - 86400000L,
                mimeType = "application/x-subrip",
                downloadUrl = "https://example.com/download/sub1",
                streamUrl = null,
                isPlayable = false,
                status = FileStatus.READY
            )
        )
        
        // Test other files
        val testOtherFiles = listOf(
            FileItem.File(
                id = "doc1",
                name = "Test Document.pdf",
                size = 1024L * 1024L, // 1MB
                modifiedDate = System.currentTimeMillis() - 86400000L,
                mimeType = "application/pdf",
                downloadUrl = "https://example.com/download/doc1",
                streamUrl = null,
                isPlayable = false,
                status = FileStatus.READY
            ),
            FileItem.File(
                id = "zip1",
                name = "Test Archive.zip",
                size = 10L * 1024L * 1024L, // 10MB
                modifiedDate = System.currentTimeMillis() - 172800000L,
                mimeType = "application/zip",
                downloadUrl = "https://example.com/download/zip1",
                streamUrl = null,
                isPlayable = false,
                status = FileStatus.ERROR
            )
        )
        
        // Add all test files
        testFiles.addAll(testVideoFiles)
        testFiles.addAll(testAudioFiles)
        testFiles.addAll(testSubtitleFiles)
        testFiles.addAll(testOtherFiles)
        
        // Test torrents
        val testTorrent1 = FileItem.Torrent(
            id = "torrent1",
            name = "Test Torrent 1",
            size = 5L * 1024L * 1024L * 1024L, // 5GB
            modifiedDate = System.currentTimeMillis() - 86400000L,
            hash = "abc123def456",
            progress = 1.0f,
            status = TorrentStatus.DOWNLOADED,
            seeders = 5,
            speed = 0L,
            files = testVideoFiles.take(2)
        )
        
        val testTorrent2 = FileItem.Torrent(
            id = "torrent2",
            name = "Test Torrent 2",
            size = 3L * 1024L * 1024L * 1024L, // 3GB
            modifiedDate = System.currentTimeMillis() - 172800000L,
            hash = "def456ghi789",
            progress = 0.5f,
            status = TorrentStatus.DOWNLOADING,
            seeders = 10,
            speed = 1024L * 1024L, // 1 MB/s
            files = listOf(testVideoFiles[2])
        )
        
        testTorrents.add(testTorrent1)
        testTorrents.add(testTorrent2)
        
        // Setup torrent files mapping
        testTorrentFiles["torrent1"] = testVideoFiles.take(2)
        testTorrentFiles["torrent2"] = listOf(testVideoFiles[2])
        
        // Setup playback URLs
        testFiles.forEach { file ->
            if (file is FileItem.File && file.isPlayable) {
                testPlaybackUrls[file.id] = "https://example.com/unrestricted/${file.id}"
            }
        }
        
        // Setup cache info
        cacheInfo = CacheInfo(
            totalSize = testFiles.sumOf { it.size },
            itemCount = testFiles.size + testTorrents.size,
            lastUpdateTime = System.currentTimeMillis(),
            expirationTime = System.currentTimeMillis() + 3600000L, // 1 hour
            isExpired = false
        )
    }

    override fun getRootContent(): Flow<Result<List<FileItem>>> {
        return flow {
            if (loadingState) {
                emit(Result.Loading)
            }
            
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> {
                    emit(Result.Error(Exception("Network error")))
                }
                shouldReturnEmpty -> {
                    emit(Result.Success(emptyList()))
                }
                else -> {
                    val allItems = (testFiles + testTorrents)
                        .filterNot { deletedItems.contains(it.id) }
                    emit(Result.Success(allItems))
                }
            }
        }
    }

    override suspend fun getTorrentFiles(torrentId: String): Result<List<FileItem.File>> {
        return try {
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> Result.Error(Exception("Failed to get torrent files"))
                shouldReturnEmpty -> Result.Success(emptyList())
                else -> {
                    val files = testTorrentFiles[torrentId] ?: emptyList()
                    Result.Success(files)
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getPlaybackUrl(fileId: String, torrentId: String?): Result<String> {
        return try {
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> Result.Error(Exception("Failed to get playback URL"))
                testPlaybackUrls.containsKey(fileId) -> Result.Success(testPlaybackUrls[fileId]!!)
                else -> Result.Error(Exception("File not found or not playable"))
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun deleteItems(itemIds: Set<String>): Result<Unit> {
        return try {
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> Result.Error(Exception("Failed to delete items"))
                else -> {
                    deletedItems.addAll(itemIds)
                    Result.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun downloadFiles(fileIds: Set<String>): Result<Unit> {
        return try {
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> Result.Error(Exception("Failed to start download"))
                else -> Result.Success(Unit)
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override fun searchContent(query: String): Flow<Result<List<FileItem>>> {
        return flow {
            if (loadingState) {
                emit(Result.Loading)
            }
            
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> {
                    emit(Result.Error(Exception("Search failed")))
                }
                query.isBlank() -> {
                    emit(Result.Success(emptyList()))
                }
                else -> {
                    val results = (testFiles + testTorrents)
                        .filterNot { deletedItems.contains(it.id) }
                        .filter { it.name.contains(query, ignoreCase = true) }
                    emit(Result.Success(results))
                }
            }
        }
    }

    override fun getFilteredContent(
        filterOptions: FilterOptions,
        sortingOptions: SortingOptions
    ): Flow<Result<List<FileItem>>> {
        return flow {
            if (loadingState) {
                emit(Result.Loading)
            }
            
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> {
                    emit(Result.Error(Exception("Failed to get filtered content")))
                }
                else -> {
                    var filteredItems = (testFiles + testTorrents)
                        .filterNot { deletedItems.contains(it.id) }
                    
                    // Apply filters
                    if (filterOptions.showOnlyPlayable) {
                        filteredItems = filteredItems.filter { item ->
                            when (item) {
                                is FileItem.File -> item.isPlayable
                                is FileItem.Torrent -> item.files.any { it.isPlayable }
                                else -> false
                            }
                        }
                    }
                    
                    if (filterOptions.showOnlyDownloaded) {
                        filteredItems = filteredItems.filter { item ->
                            when (item) {
                                is FileItem.File -> item.status == FileStatus.READY
                                is FileItem.Torrent -> item.status == TorrentStatus.DOWNLOADED
                                else -> false
                            }
                        }
                    }
                    
                    if (filterOptions.fileTypeFilter.isNotEmpty()) {
                        filteredItems = filteredItems.filter { item ->
                            when (item) {
                                is FileItem.File -> {
                                    val extension = item.name.substringAfterLast('.', "")
                                    val fileType = FileType.fromExtension(extension)
                                    filterOptions.fileTypeFilter.contains(fileType)
                                }
                                else -> true
                            }
                        }
                    }
                    
                    if (filterOptions.statusFilter.isNotEmpty()) {
                        filteredItems = filteredItems.filter { item ->
                            when (item) {
                                is FileItem.File -> filterOptions.statusFilter.contains(item.status)
                                else -> true
                            }
                        }
                    }
                    
                    if (filterOptions.searchQuery.isNotBlank()) {
                        filteredItems = filteredItems.filter { item ->
                            item.name.contains(filterOptions.searchQuery, ignoreCase = true)
                        }
                    }
                    
                    // Apply sorting
                    filteredItems = when (sortingOptions.sortBy) {
                        SortBy.NAME -> filteredItems.sortedBy { it.name }
                        SortBy.SIZE -> filteredItems.sortedBy { it.size }
                        SortBy.DATE -> filteredItems.sortedBy { it.modifiedDate }
                        SortBy.TYPE -> filteredItems.sortedBy { item ->
                            when (item) {
                                is FileItem.Folder -> "1_folder"
                                is FileItem.Torrent -> "2_torrent"
                                is FileItem.File -> "3_${item.name.substringAfterLast('.', "")}"
                            }
                        }
                        SortBy.STATUS -> filteredItems.sortedBy { item ->
                            when (item) {
                                is FileItem.File -> item.status.ordinal
                                is FileItem.Torrent -> item.status.ordinal
                                else -> 0
                            }
                        }
                    }
                    
                    if (sortingOptions.sortOrder == SortOrder.DESCENDING) {
                        filteredItems = filteredItems.reversed()
                    }
                    
                    emit(Result.Success(filteredItems))
                }
            }
        }
    }

    override fun getContentByFileType(fileTypes: Set<FileType>): Flow<Result<List<FileItem>>> {
        return flow {
            if (loadingState) {
                emit(Result.Loading)
            }
            
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> {
                    emit(Result.Error(Exception("Failed to get content by file type")))
                }
                else -> {
                    val filteredItems = testFiles
                        .filterNot { deletedItems.contains(it.id) }
                        .filter { item ->
                            when (item) {
                                is FileItem.File -> {
                                    val extension = item.name.substringAfterLast('.', "")
                                    val fileType = FileType.fromExtension(extension)
                                    fileTypes.contains(fileType)
                                }
                                else -> false
                            }
                        }
                    emit(Result.Success(filteredItems))
                }
            }
        }
    }

    override fun getContentByDateRange(startDate: Long, endDate: Long): Flow<Result<List<FileItem>>> {
        return flow {
            if (loadingState) {
                emit(Result.Loading)
            }
            
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> {
                    emit(Result.Error(Exception("Failed to get content by date range")))
                }
                else -> {
                    val filteredItems = (testFiles + testTorrents)
                        .filterNot { deletedItems.contains(it.id) }
                        .filter { it.modifiedDate in startDate..endDate }
                    emit(Result.Success(filteredItems))
                }
            }
        }
    }

    override fun getContentBySizeRange(minSize: Long, maxSize: Long): Flow<Result<List<FileItem>>> {
        return flow {
            if (loadingState) {
                emit(Result.Loading)
            }
            
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> {
                    emit(Result.Error(Exception("Failed to get content by size range")))
                }
                else -> {
                    val filteredItems = (testFiles + testTorrents)
                        .filterNot { deletedItems.contains(it.id) }
                        .filter { it.size in minSize..maxSize }
                    emit(Result.Success(filteredItems))
                }
            }
        }
    }

    override suspend fun refreshContent(): Result<Unit> {
        return try {
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> Result.Error(Exception("Failed to refresh content"))
                else -> {
                    refreshCount++
                    // Simulate refresh by updating modified dates
                    testFiles.forEachIndexed { index, file ->
                        if (file is FileItem.File) {
                            testFiles[index] = file.copy(modifiedDate = System.currentTimeMillis())
                        }
                    }
                    Result.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun pullToRefresh(): Result<Unit> {
        return try {
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> Result.Error(Exception("Failed to pull to refresh"))
                else -> {
                    refreshCount++
                    deletedItems.clear()
                    setupTestData()
                    Result.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getItemDetails(itemId: String): Result<FileItem> {
        return try {
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> Result.Error(Exception("Failed to get item details"))
                else -> {
                    val item = (testFiles + testTorrents)
                        .find { it.id == itemId && !deletedItems.contains(it.id) }
                    if (item != null) {
                        Result.Success(item)
                    } else {
                        Result.Error(Exception("Item not found"))
                    }
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun isAuthenticated(): Boolean {
        return isAuthenticated
    }

    override suspend fun bulkDeleteItems(
        itemIds: Set<String>,
        onProgress: ((String, Float) -> Unit)?
    ): Result<Unit> {
        return try {
            if (shouldCancelOperations) {
                return Result.Error(Exception("Operation cancelled"))
            }
            
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> Result.Error(Exception("Bulk delete failed"))
                else -> {
                    itemIds.forEachIndexed { index, itemId ->
                        val progress = (index + 1).toFloat() / itemIds.size
                        operationProgress[itemId] = progress
                        onProgress?.invoke(itemId, progress)
                        
                        if (shouldCancelOperations) {
                            return Result.Error(Exception("Operation cancelled"))
                        }
                        
                        // Simulate some work
                        delay(100)
                    }
                    
                    deletedItems.addAll(itemIds)
                    Result.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun bulkDownloadFiles(
        fileIds: Set<String>,
        onProgress: ((String, Float) -> Unit)?
    ): Result<Unit> {
        return try {
            if (shouldCancelOperations) {
                return Result.Error(Exception("Operation cancelled"))
            }
            
            if (networkDelay > 0) {
                delay(networkDelay)
            }
            
            when {
                shouldReturnError -> Result.Error(Exception("Bulk download failed"))
                else -> {
                    fileIds.forEachIndexed { index, fileId ->
                        val progress = (index + 1).toFloat() / fileIds.size
                        operationProgress[fileId] = progress
                        onProgress?.invoke(fileId, progress)
                        
                        if (shouldCancelOperations) {
                            return Result.Error(Exception("Operation cancelled"))
                        }
                        
                        // Simulate some work
                        delay(200)
                    }
                    
                    Result.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun cancelOperations(): Result<Unit> {
        return try {
            shouldCancelOperations = true
            operationProgress.clear()
            Result.Success(Unit)
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    override suspend fun getCacheInfo(): CacheInfo {
        return cacheInfo
    }

    override suspend fun clearCache(): Result<Unit> {
        return try {
            when {
                shouldReturnError -> Result.Error(Exception("Failed to clear cache"))
                else -> {
                    cacheInfo = CacheInfo(0L, 0, 0L, 0L, true)
                    Result.Success(Unit)
                }
            }
        } catch (e: Exception) {
            Result.Error(e)
        }
    }

    // Test utilities
    fun setReturnError(returnError: Boolean) {
        shouldReturnError = returnError
    }

    fun setReturnEmpty(returnEmpty: Boolean) {
        shouldReturnEmpty = returnEmpty
    }

    fun setAuthenticated(authenticated: Boolean) {
        isAuthenticated = authenticated
    }

    fun setNetworkDelay(delayMs: Long) {
        networkDelay = delayMs
    }

    fun setLoadingState(loading: Boolean) {
        loadingState = loading
    }

    fun getTestFiles(): List<FileItem> = testFiles.toList()

    fun getTestTorrents(): List<FileItem.Torrent> = testTorrents.toList()

    fun getDeletedItems(): Set<String> = deletedItems.toSet()

    fun getRefreshCount(): Int = refreshCount

    fun addTestFile(file: FileItem) {
        testFiles.add(file)
    }

    fun addTestTorrent(torrent: FileItem.Torrent) {
        testTorrents.add(torrent)
    }

    fun clearTestData() {
        testFiles.clear()
        testTorrents.clear()
        testPlaybackUrls.clear()
        testTorrentFiles.clear()
        deletedItems.clear()
    }

    fun resetTestState() {
        shouldReturnError = false
        shouldReturnEmpty = false
        isAuthenticated = true
        networkDelay = 0L
        loadingState = false
        deletedItems.clear()
        refreshCount = 0
        operationProgress.clear()
        shouldCancelOperations = false
        setupTestData()
    }

    fun getOperationProgress(): Map<String, Float> = operationProgress.toMap()

    fun setCancelOperations(cancel: Boolean) {
        shouldCancelOperations = cancel
    }

    fun updateCacheInfo(cacheInfo: CacheInfo) {
        this.cacheInfo = cacheInfo
    }
}