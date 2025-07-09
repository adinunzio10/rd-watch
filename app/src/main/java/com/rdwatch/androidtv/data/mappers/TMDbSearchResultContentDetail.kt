package com.rdwatch.androidtv.data.mappers

import com.rdwatch.androidtv.network.models.tmdb.TMDbSearchResultResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbMultiSearchResultResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbTVResponse
import com.rdwatch.androidtv.network.api.TMDbMovieService
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import com.rdwatch.androidtv.ui.details.models.ContentType
import com.rdwatch.androidtv.ui.details.models.ContentMetadata
import com.rdwatch.androidtv.ui.details.models.ContentAction
import com.rdwatch.androidtv.ui.details.models.ContentProgress

/**
 * TMDb Search Result implementation of ContentDetail
 */
data class TMDbSearchResultContentDetail(
    private val searchResult: TMDbSearchResultResponse,
    private val progress: ContentProgress = ContentProgress(),
    private val isInWatchlist: Boolean = false,
    private val isLiked: Boolean = false,
    private val isDownloaded: Boolean = false,
    private val isDownloading: Boolean = false
) : ContentDetail {
    
    override val id: String = searchResult.id.toString()
    override val title: String = searchResult.title ?: searchResult.name ?: "Unknown Title"
    override val description: String? = searchResult.overview
    override val backgroundImageUrl: String? = getBackdropUrl(searchResult.backdropPath)
    override val cardImageUrl: String? = getPosterUrl(searchResult.posterPath)
    override val contentType: ContentType = determineContentType()
    override val videoUrl: String? = null // TMDb doesn't provide direct video URLs
    
    override val metadata: ContentMetadata = ContentMetadata(
        year = formatYear(searchResult.releaseDate ?: searchResult.firstAirDate),
        rating = if (searchResult.voteAverage > 0) formatRating(searchResult.voteAverage) else null,
        language = searchResult.originalLanguage,
        genre = emptyList(), // Genre names not available in search results, only IDs
        customMetadata = mapOf(
            "tmdb_id" to searchResult.id.toString(),
            "vote_count" to searchResult.voteCount.toString(),
            "popularity" to searchResult.popularity.toString(),
            "adult" to searchResult.adult.toString(),
            "original_title" to (searchResult.originalTitle ?: ""),
            "original_name" to (searchResult.originalName ?: ""),
            "original_language" to searchResult.originalLanguage,
            "release_date" to (searchResult.releaseDate ?: ""),
            "first_air_date" to (searchResult.firstAirDate ?: ""),
            "genre_ids" to searchResult.genreIds.joinToString(", "),
            "origin_country" to searchResult.originCountry.joinToString(", "),
            "video" to searchResult.video.toString()
        )
    )
    
    override val actions: List<ContentAction> = createContentActions(
        isInWatchlist = isInWatchlist,
        isLiked = isLiked,
        isDownloaded = isDownloaded,
        isDownloading = isDownloading,
        hasProgress = progress.hasProgress
    )
    
    /**
     * Get the underlying TMDb search result object
     */
    fun getSearchResult(): TMDbSearchResultResponse = searchResult
    
    /**
     * Get content progress information
     */
    fun getProgress(): ContentProgress = progress
    
    /**
     * Get TMDb ID
     */
    fun getTMDbId(): Int = searchResult.id
    
    /**
     * Get original title (for movies)
     */
    fun getOriginalTitle(): String? = searchResult.originalTitle
    
    /**
     * Get original name (for TV shows)
     */
    fun getOriginalName(): String? = searchResult.originalName
    
    /**
     * Get formatted release date
     */
    fun getFormattedReleaseDate(): String? = searchResult.releaseDate
    
    /**
     * Get formatted first air date
     */
    fun getFormattedFirstAirDate(): String? = searchResult.firstAirDate
    
    /**
     * Get vote average as formatted string
     */
    fun getFormattedVoteAverage(): String = formatRating(searchResult.voteAverage)
    
    /**
     * Get vote count
     */
    fun getVoteCount(): Int = searchResult.voteCount
    
    /**
     * Get popularity score
     */
    fun getPopularity(): Double = searchResult.popularity
    
    /**
     * Check if content is adult
     */
    fun isAdultContent(): Boolean = searchResult.adult
    
    /**
     * Get original language
     */
    fun getOriginalLanguage(): String = searchResult.originalLanguage
    
    /**
     * Get genre IDs
     */
    fun getGenreIds(): List<Int> = searchResult.genreIds
    
    /**
     * Get origin countries (for TV shows)
     */
    fun getOriginCountries(): List<String> = searchResult.originCountry
    
    /**
     * Check if content has video
     */
    fun hasVideoContent(): Boolean = searchResult.video
    
    /**
     * Create a copy with updated progress
     */
    fun withProgress(newProgress: ContentProgress): TMDbSearchResultContentDetail {
        return copy(progress = newProgress)
    }
    
    /**
     * Create a copy with updated watchlist status
     */
    fun withWatchlistStatus(inWatchlist: Boolean): TMDbSearchResultContentDetail {
        return copy(isInWatchlist = inWatchlist)
    }
    
    /**
     * Create a copy with updated like status
     */
    fun withLikeStatus(liked: Boolean): TMDbSearchResultContentDetail {
        return copy(isLiked = liked)
    }
    
    /**
     * Create a copy with updated download status
     */
    fun withDownloadStatus(downloaded: Boolean, downloading: Boolean = false): TMDbSearchResultContentDetail {
        return copy(isDownloaded = downloaded, isDownloading = downloading)
    }
    
    // Helper methods
    private fun determineContentType(): ContentType {
        return when {
            searchResult.title != null -> ContentType.MOVIE
            searchResult.name != null -> ContentType.TV_SHOW
            else -> ContentType.MOVIE // Default fallback
        }
    }
    
    private fun getBackdropUrl(path: String?): String? {
        return path?.let { "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}$it" }
    }
    
    private fun getPosterUrl(path: String?): String? {
        return path?.let { "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.POSTER_SIZE}$it" }
    }
    
    private fun formatRating(voteAverage: Double): String {
        return String.format("%.1f", voteAverage)
    }
    
    private fun formatYear(date: String?): String? {
        return date?.takeIf { it.isNotEmpty() }?.substring(0, 4)
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

/**
 * TMDb Multi Search Result implementation of ContentDetail
 */
data class TMDbMultiSearchResultContentDetail(
    private val multiSearchResult: TMDbMultiSearchResultResponse,
    private val progress: ContentProgress = ContentProgress(),
    private val isInWatchlist: Boolean = false,
    private val isLiked: Boolean = false,
    private val isDownloaded: Boolean = false,
    private val isDownloading: Boolean = false
) : ContentDetail {
    
    override val id: String = multiSearchResult.id.toString()
    override val title: String = multiSearchResult.title ?: multiSearchResult.name ?: "Unknown Title"
    override val description: String? = multiSearchResult.overview
    override val backgroundImageUrl: String? = getBackdropUrl(multiSearchResult.backdropPath)
    override val cardImageUrl: String? = getPosterUrl(multiSearchResult.posterPath ?: multiSearchResult.profilePath)
    override val contentType: ContentType = determineContentType()
    override val videoUrl: String? = null // TMDb doesn't provide direct video URLs
    
    override val metadata: ContentMetadata = ContentMetadata(
        year = formatYear(multiSearchResult.releaseDate ?: multiSearchResult.firstAirDate),
        rating = if (multiSearchResult.voteAverage > 0) formatRating(multiSearchResult.voteAverage) else null,
        language = multiSearchResult.originalLanguage,
        genre = emptyList(), // Genre names not available in search results, only IDs
        customMetadata = mapOf(
            "tmdb_id" to multiSearchResult.id.toString(),
            "media_type" to multiSearchResult.mediaType,
            "vote_count" to multiSearchResult.voteCount.toString(),
            "popularity" to multiSearchResult.popularity.toString(),
            "adult" to multiSearchResult.adult.toString(),
            "original_title" to (multiSearchResult.originalTitle ?: ""),
            "original_name" to (multiSearchResult.originalName ?: ""),
            "original_language" to multiSearchResult.originalLanguage,
            "release_date" to (multiSearchResult.releaseDate ?: ""),
            "first_air_date" to (multiSearchResult.firstAirDate ?: ""),
            "genre_ids" to multiSearchResult.genreIds.joinToString(", "),
            "origin_country" to multiSearchResult.originCountry.joinToString(", "),
            "video" to multiSearchResult.video.toString(),
            "gender" to multiSearchResult.gender.toString(),
            "known_for_department" to multiSearchResult.knownForDepartment
        )
    )
    
    override val actions: List<ContentAction> = createContentActions(
        isInWatchlist = isInWatchlist,
        isLiked = isLiked,
        isDownloaded = isDownloaded,
        isDownloading = isDownloading,
        hasProgress = progress.hasProgress
    )
    
    /**
     * Get the underlying TMDb multi search result object
     */
    fun getMultiSearchResult(): TMDbMultiSearchResultResponse = multiSearchResult
    
    /**
     * Get content progress information
     */
    fun getProgress(): ContentProgress = progress
    
    /**
     * Get TMDb ID
     */
    fun getTMDbId(): Int = multiSearchResult.id
    
    /**
     * Get media type
     */
    fun getMediaType(): String = multiSearchResult.mediaType
    
    /**
     * Get original title (for movies)
     */
    fun getOriginalTitle(): String? = multiSearchResult.originalTitle
    
    /**
     * Get original name (for TV shows)
     */
    fun getOriginalName(): String? = multiSearchResult.originalName
    
    /**
     * Get formatted release date
     */
    fun getFormattedReleaseDate(): String? = multiSearchResult.releaseDate
    
    /**
     * Get formatted first air date
     */
    fun getFormattedFirstAirDate(): String? = multiSearchResult.firstAirDate
    
    /**
     * Get vote average as formatted string
     */
    fun getFormattedVoteAverage(): String = formatRating(multiSearchResult.voteAverage)
    
    /**
     * Get vote count
     */
    fun getVoteCount(): Int = multiSearchResult.voteCount
    
    /**
     * Get popularity score
     */
    fun getPopularity(): Double = multiSearchResult.popularity
    
    /**
     * Check if content is adult
     */
    fun isAdultContent(): Boolean = multiSearchResult.adult
    
    /**
     * Get original language
     */
    fun getOriginalLanguage(): String = multiSearchResult.originalLanguage
    
    /**
     * Get genre IDs
     */
    fun getGenreIds(): List<Int> = multiSearchResult.genreIds
    
    /**
     * Get origin countries (for TV shows)
     */
    fun getOriginCountries(): List<String> = multiSearchResult.originCountry
    
    /**
     * Check if content has video
     */
    fun hasVideoContent(): Boolean = multiSearchResult.video
    
    /**
     * Get gender (for people)
     */
    fun getGender(): Int = multiSearchResult.gender
    
    /**
     * Get known for department (for people)
     */
    fun getKnownForDepartment(): String = multiSearchResult.knownForDepartment
    
    /**
     * Get profile path (for people)
     */
    fun getProfilePath(): String? = multiSearchResult.profilePath
    
    /**
     * Check if result is a person
     */
    fun isPerson(): Boolean = multiSearchResult.mediaType == "person"
    
    /**
     * Check if result is a movie
     */
    fun isMovie(): Boolean = multiSearchResult.mediaType == "movie"
    
    /**
     * Check if result is a TV show
     */
    fun isTVShow(): Boolean = multiSearchResult.mediaType == "tv"
    
    /**
     * Create a copy with updated progress
     */
    fun withProgress(newProgress: ContentProgress): TMDbMultiSearchResultContentDetail {
        return copy(progress = newProgress)
    }
    
    /**
     * Create a copy with updated watchlist status
     */
    fun withWatchlistStatus(inWatchlist: Boolean): TMDbMultiSearchResultContentDetail {
        return copy(isInWatchlist = inWatchlist)
    }
    
    /**
     * Create a copy with updated like status
     */
    fun withLikeStatus(liked: Boolean): TMDbMultiSearchResultContentDetail {
        return copy(isLiked = liked)
    }
    
    /**
     * Create a copy with updated download status
     */
    fun withDownloadStatus(downloaded: Boolean, downloading: Boolean = false): TMDbMultiSearchResultContentDetail {
        return copy(isDownloaded = downloaded, isDownloading = downloading)
    }
    
    // Helper methods
    private fun determineContentType(): ContentType {
        return when (multiSearchResult.mediaType) {
            "movie" -> ContentType.MOVIE
            "tv" -> ContentType.TV_SHOW
            "person" -> ContentType.DOCUMENTARY // Fallback for person results
            else -> ContentType.MOVIE // Default fallback
        }
    }
    
    private fun getBackdropUrl(path: String?): String? {
        return path?.let { "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}$it" }
    }
    
    private fun getPosterUrl(path: String?): String? {
        return path?.let { "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.POSTER_SIZE}$it" }
    }
    
    private fun formatRating(voteAverage: Double): String {
        return String.format("%.1f", voteAverage)
    }
    
    private fun formatYear(date: String?): String? {
        return date?.takeIf { it.isNotEmpty() }?.substring(0, 4)
    }
    
    private fun createContentActions(
        isInWatchlist: Boolean,
        isLiked: Boolean,
        isDownloaded: Boolean,
        isDownloading: Boolean,
        hasProgress: Boolean
    ): List<ContentAction> {
        return buildList {
            // Only add relevant actions based on media type
            if (multiSearchResult.mediaType != "person") {
                // Play action
                add(ContentAction.Play(isResume = hasProgress))
                
                // Watchlist action
                add(ContentAction.AddToWatchlist(isInWatchlist))
                
                // Like action
                add(ContentAction.Like(isLiked))
                
                // Download action
                add(ContentAction.Download(isDownloaded, isDownloading))
            }
            
            // Share action for all types
            add(ContentAction.Share())
        }
    }
}

/**
 * TMDb Episode implementation of ContentDetail
 */
data class TMDbEpisodeContentDetail(
    private val tmdbEpisode: TMDbEpisodeResponse,
    private val tmdbTV: TMDbTVResponse? = null,
    private val progress: ContentProgress = ContentProgress(),
    private val isInWatchlist: Boolean = false,
    private val isLiked: Boolean = false,
    private val isDownloaded: Boolean = false,
    private val isDownloading: Boolean = false
) : ContentDetail {
    
    override val id: String = tmdbEpisode.id.toString()
    override val title: String = tmdbEpisode.name
    override val description: String? = tmdbEpisode.overview
    override val backgroundImageUrl: String? = getStillUrl(tmdbEpisode.stillPath)
    override val cardImageUrl: String? = getStillUrl(tmdbEpisode.stillPath)
    override val contentType: ContentType = ContentType.TV_EPISODE
    override val videoUrl: String? = null // TMDb doesn't provide direct video URLs
    
    override val metadata: ContentMetadata = ContentMetadata(
        year = formatYear(tmdbEpisode.airDate),
        duration = formatRuntime(tmdbEpisode.runtime),
        rating = if (tmdbEpisode.voteAverage > 0) formatRating(tmdbEpisode.voteAverage) else null,
        season = tmdbEpisode.seasonNumber,
        episode = tmdbEpisode.episodeNumber,
        customMetadata = mapOf(
            "tmdb_id" to tmdbEpisode.id.toString(),
            "tv_show_name" to (tmdbTV?.name ?: ""),
            "tv_show_id" to (tmdbTV?.id?.toString() ?: ""),
            "season_number" to tmdbEpisode.seasonNumber.toString(),
            "episode_number" to tmdbEpisode.episodeNumber.toString(),
            "air_date" to (tmdbEpisode.airDate ?: ""),
            "vote_count" to tmdbEpisode.voteCount.toString(),
            "production_code" to tmdbEpisode.productionCode,
            "guest_stars" to tmdbEpisode.guestStars.joinToString(", ") { it.name },
            "crew" to tmdbEpisode.crew.joinToString(", ") { "${it.name} (${it.job})" }
        )
    )
    
    override val actions: List<ContentAction> = createContentActions(
        isInWatchlist = isInWatchlist,
        isLiked = isLiked,
        isDownloaded = isDownloaded,
        isDownloading = isDownloading,
        hasProgress = progress.hasProgress
    )
    
    /**
     * Get the underlying TMDb episode object
     */
    fun getTMDbEpisode(): TMDbEpisodeResponse = tmdbEpisode
    
    /**
     * Get the parent TV show object
     */
    fun getTMDbTV(): TMDbTVResponse? = tmdbTV
    
    /**
     * Get content progress information
     */
    fun getProgress(): ContentProgress = progress
    
    /**
     * Get TMDb episode ID
     */
    fun getTMDbId(): Int = tmdbEpisode.id
    
    /**
     * Get season number
     */
    fun getSeasonNumber(): Int = tmdbEpisode.seasonNumber
    
    /**
     * Get episode number
     */
    fun getEpisodeNumber(): Int = tmdbEpisode.episodeNumber
    
    /**
     * Get formatted air date
     */
    fun getFormattedAirDate(): String? = tmdbEpisode.airDate
    
    /**
     * Get vote average as formatted string
     */
    fun getFormattedVoteAverage(): String = formatRating(tmdbEpisode.voteAverage)
    
    /**
     * Get vote count
     */
    fun getVoteCount(): Int = tmdbEpisode.voteCount
    
    /**
     * Get production code
     */
    fun getProductionCode(): String = tmdbEpisode.productionCode
    
    /**
     * Get runtime in minutes
     */
    fun getRuntime(): Int? = tmdbEpisode.runtime
    
    /**
     * Get crew information
     */
    fun getCrew(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbCrewResponse> = tmdbEpisode.crew
    
    /**
     * Get guest stars
     */
    fun getGuestStars(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbCastResponse> = tmdbEpisode.guestStars
    
    /**
     * Get still path
     */
    fun getStillPath(): String? = tmdbEpisode.stillPath
    
    /**
     * Get episode title with show context
     */
    fun getFullTitle(): String {
        val showName = tmdbTV?.name ?: ""
        return if (showName.isNotEmpty()) {
            "$showName - S${tmdbEpisode.seasonNumber}E${tmdbEpisode.episodeNumber}: ${tmdbEpisode.name}"
        } else {
            "S${tmdbEpisode.seasonNumber}E${tmdbEpisode.episodeNumber}: ${tmdbEpisode.name}"
        }
    }
    
    /**
     * Create a copy with updated progress
     */
    fun withProgress(newProgress: ContentProgress): TMDbEpisodeContentDetail {
        return copy(progress = newProgress)
    }
    
    /**
     * Create a copy with updated watchlist status
     */
    fun withWatchlistStatus(inWatchlist: Boolean): TMDbEpisodeContentDetail {
        return copy(isInWatchlist = inWatchlist)
    }
    
    /**
     * Create a copy with updated like status
     */
    fun withLikeStatus(liked: Boolean): TMDbEpisodeContentDetail {
        return copy(isLiked = liked)
    }
    
    /**
     * Create a copy with updated download status
     */
    fun withDownloadStatus(downloaded: Boolean, downloading: Boolean = false): TMDbEpisodeContentDetail {
        return copy(isDownloaded = downloaded, isDownloading = downloading)
    }
    
    // Helper methods
    private fun getStillUrl(path: String?): String? {
        return path?.let { "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}$it" }
    }
    
    private fun formatRuntime(minutes: Int?): String? {
        return minutes?.let {
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
    
    private fun formatYear(airDate: String?): String? {
        return airDate?.takeIf { it.isNotEmpty() }?.substring(0, 4)
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