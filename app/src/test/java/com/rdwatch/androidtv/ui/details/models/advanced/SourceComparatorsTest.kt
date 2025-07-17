package com.rdwatch.androidtv.ui.details.models.advanced

import com.rdwatch.androidtv.ui.details.models.SourceSortOption
import org.junit.Before
import org.junit.Test
import java.util.Date
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Comprehensive tests for SourceComparators
 * Tests all sorting algorithms and comparator combinations
 */
class SourceComparatorsTest {
    private lateinit var sources: List<SourceMetadata>
    private lateinit var source4K: SourceMetadata
    private lateinit var source1080p: SourceMetadata
    private lateinit var source720p: SourceMetadata
    private lateinit var sourceHighSeeders: SourceMetadata
    private lateinit var sourceLowSeeders: SourceMetadata
    private lateinit var sourceDebrid: SourceMetadata
    private lateinit var sourceTorrent: SourceMetadata
    private lateinit var sourceRemux: SourceMetadata
    private lateinit var sourceWebDL: SourceMetadata

    @Before
    fun setup() {
        source4K =
            createTestSource(
                id = "4k_source",
                resolution = VideoResolution.RESOLUTION_4K,
                seeders = 50,
                codec = VideoCodec.HEVC,
                releaseType = ReleaseType.BLURAY,
                size = 15_000_000_000L,
            )

        source1080p =
            createTestSource(
                id = "1080p_source",
                resolution = VideoResolution.RESOLUTION_1080P,
                seeders = 200,
                codec = VideoCodec.H264,
                releaseType = ReleaseType.BLURAY,
                size = 8_000_000_000L,
            )

        source720p =
            createTestSource(
                id = "720p_source",
                resolution = VideoResolution.RESOLUTION_720P,
                seeders = 500,
                codec = VideoCodec.H264,
                releaseType = ReleaseType.WEBRIP,
                size = 3_000_000_000L,
            )

        sourceHighSeeders =
            createTestSource(
                id = "high_seeders",
                resolution = VideoResolution.RESOLUTION_1080P,
                seeders = 1000,
                codec = VideoCodec.H264,
                releaseType = ReleaseType.BLURAY,
                size = 6_000_000_000L,
            )

        sourceLowSeeders =
            createTestSource(
                id = "low_seeders",
                resolution = VideoResolution.RESOLUTION_1080P,
                seeders = 5,
                codec = VideoCodec.H264,
                releaseType = ReleaseType.BLURAY,
                size = 7_000_000_000L,
            )

        sourceDebrid =
            createTestSource(
                id = "debrid_source",
                resolution = VideoResolution.RESOLUTION_1080P,
                seeders = 0,
                codec = VideoCodec.H264,
                releaseType = ReleaseType.BLURAY,
                size = 8_000_000_000L,
                providerType = SourceProviderInfo.ProviderType.DEBRID,
                cached = true,
            )

        sourceTorrent =
            createTestSource(
                id = "torrent_source",
                resolution = VideoResolution.RESOLUTION_1080P,
                seeders = 100,
                codec = VideoCodec.H264,
                releaseType = ReleaseType.BLURAY,
                size = 8_000_000_000L,
                providerType = SourceProviderInfo.ProviderType.TORRENT,
            )

        sourceRemux =
            createTestSource(
                id = "remux_source",
                resolution = VideoResolution.RESOLUTION_1080P,
                seeders = 75,
                codec = VideoCodec.H264,
                releaseType = ReleaseType.BLURAY_REMUX,
                size = 25_000_000_000L,
            )

        sourceWebDL =
            createTestSource(
                id = "webdl_source",
                resolution = VideoResolution.RESOLUTION_1080P,
                seeders = 150,
                codec = VideoCodec.H264,
                releaseType = ReleaseType.WEB_DL,
                size = 5_000_000_000L,
            )

        sources =
            listOf(
                source4K, source1080p, source720p, sourceHighSeeders, sourceLowSeeders,
                sourceDebrid, sourceTorrent, sourceRemux, sourceWebDL,
            )
    }

