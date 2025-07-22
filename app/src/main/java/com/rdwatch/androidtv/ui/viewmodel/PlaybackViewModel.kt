package com.rdwatch.androidtv.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.rdwatch.androidtv.auth.UserSessionManager
import com.rdwatch.androidtv.data.entities.WatchProgressEntity
import com.rdwatch.androidtv.data.repository.PlaybackProgressRepository
import com.rdwatch.androidtv.media.MediaUrlResolver
import com.rdwatch.androidtv.player.ExoPlayerManager
import com.rdwatch.androidtv.player.PlaybackState
import com.rdwatch.androidtv.player.PlayerState
import com.rdwatch.androidtv.player.state.PlaybackStateRepository
import com.rdwatch.androidtv.player.state.WatchStatistics
import com.rdwatch.androidtv.repository.base.Result
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Media ready state to track preparation status
sealed class MediaReadyState {
    object Idle : MediaReadyState()

    object Preparing : MediaReadyState()

    object Ready : MediaReadyState()

    data class Error(val message: String) : MediaReadyState()
}

@UnstableApi
@HiltViewModel
class PlaybackViewModel
    @Inject
    constructor(
        private val exoPlayerManager: ExoPlayerManager,
        private val playbackStateRepository: PlaybackStateRepository,
        private val playbackProgressRepository: PlaybackProgressRepository,
        private val mediaUrlResolver: MediaUrlResolver,
        private val userSessionManager: UserSessionManager,
    ) : ViewModel() {
        // Expose player state from ExoPlayerManager
        val playerState: StateFlow<PlayerState> = exoPlayerManager.playerState

        private val _uiState = MutableStateFlow(PlaybackUiState())
        val uiState: StateFlow<PlaybackUiState> = _uiState.asStateFlow()

        // Media ready state flow to track preparation status
        private val _mediaReadyState = MutableStateFlow<MediaReadyState>(MediaReadyState.Idle)
        val mediaReadyState: StateFlow<MediaReadyState> = _mediaReadyState.asStateFlow()

        // Get current user ID from session manager
        private suspend fun getCurrentUserId(): Long = userSessionManager.getCurrentUserId()

        private val _inProgressContent = MutableStateFlow<List<WatchProgressEntity>>(emptyList())
        val inProgressContent: StateFlow<List<WatchProgressEntity>> = _inProgressContent.asStateFlow()

        private val _completedContent = MutableStateFlow<List<WatchProgressEntity>>(emptyList())
        val completedContent: StateFlow<List<WatchProgressEntity>> = _completedContent.asStateFlow()

        private val _watchStatistics = MutableStateFlow(WatchStatistics())
        val watchStatistics: StateFlow<WatchStatistics> = _watchStatistics.asStateFlow()

        init {
            loadWatchData()
        }

        /**
         * Resolve URLs that need processing before being sent to ExoPlayer
         * Uses MediaUrlResolver with caching for improved performance
         */
        private suspend fun resolvePlayableUrl(url: String): String {
            return when (val result = mediaUrlResolver.resolveUrl(url)) {
                is Result.Success -> result.data
                is Result.Error -> {
                    android.util.Log.e("PlaybackViewModel", "Error resolving URL with cache: ${result.exception.message}")
                    url // Return original URL as fallback
                }
                is Result.Loading -> url // Should not happen with current implementation but handle gracefully
            }
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

        // Episode Playback Methods
        fun startEpisodePlayback(
            tvShow: com.rdwatch.androidtv.ui.details.models.TVShowContentDetail,
            episode: com.rdwatch.androidtv.ui.details.models.TVEpisode,
            source: com.rdwatch.androidtv.ui.details.models.StreamingSource,
        ) {
            viewModelScope.launch {
                try {
                    // Create a unique content ID for the episode
                    val episodeContentId = "${tvShow.id}:${episode.seasonNumber}:${episode.episodeNumber}"

                    // Log the playback attempt
                    android.util.Log.d(
                        "PlaybackViewModel",
                        "Starting episode playback: ${episode.title} from ${source.provider.displayName}",
                    )

                    // Resolve and prepare playback with the source URL
                    val resolvedUrl = resolvePlayableUrl(source.url)
                    val episodeTitle = "${tvShow.title} - S${episode.seasonNumber}E${episode.episodeNumber}: ${episode.title}"
                    exoPlayerManager.prepareMedia(
                        mediaUrl = resolvedUrl,
                        contentId = episodeContentId,
                        title = episodeTitle,
                        shouldResume = true,
                    )

                    // Start playback
                    exoPlayerManager.play()

                    android.util.Log.d("PlaybackViewModel", "Episode playback started successfully")
                } catch (e: Exception) {
                    android.util.Log.e("PlaybackViewModel", "Failed to start episode playback: ${e.message}")
                    // Update UI state to show error
                    _uiState.value =
                        _uiState.value.copy(
                            hasError = true,
                            errorMessage = "Failed to start playback: ${e.message}",
                        )
                }
            }
        }

        /**
         * Start episode playback with advanced source metadata
         */
        fun startEpisodePlaybackWithSource(
            tvShow: com.rdwatch.androidtv.ui.details.models.TVShowContentDetail,
            episode: com.rdwatch.androidtv.ui.details.models.TVEpisode,
            source: com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata,
            onNavigateToVideoPlayer: (videoUrl: String, title: String) -> Unit = { _, _ -> },
        ) {
            viewModelScope.launch {
                try {
                    // Set state to preparing
                    _mediaReadyState.value = MediaReadyState.Preparing

                    // Create a unique content ID for the episode
                    val episodeContentId = "${tvShow.id}:${episode.seasonNumber}:${episode.episodeNumber}"

                    // Extract URL from metadata (stored in metadata map)
                    val sourceUrl = source.metadata["originalUrl"] ?: ""

                    // Log the playback attempt with enhanced source info
                    android.util.Log.d("PlaybackViewModel", "Starting episode playback with advanced source:")
                    android.util.Log.d("PlaybackViewModel", "  Episode: ${episode.title}")
                    android.util.Log.d("PlaybackViewModel", "  Provider: ${source.provider.name}")
                    android.util.Log.d("PlaybackViewModel", "  Quality: ${source.quality.resolution}")
                    android.util.Log.d("PlaybackViewModel", "  Health Score: ${source.health.seeders}/${source.health.leechers}")

                    if (sourceUrl.isBlank()) {
                        val errorMessage = "Source URL is missing or empty"
                        _mediaReadyState.value = MediaReadyState.Error(errorMessage)
                        throw IllegalArgumentException(errorMessage)
                    }

                    // Log the actual URL being used
                    android.util.Log.d("PlaybackViewModel", "  URL: $sourceUrl")

                    // Resolve URL using MediaUrlResolver with caching
                    android.util.Log.d("PlaybackViewModel", "  Resolving URL with caching...")
                    val resolvedUrl = resolvePlayableUrl(sourceUrl)

                    // Prepare and start playback with the resolved URL
                    val episodeTitle = "${tvShow.title} - S${episode.seasonNumber}E${episode.episodeNumber}: ${episode.title} [${source.quality.resolution}]"
                    exoPlayerManager.prepareMedia(
                        mediaUrl = resolvedUrl,
                        contentId = "${tvShow.id}:${episode.seasonNumber}:${episode.episodeNumber}",
                        title = episodeTitle,
                        shouldResume = true,
                    )

                    // Start playback
                    exoPlayerManager.play()

                    // Create a coroutine job to monitor media preparation
                    val mediaPreparationJob =
                        launch {
                            var timeoutCounter = 0
                            while (timeoutCounter < 150) { // 15 seconds timeout (100ms * 150)
                                val currentState = playerState.value

                                // Check for errors first
                                if (currentState.error != null) {
                                    android.util.Log.e("PlaybackViewModel", "Media preparation error: ${currentState.error}")
                                    _mediaReadyState.value = MediaReadyState.Error(currentState.error)
                                    return@launch
                                }

                                // Check if media is ready
                                if (currentState.hasVideo &&
                                    (
                                        currentState.playbackState == PlaybackState.READY ||
                                            currentState.playbackState == PlaybackState.BUFFERING
                                    )
                                ) {
                                    android.util.Log.d("PlaybackViewModel", "Media is ready, navigating to video player")
                                    _mediaReadyState.value = MediaReadyState.Ready

                                    // Navigate to video player only when media is ready
                                    onNavigateToVideoPlayer(sourceUrl, episodeTitle)
                                    return@launch
                                }

                                // Wait a bit before checking again
                                delay(100)
                                timeoutCounter++
                            }

                            // Timeout reached
                            android.util.Log.e("PlaybackViewModel", "Media preparation timeout")
                            _mediaReadyState.value = MediaReadyState.Error("Media preparation timed out")
                        }

                    android.util.Log.d("PlaybackViewModel", "Waiting for media preparation...")
                } catch (e: Exception) {
                    android.util.Log.e("PlaybackViewModel", "Failed to start advanced episode playback: ${e.message}")
                    _mediaReadyState.value = MediaReadyState.Error(e.message ?: "Unknown error")
                    // Update UI state to show error
                    _uiState.value =
                        _uiState.value.copy(
                            hasError = true,
                            errorMessage = "Failed to start playback: ${e.message}",
                        )
                }
            }
        }

        /**
         * Start movie playback with advanced source metadata
         */
        fun startMoviePlaybackWithSource(
            movie: com.rdwatch.androidtv.Movie,
            source: com.rdwatch.androidtv.ui.details.models.advanced.SourceMetadata,
        ) {
            viewModelScope.launch {
                try {
                    // Extract URL from metadata (stored in metadata map)
                    val sourceUrl = source.metadata["originalUrl"] ?: ""

                    // Log the playback attempt with enhanced source info
                    android.util.Log.d("PlaybackViewModel", "Starting movie playback with advanced source:")
                    android.util.Log.d("PlaybackViewModel", "  Movie: ${movie.title}")
                    android.util.Log.d("PlaybackViewModel", "  Provider: ${source.provider.name}")
                    android.util.Log.d("PlaybackViewModel", "  Quality: ${source.quality.resolution}")
                    android.util.Log.d("PlaybackViewModel", "  Health Score: ${source.health.seeders}/${source.health.leechers}")

                    if (sourceUrl.isBlank()) {
                        throw IllegalArgumentException("Source URL is missing or empty")
                    }

                    // Log the actual URL being used
                    android.util.Log.d("PlaybackViewModel", "  URL: $sourceUrl")

                    // Resolve URL using MediaUrlResolver with caching
                    android.util.Log.d("PlaybackViewModel", "  Resolving URL with caching...")
                    val resolvedUrl = resolvePlayableUrl(sourceUrl)

                    // Prepare and start playback with the resolved URL
                    val movieTitle = "${movie.title} [${source.quality.resolution}]"
                    exoPlayerManager.prepareMedia(
                        mediaUrl = resolvedUrl,
                        contentId = movie.id?.toString() ?: movie.title ?: "unknown",
                        title = movieTitle,
                        shouldResume = true,
                    )

                    // Start playback
                    exoPlayerManager.play()

                    android.util.Log.d("PlaybackViewModel", "Advanced movie playback started successfully")
                } catch (e: Exception) {
                    android.util.Log.e("PlaybackViewModel", "Failed to start advanced movie playback: ${e.message}")
                    // Update UI state to show error
                    _uiState.value =
                        _uiState.value.copy(
                            hasError = true,
                            errorMessage = "Failed to start playback: ${e.message}",
                        )
                }
            }
        }

        // Content Management Methods
        fun markAsWatched(contentId: String? = null) {
            exoPlayerManager.markAsWatched(contentId)
            refreshWatchData()
        }

        fun removeFromContinueWatching(contentId: String) {
            viewModelScope.launch {
                val userId = getCurrentUserId()
                playbackProgressRepository.removeProgress(userId, contentId)
                refreshWatchData()
            }
        }

        fun markAsCompleted(contentId: String) {
            viewModelScope.launch {
                val userId = getCurrentUserId()
                playbackProgressRepository.markAsCompleted(userId, contentId)
                refreshWatchData()
            }
        }

        // Data Loading Methods
        private fun loadWatchData() {
            viewModelScope.launch {
                val userId = getCurrentUserId()
                // Load in-progress content
                playbackProgressRepository.getInProgressContent(userId).collect { progress ->
                    _inProgressContent.value = progress
                }
            }

            viewModelScope.launch {
                val userId = getCurrentUserId()
                // Load completed content
                playbackProgressRepository.getCompletedContent(userId).collect { completed ->
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

        // Reset media ready state
        fun resetMediaReadyState() {
            _mediaReadyState.value = MediaReadyState.Idle
        }

        // Player State Observation
        fun observePlayerState() {
            viewModelScope.launch {
                playerState.collect { state ->
                    _uiState.value =
                        _uiState.value.copy(
                            showResumeDialog = state.shouldShowResumeDialog,
                            resumePosition = state.resumePosition,
                            formattedResumePosition = state.formattedResumePosition,
                            isLoading = state.playbackState.name == "BUFFERING",
                            hasError = !state.error.isNullOrEmpty(),
                            errorMessage = state.error,
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

        /**
         * Set up episode progress callback to connect PlaybackStateRepository with AutoPlayController
         */
        fun setupEpisodeProgressCallback(callback: ((com.rdwatch.androidtv.player.state.EpisodeMetadata, Long, Long, String?) -> Unit)?) {
            playbackStateRepository.setEpisodeProgressCallback(callback)
        }

        /**
         * Prepare and start playback for the next episode in auto-play sequence
         * This method is called by the AutoPlayController when countdown completes
         */
        suspend fun prepareNextEpisodePlayback(
            tmdbShowId: Int,
            seasonNumber: Int,
            episodeNumber: Int,
            showTitle: String,
        ) {
            try {
                _mediaReadyState.value = MediaReadyState.Preparing

                // For now, we'll create a placeholder URL structure that the actual
                // navigation system will handle. In a complete implementation, this
                // would integrate with the TMDb API to get episode details and sources.
                val episodeContentId = "$tmdbShowId:$seasonNumber:$episodeNumber"
                val episodeTitle = "$showTitle - S${seasonNumber.toString().padStart(2, '0')}E${episodeNumber.toString().padStart(2, '0')}"

                // This is a placeholder - in actual implementation, you would:
                // 1. Fetch episode details from TMDb
                // 2. Get available sources
                // 3. Select best source
                // 4. Resolve the playable URL
                // For now, we'll just set the state to ready and let navigation handle it

                _mediaReadyState.value = MediaReadyState.Ready

                android.util.Log.i("PlaybackViewModel", "Prepared next episode for auto-play: $episodeTitle")
            } catch (e: Exception) {
                android.util.Log.e("PlaybackViewModel", "Error preparing next episode playback", e)
                _mediaReadyState.value = MediaReadyState.Error("Failed to prepare next episode: ${e.message}")
            }
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
    val isFullscreen: Boolean = false,
)
