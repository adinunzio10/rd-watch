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
class VttParserTest : SubtitleTestBase() {

    @Inject
    lateinit var vttParser: VttParser

    @Before
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun `parse should handle valid VTT content correctly`() = runSubtitleTest {
        val trackData = vttParser.parse(SubtitleTestData.VALID_VTT_CONTENT)
        
        assertEquals("Should parse VTT format", SubtitleFormat.VTT, trackData.format)
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
        assertEquals("Third cue text", "Positioned subtitle text", thirdCue.text)
        assertEquals("Third cue position", 0.5f, thirdCue.position)
        assertEquals("Third cue line", 0.85f, thirdCue.line)
    }

    @Test
    fun `parse should reject VTT without WEBVTT signature`() = runSubtitleTest {
        val invalidContent = """
            00:00:01.000 --> 00:00:03.000
            This is not a valid VTT file
        """.trimIndent()
        
        assertParsingFails(
            parser = { vttParser.parse(invalidContent) },
            expectedMessagePattern = "Invalid VTT file: missing WEBVTT signature"
        )
    }

    @Test
    fun `parse should handle VTT with NOTE blocks`() = runSubtitleTest {
        val contentWithNotes = """
            WEBVTT
            
            NOTE This is a test file
            NOTE with multiple note blocks
            
            00:00:01.000 --> 00:00:03.000
            First subtitle
            
            NOTE Another note in the middle
            
            00:00:04.000 --> 00:00:06.000
            Second subtitle
        """.trimIndent()
        
        val trackData = vttParser.parse(contentWithNotes)
        
        assertEquals("Should have 2 cues", 2, trackData.cues.size)
        assertEquals("First cue text", "First subtitle", trackData.cues[0].text)
        assertEquals("Second cue text", "Second subtitle", trackData.cues[1].text)
    }

    @Test
    fun `parse should handle VTT with cue identifiers`() = runSubtitleTest {
        val contentWithIds = """
            WEBVTT
            
            cue1
            00:00:01.000 --> 00:00:03.000
            First subtitle with ID
            
            second-cue
            00:00:04.000 --> 00:00:06.000
            Second subtitle with ID
        """.trimIndent()
        
        val trackData = vttParser.parse(contentWithIds)
        
        assertEquals("Should have 2 cues", 2, trackData.cues.size)
        assertEquals("First cue text", "First subtitle with ID", trackData.cues[0].text)
        assertEquals("Second cue text", "Second subtitle with ID", trackData.cues[1].text)
    }

    @Test
    fun `parse should handle VTT positioning settings`() = runSubtitleTest {
        val contentWithPositioning = """
            WEBVTT
            
            00:00:01.000 --> 00:00:03.000 position:25% line:90% size:75% align:start
            Positioned subtitle
            
            00:00:04.000 --> 00:00:06.000 align:center line:50%
            Centered subtitle
            
            00:00:07.000 --> 00:00:09.000 align:end vertical:rl
            Right-aligned vertical text
        """.trimIndent()
        
        val trackData = vttParser.parse(contentWithPositioning)
        
        assertEquals("Should have 3 cues", 3, trackData.cues.size)
        
        val firstCue = trackData.cues[0]
        assertEquals("First cue position", 0.25f, firstCue.position)
        assertEquals("First cue line", 0.9f, firstCue.line)
        assertEquals("First cue size", 0.75f, firstCue.size)
        assertEquals("First cue alignment", 1, firstCue.textAlignment) // START
        
        val secondCue = trackData.cues[1]
        assertEquals("Second cue alignment", 2, secondCue.textAlignment) // CENTER
        assertEquals("Second cue line", 0.5f, secondCue.line)
        
        val thirdCue = trackData.cues[2]
        assertEquals("Third cue alignment", 3, thirdCue.textAlignment) // END
        assertEquals("Third cue vertical", 1, thirdCue.verticalType) // RL
    }

