package com.rdwatch.androidtv.player.subtitle.parser

import com.rdwatch.androidtv.player.subtitle.SubtitleFormat
import com.rdwatch.androidtv.player.subtitle.test.SubtitleTestBase
import com.rdwatch.androidtv.player.subtitle.test.SubtitleTestData
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import java.io.ByteArrayInputStream
import javax.inject.Inject

@HiltAndroidTest
class SubtitleParserFactoryTest : SubtitleTestBase() {

    @Inject
    lateinit var factory: SubtitleParserFactory

    @Before
    override fun setUp() {
        super.setUp()
    }

    @Test
    fun `parseSubtitle should detect SRT format automatically`() = runSubtitleTest {
        val result = factory.parseSubtitle(SubtitleTestData.VALID_SRT_CONTENT)
        
        assertTrue("Should parse successfully", result is SubtitleParseResult.Success)
        val trackData = result.getOrThrow()
        assertEquals("Should detect SRT format", SubtitleFormat.SRT, trackData.format)
        assertEquals("Should have 3 cues", 3, trackData.cues.size)
    }

    @Test
    fun `parseSubtitle should detect VTT format automatically`() = runSubtitleTest {
        val result = factory.parseSubtitle(SubtitleTestData.VALID_VTT_CONTENT)
        
        assertTrue("Should parse successfully", result is SubtitleParseResult.Success)
        val trackData = result.getOrThrow()
        assertEquals("Should detect VTT format", SubtitleFormat.VTT, trackData.format)
        assertEquals("Should have 3 cues", 3, trackData.cues.size)
    }

    @Test
    fun `parseSubtitle should detect ASS format automatically`() = runSubtitleTest {
        val result = factory.parseSubtitle(SubtitleTestData.VALID_ASS_CONTENT)
        
        assertTrue("Should parse successfully", result is SubtitleParseResult.Success)
        val trackData = result.getOrThrow()
        assertEquals("Should detect ASS format", SubtitleFormat.ASS, trackData.format)
        assertEquals("Should have 3 cues", 3, trackData.cues.size)
    }

    @Test
    fun `parseSubtitle should use provided format when specified`() = runSubtitleTest {
        val result = factory.parseSubtitle(SubtitleTestData.VALID_SRT_CONTENT, SubtitleFormat.SRT)
        
        assertTrue("Should parse successfully", result is SubtitleParseResult.Success)
        val trackData = result.getOrThrow()
        assertEquals("Should use specified format", SubtitleFormat.SRT, trackData.format)
    }

    @Test
    fun `parseSubtitle should return error for unsupported format`() = runSubtitleTest {
        val result = factory.parseSubtitle(SubtitleTestData.VALID_SRT_CONTENT, SubtitleFormat.TTML)
        
        assertTrue("Should return error", result is SubtitleParseResult.Error)
        val error = result as SubtitleParseResult.Error
        assertTrue("Error should mention unsupported format", 
            error.exception.message?.contains("No parser available") == true)
    }

    @Test
    fun `parseSubtitle should return error for unknown format`() = runSubtitleTest {
        val unknownContent = """
            This is not a valid subtitle file
            It has no recognizable format
        """.trimIndent()
        
        val result = factory.parseSubtitle(unknownContent)
        
        assertTrue("Should return error", result is SubtitleParseResult.Error)
    }

    @Test
    fun `parseSubtitle should handle malformed content gracefully`() = runSubtitleTest {
        val result = factory.parseSubtitle(SubtitleTestData.MALFORMED_SRT_CONTENT)
        
        assertTrue("Should return error", result is SubtitleParseResult.Error)
        val error = result as SubtitleParseResult.Error
        assertNotNull("Error should have exception", error.exception)
    }

    @Test
    fun `parseSubtitle from InputStream should work correctly`() = runSubtitleTest {
        val inputStream = ByteArrayInputStream(SubtitleTestData.VALID_SRT_CONTENT.toByteArray())
        val result = factory.parseSubtitle(inputStream)
        
        assertTrue("Should parse successfully", result is SubtitleParseResult.Success)
        val trackData = result.getOrThrow()
        assertEquals("Should detect SRT format", SubtitleFormat.SRT, trackData.format)
        assertEquals("Should have 3 cues", 3, trackData.cues.size)
    }

