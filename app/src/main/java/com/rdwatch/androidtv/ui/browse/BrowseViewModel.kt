package com.rdwatch.androidtv.ui.browse

import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.MovieList
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for Browse Screen - handles content discovery and category filtering
 * Follows MVVM architecture with BaseViewModel pattern
 */
@HiltViewModel
class BrowseViewModel @Inject constructor() : BaseViewModel<BrowseUiState>() {
    
    private val _movies = MutableStateFlow<List<Movie>>(emptyList())
    val movies: StateFlow<List<Movie>> = _movies.asStateFlow()
    
    override fun createInitialState(): BrowseUiState {
        return BrowseUiState()
    }
    
    init {
        loadMovies()
    }
    
    /**
     * Load all movies from the movie list
     */
    private fun loadMovies() {
        launchSafely {
            updateState { copy(isLoading = true, error = null) }
            
            try {
                val allMovies = MovieList.list
                _movies.value = allMovies
                
                // Apply current filter
                val filteredMovies = filterMoviesByCategory(allMovies, uiState.value.selectedCategory)
                
                updateState { 
                    copy(
                        movies = filteredMovies,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                updateState { 
                    copy(
                        isLoading = false,
                        error = "Failed to load movies: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Update selected category filter
     */
    fun selectCategory(category: BrowseCategory) {
        val allMovies = _movies.value
        val filteredMovies = filterMoviesByCategory(allMovies, category)
        
        updateState { 
            copy(
                selectedCategory = category,
                movies = filteredMovies
            )
        }
    }
    
    /**
     * Search movies by query
     */
    fun searchMovies(query: String) {
        val allMovies = _movies.value
        val currentCategory = uiState.value.selectedCategory
        
        launchSafely {
            updateState { copy(searchQuery = query) }
            
            if (query.isBlank()) {
                // Reset to category filter
                val filteredMovies = filterMoviesByCategory(allMovies, currentCategory)
                updateState { copy(movies = filteredMovies) }
            } else {
                // Apply search filter on top of category filter
                val categoryFiltered = filterMoviesByCategory(allMovies, currentCategory)
                val searchFiltered = categoryFiltered.filter { movie ->
                    movie.title?.contains(query, ignoreCase = true) == true ||
                    movie.description?.contains(query, ignoreCase = true) == true ||
                    movie.studio?.contains(query, ignoreCase = true) == true
                }
                updateState { copy(movies = searchFiltered) }
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
        loadMovies()
    }
    
    /**
     * Filter movies by category
     */
    private fun filterMoviesByCategory(movies: List<Movie>, category: BrowseCategory): List<Movie> {
        return when (category) {
            BrowseCategory.ALL -> movies
            BrowseCategory.ACTION -> movies.filter { 
                it.studio?.contains("Action", ignoreCase = true) == true || 
                it.title?.contains("Action", ignoreCase = true) == true 
            }
            BrowseCategory.DRAMA -> movies.filter { 
                it.studio?.contains("Drama", ignoreCase = true) == true || 
                it.title?.contains("Drama", ignoreCase = true) == true 
            }
            BrowseCategory.COMEDY -> movies.filter { 
                it.studio?.contains("Comedy", ignoreCase = true) == true || 
                it.title?.contains("Comedy", ignoreCase = true) == true 
            }
            BrowseCategory.THRILLER -> movies.filter { 
                it.studio?.contains("Thriller", ignoreCase = true) == true || 
                it.title?.contains("Thriller", ignoreCase = true) == true 
            }
            BrowseCategory.DOCUMENTARY -> movies.filter { 
                it.studio?.contains("Documentary", ignoreCase = true) == true || 
                it.title?.contains("Documentary", ignoreCase = true) == true 
            }
        }
    }
    
    override fun handleError(exception: Throwable) {
        updateState { 
            copy(
                isLoading = false,
                error = "An error occurred: ${exception.message}"
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
    val error: String? = null
) {
    val hasMovies: Boolean get() = movies.isNotEmpty()
    val hasSearch: Boolean get() = searchQuery.isNotBlank()
}