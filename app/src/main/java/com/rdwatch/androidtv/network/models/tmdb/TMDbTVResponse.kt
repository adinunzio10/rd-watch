package com.rdwatch.androidtv.network.models.tmdb

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

/**
 * TMDb TV Show response model matching the API response structure
 */
@JsonClass(generateAdapter = true)
data class TMDbTVResponse(
    @Json(name = "adult") val adult: Boolean = false,
    @Json(name = "backdrop_path") val backdropPath: String? = null,
    @Json(name = "created_by") val createdBy: List<TMDbCreatedByResponse> = emptyList(),
    @Json(name = "episode_run_time") val episodeRunTime: List<Int> = emptyList(),
    @Json(name = "first_air_date") val firstAirDate: String? = null,
    @Json(name = "genres") val genres: List<TMDbGenreResponse> = emptyList(),
    @Json(name = "homepage") val homepage: String? = null,
    @Json(name = "id") val id: Int = 0,
    @Json(name = "in_production") val inProduction: Boolean = false,
    @Json(name = "languages") val languages: List<String> = emptyList(),
    @Json(name = "last_air_date") val lastAirDate: String? = null,
    @Json(name = "last_episode_to_air") val lastEpisodeToAir: TMDbEpisodeResponse? = null,
    @Json(name = "name") val name: String = "",
    @Json(name = "next_episode_to_air") val nextEpisodeToAir: TMDbEpisodeResponse? = null,
    @Json(name = "networks") val networks: List<TMDbNetworkResponse> = emptyList(),
    @Json(name = "number_of_episodes") val numberOfEpisodes: Int = 0,
    @Json(name = "number_of_seasons") val numberOfSeasons: Int = 0,
    @Json(name = "origin_country") val originCountry: List<String> = emptyList(),
    @Json(name = "original_language") val originalLanguage: String = "",
    @Json(name = "original_name") val originalName: String = "",
    @Json(name = "overview") val overview: String? = null,
    @Json(name = "popularity") val popularity: Double = 0.0,
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "production_companies") val productionCompanies: List<TMDbProductionCompanyResponse> = emptyList(),
    @Json(name = "production_countries") val productionCountries: List<TMDbProductionCountryResponse> = emptyList(),
    @Json(name = "seasons") val seasons: List<TMDbSeasonResponse> = emptyList(),
    @Json(name = "spoken_languages") val spokenLanguages: List<TMDbSpokenLanguageResponse> = emptyList(),
    @Json(name = "status") val status: String = "",
    @Json(name = "tagline") val tagline: String? = null,
    @Json(name = "type") val type: String = "",
    @Json(name = "vote_average") val voteAverage: Double = 0.0,
    @Json(name = "vote_count") val voteCount: Int = 0,
    
    // Additional fields for append_to_response
    @Json(name = "credits") val credits: TMDbCreditsResponse? = null,
    @Json(name = "images") val images: TMDbTVImagesResponse? = null,
    @Json(name = "videos") val videos: TMDbTVVideosResponse? = null,
    @Json(name = "recommendations") val recommendations: TMDbRecommendationsResponse? = null,
    @Json(name = "similar") val similar: TMDbRecommendationsResponse? = null,
    @Json(name = "keywords") val keywords: TMDbKeywordsResponse? = null,
    @Json(name = "reviews") val reviews: TMDbReviewsResponse? = null,
    @Json(name = "watch/providers") val watchProviders: TMDbWatchProvidersResponse? = null,
    @Json(name = "content_ratings") val contentRatings: TMDbContentRatingsResponse? = null
)

/**
 * TMDb Created By response model
 */
@JsonClass(generateAdapter = true)
data class TMDbCreatedByResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "credit_id") val creditId: String = "",
    @Json(name = "name") val name: String = "",
    @Json(name = "gender") val gender: Int = 0,
    @Json(name = "profile_path") val profilePath: String? = null
)

/**
 * TMDb Network response model
 */
@JsonClass(generateAdapter = true)
data class TMDbNetworkResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "logo_path") val logoPath: String? = null,
    @Json(name = "name") val name: String = "",
    @Json(name = "origin_country") val originCountry: String = ""
)

/**
 * TMDb Season response model
 */
@JsonClass(generateAdapter = true)
data class TMDbSeasonResponse(
    @Json(name = "air_date") val airDate: String? = null,
    @Json(name = "episode_count") val episodeCount: Int = 0,
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "overview") val overview: String = "",
    @Json(name = "poster_path") val posterPath: String? = null,
    @Json(name = "season_number") val seasonNumber: Int = 0,
    @Json(name = "vote_average") val voteAverage: Double = 0.0,
    @Json(name = "episodes") val episodes: List<TMDbEpisodeResponse> = emptyList()
)

/**
 * TMDb Episode response model
 */
@JsonClass(generateAdapter = true)
data class TMDbEpisodeResponse(
    @Json(name = "air_date") val airDate: String? = null,
    @Json(name = "episode_number") val episodeNumber: Int = 0,
    @Json(name = "id") val id: Int = 0,
    @Json(name = "name") val name: String = "",
    @Json(name = "overview") val overview: String = "",
    @Json(name = "production_code") val productionCode: String = "",
    @Json(name = "runtime") val runtime: Int? = null,
    @Json(name = "season_number") val seasonNumber: Int = 0,
    @Json(name = "still_path") val stillPath: String? = null,
    @Json(name = "vote_average") val voteAverage: Double = 0.0,
    @Json(name = "vote_count") val voteCount: Int = 0,
    @Json(name = "crew") val crew: List<TMDbCrewResponse> = emptyList(),
    @Json(name = "guest_stars") val guestStars: List<TMDbCastResponse> = emptyList()
)

/**
 * TMDb Content Ratings response model
 */
@JsonClass(generateAdapter = true)
data class TMDbContentRatingsResponse(
    @Json(name = "results") val results: List<TMDbContentRatingResponse> = emptyList()
)

/**
 * TMDb Content Rating response model
 */
@JsonClass(generateAdapter = true)
data class TMDbContentRatingResponse(
    @Json(name = "descriptors") val descriptors: List<String> = emptyList(),
    @Json(name = "iso_3166_1") val iso31661: String = "",
    @Json(name = "rating") val rating: String = ""
)

/**
 * TMDb TV Images response model
 */
@JsonClass(generateAdapter = true)
data class TMDbTVImagesResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "backdrops") val backdrops: List<TMDbImageResponse> = emptyList(),
    @Json(name = "logos") val logos: List<TMDbImageResponse> = emptyList(),
    @Json(name = "posters") val posters: List<TMDbImageResponse> = emptyList()
)

/**
 * TMDb TV Videos response model
 */
@JsonClass(generateAdapter = true)
data class TMDbTVVideosResponse(
    @Json(name = "id") val id: Int = 0,
    @Json(name = "results") val results: List<TMDbVideoResponse> = emptyList()
)