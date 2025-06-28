package com.rdwatch.androidtv.data.dao

import com.rdwatch.androidtv.data.entities.*
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiProvider
import com.rdwatch.androidtv.player.subtitle.models.SubtitleFormat
import com.rdwatch.androidtv.player.subtitle.models.MatchType
import com.rdwatch.androidtv.test.HiltTestBase
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import javax.inject.Inject

/**
 * Test class for SubtitleDao database operations.
 * Tests CRUD operations, query performance, indexing, cascade deletes, and database migration validation.
 */
@HiltAndroidTest
class SubtitleDaoTest : HiltTestBase() {

    @Inject
    lateinit var subtitleDao: SubtitleDao

    @Test
    fun insertAndRetrieveCacheEntry() = runTest {
        // Given
        val cacheEntry = createTestCacheEntity(
            searchKey = "test_movie_2023_en",
            contentId = "tt1234567",
            contentTitle = "Test Movie"
        )

        // When
        val cacheId = subtitleDao.insertCacheEntry(cacheEntry)
        val retrieved = subtitleDao.getCachedSearch(cacheEntry.searchKey)

        // Then
        assertTrue("Insert should return positive ID", cacheId > 0)
        assertNotNull("Retrieved cache should not be null", retrieved)
        assertEquals("Search keys should match", cacheEntry.searchKey, retrieved?.cache?.searchKey)
        assertEquals("Content IDs should match", cacheEntry.contentId, retrieved?.cache?.contentId)
        assertEquals("Content titles should match", cacheEntry.contentTitle, retrieved?.cache?.contentTitle)
        assertTrue("Results list should be empty initially", retrieved?.results?.isEmpty() == true)
    }

    @Test
    fun insertCacheEntryWithResults() = runTest {
        // Given
        val cacheEntry = createTestCacheEntity(resultCount = 2)
        val cacheId = subtitleDao.insertCacheEntry(cacheEntry)
        
        val results = listOf(
            createTestResultEntity(cacheId, "SUB001", "en", "English"),
            createTestResultEntity(cacheId, "SUB002", "es", "Spanish")
        )

        // When
        subtitleDao.insertResults(results)
        val cachedData = subtitleDao.getCachedSearch(cacheEntry.searchKey)

        // Then
        assertNotNull("Cached data should not be null", cachedData)
        assertEquals("Should have 2 results", 2, cachedData?.results?.size)
        
        val englishResult = cachedData?.results?.find { it.language == "en" }
        assertNotNull("English result should exist", englishResult)
        assertEquals("Provider ID should match", "SUB001", englishResult?.providerId)
        assertEquals("Language name should match", "English", englishResult?.languageName)
        
        val spanishResult = cachedData?.results?.find { it.language == "es" }
        assertNotNull("Spanish result should exist", spanishResult)
        assertEquals("Provider ID should match", "SUB002", spanishResult?.providerId)
        assertEquals("Language name should match", "Spanish", spanishResult?.languageName)
    }

    @Test
    fun getCachedSearchWithExpiredEntry() = runTest {
        // Given - Create an expired cache entry
        val expiredTime = System.currentTimeMillis() - (25 * 60 * 60 * 1000L) // 25 hours ago
        val cacheEntry = createTestCacheEntity(expiresAt = expiredTime)
        subtitleDao.insertCacheEntry(cacheEntry)

        // When
        val retrieved = subtitleDao.getCachedSearch(cacheEntry.searchKey)

        // Then
        assertNull("Expired cache entry should not be retrieved", retrieved)
    }

    @Test
    fun cleanupExpiredCache() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val validExpiry = currentTime + (23 * 60 * 60 * 1000L) // 23 hours from now
        val expiredTime = currentTime - (1 * 60 * 60 * 1000L) // 1 hour ago
        
        val validCache = createTestCacheEntity(searchKey = "valid_cache", expiresAt = validExpiry)
        val expiredCache = createTestCacheEntity(searchKey = "expired_cache", expiresAt = expiredTime)
        
