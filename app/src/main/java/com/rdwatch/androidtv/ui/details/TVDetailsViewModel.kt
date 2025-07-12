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
     */
    fun selectSeason(season: TVSeason) {
        android.util.Log.d("TVDetailsViewModel", "=== Season Selection Debug ===")
        android.util.Log.d("TVDetailsViewModel", "Selecting season ${season.seasonNumber}: ${season.name}")
        android.util.Log.d("TVDetailsViewModel", "  - Episodes loaded: ${season.episodes.size}")
        android.util.Log.d("TVDetailsViewModel", "  - Episode count claimed: ${season.episodeCount}")
        android.util.Log.d("TVDetailsViewModel", "  - Episodes have valid data: ${season.episodes.any { it.id != "0" && it.title.isNotBlank() }}")
        
        _selectedSeason.value = season
        
        // Only load season on demand if ALL conditions are met:
        // 1. No episodes are currently loaded
        // 2. Season claims to have episodes (episodeCount > 0)
        // 3. Season is not already being loaded (to prevent duplicate requests)
        val shouldLoadOnDemand = season.episodes.isEmpty() && 
                                 season.episodeCount > 0 && 
                                 !activeSeasonRequests.contains(season.seasonNumber)
        
        if (shouldLoadOnDemand) {
            android.util.Log.d("TVDetailsViewModel", "Season ${season.seasonNumber} needs on-demand loading: no episodes loaded but episodeCount=${season.episodeCount}")
            loadSeasonOnDemand(season.seasonNumber)
            
            // Don't set selected episode yet since we're loading data
            selectEpisodeInternal(null)
        } else {
            android.util.Log.d("TVDetailsViewModel", "Season ${season.seasonNumber} using existing episode data")
            
            // Select first episode of the season or next unwatched episode
            val nextEpisode = season.episodes.find { !it.isWatched } ?: season.episodes.firstOrNull()
            selectEpisodeInternal(nextEpisode)
            
            android.util.Log.d("TVDetailsViewModel", "Selected episode: ${nextEpisode?.title ?: "None"} (S${season.seasonNumber}E${nextEpisode?.episodeNumber ?: 0})")
        }
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
            
            try {
                android.util.Log.d("TVDetailsViewModel", "=== Season Loading Debug ===")
                android.util.Log.d("TVDetailsViewModel", "TV Show: ${tvShowDetails.title} (ID: $tmdbId)")
                android.util.Log.d("TVDetailsViewModel", "Number of seasons from initial data: $numberOfSeasons")
                android.util.Log.d("TVDetailsViewModel", "Existing seasons count: ${existingSeasons.size}")
                
                // Check if we already have episodes from the initial API response
                val hasEpisodesInInitialData = existingSeasons.any { it.episodes.isNotEmpty() }
                if (hasEpisodesInInitialData) {
                    android.util.Log.d("TVDetailsViewModel", "Initial data already contains episodes, using existing data")
                    updateTVShowWithSeasons(tvShowDetail, existingSeasons)
                    return@launch
                }
                
                // Only load season 1 initially to avoid excessive API calls
                val initialSeasonToLoad = if (existingSeasons.isNotEmpty()) {
                    existingSeasons.filter { it.seasonNumber > 0 }.minByOrNull { it.seasonNumber }?.seasonNumber ?: 1
                } else {
                    1
                }
                
                android.util.Log.d("TVDetailsViewModel", "Loading only season $initialSeasonToLoad initially")
                
                // Check if we're already loading this season to prevent duplicates
                if (activeSeasonRequests.contains(initialSeasonToLoad)) {
                    android.util.Log.d("TVDetailsViewModel", "Season $initialSeasonToLoad already being loaded, skipping duplicate request")
                    return@launch
                }
                
                activeSeasonRequests.add(initialSeasonToLoad)
                
                try {
                    // Load just the first season's details with timeout protection
                    val result = withTimeoutOrNull(30000) { // 30 second timeout
                        tmdbTVRepository.getSeasonDetails(tmdbId, initialSeasonToLoad)
                            .first { result -> 
                                // Wait for Success or Error, skip Loading states
                                result !is com.rdwatch.androidtv.repository.base.Result.Loading
                            }
                    }
                    
                    when (result) {
                        is com.rdwatch.androidtv.repository.base.Result.Success -> {
                            val seasonResponse = result.data
                            android.util.Log.d("TVDetailsViewModel", "Season $initialSeasonToLoad response: id=${seasonResponse.id}, episodes=${seasonResponse.episodes.size}")
                            
                            if (seasonResponse.id != 0 && seasonResponse.episodes.isNotEmpty()) {
                                val tvSeason = mapTMDbSeasonResponseToTVSeason(seasonResponse)
                                android.util.Log.d("TVDetailsViewModel", "Mapped season $initialSeasonToLoad: ${tvSeason.name} with ${tvSeason.episodes.size} episodes")
                                
                                // Create a list with the loaded season and placeholder info for other seasons
                                val allSeasons = mutableListOf<TVSeason>()
                                
                                // Add all seasons with basic info, but only the loaded one has episodes
                                existingSeasons.forEach { existingSeason ->
                                    if (existingSeason.seasonNumber == initialSeasonToLoad) {
                                        allSeasons.add(tvSeason)
                                    } else {
                                        // Keep the basic season info without episodes
                                        allSeasons.add(existingSeason)
                                    }
                                }
                                
                                // If we don't have existing seasons info, create placeholders
                                if (existingSeasons.isEmpty()) {
                                    allSeasons.add(tvSeason)
                                    // Add placeholders for other seasons
                                    (1..numberOfSeasons).forEach { seasonNum ->
                                        if (seasonNum != initialSeasonToLoad) {
                                            allSeasons.add(
                                                TVSeason(
                                                    id = "season_$seasonNum",
                                                    seasonNumber = seasonNum,
                                                    name = "Season $seasonNum",
                                                    overview = "",
                                                    posterPath = null,
                                                    airDate = null,
                                                    episodeCount = 1, // Show at least 1 to indicate there are episodes to load
                                                    episodes = emptyList()
                                                )
                                            )
                                        }
                                    }
                                }
                                
                                val sortedSeasons = allSeasons.sortedBy { it.seasonNumber }
                                android.util.Log.d("TVDetailsViewModel", "Updating UI with season $initialSeasonToLoad loaded and ${sortedSeasons.size} total seasons")
                                updateTVShowWithSeasons(tvShowDetail, sortedSeasons)
                            } else {
                                android.util.Log.w("TVDetailsViewModel", "Season $initialSeasonToLoad response was invalid")
                                useDefaultOrExistingSeasons(tvShowDetail, existingSeasons)
                            }
                        }
                        is com.rdwatch.androidtv.repository.base.Result.Error -> {
                            android.util.Log.e("TVDetailsViewModel", "Failed to load season $initialSeasonToLoad: ${result.exception.message}")
                            useDefaultOrExistingSeasons(tvShowDetail, existingSeasons)
                        }
                        is com.rdwatch.androidtv.repository.base.Result.Loading -> {
                            // This shouldn't happen with first{}, but handle it anyway
                            android.util.Log.w("TVDetailsViewModel", "Unexpected loading state received")
                            useDefaultOrExistingSeasons(tvShowDetail, existingSeasons)
                        }
                        null -> {
                            android.util.Log.w("TVDetailsViewModel", "Season $initialSeasonToLoad loading timed out after 30 seconds")
                            useDefaultOrExistingSeasons(tvShowDetail, existingSeasons)
                        }
                    }
                } finally {
                    activeSeasonRequests.remove(initialSeasonToLoad)
                }
                
            } catch (e: Exception) {
                android.util.Log.e("TVDetailsViewModel", "Error loading seasons: ${e.message}")
                useDefaultOrExistingSeasons(tvShowDetail, existingSeasons)
                // Clean up active requests - remove the season we were trying to load
                val initialSeasonToLoad = if (existingSeasons.isNotEmpty()) {
                    existingSeasons.filter { it.seasonNumber > 0 }.minByOrNull { it.seasonNumber }?.seasonNumber ?: 1
                } else {
                    1
                }
                activeSeasonRequests.remove(initialSeasonToLoad)
            }
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
        
        val currentTvShow = _tvShowState.value
        if (currentTvShow == null) {
            android.util.Log.w("TVDetailsViewModel", "No TV show loaded, cannot load season $seasonNumber")
            return
        }
        
        val tmdbId = currentTvShow.id.toIntOrNull()
        if (tmdbId == null) {
            android.util.Log.w("TVDetailsViewModel", "Invalid TMDb ID '${currentTvShow.id}', cannot load season $seasonNumber")
            return
        }
        
        // Cancel any existing job for this season to prevent duplicates
        onDemandSeasonJobs[seasonNumber]?.cancel()
        android.util.Log.d("TVDetailsViewModel", "Cancelled any existing job for season $seasonNumber")
        
        // Check if we're already loading this season
        if (activeSeasonRequests.contains(seasonNumber)) {
            android.util.Log.d("TVDetailsViewModel", "Season $seasonNumber already being loaded, skipping duplicate request")
            return
        }
        
        // Check if season already has valid episodes loaded
        val currentSeason = currentTvShow.getTVShowDetail().seasons.find { it.seasonNumber == seasonNumber }
        val hasValidEpisodes = currentSeason?.episodes?.isNotEmpty() == true && 
                              currentSeason.episodes.any { it.id != "0" && it.title.isNotBlank() }
        
        if (hasValidEpisodes) {
            android.util.Log.d("TVDetailsViewModel", "Season $seasonNumber already has ${currentSeason?.episodes?.size} valid episodes, skipping load")
            return
        }
        
        android.util.Log.d("TVDetailsViewModel", "Season $seasonNumber validation passed:")
        android.util.Log.d("TVDetailsViewModel", "  - Current episodes: ${currentSeason?.episodes?.size ?: 0}")
        android.util.Log.d("TVDetailsViewModel", "  - Has valid episodes: $hasValidEpisodes")
        android.util.Log.d("TVDetailsViewModel", "  - Episode count claimed: ${currentSeason?.episodeCount ?: 0}")
        android.util.Log.d("TVDetailsViewModel", "  - Proceeding with API call")
        
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
                        val seasonResponse = result.data
                        if (seasonResponse.id != 0 && seasonResponse.episodes.isNotEmpty()) {
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
                                
                                android.util.Log.d("TVDetailsViewModel", "Season $seasonNumber loaded on demand with ${tvSeason.episodes.size} episodes")
                            }
                        } else {
                            android.util.Log.w("TVDetailsViewModel", "Season $seasonNumber response was invalid (id=${seasonResponse.id}, episodes=${seasonResponse.episodes.size})")
                        }
                    }
                    is com.rdwatch.androidtv.repository.base.Result.Error -> {
                        android.util.Log.e("TVDetailsViewModel", "Failed to load season $seasonNumber on demand: ${result.exception.message}")
                    }
                    is com.rdwatch.androidtv.repository.base.Result.Loading -> {
                        // This shouldn't happen with first{}, but handle it anyway
                        android.util.Log.w("TVDetailsViewModel", "Unexpected loading state received for season $seasonNumber")
                    }
                    null -> {
                        android.util.Log.w("TVDetailsViewModel", "Season $seasonNumber loading timed out after 30 seconds")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TVDetailsViewModel", "Error loading season $seasonNumber on demand: ${e.message}")
            } finally {
                activeSeasonRequests.remove(seasonNumber)
                onDemandSeasonJobs.remove(seasonNumber)
            }
        }
    }
    
    /**
     * Update the TV show with loaded seasons and select first episode
     */
    private fun updateTVShowWithSeasons(originalTvShow: TVShowContentDetail, seasons: List<TVSeason>) {
        android.util.Log.d("TVDetailsViewModel", "=== Updating TV Show with Seasons ===")
        seasons.forEach { season ->
            android.util.Log.d("TVDetailsViewModel", "Season ${season.seasonNumber}: ${season.name} - ${season.episodes.size} episodes loaded (claimed: ${season.episodeCount})")
        }
        
        val updatedTvShowDetail = originalTvShow.getTVShowDetail().copy(seasons = seasons)
        val updatedTvShow = originalTvShow.withTVShowDetail(updatedTvShowDetail)
        
        _tvShowState.value = updatedTvShow
        
        // Smart state synchronization: preserve user selections while updating with new data
        val currentSeason = _selectedSeason.value
        val currentEpisode = _selectedEpisode.value
        
        android.util.Log.d("TVDetailsViewModel", "=== State Synchronization Debug ===")
        android.util.Log.d("TVDetailsViewModel", "Current season: ${currentSeason?.seasonNumber} (${currentSeason?.episodes?.size} episodes)")
        android.util.Log.d("TVDetailsViewModel", "Current episode: ${currentEpisode?.episodeNumber}")
        android.util.Log.d("TVDetailsViewModel", "New seasons available: ${seasons.map { "${it.seasonNumber}(${it.episodes.size}ep)" }}")
        
        // Check if current season selection is still valid and has been updated with new data
        val updatedCurrentSeason = currentSeason?.let { selected ->
            seasons.find { it.seasonNumber == selected.seasonNumber }
        }
        
        // Determine the best season to select
        val seasonToSelect = when {
            // Case 1: Current season is valid and now has episodes (was just loaded)
            updatedCurrentSeason != null && currentSeason?.episodes?.isEmpty() == true && updatedCurrentSeason.episodes.isNotEmpty() -> {
                android.util.Log.d("TVDetailsViewModel", "Case 1: Current season ${updatedCurrentSeason.seasonNumber} now has episodes")
                updatedCurrentSeason
            }
            // Case 2: Current season is valid and still has the same or more episodes (no change needed)
            updatedCurrentSeason != null && updatedCurrentSeason.episodes.size >= (currentSeason?.episodes?.size ?: 0) -> {
                android.util.Log.d("TVDetailsViewModel", "Case 2: Current season ${updatedCurrentSeason.seasonNumber} data is stable")
                updatedCurrentSeason
            }
            // Case 3: No current season selected, select first available season with episodes
            currentSeason == null -> {
                val firstSeasonWithEpisodes = seasons.find { it.episodes.isNotEmpty() } ?: seasons.firstOrNull()
                android.util.Log.d("TVDetailsViewModel", "Case 3: No current season, selecting ${firstSeasonWithEpisodes?.seasonNumber}")
                firstSeasonWithEpisodes
            }
            // Case 4: Current season no longer exists, select first available
            updatedCurrentSeason == null -> {
                val fallbackSeason = seasons.firstOrNull()
                android.util.Log.d("TVDetailsViewModel", "Case 4: Current season ${currentSeason?.seasonNumber} no longer exists, selecting ${fallbackSeason?.seasonNumber}")
                fallbackSeason
            }
            // Default: keep current season
            else -> {
                android.util.Log.d("TVDetailsViewModel", "Default: Keeping current season ${currentSeason?.seasonNumber}")
                updatedCurrentSeason
            }
        }
        
        // Update season selection if it changed
        if (seasonToSelect != currentSeason) {
            android.util.Log.d("TVDetailsViewModel", "Updating season selection from ${currentSeason?.seasonNumber} to ${seasonToSelect?.seasonNumber}")
            _selectedSeason.value = seasonToSelect
            
            // Select appropriate episode for the new season
            val episodeToSelect = when {
                // Try to preserve current episode if it exists in the new season
                currentEpisode != null && seasonToSelect?.episodes?.any { it.episodeNumber == currentEpisode.episodeNumber } == true -> {
                    seasonToSelect.episodes.find { it.episodeNumber == currentEpisode.episodeNumber }
                }
                // Otherwise select first unwatched or first episode
                else -> {
                    seasonToSelect?.episodes?.find { !it.isWatched } ?: seasonToSelect?.episodes?.firstOrNull()
                }
            }
            
            selectEpisodeInternal(episodeToSelect)
            android.util.Log.d("TVDetailsViewModel", "Updated episode selection to: ${episodeToSelect?.title}")
            
            updateState { 
                copy(
                    tvShow = updatedTvShow,
                    currentSeason = seasonToSelect
                )
            }
        } else {
            // Season didn't change, but update the season data and preserve episode selection
            android.util.Log.d("TVDetailsViewModel", "Season selection unchanged, updating data only")
            
            // Check if current episode is still valid in the updated season data
            val updatedCurrentEpisode = currentEpisode?.let { selected ->
                seasonToSelect?.episodes?.find { it.episodeNumber == selected.episodeNumber }
            }
            
            if (updatedCurrentEpisode != null && updatedCurrentEpisode != currentEpisode) {
                android.util.Log.d("TVDetailsViewModel", "Updating episode data but preserving selection")
                selectEpisodeInternal(updatedCurrentEpisode)
            }
            
            updateState { copy(tvShow = updatedTvShow) }
        }
        
        // Don't auto-load sources on TV show load - wait for episode selection
        // This addresses the user's feedback about premature source loading
        android.util.Log.d("TVDetailsViewModel", "Updated TV show with ${seasons.size} seasons")
    }
    
    /**
     * Map TMDb season response to TVSeason
     */
    private fun mapTMDbSeasonResponseToTVSeason(seasonResponse: com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse): TVSeason {
        val episodes = mapTMDbEpisodesToTVEpisodes(seasonResponse.episodes, seasonResponse.seasonNumber)
        return TVSeason(
            id = seasonResponse.id.toString(),
            seasonNumber = seasonResponse.seasonNumber,
            name = seasonResponse.name,
            overview = seasonResponse.overview,
            posterPath = seasonResponse.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            airDate = seasonResponse.airDate,
            episodeCount = episodes.size, // Use actual loaded episodes count
            episodes = episodes,
            voteAverage = seasonResponse.voteAverage.toFloat()
        )
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
    
    /**
     * Map TMDb seasons to UI TVSeason models
     */
    private fun mapTMDbSeasonsToTVSeasons(tmdbSeasons: List<com.rdwatch.androidtv.network.models.tmdb.TMDbSeasonResponse>): List<TVSeason> {
        return tmdbSeasons.map { tmdbSeason ->
            TVSeason(
                id = tmdbSeason.id.toString(),
                seasonNumber = tmdbSeason.seasonNumber,
                name = tmdbSeason.name,
                overview = tmdbSeason.overview,
                posterPath = tmdbSeason.posterPath?.let { "https://image.tmdb.org/t/p/w500$it" },
                airDate = tmdbSeason.airDate,
                episodeCount = tmdbSeason.episodeCount,
                episodes = mapTMDbEpisodesToTVEpisodes(tmdbSeason.episodes, tmdbSeason.seasonNumber),
                voteAverage = tmdbSeason.voteAverage.toFloat()
            )
        }
    }
    
    /**
     * Map TMDb episodes to UI TVEpisode models
     */
    private fun mapTMDbEpisodesToTVEpisodes(tmdbEpisodes: List<com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse>, seasonNumber: Int): List<TVEpisode> {
        return tmdbEpisodes.map { tmdbEpisode ->
            mapTMDbEpisodeToTVEpisode(tmdbEpisode, seasonNumber)
        }
    }
    
    /**
     * Map a single TMDb episode to UI TVEpisode model
     */
    private fun mapTMDbEpisodeToTVEpisode(tmdbEpisode: com.rdwatch.androidtv.network.models.tmdb.TMDbEpisodeResponse, seasonNumber: Int? = null): TVEpisode {
        return TVEpisode(
            id = tmdbEpisode.id.toString(),
            seasonNumber = seasonNumber ?: tmdbEpisode.seasonNumber,
            episodeNumber = tmdbEpisode.episodeNumber,
            title = tmdbEpisode.name,
            description = tmdbEpisode.overview,
            thumbnailUrl = tmdbEpisode.stillPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            airDate = tmdbEpisode.airDate,
            runtime = tmdbEpisode.runtime,
            stillPath = tmdbEpisode.stillPath?.let { "https://image.tmdb.org/t/p/w500$it" },
            voteAverage = tmdbEpisode.voteAverage.toFloat(),
            voteCount = tmdbEpisode.voteCount,
            overview = tmdbEpisode.overview,
            isWatched = false,
            watchProgress = 0f,
            resumePosition = 0L,
            videoUrl = null // Will be populated later with streaming sources
        )
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