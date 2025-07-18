package com.rdwatch.androidtv.data.repository

import com.rdwatch.androidtv.data.dao.TMDbSearchDao
import com.rdwatch.androidtv.data.dao.TMDbTVDao
import com.rdwatch.androidtv.data.entities.TMDbEpisodeExternalIdsEntity
import com.rdwatch.androidtv.data.mappers.*
import com.rdwatch.androidtv.data.mappers.TMDbEpisodeContentDetail
import com.rdwatch.androidtv.data.mappers.TMDbToContentDetailMapper
import com.rdwatch.androidtv.network.api.TMDbTVService
import com.rdwatch.androidtv.network.models.tmdb.*
import com.rdwatch.androidtv.network.response.ApiException
import com.rdwatch.androidtv.network.response.ApiResponse
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.repository.base.networkBoundResource
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Implementation of TMDbTVRepository using NetworkBoundResource pattern
 * Provides offline-first access to TV show data with proper caching
 */
@Singleton
class TMDbTVRepositoryImpl
    @Inject
    constructor(
        private val tmdbTVService: TMDbTVService,
        private val tmdbTVDao: TMDbTVDao,
        private val tmdbSearchDao: TMDbSearchDao,
        private val contentDetailMapper: TMDbToContentDetailMapper,
    ) : TMDbTVRepository {
        companion object {
            private const val CACHE_TIMEOUT_HOURS = 24
            private const val CACHE_TIMEOUT_MS = CACHE_TIMEOUT_HOURS * 60 * 60 * 1000L
        }

        override fun getTVDetails(
            tvId: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<TMDbTVResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbTVDao.getTVShowById(tvId).map { it?.toTVResponse() ?: TMDbTVResponse() }
                },
                shouldFetch = { cachedTV ->
                    forceRefresh || cachedTV == null || shouldRefreshCache(tvId, "tv")
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getTVDetails(tvId, "credits,images,videos,recommendations,similar", language))
                },
                saveCallResult = { tvResponse ->
                    tmdbTVDao.insertTVShow(tvResponse.toEntity())
                },
            )

        override fun getTVContentDetail(
            tvId: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<ContentDetail>> =
            getTVDetails(tvId, forceRefresh, language).map { result: Result<TMDbTVResponse> ->
                when (result) {
                    is Result.Success -> {
                        val contentDetail =
                            result.data?.let { tvResponse ->
                                // Map TV show data without external IDs for faster loading
                                // External IDs will be fetched on-demand at episode level when needed for source scraping
                                android.util.Log.d("TMDbTVRepository", "Creating TV content detail without external IDs for faster loading")
                                contentDetailMapper.mapTVToContentDetail(tvResponse)
                            }
                        Result.Success(contentDetail) as Result<ContentDetail>
                    }
                    is Result.Error -> Result.Error(result.exception) as Result<ContentDetail>
                    is Result.Loading -> Result.Loading as Result<ContentDetail>
                }
            }

        override fun getTVCredits(
            tvId: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<TMDbCreditsResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbSearchDao.getCredits(tvId, "tv").map { it?.toCreditsResponse() ?: TMDbCreditsResponse() }
                },
                shouldFetch = { cachedCredits ->
                    forceRefresh || cachedCredits == null || shouldRefreshCache(tvId, "credits")
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getTVCredits(tvId, language))
                },
                saveCallResult = { creditsResponse ->
                    tmdbSearchDao.insertCredits(creditsResponse.toEntity(tvId, "tv"))
                },
            )

        override fun getTVRecommendations(
            tvId: Int,
            page: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<TMDbRecommendationsResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbSearchDao.getRecommendations(tvId, "tv", "recommendations", page)
                        .map { it?.toRecommendationsResponse() ?: TMDbRecommendationsResponse() }
                },
                shouldFetch = { cachedRecommendations ->
                    forceRefresh || cachedRecommendations == null || shouldRefreshCache(tvId, "recommendations")
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getTVRecommendations(tvId, language, page))
                },
                saveCallResult = { recommendationsResponse ->
                    tmdbSearchDao.insertRecommendations(
                        recommendationsResponse.toEntity(tvId, "tv", "recommendations", page),
                    )
                },
            )

        override fun getSimilarTVShows(
            tvId: Int,
            page: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<TMDbRecommendationsResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbSearchDao.getRecommendations(tvId, "tv", "similar", page)
                        .map { it?.toRecommendationsResponse() ?: TMDbRecommendationsResponse() }
                },
                shouldFetch = { cachedSimilar ->
                    forceRefresh || cachedSimilar == null || shouldRefreshCache(tvId, "similar")
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getSimilarTVShows(tvId, language, page))
                },
                saveCallResult = { similarResponse ->
                    tmdbSearchDao.insertRecommendations(
                        similarResponse.toEntity(tvId, "tv", "similar", page),
                    )
                },
            )

        override fun getTVImages(
            tvId: Int,
            forceRefresh: Boolean,
            includeImageLanguage: String?,
        ): Flow<Result<TMDbTVImagesResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbSearchDao.getImages(tvId, "tv").map { it?.toTVImagesResponse() ?: TMDbTVImagesResponse() }
                },
                shouldFetch = { cachedImages ->
                    forceRefresh || cachedImages == null || shouldRefreshCache(tvId, "images")
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getTVImages(tvId, includeImageLanguage))
                },
                saveCallResult = { imagesResponse ->
                    tmdbSearchDao.insertImages(imagesResponse.toEntity(tvId, "tv"))
                },
            )

        /**
         * Helper function to convert Call<ApiResponse<T>> to suspend function result
         * Compatible with safeCall pattern used by NetworkBoundResource
         */
        private suspend inline fun <reified T> executeApiCall(call: retrofit2.Call<ApiResponse<T>>): T {
            return suspendCancellableCoroutine { continuation ->
                call.enqueue(
                    object : retrofit2.Callback<ApiResponse<T>> {
                        override fun onResponse(
                            call: retrofit2.Call<ApiResponse<T>>,
                            response: retrofit2.Response<ApiResponse<T>>,
                        ) {
                            if (response.isSuccessful) {
                                val apiResponse = response.body()
                                when (apiResponse) {
                                    is ApiResponse.Success -> continuation.resume(apiResponse.data)
                                    is ApiResponse.Error -> continuation.resumeWithException(apiResponse.exception)
                                    is ApiResponse.Loading -> continuation.resumeWithException(Exception("Unexpected loading state"))
                                    null -> continuation.resumeWithException(Exception("Response body was null"))
                                }
                            } else {
                                continuation.resumeWithException(
                                    ApiException.HttpException(
                                        code = response.code(),
                                        message = response.message(),
                                        body = response.errorBody()?.string(),
                                    ),
                                )
                            }
                        }

                        override fun onFailure(
                            call: retrofit2.Call<ApiResponse<T>>,
                            t: Throwable,
                        ) {
                            continuation.resumeWithException(t)
                        }
                    },
                )

                continuation.invokeOnCancellation {
                    call.cancel()
                }
            }
        }

        /**
         * Helper function to convert Call<ApiResponse<T>> to suspend function result
         */
        private suspend inline fun <reified T> awaitApiResponse(call: retrofit2.Call<ApiResponse<T>>): T {
            return executeApiCall(call)
        }

        override fun getTVVideos(
            tvId: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<TMDbTVVideosResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbSearchDao.getVideos(tvId, "tv").map { it?.toTVVideosResponse() ?: TMDbTVVideosResponse() }
                },
                shouldFetch = { cachedVideos ->
                    forceRefresh || cachedVideos == null || shouldRefreshCache(tvId, "videos")
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getTVVideos(tvId, language))
                },
                saveCallResult = { videosResponse ->
                    tmdbSearchDao.insertVideos(videosResponse.toEntity(tvId, "tv"))
                },
            )

        override fun getTVExternalIds(
            tvId: Int,
            forceRefresh: Boolean,
        ): Flow<Result<TMDbExternalIdsResponse>> =
            networkBoundResource(
                loadFromDb = {
                    // Load from TV show cache - external IDs are now cached in TMDbTVEntity
                    tmdbTVDao.getTVShowById(tvId).map { tvEntity ->
                        tvEntity?.let {
                            TMDbExternalIdsResponse(
                                imdbId = it.imdbId,
                                tvdbId = it.tvdbId?.toIntOrNull(),
                                facebookId = it.facebookId,
                                instagramId = it.instagramId,
                                twitterId = it.twitterId,
                            )
                        } ?: TMDbExternalIdsResponse()
                    }
                },
                shouldFetch = { cachedExternalIds ->
                    forceRefresh || cachedExternalIds == null || cachedExternalIds.imdbId.isNullOrEmpty() ||
                        shouldRefreshCache(tvId, "tv_external_ids")
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getTVExternalIds(tvId))
                },
                saveCallResult = { externalIdsResponse ->
                    // Update the existing TV show entity with external IDs
                    val existingTV = tmdbTVDao.getTVShowByIdSuspend(tvId)
                    if (existingTV != null) {
                        val updatedTV =
                            existingTV.copy(
                                imdbId = externalIdsResponse.imdbId,
                                tvdbId = externalIdsResponse.tvdbId?.toString(),
                                facebookId = externalIdsResponse.facebookId,
                                instagramId = externalIdsResponse.instagramId,
                                twitterId = externalIdsResponse.twitterId,
                                lastUpdated = System.currentTimeMillis(),
                            )
                        tmdbTVDao.insertTVShow(updatedTV)
                    }
                },
            )

        override fun getSeasonDetails(
            tvId: Int,
            seasonNumber: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<TMDbSeasonResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbTVDao.getTVShowById(tvId).map { tvEntity ->
                        tvEntity?.toTVResponse()?.seasons?.find { it.seasonNumber == seasonNumber }
                            ?: TMDbSeasonResponse() // Return empty season when not found
                    }
                },
                shouldFetch = { cachedSeason ->
                    forceRefresh || cachedSeason == null || cachedSeason.id == 0 ||
                        isSeasonDataInvalid(cachedSeason) || isSeasonDataStale(cachedSeason)
                },
                createCall = {
                    executeApiCall(tmdbTVService.getSeasonDetails(tvId, seasonNumber, language))
                },
                saveCallResult = { seasonResponse ->
                    updateSeasonInDatabase(tvId, seasonNumber, seasonResponse)
                },
            )

        override fun getEpisodeDetails(
            tvId: Int,
            seasonNumber: Int,
            episodeNumber: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<TMDbEpisodeResponse>> =
            networkBoundResource(
                loadFromDb = {
                    // Try to load episode from cached season data
                    tmdbTVDao.getTVShowById(tvId).map { tvEntity ->
                        tvEntity?.toTVResponse()?.seasons
                            ?.find { it.seasonNumber == seasonNumber }
                            ?.episodes
                            ?.find { it.episodeNumber == episodeNumber }
                            ?: TMDbEpisodeResponse()
                    }
                },
                shouldFetch = { cachedEpisode ->
                    // Always fetch episode details for complete information
                    forceRefresh || cachedEpisode?.id == 0
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getEpisodeDetails(tvId, seasonNumber, episodeNumber, language))
                },
                saveCallResult = { episodeResponse ->
                    // Update the TV show entity with updated episode information
                    val existingTV = tmdbTVDao.getTVShowByIdSuspend(tvId)
                    if (existingTV != null) {
                        val updatedSeasons =
                            existingTV.toTVResponse().seasons.map { season ->
                                if (season.seasonNumber == seasonNumber) {
                                    val updatedEpisodes =
                                        if (season.episodes.any { it.episodeNumber == episodeNumber }) {
                                            season.episodes.map { episode ->
                                                if (episode.episodeNumber == episodeNumber) {
                                                    episodeResponse
                                                } else {
                                                    episode
                                                }
                                            }
                                        } else {
                                            season.episodes + episodeResponse
                                        }
                                    season.copy(episodes = updatedEpisodes)
                                } else {
                                    season
                                }
                            }
                        val updatedTV = existingTV.toTVResponse().copy(seasons = updatedSeasons)
                        tmdbTVDao.insertTVShow(updatedTV.toEntity())
                    }
                },
            )

        override fun getEpisodeContentDetail(
            tvId: Int,
            seasonNumber: Int,
            episodeNumber: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<ContentDetail>> =
            getEpisodeDetails(tvId, seasonNumber, episodeNumber, forceRefresh, language).map { result ->
                when (result) {
                    is Result.Success -> {
                        val episode = result.data
                        if (episode != null && episode.id != 0) {
                            // Get the parent TV show for additional metadata
                            val tvShow = tmdbTVDao.getTVShowByIdSuspend(tvId)?.toTVResponse()

                            // Fetch episode external IDs for enhanced source scraping
                            val episodeExternalIds =
                                try {
                                    runBlocking {
                                        val externalIdsResult =
                                            getEpisodeExternalIds(
                                                tvId,
                                                seasonNumber,
                                                episodeNumber,
                                                forceRefresh,
                                            ).first()
                                        when (externalIdsResult) {
                                            is Result.Success -> {
                                                android.util.Log.d(
                                                    "TMDbTVRepository",
                                                    "Episode external IDs fetched: IMDb=${externalIdsResult.data?.imdbId}",
                                                )
                                                externalIdsResult.data
                                            }
                                            is Result.Error -> {
                                                android.util.Log.w(
                                                    "TMDbTVRepository",
                                                    "Episode external IDs fetch failed: ${externalIdsResult.exception?.message}",
                                                )
                                                null
                                            }
                                            is Result.Loading -> null
                                        }
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.w("TMDbTVRepository", "Exception fetching episode external IDs: ${e.message}")
                                    null
                                }

                            Result.Success(
                                TMDbEpisodeContentDetail(
                                    tmdbEpisode = episode,
                                    tmdbTV = tvShow,
                                    episodeExternalIds = episodeExternalIds,
                                ),
                            )
                        } else {
                            Result.Error(Exception("Episode not found"))
                        }
                    }
                    is Result.Error -> Result.Error(result.exception)
                    is Result.Loading -> Result.Loading
                }
            }

        override fun getEpisodeExternalIds(
            tvId: Int,
            seasonNumber: Int,
            episodeNumber: Int,
            forceRefresh: Boolean,
        ): Flow<Result<TMDbExternalIdsResponse>> =
            networkBoundResource(
                loadFromDb = {
                    // Load from episode external IDs cache
                    val episodeId = "${tvId}_${seasonNumber}_$episodeNumber"
                    tmdbSearchDao.getEpisodeExternalIds(episodeId).map { cached ->
                        cached?.let {
                            TMDbExternalIdsResponse(
                                imdbId = it.imdbId,
                                tvdbId = it.tvdbId?.toIntOrNull(),
                                facebookId = it.facebookId,
                                instagramId = it.instagramId,
                                twitterId = it.twitterId,
                            )
                        } ?: TMDbExternalIdsResponse()
                    }
                },
                shouldFetch = { cachedExternalIds ->
                    forceRefresh || cachedExternalIds == null || cachedExternalIds.imdbId.isNullOrEmpty() ||
                        runBlocking { shouldRefreshEpisodeExternalIds(tvId, seasonNumber, episodeNumber) }
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getEpisodeExternalIds(tvId, seasonNumber, episodeNumber))
                },
                saveCallResult = { externalIdsResponse ->
                    // Cache episode external IDs
                    val episodeId = "${tvId}_${seasonNumber}_$episodeNumber"
                    val entityToCache =
                        TMDbEpisodeExternalIdsEntity(
                            id = episodeId,
                            tvId = tvId,
                            seasonNumber = seasonNumber,
                            episodeNumber = episodeNumber,
                            imdbId = externalIdsResponse.imdbId,
                            tvdbId = externalIdsResponse.tvdbId?.toString(),
                            facebookId = externalIdsResponse.facebookId,
                            instagramId = externalIdsResponse.instagramId,
                            twitterId = externalIdsResponse.twitterId,
                        )
                    tmdbSearchDao.insertEpisodeExternalIds(entityToCache)
                },
            )

        override fun getPopularTVShows(
            page: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<TMDbRecommendationsResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbSearchDao.getRecommendations(0, "tv", "popular", page)
                        .map { it?.toRecommendationsResponse() ?: TMDbRecommendationsResponse() }
                },
                shouldFetch = { cachedPopular ->
                    forceRefresh || cachedPopular == null || shouldRefreshCache(0, "popular")
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getPopularTVShows(language, page))
                },
                saveCallResult = { popularResponse ->
                    tmdbSearchDao.insertRecommendations(
                        popularResponse.toEntity(0, "tv", "popular", page),
                    )
                },
            )

        override fun getTopRatedTVShows(
            page: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<TMDbRecommendationsResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbSearchDao.getRecommendations(0, "tv", "top_rated", page)
                        .map { it?.toRecommendationsResponse() ?: TMDbRecommendationsResponse() }
                },
                shouldFetch = { cachedTopRated ->
                    forceRefresh || cachedTopRated == null || shouldRefreshCache(0, "top_rated")
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getTopRatedTVShows(language, page))
                },
                saveCallResult = { topRatedResponse ->
                    tmdbSearchDao.insertRecommendations(
                        topRatedResponse.toEntity(0, "tv", "top_rated", page),
                    )
                },
            )

        override fun getAiringTodayTVShows(
            page: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<TMDbRecommendationsResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbSearchDao.getRecommendations(0, "tv", "airing_today", page)
                        .map { it?.toRecommendationsResponse() ?: TMDbRecommendationsResponse() }
                },
                shouldFetch = { cachedAiringToday ->
                    forceRefresh || cachedAiringToday == null || shouldRefreshCache(0, "airing_today")
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getAiringTodayTVShows(language, page))
                },
                saveCallResult = { airingTodayResponse ->
                    tmdbSearchDao.insertRecommendations(
                        airingTodayResponse.toEntity(0, "tv", "airing_today", page),
                    )
                },
            )

        override fun getOnTheAirTVShows(
            page: Int,
            forceRefresh: Boolean,
            language: String,
        ): Flow<Result<TMDbRecommendationsResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbSearchDao.getRecommendations(0, "tv", "on_the_air", page)
                        .map { it?.toRecommendationsResponse() ?: TMDbRecommendationsResponse() }
                },
                shouldFetch = { cachedOnTheAir ->
                    forceRefresh || cachedOnTheAir == null || shouldRefreshCache(0, "on_the_air")
                },
                createCall = {
                    awaitApiResponse(tmdbTVService.getOnTheAirTVShows(language, page))
                },
                saveCallResult = { onTheAirResponse ->
                    tmdbSearchDao.insertRecommendations(
                        onTheAirResponse.toEntity(0, "tv", "on_the_air", page),
                    )
                },
            )

        override fun searchTVShows(
            query: String,
            page: Int,
            language: String,
            includeAdult: Boolean,
            firstAirDateYear: Int?,
        ): Flow<Result<TMDbSearchResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbSearchDao.getSearchResults(query, "tv", page)
                        .map { it?.toSearchResponse() ?: TMDbSearchResponse() }
                },
                shouldFetch = { cachedSearch ->
                    // Always fetch search results as they can change frequently
                    true
                },
                createCall = {
                    // For now, we'll return empty results for search as TMDbSearchService is not implemented
                    // In a real implementation, you'd have a proper search endpoint
                    TMDbSearchResponse(page = page, totalPages = 1, totalResults = 0, results = emptyList())
                },
                saveCallResult = { searchResponse ->
                    val searchId = "$query-$page-tv"
                    tmdbSearchDao.insertSearchResult((searchResponse as TMDbSearchResponse).toEntity(searchId, query, page, "tv"))
                },
            )

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
            withKeywords: String?,
        ): Flow<Result<TMDbSearchResponse>> =
            networkBoundResource(
                loadFromDb = {
                    // Create a unique cache key based on discovery parameters
                    val cacheKey = "discover_tv_${sortBy}_${page}_${firstAirDateYear}_${withGenres}_$withNetworks"
                    tmdbSearchDao.getSearchResults(cacheKey, "tv", page)
                        .map { it?.toSearchResponse() ?: TMDbSearchResponse() }
                },
                shouldFetch = { cachedResults ->
                    // Always fetch discovery results as they can change based on parameters
                    true
                },
                createCall = {
                    // For now, we'll use popular TV shows as a fallback for discovery
                    // In a real implementation, you'd have a separate discover endpoint
                    awaitApiResponse(tmdbTVService.getPopularTVShows(language, page))
                },
                saveCallResult = { searchResponse ->
                    // Convert recommendations response to search response for caching
                    val cacheKey = "discover_tv_${sortBy}_${page}_${firstAirDateYear}_${withGenres}_$withNetworks"
                    val convertedResponse =
                        TMDbSearchResponse(
                            page = searchResponse.page,
                            totalPages = searchResponse.totalPages,
                            totalResults = searchResponse.totalResults,
                            results = searchResponse.results.map { it.toTMDbSearchItemResponse() },
                        )
                    tmdbSearchDao.insertSearchResult(convertedResponse.toEntity(cacheKey, cacheKey, page, "tv"))
                },
            )

        override fun getTrendingTVShows(
            timeWindow: String,
            page: Int,
            language: String,
        ): Flow<Result<TMDbSearchResponse>> =
            networkBoundResource(
                loadFromDb = {
                    tmdbSearchDao.getRecommendations(0, "tv", "trending_$timeWindow", page)
                        .map { it?.toSearchResponse() ?: TMDbSearchResponse() }
                },
                shouldFetch = { cachedTrending ->
                    // Always fetch trending results as they change frequently
                    true
                },
                createCall = {
                    // For now, we'll use popular TV shows as a fallback for trending
                    // In a real implementation, you'd have a separate trending endpoint
                    awaitApiResponse(tmdbTVService.getPopularTVShows(language, page))
                },
                saveCallResult = { trendingResponse ->
                    tmdbSearchDao.insertRecommendations(
                        trendingResponse.toEntity(0, "tv", "trending_$timeWindow", page),
                    )
                },
            )

        override suspend fun clearCache() {
            tmdbTVDao.deleteAllTVShows()
            tmdbSearchDao.deleteAllSearchResults()
            tmdbSearchDao.deleteAllCredits()
            tmdbSearchDao.deleteAllRecommendations()
            tmdbSearchDao.deleteAllImages()
            tmdbSearchDao.deleteAllVideos()
        }

        override suspend fun clearTVCache(tvId: Int) {
            tmdbTVDao.deleteTVShowById(tvId)
            tmdbSearchDao.deleteCredits(tvId, "tv")
            tmdbSearchDao.deleteRecommendations(tvId, "tv", "recommendations")
            tmdbSearchDao.deleteRecommendations(tvId, "tv", "similar")
            tmdbSearchDao.deleteImages(tvId, "tv")
            tmdbSearchDao.deleteVideos(tvId, "tv")
        }

        override suspend fun clearSeasonCache(
            tvId: Int,
            seasonNumber: Int,
        ) {
            // Clear the specific season data by refreshing the TV show without that season's detailed data
            val existingTV = tmdbTVDao.getTVShowByIdSuspend(tvId)
            if (existingTV != null) {
                val updatedSeasons =
                    existingTV.toTVResponse().seasons.map { season ->
                        if (season.seasonNumber == seasonNumber) {
                            // Reset to basic season info without episodes
                            season.copy(episodes = emptyList())
                        } else {
                            season
                        }
                    }
                val updatedTV = existingTV.toTVResponse().copy(seasons = updatedSeasons)
                tmdbTVDao.insertTVShow(updatedTV.toEntity())
            }
        }

        override suspend fun clearEpisodeCache(
            tvId: Int,
            seasonNumber: Int,
            episodeNumber: Int,
        ) {
            // Clear the specific episode data
            val existingTV = tmdbTVDao.getTVShowByIdSuspend(tvId)
            if (existingTV != null) {
                val updatedSeasons =
                    existingTV.toTVResponse().seasons.map { season ->
                        if (season.seasonNumber == seasonNumber) {
                            val updatedEpisodes = season.episodes.filterNot { it.episodeNumber == episodeNumber }
                            season.copy(episodes = updatedEpisodes)
                        } else {
                            season
                        }
                    }
                val updatedTV = existingTV.toTVResponse().copy(seasons = updatedSeasons)
                tmdbTVDao.insertTVShow(updatedTV.toEntity())
            }
        }

        // Helper methods

        private fun shouldRefreshCache(
            contentId: Int,
            type: String,
        ): Boolean {
            // Check cache timestamp based on content type
            return when (type) {
                "tv" -> {
                    val lastUpdated = runBlocking { tmdbTVDao.getTVShowLastUpdated(contentId) }
                    lastUpdated == null || (System.currentTimeMillis() - lastUpdated) > CACHE_TIMEOUT_MS
                }
                "credits", "recommendations", "similar", "images", "videos" -> {
                    // For other content types, we can't easily check timestamps without fetching
                    // So we'll use a more conservative approach
                    true
                }
                "popular", "top_rated", "airing_today", "on_the_air" -> {
                    // For discovery endpoints, always refresh as they change frequently
                    true
                }
                else -> true
            }
        }

        /**
         * Helper methods for optimized season caching strategy
         */

        /**
         * Check if cached season data is invalid and needs to be refetched
         */
        private fun isSeasonDataInvalid(cachedSeason: TMDbSeasonResponse?): Boolean {
            return cachedSeason != null && (cachedSeason.id > 0 && cachedSeason.episodeCount > 0 && cachedSeason.episodes.isEmpty())
        }

        /**
         * Check if cached season data is stale based on cache timeout and data completeness
         * Uses multiple heuristics to determine if the cached data needs refreshing
         */
        private fun isSeasonDataStale(cachedSeason: TMDbSeasonResponse?): Boolean {
            if (cachedSeason == null) return false
            // Primary check: if episodes are missing when they should exist
            if (cachedSeason.episodes.isEmpty() && cachedSeason.episodeCount > 0) {
                return true
            }

            // Secondary check: if episode count doesn't match actual episodes
            if (cachedSeason.episodeCount > 0 &&
                cachedSeason.episodes.size != cachedSeason.episodeCount
            ) {
                return true
            }

            // Tertiary check: if episodes have incomplete data (basic validation)
            if (cachedSeason.episodes.isNotEmpty() &&
                cachedSeason.episodes.any { it.id == 0 || it.name.isBlank() }
            ) {
                return true
            }

            return false
        }

        /**
         * Optimized database update for season data
         */
        private suspend fun updateSeasonInDatabase(
            tvId: Int,
            seasonNumber: Int,
            seasonResponse: TMDbSeasonResponse,
        ) {
            val existingTV = tmdbTVDao.getTVShowByIdSuspend(tvId)
            if (existingTV != null) {
                // Only update the specific season that changed
                val existingTVResponse = existingTV.toTVResponse()
                val seasonIndex = existingTVResponse.seasons.indexOfFirst { it.seasonNumber == seasonNumber }

                if (seasonIndex != -1) {
                    // Update existing season
                    val updatedSeasons = existingTVResponse.seasons.toMutableList()
                    updatedSeasons[seasonIndex] = seasonResponse
                    val updatedTV = existingTVResponse.copy(seasons = updatedSeasons)
                    tmdbTVDao.insertTVShow(updatedTV.toEntity())
                } else {
                    // Add new season to the list
                    val updatedSeasons = existingTVResponse.seasons + seasonResponse
                    val updatedTV = existingTVResponse.copy(seasons = updatedSeasons.sortedBy { it.seasonNumber })
                    tmdbTVDao.insertTVShow(updatedTV.toEntity())
                }
            }
            // Note: We don't create a new TV show entry if one doesn't exist,
            // as season details should only be fetched for existing shows
        }

        /**
         * Enhanced cache invalidation methods
         */

        /**
         * Invalidate specific season cache if it becomes stale or invalid
         */
        suspend fun invalidateSeasonCacheIfNeeded(
            tvId: Int,
            seasonNumber: Int,
        ) {
            val existingTV = tmdbTVDao.getTVShowByIdSuspend(tvId)
            if (existingTV != null) {
                val cachedSeason = existingTV.toTVResponse().seasons.find { it.seasonNumber == seasonNumber }
                if (cachedSeason != null && (isSeasonDataInvalid(cachedSeason) || isSeasonDataStale(cachedSeason))) {
                    clearSeasonCache(tvId, seasonNumber)
                }
            }
        }

        /**
         * Validate and potentially invalidate all seasons for a TV show
         */
        suspend fun validateAndInvalidateAllSeasons(tvId: Int) {
            val existingTV = tmdbTVDao.getTVShowByIdSuspend(tvId)
            if (existingTV != null) {
                val tvResponse = existingTV.toTVResponse()
                val invalidSeasons =
                    tvResponse.seasons.filter { season ->
                        isSeasonDataInvalid(season) || isSeasonDataStale(season)
                    }

                if (invalidSeasons.isNotEmpty()) {
                    invalidSeasons.forEach { season ->
                        clearSeasonCache(tvId, season.seasonNumber)
                    }
                }
            }
        }

        /**
         * Check if episode external IDs need refresh
         * @param tvId TMDb TV show ID
         * @param seasonNumber Season number
         * @param episodeNumber Episode number
         * @return True if episode external IDs need refresh
         */
        private suspend fun shouldRefreshEpisodeExternalIds(
            tvId: Int,
            seasonNumber: Int,
            episodeNumber: Int,
        ): Boolean {
            val episodeId = "${tvId}_${seasonNumber}_$episodeNumber"
            val lastUpdated = tmdbSearchDao.getEpisodeExternalIdsLastUpdated(episodeId)
            return lastUpdated == null || (System.currentTimeMillis() - lastUpdated) > CACHE_TIMEOUT_MS
        }
    }
