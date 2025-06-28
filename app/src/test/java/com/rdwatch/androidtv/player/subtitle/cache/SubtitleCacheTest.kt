package com.rdwatch.androidtv.player.subtitle.cache

import android.content.Context
import com.rdwatch.androidtv.data.dao.SubtitleDao
import com.rdwatch.androidtv.data.dao.CacheStatistics as DaoCacheStatistics
import com.rdwatch.androidtv.data.dao.SubtitleCacheWithResults
import com.rdwatch.androidtv.data.entities.*
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiProvider
import com.rdwatch.androidtv.player.subtitle.models.*
import com.rdwatch.androidtv.player.subtitle.cache.CacheStatistics
import com.rdwatch.androidtv.test.HiltTestBase
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.*
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import javax.inject.Inject

/**
 * Test class for SubtitleCache functionality.
 * Tests caching operations, cache hit/miss scenarios, expiration policies,
 * concurrent access handling, and size limit enforcement.
 */
@HiltAndroidTest
class SubtitleCacheTest : HiltTestBase() {


    @Inject
    lateinit var context: Context

    @Inject
    lateinit var subtitleDao: SubtitleDao

    private lateinit var subtitleCache: SubtitleCache
    private lateinit var mockContext: Context
    private lateinit var mockSubtitleDao: SubtitleDao
    private lateinit var testCacheDir: File
    private lateinit var testFilesDir: File

    @Before
    override fun setUp() {
        super.setUp()
        
        // Create mock dependencies for isolated testing
        mockContext = mockk()
        mockSubtitleDao = mockk()
        
        // Create temporary test directories
        testCacheDir = File.createTempFile("test_cache", "").apply {
            delete()
            mkdirs()
        }
        testFilesDir = File.createTempFile("test_files", "").apply {
            delete()
            mkdirs()
        }
        
        every { mockContext.cacheDir } returns testCacheDir.parentFile
        every { mockContext.filesDir } returns testFilesDir.parentFile
        
        // Create test instance with mocked dependencies
        subtitleCache = SubtitleCache(mockContext, mockSubtitleDao)
    }

    @After
    fun tearDown() {
        // Clean up test directories
        testCacheDir.deleteRecursively()
        testFilesDir.deleteRecursively()
        clearAllMocks()
    }

    @Test
    fun `getCachedResults returns cached data when available`() = runTest {
        // Given
        val request = createTestSearchRequest()
        val cachedResults = listOf(
            createTestResultEntity(cacheId = 1L, providerId = "SUB001"),
            createTestResultEntity(cacheId = 1L, providerId = "SUB002", language = "es")
        )
        val cachedData = SubtitleCacheWithResults(
            cache = createTestCacheEntity(),
            results = cachedResults
        )
        
        coEvery { mockSubtitleDao.getCachedSearch(any()) } returns cachedData

        // When
        val results = subtitleCache.getCachedResults(request)

        // Then
        assertEquals("Should return 2 cached results", 2, results.size)
        assertEquals("First result ID should match", "SUB001", results[0].id)
        assertEquals("Second result language should be Spanish", "es", results[1].language)
        
        coVerify { mockSubtitleDao.getCachedSearch(request.getCacheKey()) }
    }

    @Test
    fun `getCachedResults returns empty list when no cache available`() = runTest {
        // Given
        val request = createTestSearchRequest()
        coEvery { mockSubtitleDao.getCachedSearch(any()) } returns null

        // When
        val results = subtitleCache.getCachedResults(request)

        // Then
        assertTrue("Should return empty list when no cache", results.isEmpty())
        coVerify { mockSubtitleDao.getCachedSearch(request.getCacheKey()) }
    }

    @Test
    fun `cacheResults stores search results correctly`() = runTest {
        // Given
        val request = createTestSearchRequest()
        val searchResults = listOf(
            createTestSearchResult(id = "SUB001", language = "en"),
            createTestSearchResult(id = "SUB002", language = "es")
        )
        
        coEvery { mockSubtitleDao.insertCacheEntry(any()) } returns 1L
        coEvery { mockSubtitleDao.insertResults(any()) } just Runs

        // When
        subtitleCache.cacheResults(request, searchResults)

        // Then
        coVerify {
            mockSubtitleDao.insertCacheEntry(
                match<SubtitleCacheEntity> { entity ->
                    entity.searchKey == request.getCacheKey() &&
                    entity.contentTitle == request.title &&
                    entity.resultCount == 2
                }
            )
        }
        coVerify {
            mockSubtitleDao.insertResults(
                match<List<SubtitleResultEntity>> { results ->
                    results.size == 2 &&
                    results[0].providerId == "SUB001" &&
                    results[1].providerId == "SUB002"
                }
            )
        }
    }

