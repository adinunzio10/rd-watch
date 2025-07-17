package com.rdwatch.androidtv.data.mappers

import com.rdwatch.androidtv.network.api.TMDbMovieService
import com.rdwatch.androidtv.network.models.tmdb.TMDbCreditsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieResponse
import com.rdwatch.androidtv.ui.details.models.CastMember
import com.rdwatch.androidtv.ui.details.models.ContentAction
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import com.rdwatch.androidtv.ui.details.models.ContentMetadata
import com.rdwatch.androidtv.ui.details.models.ContentProgress
import com.rdwatch.androidtv.ui.details.models.ContentType
import com.rdwatch.androidtv.ui.details.models.CrewMember
import com.rdwatch.androidtv.ui.details.models.ExtendedContentMetadata

/**
 * TMDb Movie implementation of ContentDetail
 */
data class TMDbMovieContentDetail(
    private val tmdbMovie: TMDbMovieResponse,
    private val credits: TMDbCreditsResponse? = null,
    private val progress: ContentProgress = ContentProgress(),
    private val isInWatchlist: Boolean = false,
    private val isLiked: Boolean = false,
    private val isDownloaded: Boolean = false,
    private val isDownloading: Boolean = false,
) : ContentDetail {
    override val id: String = tmdbMovie.id.toString()
    override val title: String = tmdbMovie.title
    override val description: String? = tmdbMovie.overview
    override val backgroundImageUrl: String? = getBackdropUrl(tmdbMovie.backdropPath)
    override val cardImageUrl: String? = getPosterUrl(tmdbMovie.posterPath)
    override val contentType: ContentType = ContentType.MOVIE
    override val videoUrl: String? = null // TMDb doesn't provide direct video URLs

    val extendedMetadata: ExtendedContentMetadata =
        ExtendedContentMetadata(
            year = formatYear(tmdbMovie.releaseDate),
            duration = formatRuntime(tmdbMovie.runtime),
            rating = if (tmdbMovie.voteAverage > 0) formatRating(tmdbMovie.voteAverage) else null,
            language = tmdbMovie.originalLanguage,
            genre = extractGenreNames(tmdbMovie.genres),
            studio = tmdbMovie.productionCompanies.firstOrNull()?.name,
            cast = extractCastNames(credits),
            fullCast = extractFullCast(credits),
            director = extractDirector(credits),
            crew = extractFullCrew(credits),
            customMetadata =
                mapOf(
                    "tmdb_id" to tmdbMovie.id.toString(),
                    "imdb_id" to (tmdbMovie.imdbId ?: ""),
                    "vote_count" to tmdbMovie.voteCount.toString(),
                    "popularity" to tmdbMovie.popularity.toString(),
                    "budget" to tmdbMovie.budget.toString(),
                    "revenue" to tmdbMovie.revenue.toString(),
                    "status" to tmdbMovie.status,
                    "tagline" to (tmdbMovie.tagline ?: ""),
                    "homepage" to (tmdbMovie.homepage ?: ""),
                    "adult" to tmdbMovie.adult.toString(),
                    "original_title" to tmdbMovie.originalTitle,
                    "original_language" to tmdbMovie.originalLanguage,
                    "production_companies" to extractProductionCompanyNames(tmdbMovie.productionCompanies).joinToString(", "),
                    "production_countries" to tmdbMovie.productionCountries.joinToString(", ") { it.name },
                    "spoken_languages" to extractSpokenLanguageNames(tmdbMovie.spokenLanguages).joinToString(", "),
                    "belongs_to_collection" to (tmdbMovie.belongsToCollection?.name ?: ""),
                ),
        )

    override val metadata: ContentMetadata = extendedMetadata.toContentMetadata()

    override val actions: List<ContentAction> =
        createContentActions(
            isInWatchlist = isInWatchlist,
            isLiked = isLiked,
            isDownloaded = isDownloaded,
            isDownloading = isDownloading,
            hasProgress = progress.hasProgress,
        )

    /**
     * Get the underlying TMDb movie object
     */
    fun getTMDbMovie(): TMDbMovieResponse = tmdbMovie

    /**
     * Get the credits information
     */
    fun getCredits(): TMDbCreditsResponse? = credits

    /**
     * Get content progress information
     */
    fun getProgress(): ContentProgress = progress

    /**
     * Get TMDb movie ID
     */
    fun getTMDbId(): Int = tmdbMovie.id

    /**
     * Get IMDb ID if available
     */
    fun getImdbId(): String? = tmdbMovie.imdbId

    /**
     * Get collection information if movie belongs to a collection
     */
    fun getCollection(): com.rdwatch.androidtv.network.models.tmdb.TMDbCollectionResponse? = tmdbMovie.belongsToCollection

    /**
     * Get formatted release date
     */
    fun getFormattedReleaseDate(): String? = tmdbMovie.releaseDate

    /**
     * Get vote average as formatted string
     */
    fun getFormattedVoteAverage(): String = formatRating(tmdbMovie.voteAverage)

    /**
     * Get vote count
     */
    fun getVoteCount(): Int = tmdbMovie.voteCount

    /**
     * Get popularity score
     */
    fun getPopularity(): Double = tmdbMovie.popularity

    /**
     * Get budget
     */
    fun getBudget(): Long = tmdbMovie.budget

    /**
     * Get revenue
     */
    fun getRevenue(): Long = tmdbMovie.revenue

    /**
     * Get movie status
     */
    fun getStatus(): String = tmdbMovie.status

    /**
     * Get tagline
     */
    fun getTagline(): String? = tmdbMovie.tagline

    /**
     * Get homepage URL
     */
    fun getHomepage(): String? = tmdbMovie.homepage

    /**
     * Check if movie is adult content
     */
    fun isAdultContent(): Boolean = tmdbMovie.adult

    /**
     * Get original title
     */
    fun getOriginalTitle(): String = tmdbMovie.originalTitle

    /**
     * Get original language
     */
    fun getOriginalLanguage(): String = tmdbMovie.originalLanguage

    /**
     * Get production companies
     */
    fun getProductionCompanies(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbProductionCompanyResponse> =
        tmdbMovie.productionCompanies

    /**
     * Get production countries
     */
    fun getProductionCountries(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbProductionCountryResponse> =
        tmdbMovie.productionCountries

    /**
     * Get spoken languages
     */
    fun getSpokenLanguages(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbSpokenLanguageResponse> = tmdbMovie.spokenLanguages

    /**
     * Get genres
     */
    fun getGenres(): List<com.rdwatch.androidtv.network.models.tmdb.TMDbGenreResponse> = tmdbMovie.genres

    /**
     * Check if movie has video content
     */
    fun hasVideoContent(): Boolean = tmdbMovie.video

    /**
     * Create a copy with updated progress
     */
    fun withProgress(newProgress: ContentProgress): TMDbMovieContentDetail {
        return copy(progress = newProgress)
    }

    /**
     * Create a copy with updated watchlist status
     */
    fun withWatchlistStatus(inWatchlist: Boolean): TMDbMovieContentDetail {
        return copy(isInWatchlist = inWatchlist)
    }

    /**
     * Create a copy with updated like status
     */
    fun withLikeStatus(liked: Boolean): TMDbMovieContentDetail {
        return copy(isLiked = liked)
    }

    /**
     * Create a copy with updated download status
     */
    fun withDownloadStatus(
        downloaded: Boolean,
        downloading: Boolean = false,
    ): TMDbMovieContentDetail {
        return copy(isDownloaded = downloaded, isDownloading = downloading)
    }

    /**
     * Create a copy with updated credits
     */
    fun withCredits(newCredits: TMDbCreditsResponse?): TMDbMovieContentDetail {
        return copy(credits = newCredits)
    }

    // Helper methods from mapper
    private fun getBackdropUrl(path: String?): String? {
        return path?.let { "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.BACKDROP_SIZE}$it" }
    }

    private fun getPosterUrl(path: String?): String? {
        return path?.let { "${TMDbMovieService.IMAGE_BASE_URL}${TMDbMovieService.POSTER_SIZE}$it" }
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

    private fun formatYear(releaseDate: String?): String? {
        return releaseDate?.takeIf { it.isNotEmpty() }?.substring(0, 4)
    }

    private fun extractCastNames(
        credits: TMDbCreditsResponse?,
        limit: Int = 5,
    ): List<String> {
        return credits?.cast?.take(limit)?.map { it.name } ?: emptyList()
    }

    private fun extractDirector(credits: TMDbCreditsResponse?): String? {
        return credits?.crew?.firstOrNull { it.job == "Director" }?.name
    }

    private fun extractGenreNames(genres: List<com.rdwatch.androidtv.network.models.tmdb.TMDbGenreResponse>): List<String> {
        return genres.map { it.name }
    }

    private fun extractProductionCompanyNames(
        companies: List<com.rdwatch.androidtv.network.models.tmdb.TMDbProductionCompanyResponse>,
    ): List<String> {
        return companies.map { it.name }
    }

    private fun extractSpokenLanguageNames(
        languages: List<com.rdwatch.androidtv.network.models.tmdb.TMDbSpokenLanguageResponse>,
    ): List<String> {
        return languages.map { it.name }
    }

    private fun extractFullCast(
        credits: TMDbCreditsResponse?,
        limit: Int = 20,
    ): List<CastMember> {
        return credits?.cast?.take(limit)?.map { castMember ->
            CastMember(
                id = castMember.id,
                name = castMember.name,
                character = castMember.character,
                profileImageUrl = CastMember.buildProfileImageUrl(castMember.profilePath),
                order = castMember.order,
            )
        } ?: emptyList()
    }

    private fun extractFullCrew(credits: TMDbCreditsResponse?): List<CrewMember> {
        return credits?.crew?.filter { crewMember ->
            CrewMember.isKeyRole(crewMember.job)
        }?.map { crewMember ->
            CrewMember(
                id = crewMember.id,
                name = crewMember.name,
                job = crewMember.job,
                department = crewMember.department,
                profileImageUrl = CrewMember.buildProfileImageUrl(crewMember.profilePath),
            )
        } ?: emptyList()
    }

    private fun createContentActions(
        isInWatchlist: Boolean,
        isLiked: Boolean,
        isDownloaded: Boolean,
        isDownloading: Boolean,
        hasProgress: Boolean,
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
