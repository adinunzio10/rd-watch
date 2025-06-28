package com.rdwatch.androidtv.player.subtitle.parser

import com.rdwatch.androidtv.player.subtitle.SubtitleFormat
import com.rdwatch.androidtv.player.subtitle.test.SubtitleTestBase
import com.rdwatch.androidtv.player.subtitle.test.SubtitleTestData
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import javax.inject.Inject

@HiltAndroidTest
class SrtParserTest : SubtitleTestBase() {

    @Inject
    lateinit var srtParser: SrtParser

    @Before
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun `parse should handle valid SRT content correctly`() = runSubtitleTest {
        val trackData = srtParser.parse(SubtitleTestData.VALID_SRT_CONTENT)
        
        assertEquals("Should parse SRT format", SubtitleFormat.SRT, trackData.format)
        assertEquals("Should have 3 cues", 3, trackData.cues.size)
        
        val firstCue = trackData.cues[0]
        assertEquals("First cue start time", 1000L, firstCue.startTimeMs)
        assertEquals("First cue end time", 3000L, firstCue.endTimeMs)
        assertEquals("First cue text", "First subtitle line", firstCue.text)
        
        val secondCue = trackData.cues[1]
        assertEquals("Second cue start time", 4000L, secondCue.startTimeMs)
        assertEquals("Second cue end time", 6000L, secondCue.endTimeMs)
        assertEquals("Second cue text", "Second subtitle line\nwith multiple lines", secondCue.text)
        
        val thirdCue = trackData.cues[2]
        assertEquals("Third cue start time", 7500L, thirdCue.startTimeMs)
        assertEquals("Third cue end time", 9000L, thirdCue.endTimeMs)
        assertEquals("Third cue text", "Third subtitle with bold text", thirdCue.text)
    }

    @Test
    fun `parse should handle malformed SRT content gracefully`() = runSubtitleTest {
        assertParsingFails(
            parser = { srtParser.parse(SubtitleTestData.MALFORMED_SRT_CONTENT) },
            expectedMessagePattern = "Invalid timing format",
            expectedFormat = SubtitleFormat.SRT
        )
    }

    @Test
    fun `parse should handle invalid timing order`() = runSubtitleTest {
        assertParsingFails(
            parser = { srtParser.parse(SubtitleTestData.INVALID_TIMING_SRT) },
            expectedMessagePattern = "End time must be after start time"
        )
    }

    @Test
    fun `parse should handle empty content`() = runSubtitleTest {
        assertParsingFails(
            parser = { srtParser.parse(SubtitleTestData.EMPTY_CONTENT) },
            expectedMessagePattern = "No valid subtitle cues found"
        )
    }

    @Test
    fun `parse should handle whitespace-only content`() = runSubtitleTest {
        assertParsingFails(
            parser = { srtParser.parse(SubtitleTestData.WHITESPACE_CONTENT) },
            expectedMessagePattern = "No valid subtitle cues found"
        )
    }

    @Test
    fun `parse should handle Unicode characters correctly`() = runSubtitleTest {
        val trackData = srtParser.parse(SubtitleTestData.UNICODE_SRT_CONTENT)
        
        assertEquals("Should have 2 cues", 2, trackData.cues.size)
        
        val firstCue = trackData.cues[0]
        assertEquals("First cue with Unicode", "Héllo wörld! 你好世界 🎬", firstCue.text)
        
        val secondCue = trackData.cues[1]
        assertEquals("Second cue with special chars", "Special chars: \"quotes\" & <tags> [brackets]", secondCue.text)
    }

    @Test
    fun `parse should extract HTML formatting and clean text`() = runSubtitleTest {
        val trackData = srtParser.parse(SubtitleTestData.FORMATTED_SRT_CONTENT)
        
        assertEquals("Should have 3 cues", 3, trackData.cues.size)
        
        val firstCue = trackData.cues[0]
        assertEquals("First cue cleaned text", "Red text", firstCue.text)
        
        val secondCue = trackData.cues[1]
        assertEquals("Second cue cleaned text", "Bold and italic and underlined", secondCue.text)
        
        val thirdCue = trackData.cues[2]
        assertEquals("Third cue cleaned text", "Mixed bold italic formatting", thirdCue.text)
    }

