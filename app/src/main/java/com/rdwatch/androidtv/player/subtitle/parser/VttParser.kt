package com.rdwatch.androidtv.player.subtitle.parser

import androidx.media3.common.text.Cue
import com.rdwatch.androidtv.player.subtitle.SubtitleCue
import com.rdwatch.androidtv.player.subtitle.SubtitleFormat
import com.rdwatch.androidtv.player.subtitle.SubtitleTrackData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for WebVTT (.vtt) subtitle format
 * 
 * VTT Format:
 * WEBVTT
 * 
 * 00:00:12.000 --> 00:00:15.123
 * This is the first subtitle
 * 
 * 00:00:16.000 --> 00:00:18.000 align:center line:90%
 * This is the second subtitle
 * with positioning
 */
@Singleton
class VttParser @Inject constructor() : SubtitleParser {
    
    companion object {
        private const val VTT_SIGNATURE = "WEBVTT"
        private val TIMING_REGEX = """(\d{1,2}:\d{2}:\d{2}\.\d{3})\s*-->\s*(\d{1,2}:\d{2}:\d{2}\.\d{3})(?:\s+(.*))?""".toRegex()
        private val SETTINGS_REGEX = """(\w+):([^\s]+)""".toRegex()
    }
    
    override suspend fun parse(content: String, encoding: String): SubtitleTrackData = withContext(Dispatchers.IO) {
        try {
            val lines = content.split('\n').map { it.trim() }
            
            // Validate VTT signature
            if (lines.isEmpty() || !lines[0].startsWith(VTT_SIGNATURE)) {
                throw SubtitleParsingException("Invalid VTT file: missing WEBVTT signature")
            }
            
            val cues = mutableListOf<SubtitleCue>()
            var currentLineIndex = findFirstCueStart(lines)
            
            while (currentLineIndex < lines.size) {
                val cueResult = parseNextCue(lines, currentLineIndex)
                if (cueResult != null) {
                    cues.add(cueResult.first)
                    currentLineIndex = cueResult.second
                } else {
                    currentLineIndex++
                }
            }
            
            if (cues.isEmpty()) {
                throw SubtitleParsingException("No valid subtitle cues found in VTT content")
            }
            
            // Sort cues by start time
            cues.sortWith(compareBy { it.startTimeMs })
            
            SubtitleTrackData(
                cues = cues,
                format = SubtitleFormat.VTT,
                encoding = encoding
            )
            
        } catch (e: SubtitleParsingException) {
            throw e
        } catch (e: Exception) {
            throw SubtitleParsingException("Failed to parse VTT content", e, SubtitleFormat.VTT)
        }
    }
    
    override suspend fun parse(inputStream: InputStream, encoding: String): SubtitleTrackData {
        return withContext(Dispatchers.IO) {
            val content = inputStream.bufferedReader(charset(encoding)).use { it.readText() }
            parse(content, encoding)
        }
    }
    
    override fun getSupportedFormats(): List<SubtitleFormat> = listOf(SubtitleFormat.VTT)
    
    override fun supportsFormat(format: SubtitleFormat): Boolean = format == SubtitleFormat.VTT
    
    private fun findFirstCueStart(lines: List<String>): Int {
        // Skip header, metadata, and styles
        for (i in 1 until lines.size) {
            val line = lines[i]
            if (line.isEmpty()) continue
            if (TIMING_REGEX.matches(line)) {
                return i
            }
            // Skip NOTE blocks
            if (line.startsWith("NOTE")) {
                // Find next empty line
                val nextEmpty = lines.drop(i + 1).indexOfFirst { it.isEmpty() }
                if (nextEmpty != -1) {
                    return i + nextEmpty + 1
                }
            }
        }
        return lines.size
    }
    
