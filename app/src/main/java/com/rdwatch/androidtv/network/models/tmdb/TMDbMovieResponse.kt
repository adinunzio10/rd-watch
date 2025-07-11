package com.rdwatch.androidtv.network.models.tmdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * TMDb Movie response model matching the API response structure
 */
@JsonClass(generateAdapter = true)
data class TMDbMovieResponse(
    @Json(name = "adult") val adult: Boolean = false,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "belongs_to_collection") val belongsToCollection: TMDbCollectionResponse? = null,
    @Json(name = "budget") val budget: Long = 0,
    @Json(name = "genres") val genres: List<TMDbGenreResponse> = emptyList(),
    @Json(name = "homepage") val homepage: String? = null,
    @Json(name = "id") val id: Int = 0,
    @Json(name = "imdb_id") val imdbId: String? = null,
    @Json(name = "original_language") val originalLanguage: String = "",
    @Json(name = "original_title") val originalTitle: String = "",
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "popularity") val popularity: Double = 0.0,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "production_companies") val productionCompanies: List<TMDbProductionCompanyResponse> = emptyList(),
    @Json(name = "production_countries") val productionCountries: List<TMDbProductionCountryResponse> = emptyList(),
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "revenue") val revenue: Long = 0,
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "spoken_languages") val spokenLanguages: List<TMDbSpokenLanguageResponse> = emptyList(),
    @Json(name = "status") val status: String = "",
    @Json(name = "tagline") val tagline: String? = null,
    @Json(name = "title") val title: String = "",
    @Json(name = "video") val video: Boolean = false,
    @Json(name = "vote_average") val voteAverage: Double = 0.0,
    @Json(name = "vote_count") val voteCount: Int = 0,
    
    // Additional fields for append_to_response
    @Json(name = "credits") val credits: TMDbCreditsResponse? = null,
    @Json(name = "images") val images: TMDbMovieImagesResponse? = null,
    @Json(name = "videos") val videos: TMDbMovieVideosResponse? = null,
    @Json(name = "recommendations") val recommendations: TMDbRecommendationsResponse? = null,
    @Json(name = "similar") val similar: TMDbRecommendationsResponse? = null,
    @Json(name = "keywords") val keywords: TMDbKeywordsResponse? = null,
    @Json(name = "reviews") val reviews: TMDbReviewsResponse? = null,
    @Json(name = "watch/providers") val watchProviders: TMDbWatchProvidersResponse? = null,
    @Json(name = "release_dates") val releaseDates: TMDbReleaseDatesResponse? = null
)

/**
 * TMDb Collection response model
 */
@JsonClass(generateAdapter = true)
data class TMDbCollectionResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "parts") val parts: List<TMDbMovieResponse> = emptyList()
)

/**
 * TMDb Genre response model
 */
@JsonClass(generateAdapter = true)
data class TMDbGenreResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = ""
)

/**
 * TMDb Production Company response model
 */
@JsonClass(generateAdapter = true)
data class TMDbProductionCompanyResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "logo_path") val logoPath: String? = null,
    @Json(name = "name") val name: String = "",
    @Json(name = "origin_country") val originCountry: String = ""
)

/**
 * TMDb Production Country response model
 */
@JsonClass(generateAdapter = true)
data class TMDbProductionCountryResponse(
    @Json(name = "iso_3166_1") val iso31661: String = "",
    @Json(name = "name") val name: String = ""
)

/**
 * TMDb Spoken Language response model
 */
@JsonClass(generateAdapter = true)
data class TMDbSpokenLanguageResponse(
    @Json(name = "english_name") val englishName: String = "",
    @Json(name = "iso_639_1") val iso6391: String = "",
    @Json(name = "name") val name: String = ""
)

/**
 * TMDb Keywords response model
 */
@JsonClass(generateAdapter = true)
data class TMDbKeywordsResponse(
    @Json(name = "keywords") val keywords: List<TMDbKeywordResponse> = emptyList()
)

/**
 * TMDb Keyword response model
 */
@JsonClass(generateAdapter = true)
data class TMDbKeywordResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = ""
)

/**
 * TMDb Reviews response model
 */
@JsonClass(generateAdapter = true)
data class TMDbReviewsResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "page") val page: Int = 1,
    @Json(name = "results") val results: List<TMDbReviewResponse> = emptyList(),
    @Json(name = "total_pages") val totalPages: Int = 0,
    @Json(name = "total_results") val totalResults: Int = 0
)

/**
 * TMDb Review response model
 */
@JsonClass(generateAdapter = true)
data class TMDbReviewResponse(
    @Json(name = "author") val author: String = "",
    @Json(name = "author_details") val authorDetails: TMDbAuthorDetailsResponse? = null,
    @Json(name = "content") val content: String = "",
    @Json(name = "created_at") val createdAt: String = "",
    @Json(name = "id") val id: String = "",
    @Json(name = "updated_at") val updatedAt: String = "",
    @Json(name = "url") val url: String = ""
)

/**
 * TMDb Author Details response model
 */
@JsonClass(generateAdapter = true)
data class TMDbAuthorDetailsResponse(
    @Json(name = "name") val name: String = "",
    @Json(name = "username") val username: String = "",
    @Json(name = "avatar_path") val avatarPath: String? = null,
    @Json(name = "rating") val rating: Double? = null
)

/**
 * TMDb Release Dates response model
 */
@JsonClass(generateAdapter = true)
data class TMDbReleaseDatesResponse(
    @Json(name = "results") val results: List<TMDbReleaseDateInfoResponse> = emptyList()
)

/**
 * TMDb Release Date Info response model
 */
@JsonClass(generateAdapter = true)
data class TMDbReleaseDateInfoResponse(
    @Json(name = "iso_3166_1") val iso31661: String = "",
    @Json(name = "release_dates") val releaseDates: List<TMDbReleaseDateResponse> = emptyList()
)

/**
 * TMDb Release Date response model
 */
@JsonClass(generateAdapter = true)
data class TMDbReleaseDateResponse(
    @Json(name = "certification") val certification: String = "",
    @Json(name = "iso_639_1") val iso6391: String = "",
    @Json(name = "note") val note: String = "",
    @Json(name = "release_date") val releaseDate: String = "",
    @Json(name = "type") val type: Int = 0
)

/**
 * TMDb Watch Providers response model
 */
@JsonClass(generateAdapter = true)
data class TMDbWatchProvidersResponse(
    @Json(name = "results") val results: Map<String, TMDbWatchProvidersByRegionResponse> = emptyMap()
)

/**
 * TMDb Watch Providers by Region response model
 */
@JsonClass(generateAdapter = true)
data class TMDbWatchProvidersByRegionResponse(
    @Json(name = "link") val link: String = "",
    @Json(name = "rent") val rent: List<TMDbWatchProviderResponse> = emptyList(),
    @Json(name = "buy") val buy: List<TMDbWatchProviderResponse> = emptyList(),
    @Json(name = "flatrate") val flatrate: List<TMDbWatchProviderResponse> = emptyList()
)

/**
 * TMDb Watch Provider response model
 */
@JsonClass(generateAdapter = true)
data class TMDbWatchProviderResponse(
    @Json(name = "display_priority") val displayPriority: Int = 0,
    @Json(name = "logo_path") val logoPath: String = "",
    @Json(name = "provider_id") val providerId: Int = 0,
    @Json(name = "provider_name") val providerName: String = ""
)