    @Test
    fun `parse should handle cues with only index and timing (no text)`() = runSubtitleTest {
        val content = """
            1
            00:00:01,000 --> 00:00:03,000
        """.trimIndent()
        
        assertParsingFails(
            parser = { srtParser.parse(content) },
            expectedMessagePattern = "Invalid SRT block: must have at least 3 lines"
        )
    }

    @Test
    fun `parse should handle invalid subtitle index`() = runSubtitleTest {
        val content = """
            not_a_number
            00:00:01,000 --> 00:00:03,000
            Test subtitle
        """.trimIndent()
        
        assertParsingFails(
            parser = { srtParser.parse(content) },
            expectedMessagePattern = "Invalid subtitle index"
        )
    }

    @Test
    fun `parse should sort cues by start time`() = runSubtitleTest {
        val outOfOrderSrt = """
            1
            00:00:07,000 --> 00:00:09,000
            Third subtitle
            
            2
            00:00:01,000 --> 00:00:03,000
            First subtitle
            
            3
            00:00:04,000 --> 00:00:06,000
            Second subtitle
        """.trimIndent()
        
        val trackData = srtParser.parse(outOfOrderSrt)
        
        assertEquals("Should have 3 cues", 3, trackData.cues.size)
        assertEquals("First cue should be earliest", 1000L, trackData.cues[0].startTimeMs)
        assertEquals("Second cue should be middle", 4000L, trackData.cues[1].startTimeMs)
        assertEquals("Third cue should be latest", 7000L, trackData.cues[2].startTimeMs)
    }

    @Test
    fun `parse should handle different timing formats`() = runSubtitleTest {
        val content = """
            1
            01:23:45,678 --> 01:23:47,890
            Long timestamp format
        """.trimIndent()
        
        val trackData = srtParser.parse(content)
        
        assertEquals("Should have 1 cue", 1, trackData.cues.size)
        
        val cue = trackData.cues[0]
        val expectedStart = (1 * 3600 + 23 * 60 + 45) * 1000L + 678
        val expectedEnd = (1 * 3600 + 23 * 60 + 47) * 1000L + 890
        
        assertEquals("Start time should be correct", expectedStart, cue.startTimeMs)
        assertEquals("End time should be correct", expectedEnd, cue.endTimeMs)
    }

    @Test
    fun `parse should handle mixed spacing in timing line`() = runSubtitleTest {
        val content = """
            1
            00:00:01,000-->00:00:03,000
            No spaces around arrow
            
            2
            00:00:04,000   -->   00:00:06,000
            Extra spaces around arrow
        """.trimIndent()
        
        val trackData = srtParser.parse(content)
        
        assertEquals("Should have 2 cues", 2, trackData.cues.size)
        assertEquals("First cue start", 1000L, trackData.cues[0].startTimeMs)
        assertEquals("Second cue start", 4000L, trackData.cues[1].startTimeMs)
    }

    @Test
    fun `parse should skip empty subtitle blocks`() = runSubtitleTest {
        val content = """
            1
            00:00:01,000 --> 00:00:03,000
            First subtitle
            
            2
            00:00:04,000 --> 00:00:06,000
            
            
            3
            00:00:07,000 --> 00:00:09,000
            Third subtitle
        """.trimIndent()
        
        val trackData = srtParser.parse(content)
        
        assertEquals("Should have 2 cues (skip empty)", 2, trackData.cues.size)
        assertEquals("First cue text", "First subtitle", trackData.cues[0].text)
        assertEquals("Second cue text", "Third subtitle", trackData.cues[1].text)
    }

    @Test
    fun `parse should handle color formatting tags`() = runSubtitleTest {
        val content = """
            1
            00:00:01,000 --> 00:00:03,000
            <font color="red">Red text</font>
            
            2
            00:00:04,000 --> 00:00:06,000
            <font color="#FF0000">Hex red text</font>
            
            3
            00:00:07,000 --> 00:00:09,000
            <font color="rgb(255,0,0)">RGB red text</font>
        """.trimIndent()
        
        val trackData = srtParser.parse(content)
        
        assertEquals("Should have 3 cues", 3, trackData.cues.size)
        assertEquals("First cue cleaned", "Red text", trackData.cues[0].text)
        assertEquals("Second cue cleaned", "Hex red text", trackData.cues[1].text)
        assertEquals("Third cue cleaned", "RGB red text", trackData.cues[2].text)
    }

