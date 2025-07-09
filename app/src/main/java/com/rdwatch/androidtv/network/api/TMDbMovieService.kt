package com.rdwatch.androidtv.network.api

import com.rdwatch.androidtv.network.response.ApiResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbCreditsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbRecommendationsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbSearchResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieImagesResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieVideosResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDb API service interface for movie-related endpoints
 * Follows existing ApiService pattern with ApiResponse wrapper
 */
interface TMDbMovieService {
    
    companion object {
        const val BASE_URL = "https://api.themoviedb.org/3/"
        const val IMAGE_BASE_URL = "https://image.tmdb.org/t/p/"
        const val BACKDROP_SIZE = "w1280"
        const val POSTER_SIZE = "w500"
        const val PROFILE_SIZE = "w185"
    }
    
    /**
     * Get movie details by ID
     * @param movieId TMDb movie ID
     * @param appendToResponse Additional data to include (optional)
     * @param language Language for the response (default: en-US)
     */
    @GET("movie/{movie_id}")
    fun getMovieDetails(
        @Path("movie_id") movieId: Int,
        @Query("append_to_response") appendToResponse: String? = null,
        @Query("language") language: String = "en-US"
    ): Call<ApiResponse<TMDbMovieResponse>>
    
    /**
     * Get movie credits (cast and crew)
     * @param movieId TMDb movie ID
     * @param language Language for the response (default: en-US)
     */
    @GET("movie/{movie_id}/credits")
    fun getMovieCredits(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en-US"
    ): Call<ApiResponse<TMDbCreditsResponse>>
    
    /**
     * Get movie recommendations
     * @param movieId TMDb movie ID
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     */
    @GET("movie/{movie_id}/recommendations")
    fun getMovieRecommendations(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): Call<ApiResponse<TMDbRecommendationsResponse>>
    
    /**
     * Get similar movies
     * @param movieId TMDb movie ID
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     */
    @GET("movie/{movie_id}/similar")
    fun getSimilarMovies(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1
    ): Call<ApiResponse<TMDbRecommendationsResponse>>
    
    /**
     * Get movie images (posters, backdrops, logos)
     * @param movieId TMDb movie ID
     * @param includeImageLanguage Additional image languages to include
     */
    @GET("movie/{movie_id}/images")
    fun getMovieImages(
        @Path("movie_id") movieId: Int,
        @Query("include_image_language") includeImageLanguage: String? = null
    ): Call<ApiResponse<TMDbMovieImagesResponse>>
    
    /**
     * Get movie videos (trailers, teasers, etc.)
     * @param movieId TMDb movie ID
     * @param language Language for the response (default: en-US)
     */
    @GET("movie/{movie_id}/videos")
    fun getMovieVideos(
        @Path("movie_id") movieId: Int,
        @Query("language") language: String = "en-US"
    ): Call<ApiResponse<TMDbMovieVideosResponse>>
    
    /**
     * Get popular movies
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     * @param region Region for release dates and certifications
     */
    @GET("movie/popular")
    fun getPopularMovies(
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null
    ): Call<ApiResponse<TMDbRecommendationsResponse>>
    
    /**
     * Get top rated movies
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     * @param region Region for release dates and certifications
     */
    @GET("movie/top_rated")
    fun getTopRatedMovies(
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null
    ): Call<ApiResponse<TMDbRecommendationsResponse>>
    
    /**
     * Get now playing movies
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     * @param region Region for release dates and certifications
     */
    @GET("movie/now_playing")
    fun getNowPlayingMovies(
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null
    ): Call<ApiResponse<TMDbRecommendationsResponse>>
    
    /**
     * Get upcoming movies
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     * @param region Region for release dates and certifications
     */
    @GET("movie/upcoming")
    fun getUpcomingMovies(
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("region") region: String? = null
    ): Call<ApiResponse<TMDbRecommendationsResponse>>
}