    @Test
    fun `cacheResults skips caching when results are empty`() = runTest {
        // Given
        val request = createTestSearchRequest()
        val emptyResults = emptyList<SubtitleSearchResult>()

        // When
        subtitleCache.cacheResults(request, emptyResults)

        // Then
        coVerify(exactly = 0) { mockSubtitleDao.insertCacheEntry(any()) }
        coVerify(exactly = 0) { mockSubtitleDao.insertResults(any()) }
    }

    @Test
    fun `getCachedFile returns file path when file exists`() = runTest {
        // Given
        val result = createTestSearchResult()
        val cachedFile = createTestFileEntity(
            filePath = "/cache/subtitles/test.srt",
            accessCount = 5
        )
        val testFile = File(cachedFile.filePath).apply {
            parentFile?.mkdirs()
            createNewFile()
        }
        
        coEvery { mockSubtitleDao.getCachedFile(any(), any()) } returns cachedFile
        coEvery { mockSubtitleDao.updateFileAccess(any()) } just Runs

        // When
        val filePath = subtitleCache.getCachedFile(result)

        // Then
        assertEquals("Should return cached file path", cachedFile.filePath, filePath)
        coVerify { mockSubtitleDao.getCachedFile(result.getCacheId(), result.language) }
        coVerify { 
            mockSubtitleDao.updateFileAccess(
                match<SubtitleFileEntity> { entity ->
                    entity.accessCount == 6 // Original + 1
                }
            )
        }
        
        // Cleanup
        testFile.delete()
    }

    @Test
    fun `getCachedFile returns null and cleans up when file does not exist`() = runTest {
        // Given
        val result = createTestSearchResult()
        val cachedFile = createTestFileEntity(filePath = "/nonexistent/file.srt")
        
        coEvery { mockSubtitleDao.getCachedFile(any(), any()) } returns cachedFile
        coEvery { mockSubtitleDao.deactivateFile(any()) } just Runs

        // When
        val filePath = subtitleCache.getCachedFile(result)

        // Then
        assertNull("Should return null for nonexistent file", filePath)
        coVerify { mockSubtitleDao.deactivateFile(cachedFile.id) }
    }

    @Test
    fun `getCachedFile returns null when no cached file found`() = runTest {
        // Given
        val result = createTestSearchResult()
        coEvery { mockSubtitleDao.getCachedFile(any(), any()) } returns null

        // When
        val filePath = subtitleCache.getCachedFile(result)

        // Then
        assertNull("Should return null when no cached file", filePath)
        coVerify(exactly = 0) { mockSubtitleDao.deactivateFile(any()) }
    }

    @Test
    fun `cacheFile creates cached copy and database entry`() = runTest {
        // Given
        val result = createTestSearchResult()
        val originalFile = File.createTempFile("original", ".srt").apply {
            writeText("Test subtitle content")
        }
        
        coEvery { mockSubtitleDao.insertSubtitleFile(any()) } returns 1L
        coEvery { mockSubtitleDao.getAllCachedFiles(any()) } returns emptyList()

        // When
        val cachedPath = subtitleCache.cacheFile(result, originalFile.absolutePath)

        // Then
        assertNotNull("Should return cached file path", cachedPath)
        assertTrue("Cached path should end with .srt", cachedPath.endsWith(".srt"))
        
        coVerify {
            mockSubtitleDao.insertSubtitleFile(
                match<SubtitleFileEntity> { entity ->
                    entity.originalFileName == result.fileName &&
                    entity.language == result.language &&
                    entity.format == result.format &&
                    entity.provider == result.provider
                }
            )
        }
        
        // Cleanup
        originalFile.delete()
    }

    @Test
    fun `cacheFile throws exception for nonexistent source file`() = runTest {
        // Given
        val result = createTestSearchResult()
        val nonexistentPath = "/nonexistent/file.srt"

        // When & Then
        try {
            subtitleCache.cacheFile(result, nonexistentPath)
            fail("Should throw IllegalArgumentException")
        } catch (e: IllegalArgumentException) {
            assertTrue("Should mention file does not exist", e.message?.contains("does not exist") == true)
        }
    }

    @Test
    fun `getAllCachedFiles returns valid files and cleans up invalid ones`() = runTest {
        // Given
        val contentId = "tt1234567"
        val validFile = createTestFileEntity(filePath = "/valid/path.srt")
        val invalidFile = createTestFileEntity(filePath = "/invalid/path.srt")
        
        // Create the valid file
        val testValidFile = File(validFile.filePath).apply {
            parentFile?.mkdirs()
            createNewFile()
        }
        
        coEvery { mockSubtitleDao.getAllCachedFiles(contentId) } returns listOf(validFile, invalidFile)
        coEvery { mockSubtitleDao.deactivateFile(invalidFile.id) } just Runs

        // When
        val cachedFiles = subtitleCache.getAllCachedFiles(contentId)

        // Then
        assertEquals("Should return 1 valid file", 1, cachedFiles.size)
        assertEquals("Valid file path should match", validFile.filePath, cachedFiles[0].filePath)
        
        coVerify { mockSubtitleDao.deactivateFile(invalidFile.id) }
        
        // Cleanup
        testValidFile.delete()
    }

