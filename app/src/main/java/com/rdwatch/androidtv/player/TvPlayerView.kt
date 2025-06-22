package com.rdwatch.androidtv.player

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.ui.PlayerView
import androidx.media3.common.util.UnstableApi
import com.rdwatch.androidtv.player.controls.TvPlayerControls
import com.rdwatch.androidtv.player.controls.TvKeyHandler
import com.rdwatch.androidtv.player.subtitle.SubtitleManager
import kotlinx.coroutines.delay

@UnstableApi
@Composable
fun TvPlayerView(
    exoPlayerManager: ExoPlayerManager,
    subtitleManager: SubtitleManager,
    modifier: Modifier = Modifier,
    onMenuToggle: () -> Unit = {},
    autoHideDelay: Long = 5000L
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
        modifier = modifier
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
                    }
                )
            }
    ) {
        // ExoPlayer view with subtitle support
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayerManager.exoPlayer
                    useController = false // We use our custom controls
                    setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
                    setKeepContentOnPlayerReset(true)
                    
                    // Configure subtitle view
                    subtitleView?.let { subtitleView ->
                        subtitleManager.configureSubtitleView(subtitleView)
                    }
                    
                    setOnClickListener {
                        onUserInteraction()
                    }
                }
            },
            update = { playerView ->
                playerView.player = exoPlayerManager.exoPlayer
                
                // Update subtitle styling if changed
                playerView.subtitleView?.let { subtitleView ->
                    subtitleManager.configureSubtitleView(subtitleView)
                }
            },
            modifier = Modifier.fillMaxSize()
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
            }
        )
    }
}