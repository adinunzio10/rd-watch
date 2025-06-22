package com.rdwatch.androidtv.player

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import com.rdwatch.androidtv.player.state.PlaybackStateRepository
import com.rdwatch.androidtv.player.state.PlaybackSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class ExoPlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mediaSourceFactory: MediaSourceFactory,
    private val stateRepository: PlaybackStateRepository
) {
    private var _exoPlayer: ExoPlayer? = null
    val exoPlayer: ExoPlayer get() = _exoPlayer ?: createPlayer()
    
    private val trackSelector = DefaultTrackSelector(context)
    private val scope = CoroutineScope(Dispatchers.Main)
    
    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()
    
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            super.onIsPlayingChanged(isPlaying)
            updatePlayerState { copy(isPlaying = isPlaying) }
        }
        
        
        override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
            super.onPlayerError(error)
            updatePlayerState { copy(error = error.message) }
        }
        
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int
        ) {
            super.onPositionDiscontinuity(oldPosition, newPosition, reason)
            val position = newPosition.positionMs
            val duration = exoPlayer.duration.takeIf { it != androidx.media3.common.C.TIME_UNSET } ?: 0L
            
            updatePlayerState { 
                copy(
                    currentPosition = position,
                    duration = duration
                )
            }
            
            // Save position every 30 seconds or on significant jumps
            savePositionPeriodically(position, duration)
        }
        
        override fun onPlaybackStateChanged(playbackState: Int) {
            super.onPlaybackStateChanged(playbackState)
            val state = when (playbackState) {
                Player.STATE_IDLE -> PlaybackState.IDLE
                Player.STATE_BUFFERING -> PlaybackState.BUFFERING
                Player.STATE_READY -> PlaybackState.READY
                Player.STATE_ENDED -> {
                    handlePlaybackEnded()
                    PlaybackState.ENDED
                }
                else -> PlaybackState.IDLE
            }
            
            updatePlayerState { copy(playbackState = state) }
        }
    }
    
    private fun createPlayer(): ExoPlayer {
        return ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
            .also { player ->
                player.addListener(playerListener)
                _exoPlayer = player
            }
    }
    
    fun prepareMedia(mediaUrl: String, title: String? = null, metadata: MediaMetadata? = null) {
        val mediaItem = MediaItem.Builder()
            .setUri(mediaUrl)
            .apply { 
                title?.let { 
                    setMediaMetadata(
                        androidx.media3.common.MediaMetadata.Builder()
                            .setTitle(it)
                            .apply { 
                                metadata?.description?.let { setDescription(it) }
                                metadata?.thumbnailUrl?.let { setArtworkUri(android.net.Uri.parse(it)) }
                            }
                            .build()
                    ) 
                } 
            }
            .build()
        
        // Create appropriate media source based on format
        val mediaSource = mediaSourceFactory.createMediaSource(mediaItem)
        exoPlayer.setMediaSource(mediaSource)
        exoPlayer.prepare()
        
        updatePlayerState { 
            copy(
                mediaUrl = mediaUrl,
                title = title,
                error = null
            )
        }
    }
    
    fun isFormatSupported(url: String): Boolean {
        return mediaSourceFactory.isFormatSupported(url)
    }
    
    fun getSupportedFormats(): List<String> {
        return mediaSourceFactory.getSupportedFormats()
    }
    
    fun play() {
        exoPlayer.play()
    }
    
    fun pause() {
        exoPlayer.pause()
    }
    
    fun seekTo(positionMs: Long) {
        exoPlayer.seekTo(positionMs)
    }
    
    fun seekForward(incrementMs: Long = 10_000L) {
        val newPosition = (exoPlayer.currentPosition + incrementMs).coerceAtMost(exoPlayer.duration)
        exoPlayer.seekTo(newPosition)
    }
    
    fun seekBackward(decrementMs: Long = 10_000L) {
        val newPosition = (exoPlayer.currentPosition - decrementMs).coerceAtLeast(0L)
        exoPlayer.seekTo(newPosition)
    }
    
    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        updatePlayerState { copy(playbackSpeed = speed) }
    }
    
    fun release() {
        _exoPlayer?.removeListener(playerListener)
        _exoPlayer?.release()
        _exoPlayer = null
        _playerState.value = PlayerState()
    }
    
    private fun updatePlayerState(update: PlayerState.() -> PlayerState) {
        _playerState.value = _playerState.value.update()
    }
    
    private var lastSavedPosition = 0L
    private fun savePositionPeriodically(position: Long, duration: Long) {
        // Save position every 30 seconds
        if (position - lastSavedPosition > 30_000L) {
            val currentState = _playerState.value
            currentState.mediaUrl?.let { url ->
                scope.launch {
                    stateRepository.savePlaybackPosition(url, position, duration)
                }
            }
            lastSavedPosition = position
        }
    }
    
    private fun handlePlaybackEnded() {
        scope.launch {
            stateRepository.endPlaybackSession()
        }
    }
    
    fun startPlaybackSession(mediaUrl: String, title: String?) {
        val session = PlaybackSession(
            mediaUrl = mediaUrl,
            title = title,
            currentPosition = 0L,
            duration = 0L
        )
        
        scope.launch {
            stateRepository.startPlaybackSession(session)
        }
    }
    
    fun resumeFromSavedPosition(mediaUrl: String): Long? {
        return stateRepository.getPlaybackPosition(mediaUrl)?.position
    }
    
    fun markAsWatched(mediaUrl: String) {
        scope.launch {
            stateRepository.removePlaybackPosition(mediaUrl)
        }
    }
}

data class PlayerState(
    val isPlaying: Boolean = false,
    val playbackState: PlaybackState = PlaybackState.IDLE,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val mediaUrl: String? = null,
    val title: String? = null,
    val playbackSpeed: Float = 1.0f,
    val error: String? = null
)

enum class PlaybackState {
    IDLE, BUFFERING, READY, ENDED
}