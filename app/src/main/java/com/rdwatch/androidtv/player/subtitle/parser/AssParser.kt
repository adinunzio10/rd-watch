package com.rdwatch.androidtv.player.subtitle.parser

import androidx.media3.common.text.Cue
import com.rdwatch.androidtv.player.subtitle.SubtitleCue
import androidx.media3.common.util.UnstableApi
import com.rdwatch.androidtv.player.subtitle.SubtitleFormat
import com.rdwatch.androidtv.player.subtitle.SubtitleTrackData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for Advanced SubStation Alpha (.ass) and SubStation Alpha (.ssa) subtitle formats
 * 
 * This is a simplified parser that focuses on extracting timing and text information.
 * Advanced styling features are partially supported.
 * 
 * ASS/SSA Format structure:
 * [Script Info]
 * [V4+ Styles] (ASS) or [V4 Styles] (SSA)
 * [Events]
 */
@UnstableApi
@Singleton
class AssParser @Inject constructor() : SubtitleParser {
    
    companion object {
        private val SECTION_REGEX = """\[([^\]]+)\]""".toRegex()
        private val DIALOGUE_REGEX = """^Dialogue:\s*(.+)$""".toRegex()
        private val COMMENT_REGEX = """^Comment:\s*(.+)$""".toRegex()
        private val STYLE_REGEX = """^Style:\s*(.+)$""".toRegex()
    }
    
    override suspend fun parse(content: String, encoding: String): SubtitleTrackData = withContext(Dispatchers.IO) {
        try {
            val lines = content.split('\n').map { it.trim() }
            
            // Detect format (ASS or SSA)
            val format = detectFormat(lines)
            
            // Parse sections
            val sections = parseSections(lines)
            
            // Parse styles for reference
            val styles = parseStyles(sections["V4+ Styles"] ?: sections["V4 Styles"] ?: emptyList())
            
            // Parse events (dialogue lines)
            val events = sections["Events"] ?: throw SubtitleParsingException("No [Events] section found")
            val cues = parseEvents(events, styles, format)
            
            if (cues.isEmpty()) {
                throw SubtitleParsingException("No valid dialogue events found in ASS/SSA content")
            }
            
            // Sort cues by start time
            val sortedCues = cues.sortedBy { it.startTimeMs }
            
            SubtitleTrackData(
                cues = sortedCues,
                format = format,
                encoding = encoding
            )
            
        } catch (e: SubtitleParsingException) {
            throw e
        } catch (e: Exception) {
            throw SubtitleParsingException("Failed to parse ASS/SSA content", e)
        }
    }
    
    override suspend fun parse(inputStream: InputStream, encoding: String): SubtitleTrackData {
        return withContext(Dispatchers.IO) {
            val content = inputStream.bufferedReader(charset(encoding)).use { it.readText() }
            parse(content, encoding)
        }
    }
    
    override fun getSupportedFormats(): List<SubtitleFormat> = listOf(SubtitleFormat.ASS, SubtitleFormat.SSA)
    
    override fun supportsFormat(format: SubtitleFormat): Boolean = 
        format == SubtitleFormat.ASS || format == SubtitleFormat.SSA
    
    private fun detectFormat(lines: List<String>): SubtitleFormat {
        return if (lines.any { it.contains("V4+ Styles") || it.contains("ScriptType: v4.00+") }) {
            SubtitleFormat.ASS
        } else {
            SubtitleFormat.SSA
        }
    }
    
    private fun parseSections(lines: List<String>): Map<String, List<String>> {
        val sections = mutableMapOf<String, MutableList<String>>()
        var currentSection: String? = null
        
        for (line in lines) {
            if (line.isEmpty() || line.startsWith(";")) continue
            
            val sectionMatch = SECTION_REGEX.find(line)
            if (sectionMatch != null) {
                currentSection = sectionMatch.groupValues[1]
                sections[currentSection] = mutableListOf()
            } else if (currentSection != null) {
                sections[currentSection]?.add(line)
            }
        }
        
        return sections
    }
    
    private fun parseStyles(styleLines: List<String>): Map<String, AssStyle> {
        val styles = mutableMapOf<String, AssStyle>()
        var format: List<String>? = null
        
        for (line in styleLines) {
            when {
                line.startsWith("Format:") -> {
                    format = line.removePrefix("Format:")
                        .split(',')
                        .map { it.trim() }
                }
                line.startsWith("Style:") -> {
                    val values = line.removePrefix("Style:")
                        .split(',')
                        .map { it.trim() }
                    
                    if (format != null && values.isNotEmpty()) {
                        val styleName = values[0]
                        styles[styleName] = parseStyleValues(format, values)
                    }
                }
            }
        }
        
        return styles
    }
    