    @Test
    fun `parse should handle nested formatting tags`() = runSubtitleTest {
        val content = """
            1
            00:00:01,000 --> 00:00:03,000
            <b><i>Bold and italic</i></b> text
        """.trimIndent()
        
        val trackData = srtParser.parse(content)
        
        assertEquals("Should have 1 cue", 1, trackData.cues.size)
        assertEquals("Nested tags cleaned", "Bold and italic text", trackData.cues[0].text)
    }

    @Test
    fun `parse should handle very large subtitle files`() = runSubtitleTest {
        val largeContent = StringBuilder()
        
        // Generate 1000 subtitle blocks
        for (i in 1..1000) {
            val startMs = i * 2000L
            val endMs = startMs + 1500L
            val hours = startMs / 3600000
            val minutes = (startMs % 3600000) / 60000
            val seconds = (startMs % 60000) / 1000
            val ms = startMs % 1000
            
            val endHours = endMs / 3600000
            val endMinutes = (endMs % 3600000) / 60000
            val endSeconds = (endMs % 60000) / 1000
            val endMsRemainder = endMs % 1000
            
            largeContent.append("$i\n")
            largeContent.append(String.format("%02d:%02d:%02d,%03d --> %02d:%02d:%02d,%03d\n", 
                hours, minutes, seconds, ms, endHours, endMinutes, endSeconds, endMsRemainder))
            largeContent.append("Subtitle number $i\n\n")
        }
        
        val trackData = srtParser.parse(largeContent.toString())
        
        assertEquals("Should parse all 1000 cues", 1000, trackData.cues.size)
        assertEquals("First cue timing", 2000L, trackData.cues[0].startTimeMs)
        assertEquals("Last cue timing", 2000000L, trackData.cues[999].startTimeMs)
    }

    @Test
    fun `getSupportedFormats should return SRT format`() {
        val formats = srtParser.getSupportedFormats()
        assertEquals("Should support only SRT", listOf(SubtitleFormat.SRT), formats)
    }

    @Test
    fun `supportsFormat should return true for SRT only`() {
        assertTrue("Should support SRT", srtParser.supportsFormat(SubtitleFormat.SRT))
        assertFalse("Should not support VTT", srtParser.supportsFormat(SubtitleFormat.VTT))
        assertFalse("Should not support ASS", srtParser.supportsFormat(SubtitleFormat.ASS))
        assertFalse("Should not support SSA", srtParser.supportsFormat(SubtitleFormat.SSA))
    }

    @Test
    fun `parse should handle extremely short timing`() = runSubtitleTest {
        val content = """
            1
            00:00:00,001 --> 00:00:00,002
            Very short subtitle
        """.trimIndent()
        
        val trackData = srtParser.parse(content)
        
        assertEquals("Should have 1 cue", 1, trackData.cues.size)
        assertEquals("Start time", 1L, trackData.cues[0].startTimeMs)
        assertEquals("End time", 2L, trackData.cues[0].endTimeMs)
    }

    @Test
    fun `parse should handle zero start time`() = runSubtitleTest {
        val content = """
            1
            00:00:00,000 --> 00:00:02,000
            Subtitle starting at zero
        """.trimIndent()
        
        val trackData = srtParser.parse(content)
        
        assertEquals("Should have 1 cue", 1, trackData.cues.size)
        assertEquals("Start time should be zero", 0L, trackData.cues[0].startTimeMs)
        assertEquals("End time", 2000L, trackData.cues[0].endTimeMs)
    }

    @Test
    fun `parse should handle missing final newline`() = runSubtitleTest {
        val content = """1
00:00:01,000 --> 00:00:03,000
Final subtitle without newline"""
        
        val trackData = srtParser.parse(content)
        
        assertEquals("Should have 1 cue", 1, trackData.cues.size)
        assertEquals("Text should be parsed", "Final subtitle without newline", trackData.cues[0].text)
    }
}