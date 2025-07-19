package com.rdwatch.androidtv.network.models.tmdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * TMDb Credits response model containing cast and crew information
 */
@JsonClass(generateAdapter = true)
data class TMDbCreditsResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "cast") val cast: List<TMDbCastResponse> = emptyList(),
    @Json(name = "crew") val crew: List<TMDbCrewResponse> = emptyList(),
)

/**
 * TMDb Cast response model
 */
@JsonClass(generateAdapter = true)
data class TMDbCastResponse(
    @Json(name = "adult") val adult: Boolean = false,
    @Json(name = "gender") val gender: Int = 0,
    @Json(name = "id") val id: Int = 0,
    @Json(name = "known_for_department") val knownForDepartment: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "original_name") val originalName: String = "",
    @Json(name = "popularity") val popularity: Double = 0.0,
    @Json(name = "profile_path") val profilePath: String? = null,
    @Json(name = "cast_id") val castId: Int = 0,
    @Json(name = "character") val character: String = "",
    @Json(name = "credit_id") val creditId: String = "",
    @Json(name = "order") val order: Int = 0,
)

/**
 * TMDb Crew response model
 */
@JsonClass(generateAdapter = true)
data class TMDbCrewResponse(
    @Json(name = "adult") val adult: Boolean = false,
    @Json(name = "gender") val gender: Int = 0,
    @Json(name = "id") val id: Int = 0,
    @Json(name = "known_for_department") val knownForDepartment: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "original_name") val originalName: String = "",
    @Json(name = "popularity") val popularity: Double = 0.0,
    @Json(name = "profile_path") val profilePath: String? = null,
    @Json(name = "credit_id") val creditId: String = "",
    @Json(name = "department") val department: String = "",
    @Json(name = "job") val job: String = "",
)

/**
 * TMDb Person response model (used in search results)
 */
@JsonClass(generateAdapter = true)
data class TMDbPersonResponse(
    @Json(name = "adult") val adult: Boolean = false,
    @Json(name = "also_known_as") val alsoKnownAs: List<String> = emptyList(),
    @Json(name = "biography") val biography: String = "",
    @Json(name = "birthday") val birthday: String? = null,
    @Json(name = "deathday") val deathday: String? = null,
    @Json(name = "gender") val gender: Int = 0,
    @Json(name = "homepage") val homepage: String? = null,
    @Json(name = "id") val id: Int = 0,
    @Json(name = "imdb_id") val imdbId: String? = null,
    @Json(name = "known_for_department") val knownForDepartment: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "place_of_birth") val placeOfBirth: String? = null,
    @Json(name = "popularity") val popularity: Double = 0.0,
    @Json(name = "profile_path") val profilePath: String? = null,
    @Json(name = "known_for") val knownFor: List<TMDbKnownForResponse> = emptyList(),
)

/**
 * TMDb Known For response model (used in person search)
 */
@JsonClass(generateAdapter = true)
data class TMDbKnownForResponse(
    @Json(name = "adult") val adult: Boolean = false,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "genre_ids") val genreIds: List<Int> = emptyList(),
    @Json(name = "id") val id: Int = 0,
    @Json(name = "media_type") val mediaType: String = "",
    @Json(name = "original_language") val originalLanguage: String = "",
    @Json(name = "original_title") val originalTitle: String? = null,
    @Json(name = "overview") val overview: String = "",
    @Json(name = "popularity") val popularity: Double = 0.0,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "release_date") val releaseDate: String? = null,
    @Json(name = "title") val title: String? = null,
    @Json(name = "video") val video: Boolean = false,
    @Json(name = "vote_average") val voteAverage: Double = 0.0,
    @Json(name = "vote_count") val voteCount: Int = 0,
    // TV Show specific fields
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "name") val name: String? = null,
    @Json(name = "origin_country") val originCountry: List<String> = emptyList(),
    @Json(name = "original_name") val originalName: String? = null,
)

/**
 * Type aliases for mapper compatibility
 */
typealias TMDbCastMemberResponse = TMDbCastResponse
typealias TMDbCrewMemberResponse = TMDbCrewResponse
