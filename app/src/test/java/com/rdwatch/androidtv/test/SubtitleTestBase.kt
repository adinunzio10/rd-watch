package com.rdwatch.androidtv.player.subtitle.test

import com.rdwatch.androidtv.player.subtitle.SubtitleCue
import com.rdwatch.androidtv.player.subtitle.SubtitleFormat
import com.rdwatch.androidtv.player.subtitle.SubtitleTrackData
import com.rdwatch.androidtv.test.HiltTestBase
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before

/**
 * Base class for subtitle component tests.
 * Provides common utilities and test data for subtitle testing.
 */
@HiltAndroidTest
abstract class SubtitleTestBase : HiltTestBase() {
    @Before
    override fun setUp() {
        super.setUp()
    }

    /**
     * Create a simple test subtitle cue
     */
    protected fun createTestCue(
        startMs: Long = 1000L,
        endMs: Long = 3000L,
        text: String = "Test subtitle",
    ): SubtitleCue {
        return SubtitleCue(
            startTimeMs = startMs,
            endTimeMs = endMs,
            text = text,
        )
    }

    /**
     * Create test subtitle track data
     */
    protected fun createTestTrackData(
        cues: List<SubtitleCue> = listOf(createTestCue()),
        format: SubtitleFormat = SubtitleFormat.SRT,
        language: String? = "en",
        title: String? = "Test Track",
    ): SubtitleTrackData {
        return SubtitleTrackData(
            cues = cues,
            format = format,
            language = language,
            title = title,
        )
    }

    /**
     * Assert that two subtitle cues are equal
     */
    protected fun assertCueEquals(
        expected: SubtitleCue,
        actual: SubtitleCue,
    ) {
        assertEquals("Start time mismatch", expected.startTimeMs, actual.startTimeMs)
        assertEquals("End time mismatch", expected.endTimeMs, actual.endTimeMs)
        assertEquals("Text mismatch", expected.text, actual.text)
        assertEquals("Position mismatch", expected.position, actual.position)
        assertEquals("Line mismatch", expected.line, actual.line)
        assertEquals("Size mismatch", expected.size, actual.size)
        assertEquals("Text alignment mismatch", expected.textAlignment, actual.textAlignment)
        assertEquals("Vertical type mismatch", expected.verticalType, actual.verticalType)
        assertEquals("Window color mismatch", expected.windowColor, actual.windowColor)
        assertEquals("Text color mismatch", expected.textColor, actual.textColor)
        assertEquals("Background color mismatch", expected.backgroundColor, actual.backgroundColor)
    }

    /**
     * Assert that two subtitle track data objects are equal
     */
    protected fun assertTrackDataEquals(
        expected: SubtitleTrackData,
        actual: SubtitleTrackData,
    ) {
        assertEquals("Format mismatch", expected.format, actual.format)
        assertEquals("Language mismatch", expected.language, actual.language)
        assertEquals("Title mismatch", expected.title, actual.title)
        assertEquals("Encoding mismatch", expected.encoding, actual.encoding)
        assertEquals("Cue count mismatch", expected.cues.size, actual.cues.size)

        expected.cues.zip(actual.cues).forEach { (expectedCue, actualCue) ->
            assertCueEquals(expectedCue, actualCue)
        }
    }

    /**
     * Assert that subtitle parsing throws expected exception
     */
    protected suspend fun assertParsingFails(
        parser: suspend () -> SubtitleTrackData,
        expectedMessagePattern: String? = null,
        expectedFormat: SubtitleFormat? = null,
        expectedLineNumber: Int? = null,
    ) {
        try {
            parser()
            fail("Expected SubtitleParsingException to be thrown")
        } catch (e: com.rdwatch.androidtv.player.subtitle.parser.SubtitleParsingException) {
            expectedMessagePattern?.let { pattern ->
                assertTrue(
                    "Exception message '${e.message}' does not match pattern '$pattern'",
                    e.message?.contains(pattern, ignoreCase = true) == true,
                )
            }
            expectedFormat?.let { format ->
                assertEquals("Exception format mismatch", format, e.format)
            }
            expectedLineNumber?.let { lineNumber ->
                assertEquals("Exception line number mismatch", lineNumber, e.lineNumber)
            }
        } catch (e: Exception) {
            fail("Expected SubtitleParsingException but got ${e::class.simpleName}: ${e.message}")
        }
    }

