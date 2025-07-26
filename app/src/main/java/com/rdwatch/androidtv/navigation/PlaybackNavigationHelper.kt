package com.rdwatch.androidtv.navigation

import androidx.media3.common.util.UnstableApi
import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.autoplay.AutoPlayEpisodeResult
import com.rdwatch.androidtv.autoplay.EpisodeAutoPlayService
import com.rdwatch.androidtv.data.repository.NextEpisodeResult
import com.rdwatch.androidtv.player.ExoPlayerManager
import com.rdwatch.androidtv.player.MediaMetadata
import com.rdwatch.androidtv.player.state.PlaybackStateRepository
import com.rdwatch.androidtv.util.DebugLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class PlaybackNavigationHelper
    @Inject
    constructor(
        private val exoPlayerManager: ExoPlayerManager,
        private val playbackStateRepository: PlaybackStateRepository,
        private val episodeAutoPlayService: EpisodeAutoPlayService,
    ) {
        // Coroutine scope for handling async auto-play operations
        private val autoPlayScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Navigate to video player with proper content setup
         */
        fun navigateToPlayer(
            movie: Movie,
            shouldResume: Boolean = true,
            startFromBeginning: Boolean = false,
        ) {
            val contentId = movie.videoUrl ?: movie.title ?: "unknown"

            if (startFromBeginning) {
                // Remove any existing progress to start fresh
                playbackStateRepository.removePlaybackPosition(contentId)
            }

            // Prepare media with content ID for progress tracking
            exoPlayerManager.prepareMedia(
                mediaUrl = movie.videoUrl ?: "",
                contentId = contentId,
                title = movie.title,
                metadata = createMediaMetadata(movie),
                shouldResume = shouldResume && !startFromBeginning,
            )

            // TODO: Navigate to player screen
            // This would typically use a navigation controller like Jetpack Navigation
            // For now, we'll just start playback
            exoPlayerManager.play()
        }

        /**
         * Navigate back from player with proper cleanup
         */
        fun navigateBackFromPlayer() {
            // Save current position before leaving
            saveCurrentProgress()

            // Pause playback but don't release player (in case user returns quickly)
            exoPlayerManager.pause()

            // TODO: Navigate back to previous screen
            // This would use the navigation controller to pop back stack
        }

        /**
         * Handle app going to background
         */
        fun handleAppBackground() {
            // Save current position
            saveCurrentProgress()

            // Pause playback to be respectful of system resources
            exoPlayerManager.pause()
        }

        /**
         * Handle app returning to foreground
         */
        fun handleAppForeground() {
            // Resume playback if it was playing before
            val playerState = exoPlayerManager.playerState.value
            if (playerState.isPlaying) {
                exoPlayerManager.play()
            }
        }

        /**
         * Handle app termination cleanup
         */
        fun handleAppTermination() {
            // Save final position
            saveCurrentProgress()

            // Mark content as completed if we're near the end
            val playerState = exoPlayerManager.playerState.value
            if (playerState.progressPercentage >= 0.9f && playerState.mediaUrl != null) {
                val contentId = getCurrentContentId()
                if (contentId != null) {
                    // Mark as completed by removing from continue watching
                    playbackStateRepository.removePlaybackPosition(contentId)
                }
            }

            // Release player resources
            exoPlayerManager.release()
        }

        /**
         * Resume content from Continue Watching
         */
        fun resumeContent(movie: Movie) {
            navigateToPlayer(
                movie = movie,
                shouldResume = true,
                startFromBeginning = false,
            )
        }

        /**
         * Start content from beginning
         */
        fun startContentFromBeginning(movie: Movie) {
            navigateToPlayer(
                movie = movie,
                shouldResume = false,
                startFromBeginning = true,
            )
        }

        /**
         * Show resume dialog for content with existing progress
         */
        fun checkAndShowResumeDialog(movie: Movie): Boolean {
            val contentId = movie.videoUrl ?: movie.title ?: "unknown"
            val playbackPosition = playbackStateRepository.getPlaybackPosition(contentId)

            return if (playbackPosition != null && playbackPosition.progressPercentage > 0.05f && playbackPosition.progressPercentage < 0.95f) {
                // Content has meaningful progress, show dialog
                // This would typically trigger a dialog in the UI
                true
            } else {
                // No meaningful progress, start normally
                navigateToPlayer(movie, shouldResume = false)
                false
            }
        }

        private fun saveCurrentProgress() {
            val playerState = exoPlayerManager.playerState.value
            val contentId = getCurrentContentId()

            if (contentId != null && playerState.currentPosition > 0 && playerState.duration > 0) {
                // The ExoPlayerManager already handles auto-saving, but we can force a save here
                // for critical navigation points
            }
        }

        private fun getCurrentContentId(): String? {
            return exoPlayerManager.playerState.value.mediaUrl
        }

        /**
         * Navigate to the next episode for auto-play functionality
         * This method is called from the AutoPlayController when countdown completes
         * Now integrates with EpisodeAutoPlayService for complete source resolution
         */
        fun navigateToNextEpisode(
            nextEpisode: NextEpisodeResult,
            showTitle: String,
            onSuccess: ((streamingUrl: String, episodeTitle: String) -> Unit)? = null,
            onError: ((errorMessage: String) -> Unit)? = null,
        ) {
            DebugLogger.d(
                "PlaybackNavigationHelper",
                "Starting auto-play navigation for ${nextEpisode.getDisplayText()}",
            )

            // Save current progress before switching episodes
            saveCurrentProgress()

            // Resolve episode sources asynchronously
            autoPlayScope.launch {
                try {
                    val result =
                        episodeAutoPlayService.resolveEpisodeForAutoPlay(
                            nextEpisode = nextEpisode,
                            showTitle = showTitle,
                        )

                    when (result) {
                        is AutoPlayEpisodeResult.Success -> {
                            val episodeTitle = "$showTitle - ${nextEpisode.getFormattedEpisodeId()}"
                            val fullTitle =
                                if (nextEpisode.episodeTitle != null) {
                                    "$episodeTitle: ${nextEpisode.episodeTitle}"
                                } else {
                                    episodeTitle
                                }

                            val episodeContentId = "${nextEpisode.tmdbShowId}:${nextEpisode.seasonNumber}:${nextEpisode.episodeNumber}"

                            // Create episode metadata for the player
                            val episodeMetadata =
                                MediaMetadata(
                                    title = fullTitle,
                                    description = "Auto-playing next episode",
                                    thumbnailUrl = null, // Would come from TMDb episode data
                                )

                            DebugLogger.i(
                                "PlaybackNavigationHelper",
                                "Successfully resolved episode URL for auto-play: $fullTitle",
                            )

                            // Prepare the media with ExoPlayer
                            exoPlayerManager.prepareMedia(
                                mediaUrl = result.streamingUrl,
                                contentId = episodeContentId,
                                title = fullTitle,
                                metadata = episodeMetadata,
                                shouldResume = false, // Always start new episodes from beginning
                            )

                            // Start playback
                            exoPlayerManager.play()

                            // Notify success
                            onSuccess?.invoke(result.streamingUrl, fullTitle)

                            DebugLogger.i(
                                "PlaybackNavigationHelper",
                                "Auto-play navigation completed successfully for ${nextEpisode.getDisplayText()}",
                            )
                        }

                        is AutoPlayEpisodeResult.NoSourcesFound -> {
                            val errorMsg = "No sources found for ${nextEpisode.getDisplayText()}"
                            DebugLogger.w("PlaybackNavigationHelper", errorMsg)
                            onError?.invoke(errorMsg)
                        }

                        is AutoPlayEpisodeResult.NoSuitableSource -> {
                            val errorMsg = "No suitable source found for auto-play (${result.availableSources.size} sources available)"
                            DebugLogger.w("PlaybackNavigationHelper", errorMsg)
                            onError?.invoke(errorMsg)
                        }

                        is AutoPlayEpisodeResult.UrlResolutionFailed -> {
                            val errorMsg = "Failed to resolve streaming URL for ${result.source.id}"
                            DebugLogger.w("PlaybackNavigationHelper", errorMsg)
                            onError?.invoke(errorMsg)
                        }

                        is AutoPlayEpisodeResult.ResolutionError -> {
                            val errorMsg = "Episode resolution error: ${result.errorMessage}"
                            DebugLogger.e("PlaybackNavigationHelper", errorMsg)
                            onError?.invoke(errorMsg)
                        }
                    }
                } catch (e: Exception) {
                    val errorMsg = "Unexpected error during auto-play navigation: ${e.message}"
                    DebugLogger.e("PlaybackNavigationHelper", errorMsg, e)
                    onError?.invoke(errorMsg)
                }
            }
        }

        /**
         * Check if auto-play navigation is supported for the current content
         */
        fun isAutoPlayNavigationSupported(): Boolean {
            // Check if we have the necessary navigation infrastructure in place
            return true // For now, always return true
        }

        private fun createMediaMetadata(movie: Movie): MediaMetadata {
            return MediaMetadata(
                title = movie.title,
                description = movie.description,
                thumbnailUrl = movie.cardImageUrl,
            )
        }
    }

/**
 * Navigation events for playback
 */
sealed class PlaybackNavigationEvent {
    object NavigateToPlayer : PlaybackNavigationEvent()

    object NavigateBack : PlaybackNavigationEvent()

    data class ShowResumeDialog(val movie: Movie) : PlaybackNavigationEvent()

    data class ShowError(val message: String) : PlaybackNavigationEvent()

    data class AutoPlayNextEpisode(val nextEpisode: NextEpisodeResult, val showTitle: String) : PlaybackNavigationEvent()

    data class ShowAutoPlayCountdown(val nextEpisode: NextEpisodeResult, val showTitle: String) : PlaybackNavigationEvent()

    object CancelAutoPlay : PlaybackNavigationEvent()
}

/**
 * Navigation state for playback
 */
data class PlaybackNavigationState(
    val isPlayerActive: Boolean = false,
    val currentMovie: Movie? = null,
    val showResumeDialog: Boolean = false,
    val navigationEvent: PlaybackNavigationEvent? = null,
)
