package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.network.models.tmdb.TMDbMultiSearchResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbSearchResponse
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for TMDb search operations
 * Provides caching and offline-first access to search data
 */
interface TMDbSearchRepository {
    /**
     * Search movies
     * @param query Search query
     * @param page Page number for pagination
     * @param language Language for the response
     * @param includeAdult Include adult content
     * @param region Region for filtering
     * @param year Filter by year
     * @param primaryReleaseYear Filter by primary release year
     * @return Flow of Result containing search results
     */
    fun searchMovies(
        query: String,
        page: Int = 1,
        language: String = "en-US",
        includeAdult: Boolean = false,
        region: String? = null,
        year: Int? = null,
        primaryReleaseYear: Int? = null,
    ): Flow<Result<TMDbSearchResponse>>

    /**
     * Search TV shows
     * @param query Search query
     * @param page Page number for pagination
     * @param language Language for the response
     * @param includeAdult Include adult content
     * @param firstAirDateYear Filter by first air date year
     * @return Flow of Result containing search results
     */
    fun searchTVShows(
        query: String,
        page: Int = 1,
        language: String = "en-US",
        includeAdult: Boolean = false,
        firstAirDateYear: Int? = null,
    ): Flow<Result<TMDbSearchResponse>>

    /**
     * Search people
     * @param query Search query
     * @param page Page number for pagination
     * @param language Language for the response
     * @param includeAdult Include adult content
     * @param region Region for filtering
     * @return Flow of Result containing search results
     */
    fun searchPeople(
        query: String,
        page: Int = 1,
        language: String = "en-US",
        includeAdult: Boolean = false,
        region: String? = null,
    ): Flow<Result<TMDbSearchResponse>>

    /**
     * Multi-search across movies, TV shows, and people
     * @param query Search query
     * @param page Page number for pagination
     * @param language Language for the response
     * @param includeAdult Include adult content
     * @param region Region for filtering
     * @return Flow of Result containing multi-search results
     */
    fun multiSearch(
        query: String,
        page: Int = 1,
        language: String = "en-US",
        includeAdult: Boolean = false,
        region: String? = null,
    ): Flow<Result<TMDbMultiSearchResponse>>

    /**
     * Multi-search and convert to ContentDetail list
     * @param query Search query
     * @param page Page number for pagination
     * @param language Language for the response
     * @param includeAdult Include adult content
     * @param region Region for filtering
     * @return Flow of Result containing ContentDetail list
     */
    fun multiSearchAsContentDetails(
        query: String,
        page: Int = 1,
        language: String = "en-US",
        includeAdult: Boolean = false,
        region: String? = null,
    ): Flow<Result<List<ContentDetail>>>

    /**
     * Search collections
     * @param query Search query
     * @param page Page number for pagination
     * @param language Language for the response
     * @return Flow of Result containing search results
     */
    fun searchCollections(
        query: String,
        page: Int = 1,
        language: String = "en-US",
    ): Flow<Result<TMDbSearchResponse>>

    /**
     * Search companies
     * @param query Search query
     * @param page Page number for pagination
     * @return Flow of Result containing search results
     */
    fun searchCompanies(
        query: String,
        page: Int = 1,
    ): Flow<Result<TMDbSearchResponse>>

    /**
     * Search keywords
     * @param query Search query
     * @param page Page number for pagination
     * @return Flow of Result containing search results
     */
    fun searchKeywords(
        query: String,
        page: Int = 1,
    ): Flow<Result<TMDbSearchResponse>>

    /**
     * Get trending content
     * @param mediaType Media type (all, movie, tv, person)
     * @param timeWindow Time window (day, week)
     * @param language Language for the response
     * @param page Page number for pagination
     * @return Flow of Result containing trending content
     */
    fun getTrending(
        mediaType: String = "all",
        timeWindow: String = "day",
        language: String = "en-US",
        page: Int = 1,
    ): Flow<Result<TMDbSearchResponse>>

    /**
     * Get trending content as ContentDetail list
     * @param mediaType Media type (all, movie, tv, person)
     * @param timeWindow Time window (day, week)
     * @param language Language for the response
     * @param page Page number for pagination
     * @return Flow of Result containing ContentDetail list
     */
    fun getTrendingAsContentDetails(
        mediaType: String = "all",
        timeWindow: String = "day",
        language: String = "en-US",
        page: Int = 1,
    ): Flow<Result<List<ContentDetail>>>