    /**
     * Run test with coroutines
     */
    protected fun runSubtitleTest(testBody: suspend () -> Unit) =
        runTest {
            testBody()
        }
}

/**
 * Test data factory for creating subtitle content
 */
object SubtitleTestData {
    /**
     * Valid SRT content for testing
     */
    val VALID_SRT_CONTENT =
        """
        1
        00:00:01,000 --> 00:00:03,000
        First subtitle line
        
        2
        00:00:04,000 --> 00:00:06,000
        Second subtitle line
        with multiple lines
        
        3
        00:00:07,500 --> 00:00:09,000
        Third subtitle with <b>bold</b> text
        """.trimIndent()

    /**
     * Valid VTT content for testing
     */
    val VALID_VTT_CONTENT =
        """
        WEBVTT
        
        NOTE This is a test VTT file
        
        00:00:01.000 --> 00:00:03.000
        First subtitle line
        
        00:00:04.000 --> 00:00:06.000
        Second subtitle line
        with multiple lines
        
        00:00:07.500 --> 00:00:09.000 position:50% line:85%
        Positioned subtitle text
        """.trimIndent()

    /**
     * Valid ASS content for testing
     */
    val VALID_ASS_CONTENT =
        """
        [Script Info]
        Title: Test ASS
        ScriptType: v4.00+
        
        [V4+ Styles]
        Format: Name, Fontname, Fontsize, PrimaryColour, SecondaryColour, OutlineColour, BackColour, Bold, Italic, Underline, StrikeOut, ScaleX, ScaleY, Spacing, Angle, BorderStyle, Outline, Shadow, Alignment, MarginL, MarginR, MarginV, Encoding
        Style: Default,Arial,20,&H00FFFFFF,&H000000FF,&H00000000,&H80000000,0,0,0,0,100,100,0,0,1,0,2,2,10,10,10,1
        
        [Events]
        Format: Layer, Start, End, Style, Name, MarginL, MarginR, MarginV, Effect, Text
        Dialogue: 0,0:00:01.00,0:00:03.00,Default,,0,0,0,,First subtitle line
        Dialogue: 0,0:00:04.00,0:00:06.00,Default,,0,0,0,,Second subtitle line\Nwith multiple lines
        Dialogue: 0,0:00:07.50,0:00:09.00,Default,,0,0,0,,{\b1}Bold text{\b0}
        """.trimIndent()

    /**
     * Malformed SRT content for error testing
     */
    val MALFORMED_SRT_CONTENT =
        """
        1
        invalid timing format
        This should fail
        
        2
        00:00:04,000 --> 00:00:06,000
        This line should work
        """.trimIndent()

    /**
     * SRT with invalid timing
     */
    val INVALID_TIMING_SRT =
        """
        1
        00:00:05,000 --> 00:00:03,000
        End time before start time
        """.trimIndent()

    /**
     * Empty subtitle content
     */
    val EMPTY_CONTENT = ""

    /**
     * Content with only whitespace
     */
    val WHITESPACE_CONTENT = "   \n\n   \t  \n  "

    /**
     * SRT with Unicode and special characters
     */
    val UNICODE_SRT_CONTENT =
        """
        1
        00:00:01,000 --> 00:00:03,000
        Héllo wörld! 你好世界 🎬
        
        2
        00:00:04,000 --> 00:00:06,000
        Special chars: "quotes" & <tags> [brackets]
        """.trimIndent()

    /**
     * SRT with complex formatting
     */
    val FORMATTED_SRT_CONTENT =
        """
        1
        00:00:01,000 --> 00:00:03,000
        <font color="red" size="20">Red text</font>
        
        2
        00:00:04,000 --> 00:00:06,000
        <b>Bold</b> and <i>italic</i> and <u>underlined</u>
        
        3
        00:00:07,000 --> 00:00:09,000
        Mixed <b><i>bold italic</i></b> formatting
        """.trimIndent()
}
