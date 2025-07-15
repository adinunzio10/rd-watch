package com.rdwatch.androidtv.ui.details.models.advanced

import org.junit.Test
import org.junit.Before
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import java.util.Date

/**
 * Comprehensive tests for SourceMetadata model
 * Tests quality scoring, badge generation, and filtering functionality
 */
class SourceMetadataTest {

    private lateinit var testSourceMetadata: SourceMetadata
    private lateinit var healthMonitor: HealthMonitor

    @Before
    fun setup() {
        healthMonitor = HealthMonitor()
        testSourceMetadata = createTestSourceMetadata()
    }

    private fun createTestSourceMetadata(
        resolution: VideoResolution = VideoResolution.RESOLUTION_1080P,
        codec: VideoCodec = VideoCodec.H264,
        audioFormat: AudioFormat = AudioFormat.AC3,
        releaseType: ReleaseType = ReleaseType.BLURAY,
        seeders: Int = 100,
        fileSize: Long = 4_000_000_000L
    ): SourceMetadata {
        return SourceMetadata(
            id = "test_source_123",
            provider = SourceProviderInfo(
                id = "test_provider",
                name = "Test Provider",
                displayName = "Test Provider",
                logoUrl = null,
                type = SourceProviderInfo.ProviderType.TORRENT,
                reliability = SourceProviderInfo.ProviderReliability.GOOD
            ),
            quality = QualityInfo(
                resolution = resolution,
                bitrate = 8_000_000L,
                hdr10 = false,
                hdr10Plus = false,
                dolbyVision = false,
                frameRate = 24
            ),
            codec = CodecInfo(
                type = codec,
                profile = "High",
                level = "4.1"
            ),
            audio = AudioInfo(
                format = audioFormat,
                channels = "5.1",
                bitrate = 640,
                language = "en",
                dolbyAtmos = false,
                dtsX = false
            ),
            release = ReleaseInfo(
                type = releaseType,
                group = "TEST",
                edition = null,
                year = 2023
            ),
            file = FileInfo(
                name = "Test.Movie.2023.1080p.BluRay.x264-TEST.mkv",
                sizeInBytes = fileSize,
                extension = "mkv",
                hash = "abcd1234567890ef",
                addedDate = Date()
            ),
            health = HealthInfo(
                seeders = seeders,
                leechers = 25,
                downloadSpeed = 5_000_000L,
                uploadSpeed = 1_000_000L,
                availability = 1.0f,
                lastChecked = Date()
            ),
            features = FeatureInfo(
                subtitles = listOf(
                    SubtitleInfo("English", "en", SubtitleInfo.SubtitleType.EMBEDDED)
                ),
                has3D = false,
                hasChapters = true,
                hasMultipleAudioTracks = false,
                isDirectPlay = true,
                requiresTranscoding = false
            ),
            availability = AvailabilityInfo(
                isAvailable = true,
                region = "US",
                expiryDate = null,
                debridService = null,
                cached = false
            )
        )
    }

    @Test
    fun `getQualityScore - calculates basic quality score correctly`() {
        val score = testSourceMetadata.getQualityScore()
        
        // Should include resolution base score (700) + codec bonus (30) + audio bonus (30) + release bonus (90)
        // Plus seeder-based health score and provider reliability
        assertTrue(score > 850, "Quality score should be above 850, was $score")
        assertTrue(score < 1000, "Quality score should be below 1000, was $score")
    }

    @Test
    fun `getQualityScore - 4K resolution gets higher score than 1080p`() {
        val source4K = createTestSourceMetadata(resolution = VideoResolution.RESOLUTION_4K)
        val source1080p = createTestSourceMetadata(resolution = VideoResolution.RESOLUTION_1080P)
        
        val score4K = source4K.getQualityScore()
        val score1080p = source1080p.getQualityScore()
        
        assertTrue(score4K > score1080p, "4K should score higher than 1080p")
        assertEquals(200, score4K - score1080p, "4K should score 200 points higher than 1080p")
    }

