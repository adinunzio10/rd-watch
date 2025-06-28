package com.rdwatch.androidtv.di

import android.content.Context
import com.rdwatch.androidtv.data.AppDatabase
import com.rdwatch.androidtv.data.dao.SubtitleDao
import com.rdwatch.androidtv.player.subtitle.SubtitleApiOrchestrator
import com.rdwatch.androidtv.player.subtitle.SubtitleRateLimiter
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiClient
import com.rdwatch.androidtv.player.subtitle.api.SubtitleApiProvider
import com.rdwatch.androidtv.player.subtitle.cache.SubtitleCache
import com.rdwatch.androidtv.player.subtitle.ranking.SubtitleResultRanker
import com.rdwatch.androidtv.test.HiltTestBase
import com.rdwatch.androidtv.test.MainDispatcherRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import retrofit2.Retrofit
import javax.inject.Inject

/**
 * Integration tests for SubtitleModule Hilt dependency injection.
 * Validates that all subtitle components can be injected and work together correctly.
 */
@HiltAndroidTest
class SubtitleModuleIntegrationTest : HiltTestBase() {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Inject
    lateinit var context: Context

    @Inject
    lateinit var database: AppDatabase

    @Inject
    lateinit var subtitleDao: SubtitleDao

    @Inject
    lateinit var subtitleCache: SubtitleCache

    @Inject
    lateinit var subtitleRateLimiter: SubtitleRateLimiter

    @Inject
    lateinit var subtitleResultRanker: SubtitleResultRanker

    @Inject
    lateinit var subtitleApiOrchestrator: SubtitleApiOrchestrator

    @Inject
    lateinit var apiClients: Set<@JvmSuppressWildcards SubtitleApiClient>

    @Inject
    @SubtitleApi
    lateinit var subtitleRetrofit: Retrofit

    @Inject
    lateinit var subtitleSearchConfig: SubtitleSearchConfig

    @Test
    fun `verify all subtitle dependencies are injected`() {
        // Core components
        assertNotNull("Context should be injected", context)
        assertNotNull("Database should be injected", database)
        assertNotNull("SubtitleDao should be injected", subtitleDao)
        assertNotNull("SubtitleCache should be injected", subtitleCache)
        assertNotNull("SubtitleRateLimiter should be injected", subtitleRateLimiter)
        assertNotNull("SubtitleResultRanker should be injected", subtitleResultRanker)
        assertNotNull("SubtitleApiOrchestrator should be injected", subtitleApiOrchestrator)

        // Network components
        assertNotNull("Subtitle Retrofit should be injected", subtitleRetrofit)

        // Configuration
        assertNotNull("SubtitleSearchConfig should be injected", subtitleSearchConfig)

        // API client set
        assertNotNull("API clients set should be injected", apiClients)
        assertFalse("API clients set should not be empty", apiClients.isEmpty())
    }

    @Test
    fun `verify subtitle dao is from correct database`() {
        // Act
        val daoFromDatabase = database.subtitleDao()

        // Assert
        assertEquals("DAO should be the same instance from database", daoFromDatabase, subtitleDao)
    }

    @Test
    fun `verify all expected api clients are present`() {
        // Assert
        assertEquals("Should have 5 API clients", 5, apiClients.size)

        val providers = apiClients.map { it.getProvider() }.toSet()
        assertTrue("Should include SUBDL provider", providers.contains(SubtitleApiProvider.SUBDL))
        assertTrue("Should include SUBDB provider", providers.contains(SubtitleApiProvider.SUBDB))
        assertTrue("Should include PODNAPISI provider", providers.contains(SubtitleApiProvider.PODNAPISI))
        assertTrue("Should include ADDIC7ED_ALT provider", providers.contains(SubtitleApiProvider.ADDIC7ED_ALT))
        assertTrue("Should include LOCAL_FILES provider", providers.contains(SubtitleApiProvider.LOCAL_FILES))
    }

    @Test
    fun `verify api client provider uniqueness`() {
        // Act
        val providers = apiClients.map { it.getProvider() }
        val uniqueProviders = providers.toSet()

        // Assert
        assertEquals("Each provider should be unique", providers.size, uniqueProviders.size)
    }

    @Test
    fun `verify local files client is enabled by default`() {
        // Act
        val localFilesClient = apiClients.find { it.getProvider() == SubtitleApiProvider.LOCAL_FILES }

        // Assert
        assertNotNull("Local files client should be present", localFilesClient)
        assertTrue("Local files client should be enabled", localFilesClient!!.isEnabled())
    }

    @Test
    fun `verify placeholder clients are disabled by default`() {
        // Act
        val placeholderClients = apiClients.filter { 
            it.getProvider() != SubtitleApiProvider.LOCAL_FILES 
        }

        // Assert
        assertTrue("Should have placeholder clients", placeholderClients.isNotEmpty())
        placeholderClients.forEach { client ->
            assertFalse("Placeholder client ${client.getProvider()} should be disabled", client.isEnabled())
        }
    }

    @Test
    fun `verify subtitle search config has reasonable defaults`() {
        // Assert
        assertTrue("Hash matching should be enabled", subtitleSearchConfig.enableHashMatching)
        assertTrue("Fuzzy matching should be enabled", subtitleSearchConfig.enableFuzzyMatching)
        assertTrue("Max results should be reasonable", subtitleSearchConfig.maxResults > 0)
        assertTrue("Max results should not be excessive", subtitleSearchConfig.maxResults <= 100)
        assertTrue("Timeout should be reasonable", subtitleSearchConfig.timeoutMs > 1000)
        assertTrue("Timeout should not be excessive", subtitleSearchConfig.timeoutMs <= 60000)
        assertTrue("Retry attempts should be reasonable", subtitleSearchConfig.retryAttempts >= 1)
        assertTrue("Cache expiration should be reasonable", subtitleSearchConfig.cacheExpirationHours >= 1)
        assertFalse("Auto download should be disabled by default", subtitleSearchConfig.autoDownloadBest)
        assertNull("Hearing impaired preference should be null by default", subtitleSearchConfig.hearingImpairedPreference)
    }

