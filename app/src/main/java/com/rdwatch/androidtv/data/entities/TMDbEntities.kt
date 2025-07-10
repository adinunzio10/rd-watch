package com.rdwatch.androidtv.data.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.rdwatch.androidtv.data.converters.Converters
import com.squareup.moshi.JsonClass

/**
 * Room entity for TMDb movie data
 * Stores movie details for offline access and caching
 */
@Entity(tableName = "tmdb_movies")
@TypeConverters(Converters::class)
data class TMDbMovieEntity(
    @PrimaryKey val id: Int,
    val title: String,
    val originalTitle: String,
    val overview: String?,
    val releaseDate: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Float,
    val voteCount: Int,
    val popularity: Float,
    val adult: Boolean,
    val video: Boolean,
    val originalLanguage: String,
    val genreIds: List<Int>,
    val runtime: Int?,
    val budget: Long?,
    val revenue: Long?,
    val status: String?,
    val tagline: String?,
    val homepage: String?,
    val imdbId: String?,
    val spokenLanguages: List<String>?,
    val productionCompanies: List<String>?,
    val productionCountries: List<String>?,
    val genres: List<String>?,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Room entity for TMDb TV show data
 */
@Entity(tableName = "tmdb_tv_shows")
@TypeConverters(Converters::class)
data class TMDbTVEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val originalName: String,
    val overview: String?,
    val firstAirDate: String?,
    val lastAirDate: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Float,
    val voteCount: Int,
    val popularity: Float,
    val adult: Boolean,
    val originalLanguage: String,
    val genreIds: List<Int>,
    val numberOfEpisodes: Int?,
    val numberOfSeasons: Int?,
    val status: String?,
    val type: String?,
    val homepage: String?,
    val inProduction: Boolean?,
    val languages: List<String>?,
    val lastEpisodeToAir: String?,
    val nextEpisodeToAir: String?,
    val networks: List<String>?,
    val originCountry: List<String>?,
    val productionCompanies: List<String>?,
    val productionCountries: List<String>?,
    val spokenLanguages: List<String>?,
    val seasons: List<String>?,
    val genres: List<String>?,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Room entity for TMDb search results
 * Caches search queries and their results
 */
@Entity(tableName = "tmdb_search_results")
@TypeConverters(Converters::class)
data class TMDbSearchResultEntity(
    @PrimaryKey val id: String, // Composite key: query + page + type
    val query: String,
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val searchType: String, // "movie", "tv", "multi"
    val results: List<TMDbSearchItemEntity>,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Individual search result item
 */
@JsonClass(generateAdapter = true)
data class TMDbSearchItemEntity(
    val id: Int,
    val mediaType: String, // "movie" or "tv"
    val title: String?, // For movies
    val name: String?, // For TV shows
    val originalTitle: String?,
    val originalName: String?,
    val overview: String?,
    val releaseDate: String?,
    val firstAirDate: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val voteAverage: Float,
    val voteCount: Int,
    val popularity: Float,
    val adult: Boolean,
    val video: Boolean?,
    val originalLanguage: String,
    val genreIds: List<Int>
)

/**
 * Room entity for TMDb credits (cast and crew)
 */
@Entity(tableName = "tmdb_credits")
@TypeConverters(Converters::class)
data class TMDbCreditsEntity(
    @PrimaryKey val id: String, // Composite key: contentId + contentType
    val contentId: Int,
    val contentType: String, // "movie" or "tv"
    val cast: List<TMDbCastMemberEntity>,
    val crew: List<TMDbCrewMemberEntity>,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Cast member data
 */
@JsonClass(generateAdapter = true)
data class TMDbCastMemberEntity(
    val id: Int,
    val name: String,
    val character: String,
    val profilePath: String?,
    val order: Int,
    val castId: Int?,
    val creditId: String,
    val adult: Boolean,
    val gender: Int?,
    val knownForDepartment: String?,
    val originalName: String,
    val popularity: Float
)

/**
 * Crew member data
 */
@JsonClass(generateAdapter = true)
data class TMDbCrewMemberEntity(
    val id: Int,
    val name: String,
    val job: String,
    val department: String,
    val profilePath: String?,
    val creditId: String,
    val adult: Boolean,
    val gender: Int?,
    val knownForDepartment: String?,
    val originalName: String,
    val popularity: Float
)

/**
 * Room entity for TMDb recommendations and similar content
 */
@Entity(tableName = "tmdb_recommendations")
@TypeConverters(Converters::class)
data class TMDbRecommendationEntity(
    @PrimaryKey val id: String, // Composite key: contentId + contentType + recommendationType
    val contentId: Int,
    val contentType: String, // "movie" or "tv"
    val recommendationType: String, // "recommendations", "similar", "popular", "top_rated", etc.
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val results: List<TMDbSearchItemEntity>,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Room entity for TMDb content images
 */
@Entity(tableName = "tmdb_images")
@TypeConverters(Converters::class)
data class TMDbImagesEntity(
    @PrimaryKey val id: String, // Composite key: contentId + contentType
    val contentId: Int,
    val contentType: String, // "movie" or "tv"
    val backdrops: List<TMDbImageEntity>,
    val posters: List<TMDbImageEntity>,
    val logos: List<TMDbImageEntity>?,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Individual image data
 */
@JsonClass(generateAdapter = true)
data class TMDbImageEntity(
    val filePath: String,
    val width: Int,
    val height: Int,
    val aspectRatio: Float,
    val voteAverage: Float,
    val voteCount: Int,
    val iso6391: String?
)

/**
 * Room entity for TMDb videos
 */
@Entity(tableName = "tmdb_videos")
@TypeConverters(Converters::class)
data class TMDbVideosEntity(
    @PrimaryKey val id: String, // Composite key: contentId + contentType
    val contentId: Int,
    val contentType: String, // "movie" or "tv"
    val results: List<TMDbVideoEntity>,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Individual video data
 */
@JsonClass(generateAdapter = true)
data class TMDbVideoEntity(
    val id: String,
    val key: String,
    val name: String,
    val site: String,
    val type: String,
    val size: Int,
    val official: Boolean,
    val publishedAt: String,
    val iso6391: String,
    val iso31661: String
)

/**
 * Room entity for TMDb genres
 */
@Entity(tableName = "tmdb_genres")
data class TMDbGenreEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val mediaType: String, // "movie" or "tv"
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Room entity for TMDb configuration data
 */
@Entity(tableName = "tmdb_config")
@TypeConverters(Converters::class)
data class TMDbConfigEntity(
    @PrimaryKey val id: String = "config",
    val imageBaseUrl: String,
    val secureBaseUrl: String,
    val backdropSizes: List<String>,
    val logoSizes: List<String>,
    val posterSizes: List<String>,
    val profileSizes: List<String>,
    val stillSizes: List<String>,
    val changeKeys: List<String>,
    val lastUpdated: Long = System.currentTimeMillis()
)

/**
 * Additional data classes for non-entity TMDb data
 */
data class TMDbTrendingItem(
    val id: Int,
    val mediaType: String,
    val title: String? = null,
    val name: String? = null,
    val overview: String = "",
    val posterPath: String? = null,
    val backdropPath: String? = null,
    val releaseDate: String? = null,
    val firstAirDate: String? = null,
    val voteAverage: Float = 0f,
    val voteCount: Int = 0,
    val popularity: Float = 0f,
    val genreIds: List<Int> = emptyList(),
    val originalLanguage: String = "",
    val adult: Boolean = false
)

data class TMDbDiscoveryFilters(
    val includeAdult: Boolean = false,
    val includeVideo: Boolean = false,
    val primaryReleaseYear: Int? = null,
    val year: Int? = null,
    val firstAirDateYear: Int? = null,
    val withGenres: String? = null,
    val withoutGenres: String? = null,
    val withRuntimeGte: Int? = null,
    val withRuntimeLte: Int? = null,
    val voteAverageGte: Float? = null,
    val voteAverageLte: Float? = null,
    val voteCountGte: Int? = null,
    val withOriginalLanguage: String? = null,
    val withWatchProviders: String? = null,
    val watchRegion: String? = null,
    val region: String? = null,
    val withNetworks: String? = null,
    val withStatus: String? = null,
    val withType: String? = null,
    val withKeywords: String? = null,
    val withoutKeywords: String? = null,
    val airDateGte: String? = null,
    val airDateLte: String? = null,
    val firstAirDateGte: String? = null,
    val firstAirDateLte: String? = null,
    val timezone: String? = null,
    val includeNullFirstAirDates: Boolean? = null
)

data class TMDbGenreItem(
    val id: Int,
    val name: String
)