    @Test
    fun `getQualityScore - HDR variants increase score correctly`() {
        val sourceBase = createTestSourceMetadata()
        val sourceHDR10 = sourceBase.copy(
            quality = sourceBase.quality.copy(hdr10 = true)
        )
        val sourceHDR10Plus = sourceBase.copy(
            quality = sourceBase.quality.copy(hdr10Plus = true)
        )
        val sourceDolbyVision = sourceBase.copy(
            quality = sourceBase.quality.copy(dolbyVision = true)
        )
        
        val baseScore = sourceBase.getQualityScore()
        val hdr10Score = sourceHDR10.getQualityScore()
        val hdr10PlusScore = sourceHDR10Plus.getQualityScore()
        val dolbyVisionScore = sourceDolbyVision.getQualityScore()
        
        assertTrue(hdr10Score > baseScore, "HDR10 should increase score")
        assertTrue(hdr10PlusScore > hdr10Score, "HDR10+ should score higher than HDR10")
        assertTrue(dolbyVisionScore > hdr10PlusScore, "Dolby Vision should score highest")
        
        assertEquals(20, hdr10Score - baseScore, "HDR10 should add 20 points")
        assertEquals(25, hdr10PlusScore - baseScore, "HDR10+ should add 25 points")
        assertEquals(30, dolbyVisionScore - baseScore, "Dolby Vision should add 30 points")
    }

    @Test
    fun `getQualityScore - codec efficiency affects score`() {
        val sourceH264 = createTestSourceMetadata(codec = VideoCodec.H264)
        val sourceHEVC = createTestSourceMetadata(codec = VideoCodec.HEVC)
        val sourceAV1 = createTestSourceMetadata(codec = VideoCodec.AV1)
        
        val h264Score = sourceH264.getQualityScore()
        val hevcScore = sourceHEVC.getQualityScore()
        val av1Score = sourceAV1.getQualityScore()
        
        assertTrue(hevcScore > h264Score, "HEVC should score higher than H.264")
        assertTrue(av1Score > hevcScore, "AV1 should score highest")
        
        assertEquals(10, hevcScore - h264Score, "HEVC should score 10 points higher than H.264")
        assertEquals(20, av1Score - h264Score, "AV1 should score 20 points higher than H.264")
    }

    @Test
    fun `getQualityScore - seeder count affects health score`() {
        val source1Seeder = createTestSourceMetadata(seeders = 1)
        val source50Seeders = createTestSourceMetadata(seeders = 50)
        val source500Seeders = createTestSourceMetadata(seeders = 500)
        val source1000Seeders = createTestSourceMetadata(seeders = 1000)
        
        val score1 = source1Seeder.getQualityScore()
        val score50 = source50Seeders.getQualityScore()
        val score500 = source500Seeders.getQualityScore()
        val score1000 = source1000Seeders.getQualityScore()
        
        assertTrue(score50 > score1, "50 seeders should score higher than 1")
        assertTrue(score500 > score50, "500 seeders should score higher than 50")
        assertTrue(score1000 > score500, "1000 seeders should score higher than 500")
    }

    @Test
    fun `getQualityScore - release type affects score`() {
        val sourceBlurayRemux = createTestSourceMetadata(releaseType = ReleaseType.BLURAY_REMUX)
        val sourceBluray = createTestSourceMetadata(releaseType = ReleaseType.BLURAY)
        val sourceWebDL = createTestSourceMetadata(releaseType = ReleaseType.WEB_DL)
        val sourceCam = createTestSourceMetadata(releaseType = ReleaseType.CAM)
        
        val remuxScore = sourceBlurayRemux.getQualityScore()
        val blurayScore = sourceBluray.getQualityScore()
        val webdlScore = sourceWebDL.getQualityScore()
        val camScore = sourceCam.getQualityScore()
        
        assertTrue(remuxScore > blurayScore, "REMUX should score higher than BluRay")
        assertTrue(blurayScore > webdlScore, "BluRay should score higher than WEB-DL")
        assertTrue(webdlScore > camScore, "WEB-DL should score higher than CAM")
    }

