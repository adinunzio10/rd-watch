package com.rdwatch.androidtv.data.mappers

import com.rdwatch.androidtv.network.models.tmdb.TMDbTVResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbCreditsResponse
import com.rdwatch.androidtv.network.api.TMDbMovieService
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import com.rdwatch.androidtv.ui.details.models.ContentType
import com.rdwatch.androidtv.ui.details.models.ContentMetadata
import com.rdwatch.androidtv.ui.details.models.ContentAction
import com.rdwatch.androidtv.ui.details.models.ContentProgress
import com.rdwatch.androidtv.ui.details.models.ExtendedContentMetadata
import com.rdwatch.androidtv.ui.details.models.CastMember
import com.rdwatch.androidtv.ui.details.models.CrewMember

/**
 * TMDb TV Show implementation of ContentDetail
 */
data class TMDbTVContentDetail(
    private val tmdbTV: TMDbTVResponse,
    private val credits: TMDbCreditsResponse? = null,
    private val progress: ContentProgress = ContentProgress(),
    private val isInWatchlist: Boolean = false,
    private val isLiked: Boolean = false,
    private val isDownloaded: Boolean = false,
    private val isDownloading: Boolean = false,
    private val imdbId: String? = null
) : ContentDetail {
    
    override val id: String = tmdbTV.id.toString()
    override val title: String = tmdbTV.name
    override val description: String? = tmdbTV.overview
    override val backgroundImageUrl: String? = getBackdropUrl(tmdbTV.backdropPath)
    override val cardImageUrl: String? = getPosterUrl(tmdbTV.posterPath)
    override val contentType: ContentType = ContentType.TV_SHOW
    override val videoUrl: String? = null // TMDb doesn't provide direct video URLs
    
    val extendedMetadata: ExtendedContentMetadata = ExtendedContentMetadata(
        year = formatYear(tmdbTV.firstAirDate),
        duration = formatEpisodeRuntime(tmdbTV.episodeRunTime),
        rating = if (tmdbTV.voteAverage > 0) formatRating(tmdbTV.voteAverage) else null,
        language = tmdbTV.originalLanguage,
        genre = extractGenreNames(tmdbTV.genres),
        studio = tmdbTV.networks.firstOrNull()?.name ?: tmdbTV.productionCompanies.firstOrNull()?.name,
        cast = extractCastNames(credits),
        fullCast = extractFullCast(credits),
        director = extractCreator(tmdbTV),
        crew = extractFullCrew(credits, tmdbTV),
        customMetadata = mapOf(
            "tmdb_id" to tmdbTV.id.toString(),
            "imdb_id" to (imdbId ?: ""),
            "vote_count" to tmdbTV.voteCount.toString(),
            "popularity" to tmdbTV.popularity.toString(),
            "status" to tmdbTV.status,
            "tagline" to (tmdbTV.tagline ?: ""),
            "homepage" to (tmdbTV.homepage ?: ""),
            "adult" to tmdbTV.adult.toString(),
            "original_name" to tmdbTV.originalName,
            "original_language" to tmdbTV.originalLanguage,
            "first_air_date" to (tmdbTV.firstAirDate ?: ""),
            "last_air_date" to (tmdbTV.lastAirDate ?: ""),
            "number_of_episodes" to tmdbTV.numberOfEpisodes.toString(),
            "number_of_seasons" to tmdbTV.numberOfSeasons.toString(),
            "in_production" to tmdbTV.inProduction.toString(),
            "type" to tmdbTV.type,
            "networks" to tmdbTV.networks.joinToString(", ") { it.name },
            "production_companies" to extractProductionCompanyNames(tmdbTV.productionCompanies).joinToString(", "),
            "production_countries" to tmdbTV.productionCountries.joinToString(", ") { it.name },
            "spoken_languages" to extractSpokenLanguageNames(tmdbTV.spokenLanguages).joinToString(", "),
            "origin_country" to tmdbTV.originCountry.joinToString(", "),
            "languages" to tmdbTV.languages.joinToString(", "),
            "created_by" to tmdbTV.createdBy.joinToString(", ") { it.name },
            "episode_run_time" to tmdbTV.episodeRunTime.joinToString(", ") { "${it}min" }
        )
    )
    
    override val metadata: ContentMetadata = extendedMetadata.toContentMetadata()
    
    override val actions: List<ContentAction> = createContentActions(
        isInWatchlist = isInWatchlist,
        isLiked = isLiked,
        isDownloaded = isDownloaded,
        isDownloading = isDownloading,
        hasProgress = progress.hasProgress
    )
    
    /**
     * Get the underlying TMDb TV show object
     */
    fun getTMDbTV(): TMDbTVResponse = tmdbTV
    
    /**
     * Get the credits information
     */
    fun getCredits(): TMDbCreditsResponse? = credits
    
    /**
     * Get content progress information
     */
    fun getProgress(): ContentProgress = progress
    
    /**
     * Get TMDb TV show ID
     */
    fun getTMDbId(): Int = tmdbTV.id
    
    /**
     * Get IMDb ID if available
     */
    fun getImdbId(): String? = extendedMetadata.customMetadata["imdb_id"]?.takeIf { it.isNotEmpty() }
    
    /**
     * Get original name
     */
    fun getOriginalName(): String = tmdbTV.originalName
    
    /**
     * Get formatted first air date
     */
    fun getFormattedFirstAirDate(): String? = tmdbTV.firstAirDate
    
    /**
     * Get formatted last air date
     */
    fun getFormattedLastAirDate(): String? = tmdbTV.lastAirDate
    
    /**
     * Get vote average as formatted string
     */
    fun getFormattedVoteAverage(): String = formatRating(tmdbTV.voteAverage)
    
    /**
     * Get vote count
     */
    fun getVoteCount(): Int = tmdbTV.voteCount
    
    /**
     * Get popularity score
     */
    fun getPopularity(): Double = tmdbTV.popularity
    
    /**
     * Get show status
     */
    fun getStatus(): String = tmdbTV.status
    
    /**
     * Get tagline
     */
    fun getTagline(): String? = tmdbTV.tagline
    
    /**
     * Get homepage URL
     */
    fun getHomepage(): String? = tmdbTV.homepage
    
    /**
     * Check if show is adult content
     */
    fun isAdultContent(): Boolean = tmdbTV.adult
    
    /**
     * Get original language
     */
    fun getOriginalLanguage(): String = tmdbTV.originalLanguage
    
    /**
     * Get origin countries
     */
    fun getOriginCountries(): List<String> = tmdbTV.originCountry
    
    /**
     * Get number of episodes
     */
    fun getNumberOfEpisodes(): Int = tmdbTV.numberOfEpisodes
    
    /**
     * Get number of seasons
     */
    fun getNumberOfSeasons(): Int = tmdbTV.numberOfSeasons
    
    /**
     * Check if show is in production
     */
    fun isInProduction(): Boolean = tmdbTV.inProduction
    
    /**
     * Get show type
     */
    fun getType(): String = tmdbTV.type
    
    /**
     * Get networks
     */
    fun getNetworks(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbNetworkResponse> = tmdbTV.networks
    
    /**
     * Get production companies
     */
    fun getProductionCompanies(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbProductionCompanyResponse> = tmdbTV.productionCompanies
    
    /**
     * Get production countries
     */
    fun getProductionCountries(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbProductionCountryResponse> = tmdbTV.productionCountries
    
    /**
     * Get spoken languages
     */
    fun getSpokenLanguages(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbSpokenLanguageResponse> = tmdbTV.spokenLanguages
    
    /**
     * Get genres
     */
    fun getGenres(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbGenreResponse> = tmdbTV.genres
    
    /**
     * Get created by information
     */
    fun getCreatedBy(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbCreatedByResponse> = tmdbTV.createdBy
    
    /**
     * Get episode run time
     */
    fun getEpisodeRunTime(): List<Int> = tmdbTV.episodeRunTime
    
    /**
     * Get languages
     */
    fun getLanguages(): List<String> = tmdbTV.languages
    
    /**
     * Get seasons
     */
    fun getSeasons(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse> = tmdbTV.seasons
    
    
    /**
     * Get last episode to air
     */
    fun getLastEpisodeToAir(): com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse? = tmdbTV.lastEpisodeToAir
    
    /**
     * Get next episode to air
     */
    fun getNextEpisodeToAir(): com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse? = tmdbTV.nextEpisodeToAir
    
    /**
     * Create a copy with updated progress
     */
    fun withProgress(newProgress: ContentProgress): TMDbTVContentDetail {
        return copy(progress = newProgress)
    }
    
    /**
     * Create a copy with updated IMDB ID
     */
    fun withImdbId(newImdbId: String?): TMDbTVContentDetail {
        return copy(imdbId = newImdbId)
    }
    
    /**
     * Create a copy with updated watchlist status
     */
    fun withWatchlistStatus(inWatchlist: Boolean): TMDbTVContentDetail {
        return copy(isInWatchlist = inWatchlist)
    }
    
    /**
     * Create a copy with updated like status
     */
    fun withLikeStatus(liked: Boolean): TMDbTVContentDetail {
        return copy(isLiked = liked)
    }
    
    /**
     * Create a copy with updated download status
     */
    fun withDownloadStatus(downloaded: Boolean, downloading: Boolean = false): TMDbTVContentDetail {
        return copy(isDownloaded = downloaded, isDownloading = downloading)
    }
    
    /**
     * Create a copy with updated credits
     */
    fun withCredits(newCredits: TMDbCreditsResponse?): TMDbTVContentDetail {
        return copy(credits = newCredits)
    }
    
    // Helper methods from mapper
    private fun getBackdropUrl(path: String?): String? {
        return path?.let { "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}$it" }
    }
    
    private fun getPosterUrl(path: String?): String? {
        return path?.let { "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.POSTER_SIZE}$it" }
    }
    
    private fun formatEpisodeRuntime(runtimes: List<Int>): String? {
        return runtimes.firstOrNull()?.let {
            val hours = it / 60
            val mins = it % 60
            if (hours > 0) {
                "${hours}h ${mins}m"
            } else {
                "${mins}m"
            }
        }
    }
    
    private fun formatRating(voteAverage: Double): String {
        return String.format("%.1f", voteAverage)
    }
    
    private fun formatYear(firstAirDate: String?): String? {
        return firstAirDate?.takeIf { it.isNotEmpty() }?.substring(0, 4)
    }
    
    private fun extractCastNames(credits: TMDbCreditsResponse?, limit: Int = 5): List<String> {
        return credits?.cast?.take(limit)?.map { it.name } ?: emptyList()
    }
    
    private fun extractCreator(tmdbTV: TMDbTVResponse): String? {
        return tmdbTV.createdBy.firstOrNull()?.name
    }
    
    private fun extractGenreNames(genres: List<com.rdwatch.androidtv.network.models.tmdb.TMDbGenreResponse>): List<String> {
        return genres.map { it.name }
    }
    
    private fun extractProductionCompanyNames(companies: List<com.rdwatch.androidtv.network.models.tmdb.TMDbProductionCompanyResponse>): List<String> {
        return companies.map { it.name }
    }
    
    private fun extractSpokenLanguageNames(languages: List<com.rdwatch.androidtv.network.models.tmdb.TMDbSpokenLanguageResponse>): List<String> {
        return languages.map { it.name }
    }
    
    private fun extractFullCast(credits: TMDbCreditsResponse?, limit: Int = 20): List<CastMember> {
        return credits?.cast?.take(limit)?.map { castMember ->
            CastMember(
                id = castMember.id,
                name = castMember.name,
                character = castMember.character,
                profileImageUrl = CastMember.buildProfileImageUrl(castMember.profilePath),
                order = castMember.order
            )
        } ?: emptyList()
    }
    
    private fun extractFullCrew(credits: TMDbCreditsResponse?, tmdbTV: TMDbTVResponse): List<CrewMember> {
        val crewFromCredits = credits?.crew?.filter { crewMember ->
            CrewMember.isKeyRole(crewMember.job)
        }?.map { crewMember ->
            CrewMember(
                id = crewMember.id,
                name = crewMember.name,
                job = crewMember.job,
                department = crewMember.department,
                profileImageUrl = CrewMember.buildProfileImageUrl(crewMember.profilePath)
            )
        } ?: emptyList()
        
        // Add creators from TV show data as crew members
        val creators = tmdbTV.createdBy.map { creator ->
            CrewMember(
                id = creator.id,
                name = creator.name,
                job = "Creator",
                department = "Writing",
                profileImageUrl = CastMember.buildProfileImageUrl(creator.profilePath)
            )
        }
        
        return (creators + crewFromCredits).distinctBy { it.id }
    }
    
    private fun createContentActions(
        isInWatchlist: Boolean,
        isLiked: Boolean,
        isDownloaded: Boolean,
        isDownloading: Boolean,
        hasProgress: Boolean
    ): List<ContentAction> {
        return buildList {
            // Play action
            add(ContentAction.Play(isResume = hasProgress))
            
            // Watchlist action
            add(ContentAction.AddToWatchlist(isInWatchlist))
            
            // Like action
            add(ContentAction.Like(isLiked))
            
            // Share action
            add(ContentAction.Share())
            
            // Download action
            add(ContentAction.Download(isDownloaded, isDownloading))
        }
    }
}