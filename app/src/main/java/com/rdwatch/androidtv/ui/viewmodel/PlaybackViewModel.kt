package com.rdwatch.androidtv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rdwatch.androidtv.data.entities.WatchProgressEntity
import com.rdwatch.androidtv.data.repository.PlaybackProgressRepository
import com.rdwatch.androidtv.player.ExoPlayerManager
import com.rdwatch.androidtv.player.PlayerState
import com.rdwatch.androidtv.player.state.PlaybackStateRepository
import com.rdwatch.androidtv.player.state.WatchStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.media3.common.util.UnstableApi

@UnstableApi
@HiltViewModel
class PlaybackViewModel @Inject constructor(
    private val exoPlayerManager: ExoPlayerManager,
    private val playbackStateRepository: PlaybackStateRepository,
    private val playbackProgressRepository: PlaybackProgressRepository
) : ViewModel() {
    
    // Expose player state from ExoPlayerManager
    val playerState: StateFlow<PlayerState> = exoPlayerManager.playerState
    
    private val _uiState = MutableStateFlow(PlaybackUiState())
    val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()
    
    // Default user ID for now - in a real app this would come from user context
    private val currentUserId: Long get() = 1L
    
    private val _inProgressContent = MutableStateFlow<List<WatchProgressEntity>>(emptyList())
    val inProgressContent: StateFlow<List<WatchProgressEntity>> = _inProgressContent.asStateFlow()
    
    private val _completedContent = MutableStateFlow<List<WatchProgressEntity>>(emptyList())
    val completedContent: StateFlow<List<WatchProgressEntity>> = _completedContent.asStateFlow()
    
    private val _watchStatistics = MutableStateFlow(WatchStatistics())
    val watchStatistics: StateFlow<WatchStatistics> = _watchStatistics.asStateFlow()
    
    init {
        loadWatchData()
    }
    
    // Playback Control Methods
    fun play() {
        exoPlayerManager.play()
    }
    
    fun pause() {
        exoPlayerManager.pause()
    }
    
    fun seekTo(positionMs: Long) {
        exoPlayerManager.seekTo(positionMs)
    }
    
    fun seekForward(incrementMs: Long = 10_000L) {
        exoPlayerManager.seekForward(incrementMs)
    }
    
    fun seekBackward(decrementMs: Long = 10_000L) {
        exoPlayerManager.seekBackward(decrementMs)
    }
    
    fun setPlaybackSpeed(speed: Float) {
        exoPlayerManager.setPlaybackSpeed(speed)
    }
    
    // Resume Dialog Methods
    fun dismissResumeDialog() {
        exoPlayerManager.dismissResumeDialog()
        _uiState.value = _uiState.value.copy(showResumeDialog = false)
    }
    
    fun resumeFromDialog() {
        exoPlayerManager.resumeFromDialog()
        _uiState.value = _uiState.value.copy(showResumeDialog = false)
    }
    
    fun restartFromBeginning() {
        exoPlayerManager.restartFromBeginning()
        _uiState.value = _uiState.value.copy(showResumeDialog = false)
    }
    
    // Content Management Methods
    fun markAsWatched(contentId: String? = null) {
        exoPlayerManager.markAsWatched(contentId)
        refreshWatchData()
    }
    
    fun removeFromContinueWatching(contentId: String) {
        viewModelScope.launch {
            playbackProgressRepository.removeProgress(currentUserId, contentId)
            refreshWatchData()
        }
    }
    
    fun markAsCompleted(contentId: String) {
        viewModelScope.launch {
            playbackProgressRepository.markAsCompleted(currentUserId, contentId)
            refreshWatchData()
        }
    }
    
    // Data Loading Methods
    private fun loadWatchData() {
        viewModelScope.launch {
            // Load in-progress content
            playbackProgressRepository.getInProgressContent(currentUserId).collect { progress ->
                _inProgressContent.value = progress
            }
        }
        
        viewModelScope.launch {
            // Load completed content
            playbackProgressRepository.getCompletedContent(currentUserId).collect { completed ->
                _completedContent.value = completed
            }
        }
        
        viewModelScope.launch {
            // Load watch statistics
            val stats = playbackStateRepository.getWatchStatistics()
            _watchStatistics.value = stats
        }
    }
    
    private fun refreshWatchData() {
        viewModelScope.launch {
            val stats = playbackStateRepository.getWatchStatistics()
            _watchStatistics.value = stats
        }
    }
    
    // Player State Observation
    fun observePlayerState() {
        viewModelScope.launch {
            playerState.collect { state ->
                _uiState.value = _uiState.value.copy(
                    showResumeDialog = state.shouldShowResumeDialog,
                    resumePosition = state.resumePosition,
                    formattedResumePosition = state.formattedResumePosition,
                    isLoading = state.playbackState.name == "BUFFERING",
                    hasError = !state.error.isNullOrEmpty(),
                    errorMessage = state.error
                )
            }
        }
    }
    
    // Progress Methods for UI
    fun getContentProgress(contentId: String): Float {
        val progress = _inProgressContent.value.find { it.contentId == contentId }
        return progress?.watchPercentage ?: 0f
    }
    
    fun isContentCompleted(contentId: String): Boolean {
        return _completedContent.value.any { it.contentId == contentId }
    }
    
    fun shouldShowContinueWatching(): Boolean {
        return _inProgressContent.value.isNotEmpty()
    }
    
    fun getContinueWatchingContent(limit: Int = 10): List<WatchProgressEntity> {
        return _inProgressContent.value.take(limit)
    }
    
    // Cleanup
    override fun onCleared() {
        super.onCleared()
        // ViewModel cleanup - ExoPlayerManager has its own lifecycle
    }
    
    // Utility Methods
    fun formatWatchTime(seconds: Long): String {
        val hours = seconds / 3600
        val minutes = (seconds % 3600) / 60
        val secs = seconds % 60
        
        return when {
            hours > 0 -> String.format("%dh %dm", hours, minutes)
            minutes > 0 -> String.format("%dm %ds", minutes, secs)
            else -> String.format("%ds", secs)
        }
    }
    
    fun formatPercentage(percentage: Float): String {
        return String.format("%.1f%%", percentage * 100)
    }
}

data class PlaybackUiState(
    val showResumeDialog: Boolean = false,
    val resumePosition: Long? = null,
    val formattedResumePosition: String = "",
    val isLoading: Boolean = false,
    val hasError: Boolean = false,
    val errorMessage: String? = null,
    val showControls: Boolean = true,
    val isFullscreen: Boolean = false
)