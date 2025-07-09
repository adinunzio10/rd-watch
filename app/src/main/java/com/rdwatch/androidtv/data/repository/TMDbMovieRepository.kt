package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbCreditsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbRecommendationsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieImagesResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieVideosResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbSearchResponse
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for TMDb movie operations
 * Provides caching and offline-first access to movie data
 */
interface TMDbMovieRepository {
    
    /**
     * Get movie details by TMDb ID
     * @param movieId TMDb movie ID
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing movie details
     */
    fun getMovieDetails(
        movieId: Int,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbMovieResponse?>>
    
    /**
     * Get movie details as ContentDetail for UI consumption
     * @param movieId TMDb movie ID
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing ContentDetail
     */
    fun getMovieContentDetail(
        movieId: Int,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<ContentDetail?>>
    
    /**
     * Get movie credits (cast and crew)
     * @param movieId TMDb movie ID
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing credits
     */
    fun getMovieCredits(
        movieId: Int,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbCreditsResponse?>>
    
    /**
     * Get movie recommendations
     * @param movieId TMDb movie ID
     * @param page Page number for pagination
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing recommendations
     */
    fun getMovieRecommendations(
        movieId: Int,
        page: Int = 1,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbRecommendationsResponse>>
    
    /**
     * Get similar movies
     * @param movieId TMDb movie ID
     * @param page Page number for pagination
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing similar movies
     */
    fun getSimilarMovies(
        movieId: Int,
        page: Int = 1,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbRecommendationsResponse>>
    
    /**
     * Get movie images
     * @param movieId TMDb movie ID
     * @param forceRefresh Force refresh from network
     * @param includeImageLanguage Additional image languages
     * @return Flow of Result containing images
     */
    fun getMovieImages(
        movieId: Int,
        forceRefresh: Boolean = false,
        includeImageLanguage: String? = null
    ): Flow<Result<TMDbMovieImagesResponse>>
    
    /**
     * Get movie videos
     * @param movieId TMDb movie ID
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @return Flow of Result containing videos
     */
    fun getMovieVideos(
        movieId: Int,
        forceRefresh: Boolean = false,
        language: String = "en-US"
    ): Flow<Result<TMDbMovieVideosResponse>>
    
    /**
     * Get popular movies
     * @param page Page number for pagination
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @param region Region for filtering
     * @return Flow of Result containing popular movies
     */
    fun getPopularMovies(
        page: Int = 1,
        forceRefresh: Boolean = false,
        language: String = "en-US",
        region: String? = null
    ): Flow<Result<TMDbRecommendationsResponse>>
    
    /**
     * Get top rated movies
     * @param page Page number for pagination
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @param region Region for filtering
     * @return Flow of Result containing top rated movies
     */
    fun getTopRatedMovies(
        page: Int = 1,
        forceRefresh: Boolean = false,
        language: String = "en-US",
        region: String? = null
    ): Flow<Result<TMDbRecommendationsResponse>>
    
    /**
     * Get now playing movies
     * @param page Page number for pagination
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @param region Region for filtering
     * @return Flow of Result containing now playing movies
     */
    fun getNowPlayingMovies(
        page: Int = 1,
        forceRefresh: Boolean = false,
        language: String = "en-US",
        region: String? = null
    ): Flow<Result<TMDbRecommendationsResponse>>
    
    /**
     * Get upcoming movies
     * @param page Page number for pagination
     * @param forceRefresh Force refresh from network
     * @param language Language for the response
     * @param region Region for filtering
     * @return Flow of Result containing upcoming movies
     */
    fun getUpcomingMovies(
        page: Int = 1,
        forceRefresh: Boolean = false,
        language: String = "en-US",
        region: String? = null
    ): Flow<Result<TMDbRecommendationsResponse>>
    
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
        primaryReleaseYear: Int? = null
    ): Flow<Result<TMDbSearchResponse>>
    
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
        watchRegion: String? = null
    ): Flow<Result<TMDbSearchResponse>>
    
    /**
     * Clear all cached movie data
     */
    suspend fun clearCache()
    
    /**
     * Clear specific movie cache
     * @param movieId TMDb movie ID
     */
    suspend fun clearMovieCache(movieId: Int)
    
    /**
     * Get trending movies
     * @param timeWindow Time window (day, week)
     * @param page Page number for pagination
     * @param language Language for the response
     * @return Flow of Result containing trending movies
     */
    fun getTrendingMovies(
        timeWindow: String = "day",
        page: Int = 1,
        language: String = "en-US"
    ): Flow<Result<TMDbSearchResponse>>
}