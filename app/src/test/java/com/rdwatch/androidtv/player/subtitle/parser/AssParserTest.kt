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
class AssParserTest : SubtitleTestBase() {

    @Inject
    lateinit var assParser: AssParser

    @Before
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun `parse should handle valid ASS content correctly`() = runSubtitleTest {
        val trackData = assParser.parse(SubtitleTestData.VALID_ASS_CONTENT)
        
        assertEquals("Should parse ASS format", SubtitleFormat.ASS, trackData.format)
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
        assertEquals("Third cue text", "Bold text", thirdCue.text)
    }

    @Test
    fun `parse should detect ASS format correctly`() = runSubtitleTest {
        val assContent = """
            [Script Info]
            Title: Test ASS
            ScriptType: v4.00+
            
            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
            Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,0,2,2,10,10,10,1
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Test dialogue
        """.trimIndent()
        
        val trackData = assParser.parse(assContent)
        assertEquals("Should detect ASS format", SubtitleFormat.ASS, trackData.format)
    }

    @Test
    fun `parse should detect SSA format correctly`() = runSubtitleTest {
        val ssaContent = """
            [Script Info]
            Title: Test SSA
            
            [V4 Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, TertiaryColour, BackColour, Bold, Italic, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, AlphaLevel, Encoding, AngleZ, AngleY, AngleX, Outline, Shadow
            Style: Default,Arial,20,16777215,255,0,0,0,0,0,0,0,2,10,10,10,0,1,0,0,0,0,0
            
            [Events]
            Format: Marked, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: Marked=0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Test dialogue
        """.trimIndent()
        
        val trackData = assParser.parse(ssaContent)
        assertEquals("Should detect SSA format", SubtitleFormat.SSA, trackData.format)
    }

    @Test
    fun `parse should handle ASS without Events section`() = runSubtitleTest {
        val contentWithoutEvents = """
            [Script Info]
            Title: Test ASS
            ScriptType: v4.00+
            
            [V4+ Styles]
            Format: Name, Fontname, Fontsize
            Style: Default,Arial,20
        """.trimIndent()
        
        assertParsingFails(
            parser = { assParser.parse(contentWithoutEvents) },
            expectedMessagePattern = "No \\[Events\\] section found"
        )
    }

    @Test
    fun `parse should handle ASS time format correctly`() = runSubtitleTest {
        val content = """
            [Script Info]
            Title: Time Test
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,1:23:45.67,1:23:47.89,Default,,0,0,0,,Long time format
            Dialogue: 0,0:01:23.45,0:01:25.67,Default,,0,0,0,,Standard time format
        """.trimIndent()
        
        val trackData = assParser.parse(content)
        
        assertEquals("Should have 2 cues", 2, trackData.cues.size)
        
        val firstCue = trackData.cues[0]
        val expectedStart = (1 * 3600 + 23 * 60 + 45) * 1000L + 670
        val expectedEnd = (1 * 3600 + 23 * 60 + 47) * 1000L + 890
        assertEquals("First cue start time", expectedStart, firstCue.startTimeMs)
        assertEquals("First cue end time", expectedEnd, firstCue.endTimeMs)
        
        val secondCue = trackData.cues[1]
        val expectedStart2 = (1 * 60 + 23) * 1000L + 450
        val expectedEnd2 = (1 * 60 + 25) * 1000L + 670
        assertEquals("Second cue start time", expectedStart2, secondCue.startTimeMs)
        assertEquals("Second cue end time", expectedEnd2, secondCue.endTimeMs)
    }

    @Test
    fun `parse should clean ASS formatting tags`() = runSubtitleTest {
        val content = """
            [Script Info]
            Title: Formatting Test
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,{\\b1}Bold text{\\b0}
            Dialogue: 0,0:00:04.00,0:00:06.00,Default,,0,0,0,,{\\i1}Italic text{\\i0}
            Dialogue: 0,0:00:07.00,0:00:09.00,Default,,0,0,0,,{\\c&HFF0000&}Colored text{\\c}
            Dialogue: 0,0:00:10.00,0:00:12.00,Default,,0,0,0,,{\\pos(100,200)}Positioned text
        """.trimIndent()
        
        val trackData = assParser.parse(content)
        
        assertEquals("Should have 4 cues", 4, trackData.cues.size)
        assertEquals("Bold text cleaned", "Bold text", trackData.cues[0].text)
        assertEquals("Italic text cleaned", "Italic text", trackData.cues[1].text)
        assertEquals("Colored text cleaned", "Colored text", trackData.cues[2].text)
        assertEquals("Positioned text cleaned", "Positioned text", trackData.cues[3].text)
    }

