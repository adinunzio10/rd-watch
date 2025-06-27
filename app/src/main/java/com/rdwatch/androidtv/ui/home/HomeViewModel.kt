package com.rdwatch.androidtv.ui.home

import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.data.mappers.ContentEntityToMovieMapper.toMovies
import com.rdwatch.androidtv.data.mappers.ContentEntityToMovieMapper.findMovieById
import com.rdwatch.androidtv.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Home Screen - combines local and RD content with filtering
 * Follows MVVM architecture with BaseViewModel pattern
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val realDebridContentRepository: RealDebridContentRepository
) : BaseViewModel<HomeUiState>() {
    
    private val _contentState = MutableStateFlow<UiState<HomeContent>>(UiState.Loading)
    val contentState: StateFlow<UiState<HomeContent>> = _contentState.asStateFlow()
    
    private val _allMovies = MutableStateFlow<List<Movie>>(emptyList())
    
    override fun createInitialState(): HomeUiState {
        return HomeUiState()
    }
    
    init {
        loadContent()
    }
    
    /**
     * Load all content and organize by categories
     */
    private fun loadContent() {
        viewModelScope.launch {
            _contentState.value = UiState.Loading
            updateState { copy(isLoading = true, error = null) }
            
            realDebridContentRepository.getAllContent()
                .catch { e ->
                    _contentState.value = UiState.Error(
                        message = "Failed to load content: ${e.message}",
                        throwable = e
                    )
                    updateState { 
                        copy(
                            isLoading = false,
                            error = "Failed to load content: ${e.message}"
                        )
                    }
                }
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val movies = result.data.toMovies()
                            _allMovies.value = movies
                            val filteredContent = filterContent(movies, uiState.value.contentFilter)
                            
                            _contentState.value = UiState.Success(filteredContent)
                            updateState { 
                                copy(
                                    isLoading = false,
                                    error = null
                                )
                            }
                        }
                        is Result.Error -> {
                            _contentState.value = UiState.Error(
                                message = "Failed to load content: ${result.exception.message}",
                                throwable = result.exception
                            )
                            updateState { 
                                copy(
                                    isLoading = false,
                                    error = "Failed to load content: ${result.exception.message}"
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
     * Update content filter (all/local/rd)
     */
    fun updateContentFilter(filter: ContentFilter) {
        updateState { copy(contentFilter = filter) }
        
        val allMovies = _allMovies.value
        val filteredContent = filterContent(allMovies, filter)
        
        _contentState.value = UiState.Success(filteredContent)
    }
    
    /**
     * Search content
     */
    fun searchContent(query: String) {
        viewModelScope.launch {
            updateState { copy(searchQuery = query, isLoading = true) }
            
            if (query.isBlank()) {
                // Reset to filtered content
                val allMovies = _allMovies.value
                val filteredContent = filterContent(allMovies, uiState.value.contentFilter)
                _contentState.value = UiState.Success(filteredContent)
                updateState { copy(isLoading = false) }
            } else {
                realDebridContentRepository.searchContent(query)
                    .catch { e ->
                        _contentState.value = UiState.Error(
                            message = "Search failed: ${e.message}",
                            throwable = e
                        )
                        updateState { 
                            copy(
                                isLoading = false,
                                error = "Search failed: ${e.message}"
                            )
                        }
                    }
                    .collect { result ->
                        when (result) {
                            is Result.Success -> {
                                val movies = result.data.toMovies()
                                val filteredResults = filterContent(movies, uiState.value.contentFilter)
                                _contentState.value = UiState.Success(filteredResults)
                                updateState { copy(isLoading = false, error = null) }
                            }
                            is Result.Error -> {
                                _contentState.value = UiState.Error(
                                    message = "Search failed: ${result.exception.message}",
                                    throwable = result.exception
                                )
                                updateState { 
                                    copy(
                                        isLoading = false,
                                        error = "Search failed: ${result.exception.message}"
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
     * Refresh content
     */
    fun refresh() {
        viewModelScope.launch {
            updateState { copy(isRefreshing = true, error = null) }
            
            when (val result = realDebridContentRepository.syncContent()) {
                is Result.Success -> {
                    loadContent()
                    updateState { copy(isRefreshing = false) }
                }
                is Result.Error -> {
                    _contentState.value = UiState.Error(
                        message = "Refresh failed: ${result.exception.message}",
                        throwable = result.exception
                    )
                    updateState { 
                        copy(
                            isRefreshing = false,
                            error = "Refresh failed: ${result.exception.message}"
                        )
                    }
                }
                is Result.Loading -> {
                    // Keep refreshing state
                }
            }
        }
    }
    
    /**
     * Filter content based on source
     */
    private fun filterContent(movies: List<Movie>, filter: ContentFilter): HomeContent {
        val filteredMovies = when (filter) {
            ContentFilter.ALL -> movies
            ContentFilter.LOCAL -> movies.filter { 
                it.studio?.contains("RealDebrid", ignoreCase = true) != true 
            }
            ContentFilter.REAL_DEBRID -> movies.filter { 
                it.studio?.contains("RealDebrid", ignoreCase = true) == true 
            }
        }
        
        return HomeContent(
            featured = filteredMovies.take(5),
            recentlyAdded = filteredMovies.sortedByDescending { it.id }.take(10),
            continueWatching = emptyList(), // TODO: Implement when playback progress is available
            byGenre = organizeByGenre(filteredMovies),
            allContent = filteredMovies
        )
    }
    
    /**
     * Organize movies by genre
     */
    private fun organizeByGenre(movies: List<Movie>): Map<String, List<Movie>> {
        val genres = mutableMapOf<String, MutableList<Movie>>()
        
        movies.forEach { movie ->
            // Extract genre from studio or title for now
            val genre = when {
                movie.studio?.contains("Action", ignoreCase = true) == true -> "Action"
                movie.studio?.contains("Drama", ignoreCase = true) == true -> "Drama"
                movie.studio?.contains("Comedy", ignoreCase = true) == true -> "Comedy"
                movie.studio?.contains("Thriller", ignoreCase = true) == true -> "Thriller"
                movie.studio?.contains("Documentary", ignoreCase = true) == true -> "Documentary"
                else -> "Other"
            }
            
            genres.getOrPut(genre) { mutableListOf() }.add(movie)
        }
        
        return genres.mapValues { it.value.take(10) } // Limit to 10 per genre
    }
    
    /**
     * Find a movie by ID for navigation purposes
     */
    fun findMovieById(movieId: Long): Movie? {
        return _allMovies.value.find { it.id == movieId }
    }
    
    override fun handleError(exception: Throwable) {
        _contentState.value = UiState.Error(
            message = "An error occurred: ${exception.message}",
            throwable = exception
        )
        updateState { 
            copy(
                isLoading = false,
                isRefreshing = false,
                error = "An error occurred: ${exception.message}"
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
    val error: String? = null
) {
    val hasSearch: Boolean get() = searchQuery.isNotBlank()
}

/**
 * Content filter options
 */
enum class ContentFilter {
    ALL,
    LOCAL,
    REAL_DEBRID
}

/**
 * Home screen content organization
 */
data class HomeContent(
    val featured: List<Movie> = emptyList(),
    val recentlyAdded: List<Movie> = emptyList(),
    val continueWatching: List<Movie> = emptyList(),
    val byGenre: Map<String, List<Movie>> = emptyMap(),
    val allContent: List<Movie> = emptyList()
) {
    val hasFeatured: Boolean get() = featured.isNotEmpty()
    val hasRecentlyAdded: Boolean get() = recentlyAdded.isNotEmpty()
    val hasContinueWatching: Boolean get() = continueWatching.isNotEmpty()
    val hasGenres: Boolean get() = byGenre.isNotEmpty()
    val hasContent: Boolean get() = allContent.isNotEmpty()
}