package com.rdwatch.androidtv.player.subtitle.parser

import com.rdwatch.androidtv.player.subtitle.SubtitleFormat
import com.rdwatch.androidtv.player.subtitle.SubtitleTrackData
import java.io.InputStream

/**
 * Base interface for subtitle parsers
 */
interface SubtitleParser {
    /**
     * Parse subtitle content from string
     */
    suspend fun parse(
        content: String,
        encoding: String = "UTF-8",
    ): SubtitleTrackData

    /**
     * Parse subtitle content from InputStream
     */
    suspend fun parse(
        inputStream: InputStream,
        encoding: String = "UTF-8",
    ): SubtitleTrackData

    /**
     * Get supported subtitle formats
     */
    fun getSupportedFormats(): List<SubtitleFormat>

    /**
     * Check if parser supports the given format
     */
    fun supportsFormat(format: SubtitleFormat): Boolean
}

/**
 * Exception thrown when subtitle parsing fails
 */
class SubtitleParsingException(
    message: String,
    cause: Throwable? = null,
    val format: SubtitleFormat? = null,
    val lineNumber: Int? = null,
) : Exception(message, cause)

/**
 * Result of subtitle parsing operation
 */
sealed class SubtitleParseResult {
    data class Success(val trackData: SubtitleTrackData) : SubtitleParseResult()

    data class Error(val exception: SubtitleParsingException) : SubtitleParseResult()

    fun getOrNull(): SubtitleTrackData? =
        when (this) {
            is Success -> trackData
            is Error -> null
        }

    fun getOrThrow(): SubtitleTrackData =
        when (this) {
            is Success -> trackData
            is Error -> throw exception
        }
}

/**
 * Utility functions for subtitle parsing
 */
object SubtitleParsingUtils {
    /**
     * Parse time string in format HH:MM:SS,mmm or HH:MM:SS.mmm
     */
    fun parseTimeString(timeString: String): Long {
        val cleanTime = timeString.trim()

        // Handle different time formats
        val timeRegex =
            when {
                cleanTime.contains(',') -> """(\d{1,2}):(\d{2}):(\d{2}),(\d{3})""".toRegex()
                cleanTime.contains('.') && cleanTime.count { it == '.' } == 1 ->
                    """(\d{1,2}):(\d{2}):(\d{2})\.(\d{3})""".toRegex()
                else -> """(\d{1,2}):(\d{2}):(\d{2})""".toRegex()
            }

        val matchResult =
            timeRegex.find(cleanTime)
                ?: throw SubtitleParsingException("Invalid time format: $timeString")

        val hours = matchResult.groupValues[1].toInt()
        val minutes = matchResult.groupValues[2].toInt()
        val seconds = matchResult.groupValues[3].toInt()
        val milliseconds =
            if (matchResult.groupValues.size > 4) {
                matchResult.groupValues[4].toInt()
            } else {
                0
            }

        return (hours * 3600 + minutes * 60 + seconds) * 1000L + milliseconds
    }

    /**
     * Clean subtitle text by removing formatting tags
     */
    fun cleanText(text: String): String {
        return text
            .replace("""<[^>]*>""".toRegex(), "") // Remove HTML tags
            .replace("""\{[^}]*\}""".toRegex(), "") // Remove SSA/ASS tags
            .replace("\\n", "\n") // Convert literal \n to newlines
            .replace("\\N", "\n") // Convert literal \N to newlines
            .trim()
    }

    /**
     * Extract style information from text
     */
    fun extractStyleInfo(text: String): Pair<String, Map<String, String>> {
        val styleMap = mutableMapOf<String, String>()
        var cleanText = text

        // Extract HTML-like formatting
        val htmlTagRegex = """<(\w+)(?:\s+([^>]*))?>""".toRegex()
        htmlTagRegex.findAll(text).forEach { match ->
            val tag = match.groupValues[1].lowercase()
            val attributes = match.groupValues[2]

            when (tag) {
                "font" -> {
                    // Extract font attributes
                    val colorMatch = """color="([^"]*)\"""".toRegex().find(attributes)
                    colorMatch?.let { styleMap["color"] = it.groupValues[1] }

                    val sizeMatch = """size="([^"]*)\"""".toRegex().find(attributes)
                    sizeMatch?.let { styleMap["size"] = it.groupValues[1] }
                }
                "b" -> styleMap["bold"] = "true"
                "i" -> styleMap["italic"] = "true"
                "u" -> styleMap["underline"] = "true"
            }
        }

        // Clean text of HTML tags
        cleanText = cleanText.replace("""<[^>]*>""".toRegex(), "")

        return Pair(cleanText.trim(), styleMap)
    }

    /**
     * Validate subtitle cue timing
     */
    fun validateTiming(
        startTime: Long,
        endTime: Long,
        lineNumber: Int? = null,
    ): Boolean {
        if (startTime < 0 || endTime < 0) {
            throw SubtitleParsingException(
                "Negative timestamps not allowed",
                lineNumber = lineNumber,
            )
        }

        if (endTime <= startTime) {
            throw SubtitleParsingException(
                "End time must be after start time",
                lineNumber = lineNumber,
            )
        }

        return true
    }

    /**
     * Format time in milliseconds to HH:MM:SS,mmm format
     */
    fun formatTime(timeMs: Long): String {
        val hours = timeMs / 3600000
        val minutes = (timeMs % 3600000) / 60000
        val seconds = (timeMs % 60000) / 1000
        val milliseconds = timeMs % 1000

        return String.format("%02d:%02d:%02d,%03d", hours, minutes, seconds, milliseconds)
    }
}
