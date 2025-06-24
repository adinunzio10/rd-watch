package com.rdwatch.androidtv.navigation

import com.rdwatch.androidtv.Movie
import com.rdwatch.androidtv.player.ExoPlayerManager
import com.rdwatch.androidtv.ui.viewmodel.PlaybackViewModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackNavigationHelper @Inject constructor(
    private val exoPlayerManager: ExoPlayerManager,
    private val playbackViewModel: PlaybackViewModel
) {
    
    /**
     * Navigate to video player with proper content setup
     */
    fun navigateToPlayer(
        movie: Movie,
        shouldResume: Boolean = true,
        startFromBeginning: Boolean = false
    ) {
        val contentId = movie.videoUrl ?: movie.title ?: "unknown"
        
        if (startFromBeginning) {
            // Remove any existing progress to start fresh
            playbackViewModel.removeFromContinueWatching(contentId)
        }
        
        // Prepare media with content ID for progress tracking
        exoPlayerManager.prepareMedia(
            mediaUrl = movie.videoUrl ?: "",
            contentId = contentId,
            title = movie.title,
            metadata = createMediaMetadata(movie),
            shouldResume = shouldResume && !startFromBeginning
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
                playbackViewModel.markAsCompleted(contentId)
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
            startFromBeginning = false
        )
    }
    
    /**
     * Start content from beginning
     */
    fun startContentFromBeginning(movie: Movie) {
        navigateToPlayer(
            movie = movie,
            shouldResume = false,
            startFromBeginning = true
        )
    }
    
    /**
     * Show resume dialog for content with existing progress
     */
    fun checkAndShowResumeDialog(movie: Movie): Boolean {
        val contentId = movie.videoUrl ?: movie.title ?: "unknown"
        val progress = playbackViewModel.getContentProgress(contentId)
        
        return if (progress > 0.05f && progress < 0.95f) {
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
    
    private fun createMediaMetadata(movie: Movie): MediaMetadata {
        return MediaMetadata(
            description = movie.description,
            thumbnailUrl = movie.cardImageUrl
        )
    }
}

/**
 * Media metadata for playback
 */
data class MediaMetadata(
    val description: String? = null,
    val thumbnailUrl: String? = null,
    val studio: String? = null,
    val genre: String? = null,
    val year: Int? = null,
    val duration: Long? = null
)

/**
 * Navigation events for playback
 */
sealed class PlaybackNavigationEvent {
    object NavigateToPlayer : PlaybackNavigationEvent()
    object NavigateBack : PlaybackNavigationEvent()
    data class ShowResumeDialog(val movie: Movie) : PlaybackNavigationEvent()
    data class ShowError(val message: String) : PlaybackNavigationEvent()
}

/**
 * Navigation state for playback
 */
data class PlaybackNavigationState(
    val isPlayerActive: Boolean = false,
    val currentMovie: Movie? = null,
    val showResumeDialog: Boolean = false,
    val navigationEvent: PlaybackNavigationEvent? = null
)