    @Test
    fun `parseSubtitle should handle different encodings`() = runSubtitleTest {
        val result = factory.parseSubtitle(SubtitleTestData.UNICODE_SRT_CONTENT, encoding = "UTF-8")
        
        assertTrue("Should parse successfully", result is SubtitleParseResult.Success)
        val trackData = result.getOrThrow()
        assertEquals("Should have 2 cues", 2, trackData.cues.size)
        assertEquals("Should preserve Unicode", "Héllo wörld! 你好世界 🎬", trackData.cues[0].text)
    }

    @Test
    fun `getParserForFormat should return correct parser`() {
        val srtParser = factory.getParserForFormat(SubtitleFormat.SRT)
        val vttParser = factory.getParserForFormat(SubtitleFormat.VTT)
        val assParser = factory.getParserForFormat(SubtitleFormat.ASS)
        val ssaParser = factory.getParserForFormat(SubtitleFormat.SSA)
        val unknownParser = factory.getParserForFormat(SubtitleFormat.UNKNOWN)
        
        assertNotNull("Should have SRT parser", srtParser)
        assertNotNull("Should have VTT parser", vttParser)
        assertNotNull("Should have ASS parser", assParser)
        assertNotNull("Should have SSA parser", ssaParser)
        assertNull("Should not have UNKNOWN parser", unknownParser)
        
        assertTrue("SRT parser should support SRT", srtParser?.supportsFormat(SubtitleFormat.SRT) == true)
        assertTrue("VTT parser should support VTT", vttParser?.supportsFormat(SubtitleFormat.VTT) == true)
        assertTrue("ASS parser should support ASS", assParser?.supportsFormat(SubtitleFormat.ASS) == true)
        assertTrue("SSA parser should support SSA", ssaParser?.supportsFormat(SubtitleFormat.SSA) == true)
    }

    @Test
    fun `getSupportedFormats should return all supported formats`() {
        val formats = factory.getSupportedFormats()
        
        assertTrue("Should support SRT", formats.contains(SubtitleFormat.SRT))
        assertTrue("Should support VTT", formats.contains(SubtitleFormat.VTT))
        assertTrue("Should support ASS", formats.contains(SubtitleFormat.ASS))
        assertTrue("Should support SSA", formats.contains(SubtitleFormat.SSA))
        
        assertFalse("Should not support UNKNOWN", formats.contains(SubtitleFormat.UNKNOWN))
        assertFalse("Should not support TTML", formats.contains(SubtitleFormat.TTML))
    }

    @Test
    fun `isFormatSupported should return correct results`() {
        assertTrue("Should support SRT", factory.isFormatSupported(SubtitleFormat.SRT))
        assertTrue("Should support VTT", factory.isFormatSupported(SubtitleFormat.VTT))
        assertTrue("Should support ASS", factory.isFormatSupported(SubtitleFormat.ASS))
        assertTrue("Should support SSA", factory.isFormatSupported(SubtitleFormat.SSA))
        
        assertFalse("Should not support UNKNOWN", factory.isFormatSupported(SubtitleFormat.UNKNOWN))
        assertFalse("Should not support TTML", factory.isFormatSupported(SubtitleFormat.TTML))
    }

    @Test
    fun `validateSubtitle should return valid result for correct content`() = runSubtitleTest {
        val result = factory.validateSubtitle(SubtitleTestData.VALID_SRT_CONTENT)
        
        assertTrue("Should be valid", result is SubtitleValidationResult.Valid)
        val validResult = result as SubtitleValidationResult.Valid
        assertEquals("Should detect SRT format", SubtitleFormat.SRT, validResult.format)
        assertEquals("Should have 3 cues", 3, validResult.cueCount)
        assertTrue("Should have positive duration", validResult.duration > 0)
    }

    @Test
    fun `validateSubtitle should return invalid result for malformed content`() = runSubtitleTest {
        val result = factory.validateSubtitle(SubtitleTestData.MALFORMED_SRT_CONTENT)
        
        assertTrue("Should be invalid", result is SubtitleValidationResult.Invalid)
        val invalidResult = result as SubtitleValidationResult.Invalid
        assertNotNull("Should have reason", invalidResult.reason)
        assertTrue("Reason should mention error", invalidResult.reason.isNotEmpty())
    }

    @Test
    fun `validateSubtitle should handle empty content`() = runSubtitleTest {
        val result = factory.validateSubtitle(SubtitleTestData.EMPTY_CONTENT)
        
        assertTrue("Should be invalid", result is SubtitleValidationResult.Invalid)
    }