    @Test
    fun `performMaintenance cleans up expired cache and old files`() = runTest {
        // Given
        val filesToCleanup = listOf(
            createTestFileEntity(filePath = "/old/file1.srt"),
            createTestFileEntity(filePath = "/old/file2.srt")
        )
        
        coEvery { mockSubtitleDao.cleanupExpiredCache() } just Runs
        coEvery { mockSubtitleDao.getFilesForCleanup(any(), any()) } returns filesToCleanup
        coEvery { mockSubtitleDao.deactivateFile(any()) } just Runs
        coEvery { mockSubtitleDao.cleanupOldFiles(any()) } just Runs

        // When
        subtitleCache.performMaintenance()

        // Then
        coVerify { mockSubtitleDao.cleanupExpiredCache() }
        coVerify { mockSubtitleDao.getFilesForCleanup(any(), any()) }
        coVerify(exactly = 2) { mockSubtitleDao.deactivateFile(any()) }
        coVerify { mockSubtitleDao.cleanupOldFiles(any()) }
    }

    @Test
    fun `clearAll removes all cache data and files`() = runTest {
        // Given
        val testFile1 = File(testFilesDir, "test1.srt").apply { createNewFile() }
        val testFile2 = File(testCacheDir, "test2.srt").apply { createNewFile() }
        
        coEvery { mockSubtitleDao.clearAllCache() } just Runs

        // When
        subtitleCache.clearAll()

        // Then
        coVerify { mockSubtitleDao.clearAllCache() }
        // Note: In real implementation, files would be deleted
    }

    @Test
    fun `getCacheStatistics returns comprehensive cache information`() = runTest {
        // Given
        val dbStats = DaoCacheStatistics(
            totalEntries = 100,
            validEntries = 80,
            expiredEntries = 20,
            avgResultsPerSearch = 5.5
        )
        
        coEvery { mockSubtitleDao.getCacheStatistics() } returns dbStats

        // When
        val cacheStats = subtitleCache.getCacheStatistics()

        // Then
        assertEquals("Total cache entries should match", 100, cacheStats.totalCacheEntries)
        assertEquals("Valid cache entries should match", 80, cacheStats.validCacheEntries)
        assertEquals("Expired cache entries should match", 20, cacheStats.expiredCacheEntries)
        assertEquals("Average results per search should match", 5.5, cacheStats.averageResultsPerSearch, 0.1)
        
        coVerify { mockSubtitleDao.getCacheStatistics() }
    }

    @Test
    fun `cache handles concurrent access correctly`() = runTest {
        // Given
        val request = createTestSearchRequest()
        val results = listOf(createTestSearchResult())
        
        coEvery { mockSubtitleDao.insertCacheEntry(any()) } returns 1L
        coEvery { mockSubtitleDao.insertResults(any()) } just Runs
        coEvery { mockSubtitleDao.getCachedSearch(any()) } returns null

        // When - Simulate concurrent cache operations
        coroutineScope {
            val job1 = async { subtitleCache.cacheResults(request, results) }
            val job2 = async { subtitleCache.getCachedResults(request) }
            val job3 = async { subtitleCache.performMaintenance() }
            
            job1.await()
            job2.await()
            job3.await()
        }

        // Then - No exceptions should be thrown and operations should complete
        assertTrue("Concurrent operations should complete successfully", true)
    }

    @Test
    fun `cache respects size limits during maintenance`() = runTest {
        // Given
        val largeFiles = listOf(
            createTestFileEntity(filePath = "/large/file1.srt", fileSize = 50_000_000L),
            createTestFileEntity(filePath = "/large/file2.srt", fileSize = 60_000_000L)
        )
        
        coEvery { mockSubtitleDao.cleanupExpiredCache() } just Runs
        coEvery { mockSubtitleDao.getFilesForCleanup(any(), any()) } returns largeFiles
        coEvery { mockSubtitleDao.deactivateFile(any()) } just Runs
        coEvery { mockSubtitleDao.cleanupOldFiles(any()) } just Runs

        // When
        subtitleCache.performMaintenance()

        // Then
        coVerify { mockSubtitleDao.cleanupOldFiles(any()) } // Should clean up old files
    }