    private fun createTestSource(
        id: String,
        resolution: VideoResolution,
        seeders: Int,
        codec: VideoCodec,
        releaseType: ReleaseType,
        size: Long,
        providerType: SourceProviderInfo.ProviderType = SourceProviderInfo.ProviderType.TORRENT,
        cached: Boolean = false,
    ): SourceMetadata {
        return SourceMetadata(
            id = id,
            provider =
                SourceProviderInfo(
                    id = "provider_$id",
                    name = "Provider $id",
                    displayName = "Provider $id",
                    logoUrl = null,
                    type = providerType,
                    reliability = SourceProviderInfo.ProviderReliability.GOOD,
                ),
            quality =
                QualityInfo(
                    resolution = resolution,
                    bitrate = 8_000_000L,
                    hdr10 = false,
                    hdr10Plus = false,
                    dolbyVision = false,
                    frameRate = 24,
                ),
            codec =
                CodecInfo(
                    type = codec,
                    profile = "High",
                    level = "4.1",
                ),
            audio =
                AudioInfo(
                    format = AudioFormat.AC3,
                    channels = "5.1",
                    bitrate = 640,
                    language = "en",
                    dolbyAtmos = false,
                    dtsX = false,
                ),
            release =
                ReleaseInfo(
                    type = releaseType,
                    group = "TEST",
                    edition = null,
                    year = 2023,
                ),
            file =
                FileInfo(
                    name = "$id.mkv",
                    sizeInBytes = size,
                    extension = "mkv",
                    hash = "${id}_hash",
                    addedDate = Date(),
                ),
            health =
                HealthInfo(
                    seeders = seeders,
                    leechers = seeders / 4,
                    downloadSpeed = 5_000_000L,
                    uploadSpeed = 1_000_000L,
                    availability = 1.0f,
                    lastChecked = Date(),
                ),
            features = FeatureInfo(),
            availability =
                AvailabilityInfo(
                    isAvailable = true,
                    cached = cached,
                ),
        )
    }

    @Test
    fun `qualityComparator - sorts by quality score correctly`() {
        val sorted = sources.sortedWith(SourceComparators.qualityComparator)

        // First source should have highest quality score
        val firstSource = sorted.first()
        val lastSource = sorted.last()

        assertTrue(
            firstSource.getQualityScore() >= lastSource.getQualityScore(),
            "First source should have higher quality score than last",
        )

        // 4K should generally rank high despite lower seeders
        assertTrue(
            sorted.indexOf(source4K) < sorted.indexOf(source720p),
            "4K should rank higher than 720p in quality sort",
        )
    }

    @Test
    fun `healthComparator - sorts by seeder count correctly`() {
        val sorted = sources.sortedWith(SourceComparators.healthComparator)

        // High seeder source should be first
        assertEquals(sourceHighSeeders.id, sorted.first().id, "High seeder source should be first")

        // Low seeder source should be near the end
        assertTrue(
            sorted.indexOf(sourceLowSeeders) > sorted.indexOf(sourceHighSeeders),
            "Low seeder source should rank lower than high seeder source",
        )

        // Verify seeder order
        for (i in 0 until sorted.size - 1) {
            val current = sorted[i].health.seeders ?: -1
            val next = sorted[i + 1].health.seeders ?: -1
            assertTrue(
                current >= next,
                "Sources should be sorted by seeders descending",
            )
        }
    }

    @Test
    fun `sizeComparator - sorts by file size correctly`() {
        val sorted = sources.sortedWith(SourceComparators.sizeComparator)

        // Smallest file should be first
        assertTrue(
            sorted.first().file.sizeInBytes!! <= sorted.last().file.sizeInBytes!!,
            "Smallest file should be first",
        )

        // Verify size order (ascending)
        for (i in 0 until sorted.size - 1) {
            val current = sorted[i].file.sizeInBytes ?: Long.MAX_VALUE
            val next = sorted[i + 1].file.sizeInBytes ?: Long.MAX_VALUE
            assertTrue(
                current <= next,
                "Sources should be sorted by size ascending",
            )
        }
    }