    @Test
    fun `format detection should handle edge cases`() = runSubtitleTest {
        // Test SRT with minimal content
        val minimalSrt = """
            1
            00:00:01,000 --> 00:00:02,000
            Text
        """.trimIndent()
        
        val srtResult = factory.parseSubtitle(minimalSrt)
        assertTrue("Minimal SRT should parse", srtResult is SubtitleParseResult.Success)
        assertEquals("Should detect SRT", SubtitleFormat.SRT, srtResult.getOrThrow().format)
        
        // Test VTT with minimal content
        val minimalVtt = """
            WEBVTT
            
            00:00:01.000 --> 00:00:02.000
            Text
        """.trimIndent()
        
        val vttResult = factory.parseSubtitle(minimalVtt)
        assertTrue("Minimal VTT should parse", vttResult is SubtitleParseResult.Success)
        assertEquals("Should detect VTT", SubtitleFormat.VTT, vttResult.getOrThrow().format)
    }

    @Test
    fun `parseSubtitle should handle concurrent parsing`() = runSubtitleTest {
        // Test that multiple simultaneous parsing operations work correctly
        val results = listOf(
            factory.parseSubtitle(SubtitleTestData.VALID_SRT_CONTENT),
            factory.parseSubtitle(SubtitleTestData.VALID_VTT_CONTENT),
            factory.parseSubtitle(SubtitleTestData.VALID_ASS_CONTENT)
        )
        
        results.forEach { result ->
            assertTrue("All results should be successful", result is SubtitleParseResult.Success)
        }
        
        val formats = results.map { it.getOrThrow().format }
        assertTrue("Should have SRT", formats.contains(SubtitleFormat.SRT))
        assertTrue("Should have VTT", formats.contains(SubtitleFormat.VTT))
        assertTrue("Should have ASS", formats.contains(SubtitleFormat.ASS))
    }

    @Test
    fun `parseSubtitle should preserve encoding information`() = runSubtitleTest {
        val customEncoding = "ISO-8859-1"
        val result = factory.parseSubtitle(SubtitleTestData.VALID_SRT_CONTENT, encoding = customEncoding)
        
        assertTrue("Should parse successfully", result is SubtitleParseResult.Success)
        val trackData = result.getOrThrow()
        assertEquals("Should preserve encoding", customEncoding, trackData.encoding)
    }

    @Test
    fun `format detection should handle content with BOM`() = runSubtitleTest {
        // UTF-8 BOM + SRT content
        val bomSrtContent = "\uFEFF" + SubtitleTestData.VALID_SRT_CONTENT
        val result = factory.parseSubtitle(bomSrtContent)
        
        assertTrue("Should parse BOM content", result is SubtitleParseResult.Success)
        assertEquals("Should detect SRT despite BOM", SubtitleFormat.SRT, result.getOrThrow().format)
    }

    @Test
    fun `format detection should handle mixed line endings`() = runSubtitleTest {
        val mixedLineEndingSrt = SubtitleTestData.VALID_SRT_CONTENT
            .replace("\n", "\r\n") // Convert to Windows line endings
        
        val result = factory.parseSubtitle(mixedLineEndingSrt)
        
        assertTrue("Should parse mixed line endings", result is SubtitleParseResult.Success)
        assertEquals("Should detect SRT format", SubtitleFormat.SRT, result.getOrThrow().format)
    }

    @Test
    fun `error handling should provide meaningful messages`() = runSubtitleTest {
        val malformedContent = """
            This looks like it could be a subtitle file
            1
            But the timing is wrong: invalid --> also invalid
            Some text here
        """.trimIndent()
        
        val result = factory.parseSubtitle(malformedContent)
        
        assertTrue("Should return error", result is SubtitleParseResult.Error)
        val error = result as SubtitleParseResult.Error
        assertNotNull("Should have error message", error.exception.message)
        assertTrue("Message should be meaningful", error.exception.message!!.isNotEmpty())
    }

    @Test
    fun `parseSubtitle should handle very large files efficiently`() = runSubtitleTest {
        // Generate a large SRT file
        val largeContent = StringBuilder()
        for (i in 1..100) {
            largeContent.append("$i\n")
            largeContent.append("00:${String.format("%02d", i)}:00,000 --> 00:${String.format("%02d", i)}:02,000\n")
            largeContent.append("Subtitle number $i\n\n")
        }
        
        val result = factory.parseSubtitle(largeContent.toString())
        
        assertTrue("Should parse large file", result is SubtitleParseResult.Success)
        val trackData = result.getOrThrow()
        assertEquals("Should have 100 cues", 100, trackData.cues.size)
    }
}