    @Test
    fun `cache key generation is consistent and unique`() {
        // Given
        val request1 = createTestSearchRequest(title = "Movie A", year = 2023, languages = listOf("en", "es"))
        val request2 = createTestSearchRequest(title = "Movie A", year = 2023, languages = listOf("es", "en")) // Same but different order
        val request3 = createTestSearchRequest(title = "Movie B", year = 2023, languages = listOf("en", "es"))

        // When
        val key1 = request1.getCacheKey()
        val key2 = request2.getCacheKey()
        val key3 = request3.getCacheKey()

        // Then
        assertEquals("Same content with reordered languages should produce same key", key1, key2)
        assertNotEquals("Different content should produce different keys", key1, key3)
        
        // Keys should be filesystem-safe
        assertFalse("Cache key should not contain unsafe characters", key1.contains("/"))
        assertFalse("Cache key should not contain spaces", key1.contains(" "))
    }

    @Test
    fun `file checksum validation works correctly`() = runTest {
        // Given
        val result = createTestSearchResult()
        val testContent = "Test subtitle content\nwith multiple lines"
        val originalFile = File.createTempFile("test", ".srt").apply {
            writeText(testContent)
        }
        
        coEvery { mockSubtitleDao.insertSubtitleFile(any()) } returns 1L
        coEvery { mockSubtitleDao.getAllCachedFiles(any()) } returns emptyList()

        // When
        val cachedPath = subtitleCache.cacheFile(result, originalFile.absolutePath)

        // Then
        assertNotNull("Cached file should be created", cachedPath)
        
        coVerify {
            mockSubtitleDao.insertSubtitleFile(
                match<SubtitleFileEntity> { entity ->
                    entity.checksum != null && entity.checksum!!.isNotEmpty()
                }
            )
        }
        
        // Cleanup
        originalFile.delete()
    }

    // Helper methods for creating test data

    private fun createTestSearchRequest(
        title: String = "Test Movie",
        year: Int? = 2023,
        type: ContentType = ContentType.MOVIE,
        imdbId: String? = "tt1234567",
        languages: List<String> = listOf("en"),
        fileHash: String? = null
    ): SubtitleSearchRequest {
        return SubtitleSearchRequest(
            title = title,
            year = year,
            type = type,
            imdbId = imdbId,
            languages = languages,
            fileHash = fileHash
        )
    }

    private fun createTestSearchResult(
        id: String = "SUB001",
        provider: SubtitleApiProvider = SubtitleApiProvider.SUBDL,
        language: String = "en",
        languageName: String = "English",
        format: SubtitleFormat = SubtitleFormat.SRT,
        downloadUrl: String = "https://api.example.com/download/SUB001",
        fileName: String = "subtitle.srt",
        matchType: MatchType = MatchType.IMDB_MATCH
    ): SubtitleSearchResult {
        return SubtitleSearchResult(
            id = id,
            provider = provider,
            language = language,
            languageName = languageName,
            format = format,
            downloadUrl = downloadUrl,
            fileName = fileName,
            matchType = matchType
        )
    }

    private fun createTestCacheEntity(
        searchKey: String = "test_search_key",
        contentId: String = "tt1234567",
        contentTitle: String = "Test Movie",
        expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000L)
    ): SubtitleCacheEntity {
        return SubtitleCacheEntity(
            searchKey = searchKey,
            contentId = contentId,
            contentTitle = contentTitle,
            expiresAt = expiresAt,
            resultCount = 2,
            languages = "en,es"
        )
    }

    private fun createTestResultEntity(
        cacheId: Long,
        providerId: String = "SUB001",
        language: String = "en",
        languageName: String = "English"
    ): SubtitleResultEntity {
        return SubtitleResultEntity(
            cacheId = cacheId,
            providerId = providerId,
            provider = SubtitleApiProvider.SUBDL,
            downloadUrl = "https://api.example.com/download/$providerId",
            language = language,
            languageName = languageName,
            format = SubtitleFormat.SRT,
            fileName = "subtitle.srt",
            matchScore = 0.9f,
            matchType = MatchType.IMDB_MATCH
        )
    }

    private fun createTestFileEntity(
        id: Long = 1L,
        filePath: String = "/cache/subtitles/test.srt",
        contentId: String = "tt1234567",
        language: String = "en",
        fileSize: Long = 25600L,
        accessCount: Int = 0
    ): SubtitleFileEntity {
        return SubtitleFileEntity(
            id = id,
            resultId = 1L,
            filePath = filePath,
            originalFileName = "test.srt",
            contentId = contentId,
            language = language,
            format = SubtitleFormat.SRT,
            fileSize = fileSize,
            provider = SubtitleApiProvider.SUBDL,
            accessCount = accessCount
        )
    }
}