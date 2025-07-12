package com.rdwatch.androidtv.ui.details

import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.details.models.*
import com.rdwatch.androidtv.ui.details.managers.ScraperSourceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

/**
 * ViewModel for TV Details Screen - handles TV show details loading and episode management
 * Follows MVVM architecture with BaseViewModel pattern
 */
@HiltViewModel
class TVDetailsViewModel @Inject constructor(
    private val realDebridContentRepository: RealDebridContentRepository,
    private val tmdbTVRepository: com.rdwatch.androidtv.data.repository.TMDbTVRepository,
    private val scraperSourceManager: ScraperSourceManager
) : BaseViewModel<TVDetailsUiState>() {
    
    // Job management for canceling concurrent API requests
    private var seasonLoadingJob: Job? = null
    private val onDemandSeasonJobs = mutableMapOf<Int, Job>()
    private val activeSeasonRequests = mutableSetOf<Int>()
    
    private val _tvShowState = MutableStateFlow<TVShowContentDetail?>(null)
    val tvShowState: StateFlow<TVShowContentDetail?> = _tvShowState.asStateFlow()
    
    private val _selectedSeason = MutableStateFlow<TVSeason?>(null)
    val selectedSeason: StateFlow<TVSeason?> = _selectedSeason.asStateFlow()
    
    private val _selectedEpisode = MutableStateFlow<TVEpisode?>(null)
    val selectedEpisode: StateFlow<TVEpisode?> = _selectedEpisode.asStateFlow()
    
    private val _relatedShowsState = MutableStateFlow<UiState<List<TVShowContentDetail>>>(UiState.Loading)
    val relatedShowsState: StateFlow<UiState<List<TVShowContentDetail>>> = _relatedShowsState.asStateFlow()
    
    private val _selectedTabIndex = MutableStateFlow(0)
    val selectedTabIndex: StateFlow<Int> = _selectedTabIndex.asStateFlow()
    
    private val _creditsState = MutableStateFlow<UiState<ExtendedContentMetadata>>(UiState.Loading)
    val creditsState: StateFlow<UiState<ExtendedContentMetadata>> = _creditsState.asStateFlow()
    
    private val _sourcesState = MutableStateFlow<UiState<List<StreamingSource>>>(UiState.Idle)
    val sourcesState: StateFlow<UiState<List<StreamingSource>>> = _sourcesState.asStateFlow()
    
    override fun createInitialState(): TVDetailsUiState {
        return TVDetailsUiState()
    }
    
    /**
     * Load TV show details and related content from TMDb
     */
    fun loadTVShow(tvShowId: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            
            try {
                // Debug logging for ID tracking
                android.util.Log.d("TVDetailsViewModel", "=== TV Show ID Debug ===")
                android.util.Log.d("TVDetailsViewModel", "Raw tvShowId received: '$tvShowId'")
                android.util.Log.d("TVDetailsViewModel", "tvShowId length: ${tvShowId.length}")
                android.util.Log.d("TVDetailsViewModel", "tvShowId bytes: ${tvShowId.toByteArray().contentToString()}")
                
                // Sanitize input - trim whitespace and validate format
                val sanitizedId = tvShowId.trim()
                android.util.Log.d("TVDetailsViewModel", "Sanitized tvShowId: '$sanitizedId'")
                
                // Convert tvShowId to Int for TMDb API
                val tmdbId = sanitizedId.toIntOrNull()
                android.util.Log.d("TVDetailsViewModel", "toIntOrNull() result: $tmdbId")
                
                if (tmdbId == null) {
                    val errorMessage = "Invalid TMDb TV show ID: '$sanitizedId' (original: '$tvShowId')"
                    android.util.Log.e("TVDetailsViewModel", errorMessage)
                    updateState { 
                        copy(
                            isLoading = false,
                            error = errorMessage
                        )
                    }
                    return@launch
                }
                
                android.util.Log.d("TVDetailsViewModel", "Successfully converted to TMDb ID: $tmdbId")
                
                // Load TV show details from TMDb
                tmdbTVRepository.getTVContentDetail(tmdbId).collect { result ->
                    when (result) {
                        is com.rdwatch.androidtv.repository.base.Result.Loading -> {
                            updateState { copy(isLoading = true, error = null) }
                        }
                        is com.rdwatch.androidtv.repository.base.Result.Success -> {
                            val contentDetail = result.data
                            
                            // Create TVShowDetail from TMDb data
                            val tmdbTvDetail = when (contentDetail) {
                                is com.rdwatch.androidtv.ui.details.models.TMDbTVContentDetail -> {
                                    // Use TMDb data from models package
                                    TVShowDetail(
                                        id = contentDetail.id,
                                        title = contentDetail.title,
                                        originalTitle = contentDetail.originalTitle,
                                        overview = contentDetail.description,
                                        posterPath = contentDetail.cardImageUrl,
                                        backdropPath = contentDetail.backgroundImageUrl,
                                        firstAirDate = contentDetail.firstAirDate,
                                        lastAirDate = contentDetail.lastAirDate,
                                        status = contentDetail.status ?: "Unknown",
                                        type = contentDetail.type ?: "Scripted",
                                        genres = contentDetail.genres,
                                        languages = contentDetail.spokenLanguages,
                                        originCountry = contentDetail.originCountry,
                                        numberOfSeasons = contentDetail.numberOfSeasons ?: 1,
                                        numberOfEpisodes = contentDetail.numberOfEpisodes ?: 0,
                                        seasons = emptyList(), // Will be populated after loading season details
                                        networks = contentDetail.networks,
                                        productionCompanies = contentDetail.productionCompanies,
                                        creators = emptyList(), // TODO: Load from TMDb credits
                                        cast = emptyList(), // TODO: Load from TMDb credits
                                        voteAverage = contentDetail.voteAverage,
                                        voteCount = contentDetail.voteCount,
                                        popularity = contentDetail.popularity,
                                        adult = contentDetail.adult,
                                        homepage = contentDetail.homepage,
                                        tagline = null, // Not available in this model
                                        inProduction = contentDetail.inProduction ?: false,
                                        episodeRunTime = emptyList(), // TODO: Parse from TMDb data
                                        lastEpisodeToAir = null, // TODO: Load from TMDb
                                        nextEpisodeToAir = null // TODO: Load from TMDb
                                    )
                                }
                                is com.rdwatch.androidtv.data.mappers.TMDbTVContentDetail -> {
                                    // Use real TMDb data from mappers package (with seasons)
                                    TVShowDetail(
                                        id = contentDetail.id,
                                        title = contentDetail.title,
                                        originalTitle = contentDetail.getOriginalName(),
                                        overview = contentDetail.description,
                                        posterPath = contentDetail.cardImageUrl,
                                        backdropPath = contentDetail.backgroundImageUrl,
                                        firstAirDate = contentDetail.getFormattedFirstAirDate(),
                                        lastAirDate = contentDetail.getFormattedLastAirDate(),
                                        status = contentDetail.getStatus(),
                                        type = contentDetail.getType(),
                                        genres = contentDetail.getGenres().map { it.name },
                                        languages = contentDetail.getSpokenLanguages().map { it.name },
                                        originCountry = contentDetail.getOriginCountries(),
                                        numberOfSeasons = contentDetail.getNumberOfSeasons(),
                                        numberOfEpisodes = contentDetail.getNumberOfEpisodes(),
                                        seasons = mapTMDbSeasonsToTVSeasons(contentDetail.getSeasons()),
                                        networks = contentDetail.getNetworks().map { it.name },
                                        productionCompanies = contentDetail.getProductionCompanies().map { it.name },
                                        creators = contentDetail.getCreatedBy().map { it.name },
                                        cast = emptyList(), // TODO: Load from TMDb credits
                                        voteAverage = contentDetail.getFormattedVoteAverage().toFloat(),
                                        voteCount = contentDetail.getVoteCount(),
                                        popularity = contentDetail.getPopularity().toFloat(),
                                        adult = contentDetail.isAdultContent(),
                                        homepage = contentDetail.getHomepage(),
                                        tagline = contentDetail.getTagline(),
                                        inProduction = contentDetail.isInProduction(),
                                        episodeRunTime = contentDetail.getEpisodeRunTime(),
                                        lastEpisodeToAir = contentDetail.getLastEpisodeToAir()?.let { mapTMDbEpisodeToTVEpisode(it) },
                                        nextEpisodeToAir = contentDetail.getNextEpisodeToAir()?.let { mapTMDbEpisodeToTVEpisode(it) }
                                    )
                                }
                                else -> {
                                    // Fallback for non-TMDb content
                                    TVShowDetail(
                                        id = contentDetail.id,
                                        title = contentDetail.title,
                                        originalTitle = contentDetail.title,
                                        overview = contentDetail.description,
                                        posterPath = contentDetail.cardImageUrl,
                                        backdropPath = contentDetail.backgroundImageUrl,
                                        firstAirDate = contentDetail.metadata.year?.let { "$it-01-01" },
                                        lastAirDate = null,
                                        status = "Unknown",
                                        type = "Scripted",
                                        genres = contentDetail.metadata.genre,
                                        numberOfSeasons = 1,
                                        numberOfEpisodes = 0,
                                        seasons = emptyList(),
                                        networks = listOf(contentDetail.metadata.studio ?: "Unknown"),
                                        voteAverage = 0f,
                                        voteCount = 0
                                    )
                                }
                            }
                            
                            val tvShowDetail = TVShowContentDetail.from(tmdbTvDetail)
                            
                            _tvShowState.value = tvShowDetail
                            
                            // Select first season by default
                            val firstSeason = tvShowDetail.getSeasons().firstOrNull()
                            _selectedSeason.value = firstSeason
                            
                            // Select first episode of first season by default
                            val firstEpisode = firstSeason?.episodes?.firstOrNull()
                            selectEpisodeInternal(firstEpisode)
                            
                            updateState { 
                                copy(
                                    tvShow = tvShowDetail,
                                    isLoading = false,
                                    error = null,
                                    isLoaded = true,
                                    currentSeason = firstSeason
                                )
                            }
                            
                            // Load related shows
                            loadRelatedShows(tvShowDetail)
                            
                            // Load credits
                            loadTVCredits(tmdbId)
                            
                            // Load detailed season data with episodes
                            loadSeasonsWithEpisodes(tmdbId, tvShowDetail)
                        }
                        is com.rdwatch.androidtv.repository.base.Result.Error -> {
                            updateState { 
                                copy(
                                    isLoading = false,
                                    error = "Failed to load TV show: ${result.exception.message}"
                                )
                            }
                        }
                    }
                }
                
            } catch (e: Exception) {
                updateState { 
                    copy(
                        isLoading = false,
                        error = "Failed to load TV show: ${e.message}"
                    )
                }
            }
        }
    }
    
    /**
     * Select a season and update the selected episode
     * Uses single source of truth pattern - always gets fresh data from tvShowState
     */
    fun selectSeason(season: TVSeason) {
        android.util.Log.d("TVDetailsViewModel", "=== Season Selection Debug ===")
        android.util.Log.d("TVDetailsViewModel", "Request to select season ${season.seasonNumber}: ${season.name}")
        
        // CRITICAL: Always get the current season data from the single source of truth
        val currentTvShow = _tvShowState.value
        if (currentTvShow == null) {
            android.util.Log.w("TVDetailsViewModel", "Cannot select season: no TV show loaded")
            return
        }
        
        // Find the current version of this season from the authoritative source
        val authoritativeSeason = currentTvShow.getSeasons().find { it.seasonNumber == season.seasonNumber }
        if (authoritativeSeason == null) {
            android.util.Log.w("TVDetailsViewModel", "Season ${season.seasonNumber} not found in current TV show data")
            return
        }
        
        android.util.Log.d("TVDetailsViewModel", "Using authoritative season data:")
        android.util.Log.d("TVDetailsViewModel", "  - Episodes loaded: ${authoritativeSeason.episodes.size}")
        android.util.Log.d("TVDetailsViewModel", "  - Episode count claimed: ${authoritativeSeason.episodeCount}")
        android.util.Log.d("TVDetailsViewModel", "  - Episodes have valid data: ${authoritativeSeason.episodes.any { it.id != "0" && it.title.isNotBlank() }}")
        
        // Update selected season with authoritative data
        _selectedSeason.value = authoritativeSeason
        
        // Determine if we need to load season data on demand
        val shouldLoadOnDemand = shouldLoadSeasonOnDemand(authoritativeSeason)
        
        if (shouldLoadOnDemand) {
            android.util.Log.d("TVDetailsViewModel", "Season ${authoritativeSeason.seasonNumber} needs on-demand loading")
            loadSeasonOnDemand(authoritativeSeason.seasonNumber)
            
            // Don't set selected episode yet since we're loading data
            selectEpisodeInternal(null)
        } else {
            android.util.Log.d("TVDetailsViewModel", "Season ${authoritativeSeason.seasonNumber} using existing episode data")
            
            // Select appropriate episode from authoritative data
            val selectedEpisode = selectAppropriateEpisode(authoritativeSeason)
            selectEpisodeInternal(selectedEpisode)
            
            android.util.Log.d("TVDetailsViewModel", "Selected episode: ${selectedEpisode?.title ?: "None"} (S${authoritativeSeason.seasonNumber}E${selectedEpisode?.episodeNumber ?: 0})")
        }
        
        // Update UI state with authoritative season
        updateState { copy(currentSeason = authoritativeSeason) }
    }
    
    /**
     * Determine if a season needs on-demand loading
     */
    private fun shouldLoadSeasonOnDemand(season: TVSeason): Boolean {
        return season.episodes.isEmpty() && 
               season.episodeCount > 0 && 
               !activeSeasonRequests.contains(season.seasonNumber)
    }
    
    /**
     * Select appropriate episode from a season (first unwatched or first episode)
     */
    private fun selectAppropriateEpisode(season: TVSeason): TVEpisode? {
        return season.episodes.find { !it.isWatched } ?: season.episodes.firstOrNull()
    }
    
    /**
     * Get current season data from single source of truth
     * This ensures UI components always get consistent, up-to-date season data
     */
    fun getCurrentSeasonFromAuthoritativeSource(): TVSeason? {
        val currentTvShow = _tvShowState.value
        val selectedSeasonNumber = _selectedSeason.value?.seasonNumber
        
        return if (currentTvShow != null && selectedSeasonNumber != null) {
            currentTvShow.getSeasons().find { it.seasonNumber == selectedSeasonNumber }
        } else {
            null
        }
    }
    
    /**
     * Get all seasons from single source of truth
     * This ensures UI components always get consistent season list
     */
    fun getAllSeasonsFromAuthoritativeSource(): List<TVSeason> {
        return _tvShowState.value?.getSeasons() ?: emptyList()
    }
    
    /**
     * Get season by number from single source of truth
     */
    fun getSeasonByNumberFromAuthoritativeSource(seasonNumber: Int): TVSeason? {
        return _tvShowState.value?.getSeasons()?.find { it.seasonNumber == seasonNumber }
    }
    
    /**
     * Select an episode and load sources (called by user interaction)
     */
    fun selectEpisode(episode: TVEpisode) {
        android.util.Log.d("TVDetailsViewModel", "=== Episode Selection (User) ===")
        android.util.Log.d("TVDetailsViewModel", "User selected episode: S${episode.seasonNumber}E${episode.episodeNumber} - ${episode.title}")
        
        _selectedEpisode.value = episode
        updateState { copy(currentEpisode = episode) }
        
        // Load sources for the selected episode (only on explicit user selection)
        _tvShowState.value?.let { tvShow ->
            android.util.Log.d("TVDetailsViewModel", "Loading sources for user-selected episode")
            loadSourcesForEpisode(tvShow, episode)
        }
    }
    
    /**
     * Internal method to select episode without loading sources (for system updates)
     */
    private fun selectEpisodeInternal(episode: TVEpisode?) {
        android.util.Log.d("TVDetailsViewModel", "=== Episode Selection (Internal) ===")
        android.util.Log.d("TVDetailsViewModel", "System selected episode: ${episode?.let { "S${it.seasonNumber}E${it.episodeNumber} - ${it.title}" } ?: "None"}")
        
        _selectedEpisode.value = episode
        updateState { copy(currentEpisode = episode) }
        
        // Do NOT load sources - this is for internal state management only
        android.util.Log.d("TVDetailsViewModel", "NOT loading sources for system-selected episode")
    }
    
    /**
     * Toggle watchlist status for the TV show
     */
    fun toggleWatchlist(tvShowId: String) {
        viewModelScope.launch {
            _tvShowState.value?.let { tvShow ->
                val currentActions = tvShow.actions.toMutableList()
                val watchlistIndex = currentActions.indexOfFirst { it is ContentAction.AddToWatchlist }
                
                if (watchlistIndex != -1) {
                    val currentAction = currentActions[watchlistIndex] as ContentAction.AddToWatchlist
                    currentActions[watchlistIndex] = ContentAction.AddToWatchlist(isInWatchlist = !currentAction.isInWatchlist)
                    
                    val updatedTvShow = tvShow.withActions(currentActions)
                    _tvShowState.value = updatedTvShow
                    updateState { copy(tvShow = updatedTvShow) }
                }
            }
        }
    }
    
    /**
     * Toggle like status for the TV show
     */
    fun toggleLike(tvShowId: String) {
        viewModelScope.launch {
            _tvShowState.value?.let { tvShow ->
                val currentActions = tvShow.actions.toMutableList()
                val likeIndex = currentActions.indexOfFirst { it is ContentAction.Like }
                
                if (likeIndex != -1) {
                    val currentAction = currentActions[likeIndex] as ContentAction.Like
                    currentActions[likeIndex] = ContentAction.Like(isLiked = !currentAction.isLiked)
                    
                    val updatedTvShow = tvShow.withActions(currentActions)
                    _tvShowState.value = updatedTvShow
                    updateState { copy(tvShow = updatedTvShow) }
                }
            }
        }
    }
    
    /**
     * Share TV show content
     */
    fun shareContent(tvShow: TVShowContentDetail) {
        viewModelScope.launch {
            // In a real app, this would trigger a share intent
            // For now, just log the action
            println("Sharing TV show: ${tvShow.title}")
        }
    }
    
    /**
     * Download an episode
     */
    fun downloadEpisode(episode: TVEpisode) {
        viewModelScope.launch {
            _tvShowState.value?.let { tvShow ->
                val currentActions = tvShow.actions.toMutableList()
                val downloadIndex = currentActions.indexOfFirst { it is ContentAction.Download }
                
                if (downloadIndex != -1) {
                    val currentAction = currentActions[downloadIndex] as ContentAction.Download
                    currentActions[downloadIndex] = ContentAction.Download(isDownloaded = currentAction.isDownloaded, isDownloading = true)
                    
                    val updatedTvShow = tvShow.withActions(currentActions)
                    _tvShowState.value = updatedTvShow
                    updateState { copy(tvShow = updatedTvShow) }
                    
                    // Simulate download process
                    // In a real app, this would trigger actual download
                    println("Downloading episode: ${episode.title}")
                }
            }
        }
    }
    
    /**
     * Mark episode as watched
     */
    fun markEpisodeWatched(episode: TVEpisode, progress: Float = 1.0f) {
        viewModelScope.launch {
            _tvShowState.value?.let { tvShow ->
                val tvShowDetail = tvShow.getTVShowDetail()
                val updatedSeasons = tvShowDetail.seasons.map { season ->
                    season.copy(
                        episodes = season.episodes.map { ep ->
                            if (ep.id == episode.id) {
                                ep.copy(isWatched = progress >= 0.9f, watchProgress = progress)
                            } else {
                                ep
                            }
                        }
                    )
                }
                
                val updatedTvShowDetail = tvShowDetail.copy(seasons = updatedSeasons)
                val updatedTvShow = tvShow.withTVShowDetail(updatedTvShowDetail)
                
                _tvShowState.value = updatedTvShow
                updateState { copy(tvShow = updatedTvShow) }
            }
        }
    }
    
    /**
     * Load related TV shows from TMDb
     */
    private fun loadRelatedShows(tvShow: TVShowContentDetail) {
        viewModelScope.launch {
            _relatedShowsState.value = UiState.Loading
            
            try {
                val tmdbId = tvShow.id.toIntOrNull()
                if (tmdbId != null) {
                    // Load recommendations from TMDb
                    tmdbTVRepository.getTVRecommendations(tmdbId).collect { result ->
                        when (result) {
                            is com.rdwatch.androidtv.repository.base.Result.Success -> {
                                // For now, return empty list until we implement proper mapping
                                // TODO: Convert TMDb recommendations to TVShowContentDetail list
                                val relatedShows = emptyList<TVShowContentDetail>()
                                
                                _relatedShowsState.value = UiState.Success(relatedShows)
                                updateState { copy(relatedShows = relatedShows) }
                            }
                            is com.rdwatch.androidtv.repository.base.Result.Error -> {
                                _relatedShowsState.value = UiState.Error(
                                    message = "Failed to load related shows: ${result.exception.message}",
                                    throwable = result.exception
                                )
                            }
                            is com.rdwatch.androidtv.repository.base.Result.Loading -> {
                                // Already set to Loading above
                            }
                        }
                    }
                } else {
                    // Invalid ID, return empty list
                    val relatedShows = emptyList<TVShowContentDetail>()
                    _relatedShowsState.value = UiState.Success(relatedShows)
                    updateState { copy(relatedShows = relatedShows) }
                }
                
            } catch (e: Exception) {
                _relatedShowsState.value = UiState.Error(
                    message = "Failed to load related shows: ${e.message}",
                    throwable = e
                )
            }
        }
    }
    
    /**
     * Load detailed season data with episodes from TMDb
     */
    private fun loadSeasonsWithEpisodes(tmdbId: Int, tvShowDetail: TVShowContentDetail) {
        // Cancel any existing season loading job to prevent race conditions
        seasonLoadingJob?.cancel()
        
        seasonLoadingJob = viewModelScope.launch {
            val tvShowDetails = tvShowDetail.getTVShowDetail()
            val numberOfSeasons = tvShowDetails.numberOfSeasons
            val existingSeasons = tvShowDetails.seasons
            
            android.util.Log.d("TVDetailsViewModel", "=== Season Loading Debug ===")
            android.util.Log.d("TVDetailsViewModel", "TV Show: ${tvShowDetails.title} (ID: $tmdbId)")
            android.util.Log.d("TVDetailsViewModel", "Number of seasons from initial data: $numberOfSeasons")
            android.util.Log.d("TVDetailsViewModel", "Existing seasons count: ${existingSeasons.size}")
            
            try {
                // Check if we already have episodes from the initial API response
                val hasEpisodesInInitialData = existingSeasons.any { it.episodes.isNotEmpty() }
                if (hasEpisodesInInitialData) {
                    android.util.Log.d("TVDetailsViewModel", "Initial data already contains episodes, using existing data")
                    updateTVShowWithSeasons(tvShowDetail, existingSeasons)
                    return@launch
                }
                
                // Determine which season to load initially
                val initialSeasonToLoad = determineInitialSeasonToLoad(existingSeasons)
                
                android.util.Log.d("TVDetailsViewModel", "Loading only season $initialSeasonToLoad initially")
                
                // Load the initial season with proper error handling
                loadInitialSeason(tmdbId, initialSeasonToLoad, tvShowDetail, existingSeasons, numberOfSeasons)
                
            } catch (e: Exception) {
                android.util.Log.e("TVDetailsViewModel", "Critical error in season loading: ${e.message}")
                handleSeasonLoadingError(tvShowDetail, existingSeasons, SeasonLoadingError.CriticalError(e))
            }
        }
    }
    
    /**
     * Determine which season to load initially based on existing data
     */
    private fun determineInitialSeasonToLoad(existingSeasons: List<TVSeason>): Int {
        return if (existingSeasons.isNotEmpty()) {
            existingSeasons.filter { it.seasonNumber > 0 }.minByOrNull { it.seasonNumber }?.seasonNumber ?: 1
        } else {
            1
        }
    }
    
    /**
     * Load the initial season with comprehensive error handling
     */
    private suspend fun loadInitialSeason(
        tmdbId: Int, 
        seasonNumber: Int, 
        tvShowDetail: TVShowContentDetail, 
        existingSeasons: List<TVSeason>,
        totalSeasons: Int
    ) {
        // Check for duplicate requests
        if (activeSeasonRequests.contains(seasonNumber)) {
            android.util.Log.d("TVDetailsViewModel", "Season $seasonNumber already being loaded, skipping duplicate request")
            return
        }
        
        activeSeasonRequests.add(seasonNumber)
        
        try {
            // Load season with timeout protection
            val result = withTimeoutOrNull(30000) { // 30 second timeout
                tmdbTVRepository.getSeasonDetails(tmdbId, seasonNumber)
                    .first { result -> 
                        // Wait for Success or Error, skip Loading states
                        result !is com.rdwatch.androidtv.repository.base.Result.Loading
                    }
            }
            
            when (result) {
                is com.rdwatch.androidtv.repository.base.Result.Success -> {
                    handleSeasonLoadSuccess(result.data, seasonNumber, tvShowDetail, existingSeasons, totalSeasons)
                }
                is com.rdwatch.androidtv.repository.base.Result.Error -> {
                    android.util.Log.e("TVDetailsViewModel", "API error loading season $seasonNumber: ${result.exception.message}")
                    handleSeasonLoadingError(tvShowDetail, existingSeasons, SeasonLoadingError.ApiError(result.exception))
                }
                is com.rdwatch.androidtv.repository.base.Result.Loading -> {
                    // This shouldn't happen with first{}, but handle it anyway
                    android.util.Log.w("TVDetailsViewModel", "Unexpected loading state received for season $seasonNumber")
                    handleSeasonLoadingError(tvShowDetail, existingSeasons, SeasonLoadingError.UnexpectedState)
                }
                null -> {
                    android.util.Log.w("TVDetailsViewModel", "Season $seasonNumber loading timed out after 30 seconds")
                    handleSeasonLoadingError(tvShowDetail, existingSeasons, SeasonLoadingError.Timeout)
                }
            }
        } finally {
            activeSeasonRequests.remove(seasonNumber)
        }
    }
    
    /**
     * Handle successful season loading
     */
    private fun handleSeasonLoadSuccess(
        seasonResponse: com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse,
        seasonNumber: Int,
        tvShowDetail: TVShowContentDetail,
        existingSeasons: List<TVSeason>,
        totalSeasons: Int
    ) {
        android.util.Log.d("TVDetailsViewModel", "Season $seasonNumber response: id=${seasonResponse.id}, episodes=${seasonResponse.episodes.size}")
        
        if (seasonResponse.id != 0 && (seasonResponse.episodes.isNotEmpty() || seasonResponse.episodeCount > 0)) {
            val tvSeason = mapTMDbSeasonResponseToTVSeason(seasonResponse)
            android.util.Log.d("TVDetailsViewModel", "Mapped season $seasonNumber: ${tvSeason.name} with ${tvSeason.episodes.size} episodes")
            
            // Build complete season list with loaded season and placeholders
            val allSeasons = buildSeasonsList(tvSeason, existingSeasons, totalSeasons)
            val sortedSeasons = allSeasons.sortedBy { it.seasonNumber }
            
            android.util.Log.d("TVDetailsViewModel", "Updating UI with season $seasonNumber loaded and ${sortedSeasons.size} total seasons")
            updateTVShowWithSeasons(tvShowDetail, sortedSeasons)
        } else {
            android.util.Log.w("TVDetailsViewModel", "Season $seasonNumber response was invalid (id=${seasonResponse.id}, episodes=${seasonResponse.episodes.size})")
            handleSeasonLoadingError(tvShowDetail, existingSeasons, SeasonLoadingError.InvalidData)
        }
    }
    
    /**
     * Build seasons list combining loaded season with existing/placeholder seasons
     */
    private fun buildSeasonsList(loadedSeason: TVSeason, existingSeasons: List<TVSeason>, totalSeasons: Int): List<TVSeason> {
        val allSeasons = mutableListOf<TVSeason>()
        
        if (existingSeasons.isNotEmpty()) {
            // Replace existing season with loaded version
            existingSeasons.forEach { existingSeason ->
                if (existingSeason.seasonNumber == loadedSeason.seasonNumber) {
                    allSeasons.add(loadedSeason)
                } else {
                    allSeasons.add(existingSeason)
                }
            }
        } else {
            // Create new list with loaded season and placeholders
            allSeasons.add(loadedSeason)
            (1..totalSeasons).forEach { seasonNum ->
                if (seasonNum != loadedSeason.seasonNumber) {
                    allSeasons.add(createPlaceholderSeason(seasonNum))
                }
            }
        }
        
        return allSeasons
    }
    
    /**
     * Create placeholder season for on-demand loading
     */
    private fun createPlaceholderSeason(seasonNumber: Int): TVSeason {
        return TVSeason(
            id = "season_$seasonNumber",
            seasonNumber = seasonNumber,
            name = "Season $seasonNumber",
            overview = "",
            posterPath = null,
            airDate = null,
            episodeCount = 1, // Show at least 1 to indicate there are episodes to load
            episodes = emptyList()
        )
    }
    
    /**
     * Handle various types of season loading errors
     */
    private fun handleSeasonLoadingError(
        tvShowDetail: TVShowContentDetail, 
        existingSeasons: List<TVSeason>, 
        error: SeasonLoadingError
    ) {
        android.util.Log.e("TVDetailsViewModel", "Season loading error: ${error.getMessage()}")
        
        // Update UI state to reflect error
        updateState { 
            copy(
                error = "Failed to load season details: ${error.getUserMessage()}",
                isLoading = false
            )
        }
        
        // Fall back to existing seasons or defaults
        useDefaultOrExistingSeasons(tvShowDetail, existingSeasons)
    }
    
    /**
     * Sealed class for different types of season loading errors
     */
    private sealed class SeasonLoadingError {
        data class ApiError(val exception: Throwable) : SeasonLoadingError()
        data class CriticalError(val exception: Throwable) : SeasonLoadingError()
        object Timeout : SeasonLoadingError()
        object InvalidData : SeasonLoadingError()
        object UnexpectedState : SeasonLoadingError()
        
        fun getMessage(): String = when (this) {
            is ApiError -> "API error: ${exception.message}"
            is CriticalError -> "Critical error: ${exception.message}"
            is Timeout -> "Request timed out"
            is InvalidData -> "Invalid season data received"
            is UnexpectedState -> "Unexpected loading state"
        }
        
        fun getUserMessage(): String = when (this) {
            is ApiError -> "Network error occurred"
            is CriticalError -> "Unexpected error occurred"
            is Timeout -> "Request timed out"
            is InvalidData -> "Invalid data received"
            is UnexpectedState -> "Loading error"
        }
    }
    
    /**
     * Helper method to use default or existing seasons when loading fails
     */
    private fun useDefaultOrExistingSeasons(tvShowDetail: TVShowContentDetail, existingSeasons: List<TVSeason>) {
        if (existingSeasons.isNotEmpty()) {
            android.util.Log.d("TVDetailsViewModel", "Using existing seasons from initial data")
            updateTVShowWithSeasons(tvShowDetail, existingSeasons)
        } else {
            android.util.Log.w("TVDetailsViewModel", "Using default seasons as fallback")
            val defaultSeasons = getDefaultSeasons()
            updateTVShowWithSeasons(tvShowDetail, defaultSeasons)
        }
    }
    
    /**
     * Load additional seasons on demand (when user navigates to them)
     */
    fun loadSeasonOnDemand(seasonNumber: Int) {
        android.util.Log.d("TVDetailsViewModel", "=== Load Season On Demand Debug ===")
        android.util.Log.d("TVDetailsViewModel", "Requested season: $seasonNumber")
        
        // Validate preconditions
        val validationResult = validateOnDemandLoadingPreconditions(seasonNumber)
        if (!validationResult.isValid) {
            android.util.Log.w("TVDetailsViewModel", "Validation failed: ${validationResult.reason}")
            return
        }
        
        val tmdbId = validationResult.tmdbId!!
        val currentTvShow = validationResult.tvShow!!
        
        // Start on-demand loading
        startOnDemandSeasonLoading(seasonNumber, tmdbId, currentTvShow)
    }
    
    /**
     * Validation result for on-demand loading preconditions
     */
    private data class OnDemandValidationResult(
        val isValid: Boolean,
        val reason: String? = null,
        val tmdbId: Int? = null,
        val tvShow: TVShowContentDetail? = null
    )
    
    /**
     * Validate preconditions for on-demand season loading
     */
    private fun validateOnDemandLoadingPreconditions(seasonNumber: Int): OnDemandValidationResult {
        val currentTvShow = _tvShowState.value
        if (currentTvShow == null) {
            return OnDemandValidationResult(false, "No TV show loaded")
        }
        
        val tmdbId = currentTvShow.id.toIntOrNull()
        if (tmdbId == null) {
            return OnDemandValidationResult(false, "Invalid TMDb ID '${currentTvShow.id}'")
        }
        
        // Check if we're already loading this season
        if (activeSeasonRequests.contains(seasonNumber)) {
            return OnDemandValidationResult(false, "Season $seasonNumber already being loaded")
        }
        
        // Check if season already has valid episodes loaded
        val currentSeason = currentTvShow.getTVShowDetail().seasons.find { it.seasonNumber == seasonNumber }
        val hasValidEpisodes = currentSeason?.episodes?.isNotEmpty() == true && 
                              currentSeason.episodes.any { it.id != "0" && it.title.isNotBlank() }
        
        if (hasValidEpisodes) {
            return OnDemandValidationResult(false, "Season $seasonNumber already has ${currentSeason?.episodes?.size} valid episodes")
        }
        
        android.util.Log.d("TVDetailsViewModel", "Season $seasonNumber validation passed:")
        android.util.Log.d("TVDetailsViewModel", "  - Current episodes: ${currentSeason?.episodes?.size ?: 0}")
        android.util.Log.d("TVDetailsViewModel", "  - Has valid episodes: $hasValidEpisodes")
        android.util.Log.d("TVDetailsViewModel", "  - Episode count claimed: ${currentSeason?.episodeCount ?: 0}")
        
        return OnDemandValidationResult(true, tmdbId = tmdbId, tvShow = currentTvShow)
    }
    
    /**
     * Start on-demand season loading with proper error handling
     */
    private fun startOnDemandSeasonLoading(seasonNumber: Int, tmdbId: Int, currentTvShow: TVShowContentDetail) {
        // Cancel any existing job for this season to prevent duplicates
        onDemandSeasonJobs[seasonNumber]?.cancel()
        android.util.Log.d("TVDetailsViewModel", "Cancelled any existing job for season $seasonNumber")
        
        onDemandSeasonJobs[seasonNumber] = viewModelScope.launch {
            android.util.Log.d("TVDetailsViewModel", "Loading season $seasonNumber on demand for TV $tmdbId")
            
            activeSeasonRequests.add(seasonNumber)
            
            try {
                val result = withTimeoutOrNull(30000) { // 30 second timeout
                    tmdbTVRepository.getSeasonDetails(tmdbId, seasonNumber)
                        .first { result -> 
                            // Wait for Success or Error, skip Loading states
                            result !is com.rdwatch.androidtv.repository.base.Result.Loading
                        }
                }
                
                when (result) {
                    is com.rdwatch.androidtv.repository.base.Result.Success -> {
                        handleOnDemandSeasonLoadSuccess(result.data, seasonNumber)
                    }
                    is com.rdwatch.androidtv.repository.base.Result.Error -> {
                        android.util.Log.e("TVDetailsViewModel", "API error loading season $seasonNumber on demand: ${result.exception.message}")
                        handleOnDemandSeasonLoadError(seasonNumber, OnDemandSeasonLoadingError.ApiError(result.exception))
                    }
                    is com.rdwatch.androidtv.repository.base.Result.Loading -> {
                        // This shouldn't happen with first{}, but handle it anyway
                        android.util.Log.w("TVDetailsViewModel", "Unexpected loading state received for season $seasonNumber")
                        handleOnDemandSeasonLoadError(seasonNumber, OnDemandSeasonLoadingError.UnexpectedState)
                    }
                    null -> {
                        android.util.Log.w("TVDetailsViewModel", "Season $seasonNumber loading timed out after 30 seconds")
                        handleOnDemandSeasonLoadError(seasonNumber, OnDemandSeasonLoadingError.Timeout)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TVDetailsViewModel", "Critical error loading season $seasonNumber on demand: ${e.message}")
                handleOnDemandSeasonLoadError(seasonNumber, OnDemandSeasonLoadingError.CriticalError(e))
            } finally {
                activeSeasonRequests.remove(seasonNumber)
                onDemandSeasonJobs.remove(seasonNumber)
            }
        }
    }
    
    /**
     * Handle successful on-demand season loading
     */
    private fun handleOnDemandSeasonLoadSuccess(
        seasonResponse: com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse,
        seasonNumber: Int
    ) {
        if (seasonResponse.id != 0 && (seasonResponse.episodes.isNotEmpty() || seasonResponse.episodeCount > 0)) {
            val tvSeason = mapTMDbSeasonResponseToTVSeason(seasonResponse)
            
            // Get the current TV show state again in case it changed
            val latestTvShow = _tvShowState.value
            if (latestTvShow != null) {
                // Update the specific season in the current data
                val currentTvShowDetail = latestTvShow.getTVShowDetail()
                val updatedSeasons = currentTvShowDetail.seasons.map { season ->
                    if (season.seasonNumber == seasonNumber) {
                        tvSeason
                    } else {
                        season
                    }
                }
                
                val updatedTvShowDetail = currentTvShowDetail.copy(seasons = updatedSeasons)
                val updatedTvShow = latestTvShow.withTVShowDetail(updatedTvShowDetail)
                
                _tvShowState.value = updatedTvShow
                updateState { copy(tvShow = updatedTvShow) }
                
                // CRITICAL: Update the selected season state if it matches the loaded season
                val currentSelectedSeason = _selectedSeason.value
                if (currentSelectedSeason?.seasonNumber == seasonNumber) {
                    android.util.Log.d("TVDetailsViewModel", "Updating selected season state with loaded data")
                    _selectedSeason.value = tvSeason
                }
                
                android.util.Log.d("TVDetailsViewModel", "Season $seasonNumber loaded on demand with ${tvSeason.episodes.size} episodes")
                android.util.Log.d("TVDetailsViewModel", "Updated season episodeCount: ${tvSeason.episodeCount}")
            } else {
                android.util.Log.w("TVDetailsViewModel", "TV show state became null during season loading")
                handleOnDemandSeasonLoadError(seasonNumber, OnDemandSeasonLoadingError.StateCorrupted)
            }
        } else {
            android.util.Log.w("TVDetailsViewModel", "Season $seasonNumber response was invalid (id=${seasonResponse.id}, episodes=${seasonResponse.episodes.size})")
            handleOnDemandSeasonLoadError(seasonNumber, OnDemandSeasonLoadingError.InvalidData)
        }
    }
    
    /**
     * Handle on-demand season loading errors
     */
    private fun handleOnDemandSeasonLoadError(seasonNumber: Int, error: OnDemandSeasonLoadingError) {
        android.util.Log.e("TVDetailsViewModel", "On-demand season loading error for season $seasonNumber: ${error.getMessage()}")
        
        // For on-demand loading errors, we don't want to clear the whole UI state
        // Instead, we might want to show a toast or update a specific error state
        // For now, just log the error - the UI will continue to show the placeholder
    }
    
    /**
     * Sealed class for on-demand season loading errors
     */
    private sealed class OnDemandSeasonLoadingError {
        data class ApiError(val exception: Throwable) : OnDemandSeasonLoadingError()
        data class CriticalError(val exception: Throwable) : OnDemandSeasonLoadingError()
        object Timeout : OnDemandSeasonLoadingError()
        object InvalidData : OnDemandSeasonLoadingError()
        object UnexpectedState : OnDemandSeasonLoadingError()
        object StateCorrupted : OnDemandSeasonLoadingError()
        
        fun getMessage(): String = when (this) {
            is ApiError -> "API error: ${exception.message}"
            is CriticalError -> "Critical error: ${exception.message}"
            is Timeout -> "Request timed out"
            is InvalidData -> "Invalid season data received"
            is UnexpectedState -> "Unexpected loading state"
            is StateCorrupted -> "Application state corrupted"
        }
    }
    
    /**
     * Update the TV show with loaded seasons and synchronize UI state
     */
    private fun updateTVShowWithSeasons(originalTvShow: TVShowContentDetail, seasons: List<TVSeason>) {
        android.util.Log.d("TVDetailsViewModel", "=== Updating TV Show with Seasons ===")
        seasons.forEach { season ->
            android.util.Log.d("TVDetailsViewModel", "Season ${season.seasonNumber}: ${season.name} - ${season.episodes.size} episodes loaded (claimed: ${season.episodeCount})")
        }
        
        // Step 1: Update the TV show data (single source of truth)
        val updatedTvShowDetail = originalTvShow.getTVShowDetail().copy(seasons = seasons)
        val updatedTvShow = originalTvShow.withTVShowDetail(updatedTvShowDetail)
        _tvShowState.value = updatedTvShow
        
        // Step 2: Synchronize selected season with updated data
        synchronizeSeasonSelection(seasons)
        
        android.util.Log.d("TVDetailsViewModel", "Updated TV show with ${seasons.size} seasons")
    }
    
    /**
     * Synchronize selected season state with updated season data
     * This ensures UI state remains consistent with data updates
     */
    private fun synchronizeSeasonSelection(updatedSeasons: List<TVSeason>) {
        val currentSelectedSeason = _selectedSeason.value
        val currentSelectedEpisode = _selectedEpisode.value
        
        android.util.Log.d("TVDetailsViewModel", "=== Season Selection Sync ===")
        android.util.Log.d("TVDetailsViewModel", "Current: Season ${currentSelectedSeason?.seasonNumber}, Episode ${currentSelectedEpisode?.episodeNumber}")
        
        // Find the updated version of the currently selected season
        val updatedSelectedSeason = currentSelectedSeason?.let { selected ->
            updatedSeasons.find { it.seasonNumber == selected.seasonNumber }
        } ?: updatedSeasons.firstOrNull() // Default to first season if none selected
        
        if (updatedSelectedSeason != null) {
            // Update selected season with fresh data
            _selectedSeason.value = updatedSelectedSeason
            
            // Synchronize episode selection if needed
            synchronizeEpisodeSelection(updatedSelectedSeason, currentSelectedEpisode)
            
            // Update UI state
            updateState { 
                copy(
                    tvShow = _tvShowState.value,
                    currentSeason = updatedSelectedSeason
                )
            }
            
            android.util.Log.d("TVDetailsViewModel", "Synchronized to Season ${updatedSelectedSeason.seasonNumber} with ${updatedSelectedSeason.episodes.size} episodes")
        } else {
            android.util.Log.w("TVDetailsViewModel", "No seasons available to select")
        }
    }
    
    /**
     * Synchronize episode selection within the updated season data
     */
    private fun synchronizeEpisodeSelection(updatedSeason: TVSeason, currentEpisode: TVEpisode?) {
        val episodeToSelect = when {
            // Try to preserve current episode if it exists in updated season
            currentEpisode != null -> {
                updatedSeason.episodes.find { it.episodeNumber == currentEpisode.episodeNumber }
                    ?: getDefaultEpisodeSelection(updatedSeason)
            }
            // No current episode, select default
            else -> getDefaultEpisodeSelection(updatedSeason)
        }
        
        if (episodeToSelect != null) {
            selectEpisodeInternal(episodeToSelect)
            android.util.Log.d("TVDetailsViewModel", "Selected episode: S${updatedSeason.seasonNumber}E${episodeToSelect.episodeNumber} - ${episodeToSelect.title}")
        }
    }
    
    /**
     * Get default episode selection for a season (first unwatched or first episode)
     */
    private fun getDefaultEpisodeSelection(season: TVSeason): TVEpisode? {
        return season.episodes.find { !it.isWatched } ?: season.episodes.firstOrNull()
    }
    
    /**
     * Consolidated TMDb to UI model mapper - handles both single seasons and season lists
     */
    object TMDbMapper {
        /**
         * Map TMDb season response to TVSeason (handles both detailed and basic season data)
         */
        fun mapSeasonToTVSeason(seasonResponse: com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse): TVSeason {
            // Validate essential data before mapping
            validateSeasonData(seasonResponse)
            
            val mappedEpisodes = mapEpisodesToTVEpisodes(seasonResponse.episodes, seasonResponse.seasonNumber)
            
            // Validate episode count consistency after mapping
            validateEpisodeCountConsistency(seasonResponse, mappedEpisodes)
            
            return TVSeason(
                id = seasonResponse.id.toString(),
                seasonNumber = seasonResponse.seasonNumber,
                name = seasonResponse.name.ifBlank { "Season ${seasonResponse.seasonNumber}" },
                overview = seasonResponse.overview,
                posterPath = seasonResponse.posterPath?.let { buildImageUrl(it) },
                airDate = seasonResponse.airDate,
                episodeCount = seasonResponse.episodeCount,
                episodes = mappedEpisodes,
                voteAverage = seasonResponse.voteAverage.toFloat()
            )
        }
        
        /**
         * Map list of TMDb seasons to UI TVSeason models
         */
        fun mapSeasonsToTVSeasons(tmdbSeasons: List<com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse>): List<TVSeason> {
            return tmdbSeasons.map { mapSeasonToTVSeason(it) }
        }
        
        /**
         * Map TMDb episodes to UI TVEpisode models with enhanced data preservation
         */
        fun mapEpisodesToTVEpisodes(tmdbEpisodes: List<com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse>, seasonNumber: Int): List<TVEpisode> {
            return tmdbEpisodes.map { mapEpisodeToTVEpisode(it, seasonNumber) }
        }
        
        /**
         * Map single TMDb episode to UI TVEpisode model with complete data preservation
         */
        fun mapEpisodeToTVEpisode(tmdbEpisode: com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse, seasonNumber: Int? = null): TVEpisode {
            // Validate episode data before mapping
            val finalSeasonNumber = seasonNumber ?: tmdbEpisode.seasonNumber
            validateEpisodeData(tmdbEpisode, finalSeasonNumber)
            
            return TVEpisode(
                id = tmdbEpisode.id.toString(),
                seasonNumber = finalSeasonNumber,
                episodeNumber = tmdbEpisode.episodeNumber,
                title = tmdbEpisode.name.ifBlank { "Episode ${tmdbEpisode.episodeNumber}" },
                description = tmdbEpisode.overview,
                thumbnailUrl = tmdbEpisode.stillPath?.let { buildImageUrl(it) },
                airDate = tmdbEpisode.airDate,
                runtime = tmdbEpisode.runtime,
                stillPath = tmdbEpisode.stillPath?.let { buildImageUrl(it) },
                voteAverage = tmdbEpisode.voteAverage.toFloat(),
                voteCount = tmdbEpisode.voteCount,
                overview = tmdbEpisode.overview,
                isWatched = false,
                watchProgress = 0f,
                resumePosition = 0L,
                videoUrl = null // Will be populated later with streaming sources
            )
        }
        
        /**
         * Data preservation validation methods
         */
        
        /**
         * Validate essential season data to prevent data loss
         */
        private fun validateSeasonData(seasonResponse: com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse) {
            // Log warnings for potential data issues that could cause loss
            if (seasonResponse.id == 0) {
                android.util.Log.w("TMDbMapper", "Season ${seasonResponse.seasonNumber} has invalid ID (0)")
            }
            if (seasonResponse.name.isBlank()) {
                android.util.Log.w("TMDbMapper", "Season ${seasonResponse.seasonNumber} has blank name")
            }
            if (seasonResponse.episodeCount > 0 && seasonResponse.episodes.isEmpty()) {
                android.util.Log.w("TMDbMapper", "Season ${seasonResponse.seasonNumber} claims ${seasonResponse.episodeCount} episodes but has no episode data")
            }
        }
        
        /**
         * Validate episode count consistency to prevent data loss
         */
        private fun validateEpisodeCountConsistency(
            seasonResponse: com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse,
            mappedEpisodes: List<TVEpisode>
        ) {
            if (seasonResponse.episodeCount > 0 && mappedEpisodes.size != seasonResponse.episodeCount) {
                android.util.Log.w("TMDbMapper", 
                    "Episode count mismatch for season ${seasonResponse.seasonNumber}: " +
                    "claimed ${seasonResponse.episodeCount}, mapped ${mappedEpisodes.size}"
                )
            }
        }
        
        /**
         * Validate episode data completeness
         */
        private fun validateEpisodeData(tmdbEpisode: com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse, seasonNumber: Int) {
            if (tmdbEpisode.id == 0) {
                android.util.Log.w("TMDbMapper", "Episode S${seasonNumber}E${tmdbEpisode.episodeNumber} has invalid ID (0)")
            }
            if (tmdbEpisode.name.isBlank()) {
                android.util.Log.w("TMDbMapper", "Episode S${seasonNumber}E${tmdbEpisode.episodeNumber} has blank title")
            }
        }
        
        /**
         * Centralized image URL builder for consistency
         */
        private fun buildImageUrl(path: String): String {
            return "https://image.tmdb.org/t/p/w500$path"
        }
    }
    
    // Legacy wrapper methods for backward compatibility
    private fun mapTMDbSeasonResponseToTVSeason(seasonResponse: com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse): TVSeason {
        return TMDbMapper.mapSeasonToTVSeason(seasonResponse)
    }
    
    /**
     * Load TV show credits (cast and crew) from TMDb
     */
    private fun loadTVCredits(tmdbId: Int) {
        viewModelScope.launch {
            _creditsState.value = UiState.Loading
            
            tmdbTVRepository.getTVCredits(tmdbId).collect { result ->
                when (result) {
                    is com.rdwatch.androidtv.repository.base.Result.Loading -> {
                        _creditsState.value = UiState.Loading
                    }
                    is com.rdwatch.androidtv.repository.base.Result.Success -> {
                        val creditsResponse = result.data
                        
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
                        
                        // Update the TV show detail with cast and crew
                        _tvShowState.value?.let { tvShow ->
                            val tvShowDetail = tvShow.getTVShowDetail()
                            val updatedTvShowDetail = tvShowDetail.copy(
                                fullCast = castMembers,
                                crew = crewMembers,
                                cast = castMembers.take(5).map { it.name } // Keep top 5 for legacy compatibility
                            )
                            val updatedTvShow = tvShow.withTVShowDetail(updatedTvShowDetail)
                            
                            _tvShowState.value = updatedTvShow
                            updateState { copy(tvShow = updatedTvShow) }
                        }
                    }
                    is com.rdwatch.androidtv.repository.base.Result.Error -> {
                        _creditsState.value = UiState.Error(
                            message = "Failed to load TV show credits: ${result.exception.message}",
                            throwable = result.exception
                        )
                    }
                }
            }
        }
    }
    
    /**
     * Refresh TV show data
     */
    fun refresh() {
        _tvShowState.value?.let { tvShow ->
            loadTVShow(tvShow.id)
        }
    }
    
    /**
     * Select a tab
     */
    fun selectTab(tabIndex: Int) {
        _selectedTabIndex.value = tabIndex
    }
    
    /**
     * Load streaming sources for a TV show episode from scrapers
     */
    fun loadSourcesForEpisode(tvShow: TVShowContentDetail, episode: TVEpisode) {
        viewModelScope.launch {
            _sourcesState.value = UiState.Loading
            updateState { copy(sourcesLoading = true, sourcesError = null) }
            
            try {
                android.util.Log.d("TVDetailsViewModel", "Loading sources for episode: S${episode.seasonNumber}E${episode.episodeNumber} - ${episode.title}")
                
                val sources = scraperSourceManager.getSourcesForTVEpisode(
                    tvShowId = tvShow.id,
                    seasonNumber = episode.seasonNumber,
                    episodeNumber = episode.episodeNumber,
                    imdbId = null, // TMDb TV shows don't have IMDb IDs in this model
                    tmdbId = tvShow.id
                )
                
                android.util.Log.d("TVDetailsViewModel", "Loaded ${sources.size} sources for episode")
                
                _sourcesState.value = UiState.Success(sources)
                updateState { copy(availableSources = sources, sourcesLoading = false) }
            } catch (e: Exception) {
                val errorMessage = when {
                    e.message?.contains("timeout", ignoreCase = true) == true -> "Request timed out. Please try again."
                    e.message?.contains("network", ignoreCase = true) == true -> "Network error. Check your connection."
                    e.message?.contains("unauthorized", ignoreCase = true) == true -> "Scraper authentication failed."
                    e.message?.contains("not found", ignoreCase = true) == true -> "Episode not found in scrapers."
                    else -> "Failed to load sources: ${e.message}"
                }
                
                android.util.Log.e("TVDetailsViewModel", "Failed to load sources for episode: $errorMessage")
                
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
     * Retry loading sources for current episode
     */
    fun retryLoadingSources() {
        val currentTvShow = _tvShowState.value
        val currentEpisode = _selectedEpisode.value
        
        if (currentTvShow != null && currentEpisode != null) {
            updateState { copy(sourcesError = null) }
            loadSourcesForEpisode(currentTvShow, currentEpisode)
        }
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
     * Clear all data
     */
    fun clearData() {
        // Cancel all ongoing season loading jobs
        seasonLoadingJob?.cancel()
        onDemandSeasonJobs.values.forEach { it.cancel() }
        onDemandSeasonJobs.clear()
        activeSeasonRequests.clear()
        
        _tvShowState.value = null
        _selectedSeason.value = null
        selectEpisodeInternal(null)
        _relatedShowsState.value = UiState.Loading
        _creditsState.value = UiState.Loading
        _sourcesState.value = UiState.Idle
        _selectedTabIndex.value = 0
        
        updateState { createInitialState() }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up jobs when ViewModel is destroyed
        seasonLoadingJob?.cancel()
        onDemandSeasonJobs.values.forEach { it.cancel() }
        onDemandSeasonJobs.clear()
        activeSeasonRequests.clear()
    }
    
    /**
     * Get default seasons for TV shows that don't have season data
     */
    private fun getDefaultSeasons(): List<TVSeason> {
        return listOf(
            TVSeason(
                id = "season_1",
                seasonNumber = 1,
                name = "Season 1",
                overview = "First season of the show",
                posterPath = null,
                airDate = "2023-01-01",
                episodeCount = 10,
                episodes = (1..10).map { episodeNum ->
                    TVEpisode(
                        id = "episode_${episodeNum}",
                        seasonNumber = 1,
                        episodeNumber = episodeNum,
                        title = "Episode $episodeNum",
                        description = "Description for episode $episodeNum",
                        thumbnailUrl = null,
                        airDate = "2023-01-${episodeNum.toString().padStart(2, '0')}",
                        runtime = 45,
                        stillPath = null,
                        isWatched = false,
                        watchProgress = 0f
                    )
                }
            )
        )
    }
    
    private fun mapTMDbSeasonsToTVSeasons(tmdbSeasons: List<com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse>): List<TVSeason> {
        return TMDbMapper.mapSeasonsToTVSeasons(tmdbSeasons)
    }
    
    private fun mapTMDbEpisodesToTVEpisodes(tmdbEpisodes: List<com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse>, seasonNumber: Int): List<TVEpisode> {
        return TMDbMapper.mapEpisodesToTVEpisodes(tmdbEpisodes, seasonNumber)
    }
    
    private fun mapTMDbEpisodeToTVEpisode(tmdbEpisode: com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse, seasonNumber: Int? = null): TVEpisode {
        return TMDbMapper.mapEpisodeToTVEpisode(tmdbEpisode, seasonNumber)
    }
}

/**
 * UI State for TV Details Screen
 */
data class TVDetailsUiState(
    val tvShow: TVShowContentDetail? = null,
    val currentSeason: TVSeason? = null,
    val currentEpisode: TVEpisode? = null,
    val relatedShows: List<TVShowContentDetail> = emptyList(),
    val isLoading: Boolean = false,
    val isLoaded: Boolean = false,
    val error: String? = null,
    val isFromRealDebrid: Boolean = false,
    val availableSources: List<StreamingSource> = emptyList(),
    val sourcesLoading: Boolean = false,
    val sourcesError: String? = null
) {
    val hasSourcesError: Boolean get() = sourcesError != null
    val isSourcesLoading: Boolean get() = sourcesLoading
    val hasAvailableSources: Boolean get() = availableSources.isNotEmpty()
}