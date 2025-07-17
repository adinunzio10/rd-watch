package com.rdwatch.androidtv.ui.details.models

/**
 * Implementation of ContentDetail for TV shows
 * Provides TV show-specific functionality and metadata
 */
data class TVShowContentDetail(
    private val tvShowDetail: TVShowDetail,
    private val progress: ContentProgress = ContentProgress(),
    private val actionOverrides: List<ContentAction> = emptyList(),
) : ContentDetail {
    override val id: String = tvShowDetail.id
    override val title: String = tvShowDetail.title
    override val description: String? = tvShowDetail.overview
    override val backgroundImageUrl: String? = tvShowDetail.backdropPath
    override val cardImageUrl: String? = tvShowDetail.posterPath
    override val contentType: ContentType = ContentType.TV_SHOW
    override val videoUrl: String? = null // TV shows don't have a single video URL

    override val metadata: ContentMetadata =
        ContentMetadata(
            year = tvShowDetail.firstAirDate?.take(4),
            duration = tvShowDetail.getFormattedRuntime(),
            rating = if (tvShowDetail.voteAverage > 0) "${tvShowDetail.voteAverage}/10" else null,
            genre = tvShowDetail.genres,
            studio = tvShowDetail.networks.firstOrNull(),
            cast = tvShowDetail.cast,
            season = null, // Not applicable for TV show overview
            episode = null, // Not applicable for TV show overview
            customMetadata =
                mapOf(
                    "status" to (tvShowDetail.status ?: ""),
                    "type" to (tvShowDetail.type ?: ""),
                    "seasons" to tvShowDetail.numberOfSeasons.toString(),
                    "episodes" to tvShowDetail.numberOfEpisodes.toString(),
                    "creators" to tvShowDetail.creators.joinToString(", "),
                    "networks" to tvShowDetail.networks.joinToString(", "),
                    "countries" to tvShowDetail.originCountry.joinToString(", "),
                    "languages" to tvShowDetail.languages.joinToString(", "),
                    "tagline" to (tvShowDetail.tagline ?: ""),
                    "homepage" to (tvShowDetail.homepage ?: ""),
                    "popularity" to tvShowDetail.popularity.toString(),
                    "vote_count" to tvShowDetail.voteCount.toString(),
                    "adult" to tvShowDetail.adult.toString(),
                    "in_production" to tvShowDetail.inProduction.toString(),
                    "air_date_range" to (tvShowDetail.getFormattedAirDateRange() ?: ""),
                    "episode_count" to tvShowDetail.getFormattedCount(),
                ).filterValues { it.isNotBlank() },
        )

    /**
     * Get extended metadata with full cast and crew information
     */
    val extendedMetadata: ExtendedContentMetadata =
        ExtendedContentMetadata(
            year = tvShowDetail.firstAirDate?.take(4),
            duration = tvShowDetail.getFormattedRuntime(),
            rating = if (tvShowDetail.voteAverage > 0) "${tvShowDetail.voteAverage}/10" else null,
            genre = tvShowDetail.genres,
            studio = tvShowDetail.networks.firstOrNull(),
            cast = tvShowDetail.cast,
            fullCast = tvShowDetail.fullCast,
            director = tvShowDetail.crew.find { it.job == "Director" }?.name,
            crew = tvShowDetail.crew,
            season = null, // Not applicable for TV show overview
            episode = null, // Not applicable for TV show overview
            customMetadata =
                mapOf(
                    "status" to (tvShowDetail.status ?: ""),
                    "type" to (tvShowDetail.type ?: ""),
                    "seasons" to tvShowDetail.numberOfSeasons.toString(),
                    "episodes" to tvShowDetail.numberOfEpisodes.toString(),
                    "creators" to tvShowDetail.creators.joinToString(", "),
                    "networks" to tvShowDetail.networks.joinToString(", "),
                    "countries" to tvShowDetail.originCountry.joinToString(", "),
                    "languages" to tvShowDetail.languages.joinToString(", "),
                    "tagline" to (tvShowDetail.tagline ?: ""),
                    "homepage" to (tvShowDetail.homepage ?: ""),
                    "popularity" to tvShowDetail.popularity.toString(),
                    "vote_count" to tvShowDetail.voteCount.toString(),
                    "adult" to tvShowDetail.adult.toString(),
                    "in_production" to tvShowDetail.inProduction.toString(),
                    "air_date_range" to (tvShowDetail.getFormattedAirDateRange() ?: ""),
                    "episode_count" to tvShowDetail.getFormattedCount(),
                ).filterValues { it.isNotBlank() },
        )

    override val actions: List<ContentAction> =
        if (actionOverrides.isNotEmpty()) {
            actionOverrides
        } else {
            buildDefaultActions()
        }

    /**
     * Get the underlying TV show detail
     */
    fun getTVShowDetail(): TVShowDetail = tvShowDetail

    /**
     * Get current watching progress
     */
    fun getProgress(): ContentProgress = progress

    /**
     * Get next episode to watch
     */
    fun getNextEpisode(): TVEpisode? = tvShowDetail.getNextUnwatchedEpisode()

    /**
     * Get current episode being watched
     */
    fun getCurrentEpisode(): TVEpisode? = tvShowDetail.getCurrentWatchingEpisode()

    /**
     * Get latest season
     */
    fun getLatestSeason(): TVSeason? = tvShowDetail.getLatestSeason()

    /**
     * Get all seasons
     */
    fun getSeasons(): List<TVSeason> = tvShowDetail.seasons

    /**
     * Get specific season by number
     */
    fun getSeasonByNumber(seasonNumber: Int): TVSeason? {
        return tvShowDetail.seasons.find { it.seasonNumber == seasonNumber }
    }

    /**
     * Get episodes for a specific season
     */
    fun getEpisodesForSeason(seasonNumber: Int): List<TVEpisode> {
        return getSeasonByNumber(seasonNumber)?.episodes ?: emptyList()
    }

    /**
     * Check if show is currently airing
     */
    fun isCurrentlyAiring(): Boolean = tvShowDetail.isCurrentlyAiring()

    /**
     * Check if show has multiple seasons
     */
    fun hasMultipleSeasons(): Boolean = tvShowDetail.numberOfSeasons > 1

    /**
     * Get overall watch progress across all seasons
     */
    fun getOverallWatchProgress(): Float = tvShowDetail.getOverallWatchProgress()

    /**
     * Get formatted status text
     */
    fun getStatusText(): String {
        return when (tvShowDetail.status) {
            "Returning Series" -> if (tvShowDetail.inProduction) "New episodes coming" else "Returning Series"
            "Ended" -> "Series ended"
            "Cancelled" -> "Cancelled"
            else -> tvShowDetail.status ?: "Unknown"
        }
    }

    /**
     * Get last episode aired information
     */
    fun getLastEpisodeInfo(): String? {
        return tvShowDetail.lastEpisodeToAir?.let { episode ->
            "Last episode: S${episode.seasonNumber}E${episode.episodeNumber} - ${episode.title}"
        }
    }

    /**
     * Get next episode air information
     */
    fun getNextEpisodeInfo(): String? {
        return tvShowDetail.nextEpisodeToAir?.let { episode ->
            "Next episode: S${episode.seasonNumber}E${episode.episodeNumber} - ${episode.title}"
        }
    }

    override fun getDisplayTitle(): String {
        return if (tvShowDetail.originalTitle != null && tvShowDetail.originalTitle != tvShowDetail.title) {
            "${tvShowDetail.title} (${tvShowDetail.originalTitle})"
        } else {
            tvShowDetail.title
        }
    }

    override fun getDisplayDescription(): String {
        return tvShowDetail.overview ?: "No description available"
    }

    override fun getPrimaryImageUrl(): String? {
        return tvShowDetail.backdropPath ?: tvShowDetail.posterPath
    }

    override fun isPlayable(): Boolean {
        // TV shows are playable if they have at least one episode with a video URL
        return tvShowDetail.seasons.any { season ->
            season.episodes.any { episode -> episode.videoUrl != null }
        }
    }

    override fun getMetadataChips(): List<MetadataChip> {
        val chips = mutableListOf<MetadataChip>()

        // Add year range
        tvShowDetail.getFormattedAirDateRange()?.let { chips.add(MetadataChip.Year(it)) }

        // Add rating
        if (tvShowDetail.voteAverage > 0) {
            chips.add(MetadataChip.Rating("${tvShowDetail.voteAverage}/10"))
        }

        // Add status
        tvShowDetail.status?.let { chips.add(MetadataChip.Custom(it)) }

        // Add episode count
        chips.add(MetadataChip.Custom(tvShowDetail.getFormattedCount()))

        // Add runtime
        tvShowDetail.getFormattedRuntime()?.let { chips.add(MetadataChip.Duration(it)) }

        // Add network
        tvShowDetail.networks.firstOrNull()?.let { chips.add(MetadataChip.Studio(it)) }

        // Add genres
        tvShowDetail.genres.take(2).forEach { genre ->
            chips.add(MetadataChip.Genre(genre))
        }

        return chips
    }

    /**
     * Build default actions for TV show
     */
    private fun buildDefaultActions(): List<ContentAction> {
        val actions = mutableListOf<ContentAction>()

        // Add play/resume action
        val currentEpisode = getCurrentEpisode()
        val nextEpisode = getNextEpisode()

        when {
            currentEpisode != null -> {
                actions.add(ContentAction.Play(isResume = true))
            }
            nextEpisode != null -> {
                actions.add(ContentAction.Play(isResume = false))
            }
            isPlayable() -> {
                actions.add(ContentAction.Play(isResume = false))
            }
        }

        // Add watchlist action
        actions.add(ContentAction.AddToWatchlist())

        // Add like action
        actions.add(ContentAction.Like())

        // Add share action
        actions.add(ContentAction.Share())

        // Add download action if episodes are available
        if (isPlayable()) {
            actions.add(ContentAction.Download())
        }

        return actions
    }

    /**
     * Create a copy with updated progress
     */
    fun withProgress(newProgress: ContentProgress): TVShowContentDetail {
        return copy(progress = newProgress)
    }

    /**
     * Create a copy with updated actions
     */
    fun withActions(newActions: List<ContentAction>): TVShowContentDetail {
        return copy(actionOverrides = newActions)
    }

    /**
     * Create a copy with updated TV show detail
     */
    fun withTVShowDetail(newTVShowDetail: TVShowDetail): TVShowContentDetail {
        return copy(tvShowDetail = newTVShowDetail)
    }

    companion object {
        /**
         * Create TVShowContentDetail from TVShowDetail
         */
        fun from(
            tvShowDetail: TVShowDetail,
            progress: ContentProgress = ContentProgress(),
            actions: List<ContentAction> = emptyList(),
        ): TVShowContentDetail {
            return TVShowContentDetail(
                tvShowDetail = tvShowDetail,
                progress = progress,
                actionOverrides = actions,
            )
        }

        /**
         * Create a demo TVShowContentDetail for previews
         */
        fun createDemo(): TVShowContentDetail {
            val demoEpisodes =
                listOf(
                    TVEpisode(
                        id = "1",
                        seasonNumber = 1,
                        episodeNumber = 1,
                        title = "Pilot",
                        description = "The beginning of an epic story",
                        thumbnailUrl = "https://image.tmdb.org/t/p/w500/demo1.jpg",
                        airDate = "2023-01-01",
                        runtime = 45,
                        stillPath = null,
                        isWatched = true,
                        watchProgress = 1.0f,
                    ),
                    TVEpisode(
                        id = "2",
                        seasonNumber = 1,
                        episodeNumber = 2,
                        title = "The Mystery Deepens",
                        description = "Our heroes face their first challenge",
                        thumbnailUrl = "https://image.tmdb.org/t/p/w500/demo2.jpg",
                        airDate = "2023-01-08",
                        runtime = 42,
                        stillPath = null,
                        isWatched = false,
                        watchProgress = 0.3f,
                    ),
                )

            val demoSeason =
                TVSeason(
                    id = "s1",
                    seasonNumber = 1,
                    name = "Season 1",
                    overview = "The first season of this amazing show",
                    posterPath = "https://image.tmdb.org/t/p/w500/season1.jpg",
                    airDate = "2023-01-01",
                    episodeCount = 10,
                    episodes = demoEpisodes,
                )

            val demoTVShow =
                TVShowDetail(
                    id = "demo-show",
                    title = "Demo TV Show",
                    originalTitle = "Demo TV Show",
                    overview = "This is a demo TV show for preview purposes",
                    posterPath = "https://image.tmdb.org/t/p/w500/demo-poster.jpg",
                    backdropPath = "https://image.tmdb.org/t/p/w1280/demo-backdrop.jpg",
                    firstAirDate = "2023-01-01",
                    lastAirDate = null,
                    status = "Returning Series",
                    type = "Scripted",
                    genres = listOf("Drama", "Mystery"),
                    numberOfSeasons = 2,
                    numberOfEpisodes = 20,
                    seasons = listOf(demoSeason),
                    networks = listOf("Demo Network"),
                    voteAverage = 8.5f,
                    voteCount = 1234,
                )

            return TVShowContentDetail.from(demoTVShow)
        }
    }
}