    @Test
    fun `parse should handle ASS line breaks correctly`() = runSubtitleTest {
        val content = """
            [Script Info]
            Title: Line Break Test
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Line one\NLine two
            Dialogue: 0,0:00:04.00,0:00:06.00,Default,,0,0,0,,Line one\nLine two
            Dialogue: 0,0:00:07.00,0:00:09.00,Default,,0,0,0,,Line one\hhard space
        """.trimIndent()
        
        val trackData = assParser.parse(content)
        
        assertEquals("Should have 3 cues", 3, trackData.cues.size)
        assertEquals("\\N converted to newline", "Line one\nLine two", trackData.cues[0].text)
        assertEquals("\\n converted to newline", "Line one\nLine two", trackData.cues[1].text)
        assertEquals("\\h converted to space", "Line one hard space", trackData.cues[2].text)
    }

    @Test
    fun `parse should handle Comment lines correctly`() = runSubtitleTest {
        val content = """
            [Script Info]
            Title: Comment Test
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Regular dialogue
            Comment: 0,0:00:02.00,0:00:04.00,Default,,0,0,0,,This is a comment
            Dialogue: 0,0:00:05.00,0:00:07.00,Default,,0,0,0,,Another dialogue
        """.trimIndent()
        
        val trackData = assParser.parse(content)
        
        // Comments should be ignored, only dialogue lines should be parsed
        assertEquals("Should have 2 cues (comments ignored)", 2, trackData.cues.size)
        assertEquals("First dialogue", "Regular dialogue", trackData.cues[0].text)
        assertEquals("Second dialogue", "Another dialogue", trackData.cues[1].text)
    }

    @Test
    fun `parse should handle styles with colors`() = runSubtitleTest {
        val content = """
            [Script Info]
            Title: Style Test
            
            [V4+ Styles]
            Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
            Style: Default,Arial,20,&H00FF0000,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,0,2,2,10,10,10,1
            Style: RedText,Arial,18,&H000000FF,&H00FFFFFF,&H00000000,&H80000000,1,0,0,0,100,100,0,0,1,0,2,2,10,10,10,1
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Default style text
            Dialogue: 0,0:00:04.00,0:00:06.00,RedText,,0,0,0,,Red style text
        """.trimIndent()
        
        val trackData = assParser.parse(content)
        
        assertEquals("Should have 2 cues", 2, trackData.cues.size)
        
        val firstCue = trackData.cues[0]
        assertNotNull("First cue should have text color", firstCue.textColor)
        
        val secondCue = trackData.cues[1]
        assertNotNull("Second cue should have text color", secondCue.textColor)
    }

    @Test
    fun `parse should handle invalid ASS time format gracefully`() = runSubtitleTest {
        val content = """
            [Script Info]
            Title: Invalid Time Test
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,invalid:time,0:00:03.00,Default,,0,0,0,,This should be skipped
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,This should work
        """.trimIndent()
        
        val trackData = assParser.parse(content)
        
        // Invalid lines should be skipped
        assertEquals("Should have 1 cue (invalid line skipped)", 1, trackData.cues.size)
        assertEquals("Valid cue text", "This should work", trackData.cues[0].text)
    }

    @Test
    fun `parse should handle empty dialogue text`() = runSubtitleTest {
        val content = """
            [Script Info]
            Title: Empty Text Test
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,
            Dialogue: 0,0:00:04.00,0:00:06.00,Default,,0,0,0,,   
            Dialogue: 0,0:00:07.00,0:00:09.00,Default,,0,0,0,,Valid text
        """.trimIndent()
        
        val trackData = assParser.parse(content)
        
        // Empty text lines should be skipped
        assertEquals("Should have 1 cue (empty lines skipped)", 1, trackData.cues.size)
        assertEquals("Valid cue text", "Valid text", trackData.cues[0].text)
    }

