package com.rdwatch.androidtv.player.subtitle.parser

import com.rdwatch.androidtv.player.subtitle.SubtitleCue
import com.rdwatch.androidtv.player.subtitle.SubtitleFormat
import com.rdwatch.androidtv.player.subtitle.SubtitleTrackData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Parser for SubRip (.srt) subtitle format
 *
 * SRT Format:
 * 1
 * 00:00:12,000 --> 00:00:15,123
 * This is the first subtitle
 *
 * 2
 * 00:00:16,000 --> 00:00:18,000
 * This is the second subtitle
 * with multiple lines
 */
@Singleton
class SrtParser
    @Inject
    constructor() : SubtitleParser {
        override suspend fun parse(
            content: String,
            encoding: String,
        ): SubtitleTrackData =
            withContext(Dispatchers.IO) {
                try {
                    val cues = mutableListOf<SubtitleCue>()
                    val blocks = content.split("\n\n").filter { it.trim().isNotEmpty() }

                    blocks.forEachIndexed { blockIndex, block ->
                        try {
                            parseSubtitleBlock(block.trim(), blockIndex + 1)?.let { cue ->
                                cues.add(cue)
                            }
                        } catch (e: Exception) {
                            // Log error but continue parsing other blocks
                            android.util.Log.w("SrtParser", "Error parsing block ${blockIndex + 1}: ${e.message}")
                        }
                    }

                    if (cues.isEmpty()) {
                        throw SubtitleParsingException("No valid subtitle cues found in SRT content")
                    }

                    // Sort cues by start time
                    cues.sortWith(compareBy { it.startTimeMs })

                    SubtitleTrackData(
                        cues = cues,
                        format = SubtitleFormat.SRT,
                        encoding = encoding,
                    )
                } catch (e: SubtitleParsingException) {
                    throw e
                } catch (e: Exception) {
                    throw SubtitleParsingException("Failed to parse SRT content", e, SubtitleFormat.SRT)
                }
            }

        override suspend fun parse(
            inputStream: InputStream,
            encoding: String,
        ): SubtitleTrackData {
            return withContext(Dispatchers.IO) {
                val content = inputStream.bufferedReader(charset(encoding)).use { it.readText() }
                parse(content, encoding)
            }
        }

        override fun getSupportedFormats(): List<SubtitleFormat> = listOf(SubtitleFormat.SRT)

        override fun supportsFormat(format: SubtitleFormat): Boolean = format == SubtitleFormat.SRT

        private fun parseSubtitleBlock(
            block: String,
            blockNumber: Int,
        ): SubtitleCue? {
            val lines = block.split('\n').map { it.trim() }.filter { it.isNotEmpty() }

            if (lines.size < 3) {
                throw SubtitleParsingException(
                    "Invalid SRT block: must have at least 3 lines (index, timing, text)",
                    lineNumber = blockNumber,
                )
            }

            // Parse subtitle index (first line)
            val indexLine = lines[0]
            val index =
                try {
                    indexLine.toInt()
                } catch (e: NumberFormatException) {
                    throw SubtitleParsingException(
                        "Invalid subtitle index: $indexLine",
                        e,
                        SubtitleFormat.SRT,
                        blockNumber,
                    )
                }

            // Parse timing line (second line)
            val timingLine = lines[1]
            val (startTime, endTime) = parseTimingLine(timingLine, blockNumber)

            // Parse subtitle text (remaining lines)
            val textLines = lines.drop(2)
            val rawText = textLines.joinToString("\n")

            // Extract style information and clean text
            val (cleanText, styleInfo) = SubtitleParsingUtils.extractStyleInfo(rawText)

            if (cleanText.isEmpty()) {
                // Skip empty subtitles
                return null
            }

            // Apply basic style information to cue
            var textColor: Int? = null
            var backgroundColor: Int? = null

            styleInfo["color"]?.let { color ->
                textColor = parseColor(color)
            }

            return SubtitleCue(
                startTimeMs = startTime,
                endTimeMs = endTime,
                text = cleanText,
                textColor = textColor,
                backgroundColor = backgroundColor,
            )
        }

        private fun parseTimingLine(
            timingLine: String,
            blockNumber: Int,
        ): Pair<Long, Long> {
            val timingRegex = """(\d{2}:\d{2}:\d{2},\d{3})\s*-->\s*(\d{2}:\d{2}:\d{2},\d{3})""".toRegex()
            val matchResult =
                timingRegex.find(timingLine)
                    ?: throw SubtitleParsingException(
                        "Invalid timing format: $timingLine. Expected format: HH:MM:SS,mmm --> HH:MM:SS,mmm",
                        lineNumber = blockNumber,
                    )

            val startTimeString = matchResult.groupValues[1]
            val endTimeString = matchResult.groupValues[2]

            val startTime =
                try {
                    SubtitleParsingUtils.parseTimeString(startTimeString)
                } catch (e: Exception) {
                    throw SubtitleParsingException(
                        "Invalid start time: $startTimeString",
                        e,
                        SubtitleFormat.SRT,
                        blockNumber,
                    )
                }

            val endTime =
                try {
                    SubtitleParsingUtils.parseTimeString(endTimeString)
                } catch (e: Exception) {
                    throw SubtitleParsingException(
                        "Invalid end time: $endTimeString",
                        e,
                        SubtitleFormat.SRT,
                        blockNumber,
                    )
                }

            // Validate timing
            SubtitleParsingUtils.validateTiming(startTime, endTime, blockNumber)

            return Pair(startTime, endTime)
        }

        private fun parseColor(colorString: String): Int? {
            return try {
                when {
                    colorString.startsWith("#") -> {
                        android.graphics.Color.parseColor(colorString)
                    }
                    colorString.startsWith("rgb(") -> {
                        // Parse rgb(r,g,b) format
                        val rgbValues =
                            colorString.removePrefix("rgb(").removeSuffix(")")
                                .split(",").map { it.trim().toInt() }
                        if (rgbValues.size == 3) {
                            android.graphics.Color.rgb(rgbValues[0], rgbValues[1], rgbValues[2])
                        } else {
                            null
                        }
                    }
                    else -> {
                        // Try to parse as named color
                        when (colorString.lowercase()) {
                            "white" -> android.graphics.Color.WHITE
                            "black" -> android.graphics.Color.BLACK
                            "red" -> android.graphics.Color.RED
                            "green" -> android.graphics.Color.GREEN
                            "blue" -> android.graphics.Color.BLUE
                            "yellow" -> android.graphics.Color.YELLOW
                            "cyan" -> android.graphics.Color.CYAN
                            "magenta" -> android.graphics.Color.MAGENTA
                            else -> null
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.w("SrtParser", "Failed to parse color: $colorString")
                null
            }
        }
    }