    @Test
    fun `getQualityScore - with advanced health data provides comprehensive scoring`() {
        val healthData = createTestHealthData()
        val score = testSourceMetadata.getQualityScore(healthData = healthData)
        val basicScore = testSourceMetadata.getQualityScore()
        
        // Advanced health data should provide more detailed scoring
        assertNotNull(score)
        assertTrue(score != basicScore, "Advanced health scoring should differ from basic")
    }

    @Test
    fun `getQualityBadges - returns expected badges for basic source`() {
        val badges = testSourceMetadata.getQualityBadges()
        
        assertTrue(badges.isNotEmpty(), "Should return at least one badge")
        
        val badgeTexts = badges.map { it.text }
        assertTrue(badgeTexts.contains("1080p"), "Should include resolution badge")
        assertTrue(badgeTexts.contains("H.264"), "Should include codec badge")
        assertTrue(badgeTexts.contains("AC3"), "Should include audio badge")
        assertTrue(badgeTexts.contains("BluRay"), "Should include release type badge")
    }

    @Test
    fun `getQualityBadges - HDR badges appear correctly`() {
        val sourceDolbyVision = testSourceMetadata.copy(
            quality = testSourceMetadata.quality.copy(dolbyVision = true)
        )
        val sourceHDR10Plus = testSourceMetadata.copy(
            quality = testSourceMetadata.quality.copy(hdr10Plus = true)
        )
        val sourceHDR10 = testSourceMetadata.copy(
            quality = testSourceMetadata.quality.copy(hdr10 = true)
        )
        
        val dvBadges = sourceDolbyVision.getQualityBadges()
        val hdr10PlusBadges = sourceHDR10Plus.getQualityBadges()
        val hdr10Badges = sourceHDR10.getQualityBadges()
        
        assertTrue(dvBadges.any { it.text == "DV" }, "Should include Dolby Vision badge")
        assertTrue(hdr10PlusBadges.any { it.text == "HDR10+" }, "Should include HDR10+ badge")
        assertTrue(hdr10Badges.any { it.text == "HDR10" }, "Should include HDR10 badge")
    }

    @Test
    fun `getQualityBadges - badges are sorted by priority`() {
        val badges = testSourceMetadata.getQualityBadges()
        
        // Verify badges are sorted by priority (descending)
        for (i in 0 until badges.size - 1) {
            assertTrue(
                badges[i].priority >= badges[i + 1].priority,
                "Badges should be sorted by priority descending"
            )
        }
    }

    @Test
    fun `matchesFilter - quality filters work correctly`() {
        val filter = SourceFilter(
            minQuality = VideoResolution.RESOLUTION_720P,
            requireHDR = false,
            maxSizeGB = 5.0f
        )
        
        val source1080p = createTestSourceMetadata(resolution = VideoResolution.RESOLUTION_1080P)
        val source480p = createTestSourceMetadata(resolution = VideoResolution.RESOLUTION_480P)
        
        assertTrue(source1080p.matchesFilter(filter), "1080p should match ≥720p filter")
        assertFalse(source480p.matchesFilter(filter), "480p should not match ≥720p filter")
    }

    @Test
    fun `matchesFilter - HDR requirement works correctly`() {
        val hdrFilter = SourceFilter(requireHDR = true)
        
        val sourceNoHDR = testSourceMetadata
        val sourceWithHDR = testSourceMetadata.copy(
            quality = testSourceMetadata.quality.copy(hdr10 = true)
        )
        
        assertFalse(sourceNoHDR.matchesFilter(hdrFilter), "Non-HDR source should not match HDR filter")
        assertTrue(sourceWithHDR.matchesFilter(hdrFilter), "HDR source should match HDR filter")
    }