    @Test
    fun `sizeLargestFirstComparator - sorts by file size descending`() {
        val sorted = sources.sortedWith(SourceComparators.sizeLargestFirstComparator)

        // Largest file should be first
        assertTrue(
            sorted.first().file.sizeInBytes!! >= sorted.last().file.sizeInBytes!!,
            "Largest file should be first",
        )

        // REMUX (25GB) should be first due to large size
        assertEquals(sourceRemux.id, sorted.first().id, "REMUX should be first (largest)")
    }

    @Test
    fun `providerReliabilityComparator - sorts by provider reliability`() {
        val sorted = sources.sortedWith(SourceComparators.providerReliabilityComparator)

        // All test sources have GOOD reliability, so should sort by display name
        for (i in 0 until sorted.size - 1) {
            assertTrue(
                sorted[i].provider.reliability.ordinal >= sorted[i + 1].provider.reliability.ordinal,
                "Sources should be sorted by reliability descending",
            )
        }
    }

    @Test
    fun `releaseTypeComparator - sorts by release quality`() {
        val sorted = sources.sortedWith(SourceComparators.releaseTypeComparator)

        // REMUX should be first (highest quality release type)
        assertEquals(sourceRemux.id, sorted.first().id, "REMUX should be first")

        // Verify release type ordering
        val remuxIndex = sorted.indexOfFirst { it.release.type == ReleaseType.BLURAY_REMUX }
        val blurayIndex = sorted.indexOfFirst { it.release.type == ReleaseType.BLURAY }
        val webdlIndex = sorted.indexOfFirst { it.release.type == ReleaseType.WEB_DL }
        val webripIndex = sorted.indexOfFirst { it.release.type == ReleaseType.WEBRIP }

        assertTrue(remuxIndex < blurayIndex, "REMUX should rank higher than BluRay")
        assertTrue(blurayIndex < webdlIndex, "BluRay should rank higher than WEB-DL")
        assertTrue(webdlIndex < webripIndex, "WEB-DL should rank higher than WebRip")
    }

    @Test
    fun `cachedFirstComparator - prioritizes cached sources`() {
        val sorted = sources.sortedWith(SourceComparators.cachedFirstComparator)

        // Cached debrid source should be first
        assertEquals(sourceDebrid.id, sorted.first().id, "Cached source should be first")

        // All cached sources should come before non-cached
        val firstNonCached = sorted.indexOfFirst { !it.availability.cached }
        val lastCached = sorted.indexOfLast { it.availability.cached }

        if (lastCached != -1 && firstNonCached != -1) {
            assertTrue(
                lastCached < firstNonCached,
                "All cached sources should come before non-cached sources",
            )
        }
    }

    @Test
    fun `debridFirstComparator - prioritizes debrid sources`() {
        val sorted = sources.sortedWith(SourceComparators.debridFirstComparator)

        // Debrid source should be first
        assertEquals(sourceDebrid.id, sorted.first().id, "Debrid source should be first")

        // Verify provider type ordering
        for (i in 0 until sorted.size - 1) {
            val currentType = sorted[i].provider.type
            val nextType = sorted[i + 1].provider.type

            val currentPriority =
                when (currentType) {
                    SourceProviderInfo.ProviderType.DEBRID -> 0
                    SourceProviderInfo.ProviderType.DIRECT_STREAM -> 1
                    SourceProviderInfo.ProviderType.TORRENT -> 2
                    else -> 3
                }

            val nextPriority =
                when (nextType) {
                    SourceProviderInfo.ProviderType.DEBRID -> 0
                    SourceProviderInfo.ProviderType.DIRECT_STREAM -> 1
                    SourceProviderInfo.ProviderType.TORRENT -> 2
                    else -> 3
                }

            assertTrue(
                currentPriority <= nextPriority,
                "Provider types should be sorted correctly",
            )
        }
    }

