package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.TMDbSearchDao
import com.rdwatch.androidtv.data.entities.*
import com.rdwatch.androidtv.data.mappers.*
import com.rdwatch.androidtv.network.api.TMDbSearchService
import com.rdwatch.androidtv.network.models.tmdb.*
import com.rdwatch.androidtv.network.response.ApiResponse
import com.rdwatch.androidtv.network.response.ApiException
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.repository.base.networkBoundResource
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TMDbSearchRepository using NetworkBoundResource pattern
 * Provides offline-first access to search data with proper caching
 */
@Singleton
class TMDbSearchRepositoryImpl @Inject constructor(
    private val tmdbSearchService: TMDbSearchService,
    private val tmdbSearchDao: TMDbSearchDao,
    private val tmdbToContentDetailMapper: TMDbToContentDetailMapper
) : TMDbSearchRepository {

    companion object {
        private const val SEARCH_CACHE_TIMEOUT_MINUTES = 30
        private const val SEARCH_CACHE_TIMEOUT_MS = SEARCH_CACHE_TIMEOUT_MINUTES * 60 * 1000L
    }

    override fun searchMovies(
        query: String,
        page: Int,
        language: String,
        includeAdult: Boolean,
        region: String?,
        year: Int?,
        primaryReleaseYear: Int?
    ): Flow<Result<TMDbSearchResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getSearchResults(query, "movie", page)
                .map { it?.toSearchResponse() ?: TMDbSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || true // Always fetch search results as they can change frequently
        },
        createCall = {
            val response = tmdbSearchService.searchMovies(
                query, language, page, includeAdult, region, year, primaryReleaseYear
            ).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { searchResponse ->
            val searchId = buildSearchId(query, page, "movie")
            tmdbSearchDao.insertSearchResult(searchResponse.toEntity(searchId, query, page, "movie"))
        }
    )

    override fun searchTVShows(
        query: String,
        page: Int,
        language: String,
        includeAdult: Boolean,
        firstAirDateYear: Int?
    ): Flow<Result<TMDbSearchResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getSearchResults(query, "tv", page)
                .map { it?.toSearchResponse() ?: TMDbSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || true // Always fetch search results as they can change frequently
        },
        createCall = {
            val response = tmdbSearchService.searchTVShows(
                query, language, page, includeAdult, firstAirDateYear
            ).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { searchResponse ->
            val searchId = buildSearchId(query, page, "tv")
            tmdbSearchDao.insertSearchResult(searchResponse.toEntity(searchId, query, page, "tv"))
        }
    )

    override fun searchPeople(
        query: String,
        page: Int,
        language: String,
        includeAdult: Boolean,
        region: String?
    ): Flow<Result<TMDbSearchResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getSearchResults(query, "person", page)
                .map { it?.toSearchResponse() ?: TMDbSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || true // Always fetch search results as they can change frequently
        },
        createCall = {
            val response = tmdbSearchService.searchPeople(
                query, language, page, includeAdult, region
            ).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { searchResponse ->
            val searchId = buildSearchId(query, page, "person")
            tmdbSearchDao.insertSearchResult(searchResponse.toEntity(searchId, query, page, "person"))
        }
    )

    override fun multiSearch(
        query: String,
        page: Int,
        language: String,
        includeAdult: Boolean,
        region: String?
    ): Flow<Result<TMDbMultiSearchResponse>> {
        android.util.Log.d("TMDbSearchRepo", "multiSearch: query='$query', page=$page, language=$language, includeAdult=$includeAdult, region=$region")
        
        return networkBoundResource(
            loadFromDb = {
                android.util.Log.d("TMDbSearchRepo", "Loading multi-search from DB for query: $query")
                tmdbSearchDao.getSearchResults(query, "multi", page)
                    .map { it?.toMultiSearchResponse() ?: TMDbMultiSearchResponse() }
            },
            shouldFetch = { cachedSearch ->
                val shouldFetch = cachedSearch == null || true // Always fetch search results as they can change frequently
                android.util.Log.d("TMDbSearchRepo", "Should fetch multi-search for '$query': $shouldFetch")
                shouldFetch
            },
            createCall = {
                android.util.Log.d("TMDbSearchRepo", "Making TMDb API call for multi-search: '$query'")
                try {
                    val response = tmdbSearchService.multiSearch(
                        query, language, page, includeAdult, region
                    ).execute()
                    
                    android.util.Log.d("TMDbSearchRepo", "TMDb API response: isSuccessful=${response.isSuccessful}, code=${response.code()}")
                    
                    val result = handleRawApiResponse(response)
                    android.util.Log.d("TMDbSearchRepo", "Multi-search API success: ${result.results.size} results")
                    result
                } catch (e: Exception) {
                    android.util.Log.e("TMDbSearchRepo", "Exception in createCall for multi-search", e)
                    throw e
                }
            },
            saveCallResult = { multiSearchResponse ->
                android.util.Log.d("TMDbSearchRepo", "Saving multi-search results to DB for query: $query")
                val searchId = buildSearchId(query, page, "multi")
                tmdbSearchDao.insertSearchResult(multiSearchResponse.toEntity(searchId, query, page, "multi"))
            }
        )
    }

    override fun multiSearchAsContentDetails(
        query: String,
        page: Int,
        language: String,
        includeAdult: Boolean,
        region: String?
    ): Flow<Result<List<ContentDetail>>> {
        android.util.Log.d("TMDbSearchRepo", "multiSearchAsContentDetails: query='$query', page=$page, includeAdult=$includeAdult")
        
        return multiSearch(query, page, language, includeAdult, region).map { result ->
            when (result) {
                is Result.Success -> {
                    try {
                        android.util.Log.d("TMDbSearchRepo", "Multi-search returned ${result.data.results.size} results")
                        
                        val contentDetails = tmdbToContentDetailMapper.mapMultiSearchResultsToContentDetails(result.data.results)
                        
                        android.util.Log.d("TMDbSearchRepo", "Mapped to ${contentDetails.size} ContentDetails")
                        Result.Success(contentDetails)
                    } catch (e: Exception) {
                        android.util.Log.e("TMDbSearchRepo", "Error mapping search results to ContentDetails", e)
                        Result.Error(e)
                    }
                }
                is Result.Error -> {
                    android.util.Log.e("TMDbSearchRepo", "Multi-search failed", result.exception)
                    result
                }
                is Result.Loading -> {
                    android.util.Log.d("TMDbSearchRepo", "Multi-search loading...")
                    result
                }
            }
        }
    }

    override fun searchCollections(
        query: String,
        page: Int,
        language: String
    ): Flow<Result<TMDbSearchResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getSearchResults(query, "collection", page)
                .map { it?.toSearchResponse() ?: TMDbSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || true // Always fetch search results as they can change frequently
        },
        createCall = {
            val response = tmdbSearchService.searchCollections(query, language, page).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { searchResponse ->
            val searchId = buildSearchId(query, page, "collection")
            tmdbSearchDao.insertSearchResult(searchResponse.toEntity(searchId, query, page, "collection"))
        }
    )

    override fun searchCompanies(
        query: String,
        page: Int
    ): Flow<Result<TMDbSearchResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getSearchResults(query, "company", page)
                .map { it?.toSearchResponse() ?: TMDbSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || true // Always fetch search results as they can change frequently
        },
        createCall = {
            val response = tmdbSearchService.searchCompanies(query, page).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { searchResponse ->
            val searchId = buildSearchId(query, page, "company")
            tmdbSearchDao.insertSearchResult(searchResponse.toEntity(searchId, query, page, "company"))
        }
    )

    override fun searchKeywords(
        query: String,
        page: Int
    ): Flow<Result<TMDbSearchResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getSearchResults(query, "keyword", page)
                .map { it?.toSearchResponse() ?: TMDbSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || true // Always fetch search results as they can change frequently
        },
        createCall = {
            val response = tmdbSearchService.searchKeywords(query, page).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { searchResponse ->
            val searchId = buildSearchId(query, page, "keyword")
            tmdbSearchDao.insertSearchResult(searchResponse.toEntity(searchId, query, page, "keyword"))
        }
    )

    override fun getTrending(
        mediaType: String,
        timeWindow: String,
        language: String,
        page: Int
    ): Flow<Result<TMDbSearchResponse>> = networkBoundResource(
        loadFromDb = {
            android.util.Log.d("TMDbSearchRepo", "=== Loading from DB ===")
            val searchId = buildSearchId("trending_${mediaType}_$timeWindow", page, mediaType)
            android.util.Log.d("TMDbSearchRepo", "Searching DB for searchId: $searchId")
            
            val dbFlow = tmdbSearchDao.getSearchResults("trending_${mediaType}_$timeWindow", mediaType, page)
                .map { 
                    val result = it?.toSearchResponse() ?: TMDbSearchResponse()
                    android.util.Log.d("TMDbSearchRepo", "DB returned: ${if (it != null) "cached data with ${result.results.size} results" else "null (will use empty response)"}")
                    result
                }
            dbFlow
        },
        shouldFetch = { cachedTrending ->
            android.util.Log.d("TMDbSearchRepo", "=== Should Fetch Check ===")
            android.util.Log.d("TMDbSearchRepo", "Cached trending has ${cachedTrending?.results?.size ?: 0} results")
            val shouldFetch = true // Always fetch trending as it changes frequently
            android.util.Log.d("TMDbSearchRepo", "Should fetch from network: $shouldFetch")
            shouldFetch
        },
        createCall = {
            android.util.Log.d("TMDbSearchRepo", "=== Creating API Call ===")
            android.util.Log.d("TMDbSearchRepo", "Making getTrending API call: mediaType=$mediaType, timeWindow=$timeWindow, language=$language, page=$page")
            
            try {
                val response = tmdbSearchService.getTrending(mediaType, timeWindow, language, page).execute()
                
                android.util.Log.d("TMDbSearchRepo", "API call completed, processing response...")
                
                val result = handleRawApiResponse(response)
                android.util.Log.d("TMDbSearchRepo", "CreateCall returning success data with ${result.results.size} results")
                result
            } catch (e: Exception) {
                android.util.Log.e("TMDbSearchRepo", "Exception in createCall for trending", e)
                throw e
            }
        },
        saveCallResult = { trendingResponse ->
            val searchId = buildSearchId("trending_${mediaType}_$timeWindow", page, mediaType)
            tmdbSearchDao.insertSearchResult(
                trendingResponse.toEntity(searchId, "trending_${mediaType}_$timeWindow", page, mediaType)
            )
        }
    )

    override fun getTrendingAsContentDetails(
        mediaType: String,
        timeWindow: String,
        language: String,
        page: Int
    ): Flow<Result<List<ContentDetail>>> {
        android.util.Log.d("TMDbSearchRepo", "getTrendingAsContentDetails: mediaType='$mediaType', timeWindow='$timeWindow', page=$page")
        
        return getTrending(mediaType, timeWindow, language, page).map { result ->
            when (result) {
                is Result.Success -> {
                    try {
                        android.util.Log.d("TMDbSearchRepo", "Trending returned ${result.data.results.size} results")
                        
                        val contentDetails = tmdbToContentDetailMapper.mapSearchResultsToContentDetails(
                            result.data.results, 
                            mediaType.takeIf { it != "all" } // Pass media type hint if specific
                        )
                        
                        android.util.Log.d("TMDbSearchRepo", "Mapped trending to ${contentDetails.size} ContentDetails")
                        Result.Success(contentDetails)
                    } catch (e: Exception) {
                        android.util.Log.e("TMDbSearchRepo", "Error mapping trending results to ContentDetails", e)
                        Result.Error(e)
                    }
                }
                is Result.Error -> {
                    android.util.Log.e("TMDbSearchRepo", "Trending failed", result.exception)
                    result
                }
                is Result.Loading -> {
                    android.util.Log.d("TMDbSearchRepo", "Trending loading...")
                    result
                }
            }
        }
    }

    override fun discoverMovies(
        page: Int,
        language: String,
        region: String?,
        sortBy: String,
        includeAdult: Boolean,
        includeVideo: Boolean,
        primaryReleaseYear: Int?,
        withGenres: String?,
        withoutGenres: String?,
        withRuntimeGte: Int?,
        withRuntimeLte: Int?,
        voteAverageGte: Float?,
        voteAverageLte: Float?,
        voteCountGte: Int?,
        withOriginalLanguage: String?,
        withWatchProviders: String?,
        watchRegion: String?
    ): Flow<Result<TMDbSearchResponse>> {
        // TODO: Implement movie discovery
        return flowOf(Result.Success(TMDbSearchResponse(page = 1, totalPages = 1, totalResults = 0, results = emptyList())))
    }

    override fun discoverTVShows(
        page: Int,
        language: String,
        sortBy: String,
        airDateGte: String?,
        airDateLte: String?,
        firstAirDateGte: String?,
        firstAirDateLte: String?,
        firstAirDateYear: Int?,
        timezone: String?,
        voteAverageGte: Float?,
        voteCountGte: Int?,
        withGenres: String?,
        withNetworks: String?,
        withoutGenres: String?,
        withRuntimeGte: Int?,
        withRuntimeLte: Int?,
        includeNullFirstAirDates: Boolean?,
        withOriginalLanguage: String?,
        withoutKeywords: String?,
        withWatchProviders: String?,
        watchRegion: String?,
        withStatus: String?,
        withType: String?,
        withKeywords: String?
    ): Flow<Result<TMDbSearchResponse>> {
        // TODO: Implement TV show discovery
        return flowOf(Result.Success(TMDbSearchResponse(page = 1, totalPages = 1, totalResults = 0, results = emptyList())))
    }

    override fun getSearchSuggestions(
        query: String,
        limit: Int,
        language: String
    ): Flow<Result<List<String>>> {
        // TODO: Implement search suggestions
        return flowOf(Result.Success(emptyList()))
    }

    override fun getCachedSearchResults(
        query: String,
        mediaType: String?
    ): Flow<Result<List<ContentDetail>>> {
        // TODO: Implement cached search results
        return flowOf(Result.Success(emptyList()))
    }

    override suspend fun saveSearchHistory(
        query: String,
        mediaType: String?,
        resultCount: Int
    ) {
        // TODO: Implement search history saving
    }

    override fun getSearchHistory(
        limit: Int
    ): Flow<Result<List<String>>> {
        return tmdbSearchDao.getRecentSearchQueries(limit).map { queries ->
            Result.Success(queries)
        }
    }

    override suspend fun clearSearchHistory() {
        // TODO: Implement search history clearing
    }

    override suspend fun clearSearchCache() {
        tmdbSearchDao.deleteAllSearchResults()
    }

    override suspend fun clearSearchCache(query: String) {
        tmdbSearchDao.deleteSearchResults(query)
    }

    // Helper methods

    private fun buildSearchId(query: String, page: Int, searchType: String): String {
        return "$query-$page-$searchType"
    }

    private fun shouldRefreshSearchCache(lastUpdated: Long): Boolean {
        return System.currentTimeMillis() - lastUpdated > SEARCH_CACHE_TIMEOUT_MS
    }

    private fun <T> handleRawApiResponse(response: retrofit2.Response<T>): T {
        android.util.Log.d("TMDbSearchRepo", "=== API Response Debug ===")
        android.util.Log.d("TMDbSearchRepo", "Response URL: ${response.raw().request.url}")
        android.util.Log.d("TMDbSearchRepo", "Response code: ${response.code()}")
        android.util.Log.d("TMDbSearchRepo", "Response message: ${response.message()}")
        android.util.Log.d("TMDbSearchRepo", "Response isSuccessful: ${response.isSuccessful}")
        
        return if (response.isSuccessful) {
            val body = response.body()
            android.util.Log.d("TMDbSearchRepo", "Response body is null: ${body == null}")
            
            if (body != null) {
                android.util.Log.d("TMDbSearchRepo", "Response body type: ${body::class.simpleName}")
                
                // Log specific details for TMDb responses
                when (body) {
                    is TMDbSearchResponse -> {
                        android.util.Log.d("TMDbSearchRepo", "TMDbSearchResponse with ${body.results.size} results, page ${body.page}/${body.totalPages}")
                    }
                    is TMDbMultiSearchResponse -> {
                        android.util.Log.d("TMDbSearchRepo", "TMDbMultiSearchResponse with ${body.results.size} results, page ${body.page}/${body.totalPages}")
                    }
                }
            }
            
            body ?: throw ApiException.ParseException("Empty response body")
        } else {
            val errorBody = response.errorBody()?.string()
            android.util.Log.e("TMDbSearchRepo", "HTTP Error ${response.code()}: ${response.message()}")
            android.util.Log.e("TMDbSearchRepo", "Error body: $errorBody")
            
            throw ApiException.HttpException(
                code = response.code(),
                message = response.message(),
                body = errorBody
            )
        }
    }
}