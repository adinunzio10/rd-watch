package com.rdwatch.androidtv.data.paging

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.rdwatch.androidtv.repository.FileBrowserDataProcessor
import com.rdwatch.androidtv.repository.cache.FileBrowserCacheManager
import com.rdwatch.androidtv.ui.filebrowser.models.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

/**
 * Specialized PagingSource for search operations with real-time filtering
 * and ranking-based pagination
 */
class SearchPagingSource @Inject constructor(
    private val dataProcessor: FileBrowserDataProcessor,
    private val cacheManager: FileBrowserCacheManager,
    private val searchQuery: String,
    private val additionalFilter: FileFilterCriteria,
    private val sortOption: FileEnhancedSortOption,
    private val useRanking: Boolean = true
) : PagingSource<Int, AccountFileItem>() {

    companion object {
        private const val STARTING_PAGE_INDEX = 0
        private const val SEARCH_PAGE_SIZE = 15
        private const val MIN_QUERY_LENGTH = 2
    }

    private var cachedSearchResults: List<AccountFileItem>? = null
    private var lastSearchQuery: String = ""

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, AccountFileItem> {
        return try {
            val pageIndex = params.key ?: STARTING_PAGE_INDEX
            val requestedLoadSize = params.loadSize.coerceAtLeast(SEARCH_PAGE_SIZE)
            
            withContext(Dispatchers.IO) {
                // Early return for empty or too short query
                if (searchQuery.isBlank() || searchQuery.length < MIN_QUERY_LENGTH) {
                    return@withContext LoadResult.Page(
                        data = emptyList(),
                        prevKey = null,
                        nextKey = null
                    )
                }
                
                // Get search results (cached or fresh)
                val searchResults = getSearchResults()
                
                // Apply pagination
                val startIndex = pageIndex * SEARCH_PAGE_SIZE
                val endIndex = (startIndex + requestedLoadSize).coerceAtMost(searchResults.size)
                
                val pageData = if (startIndex < searchResults.size) {
                    searchResults.subList(startIndex, endIndex)
                } else {
                    emptyList()
                }
                
                LoadResult.Page(
                    data = pageData,
                    prevKey = if (pageIndex == STARTING_PAGE_INDEX) null else pageIndex - 1,
                    nextKey = if (endIndex >= searchResults.size) null else pageIndex + 1
                )
            }

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

    private suspend fun getSearchResults(): List<AccountFileItem> {
        // Use cached results if query hasn't changed
        if (cachedSearchResults != null && lastSearchQuery == searchQuery) {
            return cachedSearchResults!!
        }
        
        // Get all files from cache
        val allFiles = try {
            cacheManager.getAccountFiles().first()
        } catch (e: Exception) {
            emptyList()
        }
        
        if (allFiles.isEmpty()) {
            return emptyList()
        }
        
        // Perform search with ranking
        val searchResults = dataProcessor.searchFiles(
            files = allFiles,
            query = searchQuery,
            sortOption = sortOption,
            additionalFilter = additionalFilter,
            limit = null, // Don't limit here, pagination handles it
            useRanking = useRanking
        )
        
        // Cache results
        cachedSearchResults = searchResults.files
        lastSearchQuery = searchQuery
        
        return searchResults.files
    }
}

/**
 * Factory for creating SearchPagingSource instances
 */
class SearchPagingSourceFactory @Inject constructor(
    private val dataProcessor: FileBrowserDataProcessor,
    private val cacheManager: FileBrowserCacheManager
) {
    fun create(
        searchQuery: String,
        additionalFilter: FileFilterCriteria = FileFilterCriteria(),
        sortOption: FileEnhancedSortOption = FileEnhancedSortOption.DATE_DESC,
        useRanking: Boolean = true
    ): SearchPagingSource {
        return SearchPagingSource(
            dataProcessor = dataProcessor,
            cacheManager = cacheManager,
            searchQuery = searchQuery,
            additionalFilter = additionalFilter,
            sortOption = sortOption,
            useRanking = useRanking
        )
    }
}