    @Test
    fun `androidTVOptimizedComparator - optimizes for Android TV playback`() {
        val sorted = sources.sortedWith(SourceComparators.androidTVOptimizedComparator)

        // Cached debrid should be first (best for Android TV)
        assertEquals(sourceDebrid.id, sorted.first().id, "Cached debrid should be first for Android TV")

        // High seeder torrents should rank better than low seeder ones
        val highSeederIndex = sorted.indexOf(sourceHighSeeders)
        val lowSeederIndex = sorted.indexOf(sourceLowSeeders)

        assertTrue(
            highSeederIndex < lowSeederIndex,
            "High seeder torrents should rank higher for Android TV",
        )
    }

    @Test
    fun `createComparator - returns correct comparator for sort option`() {
        val qualityComparator = SourceComparators.createComparator(SourceSortOption.QUALITY_SCORE)
        val healthComparator = SourceComparators.createComparator(SourceSortOption.SEEDERS)
        val sizeComparator = SourceComparators.createComparator(SourceSortOption.FILE_SIZE)
        val recentComparator = SourceComparators.createComparator(SourceSortOption.ADDED_DATE)
        val providerComparator = SourceComparators.createComparator(SourceSortOption.PROVIDER)
        val releaseComparator = SourceComparators.createComparator(SourceSortOption.RELEASE_TYPE)

        // Test that they are different comparators
        assertTrue(qualityComparator !== healthComparator, "Should return different comparator instances")

        // Test that they produce different orderings
        val qualitySorted = sources.sortedWith(qualityComparator)
        val healthSorted = sources.sortedWith(healthComparator)

        // Unless by coincidence, these should produce different orderings
        val differentOrder = qualitySorted.zip(healthSorted).any { (a, b) -> a.id != b.id }
        assertTrue(differentOrder, "Different comparators should generally produce different orderings")
    }

    @Test
    fun `createCompositeComparator - combines multiple comparators correctly`() {
        val compositeComparator =
            SourceComparators.createCompositeComparator(
                primary = SourceComparators.cachedFirstComparator,
                secondary = SourceComparators.qualityComparator,
                tertiary = SourceComparators.healthComparator,
            )

        val sorted = sources.sortedWith(compositeComparator)

        // Cached sources should still be first
        assertEquals(sourceDebrid.id, sorted.first().id, "Primary comparator should take precedence")

        // Among non-cached sources, quality should be secondary criteria
        val nonCachedSources = sorted.filter { !it.availability.cached }
        if (nonCachedSources.size > 1) {
            val firstNonCached = nonCachedSources[0]
            val secondNonCached = nonCachedSources[1]

            assertTrue(
                firstNonCached.getQualityScore() >= secondNonCached.getQualityScore(),
                "Secondary comparator should order non-cached sources by quality",
            )
        }
    }

    @Test
    fun `createWeightedComparator - balances multiple criteria`() {
        val weightedComparator =
            SourceComparators.createWeightedComparator(
                qualityWeight = 1.0f,
                healthWeight = 0.5f,
                sizeWeight = 0.2f,
                providerWeight = 0.3f,
            )

        val sorted = sources.sortedWith(weightedComparator)

        // Should produce a balanced ranking
        assertNotNull(sorted.first())
        assertTrue(sorted.isNotEmpty())

        // Higher quality sources should generally rank well
        val source4KIndex = sorted.indexOf(source4K)
        val source720pIndex = sorted.indexOf(source720p)

        assertTrue(
            source4KIndex < source720pIndex,
            "Higher quality should rank better in weighted sort",
        )
    }

    @Test
    fun `chain - combines multiple comparators in order`() {
        val chainedComparator =
            SourceComparators.chain(
                SourceComparators.cachedFirstComparator,
                SourceComparators.qualityComparator,
                SourceComparators.healthComparator,
            )

        val sorted = sources.sortedWith(chainedComparator)

        // Should behave like composite comparator
        assertEquals(sourceDebrid.id, sorted.first().id, "First comparator should take precedence")
    }