        val validCacheId = subtitleDao.insertCacheEntry(validCache)
        val expiredCacheId = subtitleDao.insertCacheEntry(expiredCache)
        
        // Add results to both caches
        subtitleDao.insertResults(listOf(createTestResultEntity(validCacheId, "SUB001")))
        subtitleDao.insertResults(listOf(createTestResultEntity(expiredCacheId, "SUB002")))

        // When
        subtitleDao.cleanupExpiredCache()

        // Then
        val validRetrieved = subtitleDao.getCachedSearch(validCache.searchKey)
        val expiredRetrieved = subtitleDao.getCachedSearch(expiredCache.searchKey)
        
        assertNotNull("Valid cache should still exist", validRetrieved)
        assertNull("Expired cache should be cleaned up", expiredRetrieved)
    }

    @Test
    fun getCacheStatistics() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val validExpiry = currentTime + (23 * 60 * 60 * 1000L)
        val expiredTime = currentTime - (1 * 60 * 60 * 1000L)
        
        // Insert valid and expired cache entries
        subtitleDao.insertCacheEntry(createTestCacheEntity(searchKey = "valid1", expiresAt = validExpiry, resultCount = 5))
        subtitleDao.insertCacheEntry(createTestCacheEntity(searchKey = "valid2", expiresAt = validExpiry, resultCount = 3))
        subtitleDao.insertCacheEntry(createTestCacheEntity(searchKey = "expired1", expiresAt = expiredTime, resultCount = 2))

        // When
        val stats = subtitleDao.getCacheStatistics()

        // Then
        assertEquals("Total entries should be 3", 3, stats.totalEntries)
        assertEquals("Valid entries should be 2", 2, stats.validEntries)
        assertEquals("Expired entries should be 1", 1, stats.expiredEntries)
        assertEquals("Average results per search should be 3.33", 3.33, stats.avgResultsPerSearch, 0.1)
    }

    @Test
    fun insertAndRetrieveCachedFile() = runTest {
        // Given
        val fileEntity = createTestFileEntity(
            filePath = "/cache/subtitles/test_movie_en.srt",
            contentId = "tt1234567",
            language = "en"
        )

        // When
        val fileId = subtitleDao.insertSubtitleFile(fileEntity)
        val retrieved = subtitleDao.getCachedFile(fileEntity.contentId, fileEntity.language)

        // Then
        assertTrue("Insert should return positive ID", fileId > 0)
        assertNotNull("Retrieved file should not be null", retrieved)
        assertEquals("File paths should match", fileEntity.filePath, retrieved?.filePath)
        assertEquals("Content IDs should match", fileEntity.contentId, retrieved?.contentId)
        assertEquals("Languages should match", fileEntity.language, retrieved?.language)
        assertTrue("File should be active", retrieved?.isActive == true)
    }

    @Test
    fun getAllCachedFilesForContent() = runTest {
        // Given
        val contentId = "tt1234567"
        val files = listOf(
            createTestFileEntity(filePath = "/cache/subtitles/movie_en.srt", contentId = contentId, language = "en"),
            createTestFileEntity(filePath = "/cache/subtitles/movie_es.srt", contentId = contentId, language = "es"),
            createTestFileEntity(filePath = "/cache/subtitles/movie_fr.srt", contentId = contentId, language = "fr")
        )

        // When
        files.forEach { subtitleDao.insertSubtitleFile(it) }
        val retrievedFiles = subtitleDao.getAllCachedFiles(contentId)

        // Then
        assertEquals("Should have 3 cached files", 3, retrievedFiles.size)
        val languages = retrievedFiles.map { it.language }
        assertTrue("Should contain English", languages.contains("en"))
        assertTrue("Should contain Spanish", languages.contains("es"))
        assertTrue("Should contain French", languages.contains("fr"))
    }

    @Test
    fun updateFileAccess() = runTest {
        // Given
        val originalFile = createTestFileEntity(accessCount = 0)
        val fileId = subtitleDao.insertSubtitleFile(originalFile)
        
        val updatedFile = originalFile.copy(
            id = fileId,
            accessCount = 5,
            lastAccessTime = System.currentTimeMillis()
        )

        // When
        subtitleDao.updateFileAccess(updatedFile)
        val retrieved = subtitleDao.getCachedFile(originalFile.contentId, originalFile.language)

        // Then
        assertNotNull("Updated file should be retrievable", retrieved)
        assertEquals("Access count should be updated", 5, retrieved?.accessCount)
    }

    @Test
    fun deactivateFile() = runTest {
        // Given
        val fileEntity = createTestFileEntity()
        val fileId = subtitleDao.insertSubtitleFile(fileEntity)

        // When
        subtitleDao.deactivateFile(fileId)
        val retrieved = subtitleDao.getCachedFile(fileEntity.contentId, fileEntity.language)

        // Then
        assertNull("Deactivated file should not be retrieved", retrieved)
    }

    @Test
    fun cleanupOldFiles() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val oldTime = currentTime - (8 * 24 * 60 * 60 * 1000L) // 8 days ago
        val recentTime = currentTime - (6 * 24 * 60 * 60 * 1000L) // 6 days ago
        
        val oldFile = createTestFileEntity(
            filePath = "/cache/subtitles/old_movie.srt",
            downloadTimestamp = oldTime,
            accessCount = 0
        )
        val recentFile = createTestFileEntity(
            filePath = "/cache/subtitles/recent_movie.srt",
            downloadTimestamp = recentTime,
            accessCount = 2
        )

        subtitleDao.insertSubtitleFile(oldFile)
        subtitleDao.insertSubtitleFile(recentFile)

        // When
        val cutoffTime = currentTime - (7 * 24 * 60 * 60 * 1000L) // 7 days ago
        subtitleDao.cleanupOldFiles(cutoffTime, minAccessCount = 1)

        // Then
        val oldRetrieved = subtitleDao.getCachedFile(oldFile.contentId, oldFile.language)
        val recentRetrieved = subtitleDao.getCachedFile(recentFile.contentId, recentFile.language)
        
        assertNull("Old file should be cleaned up", oldRetrieved)
        assertNotNull("Recent file should remain", recentRetrieved)
    }

    @Test
    fun getFilesForCleanup() = runTest {
        // Given
        val currentTime = System.currentTimeMillis()
        val oldDownloadTime = currentTime - (8 * 24 * 60 * 60 * 1000L) // 8 days ago
        val oldAccessTime = currentTime - (25 * 60 * 60 * 1000L) // 25 hours ago
        
        val fileForCleanup = createTestFileEntity(
            downloadTimestamp = oldDownloadTime,
            lastAccessTime = oldAccessTime
        )
        val recentFile = createTestFileEntity(
            downloadTimestamp = currentTime - (1 * 24 * 60 * 60 * 1000L), // 1 day ago
            lastAccessTime = currentTime - (1 * 60 * 60 * 1000L) // 1 hour ago
        )

        subtitleDao.insertSubtitleFile(fileForCleanup)
        subtitleDao.insertSubtitleFile(recentFile)

        // When
        val cutoffTime = currentTime - (7 * 24 * 60 * 60 * 1000L) // 7 days ago
        val lastAccessCutoff = currentTime - (24 * 60 * 60 * 1000L) // 24 hours ago
        val filesToCleanup = subtitleDao.getFilesForCleanup(cutoffTime, lastAccessCutoff)

        // Then
        assertEquals("Should have 1 file for cleanup", 1, filesToCleanup.size)
        assertEquals("Cleanup file should match", fileForCleanup.filePath, filesToCleanup[0].filePath)
    }

    @Test
    fun insertAndRetrieveProviderStats() = runTest {
        // Given
        val stats = createTestProviderStats(
            provider = SubtitleApiProvider.SUBDL,
            totalRequests = 100,
            successfulRequests = 95,
            failedRequests = 5
        )

        // When
        subtitleDao.updateProviderStats(stats)
        val retrieved = subtitleDao.getProviderStats(SubtitleApiProvider.SUBDL)

        // Then
        assertNotNull("Retrieved stats should not be null", retrieved)
        assertEquals("Provider should match", SubtitleApiProvider.SUBDL, retrieved?.provider)
        assertEquals("Total requests should match", 100, retrieved?.totalRequests)
        assertEquals("Successful requests should match", 95, retrieved?.successfulRequests)
        assertEquals("Failed requests should match", 5, retrieved?.failedRequests)
        assertEquals("Success rate should be 0.95", 0.95f, retrieved?.getSuccessRate() ?: 0.0f, 0.01f)
    }

    @Test
    fun getAllProviderStats() = runTest {
        // Given
        val stats1 = createTestProviderStats(provider = SubtitleApiProvider.SUBDL, totalRequests = 100)
        val stats2 = createTestProviderStats(provider = SubtitleApiProvider.SUBDB, totalRequests = 50)
        val stats3 = createTestProviderStats(provider = SubtitleApiProvider.PODNAPISI, totalRequests = 200)

        // When
        subtitleDao.updateProviderStats(stats1)
        subtitleDao.updateProviderStats(stats2)
        subtitleDao.updateProviderStats(stats3)
        
        val allStats = subtitleDao.getAllProviderStats()

        // Then
        assertEquals("Should have 3 provider stats", 3, allStats.size)
        // Should be ordered by totalRequests DESC
        assertEquals("First should be PODNAPISI", SubtitleApiProvider.PODNAPISI, allStats[0].provider)
        assertEquals("Second should be SUBDL", SubtitleApiProvider.SUBDL, allStats[1].provider)
        assertEquals("Third should be SUBDB", SubtitleApiProvider.SUBDB, allStats[2].provider)
    }

    @Test
    fun getAllProviderHealth() = runTest {
        // Given
        val healthyStats = createTestProviderStats(
            provider = SubtitleApiProvider.SUBDL,
            isEnabled = true,
            consecutiveFailures = 0,
            lastSuccessTime = System.currentTimeMillis()
        )
        val unhealthyStats = createTestProviderStats(
            provider = SubtitleApiProvider.SUBDB,
            isEnabled = false,
            consecutiveFailures = 5,
            lastFailureTime = System.currentTimeMillis()
        )

        subtitleDao.updateProviderStats(healthyStats)
        subtitleDao.updateProviderStats(unhealthyStats)

        // When
        val healthStatuses = subtitleDao.getAllProviderHealth().first()

        // Then
        assertEquals("Should have 2 health statuses", 2, healthStatuses.size)
        
        val subdlHealth = healthStatuses.find { it.provider == SubtitleApiProvider.SUBDL }
        assertNotNull("SUBDL health should exist", subdlHealth)
        assertTrue("SUBDL should be enabled", subdlHealth?.isEnabled == true)
        assertEquals("SUBDL should have 0 failures", 0, subdlHealth?.consecutiveFailures ?: -1)
        
        val subdbHealth = healthStatuses.find { it.provider == SubtitleApiProvider.SUBDB }
        assertNotNull("SUBDB health should exist", subdbHealth)
        assertFalse("SUBDB should be disabled", subdbHealth?.isEnabled == true)
        assertEquals("SUBDB should have 5 failures", 5, subdbHealth?.consecutiveFailures ?: -1)
    }

    @Test
    fun searchCachedContent() = runTest {
        // Given
        val cache1 = createTestCacheEntity(
            searchKey = "search1",
            contentTitle = "The Matrix",
            contentId = "tt0133093"
        )
        val cache2 = createTestCacheEntity(
            searchKey = "search2",
            contentTitle = "Matrix Reloaded",
            contentId = "tt0234215"
        )
        val cache3 = createTestCacheEntity(
            searchKey = "search3",
            contentTitle = "The Avengers",
            contentId = "tt0848228"
        )

        subtitleDao.insertCacheEntry(cache1)
        subtitleDao.insertCacheEntry(cache2)
        subtitleDao.insertCacheEntry(cache3)

        // When
        val matrixResults = subtitleDao.searchCachedContent("Matrix", limit = 10)

        // Then
        assertEquals("Should find 2 Matrix movies", 2, matrixResults.size)
        val titles = matrixResults.map { it.cache.contentTitle }
        assertTrue("Should contain The Matrix", titles.contains("The Matrix"))
        assertTrue("Should contain Matrix Reloaded", titles.contains("Matrix Reloaded"))
        assertFalse("Should not contain The Avengers", titles.contains("The Avengers"))
    }

    @Test
    fun getPopularLanguages() = runTest {
        // Given
        val cacheId1 = subtitleDao.insertCacheEntry(createTestCacheEntity(searchKey = "cache1"))
        val cacheId2 = subtitleDao.insertCacheEntry(createTestCacheEntity(searchKey = "cache2"))
        
        // Insert results with different languages
        val results = listOf(
            createTestResultEntity(cacheId1, "SUB001", "en", "English"),
            createTestResultEntity(cacheId1, "SUB002", "es", "Spanish"),
            createTestResultEntity(cacheId2, "SUB003", "en", "English"),
            createTestResultEntity(cacheId2, "SUB004", "en", "English"),
            createTestResultEntity(cacheId2, "SUB005", "fr", "French")
        )
        subtitleDao.insertResults(results)

        // When
        val popularLanguages = subtitleDao.getPopularLanguages(limit = 5)

        // Then
        assertEquals("Should have 3 languages", 3, popularLanguages.size)
        // Should be ordered by count DESC
        assertEquals("English should be most popular", "en", popularLanguages[0].language)
        assertEquals("English should have 3 results", 3, popularLanguages[0].count)
        assertEquals("Spanish should be second", "es", popularLanguages[1].language)
        assertEquals("French should be third", "fr", popularLanguages[2].language)
    }

    @Test
    fun getProviderPerformanceMetrics() = runTest {
        // Given
        val cacheId = subtitleDao.insertCacheEntry(createTestCacheEntity())
        
        val results = listOf(
            createTestResultEntity(cacheId, "SUB001", provider = SubtitleApiProvider.SUBDL, matchScore = 0.9f, downloadCount = 100),
            createTestResultEntity(cacheId, "SUB002", provider = SubtitleApiProvider.SUBDL, matchScore = 0.8f, downloadCount = 50, isVerified = true),
            createTestResultEntity(cacheId, "SUB003", provider = SubtitleApiProvider.SUBDB, matchScore = 0.7f, downloadCount = 200)
        )
        subtitleDao.insertResults(results)

        // When
        val metrics = subtitleDao.getProviderPerformanceMetrics()

        // Then
        assertEquals("Should have 2 providers", 2, metrics.size)
        
        val subdlMetrics = metrics.find { it.provider == SubtitleApiProvider.SUBDL }
        assertNotNull("SUBDL metrics should exist", subdlMetrics)
        assertEquals("SUBDL should have 2 results", 2, subdlMetrics?.totalResults)
        assertEquals("SUBDL avg match score should be 0.85", 0.85, subdlMetrics?.avgMatchScore ?: 0.0, 0.01)
        assertEquals("SUBDL should have 1 verified result", 1, subdlMetrics?.verifiedCount)
        
        val subdbMetrics = metrics.find { it.provider == SubtitleApiProvider.SUBDB }
        assertNotNull("SUBDB metrics should exist", subdbMetrics)
        assertEquals("SUBDB should have 1 result", 1, subdbMetrics?.totalResults)
        assertEquals("SUBDB avg match score should be 0.7", 0.7, subdbMetrics?.avgMatchScore ?: 0.0, 0.01)
    }

    @Test
    fun getDatabaseSize() = runTest {
        // Given
        val cacheId = subtitleDao.insertCacheEntry(createTestCacheEntity())
        subtitleDao.insertResults(listOf(createTestResultEntity(cacheId, "SUB001")))
        subtitleDao.insertSubtitleFile(createTestFileEntity())
        subtitleDao.updateProviderStats(createTestProviderStats())

        // When
        val sizeInfo = subtitleDao.getDatabaseSize()

        // Then
        assertEquals("Should have 1 cache entry", 1, sizeInfo.cacheEntries)
        assertEquals("Should have 1 result entry", 1, sizeInfo.resultEntries)
        assertEquals("Should have 1 file entry", 1, sizeInfo.fileEntries)
        assertEquals("Should have 1 provider entry", 1, sizeInfo.providerEntries)
        assertEquals("Total should be 4", 4, sizeInfo.totalEntries)
    }

    @Test
    fun clearAllCache() = runTest {
        // Given
        val cacheId = subtitleDao.insertCacheEntry(createTestCacheEntity())
        subtitleDao.insertResults(listOf(createTestResultEntity(cacheId, "SUB001")))
        subtitleDao.insertSubtitleFile(createTestFileEntity())
        subtitleDao.updateProviderStats(createTestProviderStats())

        // When
        subtitleDao.clearAllCache()

        // Then
        val sizeInfo = subtitleDao.getDatabaseSize()
        assertEquals("All entries should be cleared", 0, sizeInfo.totalEntries)
    }

    @Test
    fun cascadeDeleteOnCacheRemoval() = runTest {
        // Given
        val cacheEntry = createTestCacheEntity()
        val cacheId = subtitleDao.insertCacheEntry(cacheEntry)
        
        val results = listOf(
            createTestResultEntity(cacheId, "SUB001"),
            createTestResultEntity(cacheId, "SUB002")
        )
        subtitleDao.insertResults(results)

        // Verify results exist
        val cachedData = subtitleDao.getCachedSearch(cacheEntry.searchKey)
        assertEquals("Should have 2 results before deletion", 2, cachedData?.results?.size)

        // When - Delete cache entry (should cascade to results)
        subtitleDao.clearAllCacheEntries()

        // Then
        val sizeInfo = subtitleDao.getDatabaseSize()
        assertEquals("Cache entries should be 0", 0, sizeInfo.cacheEntries)
        assertEquals("Result entries should be 0 (cascaded)", 0, sizeInfo.resultEntries)
    }

    // Helper methods for creating test entities

    private fun createTestCacheEntity(
        searchKey: String = "test_search_key",
        contentId: String = "tt1234567",
        contentTitle: String = "Test Movie",
        contentYear: Int? = 2023,
        season: Int? = null,
        episode: Int? = null,
        expiresAt: Long = System.currentTimeMillis() + (24 * 60 * 60 * 1000L), // 24 hours from now
        resultCount: Int = 0,
        languages: String = "en,es",
        hasFileHash: Boolean = false,
        hasImdbId: Boolean = true,
        hasTmdbId: Boolean = false
    ): SubtitleCacheEntity {
        return SubtitleCacheEntity(
            searchKey = searchKey,
            contentId = contentId,
            contentTitle = contentTitle,
            contentYear = contentYear,
            season = season,
            episode = episode,
            expiresAt = expiresAt,
            resultCount = resultCount,
            languages = languages,
            hasFileHash = hasFileHash,
            hasImdbId = hasImdbId,
            hasTmdbId = hasTmdbId
        )
    }

    private fun createTestResultEntity(
        cacheId: Long,
        providerId: String = "SUB001",
        language: String = "en",
        languageName: String = "English",
        provider: SubtitleApiProvider = SubtitleApiProvider.SUBDL,
        format: SubtitleFormat = SubtitleFormat.SRT,
        downloadUrl: String = "https://api.example.com/download/$providerId",
        fileName: String = "subtitle.srt",
        fileSize: Long? = 25600L,
        downloadCount: Int? = 100,
        rating: Float? = 4.5f,
        matchScore: Float = 0.9f,
        matchType: MatchType = MatchType.IMDB_MATCH,
        uploadDate: Long? = System.currentTimeMillis(),
        uploader: String? = "test_user",
        isVerified: Boolean = false,
        hearingImpaired: Boolean? = false,
        releaseGroup: String? = "YIFY",
        version: String? = "v1.0",
        comments: String? = null,
        contentHash: String? = null
    ): SubtitleResultEntity {
        return SubtitleResultEntity(
            cacheId = cacheId,
            providerId = providerId,
            provider = provider,
            downloadUrl = downloadUrl,
            language = language,
            languageName = languageName,
            format = format,
            fileName = fileName,
            fileSize = fileSize,
            downloadCount = downloadCount,
            rating = rating,
            matchScore = matchScore,
            matchType = matchType,
            uploadDate = uploadDate,
            uploader = uploader,
            isVerified = isVerified,
            hearingImpaired = hearingImpaired,
            releaseGroup = releaseGroup,
            version = version,
            comments = comments,
            contentHash = contentHash
        )
    }

    private fun createTestFileEntity(
        resultId: Long = 1L,
        filePath: String = "/cache/subtitles/test.srt",
        originalFileName: String = "test.srt",
        contentId: String = "tt1234567",
        language: String = "en",
        format: SubtitleFormat = SubtitleFormat.SRT,
        fileSize: Long = 25600L,
        downloadTimestamp: Long = System.currentTimeMillis(),
        provider: SubtitleApiProvider = SubtitleApiProvider.SUBDL,
        downloadUrl: String? = "https://api.example.com/download/test",
        isActive: Boolean = true,
        checksum: String? = "abc123def456",
        lastAccessTime: Long = System.currentTimeMillis(),
        accessCount: Int = 0
    ): SubtitleFileEntity {
        return SubtitleFileEntity(
            resultId = resultId,
            filePath = filePath,
            originalFileName = originalFileName,
            contentId = contentId,
            language = language,
            format = format,
            fileSize = fileSize,
            downloadTimestamp = downloadTimestamp,
            provider = provider,
            downloadUrl = downloadUrl,
            isActive = isActive,
            checksum = checksum,
            lastAccessTime = lastAccessTime,
            accessCount = accessCount
        )
    }

    private fun createTestProviderStats(
        provider: SubtitleApiProvider = SubtitleApiProvider.SUBDL,
        totalRequests: Long = 100L,
        successfulRequests: Long = 95L,
        failedRequests: Long = 5L,
        lastRequestTime: Long? = System.currentTimeMillis(),
        requestsInCurrentWindow: Int = 10,
        windowResetTime: Long = System.currentTimeMillis() + (60 * 60 * 1000L), // 1 hour from now
        isEnabled: Boolean = true,
        lastSuccessTime: Long? = System.currentTimeMillis(),
        lastFailureTime: Long? = null,
        lastFailureReason: String? = null,
        consecutiveFailures: Int = 0,
        averageResponseTimeMs: Long = 500L
    ): SubtitleProviderStatsEntity {
        return SubtitleProviderStatsEntity(
            provider = provider,
            totalRequests = totalRequests,
            successfulRequests = successfulRequests,
            failedRequests = failedRequests,
            lastRequestTime = lastRequestTime,
            requestsInCurrentWindow = requestsInCurrentWindow,
            windowResetTime = windowResetTime,
            isEnabled = isEnabled,
            lastSuccessTime = lastSuccessTime,
            lastFailureTime = lastFailureTime,
            lastFailureReason = lastFailureReason,
            consecutiveFailures = consecutiveFailures,
            averageResponseTimeMs = averageResponseTimeMs
        )
    }
}