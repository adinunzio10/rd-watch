package com.rdwatch.androidtv.player.state

import android.content.Context
import android.content.SharedPreferences
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@UnstableApi
@Singleton
class PlaybackStateRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("playback_state", Context.MODE_PRIVATE)
    
    private val _currentSession = MutableStateFlow<PlaybackSession?>(null)
    val currentSession: StateFlow<PlaybackSession?> = _currentSession.asStateFlow()
    
    fun savePlaybackPosition(mediaUrl: String, position: Long, duration: Long) {
        val progress = if (duration > 0) (position.toFloat() / duration.toFloat()) else 0f
        
        // Only save if we're past 5% and before 95% of the content
        if (progress > 0.05f && progress < 0.95f) {
            prefs.edit()
                .putLong("${mediaUrl}_position", position)
                .putLong("${mediaUrl}_duration", duration)
                .putLong("${mediaUrl}_timestamp", System.currentTimeMillis())
                .apply()
        }
    }
    
    fun getPlaybackPosition(mediaUrl: String): PlaybackPosition? {
        val position = prefs.getLong("${mediaUrl}_position", -1L)
        val duration = prefs.getLong("${mediaUrl}_duration", -1L)
        val timestamp = prefs.getLong("${mediaUrl}_timestamp", -1L)
        
        return if (position != -1L && duration != -1L && timestamp != -1L) {
            PlaybackPosition(
                mediaUrl = mediaUrl,
                position = position,
                duration = duration,
                lastUpdated = timestamp
            )
        } else null
    }
    
    fun removePlaybackPosition(mediaUrl: String) {
        prefs.edit()
            .remove("${mediaUrl}_position")
            .remove("${mediaUrl}_duration")
            .remove("${mediaUrl}_timestamp")
            .apply()
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