package com.rdwatch.androidtv.ui.browse

import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import com.rdwatch.androidtv.repository.MovieRepository
import com.rdwatch.androidtv.ui.common.UiState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for Browse Screen - handles content discovery and category filtering
 * Follows MVVM architecture with BaseViewModel pattern
 */
@HiltViewModel
class BrowseViewModel
    @Inject
    constructor(
        private val movieRepository: MovieRepository,
    ) : BaseViewModel<BrowseUiState>() {
        private val _contentState = MutableStateFlow<UiState<List<Movie>>>(UiState.Loading)
        val contentState: StateFlow<UiState<List<Movie>>> = _contentState.asStateFlow()

        private val _allMovies = MutableStateFlow<List<Movie>>(emptyList())

        override fun createInitialState(): BrowseUiState {
            return BrowseUiState()
        }

        init {
            loadMovies()
        }

        /**
         * Load all movies from the repository
         */
        private fun loadMovies() {
            viewModelScope.launch {
                _contentState.value = UiState.Loading

                movieRepository.getAllMovies()
                    .catch { e ->
                        _contentState.value =
                            UiState.Error(
                                message = "Failed to load movies: ${e.message}",
                                throwable = e,
                            )
                        updateState {
                            copy(
                                movies = emptyList(),
                                isLoading = false,
                                error = "Failed to load movies: ${e.message}",
                            )
                        }
                    }
                    .collect { movies ->
                        _allMovies.value = movies
                        val filteredMovies = filterMoviesByCategory(movies, uiState.value.selectedCategory)

                        _contentState.value = UiState.Success(filteredMovies)
                        updateState {
                            copy(
                                movies = filteredMovies,
                                isLoading = false,
                                error = null,
                            )
                        }
                    }
            }
        }

        /**
         * Update selected category filter
         */
        fun selectCategory(category: BrowseCategory) {
            val allMovies = _allMovies.value
            val filteredMovies = filterMoviesByCategory(allMovies, category)

            _contentState.value = UiState.Success(filteredMovies)
            updateState {
                copy(
                    selectedCategory = category,
                    movies = filteredMovies,
                )
            }
        }

        /**
         * Search movies by query
         */
        fun searchMovies(query: String) {
            viewModelScope.launch {
                updateState { copy(searchQuery = query, isLoading = true) }

                if (query.isBlank()) {
                    // Reset to category filter
                    val allMovies = _allMovies.value
                    val filteredMovies = filterMoviesByCategory(allMovies, uiState.value.selectedCategory)
                    _contentState.value = UiState.Success(filteredMovies)
                    updateState {
                        copy(
                            movies = filteredMovies,
                            isLoading = false,
                        )
                    }
                } else {
                    movieRepository.searchMovies(query)
                        .catch { e ->
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
                        .collect { searchResults ->
                            // Apply category filter to search results
                            val filteredResults =
                                if (uiState.value.selectedCategory != BrowseCategory.ALL) {
                                    filterMoviesByCategory(searchResults, uiState.value.selectedCategory)
                                } else {
                                    searchResults
                                }

                            _contentState.value = UiState.Success(filteredResults)
                            updateState {
                                copy(
                                    movies = filteredResults,
                                    isLoading = false,
                                    error = null,
                                )
                            }
                        }
                }
            }
        }

        /**
         * Clear search query
         */
        fun clearSearch() {
            searchMovies("")
        }

        /**
         * Refresh movie data
         */
        fun refresh() {
            viewModelScope.launch {
                updateState { copy(isLoading = true, error = null) }

                movieRepository.refreshMovies()
                    .fold(
                        onSuccess = {
                            loadMovies()
                        },
                        onFailure = { e ->
                            _contentState.value =
                                UiState.Error(
                                    message = "Refresh failed: ${e.message}",
                                    throwable = e,
                                )
                            updateState {
                                copy(
                                    isLoading = false,
                                    error = "Refresh failed: ${e.message}",
                                )
                            }
                        },
                    )
            }
        }

        /**
         * Filter movies by category
         */
        private fun filterMoviesByCategory(
            movies: List<Movie>,
            category: BrowseCategory,
        ): List<Movie> {
            return when (category) {
                BrowseCategory.ALL -> movies
                BrowseCategory.ACTION ->
                    movies.filter {
                        it.studio?.contains("Action", ignoreCase = true) == true ||
                            it.title?.contains("Action", ignoreCase = true) == true
                    }
                BrowseCategory.DRAMA ->
                    movies.filter {
                        it.studio?.contains("Drama", ignoreCase = true) == true ||
                            it.title?.contains("Drama", ignoreCase = true) == true
                    }
                BrowseCategory.COMEDY ->
                    movies.filter {
                        it.studio?.contains("Comedy", ignoreCase = true) == true ||
                            it.title?.contains("Comedy", ignoreCase = true) == true
                    }
                BrowseCategory.THRILLER ->
                    movies.filter {
                        it.studio?.contains("Thriller", ignoreCase = true) == true ||
                            it.title?.contains("Thriller", ignoreCase = true) == true
                    }
                BrowseCategory.DOCUMENTARY ->
                    movies.filter {
                        it.studio?.contains("Documentary", ignoreCase = true) == true ||
                            it.title?.contains("Documentary", ignoreCase = true) == true
                    }
            }
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
                    error = "An error occurred: ${exception.message}",
                )
            }
        }
    }

/**
 * UI State for Browse Screen
 */
data class BrowseUiState(
    val movies: List<Movie> = emptyList(),
    val selectedCategory: BrowseCategory = BrowseCategory.ALL,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
) {
    val hasMovies: Boolean get() = movies.isNotEmpty()
    val hasSearch: Boolean get() = searchQuery.isNotBlank()
}
