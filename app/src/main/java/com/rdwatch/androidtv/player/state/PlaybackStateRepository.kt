package com.rdwatch.androidtv.player.state

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.util.UnstableApi
import com.rdwatch.androidtv.data.repository.PlaybackProgressRepository
import com.rdwatch.androidtv.data.entities.WatchProgressEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class PlaybackStateRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val playbackProgressRepository: PlaybackProgressRepository
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("playback_state", Context.MODE_PRIVATE)
    
    private val _currentSession = MutableStateFlow<PlaybackSession?>(null)
    val currentSession: StateFlow<PlaybackSession?> = _currentSession.asStateFlow()
    
    // Default user ID for now - in a real app this would come from user context
    private val currentUserId: Long get() = 1L
    
    fun savePlaybackPosition(mediaUrl: String, position: Long, duration: Long) {
        val progress = if (duration > 0) (position.toFloat() / duration.toFloat()) else 0f
        
        // Only save if we're past 5% and before 95% of the content
        if (progress > 0.05f && progress < 0.95f) {
            // Save to Room database
            runBlocking {
                try {
                    playbackProgressRepository.savePlaybackProgress(
                        userId = currentUserId,
                        contentId = mediaUrl,
                        progressSeconds = position / 1000, // Convert to seconds
                        durationSeconds = duration / 1000, // Convert to seconds
                        deviceInfo = getDeviceInfo()
                    )
                } catch (e: Exception) {
                    // Fallback to SharedPreferences for backward compatibility
                    prefs.edit()
                        .putLong("${mediaUrl}_position", position)
                        .putLong("${mediaUrl}_duration", duration)
                        .putLong("${mediaUrl}_timestamp", System.currentTimeMillis())
                        .apply()
                }
            }
        }
    }
    
    fun getPlaybackPosition(mediaUrl: String): PlaybackPosition? {
        return try {
            // Try to get from Room database first
            runBlocking {
                val progress = playbackProgressRepository.getPlaybackProgress(currentUserId, mediaUrl)
                progress?.let {
                    PlaybackPosition(
                        mediaUrl = it.contentId,
                        position = it.progressSeconds * 1000, // Convert to milliseconds
                        duration = it.durationSeconds * 1000, // Convert to milliseconds
                        lastUpdated = it.updatedAt.time
                    )
                }
            }
        } catch (e: Exception) {
            // Fallback to SharedPreferences
            val position = prefs.getLong("${mediaUrl}_position", -1L)
            val duration = prefs.getLong("${mediaUrl}_duration", -1L)
            val timestamp = prefs.getLong("${mediaUrl}_timestamp", -1L)
            
            if (position != -1L && duration != -1L && timestamp != -1L) {
                PlaybackPosition(
                    mediaUrl = mediaUrl,
                    position = position,
                    duration = duration,
                    lastUpdated = timestamp
                )
            } else null
        }
    }
    
    fun removePlaybackPosition(mediaUrl: String) {
        try {
            // Remove from Room database
            runBlocking {
                playbackProgressRepository.removeProgress(currentUserId, mediaUrl)
            }
        } catch (e: Exception) {
            // Fallback to SharedPreferences cleanup
            prefs.edit()
                .remove("${mediaUrl}_position")
                .remove("${mediaUrl}_duration")
                .remove("${mediaUrl}_timestamp")
                .apply()
        }
    }
    
    fun startPlaybackSession(session: PlaybackSession) {
        _currentSession.value = session
        saveSessionState(session)
    }
    
    fun updatePlaybackSession(update: PlaybackSession.() -> PlaybackSession) {
        val current = _currentSession.value
        if (current != null) {
            val updated = current.update()
            _currentSession.value = updated
            saveSessionState(updated)
        }
    }
    
    fun endPlaybackSession() {
        val session = _currentSession.value
        if (session != null) {
            // Save final position
            savePlaybackPosition(session.mediaUrl, session.currentPosition, session.duration)
            
            // Clear session
            clearSessionState()
            _currentSession.value = null
        }
    }
    
    fun getRecentSessions(): List<PlaybackSession> {
        val sessionsJson = prefs.getString("recent_sessions", "[]") ?: "[]"
        return try {
            // Parse JSON or return empty list - simplified for now
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    private fun saveSessionState(session: PlaybackSession) {
        prefs.edit()
            .putString("current_session_url", session.mediaUrl)
            .putString("current_session_title", session.title)
            .putLong("current_session_position", session.currentPosition)
            .putLong("current_session_duration", session.duration)
            .putLong("current_session_start", session.startTime)
            .putFloat("current_session_speed", session.playbackSpeed)
            .apply()
    }
    
    private fun clearSessionState() {
        prefs.edit()
            .remove("current_session_url")
            .remove("current_session_title")
            .remove("current_session_position")
            .remove("current_session_duration")
            .remove("current_session_start")
            .remove("current_session_speed")
            .apply()
    }
    
    fun restoreSession(): PlaybackSession? {
        val url = prefs.getString("current_session_url", null) ?: return null
        val title = prefs.getString("current_session_title", null)
        val position = prefs.getLong("current_session_position", 0L)
        val duration = prefs.getLong("current_session_duration", 0L)
        val startTime = prefs.getLong("current_session_start", System.currentTimeMillis())
        val speed = prefs.getFloat("current_session_speed", 1.0f)
        
        return PlaybackSession(
            mediaUrl = url,
            title = title,
            currentPosition = position,
            duration = duration,
            startTime = startTime,
            playbackSpeed = speed
        )
    }
    
    fun clearAllData() {
        prefs.edit().clear().apply()
    }
    
    private fun getDeviceInfo(): String {
        return "${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL} (Android ${android.os.Build.VERSION.RELEASE})"
    }
    
    // New methods for Room database integration
    suspend fun getInProgressContent(): List<WatchProgressEntity> {
        return try {
            playbackProgressRepository.getInProgressContent(currentUserId)
                .let { flow ->
                    // For now, we'll get the current state. In production, this should be observed
                    runBlocking { 
                        var result: List<WatchProgressEntity> = emptyList()
                        flow.collect { result = it }
                        result
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun getCompletedContent(): List<WatchProgressEntity> {
        return try {
            playbackProgressRepository.getCompletedContent(currentUserId)
                .let { flow ->
                    runBlocking { 
                        var result: List<WatchProgressEntity> = emptyList()
                        flow.collect { result = it }
                        result
                    }
                }
        } catch (e: Exception) {
            emptyList()
        }
    }
    
    suspend fun markAsCompleted(mediaUrl: String) {
        try {
            playbackProgressRepository.markAsCompleted(currentUserId, mediaUrl)
        } catch (e: Exception) {
            // Handle error - could log or fallback
        }
    }
    
    suspend fun getTotalWatchTimeSeconds(): Long {
        return try {
            playbackProgressRepository.getTotalWatchTimeSeconds(currentUserId)
        } catch (e: Exception) {
            0L
        }
    }
    
    suspend fun getWatchStatistics(): WatchStatistics {
        return try {
            val totalTimeSeconds = playbackProgressRepository.getTotalWatchTimeSeconds(currentUserId)
            val completedCount = playbackProgressRepository.getCompletedWatchCount(currentUserId)
            val averagePercentage = playbackProgressRepository.getAverageWatchPercentage(currentUserId)
            
            WatchStatistics(
                totalWatchTimeSeconds = totalTimeSeconds,
                completedContentCount = completedCount,
                averageWatchPercentage = averagePercentage
            )
        } catch (e: Exception) {
            WatchStatistics()
        }
    }
    
    suspend fun cleanupOldProgress() {
        try {
            playbackProgressRepository.cleanupMinimalProgress()
        } catch (e: Exception) {
            // Handle error - could log
        }
    }
}

data class WatchStatistics(
    val totalWatchTimeSeconds: Long = 0L,
    val completedContentCount: Int = 0,
    val averageWatchPercentage: Float = 0f
) {
    val totalWatchTimeHours: Float
        get() = totalWatchTimeSeconds / 3600f
}

data class PlaybackPosition(
    val mediaUrl: String,
    val position: Long,
    val duration: Long,
    val lastUpdated: Long
) {
    val progressPercentage: Float
        get() = if (duration > 0) (position.toFloat() / duration.toFloat()) else 0f
        
    val isRecent: Boolean
        get() = System.currentTimeMillis() - lastUpdated < 7 * 24 * 60 * 60 * 1000L // 7 days
}

data class PlaybackSession(
    val mediaUrl: String,
    val title: String? = null,
    val currentPosition: Long = 0L,
    val duration: Long = 0L,
    val startTime: Long = System.currentTimeMillis(),
    val playbackSpeed: Float = 1.0f,
    val subtitleTrack: String? = null,
    val audioTrack: String? = null,
    val quality: String? = null
) {
    val watchTime: Long
        get() = System.currentTimeMillis() - startTime
        
    val progressPercentage: Float
        get() = if (duration > 0) (currentPosition.toFloat() / duration.toFloat()) else 0f
}