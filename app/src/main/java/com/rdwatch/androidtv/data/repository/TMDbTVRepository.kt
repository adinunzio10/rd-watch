package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.network.models.tmdb.TMDbTVResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbCreditsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbRecommendationsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbTVImagesResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbTVVideosResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbSearchResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbExternalIdsResponse
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for TMDb TV show operations
 * Provides caching and offline-first access to TV show data
 */
interface TMDbTVRepository {
    
    /**
     * Get TV show details by TMDb ID
     * @param tvId TMDb TV show ID
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing TV show details
     */
    fun getTVDetails(
        tvId: Int,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbTVResponse>>
    
    /**
     * Get TV show details as ContentDetail for UI consumption
     * @param tvId TMDb TV show ID
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing ContentDetail
     */
    fun getTVContentDetail(
        tvId: Int,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<ContentDetail>>
    
    /**
     * Get TV show credits (cast and crew)
     * @param tvId TMDb TV show ID
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing credits
     */
    fun getTVCredits(
        tvId: Int,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbCreditsResponse>>
    
    /**
     * Get TV show recommendations
     * @param tvId TMDb TV show ID
     * @param page Page number for pagination
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing recommendations
     */
    fun getTVRecommendations(
        tvId: Int,
        page: Int = 1,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbRecommendationsResponse>>
    
    /**
     * Get similar TV shows
     * @param tvId TMDb TV show ID
     * @param page Page number for pagination
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing similar TV shows
     */
    fun getSimilarTVShows(
        tvId: Int,
        page: Int = 1,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbRecommendationsResponse>>
    
    /**
     * Get TV show images
     * @param tvId TMDb TV show ID
     * @param forceRefresh Force refresh from network
     * @param includeImageLanguage Additional image languages
     * @return Flow of Result containing images
     */
    fun getTVImages(
        tvId: Int,
        forceRefresh: Boolean = false,
        includeImageLanguage: String? = null
    ): Flow<Result<TMDbTVImagesResponse>>
    
    /**
     * Get TV show videos
     * @param tvId TMDb TV show ID
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing videos
     */
    fun getTVVideos(
        tvId: Int,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbTVVideosResponse>>
    
    /**
     * Get TV show external IDs (IMDb ID, TVDB ID, etc.)
     * @param tvId TMDb TV show ID
     * @param forceRefresh Force refresh from network
     * @return Flow of Result containing external IDs
     */
    fun getTVExternalIds(
        tvId: Int,
        forceRefresh: Boolean = false
    ): Flow<Result<TMDbExternalIdsResponse>>
    
    /**
     * Get season details
     * @param tvId TMDb TV show ID
     * @param seasonNumber Season number
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing season details
     */
    fun getSeasonDetails(
        tvId: Int,
        seasonNumber: Int,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbSeasonResponse>>
    
    /**
     * Get episode details
     * @param tvId TMDb TV show ID
     * @param seasonNumber Season number
     * @param episodeNumber Episode number
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing episode details
     */
    fun getEpisodeDetails(
        tvId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbEpisodeResponse>>
    
    /**
     * Get episode details as ContentDetail for UI consumption
     * @param tvId TMDb TV show ID
     * @param seasonNumber Season number
     * @param episodeNumber Episode number
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing ContentDetail
     */
    fun getEpisodeContentDetail(
        tvId: Int,
        seasonNumber: Int,
        episodeNumber: Int,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<ContentDetail>>
    
    /**
     * Get popular TV shows
     * @param page Page number for pagination
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing popular TV shows
     */
    fun getPopularTVShows(
        page: Int = 1,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbRecommendationsResponse>>
    
    /**
     * Get top rated TV shows
     * @param page Page number for pagination
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing top rated TV shows
     */
    fun getTopRatedTVShows(
        page: Int = 1,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbRecommendationsResponse>>
    
    /**
     * Get TV shows airing today
     * @param page Page number for pagination
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing TV shows airing today
     */
    fun getAiringTodayTVShows(
        page: Int = 1,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbRecommendationsResponse>>
    
    /**
     * Get TV shows on the air
     * @param page Page number for pagination
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing TV shows on the air
     */
    fun getOnTheAirTVShows(
        page: Int = 1,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbRecommendationsResponse>>
    
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
        firstAirDateYear: Int? = null
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
        withKeywords: String? = null
    ): Flow<Result<TMDbSearchResponse>>
    
    /**
     * Get trending TV shows
     * @param timeWindow Time window (day, week)
     * @param page Page number for pagination
     * @param language Language for the response
     * @return Flow of Result containing trending TV shows
     */
    fun getTrendingTVShows(
        timeWindow: String = "day",
        page: Int = 1,
        language: String = "en-US"
    ): Flow<Result<TMDbSearchResponse>>
    
    /**
     * Clear all cached TV show data
     */
    suspend fun clearCache()
    
    /**
     * Clear specific TV show cache
     * @param tvId TMDb TV show ID
     */
    suspend fun clearTVCache(tvId: Int)
    
    /**
     * Clear specific season cache
     * @param tvId TMDb TV show ID
     * @param seasonNumber Season number
     */
    suspend fun clearSeasonCache(tvId: Int, seasonNumber: Int)
    
    /**
     * Clear specific episode cache
     * @param tvId TMDb TV show ID
     * @param seasonNumber Season number
     * @param episodeNumber Episode number
     */
    suspend fun clearEpisodeCache(tvId: Int, seasonNumber: Int, episodeNumber: Int)
}