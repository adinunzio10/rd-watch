package com.rdwatch.androidtv.network.api

import com.rdwatch.androidtv.network.models.tmdb.TMDbMultiSearchResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbSearchResponse
import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * TMDb API service interface for search-related endpoints
 * Follows existing ApiService pattern with ApiResponse wrapper
 */
interface TMDbSearchService {
    /**
     * Search for movies
     * @param query Search query string
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     * @param includeAdult Include adult content in results (default: false)
     * @param region Region for release dates and certifications
     * @param year Filter by primary release year
     * @param primaryReleaseYear Filter by primary release year (alternative)
     */
    @GET("search/movie")
    fun searchMovies(
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("region") region: String? = null,
        @Query("year") year: Int? = null,
        @Query("primary_release_year") primaryReleaseYear: Int? = null,
    ): Call<TMDbSearchResponse>

    /**
     * Search for TV shows
     * @param query Search query string
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     * @param includeAdult Include adult content in results (default: false)
     * @param firstAirDateYear Filter by first air date year
     */
    @GET("search/tv")
    fun searchTVShows(
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("first_air_date_year") firstAirDateYear: Int? = null,
    ): Call<TMDbSearchResponse>

    /**
     * Search for people (actors, directors, etc.)
     * @param query Search query string
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     * @param includeAdult Include adult content in results (default: false)
     * @param region Region for release dates and certifications
     */
    @GET("search/person")
    fun searchPeople(
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("region") region: String? = null,
    ): Call<TMDbSearchResponse>

    /**
     * Multi-search across movies, TV shows, and people
     * @param query Search query string
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     * @param includeAdult Include adult content in results (default: false)
     * @param region Region for release dates and certifications
     */
    @GET("search/multi")
    fun multiSearch(
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("region") region: String? = null,
    ): Call<TMDbMultiSearchResponse>

    /**
     * Search for collections
     * @param query Search query string
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     */
    @GET("search/collection")
    fun searchCollections(
        @Query("query") query: String,
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
    ): Call<TMDbSearchResponse>

    /**
     * Search for companies
     * @param query Search query string
     * @param page Page number for pagination (default: 1)
     */
    @GET("search/company")
    fun searchCompanies(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
    ): Call<TMDbSearchResponse>

    /**
     * Search for keywords
     * @param query Search query string
     * @param page Page number for pagination (default: 1)
     */
    @GET("search/keyword")
    fun searchKeywords(
        @Query("query") query: String,
        @Query("page") page: Int = 1,
    ): Call<TMDbSearchResponse>

    /**
     * Get trending content
     * @param mediaType Media type (all, movie, tv, person)
     * @param timeWindow Time window (day, week)
     * @param language Language for the response (default: en-US)
     * @param page Page number for pagination (default: 1)
     */
    @GET("trending/{media_type}/{time_window}")
    fun getTrending(
        @Path("media_type") mediaType: String = "all",
        @Path("time_window") timeWindow: String = "day",
        @Query("language") language: String = "en-US",
        @Query("page") page: Int = 1,
    ): Call<TMDbSearchResponse>

    /**
     * Discover movies with filtering options
     * @param language Language for the response (default: en-US)
     * @param region Region for release dates and certifications
     * @param sortBy Sort results by (popularity.desc, release_date.desc, etc.)
     * @param includeAdult Include adult content in results (default: false)
     * @param includeVideo Include video content in results (default: false)
     * @param page Page number for pagination (default: 1)
     * @param primaryReleaseYear Filter by primary release year
     * @param primaryReleaseDateGte Filter by primary release date (greater than or equal)
     * @param primaryReleaseDateLte Filter by primary release date (less than or equal)
     * @param releaseDateGte Filter by release date (greater than or equal)
     * @param releaseDateLte Filter by release date (less than or equal)
     * @param withReleaseType Filter by release type
     * @param year Filter by year
     * @param voteCountGte Filter by vote count (greater than or equal)
     * @param voteCountLte Filter by vote count (less than or equal)
     * @param voteAverageGte Filter by vote average (greater than or equal)
     * @param voteAverageLte Filter by vote average (less than or equal)
     * @param withCast Filter by cast member IDs
     * @param withCrew Filter by crew member IDs
     * @param withPeople Filter by person IDs
     * @param withCompanies Filter by company IDs
     * @param withGenres Filter by genre IDs
     * @param withoutGenres Exclude genre IDs
     * @param withKeywords Filter by keyword IDs
     * @param withoutKeywords Exclude keyword IDs
     * @param withRuntimeGte Filter by runtime (greater than or equal)
     * @param withRuntimeLte Filter by runtime (less than or equal)
     * @param withOriginalLanguage Filter by original language
     * @param withWatchProviders Filter by watch provider IDs
     * @param watchRegion Watch region for providers
     * @param withWatchMonetizationTypes Filter by monetization types
     * @param withoutCompanies Exclude company IDs
     */
    @GET("discover/movie")
    fun discoverMovies(
        @Query("language") language: String = "en-US",
        @Query("region") region: String? = null,
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("include_adult") includeAdult: Boolean = false,
        @Query("include_video") includeVideo: Boolean = false,
        @Query("page") page: Int = 1,
        @Query("primary_release_year") primaryReleaseYear: Int? = null,
        @Query("primary_release_date.gte") primaryReleaseDateGte: String? = null,
        @Query("primary_release_date.lte") primaryReleaseDateLte: String? = null,
        @Query("release_date.gte") releaseDateGte: String? = null,
        @Query("release_date.lte") releaseDateLte: String? = null,
        @Query("with_release_type") withReleaseType: String? = null,
        @Query("year") year: Int? = null,
        @Query("vote_count.gte") voteCountGte: Int? = null,
        @Query("vote_count.lte") voteCountLte: Int? = null,
        @Query("vote_average.gte") voteAverageGte: Float? = null,
        @Query("vote_average.lte") voteAverageLte: Float? = null,
        @Query("with_cast") withCast: String? = null,
        @Query("with_crew") withCrew: String? = null,
        @Query("with_people") withPeople: String? = null,
        @Query("with_companies") withCompanies: String? = null,
        @Query("with_genres") withGenres: String? = null,
        @Query("without_genres") withoutGenres: String? = null,
        @Query("with_keywords") withKeywords: String? = null,
        @Query("without_keywords") withoutKeywords: String? = null,
        @Query("with_runtime.gte") withRuntimeGte: Int? = null,
        @Query("with_runtime.lte") withRuntimeLte: Int? = null,
        @Query("with_original_language") withOriginalLanguage: String? = null,
        @Query("with_watch_providers") withWatchProviders: String? = null,
        @Query("watch_region") watchRegion: String? = null,
        @Query("with_watch_monetization_types") withWatchMonetizationTypes: String? = null,
        @Query("without_companies") withoutCompanies: String? = null,
    ): Call<TMDbSearchResponse>

