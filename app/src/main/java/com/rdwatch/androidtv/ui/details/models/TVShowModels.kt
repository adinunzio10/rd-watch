package com.rdwatch.androidtv.ui.details.models

/**
 * Data model representing a TV show episode
 */
data class TVEpisode(
    val id: String,
    val seasonNumber: Int,
    val episodeNumber: Int,
    val title: String,
    val description: String?,
    val thumbnailUrl: String?,
    val airDate: String?,
    val runtime: Int?, // in minutes
    val stillPath: String?,
    val voteAverage: Float = 0f,
    val voteCount: Int = 0,
    val overview: String? = null,
    val isWatched: Boolean = false,
    val watchProgress: Float = 0f, // 0.0 to 1.0
    val resumePosition: Long = 0L, // in milliseconds
    val videoUrl: String? = null
) {
    /**
     * Get formatted episode title with episode number
     */
    fun getFormattedTitle(): String {
        return "E${episodeNumber.toString().padStart(2, '0')} • $title"
    }
    
    /**
     * Get episode description or fallback
     */
    fun getDisplayDescription(): String {
        return description ?: overview ?: "No description available"
    }
    
    /**
     * Get formatted runtime
     */
    fun getFormattedRuntime(): String? {
        return runtime?.let { "${it}m" }
    }
    
    /**
     * Get progress percentage as string
     */
    fun getProgressText(): String {
        return when {
            isWatched -> "Watched"
            watchProgress > 0f -> "${(watchProgress * 100).toInt()}%"
            else -> ""
        }
    }
    
    /**
     * Check if episode has progress
     */
    fun hasProgress(): Boolean = watchProgress > 0f || isWatched
    
    /**
     * Check if episode is partially watched
     */
    fun isPartiallyWatched(): Boolean = watchProgress > 0f && !isWatched
}

/**
 * Data model representing a TV show season
 */
data class TVSeason(
    val id: String,
    val seasonNumber: Int,
    val name: String,
    val overview: String?,
    val posterPath: String?,
    val airDate: String?,
    val episodeCount: Int,
    val episodes: List<TVEpisode> = emptyList(),
    val voteAverage: Float = 0f
) {
    /**
     * Get formatted season title
     */
    fun getFormattedTitle(): String {
        return if (seasonNumber == 0) {
            "Specials"
        } else {
            "Season $seasonNumber"
        }
    }
    
    /**
     * Get season description or fallback
     */
    fun getDisplayDescription(): String {
        return overview ?: "Season $seasonNumber"
    }
    
    /**
     * Get watched episodes count
     */
    fun getWatchedEpisodesCount(): Int {
        return episodes.count { it.isWatched }
    }
    
    /**
     * Get season watch progress (0.0 to 1.0)
     */
    fun getWatchProgress(): Float {
        if (episodes.isEmpty()) return 0f
        return getWatchedEpisodesCount() / episodes.size.toFloat()
    }
    
    /**
     * Check if season is fully watched
     */
    fun isFullyWatched(): Boolean = episodes.isNotEmpty() && episodes.all { it.isWatched }
    
    /**
     * Check if season has any progress
     */
    fun hasProgress(): Boolean = episodes.any { it.hasProgress() }
    
    /**
     * Get formatted episode count
     */
    fun getFormattedEpisodeCount(): String {
        return "$episodeCount episode${if (episodeCount != 1) "s" else ""}"
    }
}

/**
 * Data model representing detailed TV show information
 */
