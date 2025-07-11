package com.rdwatch.androidtv.network.api

import com.rdwatch.androidtv.network.response.ApiResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbTVResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbCreditsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbRecommendationsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbTVImagesResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbTVVideosResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDb API service interface for TV show-related endpoints
 * Follows existing ApiService pattern with ApiResponse wrapper
 */
interface TMDbTVService {
    
    /**
     * Get TV show details by ID
     * @param tvId TMDb TV show ID
     * @param appendToResponse Additional data to include (optional)
     * @param language Language for the response (default: en-US)
     */
    @GET("tv/{tv_id}")
    fun getTVDetails(
        @Path("tv_id") tvId: Int,
        @Query("append_to_response") appendToResponse: String? = null,
        @Query("language") language: String = "en-US"
    ): Call<ApiResponse<TMDbTVResponse>>
    
    /**
     * Get TV show credits (cast and crew)
     * @param tvId TMDb TV show ID
     * @param language Language for the response (default: en-US)
     */
    @GET("tv/{tv_id}/credits")
    fun getTVCredits(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "en-US"
    ): Call<ApiResponse<TMDbCreditsResponse>>
    
    /**
     * Get TV show recommendations
     * @param tvId TMDb TV show ID
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     */
    @GET("tv/{tv_id}/recommendations")
    fun getTVRecommendations(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): Call<ApiResponse<TMDbRecommendationsResponse>>
    
    /**
     * Get similar TV shows
     * @param tvId TMDb TV show ID
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     */
    @GET("tv/{tv_id}/similar")
    fun getSimilarTVShows(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): Call<ApiResponse<TMDbRecommendationsResponse>>
    
    /**
     * Get TV show images (posters, backdrops, logos)
     * @param tvId TMDb TV show ID
     * @param includeImageLanguage Additional image languages to include
     */
    @GET("tv/{tv_id}/images")
    fun getTVImages(
        @Path("tv_id") tvId: Int,
        @Query("include_image_language") includeImageLanguage: String? = null
    ): Call<ApiResponse<TMDbTVImagesResponse>>
    
    /**
     * Get TV show videos (trailers, teasers, etc.)
     * @param tvId TMDb TV show ID
     * @param language Language for the response (default: en-US)
     */
    @GET("tv/{tv_id}/videos")
    fun getTVVideos(
        @Path("tv_id") tvId: Int,
        @Query("language") language: String = "en-US"
    ): Call<ApiResponse<TMDbTVVideosResponse>>
    
    /**
     * Get TV show season details
     * @param tvId TMDb TV show ID
     * @param seasonNumber Season number
     * @param language Language for the response (default: en-US)
     */
    @GET("tv/{tv_id}/season/{season_number}")
    fun getSeasonDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Query("language") language: String = "en-US"
    ): Call<ApiResponse<TMDbSeasonResponse>>
    
    /**
     * Get TV show episode details
     * @param tvId TMDb TV show ID
     * @param seasonNumber Season number
     * @param episodeNumber Episode number
     * @param language Language for the response (default: en-US)
     */
    @GET("tv/{tv_id}/season/{season_number}/episode/{episode_number}")
    fun getEpisodeDetails(
        @Path("tv_id") tvId: Int,
        @Path("season_number") seasonNumber: Int,
        @Path("episode_number") episodeNumber: Int,
        @Query("language") language: String = "en-US"
    ): Call<ApiResponse<TMDbEpisodeResponse>>
    
    /**
     * Get popular TV shows
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     */
    @GET("tv/popular")
    fun getPopularTVShows(
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): Call<ApiResponse<TMDbRecommendationsResponse>>
    
    /**
     * Get top rated TV shows
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     */
    @GET("tv/top_rated")
    fun getTopRatedTVShows(
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): Call<ApiResponse<TMDbRecommendationsResponse>>
    
    /**
     * Get TV shows airing today
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     */
    @GET("tv/airing_today")
    fun getAiringTodayTVShows(
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): Call<ApiResponse<TMDbRecommendationsResponse>>
    
    /**
     * Get TV shows on the air
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     */
    @GET("tv/on_the_air")
    fun getOnTheAirTVShows(
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): Call<ApiResponse<TMDbRecommendationsResponse>>
}