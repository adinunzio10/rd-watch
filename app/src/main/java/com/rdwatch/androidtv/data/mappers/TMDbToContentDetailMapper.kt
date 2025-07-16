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
     * @param externalIds Optional external IDs response containing IMDb ID
     * @return ContentDetail for UI consumption
     */
    fun mapTVToContentDetail(
        tvResponse: TMDbTVResponse, 
        externalIds: com.rdwatch.androidtv.network.models.tmdb.TMDbExternalIdsResponse? = null
    ): ContentDetail {
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
            videoUrl = null, // TMDb doesn't provide direct video URLs
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
            imdbId = externalIds?.imdbId, // Now fetched from external IDs endpoint!
            networks = tvResponse.networks.map { it.name },
            originCountry = tvResponse.originCountry,
            productionCompanies = tvResponse.productionCompanies.map { it.name },
            productionCountries = tvResponse.productionCountries.map { it.name },
            spokenLanguages = tvResponse.spokenLanguages.map { it.name }
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
     * Maps TMDb multi-search result to ContentDetail
     * @param multiSearchResult TMDb multi-search result response
     * @return ContentDetail for UI consumption or null if unsupported type
     */
    fun mapMultiSearchResultToContentDetail(multiSearchResult: com.rdwatch.androidtv.network.models.tmdb.TMDbMultiSearchResultResponse): ContentDetail? {
        android.util.Log.d("TMDbMapper", "Mapping multi-search result: mediaType=${multiSearchResult.mediaType}, id=${multiSearchResult.id}, title=${multiSearchResult.title ?: multiSearchResult.name}")
        
        return when (multiSearchResult.mediaType) {
            "movie" -> {
                // Map movie result to TMDbMovieContentDetail
                TMDbMovieContentDetail(
                    id = "movie:${multiSearchResult.id}",
                    tmdbId = multiSearchResult.id,
                    title = multiSearchResult.title ?: "Unknown Movie",
                    originalTitle = multiSearchResult.originalTitle ?: multiSearchResult.title ?: "Unknown Movie",
                    description = multiSearchResult.overview.takeIf { it.isNotBlank() },
                    backgroundImageUrl = multiSearchResult.backdropPath?.let { 
                        "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}$it" 
                    },
                    cardImageUrl = multiSearchResult.posterPath?.let { 
                        "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.POSTER_SIZE}$it" 
                    },
                    releaseDate = multiSearchResult.releaseDate,
                    voteAverage = multiSearchResult.voteAverage.toFloat(),
                    voteCount = multiSearchResult.voteCount,
                    popularity = multiSearchResult.popularity.toFloat(),
                    adult = multiSearchResult.adult,
                    originalLanguage = multiSearchResult.originalLanguage,
                    genres = emptyList(), // Genre names not available in search results, only IDs
                    runtime = null, // Not available in search results
                    budget = 0L, // Not available in search results
                    revenue = 0L, // Not available in search results
                    status = "", // Not available in search results
                    tagline = null, // Not available in search results
                    homepage = null, // Not available in search results
                    imdbId = null, // Not available in search results
                    productionCompanies = emptyList(), // Not available in search results
                    productionCountries = emptyList(), // Not available in search results
                    spokenLanguages = emptyList(), // Not available in search results
                    videoUrl = null // TMDb doesn't provide direct video URLs
                )
            }
            "tv" -> {
                // Map TV show result to TMDbTVContentDetail
                TMDbTVContentDetail(
                    id = "tv:${multiSearchResult.id}",
                    tmdbId = multiSearchResult.id,
                    title = multiSearchResult.name ?: "Unknown TV Show",
                    originalTitle = multiSearchResult.originalName ?: multiSearchResult.name ?: "Unknown TV Show",
                    description = multiSearchResult.overview.takeIf { it.isNotBlank() },
                    backgroundImageUrl = multiSearchResult.backdropPath?.let { 
                        "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}$it" 
                    },
                    cardImageUrl = multiSearchResult.posterPath?.let { 
                        "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.POSTER_SIZE}$it" 
                    },
                    videoUrl = null, // TMDb doesn't provide direct video URLs
                    firstAirDate = multiSearchResult.firstAirDate,
                    lastAirDate = null, // Not available in search results
                    voteAverage = multiSearchResult.voteAverage.toFloat(),
                    voteCount = multiSearchResult.voteCount,
                    popularity = multiSearchResult.popularity.toFloat(),
                    adult = multiSearchResult.adult,
                    originalLanguage = multiSearchResult.originalLanguage,
                    genres = emptyList(), // Genre names not available in search results, only IDs
                    numberOfEpisodes = null, // Not available in search results
                    numberOfSeasons = null, // Not available in search results
                    status = null, // Not available in search results
                    type = null, // Not available in search results
                    homepage = null, // Not available in search results
                    inProduction = null, // Not available in search results
                    imdbId = null, // TODO: Fetch from external IDs endpoint for TV shows
                    networks = emptyList(), // Not available in search results
                    originCountry = multiSearchResult.originCountry,
                    productionCompanies = emptyList(), // Not available in search results
                    productionCountries = emptyList(), // Not available in search results
                    spokenLanguages = emptyList() // Not available in search results
                )
            }
            "person" -> {
                // Skip person results for now as we don't have a ContentDetail implementation for them
                android.util.Log.d("TMDbMapper", "Skipping person result: ${multiSearchResult.name}")
                null
            }
            else -> {
                android.util.Log.w("TMDbMapper", "Unknown media type: ${multiSearchResult.mediaType}")
                null
            }
        }
    }
    
    /**
     * Maps TMDb search result to ContentDetail (for trending/discovery APIs)
     * @param searchResult TMDb search result response
     * @param mediaType Media type hint (movie/tv) - helps when not specified in result
     * @return ContentDetail for UI consumption
     */
    fun mapSearchResultToContentDetail(searchResult: com.rdwatch.androidtv.network.models.tmdb.TMDbSearchResultResponse, mediaType: String? = null): ContentDetail {
        // Determine if this is a movie or TV show based on available fields
        val isMovie = searchResult.title != null || mediaType == "movie"
        val isTV = searchResult.name != null || mediaType == "tv"
        
        android.util.Log.d("TMDbMapper", "Mapping search result: id=${searchResult.id}, isMovie=$isMovie, isTV=$isTV, title=${searchResult.title ?: searchResult.name}")
        
        return if (isMovie && !isTV) {
            // Map as movie
            TMDbMovieContentDetail(
                id = "movie:${searchResult.id}",
                tmdbId = searchResult.id,
                title = searchResult.title ?: "Unknown Movie",
                originalTitle = searchResult.originalTitle ?: searchResult.title ?: "Unknown Movie",
                description = searchResult.overview.takeIf { it.isNotBlank() },
                backgroundImageUrl = searchResult.backdropPath?.let { 
                    "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}$it" 
                },
                cardImageUrl = searchResult.posterPath?.let { 
                    "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.POSTER_SIZE}$it" 
                },
                releaseDate = searchResult.releaseDate,
                voteAverage = searchResult.voteAverage.toFloat(),
                voteCount = searchResult.voteCount,
                popularity = searchResult.popularity.toFloat(),
                adult = searchResult.adult,
                originalLanguage = searchResult.originalLanguage,
                genres = emptyList(), // Genre names not available in search results, only IDs
                runtime = null, // Not available in search results
                budget = 0L, // Not available in search results
                revenue = 0L, // Not available in search results
                status = "", // Not available in search results
                tagline = null, // Not available in search results
                homepage = null, // Not available in search results
                imdbId = null, // Not available in search results
                productionCompanies = emptyList(), // Not available in search results
                productionCountries = emptyList(), // Not available in search results
                spokenLanguages = emptyList(), // Not available in search results
                videoUrl = null // TMDb doesn't provide direct video URLs
            )
        } else {
            // Map as TV show (default for trending mixed results)
            TMDbTVContentDetail(
                id = "tv:${searchResult.id}",
                tmdbId = searchResult.id,
                title = searchResult.name ?: searchResult.title ?: "Unknown TV Show",
                originalTitle = searchResult.originalName ?: searchResult.name ?: searchResult.title ?: "Unknown TV Show",
                description = searchResult.overview.takeIf { it.isNotBlank() },
                backgroundImageUrl = searchResult.backdropPath?.let { 
                    "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}$it" 
                },
                cardImageUrl = searchResult.posterPath?.let { 
                    "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.POSTER_SIZE}$it" 
                },
                videoUrl = null, // TMDb doesn't provide direct video URLs
                firstAirDate = searchResult.firstAirDate,
                lastAirDate = null, // Not available in search results
                voteAverage = searchResult.voteAverage.toFloat(),
                voteCount = searchResult.voteCount,
                popularity = searchResult.popularity.toFloat(),
                adult = searchResult.adult,
                originalLanguage = searchResult.originalLanguage,
                genres = emptyList(), // Genre names not available in search results, only IDs
                numberOfEpisodes = null, // Not available in search results
                numberOfSeasons = null, // Not available in search results
                status = null, // Not available in search results
                type = null, // Not available in search results
                homepage = null, // Not available in search results
                inProduction = null, // Not available in search results
                imdbId = null, // TODO: Fetch from external IDs endpoint for TV shows
                networks = emptyList(), // Not available in search results
                originCountry = searchResult.originCountry,
                productionCompanies = emptyList(), // Not available in search results
                productionCountries = emptyList(), // Not available in search results
                spokenLanguages = emptyList() // Not available in search results
            )
        }
    }
    
    /**
     * Maps a list of TMDb search results to ContentDetail list (for trending/discovery APIs)
     * @param searchResults List of TMDb search result responses
     * @param mediaType Media type hint (movie/tv) for all results
     * @return List of ContentDetail objects
     */
    fun mapSearchResultsToContentDetails(searchResults: List<com.rdwatch.androidtv.network.models.tmdb.TMDbSearchResultResponse>, mediaType: String? = null): List<ContentDetail> {
        android.util.Log.d("TMDbMapper", "Mapping ${searchResults.size} search results to ContentDetails (mediaType: $mediaType)")
        
        return searchResults.mapNotNull { result ->
            try {
                mapSearchResultToContentDetail(result, mediaType)
            } catch (e: Exception) {
                android.util.Log.e("TMDbMapper", "Error mapping search result: ${e.message}", e)
                null
            }
        }.also { contentDetails ->
            android.util.Log.d("TMDbMapper", "Successfully mapped ${contentDetails.size} ContentDetails")
        }
    }
    
    /**
     * Maps a list of TMDb multi-search results to ContentDetail list
     * @param multiSearchResults List of TMDb multi-search result responses
     * @return List of ContentDetail objects (excluding unsupported types)
     */
    fun mapMultiSearchResultsToContentDetails(multiSearchResults: List<com.rdwatch.androidtv.network.models.tmdb.TMDbMultiSearchResultResponse>): List<ContentDetail> {
        android.util.Log.d("TMDbMapper", "Mapping ${multiSearchResults.size} multi-search results to ContentDetails")
        
        return multiSearchResults.mapNotNull { result ->
            try {
                mapMultiSearchResultToContentDetail(result)
            } catch (e: Exception) {
                android.util.Log.e("TMDbMapper", "Error mapping search result: ${e.message}", e)
                null
            }
        }.also { contentDetails ->
            android.util.Log.d("TMDbMapper", "Successfully mapped ${contentDetails.size} ContentDetails")
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