    @Test
    fun `parse should handle different VTT time formats`() = runSubtitleTest {
        val contentWithDifferentTimes = """
            WEBVTT
            
            00:01:23.456 --> 00:01:25.789
            Long format with hours
            
            01:23.456 --> 01:25.789
            Short format without hours
        """.trimIndent()
        
        val trackData = vttParser.parse(contentWithDifferentTimes)
        
        assertEquals("Should have 2 cues", 2, trackData.cues.size)
        
        val firstCue = trackData.cues[0]
        val expectedStart1 = (1 * 60 + 23) * 1000L + 456
        val expectedEnd1 = (1 * 60 + 25) * 1000L + 789
        assertEquals("First cue start time", expectedStart1, firstCue.startTimeMs)
        assertEquals("First cue end time", expectedEnd1, firstCue.endTimeMs)
        
        val secondCue = trackData.cues[1]
        val expectedStart2 = (1 * 60 + 23) * 1000L + 456
        val expectedEnd2 = (1 * 60 + 25) * 1000L + 789
        assertEquals("Second cue start time", expectedStart2, secondCue.startTimeMs)
        assertEquals("Second cue end time", expectedEnd2, secondCue.endTimeMs)
    }

    @Test
    fun `parse should handle VTT with style blocks`() = runSubtitleTest {
        val contentWithStyles = """
            WEBVTT
            
            STYLE
            ::cue {
              background-color: black;
              color: white;
            }
            
            00:00:01.000 --> 00:00:03.000
            Styled subtitle text
        """.trimIndent()
        
        val trackData = vttParser.parse(contentWithStyles)
        
        assertEquals("Should have 1 cue", 1, trackData.cues.size)
        assertEquals("Cue text", "Styled subtitle text", trackData.cues[0].text)
    }

    @Test
    fun `parse should handle invalid VTT timing format`() = runSubtitleTest {
        val invalidTiming = """
            WEBVTT
            
            invalid timing --> 00:00:03.000
            This should fail
        """.trimIndent()
        
        assertParsingFails(
            parser = { vttParser.parse(invalidTiming) },
            expectedMessagePattern = "Invalid VTT timing format"
        )
    }

    @Test
    fun `parse should handle VTT with empty cues`() = runSubtitleTest {
        val contentWithEmptyCues = """
            WEBVTT
            
            00:00:01.000 --> 00:00:03.000
            Valid subtitle
            
            00:00:04.000 --> 00:00:06.000
            
            
            00:00:07.000 --> 00:00:09.000
            Another valid subtitle
        """.trimIndent()
        
        val trackData = vttParser.parse(contentWithEmptyCues)
        
        assertEquals("Should have 2 cues (skip empty)", 2, trackData.cues.size)
        assertEquals("First cue text", "Valid subtitle", trackData.cues[0].text)
        assertEquals("Second cue text", "Another valid subtitle", trackData.cues[1].text)
    }

    @Test
    fun `parse should handle VTT with HTML formatting`() = runSubtitleTest {
        val contentWithFormatting = """
            WEBVTT
            
            00:00:01.000 --> 00:00:03.000
            <b>Bold text</b> and <i>italic text</i>
            
            00:00:04.000 --> 00:00:06.000
            <c.red>Red text</c> and <u>underlined</u>
        """.trimIndent()
        
        val trackData = vttParser.parse(contentWithFormatting)
        
        assertEquals("Should have 2 cues", 2, trackData.cues.size)
        assertEquals("First cue cleaned", "Bold text and italic text", trackData.cues[0].text)
        assertEquals("Second cue cleaned", "Red text and underlined", trackData.cues[1].text)
    }

    @Test
    fun `parse should handle line positioning with numbers`() = runSubtitleTest {
        val contentWithLineNumbers = """
            WEBVTT
            
            00:00:01.000 --> 00:00:03.000 line:5
            Text with line number positioning
            
            00:00:04.000 --> 00:00:06.000 line:-2
            Text with negative line number
        """.trimIndent()
        
        val trackData = vttParser.parse(contentWithLineNumbers)
        
        assertEquals("Should have 2 cues", 2, trackData.cues.size)
        
        val firstCue = trackData.cues[0]
        assertEquals("First cue line position", 0.25f, firstCue.line) // 5/20 = 0.25
        
        // Note: The parser should handle negative line numbers appropriately
        val secondCue = trackData.cues[1]
        assertNotNull("Second cue should have line position", secondCue.line)
    }

    @Test
    fun `parse should handle VTT with complex metadata`() = runSubtitleTest {
        val contentWithMetadata = """
            WEBVTT - This file has a description
            
            NOTE This is a complex VTT file
            NOTE with multiple metadata sections
            
            REGION
            id:speaker
            width:40%
            lines:3
            regionanchor:0%,100%
            viewportanchor:10%,90%
            scroll:up
            
            00:00:01.000 --> 00:00:03.000 region:speaker
            Text in a region
        """.trimIndent()
        
        val trackData = vttParser.parse(contentWithMetadata)
        
        assertEquals("Should have 1 cue", 1, trackData.cues.size)
        assertEquals("Cue text", "Text in a region", trackData.cues[0].text)
    }

