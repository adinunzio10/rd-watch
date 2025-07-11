package com.rdwatch.androidtv.player.subtitle

import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Handles synchronization of external subtitles with ExoPlayer timeline
 */
@UnstableApi
@Singleton
class SubtitleSynchronizer @Inject constructor() {
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var syncJob: Job? = null
    private var player: ExoPlayer? = null
    private var subtitleManager: SubtitleManager? = null
    
    // Synchronization settings
    private var syncIntervalMs: Long = 100L // Update every 100ms
    private var offsetMs: Long = 0L // Manual subtitle offset
    
    private val _synchronizationState = MutableStateFlow<SynchronizationState>(SynchronizationState.Stopped)
    val synchronizationState: StateFlow<SynchronizationState> = _synchronizationState.asStateFlow()
    
    private val _currentPosition = MutableStateFlow(0L)
    val currentPosition: StateFlow<Long> = _currentPosition.asStateFlow()
    
    /**
     * Initialize synchronizer with ExoPlayer and SubtitleManager
     */
    fun initialize(exoPlayer: ExoPlayer, subtitleManager: SubtitleManager) {
        this.player = exoPlayer
        this.subtitleManager = subtitleManager
        
        // Listen to player state changes
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) {
                    startSynchronization()
                } else {
                    pauseSynchronization()
                }
            }
            
            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int
            ) {
                // Handle seeks and other position jumps
                updateSubtitlePosition(newPosition.positionMs + offsetMs)
            }
        })
    }
    
    /**
     * Start subtitle synchronization
     */
    fun startSynchronization() {
        if (syncJob?.isActive == true) return
        
        _synchronizationState.value = SynchronizationState.Running
        
        syncJob = scope.launch {
            while (isActive) {
                val player = this@SubtitleSynchronizer.player
                val subtitleManager = this@SubtitleSynchronizer.subtitleManager
                
                if (player != null && subtitleManager != null) {
                    val currentPos = player.currentPosition
                    val adjustedPos = currentPos + offsetMs
                    
                    _currentPosition.value = adjustedPos
                    
                    // Update subtitles for current position
                    subtitleManager.updateSubtitlesForPosition(adjustedPos)
                    
                    // Check for subtitle timing adjustments
                    checkTimingAccuracy(currentPos, adjustedPos)
                }
                
                delay(syncIntervalMs)
            }
        }
    }
    
    /**
     * Pause subtitle synchronization
     */
    fun pauseSynchronization() {
        _synchronizationState.value = SynchronizationState.Paused
        syncJob?.cancel()
        syncJob = null
    }
    
    /**
     * Stop subtitle synchronization
     */
    fun stopSynchronization() {
        _synchronizationState.value = SynchronizationState.Stopped
        syncJob?.cancel()
        syncJob = null
        _currentPosition.value = 0L
    }
    
    /**
     * Set manual subtitle offset (positive = delay subtitles, negative = advance subtitles)
     */
    fun setSubtitleOffset(offsetMs: Long) {
        this.offsetMs = offsetMs
        
        // Immediately update current position if playing
        player?.let { player ->
            if (player.isPlaying) {
                updateSubtitlePosition(player.currentPosition + offsetMs)
            }
        }
    }
    
    /**
     * Get current subtitle offset
     */
    fun getSubtitleOffset(): Long = offsetMs
    
    /**
     * Set synchronization update interval
     */
    fun setSyncInterval(intervalMs: Long) {
        require(intervalMs > 0) { "Sync interval must be positive" }
        this.syncIntervalMs = intervalMs.coerceIn(50L, 1000L) // Limit between 50ms and 1000ms
    }
    
    /**
     * Manually sync to specific position
     */
    fun syncToPosition(positionMs: Long) {
        val adjustedPos = positionMs + offsetMs
        _currentPosition.value = adjustedPos
        subtitleManager?.updateSubtitlesForPosition(adjustedPos)
    }
    
    /**
     * Auto-adjust subtitle timing based on playback characteristics
     */
    private fun checkTimingAccuracy(playerPos: Long, adjustedPos: Long) {
        // This could be enhanced with more sophisticated timing analysis
        // For now, we just ensure the position is reasonable
        
        val timeDiff = kotlin.math.abs(playerPos - (adjustedPos - offsetMs))
        if (timeDiff > 1000L) { // More than 1 second difference
            // Force resync
            updateSubtitlePosition(playerPos + offsetMs)
        }
    }
    
    /**
     * Update subtitle position immediately
     */
    private fun updateSubtitlePosition(positionMs: Long) {
        _currentPosition.value = positionMs
        subtitleManager?.updateSubtitlesForPosition(positionMs)
    }
    
    /**
     * Get timing statistics for debugging
     */
    fun getTimingStats(): TimingStats {
        val player = this.player ?: return TimingStats()
        
        return TimingStats(
            playerPosition = player.currentPosition,
            adjustedPosition = player.currentPosition + offsetMs,
            offset = offsetMs,
            syncInterval = syncIntervalMs,
            isRunning = syncJob?.isActive == true
        )
    }
    
    /**
     * Reset synchronizer state
     */
    fun reset() {
        stopSynchronization()
        offsetMs = 0L
        syncIntervalMs = 100L
    }
    
    /**
     * Clean up resources
     */
    fun dispose() {
        stopSynchronization()
        player = null
        subtitleManager = null
    }
}

/**
 * State of subtitle synchronization
 */
sealed class SynchronizationState {
    object Stopped : SynchronizationState()
    object Running : SynchronizationState()
    object Paused : SynchronizationState()
}

/**
 * Timing statistics for debugging and monitoring
 */
data class TimingStats(
    val playerPosition: Long = 0L,
    val adjustedPosition: Long = 0L,
    val offset: Long = 0L,
    val syncInterval: Long = 100L,
    val isRunning: Boolean = false
) {
    val offsetSeconds: Double get() = offset / 1000.0
    val positionSeconds: Double get() = playerPosition / 1000.0
    val adjustedPositionSeconds: Double get() = adjustedPosition / 1000.0
}

/**
 * Configuration for subtitle synchronization
 */
data class SubtitleSyncConfig(
    val enabled: Boolean = true,
    val offsetMs: Long = 0L,
    val syncIntervalMs: Long = 100L,
    val autoAdjustTiming: Boolean = true,
    val maxTimingDriftMs: Long = 1000L
) {
    
    fun validate(): SubtitleSyncConfig {
        return copy(
            syncIntervalMs = syncIntervalMs.coerceIn(50L, 1000L),
            maxTimingDriftMs = maxTimingDriftMs.coerceIn(100L, 5000L)
        )
    }
}