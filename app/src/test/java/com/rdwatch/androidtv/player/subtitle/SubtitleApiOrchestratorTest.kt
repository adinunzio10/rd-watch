package com.rdwatch.androidtv.player.subtitle

import com.rdwatch.androidtv.core.reactive.DispatcherProvider
import com.rdwatch.androidtv.core.error.ErrorHandler
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiClient
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiProvider
import com.rdwatch.androidtv.player.subtitle.api.RateLimitStatus
import com.rdwatch.androidtv.player.subtitle.cache.SubtitleCache
import com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchRequest
import com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchResult
import com.rdwatch.androidtv.player.subtitle.models.ContentType
import com.rdwatch.androidtv.player.subtitle.models.SubtitleFormat
import com.rdwatch.androidtv.player.subtitle.models.MatchType
import com.rdwatch.androidtv.player.subtitle.ranking.SubtitleResultRanker
import com.rdwatch.androidtv.player.subtitle.test.SubtitleTestBase
import com.rdwatch.androidtv.test.MainDispatcherRule
import com.rdwatch.androidtv.player.subtitle.SubtitleRateLimiter
import com.rdwatch.androidtv.player.subtitle.SubtitleDownloadState
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Comprehensive tests for SubtitleApiOrchestrator covering:
 * - Provider priority handling and fallback logic
 * - Concurrent request management
 * - Rate limiting coordination
 * - Cache integration and management
 * - Error handling across multiple providers
 * - Result ranking and aggregation
 */