    private fun parseNextCue(lines: List<String>, startIndex: Int): Pair<SubtitleCue, Int>? {
        var currentIndex = startIndex
        
        // Skip empty lines
        while (currentIndex < lines.size && lines[currentIndex].isEmpty()) {
            currentIndex++
        }
        
        if (currentIndex >= lines.size) return null
        
        var cueId: String? = null
        var timingLine: String? = null
        
        // Check if first line is a cue identifier
        val firstLine = lines[currentIndex]
        if (!TIMING_REGEX.matches(firstLine)) {
            // This line is a cue identifier
            cueId = firstLine
            currentIndex++
            if (currentIndex >= lines.size) return null
            timingLine = lines[currentIndex]
        } else {
            timingLine = firstLine
        }
        
        // Parse timing line
        val (startTime, endTime, settings) = parseTimingLine(timingLine, currentIndex)
        currentIndex++
        
        // Collect cue text until empty line or end of file
        val textLines = mutableListOf<String>()
        while (currentIndex < lines.size && lines[currentIndex].isNotEmpty()) {
            textLines.add(lines[currentIndex])
            currentIndex++
        }
        
        if (textLines.isEmpty()) {
            // Skip empty cues
            return null
        }
        
        val rawText = textLines.joinToString("\n")
        val (cleanText, styleInfo) = SubtitleParsingUtils.extractStyleInfo(rawText)
        
        // Parse VTT positioning settings
        val position = settings["position"]?.removeSuffix("%")?.toFloatOrNull()?.div(100f)
        val line = settings["line"]?.let { parseLinePosition(it) }
        val size = settings["size"]?.removeSuffix("%")?.toFloatOrNull()?.div(100f)
        val align = settings["align"]?.let { parseTextAlignment(it) }
        val vertical = settings["vertical"]?.let { parseVerticalType(it) }
        
        val cue = SubtitleCue(
            startTimeMs = startTime,
            endTimeMs = endTime,
            text = cleanText,
            position = position,
            line = line,
            size = size,
            textAlignment = align,
            verticalType = vertical
        )
        
        return Pair(cue, currentIndex)
    }
    
    private fun parseTimingLine(timingLine: String, lineNumber: Int): Triple<Long, Long, Map<String, String>> {
        val matchResult = TIMING_REGEX.find(timingLine)
            ?: throw SubtitleParsingException(
                "Invalid VTT timing format: $timingLine",
                lineNumber = lineNumber
            )
        
        val startTimeString = matchResult.groupValues[1]
        val endTimeString = matchResult.groupValues[2]
        val settingsString = matchResult.groupValues[3]
        
        val startTime = try {
            parseVttTime(startTimeString)
        } catch (e: Exception) {
            throw SubtitleParsingException(
                "Invalid start time: $startTimeString",
                e,
                SubtitleFormat.VTT,
                lineNumber
            )
        }
        
        val endTime = try {
            parseVttTime(endTimeString)
        } catch (e: Exception) {
            throw SubtitleParsingException(
                "Invalid end time: $endTimeString",
                e,
                SubtitleFormat.VTT,
                lineNumber
            )
        }
        
        // Validate timing
        SubtitleParsingUtils.validateTiming(startTime, endTime, lineNumber)
        
        // Parse settings
        val settings = mutableMapOf<String, String>()
        if (settingsString.isNotEmpty()) {
            SETTINGS_REGEX.findAll(settingsString).forEach { match ->
                val key = match.groupValues[1]
                val value = match.groupValues[2]
                settings[key] = value
            }
        }
        
        return Triple(startTime, endTime, settings)
    }
    
    private fun parseVttTime(timeString: String): Long {
        // VTT uses HH:MM:SS.mmm or MM:SS.mmm format
        val parts = timeString.split(':')
        
        return when (parts.size) {
            2 -> {
                // MM:SS.mmm format
                val minutes = parts[0].toInt()
                val secondsParts = parts[1].split('.')
                val seconds = secondsParts[0].toInt()
                val milliseconds = secondsParts[1].toInt()
                
                (minutes * 60 + seconds) * 1000L + milliseconds
            }
            3 -> {
                // HH:MM:SS.mmm format
                val hours = parts[0].toInt()
                val minutes = parts[1].toInt()
                val secondsParts = parts[2].split('.')
                val seconds = secondsParts[0].toInt()
                val milliseconds = secondsParts[1].toInt()
                
                (hours * 3600 + minutes * 60 + seconds) * 1000L + milliseconds
            }
            else -> throw IllegalArgumentException("Invalid VTT time format: $timeString")
        }
    }
    
    private fun parseLinePosition(lineValue: String): Float {
        return when {
            lineValue.endsWith("%") -> {
                lineValue.removeSuffix("%").toFloat() / 100f
            }
            else -> {
                // Line number (convert to approximate percentage)
                val lineNumber = lineValue.toInt()
                // Assume 20 lines max for percentage conversion
                (lineNumber / 20f).coerceIn(0f, 1f)
            }
        }
    }
    
    private fun parseTextAlignment(alignValue: String): Int {
        return when (alignValue.lowercase()) {
            "start", "left" -> 1    // TEXT_ALIGNMENT_START
            "center", "middle" -> 2 // TEXT_ALIGNMENT_CENTER
            "end", "right" -> 3     // TEXT_ALIGNMENT_END
            else -> 2               // TEXT_ALIGNMENT_CENTER
        }
    }
    
    private fun parseVerticalType(verticalValue: String): Int {
        return when (verticalValue.lowercase()) {
            "rl" -> 1 // VERTICAL_TYPE_RL
            "lr" -> 2 // VERTICAL_TYPE_LR
            else -> 0 // TYPE_UNSET
        }
    }
}