package com.rdwatch.androidtv.ui.details

import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import com.rdwatch.androidtv.data.repository.TMDbMovieRepository
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.details.models.MovieContentDetail
import com.rdwatch.androidtv.ui.details.models.ContentProgress
import com.rdwatch.androidtv.network.models.tmdb.TMDbRecommendationsResponse
import com.rdwatch.androidtv.network.models.tmdb.TMDbMovieResponse
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
    private val tmdbMovieRepository: TMDbMovieRepository
) : BaseViewModel<MovieDetailsUiState>() {
    
    private val _movieState = MutableStateFlow<UiState<Movie>>(UiState.Loading)
    val movieState: StateFlow<UiState<Movie>> = _movieState.asStateFlow()
    
    private val _relatedMoviesState = MutableStateFlow<UiState<List<Movie>>>(UiState.Loading)
    val relatedMoviesState: StateFlow<UiState<List<Movie>>> = _relatedMoviesState.asStateFlow()
    
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()
    
    override fun createInitialState(): MovieDetailsUiState {
        return MovieDetailsUiState()
    }
    
    /**
     * Load movie details and related content from TMDb
     */
    fun loadMovieDetails(movieId: String) {
        println("DEBUG [MovieDetailsViewModel]: loadMovieDetails called with movieId: $movieId")
        viewModelScope.launch {
            _movieState.value = UiState.Loading
            updateState { copy(isLoading = true, error = null) }
            
            try {
                // Convert movieId to Int for TMDb API
                val tmdbId = movieId.toIntOrNull() ?: run {
                    println("DEBUG [MovieDetailsViewModel]: Invalid movie ID: $movieId")
                    _movieState.value = UiState.Error("Invalid TMDb movie ID")
                    updateState { 
                        copy(
                            isLoading = false,
                            error = "Invalid TMDb movie ID"
                        )
                    }
                    return@launch
                }
                
                println("DEBUG [MovieDetailsViewModel]: Converted movieId $movieId to tmdbId $tmdbId")
                
                // Fetch movie details from TMDb using simpler method
                println("DEBUG [MovieDetailsViewModel]: Calling tmdbMovieRepository.getMovieDetails($tmdbId)")
                tmdbMovieRepository.getMovieDetails(tmdbId)
                    .take(1) // Just take the first emission
                    .collect { result ->
                        println("DEBUG [MovieDetailsViewModel]: Received result: $result")
                        when (result) {
                            is Result.Success -> {
                                println("DEBUG [MovieDetailsViewModel]: Success result received")
                                val movieResponse = result.data
                                
                                if (movieResponse == null) {
                                    println("DEBUG [MovieDetailsViewModel]: MovieResponse is null")
                                    _movieState.value = UiState.Error("Movie not found on TMDb")
                                    updateState { 
                                        copy(
                                            isLoading = false,
                                            error = "Movie not found on TMDb"
                                        )
                                    }
                                    return@collect
                                }
                                
                                println("DEBUG [MovieDetailsViewModel]: Converting MovieResponse to Movie: ${movieResponse.title}")
                                // Convert TMDbMovieResponse to Movie for UI compatibility
                                val movie = Movie(
                                    id = tmdbId.toLong(),
                                    title = movieResponse.title,
                                    description = movieResponse.overview,
                                    cardImageUrl = movieResponse.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
                                    backgroundImageUrl = movieResponse.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
                                    videoUrl = null, // TMDb doesn't provide video URLs
                                    studio = "TMDb"
                                )
                                
                                _movieState.value = UiState.Success(movie)
                                updateState { 
                                    copy(
                                        movie = movie,
                                        isLoading = false,
                                        error = null,
                                        isLoaded = true,
                                        isFromRealDebrid = false // TMDb content is not from Real Debrid
                                    )
                                }
                                
                                // Load related movies
                                loadRelatedMovies(tmdbId)
                            }
                            is Result.Error -> {
                                println("DEBUG [MovieDetailsViewModel]: Error result received: ${result.exception.message}")
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
                                println("DEBUG [MovieDetailsViewModel]: Loading result received")
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
            
            // Load related movies using TMDb ID
            val tmdbId = movie.id.toInt()
            loadRelatedMovies(tmdbId)
        }
    }
    
    /**
     * Load related movies from TMDb recommendations
     */
    private fun loadRelatedMovies(tmdbId: Int) {
        viewModelScope.launch {
            _relatedMoviesState.value = UiState.Loading
            
            tmdbMovieRepository.getMovieRecommendations(tmdbId)
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
                            val recommendationsResponse = result.data
                            
                            // Convert TMDb recommendations to Movie objects
                            val relatedMovies = recommendationsResponse.results.take(6).map { tmdbMovie ->
                                Movie(
                                    id = tmdbMovie.id.toLong(),
                                    title = tmdbMovie.title ?: "Unknown Title",
                                    description = tmdbMovie.overview,
                                    cardImageUrl = tmdbMovie.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
                                    backgroundImageUrl = tmdbMovie.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
                                    videoUrl = null,
                                    studio = "TMDb"
                                )
                            }
                            
                            _relatedMoviesState.value = UiState.Success(relatedMovies)
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
            try {
                // Enhanced watchlist functionality - placeholder implementation
                // In real implementation, this would save to local database or cloud service
                updateState { copy(isInWatchlist = true) }
                
                // Future: Save to local database
                // watchlistRepository.addToWatchlist(currentMovie.id)
            } catch (e: Exception) {
                updateState { copy(error = "Failed to add to watchlist: ${e.message}") }
            }
        }
    }
    
    /**
     * Remove movie from watchlist
     */
    fun removeFromWatchlist() {
        viewModelScope.launch {
            try {
                // Enhanced watchlist functionality - placeholder implementation
                updateState { copy(isInWatchlist = false) }
                
                // Future: Remove from local database
                // watchlistRepository.removeFromWatchlist(currentMovie.id)
            } catch (e: Exception) {
                updateState { copy(error = "Failed to remove from watchlist: ${e.message}") }
            }
        }
    }
    
    /**
     * Toggle like status
     */
    fun toggleLike() {
        viewModelScope.launch {
            try {
                val currentState = uiState.value.isLiked
                val newState = !currentState
                updateState { copy(isLiked = newState) }
                
                // Enhanced favorites functionality - placeholder implementation
                // Future: Save to favorites repository
                // if (newState) {
                //     favoritesRepository.addToFavorites(currentMovie.id)
                // } else {
                //     favoritesRepository.removeFromFavorites(currentMovie.id)
                // }
            } catch (e: Exception) {
                updateState { copy(error = "Failed to update favorite status: ${e.message}") }
            }
        }
    }
    
    /**
     * Share movie
     */
    fun shareMovie() {
        val currentMovie = uiState.value.movie ?: return
        
        viewModelScope.launch {
            // Enhanced share functionality - create share intent data
            val shareText = buildString {
                append("Check out this movie: ${currentMovie.title}")
                currentMovie.description?.let { desc ->
                    append("\n\n$desc")
                }
                currentMovie.videoUrl?.let { url ->
                    append("\n\nWatch: $url")
                }
            }
            
            // Share action implementation would trigger system share dialog
            // For now, just log that share was triggered
        }
    }
    
    /**
     * Download movie for offline viewing (placeholder for TMDb content)
     */
    fun downloadMovie() {
        val currentMovie = uiState.value.movie ?: return
        
        viewModelScope.launch {
            updateState { copy(isDownloading = true) }
            
            // For TMDb content, downloading would require additional scraper integration
            // This is a placeholder implementation
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
     * Delete content (not applicable for TMDb content)
     */
    fun deleteFromRealDebrid() {
        // TMDb content cannot be deleted as it's not stored locally
        // This method is kept for UI compatibility but does nothing
        updateState { copy(error = "Cannot delete TMDb content") }
    }
    
    /**
     * Get formatted duration string (placeholder - would come from TMDb metadata)
     */
    fun getFormattedDuration(): String {
        // In a complete implementation, this would extract runtime from TMDb movie details
        return "2h 15m"
    }
    
    /**
     * Get movie rating (placeholder - would come from TMDb metadata)
     */
    fun getMovieRating(): String {
        // In a complete implementation, this would extract rating from TMDb movie details
        return "PG-13"
    }
    
    /**
     * Get movie language (placeholder - would come from TMDb metadata)
     */
    fun getMovieLanguage(): String {
        // In a complete implementation, this would extract language from TMDb movie details
        return "English"
    }
    
    /**
     * Get movie year (placeholder - would come from TMDb metadata)
     */
    fun getMovieYear(): String {
        // In a complete implementation, this would extract year from TMDb movie details
        return "2023"
    }
    
    /**
     * Select a tab
     */
    fun selectTab(tabIndex: Int) {
        _selectedTabIndex.value = tabIndex
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