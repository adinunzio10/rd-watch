package com.rdwatch.androidtv.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rdwatch.androidtv.data.entities.TorrentEntity
import com.rdwatch.androidtv.network.api.RealDebridApiService
import com.rdwatch.androidtv.network.models.TorrentInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException
import java.util.*
import javax.inject.Inject

/**
 * PagingSource for Real-Debrid torrents list with pagination support
 * 
 * Since Real-Debrid API supports offset/limit pagination, we implement
 * manual pagination with configurable page size.
 */
class RealDebridPagingSource @Inject constructor(
    private val apiService: RealDebridApiService,
    private val filter: String? = null
) : PagingSource<Int, TorrentEntity>() {

    companion object {
        private const val STARTING_PAGE_INDEX = 0
        private const val PAGE_SIZE = 20
    }

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, TorrentEntity> {
        return try {
            val pageIndex = params.key ?: STARTING_PAGE_INDEX
            val offset = pageIndex * PAGE_SIZE
            
            withContext(Dispatchers.IO) {
                val response = apiService.getTorrents(
                    offset = offset,
                    limit = PAGE_SIZE,
                    filter = filter
                )

                if (response.isSuccessful) {
                    val torrents = response.body() ?: emptyList()
                    val torrentEntities = torrents.map { it.toTorrentEntity() }

                    LoadResult.Page(
                        data = torrentEntities,
                        prevKey = if (pageIndex == STARTING_PAGE_INDEX) null else pageIndex - 1,
                        nextKey = if (torrentEntities.isEmpty() || torrentEntities.size < PAGE_SIZE) {
                            null
                        } else {
                            pageIndex + 1
                        }
                    )
                } else {
                    LoadResult.Error(HttpException(response))
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

    override fun getRefreshKey(state: PagingState<Int, TorrentEntity>): Int? {
        return state.anchorPosition?.let { anchorPosition ->
            val anchorPage = state.closestPageToPosition(anchorPosition)
            anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
        }
    }
}

/**
 * Factory for creating RealDebridPagingSource instances with different filters
 */
class RealDebridPagingSourceFactory @Inject constructor(
    private val apiService: RealDebridApiService
) {
    fun create(filter: String? = null): RealDebridPagingSource {
        return RealDebridPagingSource(apiService, filter)
    }
}

/**
 * Extension function to convert TorrentInfo to TorrentEntity for paging
 */
private fun TorrentInfo.toTorrentEntity(): TorrentEntity {
    // Parse date string to Date
    val addedDate = try {
        java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.parse(added) ?: Date()
    } catch (e: Exception) {
        Date()
    }
    
    val endedDate = ended?.let { dateStr ->
        try {
            java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }.parse(dateStr)
        } catch (e: Exception) {
            null
        }
    }

    return TorrentEntity(
        id = id,
        hash = hash,
        filename = filename,
        bytes = bytes,
        links = links,
        split = split,
        progress = progress,
        status = status,
        added = addedDate,
        speed = speed,
        seeders = seeders,
        ended = endedDate
    )
}