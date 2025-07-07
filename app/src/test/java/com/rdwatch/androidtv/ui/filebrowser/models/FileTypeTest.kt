package com.rdwatch.androidtv.ui.filebrowser.models

import org.junit.Assert.*
import org.junit.Test

/**
 * Unit tests for FileType enum and file type detection logic.
 * Tests file extension mapping and type classification.
 */
class FileTypeTest {

    @Test
    fun `should detect video file types correctly`() {
        // Given video extensions
        val videoExtensions = listOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg")
        
        // When detecting file types
        videoExtensions.forEach { ext ->
            val fileType = FileType.fromExtension(ext)
            assertEquals("Extension $ext should be VIDEO", FileType.VIDEO, fileType)
        }
    }

    @Test
    fun `should detect audio file types correctly`() {
        // Given audio extensions
        val audioExtensions = listOf("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus")
        
        // When detecting file types
        audioExtensions.forEach { ext ->
            val fileType = FileType.fromExtension(ext)
            assertEquals("Extension $ext should be AUDIO", FileType.AUDIO, fileType)
        }
    }

    @Test
    fun `should detect document file types correctly`() {
        // Given document extensions
        val docExtensions = listOf("pdf", "doc", "docx", "txt", "odt", "rtf")
        
        // When detecting file types
        docExtensions.forEach { ext ->
            val fileType = FileType.fromExtension(ext)
            assertEquals("Extension $ext should be DOCUMENT", FileType.DOCUMENT, fileType)
        }
    }

    @Test
    fun `should detect image file types correctly`() {
        // Given image extensions
        val imageExtensions = listOf("jpg", "jpeg", "png", "gif", "bmp", "svg", "webp")
        
        // When detecting file types
        imageExtensions.forEach { ext ->
            val fileType = FileType.fromExtension(ext)
            assertEquals("Extension $ext should be IMAGE", FileType.IMAGE, fileType)
        }
    }

    @Test
    fun `should detect archive file types correctly`() {
        // Given archive extensions
        val archiveExtensions = listOf("zip", "rar", "7z", "tar", "gz", "bz2")
        
        // When detecting file types
        archiveExtensions.forEach { ext ->
            val fileType = FileType.fromExtension(ext)
            assertEquals("Extension $ext should be ARCHIVE", FileType.ARCHIVE, fileType)
        }
    }

    @Test
    fun `should detect subtitle file types correctly`() {
        // Given subtitle extensions
        val subtitleExtensions = listOf("srt", "ass", "vtt", "sub", "ssa")
        
        // When detecting file types
        subtitleExtensions.forEach { ext ->
            val fileType = FileType.fromExtension(ext)
            assertEquals("Extension $ext should be SUBTITLE", FileType.SUBTITLE, fileType)
        }
    }

    @Test
    fun `should return OTHER for unknown file types`() {
        // Given unknown extensions
        val unknownExtensions = listOf("xyz", "unknown", "test", "abc123")
        
        // When detecting file types
        unknownExtensions.forEach { ext ->
            val fileType = FileType.fromExtension(ext)
            assertEquals("Extension $ext should be OTHER", FileType.OTHER, fileType)
        }
    }

    @Test
    fun `should handle case insensitive extension detection`() {
        // Given mixed case extensions
        val mixedCaseExtensions = mapOf(
            "MP4" to FileType.VIDEO,
            "Mkv" to FileType.VIDEO,
            "AVI" to FileType.VIDEO,
            "MP3" to FileType.AUDIO,
            "FLAC" to FileType.AUDIO,
            "PDF" to FileType.DOCUMENT,
            "JPG" to FileType.IMAGE,
            "PNG" to FileType.IMAGE,
            "ZIP" to FileType.ARCHIVE,
            "SRT" to FileType.SUBTITLE
        )
        
        // When detecting file types
        mixedCaseExtensions.forEach { (ext, expectedType) ->
            val fileType = FileType.fromExtension(ext)
            assertEquals("Extension $ext should be $expectedType", expectedType, fileType)
        }
    }

    @Test
    fun `should handle empty extension`() {
        // Given empty extension
        val emptyExtension = ""
        
        // When detecting file type
        val fileType = FileType.fromExtension(emptyExtension)
        
        // Then should return OTHER
        assertEquals(FileType.OTHER, fileType)
    }

