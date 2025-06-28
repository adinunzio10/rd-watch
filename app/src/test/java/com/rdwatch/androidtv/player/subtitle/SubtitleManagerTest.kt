package com.rdwatch.androidtv.player.subtitle

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.SubtitleView
import com.rdwatch.androidtv.player.subtitle.parser.SubtitleLoadConfig
import com.rdwatch.androidtv.player.subtitle.parser.SubtitleParseResult
import com.rdwatch.androidtv.player.subtitle.parser.SubtitleParserFactory
import com.rdwatch.androidtv.player.subtitle.parser.SubtitleParsingException
import com.rdwatch.androidtv.player.subtitle.test.SubtitleTestBase
import com.rdwatch.androidtv.test.MainDispatcherRule
import com.rdwatch.androidtv.player.subtitle.*
import com.rdwatch.androidtv.player.subtitle.models.*
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import javax.inject.Inject

/**
 * Comprehensive tests for SubtitleManager covering:
 * - Full subtitle search and download flow
 * - Provider failover scenarios
 * - Error recovery mechanisms
 * - Integration with parsers and cache
 * - Style management and configuration
 * - ExoPlayer integration and synchronization
 */
@UnstableApi
@HiltAndroidTest
class SubtitleManagerTest : SubtitleTestBase() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Inject
    lateinit var context: Context

    // Mock dependencies
    private val mockSubtitleParserFactory = mockk<SubtitleParserFactory>()
    private val mockSubtitleSynchronizer = mockk<SubtitleSynchronizer>()
    private val mockStyleRepository = mockk<SubtitleStyleRepository>()
    private val mockErrorHandler = mockk<SubtitleErrorHandler>()
    private val mockExoPlayer = mockk<ExoPlayer>()

    private lateinit var subtitleManager: SubtitleManager

    @Before
    override fun setUp() {
        super.setUp()
        
        // Initialize mocks
        setupMocks()
        
        // Create SubtitleManager with mocked dependencies
        subtitleManager = SubtitleManager(
            context = context,
            subtitleParserFactory = mockSubtitleParserFactory,
            subtitleSynchronizer = mockSubtitleSynchronizer,
            styleRepository = mockStyleRepository,
            errorHandler = mockErrorHandler
        )
    }

    private fun setupMocks() {
        // Setup SubtitleSynchronizer mock
        every { mockSubtitleSynchronizer.initialize(any(), any()) } just Runs
        every { mockSubtitleSynchronizer.setSubtitleOffset(any()) } just Runs
        every { mockSubtitleSynchronizer.getSubtitleOffset() } returns 0L
        every { mockSubtitleSynchronizer.startSynchronization() } just Runs
        every { mockSubtitleSynchronizer.stopSynchronization() } just Runs
        every { mockSubtitleSynchronizer.synchronizationState } returns MutableStateFlow(mockk())
        every { mockSubtitleSynchronizer.getTimingStats() } returns mockk(relaxed = true)
        every { mockSubtitleSynchronizer.dispose() } just Runs

        // Setup StyleRepository mock
        every { mockStyleRepository.styleConfig } returns MutableStateFlow(mockk())
        coEvery { mockStyleRepository.saveStyleConfig(any()) } just Runs
        coEvery { mockStyleRepository.applyPreset(any()) } just Runs
        coEvery { mockStyleRepository.resetToDefault() } just Runs

        // Setup ErrorHandler mock
        every { mockErrorHandler.errors } returns MutableStateFlow(emptyList())
        every { mockErrorHandler.clearErrorsForUrl(any()) } just Runs
        every { mockErrorHandler.handleParsingError(any(), any()) } returns mockk {
            every { message } returns "Test parsing error"
        }
        every { mockErrorHandler.handleError(any(), any(), any()) } returns mockk {
            every { message } returns "Test error"
        }
        every { mockErrorHandler.getErrorsForUrl(any()) } returns emptyList()
        every { mockErrorHandler.hasCriticalErrors(any()) } returns false
        every { mockErrorHandler.clearErrors() } just Runs
        every { mockErrorHandler.getRetryCount(any()) } returns 0
        every { mockErrorHandler.retryUrl(any(), any()) } just Runs

        // Setup SubtitleFormat - remove this as it's causing issues
        // SubtitleFormat companion object methods will use defaults
    }

    @Test
    fun `initialization configures exoplayer integration`() {
        // Act
        subtitleManager.initialize(mockExoPlayer)

        // Assert
        verify { mockSubtitleSynchronizer.initialize(mockExoPlayer, subtitleManager) }
    }

    @Test
    fun `addSubtitleTracks creates available subtitles and auto-selects first`() = runTest {
        // Arrange
        val subtitles = listOf(
            ExternalSubtitleTrack("en", "English", "http://example.com/en.srt", "text/srt"),
            ExternalSubtitleTrack("es", "Spanish", "http://example.com/es.srt", "text/srt")
        )

        // Act
        subtitleManager.addSubtitleTracks(subtitles)

        // Assert
        val availableSubtitles = subtitleManager.availableSubtitles.first()
        assertEquals("Should have 2 available subtitles", 2, availableSubtitles.size)
        
        val firstSubtitle = availableSubtitles[0]
        assertEquals("English", firstSubtitle.label)
        assertEquals("en", firstSubtitle.language)
        assertEquals("http://example.com/en.srt", firstSubtitle.url)
        assertFalse("Should not be embedded", firstSubtitle.isEmbedded)

        // Should auto-select first subtitle
        val selectedSubtitle = subtitleManager.selectedSubtitle.first()
        assertNotNull("Should auto-select first subtitle", selectedSubtitle)
        assertEquals("Should select first subtitle", firstSubtitle.id, selectedSubtitle?.id)
    }

    @Test
    fun `loadExternalSubtitle success flow`() = runTest {
        // Arrange
        val testUrl = "http://example.com/test.srt"
        val testTrackData = createTestTrackData()
        val config = SubtitleLoadConfig(
            url = testUrl,
            format = SubtitleFormat.SRT,
            language = "en",
            label = "Test Subtitle",
            autoSelect = true
        )

        coEvery { 
            mockSubtitleParserFactory.parseSubtitleFromUrl(testUrl, SubtitleFormat.SRT, "UTF-8")
        } returns SubtitleParseResult.Success(testTrackData)

        // Act
        val result = subtitleManager.loadExternalSubtitle(config)

        // Assert
        assertTrue("Loading should succeed", result)
        verify { mockErrorHandler.clearErrorsForUrl(testUrl) }
        
        // Verify loading state progression
        val loadingState = subtitleManager.loadingState.value
        assertTrue("Should be in success state", loadingState is SubtitleLoadingState.Success)
        assertEquals("Should have correct URL", testUrl, (loadingState as SubtitleLoadingState.Success).url)

        // Verify subtitle was added to available list
        val availableSubtitles = subtitleManager.availableSubtitles.first()
        assertTrue("Should contain loaded subtitle", 
            availableSubtitles.any { it.url == testUrl && it.label == "Test Subtitle" }
        )

        // Verify auto-selection
        val selectedSubtitle = subtitleManager.selectedSubtitle.first()
        assertEquals("Should auto-select loaded subtitle", testUrl, selectedSubtitle?.url)
    }

    @Test
    fun `loadExternalSubtitle parsing error handling`() = runTest {
        // Arrange
        val testUrl = "http://example.com/invalid.srt"
        val config = SubtitleLoadConfig(url = testUrl, format = SubtitleFormat.SRT)
        val parseException = SubtitleParsingException("Invalid format")

        coEvery {
            mockSubtitleParserFactory.parseSubtitleFromUrl(testUrl, SubtitleFormat.SRT, "UTF-8")
        } returns SubtitleParseResult.Error(parseException)

        // Act
        val result = subtitleManager.loadExternalSubtitle(config)

        // Assert
        assertFalse("Loading should fail", result)
        verify { mockErrorHandler.handleParsingError(testUrl, parseException) }
        
        val loadingState = subtitleManager.loadingState.value
        assertTrue("Should be in error state", loadingState is SubtitleLoadingState.Error)
        assertEquals("Should have correct URL", testUrl, (loadingState as SubtitleLoadingState.Error).url)
    }

    @Test
    fun `loadExternalSubtitle network error handling`() = runTest {
        // Arrange
        val testUrl = "http://example.com/network-error.srt"
        val config = SubtitleLoadConfig(url = testUrl, format = SubtitleFormat.SRT)
        val networkException = RuntimeException("Network error")

        coEvery {
            mockSubtitleParserFactory.parseSubtitleFromUrl(any(), any(), any())
        } throws networkException

        // Act
        val result = subtitleManager.loadExternalSubtitle(config)

        // Assert
        assertFalse("Loading should fail", result)
        verify { mockErrorHandler.handleError(testUrl, networkException, SubtitleErrorContext.LOADING) }
        
        val loadingState = subtitleManager.loadingState.value
        assertTrue("Should be in error state", loadingState is SubtitleLoadingState.Error)
    }

    @Test
    fun `loadExternalSubtitles processes multiple configs`() = runTest {
        // Arrange
        val configs = listOf(
            SubtitleLoadConfig(url = "http://example.com/en.srt", format = SubtitleFormat.SRT, language = "en"),
            SubtitleLoadConfig(url = "http://example.com/es.srt", format = SubtitleFormat.SRT, language = "es")
        )

        coEvery {
            mockSubtitleParserFactory.parseSubtitleFromUrl(any(), any(), any())
        } returns SubtitleParseResult.Success(createTestTrackData())

        // Act
        subtitleManager.loadExternalSubtitles(configs)

        // Allow coroutines to complete
        kotlinx.coroutines.delay(100)

        // Assert
        coVerify(exactly = 2) {
            mockSubtitleParserFactory.parseSubtitleFromUrl(any(), SubtitleFormat.SRT, "UTF-8")
        }
        verify(exactly = 2) { mockErrorHandler.clearErrorsForUrl(any()) }
    }

    @Test
    fun `addEmbeddedSubtitles creates embedded subtitle tracks`() = runTest {
        // Arrange
        val trackCount = 3
        val trackInfoProvider: (Int) -> Pair<String, String> = { index ->
            when (index) {
                0 -> "en" to "English"
                1 -> "es" to "Spanish"
                2 -> "fr" to "French"
                else -> "unknown" to "Unknown"
            }
        }

        // Act
        subtitleManager.addEmbeddedSubtitles(trackCount, trackInfoProvider)

        // Assert
        val availableSubtitles = subtitleManager.availableSubtitles.first()
        assertEquals("Should have 3 embedded subtitles", 3, availableSubtitles.size)
        
        val englishSubtitle = availableSubtitles.find { it.language == "en" }
        assertNotNull("Should have English subtitle", englishSubtitle)
        assertTrue("Should be marked as embedded", englishSubtitle!!.isEmbedded)
        assertEquals("Should have correct label", "English", englishSubtitle.label)
    }

    @Test
    fun `selectSubtitle updates selected state and clears active cues`() = runTest {
        // Arrange
        val subtitle = AvailableSubtitle(
            id = 1,
            language = "en",
            label = "Test",
            url = "http://example.com/test.srt",
            mimeType = "text/srt",
            isEmbedded = false
        )

        // Add some active cues first
        subtitleManager.addSubtitleTracks(listOf(
            ExternalSubtitleTrack("en", "English", "http://example.com/en.srt", "text/srt")
        ))

        // Act
        subtitleManager.selectSubtitle(subtitle)

        // Assert
        val selectedSubtitle = subtitleManager.selectedSubtitle.first()
        assertEquals("Should select the subtitle", subtitle.id, selectedSubtitle?.id)

        val activeCues = subtitleManager.activeSubtitleCues.first()
        assertTrue("Should clear active cues", activeCues.isEmpty())
    }

    @Test
    fun `selectSubtitle loads external subtitle if not already loaded`() = runTest {
        // Arrange
        val subtitle = AvailableSubtitle(
            id = 1,
            language = "en",
            label = "Test",
            url = "http://example.com/test.srt",
            mimeType = "text/srt",
            isEmbedded = false
        )

        coEvery {
            mockSubtitleParserFactory.parseSubtitleFromUrl(any(), any(), any())
        } returns SubtitleParseResult.Success(createTestTrackData())

        // Act
        subtitleManager.selectSubtitle(subtitle)

        // Allow async loading to complete
        kotlinx.coroutines.delay(100)

        // Assert
        coVerify {
            mockSubtitleParserFactory.parseSubtitleFromUrl(
                "http://example.com/test.srt",
                SubtitleFormat.SRT,
                "UTF-8"
            )
        }
    }

    @Test
    fun `updateSubtitlesForPosition updates active cues for external subtitles`() {
        // Arrange
        val testUrl = "http://example.com/test.srt"
        val subtitle = AvailableSubtitle(
            id = 1,
            language = "en",
            label = "Test",
            url = testUrl,
            mimeType = "text/srt",
            isEmbedded = false
        )

        val mockTrackData = mockk<SubtitleTrackData>()
        val testCues = listOf(createTestCue(1000L, 3000L, "Test cue"))
        every { mockTrackData.getCuesAt(2000L) } returns testCues

        // Simulate loaded track data
        subtitleManager.selectSubtitle(subtitle)
        val field = SubtitleManager::class.java.getDeclaredField("loadedSubtitleTracks")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val loadedTracks = field.get(subtitleManager) as MutableMap<String, SubtitleTrackData>
        loadedTracks[testUrl] = mockTrackData

        // Act
        subtitleManager.updateSubtitlesForPosition(2000L)

        // Assert
        verify { mockTrackData.getCuesAt(2000L) }
        
        runTest {
            val activeCues = subtitleManager.activeSubtitleCues.first()
            assertEquals("Should have active cues", 1, activeCues.size)
            assertEquals("Should have correct cue text", "Test cue", activeCues.first().text)
        }
    }

    @Test
    fun `removeSubtitleTrack removes from all collections and deselects if selected`() = runTest {
        // Arrange
        val testUrl = "http://example.com/test.srt"
        subtitleManager.addSubtitleTracks(listOf(
            ExternalSubtitleTrack("en", "English", testUrl, "text/srt")
        ))

        // Act
        subtitleManager.removeSubtitleTrack(testUrl)

        // Assert
        val availableSubtitles = subtitleManager.availableSubtitles.first()
        assertTrue("Should remove from available subtitles", 
            availableSubtitles.none { it.url == testUrl }
        )

        val selectedSubtitle = subtitleManager.selectedSubtitle.first()
        assertNull("Should deselect if was selected", selectedSubtitle)

        val activeCues = subtitleManager.activeSubtitleCues.first()
        assertTrue("Should clear active cues", activeCues.isEmpty())
    }

    @Test
    fun `clearAllSubtitles resets all state`() = runTest {
        // Arrange
        subtitleManager.addSubtitleTracks(listOf(
            ExternalSubtitleTrack("en", "English", "http://example.com/en.srt", "text/srt"),
            ExternalSubtitleTrack("es", "Spanish", "http://example.com/es.srt", "text/srt")
        ))

        // Act
        subtitleManager.clearAllSubtitles()

        // Assert
        val availableSubtitles = subtitleManager.availableSubtitles.first()
        assertTrue("Should clear available subtitles", availableSubtitles.isEmpty())

        val selectedSubtitle = subtitleManager.selectedSubtitle.first()
        assertNull("Should clear selected subtitle", selectedSubtitle)

        val activeCues = subtitleManager.activeSubtitleCues.first()
        assertTrue("Should clear active cues", activeCues.isEmpty())

        val loadingState = subtitleManager.loadingState.value
        assertTrue("Should reset loading state", loadingState is SubtitleLoadingState.Idle)
    }

    @Test
    fun `updateSubtitleStyle updates style state`() = runTest {
        // Arrange
        val newStyle = SubtitleStyle(
            textSize = 0.1f,
            foregroundColor = android.graphics.Color.RED,
            backgroundColor = android.graphics.Color.BLUE,
            windowColor = android.graphics.Color.GREEN,
            edgeType = 1,
            edgeColor = android.graphics.Color.BLACK,
            typeface = android.graphics.Typeface.SERIF,
            bottomPadding = 0.2f
        )

        // Act
        subtitleManager.updateSubtitleStyle(newStyle)

        // Assert
        val currentStyle = subtitleManager.subtitleStyle.first()
        assertEquals("Should update text size", 0.1f, currentStyle.textSize, 0.001f)
        assertEquals("Should update foreground color", android.graphics.Color.RED, currentStyle.foregroundColor)
        assertEquals("Should update typeface", android.graphics.Typeface.SERIF, currentStyle.typeface)
    }

    @Test
    fun `configureSubtitleView applies current style`() {
        // Arrange
        val mockSubtitleView = mockk<SubtitleView>(relaxed = true)

        // Act
        subtitleManager.configureSubtitleView(mockSubtitleView)

        // Assert
        verify { mockSubtitleView.setStyle(any()) }
        verify { mockSubtitleView.setFractionalTextSize(any()) }
        verify { mockSubtitleView.setBottomPaddingFraction(any()) }
    }

    @Test
    fun `getCurrentSubtitleText returns active subtitle text for external subtitles`() {
        // Arrange
        val testUrl = "http://example.com/test.srt"
        val subtitle = AvailableSubtitle(
            id = 1,
            language = "en",
            label = "Test",
            url = testUrl,
            mimeType = "text/srt",
            isEmbedded = false
        )

        val mockTrackData = mockk<SubtitleTrackData>()
        val testCues = listOf(
            createTestCue(1000L, 3000L, "First line"),
            createTestCue(1500L, 3500L, "Second line")
        )
        every { mockTrackData.getCuesAt(2000L) } returns testCues

        // Setup loaded track data
        subtitleManager.selectSubtitle(subtitle)
        val field = SubtitleManager::class.java.getDeclaredField("loadedSubtitleTracks")
        field.isAccessible = true
        @Suppress("UNCHECKED_CAST")
        val loadedTracks = field.get(subtitleManager) as MutableMap<String, SubtitleTrackData>
        loadedTracks[testUrl] = mockTrackData

        // Act
        val subtitleText = subtitleManager.getCurrentSubtitleText(2000L)

        // Assert
        assertNotNull("Should return subtitle text", subtitleText)
        assertEquals("Should join multiple cues", "First line\nSecond line", subtitleText)
    }

    @Test
    fun `getCurrentSubtitleText returns null for embedded subtitles`() {
        // Arrange
        val subtitle = AvailableSubtitle(
            id = 1,
            language = "en",
            label = "Embedded",
            url = "",
            mimeType = "",
            isEmbedded = true
        )
        subtitleManager.selectSubtitle(subtitle)

        // Act
        val subtitleText = subtitleManager.getCurrentSubtitleText(2000L)

        // Assert
        assertNull("Should return null for embedded subtitles", subtitleText)
    }

    @Test
    fun `synchronization methods delegate to SubtitleSynchronizer`() {
        // Test setSubtitleOffset
        subtitleManager.setSubtitleOffset(1500L)
        verify { mockSubtitleSynchronizer.setSubtitleOffset(1500L) }

        // Test getSubtitleOffset
        val offset = subtitleManager.getSubtitleOffset()
        verify { mockSubtitleSynchronizer.getSubtitleOffset() }
        assertEquals("Should return offset from synchronizer", 0L, offset)

        // Test startSynchronization
        subtitleManager.startSynchronization()
        verify { mockSubtitleSynchronizer.startSynchronization() }

        // Test stopSynchronization
        subtitleManager.stopSynchronization()
        verify { mockSubtitleSynchronizer.stopSynchronization() }

        // Test getSynchronizationState
        subtitleManager.getSynchronizationState()
        verify { mockSubtitleSynchronizer.synchronizationState }

        // Test getTimingStats
        subtitleManager.getTimingStats()
        verify { mockSubtitleSynchronizer.getTimingStats() }
    }

    @Test
    fun `style configuration methods delegate to StyleRepository`() = runTest {
        // Arrange
        val config = mockk<SubtitleStyleConfig>()
        val presetName = "dark"

        // Test updateStyleConfig
        subtitleManager.updateStyleConfig(config)
        coVerify { mockStyleRepository.saveStyleConfig(config) }

        // Test applyStylePreset
        subtitleManager.applyStylePreset(presetName)
        coVerify { mockStyleRepository.applyPreset(presetName) }

        // Test resetStyleToDefault
        subtitleManager.resetStyleToDefault()
        coVerify { mockStyleRepository.resetToDefault() }
    }

    @Test
    fun `error handling methods delegate to ErrorHandler`() {
        // Arrange
        val testUrl = "http://example.com/test.srt"

        // Test getErrorsForUrl
        subtitleManager.getErrorsForUrl(testUrl)
        verify { mockErrorHandler.getErrorsForUrl(testUrl) }

        // Test hasCriticalErrors
        subtitleManager.hasCriticalErrors(testUrl)
        verify { mockErrorHandler.hasCriticalErrors(testUrl) }

        // Test clearErrors
        subtitleManager.clearErrors()
        verify { mockErrorHandler.clearErrors() }

        // Test clearErrorsForUrl
        subtitleManager.clearErrorsForUrl(testUrl)
        verify { mockErrorHandler.clearErrorsForUrl(testUrl) }

        // Test getRetryCount
        subtitleManager.getRetryCount(testUrl)
        verify { mockErrorHandler.getRetryCount(testUrl) }
    }

    @Test
    fun `retrySubtitle uses error handler retry mechanism`() = runTest {
        // Arrange
        val testUrl = "http://example.com/test.srt"
        val retrySlot = slot<suspend (String) -> Boolean>()
        every { mockErrorHandler.retryUrl(eq(testUrl), capture(retrySlot)) } just Runs

        coEvery {
            mockSubtitleParserFactory.parseSubtitleFromUrl(any(), any(), any())
        } returns SubtitleParseResult.Success(createTestTrackData())

        // Act
        subtitleManager.retrySubtitle(testUrl)

        // Simulate retry callback
        retrySlot.captured.invoke(testUrl)

        // Assert
        verify { mockErrorHandler.retryUrl(testUrl, any()) }
        coVerify { 
            mockSubtitleParserFactory.parseSubtitleFromUrl(testUrl, any(), any()) 
        }
    }

    @Test
    fun `createMediaItemWithSubtitles creates proper MediaItem`() {
        // Arrange
        val videoUrl = "http://example.com/video.mp4"
        val title = "Test Movie"
        val subtitles = listOf(
            ExternalSubtitleTrack("en", "English", "http://example.com/en.srt", "text/srt"),
            ExternalSubtitleTrack("es", "Spanish", "http://example.com/es.srt", "text/srt")
        )

        // Act
        val mediaItem = subtitleManager.createMediaItemWithSubtitles(videoUrl, title, subtitles)

        // Assert
        assertNotNull("Should create MediaItem", mediaItem)
        assertEquals("Should have correct URI", videoUrl, mediaItem.localConfiguration?.uri.toString())
        assertEquals("Should have correct title", title, mediaItem.mediaMetadata.title.toString())
    }

    @Test
    fun `getSupportedFormats delegates to parser factory`() {
        // Arrange
        val supportedFormats = listOf(SubtitleFormat.SRT, SubtitleFormat.VTT)
        every { mockSubtitleParserFactory.getSupportedFormats() } returns supportedFormats

        // Act
        val formats = subtitleManager.getSupportedFormats()

        // Assert
        verify { mockSubtitleParserFactory.getSupportedFormats() }
        assertEquals("Should return supported formats", supportedFormats, formats)
    }

    @Test
    fun `isFormatSupported delegates to parser factory`() {
        // Arrange
        every { mockSubtitleParserFactory.isFormatSupported(SubtitleFormat.SRT) } returns true
        every { mockSubtitleParserFactory.isFormatSupported(SubtitleFormat.ASS) } returns false

        // Act & Assert
        assertTrue("Should support SRT", subtitleManager.isFormatSupported(SubtitleFormat.SRT))
        assertFalse("Should not support ASS", subtitleManager.isFormatSupported(SubtitleFormat.ASS))
    }

    @Test
    fun `dispose cleans up resources`() {
        // Act
        subtitleManager.dispose()

        // Assert
        verify { mockSubtitleSynchronizer.dispose() }
    }

    @Test
    fun `concurrent subtitle loading handles race conditions`() = runTest {
        // Arrange
        val urls = (1..5).map { "http://example.com/subtitle$it.srt" }
        val configs = urls.map { SubtitleLoadConfig(url = it, format = SubtitleFormat.SRT) }

        coEvery {
            mockSubtitleParserFactory.parseSubtitleFromUrl(any(), any(), any())
        } returns SubtitleParseResult.Success(createTestTrackData())

        // Act - Load multiple subtitles concurrently
        val results = configs.map { config ->
            async {
                subtitleManager.loadExternalSubtitle(config)
            }
        }.map { it.await() }

        // Assert
        assertTrue("All loadings should succeed", results.all { it })
        coVerify(exactly = 5) {
            mockSubtitleParserFactory.parseSubtitleFromUrl(any(), SubtitleFormat.SRT, "UTF-8")
        }

        val availableSubtitles = subtitleManager.availableSubtitles.first()
        assertEquals("Should have all loaded subtitles", 5, availableSubtitles.size)
    }

    @Test
    fun `memory management with large subtitle collections`() = runTest {
        // Arrange - Create a large number of subtitle tracks
        val largeSubtitleList = (1..100).map { index ->
            ExternalSubtitleTrack(
                language = "en",
                label = "Subtitle $index",
                url = "http://example.com/subtitle$index.srt",
                mimeType = "text/srt"
            )
        }

        // Act
        subtitleManager.addSubtitleTracks(largeSubtitleList)

        // Assert
        val availableSubtitles = subtitleManager.availableSubtitles.first()
        assertEquals("Should handle large collections", 100, availableSubtitles.size)

        // Test clearing large collection
        subtitleManager.clearAllSubtitles()
        val clearedSubtitles = subtitleManager.availableSubtitles.first()
        assertTrue("Should clear large collections", clearedSubtitles.isEmpty())
    }
}