    private fun parseStyleValues(format: List<String>, values: List<String>): AssStyle {
        val styleMap = format.zip(values).toMap()
        
        return AssStyle(
            name = styleMap["Name"] ?: "Default",
            fontName = styleMap["Fontname"] ?: "Arial",
            fontSize = styleMap["Fontsize"]?.toIntOrNull() ?: 20,
            primaryColor = parseAssColor(styleMap["PrimaryColour"]),
            secondaryColor = parseAssColor(styleMap["SecondaryColour"]),
            outlineColor = parseAssColor(styleMap["OutlineColour"]),
            backColor = parseAssColor(styleMap["BackColour"]),
            alignment = styleMap["Alignment"]?.toIntOrNull() ?: 2,
            marginL = styleMap["MarginL"]?.toIntOrNull() ?: 0,
            marginR = styleMap["MarginR"]?.toIntOrNull() ?: 0,
            marginV = styleMap["MarginV"]?.toIntOrNull() ?: 0
        )
    }
    
    private fun parseEvents(eventLines: List<String>, styles: Map<String, AssStyle>, format: SubtitleFormat): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        var eventFormat: List<String>? = null
        
        for ((lineIndex, line) in eventLines.withIndex()) {
            try {
                when {
                    line.startsWith("Format:") -> {
                        eventFormat = line.removePrefix("Format:")
                            .split(',')
                            .map { it.trim() }
                    }
                    DIALOGUE_REGEX.matches(line) -> {
                        val dialogueData = DIALOGUE_REGEX.find(line)?.groupValues?.get(1) ?: continue
                        val values = dialogueData.split(',').map { it.trim() }
                        
                        if (eventFormat != null) {
                            parseDialogueLine(eventFormat, values, styles, lineIndex)?.let { cue ->
                                cues.add(cue)
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("AssParser", "Error parsing event line $lineIndex: ${e.message}")
            }
        }
        
        return cues
    }
    
    private fun parseDialogueLine(
        format: List<String>, 
        values: List<String>, 
        styles: Map<String, AssStyle>,
        lineIndex: Int
    ): SubtitleCue? {
        if (values.size < format.size) return null
        
        val eventMap = format.zip(values).toMap()
        
        val startTimeStr = eventMap["Start"] ?: return null
        val endTimeStr = eventMap["End"] ?: return null
        val styleStr = eventMap["Style"] ?: "Default"
        val textStr = eventMap["Text"] ?: return null
        
        val startTime = parseAssTime(startTimeStr)
        val endTime = parseAssTime(endTimeStr)
        
        if (startTime < 0 || endTime < 0 || endTime <= startTime) {
            return null
        }
        
        // Clean text of ASS formatting codes
        val cleanText = cleanAssText(textStr)
        
        if (cleanText.isEmpty()) return null
        
        // Apply style if available
        val style = styles[styleStr]
        val textAlignment = style?.let { convertAssAlignment(it.alignment) }
        
        return SubtitleCue(
            startTimeMs = startTime,
            endTimeMs = endTime,
            text = cleanText,
            textAlignment = textAlignment,
            textColor = style?.primaryColor
        )
    }
    
    private fun parseAssTime(timeString: String): Long {
        // ASS time format: H:MM:SS.cc (centiseconds)
        val regex = """(\d):(\d{2}):(\d{2})\.(\d{2})""".toRegex()
        val match = regex.find(timeString) ?: return -1
        
        val hours = match.groupValues[1].toInt()
        val minutes = match.groupValues[2].toInt()
        val seconds = match.groupValues[3].toInt()
        val centiseconds = match.groupValues[4].toInt()
        
        return (hours * 3600 + minutes * 60 + seconds) * 1000L + centiseconds * 10L
    }
    
    private fun cleanAssText(text: String): String {
        return text
            .replace("""\{[^}]*\}""".toRegex(), "") // Remove ASS override tags
            .replace("\\N", "\n") // Convert \N to newlines
            .replace("\\n", "\n") // Convert \n to newlines
            .replace("\\h", " ") // Convert \h to hard space
            .trim()
    }
    
    private fun parseAssColor(colorString: String?): Int? {
        if (colorString == null) return null
        
        return try {
            // ASS colors are in BGR format as decimal or hex
            val colorValue = if (colorString.startsWith("&H")) {
                colorString.removePrefix("&H").removeSuffix("&").toInt(16)
            } else {
                colorString.toInt()
            }
            
            // Convert BGR to RGB
            val b = (colorValue shr 16) and 0xFF
            val g = (colorValue shr 8) and 0xFF
            val r = colorValue and 0xFF
            
            android.graphics.Color.rgb(r, g, b)
        } catch (e: Exception) {
            null
        }
    }
    
    private fun convertAssAlignment(assAlignment: Int): Int {
        // Convert ASS alignment to ExoPlayer alignment
        return when (assAlignment) {
            1, 2, 3 -> 2   // TEXT_ALIGNMENT_CENTER - Bottom alignments
            5, 6, 7 -> 2   // TEXT_ALIGNMENT_CENTER - Middle alignments  
            9, 10, 11 -> 2 // TEXT_ALIGNMENT_CENTER - Top alignments
            else -> 2      // TEXT_ALIGNMENT_CENTER
        }
    }
}

/**
 * Represents an ASS/SSA style
 */
data class AssStyle(
    val name: String,
    val fontName: String,
    val fontSize: Int,
    val primaryColor: Int?,
    val secondaryColor: Int?,
    val outlineColor: Int?,
    val backColor: Int?,
    val alignment: Int,
    val marginL: Int,
    val marginR: Int,
    val marginV: Int
)