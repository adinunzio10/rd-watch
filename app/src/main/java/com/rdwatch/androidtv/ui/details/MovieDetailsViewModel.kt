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
import com.rdwatch.androidtv.network.models.tmdb.TMDbCreditsResponse
import com.rdwatch.androidtv.ui.details.models.ExtendedContentMetadata
import com.rdwatch.androidtv.ui.details.models.CastMember
import com.rdwatch.androidtv.ui.details.models.CrewMember
import com.rdwatch.androidtv.ui.details.models.StreamingSource
import com.rdwatch.androidtv.ui.details.models.SourceType
import com.rdwatch.androidtv.ui.details.models.advanced.*
import com.rdwatch.androidtv.ui.details.models.SourceSortOption
import com.rdwatch.androidtv.ui.details.managers.ScraperSourceManager
import com.rdwatch.androidtv.ui.details.viewmodels.SourceListViewModel
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Date
import kotlinx.coroutines.withContext
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
    private val tmdbMovieRepository: TMDbMovieRepository,
    private val tmdbMovieService: com.rdwatch.androidtv.network.api.TMDbMovieService,
    private val scraperSourceManager: ScraperSourceManager,
    @ApplicationContext private val context: Context
) : BaseViewModel<MovieDetailsUiState>() {
    
    // Advanced source management
    private val advancedSourceManager = AdvancedSourceManager(context)
    private val sourceListViewModel = SourceListViewModel()
    
    private val _movieState = MutableStateFlow<UiState<Movie>>(UiState.Loading)
    val movieState: StateFlow<UiState<Movie>> = _movieState.asStateFlow()
    
    private val _relatedMoviesState = MutableStateFlow<UiState<List<Movie>>>(UiState.Loading)
    val relatedMoviesState: StateFlow<UiState<List<Movie>>> = _relatedMoviesState.asStateFlow()
    
    private val _creditsState = MutableStateFlow<UiState<ExtendedContentMetadata>>(UiState.Loading)
    val creditsState: StateFlow<UiState<ExtendedContentMetadata>> = _creditsState.asStateFlow()
    
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()
    
    private val _sourcesState = MutableStateFlow<UiState<List<StreamingSource>>>(UiState.Loading)
    val sourcesState: StateFlow<UiState<List<StreamingSource>>> = _sourcesState.asStateFlow()
    
    // Advanced source management state
    private val _advancedSources = MutableStateFlow<List<SourceMetadata>>(emptyList())
    val advancedSources: StateFlow<List<SourceMetadata>> = _advancedSources.asStateFlow()
    
    private val _sourceSelectionState = MutableStateFlow(SourceSelectionState())
    val sourceSelectionState: StateFlow<SourceSelectionState> = _sourceSelectionState.asStateFlow()
    
    private val _showSourceSelection = MutableStateFlow(false)
    val showSourceSelection: StateFlow<Boolean> = _showSourceSelection.asStateFlow()
    
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
                
                // Direct TMDb API call for testing
                println("DEBUG [MovieDetailsViewModel]: Making direct TMDb API call for movie $tmdbId")
                
                try {
                    val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                        tmdbMovieService.getMovieDetails(tmdbId, null, "en-US").execute()
                    }
                    
                    println("DEBUG [MovieDetailsViewModel]: Direct call response code: ${response.code()}")
                    println("DEBUG [MovieDetailsViewModel]: Direct call isSuccessful: ${response.isSuccessful}")
                    
                    if (response.isSuccessful && response.body() != null) {
                        val movieResponse = response.body()!!
                        
                        val movie = Movie(
                            id = tmdbId.toLong(),
                            title = movieResponse.title,
                            description = movieResponse.overview,
                            cardImageUrl = movieResponse.posterPath?.let { "https://image.tmdb.org/t/p/w342$it" },
                            backgroundImageUrl = movieResponse.backdropPath?.let { "https://image.tmdb.org/t/p/w780$it" },
                            videoUrl = null,
                            studio = "TMDb"
                        )
                        
                        _movieState.value = UiState.Success(movie)
                        updateState { 
                            copy(
                                movie = movie,
                                isLoading = false,
                                error = null,
                                isLoaded = true,
                                isFromRealDebrid = false,
                                tmdbResponse = movieResponse
                            )
                        }
                        println("DEBUG [MovieDetailsViewModel]: Movie loaded successfully: ${movie.title}")
                        
                        // Load related movies and credits
                        loadRelatedMovies(tmdbId)
                        loadMovieCredits(tmdbId)
                        
                        // Load scraper sources
                        loadSourcesForMovie(tmdbId.toString(), movieResponse.imdbId)
                        return@launch
                    } else {
                        println("DEBUG [MovieDetailsViewModel]: API call failed: ${response.code()} - ${response.message()}")
                    }
                } catch (e: Exception) {
                    println("DEBUG [MovieDetailsViewModel]: Exception during direct API call: ${e.message}")
                    e.printStackTrace()
                }
                
                // Fallback to repository approach if direct call fails
                println("DEBUG [MovieDetailsViewModel]: Falling back to repository approach")
                tmdbMovieRepository.getMovieDetails(tmdbId)
                    .take(1)
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
                                        isFromRealDebrid = false, // TMDb content is not from Real Debrid
                                        tmdbResponse = movieResponse
                                    )
                                }
                                
                                // Load related movies and credits
                                loadRelatedMovies(tmdbId)
                                loadMovieCredits(tmdbId)
                                
                                // Load scraper sources
                                loadSourcesForMovie(tmdbId.toString(), movieResponse.imdbId)
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
            
            // Load related movies and credits using TMDb ID
            val tmdbId = movie.id.toInt()
            loadRelatedMovies(tmdbId)
            loadMovieCredits(tmdbId)
            
            // Load scraper sources
            loadSourcesForMovie(tmdbId.toString(), null)
        }
    }
    
    /**
     * Load movie credits (cast and crew) from TMDb
     */
    private fun loadMovieCredits(tmdbId: Int) {
        viewModelScope.launch {
            _creditsState.value = UiState.Loading
            
            try {
                val response = withContext(kotlinx.coroutines.Dispatchers.IO) {
                    tmdbMovieService.getMovieCredits(tmdbId, "en-US").execute()
                }
                
                if (response.isSuccessful && response.body() != null) {
                    val creditsResponse = response.body()!!
                    
                    // Convert TMDb cast to CastMember objects
                    val castMembers = creditsResponse.cast.take(20).map { tmdbCast ->
                        CastMember(
                            id = tmdbCast.id,
                            name = tmdbCast.name,
                            character = tmdbCast.character,
                            profileImageUrl = CastMember.buildProfileImageUrl(tmdbCast.profilePath),
                            order = tmdbCast.order
                        )
                    }
                    
                    // Convert TMDb crew to CrewMember objects
                    val crewMembers = creditsResponse.crew.map { tmdbCrew ->
                        CrewMember(
                            id = tmdbCrew.id,
                            name = tmdbCrew.name,
                            job = tmdbCrew.job,
                            department = tmdbCrew.department,
                            profileImageUrl = CrewMember.buildProfileImageUrl(tmdbCrew.profilePath)
                        )
                    }
                    
                    // Create ExtendedContentMetadata with cast and crew
                    val extendedMetadata = ExtendedContentMetadata(
                        fullCast = castMembers,
                        crew = crewMembers
                    )
                    
                    _creditsState.value = UiState.Success(extendedMetadata)
                } else {
                    _creditsState.value = UiState.Error("Failed to load movie credits")
                }
            } catch (e: Exception) {
                _creditsState.value = UiState.Error(
                    message = "Failed to load movie credits: ${e.message}",
                    throwable = e
                )
            }
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
     * Load streaming sources for the movie from scrapers
     */
    fun loadSourcesForMovie(tmdbId: String, imdbId: String?) {
        println("DEBUG [MovieDetailsViewModel]: loadSourcesForMovie called with tmdbId: $tmdbId, imdbId: $imdbId")
        
        // Run scraper integration test first
        println("🧪 RUNNING SCRAPER INTEGRATION TEST:")
        try {
            val testResults = com.rdwatch.androidtv.scraper.api.ScraperTestRunner.runAllTests()
            println(testResults)
        } catch (e: Exception) {
            println("❌ Test runner failed: ${e.message}")
        }
        println("🧪 END SCRAPER INTEGRATION TEST")
        println()
        
        viewModelScope.launch {
            _sourcesState.value = UiState.Loading
            updateState { copy(sourcesLoading = true, sourcesError = null) }
            
            try {
                println("DEBUG [MovieDetailsViewModel]: Starting scraper query for movie $tmdbId")
                val sources = scraperSourceManager.getSourcesForContent(
                    contentId = tmdbId,
                    contentType = "movie",
                    imdbId = imdbId,
                    tmdbId = tmdbId
                )
                
                println("DEBUG [MovieDetailsViewModel]: Scrapers returned ${sources.size} sources")
                sources.forEach { source ->
                    println("DEBUG [MovieDetailsViewModel]: Source: ${source.provider.displayName} - ${source.quality.displayName} - ${source.url}")
                }
                
                _sourcesState.value = UiState.Success(sources)
                updateState { copy(availableSources = sources, sourcesLoading = false) }
                
                // Process sources with advanced manager
                loadAdvancedSourcesForMovie(sources, tmdbId)
                
                println("DEBUG [MovieDetailsViewModel]: Updated sourcesState with ${sources.size} sources")
                println("DEBUG [MovieDetailsViewModel]: Updated uiState.availableSources with ${sources.size} sources")
            } catch (e: Exception) {
                println("DEBUG [MovieDetailsViewModel]: Exception loading sources: ${e.message}")
                e.printStackTrace()
                
                val errorMessage = when {
                    e.message?.contains("timeout", ignoreCase = true) == true -> "Request timed out. Please try again."
                    e.message?.contains("network", ignoreCase = true) == true -> "Network error. Check your connection."
                    e.message?.contains("unauthorized", ignoreCase = true) == true -> "Scraper authentication failed."
                    else -> "Failed to load sources: ${e.message}"
                }
                
                _sourcesState.value = UiState.Error(
                    message = errorMessage,
                    throwable = e
                )
                updateState { 
                    copy(
                        sourcesLoading = false,
                        sourcesError = errorMessage
                    )
                }
            }
        }
    }
    
    /**
     * Retry loading sources
     */
    fun retryLoadingSources() {
        val currentMovie = uiState.value.movie ?: return
        val tmdbId = currentMovie.id.toString()
        val imdbId = uiState.value.tmdbResponse?.imdbId
        
        updateState { copy(sourcesError = null) }
        loadSourcesForMovie(tmdbId, imdbId)
    }
    
    /**
     * Refresh sources (clear cache and reload)
     */
    fun refreshSources() {
        viewModelScope.launch {
            try {
                scraperSourceManager.refreshProviders()
                retryLoadingSources()
            } catch (e: Exception) {
                updateState { copy(sourcesError = "Failed to refresh providers: ${e.message}") }
            }
        }
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
    
    // ===== ADVANCED SOURCE MANAGEMENT METHODS =====
    
    /**
     * Load advanced sources for a movie with enhanced processing
     */
    private fun loadAdvancedSourcesForMovie(streamingSources: List<StreamingSource>, movieId: String) {
        viewModelScope.launch {
            try {
                android.util.Log.d("MovieDetailsViewModel", "Loading advanced sources for movie: $movieId")
                
                // Convert to SourceMetadata for advanced processing
                val sourceMetadata = streamingSources.map { streamingSource ->
                    convertStreamingSourceToMetadata(streamingSource, movieId)
                }
                
                // Process sources with advanced manager
                val processedSources = sourceMetadata.map { source ->
                    advancedSourceManager.processSource(source)
                }
                
                // Update advanced sources
                _advancedSources.value = processedSources.map { it.sourceMetadata }
                
                android.util.Log.d("MovieDetailsViewModel", "Loaded ${processedSources.size} advanced sources for movie")
                
            } catch (e: Exception) {
                android.util.Log.e("MovieDetailsViewModel", "Failed to load advanced sources: ${e.message}")
            }
        }
    }
    
    /**
     * Trigger advanced source selection UI for movie
     */
    fun selectAdvancedSources() {
        val sources = _advancedSources.value
        println("DEBUG [MovieDetailsViewModel]: selectAdvancedSources() called with ${sources.size} sources")
        sources.forEach { source ->
            println("DEBUG [MovieDetailsViewModel]: Advanced Source: ${source.provider.displayName} - ${source.quality.resolution.shortName}")
        }
        
        // Update source selection state
        _sourceSelectionState.value = SourceSelectionState(
            sources = sources,
            filteredSources = sources
        )
        
        _showSourceSelection.value = true
        println("DEBUG [MovieDetailsViewModel]: Advanced source selection UI triggered")
    }
    
    /**
     * Hide source selection UI
     */
    fun hideSourceSelection() {
        _showSourceSelection.value = false
    }
    
    /**
     * Handle source selection from UI
     */
    fun onSourceSelected(source: SourceMetadata) {
        android.util.Log.d("MovieDetailsViewModel", "Source selected for movie: ${source.provider.name}")
        
        // Update selected source in state
        _sourceSelectionState.value = _sourceSelectionState.value.copy(
            selectedSource = source
        )
        
        // Hide source selection
        hideSourceSelection()
        
        // TODO: Trigger playback with selected source
        // This will be handled by the PlaybackViewModel or similar component
    }
    
    /**
     * Update source filter
     */
    fun updateSourceFilter(filter: SourceFilter) {
        println("DEBUG [MovieDetailsViewModel]: Updating source filter: $filter")
        val currentState = _sourceSelectionState.value
        val filteredSources = currentState.sources.filter { source ->
            // Apply the filter logic
            var matches = true
            
            // Quality filter
            filter.minQuality?.let { minQuality ->
                matches = matches && source.quality.resolution.baseScore >= minQuality.baseScore
            }
            
            // HDR filter
            if (filter.requireHDR) {
                matches = matches && source.quality.hasHDR()
            }
            
            // Cached filter
            if (filter.requireCached) {
                matches = matches && source.availability.cached
            }
            
            // Seeders filter
            filter.minSeeders?.let { minSeeders ->
                matches = matches && (source.health.seeders ?: 0) >= minSeeders
            }
            
            matches
        }
        
        println("DEBUG [MovieDetailsViewModel]: Filter applied: ${currentState.sources.size} → ${filteredSources.size} sources")
        
        _sourceSelectionState.value = currentState.copy(
            filter = filter,
            filteredSources = filteredSources
        )
    }
    
    /**
     * Update sort option
     */
    fun updateSortOption(sortOption: SourceSortOption) {
        val currentState = _sourceSelectionState.value
        
        // Apply sorting to filtered sources
        val sortedSources = when (sortOption) {
            SourceSortOption.QUALITY_SCORE, SourceSortOption.QUALITY -> currentState.filteredSources.sortedByDescending { 
                it.quality.resolution.baseScore 
            }
            SourceSortOption.FILE_SIZE -> currentState.filteredSources.sortedByDescending { 
                it.file.sizeInBytes ?: 0 
            }
            SourceSortOption.SEEDERS -> currentState.filteredSources.sortedByDescending { 
                it.health.seeders ?: 0 
            }
            SourceSortOption.RELIABILITY -> currentState.filteredSources.sortedByDescending { 
                it.provider.reliability.ordinal 
            }
            SourceSortOption.PROVIDER -> currentState.filteredSources.sortedBy { 
                it.provider.displayName 
            }
            SourceSortOption.PRIORITY -> currentState.filteredSources.sortedByDescending { source ->
                // Priority combines quality and reliability
                val qualityScore = source.quality.resolution.baseScore
                val reliabilityScore = source.provider.reliability.ordinal * 1000
                qualityScore + reliabilityScore
            }
            SourceSortOption.AVAILABILITY -> currentState.filteredSources.sortedByDescending { source ->
                if (source.availability.isAvailable) 1 else 0 
            }
            SourceSortOption.RELEASE_TYPE -> currentState.filteredSources.sortedBy { source ->
                source.release.type.ordinal 
            }
            else -> currentState.filteredSources
        }
        
        _sourceSelectionState.value = currentState.copy(
            sortOption = sortOption,
            filteredSources = sortedSources
        )
    }
    
    /**
     * Update view mode
     */
    fun updateViewMode(viewMode: SourceSelectionState.ViewMode) {
        println("DEBUG [MovieDetailsViewModel]: Updating view mode to: $viewMode")
        _sourceSelectionState.value = _sourceSelectionState.value.copy(
            viewMode = viewMode
        )
    }
    
    /**
     * Toggle group expansion
     */
    fun toggleGroup(groupId: String) {
        val currentState = _sourceSelectionState.value
        val expandedGroups = currentState.expandedGroups.toMutableSet()
        
        if (expandedGroups.contains(groupId)) {
            expandedGroups.remove(groupId)
        } else {
            expandedGroups.add(groupId)
        }
        
        _sourceSelectionState.value = currentState.copy(
            expandedGroups = expandedGroups
        )
    }
    
    /**
     * Convert StreamingSource to SourceMetadata for advanced processing
     */
    private fun convertStreamingSourceToMetadata(
        streamingSource: StreamingSource,
        movieId: String
    ): SourceMetadata {
        return SourceMetadata(
            id = streamingSource.id,
            provider = SourceProviderInfo(
                id = streamingSource.provider.name.lowercase().replace(" ", "-"),
                name = streamingSource.provider.name,
                displayName = streamingSource.provider.displayName,
                logoUrl = null,
                type = if (streamingSource.features.supportsP2P || streamingSource.sourceType.type == SourceType.ScraperSourceType.TORRENT) SourceProviderInfo.ProviderType.TORRENT else SourceProviderInfo.ProviderType.DIRECT_STREAM,
                reliability = SourceProviderInfo.ProviderReliability.GOOD
            ),
            quality = QualityInfo(
                resolution = mapStreamingQualityToVideoResolution(streamingSource.quality),
                bitrate = null,
                hdr10 = false, // Not available in StreamingSource features
                dolbyVision = streamingSource.features.supportsDolbyVision,
                hdr10Plus = false // Not available in StreamingSource features
            ),
            codec = CodecInfo(
                type = VideoCodec.H264, // Default, would need better detection
                profile = null,
                level = null
            ),
            audio = AudioInfo(
                format = AudioFormat.AAC, // Default, would need better detection
                channels = null,
                bitrate = null,
                language = null,
                dolbyAtmos = streamingSource.features.supportsDolbyAtmos,
                dtsX = false // Not available in StreamingSource features
            ),
            release = ReleaseInfo(
                type = ReleaseType.WEB_DL, // Default, would need better detection
                group = null,
                edition = null,
                year = null
            ),
            file = FileInfo(
                name = null,
                sizeInBytes = null,
                extension = "mkv", // Default
                hash = null
            ),
            health = HealthInfo(
                seeders = streamingSource.features.seeders,
                leechers = streamingSource.features.leechers,
                downloadSpeed = null,
                uploadSpeed = null,
                availability = null,
                lastChecked = null
            ),
            features = FeatureInfo(
                subtitles = emptyList(),
                has3D = false,
                hasChapters = false,
                hasMultipleAudioTracks = false,
                isDirectPlay = false,
                requiresTranscoding = false
            ),
            availability = AvailabilityInfo(
                isAvailable = true,
                region = null,
                expiryDate = null,
                debridService = null,
                cached = false // Not available in StreamingSource
            ),
            metadata = mapOf(
                "movieId" to movieId,
                "originalUrl" to (streamingSource.url ?: "")
            )
        )
    }
    
    /**
     * Map StreamingSource quality to VideoResolution
     */
    private fun mapStreamingQualityToVideoResolution(quality: com.rdwatch.androidtv.ui.details.models.SourceQuality): VideoResolution {
        return when (quality) {
            com.rdwatch.androidtv.ui.details.models.SourceQuality.QUALITY_8K -> VideoResolution.RESOLUTION_8K
            com.rdwatch.androidtv.ui.details.models.SourceQuality.QUALITY_4K, 
            com.rdwatch.androidtv.ui.details.models.SourceQuality.QUALITY_4K_HDR -> VideoResolution.RESOLUTION_4K
            com.rdwatch.androidtv.ui.details.models.SourceQuality.QUALITY_1080P,
            com.rdwatch.androidtv.ui.details.models.SourceQuality.QUALITY_1080P_HDR -> VideoResolution.RESOLUTION_1080P
            com.rdwatch.androidtv.ui.details.models.SourceQuality.QUALITY_720P,
            com.rdwatch.androidtv.ui.details.models.SourceQuality.QUALITY_720P_HDR -> VideoResolution.RESOLUTION_720P
            com.rdwatch.androidtv.ui.details.models.SourceQuality.QUALITY_480P -> VideoResolution.RESOLUTION_480P
            com.rdwatch.androidtv.ui.details.models.SourceQuality.QUALITY_360P -> VideoResolution.RESOLUTION_360P
            com.rdwatch.androidtv.ui.details.models.SourceQuality.QUALITY_240P -> VideoResolution.RESOLUTION_240P
            else -> VideoResolution.UNKNOWN
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
    val error: String? = null,
    val tmdbResponse: TMDbMovieResponse? = null,
    val availableSources: List<StreamingSource> = emptyList(),
    val sourcesLoading: Boolean = false,
    val sourcesError: String? = null
) {
    val hasMovie: Boolean get() = movie != null
    val canPlay: Boolean get() = movie?.videoUrl != null && !isDeleted
    val canDelete: Boolean get() = isFromRealDebrid && !isDeleted && !isDeleting
    val hasSourcesError: Boolean get() = sourcesError != null
    val isSourcesLoading: Boolean get() = sourcesLoading
    val hasAvailableSources: Boolean get() = availableSources.isNotEmpty()
    
    // Helper methods to extract metadata from TMDb response
    fun getMovieYear(): String {
        return tmdbResponse?.releaseDate?.let { releaseDate ->
            // Extract year from release date (format: YYYY-MM-DD)
            releaseDate.split("-").firstOrNull() ?: "Unknown"
        } ?: "Unknown"
    }
    
    fun getMovieRating(): String {
        return tmdbResponse?.voteAverage?.let { rating ->
            String.format("%.1f", rating)
        } ?: "N/A"
    }
    
    fun getMovieRuntime(): String {
        return tmdbResponse?.runtime?.let { runtime ->
            val hours = runtime / 60
            val minutes = runtime % 60
            if (hours > 0) {
                "${hours}h ${minutes}m"
            } else {
                "${minutes}m"
            }
        } ?: "Unknown"
    }
}