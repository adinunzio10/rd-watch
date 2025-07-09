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
    private val tmdbSearchDao: TMDbSearchDao
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
                .map { it?.toSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || shouldRefreshSearchCache(cachedSearch.lastUpdated)
        },
        createCall = {
            val response = tmdbSearchService.searchMovies(
                query, page, language, includeAdult, region, year, primaryReleaseYear
            ).execute()
            when (val apiResponse = handleApiResponse(response)) {
                is ApiResponse.Success -> apiResponse.data
                is ApiResponse.Error -> throw apiResponse.exception
                is ApiResponse.Loading -> throw Exception("Unexpected loading state")
            }
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
                .map { it?.toSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || shouldRefreshSearchCache(cachedSearch.lastUpdated)
        },
        createCall = {
            val response = tmdbSearchService.searchTVShows(
                query, page, language, includeAdult, firstAirDateYear
            ).execute()
            when (val apiResponse = handleApiResponse(response)) {
                is ApiResponse.Success -> apiResponse.data
                is ApiResponse.Error -> throw apiResponse.exception
                is ApiResponse.Loading -> throw Exception("Unexpected loading state")
            }
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
                .map { it?.toSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || shouldRefreshSearchCache(cachedSearch.lastUpdated)
        },
        createCall = {
            val response = tmdbSearchService.searchPeople(
                query, page, language, includeAdult, region
            ).execute()
            when (val apiResponse = handleApiResponse(response)) {
                is ApiResponse.Success -> apiResponse.data
                is ApiResponse.Error -> throw apiResponse.exception
                is ApiResponse.Loading -> throw Exception("Unexpected loading state")
            }
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
    ): Flow<Result<TMDbMultiSearchResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getSearchResults(query, "multi", page)
                .map { it?.toMultiSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || shouldRefreshSearchCache(cachedSearch.lastUpdated)
        },
        createCall = {
            val response = tmdbSearchService.searchMulti(
                query, page, language, includeAdult, region
            ).execute()
            when (val apiResponse = handleApiResponse(response)) {
                is ApiResponse.Success -> apiResponse.data
                is ApiResponse.Error -> throw apiResponse.exception
                is ApiResponse.Loading -> throw Exception("Unexpected loading state")
            }
        },
        saveCallResult = { multiSearchResponse ->
            val searchId = buildSearchId(query, page, "multi")
            tmdbSearchDao.insertSearchResult(multiSearchResponse.toEntity(searchId, query, page, "multi"))
        }
    )

    override fun multiSearchAsContentDetails(
        query: String,
        page: Int,
        language: String,
        includeAdult: Boolean,
        region: String?
    ): Flow<Result<List<ContentDetail>>> {
        // TODO: Implement ContentDetail mapping
        return flowOf(Result.Success(emptyList()))
    }

    override fun searchCollections(
        query: String,
        page: Int,
        language: String
    ): Flow<Result<TMDbSearchResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getSearchResults(query, "collection", page)
                .map { it?.toSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || shouldRefreshSearchCache(cachedSearch.lastUpdated)
        },
        createCall = {
            val response = tmdbSearchService.searchCollections(query, language, page).execute()
            when (val apiResponse = handleApiResponse(response)) {
                is ApiResponse.Success -> apiResponse.data
                is ApiResponse.Error -> throw apiResponse.exception
                is ApiResponse.Loading -> throw Exception("Unexpected loading state")
            }
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
                .map { it?.toSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || shouldRefreshSearchCache(cachedSearch.lastUpdated)
        },
        createCall = {
            val response = tmdbSearchService.searchCompanies(query, page).execute()
            when (val apiResponse = handleApiResponse(response)) {
                is ApiResponse.Success -> apiResponse.data
                is ApiResponse.Error -> throw apiResponse.exception
                is ApiResponse.Loading -> throw Exception("Unexpected loading state")
            }
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
                .map { it?.toSearchResponse() }
        },
        shouldFetch = { cachedSearch ->
            cachedSearch == null || shouldRefreshSearchCache(cachedSearch.lastUpdated)
        },
        createCall = {
            val response = tmdbSearchService.searchKeywords(query, page).execute()
            when (val apiResponse = handleApiResponse(response)) {
                is ApiResponse.Success -> apiResponse.data
                is ApiResponse.Error -> throw apiResponse.exception
                is ApiResponse.Loading -> throw Exception("Unexpected loading state")
            }
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
            val searchId = buildSearchId("trending_${mediaType}_$timeWindow", page, mediaType)
            tmdbSearchDao.getSearchResults("trending_${mediaType}_$timeWindow", mediaType, page)
                .map { it?.toSearchResponse() }
        },
        shouldFetch = { cachedTrending ->
            // Always fetch trending as it changes frequently
            true
        },
        createCall = {
            val response = tmdbSearchService.getTrending(mediaType, timeWindow, page, language).execute()
            when (val apiResponse = handleApiResponse(response)) {
                is ApiResponse.Success -> apiResponse.data
                is ApiResponse.Error -> throw apiResponse.exception
                is ApiResponse.Loading -> throw Exception("Unexpected loading state")
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
        // TODO: Implement ContentDetail mapping
        return flowOf(Result.Success(emptyList()))
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

    private fun <T> handleApiResponse(response: retrofit2.Response<ApiResponse<T>>): ApiResponse<T> {
        return if (response.isSuccessful) {
            response.body() ?: ApiResponse.Error(ApiException.ParseException("Empty response body"))
        } else {
            ApiResponse.Error(ApiException.HttpException(
                code = response.code(),
                message = response.message(),
                body = response.errorBody()?.string()
            ))
        }
    }
}