package com.rdwatch.androidtv.data.mappers

import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbTVResponse
import com.rdwatch.androidtv.network.api.TMDbMovieService
import com.rdwatch.androidtv.ui.details.models.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mapper service for converting TMDb DTOs to ContentDetail models
 * Handles the transformation between TMDb API responses and UI-friendly ContentDetail objects
 */
@Singleton
class TMDbToContentDetailMapper @Inject constructor() {
    
    /**
     * Maps TMDb movie response to ContentDetail
     * @param movieResponse TMDb movie response
     * @return ContentDetail for UI consumption
     */
    fun mapMovieToContentDetail(movieResponse: TMDbMovieResponse): ContentDetail {
        return TMDbMovieContentDetail(
            id = "tmdb_movie_${movieResponse.id}",
            tmdbId = movieResponse.id,
            title = movieResponse.title,
            originalTitle = movieResponse.originalTitle,
            description = movieResponse.overview,
            backgroundImageUrl = movieResponse.backdropPath?.let { 
                "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}$it" 
            },
            cardImageUrl = movieResponse.posterPath?.let { 
                "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.POSTER_SIZE}$it" 
            },
            releaseDate = movieResponse.releaseDate,
            voteAverage = movieResponse.voteAverage.toFloat(),
            voteCount = movieResponse.voteCount,
            popularity = movieResponse.popularity.toFloat(),
            adult = movieResponse.adult,
            originalLanguage = movieResponse.originalLanguage,
            genres = movieResponse.genres.map { it.name },
            runtime = movieResponse.runtime,
            budget = movieResponse.budget,
            revenue = movieResponse.revenue,
            status = movieResponse.status,
            tagline = movieResponse.tagline,
            homepage = movieResponse.homepage,
            imdbId = movieResponse.imdbId,
            productionCompanies = movieResponse.productionCompanies.map { it.name },
            productionCountries = movieResponse.productionCountries.map { it.name },
            spokenLanguages = movieResponse.spokenLanguages.map { it.name },
            videoUrl = null // TMDb doesn't provide direct video URLs
        )
    }
    
    /**
     * Maps TMDb TV response to ContentDetail
     * @param tvResponse TMDb TV response
     * @return ContentDetail for UI consumption
     */
    fun mapTVToContentDetail(tvResponse: TMDbTVResponse): ContentDetail {
        return TMDbTVContentDetail(
            id = "tmdb_tv_${tvResponse.id}",
            tmdbId = tvResponse.id,
            title = tvResponse.name,
            originalTitle = tvResponse.originalName,
            description = tvResponse.overview,
            backgroundImageUrl = tvResponse.backdropPath?.let { 
                "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}$it" 
            },
            cardImageUrl = tvResponse.posterPath?.let { 
                "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.POSTER_SIZE}$it" 
            },
            firstAirDate = tvResponse.firstAirDate,
            lastAirDate = tvResponse.lastAirDate,
            voteAverage = tvResponse.voteAverage.toFloat(),
            voteCount = tvResponse.voteCount,
            popularity = tvResponse.popularity.toFloat(),
            adult = tvResponse.adult,
            originalLanguage = tvResponse.originalLanguage,
            genres = tvResponse.genres.map { it.name },
            numberOfEpisodes = tvResponse.numberOfEpisodes,
            numberOfSeasons = tvResponse.numberOfSeasons,
            status = tvResponse.status,
            type = tvResponse.type,
            homepage = tvResponse.homepage,
            inProduction = tvResponse.inProduction,
            networks = tvResponse.networks.map { it.name },
            originCountry = tvResponse.originCountry,
            productionCompanies = tvResponse.productionCompanies.map { it.name },
            productionCountries = tvResponse.productionCountries.map { it.name },
            spokenLanguages = tvResponse.spokenLanguages.map { it.name },
            videoUrl = null // TMDb doesn't provide direct video URLs
        )
    }
    
    /**
     * Formats runtime in minutes to display string
     * @param runtime Runtime in minutes
     * @return Formatted runtime string (e.g., "2h 30m")
     */
    private fun formatRuntime(runtime: Int?): String? {
        return runtime?.let {
            if (it < 60) {
                "${it}m"
            } else {
                val hours = it / 60
                val minutes = it % 60
                if (minutes == 0) {
                    "${hours}h"
                } else {
                    "${hours}h ${minutes}m"
                }
            }
        }
    }
    
    /**
     * Formats vote average to display rating
     * @param voteAverage Vote average (0.0 to 10.0)
     * @return Formatted rating string (e.g., "8.5/10")
     */
    private fun formatRating(voteAverage: Float): String? {
        return if (voteAverage > 0) {
            "${"%.1f".format(voteAverage)}/10"
        } else {
            null
        }
    }
    
    /**
     * Formats release date to display year
     * @param releaseDate Release date (YYYY-MM-DD)
     * @return Year string or null
     */
    private fun formatYear(releaseDate: String?): String? {
        return releaseDate?.take(4)
    }
}