    @Test
    fun `matchesFilter - codec filters work correctly`() {
        val codecFilter = SourceFilter(
            codecs = setOf(VideoCodec.HEVC, VideoCodec.AV1)
        )
        
        val sourceH264 = createTestSourceMetadata(codec = VideoCodec.H264)
        val sourceHEVC = createTestSourceMetadata(codec = VideoCodec.HEVC)
        
        assertFalse(sourceH264.matchesFilter(codecFilter), "H.264 should not match HEVC/AV1 filter")
        assertTrue(sourceHEVC.matchesFilter(codecFilter), "HEVC should match HEVC/AV1 filter")
    }

    @Test
    fun `matchesFilter - file size filters work correctly`() {
        val sizeFilter = SourceFilter(maxSizeGB = 3.0f)
        
        val smallSource = createTestSourceMetadata(fileSize = 2_000_000_000L) // 2GB
        val largeSource = createTestSourceMetadata(fileSize = 5_000_000_000L) // 5GB
        
        assertTrue(smallSource.matchesFilter(sizeFilter), "2GB source should match ≤3GB filter")
        assertFalse(largeSource.matchesFilter(sizeFilter), "5GB source should not match ≤3GB filter")
    }

    @Test
    fun `matchesFilter - seeder filters work correctly`() {
        val seederFilter = SourceFilter(minSeeders = 50)
        
        val lowSeedSource = createTestSourceMetadata(seeders = 25)
        val highSeedSource = createTestSourceMetadata(seeders = 100)
        
        assertFalse(lowSeedSource.matchesFilter(seederFilter), "25 seeders should not match ≥50 filter")
        assertTrue(highSeedSource.matchesFilter(seederFilter), "100 seeders should match ≥50 filter")
    }

    @Test
    fun `matchesFilter - empty filter matches everything`() {
        val emptyFilter = SourceFilter()
        
        assertTrue(testSourceMetadata.matchesFilter(emptyFilter), "Empty filter should match everything")
    }

    @Test
    fun `FileInfo getFormattedSize works correctly`() {
        val fileInfo1KB = FileInfo(sizeInBytes = 1_024L)
        val fileInfo1MB = FileInfo(sizeInBytes = 1_048_576L)
        val fileInfo1GB = FileInfo(sizeInBytes = 1_073_741_824L)
        val fileInfo1TB = FileInfo(sizeInBytes = 1_099_511_627_776L)
        
        assertEquals("1.02 KB", fileInfo1KB.getFormattedSize())
        assertEquals("1.05 MB", fileInfo1MB.getFormattedSize())
        assertEquals("1.07 GB", fileInfo1GB.getFormattedSize())
        assertEquals("1.10 TB", fileInfo1TB.getFormattedSize())
    }

    @Test
    fun `VideoResolution fromString parses correctly`() {
        assertEquals(VideoResolution.RESOLUTION_4K, VideoResolution.fromString("4K"))
        assertEquals(VideoResolution.RESOLUTION_4K, VideoResolution.fromString("4k ultra hd"))
        assertEquals(VideoResolution.RESOLUTION_1080P, VideoResolution.fromString("1080p"))
        assertEquals(VideoResolution.RESOLUTION_1080P, VideoResolution.fromString("full hd"))
        assertEquals(VideoResolution.RESOLUTION_720P, VideoResolution.fromString("720P"))
        assertEquals(VideoResolution.UNKNOWN, VideoResolution.fromString("invalid"))
    }

    @Test
    fun `VideoCodec fromString parses correctly`() {
        assertEquals(VideoCodec.H264, VideoCodec.fromString("x264"))
        assertEquals(VideoCodec.H264, VideoCodec.fromString("avc"))
        assertEquals(VideoCodec.HEVC, VideoCodec.fromString("x265"))
        assertEquals(VideoCodec.HEVC, VideoCodec.fromString("h265"))
        assertEquals(VideoCodec.AV1, VideoCodec.fromString("av1"))
        assertEquals(VideoCodec.UNKNOWN, VideoCodec.fromString("invalid"))
    }

