package com.rdwatch.androidtv.ui.home

import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.data.repository.PlaybackProgressRepository
import com.rdwatch.androidtv.data.repository.TMDbSearchRepository
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.details.models.ContentDetail
import com.rdwatch.androidtv.ui.details.models.ContentType
import com.rdwatch.androidtv.ui.details.models.TMDbMovieContentDetail
import com.rdwatch.androidtv.ui.details.models.TMDbTVContentDetail
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home Screen - shows TMDb trending and popular content
 * Follows MVVM architecture with BaseViewModel pattern
 */
@HiltViewModel
class HomeViewModel
    @Inject
    constructor(
        private val tmdbSearchRepository: TMDbSearchRepository,
        private val playbackProgressRepository: PlaybackProgressRepository,
    ) : BaseViewModel<HomeUiState>() {
        private val _contentState = MutableStateFlow<UiState<HomeContent>>(UiState.Loading)
        val contentState: StateFlow<UiState<HomeContent>> = _contentState.asStateFlow()

        private val _allContent = MutableStateFlow<List<ContentDetail>>(emptyList())

        override fun createInitialState(): HomeUiState {
            return HomeUiState()
        }

        init {
            loadContent()
        }

        /**
         * Load trending content from TMDb and organize by categories
         */
        private fun loadContent() {
            viewModelScope.launch {
                _contentState.value = UiState.Loading
                updateState { copy(isLoading = true, error = null) }

                // Load trending content from TMDb
                tmdbSearchRepository.getTrendingAsContentDetails(
                    mediaType = "all", // Get both movies and TV shows
                    timeWindow = "day",
                )
                    .catch { e ->
                        android.util.Log.e("HomeViewModel", "Failed to load trending content", e)
                        _contentState.value =
                            UiState.Error(
                                message = "Failed to load content: ${e.message}",
                                throwable = e,
                            )
                        updateState {
                            copy(
                                isLoading = false,
                                error = "Failed to load content: ${e.message}",
                            )
                        }
                    }
                    .collect { result ->
                        when (result) {
                            is Result.Success -> {
                                android.util.Log.d("HomeViewModel", "Loaded ${result.data.size} trending content items")
                                _allContent.value = result.data
                                val homeContent = organizeContent(result.data)

                                _contentState.value = UiState.Success(homeContent)
                                updateState {
                                    copy(
                                        isLoading = false,
                                        error = null,
                                    )
                                }
                            }
                            is Result.Error -> {
                                android.util.Log.e("HomeViewModel", "Failed to load trending content", result.exception)
                                _contentState.value =
                                    UiState.Error(
                                        message = "Failed to load content: ${result.exception.message}",
                                        throwable = result.exception,
                                    )
                                updateState {
                                    copy(
                                        isLoading = false,
                                        error = "Failed to load content: ${result.exception.message}",
                                    )
                                }
                            }
                            is Result.Loading -> {
                                // Keep loading state
                            }
                        }
                    }
            }
        }

        /**
         * Update content filter (movies/tv shows/all)
         */
        fun updateContentFilter(filter: ContentFilter) {
            updateState { copy(contentFilter = filter) }

            viewModelScope.launch {
                val allContent = _allContent.value
                val filteredContent = filterContent(allContent, filter)
                val homeContent = organizeContent(filteredContent)

                _contentState.value = UiState.Success(homeContent)
            }
        }

        /**
         * Search content using TMDb
         */
        fun searchContent(query: String) {
            viewModelScope.launch {
                updateState { copy(searchQuery = query, isLoading = true) }

                if (query.isBlank()) {
                    // Reset to filtered content
                    val allContent = _allContent.value
                    val filteredContent = filterContent(allContent, uiState.value.contentFilter)
                    val homeContent = organizeContent(filteredContent)
                    _contentState.value = UiState.Success(homeContent)
                    updateState { copy(isLoading = false) }
                } else {
                    // Use TMDb search for home screen searches
                    tmdbSearchRepository.multiSearchAsContentDetails(
                        query = query,
                        includeAdult = false,
                    )
                        .catch { e ->
                            android.util.Log.e("HomeViewModel", "Search failed", e)
                            _contentState.value =
                                UiState.Error(
                                    message = "Search failed: ${e.message}",
                                    throwable = e,
                                )
                            updateState {
                                copy(
                                    isLoading = false,
                                    error = "Search failed: ${e.message}",
                                )
                            }
                        }
                        .collect { result ->
                            when (result) {
                                is Result.Success -> {
                                    android.util.Log.d("HomeViewModel", "Search returned ${result.data.size} results")
                                    val filteredResults = filterContent(result.data, uiState.value.contentFilter)
                                    val homeContent = organizeContent(filteredResults)
                                    _contentState.value = UiState.Success(homeContent)
                                    updateState { copy(isLoading = false, error = null) }
                                }
                                is Result.Error -> {
                                    android.util.Log.e("HomeViewModel", "Search failed", result.exception)
                                    _contentState.value =
                                        UiState.Error(
                                            message = "Search failed: ${result.exception.message}",
                                            throwable = result.exception,
                                        )
                                    updateState {
                                        copy(
                                            isLoading = false,
                                            error = "Search failed: ${result.exception.message}",
                                        )
                                    }
                                }
                                is Result.Loading -> {
                                    // Keep loading state
                                }
                            }
                        }
                }
            }
        }

        /**
         * Clear search
         */
        fun clearSearch() {
            searchContent("")
        }

        /**
         * Refresh trending content from TMDb
         */
        fun refresh() {
            viewModelScope.launch {
                updateState { copy(isRefreshing = true, error = null) }

                // Simply reload the trending content
                loadContent()
                updateState { copy(isRefreshing = false) }
            }
        }

        /**
         * Filter content based on type
         */
        private fun filterContent(
            content: List<ContentDetail>,
            filter: ContentFilter,
        ): List<ContentDetail> {
            return when (filter) {
                ContentFilter.ALL -> content
                ContentFilter.MOVIES -> content.filter { it.contentType == ContentType.MOVIE }
                ContentFilter.TV_SHOWS -> content.filter { it.contentType == ContentType.TV_SHOW }
            }
        }

        /**
         * Organize content into home screen sections
         */
        private suspend fun organizeContent(content: List<ContentDetail>): HomeContent {
            android.util.Log.d("HomeViewModel", "Organizing ${content.size} content items")

            // Get continue watching content from playback progress
            val continueWatchingContent =
                try {
                    val currentUserId = 1L // Default user ID for now
                    val inProgressContent = playbackProgressRepository.getInProgressContent(currentUserId).first()

                    // Map progress entities to content that exists in our collection
                    inProgressContent.mapNotNull { progressEntity ->
                        content.find { contentItem ->
                            contentItem.id == progressEntity.contentId ||
                                contentItem.title == progressEntity.contentId
                        }
                    }.take(10) // Limit to 10 continue watching items
                } catch (e: Exception) {
                    android.util.Log.w("HomeViewModel", "Failed to load continue watching content", e)
                    emptyList() // Fallback to empty list if progress data unavailable
                }

            // Separate movies and TV shows for better organization
            val movies = content.filter { it.contentType == ContentType.MOVIE }
            val tvShows = content.filter { it.contentType == ContentType.TV_SHOW }

            return HomeContent(
                featured = content.take(5), // Top 5 trending items as featured
                recentlyAdded = content.take(10), // Most recent trending items
                continueWatching = continueWatchingContent,
                byGenre = organizeByContentType(movies, tvShows),
                allContent = content,
            )
        }

        /**
         * Organize content by type and other categories
         */
        private fun organizeByContentType(
            movies: List<ContentDetail>,
            tvShows: List<ContentDetail>,
        ): Map<String, List<ContentDetail>> {
            val contentMap = mutableMapOf<String, List<ContentDetail>>()

            if (movies.isNotEmpty()) {
                contentMap["Trending Movies"] = movies.take(10)
            }

            if (tvShows.isNotEmpty()) {
                contentMap["Trending TV Shows"] = tvShows.take(10)
            }

            // Organize by genre if we have genre information
            val allContent = movies + tvShows
            val genreMap =
                allContent.groupBy { content ->
                    // Get the first genre from metadata, or use a default
                    content.metadata.genre.firstOrNull() ?: "Popular"
                }.filter { (_, contentList) -> contentList.isNotEmpty() }

            // Add top genres
            genreMap.entries.sortedByDescending { it.value.size }
                .take(5) // Limit to top 5 genres
                .forEach { (genre, contentList) ->
                    if (genre != "Popular") { // Don't duplicate if we already have "Popular"
                        contentMap[genre] = contentList.take(10)
                    }
                }

            return contentMap
        }

        /**
         * Find content by ID for navigation purposes
         */
        fun findContentById(contentId: String): ContentDetail? {
            return _allContent.value.find { it.id == contentId }
        }

        /**
         * Get content type by movie ID for navigation routing
         */
        fun getContentTypeByMovieId(movieId: String): ContentType {
            val movieIdLong = movieId.toLongOrNull() ?: 0L

            // Find the corresponding ContentDetail by matching the converted Movie ID
            val contentDetail =
                _allContent.value.find { content ->
                    when (content) {
                        is com.rdwatch.androidtv.ui.details.models.TMDbMovieContentDetail -> content.tmdbId.toLong() == movieIdLong
                        is com.rdwatch.androidtv.ui.details.models.TMDbTVContentDetail -> content.tmdbId.toLong() == movieIdLong
                        else -> content.id.hashCode().toLong() == movieIdLong
                    }
                }

            return contentDetail?.contentType ?: ContentType.MOVIE // Default to movie if not found
        }

        /**
         * Convert ContentDetail to Movie for UI compatibility
         */
        private fun ContentDetail.toMovie(): Movie {
            return Movie(
                id =
                    when (this) {
                        is com.rdwatch.androidtv.ui.details.models.TMDbMovieContentDetail -> this.tmdbId.toLong()
                        is com.rdwatch.androidtv.ui.details.models.TMDbTVContentDetail -> this.tmdbId.toLong()
                        else -> this.id.hashCode().toLong()
                    },
                title = this.title,
                description = this.description,
                cardImageUrl = this.cardImageUrl,
                backgroundImageUrl = this.backgroundImageUrl,
                videoUrl = this.videoUrl, // Will be null for TMDb content - needs scraper integration
                studio = this.metadata.studio ?: "TMDb",
            )
        }

        /**
         * Convert list of ContentDetail to Movie list
         */
        private fun List<ContentDetail>.toMovies(): List<Movie> {
            return this.map { it.toMovie() }
        }

        override fun handleError(exception: Throwable) {
            _contentState.value =
                UiState.Error(
                    message = "An error occurred: ${exception.message}",
                    throwable = exception,
                )
            updateState {
                copy(
                    isLoading = false,
                    isRefreshing = false,
                    error = "An error occurred: ${exception.message}",
                )
            }
        }
    }

