package com.rdwatch.androidtv.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rdwatch.androidtv.network.api.RealDebridApiService
import com.rdwatch.androidtv.repository.FileBrowserDataProcessor
import com.rdwatch.androidtv.repository.cache.FileBrowserCacheManager
import com.rdwatch.androidtv.ui.filebrowser.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

/**
 * Enhanced PagingSource for Real-Debrid account files with advanced filtering,
 * sorting, caching, and performance optimizations
 */
class EnhancedAccountFilesPagingSource @Inject constructor(
    private val apiService: RealDebridApiService,
    private val dataProcessor: FileBrowserDataProcessor,
    private val cacheManager: FileBrowserCacheManager,
    private val sortOption: FileEnhancedSortOption,
    private val filter: FileFilterCriteria
) : PagingSource<Int, AccountFileItem>() {

    companion object {
        private const val STARTING_PAGE_INDEX = 0
        private const val DEFAULT_PAGE_SIZE = 20
        private const val PREFETCH_SIZE = 40 // Prefetch more items for smooth scrolling
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AccountFileItem> {
        return try {
            val pageIndex = params.key ?: STARTING_PAGE_INDEX
            val requestedLoadSize = params.loadSize.coerceAtLeast(DEFAULT_PAGE_SIZE)
            
            withContext(Dispatchers.IO) {
                // Try to get data from cache first
                val cachedFiles = getCachedFiles()
                
                if (cachedFiles.isNotEmpty()) {
                    // Use cached data with client-side pagination
                    val result = processLocalPagination(cachedFiles, pageIndex, requestedLoadSize)
                    
                    // Trigger background refresh if cache might be stale
                    if (shouldRefreshCache()) {
                        refreshCacheInBackground()
                    }
                    
                    result
                } else {
                    // Fetch fresh data from API
                    fetchAndCacheData(pageIndex, requestedLoadSize)
                }
            }

        } catch (exception: IOException) {
            LoadResult.Error(exception)
        } catch (exception: HttpException) {
            LoadResult.Error(exception)
        } catch (exception: Exception) {
            LoadResult.Error(exception)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, AccountFileItem>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }

    // Private helper methods

    private suspend fun getCachedFiles(): List<AccountFileItem> {
        return try {
            cacheManager.getAccountFiles(filter).first()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private suspend fun processLocalPagination(
        allFiles: List<AccountFileItem>,
        pageIndex: Int,
        loadSize: Int
    ): LoadResult<Int, AccountFileItem> {
        
        // Process files with sorting and filtering
        val processedResult = dataProcessor.processFiles(
            files = allFiles,
            sortOption = sortOption,
            filter = filter,
            limit = null // Don't limit here, we'll handle pagination
        )
        
        val sortedAndFilteredFiles = processedResult.files
        
        // Apply pagination
        val startIndex = pageIndex * DEFAULT_PAGE_SIZE
        val endIndex = (startIndex + loadSize).coerceAtMost(sortedAndFilteredFiles.size)
        
        val pageData = if (startIndex < sortedAndFilteredFiles.size) {
            sortedAndFilteredFiles.subList(startIndex, endIndex)
        } else {
            emptyList()
        }
        
        return LoadResult.Page(
            data = pageData,
            prevKey = if (pageIndex == STARTING_PAGE_INDEX) null else pageIndex - 1,
            nextKey = if (endIndex >= sortedAndFilteredFiles.size) null else pageIndex + 1
        )
    }

    private suspend fun fetchAndCacheData(
        pageIndex: Int,
        loadSize: Int
    ): LoadResult<Int, AccountFileItem> = coroutineScope {
        
        // Calculate API pagination parameters
        val apiOffset = pageIndex * DEFAULT_PAGE_SIZE
        val apiLimit = (loadSize * 2).coerceAtLeast(PREFETCH_SIZE) // Fetch more for caching
        
        // Fetch both torrents and downloads in parallel
        val torrentsDeferred = async {
            try {
                val response = apiService.getTorrents(
                    offset = apiOffset,
                    limit = apiLimit
                )
                if (response.isSuccessful) {
                    response.body()?.map { mapTorrentToAccountFile(it) } ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList<AccountFileItem>()
            }
        }

        val downloadsDeferred = async {
            try {
                val response = apiService.getDownloads(
                    offset = apiOffset,
                    limit = apiLimit
                )
                if (response.isSuccessful) {
                    response.body()?.map { mapDownloadToAccountFile(it as Map<String, Any>) } ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList<AccountFileItem>()
            }
        }

        val torrents = torrentsDeferred.await()
        val downloads = downloadsDeferred.await()
        
        val allFetchedFiles = (torrents + downloads).sortedByDescending { it.dateAdded }
        
        // Cache the fetched data for future use
        if (allFetchedFiles.isNotEmpty()) {
            cacheManager.cacheAccountFiles(allFetchedFiles)
        }
        
        // Process with sorting and filtering
        val processedResult = dataProcessor.processFiles(
            files = allFetchedFiles,
            sortOption = sortOption,
            filter = filter,
            limit = loadSize
        )
        
        val pageData = processedResult.files
        
        LoadResult.Page(
            data = pageData,
            prevKey = if (pageIndex == STARTING_PAGE_INDEX) null else pageIndex - 1,
            nextKey = if (pageData.size < loadSize) null else pageIndex + 1
        )
    }

    private suspend fun shouldRefreshCache(): Boolean {
        // Simple heuristic: refresh cache if it's older than 5 minutes
        // This could be made more sophisticated based on user activity
        return false // For now, rely on explicit refresh
    }

    private suspend fun refreshCacheInBackground() {
        // Trigger a background refresh of the cache
        // This runs in the background and doesn't block the UI
        try {
            val freshData = fetchAllAccountFiles()
            if (freshData.isNotEmpty()) {
                cacheManager.cacheAccountFiles(freshData)
            }
        } catch (e: Exception) {
            // Silently handle background refresh failures
        }
    }

    private suspend fun fetchAllAccountFiles(): List<AccountFileItem> = coroutineScope {
        val torrentsDeferred = async {
            try {
                val response = apiService.getTorrents(limit = 100)
                if (response.isSuccessful) {
                    response.body()?.map { mapTorrentToAccountFile(it) } ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList<AccountFileItem>()
            }
        }

        val downloadsDeferred = async {
            try {
                val response = apiService.getDownloads(limit = 100)
                if (response.isSuccessful) {
                    response.body()?.map { mapDownloadToAccountFile(it as Map<String, Any>) } ?: emptyList()
                } else {
                    emptyList()
                }
            } catch (e: Exception) {
                emptyList<AccountFileItem>()
            }
        }

        val torrents = torrentsDeferred.await()
        val downloads = downloadsDeferred.await()

        (torrents + downloads).sortedByDescending { it.dateAdded }
    }

    private fun mapTorrentToAccountFile(torrent: com.rdwatch.androidtv.network.models.TorrentInfo): AccountFileItem {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }

        val addedDate = try {
            dateFormat.parse(torrent.added) ?: Date()
        } catch (e: Exception) {
            Date()
        }

        return AccountFileItem(
            id = torrent.id,
            filename = torrent.filename,
            filesize = torrent.bytes,
            source = FileSource.TORRENT,
            mimeType = null, // Not available for torrents
            downloadUrl = null, // Would need to unrestrict links
            streamUrl = null,
            host = torrent.host,
            dateAdded = addedDate,
            isStreamable = torrent.status == "downloaded",
            parentTorrentId = torrent.id,
            parentTorrentName = torrent.filename,
            torrentProgress = torrent.progress,
            torrentStatus = torrent.status
        )
    }

    private fun mapDownloadToAccountFile(download: Map<String, Any>): AccountFileItem {
        val id = download["id"] as? String ?: ""
        val filename = download["filename"] as? String ?: "Unknown"
        val filesize = (download["filesize"] as? Number)?.toLong() ?: 0L
        val mimeType = download["mime"] as? String ?: ""
        val downloadUrl = download["download"] as? String ?: ""
        val streamUrl = if (download["streamable"] as? Boolean == true) download["link"] as? String else null
        val host = download["host"] as? String ?: ""
        val isStreamable = download["streamable"] as? Boolean ?: false

        val dateAdded = try {
            val generated = download["generated"] as? String
            if (generated != null) {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                    timeZone = TimeZone.getTimeZone("UTC")
                }.parse(generated) ?: Date()
            } else Date()
        } catch (e: Exception) {
            Date()
        }

        return AccountFileItem(
            id = id,
            filename = filename,
            filesize = filesize,
            source = FileSource.DOWNLOAD,
            mimeType = mimeType,
            downloadUrl = downloadUrl,
            streamUrl = streamUrl,
            host = host,
            dateAdded = dateAdded,
            isStreamable = isStreamable
        )
    }
}

/**
 * Factory for creating EnhancedAccountFilesPagingSource instances
 */
class EnhancedAccountFilesPagingSourceFactory @Inject constructor(
    private val apiService: RealDebridApiService,
    private val dataProcessor: FileBrowserDataProcessor,
    private val cacheManager: FileBrowserCacheManager
) {
    fun create(
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        filter: FileFilterCriteria = FileFilterCriteria()
    ): EnhancedAccountFilesPagingSource {
        return EnhancedAccountFilesPagingSource(
            apiService = apiService,
            dataProcessor = dataProcessor,
            cacheManager = cacheManager,
            sortOption = sortOption,
            filter = filter
        )
    }
}