    @Test
    fun `AudioFormat fromString parses correctly`() {
        assertEquals(AudioFormat.TRUEHD, AudioFormat.fromString("truehd"))
        assertEquals(AudioFormat.DTS_HD_MA, AudioFormat.fromString("dts-hd ma"))
        assertEquals(AudioFormat.EAC3, AudioFormat.fromString("e-ac3"))
        assertEquals(AudioFormat.AC3, AudioFormat.fromString("ac3"))
        assertEquals(AudioFormat.UNKNOWN, AudioFormat.fromString("invalid"))
    }

    @Test
    fun `ReleaseType fromString parses correctly`() {
        assertEquals(ReleaseType.BLURAY_REMUX, ReleaseType.fromString("bluray remux"))
        assertEquals(ReleaseType.BLURAY, ReleaseType.fromString("bluray"))
        assertEquals(ReleaseType.WEB_DL, ReleaseType.fromString("web-dl"))
        assertEquals(ReleaseType.WEBRIP, ReleaseType.fromString("webrip"))
        assertEquals(ReleaseType.HDTV, ReleaseType.fromString("hdtv"))
        assertEquals(ReleaseType.UNKNOWN, ReleaseType.fromString("invalid"))
    }

    @Test
    fun `QualityInfo hasHDR works correctly`() {
        val noHDR = QualityInfo(VideoResolution.RESOLUTION_1080P)
        val withHDR10 = QualityInfo(VideoResolution.RESOLUTION_1080P, hdr10 = true)
        val withHDR10Plus = QualityInfo(VideoResolution.RESOLUTION_1080P, hdr10Plus = true)
        val withDolbyVision = QualityInfo(VideoResolution.RESOLUTION_1080P, dolbyVision = true)
        
        assertFalse(noHDR.hasHDR(), "Should not have HDR")
        assertTrue(withHDR10.hasHDR(), "Should have HDR10")
        assertTrue(withHDR10Plus.hasHDR(), "Should have HDR10+")
        assertTrue(withDolbyVision.hasHDR(), "Should have Dolby Vision")
    }

    @Test
    fun `HealthInfo getHealthStatus works correctly`() {
        val deadSource = HealthInfo(seeders = 0)
        val poorSource = HealthInfo(seeders = 3)
        val fairSource = HealthInfo(seeders = 15)
        val goodSource = HealthInfo(seeders = 30)
        val excellentSource = HealthInfo(seeders = 100)
        
        assertEquals(HealthInfo.HealthStatus.DEAD, deadSource.getHealthStatus())
        assertEquals(HealthInfo.HealthStatus.POOR, poorSource.getHealthStatus())
        assertEquals(HealthInfo.HealthStatus.FAIR, fairSource.getHealthStatus())
        assertEquals(HealthInfo.HealthStatus.GOOD, goodSource.getHealthStatus())
        assertEquals(HealthInfo.HealthStatus.EXCELLENT, excellentSource.getHealthStatus())
    }

    @Test
    fun `SourceFilter toAdvancedFilter conversion works correctly`() {
        val legacyFilter = SourceFilter(
            minQuality = VideoResolution.RESOLUTION_1080P,
            requireHDR = true,
            codecs = setOf(VideoCodec.HEVC),
            audioFormats = setOf(AudioFormat.TRUEHD),
            maxSizeGB = 10.0f,
            minSeeders = 50,
            requireCached = true
        )
        
        val advancedFilter = legacyFilter.toAdvancedFilter()
        
        assertEquals(VideoResolution.RESOLUTION_1080P, advancedFilter.qualityFilters.minResolution)
        assertTrue(advancedFilter.qualityFilters.requireHDR)
        assertTrue(advancedFilter.codecFilters.allowedCodecs.contains(VideoCodec.HEVC))
        assertTrue(advancedFilter.audioFilters.allowedFormats.contains(AudioFormat.TRUEHD))
        assertEquals(10.0, advancedFilter.fileSizeFilters.maxSizeGB)
        assertEquals(50, advancedFilter.healthFilters.minSeeders)
        assertTrue(advancedFilter.sourceTypeFilters.cachedOnly)
    }

    private fun createTestHealthData(): HealthData {
        return healthMonitor.calculateHealthScore(
            testSourceMetadata.health,
            testSourceMetadata.provider
        )
    }
}