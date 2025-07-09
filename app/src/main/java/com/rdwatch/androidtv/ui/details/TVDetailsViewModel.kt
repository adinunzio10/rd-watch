package com.rdwatch.androidtv.ui.details

import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.presentation.viewmodel.BaseViewModel
import com.rdwatch.androidtv.repository.RealDebridContentRepository
import com.rdwatch.androidtv.repository.base.Result
import com.rdwatch.androidtv.ui.common.UiState
import com.rdwatch.androidtv.ui.details.models.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * ViewModel for TV Details Screen - handles TV show details loading and episode management
 * Follows MVVM architecture with BaseViewModel pattern
 */
@HiltViewModel
class TVDetailsViewModel @Inject constructor(
    private val realDebridContentRepository: RealDebridContentRepository
) : BaseViewModel<TVDetailsUiState>() {
    
    private val _tvShowState = MutableStateFlow<TVShowContentDetail?>(null)
    val tvShowState: StateFlow<TVShowContentDetail?> = _tvShowState.asStateFlow()
    
    private val _selectedSeason = MutableStateFlow<TVSeason?>(null)
    val selectedSeason: StateFlow<TVSeason?> = _selectedSeason.asStateFlow()
    
    private val _selectedEpisode = MutableStateFlow<TVEpisode?>(null)
    val selectedEpisode: StateFlow<TVEpisode?> = _selectedEpisode.asStateFlow()
    
    private val _relatedShowsState = MutableStateFlow<UiState<List<TVShowContentDetail>>>(UiState.Loading)
    val relatedShowsState: StateFlow<UiState<List<TVShowContentDetail>>> = _relatedShowsState.asStateFlow()
    
    override fun createInitialState(): TVDetailsUiState {
        return TVDetailsUiState()
    }
    
    /**
     * Load TV show details and related content
     */
    fun loadTVShow(tvShowId: String) {
        viewModelScope.launch {
            updateState { copy(isLoading = true, error = null) }
            
            try {
                // For now, use the demo TV show content
                // In a real app, this would fetch from a repository
                val tvShowDetail = TVShowContentDetail.createDemo()
                
                _tvShowState.value = tvShowDetail
                
                // Select first season by default
                val firstSeason = tvShowDetail.getSeasons().firstOrNull()
                _selectedSeason.value = firstSeason
                
                // Select first episode of first season by default
                val firstEpisode = firstSeason?.episodes?.firstOrNull()
                _selectedEpisode.value = firstEpisode
                
                updateState { 
                    copy(
                        tvShow = tvShowDetail,
                        isLoading = false,
                        error = null,
                        isLoaded = true,
                        currentSeason = firstSeason,
                        currentEpisode = firstEpisode
                    )
                }
                
                // Load related shows
                loadRelatedShows(tvShowDetail)
                
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
        _selectedSeason.value = season
        
        // Select first episode of the season or next unwatched episode
        val nextEpisode = season.episodes.find { !it.isWatched } ?: season.episodes.firstOrNull()
        _selectedEpisode.value = nextEpisode
        
        updateState { 
            copy(
                currentSeason = season,
                currentEpisode = nextEpisode
            )
        }
    }
    
    /**
     * Select an episode
     */
    fun selectEpisode(episode: TVEpisode) {
        _selectedEpisode.value = episode
        updateState { copy(currentEpisode = episode) }
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
     * Load related TV shows
     */
    private fun loadRelatedShows(tvShow: TVShowContentDetail) {
        viewModelScope.launch {
            _relatedShowsState.value = UiState.Loading
            
            try {
                // For now, return empty list
                // In a real app, this would fetch related shows from repository
                val relatedShows = emptyList<TVShowContentDetail>()
                
                _relatedShowsState.value = UiState.Success(relatedShows)
                updateState { copy(relatedShows = relatedShows) }
                
            } catch (e: Exception) {
                _relatedShowsState.value = UiState.Error(
                    message = "Failed to load related shows: ${e.message}",
                    throwable = e
                )
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
     * Clear all data
     */
    fun clearData() {
        _tvShowState.value = null
        _selectedSeason.value = null
        _selectedEpisode.value = null
        _relatedShowsState.value = UiState.Loading
        
        updateState { createInitialState() }
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
    val isFromRealDebrid: Boolean = false
)