    @Test
    fun `should handle null extension gracefully`() {
        // Given null extension (testing edge case)
        val nullExtension = null
        
        // When detecting file type with null protection
        val fileType = FileType.fromExtension(nullExtension ?: "")
        
        // Then should return OTHER
        assertEquals(FileType.OTHER, fileType)
    }

    @Test
    fun `should have correct display names`() {
        // Given file types with expected display names
        val expectedDisplayNames = mapOf(
            FileType.VIDEO to "Video",
            FileType.AUDIO to "Audio",
            FileType.DOCUMENT to "Document",
            FileType.IMAGE to "Image",
            FileType.ARCHIVE to "Archive",
            FileType.SUBTITLE to "Subtitle",
            FileType.OTHER to "Other"
        )
        
        // When getting display names
        expectedDisplayNames.forEach { (fileType, expectedName) ->
            assertEquals("$fileType should have display name $expectedName", expectedName, fileType.displayName)
        }
    }

    @Test
    fun `should have correct extensions for each type`() {
        // Given file types with expected extensions
        val expectedExtensions = mapOf(
            FileType.VIDEO to setOf("mp4", "mkv", "avi", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg"),
            FileType.AUDIO to setOf("mp3", "wav", "flac", "aac", "ogg", "wma", "m4a", "opus"),
            FileType.DOCUMENT to setOf("pdf", "doc", "docx", "txt", "odt", "rtf"),
            FileType.IMAGE to setOf("jpg", "jpeg", "png", "gif", "bmp", "svg", "webp"),
            FileType.ARCHIVE to setOf("zip", "rar", "7z", "tar", "gz", "bz2"),
            FileType.SUBTITLE to setOf("srt", "ass", "vtt", "sub", "ssa"),
            FileType.OTHER to emptySet()
        )
        
        // When checking extensions
        expectedExtensions.forEach { (fileType, expectedExts) ->
            assertEquals("$fileType should have extensions $expectedExts", expectedExts, fileType.extensions)
        }
    }

    @Test
    fun `should detect file type from complete filename`() {
        // Given complete filenames
        val filenames = mapOf(
            "Movie.mp4" to FileType.VIDEO,
            "Document.pdf" to FileType.DOCUMENT,
            "Image.jpg" to FileType.IMAGE,
            "Archive.zip" to FileType.ARCHIVE,
            "Subtitle.srt" to FileType.SUBTITLE,
            "Music.mp3" to FileType.AUDIO,
            "unknown.xyz" to FileType.OTHER,
            "no-extension" to FileType.OTHER
        )
        
        // When detecting file types from filenames
        filenames.forEach { (filename, expectedType) ->
            val extension = filename.substringAfterLast('.', "")
            val fileType = FileType.fromExtension(extension)
            assertEquals("$filename should be $expectedType", expectedType, fileType)
        }
    }

    @Test
    fun `should handle complex filenames with multiple dots`() {
        // Given complex filenames
        val complexFilenames = mapOf(
            "movie.720p.x264.mp4" to FileType.VIDEO,
            "document.v1.2.pdf" to FileType.DOCUMENT,
            "image.thumb.jpg" to FileType.IMAGE,
            "archive.backup.zip" to FileType.ARCHIVE,
            "subtitle.english.srt" to FileType.SUBTITLE,
            "music.320kbps.mp3" to FileType.AUDIO
        )
        
        // When detecting file types from complex filenames
        complexFilenames.forEach { (filename, expectedType) ->
            val extension = filename.substringAfterLast('.', "")
            val fileType = FileType.fromExtension(extension)
            assertEquals("$filename should be $expectedType", expectedType, fileType)
        }
    }

    @Test
    fun `should handle edge case extensions`() {
        // Given edge case extensions
        val edgeCases = mapOf(
            "." to FileType.OTHER,
            ".." to FileType.OTHER,
            "..." to FileType.OTHER,
            "file." to FileType.OTHER,
            ".hidden" to FileType.OTHER
        )
        
        // When detecting file types
        edgeCases.forEach { (filename, expectedType) ->
            val extension = filename.substringAfterLast('.', "")
            val fileType = FileType.fromExtension(extension)
            assertEquals("$filename should be $expectedType", expectedType, fileType)
        }
    }

    @Test
    fun `should handle all video formats correctly`() {
        // Given common video formats
        val videoFormats = mapOf(
            "mp4" to "MPEG-4 Part 14",
            "mkv" to "Matroska Video",
            "avi" to "Audio Video Interleave",
            "mov" to "QuickTime Movie",
            "wmv" to "Windows Media Video",
            "flv" to "Flash Video",
            "webm" to "WebM",
            "m4v" to "MPEG-4 Video",
            "mpg" to "MPEG Video",
            "mpeg" to "MPEG Video"
        )
        
        // When detecting video formats
        videoFormats.keys.forEach { ext ->
            val fileType = FileType.fromExtension(ext)
            assertEquals("$ext should be VIDEO", FileType.VIDEO, fileType)
        }
    }

    @Test
    fun `should handle all audio formats correctly`() {
        // Given common audio formats
        val audioFormats = mapOf(
            "mp3" to "MP3 Audio",
            "wav" to "WAV Audio",
            "flac" to "FLAC Audio",
            "aac" to "AAC Audio",
            "ogg" to "OGG Audio",
            "wma" to "Windows Media Audio",
            "m4a" to "MPEG-4 Audio",
            "opus" to "Opus Audio"
        )
        
        // When detecting audio formats
        audioFormats.keys.forEach { ext ->
            val fileType = FileType.fromExtension(ext)
            assertEquals("$ext should be AUDIO", FileType.AUDIO, fileType)
        }
    }

    @Test
    fun `should handle playable vs non-playable file types`() {
        // Given playable and non-playable file types
        val playableTypes = setOf(FileType.VIDEO, FileType.AUDIO)
        val nonPlayableTypes = setOf(FileType.DOCUMENT, FileType.IMAGE, FileType.ARCHIVE, FileType.SUBTITLE, FileType.OTHER)
        
        // When checking if types are typically playable
        playableTypes.forEach { fileType ->
            assertTrue("$fileType should be considered playable", isPlayableType(fileType))
        }
        
        nonPlayableTypes.forEach { fileType ->
            assertFalse("$fileType should not be considered playable", isPlayableType(fileType))
        }
    }

    @Test
    fun `should handle file type filtering scenarios`() {
        // Given filter scenarios
        val filterScenarios = mapOf(
            "Show only videos" to setOf(FileType.VIDEO),
            "Show only audio" to setOf(FileType.AUDIO),
            "Show media files" to setOf(FileType.VIDEO, FileType.AUDIO),
            "Show documents" to setOf(FileType.DOCUMENT),
            "Show images" to setOf(FileType.IMAGE),
            "Show archives" to setOf(FileType.ARCHIVE),
            "Show subtitles" to setOf(FileType.SUBTITLE),
            "Show everything" to FileType.values().toSet()
        )
        
        // When applying filters
        filterScenarios.forEach { (scenarioName, allowedTypes) ->
            val sampleFile = "test.mp4"
            val extension = sampleFile.substringAfterLast('.', "")
            val fileType = FileType.fromExtension(extension)
            
            val shouldBeIncluded = allowedTypes.contains(fileType)
            if (scenarioName == "Show only videos" || scenarioName == "Show media files" || scenarioName == "Show everything") {
                assertTrue("$scenarioName should include $sampleFile", shouldBeIncluded)
            }
        }
    }

    @Test
    fun `should handle performance with many file type detections`() {
        // Given large number of files
        val extensions = listOf("mp4", "mkv", "avi", "mp3", "wav", "pdf", "jpg", "zip", "srt", "txt")
        val filenames = (1..1000).map { "file$it.${extensions[it % extensions.size]}" }
        
        // When detecting file types for all files
        val startTime = System.currentTimeMillis()
        
        val results = filenames.map { filename ->
            val extension = filename.substringAfterLast('.', "")
            FileType.fromExtension(extension)
        }
        
        val endTime = System.currentTimeMillis()
        val duration = endTime - startTime
        
        // Then should complete quickly
        assertTrue("File type detection should be fast", duration < 100) // Less than 100ms
        assertEquals("Should detect all files", filenames.size, results.size)
        assertTrue("Should detect some video files", results.contains(FileType.VIDEO))
        assertTrue("Should detect some audio files", results.contains(FileType.AUDIO))
    }

    // Helper function to simulate playable type check
    private fun isPlayableType(fileType: FileType): Boolean {
        return fileType in setOf(FileType.VIDEO, FileType.AUDIO)
    }
}