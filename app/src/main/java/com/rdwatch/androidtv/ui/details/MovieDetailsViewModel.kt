package com.rdwatch.androidtv.ui.details

import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.MovieList
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

/**
 * ViewModel for Movie Details Screen - handles movie details loading and related content
 * Follows MVVM architecture with BaseViewModel pattern
 */
@HiltViewModel
class MovieDetailsViewModel @Inject constructor() : BaseViewModel<MovieDetailsUiState>() {
    
    private val _relatedMovies = MutableStateFlow<List<Movie>>(emptyList())
    val relatedMovies: StateFlow<List<Movie>> = _relatedMovies.asStateFlow()
    
    override fun createInitialState(): MovieDetailsUiState {
        return MovieDetailsUiState()
    }
    
    /**
     * Load movie details and related content
     */
    fun loadMovieDetails(movieId: String) {
        launchSafely {
            updateState { it.copy(isLoading = true, error = null) }
            
            try {
                // Find the movie by ID (converted to Long)
                val movie = MovieList.list.find { it.id.toString() == movieId }
                
                if (movie == null) {
                    updateState { 
                        it.copy(
                            isLoading = false,
                            error = "Movie not found"
                        )
                    }
                    return@launchSafely
                }
                
                // Load related movies (excluding current movie)
                val related = MovieList.list.filter { it.id != movie.id }.take(6)
                _relatedMovies.value = related
                
                updateState { 
                    it.copy(
                        movie = movie,
                        isLoading = false,
                        error = null,
                        isLoaded = true
                    )
                }
                
            } catch (e: Exception) {
                updateState { 
                    it.copy(
                        isLoading = false,
                        error = "Failed to load movie details: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Load movie by direct Movie object
     */
    fun loadMovie(movie: Movie) {
        launchSafely {
            updateState { 
                it.copy(
                    movie = movie,
                    isLoading = false,
                    error = null,
                    isLoaded = true
                )
            }
            
            // Load related movies
            val related = MovieList.list.filter { it.id != movie.id }.take(6)
            _relatedMovies.value = related
        }
    }
    
    /**
     * Add movie to watchlist
     */
    fun addToWatchlist() {
        val currentMovie = uiState.value.movie ?: return
        
        launchSafely {
            // TODO: In real implementation, save to database or remote service
            updateState { it.copy(isInWatchlist = true) }
        }
    }
    
    /**
     * Remove movie from watchlist
     */
    fun removeFromWatchlist() {
        launchSafely {
            // TODO: In real implementation, remove from database or remote service
            updateState { it.copy(isInWatchlist = false) }
        }
    }
    
    /**
     * Toggle like status
     */
    fun toggleLike() {
        launchSafely {
            val currentState = uiState.value.isLiked
            updateState { it.copy(isLiked = !currentState) }
            // TODO: In real implementation, save to database or remote service
        }
    }
    
    /**
     * Share movie
     */
    fun shareMovie() {
        val currentMovie = uiState.value.movie ?: return
        
        launchSafely {
            // TODO: In real implementation, create share intent or generate share link
            // For now, just show that action was triggered
        }
    }
    
    /**
     * Download movie for offline viewing
     */
    fun downloadMovie() {
        val currentMovie = uiState.value.movie ?: return
        
        launchSafely {
            updateState { it.copy(isDownloading = true) }
            
            // TODO: In real implementation, start download service
            // Simulate download process
            kotlinx.coroutines.delay(2000)
            
            updateState { 
                it.copy(
                    isDownloading = false,
                    isDownloaded = true
                )
            }
        }
    }
    
    /**
     * Get formatted duration string
     */
    fun getFormattedDuration(): String {
        // TODO: In real implementation, get from movie metadata
        return "2h 15m"
    }
    
    /**
     * Get movie rating
     */
    fun getMovieRating(): String {
        // TODO: In real implementation, get from movie metadata
        return "PG-13"
    }
    
    /**
     * Get movie language
     */
    fun getMovieLanguage(): String {
        // TODO: In real implementation, get from movie metadata
        return "English"
    }
    
    /**
     * Get movie year
     */
    fun getMovieYear(): String {
        // TODO: In real implementation, get from movie metadata
        return "2023"
    }
    
    override fun handleError(exception: Throwable) {
        updateState { 
            it.copy(
                isLoading = false,
                isDownloading = false,
                error = "An error occurred: ${exception.message}"
            )
        }
    }
}

/**
 * UI State for Movie Details Screen
 */
data class MovieDetailsUiState(
    val movie: Movie? = null,
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val isInWatchlist: Boolean = false,
    val isLiked: Boolean = false,
    val isDownloading: Boolean = false,
    val isDownloaded: Boolean = false,
    val error: String? = null
) {
    val hasMovie: Boolean get() = movie != null
    val canPlay: Boolean get() = movie?.videoUrl != null
}