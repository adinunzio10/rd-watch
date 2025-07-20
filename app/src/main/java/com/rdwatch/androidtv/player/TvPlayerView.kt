package com.rdwatch.androidtv.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.PlayerView
import com.rdwatch.androidtv.player.controls.TvKeyHandler
import com.rdwatch.androidtv.player.controls.TvPlayerControls
import com.rdwatch.androidtv.player.subtitle.SubtitleManager
import com.rdwatch.androidtv.util.DebugLogger
import kotlinx.coroutines.delay

@UnstableApi
@Composable
fun TvPlayerView(
    exoPlayerManager: ExoPlayerManager,
    subtitleManager: SubtitleManager,
    modifier: Modifier = Modifier,
    onMenuToggle: () -> Unit = {},
    autoHideDelay: Long = 5000L,
) {
    val context = LocalContext.current
    val playerState by exoPlayerManager.playerState.collectAsState()
    val keyHandler = remember { TvKeyHandler() }

    var showControls by remember { mutableStateOf(true) }
    var lastInteractionTime by remember { mutableStateOf(System.currentTimeMillis()) }

    // Auto-hide controls after inactivity
    LaunchedEffect(lastInteractionTime, playerState.isPlaying) {
        if (playerState.isPlaying) {
            delay(autoHideDelay)
            if (System.currentTimeMillis() - lastInteractionTime >= autoHideDelay) {
                showControls = false
            }
        }
    }

    // Show controls on any user interaction
    fun onUserInteraction() {
        showControls = true
        lastInteractionTime = System.currentTimeMillis()
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .onKeyEvent { keyEvent ->
                    keyHandler.handleKeyEvent(
                        keyEvent = keyEvent,
                        onPlayPause = {
                            onUserInteraction()
                            if (playerState.isPlaying) {
                                exoPlayerManager.pause()
                            } else {
                                exoPlayerManager.play()
                            }
                        },
                        onSeekBackward = {
                            onUserInteraction()
                            exoPlayerManager.seekBackward()
                        },
                        onSeekForward = {
                            onUserInteraction()
                            exoPlayerManager.seekForward()
                        },
                        onMenuToggle = {
                            onUserInteraction()
                            onMenuToggle()
                        },
                        onShowControls = {
                            onUserInteraction()
                        },
                        onSpeedIncrease = {
                            onUserInteraction()
                            val newSpeed = TvKeyHandler.getNextSpeed(playerState.playbackSpeed, true)
                            exoPlayerManager.setPlaybackSpeed(newSpeed)
                        },
                        onSpeedDecrease = {
                            onUserInteraction()
                            val newSpeed = TvKeyHandler.getNextSpeed(playerState.playbackSpeed, false)
                            exoPlayerManager.setPlaybackSpeed(newSpeed)
                        },
                    )
                },
    ) {
        // ExoPlayer view with subtitle support
        AndroidView(
            factory = { ctx ->
                DebugLogger.d("TvPlayerView", "Creating PlayerView in factory")
                PlayerView(ctx).apply {
                    DebugLogger.d("TvPlayerView", "Configuring PlayerView")

                    // Set player and log the assignment
                    val currentPlayer = exoPlayerManager.exoPlayer
                    player = currentPlayer
                    DebugLogger.d("TvPlayerView", "PlayerView assigned ExoPlayer: ${currentPlayer.hashCode()}")

                    // Enhanced configuration for Android TV fullscreen mode
                    useController = false // We use our custom controls
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setKeepContentOnPlayerReset(true)

                    // Fullscreen configuration for edge-to-edge rendering
                    resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT
                    setUseArtwork(false)
                    defaultArtwork = null

                    // Configure for immersive fullscreen experience
                    setControllerShowTimeoutMs(5000) // Hide controls after 5 seconds
                    setControllerHideOnTouch(true)

                    // Ensure video surface fills entire screen
                    videoSurfaceView?.let { surfaceView ->
                        DebugLogger.d("TvPlayerView", "Configuring video surface view for fullscreen")
                        // Configure surface view for fullscreen rendering
                        surfaceView.layoutParams =
                            android.view.ViewGroup.LayoutParams(
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                            )
                    }

                    // Force layout to ensure surface is properly sized
                    DebugLogger.d("TvPlayerView", "PlayerView layout: width=$width, height=$height")

                    // Configure subtitle view
                    subtitleView?.let { subtitleView ->
                        subtitleManager.configureSubtitleView(subtitleView)
                        DebugLogger.d("TvPlayerView", "Subtitle view configured")
                    }

                    setOnClickListener {
                        onUserInteraction()
                    }

                    // Log player state when view is created
                    val playerState = exoPlayerManager.playerState.value
                    DebugLogger.d("TvPlayerView", "PlayerView created with state:")
                    DebugLogger.d("TvPlayerView", "  - Playback state: ${playerState.playbackState}")
                    DebugLogger.d("TvPlayerView", "  - Has video: ${playerState.hasVideo}")
                    DebugLogger.d("TvPlayerView", "  - Is playing: ${playerState.isPlaying}")
                }
            },
            update = { playerView ->
                DebugLogger.d("TvPlayerView", "Updating PlayerView")

                val currentPlayer = exoPlayerManager.exoPlayer
                if (playerView.player != currentPlayer) {
                    DebugLogger.d("TvPlayerView", "Updating PlayerView with new ExoPlayer: ${currentPlayer.hashCode()}")
                    playerView.player = currentPlayer
                } else {
                    DebugLogger.d("TvPlayerView", "PlayerView already has correct ExoPlayer instance")
                }

                // Update subtitle styling if changed
                playerView.subtitleView?.let { subtitleView ->
                    subtitleManager.configureSubtitleView(subtitleView)
                }

                // Log current player state during update
                val playerState = exoPlayerManager.playerState.value
                DebugLogger.d("TvPlayerView", "PlayerView update - current state:")
                DebugLogger.d("TvPlayerView", "  - Playback state: ${playerState.playbackState}")
                DebugLogger.d("TvPlayerView", "  - Has video: ${playerState.hasVideo}")
                DebugLogger.d("TvPlayerView", "  - Is playing: ${playerState.isPlaying}")

                // Check if video surface is available and ready
                playerView.videoSurfaceView?.let { surfaceView ->
                    DebugLogger.d("TvPlayerView", "Video surface view available: ${surfaceView.width}x${surfaceView.height}")
                } ?: DebugLogger.w("TvPlayerView", "No video surface view available")
            },
            modifier = Modifier.fillMaxSize(),
        )

        // Custom TV controls overlay
        TvPlayerControls(
            playerState = playerState,
            isVisible = showControls,
            onPlayPause = {
                onUserInteraction()
                if (playerState.isPlaying) {
                    exoPlayerManager.pause()
                } else {
                    exoPlayerManager.play()
                }
            },
            onSeekBackward = {
                onUserInteraction()
                exoPlayerManager.seekBackward()
            },
            onSeekForward = {
                onUserInteraction()
                exoPlayerManager.seekForward()
            },
            onSeek = { position ->
                onUserInteraction()
                exoPlayerManager.seekTo(position)
            },
            onSpeedChange = { speed ->
                onUserInteraction()
                exoPlayerManager.setPlaybackSpeed(speed)
            },
            onMenuToggle = {
                onUserInteraction()
                onMenuToggle()
            },
        )
    }
}