    /**
     * Discover TV shows with filtering options
     * @param language Language for the response (default: en-US)
     * @param sortBy Sort results by (popularity.desc, vote_average.desc, etc.)
     * @param airDateGte Filter by air date (greater than or equal)
     * @param airDateLte Filter by air date (less than or equal)
     * @param firstAirDateGte Filter by first air date (greater than or equal)
     * @param firstAirDateLte Filter by first air date (less than or equal)
     * @param firstAirDateYear Filter by first air date year
     * @param page Page number for pagination (default: 1)
     * @param timezone Filter by timezone
     * @param voteAverageGte Filter by vote average (greater than or equal)
     * @param voteCountGte Filter by vote count (greater than or equal)
     * @param withGenres Filter by genre IDs
     * @param withNetworks Filter by network IDs
     * @param withoutGenres Exclude genre IDs
     * @param withRuntimeGte Filter by runtime (greater than or equal)
     * @param withRuntimeLte Filter by runtime (less than or equal)
     * @param includeNullFirstAirDates Include shows with null first air dates
     * @param withOriginalLanguage Filter by original language
     * @param withoutKeywords Exclude keyword IDs
     * @param screendDaily Screen daily content
     * @param withWatchProviders Filter by watch provider IDs
     * @param watchRegion Watch region for providers
     * @param withWatchMonetizationTypes Filter by monetization types
     * @param withStatus Filter by status
     * @param withType Filter by type
     * @param withKeywords Filter by keyword IDs
     */
    @GET("discover/tv")
    fun discoverTVShows(
        @Query("language") language: String = "en-US",
        @Query("sort_by") sortBy: String = "popularity.desc",
        @Query("air_date.gte") airDateGte: String? = null,
        @Query("air_date.lte") airDateLte: String? = null,
        @Query("first_air_date.gte") firstAirDateGte: String? = null,
        @Query("first_air_date.lte") firstAirDateLte: String? = null,
        @Query("first_air_date_year") firstAirDateYear: Int? = null,
        @Query("page") page: Int = 1,
        @Query("timezone") timezone: String? = null,
        @Query("vote_average.gte") voteAverageGte: Float? = null,
        @Query("vote_count.gte") voteCountGte: Int? = null,
        @Query("with_genres") withGenres: String? = null,
        @Query("with_networks") withNetworks: String? = null,
        @Query("without_genres") withoutGenres: String? = null,
        @Query("with_runtime.gte") withRuntimeGte: Int? = null,
        @Query("with_runtime.lte") withRuntimeLte: Int? = null,
        @Query("include_null_first_air_dates") includeNullFirstAirDates: Boolean? = null,
        @Query("with_original_language") withOriginalLanguage: String? = null,
        @Query("without_keywords") withoutKeywords: String? = null,
        @Query("screened_theatrically") screendDaily: Boolean? = null,
        @Query("with_watch_providers") withWatchProviders: String? = null,
        @Query("watch_region") watchRegion: String? = null,
        @Query("with_watch_monetization_types") withWatchMonetizationTypes: String? = null,
        @Query("with_status") withStatus: String? = null,
        @Query("with_type") withType: String? = null,
        @Query("with_keywords") withKeywords: String? = null,
    ): Call<TMDbSearchResponse>
}
