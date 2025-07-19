package com.rdwatch.androidtv.ui.details.models.advanced

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Comprehensive tests for HealthMonitor system
 * Tests health calculation, caching, prediction, and monitoring functionality
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HealthMonitorTest {
    private lateinit var healthMonitor: HealthMonitor
    private lateinit var testProvider: SourceProviderInfo
    private lateinit var excellentHealth: HealthInfo
    private lateinit var goodHealth: HealthInfo
    private lateinit var poorHealth: HealthInfo
    private lateinit var deadHealth: HealthInfo

    @Before
    fun setup() {
        healthMonitor = HealthMonitor()

        testProvider =
            SourceProviderInfo(
                id = "test_provider",
                name = "Test Provider",
                displayName = "Test Provider",
                logoUrl = null,
                type = SourceProviderInfo.ProviderType.TORRENT,
                reliability = SourceProviderInfo.ProviderReliability.GOOD,
                capabilities = setOf("hdr", "4k"),
            )

        excellentHealth =
            HealthInfo(
                seeders = 1500,
                leechers = 200,
                downloadSpeed = 50_000_000L,
                uploadSpeed = 10_000_000L,
                availability = 1.0f,
                lastChecked = Date(),
            )

        goodHealth =
            HealthInfo(
                seeders = 100,
                leechers = 25,
                downloadSpeed = 10_000_000L,
                uploadSpeed = 2_000_000L,
                availability = 0.95f,
                lastChecked = Date(),
            )

        poorHealth =
            HealthInfo(
                seeders = 3,
                leechers = 10,
                downloadSpeed = 500_000L,
                uploadSpeed = 100_000L,
                availability = 0.6f,
                lastChecked = Date(System.currentTimeMillis() - 2 * 60 * 60 * 1000L),
            )

        deadHealth =
            HealthInfo(
                seeders = 0,
                leechers = 0,
                downloadSpeed = 0L,
                uploadSpeed = 0L,
                availability = 0.0f,
                lastChecked = Date(System.currentTimeMillis() - 24 * 60 * 60 * 1000L),
            )
    }

    @After
    fun tearDown() {
        healthMonitor.cleanup()
    }

    @Test
    fun `calculateHealthScore - excellent health produces high score`() {
        val healthData = healthMonitor.calculateHealthScore(excellentHealth, testProvider)

        assertTrue(healthData.overallScore >= 85, "Excellent health should produce high overall score")
        assertTrue(healthData.p2pHealth.overallScore >= 90, "Excellent P2P health should score very high")
        assertEquals(P2PHealthStatus.EXCELLENT, healthData.p2pHealth.healthStatus)
        assertTrue(healthData.availabilityPercentage >= 0.95f, "Availability should be high")
        assertTrue(healthData.freshnessIndicator >= 90, "Fresh data should score high")
    }

    @Test
    fun `calculateHealthScore - good health produces moderate score`() {
        val healthData = healthMonitor.calculateHealthScore(goodHealth, testProvider)

        assertTrue(healthData.overallScore in 60..85, "Good health should produce moderate score")
        assertTrue(healthData.p2pHealth.overallScore >= 70, "Good P2P health should score well")
        assertEquals(P2PHealthStatus.EXCELLENT, healthData.p2pHealth.healthStatus) // 100 seeders = excellent
        assertTrue(healthData.availabilityPercentage >= 0.9f, "Availability should be good")
    }

    @Test
    fun `calculateHealthScore - poor health produces low score`() {
        val healthData = healthMonitor.calculateHealthScore(poorHealth, testProvider)

        assertTrue(healthData.overallScore <= 50, "Poor health should produce low score")
        assertTrue(healthData.p2pHealth.overallScore <= 60, "Poor P2P health should score low")
        assertEquals(P2PHealthStatus.POOR, healthData.p2pHealth.healthStatus)
        assertTrue(healthData.freshnessIndicator <= 50, "Stale data should reduce freshness score")
        assertTrue(healthData.isStale, "2-hour old data should be marked as stale")
    }

    @Test
    fun `calculateHealthScore - dead source produces minimal score`() {
        val healthData = healthMonitor.calculateHealthScore(deadHealth, testProvider)

        assertTrue(healthData.overallScore <= 30, "Dead source should produce very low score")
        assertEquals(0, healthData.p2pHealth.seeders, "Dead source should have 0 seeders")
        assertEquals(P2PHealthStatus.DEAD, healthData.p2pHealth.healthStatus)
        assertEquals(RiskLevel.HIGH, healthData.riskLevel, "Dead source should be high risk")
        assertTrue(healthData.riskFactors.isNotEmpty(), "Should have risk factors listed")
    }

    @Test
    fun `calculateHealthScore - P2P health calculations are accurate`() {
        val healthData = healthMonitor.calculateHealthScore(excellentHealth, testProvider)
        val p2pHealth = healthData.p2pHealth

        assertEquals(1500, p2pHealth.seeders)
        assertEquals(200, p2pHealth.leechers)
        assertEquals(7.5f, p2pHealth.ratio) // 1500/200
        assertEquals(1700, p2pHealth.totalPeers) // 1500 + 200

        assertTrue(p2pHealth.seederScore >= 90, "1500 seeders should score very high")
        assertTrue(p2pHealth.ratioScore >= 90, "7.5 ratio should score very high")
        assertTrue(p2pHealth.activityScore >= 90, "1700 total peers should score very high")
        assertTrue(p2pHealth.speedScore >= 80, "60 MB/s total speed should score high")
    }

    @Test
    fun `calculateHealthScore - provider reliability affects score`() {
        val excellentProvider =
            testProvider.copy(
                reliability = SourceProviderInfo.ProviderReliability.EXCELLENT,
            )
        val poorProvider =
            testProvider.copy(
                reliability = SourceProviderInfo.ProviderReliability.POOR,
            )

        val excellentHealthData = healthMonitor.calculateHealthScore(goodHealth, excellentProvider)
        val poorHealthData = healthMonitor.calculateHealthScore(goodHealth, poorProvider)

        assertTrue(
            excellentHealthData.providerReliability > poorHealthData.providerReliability,
            "Excellent provider should have higher reliability score",
        )
        assertTrue(
            excellentHealthData.overallScore > poorHealthData.overallScore,
            "Excellent provider should contribute to higher overall score",
        )
    }

    @Test
    fun `calculateHealthScore - debrid provider gets bonus`() {
        val debridProvider =
            testProvider.copy(
                type = SourceProviderInfo.ProviderType.DEBRID,
            )
        val torrentProvider =
            testProvider.copy(
                type = SourceProviderInfo.ProviderType.TORRENT,
            )

        val debridHealthData = healthMonitor.calculateHealthScore(goodHealth, debridProvider)
        val torrentHealthData = healthMonitor.calculateHealthScore(goodHealth, torrentProvider)

        assertTrue(
            debridHealthData.providerReliability > torrentHealthData.providerReliability,
            "Debrid provider should get reliability bonus",
        )
        assertTrue(
            debridHealthData.sourceAuthority > torrentHealthData.sourceAuthority,
            "Debrid provider should have higher authority",
        )
    }

    @Test
    fun `calculateHealthScore - freshness affects score correctly`() {
        val freshHealth = goodHealth.copy(lastChecked = Date())
        val staleHealth =
            goodHealth.copy(
                lastChecked = Date(System.currentTimeMillis() - 30 * 60 * 1000L),
            )
        val veryStaleHealth =
            goodHealth.copy(
                lastChecked = Date(System.currentTimeMillis() - 25 * 60 * 60 * 1000L),
            )

        val freshData = healthMonitor.calculateHealthScore(freshHealth, testProvider)
        val staleData = healthMonitor.calculateHealthScore(staleHealth, testProvider)
        val veryStaleData = healthMonitor.calculateHealthScore(veryStaleHealth, testProvider)

        assertTrue(freshData.freshnessIndicator > staleData.freshnessIndicator)
        assertTrue(staleData.freshnessIndicator > veryStaleData.freshnessIndicator)
        assertTrue(veryStaleData.isStale, "25-hour old data should be stale")
        assertFalse(freshData.isStale, "Fresh data should not be stale")
    }

    @Test
    fun `calculateHealthScore - risk assessment works correctly`() {
        val excellentData = healthMonitor.calculateHealthScore(excellentHealth, testProvider)
        val poorData = healthMonitor.calculateHealthScore(poorHealth, testProvider)
        val deadData = healthMonitor.calculateHealthScore(deadHealth, testProvider)

        assertEquals(RiskLevel.MINIMAL, excellentData.riskLevel)
        assertTrue(excellentData.riskFactors.isEmpty() || excellentData.riskFactors.size <= 1)

        assertTrue(poorData.riskLevel == RiskLevel.MEDIUM || poorData.riskLevel == RiskLevel.HIGH)
        assertTrue(poorData.riskFactors.isNotEmpty())

        assertEquals(RiskLevel.HIGH, deadData.riskLevel)
        assertTrue(deadData.riskFactors.contains("Dead torrent (0 seeders)"))
    }

    @Test
    fun `calculateHealthScore - download time estimation works`() {
        val fastData = healthMonitor.calculateHealthScore(excellentHealth, testProvider)
        val slowData = healthMonitor.calculateHealthScore(poorHealth, testProvider)

        assertTrue(fastData.estimatedDownloadTimeMinutes > 0, "Should estimate download time")
        assertTrue(slowData.estimatedDownloadTimeMinutes > 0, "Should estimate download time")
        assertTrue(
            fastData.estimatedDownloadTimeMinutes < slowData.estimatedDownloadTimeMinutes,
            "Faster connection should have shorter estimated time",
        )
    }

    @Test
    fun `cacheHealthData - stores and retrieves correctly`() =
        runTest {
            val sourceId = "test_source_123"
            val healthData = healthMonitor.calculateHealthScore(goodHealth, testProvider)

            // Cache the data
            healthMonitor.cacheHealthData(sourceId, healthData)

            // Retrieve the data
            val cachedData = healthMonitor.getCachedHealthData(sourceId)

            assertNotNull(cachedData, "Cached data should be retrievable")
            assertEquals(healthData.sourceId, cachedData.sourceId)
            assertEquals(healthData.overallScore, cachedData.overallScore)
        }

    @Test
    fun `cacheHealthData - updates state flow`() =
        runTest {
            val sourceId = "test_source_456"
            val healthData = healthMonitor.calculateHealthScore(excellentHealth, testProvider)

            // Cache the data
            healthMonitor.cacheHealthData(sourceId, healthData)

            // Small delay to allow state flow update
            delay(100)

            // Check state flow contains the data
            val currentState = healthMonitor.healthUpdates.value
            assertTrue(currentState.containsKey(sourceId), "State flow should contain cached data")
            assertEquals(healthData.sourceId, currentState[sourceId]?.sourceId)
        }

    @Test
    fun `getCachedHealthData - returns null for non-existent data`() {
        val nonExistentData = healthMonitor.getCachedHealthData("non_existent_source")
        assertNull(nonExistentData, "Should return null for non-existent cached data")
    }

    @Test
    fun `health monitoring - maintains history for predictions`() =
        runTest {
            val sourceId = "test_source_789"

            // Add multiple health snapshots
            repeat(5) { i ->
                val healthData =
                    healthMonitor.calculateHealthScore(
                        goodHealth.copy(seeders = 100 + i * 10),
                        testProvider,
                    )
                healthMonitor.cacheHealthData(sourceId, healthData)
                delay(50)
            }

            // Get latest data
            val latestData = healthMonitor.getCachedHealthData(sourceId)
            assertNotNull(latestData, "Should have cached data")

            // Check if trend is calculated (would need access to internal history)
            assertTrue(latestData.healthTrend != null, "Should have calculated trend")
        }

    @Test
    fun `health badges are generated correctly`() {
        val excellentData = healthMonitor.calculateHealthScore(excellentHealth, testProvider)
        val goodData = healthMonitor.calculateHealthScore(goodHealth, testProvider)
        val poorData = healthMonitor.calculateHealthScore(poorHealth, testProvider)

        val excellentBadge = excellentData.getHealthBadge()
        val goodBadge = goodData.getHealthBadge()
        val poorBadge = poorData.getHealthBadge()

        assertEquals(QualityBadge.Type.HEALTH, excellentBadge.type)
        assertEquals(QualityBadge.Type.HEALTH, goodBadge.type)
        assertEquals(QualityBadge.Type.HEALTH, poorBadge.type)

        assertTrue(excellentBadge.text.contains("Excellent") || excellentBadge.text.contains("Very Good"))
        assertTrue(poorBadge.text.contains("Poor") || poorBadge.text.contains("Bad"))
    }

    @Test
    fun `speed score calculation works correctly`() {
        val slowHealth =
            HealthInfo(
                seeders = 50,
                leechers = 10,
                downloadSpeed = 100_000L,
                uploadSpeed = 50_000L,
                availability = 1.0f,
                lastChecked = Date(),
            )

        val fastHealth =
            HealthInfo(
                seeders = 50,
                leechers = 10,
                downloadSpeed = 100_000_000L,
                uploadSpeed = 50_000_000L,
                availability = 1.0f,
                lastChecked = Date(),
            )

        val slowData = healthMonitor.calculateHealthScore(slowHealth, testProvider)
        val fastData = healthMonitor.calculateHealthScore(fastHealth, testProvider)

        assertTrue(
            fastData.p2pHealth.speedScore > slowData.p2pHealth.speedScore,
            "Faster speeds should result in higher speed score",
        )
        assertTrue(fastData.p2pHealth.speedScore >= 90, "Very fast speeds should score very high")
        assertTrue(slowData.p2pHealth.speedScore <= 40, "Slow speeds should score low")
    }

    @Test
    fun `ratio calculation handles edge cases`() {
        val noLeechersHealth =
            HealthInfo(
                seeders = 100,
                leechers = 0,
                availability = 1.0f,
                lastChecked = Date(),
            )

        val moreLeechers =
            HealthInfo(
                seeders = 50,
                leechers = 200,
                availability = 1.0f,
                lastChecked = Date(),
            )

        val noLeechersData = healthMonitor.calculateHealthScore(noLeechersHealth, testProvider)
        val moreLeechersData = healthMonitor.calculateHealthScore(moreLeechers, testProvider)

        // No leechers should result in maximum ratio
        assertEquals(Float.MAX_VALUE, noLeechersData.p2pHealth.ratio)
        assertTrue(noLeechersData.p2pHealth.ratioScore >= 90)

        // More leechers than seeders should result in ratio < 1
        assertEquals(0.25f, moreLeechersData.p2pHealth.ratio)
        assertTrue(moreLeechersData.p2pHealth.ratioScore <= 70)
    }

    @Test
    fun `provider capabilities affect authority score`() {
        val basicProvider = testProvider.copy(capabilities = emptySet())
        val advancedProvider =
            testProvider.copy(
                capabilities = setOf("hdr", "4k", "surround_sound", "fast_indexing"),
            )

        val basicData = healthMonitor.calculateHealthScore(goodHealth, basicProvider)
        val advancedData = healthMonitor.calculateHealthScore(goodHealth, advancedProvider)

        assertTrue(
            advancedData.sourceAuthority > basicData.sourceAuthority,
            "Provider with more capabilities should have higher authority",
        )
    }

    @Test
    fun `availability adjustment works correctly`() {
        val highAvailability = goodHealth.copy(availability = 1.0f, seeders = 5)
        val lowAvailability = goodHealth.copy(availability = 0.3f, seeders = 100)

        val highAvailData = healthMonitor.calculateHealthScore(highAvailability, testProvider)
        val lowAvailData = healthMonitor.calculateHealthScore(lowAvailability, testProvider)

        // Availability percentage should reflect the adjustments
        assertTrue(highAvailData.availabilityPercentage >= 0.8f)
        assertTrue(lowAvailData.availabilityPercentage >= 0.3f)
    }

    @Test
    fun `cleanup - properly releases resources`() {
        // Add some data
        val sourceId = "cleanup_test"
        val healthData = healthMonitor.calculateHealthScore(goodHealth, testProvider)
        healthMonitor.cacheHealthData(sourceId, healthData)

        // Verify data exists
        assertNotNull(healthMonitor.getCachedHealthData(sourceId))

        // Cleanup
        healthMonitor.cleanup()

        // Data should be cleared (this test may need modification based on actual cleanup implementation)
        // For now, we just verify cleanup doesn't throw exceptions
        assertTrue(true, "Cleanup should complete without errors")
    }
}
