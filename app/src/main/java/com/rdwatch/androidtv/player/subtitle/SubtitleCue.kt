package com.rdwatch.androidtv.player.subtitle

import androidx.media3.common.text.Cue
import android.text.Layout

/**
 * Represents a subtitle cue with timing information and formatting
 */
data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val position: Float? = null,
    val line: Float? = null,
    val size: Float? = null,
    val textAlignment: Int? = null,
    val verticalType: Int? = null,
    val windowColor: Int? = null,
    val textColor: Int? = null,
    val backgroundColor: Int? = null
) {
    /**
     * Convert to ExoPlayer's Cue format for rendering
     */
    fun toExoPlayerCue(): Cue {
        val builder = Cue.Builder()
            .setText(text)
            
        position?.let { builder.setPosition(it) }
        line?.let { builder.setLine(it, Cue.LINE_TYPE_FRACTION) }
        size?.let { builder.setSize(it) }
        textAlignment?.let { 
            val alignment = when (it) {
                1 -> Layout.Alignment.ALIGN_NORMAL    // TEXT_ALIGNMENT_START
                2 -> Layout.Alignment.ALIGN_CENTER    // TEXT_ALIGNMENT_CENTER
                3 -> Layout.Alignment.ALIGN_OPPOSITE  // TEXT_ALIGNMENT_END
                else -> Layout.Alignment.ALIGN_CENTER
            }
            builder.setTextAlignment(alignment)
        }
        verticalType?.let { builder.setVerticalType(it) }
        windowColor?.let { builder.setWindowColor(it) }
        
        return builder.build()
    }
    
    /**
     * Check if this cue should be displayed at the given time
     */
    fun isActiveAt(timeMs: Long): Boolean {
        return timeMs >= startTimeMs && timeMs <= endTimeMs
    }
    
    /**
     * Get duration of this cue in milliseconds
     */
    val durationMs: Long get() = endTimeMs - startTimeMs
}

/**
 * Collection of subtitle cues with metadata
 */
data class SubtitleTrackData(
    val cues: List<SubtitleCue>,
    val language: String? = null,
    val title: String? = null,
    val format: SubtitleFormat,
    val encoding: String = "UTF-8"
) {
    /**
     * Get all cues that should be active at a specific time
     */
    fun getCuesAt(timeMs: Long): List<SubtitleCue> {
        return cues.filter { it.isActiveAt(timeMs) }
    }
    
    /**
     * Get the next cue after the given time
     */
    fun getNextCueAfter(timeMs: Long): SubtitleCue? {
        return cues.firstOrNull { it.startTimeMs > timeMs }
    }
    
    /**
     * Get the previous cue before the given time
     */
    fun getPreviousCueBefore(timeMs: Long): SubtitleCue? {
        return cues.lastOrNull { it.endTimeMs < timeMs }
    }
    
    /**
     * Check if track has any cues
     */
    val isEmpty: Boolean get() = cues.isEmpty()
    
    /**
     * Get total duration covered by all cues
     */
    val totalDurationMs: Long get() {
        if (cues.isEmpty()) return 0L
        return (cues.maxOfOrNull { it.endTimeMs } ?: 0L) - (cues.minOfOrNull { it.startTimeMs } ?: 0L)
    }
}

/**
 * Supported subtitle formats
 */
enum class SubtitleFormat(val mimeType: String, val extensions: List<String>) {
    SRT("application/x-subrip", listOf("srt")),
    VTT("text/vtt", listOf("vtt", "webvtt")),
    SSA("text/x-ssa", listOf("ssa")),
    ASS("text/x-ass", listOf("ass")),
    TTML("application/ttml+xml", listOf("ttml", "xml")),
    UNKNOWN("text/plain", emptyList());
    
    companion object {
        fun fromMimeType(mimeType: String): SubtitleFormat {
            return values().find { it.mimeType.equals(mimeType, ignoreCase = true) } ?: UNKNOWN
        }
        
        fun fromFileName(fileName: String): SubtitleFormat {
            val extension = fileName.substringAfterLast('.', "").lowercase()
            return values().find { it.extensions.contains(extension) } ?: UNKNOWN
        }
        
        fun fromUrl(url: String): SubtitleFormat {
            return fromFileName(url)
        }
    }
}