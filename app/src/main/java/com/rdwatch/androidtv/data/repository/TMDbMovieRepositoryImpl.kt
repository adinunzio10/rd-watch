package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.TMDbMovieDao
import com.rdwatch.androidtv.data.dao.TMDbSearchDao
import com.rdwatch.androidtv.data.entities.*
import com.rdwatch.androidtv.data.mappers.TMDbToContentDetailMapper
import com.rdwatch.androidtv.data.mappers.*
import com.rdwatch.androidtv.network.api.TMDbMovieService
import com.rdwatch.androidtv.network.models.tmdb.*
import com.rdwatch.androidtv.network.response.ApiException
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.repository.base.networkBoundResource
import com.rdwatch.androidtv.repository.base.safeCall
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of TMDbMovieRepository using NetworkBoundResource pattern
 * Provides offline-first access to movie data with proper caching
 */
@Singleton
class TMDbMovieRepositoryImpl @Inject constructor(
    private val tmdbMovieService: TMDbMovieService,
    private val tmdbMovieDao: TMDbMovieDao,
    private val tmdbSearchDao: TMDbSearchDao,
    private val contentDetailMapper: TMDbToContentDetailMapper
) : TMDbMovieRepository {

    companion object {
        private const val CACHE_TIMEOUT_HOURS = 24
        private const val CACHE_TIMEOUT_MS = CACHE_TIMEOUT_HOURS * 60 * 60 * 1000L
    }

    override fun getMovieDetails(
        movieId: Int,
        forceRefresh: Boolean,
        language: String
    ): Flow<Result<TMDbMovieResponse?>> = networkBoundResource(
        loadFromDb = { 
            android.util.Log.d("TMDbMovieRepo", "=== Loading from DB for movieId: $movieId ===")
            val dbFlow = tmdbMovieDao.getMovieById(movieId).map { 
                val result = it?.toMovieResponse()
                android.util.Log.d("TMDbMovieRepo", "DB returned: ${if (result != null) "cached movie ${result.title}" else "null (no cached data)"}")
                result
            }
            dbFlow
        },
        shouldFetch = { cachedMovie ->
            android.util.Log.d("TMDbMovieRepo", "=== Should Fetch Check ===")
            android.util.Log.d("TMDbMovieRepo", "Cached movie: ${cachedMovie?.title ?: "null"}")
            android.util.Log.d("TMDbMovieRepo", "forceRefresh: $forceRefresh")
            val shouldFetch = forceRefresh || cachedMovie == null || shouldRefreshCache(movieId, "movie")
            android.util.Log.d("TMDbMovieRepo", "Decision: shouldFetch = $shouldFetch")
            shouldFetch
        },
        createCall = {
            android.util.Log.d("TMDbMovieRepo", "=== Creating API Call for movieId: $movieId ===")
            val response = tmdbMovieService.getMovieDetails(movieId, null, language).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { movieResponse ->
            android.util.Log.d("TMDbMovieRepo", "=== Saving to DB: ${movieResponse.title} (ID: ${movieResponse.id}) ===")
            tmdbMovieDao.insertMovie(movieResponse.toEntity())
        },
        onFetchFailed = { throwable ->
            android.util.Log.e("TMDbMovieRepo", "=== Fetch Failed ===", throwable)
            android.util.Log.e("TMDbMovieRepo", "Error message: ${throwable.message}")
            // Log error or handle specific error cases
            // For now, we'll just let the cached data be returned
        }
    )

    override fun getMovieContentDetail(
        movieId: Int,
        forceRefresh: Boolean,
        language: String
    ): Flow<Result<ContentDetail?>> = 
        getMovieDetails(movieId, forceRefresh, language).map { result ->
            when (result) {
                is Result.Success -> {
                    val contentDetail = result.data?.let { movieResponse ->
                        contentDetailMapper.mapMovieToContentDetail(movieResponse)
                    }
                    Result.Success(contentDetail)
                }
                is Result.Error -> Result.Error(result.exception)
                is Result.Loading -> Result.Loading
            }
        }

    override fun getMovieCredits(
        movieId: Int,
        forceRefresh: Boolean,
        language: String
    ): Flow<Result<TMDbCreditsResponse?>> = networkBoundResource(
        loadFromDb = { 
            tmdbSearchDao.getCredits(movieId, "movie").map { entity -> 
                entity?.toCreditsResponse() 
            }
        },
        shouldFetch = { cachedCredits ->
            forceRefresh || cachedCredits == null || shouldRefreshCache(movieId, "credits")
        },
        createCall = {
            val response = tmdbMovieService.getMovieCredits(movieId, language).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { creditsResponse ->
            tmdbSearchDao.insertCredits(creditsResponse.toEntity(movieId, "movie"))
        }
    )

    override fun getMovieRecommendations(
        movieId: Int,
        page: Int,
        forceRefresh: Boolean,
        language: String
    ): Flow<Result<TMDbRecommendationsResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getRecommendations(movieId, "movie", "recommendations", page)
                .map { it?.toRecommendationsResponse() ?: TMDbRecommendationsResponse() }
        },
        shouldFetch = { cachedRecommendations ->
            forceRefresh || cachedRecommendations == null || shouldRefreshCache(movieId, "recommendations")
        },
        createCall = {
            val response = tmdbMovieService.getMovieRecommendations(movieId, language, page).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { recommendationsResponse ->
            tmdbSearchDao.insertRecommendations(
                recommendationsResponse.toEntity(movieId, "movie", "recommendations", page)
            )
        }
    )

    override fun getSimilarMovies(
        movieId: Int,
        page: Int,
        forceRefresh: Boolean,
        language: String
    ): Flow<Result<TMDbRecommendationsResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getRecommendations(movieId, "movie", "similar", page)
                .map { it?.toRecommendationsResponse() ?: TMDbRecommendationsResponse() }
        },
        shouldFetch = { cachedSimilar ->
            forceRefresh || cachedSimilar == null || shouldRefreshCache(movieId, "similar")
        },
        createCall = {
            val response = tmdbMovieService.getSimilarMovies(movieId, language, page).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { similarResponse ->
            tmdbSearchDao.insertRecommendations(
                similarResponse.toEntity(movieId, "movie", "similar", page)
            )
        }
    )

    override fun getMovieImages(
        movieId: Int,
        forceRefresh: Boolean,
        includeImageLanguage: String?
    ): Flow<Result<TMDbMovieImagesResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getImages(movieId, "movie").map { it?.toImagesResponse() ?: TMDbMovieImagesResponse() }
        },
        shouldFetch = { cachedImages ->
            forceRefresh || cachedImages == null || shouldRefreshCache(movieId, "images")
        },
        createCall = {
            val response = tmdbMovieService.getMovieImages(movieId, includeImageLanguage).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { imagesResponse ->
            tmdbSearchDao.insertImages(imagesResponse.toEntity(movieId, "movie"))
        }
    )

    override fun getMovieVideos(
        movieId: Int,
        forceRefresh: Boolean,
        language: String
    ): Flow<Result<TMDbMovieVideosResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getVideos(movieId, "movie").map { it?.toVideosResponse() ?: TMDbMovieVideosResponse() }
        },
        shouldFetch = { cachedVideos ->
            forceRefresh || cachedVideos == null || shouldRefreshCache(movieId, "videos")
        },
        createCall = {
            val response = tmdbMovieService.getMovieVideos(movieId, language).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { videosResponse ->
            tmdbSearchDao.insertVideos(videosResponse.toEntity(movieId, "movie"))
        }
    )

    override fun getPopularMovies(
        page: Int,
        forceRefresh: Boolean,
        language: String,
        region: String?
    ): Flow<Result<TMDbRecommendationsResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getRecommendations(0, "movie", "popular", page)
                .map { it?.toRecommendationsResponse() ?: TMDbRecommendationsResponse() }
        },
        shouldFetch = { cachedPopular ->
            forceRefresh || cachedPopular == null || shouldRefreshCache(0, "popular")
        },
        createCall = {
            val response = tmdbMovieService.getPopularMovies(language, page, region).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { popularResponse ->
            tmdbSearchDao.insertRecommendations(
                popularResponse.toEntity(0, "movie", "popular", page)
            )
        }
    )

    override fun getTopRatedMovies(
        page: Int,
        forceRefresh: Boolean,
        language: String,
        region: String?
    ): Flow<Result<TMDbRecommendationsResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getRecommendations(0, "movie", "top_rated", page)
                .map { it?.toRecommendationsResponse() ?: TMDbRecommendationsResponse() }
        },
        shouldFetch = { cachedTopRated ->
            forceRefresh || cachedTopRated == null || shouldRefreshCache(0, "top_rated")
        },
        createCall = {
            val response = tmdbMovieService.getTopRatedMovies(language, page, region).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { topRatedResponse ->
            tmdbSearchDao.insertRecommendations(
                topRatedResponse.toEntity(0, "movie", "top_rated", page)
            )
        }
    )

    override fun getNowPlayingMovies(
        page: Int,
        forceRefresh: Boolean,
        language: String,
        region: String?
    ): Flow<Result<TMDbRecommendationsResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getRecommendations(0, "movie", "now_playing", page)
                .map { it?.toRecommendationsResponse() ?: TMDbRecommendationsResponse() }
        },
        shouldFetch = { cachedNowPlaying ->
            forceRefresh || cachedNowPlaying == null || shouldRefreshCache(0, "now_playing")
        },
        createCall = {
            val response = tmdbMovieService.getNowPlayingMovies(language, page, region).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { nowPlayingResponse ->
            tmdbSearchDao.insertRecommendations(
                nowPlayingResponse.toEntity(0, "movie", "now_playing", page)
            )
        }
    )

    override fun getUpcomingMovies(
        page: Int,
        forceRefresh: Boolean,
        language: String,
        region: String?
    ): Flow<Result<TMDbRecommendationsResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getRecommendations(0, "movie", "upcoming", page)
                .map { it?.toRecommendationsResponse() ?: TMDbRecommendationsResponse() }
        },
        shouldFetch = { cachedUpcoming ->
            forceRefresh || cachedUpcoming == null || shouldRefreshCache(0, "upcoming")
        },
        createCall = {
            val response = tmdbMovieService.getUpcomingMovies(language, page, region).execute()
            handleRawApiResponse(response)
        },
        saveCallResult = { upcomingResponse ->
            tmdbSearchDao.insertRecommendations(
                upcomingResponse.toEntity(0, "movie", "upcoming", page)
            )
        }
    )

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
            // Always fetch search results as they can change frequently
            true
        },
        createCall = {
            // Note: TMDb search service not implemented in architecture, using movie service
            // This would need to be implemented in TMDbSearchService
            throw NotImplementedError("Search functionality requires TMDbSearchService implementation")
        },
        saveCallResult = { searchResponse ->
            val searchId = "$query-$page-movie"
            tmdbSearchDao.insertSearchResult((searchResponse as TMDbSearchResponse).toEntity(searchId, query, page, "movie"))
        }
    )

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
    ): Flow<Result<TMDbSearchResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getSearchResults("discover", "movie", page)
                .map { it?.toSearchResponse() ?: TMDbSearchResponse() }
        },
        shouldFetch = { cachedDiscovery ->
            // Always fetch discovery results as they can change frequently
            true
        },
        createCall = {
            // Note: TMDb discovery service not implemented in architecture
            // This would need to be implemented in TMDbMovieService
            throw NotImplementedError("Discovery functionality requires TMDbMovieService implementation")
        },
        saveCallResult = { discoveryResponse ->
            val discoveryId = "discover-$page-movie"
            tmdbSearchDao.insertSearchResult((discoveryResponse as TMDbSearchResponse).toEntity(discoveryId, "discover", page, "movie"))
        }
    )

    override fun getTrendingMovies(
        timeWindow: String,
        page: Int,
        language: String
    ): Flow<Result<TMDbSearchResponse>> = networkBoundResource(
        loadFromDb = {
            tmdbSearchDao.getRecommendations(0, "movie", "trending_$timeWindow", page)
                .map { it?.toSearchResponse() ?: TMDbSearchResponse() }
        },
        shouldFetch = { cachedTrending ->
            // Always fetch trending results as they change frequently
            true
        },
        createCall = {
            // Note: TMDb trending service not implemented in architecture
            // This would need to be implemented in TMDbMovieService
            throw NotImplementedError("Trending functionality requires TMDbMovieService implementation")
        },
        saveCallResult = { trendingResponse ->
            tmdbSearchDao.insertRecommendations(
                trendingResponse.toRecommendationEntity(0, "movie", "trending_$timeWindow", page)
            )
        }
    )

    override suspend fun clearCache() {
        tmdbMovieDao.deleteAllMovies()
        tmdbSearchDao.deleteAllSearchResults()
        tmdbSearchDao.deleteAllCredits()
        tmdbSearchDao.deleteAllRecommendations()
        tmdbSearchDao.deleteAllImages()
        tmdbSearchDao.deleteAllVideos()
    }

    override suspend fun clearMovieCache(movieId: Int) {
        tmdbMovieDao.deleteMovieById(movieId)
        tmdbSearchDao.deleteCredits(movieId, "movie")
        tmdbSearchDao.deleteRecommendations(movieId, "movie", "recommendations")
        tmdbSearchDao.deleteRecommendations(movieId, "movie", "similar")
        tmdbSearchDao.deleteImages(movieId, "movie")
        tmdbSearchDao.deleteVideos(movieId, "movie")
    }

    // Helper methods

    private fun shouldRefreshCache(contentId: Int, type: String): Boolean {
        // For now, we'll use a simpler approach - always refresh if older than cache timeout
        // This avoids the suspend context requirement
        return true // TODO: Implement proper cache checking
    }

    private fun <T> handleRawApiResponse(response: retrofit2.Response<T>): T {
        android.util.Log.d("TMDbMovieRepo", "=== API Response Debug ===")
        android.util.Log.d("TMDbMovieRepo", "Response URL: ${response.raw().request.url}")
        android.util.Log.d("TMDbMovieRepo", "Response code: ${response.code()}")
        android.util.Log.d("TMDbMovieRepo", "Response message: ${response.message()}")
        android.util.Log.d("TMDbMovieRepo", "Response isSuccessful: ${response.isSuccessful}")
        
        return if (response.isSuccessful) {
            val body = response.body()
            android.util.Log.d("TMDbMovieRepo", "Response body is null: ${body == null}")
            
            if (body != null) {
                android.util.Log.d("TMDbMovieRepo", "Response body type: ${body::class.simpleName}")
                
                // Log specific details for TMDb responses
                when (body) {
                    is TMDbMovieResponse -> {
                        android.util.Log.d("TMDbMovieRepo", "TMDbMovieResponse: ${body.title} (ID: ${body.id})")
                    }
                    is TMDbRecommendationsResponse -> {
                        android.util.Log.d("TMDbMovieRepo", "TMDbRecommendationsResponse with ${body.results.size} results, page ${body.page}/${body.totalPages}")
                    }
                }
            }
            
            body ?: throw ApiException.ParseException("Empty response body")
        } else {
            val errorBody = response.errorBody()?.string()
            android.util.Log.e("TMDbMovieRepo", "HTTP Error ${response.code()}: ${response.message()}")
            android.util.Log.e("TMDbMovieRepo", "Error body: $errorBody")
            
            throw ApiException.HttpException(
                code = response.code(),
                message = response.message(),
                body = errorBody
            )
        }
    }

}