    @Test
    fun `parse should handle malformed VTT timing arrows`() = runSubtitleTest {
        val malformedArrows = """
            WEBVTT
            
            00:00:01.000 -> 00:00:03.000
            Wrong arrow format
        """.trimIndent()
        
        assertParsingFails(
            parser = { vttParser.parse(malformedArrows) },
            expectedMessagePattern = "Invalid VTT timing format"
        )
    }

    @Test
    fun `parse should handle VTT with vertical writing modes`() = runSubtitleTest {
        val verticalContent = """
            WEBVTT
            
            00:00:01.000 --> 00:00:03.000 vertical:rl
            Right to left vertical text
            
            00:00:04.000 --> 00:00:06.000 vertical:lr
            Left to right vertical text
        """.trimIndent()
        
        val trackData = vttParser.parse(verticalContent)
        
        assertEquals("Should have 2 cues", 2, trackData.cues.size)
        assertEquals("First cue vertical RL", 1, trackData.cues[0].verticalType)
        assertEquals("Second cue vertical LR", 2, trackData.cues[1].verticalType)
    }

    @Test
    fun `parse should sort cues by start time`() = runSubtitleTest {
        val outOfOrderVtt = """
            WEBVTT
            
            00:00:07.000 --> 00:00:09.000
            Third subtitle
            
            00:00:01.000 --> 00:00:03.000
            First subtitle
            
            00:00:04.000 --> 00:00:06.000
            Second subtitle
        """.trimIndent()
        
        val trackData = vttParser.parse(outOfOrderVtt)
        
        assertEquals("Should have 3 cues", 3, trackData.cues.size)
        assertEquals("First cue should be earliest", 1000L, trackData.cues[0].startTimeMs)
        assertEquals("Second cue should be middle", 4000L, trackData.cues[1].startTimeMs)
        assertEquals("Third cue should be latest", 7000L, trackData.cues[2].startTimeMs)
    }

    @Test
    fun `getSupportedFormats should return VTT format`() {
        val formats = vttParser.getSupportedFormats()
        assertEquals("Should support only VTT", listOf(SubtitleFormat.VTT), formats)
    }

    @Test
    fun `supportsFormat should return true for VTT only`() {
        assertTrue("Should support VTT", vttParser.supportsFormat(SubtitleFormat.VTT))
        assertFalse("Should not support SRT", vttParser.supportsFormat(SubtitleFormat.SRT))
        assertFalse("Should not support ASS", vttParser.supportsFormat(SubtitleFormat.ASS))
        assertFalse("Should not support SSA", vttParser.supportsFormat(SubtitleFormat.SSA))
    }

    @Test
    fun `parse should handle VTT with WEBVTT signature variations`() = runSubtitleTest {
        val contentWithDescription = """
            WEBVTT - English subtitles
            
            00:00:01.000 --> 00:00:03.000
            Subtitle with description in signature
        """.trimIndent()
        
        val trackData = vttParser.parse(contentWithDescription)
        
        assertEquals("Should have 1 cue", 1, trackData.cues.size)
        assertEquals("Cue text", "Subtitle with description in signature", trackData.cues[0].text)
    }

    @Test
    fun `parse should handle zero duration cues`() = runSubtitleTest {
        val contentWithZeroDuration = """
            WEBVTT
            
            00:00:01.000 --> 00:00:01.000
            Zero duration cue
        """.trimIndent()
        
        assertParsingFails(
            parser = { vttParser.parse(contentWithZeroDuration) },
            expectedMessagePattern = "End time must be after start time"
        )
    }

    @Test
    fun `parse should handle cues with multiple line breaks`() = runSubtitleTest {
        val contentWithLineBreaks = """
            WEBVTT
            
            00:00:01.000 --> 00:00:05.000
            Line one
            Line two
            Line three
            Line four
        """.trimIndent()
        
        val trackData = vttParser.parse(contentWithLineBreaks)
        
        assertEquals("Should have 1 cue", 1, trackData.cues.size)
        assertEquals("Multi-line cue", "Line one\nLine two\nLine three\nLine four", trackData.cues[0].text)
    }
}