/**
 * UI State for Home Screen
 */
data class HomeUiState(
    val contentFilter: ContentFilter = ContentFilter.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val error: String? = null,
) {
    val hasSearch: Boolean get() = searchQuery.isNotBlank()
}

/**
 * Content filter options
 */
enum class ContentFilter {
    ALL,
    MOVIES,
    TV_SHOWS,
}

/**
 * Home screen content organization
 */
data class HomeContent(
    val featured: List<ContentDetail> = emptyList(),
    val recentlyAdded: List<ContentDetail> = emptyList(),
    val continueWatching: List<ContentDetail> = emptyList(),
    val byGenre: Map<String, List<ContentDetail>> = emptyMap(),
    val allContent: List<ContentDetail> = emptyList(),
) {
    val hasFeatured: Boolean get() = featured.isNotEmpty()
    val hasRecentlyAdded: Boolean get() = recentlyAdded.isNotEmpty()
    val hasContinueWatching: Boolean get() = continueWatching.isNotEmpty()
    val hasGenres: Boolean get() = byGenre.isNotEmpty()
    val hasContent: Boolean get() = allContent.isNotEmpty()

    // Temporary compatibility methods for existing UI
    fun featuredAsMovies(): List<Movie> = featured.toMovies()

    fun recentlyAddedAsMovies(): List<Movie> = recentlyAdded.toMovies()

    fun continueWatchingAsMovies(): List<Movie> = continueWatching.toMovies()

    fun byGenreAsMovies(): Map<String, List<Movie>> = byGenre.mapValues { it.value.toMovies() }

    fun allContentAsMovies(): List<Movie> = allContent.toMovies()

    private fun ContentDetail.toMovie(): Movie {
        return Movie(
            id =
                when (this) {
                    is com.rdwatch.androidtv.ui.details.models.TMDbMovieContentDetail -> this.tmdbId.toLong()
                    is com.rdwatch.androidtv.ui.details.models.TMDbTVContentDetail -> this.tmdbId.toLong()
                    else -> this.id.hashCode().toLong()
                },
            title = this.title,
            description = this.description,
            cardImageUrl = this.cardImageUrl,
            backgroundImageUrl = this.backgroundImageUrl,
            videoUrl = this.videoUrl, // Will be null for TMDb content - needs scraper integration
            studio = this.metadata.studio ?: "TMDb",
        )
    }

    private fun List<ContentDetail>.toMovies(): List<Movie> {
        return this.map { it.toMovie() }
    }
}