@HiltAndroidTest
class SubtitleApiOrchestratorTest : SubtitleTestBase() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // Mock dependencies
    private val mockApiClient1 = mockk<SubtitleApiClient>()
    private val mockApiClient2 = mockk<SubtitleApiClient>()
    private val mockApiClient3 = mockk<SubtitleApiClient>()
    private val mockSubtitleCache = mockk<SubtitleCache>()
    private val mockResultRanker = mockk<SubtitleResultRanker>()
    private val mockRateLimiter = mockk<SubtitleRateLimiter>()
    private val mockDispatcherProvider = mockk<DispatcherProvider>()
    private val mockErrorHandler = mockk<ErrorHandler>()

    private lateinit var orchestrator: SubtitleApiOrchestrator

    @Before
    override fun setUp() {
        super.setUp()
        setupMocks()
        
        orchestrator = SubtitleApiOrchestrator(
            apiClients = setOf(mockApiClient1, mockApiClient2, mockApiClient3),
            subtitleCache = mockSubtitleCache,
            resultRanker = mockResultRanker,
            rateLimiter = mockRateLimiter,
            dispatcherProvider = mockDispatcherProvider,
            errorHandler = mockErrorHandler
        )
    }

    private fun setupMocks() {
        // Setup DispatcherProvider
        every { mockDispatcherProvider.io } returns kotlinx.coroutines.Dispatchers.Unconfined
        every { mockDispatcherProvider.main } returns kotlinx.coroutines.Dispatchers.Unconfined
        every { mockDispatcherProvider.default } returns kotlinx.coroutines.Dispatchers.Unconfined

        // Setup API clients
        every { mockApiClient1.getProvider() } returns SubtitleApiProvider.SUBDL
        every { mockApiClient1.isEnabled() } returns true
        every { mockApiClient2.getProvider() } returns SubtitleApiProvider.SUBDB
        every { mockApiClient2.isEnabled() } returns true
        every { mockApiClient3.getProvider() } returns SubtitleApiProvider.PODNAPISI
        every { mockApiClient3.isEnabled() } returns true

        // Setup rate limiter
        coEvery { mockRateLimiter.canMakeRequest(any()) } returns true
        coEvery { mockRateLimiter.recordRequest(any()) } returns Unit
        every { mockRateLimiter.getStatus(any()) } returns RateLimitStatus(
            requestsRemaining = 100,
            resetTimeMs = System.currentTimeMillis() + 3600000,
            isLimited = false
        )

        // Setup error handler
        every { mockErrorHandler.handleError(any()) } returns mockk()

        // Setup cache (default to empty cache)
        coEvery { mockSubtitleCache.getCachedResults(any()) } returns emptyList()
        coEvery { mockSubtitleCache.cacheResults(any(), any()) } returns Unit
        coEvery { mockSubtitleCache.getCachedFile(any()) } returns null
        coEvery { mockSubtitleCache.cacheFile(any(), any()) } returns "/cached/path/subtitle.srt"
        coEvery { mockSubtitleCache.clearAll() } returns Unit

        // Setup result ranker
        every { mockResultRanker.rankResults(any(), any()) } returnsArgument 0
    }

    private fun createTestSearchRequest(): SubtitleSearchRequest {
        return SubtitleSearchRequest(
            title = "Test Movie",
            year = 2023,
            type = ContentType.MOVIE,
            languages = listOf("en"),
            imdbId = "tt1234567"
        )
    }

    private fun createTestSearchResult(
        provider: SubtitleApiProvider,
        id: String = "test-id",
        language: String = "en",
        matchType: MatchType = MatchType.IMDB_MATCH
    ): SubtitleSearchResult {
        return SubtitleSearchResult(
            id = id,
            provider = provider,
            language = language,
            languageName = "English",
            format = SubtitleFormat.SRT,
            downloadUrl = "http://example.com/subtitle.srt",
            fileName = "subtitle.srt",
            matchType = matchType,
            matchScore = matchType.confidence
        )
    }

    @Test
    fun `searchSubtitles returns cached results when available`() = runTest {
        // Arrange
        val request = createTestSearchRequest()
        val cachedResults = listOf(
            createTestSearchResult(SubtitleApiProvider.SUBDL, "cached-1"),
            createTestSearchResult(SubtitleApiProvider.SUBDB, "cached-2")
        )
        
        coEvery { mockSubtitleCache.getCachedResults(request) } returns cachedResults

        // Act
        val results = orchestrator.searchSubtitles(request).first()

        // Assert
        assertEquals("Should return cached results", cachedResults, results)
        coVerify { mockSubtitleCache.getCachedResults(request) }
        
        // Should not call API clients when cache hit
        coVerify(exactly = 0) { mockApiClient1.searchSubtitles(any()) }
        coVerify(exactly = 0) { mockApiClient2.searchSubtitles(any()) }
        coVerify(exactly = 0) { mockApiClient3.searchSubtitles(any()) }
    }

    @Test
    fun `searchSubtitles coordinates multiple providers when cache empty`() = runTest {
        // Arrange
        val request = createTestSearchRequest()
        val results1 = listOf(createTestSearchResult(SubtitleApiProvider.SUBDL, "result-1"))
        val results2 = listOf(createTestSearchResult(SubtitleApiProvider.SUBDB, "result-2"))
        val results3 = listOf(createTestSearchResult(SubtitleApiProvider.PODNAPISI, "result-3"))
        
        coEvery { mockApiClient1.searchSubtitles(request) } returns results1
        coEvery { mockApiClient2.searchSubtitles(request) } returns results2
        coEvery { mockApiClient3.searchSubtitles(request) } returns results3

        val expectedRankedResults = results1 + results2 + results3
        every { mockResultRanker.rankResults(expectedRankedResults, request) } returns expectedRankedResults

        // Act
        val results = orchestrator.searchSubtitles(request).first()

        // Assert
        assertEquals("Should return combined results", expectedRankedResults, results)
        
        // Verify all clients were called
        coVerify { mockApiClient1.searchSubtitles(request) }
        coVerify { mockApiClient2.searchSubtitles(request) }
        coVerify { mockApiClient3.searchSubtitles(request) }
        
        // Verify rate limiting was applied
        coVerify { mockRateLimiter.canMakeRequest(SubtitleApiProvider.SUBDL) }
        coVerify { mockRateLimiter.canMakeRequest(SubtitleApiProvider.SUBDB) }
        coVerify { mockRateLimiter.canMakeRequest(SubtitleApiProvider.PODNAPISI) }
        coVerify { mockRateLimiter.recordRequest(SubtitleApiProvider.SUBDL) }
        coVerify { mockRateLimiter.recordRequest(SubtitleApiProvider.SUBDB) }
        coVerify { mockRateLimiter.recordRequest(SubtitleApiProvider.PODNAPISI) }
        
        // Verify results were ranked and cached
        verify { mockResultRanker.rankResults(expectedRankedResults, request) }
        coVerify { mockSubtitleCache.cacheResults(request, expectedRankedResults) }
    }

    @Test
    fun `searchSubtitles respects rate limiting`() = runTest {
        // Arrange
        val request = createTestSearchRequest()
        
        // Rate limit client 2
        coEvery { mockRateLimiter.canMakeRequest(SubtitleApiProvider.SUBDL) } returns true
        coEvery { mockRateLimiter.canMakeRequest(SubtitleApiProvider.SUBDB) } returns false
        coEvery { mockRateLimiter.canMakeRequest(SubtitleApiProvider.PODNAPISI) } returns true

        val results1 = listOf(createTestSearchResult(SubtitleApiProvider.SUBDL, "result-1"))
        val results3 = listOf(createTestSearchResult(SubtitleApiProvider.PODNAPISI, "result-3"))
        
        coEvery { mockApiClient1.searchSubtitles(request) } returns results1
        coEvery { mockApiClient3.searchSubtitles(request) } returns results3

        // Act
        val results = orchestrator.searchSubtitles(request).first()

        // Assert
        assertEquals("Should only include results from non-rate-limited providers", 2, results.size)
        assertTrue("Should include SUBDL results", results.any { it.provider == SubtitleApiProvider.SUBDL })
        assertTrue("Should include Podnapisi results", results.any { it.provider == SubtitleApiProvider.PODNAPISI })
        assertFalse("Should not include SUBDB results", results.any { it.provider == SubtitleApiProvider.SUBDB })
        
        // Verify only non-rate-limited clients were called
        coVerify { mockApiClient1.searchSubtitles(request) }
        coVerify(exactly = 0) { mockApiClient2.searchSubtitles(request) }
        coVerify { mockApiClient3.searchSubtitles(request) }
    }

    @Test
    fun `searchSubtitles skips disabled providers`() = runTest {
        // Arrange
        val request = createTestSearchRequest()
        
        // Disable client 2
        every { mockApiClient2.isEnabled() } returns false
        
        val results1 = listOf(createTestSearchResult(SubtitleApiProvider.SUBDL, "result-1"))
        val results3 = listOf(createTestSearchResult(SubtitleApiProvider.PODNAPISI, "result-3"))
        
        coEvery { mockApiClient1.searchSubtitles(request) } returns results1
        coEvery { mockApiClient3.searchSubtitles(request) } returns results3

        // Act
        val results = orchestrator.searchSubtitles(request).first()

        // Assert
        assertEquals("Should only include results from enabled providers", 2, results.size)
        
        // Verify only enabled clients were called
        coVerify { mockApiClient1.searchSubtitles(request) }
        coVerify(exactly = 0) { mockApiClient2.searchSubtitles(request) }
        coVerify { mockApiClient3.searchSubtitles(request) }
    }

    @Test
    fun `searchSubtitles handles provider failures gracefully`() = runTest {
        // Arrange
        val request = createTestSearchRequest()
        
        val results1 = listOf(createTestSearchResult(SubtitleApiProvider.SUBDL, "result-1"))
        val results3 = listOf(createTestSearchResult(SubtitleApiProvider.PODNAPISI, "result-3"))
        
        coEvery { mockApiClient1.searchSubtitles(request) } returns results1
        coEvery { mockApiClient2.searchSubtitles(request) } throws RuntimeException("API Error")
        coEvery { mockApiClient3.searchSubtitles(request) } returns results3

        // Act
        val results = orchestrator.searchSubtitles(request).first()

        // Assert
        assertEquals("Should return results from successful providers", 2, results.size)
        assertTrue("Should include successful results", results.any { it.provider == SubtitleApiProvider.SUBDL })
        assertTrue("Should include successful results", results.any { it.provider == SubtitleApiProvider.PODNAPISI })
        
        // Verify error was handled
        verify { mockErrorHandler.handleError(any<RuntimeException>()) }
    }

    @Test
    fun `downloadSubtitle returns cached file when available`() = runTest {
        // Arrange
        val result = createTestSearchResult(SubtitleApiProvider.SUBDL)
        val cachedFilePath = "/cache/subtitle.srt"
        
        coEvery { mockSubtitleCache.getCachedFile(result) } returns cachedFilePath

        // Act
        val downloadStates = orchestrator.downloadSubtitle(result).toList()

        // Assert
        assertEquals("Should have 2 states: Loading and Success", 2, downloadStates.size)
        assertTrue("First state should be Loading", downloadStates[0] is SubtitleDownloadState.Loading)
        assertTrue("Second state should be Success", downloadStates[1] is SubtitleDownloadState.Success)
        
        val successState = downloadStates[1] as SubtitleDownloadState.Success
        assertEquals("Should return cached file path", cachedFilePath, successState.filePath)
        
        // Should not call download API when cache hit
        coVerify(exactly = 0) { mockApiClient1.downloadSubtitle(any()) }
    }

    @Test
    fun `downloadSubtitle downloads from provider when not cached`() = runTest {
        // Arrange
        val result = createTestSearchResult(SubtitleApiProvider.SUBDL)
        val downloadedFilePath = "/downloads/subtitle.srt"
        
        coEvery { mockApiClient1.downloadSubtitle(result) } returns downloadedFilePath

        // Act
        val downloadStates = orchestrator.downloadSubtitle(result).toList()

        // Assert
        assertEquals("Should have 2 states: Loading and Success", 2, downloadStates.size)
        assertTrue("First state should be Loading", downloadStates[0] is SubtitleDownloadState.Loading)
        assertTrue("Second state should be Success", downloadStates[1] is SubtitleDownloadState.Success)
        
        val successState = downloadStates[1] as SubtitleDownloadState.Success
        assertEquals("Should return downloaded file path", downloadedFilePath, successState.filePath)
        
        // Verify download was called and file cached
        coVerify { mockApiClient1.downloadSubtitle(result) }
        coVerify { mockRateLimiter.canMakeRequest(SubtitleApiProvider.SUBDL) }
        coVerify { mockRateLimiter.recordRequest(SubtitleApiProvider.SUBDL) }
        coVerify { mockSubtitleCache.cacheFile(result, downloadedFilePath) }
    }

    @Test
    fun `downloadSubtitle respects rate limiting`() = runTest {
        // Arrange
        val result = createTestSearchResult(SubtitleApiProvider.SUBDL)
        
        coEvery { mockRateLimiter.canMakeRequest(SubtitleApiProvider.SUBDL) } returns false

        // Act
        val downloadStates = orchestrator.downloadSubtitle(result).toList()

        // Assert
        assertEquals("Should have 2 states: Loading and RateLimited", 2, downloadStates.size)
        assertTrue("First state should be Loading", downloadStates[0] is SubtitleDownloadState.Loading)
        assertTrue("Second state should be RateLimited", downloadStates[1] is SubtitleDownloadState.RateLimited)
        
        // Should not call download API when rate limited
        coVerify(exactly = 0) { mockApiClient1.downloadSubtitle(any()) }
    }

    @Test
    fun `downloadSubtitle handles download errors`() = runTest {
        // Arrange
        val result = createTestSearchResult(SubtitleApiProvider.SUBDL)
        val downloadError = RuntimeException("Download failed")
        
        coEvery { mockApiClient1.downloadSubtitle(result) } throws downloadError

        // Act
        val downloadStates = orchestrator.downloadSubtitle(result).toList()

        // Assert
        assertEquals("Should have 2 states: Loading and Error", 2, downloadStates.size)
        assertTrue("First state should be Loading", downloadStates[0] is SubtitleDownloadState.Loading)
        assertTrue("Second state should be Error", downloadStates[1] is SubtitleDownloadState.Error)
        
        val errorState = downloadStates[1] as SubtitleDownloadState.Error
        assertEquals("Should contain original error", downloadError, errorState.exception)
        
        // Verify error was handled
        verify { mockErrorHandler.handleError(downloadError) }
    }

    @Test
    fun `downloadSubtitle fails when no client found for provider`() = runTest {
        // Arrange
        val result = createTestSearchResult(SubtitleApiProvider.LOCAL_FILES) // Not in our client set

        // Act
        val downloadStates = orchestrator.downloadSubtitle(result).toList()

        // Assert
        assertEquals("Should have 2 states: Loading and Error", 2, downloadStates.size)
        assertTrue("First state should be Loading", downloadStates[0] is SubtitleDownloadState.Loading)
        assertTrue("Second state should be Error", downloadStates[1] is SubtitleDownloadState.Error)
        
        val errorState = downloadStates[1] as SubtitleDownloadState.Error
        assertTrue("Should be IllegalStateException", errorState.exception is IllegalStateException)
        assertTrue("Should mention provider not found", 
            errorState.exception.message?.contains("No client found for provider") == true)
    }

    @Test
    fun `getProviderStatus returns correct status for all providers`() {
        // Act
        val statuses = orchestrator.getProviderStatus()

        // Assert
        assertEquals("Should have status for all 3 providers", 3, statuses.size)
        
        val subdlStatus = statuses[SubtitleApiProvider.SUBDL]
        assertNotNull("Should have SUBDL status", subdlStatus)
        assertTrue("SUBDL should be enabled", subdlStatus!!.enabled)
        assertFalse("SUBDL should not be rate limited", subdlStatus.rateLimitStatus.isLimited)
        
        val subdbStatus = statuses[SubtitleApiProvider.SUBDB]
        assertNotNull("Should have SUBDB status", subdbStatus)
        assertTrue("SUBDB should be enabled", subdbStatus!!.enabled)
        
        val podnapisiStatus = statuses[SubtitleApiProvider.PODNAPISI]
        assertNotNull("Should have Podnapisi status", podnapisiStatus)
        assertTrue("Podnapisi should be enabled", podnapisiStatus!!.enabled)
    }

    @Test
    fun `getProviderStatus reflects disabled providers`() {
        // Arrange
        every { mockApiClient2.isEnabled() } returns false

        // Act
        val statuses = orchestrator.getProviderStatus()

        // Assert
        val subdbStatus = statuses[SubtitleApiProvider.SUBDB]
        assertNotNull("Should have SUBDB status", subdbStatus)
        assertFalse("SUBDB should be disabled", subdbStatus!!.enabled)
    }

    @Test
    fun `getProviderStatus reflects rate limiting`() {
        // Arrange
        every { mockRateLimiter.getStatus(SubtitleApiProvider.SUBDL) } returns RateLimitStatus(
            requestsRemaining = 0,
            resetTimeMs = System.currentTimeMillis() + 1800000,
            isLimited = true
        )

        // Act
        val statuses = orchestrator.getProviderStatus()

        // Assert
        val subdlStatus = statuses[SubtitleApiProvider.SUBDL]
        assertNotNull("Should have SUBDL status", subdlStatus)
        assertTrue("SUBDL should be rate limited", subdlStatus!!.rateLimitStatus.isLimited)
        assertEquals("Should have 0 requests remaining", 0, subdlStatus.rateLimitStatus.requestsRemaining)
    }

    @Test
    fun `clearCache delegates to subtitle cache`() = runTest {
        // Act
        orchestrator.clearCache()

        // Assert
        coVerify { mockSubtitleCache.clearAll() }
    }

    @Test
    fun `concurrent searches are handled properly`() = runTest {
        // Arrange
        val request1 = createTestSearchRequest().copy(title = "Movie 1")
        val request2 = createTestSearchRequest().copy(title = "Movie 2")
        val request3 = createTestSearchRequest().copy(title = "Movie 3")
        
        val results1 = listOf(createTestSearchResult(SubtitleApiProvider.SUBDL, "movie1-result"))
        val results2 = listOf(createTestSearchResult(SubtitleApiProvider.SUBDB, "movie2-result"))
        val results3 = listOf(createTestSearchResult(SubtitleApiProvider.PODNAPISI, "movie3-result"))
        
        coEvery { mockApiClient1.searchSubtitles(any()) } returns results1
        coEvery { mockApiClient2.searchSubtitles(any()) } returns results2
        coEvery { mockApiClient3.searchSubtitles(any()) } returns results3

        // Act - Start concurrent searches
        val search1 = async { orchestrator.searchSubtitles(request1).first() }
        val search2 = async { orchestrator.searchSubtitles(request2).first() }
        val search3 = async { orchestrator.searchSubtitles(request3).first() }
        
        val allResults = listOf(search1.await(), search2.await(), search3.await())

        // Assert
        assertEquals("Should complete all 3 searches", 3, allResults.size)
        assertTrue("Each search should have results", allResults.all { it.isNotEmpty() })
        
        // Verify all API clients were used
        coVerify(atLeast = 3) { mockApiClient1.searchSubtitles(any()) }
        coVerify(atLeast = 3) { mockApiClient2.searchSubtitles(any()) }
        coVerify(atLeast = 3) { mockApiClient3.searchSubtitles(any()) }
    }

    @Test
    fun `concurrent downloads are handled properly`() = runTest {
        // Arrange
        val result1 = createTestSearchResult(SubtitleApiProvider.SUBDL, "download1")
        val result2 = createTestSearchResult(SubtitleApiProvider.SUBDB, "download2")
        val result3 = createTestSearchResult(SubtitleApiProvider.PODNAPISI, "download3")
        
        coEvery { mockApiClient1.downloadSubtitle(result1) } returns "/cache/download1.srt"
        coEvery { mockApiClient2.downloadSubtitle(result2) } returns "/cache/download2.srt"
        coEvery { mockApiClient3.downloadSubtitle(result3) } returns "/cache/download3.srt"

        // Act - Start concurrent downloads
        val download1 = async { orchestrator.downloadSubtitle(result1).toList() }
        val download2 = async { orchestrator.downloadSubtitle(result2).toList() }
        val download3 = async { orchestrator.downloadSubtitle(result3).toList() }
        
        val allDownloads = listOf(download1.await(), download2.await(), download3.await())

        // Assert
        assertEquals("Should complete all 3 downloads", 3, allDownloads.size)
        assertTrue("All downloads should succeed", allDownloads.all { states ->
            states.last() is SubtitleDownloadState.Success
        })
        
        // Verify all downloads were called
        coVerify { mockApiClient1.downloadSubtitle(result1) }
        coVerify { mockApiClient2.downloadSubtitle(result2) }
        coVerify { mockApiClient3.downloadSubtitle(result3) }
    }

    @Test
    fun `result ranking affects final order`() = runTest {
        // Arrange
        val request = createTestSearchRequest()
        val results = listOf(
            createTestSearchResult(SubtitleApiProvider.SUBDL, "low-score", matchType = MatchType.FUZZY_MATCH),
            createTestSearchResult(SubtitleApiProvider.SUBDB, "high-score", matchType = MatchType.HASH_MATCH),
            createTestSearchResult(SubtitleApiProvider.PODNAPISI, "med-score", matchType = MatchType.TITLE_MATCH)
        )
        
        coEvery { mockApiClient1.searchSubtitles(request) } returns listOf(results[0])
        coEvery { mockApiClient2.searchSubtitles(request) } returns listOf(results[1])
        coEvery { mockApiClient3.searchSubtitles(request) } returns listOf(results[2])
        
        // Mock ranker to return results in score order (highest first)
        val rankedResults = results.sortedByDescending { it.matchScore }
        every { mockResultRanker.rankResults(any(), request) } returns rankedResults

        // Act
        val finalResults = orchestrator.searchSubtitles(request).first()

        // Assert
        assertEquals("Should return ranked results", rankedResults, finalResults)
        assertEquals("Hash match should be first", MatchType.HASH_MATCH, finalResults[0].matchType)
        assertEquals("Title match should be second", MatchType.TITLE_MATCH, finalResults[1].matchType)
        assertEquals("Fuzzy match should be last", MatchType.FUZZY_MATCH, finalResults[2].matchType)
        
        verify { mockResultRanker.rankResults(results, request) }
    }

    @Test
    fun `cache expiration and refresh scenarios`() = runTest {
        // Arrange
        val request = createTestSearchRequest()
        val staleResults = listOf(createTestSearchResult(SubtitleApiProvider.SUBDL, "stale"))
        val freshResults = listOf(createTestSearchResult(SubtitleApiProvider.SUBDB, "fresh"))
        
        // First call returns stale cache
        coEvery { mockSubtitleCache.getCachedResults(request) } returns staleResults andThen emptyList()
        coEvery { mockApiClient1.searchSubtitles(request) } returns freshResults
        coEvery { mockApiClient2.searchSubtitles(request) } returns emptyList()
        coEvery { mockApiClient3.searchSubtitles(request) } returns emptyList()

        // Act
        val firstResults = orchestrator.searchSubtitles(request).first()
        val secondResults = orchestrator.searchSubtitles(request).first()

        // Assert
        assertEquals("First call should return cached results", staleResults, firstResults)
        assertEquals("Second call should return fresh results", freshResults, secondResults)
        
        // Cache should be updated with fresh results
        coVerify { mockSubtitleCache.cacheResults(request, freshResults) }
    }

    @Test
    fun `provider priority and fallback logic`() = runTest {
        // Arrange
        val request = createTestSearchRequest()
        
        // Primary provider fails
        coEvery { mockRateLimiter.canMakeRequest(SubtitleApiProvider.SUBDL) } returns false
        coEvery { mockRateLimiter.canMakeRequest(SubtitleApiProvider.SUBDB) } returns true
        coEvery { mockRateLimiter.canMakeRequest(SubtitleApiProvider.PODNAPISI) } returns true
        
        val fallbackResults = listOf(
            createTestSearchResult(SubtitleApiProvider.SUBDB, "fallback-1"),
            createTestSearchResult(SubtitleApiProvider.PODNAPISI, "fallback-2")
        )
        
        coEvery { mockApiClient2.searchSubtitles(request) } returns listOf(fallbackResults[0])
        coEvery { mockApiClient3.searchSubtitles(request) } returns listOf(fallbackResults[1])

        // Act
        val results = orchestrator.searchSubtitles(request).first()

        // Assert
        assertEquals("Should return results from available providers", 2, results.size)
        assertTrue("Should include SUBDB results", results.any { it.provider == SubtitleApiProvider.SUBDB })
        assertTrue("Should include Podnapisi results", results.any { it.provider == SubtitleApiProvider.PODNAPISI })
        assertFalse("Should not include SUBDL results", results.any { it.provider == SubtitleApiProvider.SUBDL })
        
        // Verify rate-limited provider was not called
        coVerify(exactly = 0) { mockApiClient1.searchSubtitles(request) }
    }
}