data class TVShowDetail(
    val id: String,
    val title: String,
    val originalTitle: String?,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val firstAirDate: String?,
    val lastAirDate: String?,
    val status: String?, // "Returning Series", "Ended", "Cancelled", etc.
    val type: String?, // "Documentary", "Reality", "Scripted", etc.
    val genres: List<String> = emptyList(),
    val languages: List<String> = emptyList(),
    val originCountry: List<String> = emptyList(),
    val numberOfSeasons: Int = 0,
    val numberOfEpisodes: Int = 0,
    val seasons: List<TVSeason> = emptyList(),
    val networks: List<String> = emptyList(),
    val productionCompanies: List<String> = emptyList(),
    val creators: List<String> = emptyList(),
    val cast: List<String> = emptyList(),
    val fullCast: List<CastMember> = emptyList(),
    val crew: List<CrewMember> = emptyList(),
    val voteAverage: Float = 0f,
    val voteCount: Int = 0,
    val popularity: Float = 0f,
    val adult: Boolean = false,
    val homepage: String? = null,
    val tagline: String? = null,
    val inProduction: Boolean = false,
    val imdbId: String? = null,
    val episodeRunTime: List<Int> = emptyList(),
    val lastEpisodeToAir: TVEpisode? = null,
    val nextEpisodeToAir: TVEpisode? = null
) {
    /**
     * Get formatted air date range
     */
    fun getFormattedAirDateRange(): String? {
        return when {
            firstAirDate != null && lastAirDate != null -> "$firstAirDate - $lastAirDate"
            firstAirDate != null -> "$firstAirDate - Present"
            else -> null
        }
    }
    
    /**
     * Get formatted runtime
     */
    fun getFormattedRuntime(): String? {
        return episodeRunTime.firstOrNull()?.let { "${it}m" }
    }
    
    /**
     * Get formatted season/episode count
     */
    fun getFormattedCount(): String {
        return "$numberOfSeasons season${if (numberOfSeasons != 1) "s" else ""} • $numberOfEpisodes episode${if (numberOfEpisodes != 1) "s" else ""}"
    }
    
    /**
     * Get overall watch progress
     */
    fun getOverallWatchProgress(): Float {
        if (seasons.isEmpty()) return 0f
        val totalProgress = seasons.sumOf { it.getWatchProgress().toDouble() }
        return (totalProgress / seasons.size).toFloat()
    }
    
    /**
     * Check if show is currently airing
     */
    fun isCurrentlyAiring(): Boolean {
        return status == "Returning Series" || inProduction
    }
    
    /**
     * Get latest available season
     */
    fun getLatestSeason(): TVSeason? {
        return seasons.maxByOrNull { it.seasonNumber }
    }
    
    /**
     * Get next unwatched episode
     */
    fun getNextUnwatchedEpisode(): TVEpisode? {
        return seasons.sortedBy { it.seasonNumber }
            .flatMap { it.episodes.sortedBy { episode -> episode.episodeNumber } }
            .firstOrNull { !it.isWatched }
    }
    
    /**
     * Get current watching episode (partially watched)
     */
    fun getCurrentWatchingEpisode(): TVEpisode? {
        return seasons.sortedBy { it.seasonNumber }
            .flatMap { it.episodes.sortedBy { episode -> episode.episodeNumber } }
            .firstOrNull { it.isPartiallyWatched() }
    }
}

/**
 * UI state for episode grid
 */
data class EpisodeGridUiState(
    val isLoading: Boolean = false,
    val selectedSeasonNumber: Int = 1,
    val availableSeasons: List<TVSeason> = emptyList(),
    val currentSeasonEpisodes: List<TVEpisode> = emptyList(),
    val focusedEpisodeId: String? = null,
    val error: String? = null,
    val isRefreshing: Boolean = false
) {
    /**
     * Get currently selected season
     */
    fun getCurrentSeason(): TVSeason? {
        return availableSeasons.find { it.seasonNumber == selectedSeasonNumber }
    }
    
    /**
     * Check if there are episodes to display
     */
    fun hasEpisodes(): Boolean = currentSeasonEpisodes.isNotEmpty()
    
    /**
     * Check if in error state
     */
    fun isInError(): Boolean = error != null
    
    /**
     * Check if should show loading state
     */
    fun shouldShowLoading(): Boolean = isLoading && currentSeasonEpisodes.isEmpty()
    
    /**
     * Check if loading episodes for current season
     */
    fun isLoadingCurrentSeason(): Boolean = isLoading && currentSeasonEpisodes.isEmpty()
    
    /**
     * Check if refreshing episodes (already has some episodes)
     */
    fun isRefreshingEpisodes(): Boolean = (isLoading || isRefreshing) && currentSeasonEpisodes.isNotEmpty()
    
    /**
     * Check if has episodes to display
     */
    fun hasEpisodesToDisplay(): Boolean = currentSeasonEpisodes.isNotEmpty()
    
    /**
     * Get loading message based on current state
     */
    fun getLoadingMessage(): String = when {
        isRefreshingEpisodes() -> "Refreshing episodes..."
        currentSeasonEpisodes.isEmpty() -> "Loading season details and episodes..."
        else -> "Loading more episodes..."
    }
    
    /**
     * Get formatted season title for selector
     */
    fun getSeasonSelectorTitle(): String {
        val season = getCurrentSeason()
        return season?.getFormattedTitle() ?: "Season $selectedSeasonNumber"
    }
}

/**
 * Episodes pagination state
 */
data class EpisodePaginationState(
    val currentPage: Int = 1,
    val totalPages: Int = 1,
    val pageSize: Int = 20,
    val totalEpisodes: Int = 0,
    val hasNextPage: Boolean = false,
    val hasPreviousPage: Boolean = false,
    val isLoadingNextPage: Boolean = false
) {
    /**
     * Check if pagination is needed
     */
    fun needsPagination(): Boolean = totalEpisodes > pageSize
    
    /**
     * Get start index for current page
     */
    fun getStartIndex(): Int = (currentPage - 1) * pageSize
    
    /**
     * Get end index for current page
     */
    fun getEndIndex(): Int = minOf(getStartIndex() + pageSize, totalEpisodes)
    
    /**
     * Get formatted page info
     */
    fun getPageInfo(): String {
        return "Page $currentPage of $totalPages"
    }
}