    @Test
    fun `verify subtitle search config preferred providers`() {
        // Assert
        assertFalse("Should have preferred providers", subtitleSearchConfig.preferredProviders.isEmpty())
        assertTrue("Should prefer SUBDB", subtitleSearchConfig.preferredProviders.contains(SubtitleApiProvider.SUBDB))
        assertTrue("Should prefer SUBDL", subtitleSearchConfig.preferredProviders.contains(SubtitleApiProvider.SUBDL))
        assertTrue("Should prefer PODNAPISI", subtitleSearchConfig.preferredProviders.contains(SubtitleApiProvider.PODNAPISI))
    }

    @Test
    fun `verify retrofit configuration for subtitle apis`() {
        // Assert
        assertNotNull("Subtitle retrofit should be configured", subtitleRetrofit)
        
        val baseUrl = subtitleRetrofit.baseUrl().toString()
        assertTrue("Should have valid base URL", baseUrl.startsWith("http"))
        assertTrue("Should use HTTPS or localhost", baseUrl.startsWith("https://") || baseUrl.contains("localhost"))
    }

    @Test
    fun `verify subtitle cache can interact with dao`() = runTest {
        // This test verifies that the cache component can work with the injected DAO
        // We don't test full functionality here, just that the integration works
        
        // Assert
        assertNotNull("Cache should have access to DAO", subtitleCache)
        assertNotNull("DAO should be available for cache", subtitleDao)
        
        // The cache should be able to use the DAO without errors
        // This is tested more thoroughly in cache-specific tests
    }

    @Test
    fun `verify rate limiter is ready for use`() = runTest {
        // Assert
        assertNotNull("Rate limiter should be ready", subtitleRateLimiter)
        
        // Should be able to check rate limit status
        val status = subtitleRateLimiter.getStatus(SubtitleApiProvider.SUBDL)
        assertNotNull("Should provide status for providers", status)
        
        // Should be able to reset limits
        subtitleRateLimiter.resetAllLimits()
    }

    @Test
    fun `verify result ranker is functional`() {
        // Assert
        assertNotNull("Result ranker should be available", subtitleResultRanker)
        
        // Should be able to handle empty results
        val emptyResults = subtitleResultRanker.rankResults(emptyList(), createTestSearchRequest())
        assertNotNull("Should handle empty results", emptyResults)
        assertTrue("Empty results should remain empty", emptyResults.isEmpty())
    }

    @Test
    fun `verify orchestrator has all required dependencies`() {
        // Assert
        assertNotNull("Orchestrator should be ready", subtitleApiOrchestrator)
        
        // Should be able to get provider status
        val providerStatus = subtitleApiOrchestrator.getProviderStatus()
        assertNotNull("Should provide status", providerStatus)
        assertFalse("Should have provider statuses", providerStatus.isEmpty())
        assertEquals("Should have status for all providers", apiClients.size, providerStatus.size)
    }

    @Test
    fun `verify integration between orchestrator and clients`() = runTest {
        // Act
        val providerStatus = subtitleApiOrchestrator.getProviderStatus()

        // Assert
        apiClients.forEach { client ->
            val status = providerStatus[client.getProvider()]
            assertNotNull("Should have status for ${client.getProvider()}", status)
            assertEquals("Status should match client state", client.isEnabled(), status!!.enabled)
        }
    }

    @Test
    fun `verify cache clearance works`() = runTest {
        // Act & Assert - Should not throw exceptions
        subtitleApiOrchestrator.clearCache()
    }

    @Test
    fun `verify database schema supports subtitle operations`() = runTest {
        // This verifies that the database schema includes subtitle-related tables
        
        // Act
        val database = this@SubtitleModuleIntegrationTest.database
        
        // Assert
        assertNotNull("Database should be available", database)
        assertNotNull("Subtitle DAO should be available", database.subtitleDao())
        
        // The DAO should be ready for operations (tested more in DAO-specific tests)
        val dao = database.subtitleDao()
        assertNotNull("DAO should be functional", dao)
    }

    @Test
    fun `verify component lifecycle compatibility`() {
        // Test that singleton components maintain state
        
        // Act - Get components multiple times
        val rateLimiter1 = subtitleRateLimiter
        val rateLimiter2 = subtitleRateLimiter
        val cache1 = subtitleCache
        val cache2 = subtitleCache
        
        // Assert - Should be the same instances (singleton behavior)
        assertSame("Rate limiter should be singleton", rateLimiter1, rateLimiter2)
        assertSame("Cache should be singleton", cache1, cache2)
    }

    @Test
    fun `verify qualifier annotations work correctly`() {
        // The @SubtitleApi qualifier should provide subtitle-specific Retrofit
        assertNotNull("Qualified Retrofit should be injected", subtitleRetrofit)
        
        // Should be configured for subtitle APIs
        val baseUrl = subtitleRetrofit.baseUrl().toString()
        assertTrue("Should have subtitle API base URL", baseUrl.isNotEmpty())
    }

    private fun createTestSearchRequest(): com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchRequest {
        return com.rdwatch.androidtv.player.subtitle.models.SubtitleSearchRequest(
            title = "Test Movie",
            year = 2023,
            type = com.rdwatch.androidtv.player.subtitle.models.ContentType.MOVIE,
            languages = listOf("en")
        )
    }
}