    @Test
    fun `extension functions work correctly`() {
        val qualitySorted = sources.sortByQuality()
        val healthSorted = sources.sortByHealth()
        val sizeSorted = sources.sortBySize()
        val sizeLargestFirst = sources.sortBySize(largestFirst = true)
        val cachedFirst = sources.sortCachedFirst()
        val androidTVSorted = sources.sortForAndroidTV()

        // Verify they all return sorted lists
        assertEquals(sources.size, qualitySorted.size, "Quality sorted should preserve count")
        assertEquals(sources.size, healthSorted.size, "Health sorted should preserve count")
        assertEquals(sources.size, sizeSorted.size, "Size sorted should preserve count")
        assertEquals(sources.size, sizeLargestFirst.size, "Size largest first should preserve count")
        assertEquals(sources.size, cachedFirst.size, "Cached first should preserve count")
        assertEquals(sources.size, androidTVSorted.size, "Android TV sorted should preserve count")

        // Verify size sorting directions are different
        assertEquals(sizeSorted.first().id, sizeLargestFirst.last().id, "Smallest first should be largest last")
        assertEquals(sizeSorted.last().id, sizeLargestFirst.first().id, "Largest first should be smallest last")

        // Cached first should put cached sources first
        assertEquals(sourceDebrid.id, cachedFirst.first().id, "Cached first should prioritize cached sources")

        // Android TV should put cached debrid first
        assertEquals(sourceDebrid.id, androidTVSorted.first().id, "Android TV should prioritize cached debrid")
    }

    @Test
    fun `SortingStability makeStable produces deterministic results`() {
        val unstableComparator = compareBy<SourceMetadata> { 0 } // Always returns 0 (unstable)
        val stableComparator = SortingStability.makeStable(unstableComparator)

        val sorted1 = sources.sortedWith(stableComparator)
        val sorted2 = sources.sortedWith(stableComparator)

        // Should produce identical results
        assertEquals(sorted1.map { it.id }, sorted2.map { it.id }, "Stable comparator should be deterministic")
    }

    @Test
    fun `SortingStability createDeterministic produces consistent ordering`() {
        val deterministicComparator = SortingStability.createDeterministic()

        val sorted1 = sources.sortedWith(deterministicComparator)
        val sorted2 = sources.sortedWith(deterministicComparator)

        // Should produce identical results
        assertEquals(sorted1.map { it.id }, sorted2.map { it.id }, "Deterministic comparator should be consistent")

        // Should sort by ID primarily
        val sortedByIdManually = sources.sortedBy { it.id }
        assertEquals(sortedByIdManually.map { it.id }, sorted1.map { it.id }, "Should sort by ID")
    }

    @Test
    fun `complex sorting scenario with multiple criteria`() {
        // Create sources that will test tie-breaking
        val sources =
            listOf(
                createTestSource("a", VideoResolution.RESOLUTION_1080P, 100, VideoCodec.H264, ReleaseType.BLURAY, 8_000_000_000L),
                createTestSource("b", VideoResolution.RESOLUTION_1080P, 100, VideoCodec.HEVC, ReleaseType.BLURAY, 8_000_000_000L),
                createTestSource("c", VideoResolution.RESOLUTION_1080P, 100, VideoCodec.H264, ReleaseType.BLURAY_REMUX, 8_000_000_000L),
            )

        val sorted = sources.sortedWith(SourceComparators.qualityComparator)

        // Source with REMUX should rank highest
        assertEquals("c", sorted.first().id, "REMUX should rank highest")

        // Source with HEVC should rank higher than H.264
        val hevcIndex = sorted.indexOfFirst { it.id == "b" }
        val h264Index = sorted.indexOfFirst { it.id == "a" }
        assertTrue(hevcIndex < h264Index, "HEVC should rank higher than H.264 with same other criteria")
    }
}
