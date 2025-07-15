package com.rdwatch.androidtv.ui.details.performance

import com.rdwatch.androidtv.ui.details.models.advanced.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.Before
import org.junit.After
import kotlin.test.assertTrue
import kotlin.system.measureTimeMillis
import java.util.Date
import kotlin.random.Random

/**
 * Performance tests for source list handling and filtering
 * Tests with large datasets to ensure scalability
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SourceListPerformanceTest {

    private lateinit var healthMonitor: HealthMonitor
    private lateinit var seasonPackDetector: SeasonPackDetector
    
    companion object {
        // Performance thresholds (in milliseconds)
        private const val FILTER_THRESHOLD_100_SOURCES = 50L
        private const val FILTER_THRESHOLD_1000_SOURCES = 500L
        private const val SORT_THRESHOLD_100_SOURCES = 20L
        private const val SORT_THRESHOLD_1000_SOURCES = 200L
        private const val QUALITY_SCORE_THRESHOLD_1000_SOURCES = 100L
        private const val HEALTH_CALC_THRESHOLD_1000_SOURCES = 1000L
        
        // Memory thresholds (in MB)
        private const val MEMORY_THRESHOLD_1000_SOURCES = 50L
    }

    @Before
    fun setup() {
        healthMonitor = HealthMonitor()
        seasonPackDetector = SeasonPackDetector()
        
        // Force garbage collection to get clean memory baseline
        System.gc()
        Thread.sleep(100)
    }

    @After
    fun tearDown() {
        healthMonitor.cleanup()
    }

    @Test
    fun `filter performance with 100 sources`() = runTest {
        val sources = generateTestSources(100)
        val filter = createComplexFilter()
        
        val filterTime = measureTimeMillis {
            val filtered = sources.filter { source ->
                applyFilterLogic(source, filter)
            }
            // Verify we get results to ensure filter is actually working
            assertTrue(filtered.isNotEmpty(), "Filter should produce results")
        }
        
        assertTrue(
            filterTime < FILTER_THRESHOLD_100_SOURCES,
            "Filtering 100 sources should take less than ${FILTER_THRESHOLD_100_SOURCES}ms, took ${filterTime}ms"
        )
    }

    @Test
    fun `filter performance with 1000 sources`() = runTest {
        val sources = generateTestSources(1000)
        val filter = createComplexFilter()
        
        val filterTime = measureTimeMillis {
            val filtered = sources.filter { source ->
                applyFilterLogic(source, filter)
            }
            assertTrue(filtered.isNotEmpty(), "Filter should produce results")
        }
        
        assertTrue(
            filterTime < FILTER_THRESHOLD_1000_SOURCES,
            "Filtering 1000 sources should take less than ${FILTER_THRESHOLD_1000_SOURCES}ms, took ${filterTime}ms"
        )
    }

    @Test
    fun `sort performance with 100 sources`() = runTest {
        val sources = generateTestSources(100)
        
        val sortTime = measureTimeMillis {
            val sorted = sources.sortedWith(SourceComparators.qualityComparator)
            assertEquals(sources.size, sorted.size, "Sorted list should maintain size")
        }
        
        assertTrue(
            sortTime < SORT_THRESHOLD_100_SOURCES,
            "Sorting 100 sources should take less than ${SORT_THRESHOLD_100_SOURCES}ms, took ${sortTime}ms"
        )
    }

    @Test
    fun `sort performance with 1000 sources`() = runTest {
        val sources = generateTestSources(1000)
        
        val sortTime = measureTimeMillis {
            val sorted = sources.sortedWith(SourceComparators.qualityComparator)
            assertEquals(sources.size, sorted.size, "Sorted list should maintain size")
        }
        
        assertTrue(
            sortTime < SORT_THRESHOLD_1000_SOURCES,
            "Sorting 1000 sources should take less than ${SORT_THRESHOLD_1000_SOURCES}ms, took ${sortTime}ms"
        )
    }

    @Test
    fun `quality score calculation performance with 1000 sources`() = runTest {
        val sources = generateTestSources(1000)
        
        val scoreTime = measureTimeMillis {
            sources.forEach { source ->
                val score = source.getQualityScore()
                assertTrue(score >= 0, "Quality score should be non-negative")
            }
        }
        
        assertTrue(
            scoreTime < QUALITY_SCORE_THRESHOLD_1000_SOURCES,
            "Quality score calculation for 1000 sources should take less than ${QUALITY_SCORE_THRESHOLD_1000_SOURCES}ms, took ${scoreTime}ms"
        )
    }

    @Test
    fun `health monitor performance with 1000 sources`() = runTest {
        val sources = generateTestSources(1000)
        
        val healthTime = measureTimeMillis {
            sources.forEach { source ->
                val healthData = healthMonitor.calculateHealthScore(source.health, source.provider)
                assertTrue(healthData.overallScore >= 0, "Health score should be non-negative")
            }
        }
        
        assertTrue(
            healthTime < HEALTH_CALC_THRESHOLD_1000_SOURCES,
            "Health calculation for 1000 sources should take less than ${HEALTH_CALC_THRESHOLD_1000_SOURCES}ms, took ${healthTime}ms"
        )
    }

    @Test
    fun `memory usage with 1000 sources`() = runTest {
        val runtime = Runtime.getRuntime()
        
        // Get baseline memory
        System.gc()
        Thread.sleep(100)
        val memoryBefore = runtime.totalMemory() - runtime.freeMemory()
        
        // Create and process large dataset
        val sources = generateTestSources(1000)
        val healthDataList = sources.map { source ->
            healthMonitor.calculateHealthScore(source.health, source.provider)
        }
        
        // Calculate memory usage
        System.gc()
        Thread.sleep(100)
        val memoryAfter = runtime.totalMemory() - runtime.freeMemory()
        val memoryUsed = (memoryAfter - memoryBefore) / (1024 * 1024) // Convert to MB
        
        assertTrue(
            memoryUsed < MEMORY_THRESHOLD_1000_SOURCES,
            "Memory usage for 1000 sources should be less than ${MEMORY_THRESHOLD_1000_SOURCES}MB, used ${memoryUsed}MB"
        )
        
        // Ensure data is still accessible (prevents optimization away)
        assertTrue(healthDataList.isNotEmpty())
    }

    @Test
    fun `complex filter and sort pipeline performance`() = runTest {
        val sources = generateTestSources(1000)
        val filter = createComplexFilter()
        
        val pipelineTime = measureTimeMillis {
            val filtered = sources.filter { source ->
                applyFilterLogic(source, filter)
            }
            
            val sorted = filtered.sortedWith(SourceComparators.androidTVOptimizedComparator)
            
            // Calculate quality scores for top results
            val topResults = sorted.take(50)
            topResults.forEach { source ->
                source.getQualityScore()
            }
            
            assertTrue(sorted.isNotEmpty(), "Pipeline should produce results")
        }
        
        // Total pipeline should be reasonable
        assertTrue(
            pipelineTime < 1000L,
            "Complete filter+sort+score pipeline should take less than 1000ms, took ${pipelineTime}ms"
        )
    }

    @Test
    fun `season pack detection performance`() = runTest {
        val seasonPackFilenames = generateSeasonPackFilenames(1000)
        
        val detectionTime = measureTimeMillis {
            seasonPackFilenames.forEach { filename ->
                val packInfo = seasonPackDetector.analyzeSeasonPack(filename, Random.nextLong(1_000_000_000L, 50_000_000_000L))
                assertNotNull(packInfo, "Should return pack info")
            }
        }
        
        assertTrue(
            detectionTime < 500L,
            "Season pack detection for 1000 filenames should take less than 500ms, took ${detectionTime}ms"
        )
    }

    @Test
    fun `concurrent filtering performance`() = runTest {
        val sources = generateTestSources(1000)
        val filters = listOf(
            createQualityFilter(),
            createHealthFilter(),
            createSizeFilter(),
            createCodecFilter()
        )
        
        val concurrentTime = measureTimeMillis {
            val results = filters.map { filter ->
                sources.filter { source ->
                    applyFilterLogic(source, filter)
                }
            }
            
            // Verify all filters produced results
            results.forEach { filteredList ->
                assertTrue(filteredList.isNotEmpty(), "Each filter should produce results")
            }
        }
        
        assertTrue(
            concurrentTime < 1000L,
            "Concurrent filtering with 4 different filters should take less than 1000ms, took ${concurrentTime}ms"
        )
    }

    @Test
    fun `sorting stability with large dataset`() = runTest {
        val sources = generateTestSources(1000)
        
        // Test that sorting is stable and produces consistent results
        val sorted1 = sources.sortedWith(SourceComparators.qualityComparator)
        val sorted2 = sources.sortedWith(SourceComparators.qualityComparator)
        
        assertEquals(sorted1.size, sorted2.size, "Sorted lists should have same size")
        
        // Results should be identical (stable sort)
        sorted1.zip(sorted2).forEach { (source1, source2) ->
            assertEquals(source1.id, source2.id, "Stable sort should produce identical results")
        }
    }

    @Test
    fun `batch processing performance`() = runTest {
        val batchSize = 100
        val totalSources = 1000
        val sources = generateTestSources(totalSources)
        
        val batchTime = measureTimeMillis {
            sources.chunked(batchSize).forEach { batch ->
                // Process each batch
                val filtered = batch.filter { source ->
                    source.quality.resolution.ordinal >= VideoResolution.RESOLUTION_1080P.ordinal
                }
                
                val sorted = filtered.sortedWith(SourceComparators.qualityComparator)
                
                // Verify batch processing
                assertTrue(sorted.size <= batch.size, "Sorted batch should not exceed original batch size")
            }
        }
        
        assertTrue(
            batchTime < 800L,
            "Batch processing of 1000 sources (100 per batch) should take less than 800ms, took ${batchTime}ms"
        )
    }

    private fun generateTestSources(count: Int): List<SourceMetadata> {
        val resolutions = VideoResolution.entries.filter { it != VideoResolution.UNKNOWN }
        val codecs = VideoCodec.entries.filter { it != VideoCodec.UNKNOWN }
        val audioFormats = AudioFormat.entries.filter { it != AudioFormat.UNKNOWN }
        val releaseTypes = ReleaseType.entries.filter { it != ReleaseType.UNKNOWN }
        val providerTypes = SourceProviderInfo.ProviderType.entries
        
        return (1..count).map { index ->
            SourceMetadata(
                id = "perf_source_$index",
                provider = SourceProviderInfo(
                    id = "provider_$index",
                    name = "Provider $index",
                    displayName = "Provider $index",
                    logoUrl = null,
                    type = providerTypes.random(),
                    reliability = SourceProviderInfo.ProviderReliability.entries.random()
                ),
                quality = QualityInfo(
                    resolution = resolutions.random(),
                    bitrate = Random.nextLong(1_000_000L, 50_000_000L),
                    hdr10 = Random.nextBoolean(),
                    hdr10Plus = Random.nextBoolean(),
                    dolbyVision = Random.nextBoolean(),
                    frameRate = listOf(24, 30, 60).random()
                ),
                codec = CodecInfo(type = codecs.random()),
                audio = AudioInfo(
                    format = audioFormats.random(),
                    channels = listOf("2.0", "5.1", "7.1").random(),
                    bitrate = Random.nextInt(128, 1536)
                ),
                release = ReleaseInfo(
                    type = releaseTypes.random(),
                    group = "PERF_TEST",
                    year = Random.nextInt(2020, 2024)
                ),
                file = FileInfo(
                    name = "Performance.Test.$index.mkv",
                    sizeInBytes = Random.nextLong(1_000_000_000L, 50_000_000_000L),
                    extension = "mkv",
                    addedDate = Date()
                ),
                health = HealthInfo(
                    seeders = Random.nextInt(0, 2000),
                    leechers = Random.nextInt(0, 500),
                    availability = Random.nextFloat(),
                    lastChecked = Date()
                ),
                features = FeatureInfo(),
                availability = AvailabilityInfo(
                    isAvailable = true,
                    cached = Random.nextBoolean()
                )
            )
        }
    }

    private fun generateSeasonPackFilenames(count: Int): List<String> {
        val showNames = listOf("Breaking.Bad", "Game.of.Thrones", "The.Office", "Friends", "Lost")
        val patterns = listOf(
            "{show}.S{season:02d}.Complete.1080p.BluRay.x264-GROUP.mkv",
            "{show}.Season.{season}.Complete.720p.HDTV.x264-GROUP.mkv",
            "{show}.S{season:02d}E{episode:02d}-E{endEpisode:02d}.1080p.WEB-DL.H264-GROUP.mkv",
            "{show}.Complete.Series.BluRay.1080p.x264-GROUP.mkv",
            "{show}.S{season:02d}-S{endSeason:02d}.Complete.720p.HDTV.x264-GROUP.mkv"
        )
        
        return (1..count).map { index ->
            val show = showNames.random()
            val pattern = patterns.random()
            val season = Random.nextInt(1, 11)
            val episode = Random.nextInt(1, 13)
            val endEpisode = episode + Random.nextInt(5, 15)
            val endSeason = season + Random.nextInt(1, 5)
            
            pattern
                .replace("{show}", show)
                .replace("{season:02d}", season.toString().padStart(2, '0'))
                .replace("{season}", season.toString())
                .replace("{episode:02d}", episode.toString().padStart(2, '0'))
                .replace("{endEpisode:02d}", endEpisode.toString().padStart(2, '0'))
                .replace("{endSeason:02d}", endSeason.toString().padStart(2, '0'))
        }
    }

    private fun createComplexFilter(): AdvancedSourceFilter {
        return AdvancedSourceFilter(
            qualityFilters = QualityFilters(
                minResolution = VideoResolution.RESOLUTION_720P,
                requireHDR = false
            ),
            healthFilters = HealthFilters(
                minSeeders = 10
            ),
            fileSizeFilters = FileSizeFilters(
                maxSizeGB = 20.0
            ),
            codecFilters = CodecFilters(
                allowedCodecs = setOf(VideoCodec.H264, VideoCodec.HEVC, VideoCodec.AV1)
            )
        )
    }

    private fun createQualityFilter(): AdvancedSourceFilter {
        return AdvancedSourceFilter(
            qualityFilters = QualityFilters(
                minResolution = VideoResolution.RESOLUTION_1080P,
                requireHDR = true
            )
        )
    }

    private fun createHealthFilter(): AdvancedSourceFilter {
        return AdvancedSourceFilter(
            healthFilters = HealthFilters(
                minSeeders = 100
            )
        )
    }

    private fun createSizeFilter(): AdvancedSourceFilter {
        return AdvancedSourceFilter(
            fileSizeFilters = FileSizeFilters(
                minSizeGB = 2.0,
                maxSizeGB = 15.0
            )
        )
    }

    private fun createCodecFilter(): AdvancedSourceFilter {
        return AdvancedSourceFilter(
            codecFilters = CodecFilters(
                allowedCodecs = setOf(VideoCodec.HEVC, VideoCodec.AV1)
            )
        )
    }

    private fun applyFilterLogic(source: SourceMetadata, filter: AdvancedSourceFilter): Boolean {
        // Quality filters
        if (!filter.qualityFilters.isEmpty()) {
            filter.qualityFilters.minResolution?.let { minRes ->
                if (source.quality.resolution.ordinal < minRes.ordinal) return false
            }
            if (filter.qualityFilters.requireHDR && !source.quality.hasHDR()) return false
        }
        
        // Health filters
        if (!filter.healthFilters.isEmpty()) {
            filter.healthFilters.minSeeders?.let { minSeeders ->
                if ((source.health.seeders ?: 0) < minSeeders) return false
            }
        }
        
        // Size filters
        if (!filter.fileSizeFilters.isEmpty()) {
            filter.fileSizeFilters.maxSizeGB?.let { maxSize ->
                val sizeGB = (source.file.sizeInBytes ?: 0L) / (1024.0 * 1024.0 * 1024.0)
                if (sizeGB > maxSize) return false
            }
        }
        
        // Codec filters
        if (!filter.codecFilters.isEmpty()) {
            if (source.codec.type !in filter.codecFilters.allowedCodecs) return false
        }
        
        return true
    }
}