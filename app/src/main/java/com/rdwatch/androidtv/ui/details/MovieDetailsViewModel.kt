package com.rdwatch.androidtv.ui.details

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
 * ViewModel for Movie Details Screen - handles movie details loading and related content
 * Follows MVVM architecture with BaseViewModel pattern
 */
@HiltViewModel
class MovieDetailsViewModel @Inject constructor(
    private val realDebridContentRepository: RealDebridContentRepository
) : BaseViewModel<MovieDetailsUiState>() {
    
    private val _movieState = MutableStateFlow<UiState<Movie>>(UiState.Loading)
    val movieState: StateFlow<UiState<Movie>> = _movieState.asStateFlow()
    
    private val _relatedMoviesState = MutableStateFlow<UiState<List<Movie>>>(UiState.Loading)
    val relatedMoviesState: StateFlow<UiState<List<Movie>>> = _relatedMoviesState.asStateFlow()
    
    override fun createInitialState(): MovieDetailsUiState {
        return MovieDetailsUiState()
    }
    
    /**
     * Load movie details and related content
     */
    fun loadMovieDetails(movieId: String) {
        viewModelScope.launch {
            _movieState.value = UiState.Loading
            updateState { copy(isLoading = true, error = null) }
            
            try {
                // Convert movieId to Long
                val id = movieId.toLongOrNull() ?: run {
                    _movieState.value = UiState.Error("Invalid movie ID")
                    updateState { 
                        copy(
                            isLoading = false,
                            error = "Invalid movie ID"
                        )
                    }
                    return@launch
                }
                
                // Fetch all content and find the movie
                realDebridContentRepository.getAllContent()
                    .take(1) // Just take the first emission
                    .collect { result ->
                        when (result) {
                            is Result.Success -> {
                                val movie = result.data.findMovieById(id)
                                
                                if (movie == null) {
                                    _movieState.value = UiState.Error("Movie not found")
                                    updateState { 
                                        copy(
                                            isLoading = false,
                                            error = "Movie not found"
                                        )
                                    }
                                    return@collect
                                }
                                
                                _movieState.value = UiState.Success(movie)
                                updateState { 
                                    copy(
                                        movie = movie,
                                        isLoading = false,
                                        error = null,
                                        isLoaded = true,
                                        isFromRealDebrid = movie.studio?.contains("RealDebrid", ignoreCase = true) == true
                                    )
                                }
                                
                                // Load related movies
                                loadRelatedMovies(movie)
                            }
                            is Result.Error -> {
                                _movieState.value = UiState.Error(
                                    message = "Failed to load movie details: ${result.exception.message}",
                                    throwable = result.exception
                                )
                                updateState { 
                                    copy(
                                        isLoading = false,
                                        error = "Failed to load movie details: ${result.exception.message}"
                                    )
                                }
                            }
                            is Result.Loading -> {
                                // Keep loading state
                            }
                        }
                    }
                
            } catch (e: Exception) {
                _movieState.value = UiState.Error(
                    message = "Failed to load movie details: ${e.message}",
                    throwable = e
                )
                updateState { 
                    copy(
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
        viewModelScope.launch {
            _movieState.value = UiState.Success(movie)
            updateState { 
                copy(
                    movie = movie,
                    isLoading = false,
                    error = null,
                    isLoaded = true,
                    isFromRealDebrid = movie.studio?.contains("RealDebrid", ignoreCase = true) == true
                )
            }
            
            // Load related movies
            loadRelatedMovies(movie)
        }
    }
    
    /**
     * Load related movies
     */
    private fun loadRelatedMovies(currentMovie: Movie) {
        viewModelScope.launch {
            _relatedMoviesState.value = UiState.Loading
            
            realDebridContentRepository.getAllContent()
                .catch { e ->
                    _relatedMoviesState.value = UiState.Error(
                        message = "Failed to load related movies",
                        throwable = e
                    )
                }
                .take(1) // Just take the first emission
                .collect { result ->
                    when (result) {
                        is Result.Success -> {
                            val allMovies = result.data.toMovies()
                            
                            // Get related movies (same studio, excluding current movie)
                            val related = allMovies
                                .filter { it.id != currentMovie.id }
                                .filter { it.studio == currentMovie.studio }
                                .take(6)
                                .ifEmpty {
                                    // If no movies from same studio, just get random movies
                                    allMovies.filter { it.id != currentMovie.id }.take(6)
                                }
                            
                            _relatedMoviesState.value = UiState.Success(related)
                        }
                        is Result.Error -> {
                            _relatedMoviesState.value = UiState.Error(
                                message = "Failed to load related movies",
                                throwable = result.exception
                            )
                        }
                        is Result.Loading -> {
                            // Keep loading state
                        }
                    }
                }
        }
    }
    
    /**
     * Add movie to watchlist
     */
    fun addToWatchlist() {
        val currentMovie = uiState.value.movie ?: return
        
        viewModelScope.launch {
            // TODO: Implement watchlist repository when available
            updateState { copy(isInWatchlist = true) }
        }
    }
    
    /**
     * Remove movie from watchlist
     */
    fun removeFromWatchlist() {
        viewModelScope.launch {
            // TODO: Implement watchlist repository when available
            updateState { copy(isInWatchlist = false) }
        }
    }
    
    /**
     * Toggle like status
     */
    fun toggleLike() {
        viewModelScope.launch {
            val currentState = uiState.value.isLiked
            updateState { copy(isLiked = !currentState) }
            // TODO: Implement favorites repository when available
        }
    }
    
    /**
     * Share movie
     */
    fun shareMovie() {
        val currentMovie = uiState.value.movie ?: return
        
        viewModelScope.launch {
            // TODO: Implement share functionality
            // For now, just show that action was triggered
        }
    }
    
    /**
     * Download movie for offline viewing
     */
    fun downloadMovie() {
        val currentMovie = uiState.value.movie ?: return
        
        viewModelScope.launch {
            updateState { copy(isDownloading = true) }
            
            // TODO: Implement download service when available
            // For RD content, this might involve different logic
            kotlinx.coroutines.delay(2000)
            
            updateState { 
                copy(
                    isDownloading = false,
                    isDownloaded = true
                )
            }
        }
    }
    
    /**
     * Delete from Real-Debrid (if applicable)
     */
    fun deleteFromRealDebrid() {
        if (!uiState.value.isFromRealDebrid) return
        
        viewModelScope.launch {
            // TODO: Implement RD deletion when RD repository is available
            updateState { copy(isDeleting = true) }
            
            try {
                // Placeholder for RD deletion
                kotlinx.coroutines.delay(1000)
                
                updateState { 
                    copy(
                        isDeleting = false,
                        isDeleted = true
                    )
                }
            } catch (e: Exception) {
                updateState { 
                    copy(
                        isDeleting = false,
                        error = "Failed to delete from Real-Debrid: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Get formatted duration string
     */
    fun getFormattedDuration(): String {
        // TODO: Get from movie metadata when available
        return "2h 15m"
    }
    
    /**
     * Get movie rating
     */
    fun getMovieRating(): String {
        // TODO: Get from movie metadata when available
        return "PG-13"
    }
    
    /**
     * Get movie language
     */
    fun getMovieLanguage(): String {
        // TODO: Get from movie metadata when available
        return "English"
    }
    
    /**
     * Get movie year
     */
    fun getMovieYear(): String {
        // TODO: Get from movie metadata when available
        return "2023"
    }
    
    /**
     * Refresh movie details
     */
    fun refresh() {
        val movieId = uiState.value.movie?.id?.toString() ?: return
        loadMovieDetails(movieId)
    }
    
    override fun handleError(exception: Throwable) {
        _movieState.value = UiState.Error(
            message = "An error occurred: ${exception.message}",
            throwable = exception
        )
        updateState { 
            copy(
                isLoading = false,
                isDownloading = false,
                isDeleting = false,
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
    val isFromRealDebrid: Boolean = false,
    val isDeleting: Boolean = false,
    val isDeleted: Boolean = false,
    val error: String? = null
) {
    val hasMovie: Boolean get() = movie != null
    val canPlay: Boolean get() = movie?.videoUrl != null && !isDeleted
    val canDelete: Boolean get() = isFromRealDebrid && !isDeleted && !isDeleting
}