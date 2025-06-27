package com.rdwatch.androidtv.player.subtitle.parser

import com.rdwatch.androidtv.player.subtitle.SubtitleFormat
import com.rdwatch.androidtv.player.subtitle.SubtitleTrackData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Factory class that manages all subtitle parsers and provides unified parsing interface
 */
@Singleton
class SubtitleParserFactory @Inject constructor(
    private val srtParser: SrtParser,
    private val vttParser: VttParser,
    private val assParser: AssParser
) {
    
    private val parsers = listOf(srtParser, vttParser, assParser)
    
    /**
     * Parse subtitle content from string with automatic format detection
     */
    suspend fun parseSubtitle(
        content: String,
        format: SubtitleFormat? = null,
        encoding: String = "UTF-8"
    ): SubtitleParseResult = withContext(Dispatchers.IO) {
        try {
            val detectedFormat = format ?: detectFormat(content)
            val parser = getParserForFormat(detectedFormat)
                ?: return@withContext SubtitleParseResult.Error(
                    SubtitleParsingException("No parser available for format: $detectedFormat")
                )
            
            val trackData = parser.parse(content, encoding)
            SubtitleParseResult.Success(trackData)
            
        } catch (e: SubtitleParsingException) {
            SubtitleParseResult.Error(e)
        } catch (e: Exception) {
            SubtitleParseResult.Error(
                SubtitleParsingException("Unexpected error during subtitle parsing", e)
            )
        }
    }
    
    /**
     * Parse subtitle content from InputStream
     */
    suspend fun parseSubtitle(
        inputStream: InputStream,
        format: SubtitleFormat? = null,
        encoding: String = "UTF-8"
    ): SubtitleParseResult = withContext(Dispatchers.IO) {
        try {
            // Read content for format detection if needed
            val content = inputStream.bufferedReader(charset(encoding)).use { it.readText() }
            parseSubtitle(content, format, encoding)
        } catch (e: Exception) {
            SubtitleParseResult.Error(
                SubtitleParsingException("Failed to read subtitle stream", e)
            )
        }
    }
    
    /**
     * Parse subtitle from URL
     */
    suspend fun parseSubtitleFromUrl(
        url: String,
        format: SubtitleFormat? = null,
        encoding: String = "UTF-8"
    ): SubtitleParseResult = withContext(Dispatchers.IO) {
        try {
            val detectedFormat = format ?: SubtitleFormat.fromUrl(url)
            
            val connection = URL(url).openConnection()
            connection.connectTimeout = 30000
            connection.readTimeout = 30000
            
            connection.getInputStream().use { inputStream ->
                parseSubtitle(inputStream, detectedFormat, encoding)
            }
            
        } catch (e: Exception) {
            SubtitleParseResult.Error(
                SubtitleParsingException("Failed to download subtitle from URL: $url", e)
            )
        }
    }
    
    /**
     * Get parser for specific format
     */
    fun getParserForFormat(format: SubtitleFormat): SubtitleParser? {
        return parsers.find { it.supportsFormat(format) }
    }
    
    /**
     * Get all supported formats
     */
    fun getSupportedFormats(): List<SubtitleFormat> {
        return parsers.flatMap { it.getSupportedFormats() }.distinct()
    }
    
    /**
     * Check if format is supported
     */
    fun isFormatSupported(format: SubtitleFormat): Boolean {
        return parsers.any { it.supportsFormat(format) }
    }
    
    /**
     * Detect subtitle format from content
     */
    private fun detectFormat(content: String): SubtitleFormat {
        val trimmedContent = content.trim()
        
        return when {
            // VTT detection
            trimmedContent.startsWith("WEBVTT") -> SubtitleFormat.VTT
            
            // ASS/SSA detection
            trimmedContent.contains("[Script Info]") && 
            (trimmedContent.contains("[V4+ Styles]") || trimmedContent.contains("ScriptType: v4.00+")) -> 
                SubtitleFormat.ASS
                
            trimmedContent.contains("[Script Info]") && 
            trimmedContent.contains("[V4 Styles]") -> 
                SubtitleFormat.SSA
            
            // SRT detection (check for timing pattern)
            trimmedContent.contains("""--\>""".toRegex()) && 
            trimmedContent.contains("""\d{2}:\d{2}:\d{2},\d{3}""".toRegex()) -> 
                SubtitleFormat.SRT
            
            // TTML detection
            trimmedContent.contains("<tt ") && trimmedContent.contains("xmlns") -> 
                SubtitleFormat.TTML
            
            else -> SubtitleFormat.UNKNOWN
        }
    }
    
    /**
     * Validate subtitle content
     */
    suspend fun validateSubtitle(
        content: String,
        format: SubtitleFormat? = null
    ): SubtitleValidationResult = withContext(Dispatchers.IO) {
        try {
            val result = parseSubtitle(content, format)
            when (result) {
                is SubtitleParseResult.Success -> {
                    val trackData = result.trackData
                    SubtitleValidationResult.Valid(
                        format = trackData.format,
                        cueCount = trackData.cues.size,
                        duration = trackData.totalDurationMs,
                        encoding = trackData.encoding
                    )
                }
                is SubtitleParseResult.Error -> {
                    SubtitleValidationResult.Invalid(result.exception.message ?: "Unknown error")
                }
            }
        } catch (e: Exception) {
            SubtitleValidationResult.Invalid("Validation failed: ${e.message}")
        }
    }
}

/**
 * Result of subtitle validation
 */
sealed class SubtitleValidationResult {
    data class Valid(
        val format: SubtitleFormat,
        val cueCount: Int,
        val duration: Long,
        val encoding: String
    ) : SubtitleValidationResult()
    
    data class Invalid(val reason: String) : SubtitleValidationResult()
}

/**
 * Subtitle loading configuration
 */
data class SubtitleLoadConfig(
    val url: String,
    val format: SubtitleFormat? = null,
    val encoding: String = "UTF-8",
    val language: String? = null,
    val label: String? = null,
    val autoSelect: Boolean = false,
    val retryCount: Int = 3,
    val timeoutMs: Int = 30000
)