    /**
     * Discover movies with filtering
     * @param page Page number for pagination
     * @param language Language for the response
     * @param region Region for filtering
     * @param sortBy Sort criteria
     * @param includeAdult Include adult content
     * @param includeVideo Include video content
     * @param primaryReleaseYear Filter by primary release year
     * @param withGenres Filter by genres
     * @param withoutGenres Exclude genres
     * @param withRuntimeGte Minimum runtime
     * @param withRuntimeLte Maximum runtime
     * @param voteAverageGte Minimum vote average
     * @param voteAverageLte Maximum vote average
     * @param voteCountGte Minimum vote count
     * @param withOriginalLanguage Filter by original language
     * @param withWatchProviders Filter by watch providers
     * @param watchRegion Watch region
     * @return Flow of Result containing discovery results
     */
    fun discoverMovies(
        page: Int = 1,
        language: String = "en-US",
        region: String? = null,
        sortBy: String = "popularity.desc",
        includeAdult: Boolean = false,
        includeVideo: Boolean = false,
        primaryReleaseYear: Int? = null,
        withGenres: String? = null,
        withoutGenres: String? = null,
        withRuntimeGte: Int? = null,
        withRuntimeLte: Int? = null,
        voteAverageGte: Float? = null,
        voteAverageLte: Float? = null,
        voteCountGte: Int? = null,
        withOriginalLanguage: String? = null,
        withWatchProviders: String? = null,
        watchRegion: String? = null,
    ): Flow<Result<TMDbSearchResponse>>

    /**
     * Discover TV shows with filtering
     * @param page Page number for pagination
     * @param language Language for the response
     * @param sortBy Sort criteria
     * @param airDateGte Minimum air date
     * @param airDateLte Maximum air date
     * @param firstAirDateGte Minimum first air date
     * @param firstAirDateLte Maximum first air date
     * @param firstAirDateYear Filter by first air date year
     * @param timezone Timezone
     * @param voteAverageGte Minimum vote average
     * @param voteCountGte Minimum vote count
     * @param withGenres Filter by genres
     * @param withNetworks Filter by networks
     * @param withoutGenres Exclude genres
     * @param withRuntimeGte Minimum runtime
     * @param withRuntimeLte Maximum runtime
     * @param includeNullFirstAirDates Include shows with null first air dates
     * @param withOriginalLanguage Filter by original language
     * @param withoutKeywords Exclude keywords
     * @param withWatchProviders Filter by watch providers
     * @param watchRegion Watch region
     * @param withStatus Filter by status
     * @param withType Filter by type
     * @param withKeywords Filter by keywords
     * @return Flow of Result containing discovery results
     */
    fun discoverTVShows(
        page: Int = 1,
        language: String = "en-US",
        sortBy: String = "popularity.desc",
        airDateGte: String? = null,
        airDateLte: String? = null,
        firstAirDateGte: String? = null,
        firstAirDateLte: String? = null,
        firstAirDateYear: Int? = null,
        timezone: String? = null,
        voteAverageGte: Float? = null,
        voteCountGte: Int? = null,
        withGenres: String? = null,
        withNetworks: String? = null,
        withoutGenres: String? = null,
        withRuntimeGte: Int? = null,
        withRuntimeLte: Int? = null,
        includeNullFirstAirDates: Boolean? = null,
        withOriginalLanguage: String? = null,
        withoutKeywords: String? = null,
        withWatchProviders: String? = null,
        watchRegion: String? = null,
        withStatus: String? = null,
        withType: String? = null,
        withKeywords: String? = null,
    ): Flow<Result<TMDbSearchResponse>>

    /**
     * Get search suggestions for a query
     * @param query Search query
     * @param limit Maximum number of suggestions
     * @param language Language for the response
     * @return Flow of Result containing search suggestions
     */
    fun getSearchSuggestions(
        query: String,
        limit: Int = 5,
        language: String = "en-US",
    ): Flow<Result<List<String>>>

    /**
     * Get cached search results
     * @param query Search query
     * @param mediaType Media type filter
     * @return Flow of Result containing cached search results
     */
    fun getCachedSearchResults(
        query: String,
        mediaType: String? = null,
    ): Flow<Result<List<ContentDetail>>>

    /**
     * Save search query to history
     * @param query Search query
     * @param mediaType Media type
     * @param resultCount Number of results
     */
    suspend fun saveSearchHistory(
        query: String,
        mediaType: String? = null,
        resultCount: Int = 0,
    )

    /**
     * Get search history
     * @param limit Maximum number of history items
     * @return Flow of Result containing search history
     */
    fun getSearchHistory(limit: Int = 10): Flow<Result<List<String>>>

    /**
     * Clear search history
     */
    suspend fun clearSearchHistory()

    /**
     * Clear search cache
     */
    suspend fun clearSearchCache()

    /**
     * Clear specific search cache
     * @param query Search query
     */
    suspend fun clearSearchCache(query: String)
}
