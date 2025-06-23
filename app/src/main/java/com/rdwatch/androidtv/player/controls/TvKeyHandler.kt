package com.rdwatch.androidtv.player.controls

import androidx.compose.ui.input.key.*

class TvKeyHandler {
    
    fun handleKeyEvent(
        keyEvent: KeyEvent,
        onPlayPause: () -> Unit,
        onSeekBackward: () -> Unit,
        onSeekForward: () -> Unit,
        onMenuToggle: () -> Unit,
        onShowControls: () -> Unit,
        onSpeedIncrease: () -> Unit = {},
        onSpeedDecrease: () -> Unit = {}
    ): Boolean {
        return when {
            keyEvent.type == KeyEventType.KeyDown -> {
                when (keyEvent.key) {
                    // Play/Pause controls
                    Key.MediaPlay, Key.MediaPause, Key.MediaPlayPause -> {
                        onPlayPause()
                        true
                    }
                    
                    Key.Spacebar -> {
                        onPlayPause()
                        true
                    }
                    
                    // Seek controls
                    Key.MediaRewind, Key.MediaPrevious -> {
                        onSeekBackward()
                        true
                    }
                    
                    Key.MediaFastForward, Key.MediaNext -> {
                        onSeekForward()
                        true
                    }
                    
                    // D-pad controls
                    Key.DirectionLeft -> {
                        onSeekBackward()
                        true
                    }
                    
                    Key.DirectionRight -> {
                        onSeekForward()
                        true
                    }
                    
                    Key.DirectionCenter, Key.Enter -> {
                        onPlayPause()
                        true
                    }
                    
                    // Menu and settings
                    Key.Menu -> {
                        onMenuToggle()
                        true
                    }
                    
                    // Show controls on any navigation
                    Key.DirectionUp, Key.DirectionDown -> {
                        onShowControls()
                        false // Don't consume the event, let focus handling work
                    }
                    
                    // Speed controls (optional)
                    Key.Plus, Key.NumPadAdd -> {
                        if (keyEvent.isCtrlPressed) {
                            onSpeedIncrease()
                            true
                        } else false
                    }
                    
                    Key.Minus, Key.NumPadSubtract -> {
                        if (keyEvent.isCtrlPressed) {
                            onSpeedDecrease()
                            true
                        } else false
                    }
                    
                    // Numbers for seeking (1-9 for 10%-90%)
                    Key.One -> {
                        // Seek to 10%
                        false // Will be handled by caller
                    }
                    Key.Two -> {
                        // Seek to 20%
                        false
                    }
                    Key.Three -> {
                        false
                    }
                    Key.Four -> {
                        false
                    }
                    Key.Five -> {
                        false
                    }
                    Key.Six -> {
                        false
                    }
                    Key.Seven -> {
                        false
                    }
                    Key.Eight -> {
                        false
                    }
                    Key.Nine -> {
                        false
                    }
                    Key.Zero -> {
                        // Seek to beginning
                        false
                    }
                    
                    else -> {
                        // Any other key shows controls
                        onShowControls()
                        false
                    }
                }
            }
            else -> false
        }
    }
    
    fun getSeekPercentage(key: Key): Float? {
        return when (key) {
            Key.Zero -> 0f
            Key.One -> 0.1f
            Key.Two -> 0.2f
            Key.Three -> 0.3f
            Key.Four -> 0.4f
            Key.Five -> 0.5f
            Key.Six -> 0.6f
            Key.Seven -> 0.7f
            Key.Eight -> 0.8f
            Key.Nine -> 0.9f
            else -> null
        }
    }
    
    companion object {
        const val SEEK_INCREMENT_MS = 10_000L // 10 seconds
        const val LONG_SEEK_INCREMENT_MS = 30_000L // 30 seconds
        
        val PLAYBACK_SPEEDS = floatArrayOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
        
        fun getNextSpeed(currentSpeed: Float, increment: Boolean): Float {
            val currentIndex = PLAYBACK_SPEEDS.indexOfFirst { kotlin.math.abs(it - currentSpeed) < 0.01f }
            val safeIndex = if (currentIndex == -1) 2 else currentIndex // Default to 1.0x if not found
            val nextIndex = if (increment) {
                (safeIndex + 1).coerceAtMost(PLAYBACK_SPEEDS.size - 1)
            } else {
                (safeIndex - 1).coerceAtLeast(0)
            }
            return PLAYBACK_SPEEDS.getOrElse(nextIndex) { currentSpeed }
        }
    }
}