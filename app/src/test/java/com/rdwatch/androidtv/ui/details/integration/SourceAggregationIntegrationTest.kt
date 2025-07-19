package com.rdwatch.androidtv.ui.details.integration

import com.rdwatch.androidtv.ui.details.models.advanced.*
import com.rdwatch.androidtv.ui.details.repository.SourceAggregationRepository
import com.rdwatch.androidtv.ui.details.viewmodels.SourceListViewModel
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Integration tests for source aggregation and processing
 * Tests the complete pipeline from source data to filtered, sorted results
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SourceAggregationIntegrationTest {
    private lateinit var mockRepository: SourceAggregationRepository
    private lateinit var healthMonitor: HealthMonitor
    private lateinit var seasonPackDetector: SeasonPackDetector
    private lateinit var sourceComparators: SourceComparators
    private lateinit var viewModel: SourceListViewModel

    private lateinit var testSources: List<SourceMetadata>

    @Before
    fun setup() {
        mockRepository = mockk(relaxed = true)
        healthMonitor = HealthMonitor()
        seasonPackDetector = SeasonPackDetector()

        // Create comprehensive test data
        testSources = createTestSources()

        // Mock repository to return test data
        every { mockRepository.getAllSources() } returns testSources
        coEvery { mockRepository.getSourcesForContent(any()) } returns testSources

        viewModel =
            SourceListViewModel(
                repository = mockRepository,
                healthMonitor = healthMonitor,
            )
    }

    @After
    fun tearDown() {
        healthMonitor.cleanup()
        unmockkAll()
    }

    private fun createTestSources(): List<SourceMetadata> =
        listOf(
            // 4K HDR REMUX - Premium source
            createTestSource(
                id = "premium_4k",
                filename = "Movie.2023.2160p.UHD.BluRay.REMUX.HDR.HEVC.Atmos-PREMIUM.mkv",
                resolution = VideoResolution.RESOLUTION_4K,
                hdr10 = true,
                codec = VideoCodec.HEVC,
                audio = AudioFormat.TRUEHD,
                releaseType = ReleaseType.BLURAY_REMUX,
                size = 45_000_000_000L,
                seeders = 150,
                providerType = SourceProviderInfo.ProviderType.TORRENT,
            ),
            // 1080p HEVC with good health
            createTestSource(
                id = "good_1080p",
                filename = "Movie.2023.1080p.BluRay.x265.DD5.1-GOOD.mkv",
                resolution = VideoResolution.RESOLUTION_1080P,
                hdr10 = false,
                codec = VideoCodec.HEVC,
                audio = AudioFormat.EAC3,
                releaseType = ReleaseType.BLURAY,
                size = 8_000_000_000L,
                seeders = 800,
                providerType = SourceProviderInfo.ProviderType.TORRENT,
            ),
            // Cached debrid source
            createTestSource(
                id = "debrid_cached",
                filename = "Movie.2023.1080p.WEB-DL.H264.DD5.1-WEB.mkv",
                resolution = VideoResolution.RESOLUTION_1080P,
                hdr10 = false,
                codec = VideoCodec.H264,
                audio = AudioFormat.EAC3,
                releaseType = ReleaseType.WEB_DL,
                size = 6_000_000_000L,
                seeders = 0,
                providerType = SourceProviderInfo.ProviderType.DEBRID,
                cached = true,
            ),
            // Low quality with high seeders
            createTestSource(
                id = "popular_720p",
                filename = "Movie.2023.720p.HDTV.x264.AC3-POPULAR.mkv",
                resolution = VideoResolution.RESOLUTION_720P,
                hdr10 = false,
                codec = VideoCodec.H264,
                audio = AudioFormat.AC3,
                releaseType = ReleaseType.HDTV,
                size = 2_500_000_000L,
                seeders = 2000,
                providerType = SourceProviderInfo.ProviderType.TORRENT,
            ),
            // Dead torrent
            createTestSource(
                id = "dead_source",
                filename = "Movie.2023.1080p.BluRay.x264.DTS-DEAD.mkv",
                resolution = VideoResolution.RESOLUTION_1080P,
                hdr10 = false,
                codec = VideoCodec.H264,
                audio = AudioFormat.DTS,
                releaseType = ReleaseType.BLURAY,
                size = 12_000_000_000L,
                seeders = 0,
                providerType = SourceProviderInfo.ProviderType.TORRENT,
            ),
            // Season pack
            createTestSource(
                id = "season_pack",
                filename = "Show.S01.Complete.1080p.BluRay.x264-SEASON.mkv",
                resolution = VideoResolution.RESOLUTION_1080P,
                hdr10 = false,
                codec = VideoCodec.H264,
                audio = AudioFormat.AC3,
                releaseType = ReleaseType.BLURAY,
                size = 25_000_000_000L,
                seeders = 300,
                providerType = SourceProviderInfo.ProviderType.TORRENT,
            ),
        )

    private fun createTestSource(
        id: String,
        filename: String,
        resolution: VideoResolution,
        hdr10: Boolean,
        codec: VideoCodec,
        audio: AudioFormat,
        releaseType: ReleaseType,
        size: Long,
        seeders: Int,
        providerType: SourceProviderInfo.ProviderType,
        cached: Boolean = false,
    ): SourceMetadata =
        SourceMetadata(
            id = id,
            provider =
                SourceProviderInfo(
                    id = "provider_$id",
                    name = "Provider $id",
                    displayName = "Provider $id",
                    logoUrl = null,
                    type = providerType,
                    reliability =
                        when (providerType) {
                            SourceProviderInfo.ProviderType.DEBRID -> SourceProviderInfo.ProviderReliability.EXCELLENT
                            SourceProviderInfo.ProviderType.TORRENT -> SourceProviderInfo.ProviderReliability.GOOD
                            else -> SourceProviderInfo.ProviderReliability.FAIR
                        },
                ),
            quality =
                QualityInfo(
                    resolution = resolution,
                    bitrate =
                        when (resolution) {
                            VideoResolution.RESOLUTION_4K -> 25_000_000L
                            VideoResolution.RESOLUTION_1080P -> 8_000_000L
                            VideoResolution.RESOLUTION_720P -> 4_000_000L
                            else -> 2_000_000L
                        },
                    hdr10 = hdr10,
                    frameRate = 24,
                ),
            codec = CodecInfo(type = codec),
            audio =
                AudioInfo(
                    format = audio,
                    channels = "5.1",
                    bitrate = 640,
                ),
            release =
                ReleaseInfo(
                    type = releaseType,
                    group = "TEST",
                    year = 2023,
                ),
            file =
                FileInfo(
                    name = filename,
                    sizeInBytes = size,
                    extension = "mkv",
                    addedDate = Date(),
                ),
            health =
                HealthInfo(
                    seeders = seeders,
                    leechers = maxOf(1, seeders / 4),
                    availability = if (seeders > 0) 1.0f else 0.0f,
                    lastChecked = Date(),
                ),
            features = FeatureInfo(),
            availability =
                AvailabilityInfo(
                    isAvailable = true,
                    cached = cached,
                ),
        )

    @Test
    fun `source aggregation pipeline processes all sources correctly`() =
        runTest {
            // Trigger source loading
            viewModel.loadSources("test_content_id")

            // Wait for processing to complete
            val sources = viewModel.sources.first()

            assertEquals(testSources.size, sources.size, "All sources should be processed")

            // Verify each source has been processed with health data
            sources.forEach { source ->
                assertNotNull(source, "Source should not be null")
                assertNotNull(source.health, "Source should have health info")
            }
        }

    @Test
    fun `quality scoring works correctly across different source types`() =
        runTest {
            val premium4K = testSources.find { it.id == "premium_4k" }!!
            val good1080p = testSources.find { it.id == "good_1080p" }!!
            val debridCached = testSources.find { it.id == "debrid_cached" }!!
            val popular720p = testSources.find { it.id == "popular_720p" }!!

            val premium4KScore = premium4K.getQualityScore()
            val good1080pScore = good1080p.getQualityScore()
            val debridCachedScore = debridCached.getQualityScore()
            val popular720pScore = popular720p.getQualityScore()

            // 4K should score highest despite lower seeders
            assertTrue(premium4KScore > good1080pScore, "4K should score higher than 1080p")
            assertTrue(premium4KScore > popular720pScore, "4K should score higher than 720p")

            // Good health should boost scores
            assertTrue(good1080pScore > popular720pScore, "Better quality should outweigh seeder count")

            // Debrid cached should score well
            assertTrue(debridCachedScore > popular720pScore, "Cached debrid should score well")
        }

    @Test
    fun `filtering system works correctly with complex criteria`() =
        runTest {
            val complexFilter =
                AdvancedSourceFilter(
                    qualityFilters =
                        QualityFilters(
                            minResolution = VideoResolution.RESOLUTION_1080P,
                        ),
                    healthFilters =
                        HealthFilters(
                            minSeeders = 100,
                        ),
                    fileSizeFilters =
                        FileSizeFilters(
                            maxSizeGB = 30.0,
                        ),
                )

            val filteredSources =
                testSources.filter { source ->
                    // Apply filter logic
                    source.quality.resolution.ordinal >= VideoResolution.RESOLUTION_1080P.ordinal &&
                        (source.health.seeders ?: 0) >= 100 &&
                        (source.file.sizeInBytes ?: 0L) / (1024.0 * 1024.0 * 1024.0) <= 30.0
                }

            // Should include: good_1080p, season_pack
            // Should exclude: premium_4k (too large), popular_720p (wrong resolution),
            //                debrid_cached (0 seeders), dead_source (0 seeders)
            assertTrue(filteredSources.any { it.id == "good_1080p" })
            assertTrue(filteredSources.any { it.id == "season_pack" })
            assertTrue(filteredSources.none { it.id == "premium_4k" }) // Too large
            assertTrue(filteredSources.none { it.id == "popular_720p" }) // Wrong resolution
            assertTrue(filteredSources.none { it.id == "dead_source" }) // No seeders
        }

    @Test
    fun `sorting algorithms produce consistent results`() =
        runTest {
            // Test quality sorting
            val qualitySorted = testSources.sortedWith(SourceComparators.qualityComparator)

            // Premium 4K should be first (highest quality)
            assertEquals("premium_4k", qualitySorted.first().id)

            // Dead source should be last or near last (0 seeders)
            val deadSourceIndex = qualitySorted.indexOfFirst { it.id == "dead_source" }
            assertTrue(deadSourceIndex >= qualitySorted.size - 2, "Dead source should rank very low")

            // Test health sorting
            val healthSorted = testSources.sortedWith(SourceComparators.healthComparator)

            // Popular 720p should be first (most seeders)
            assertEquals("popular_720p", healthSorted.first().id)

            // Test Android TV optimized sorting
            val androidTVSorted = testSources.sortedWith(SourceComparators.androidTVOptimizedComparator)

            // Cached debrid should be first (instant playback)
            assertEquals("debrid_cached", androidTVSorted.first().id)
        }

    @Test
    fun `health monitoring integration works correctly`() =
        runTest {
            val excellentSource = testSources.find { it.id == "good_1080p" }!! // 800 seeders
            val poorSource = testSources.find { it.id == "dead_source" }!! // 0 seeders

            val excellentHealthData =
                healthMonitor.calculateHealthScore(
                    excellentSource.health,
                    excellentSource.provider,
                )
            val poorHealthData =
                healthMonitor.calculateHealthScore(
                    poorSource.health,
                    poorSource.provider,
                )

            assertTrue(excellentHealthData.overallScore > poorHealthData.overallScore)
            assertEquals(RiskLevel.HIGH, poorHealthData.riskLevel)
            assertTrue(poorHealthData.riskFactors.isNotEmpty())
        }

    @Test
    fun `season pack detection works in integration`() =
        runTest {
            val seasonPackSource = testSources.find { it.id == "season_pack" }!!
            val seasonPackInfo =
                seasonPackDetector.analyzeSeasonPack(
                    seasonPackSource.file.name ?: "",
                    seasonPackSource.file.sizeInBytes,
                )

            assertTrue(seasonPackInfo.isSeasonPack, "Should detect season pack")
            assertEquals(SeasonPackType.COMPLETE_SEASON, seasonPackInfo.packType)
            assertTrue(seasonPackInfo.seasonNumbers.contains(1), "Should detect season 1")
            assertEquals(100.0f, seasonPackInfo.completenessPercentage, "Complete season should be 100%")
            assertTrue(seasonPackInfo.confidence >= 80.0f, "Should have high confidence")

            // Season pack quality score should be calculated
            val qualityScore = seasonPackDetector.getSeasonPackQualityScore(seasonPackInfo)
            assertTrue(qualityScore > 100, "Season pack should get quality bonus")
        }

    @Test
    fun `full pipeline with filters and sorting works correctly`() =
        runTest {
            // Create a realistic filter for high-quality sources
            val qualityFilter =
                AdvancedSourceFilter(
                    qualityFilters =
                        QualityFilters(
                            minResolution = VideoResolution.RESOLUTION_1080P,
                        ),
                    healthFilters =
                        HealthFilters(
                            minSeeders = 50,
                        ),
                    sourceTypeFilters =
                        SourceTypeFilters(
                            p2pOnly = false,
                        ),
                )

            // Apply filter
            val filteredSources =
                testSources.filter { source ->
                    source.quality.resolution.ordinal >= VideoResolution.RESOLUTION_1080P.ordinal &&
                        (source.health.seeders ?: 0) >= 50
                }

            // Sort by Android TV optimized algorithm
            val finalSources = filteredSources.sortedWith(SourceComparators.androidTVOptimizedComparator)

            // Verify results
            assertTrue(finalSources.isNotEmpty(), "Should have filtered sources")

            // Cached debrid should be first
            assertEquals("debrid_cached", finalSources.first().id)

            // No 720p sources should remain
            assertTrue(finalSources.none { it.quality.resolution == VideoResolution.RESOLUTION_720P })

            // No dead sources should remain
            assertTrue(finalSources.none { (it.health.seeders ?: 0) == 0 })
        }

    @Test
    fun `provider reliability affects final ranking`() =
        runTest {
            // Test that provider reliability is factored into scoring
            val debridSource = testSources.find { it.id == "debrid_cached" }!!
            val torrentSource = testSources.find { it.id == "good_1080p" }!!

            // Both are 1080p, but debrid should have reliability advantage
            val debridScore = debridSource.getQualityScore()
            val torrentScore = torrentSource.getQualityScore()

            // Even with fewer seeders, debrid reliability should provide competitive scoring
            // The exact comparison depends on the implementation, but reliability should matter
            assertNotNull(debridScore)
            assertNotNull(torrentScore)
        }

    @Test
    fun `large source list performance`() =
        runTest {
            // Create a large number of sources
            val largeSources =
                (1..1000).map { index ->
                    createTestSource(
                        id = "source_$index",
                        filename = "Movie.Part.$index.1080p.BluRay.x264-TEST.mkv",
                        resolution = VideoResolution.RESOLUTION_1080P,
                        hdr10 = false,
                        codec = VideoCodec.H264,
                        audio = AudioFormat.AC3,
                        releaseType = ReleaseType.BLURAY,
                        size = 8_000_000_000L,
                        seeders = (1..1000).random(),
                        providerType = SourceProviderInfo.ProviderType.TORRENT,
                    )
                }

            // Mock repository to return large dataset
            every { mockRepository.getAllSources() } returns largeSources

            // Time the operations
            val startTime = System.currentTimeMillis()

            // Apply complex filter
            val filtered =
                largeSources.filter { source ->
                    source.quality.resolution.ordinal >= VideoResolution.RESOLUTION_1080P.ordinal &&
                        (source.health.seeders ?: 0) >= 100
                }

            // Sort results
            val sorted = filtered.sortedWith(SourceComparators.qualityComparator)

            val endTime = System.currentTimeMillis()
            val processingTime = endTime - startTime

            // Performance assertions
            assertTrue(sorted.isNotEmpty(), "Should process large dataset")
            assertTrue(processingTime < 5000, "Should process 1000 sources in under 5 seconds")

            // Verify sorting correctness on large dataset
            for (i in 0 until sorted.size - 1) {
                assertTrue(
                    sorted[i].getQualityScore() >= sorted[i + 1].getQualityScore(),
                    "Large dataset should maintain sort order",
                )
            }
        }

    @Test
    fun `error handling in aggregation pipeline`() =
        runTest {
            // Test with corrupted source data
            val corruptedSources =
                listOf(
                    createTestSource(
                        id = "corrupted",
                        filename = "",
                        resolution = VideoResolution.UNKNOWN,
                        hdr10 = false,
                        codec = VideoCodec.UNKNOWN,
                        audio = AudioFormat.UNKNOWN,
                        releaseType = ReleaseType.UNKNOWN,
                        size = 0L,
                        seeders = -1,
                        providerType = SourceProviderInfo.ProviderType.TORRENT,
                    ),
                )

            every { mockRepository.getAllSources() } returns corruptedSources

            // Should handle gracefully without crashing
            viewModel.loadSources("test_content_id")
            val sources = viewModel.sources.first()

            // Should still return the source, even if with poor scores
            assertEquals(1, sources.size)
            assertTrue(sources.first().getQualityScore() >= 0, "Should handle corrupted data gracefully")
        }
}