    @Test
    fun `parse should handle dialogue with commas in text`() = runSubtitleTest {
        val content = """
            [Script Info]
            Title: Comma Test
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Text with, commas, in it
        """.trimIndent()
        
        val trackData = assParser.parse(content)
        
        assertEquals("Should have 1 cue", 1, trackData.cues.size)
        assertEquals("Text with commas", "Text with, commas, in it", trackData.cues[0].text)
    }

    @Test
    fun `parse should handle invalid timing order`() = runSubtitleTest {
        val content = """
            [Script Info]
            Title: Invalid Timing Test
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:05.00,0:00:03.00,Default,,0,0,0,,End before start
            Dialogue: 0,0:00:01.00,0:00:02.00,Default,,0,0,0,,Valid timing
        """.trimIndent()
        
        val trackData = assParser.parse(content)
        
        // Invalid timing should be skipped
        assertEquals("Should have 1 cue (invalid timing skipped)", 1, trackData.cues.size)
        assertEquals("Valid cue text", "Valid timing", trackData.cues[0].text)
    }

    @Test
    fun `parse should handle section parsing with missing format lines`() = runSubtitleTest {
        val content = """
            [Script Info]
            Title: Missing Format Test
            
            [Events]
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Should be ignored
        """.trimIndent()
        
        val trackData = assParser.parse(content)
        
        // Without Format line, dialogue should be ignored
        assertEquals("Should have 0 cues (no format)", 0, trackData.cues.size)
    }

    @Test
    fun `parse should sort cues by start time`() = runSubtitleTest {
        val content = """
            [Script Info]
            Title: Sort Test
            
            [Events]
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            Dialogue: 0,0:00:07.00,0:00:09.00,Default,,0,0,0,,Third subtitle
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,First subtitle
            Dialogue: 0,0:00:04.00,0:00:06.00,Default,,0,0,0,,Second subtitle
        """.trimIndent()
        
        val trackData = assParser.parse(content)
        
        assertEquals("Should have 3 cues", 3, trackData.cues.size)
        assertEquals("First cue should be earliest", 1000L, trackData.cues[0].startTimeMs)
        assertEquals("First cue text", "First subtitle", trackData.cues[0].text)
        assertEquals("Second cue should be middle", 4000L, trackData.cues[1].startTimeMs)
        assertEquals("Second cue text", "Second subtitle", trackData.cues[1].text)
        assertEquals("Third cue should be latest", 7000L, trackData.cues[2].startTimeMs)
        assertEquals("Third cue text", "Third subtitle", trackData.cues[2].text)
    }

    @Test
    fun `getSupportedFormats should return ASS and SSA formats`() {
        val formats = assParser.getSupportedFormats()
        assertEquals("Should support ASS and SSA", listOf(SubtitleFormat.ASS, SubtitleFormat.SSA), formats)
    }

    @Test
    fun `supportsFormat should return true for ASS and SSA only`() {
        assertTrue("Should support ASS", assParser.supportsFormat(SubtitleFormat.ASS))
        assertTrue("Should support SSA", assParser.supportsFormat(SubtitleFormat.SSA))
        assertFalse("Should not support SRT", assParser.supportsFormat(SubtitleFormat.SRT))
        assertFalse("Should not support VTT", assParser.supportsFormat(SubtitleFormat.VTT))
    }

    @Test
    fun `parse should handle sections with comments and empty lines`() = runSubtitleTest {
        val content = """
            [Script Info]
            ; This is a comment
            Title: Comment Test
            
            ; Another comment
            
            [Events]
            ; Event comment
            Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
            ; Dialogue comment
            Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,Test dialogue
        """.trimIndent()
        
        val trackData = assParser.parse(content)
        
        assertEquals("Should have 1 cue", 1, trackData.cues.size)
        assertEquals("Cue text", "Test dialogue", trackData.cues[0].text)
    }
}