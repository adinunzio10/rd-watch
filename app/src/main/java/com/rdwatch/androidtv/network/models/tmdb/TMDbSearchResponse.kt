package com.rdwatch.androidtv.network.models.tmdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * TMDb Search response model for paginated search results
 */
@JsonClass(generateAdapter = true)
data class TMDbSearchResponse(
    @Json(name = "page") val page: Int = 1,
    @Json(name = "results") val results: List<TMDbSearchResultResponse> = emptyList(),
    @Json(name = "total_pages") val totalPages: Int = 0,
    @Json(name = "total_results") val totalResults: Int = 0
)

/**
 * TMDb Multi Search response model for search across multiple types
 */
@JsonClass(generateAdapter = true)
data class TMDbMultiSearchResponse(
    @Json(name = "page") val page: Int = 1,
    @Json(name = "results") val results: List<TMDbMultiSearchResultResponse> = emptyList(),
    @Json(name = "total_pages") val totalPages: Int = 0,
    @Json(name = "total_results") val totalResults: Int = 0
)

/**
 * TMDb Search Result response model (for movie/tv searches)
 */
@JsonClass(generateAdapter = true)
data class TMDbSearchResultResponse(
    @Json(name = "adult") val adult: Boolean = false,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "genre_ids") val genreIds: List<Int> = emptyList(),
    @Json(name = "id") val id: Int = 0,
    @Json(name = "original_language") val originalLanguage: String = "",
    @Json(name = "overview") val overview: String = "",
    @Json(name = "popularity") val popularity: Double = 0.0,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "vote_average") val voteAverage: Double = 0.0,
    @Json(name = "vote_count") val voteCount: Int = 0,
    
    // Movie specific fields
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "video") val video: Boolean = false,
    
    // TV Show specific fields
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "origin_country") val originCountry: List<String> = emptyList(),
    @Json(name = "original_name") val originalName: String? = null
)

/**
 * TMDb Multi Search Result response model (for multi-type searches)
 */
@JsonClass(generateAdapter = true)
data class TMDbMultiSearchResultResponse(
    @Json(name = "adult") val adult: Boolean = false,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "genre_ids") val genreIds: List<Int> = emptyList(),
    @Json(name = "id") val id: Int = 0,
    @Json(name = "media_type") val mediaType: String = "",
    @Json(name = "original_language") val originalLanguage: String = "",
    @Json(name = "overview") val overview: String = "",
    @Json(name = "popularity") val popularity: Double = 0.0,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "vote_average") val voteAverage: Double = 0.0,
    @Json(name = "vote_count") val voteCount: Int = 0,
    
    // Movie specific fields
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "video") val video: Boolean = false,
    
    // TV Show specific fields
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "origin_country") val originCountry: List<String> = emptyList(),
    @Json(name = "original_name") val originalName: String? = null,
    
    // Person specific fields
    @Json(name = "gender") val gender: Int = 0,
    @Json(name = "known_for") val knownFor: List<TMDbKnownForResponse> = emptyList(),
    @Json(name = "known_for_department") val knownForDepartment: String = "",
    @Json(name = "profile_path") val profilePath: String? = null
)

/**
 * TMDb Recommendations response model
 */
@JsonClass(generateAdapter = true)
data class TMDbRecommendationsResponse(
    @Json(name = "page") val page: Int = 1,
    @Json(name = "results") val results: List<TMDbRecommendationItemResponse> = emptyList(),
    @Json(name = "total_pages") val totalPages: Int = 0,
    @Json(name = "total_results") val totalResults: Int = 0
)

/**
 * TMDb Recommendation Item response model (alias for search results)
 */
typealias TMDbRecommendationItemResponse = TMDbSearchResultResponse

/**
 * TMDb Search Item response model (alias for search results)
 */
typealias TMDbSearchItemResponse = TMDbSearchResultResponse

/**
 * TMDb Image response model
 */
@JsonClass(generateAdapter = true)
data class TMDbImageResponse(
    @Json(name = "aspect_ratio") val aspectRatio: Double = 0.0,
    @Json(name = "height") val height: Int = 0,
    @Json(name = "iso_639_1") val iso6391: String? = null,
    @Json(name = "file_path") val filePath: String = "",
    @Json(name = "vote_average") val voteAverage: Double = 0.0,
    @Json(name = "vote_count") val voteCount: Int = 0,
    @Json(name = "width") val width: Int = 0
)

/**
 * TMDb Movie Images response model
 */
@JsonClass(generateAdapter = true)
data class TMDbMovieImagesResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "backdrops") val backdrops: List<TMDbImageResponse> = emptyList(),
    @Json(name = "logos") val logos: List<TMDbImageResponse> = emptyList(),
    @Json(name = "posters") val posters: List<TMDbImageResponse> = emptyList()
)

/**
 * TMDb Video response model
 */
@JsonClass(generateAdapter = true)
data class TMDbVideoResponse(
    @Json(name = "iso_639_1") val iso6391: String = "",
    @Json(name = "iso_3166_1") val iso31661: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "key") val key: String = "",
    @Json(name = "site") val site: String = "",
    @Json(name = "size") val size: Int = 0,
    @Json(name = "type") val type: String = "",
    @Json(name = "official") val official: Boolean = false,
    @Json(name = "published_at") val publishedAt: String = "",
    @Json(name = "id") val id: String = ""
)

/**
 * TMDb Movie Videos response model
 */
@JsonClass(generateAdapter = true)
data class TMDbMovieVideosResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "results") val results: List<TMDbVideoResponse> = emptyList()
)