package com.rdwatch.androidtv.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rdwatch.androidtv.network.api.RealDebridApiService
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
 * PagingSource for Real-Debrid account files (both downloads and torrents)
 * with filtering support
 */
class AccountFilesPagingSource @Inject constructor(
    private val apiService: RealDebridApiService,
    private val filter: FileFilterCriteria
) : PagingSource<Int, AccountFileItem>() {

    companion object {
        private const val STARTING_PAGE_INDEX = 0
        private const val PAGE_SIZE = 20
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AccountFileItem> {
        return try {
            val pageIndex = params.key ?: STARTING_PAGE_INDEX
            val offset = pageIndex * PAGE_SIZE

            withContext(Dispatchers.IO) {
                val allFiles = fetchAccountFiles(offset, PAGE_SIZE)
                val filteredFiles = applyFilter(allFiles, filter)

                LoadResult.Page(
                    data = filteredFiles,
                    prevKey = if (pageIndex == STARTING_PAGE_INDEX) null else pageIndex - 1,
                    nextKey = if (filteredFiles.isEmpty() || filteredFiles.size < PAGE_SIZE) {
                        null
                    } else {
                        pageIndex + 1
                    }
                )
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

    private suspend fun fetchAccountFiles(offset: Int, limit: Int): List<AccountFileItem> = coroutineScope {
        // Fetch both torrents and downloads in parallel
        val torrentsDeferred = async {
            try {
                val response = apiService.getTorrents(
                    offset = offset,
                    limit = limit
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
                    offset = offset,
                    limit = limit
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

        // Combine and sort by date (newest first)
        (torrents + downloads).sortedByDescending { it.dateAdded }
    }

    private fun applyFilter(files: List<AccountFileItem>, filter: FileFilterCriteria): List<AccountFileItem> {
        return files.filter { file ->
            // Search query filter
            if (filter.searchQuery.isNotBlank()) {
                val query = filter.searchQuery.lowercase()
                if (!file.filename.lowercase().contains(query)) {
                    return@filter false
                }
            }

            // File type filter
            if (filter.fileTypes.isNotEmpty() && !filter.fileTypes.contains(file.fileTypeCategory)) {
                return@filter false
            }

            // Source filter
            if (filter.sources.isNotEmpty() && !filter.sources.contains(file.source)) {
                return@filter false
            }

            // Availability status filter
            if (filter.availabilityStatus.isNotEmpty() && !filter.availabilityStatus.contains(file.availabilityStatus)) {
                return@filter false
            }

            // File size filters
            filter.minFileSize?.let { minSize ->
                if (file.filesize < minSize) return@filter false
            }

            filter.maxFileSize?.let { maxSize ->
                if (file.filesize > maxSize) return@filter false
            }

            // Date filters
            filter.dateAfter?.let { afterDate ->
                if (file.dateAdded.before(afterDate)) return@filter false
            }

            filter.dateBefore?.let { beforeDate ->
                if (file.dateAdded.after(beforeDate)) return@filter false
            }

            // Streamable filter
            if (filter.isStreamableOnly && !file.isStreamable) {
                return@filter false
            }

            true
        }
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
 * Factory for creating AccountFilesPagingSource instances with different filters
 */
class AccountFilesPagingSourceFactory @Inject constructor(
    private val apiService: RealDebridApiService
) {
    fun create(filter: FileFilterCriteria = FileFilterCriteria()): AccountFilesPagingSource {
        return AccountFilesPagingSource(apiService, filter)
    }
}